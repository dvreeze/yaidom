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
package expanded

import java.net.URI
import scala.collection.immutable
import eu.cdevreeze.yaidom

/**
 * Representation of Nodes, and Elems in particular, without any prefixes.
 * After all, the prefixes have no semantical meaning in XML trees.
 *
 * This prefix-less representation does have some shortcomings, though. For example,
 * QNames in text nodes cannot be resolved by scoping information in the element tree itself, etc.
 */
sealed trait Node extends Immutable

trait ParentNode extends Node {

  def children: immutable.IndexedSeq[Node]
}

final class Document(
  val baseUriOption: Option[URI],
  val documentElement: Elem,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  val comments: immutable.IndexedSeq[Comment]) extends ParentNode {

  require(baseUriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  override def children: immutable.IndexedSeq[Node] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[Node](documentElement)
}

final class Elem(
  override val resolvedName: ExpandedName,
  override val resolvedAttributes: Map[ExpandedName, String],
  override val children: immutable.IndexedSeq[Node]) extends ParentNode with ElemLike[Elem] with HasText[Text] {

  require(resolvedName ne null)
  require(resolvedAttributes ne null)
  require(children ne null)

  override def allChildElems: immutable.Seq[Elem] = children collect { case e: Elem => e }

  override def textChildren: immutable.Seq[Text] = children collect { case t: Text => t }
}

final case class Text(text: String, isCData: Boolean) extends Node with TextLike {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

  override def toString: String = if (isCData) """<![CDATA[%s]]>""".format(text) else text
}

final case class ProcessingInstruction(target: String, data: String) extends Node {
  require(target ne null)
  require(data ne null)

  override def toString: String = """<?%s %s?>""".format(target, data)
}

final case class EntityRef(entity: String) extends Node {
  require(entity ne null)

  override def toString: String = """&%s;""".format(entity)
}

final case class Comment(text: String) extends Node {
  require(text ne null)

  override def toString: String = """<!-- %s -->""".format(text)
}

object Node {

  def fromNormalNode(n: yaidom.Node): Node = n match {
    case d: yaidom.Document => Document.fromNormalDocument(d)
    case e: yaidom.Elem => Elem.fromNormalElem(e)
    case t: yaidom.Text => Text.fromNormalText(t)
    case pi: yaidom.ProcessingInstruction => ProcessingInstruction.fromNormalProcessingInstruction(pi)
    case er: yaidom.EntityRef => EntityRef.fromNormalEntityRef(er)
    case c: yaidom.Comment => Comment.fromNormalComment(c)
  }
}

object Document {

  def fromNormalDocument(d: yaidom.Document): Document = new Document(
    baseUriOption = d.baseUriOption,
    documentElement = Elem.fromNormalElem(d.documentElement),
    processingInstructions = d.processingInstructions map { pi => ProcessingInstruction.fromNormalProcessingInstruction(pi) },
    comments = d.comments map { c => Comment.fromNormalComment(c) })
}

object Elem {

  def fromNormalElem(e: yaidom.Elem): Elem = new Elem(
    resolvedName = e.resolvedName,
    resolvedAttributes = e.resolvedAttributes,
    children = e.children map { ch => Node.fromNormalNode(ch) })
}

object Text {

  def fromNormalText(t: yaidom.Text): Text = Text(t.text, t.isCData)
}

object ProcessingInstruction {

  def fromNormalProcessingInstruction(pi: yaidom.ProcessingInstruction): ProcessingInstruction =
    ProcessingInstruction(pi.target, pi.data)
}

object EntityRef {

  def fromNormalEntityRef(er: yaidom.EntityRef): EntityRef = EntityRef(er.entity)
}

object Comment {

  def fromNormalComment(c: yaidom.Comment): Comment = Comment(c.text)
}
