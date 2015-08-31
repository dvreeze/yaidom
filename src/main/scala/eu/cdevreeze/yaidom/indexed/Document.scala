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

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.ProcessingInstruction

/**
 * `IndexedDocument`, containing an "indexed" document element with simple elements as underlying elements.
 *
 * @author Chris de Vreeze
 */
final class Document(
  xmlDeclarationOption: Option[XmlDeclaration],
  children: immutable.IndexedSeq[Nodes.CanBeDocumentChild]) extends IndexedDocument[simple.Elem](xmlDeclarationOption, children) with Immutable {

  def document: simple.Document =
    simple.Document(
      uriOption,
      xmlDeclarationOption,
      children map {
        case e: Nodes.Elem                   => documentElement.elem
        case pi: Nodes.ProcessingInstruction => ProcessingInstruction(pi.target, pi.data)
        case c: Nodes.Comment                => Comment(c.text)
      })

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document = new Document(
    xmlDeclarationOption = this.xmlDeclarationOption,
    children = this.children map {
      case elm: Nodes.Elem                => newRoot
      case node: Nodes.CanBeDocumentChild => node
    })

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): Document = new Document(
    xmlDeclarationOption = newXmlDeclarationOption,
    children = this.children)
}

object Document {

  def apply(
    xmlDeclarationOption: Option[XmlDeclaration],
    children: immutable.IndexedSeq[Nodes.CanBeDocumentChild]): Document = {

    new Document(xmlDeclarationOption, children)
  }

  def apply(
    xmlDeclarationOption: Option[XmlDeclaration],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[Comment] = immutable.IndexedSeq()): Document = {

    new Document(
      xmlDeclarationOption,
      processingInstructions ++ comments ++ Vector(documentElement))
  }

  def apply(documentElement: Elem): Document = {
    new Document(None, Vector(documentElement))
  }

  def from(d: simple.Document, uriResolver: XmlBaseSupport.UriResolver): Document = {
    val elem = Elem.Builder(uriResolver).build(d.uriOption, d.documentElement)

    val children = d.children map {
      case elm: Nodes.Elem                => elem
      case node: Nodes.CanBeDocumentChild => node
    }

    apply(d.xmlDeclarationOption, children)
  }

  /**
   * Returns `from(d.withUriOption(Some(docUri)), XmlBaseSupport.JdkUriResolver)`.
   */
  def apply(docUri: URI, d: simple.Document): Document = {
    from(d.withUriOption(Some(docUri)), XmlBaseSupport.JdkUriResolver)
  }

  /**
   * Returns `from(d, XmlBaseSupport.JdkUriResolver)`.
   */
  def apply(d: simple.Document): Document = {
    from(d, XmlBaseSupport.JdkUriResolver)
  }
}
