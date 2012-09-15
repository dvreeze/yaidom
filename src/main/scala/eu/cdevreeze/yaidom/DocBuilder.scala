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
 * Builder of a yaidom Document. Called `DocBuilder` instead of `DocumentBuilder`, because often a JAXP `DocumentBuilder` is in scope too.
 */
@SerialVersionUID(1L)
final class DocBuilder(
  val baseUriOption: Option[URI],
  val documentElement: ElemBuilder,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstructionBuilder],
  val comments: immutable.IndexedSeq[CommentBuilder]) extends Immutable with Serializable { self =>

  require(baseUriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  def children: immutable.IndexedSeq[NodeBuilder] =
    (processingInstructions ++ comments) :+ documentElement

  def build(): Document = {
    val parentScope = Scope.Empty

    Document(
      baseUriOption = self.baseUriOption,
      documentElement = documentElement.build(parentScope),
      processingInstructions = processingInstructions map { (pi: ProcessingInstructionBuilder) => pi.build(parentScope) },
      comments = comments map { (c: CommentBuilder) => c.build(parentScope) })
  }

  /** Returns the tree representation. See the corresponding method in [[eu.cdevreeze.yaidom.Document]]. */
  final def toTreeRepr(): String = build().toTreeRepr()

  /** Returns `toTreeRepr` */
  final override def toString: String = toTreeRepr()
}

object DocBuilder {

  def fromDocument(doc: Document): DocBuilder = {
    import NodeBuilder._

    val parentScope = Scope.Empty

    new DocBuilder(
      baseUriOption = doc.baseUriOption,
      documentElement = fromNode(doc.documentElement)(parentScope).asInstanceOf[ElemBuilder],
      processingInstructions = doc.processingInstructions collect { case pi: ProcessingInstruction => fromNode(pi)(parentScope).asInstanceOf[ProcessingInstructionBuilder] },
      comments = doc.comments collect { case c => fromNode(c)(parentScope).asInstanceOf[CommentBuilder] })
  }
}
