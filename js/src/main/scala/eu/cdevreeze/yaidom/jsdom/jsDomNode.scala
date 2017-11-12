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

import java.net.URI

import scala.annotation.tailrec
import scala.collection.immutable

import org.scalajs.dom.{ raw => sjsdom }

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.convert.JsDomConversions
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
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

  final override def toString: String = wrappedNode.toString

  final override def equals(obj: Any): Boolean = obj match {
    case other: JsDomNode =>
      (other.wrappedNode == this.wrappedNode)
    case _ => false
  }

  final override def hashCode: Int = wrappedNode.hashCode
}

sealed trait CanBeDomDocumentChild extends JsDomNode with Nodes.CanBeDocumentChild

/**
 * Wrapper around `org.scalajs.dom.raw.Element`, conforming to the `eu.cdevreeze.yaidom.queryapi.BackingElemApi` API.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * By design the only state of the JsDomElem is the wrapped element. Otherwise it would be easy to cause any inconsistency
 * between this wrapper element and the wrapped element.
 */
final class JsDomElem(
  override val wrappedNode: sjsdom.Element)
    extends CanBeDomDocumentChild with ResolvedNodes.Elem with BackingElemApi with ScopedElemLike with HasParent {

  require(wrappedNode ne null) // scalastyle:off null

  type ThisElem = JsDomElem

  def thisElem: ThisElem = this

  override type DomType = sjsdom.Element

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[JsDomElem] = {
    children collect { case e: JsDomElem => e }
  }

  override def resolvedName: EName = {
    // Efficient, mainly because it bypasses the expensive Scope computation

    JsDomConversions.toEName(wrappedNode)
  }

  /**
   * The attributes as an ordered mapping from `EName`s (instead of `QName`s) to values
   */
  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    // Efficient, mainly because it bypasses the expensive Scope computation

    JsDomConversions.extractResolvedAttributes(wrappedNode.attributes)
  }

  override def qname: QName = JsDomConversions.toQName(wrappedNode)

  override def attributes: immutable.IndexedSeq[(QName, String)] = {
    JsDomConversions.extractAttributes(wrappedNode.attributes)
  }

  /**
   * Returns the Scope. It is an expensive method for this element implementation.
   */
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

  override def docUriOption: Option[URI] = {
    Option(wrappedNode.ownerDocument).flatMap(d => Option(d.documentURI)).map(u => URI.create(u))
  }

  override def docUri: URI = {
    docUriOption.getOrElse(URI.create(""))
  }

  override def baseUriOption: Option[URI] = {
    XmlBaseSupport.findBaseUriByDocUriAndPath(docUriOption, rootElem, path)(XmlBaseSupport.JdkUriResolver)
  }

  override def baseUri: URI = {
    baseUriOption.getOrElse(URI.create(""))
  }

  override def parentBaseUriOption: Option[URI] = {
    val parentPathOption = path.parentPathOption

    if (parentPathOption.isEmpty) {
      docUriOption
    } else {
      XmlBaseSupport.findBaseUriByDocUriAndPath(docUriOption, rootElem, parentPathOption.get)(XmlBaseSupport.JdkUriResolver)
    }
  }

  override def namespaces: Declarations = {
    val parentScope = parentOption map { _.scope } getOrElse (Scope.Empty)
    parentScope.relativize(scope)
  }

  /**
   * Somewhat inefficient function to get the relative Path.
   */
  override def path: Path = {
    val entriesReversed: List[Path.Entry] =
      JsDomElem.getAncestorsOrSelf(wrappedNode).dropRight(1) map { elem =>
        val ename = JsDomConversions.toEName(elem)
        val cnt = filterPreviousSiblingElements(elem, (e => JsDomConversions.toEName(e) == ename)).size
        Path.Entry(ename, cnt)
      }
    Path(entriesReversed.toIndexedSeq.reverse)
  }

  override def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem] = {
    ancestorsOrSelf.reverse
  }

  override def reverseAncestry: immutable.IndexedSeq[ThisElem] = {
    ancestors.reverse
  }

  override def reverseAncestryOrSelfENames: immutable.IndexedSeq[EName] = {
    reverseAncestryOrSelf.map(_.resolvedName)
  }

  override def reverseAncestryENames: immutable.IndexedSeq[EName] = {
    reverseAncestryOrSelfENames.init
  }

  override def rootElem: ThisElem = {
    // Recursive call

    parentOption.map(_.rootElem).getOrElse(this)
  }

  private def filterPreviousSiblingElements(elem: sjsdom.Element, p: sjsdom.Element => Boolean): List[sjsdom.Element] = {
    @tailrec
    def doFilterPreviousSiblingElements(elem: sjsdom.Element, acc: List[sjsdom.Element]): List[sjsdom.Element] = {
      val prev = elem.previousElementSibling

      if (prev eq null) {
        acc
      } else {
        val newAcc = if (p(prev)) prev :: acc else acc
        // Recursive call
        doFilterPreviousSiblingElements(prev, newAcc)
      }
    }

    doFilterPreviousSiblingElements(elem, Nil)
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
