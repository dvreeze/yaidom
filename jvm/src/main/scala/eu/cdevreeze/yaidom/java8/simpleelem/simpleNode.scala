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

package eu.cdevreeze.yaidom.java8.simpleelem

import java.util.function.Predicate
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.asJavaFunction

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.java8.Attr
import eu.cdevreeze.yaidom.java8.ResolvedAttr
import eu.cdevreeze.yaidom.java8.StreamUtil.toJavaStreamFunction
import eu.cdevreeze.yaidom.java8.StreamUtil.toSingletonStream
import eu.cdevreeze.yaidom.java8.StreamUtil.toStream
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedElemLike
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedNodes
import eu.cdevreeze.yaidom.simple

/**
 * Wrapper around native yaidom simple node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class SimpleNode(val underlyingNode: simple.Node) extends StreamingScopedNodes.Node

sealed abstract class CanBeDocumentChild(override val underlyingNode: simple.CanBeDocumentChild)
  extends SimpleNode(underlyingNode) with StreamingScopedNodes.CanBeDocumentChild

/**
 * Wrapper around native yaidom simple element, offering the streaming element query API.
 */
final class SimpleElem(override val underlyingNode: simple.Elem)
  extends CanBeDocumentChild(underlyingNode)
  with StreamingScopedNodes.Elem[SimpleNode, SimpleElem]
  with StreamingScopedElemLike[SimpleElem] {

  def children: Stream[SimpleNode] = {
    val underlyingResult: Stream[simple.Node] =
      toSingletonStream(underlyingNode).flatMap(toJavaStreamFunction(e => e.children))

    underlyingResult
      .map[SimpleNode](asJavaFunction(n => SimpleNode(n)))
  }

  def findAllChildElems: Stream[SimpleElem] = {
    val underlyingResult: Stream[simple.Elem] =
      toSingletonStream(underlyingNode).flatMap(toJavaStreamFunction(e => e.findAllChildElems))

    underlyingResult.map[SimpleElem](asJavaFunction(e => new SimpleElem(e)))
  }

  def resolvedName: EName = {
    underlyingNode.resolvedName
  }

  def resolvedAttributes: Stream[ResolvedAttr] = {
    toStream(underlyingNode.resolvedAttributes).map[ResolvedAttr](asJavaFunction(attr => ResolvedAttr(attr._1, attr._2)))
  }

  def text: String = {
    underlyingNode.text
  }

  def qname: QName = {
    underlyingNode.qname
  }

  def attributes: Stream[Attr] = {
    toStream(underlyingNode.attributes).map[Attr](asJavaFunction(attr => Attr(attr._1, attr._2)))
  }

  def scope: Scope = {
    underlyingNode.scope
  }

  /**
   * Workaround for Scala issue SI-8905.
   */
  final override def getChildElem(p: Predicate[SimpleElem]): SimpleElem = {
    super.getChildElem(p)
  }

  override def equals(other: Any): Boolean = other match {
    case other: SimpleElem => this.underlyingNode == other.underlyingNode
    case _ => false
  }

  override def hashCode: Int = {
    underlyingNode.hashCode
  }
}

final class SimpleText(override val underlyingNode: simple.Text)
  extends SimpleNode(underlyingNode) with StreamingScopedNodes.Text {

  def text: String = underlyingNode.text

  def isCData: Boolean = underlyingNode.isCData

  def trimmedText: String = underlyingNode.trimmedText

  def normalizedText: String = underlyingNode.normalizedText
}

final class SimpleComment(override val underlyingNode: simple.Comment)
  extends CanBeDocumentChild(underlyingNode) with StreamingScopedNodes.Comment {

  def text: String = underlyingNode.text
}

final class SimpleProcessingInstruction(override val underlyingNode: simple.ProcessingInstruction)
  extends CanBeDocumentChild(underlyingNode) with StreamingScopedNodes.ProcessingInstruction {

  def target: String = underlyingNode.target

  def data: String = underlyingNode.data
}

final class SimpleEntityRef(override val underlyingNode: simple.EntityRef)
  extends SimpleNode(underlyingNode) with StreamingScopedNodes.EntityRef {

  def entity: String = underlyingNode.entity
}

object SimpleNode {

  def apply(underlyingNode: simple.Node): SimpleNode = {
    underlyingNode match {
      case e: simple.Elem => new SimpleElem(e)
      case t: simple.Text => new SimpleText(t)
      case c: simple.Comment => new SimpleComment(c)
      case pi: simple.ProcessingInstruction => new SimpleProcessingInstruction(pi)
      case er: simple.EntityRef => new SimpleEntityRef(er)
    }
  }
}

object SimpleElem {

  def apply(underlyingNode: simple.Elem): SimpleElem = {
    new SimpleElem(underlyingNode)
  }
}
