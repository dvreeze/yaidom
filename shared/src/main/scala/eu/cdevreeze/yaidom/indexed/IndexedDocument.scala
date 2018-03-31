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

package eu.cdevreeze.yaidom.indexed

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.ScopedNodes

/**
 * Document, containing an "indexed" document element.
 *
 * Note that class `IndexedDocument` does not have any query methods for `Elem` instances. In particular, the `ElemApi` does not
 * apply to documents. Therefore, given a document, querying for elements (other than the document element itself) always goes
 * via the document element.
 *
 * @author Chris de Vreeze
 */
abstract class IndexedDocument(
  val xmlDeclarationOption: Option[XmlDeclaration],
  val children:             immutable.IndexedSeq[IndexedScopedNode.CanBeDocumentChild]) extends BackingDocumentApi with Immutable { self =>

  require(xmlDeclarationOption ne null) // scalastyle:off null
  require(children ne null) // scalastyle:off null

  require(
    children.collect({ case elm: IndexedScopedElem[_] => elm }).size == 1,
    s"A document must have exactly one child element (${uriOption.map(_.toString).getOrElse("No URI found")})")

  require(documentElement.path == Path.Empty, "The document element must have the root Path")

  type ThisDoc <: IndexedDocument

  type UnderlyingElem <: ScopedNodes.Elem.Aux[_, UnderlyingElem]

  type UnderlyingDoc <: DocumentApi.Aux[UnderlyingDoc, UnderlyingElem]

  type DocElemType = IndexedScopedElem[UnderlyingElem]

  final def documentElement: DocElemType =
    children.collect({ case elm: IndexedScopedNode.Elem[_] => elm }).head.asInstanceOf[DocElemType]

  final def processingInstructions: immutable.IndexedSeq[IndexedScopedNode.ProcessingInstruction] =
    children.collect({ case pi: IndexedScopedNode.ProcessingInstruction => pi })

  final def comments: immutable.IndexedSeq[IndexedScopedNode.Comment] =
    children.collect({ case c: IndexedScopedNode.Comment => c })

  final def uriOption: Option[URI] = documentElement.docUriOption

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  final def uri: URI = uriOption.getOrElse(new URI(""))

  def document: DocumentApi.Aux[UnderlyingDoc, UnderlyingElem]

  final override def toString: String = document.toString

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: DocElemType): ThisDoc

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): ThisDoc
}
