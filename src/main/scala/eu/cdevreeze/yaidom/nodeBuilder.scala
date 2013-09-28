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
 * For example:
 * {{{
 * import NodeBuilder._
 *
 * elem(
 *   qname = QName("dbclass:Magazine"),
 *   attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
 *   namespaces = Declarations.from("dbclass" -> "http://www.db-class.org"),
 *   children = Vector(
 *     elem(
 *       qname = QName("dbclass:Title"),
 *       children = Vector(text("Newsweek"))))).build()
 * }}}
 *
 * The latter expression could also be written as follows:
 * {{{
 * elem(
 *   qname = QName("dbclass:Magazine"),
 *   attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
 *   namespaces = Declarations.from("dbclass" -> "http://www.db-class.org"),
 *   children = Vector(
 *     textElem(QName("dbclass:Title"), "Newsweek"))).build()
 * }}}
 *
 * There is an impedance mismatch between XML's scoping rules (which are top-down, from root to leaves) and "functional trees"
 * (which are built bottom-up, from leaves to root). In the context of the Anti-XML library, Daniel Spiewak explained this
 * impedance mismatch in https://github.com/djspiewak/anti-xml/issues/78. In yaidom, however, this impedance mismatch
 * is far less severe. Yaidom distinguishes between [[eu.cdevreeze.yaidom.Node]] and [[eu.cdevreeze.yaidom.NodeBuilder]],
 * and [[eu.cdevreeze.yaidom.Elem]] and [[eu.cdevreeze.yaidom.ElemBuilder]] in particular. `Elem`s have (fixed, resolved) `Scope`s,
 * but `ElemBuilder`s do not. Using `NodeBuilder`s, `Scope` determination is postponed. Only `ElemBuilder`s
 * can have unbound prefixes, but only `Elem`s have (resolved) scopes. Instead of a [[eu.cdevreeze.yaidom.Scope]], an `ElemBuilder`
 * has a [[eu.cdevreeze.yaidom.Declarations]].
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
 * @author Chris de Vreeze
 */
sealed trait NodeBuilder extends Immutable with Serializable {

  type NodeType <: Node

  def build(parentScope: Scope): NodeType

  /**
   * Returns `build(Scope.Empty)`
   */
  final def build(): NodeType = build(Scope.Empty)

  /** Returns the tree representation. See the corresponding method in [[eu.cdevreeze.yaidom.Node]]. */
  final def toTreeRepr(parentScope: Scope): String = build(parentScope).toTreeRepr(parentScope)

  /**
   * Returns `toTreeRepr`.
   */
  final override def toString: String = {
    val prefixes: Set[String] = this match {
      case e: ElemBuilder => e.nonDeclaredPrefixes(Scope.Empty)
      case _ => Set()
    }
    val parentScope = Scope.from(prefixes.map(pref => (pref -> "placeholder-for-namespace-uri")).toMap)
    toTreeRepr(parentScope)
  }
}

/**
 * Builder for elements. See [[eu.cdevreeze.yaidom.NodeBuilder]].
 */
