/*
 * Copyright 2011-2017 Chris de Vreeze
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

package eu.cdevreeze.yaidom.jsdom

import scala.collection.immutable

import org.scalajs.dom.{ raw => sjsdom }

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.resolved.ResolvedNodes

/**
 * Wrappers around `org.scalajs.dom.raw.Node` and subclasses, such that the wrapper around `org.scalajs.dom.raw.Element` conforms to the
 * `eu.cdevreeze.yaidom.queryapi.ElemLike` API.
 *
 * Not all DOM node types are exposed via these wrappers. For example, attributes are not nodes according to the `ElemLike` API,
 * so there is no wrapper for attributes.
 *
 * Use these wrappers only if there is a specific need for them. They are not immutable, and they are not thread-safe.
 * On the other hand, in the browser this is not an issue.
 *
 * The wrappers are very light-weight, and typically very short-lived. On the other hand, each query may create many wrapper
 * instances for the query results. By design, the only state of each wrapper instance is the wrapped DOM node, so changes to
 * the state of that wrapped DOM node cannot corrupt the wrapper instance.
 *
 * @author Chris de Vreeze
 */
sealed trait DomNode extends ResolvedNodes.Node {

  type DomType <: sjsdom.Node

  def wrappedNode: DomType
}

sealed trait CanBeDomDocumentChild extends DomNode with Nodes.CanBeDocumentChild

/**
 * Wrapper around `org.scalajs.dom.raw.Element`, conforming to the `eu.cdevreeze.yaidom.queryapi.ElemLike` API.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * By design the only state of the DomElem is the wrapped element. Otherwise it would be easy to cause any inconsistency
 * between this wrapper element and the wrapped element. The down-side is that computing the resolved name or resolved
 * attributes is expensive, because on each call first the in-scope namespaces are computed (by following namespace
 * declarations in the ancestry and in the element itself). This is done for reliable namespace support, independent
 * of namespace-awareness of the underlying element's document.
 *
 * This choice for reliable namespace support (see the documented properties of `ScopedElemApi`) and defensive handling
 * of mutable state makes this DomElem slower (when querying for resolved names or attributes) than other wrapper
 * element implementations, such as `ScalaXmlElem`. On the other hand, if the use of `org.w3c.dom` is a given, then
 * this DomElem makes namespace-aware querying of DOM elements far easier than direct querying of DOM elements.
 */
final class DomElem(
    override val wrappedNode: sjsdom.Element) extends CanBeDomDocumentChild with ResolvedNodes.Elem with ScopedElemLike with HasParent {

  require(wrappedNode ne null) // scalastyle:off null

  type ThisElem = DomElem

  def thisElem: ThisElem = this

  override type DomType = sjsdom.Element

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[DomElem] = children collect { case e: DomElem => e }

  override def resolvedName: EName = {
    // Not efficient, because of expensive Scope computation

    scope.resolveQNameOption(qname).getOrElse(sys.error(s"Element name '${qname}' should resolve to an EName in scope [${scope}]"))
  }

  /**
   * The attributes as an ordered mapping from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against
   * the attribute scope
   */
  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    val attrScope = attributeScope

    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName =
        attrScope.resolveQNameOption(attName).getOrElse(sys.error(s"Attribute name '${attName}' should resolve to an EName in scope [${attrScope}]"))

      (expandedName -> attValue)
    }
  }

  override def qname: QName = toQName(wrappedNode)

  override def attributes: immutable.IndexedSeq[(QName, String)] = extractAttributes(wrappedNode.attributes)

  override def scope: Scope = {
    val ancestryOrSelf = DomElem.getAncestorsOrSelf(this.wrappedNode)

    val resultScope =
      ancestryOrSelf.foldRight(Scope.Empty) {
        case (wrappedElem, accScope) =>
          val decls = extractNamespaceDeclarations(wrappedElem.attributes)
          accScope.resolve(decls)
      }
    resultScope
  }

  def children: immutable.IndexedSeq[DomNode] = {
    val childrenNodeList = wrappedNode.childNodes

    nodeListToIndexedSeq(childrenNodeList) flatMap { node => DomNode.wrapNodeOption(node) }
  }

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  def attributeScope: Scope = scope.withoutDefaultNamespace

  def declarations: Declarations = extractNamespaceDeclarations(wrappedNode.attributes)

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[DomText] = children collect { case t: DomText => t }

  /** Returns the comment children */
  def commentChildren: immutable.IndexedSeq[DomComment] = children collect { case c: DomComment => c }

  /**
   * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  override def parentOption: Option[DomElem] = {
    val parentNodeOption = Option(wrappedNode.parentNode)
    val parentElemOption = parentNodeOption collect { case e: sjsdom.Element => e }
    parentElemOption map { e => DomNode.wrapElement(e) }
  }

  /** Converts a `NamedNodeMap` to an `immutable.IndexedSeq[(QName, String)]`. Namespace declarations are skipped. */
  private def extractAttributes(domAttributes: sjsdom.NamedNodeMap): immutable.IndexedSeq[(QName, String)] = {
    (0 until domAttributes.length).flatMap(i => {
      val attr = domAttributes.item(i).asInstanceOf[sjsdom.Attr]

      if (isNamespaceDeclaration(attr)) {
        None
      } else {
        val qname: QName = toQName(attr)
        Some(qname -> attr.value)
      }
    }).toIndexedSeq
  }

  /** Converts the namespace declarations in a `NamedNodeMap` to a `Declarations` */
  private def extractNamespaceDeclarations(domAttributes: sjsdom.NamedNodeMap): Declarations = {
    val nsMap = {
      val result = (0 until domAttributes.length) flatMap { i =>
        val attr = domAttributes.item(i).asInstanceOf[sjsdom.Attr]

        if (isNamespaceDeclaration(attr)) {
          val result = extractNamespaceDeclaration(attr)
          Some(result) map { pair => (pair._1.getOrElse(""), pair._2) }
        } else {
          None
        }
      }
      result.toMap
    }
    Declarations.from(nsMap)
  }

  /** Helper method that converts a `NodeList` to an `IndexedSeq[org.scalajs.dom.raw.Node]` */
  private def nodeListToIndexedSeq(nodeList: sjsdom.NodeList): immutable.IndexedSeq[sjsdom.Node] = {
    val result = (0 until nodeList.length) map { i => nodeList.item(i) }
    result.toIndexedSeq
  }

  /** Extracts the `QName` of an `org.scalajs.dom.raw.Element` */
  private def toQName(v: sjsdom.Element)(implicit qnameProvider: QNameProvider): QName = {
    val name: String = v.tagName
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) qnameProvider.getUnprefixedQName(arr(0)) else qnameProvider.getQName(arr(0), arr(1))
  }

  /** Extracts the `QName` of an `org.scalajs.dom.raw.Attr`. If the `Attr` is a namespace declaration, an exception is thrown. */
  private def toQName(v: sjsdom.Attr)(implicit qnameProvider: QNameProvider): QName = {
    require(!isNamespaceDeclaration(v), "Namespace declaration not allowed")
    val name: String = v.name
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) qnameProvider.getUnprefixedQName(arr(0)) else qnameProvider.getQName(arr(0), arr(1))
  }

  /** Returns true if the `org.scalajs.dom.raw.Attr` is a namespace declaration */
  private def isNamespaceDeclaration(v: sjsdom.Attr): Boolean = {
    val name: String = v.name
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    val result = arr(0) == "xmlns"
    result
  }

  /** Extracts (optional) prefix and namespace. Call only if `isNamespaceDeclaration(v)`, since otherwise an exception is thrown. */
  private def extractNamespaceDeclaration(v: sjsdom.Attr): (Option[String], String) = {
    val name: String = v.name
    val arr = name.split(':')
    assert(arr.length >= 1 && arr.length <= 2)
    require(arr(0) == "xmlns")
    val prefixOption: Option[String] = if (arr.length == 1) None else Some(arr(1))
    val attrValue: String = v.value
    (prefixOption, attrValue)
  }
}

