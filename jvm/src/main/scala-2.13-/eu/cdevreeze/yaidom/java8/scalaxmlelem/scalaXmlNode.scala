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

package eu.cdevreeze.yaidom.java8.scalaxmlelem

import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.FunctionConverters.asJavaPredicate
import scala.xml.Atom
import scala.xml.Comment
import scala.xml.Elem
import scala.xml.EntityRef
import scala.xml.Node
import scala.xml.PCData
import scala.xml.ProcInstr
import scala.xml.Text

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
import eu.cdevreeze.yaidom.scalaxml

/**
 * Wrapper around Scala XML node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class ScalaXmlNode(val underlyingNode: Node) extends StreamingScopedNodes.Node

sealed abstract class CanBeScalaXmlDocumentChild(override val underlyingNode: Node)
  extends ScalaXmlNode(underlyingNode) with StreamingScopedNodes.CanBeDocumentChild

/**
 * Wrapper around DOM element, offering the streaming element query API.
 */
final class ScalaXmlElem(override val underlyingNode: Elem)
  extends CanBeScalaXmlDocumentChild(underlyingNode)
  with StreamingScopedNodes.Elem[ScalaXmlNode, ScalaXmlElem]
  with StreamingScopedElemLike[ScalaXmlElem] {

  def children: Stream[ScalaXmlNode] = {
    val underlyingResult: Stream[scalaxml.ScalaXmlNode] =
      toSingletonStream(scalaxml.ScalaXmlElem(underlyingNode)).flatMap(toJavaStreamFunction(e => e.children))

    underlyingResult
      .map[Optional[ScalaXmlNode]](asJavaFunction(n => ScalaXmlNode.wrapNodeOption(n.wrappedNode)))
      .filter(asJavaPredicate(_.isPresent))
      .map[ScalaXmlNode](asJavaFunction(_.get))
  }

  def findAllChildElems: Stream[ScalaXmlElem] = {
    val underlyingResult: Stream[scalaxml.ScalaXmlElem] =
      toSingletonStream(scalaxml.ScalaXmlElem(underlyingNode)).flatMap(toJavaStreamFunction(e => e.findAllChildElems))

    underlyingResult.map[ScalaXmlElem](asJavaFunction(e => new ScalaXmlElem(e.wrappedNode)))
  }

  def resolvedName: EName = {
    scalaxml.ScalaXmlElem(underlyingNode).resolvedName
  }

  def resolvedAttributes: Stream[ResolvedAttr] = {
    toStream(scalaxml.ScalaXmlElem(underlyingNode).resolvedAttributes).map[ResolvedAttr](asJavaFunction(attr => ResolvedAttr(attr._1, attr._2)))
  }

  def text: String = {
    scalaxml.ScalaXmlElem(underlyingNode).text
  }

  def qname: QName = {
    scalaxml.ScalaXmlElem(underlyingNode).qname
  }

  def attributes: Stream[Attr] = {
    toStream(scalaxml.ScalaXmlElem(underlyingNode).attributes).map[Attr](asJavaFunction(attr => Attr(attr._1, attr._2)))
  }

  def scope: Scope = {
    scalaxml.ScalaXmlElem(underlyingNode).scope
  }

  /**
   * Workaround for Scala issue SI-8905.
   */
  final override def getChildElem(p: Predicate[ScalaXmlElem]): ScalaXmlElem = {
    super.getChildElem(p)
  }

  override def equals(other: Any): Boolean = other match {
    case other: ScalaXmlElem => this.underlyingNode == other.underlyingNode
    case _ => false
  }

  override def hashCode: Int = {
    underlyingNode.hashCode
  }
}

final class ScalaXmlText(override val underlyingNode: Text)
  extends ScalaXmlNode(underlyingNode) with StreamingScopedNodes.Text {

  def text: String = scalaxml.ScalaXmlText(underlyingNode).text

  def trimmedText: String = scalaxml.ScalaXmlText(underlyingNode).trimmedText

  def normalizedText: String = scalaxml.ScalaXmlText(underlyingNode).normalizedText
}

final class ScalaXmlCData(override val underlyingNode: PCData)
  extends ScalaXmlNode(underlyingNode) with StreamingScopedNodes.Text {

  def text: String = scalaxml.ScalaXmlCData(underlyingNode).text

  def trimmedText: String = scalaxml.ScalaXmlCData(underlyingNode).trimmedText

  def normalizedText: String = scalaxml.ScalaXmlCData(underlyingNode).normalizedText
}

/**
 * Wrapper around a Scala XML Atom that is not Text or PCData.
 * See for example http://sites.google.com/site/burakemir/scalaxbook.docbk.html?attredirects=0.
 */
final class ScalaXmlAtom(override val underlyingNode: Atom[_])
  extends ScalaXmlNode(underlyingNode) with StreamingScopedNodes.Text {

  def text: String = scalaxml.ScalaXmlAtom(underlyingNode).text

  def trimmedText: String = scalaxml.ScalaXmlAtom(underlyingNode).trimmedText

  def normalizedText: String = scalaxml.ScalaXmlAtom(underlyingNode).normalizedText
}

final class ScalaXmlComment(override val underlyingNode: Comment)
  extends CanBeScalaXmlDocumentChild(underlyingNode) with StreamingScopedNodes.Comment {

  def text: String = scalaxml.ScalaXmlComment(underlyingNode).text
}

final class ScalaXmlProcessingInstruction(override val underlyingNode: ProcInstr)
  extends CanBeScalaXmlDocumentChild(underlyingNode) with StreamingScopedNodes.ProcessingInstruction {

  def target: String = scalaxml.ScalaXmlProcessingInstruction(underlyingNode).target

  def data: String = scalaxml.ScalaXmlProcessingInstruction(underlyingNode).data
}

final class ScalaXmlEntityRef(override val underlyingNode: EntityRef)
  extends ScalaXmlNode(underlyingNode) with StreamingScopedNodes.EntityRef {

  def entity: String = scalaxml.ScalaXmlEntityRef(underlyingNode).entity
}

object ScalaXmlNode {

  def wrapNodeOption(node: scala.xml.Node): Optional[ScalaXmlNode] = {
    node match {
      case e: scala.xml.Elem => Optional.of(new ScalaXmlElem(e))
      case cdata: scala.xml.PCData => Optional.of(new ScalaXmlCData(cdata))
      case t: scala.xml.Text => Optional.of(new ScalaXmlText(t))
      case at: scala.xml.Atom[_] =>
        // Possibly an evaluated "parameter" in an XML literal
        Optional.of(new ScalaXmlAtom(at))
      case pi: scala.xml.ProcInstr => Optional.of(new ScalaXmlProcessingInstruction(pi))
      case er: scala.xml.EntityRef => Optional.of(new ScalaXmlEntityRef(er))
      case c: scala.xml.Comment => Optional.of(new ScalaXmlComment(c))
      case _ => Optional.empty()
    }
  }
}

object ScalaXmlElem {

  def apply(underlyingNode: Elem): ScalaXmlElem = {
    new ScalaXmlElem(underlyingNode)
  }
}
