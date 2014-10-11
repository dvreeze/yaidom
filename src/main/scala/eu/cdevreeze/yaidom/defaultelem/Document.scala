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

package eu.cdevreeze.yaidom.defaultelem

import java.net.URI

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.PrettyPrinting.LineSeq
import eu.cdevreeze.yaidom.PrettyPrinting.LineSeqSeq
import eu.cdevreeze.yaidom.PrettyPrinting.toStringLiteral
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.DocumentApi

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
 * A `Document` is constructed from an optional URI, a document element (as `Elem`), top-level processing instructions,
 * if any, and top-level comments, if any.
 *
 * Note that class `Document` does not have any query methods for `Elem` instances. In particular, the `ParentElemApi` does not
 * apply to documents. Therefore, given a document, querying for elements (other than the document element itself) always goes
 * via the document element.
 *
 * @author Chris de Vreeze
 */
@SerialVersionUID(1L)
final class Document(
  val uriOption: Option[URI],
  val documentElement: Elem,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  val comments: immutable.IndexedSeq[Comment]) extends DocumentApi[Elem] with Immutable with Serializable {

  require(uriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  /**
   * Returns the immediate child nodes of the document. That includes at least the document element, but top-level
   * comments and processing instructions are also returned.
   */
  def children: immutable.IndexedSeq[Node] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[Node](documentElement)

  /** Expensive method to obtain all processing instructions, throughout the tree */
  def allProcessingInstructions: immutable.IndexedSeq[ProcessingInstruction] = {
    val elemPIs: immutable.IndexedSeq[ProcessingInstruction] =
      documentElement.findAllElemsOrSelf flatMap { e: Elem => e.processingInstructionChildren }
    processingInstructions ++ elemPIs
  }

  /** Expensive method to obtain all comments, throughout the tree */
  def allComments: immutable.IndexedSeq[Comment] = {
    val elemComments: immutable.IndexedSeq[Comment] =
      documentElement.findAllElemsOrSelf flatMap { e: Elem => e.commentChildren }
    comments ++ elemComments
  }

  /** Creates a copy, but with the new documentElement passed as parameter newRoot */
  def withDocumentElement(newRoot: Elem): Document = new Document(
    uriOption = this.uriOption,
    documentElement = newRoot,
    processingInstructions = this.processingInstructions,
    comments = this.comments)

  /** Creates a copy, but with the new uriOption passed as parameter newUriOption */
  def withUriOption(newUriOption: Option[URI]): Document = new Document(
    uriOption = newUriOption,
    documentElement = this.documentElement,
    processingInstructions = this.processingInstructions,
    comments = this.comments)

  /** Returns `withDocumentElement(this.documentElement.updated(path)(f))`. */
  def updated(path: Path)(f: Elem => Elem): Document = withDocumentElement(this.documentElement.updated(path)(f))

  /** Returns `updated(path) { e => newElem }` */
  def updated(path: Path, newElem: Elem): Document = updated(path) { e => newElem }

  /** Returns `withDocumentElement(this.documentElement.updatedWithNodeSeq(path)(f))` */
  def updatedWithNodeSeq(path: Path)(f: Elem => immutable.IndexedSeq[Node]): Document =
    withDocumentElement(this.documentElement.updatedWithNodeSeq(path)(f))

  /** Returns `withDocumentElement(this.documentElement.updatedWithNodeSeq(path, newNodes))` */
  def updatedWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[Node]): Document =
    withDocumentElement(this.documentElement.updatedWithNodeSeq(path, newNodes))

  /** Returns `withDocumentElement(this.documentElement.updatedAtPaths(paths)(f))` */
  def updatedAtPaths(paths: Set[Path])(f: (Elem, Path) => Elem): Document =
    withDocumentElement(this.documentElement.updatedAtPaths(paths)(f))

  /** Returns `withDocumentElement(this.documentElement.transformElemsOrSelf(f))` */
  def transformElemsOrSelf(f: Elem => Elem): Document =
    withDocumentElement(this.documentElement.transformElemsOrSelf(f))

  /** Returns `withDocumentElement(this.documentElement.transformElemsToNodeSeq(f))` */
  def transformElemsToNodeSeq(f: Elem => immutable.IndexedSeq[Node]): Document =
    withDocumentElement(this.documentElement.transformElemsToNodeSeq(f))

  final def toTreeRepr(): String = {
    val sb = new StringBuilder
    toTreeReprAsLineSeq(0)(2).addToStringBuilder(sb)
    sb.toString
  }

  /** Returns the tree representation string corresponding to this element, that is, `toTreeRepr`. Possibly expensive! */
  final override def toString: String = toTreeRepr

  private[yaidom] def toTreeReprAsLineSeq(indent: Int)(indentStep: Int): LineSeq = {
    val parentScope = Scope.Empty

    val uriOptionLineSeq: LineSeq =
      if (this.uriOption.isEmpty) {
        val line = "uriOption = None"
        LineSeq(line)
      } else {
        val line = s"uriOption = Some(${toStringLiteral(this.uriOption.get.toString)})"
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

    val contentParts: Vector[LineSeq] = Vector(Some(uriOptionLineSeq), Some(documentElementLineSeq), piLineSeqOption, commentsLineSeqOption).flatten
    val content: LineSeq = LineSeqSeq(contentParts: _*).mkLineSeq(",").shift(indentStep)

    LineSeqSeq(
      LineSeq("document("),
      content,
      LineSeq(")")).mkLineSeq.shift(indent)
  }
}

object Document {

  /**
   * Creates a `Document` from an optional URI, the document element, and the top-level comments and processing
   * instructions, if any. Unlike the primary constructor, this factory method defaults the processing instructions and
   * comments to empty collections. In other words, only the optional URI and the document element are mandatory parameters.
   */
  def apply(
    uriOption: Option[URI],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[Comment] = immutable.IndexedSeq()): Document = {

    new Document(uriOption, documentElement, processingInstructions, comments)
  }

  /**
   * Creates a `Document` from only the document element. The URI is empty, and so are the collections of top-level
   * comments and processing instructions.
   */
  def apply(documentElement: Elem): Document = apply(None, documentElement)

  def document(
    uriOption: Option[String] = None,
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = Vector(),
    comments: immutable.IndexedSeq[Comment] = Vector()): Document = {

    new Document(
      uriOption map { uriString => new URI(uriString) },
      documentElement,
      processingInstructions,
      comments)
  }
}
