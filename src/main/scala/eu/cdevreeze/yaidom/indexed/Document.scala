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

import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.XmlDeclaration

/**
 * `IndexedDocument`, containing an "indexed" document element with simple elements as underlying elements.
 *
 * @author Chris de Vreeze
 */
final class Document(
  xmlDeclarationOption: Option[XmlDeclaration],
  documentElement: Elem,
  processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  comments: immutable.IndexedSeq[Comment]) extends IndexedDocument[simple.Node, simple.Elem](xmlDeclarationOption, documentElement, processingInstructions, comments) with Immutable {

  def document: simple.Document =
    new simple.Document(uriOption, xmlDeclarationOption, documentElement.elem, processingInstructions, comments)

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document = new Document(
    xmlDeclarationOption = this.xmlDeclarationOption,
    documentElement = newRoot,
    processingInstructions = this.processingInstructions,
    comments = this.comments)

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): Document = new Document(
    xmlDeclarationOption = newXmlDeclarationOption,
    documentElement = this.documentElement,
    processingInstructions = this.processingInstructions,
    comments = this.comments)
}

object Document {

  def apply(
    xmlDeclarationOption: Option[XmlDeclaration],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[Comment] = immutable.IndexedSeq()): Document = {

    new Document(xmlDeclarationOption, documentElement, processingInstructions, comments)
  }

  def apply(documentElement: Elem): Document = {
    new Document(None, documentElement, Vector(), Vector())
  }

  def apply(docUri: URI, d: simple.Document): Document = {
    new Document(d.xmlDeclarationOption, Elem(Some(docUri), d.documentElement), d.processingInstructions, d.comments)
  }

  def apply(d: simple.Document): Document =
    new Document(d.xmlDeclarationOption, Elem(d.uriOption, d.documentElement), d.processingInstructions, d.comments)
}
