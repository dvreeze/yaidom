/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.dom

import java.net.URI

import scala.collection.immutable

import org.w3c

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.resolved.ResolvedNodes

/**
 * Wrappers around `org.w3c.dom.Node` and subclasses, such that the wrapper around `org.w3c.dom.Element` conforms to the
 * [[eu.cdevreeze.yaidom.queryapi.ElemLike]] API.
 *
 * Not all DOM node types are exposed via these wrappers. For example, attributes are not nodes according to the `ElemLike` API,
 * so there is no wrapper for attributes.
 *
 * Use these wrappers only if there is a specific need for them. They are not immutable, and they are not thread-safe.
 *
 * The wrappers are very light-weight, and typically very short-lived. On the other hand, each query may create many wrapper
 * instances for the query results. By design, the only state of each wrapper instance is the wrapped DOM node, so changes to
 * the state of that wrapped DOM node cannot corrupt the wrapper instance.
 *
 * @author Chris de Vreeze
 */
sealed trait DomNode extends ResolvedNodes.Node {

  type DomType <: w3c.dom.Node

  def wrappedNode: DomType

  final override def toString: String = wrappedNode.toString

  final override def equals(obj: Any): Boolean = obj match {
    case other: DomNode =>
      (other.wrappedNode == this.wrappedNode)
    case _ => false
  }

  final override def hashCode: Int = wrappedNode.hashCode
}

sealed trait CanBeDomDocumentChild extends DomNode with Nodes.CanBeDocumentChild

/**
 * Wrapper around `org.w3c.dom.Element`, conforming to the [[eu.cdevreeze.yaidom.queryapi.ElemLike]] API.
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
  override val wrappedNode: w3c.dom.Element) extends CanBeDomDocumentChild with ResolvedNodes.Elem with ScopedElemLike[DomElem] with HasParent[DomElem] { self =>

  require(wrappedNode ne null)

  override type DomType = w3c.dom.Element

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[DomElem] = children collect { case e: DomElem => e }

  override def resolvedName: EName = {
    // Not efficient, because of expensive Scope computation

    scope.resolveQNameOption(qname).getOrElse(sys.error(s"Element name '${qname}' should resolve to an EName in scope [${scope}]"))
  }

  /** The attributes as an ordered mapping from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against the attribute scope */
  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    val attrScope = attributeScope

    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attrScope.resolveQNameOption(attName).getOrElse(sys.error(s"Attribute name '${attName}' should resolve to an EName in scope [${attrScope}]"))
      (expandedName -> attValue)
    }
  }

  override def qname: QName = DomConversions.toQName(wrappedNode)

  override def attributes: immutable.IndexedSeq[(QName, String)] = DomConversions.extractAttributes(wrappedNode.getAttributes)

  override def scope: Scope = {
    val ancestryOrSelf = DomElem.getAncestorsOrSelf(this.wrappedNode)

    val resultScope =
      ancestryOrSelf.foldRight(Scope.Empty) {
        case (wrappedElem, accScope) =>
          val decls = DomConversions.extractNamespaceDeclarations(wrappedElem.getAttributes)
          accScope.resolve(decls)
      }
    resultScope
  }

  def children: immutable.IndexedSeq[DomNode] = {
    val childrenNodeList = wrappedNode.getChildNodes

    DomConversions.nodeListToIndexedSeq(childrenNodeList) flatMap { node => DomNode.wrapNodeOption(node) }
  }

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  def attributeScope: Scope = scope.withoutDefaultNamespace

  def declarations: Declarations = DomConversions.extractNamespaceDeclarations(wrappedNode.getAttributes)

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
    val parentNodeOption = Option(wrappedNode.getParentNode)
    val parentElemOption = parentNodeOption collect { case e: w3c.dom.Element => e }
    parentElemOption map { e => DomNode.wrapElement(e) }
  }
}

final class DomText(override val wrappedNode: w3c.dom.Text) extends DomNode with ResolvedNodes.Text {
  require(wrappedNode ne null)

  override type DomType = w3c.dom.Text

