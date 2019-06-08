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

package eu.cdevreeze.yaidom.java8.saxonelem

import java.net.URI
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.OptionConverters.RichOptionForJava8

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.saxon
import eu.cdevreeze.yaidom.java8.Attr
import eu.cdevreeze.yaidom.java8.ResolvedAttr
import eu.cdevreeze.yaidom.java8.StreamUtil.toJavaStreamFunction
import eu.cdevreeze.yaidom.java8.StreamUtil.toSingletonStream
import eu.cdevreeze.yaidom.java8.StreamUtil.toStream
import eu.cdevreeze.yaidom.java8.queryapi.StreamingBackingNodes
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedElemLike
import net.sf.saxon.om.NodeInfo

/**
 * Wrapper around Saxon wrapper node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class SaxonNode(val underlyingNode: saxon.SaxonNode) extends StreamingBackingNodes.Node

sealed abstract class CanBeDocumentChild(override val underlyingNode: saxon.SaxonCanBeDocumentChild)
  extends SaxonNode(underlyingNode) with StreamingBackingNodes.CanBeDocumentChild

/**
 * Wrapper around Saxon (wrapper) element, offering the streaming element query API.
 *
 * This wrapper does not directly wrap the underlying native Saxon nodes, but it wraps their yaidom wrappers, in
 * order to leverage much of this efficient Saxon yaidom wrapper implementation instead of having to re-implement
 * most of the same logic.
 *
 * TODO Do not extend StreamingScopedElemLike, but implement each method by invoking the corresponding underlying
 * Saxon wrapper element method.
 */
final class SaxonElem(override val underlyingNode: saxon.SaxonElem)
  extends CanBeDocumentChild(underlyingNode)
  with StreamingBackingNodes.Elem[SaxonNode, SaxonElem]
  with StreamingScopedElemLike[SaxonElem] {

  def children: Stream[SaxonNode] = {
    val underlyingResult: Stream[saxon.SaxonNode] =
      toSingletonStream(underlyingNode).flatMap(toJavaStreamFunction(e => e.children))

    underlyingResult
      .map[SaxonNode](asJavaFunction(n => SaxonNode(n)))
  }

  def findAllChildElems: Stream[SaxonElem] = {
    val underlyingResult: Stream[saxon.SaxonElem] =
      toSingletonStream(underlyingNode).flatMap(toJavaStreamFunction(e => e.findAllChildElems))

    underlyingResult.map[SaxonElem](asJavaFunction(e => new SaxonElem(e)))
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

  def namespaces: Declarations = {
    underlyingNode.namespaces
  }

  def docUriOption: Optional[URI] = {
    underlyingNode.docUriOption.asJava
  }

  def docUri: URI = {
    underlyingNode.docUri
  }

  def rootElem: SaxonElem = {
    new SaxonElem(underlyingNode.rootElem)
  }

  def path: Path = {
    underlyingNode.path
  }

  def baseUriOption: Optional[URI] = {
    underlyingNode.baseUriOption.asJava
  }

  def baseUri: URI = {
    underlyingNode.baseUri
  }

  def parentBaseUriOption: Optional[URI] = {
    underlyingNode.parentBaseUriOption.asJava
  }

  def reverseAncestryOrSelfENames: Stream[EName] = {
    toStream(underlyingNode.reverseAncestryOrSelfENames)
  }

  def reverseAncestryENames: Stream[EName] = {
    toStream(underlyingNode.reverseAncestryENames)
  }

  def reverseAncestryOrSelf: Stream[SaxonElem] = {
    toStream(underlyingNode.reverseAncestryOrSelf.map(e => new SaxonElem(e)))
  }

  def reverseAncestry: Stream[SaxonElem] = {
    toStream(underlyingNode.reverseAncestry.map(e => new SaxonElem(e)))
  }

  def parentOption: Optional[SaxonElem] = {
    underlyingNode.parentOption.map(e => new SaxonElem(e)).asJava
  }

  def parent: SaxonElem = {
    new SaxonElem(underlyingNode.parent)
  }

  def ancestorsOrSelf: Stream[SaxonElem] = {
    toStream(underlyingNode.ancestorsOrSelf.map(e => new SaxonElem(e)))
  }

  def ancestors: Stream[SaxonElem] = {
    toStream(underlyingNode.ancestors.map(e => new SaxonElem(e)))
  }

  def findAncestorOrSelf(p: Predicate[SaxonElem]): Optional[SaxonElem] = {
    underlyingNode.findAncestorOrSelf(e => p.test(new SaxonElem(e))).map(e => new SaxonElem(e)).asJava
  }

  def findAncestor(p: Predicate[SaxonElem]): Optional[SaxonElem] = {
    underlyingNode.findAncestor(e => p.test(new SaxonElem(e))).map(e => new SaxonElem(e)).asJava
  }

  /**
   * Workaround for Scala issue SI-8905.
   */
  final override def getChildElem(p: Predicate[SaxonElem]): SaxonElem = {
    super.getChildElem(p)
  }

  override def equals(other: Any): Boolean = other match {
    case other: SaxonElem => this.underlyingNode == other.underlyingNode
    case _ => false
  }

  override def hashCode: Int = {
    underlyingNode.hashCode
  }
}

final class SaxonText(override val underlyingNode: saxon.SaxonText)
  extends SaxonNode(underlyingNode) with StreamingBackingNodes.Text {

  def text: String = underlyingNode.text

  def trimmedText: String = underlyingNode.trimmedText

  def normalizedText: String = underlyingNode.normalizedText
}

final class SaxonComment(override val underlyingNode: saxon.SaxonComment)
  extends CanBeDocumentChild(underlyingNode) with StreamingBackingNodes.Comment {

  def text: String = underlyingNode.text
}

final class SaxonProcessingInstruction(override val underlyingNode: saxon.SaxonProcessingInstruction)
  extends CanBeDocumentChild(underlyingNode) with StreamingBackingNodes.ProcessingInstruction {

  def target: String = underlyingNode.target

  def data: String = underlyingNode.data
}

object SaxonNode {

  def apply(underlyingNode: saxon.SaxonNode): SaxonNode = {
    underlyingNode match {
      case e: saxon.SaxonElem => new SaxonElem(e)
      case t: saxon.SaxonText => new SaxonText(t)
      case c: saxon.SaxonComment => new SaxonComment(c)
      case pi: saxon.SaxonProcessingInstruction => new SaxonProcessingInstruction(pi)
    }
  }
}

object SaxonElem {

  def apply(nodeInfo: NodeInfo): SaxonElem = {
    apply(new saxon.SaxonElem(nodeInfo))
  }

  def apply(underlyingNode: saxon.SaxonElem): SaxonElem = {
    new SaxonElem(underlyingNode)
  }
}
