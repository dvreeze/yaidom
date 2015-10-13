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

package eu.cdevreeze.yaidom.simple

import java.net.URI

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.PrettyPrinting.LineSeq
import eu.cdevreeze.yaidom.PrettyPrinting.LineSeqSeq
import eu.cdevreeze.yaidom.PrettyPrinting.toStringLiteral
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.Nodes

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
 * A `Document` is constructed from an optional URI, an optional XML declaration, a document element (as `Elem`), top-level processing instructions,
 * if any, and top-level comments, if any.
 *
 * Note that class `Document` does not have any query methods for `Elem` instances. In particular, the `ElemApi` does not
 * apply to documents. Therefore, given a document, querying for elements (other than the document element itself) always goes
 * via the document element.
 *
 * @author Chris de Vreeze
 */
@SerialVersionUID(1L)
final class Document(
  val uriOption: Option[URI],
  val xmlDeclarationOption: Option[XmlDeclaration],
  val children: immutable.IndexedSeq[CanBeDocumentChild]) extends DocumentApi[Elem] with Immutable with Serializable {

  require(uriOption ne null)
  require(xmlDeclarationOption ne null)
  require(children ne null)

  require(
    children.collect({ case elm: Elem => elm }).size == 1,
    s"A document must have exactly one child element (${uriOption.map(_.toString).getOrElse("No URI found")})")

  val documentElement: Elem = children.collect({ case elm: Elem => elm }).head

  def processingInstructions: immutable.IndexedSeq[ProcessingInstruction] =
    children.collect({ case pi: ProcessingInstruction => pi })

  def comments: immutable.IndexedSeq[Comment] =
    children.collect({ case c: Comment => c })

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
    xmlDeclarationOption = this.xmlDeclarationOption,
    children = this.children map {
      case elm: Elem                => newRoot
      case node: CanBeDocumentChild => node
    })

  /** Creates a copy, but with the new uriOption passed as parameter newUriOption */
  def withUriOption(newUriOption: Option[URI]): Document = new Document(
    uriOption = newUriOption,
    xmlDeclarationOption = this.xmlDeclarationOption,
    children = this.children)

  /** Creates a copy, but with the new xmlDeclarationOption passed as parameter newXmlDeclarationOption */
  def withXmlDeclarationOption(newXmlDeclarationOption: Option[XmlDeclaration]): Document = new Document(
    uriOption = this.uriOption,
    xmlDeclarationOption = newXmlDeclarationOption,
    children = this.children)

  /** Returns `withDocumentElement(this.documentElement.updateElemOrSelf(path)(f))`. */
  def updateElemOrSelf(path: Path)(f: Elem => Elem): Document =
    withDocumentElement(this.documentElement.updateElemOrSelf(path)(f))

  /** Returns `updateElemOrSelf(path) { e => newElem }` */
  def updateElemOrSelf(path: Path, newElem: Elem): Document =
    updateElemOrSelf(path) { e => newElem }

  /** Returns `withDocumentElement(this.documentElement.updateElemWithNodeSeq(path)(f))` */
  def updateElemWithNodeSeq(path: Path)(f: Elem => immutable.IndexedSeq[Node]): Document =
    withDocumentElement(this.documentElement.updateElemWithNodeSeq(path)(f))

  /** Returns `withDocumentElement(this.documentElement.updateElemWithNodeSeq(path, newNodes))` */
  def updateElemWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[Node]): Document =
    withDocumentElement(this.documentElement.updateElemWithNodeSeq(path, newNodes))

  /** Returns `withDocumentElement(this.documentElement.updateElemsOrSelf(paths)(f))` */
  def updateElemsOrSelf(paths: Set[Path])(f: (Elem, Path) => Elem): Document =
    withDocumentElement(this.documentElement.updateElemsOrSelf(paths)(f))

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

    val childrenLineSeq: LineSeq = {
      val firstLine = LineSeq("children = Vector(")

      val contentLines = {
        val groups =
          this.children map { ch =>
            ch.toTreeReprAsLineSeq(parentScope, indentStep)(indentStep)
          }
        val result = LineSeqSeq(groups: _*).mkLineSeq(",")
        result
      }

      val lastLine = LineSeq(")")

      LineSeqSeq(firstLine, contentLines, lastLine).mkLineSeq
    }

    val contentParts: Vector[LineSeq] = Vector(uriOptionLineSeq, childrenLineSeq)
    val content: LineSeq = LineSeqSeq(contentParts: _*).mkLineSeq(",").shift(indentStep)

    LineSeqSeq(
      LineSeq("document("),
      content,
      LineSeq(")")).mkLineSeq.shift(indent)
  }

  @deprecated(message = "Renamed to 'updateElemOrSelf'", since = "1.5.0")
  def updated(path: Path)(f: Elem => Elem): Document =
    updateElemOrSelf(path)(f)

  @deprecated(message = "Renamed to 'updateElemOrSelf'", since = "1.5.0")
  def updated(path: Path, newElem: Elem): Document =
    updateElemOrSelf(path, newElem)

  @deprecated(message = "Renamed to 'updateElemWithNodeSeq'", since = "1.5.0")
  def updatedWithNodeSeq(path: Path)(f: Elem => immutable.IndexedSeq[Node]): Document =
    updateElemWithNodeSeq(path)(f)

  @deprecated(message = "Renamed to 'updateElemWithNodeSeq'", since = "1.5.0")
  def updatedWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[Node]): Document =
    updateElemWithNodeSeq(path, newNodes)

  @deprecated(message = "Renamed to 'updateElemsOrSelf'", since = "1.5.0")
  def updatedAtPaths(paths: Set[Path])(f: (Elem, Path) => Elem): Document =
    updateElemsOrSelf(paths)(f)
}