  def text: String = wrappedNode.getData

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

final class DomProcessingInstruction(
  override val wrappedNode: w3c.dom.ProcessingInstruction) extends CanBeDomDocumentChild with Nodes.ProcessingInstruction {

  require(wrappedNode ne null)

  override type DomType = w3c.dom.ProcessingInstruction

  def target: String = wrappedNode.getTarget

  def data: String = wrappedNode.getData
}

final class DomEntityRef(
  override val wrappedNode: w3c.dom.EntityReference) extends DomNode with Nodes.EntityRef {

  require(wrappedNode ne null)

  override type DomType = w3c.dom.EntityReference

  def entity: String = wrappedNode.getNodeName
}

final class DomComment(
  override val wrappedNode: w3c.dom.Comment) extends CanBeDomDocumentChild with Nodes.Comment {

  require(wrappedNode ne null)

  override type DomType = w3c.dom.Comment

  def text: String = wrappedNode.getData
}

object DomNode {

  def wrapNodeOption(node: w3c.dom.Node): Option[DomNode] = {
    // Pattern matching on the DOM interface type alone does not work for Saxon DOM adapters such as TextOverNodeInfo.
    // Hence the check on node type as well.

    (node, node.getNodeType) match {
      case (e: w3c.dom.Element, org.w3c.dom.Node.ELEMENT_NODE) =>
        Some(new DomElem(e))
      case (cdata: w3c.dom.CDATASection, org.w3c.dom.Node.CDATA_SECTION_NODE) =>
        Some(new DomText(cdata))
      case (t: w3c.dom.Text, org.w3c.dom.Node.TEXT_NODE) =>
        Some(new DomText(t))
      case (pi: w3c.dom.ProcessingInstruction, org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE) =>
        Some(new DomProcessingInstruction(pi))
      case (er: w3c.dom.EntityReference, org.w3c.dom.Node.ENTITY_REFERENCE_NODE) =>
        Some(new DomEntityRef(er))
      case (c: w3c.dom.Comment, org.w3c.dom.Node.COMMENT_NODE) =>
        Some(new DomComment(c))
      case _ => None
    }
  }

  def wrapElement(elm: w3c.dom.Element): DomElem = new DomElem(elm)
}

object CanBeDomDocumentChild {

  def wrapNodeOption(node: w3c.dom.Node): Option[CanBeDomDocumentChild] = {
    // Pattern matching on the DOM interface type alone does not work for Saxon DOM adapters such as TextOverNodeInfo.
    // Hence the check on node type as well.

    (node, node.getNodeType) match {
      case (e: w3c.dom.Element, org.w3c.dom.Node.ELEMENT_NODE) =>
        Some(new DomElem(e))
      case (pi: w3c.dom.ProcessingInstruction, org.w3c.dom.Node.PROCESSING_INSTRUCTION_NODE) =>
        Some(new DomProcessingInstruction(pi))
      case (c: w3c.dom.Comment, org.w3c.dom.Node.COMMENT_NODE) =>
        Some(new DomComment(c))
      case _ => None
    }
  }
}

object DomElem {

  def apply(wrappedNode: w3c.dom.Element): DomElem = new DomElem(wrappedNode)

  private def getAncestorsOrSelf(elem: w3c.dom.Element): List[w3c.dom.Element] = {
    val parentElement: w3c.dom.Element = elem.getParentNode match {
      case e: w3c.dom.Element => e
      case _                  => null
    }

    if (parentElement eq null) List(elem)
    else {
      // Recursive call
      elem :: getAncestorsOrSelf(parentElement)
    }
  }
}

object DomText {

  def apply(wrappedNode: w3c.dom.Text): DomText = new DomText(wrappedNode)
}

object DomProcessingInstruction {

  def apply(wrappedNode: w3c.dom.ProcessingInstruction): DomProcessingInstruction =
    new DomProcessingInstruction(wrappedNode)
}

object DomEntityRef {

  def apply(wrappedNode: w3c.dom.EntityReference): DomEntityRef = new DomEntityRef(wrappedNode)
}

object DomComment {

  def apply(wrappedNode: w3c.dom.Comment): DomComment = new DomComment(wrappedNode)
}
