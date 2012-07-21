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
 * DSL to build `Elem`s (or `Document`s) without having to pass parent `Scope`s around.
 * Example:
 * {{{
 * import NodeBuilder._
 * import Scope._
 *
 * elem(
 *   qname = QName("Magazine"),
 *   attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
 *   namespaces = Declarations.from("dbclass" -> "http://www.db-class.org"),
 *   children = Vector(
 *     elem(
 *       qname = QName("Title"),
 *       children = Vector(text("Newsweek"))))).build()
 * }}}
 *
 * The latter expression could also be written as follows:
 * {{{
 * elem(
 *   qname = QName("Magazine"),
 *   attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
 *   namespaces = Declarations.from("dbclass" -> "http://www.db-class.org"),
 *   children = Vector(
 *     textElem(QName("Title"), "Newsweek"))).build()
 * }}}
 *
 * There is an impedance mismatch between XML's scoping rules (which are top-down, from root to leaves) and "functional trees"
 * (which are built bottom-up, from leaves to root). In the context of the Anti-XML library, Daniel Spiewak explained this
 * impedance mismatch in https://github.com/djspiewak/anti-xml/issues/78. In yaidom, however, this impedance mismatch
 * is far less severe. Yaidom distinguishes between [[eu.cdevreeze.yaidom.Node]] and [[eu.cdevreeze.yaidom.NodeBuilder]],
 * and [[eu.cdevreeze.yaidom.Elem]] and [[eu.cdevreeze.yaidom.ElemBuilder]] in particular. `Elem`s have (fixed, resolved) `Scope`s,
 * but `ElemBuilder`s do not. Using `NodeBuilder`s, `Scope` determination is postponed. Only `ElemBuilder`s
 * can have unbound prefixes, but only `Elem`s have (resolved) scopes. Instead of a [[eu.cdevreeze.yaidom.Scope]], an `ElemBuilder`
 * has a [[eu.cdevreeze.yaidom.Scope.Declarations]].
 *
 * Another reason that the above-mentioned impedance mismatch is less of a problem in practice is that typically the XML
 * trees (as `NodeBuilder`s or directly as `Node`s) are built in a top-down manner. The [[eu.cdevreeze.yaidom.ConverterToDocument]]s
 * in package [[eu.cdevreeze.yaidom.convert]] recursively build `Elem`s in a top-down manner, possibly creating an `Elem`
 * instance (for each element) twice (first without children, and finally as a copy with children added).
 *
 * When using `NodeBuilder`s to create a `Document`, this `Document` typically contains no "ignorable whitespace". This may cause
 * the `Document` not to be pretty-printed when using a (default) [[eu.cdevreeze.yaidom.print.DocumentPrinter]] to convert the `Document`
 * to an XML string. See also the classes in package [[eu.cdevreeze.yaidom.print]].
 *
 * NodeBuilders are serializable. Serialized NodeBuilder instances may well be an interesting storage format for parsed XML stored
 * in a database. Of course, this would be a non-standard format. Moreover, as far as queries are concerned, these columns
 * are mere BLOBs (unless using Java Stored Procedures written in Scala).
 *
 * @author Chris de Vreeze
 */
sealed trait NodeBuilder extends Immutable with Serializable {

  type NodeType <: Node

  def build(parentScope: Scope): NodeType

  final def build(): NodeType = build(Scope.Empty)

  /** Returns the tree representation. See the corresponding method in [[eu.cdevreeze.yaidom.Node]]. */
  final def toTreeRepr(parentScope: Scope): String = build(parentScope).toTreeRepr(parentScope)

  /** Returns `toTreeRepr` */
  final override def toString: String = toTreeRepr(Scope.Empty)
}

trait ParentNodeBuilder extends NodeBuilder {

  def children: immutable.IndexedSeq[NodeBuilder]
}

/**
 * Builder of a yaidom Document. Called `DocBuilder` instead of `DocumentBuilder`, because often a JAXP `DocumentBuilder` is in scope too.
 */
@SerialVersionUID(1L)
final class DocBuilder(
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
    (processingInstructions ++ comments) :+ documentElement

  def build(parentScope: Scope): Document = {
    require(parentScope == Scope.Empty, "Documents are top level nodes, so have no parent scope")

    Document(
      baseUriOption = self.baseUriOption,
      documentElement = documentElement.build(parentScope),
      processingInstructions = processingInstructions map { (pi: ProcessingInstructionBuilder) => pi.build(parentScope) },
      comments = comments map { (c: CommentBuilder) => c.build(parentScope) })
  }
}

