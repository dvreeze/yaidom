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

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.simple

/**
 * `IndexedDocument`, containing an "indexed" document element with simple elements as underlying elements.
 *
 * @author Chris de Vreeze
 */
final class Document(
  xmlDeclarationOption: Option[XmlDeclaration],
  children: immutable.IndexedSeq[IndexedScopedNode.CanBeDocumentChild]) extends IndexedDocument(xmlDeclarationOption, children) with Immutable {

  type ThisDoc = Document

  type UnderlyingElem = simple.Elem

  type UnderlyingDoc = simple.Document

  def document: simple.Document = {
    val childSeq: immutable.IndexedSeq[simple.CanBeDocumentChild] =
      children map {
        case e: IndexedScopedNode.Elem[_]                => documentElement.underlyingElem
        case pi: IndexedScopedNode.ProcessingInstruction => simple.ProcessingInstruction(pi.target, pi.data)
        case c: IndexedScopedNode.Comment                => simple.Comment(c.text)
      }

    simple.Document(uriOption, xmlDeclarationOption, childSeq)
  }

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document = new Document(
    xmlDeclarationOption = this.xmlDeclarationOption,
    children = this.children map {
      case elm: IndexedScopedNode.Elem[_]             => newRoot
      case node: IndexedScopedNode.CanBeDocumentChild => node
    })

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): Document = new Document(
    xmlDeclarationOption = newXmlDeclarationOption,
    children = this.children)
}

object Document {

  def apply(
    xmlDeclarationOption: Option[XmlDeclaration],
    children: immutable.IndexedSeq[IndexedScopedNode.CanBeDocumentChild]): Document = {

    new Document(xmlDeclarationOption, children)
  }

  def apply(
    xmlDeclarationOption: Option[XmlDeclaration],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[IndexedScopedNode.ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[IndexedScopedNode.Comment] = immutable.IndexedSeq()): Document = {

    new Document(
      xmlDeclarationOption,
      processingInstructions ++ comments ++ Vector(documentElement))
  }

  def apply(documentElement: Elem): Document = {
    new Document(None, Vector(documentElement))
  }

  def from(d: simple.Document): Document = {
    val elem = IndexedScopedNode.Elem(d.uriOption, d.documentElement)

    val children = d.children map {
      case elm: simple.Elem                   => elem
      case node: simple.Comment               => IndexedScopedNode.Comment(node.text)
      case node: simple.ProcessingInstruction => IndexedScopedNode.ProcessingInstruction(node.target, node.data)
    }

    apply(d.xmlDeclarationOption, children)
  }

  /**
   * Returns `from(d.withUriOption(Some(docUri)))`.
   */
  def apply(docUri: URI, d: simple.Document): Document = {
    from(d.withUriOption(Some(docUri)))
  }

  /**
   * Returns `from(d)`.
   */
  def apply(d: simple.Document): Document = {
    from(d)
  }
}
