/*
 * Copyright 2011 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.yaidom

import java.net.URI
import scala.collection.immutable

/**
 * DSL to build Elems (or Documents) without having to pass parent Scopes around.
 * Example:
 * {{{
 * import NodeBuilder._
 *
 * elem(
 *   qname = "Magazine".qname,
 *   attributes = Map("Month".qname -> "February", "Year".qname -> "2009"),
 *   namespaces = Map("dbclass" -> "http://www.db-class.org").namespaces,
 *   children = List(
 *     elem(
 *       qname = "Title".qname,
 *       children = List(text("Newsweek"))))).build()
 * }}}
 *
 * There is an impedance mismatch between XML's scoping rules (which are top-down, from root to leaves) and "functional trees"
 * (which are built bottom-up, from leaves to root). In the context of the Anti-XML library, Daniel Spiewak explained this
 * impedance mismatch in https://github.com/djspiewak/anti-xml/issues/78. In yaidom, however, this impedance mismatch
 * is far less severe. Yaidom distinguishes between [[eu.cdevreeze.yaidom.Node]] and [[eu.cdevreeze.yaidom.NodeBuilder]],
 * and [[eu.cdevreeze.yaidom.Elem]] and [[eu.cdevreeze.yaidom.ElemBuilder]] in particular. Elems have (fixed, resolved) Scopes,
 * but ElemBuilders do not. Using NodeBuilders, Scope determination is postponed. Only ElemBuilders
 * can have unbound prefixes, but only Elems have (resolved) scopes. Instead of a [[eu.cdevreeze.yaidom.Scope]], an ElemBuilder
 * has a [[eu.cdevreeze.yaidom.Scope.Declarations]].
 *
 * @author Chris de Vreeze
 */
sealed trait NodeBuilder extends Immutable {

  type NodeType <: Node

  def build(parentScope: Scope): NodeType

  final def build(): NodeType = build(Scope.Empty)

  /** Returns the AST as String. See the corresponding method in [[eu.cdevreeze.yaidom.Node]]. */
  final def toAstString(parentScope: Scope): String = build(parentScope).toAstString(parentScope)

  /** Returns toAstString */
  final override def toString: String = toAstString(Scope.Empty)
}

trait ParentNodeBuilder extends NodeBuilder {

  def children: immutable.IndexedSeq[NodeBuilder]
}

