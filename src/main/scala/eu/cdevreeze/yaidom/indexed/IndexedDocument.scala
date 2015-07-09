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

package eu.cdevreeze.yaidom.indexed

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.simple.XmlDeclaration

/**
 * Document, containing an "indexed" document element.
 *
 * Note that class `IndexedDocument` does not have any query methods for `Elem` instances. In particular, the `ElemApi` does not
 * apply to documents. Therefore, given a document, querying for elements (other than the document element itself) always goes
 * via the document element.
 *
 * @author Chris de Vreeze
 */
abstract class IndexedDocument[N <: Nodes.Node, U <: N with ScopedElemApi[U]](
  val xmlDeclarationOption: Option[XmlDeclaration],
  val documentElement: IndexedScopedElem[U],
  val processingInstructions: immutable.IndexedSeq[N with Nodes.ProcessingInstruction],
  val comments: immutable.IndexedSeq[N with Nodes.Comment]) extends DocumentApi[IndexedScopedElem[U]] with Immutable {

  require(xmlDeclarationOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  require(documentElement.path == Path.Root, "The document element must have the root Path")

  final def uriOption: Option[URI] = documentElement.docUriOption

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  final def uri: URI = uriOption.getOrElse(new URI(""))

  def document: DocumentApi[U]

  final override def toString: String = document.toString

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: IndexedScopedElem[U]): IndexedDocument[N, U]

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): IndexedDocument[N, U]
}
