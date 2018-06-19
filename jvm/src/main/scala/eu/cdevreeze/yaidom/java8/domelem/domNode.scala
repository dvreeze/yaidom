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

package eu.cdevreeze.yaidom.java8.domelem

import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.FunctionConverters.asJavaPredicate

import org.w3c.dom.Comment
import org.w3c.dom.CDATASection
import org.w3c.dom.Element
import org.w3c.dom.EntityReference
import org.w3c.dom.Node
import org.w3c.dom.ProcessingInstruction
import org.w3c.dom.Text

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom
import eu.cdevreeze.yaidom.java8.Attr
import eu.cdevreeze.yaidom.java8.ResolvedAttr
import eu.cdevreeze.yaidom.java8.StreamUtil.toJavaStreamFunction
import eu.cdevreeze.yaidom.java8.StreamUtil.toSingletonStream
import eu.cdevreeze.yaidom.java8.StreamUtil.toStream
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedElemLike
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedNodes

/**
 * Wrapper around DOM node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class DomNode(val underlyingNode: Node) extends StreamingScopedNodes.Node

sealed abstract class CanBeDomDocumentChild(override val underlyingNode: Node)
  extends DomNode(underlyingNode) with StreamingScopedNodes.CanBeDocumentChild

/**
 * Wrapper around DOM element, offering the streaming element query API.
 */
final class DomElem(override val underlyingNode: Element)
  extends CanBeDomDocumentChild(underlyingNode) with StreamingScopedNodes.Elem[DomNode, DomElem] with StreamingScopedElemLike[DomElem] {

  def children: Stream[DomNode] = {
    val underlyingResult: Stream[dom.DomNode] =
      toSingletonStream(dom.DomElem(underlyingNode)).flatMap(toJavaStreamFunction(e => e.children))

    underlyingResult
      .map[Optional[DomNode]](asJavaFunction(n => DomNode.wrapNodeOption(n.wrappedNode)))
      .filter(asJavaPredicate(_.isPresent))
      .map[DomNode](asJavaFunction(_.get))
  }

  def findAllChildElems: Stream[DomElem] = {
    val underlyingResult: Stream[dom.DomElem] =
      toSingletonStream(dom.DomElem(underlyingNode)).flatMap(toJavaStreamFunction(e => e.findAllChildElems))

    underlyingResult.map[DomElem](asJavaFunction(e => DomElem(e.wrappedNode)))
  }

  def resolvedName: EName = {
    dom.DomElem(underlyingNode).resolvedName
  }

  def resolvedAttributes: Stream[ResolvedAttr] = {
    toStream(dom.DomElem(underlyingNode).resolvedAttributes).map[ResolvedAttr](asJavaFunction(attr => ResolvedAttr(attr._1, attr._2)))
  }

  def text: String = {
    dom.DomElem(underlyingNode).text
  }

  def qname: QName = {
    dom.DomElem(underlyingNode).qname
  }

  def attributes: Stream[Attr] = {
    toStream(dom.DomElem(underlyingNode).attributes).map[Attr](asJavaFunction(attr => Attr(attr._1, attr._2)))
  }

  def scope: Scope = {
    dom.DomElem(underlyingNode).scope
  }

  /**
   * Workaround for Scala issue SI-8905.
   */
  final override def getChildElem(p: Predicate[DomElem]): DomElem = {
    super.getChildElem(p)
  }

  override def equals(other: Any): Boolean = other match {
    case other: DomElem => this.underlyingNode == other.underlyingNode
    case _ => false
  }

  override def hashCode: Int = {
    underlyingNode.hashCode
  }
}

final class DomText(override val underlyingNode: Text)
  extends DomNode(underlyingNode) with StreamingScopedNodes.Text {

  def text: String = dom.DomText(underlyingNode).text

  def trimmedText: String = dom.DomText(underlyingNode).trimmedText

  def normalizedText: String = dom.DomText(underlyingNode).normalizedText
}

final class DomComment(override val underlyingNode: Comment)
  extends CanBeDomDocumentChild(underlyingNode) with StreamingScopedNodes.Comment {

  def text: String = dom.DomComment(underlyingNode).text
}

final class DomProcessingInstruction(override val underlyingNode: ProcessingInstruction)
  extends CanBeDomDocumentChild(underlyingNode) with StreamingScopedNodes.ProcessingInstruction {

  def target: String = dom.DomProcessingInstruction(underlyingNode).target

  def data: String = dom.DomProcessingInstruction(underlyingNode).data
}

final class DomEntityRef(override val underlyingNode: EntityReference)
  extends DomNode(underlyingNode) with StreamingScopedNodes.EntityRef {

  def entity: String = dom.DomEntityRef(underlyingNode).entity
}

object DomNode {

  def wrapNodeOption(node: Node): Optional[DomNode] = {
    // Pattern matching on the DOM interface type alone does not work for Saxon DOM adapters such as TextOverNodeInfo.
    // Hence the check on node type as well.

    (node, node.getNodeType) match {
      case (e: Element, Node.ELEMENT_NODE) =>
        Optional.of(new DomElem(e))
      case (cdata: CDATASection, Node.CDATA_SECTION_NODE) =>
        Optional.of(new DomText(cdata))
      case (t: Text, Node.TEXT_NODE) =>
        Optional.of(new DomText(t))
      case (pi: ProcessingInstruction, Node.PROCESSING_INSTRUCTION_NODE) =>
        Optional.of(new DomProcessingInstruction(pi))
      case (er: EntityReference, Node.ENTITY_REFERENCE_NODE) =>
        Optional.of(new DomEntityRef(er))
      case (c: Comment, Node.COMMENT_NODE) =>
        Optional.of(new DomComment(c))
      case _ => Optional.empty()
    }
  }
}

object DomElem {

  def apply(underlyingNode: Element): DomElem = {
    new DomElem(underlyingNode)
  }
}
