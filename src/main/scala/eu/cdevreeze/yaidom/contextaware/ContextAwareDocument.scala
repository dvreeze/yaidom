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

package eu.cdevreeze.yaidom.contextaware

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi

/**
 * Document, containing a "context-aware" document element.
 *
 * Note that class `ContextAwareDocument` does not have any query methods for `Elem` instances. In particular, the `ElemApi` does not
 * apply to documents. Therefore, given a document, querying for elements (other than the document element itself) always goes
 * via the document element.
 *
 * @author Chris de Vreeze
 */
abstract class ContextAwareDocument[U <: ScopedElemApi[U]](
  val xmlDeclarationOption: Option[XmlDeclaration],
  val children: immutable.IndexedSeq[Nodes.CanBeDocumentChild]) extends DocumentApi[ContextAwareScopedElem[U]] with Immutable {

  require(xmlDeclarationOption ne null)
  require(children ne null)

  require(
    children.collect({ case elm: Nodes.Elem => elm }).size == 1,
    s"A document must have exactly one child element")

  require(
    children.collect({ case elm: ContextAwareScopedElem[U] => elm }).size == 1,
    s"A document must have exactly one (ContextAwareScopedElem) child element (${uriOption.map(_.toString).getOrElse("No URI found")})")

  require(documentElement.parentContextPath.isEmpty, "The document element must have the empty parent ContextPath")

  final def documentElement: ContextAwareScopedElem[U] =
    children.collect({ case elm: ContextAwareScopedElem[U] => elm }).head

  final def processingInstructions: immutable.IndexedSeq[Nodes.ProcessingInstruction] =
    children.collect({ case pi: Nodes.ProcessingInstruction => pi })

  final def comments: immutable.IndexedSeq[Nodes.Comment] =
    children.collect({ case c: Nodes.Comment => c })

  final def uriOption: Option[URI] = documentElement.docUriOption

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  final def uri: URI = uriOption.getOrElse(new URI(""))

  def document: DocumentApi[U]

  final override def toString: String = document.toString

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: ContextAwareScopedElem[U]): ContextAwareDocument[U]

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): ContextAwareDocument[U]
}
