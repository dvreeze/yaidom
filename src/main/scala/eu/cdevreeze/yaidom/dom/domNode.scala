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
package dom

import java.{ util => jutil }
import org.w3c
import scala.collection.{ immutable, mutable }
import convert.DomConversions

/**
 * Wrappers around `org.w3c.dom.Node` and subclasses, such that the wrapper around `org.w3c.dom.Element` conforms to the
 * [[eu.cdevreeze.yaidom.ParentElemLike]] API.
 *
 * Not all DOM node types are exposed via these wrappers. For example, attributes are not nodes according to the `ElemLike` API,
 * so there is no wrapper for attributes.
 *
 * Use these wrappers only if there is a specific need for them. They are not immutable, they are not thread-safe, no equality
 * is defined, they are not serializable, etc.
 *
 * The wrappers are very light-weight, and typically very short-lived. On the other hand, each query may create many wrapper
 * instances for the query results. By design, the only state of each wrapper instance is the wrapped DOM node, so changes to
 * the state of that wrapped DOM node cannot corrupt the wrapper instance.
 *
 * @author Chris de Vreeze
 */
sealed trait DomNode {

  type DomType <: w3c.dom.Node

  def wrappedNode: DomType

  final override def toString: String = wrappedNode.toString
}

/** `DomDocument` or `DomElem` node */
trait DomParentNode extends DomNode {

  final def children: immutable.IndexedSeq[DomNode] = {
    val childrenNodeList = wrappedNode.getChildNodes

    DomConversions.nodeListToIndexedSeq(childrenNodeList) flatMap { node => DomNode.wrapOption(node) }
  }
}

final class DomDocument(
  override val wrappedNode: w3c.dom.Document) extends DomParentNode {

  require(wrappedNode ne null)

  override type DomType = w3c.dom.Document

  def documentElement: DomElem = DomNode.wrapElement(wrappedNode.getDocumentElement)
}

final class DomElem(
  override val wrappedNode: w3c.dom.Element) extends DomParentNode with ElemLike[DomElem] with HasText { self =>

  require(wrappedNode ne null)

  override type DomType = w3c.dom.Element

  /** Returns the element children */
  override def allChildElems: immutable.IndexedSeq[DomElem] = children collect { case e: DomElem => e }

  override def resolvedName: EName = {
    // This is of course a very inefficient implementation

    scope.resolveQName(qname).getOrElse(sys.error("Element name '%s' should resolve to an EName in scope [%s]".format(qname, scope)))
  }

  /** The attributes as a `Map` from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against the attribute scope */
  override def resolvedAttributes: Map[EName, String] = {
    val attrScope = attributeScope

    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attrScope.resolveQName(attName).getOrElse(sys.error("Attribute name '%s' should resolve to an EName in scope [%s]".format(attName, attributeScope)))
      (expandedName -> attValue)
    }
  }

  def qname: QName = DomConversions.toQName(wrappedNode)

  def attributes: Map[QName, String] = DomConversions.extractAttributes(wrappedNode.getAttributes)

  def scope: Scope = {
    val parent = wrappedNode.getParentNode
    val parentElement: w3c.dom.Element = parent match {
      case e: w3c.dom.Element => e
      case _ => null
    }

    // Recursive, but not tail-recursive
    val parentScope = if (parentElement eq null) Scope.Empty else (new DomElem(parentElement)).scope

    parentScope.resolve(declarations)
  }

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  def attributeScope: Scope = Scope(scope.map - "")

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
}

final class DomText(override val wrappedNode: w3c.dom.Text) extends DomNode {
  require(wrappedNode ne null)

  override type DomType = w3c.dom.Text

  def text: String = wrappedNode.getData

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

final class DomProcessingInstruction(override val wrappedNode: w3c.dom.ProcessingInstruction) extends DomNode {
  require(wrappedNode ne null)

  override type DomType = w3c.dom.ProcessingInstruction
}

final class DomEntityRef(override val wrappedNode: w3c.dom.EntityReference) extends DomNode {
  require(wrappedNode ne null)

  override type DomType = w3c.dom.EntityReference
}

final class DomComment(override val wrappedNode: w3c.dom.Comment) extends DomNode {
  require(wrappedNode ne null)

  override type DomType = w3c.dom.Comment

  def text: String = wrappedNode.getData
}

object DomNode {

  def wrapOption(node: w3c.dom.Node): Option[DomNode] = {
    node match {
      case e: w3c.dom.Element => Some(new DomElem(e))
      case cdata: w3c.dom.CDATASection => Some(new DomText(cdata))
      case t: w3c.dom.Text => Some(new DomText(t))
      case pi: w3c.dom.ProcessingInstruction => Some(new DomProcessingInstruction(pi))
      case er: w3c.dom.EntityReference => Some(new DomEntityRef(er))
      case c: w3c.dom.Comment => Some(new DomComment(c))
      case _ => None
    }
  }

  def wrapDocument(doc: w3c.dom.Document): DomDocument = new DomDocument(doc)

  def wrapElement(elm: w3c.dom.Element): DomElem = new DomElem(elm)
}
