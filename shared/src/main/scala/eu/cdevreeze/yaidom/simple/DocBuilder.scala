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

package eu.cdevreeze.yaidom.simple

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.DocumentApi

/**
 * Builder of a yaidom Document. Called `DocBuilder` instead of `DocumentBuilder`, because often a JAXP `DocumentBuilder` is in scope too.
 * A `DocBuilder` is itself not a `NodeBuilder`.
 *
 * A `DocBuilder` is constructed from an optional URI, an optional XML declaration, a document element (as `ElemBuilder`), top-level processing
 * instruction builders, if any, and top-level comment builders, if any.
 *
 * @author Chris de Vreeze
 */
@SerialVersionUID(1L)
@deprecated(message = "Deprecated, without replacement", since = "1.12.0")
final class DocBuilder(
    val uriOption: Option[URI],
    val xmlDeclarationOption: Option[XmlDeclaration],
    val children: immutable.IndexedSeq[CanBeDocBuilderChild]) extends DocumentApi with Serializable {

  require(uriOption ne null) // scalastyle:off null
  require(xmlDeclarationOption ne null) // scalastyle:off null
  require(children ne null) // scalastyle:off null

  require(
    children.collect({ case elm: ElemBuilder => elm }).size == 1,
    s"A document (builder) must have exactly one child element (builder) (${uriOption.map(_.toString).getOrElse("No URI found")})")

  type ThisDoc = DocBuilder

  type DocElemType = ElemBuilder

  val documentElement: ElemBuilder = children.collect({ case elm: ElemBuilder => elm }).head

  def processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder] =
    children.collect({ case pi: ProcessingInstructionBuilder => pi })

  def comments: immutable.IndexedSeq[CommentBuilder] =
    children.collect({ case c: CommentBuilder => c })

  /**
   * Creates a [[eu.cdevreeze.yaidom.simple.Document]] from this document builder.
   */
  def build(): Document = {
    val parentScope = Scope.Empty

    Document(
      uriOption = uriOption,
      xmlDeclarationOption = xmlDeclarationOption,
      children = children map { n => n.build(parentScope) })
  }

  /** Returns the tree representation. See the corresponding method in [[eu.cdevreeze.yaidom.simple.Document]]. */
  final def toTreeRepr(): String = build().toTreeRepr()

  /** Returns `toTreeRepr` */
  final override def toString: String = toTreeRepr()
}

@deprecated(message = "Deprecated, without replacement", since = "1.12.0")
object DocBuilder {

  /**
   * Creates a document builder from a document.
   */
  def fromDocument(doc: Document): DocBuilder = {
    import NodeBuilder._

    val parentScope = Scope.Empty

    new DocBuilder(
      uriOption = doc.uriOption,
      xmlDeclarationOption = doc.xmlDeclarationOption,
      children = doc.children.map(n => fromCanBeDocumentChild(n)(parentScope)))
  }

  def document(
    uriOption: Option[String],
    children: immutable.IndexedSeq[CanBeDocBuilderChild]): DocBuilder = {

    new DocBuilder(uriOption map { uriString => new URI(uriString) }, None, children)
  }

  def document(
    uriOption: Option[String],
    xmlDeclarationOption: Option[XmlDeclaration],
    children: immutable.IndexedSeq[CanBeDocBuilderChild]): DocBuilder = {

    new DocBuilder(uriOption map { uriString => new URI(uriString) }, xmlDeclarationOption, children)
  }

  def document(
    uriOption: Option[String] = None,
    xmlDeclarationOption: Option[XmlDeclaration],
    documentElement: ElemBuilder,
    processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder] = Vector(),
    comments: immutable.IndexedSeq[CommentBuilder] = Vector()): DocBuilder = {

    document(uriOption, xmlDeclarationOption, processingInstructions ++ comments ++ Vector(documentElement))
  }
}
