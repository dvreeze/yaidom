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

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi
import eu.cdevreeze.yaidom.simple

/**
 * `IndexedDocument`, containing an "indexed" document element.
 *
 * @author Chris de Vreeze
 */
final class Document(
    val xmlDeclarationOption: Option[XmlDeclaration],
    val children: IndexedSeq[IndexedNode.CanBeDocumentChild])
    extends BackingDocumentApi {

  type ThisDoc = Document

  type DocElemType = IndexedNode.Elem

  require(xmlDeclarationOption ne null) // scalastyle:off null
  require(children ne null) // scalastyle:off null

  require(
    children.collect({ case elm: IndexedNode.Elem => elm }).size == 1,
    s"A document must have exactly one child element (${uriOption.map(_.toString).getOrElse("No URI found")})"
  )

  require(documentElement.path == Path.Empty, "The document element must have the root Path")

  def document: simple.Document = {
    val childSeq: IndexedSeq[simple.CanBeDocumentChild] =
      children.map {
        case e: IndexedNode.Elem                   => documentElement.underlyingElem
        case pi: IndexedNode.ProcessingInstruction => simple.ProcessingInstruction(pi.target, pi.data)
        case c: IndexedNode.Comment                => simple.Comment(c.text)
      }

    simple.Document(uriOption, xmlDeclarationOption, childSeq)
  }

  def documentElement: IndexedNode.Elem =
    children.collect({ case elm: IndexedNode.Elem => elm }).head

  def processingInstructions: IndexedSeq[IndexedNode.ProcessingInstruction] =
    children.collect({ case pi: IndexedNode.ProcessingInstruction => pi })

  def comments: IndexedSeq[IndexedNode.Comment] =
    children.collect({ case c: IndexedNode.Comment => c })

  def uriOption: Option[URI] = documentElement.docUriOption

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  def uri: URI = uriOption.getOrElse(new URI(""))

  override def toString: String = document.toString

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document =
    new Document(
      xmlDeclarationOption = this.xmlDeclarationOption,
      children = this.children.map {
        case elm: IndexedNode.Elem                => newRoot
        case node: IndexedNode.CanBeDocumentChild => node
      }
    )

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): Document =
    new Document(xmlDeclarationOption = newXmlDeclarationOption, children = this.children)
}

object Document {

  def apply(
      xmlDeclarationOption: Option[XmlDeclaration],
      children: IndexedSeq[IndexedNode.CanBeDocumentChild]): Document = {

    new Document(xmlDeclarationOption, children)
  }

  def apply(
      xmlDeclarationOption: Option[XmlDeclaration],
      documentElement: Elem,
      processingInstructions: IndexedSeq[IndexedNode.ProcessingInstruction] = IndexedSeq(),
      comments: IndexedSeq[IndexedNode.Comment] = IndexedSeq()): Document = {

    new Document(xmlDeclarationOption, processingInstructions ++ comments ++ Vector(documentElement))
  }

  def apply(documentElement: Elem): Document = {
    new Document(None, Vector(documentElement))
  }

  def from(d: simple.Document): Document = {
    val elem = IndexedNode.Elem(d.uriOption, d.documentElement)

    val children = d.children.map {
      case elm: simple.Elem                   => elem
      case node: simple.Comment               => IndexedNode.Comment(node.text)
      case node: simple.ProcessingInstruction => IndexedNode.ProcessingInstruction(node.target, node.data)
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
