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

package eu.cdevreeze.yaidom.java8.resolvedelem

import java.util.function.Predicate
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.ScalaStreamSupport

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.java8.ResolvedAttr
import eu.cdevreeze.yaidom.java8.StreamUtil.toJavaStreamFunction
import eu.cdevreeze.yaidom.java8.StreamUtil.toSingletonStream
import eu.cdevreeze.yaidom.java8.queryapi.StreamingClarkElemLike
import eu.cdevreeze.yaidom.java8.queryapi.StreamingClarkNodes
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.resolved

/**
 * Wrapper around native yaidom resolved node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class ResolvedNode(val underlyingNode: resolved.Node) extends StreamingClarkNodes.Node

/**
 * Wrapper around native yaidom resolved element, offering the streaming element query API.
 */
final case class ResolvedElem(override val underlyingNode: resolved.Elem)
  extends ResolvedNode(underlyingNode) with StreamingClarkNodes.Elem[ResolvedNode, ResolvedElem] with StreamingClarkElemLike[ResolvedElem] {

  def children: Stream[ResolvedNode] = {
    val underlyingResult: Stream[resolved.Node] =
      toSingletonStream(underlyingNode).flatMap(toJavaStreamFunction(e => e.children))

    underlyingResult
      .map[ResolvedNode](asJavaFunction(n => ResolvedNode(n)))
  }

  def findAllChildElems: Stream[ResolvedElem] = {
    val underlyingResult: Stream[resolved.Elem] =
      toSingletonStream(underlyingNode).flatMap(toJavaStreamFunction(e => e.findAllChildElems))

    underlyingResult.map[ResolvedElem](asJavaFunction(e => new ResolvedElem(e)))
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

  /**
   * Workaround for Scala issue SI-8905.
   */
  final override def getChildElem(p: Predicate[ResolvedElem]): ResolvedElem = {
    super.getChildElem(p)
  }
}

final case class ResolvedText(override val underlyingNode: resolved.Text)
  extends ResolvedNode(underlyingNode) with StreamingClarkNodes.Text {

  def text: String = underlyingNode.text

  def trimmedText: String = underlyingNode.trimmedText

  def normalizedText: String = underlyingNode.normalizedText
}

object ResolvedNode {

  def apply(underlyingNode: resolved.Node): ResolvedNode = {
    underlyingNode match {
      case e: resolved.Elem => ResolvedElem(e)
      case t: resolved.Text => ResolvedText(t)
    }
  }
}

object ResolvedElem {

  def from(elem: ClarkNodes.Elem): ResolvedElem = {
    new ResolvedElem(resolved.Elem.from(elem))
  }
}