@SerialVersionUID(1L)
final class ElemBuilder(
  val qname: QName,
  val attributes: immutable.IndexedSeq[(QName, String)],
  val namespaces: Declarations,
  val children: immutable.IndexedSeq[NodeBuilder]) extends NodeBuilder with ParentElemLike[ElemBuilder] with TransformableElemLike[NodeBuilder, ElemBuilder] { self =>

  require(qname ne null)
  require(attributes ne null)
  require(namespaces ne null)
  require(children ne null)

  require(attributes.toMap.size == attributes.size, "There are duplicate attribute names: %s".format(attributes))

  type NodeType = Elem

  /** Returns the element children as ElemBuilder instances */
  override def findAllChildElems: immutable.IndexedSeq[ElemBuilder] = children collect { case e: ElemBuilder => e }

  override def transformChildElems(f: ElemBuilder => ElemBuilder): ElemBuilder = {
    val newChildren =
      children map {
        case e: ElemBuilder => f(e)
        case n: NodeBuilder => n
      }
    withChildren(newChildren)
  }

  override def transformChildElemsToNodeSeq(f: ElemBuilder => immutable.IndexedSeq[NodeBuilder]): ElemBuilder = {
    val newChildren =
      children flatMap {
        case e: ElemBuilder => f(e)
        case n: NodeBuilder => Vector(n)
      }
    withChildren(newChildren)
  }

  /**
   * Creates an `Elem` from this element builder, using the passed parent scope.
   *
   * The `Scope` of the created (root) element is the passed parent scope, altered by the namespace declarations
   * in this element builder, if any.
   */
  def build(parentScope: Scope): Elem = {
    val newScope = parentScope.resolve(namespaces)

    // Recursive, but not tail-recursive, calls to the same method
    Elem(
      qname,
      attributes,
      newScope,
      children map { ch => ch.build(newScope) })
  }

  /**
   * Creates a copy, altered with the explicitly passed parameters (for qname, attributes, namespaces and children).
   */
  def copy(
    qname: QName = this.qname,
    attributes: immutable.IndexedSeq[(QName, String)] = this.attributes,
    namespaces: Declarations = this.namespaces,
    children: immutable.IndexedSeq[NodeBuilder] = this.children): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, children)
  }

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  def withChildren(newChildren: immutable.IndexedSeq[NodeBuilder]): ElemBuilder = {
    copy(children = newChildren)
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  def plusChild(newChild: NodeBuilder): ElemBuilder = withChildren(self.children :+ newChild)

  /**
   * Returns true if parentScope suffices to build an `Elem`, that is, if `build(parentScope)` returns an `Elem` instead of
   * throwing an exception.
   *
   * A complete `ElemBuilder` tree should obey property `canBuild(Scope.Empty)`.
   */
  def canBuild(parentScope: Scope): Boolean = {
    val newScope = parentScope.resolve(namespaces)

    val undeclaredPrefixesForThisElem = prefixesInElemNameAndAttributes diff (newScope.withoutDefaultNamespace.prefixNamespaceMap.keySet)

    // Recursive calls (not tail-recursive)
    undeclaredPrefixesForThisElem.isEmpty && {
      findAllChildElems forall { e => e.canBuild(newScope) }
    }
  }

  /**
   * Returns the accumulated used but non-declared prefixes throughout the tree, given the passed parentScope.
   * A prefix is considered used in an element if it is used in element name and/or attribute names.
   */
  def nonDeclaredPrefixes(parentScope: Scope): Set[String] = {
    val newScope = parentScope.resolve(namespaces)

    val undeclaredPrefixesForThisElem = prefixesInElemNameAndAttributes diff (newScope.withoutDefaultNamespace.prefixNamespaceMap.keySet)

    // Recursive calls (not tail-recursive)
    val result = children.foldLeft(undeclaredPrefixesForThisElem) { (acc, child) =>
      child match {
        case e: ElemBuilder => acc union (e.nonDeclaredPrefixes(newScope))
        case _ => acc
      }
    }
    result
  }

  /**
   * Returns true if all namespace declarations, if any, are only at top level (so at the root of this tree).
   *
   * It is good practice for a complete `ElemBuilder` tree to have only top-level namespace declarations, if any.
   */
  def allDeclarationsAreAtTopLevel: Boolean = {
    val rejected = this filterElems { e => e.namespaces != Declarations.Empty }
    rejected.isEmpty
  }

  private def prefixesInElemNameAndAttributes: Set[String] = {
    val result = (qname +: attributes.map(_._1)) flatMap { qname => qname.prefixOption }
    result.toSet
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
    uriOption: Option[String] = None,
    documentElement: ElemBuilder,
    processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder] = Vector(),
    comments: immutable.IndexedSeq[CommentBuilder] = Vector()): DocBuilder = {

    new DocBuilder(
      uriOption map { uriString => new URI(uriString) },
      documentElement,
      processingInstructions,
      comments)
  }

  def elem(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)] = Vector(),
    namespaces: Declarations = Declarations.Empty,
    children: immutable.IndexedSeq[NodeBuilder] = Vector()): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, children)
  }

  def text(textValue: String): TextBuilder = TextBuilder(text = textValue, isCData = false)

  def cdata(textValue: String): TextBuilder = TextBuilder(text = textValue, isCData = true)

  def processingInstruction(target: String, data: String): ProcessingInstructionBuilder =
    ProcessingInstructionBuilder(target, data)

  def entityRef(entity: String): EntityRefBuilder = EntityRefBuilder(entity)

  def comment(textValue: String): CommentBuilder = CommentBuilder(textValue)

  def textElem(qname: QName, txt: String): ElemBuilder = {
    textElem(qname, Vector(), txt)
  }

  def textElem(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)],
    txt: String): ElemBuilder = {

    textElem(qname, attributes, Declarations.Empty, txt)
  }

  def textElem(
    qname: QName,
    attributes: immutable.IndexedSeq[(QName, String)],
    namespaces: Declarations,
    txt: String): ElemBuilder = {

    new ElemBuilder(qname, attributes, namespaces, Vector(text(txt)))
  }

  /**
   * Converts a `Node` to a `NodeBuilder`, given a parent scope.
   *
   * The following must always hold: `fromNode(node)(parentScope).build(parentScope)` "is structurally equal to" `node`
   */
  def fromNode(node: Node)(parentScope: Scope): NodeBuilder = node match {
    case t @ Text(_, _) => fromText(t)
    case pi @ ProcessingInstruction(_, _) => fromProcessingInstruction(pi)
    case er @ EntityRef(_) => fromEntityRef(er)
    case c @ Comment(_) => fromComment(c)
    case e: Elem =>
      assert(parentScope.resolve(parentScope.relativize(e.scope)) == e.scope)

      fromElem(e)(parentScope)
  }

  /**
   * Converts an `Elem` to an `ElemBuilder`, given a parent scope.
   *
   * The resulting (root) `ElemBuilder` gets the following namespace declarations: `parentScope.relativize(elm.scope)`.
   */
  def fromElem(elm: Elem)(parentScope: Scope): ElemBuilder = {
    // Recursive call into fromNode, but not tail-recursive
    new ElemBuilder(
      qname = elm.qname,
      attributes = elm.attributes,
      namespaces = parentScope.relativize(elm.scope),
      children = elm.children map { ch => fromNode(ch)(elm.scope) })
  }

  def fromText(txt: Text): TextBuilder = TextBuilder(txt.text, txt.isCData)

  def fromProcessingInstruction(pi: ProcessingInstruction): ProcessingInstructionBuilder = ProcessingInstructionBuilder(pi.target, pi.data)

  def fromEntityRef(er: EntityRef): EntityRefBuilder = EntityRefBuilder(er.entity)

  def fromComment(comment: Comment): CommentBuilder = CommentBuilder(comment.text)
}