@SerialVersionUID(1L)
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

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  def withChildren(newChildren: immutable.IndexedSeq[NodeBuilder]): ElemBuilder = {
    new ElemBuilder(qname, attributes, namespaces, newChildren)
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  def plusChild(newChild: NodeBuilder): ElemBuilder = withChildren(self.children :+ newChild)

  def withChildNodes(childNodes: immutable.IndexedSeq[Node])(parentScope: Scope): ElemBuilder = {
    new ElemBuilder(
      qname = self.qname,
      attributes = self.attributes,
      namespaces = self.namespaces,
      children = childNodes map { ch => NodeBuilder.fromNode(ch)(parentScope) })
  }
}

@SerialVersionUID(1L)
final case class TextBuilder(text: String, isCData: Boolean) extends NodeBuilder {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

  type NodeType = Text

  def build(parentScope: Scope): Text = Text(text, isCData)
}

@SerialVersionUID(1L)
final case class ProcessingInstructionBuilder(target: String, data: String) extends NodeBuilder {
  require(target ne null)
  require(data ne null)

  type NodeType = ProcessingInstruction

  def build(parentScope: Scope): ProcessingInstruction = ProcessingInstruction(target, data)
}

@SerialVersionUID(1L)
final case class EntityRefBuilder(entity: String) extends NodeBuilder {
  require(entity ne null)

  type NodeType = EntityRef

  def build(parentScope: Scope): EntityRef = EntityRef(entity)
}

@SerialVersionUID(1L)
final case class CommentBuilder(text: String) extends NodeBuilder {
  require(text ne null)

  type NodeType = Comment

  def build(parentScope: Scope): Comment = Comment(text)
}

object NodeBuilder {

  def document(
    baseUriOption: Option[String] = None,
    documentElement: ElemBuilder,
    processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder] = Vector(),
    comments: immutable.IndexedSeq[CommentBuilder] = Vector()): DocBuilder = {

    new DocBuilder(
      baseUriOption map { uriString => new URI(uriString) },
      documentElement,
      processingInstructions,
      comments)
  }

  def elem(
    qname: QName,
    children: immutable.IndexedSeq[NodeBuilder]): ElemBuilder = {

    elem(qname, Map[QName, String](), children)
  }

  def elem(
    qname: QName,
    attributes: Map[QName, String],
    children: immutable.IndexedSeq[NodeBuilder]): ElemBuilder = {

    elem(qname, attributes, Scope.Declarations.Empty, children)
  }

  def elem(
    qname: QName,
    attributes: Map[QName, String],
    namespaces: Scope.Declarations,
    children: immutable.IndexedSeq[NodeBuilder]): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, children)
  }

  def text(textValue: String): TextBuilder = TextBuilder(text = textValue, isCData = false)

  def cdata(textValue: String): TextBuilder = TextBuilder(text = textValue, isCData = true)

  def processingInstruction(target: String, data: String): ProcessingInstructionBuilder =
    ProcessingInstructionBuilder(target, data)

  def entityRef(entity: String): EntityRefBuilder = EntityRefBuilder(entity)

  def comment(textValue: String): CommentBuilder = CommentBuilder(textValue)

  def textElem(qname: QName, txt: String): ElemBuilder = {
    textElem(qname, Map[QName, String](), txt)
  }

  def textElem(
    qname: QName,
    attributes: Map[QName, String],
    txt: String): ElemBuilder = {

    textElem(qname, attributes, Scope.Declarations.Empty, txt)
  }

  def textElem(
    qname: QName,
    attributes: Map[QName, String],
    namespaces: Scope.Declarations,
    txt: String): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, Vector(text(txt)))
  }

  /**
   * Converts a `Node` to a `NodeBuilder`, given a parent scope.
   *
   * The following must always hold: `fromNode(node)(parentScope).build(parentScope)` "is structurally equal to" `node`
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
      new DocBuilder(
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

  def fromDocument(doc: Document)(parentScope: Scope): DocBuilder = fromNode(doc)(parentScope).asInstanceOf[DocBuilder]

  def fromElem(elm: Elem)(parentScope: Scope): ElemBuilder = fromNode(elm)(parentScope).asInstanceOf[ElemBuilder]
}
