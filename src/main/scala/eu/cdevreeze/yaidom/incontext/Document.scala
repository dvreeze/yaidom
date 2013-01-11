/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package incontext

import java.net.URI
import scala.collection.immutable

/**
 * `Document`, containing an "in context" document element.
 *
 * @author Chris de Vreeze
 */
final class Document(
  val baseUriOption: Option[URI],
  val documentElement: Elem,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  val comments: immutable.IndexedSeq[Comment]) extends Immutable {

  require(baseUriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  def document: eu.cdevreeze.yaidom.Document =
    new eu.cdevreeze.yaidom.Document(baseUriOption, documentElement.elem, processingInstructions, comments)

  override def toString: String = document.toString

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document = new Document(
    baseUriOption = this.baseUriOption,
    documentElement = newRoot,
    processingInstructions = this.processingInstructions,
    comments = this.comments)

  /** Creates a copy, but with the new baseUriOption passed as parameter newBaseUriOption */
  def withBaseUriOption(newBaseUriOption: Option[URI]): Document = new Document(
    baseUriOption = newBaseUriOption,
    documentElement = this.documentElement,
    processingInstructions = this.processingInstructions,
    comments = this.comments)
}

object Document {

  def apply(
    baseUriOption: Option[URI],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[Comment] = immutable.IndexedSeq()): Document = {

    new Document(baseUriOption, documentElement, processingInstructions, comments)
  }

  def apply(documentElement: Elem): Document = apply(None, documentElement)

  def apply(d: eu.cdevreeze.yaidom.Document): Document =
    new Document(d.baseUriOption, incontext.Elem(d.documentElement), d.processingInstructions, d.comments)
}
