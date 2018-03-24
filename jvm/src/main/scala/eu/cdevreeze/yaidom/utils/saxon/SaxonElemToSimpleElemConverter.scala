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

package eu.cdevreeze.yaidom.utils.saxon

import eu.cdevreeze.yaidom.saxon.SaxonComment
import eu.cdevreeze.yaidom.saxon.SaxonDocument
import eu.cdevreeze.yaidom.saxon.SaxonElem
import eu.cdevreeze.yaidom.saxon.SaxonNode
import eu.cdevreeze.yaidom.saxon.SaxonProcessingInstruction
import eu.cdevreeze.yaidom.saxon.SaxonText
import eu.cdevreeze.yaidom.simple

/**
 * Converter from yaidom Saxon wrapper elements and documents to yaidom simple elements and documents.
 * It is implemented by directly building simple Nodes from Saxon wrapper nodes.
 *
 * @author Chris de Vreeze
 */
object SaxonElemToSimpleElemConverter {

  def convertSaxonDocument(doc: SaxonDocument): simple.Document = {
    // No XML declaration, probably?

    simple.Document(
      uriOption = doc.uriOption,
      xmlDeclarationOption = None,
      children = doc.children flatMap {
        case e: SaxonElem                   => Some(convertSaxonElem(doc.documentElement))
        case pi: SaxonProcessingInstruction => Some(convertSaxonProcessingInstruction(pi))
        case c: SaxonComment                => Some(convertSaxonComment(c))
        case _                              => None
      })
  }

  def convertSaxonElem(elem: SaxonElem): simple.Elem = {
    val children = elem.children.flatMap(ch => optionallyConvertSaxonNode(ch))

    val resultElem =
      simple.Node.elem(
        elem.qname,
        elem.attributes,
        elem.scope,
        children)

    resultElem
  }

  /**
   * Converts a `SaxonNode` to an optional yaidom simple Node.
   */
  def optionallyConvertSaxonNode(node: SaxonNode): Option[simple.Node] = {
    node match {
      case e: SaxonElem =>
        Some(convertSaxonElem(e))
      case t: SaxonText =>
        Some(convertSaxonText(t))
      case pi: SaxonProcessingInstruction =>
        Some(convertSaxonProcessingInstruction(pi))
      case c: SaxonComment =>
        Some(convertSaxonComment(c))
      case _ => None
    }
  }

  def convertSaxonText(text: SaxonText): simple.Text = {
    // Never CData?
    simple.Node.text(text.text)
  }

  def convertSaxonComment(comment: SaxonComment): simple.Comment = {
    simple.Node.comment(comment.text)
  }

  def convertSaxonProcessingInstruction(processingInstruction: SaxonProcessingInstruction): simple.ProcessingInstruction = {
    simple.Node.processingInstruction(processingInstruction.target, processingInstruction.data)
  }
}
