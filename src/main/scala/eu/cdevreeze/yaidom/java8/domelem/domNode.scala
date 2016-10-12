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

package eu.cdevreeze.yaidom.java8.domelem

import java.util.function.Predicate
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.ScalaStreamSupport

import org.w3c.dom.Comment
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
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedElemLike

/**
 * Wrapper around DOM node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class DomNode(val underlyingNode: Node)

sealed abstract class CanBeDomDocumentChild(override val underlyingNode: Node) extends DomNode(underlyingNode)

/**
 * Workaround for Scala issue SI-8905.
 */
sealed abstract class AbstractDomElem(override val underlyingNode: Element) extends CanBeDomDocumentChild(underlyingNode) with StreamingScopedElemLike[DomElem] { self: DomElem =>

  final override def getChildElem(p: Predicate[DomElem]): DomElem = {
    super.getChildElem(p)
  }
}

/**
 * Wrapper around DOM element, offering the streaming element query API.
 */
final class DomElem(override val underlyingNode: Element) extends AbstractDomElem(underlyingNode) {

  def findAllChildElems: Stream[DomElem] = {
    ScalaStreamSupport.stream(dom.DomElem(underlyingNode).findAllChildElems).map[DomElem](asJavaFunction(e => new DomElem(e.wrappedNode)))
  }

  def resolvedName: EName = {
    dom.DomElem(underlyingNode).resolvedName
  }

  def resolvedAttributes: Stream[ResolvedAttr] = {
    ScalaStreamSupport.stream(dom.DomElem(underlyingNode).resolvedAttributes).map[ResolvedAttr](asJavaFunction(attr => ResolvedAttr(attr._1, attr._2)))
  }

  def text: String = {
    dom.DomElem(underlyingNode).text
  }

  def qname: QName = {
    dom.DomElem(underlyingNode).qname
  }

  def attributes: Stream[Attr] = {
    ScalaStreamSupport.stream(dom.DomElem(underlyingNode).attributes).map[Attr](asJavaFunction(attr => Attr(attr._1, attr._2)))
  }

  def scope: Scope = {
    dom.DomElem(underlyingNode).scope
  }
}

final class DomText(override val underlyingNode: Text) extends DomNode(underlyingNode) {

  def text: String = dom.DomText(underlyingNode).text

  def trimmedText: String = dom.DomText(underlyingNode).trimmedText

  def normalizedText: String = dom.DomText(underlyingNode).normalizedText
}

final class DomComment(override val underlyingNode: Comment) extends CanBeDomDocumentChild(underlyingNode) {

  def text: String = dom.DomComment(underlyingNode).text
}

final class DomProcessingInstruction(override val underlyingNode: ProcessingInstruction) extends CanBeDomDocumentChild(underlyingNode) {

  def target: String = dom.DomProcessingInstruction(underlyingNode).target

  def data: String = dom.DomProcessingInstruction(underlyingNode).data
}

final class DomEntityRef(override val underlyingNode: EntityReference) extends DomNode(underlyingNode) {

  def entity: String = dom.DomEntityRef(underlyingNode).entity
}