final class DocumentBuilder(
  val baseUriOption: Option[URI],
  val documentElement: ElemBuilder,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder],
  val comments: immutable.IndexedSeq[CommentBuilder]) extends ParentNodeBuilder { self =>

  require(baseUriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  type NodeType = Document

  override def children: immutable.IndexedSeq[NodeBuilder] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[NodeBuilder](documentElement)

  def build(parentScope: Scope): Document = {
    require(parentScope == Scope.Empty, "Documents are top level nodes, so have no parent scope")

    Document(
      baseUriOption = self.baseUriOption,
      documentElement.build(parentScope),
      processingInstructions map { pi => pi.build(parentScope) },
      comments map { c => c.build(parentScope) })
  }
}

final class ElemBuilder(
  val qname: QName,
  val attributes: Map[QName, String],
  val namespaces: Scope.Declarations,
  override val children: immutable.IndexedSeq[NodeBuilder]) extends ParentNodeBuilder { self =>

  require(qname ne null)
  require(attributes ne null)
  require(namespaces ne null)
  require(children ne null)

  type NodeType = Elem

  def build(parentScope: Scope): Elem = {
    val newScope = parentScope.resolve(namespaces)

    Elem(
      qname,
      attributes,
      newScope,
      children map { ch => ch.build(newScope) })
  }

  def withChildNodes(childNodes: immutable.Seq[Node])(parentScope: Scope): ElemBuilder = {
    new ElemBuilder(
      qname = self.qname,
      attributes = self.attributes,
      namespaces = self.namespaces,
      children = childNodes map { ch => NodeBuilder.fromNode(ch)(parentScope) } toIndexedSeq)
  }
}

final case class TextBuilder(text: String, isCData: Boolean) extends NodeBuilder with TextLike {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

  type NodeType = Text

  def build(parentScope: Scope): Text = Text(text, isCData)
}

final case class ProcessingInstructionBuilder(target: String, data: String) extends NodeBuilder {
  require(target ne null)
  require(data ne null)

  type NodeType = ProcessingInstruction

  def build(parentScope: Scope): ProcessingInstruction = ProcessingInstruction(target, data)
}

final case class EntityRefBuilder(entity: String) extends NodeBuilder {
  require(entity ne null)

  type NodeType = EntityRef

  def build(parentScope: Scope): EntityRef = EntityRef(entity)
}

final case class CommentBuilder(text: String) extends NodeBuilder {
  require(text ne null)

  type NodeType = Comment

  def build(parentScope: Scope): Comment = Comment(text)
}

object NodeBuilder {

  def document(
    baseUriOption: Option[String],
    documentElement: ElemBuilder,
    processingInstructions: immutable.Seq[ProcessingInstructionBuilder],
    comments: immutable.Seq[CommentBuilder]): DocumentBuilder = {

    new DocumentBuilder(
      baseUriOption map { uriString => new URI(uriString) },
      documentElement,
      processingInstructions.toIndexedSeq,
      comments.toIndexedSeq)
  }

  def elem(
    qname: QName,
    attributes: Map[QName, String] = Map(),
    namespaces: Scope.Declarations = new Scope.Declarations(Scope.Empty),
    children: immutable.Seq[NodeBuilder] = immutable.IndexedSeq()): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, children.toIndexedSeq)
  }

  def text(textValue: String): TextBuilder = TextBuilder(text = textValue, isCData = false)

  def cdata(textValue: String): TextBuilder = TextBuilder(text = textValue, isCData = true)

  def processingInstruction(target: String, data: String): ProcessingInstructionBuilder =
    ProcessingInstructionBuilder(target, data)

  def entityRef(entity: String): EntityRefBuilder = EntityRefBuilder(entity)

  def comment(textValue: String): CommentBuilder = CommentBuilder(textValue)

  /**
   * Converts a Node to a NodeBuilder, given a parent scope.
   *
   * The following must always hold: fromNode(node)(parentScope).build(parentScope) "is structurally equal to" node
   */
  def fromNode(node: Node)(parentScope: Scope): NodeBuilder = node match {
    case Text(s, false) => TextBuilder(s, false)
    case Text(s, true) => TextBuilder(s, true)
    case ProcessingInstruction(target, data) => ProcessingInstructionBuilder(target, data)
    case EntityRef(entity) => EntityRefBuilder(entity)
    case Comment(s) => CommentBuilder(s)
    case d: Document =>
      require(parentScope == Scope.Empty, "Documents are top level nodes, so have no parent scope")

      // Recursive calls, but not tail-recursive
      new DocumentBuilder(
        baseUriOption = d.baseUriOption,
        documentElement = fromNode(d.documentElement)(parentScope).asInstanceOf[ElemBuilder],
        processingInstructions = d.processingInstructions collect { case pi: ProcessingInstruction => fromNode(pi)(parentScope).asInstanceOf[ProcessingInstructionBuilder] },
        comments = d.comments collect { case c => fromNode(c)(parentScope).asInstanceOf[CommentBuilder] })
    case e: Elem =>
      require(parentScope.resolve(parentScope.relativize(e.scope)) == e.scope)

      // Recursive call, but not tail-recursive
      new ElemBuilder(
        qname = e.qname,
        attributes = e.attributes,
        namespaces = parentScope.relativize(e.scope),
        children = e.children map { ch => fromNode(ch)(e.scope) })
  }

  def fromDocument(doc: Document)(parentScope: Scope): DocumentBuilder = fromNode(doc)(parentScope).asInstanceOf[DocumentBuilder]

  def fromElem(elm: Elem)(parentScope: Scope): ElemBuilder = fromNode(elm)(parentScope).asInstanceOf[ElemBuilder]
}