final class DomText(override val wrappedNode: sjsdom.Text) extends DomNode with ResolvedNodes.Text {
  override type DomType = sjsdom.Text

  require(wrappedNode ne null) // scalastyle:off null

  def text: String = wrappedNode.data

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

final class DomProcessingInstruction(
    override val wrappedNode: sjsdom.ProcessingInstruction) extends CanBeDomDocumentChild with Nodes.ProcessingInstruction {

  require(wrappedNode ne null) // scalastyle:off null

  override type DomType = sjsdom.ProcessingInstruction

  def target: String = wrappedNode.target

  def data: String = wrappedNode.data
}

final class DomComment(
    override val wrappedNode: sjsdom.Comment) extends CanBeDomDocumentChild with Nodes.Comment {

  require(wrappedNode ne null) // scalastyle:off null

  override type DomType = sjsdom.Comment

  def text: String = wrappedNode.data
}

object DomNode {

  def wrapNodeOption(node: sjsdom.Node): Option[DomNode] = {
    // Pattern matching on the DOM interface type alone does not work for Saxon DOM adapters such as TextOverNodeInfo.
    // Hence the check on node type as well.

    node match {
      case e: sjsdom.Element =>
        Some(new DomElem(e))
      case cdata: sjsdom.CDATASection =>
        Some(new DomText(cdata))
      case t: sjsdom.Text =>
        Some(new DomText(t))
      case pi: sjsdom.ProcessingInstruction =>
        Some(new DomProcessingInstruction(pi))
      case c: sjsdom.Comment =>
        Some(new DomComment(c))
      case _ => None
    }
  }

  def wrapElement(elm: sjsdom.Element): DomElem = new DomElem(elm)
}

object CanBeDomDocumentChild {

  def wrapNodeOption(node: sjsdom.Node): Option[CanBeDomDocumentChild] = {
    // Pattern matching on the DOM interface type alone does not work for Saxon DOM adapters such as TextOverNodeInfo.
    // Hence the check on node type as well.

    node match {
      case e: sjsdom.Element =>
        Some(new DomElem(e))
      case pi: sjsdom.ProcessingInstruction =>
        Some(new DomProcessingInstruction(pi))
      case c: sjsdom.Comment =>
        Some(new DomComment(c))
      case _ => None
    }
  }
}

object DomElem {

  def apply(wrappedNode: sjsdom.Element): DomElem = new DomElem(wrappedNode)

  private def getAncestorsOrSelf(elem: sjsdom.Element): List[sjsdom.Element] = {
    val parentElement: sjsdom.Element = elem.parentNode match {
      case e: sjsdom.Element => e
      case _                 => null
    }

    if (parentElement eq null) {
      List(elem)
    } else {
      // Recursive call
      elem :: getAncestorsOrSelf(parentElement)
    }
  }
}

object DomText {

  def apply(wrappedNode: sjsdom.Text): DomText = new DomText(wrappedNode)
}

object DomProcessingInstruction {

  def apply(wrappedNode: sjsdom.ProcessingInstruction): DomProcessingInstruction =
    new DomProcessingInstruction(wrappedNode)
}

object DomComment {

  def apply(wrappedNode: sjsdom.Comment): DomComment = new DomComment(wrappedNode)
}
