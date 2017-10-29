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
import eu.cdevreeze.yaidom.convert.JsDomConversions
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
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
sealed trait JsDomNode extends ResolvedNodes.Node {

  type DomType <: sjsdom.Node

  def wrappedNode: DomType
}

sealed trait CanBeDomDocumentChild extends JsDomNode with Nodes.CanBeDocumentChild

/**
 * Wrapper around `org.scalajs.dom.raw.Element`, conforming to the `eu.cdevreeze.yaidom.queryapi.ElemLike` API.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * By design the only state of the JsDomElem is the wrapped element. Otherwise it would be easy to cause any inconsistency
 * between this wrapper element and the wrapped element. The down-side is that computing the resolved name or resolved
 * attributes is expensive, because on each call first the in-scope namespaces are computed (by following namespace
 * declarations in the ancestry and in the element itself). This is done for reliable namespace support, independent
 * of namespace-awareness of the underlying element's document.
 *
 * This choice for reliable namespace support (see the documented properties of `ScopedElemApi`) and defensive handling
 * of mutable state makes this JsDomElem slower (when querying for resolved names or attributes) than other wrapper
 * element implementations, such as `ScalaXmlElem`. On the other hand, if the use of `org.w3c.dom` is a given, then
 * this JsDomElem makes namespace-aware querying of DOM elements far easier than direct querying of DOM elements.
 */
final class JsDomElem(
    override val wrappedNode: sjsdom.Element) extends CanBeDomDocumentChild with ResolvedNodes.Elem with ScopedElemLike with HasParent {

  require(wrappedNode ne null) // scalastyle:off null

  type ThisElem = JsDomElem

  def thisElem: ThisElem = this

  override type DomType = sjsdom.Element

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[JsDomElem] = children collect { case e: JsDomElem => e }

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

  override def qname: QName = JsDomConversions.toQName(wrappedNode)

  override def attributes: immutable.IndexedSeq[(QName, String)] = {
    JsDomConversions.extractAttributes(wrappedNode.attributes)
  }

  override def scope: Scope = {
    val ancestryOrSelf = JsDomElem.getAncestorsOrSelf(this.wrappedNode)

    val resultScope =
      ancestryOrSelf.foldRight(Scope.Empty) {
        case (wrappedElem, accScope) =>
          val decls = JsDomConversions.extractNamespaceDeclarations(wrappedElem.attributes)
          accScope.resolve(decls)
      }
    resultScope
  }

  def children: immutable.IndexedSeq[JsDomNode] = {
    val childrenNodeList = wrappedNode.childNodes

    JsDomConversions.nodeListToIndexedSeq(childrenNodeList) flatMap { node => JsDomNode.wrapNodeOption(node) }
  }

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  def attributeScope: Scope = scope.withoutDefaultNamespace

  def declarations: Declarations = {
    JsDomConversions.extractNamespaceDeclarations(wrappedNode.attributes)
  }

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[JsDomText] = children collect { case t: JsDomText => t }

  /** Returns the comment children */
  def commentChildren: immutable.IndexedSeq[JsDomComment] = children collect { case c: JsDomComment => c }

  /**
   * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  override def parentOption: Option[JsDomElem] = {
    val parentNodeOption = Option(wrappedNode.parentNode)
    val parentElemOption = parentNodeOption collect { case e: sjsdom.Element => e }
    parentElemOption map { e => JsDomNode.wrapElement(e) }
  }
}

final class JsDomText(override val wrappedNode: sjsdom.Text) extends JsDomNode with ResolvedNodes.Text {
  override type DomType = sjsdom.Text

  require(wrappedNode ne null) // scalastyle:off null

  def text: String = wrappedNode.data

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

final class JsDomProcessingInstruction(
    override val wrappedNode: sjsdom.ProcessingInstruction) extends CanBeDomDocumentChild with Nodes.ProcessingInstruction {

  require(wrappedNode ne null) // scalastyle:off null

  override type DomType = sjsdom.ProcessingInstruction

  def target: String = wrappedNode.target

  def data: String = wrappedNode.data
}

final class JsDomComment(
    override val wrappedNode: sjsdom.Comment) extends CanBeDomDocumentChild with Nodes.Comment {

  require(wrappedNode ne null) // scalastyle:off null

  override type DomType = sjsdom.Comment

  def text: String = wrappedNode.data
}

object JsDomNode {

  def wrapNodeOption(node: sjsdom.Node): Option[JsDomNode] = {
    // Pattern matching on the DOM interface type alone does not work for Saxon DOM adapters such as TextOverNodeInfo.
    // Hence the check on node type as well.

    node match {
      case e: sjsdom.Element =>
        Some(new JsDomElem(e))
      case cdata: sjsdom.CDATASection =>
        Some(new JsDomText(cdata))
      case t: sjsdom.Text =>
        Some(new JsDomText(t))
      case pi: sjsdom.ProcessingInstruction =>
        Some(new JsDomProcessingInstruction(pi))
      case c: sjsdom.Comment =>
        Some(new JsDomComment(c))
      case _ => None
    }
  }

  def wrapElement(elm: sjsdom.Element): JsDomElem = new JsDomElem(elm)
}

object CanBeDomDocumentChild {

  def wrapNodeOption(node: sjsdom.Node): Option[CanBeDomDocumentChild] = {
    // Pattern matching on the DOM interface type alone does not work for Saxon DOM adapters such as TextOverNodeInfo.
    // Hence the check on node type as well.

    node match {
      case e: sjsdom.Element =>
        Some(new JsDomElem(e))
      case pi: sjsdom.ProcessingInstruction =>
        Some(new JsDomProcessingInstruction(pi))
      case c: sjsdom.Comment =>
        Some(new JsDomComment(c))
      case _ => None
    }
  }
}

object JsDomElem {

  def apply(wrappedNode: sjsdom.Element): JsDomElem = new JsDomElem(wrappedNode)

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

object JsDomText {

  def apply(wrappedNode: sjsdom.Text): JsDomText = new JsDomText(wrappedNode)
}

object JsDomProcessingInstruction {

  def apply(wrappedNode: sjsdom.ProcessingInstruction): JsDomProcessingInstruction =
    new JsDomProcessingInstruction(wrappedNode)
}

object JsDomComment {

  def apply(wrappedNode: sjsdom.Comment): JsDomComment = new JsDomComment(wrappedNode)
}
