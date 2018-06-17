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

package eu.cdevreeze.yaidom.java8.indexedelem

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
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.indexed.IndexedScopedNode
import eu.cdevreeze.yaidom.java8.Attr
import eu.cdevreeze.yaidom.java8.ResolvedAttr
import eu.cdevreeze.yaidom.java8.StreamUtil.toJavaStreamFunction
import eu.cdevreeze.yaidom.java8.StreamUtil.toSingletonStream
import eu.cdevreeze.yaidom.java8.StreamUtil.toStream
import eu.cdevreeze.yaidom.java8.queryapi.StreamingBackingElemApi
import eu.cdevreeze.yaidom.java8.queryapi.StreamingScopedElemLike

/**
 * Wrapper around native yaidom indexed node.
 *
 * @author Chris de Vreeze
 */
sealed abstract class IndexedNode(val underlyingNode: IndexedScopedNode.Node)

sealed abstract class CanBeDocumentChild(override val underlyingNode: IndexedScopedNode.CanBeDocumentChild) extends IndexedNode(underlyingNode)

/**
 * Wrapper around native yaidom indexed element, offering the streaming element query API.
 */
final class IndexedElem(override val underlyingNode: indexed.Elem)
  extends CanBeDocumentChild(underlyingNode)
  with StreamingBackingElemApi[IndexedElem]
  with StreamingScopedElemLike[IndexedElem] {

  def findAllChildElems: Stream[IndexedElem] = {
    val underlyingResult: Stream[indexed.Elem] =
      toSingletonStream(underlyingNode).flatMap(toJavaStreamFunction(e => e.findAllChildElems))

    underlyingResult.map[IndexedElem](asJavaFunction(e => new IndexedElem(e)))
  }

  def resolvedName: EName = {
    underlyingNode.resolvedName
  }

  def resolvedAttributes: Stream[ResolvedAttr] = {
    toStream(underlyingNode.underlyingElem.resolvedAttributes).map[ResolvedAttr](asJavaFunction(attr => ResolvedAttr(attr._1, attr._2)))
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

  def rootElem: IndexedElem = {
    new IndexedElem(underlyingNode.rootElem)
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

  def reverseAncestryOrSelf: Stream[IndexedElem] = {
    toStream(underlyingNode.reverseAncestryOrSelf.map(e => new IndexedElem(e)))
  }

  def reverseAncestry: Stream[IndexedElem] = {
    toStream(underlyingNode.reverseAncestry.map(e => new IndexedElem(e)))
  }

  def parentOption: Optional[IndexedElem] = {
    underlyingNode.parentOption.map(e => new IndexedElem(e)).asJava
  }

  def parent: IndexedElem = {
    new IndexedElem(underlyingNode.parent)
  }

  def ancestorsOrSelf: Stream[IndexedElem] = {
    toStream(underlyingNode.ancestorsOrSelf.map(e => new IndexedElem(e)))
  }

  def ancestors: Stream[IndexedElem] = {
    toStream(underlyingNode.ancestors.map(e => new IndexedElem(e)))
  }

  def findAncestorOrSelf(p: Predicate[IndexedElem]): Optional[IndexedElem] = {
    underlyingNode.findAncestorOrSelf(e => p.test(new IndexedElem(e))).map(e => new IndexedElem(e)).asJava
  }

  def findAncestor(p: Predicate[IndexedElem]): Optional[IndexedElem] = {
    underlyingNode.findAncestor(e => p.test(new IndexedElem(e))).map(e => new IndexedElem(e)).asJava
  }

  /**
   * Workaround for Scala issue SI-8905.
   */
  final override def getChildElem(p: Predicate[IndexedElem]): IndexedElem = {
    super.getChildElem(p)
  }

  override def equals(other: Any): Boolean = other match {
    case other: IndexedElem => this.underlyingNode == other.underlyingNode
    case _ => false
  }

  override def hashCode: Int = {
    underlyingNode.hashCode
  }
}

final class IndexedText(override val underlyingNode: IndexedScopedNode.Text) extends IndexedNode(underlyingNode) {

  def text: String = underlyingNode.text

  def isCData: Boolean = underlyingNode.isCData

  def trimmedText: String = underlyingNode.trimmedText

  def normalizedText: String = underlyingNode.normalizedText
}

final class IndexedComment(override val underlyingNode: IndexedScopedNode.Comment) extends CanBeDocumentChild(underlyingNode) {

  def text: String = underlyingNode.text
}

final class IndexedProcessingInstruction(override val underlyingNode: IndexedScopedNode.ProcessingInstruction) extends CanBeDocumentChild(underlyingNode) {

  def target: String = underlyingNode.target

  def data: String = underlyingNode.data
}

final class IndexedEntityRef(override val underlyingNode: IndexedScopedNode.EntityRef) extends IndexedNode(underlyingNode) {

  def entity: String = underlyingNode.entity
}

object IndexedElem {

  def apply(underlyingNode: indexed.Elem): IndexedElem = {
    new IndexedElem(underlyingNode)
  }

  def apply(elem: eu.cdevreeze.yaidom.simple.Elem, path: Path): IndexedElem = {
    new IndexedElem(IndexedScopedNode.Elem(elem, path))
  }
}
