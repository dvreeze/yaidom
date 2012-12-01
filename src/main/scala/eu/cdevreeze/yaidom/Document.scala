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

import java.net.URI
import scala.collection.{ immutable, mutable }
import PrettyPrinting._

/**
 * `Document`. Although at first sight the document root element seems to be the root node, this is not entirely true.
 * For example, there may be comments at top level, outside the document root element.
 *
 * The document is itself not a `Node`. This choice has the following advantages:
 * <ul>
 * <li>Documents are indeed prevented (at compile-time) from occurring as "child nodes"</li>
 * <li>The API is cleaner. For example, (unlike "elements") document methods like `toTreeRepr` should
 * not be passed a parent scope. By not considering a Document a Node, it is more visible that parent scopes
 * are irrelevant for Documents, unlike for "elements".</li>
 * </ul>
 *
 * @author Chris de Vreeze
 */
@SerialVersionUID(1L)
final class Document(
  val baseUriOption: Option[URI],
  val documentElement: Elem,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  val comments: immutable.IndexedSeq[Comment]) extends Immutable with Serializable {

  require(baseUriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  def children: immutable.IndexedSeq[Node] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[Node](documentElement)

  /** Expensive method to obtain all processing instructions */
  def allProcessingInstructions: immutable.IndexedSeq[ProcessingInstruction] = {
    val elemPIs: immutable.IndexedSeq[ProcessingInstruction] =
      documentElement.findAllElemsOrSelf flatMap { e: Elem => e.processingInstructionChildren }
    processingInstructions ++ elemPIs
  }

  /** Expensive method to obtain all comments */
  def allComments: immutable.IndexedSeq[Comment] = {
    val elemComments: immutable.IndexedSeq[Comment] =
      documentElement.findAllElemsOrSelf flatMap { e: Elem => e.commentChildren }
    comments ++ elemComments
  }

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

  /** Returns `withDocumentElement(this.documentElement.updated(path)(f))`. */
  def updated(path: ElemPath)(f: Elem => Elem): Document = withDocumentElement(this.documentElement.updated(path)(f))

  /** Returns `updated(path) { e => newElem }` */
  def updated(path: ElemPath, newElem: Elem): Document = updated(path) { e => newElem }

  /** Returns `withDocumentElement(this.documentElement.updated(pf))` */
  def updated(pf: PartialFunction[Elem, Elem]): Document = withDocumentElement(this.documentElement.updated(pf))

  final def toTreeRepr(): String = toTreeReprAsLineSeq(0)(2).mkString

  /** Returns the tree representation string corresponding to this element, that is, `toTreeRepr`. Possibly expensive! */
  final override def toString: String = toTreeRepr

  private[yaidom] def toTreeReprAsLineSeq(indent: Int)(indentStep: Int): LineSeq = {
    val parentScope = Scope.Empty

    val baseUriOptionLineSeq: LineSeq =
      if (this.baseUriOption.isEmpty) {
        val line = "baseUriOption = None"
        LineSeq(line)
      } else {
        val line = "baseUriOption = Some(%s)".format(toStringLiteral(this.baseUriOption.get.toString))
        LineSeq(line)
      }

    val documentElementLineSeq: LineSeq = {
      val firstLine = LineSeq("documentElement =")
      val contentLines = this.documentElement.toTreeReprAsLineSeq(parentScope, indentStep)(indentStep)
      firstLine ++ contentLines
    }

    val piLineSeqOption: Option[LineSeq] =
      if (this.processingInstructions.isEmpty) None else {
        val firstLine = LineSeq("processingInstructions = Vector(")
        val contentLines = {
          val groups =
            this.processingInstructions map { pi =>
              pi.toTreeReprAsLineSeq(parentScope, indentStep)(indentStep)
            }
          val result = LineSeqSeq(groups: _*).mkLineSeq(",")
          result
        }
        val lastLine = LineSeq(")")

        Some(LineSeqSeq(firstLine, contentLines, lastLine).mkLineSeq)
      }

    val commentsLineSeqOption: Option[LineSeq] =
      if (this.comments.isEmpty) None else {
        val firstLine = LineSeq("comments = Vector(")
        val contentLines = {
          val groups =
            this.comments map { comment =>
              comment.toTreeReprAsLineSeq(parentScope, indentStep)(indentStep)
            }
          val result = LineSeqSeq(groups: _*).mkLineSeq(",")
          result
        }
        val lastLine = LineSeq(")")

        Some(LineSeqSeq(firstLine, contentLines, lastLine).mkLineSeq)
      }

    val contentParts: Vector[LineSeq] = Vector(Some(baseUriOptionLineSeq), Some(documentElementLineSeq), piLineSeqOption, commentsLineSeqOption).flatten
    val content: LineSeq = LineSeqSeq(contentParts: _*).mkLineSeq(",").shift(indentStep)

    LineSeqSeq(
      LineSeq("document("),
      content,
      LineSeq(")")).mkLineSeq.shift(indent)
  }
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
}
