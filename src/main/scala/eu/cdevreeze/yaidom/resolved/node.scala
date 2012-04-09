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
package resolved

import scala.collection.immutable

/**
 * Immutable "resolved" Node. It can be compared for equality. This (arbitrary?) notion of equality only considers elements and
 * text nodes.
 *
 * Documents, comments, processing instructions and entity references do not occur in this node hierarchy.
 * Moreover, text nodes do not know whether they originate from (or must be serialized as) CDATA sections or not.
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable

trait ParentNode extends Node {

  def children: immutable.IndexedSeq[Node]
}

/**
 * Element as abstract data type. It contains only expanded names, not qualified names. This reminds of James Clark notation
 * for XML trees and expanded names, where qualified names are absent.
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API.
 */
final case class Elem(
  val resolvedName: ExpandedName,
  val resolvedAttributes: Map[ExpandedName, String],
  override val children: immutable.IndexedSeq[Node]) extends ParentNode with ElemLike[Elem] { self =>

  require(resolvedName ne null)
  require(resolvedAttributes ne null)
  require(children ne null)

  /** The local name (or local part). Convenience method. */
  def localName: String = resolvedName.localPart

  /** Returns all child elements */
  override def allChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[Text] = children collect { case t: Text => t }

  /**
   * Returns the concatenation of the texts of text children, including whitespace. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)`. */
  def normalizedText: String = XmlStringUtils.normalizeString(text)

  /** Creates a copy, but with (only) the children passed as parameter newChildren */
  def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(resolvedName, resolvedAttributes, newChildren)
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  def plusChild(newChild: Node): Elem = withChildren(self.children :+ newChild)
}

final case class Text(text: String) extends Node {
  require(text ne null)

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

object Node {

  def apply(n: eu.cdevreeze.yaidom.Node): Node = n match {
    case e: eu.cdevreeze.yaidom.Elem => Elem(e)
    case t: eu.cdevreeze.yaidom.Text => Text(t)
    case n => sys.error("Not an element or text node: %s".format(n))
  }
}

object Elem {

  def apply(e: eu.cdevreeze.yaidom.Elem): Elem = {
    val children = e.children collect {
      case childElm: eu.cdevreeze.yaidom.Elem => childElm
      case childText: eu.cdevreeze.yaidom.Text => childText
    }
    val resolvedChildren = children map { node => Node(node) }

    Elem(e.resolvedName, e.resolvedAttributes, resolvedChildren)
  }
}

object Text {

  def apply(t: eu.cdevreeze.yaidom.Text): Text = Text(t.text)
}