object Document {

  /**
   * Creates a `Document` from an optional URI, an optional XML declaration, and the document children.
   * Precisely one of the document children must be an element. This factory method retains the order of
   * document children, whether any comments are before or after the root element.
   */
  def apply(
    uriOption: Option[URI],
    xmlDeclarationOption: Option[XmlDeclaration],
    children: immutable.IndexedSeq[CanBeDocumentChild]): Document = {

    new Document(uriOption, xmlDeclarationOption, children)
  }

  /**
   * Creates a `Document` from an optional URI, an optional XML declaration, the document element, and the top-level comments and processing
   * instructions, if any. Unlike the primary constructor, this factory method defaults the processing instructions and
   * comments to empty collections. In other words, only the optional URI and the document element are mandatory parameters.
   */
  def apply(
    uriOption: Option[URI],
    xmlDeclarationOption: Option[XmlDeclaration],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = immutable.IndexedSeq(),
    comments: immutable.IndexedSeq[Comment] = immutable.IndexedSeq()): Document = {

    new Document(
      uriOption,
      xmlDeclarationOption,
      processingInstructions ++ comments ++ Vector(documentElement))
  }

  /**
   * Creates a `Document` from only the document element and optional URI. The collections of top-level
   * comments and processing instructions are empty, and the optional XML declaration is absent.
   */
  def apply(
    uriOption: Option[URI],
    documentElement: Elem): Document = {

    apply(uriOption, None, documentElement)
  }

  /**
   * Creates a `Document` from only the document element. The URI is empty, and so are the collections of top-level
   * comments and processing instructions.
   */
  def apply(documentElement: Elem): Document = apply(None, None, documentElement)

  def document(
    uriOption: Option[String],
    xmlDeclarationOption: Option[XmlDeclaration],
    children: immutable.IndexedSeq[CanBeDocumentChild]): Document = {

    apply(uriOption map { uriString => new URI(uriString) }, xmlDeclarationOption, children)
  }

  def document(
    uriOption: Option[String] = None,
    xmlDeclarationOption: Option[XmlDeclaration],
    documentElement: Elem,
    processingInstructions: immutable.IndexedSeq[ProcessingInstruction] = Vector(),
    comments: immutable.IndexedSeq[Comment] = Vector()): Document = {

    apply(
      uriOption map { uriString => new URI(uriString) },
      xmlDeclarationOption,
      documentElement,
      processingInstructions,
      comments)
  }
}
