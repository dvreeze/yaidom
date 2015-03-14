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

package eu.cdevreeze.yaidom.queryapitests.nodeinfo

import java.net.URI

import scala.Vector
import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import net.sf.saxon.`type`.Type
import net.sf.saxon.om.AxisInfo
import net.sf.saxon.om.DocumentInfo
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.Processor

/**
 * Base class providing Saxon yaidom wrappers for query tests (using Saxon backends).
 *
 * @author Chris de Vreeze
 */
trait SaxonTestSupport {

  protected val processor = new Processor(false)

  import ENameProvider.globalENameProvider._
  import QNameProvider.globalQNameProvider._

  abstract class DomNode(val wrappedNode: NodeInfo) {

    final override def toString: String = wrappedNode.toString

    protected def nodeInfo2EName(nodeInfo: NodeInfo): EName = {
      val ns: String = nodeInfo.getURI
      val nsOption: Option[String] = if (ns == "") None else Some(ns)
      getEName(nsOption, nodeInfo.getLocalPart)
    }

    protected def nodeInfo2QName(nodeInfo: NodeInfo): QName = {
      val pref: String = nodeInfo.getPrefix
      val prefOption: Option[String] = if (pref == "") None else Some(pref)
      getQName(prefOption, nodeInfo.getLocalPart)
    }

    final override def equals(obj: Any): Boolean = obj match {
      case other: DomNode => this.wrappedNode == other.wrappedNode
      case _ => false
    }

    final override def hashCode: Int = this.wrappedNode.hashCode
  }

  abstract class DomParentNode(override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) {

    final def children: immutable.IndexedSeq[DomNode] = {
      if (!wrappedNode.hasChildNodes) Vector()
      else {
        val it = wrappedNode.iterateAxis(AxisInfo.CHILD)
        val buf = mutable.ArrayBuffer[NodeInfo]()

        while (it.next() ne null) {
          buf += it.current()
        }

        buf.toVector.flatMap(nodeInfo => DomNode.wrapNodeOption(nodeInfo))
      }
    }
  }

  final class DomDocument(
    override val wrappedNode: DocumentInfo) extends DomParentNode(wrappedNode) with DocumentApi[DomElem] {

    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.DOCUMENT)

    def uriOption: Option[URI] = Option(wrappedNode.getSystemId).map(s => new URI(s))

    def documentElement: DomElem =
      (children collectFirst { case e: DomElem => e }).getOrElse(sys.error(s"Missing document element"))
  }

  final class DomElem(
    override val wrappedNode: NodeInfo) extends DomParentNode(wrappedNode) with ScopedElemLike[DomElem] with IsNavigable[DomElem] with HasParent[DomElem] { self =>

    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.ELEMENT)

    override def findAllChildElems: immutable.IndexedSeq[DomElem] = children collect { case e: DomElem => e }

    override def resolvedName: EName = nodeInfo2EName(wrappedNode)

    override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
      val it = wrappedNode.iterateAxis(AxisInfo.ATTRIBUTE)
      val buf = mutable.ArrayBuffer[NodeInfo]()

      while (it.next() ne null) {
        buf += it.current()
      }

      buf.toVector map { nodeInfo => nodeInfo2EName(nodeInfo) -> nodeInfo.getStringValue }
    }

    override def qname: QName = nodeInfo2QName(wrappedNode)

    override def attributes: immutable.IndexedSeq[(QName, String)] = {
      val it = wrappedNode.iterateAxis(AxisInfo.ATTRIBUTE)
      val buf = mutable.ArrayBuffer[NodeInfo]()

      while (it.next() ne null) {
        buf += it.current()
      }

      buf.toVector map { nodeInfo => nodeInfo2QName(nodeInfo) -> nodeInfo.getStringValue }
    }

    override def findChildElemByPathEntry(entry: Path.Entry): Option[DomElem] = {
      val filteredChildren = findAllChildElems.toStream filter {
        case e: DomElem if e.resolvedName == entry.elementName => true
        case _ => false
      }

      val childOption = filteredChildren.drop(entry.index).headOption
      childOption
    }

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
      val parentNodeOption = Option(wrappedNode.getParent)
      val parentElemOption = parentNodeOption collect { case e: NodeInfo if e.getNodeKind == Type.ELEMENT => e }
      parentElemOption map { e => DomNode.wrapElement(e) }
    }

    override def scope: Scope = {
      val it = wrappedNode.iterateAxis(AxisInfo.NAMESPACE)
      val buf = mutable.ArrayBuffer[NodeInfo]()

      while (it.next() ne null) {
        buf += it.current()
      }

      val resultMap = {
        val result =
          buf.toVector map { nodeInfo =>
            // Not very transparent: prefix is "display name" and namespace URI is "string value"
            val prefix = nodeInfo.getDisplayName
            val nsUri = nodeInfo.getStringValue
            (prefix -> nsUri)
          }
        result.toMap
      }

      Scope.from(resultMap - "xml")
    }
  }

  final class DomText(override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) {
    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.TEXT || wrappedNode.getNodeKind == Type.WHITESPACE_TEXT)

    def text: String = wrappedNode.getStringValue

    def trimmedText: String = text.trim

    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final class DomProcessingInstruction(override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) {
    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.PROCESSING_INSTRUCTION)
  }

  final class DomComment(override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) {
    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.COMMENT)

    def text: String = wrappedNode.getStringValue
  }

  object DomNode {

    def wrapNodeOption(node: NodeInfo): Option[DomNode] = {
      node.getNodeKind match {
        case Type.ELEMENT => Some(new DomElem(node))
        case Type.TEXT => Some(new DomText(node))
        case Type.WHITESPACE_TEXT => Some(new DomText(node))
        case Type.PROCESSING_INSTRUCTION => Some(new DomProcessingInstruction(node))
        case Type.COMMENT => Some(new DomComment(node))
        case _ => None
      }
    }

    def wrapDocument(doc: DocumentInfo): DomDocument = new DomDocument(doc)

    def wrapElement(elm: NodeInfo): DomElem = new DomElem(elm)
  }
}
