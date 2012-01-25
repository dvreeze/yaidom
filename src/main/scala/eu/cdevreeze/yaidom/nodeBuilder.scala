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

import scala.collection.immutable

/**
 * DSL to build Elems (or Documents) without having to pass parent Scopes around.
 * Example:
 * {{{
 * import Scope.Declarations._
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
 * In https://github.com/djspiewak/anti-xml/issues/78, Daniel Spiewak explains an impedance mismatch between
 * XML's scoping rules (which are top-down, from root to leaves) and Anti-XML's functional trees (which are built bottom-up,
 * from leaves to root). One way to decrease this impedance mismatch is to distinguish between Nodes and the NodeBuilders
 * below (compare with String versus StringBuilder). Both NodeBuilders and Nodes are functional trees, but only NodeBuilders
 * are easy to construct in a bottom-up manner. On the other hand, NodeBuilders are not XML representations (scoping info
 * may be missing), whereas Nodes are XML representations (scoping info is present in each Elem). That's ok, especially if
 * Nodes are constructed from NodeBuilders. Put differently, a fundamental difference between traits ElemBuilder and Elem is
 * that ElemBuilder uses Scope.Declarations (easy to use during bottom-up construction), whereas Elem uses Scope (for
 * correctness of the XML representation w.r.t. scoping). The fundamental contradiction that Daniel Spiewak mentions does
 * not apply if we distinguish NodeBuilders from Nodes, viz. only NodeBuilders can have unbound prefixes but only Nodes have
 * (resolved) scopes.
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
  val documentElement: ElemBuilder,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder],
  val comments: immutable.IndexedSeq[CommentBuilder]) extends ParentNodeBuilder { self =>

  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  type NodeType = Document

  override def children: immutable.IndexedSeq[NodeBuilder] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[NodeBuilder](documentElement)

  def build(parentScope: Scope): Document = {
    require(parentScope == Scope.Empty, "Documents are top level nodes, so have no parent scope")

    Document(
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

final case class TextBuilder(text: String) extends NodeBuilder {
  require(text ne null)

  type NodeType = Text

  def build(parentScope: Scope): Text = Text(text)
}

final case class ProcessingInstructionBuilder(target: String, data: String) extends NodeBuilder {
  require(target ne null)
  require(data ne null)

  type NodeType = ProcessingInstruction

  def build(parentScope: Scope): ProcessingInstruction = ProcessingInstruction(target, data)
}

final case class CDataBuilder(text: String) extends NodeBuilder {
  require(text ne null)
  require(!text.containsSlice("]]>"))

  type NodeType = CData

  def build(parentScope: Scope): CData = CData(text)
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
    documentElement: ElemBuilder,
    processingInstructions: immutable.Seq[ProcessingInstructionBuilder],
    comments: immutable.Seq[CommentBuilder]): DocumentBuilder = {

    new DocumentBuilder(documentElement, processingInstructions.toIndexedSeq, comments.toIndexedSeq)
  }

  def elem(
    qname: QName,
    attributes: Map[QName, String] = Map(),
    namespaces: Scope.Declarations = new Scope.Declarations(Scope.Empty),
    children: immutable.Seq[NodeBuilder] = immutable.IndexedSeq()): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, children.toIndexedSeq)
  }

  def text(textValue: String): TextBuilder = TextBuilder(textValue)

  def processingInstruction(target: String, data: String): ProcessingInstructionBuilder =
    ProcessingInstructionBuilder(target, data)

  def cdata(textValue: String): CDataBuilder = CDataBuilder(textValue)

  def entityRef(entity: String): EntityRefBuilder = EntityRefBuilder(entity)

  def comment(textValue: String): CommentBuilder = CommentBuilder(textValue)

  /**
   * Converts a Node to a NodeBuilder, given a parent scope.
   *
   * The following must always hold: fromNode(node)(parentScope).build(parentScope) "is structurally equal to" node
   */
  def fromNode(node: Node)(parentScope: Scope): NodeBuilder = node match {
    case Text(s) => TextBuilder(s)
    case ProcessingInstruction(target, data) => ProcessingInstructionBuilder(target, data)
    case CData(s) => CDataBuilder(s)
    case EntityRef(entity) => EntityRefBuilder(entity)
    case Comment(s) => CommentBuilder(s)
    case d: Document =>
      require(parentScope == Scope.Empty, "Documents are top level nodes, so have no parent scope")

      // Recursive calls, but not tail-recursive
      new DocumentBuilder(
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
