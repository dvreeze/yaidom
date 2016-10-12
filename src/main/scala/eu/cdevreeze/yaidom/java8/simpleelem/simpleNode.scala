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

package eu.cdevreeze.yaidom.java8.simpleelem

import java.util.stream.Stream
import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.ScalaStreamSupport
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.java8.Attr
import eu.cdevreeze.yaidom.java8.ResolvedAttr
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedElemLike
import eu.cdevreeze.yaidom.simple
import java.util.function.Predicate

/**
 * Wrapper around native yaidom simple node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class SimpleNode(val underlyingNode: simple.Node)

sealed abstract class CanBeDocumentChild(override val underlyingNode: simple.CanBeDocumentChild) extends SimpleNode(underlyingNode)

/**
 * Workaround for Scala issue SI-8905.
 */
sealed abstract class AbstractSimpleElem(override val underlyingNode: simple.Elem) extends CanBeDocumentChild(underlyingNode) with StreamingScopedElemLike[SimpleElem] { self: SimpleElem =>

  final override def getChildElem(p: Predicate[SimpleElem]): SimpleElem = {
    super.getChildElem(p)
  }
}

/**
 * Wrapper around native yaidom simple element, offering the streaming element query API.
 */
final class SimpleElem(override val underlyingNode: simple.Elem) extends AbstractSimpleElem(underlyingNode) {

  def findAllChildElems: Stream[SimpleElem] = {
    ScalaStreamSupport.stream(underlyingNode.findAllChildElems).map[SimpleElem](asJavaFunction(e => new SimpleElem(e)))
  }

  def resolvedName: EName = {
    underlyingNode.resolvedName
  }

  def resolvedAttributes: Stream[ResolvedAttr] = {
    ScalaStreamSupport.stream(underlyingNode.resolvedAttributes).map[ResolvedAttr](asJavaFunction(attr => ResolvedAttr(attr._1, attr._2)))
  }

  def text: String = {
    underlyingNode.text
  }

  def qname: QName = {
    underlyingNode.qname
  }

  def attributes: Stream[Attr] = {
    ScalaStreamSupport.stream(underlyingNode.attributes).map[Attr](asJavaFunction(attr => Attr(attr._1, attr._2)))
  }

  def scope: Scope = {
    underlyingNode.scope
  }
}

final class SimpleText(override val underlyingNode: simple.Text) extends SimpleNode(underlyingNode) {

  def text: String = underlyingNode.text

  def isCData: Boolean = underlyingNode.isCData

  def trimmedText: String = underlyingNode.trimmedText

  def normalizedText: String = underlyingNode.normalizedText
}

final class SimpleComment(override val underlyingNode: simple.Comment) extends CanBeDocumentChild(underlyingNode) {

  def text: String = underlyingNode.text
}

final class SimpleProcessingInstruction(override val underlyingNode: simple.ProcessingInstruction) extends CanBeDocumentChild(underlyingNode) {

  def target: String = underlyingNode.target

  def data: String = underlyingNode.data
}

final class SimpleEntityRef(override val underlyingNode: simple.EntityRef) extends SimpleNode(underlyingNode) {

  def entity: String = underlyingNode.entity
}
