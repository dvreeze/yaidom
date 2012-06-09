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

import scala.collection.{ immutable, mutable }

/**
 * Immutable "resolved" Node. It is called "resolved" because the element trees in this package only contain resolved element and
 * attribute names. Qualified names (and therefore prefixes) are gone in this representation.
 *
 * "Resolved" nodes can be compared for equality. This notion of equality only considers elements and text nodes.
 * By removing qualified names and namespace declarations from this node representation, one source of complexity for equality
 * comparisons is gone.
 *
 * The notion of equality defined here is simple to understand, but "naive". The user of the API must take control over what is
 * compared for equality. Much of the "magic" in the equality relation is gone, but the API user has to work harder to compare apples to
 * apples, as explained below. Other "magic" remains, because the text and attribute values here are untyped.
 *
 * The notion of equality remotely reminds of the standard XQuery function `fn:deep-equal`, but text and attribute values are untyped
 * in yaidom's case, among many other differences.
 *
 * As mentioned above, documents, comments, processing instructions and entity references do not occur in this node hierarchy.
 * Moreover, text nodes do not know whether they originate from (or must be serialized as) CDATA sections or not.
 *
 * There are several reasons why equality would return false for 2 elements that should be considered equal, such as:
 * <ul>
 * <li>The text and attribute values are untyped, so equality of numbers 2 and 2.0 is not detected</li>
 * <li>Differences in "ignorable whitespace", meant only for pretty-printing</li>
 * <li>Text that is possibly divided over several adjacent text nodes (possibly including CDATA text nodes), but should be "coalesced"</li>
 * <li>Text that is only equal after normalizing</li>
 * </ul>
 * Note that a validating parser knows the content model, so knows precisely which whitespace is "ignorable", for example, but once the parsed
 * XML is turned into untyped yaidom nodes, this information is lost. (Of course in principle PSVI data could be added to `Elem`s,
 * just like `ElemPath`s are added to elements in class `IndexedDocument`, using the element UIDs as keys, but that is beyond the scope
 * of yaidom.)
 *
 * Class [[eu.cdevreeze.yaidom.resolved.Elem]] has some methods to mitigate the above-mentioned small differences among elements (except
 * for the first difference, related to untyped data).
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
  val resolvedName: EName,
  val resolvedAttributes: Map[EName, String],
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

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(resolvedName, resolvedAttributes, newChildren)
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  def plusChild(newChild: Node): Elem = withChildren(self.children :+ newChild)

  /** Returns a copy where inter-element whitespace has been removed, throughout the node tree */
  def removeAllInterElementWhitespace: Elem = {
    def isWhitespaceText(n: Node): Boolean = n match {
      case t: Text if t.trimmedText.isEmpty => true
      case _ => false
    }

    def isElem(n: Node): Boolean = n match {
      case e: Elem => true
      case _ => false
    }

    val doStripWhitespace = (children forall (n => isWhitespaceText(n) || isElem(n))) && (!allChildElems.isEmpty)

    // Recursive, but not tail-recursive

    val newChildren = {
      val remainder = if (doStripWhitespace) allChildElems else children

      remainder map {
        case e: Elem => e.removeAllInterElementWhitespace
        case n => n
      }
    }

    self.withChildren(newChildren)
  }

  /** Returns a copy where adjacent text nodes have been combined into one text node, throughout the node tree */
  def coalesceAllAdjacentText: Elem = {
    val newChildren = mutable.ArrayBuffer[Node]()

    // Recursive, but not tail-recursive

    def accumulate(childNodes: Seq[Node]) {
      if (!childNodes.isEmpty) {
        val head = childNodes.head

        head match {
          case t: Text =>
            val (textNodes, remainder) = childNodes span {
              case t: Text => true
              case _ => false
            }

            val combinedText: String = textNodes collect { case t: Text => t.text } mkString ""

            newChildren += Text(combinedText)
            accumulate(remainder)
          case e: Elem =>
            newChildren += e
            accumulate(childNodes.tail)
        }
      }
    }

    accumulate(self.children)

    val resultChildren = newChildren.toIndexedSeq map { (n: Node) =>
      n match {
        case e: Elem => e.coalesceAllAdjacentText
        case n => n
      }
    }

    self.withChildren(resultChildren)
  }

  /**
   * Returns a copy where text nodes have been normalized, throughout the node tree.
   * Note that it makes little sense to call this method before `coalesceAllAdjacentText`.
   */
  def normalizeAllText: Elem = {
    // Recursive, but not tail-recursive

    val newChildren: immutable.IndexedSeq[Node] = {
      self.children map { (n: Node) =>
        n match {
          case e: Elem => e.normalizeAllText
          case t: Text => Text(t.normalizedText)
          case n => n
        }
      }
    }

    self.withChildren(newChildren)
  }

  /**
   * Returns a copy where adjacent text nodes have been combined into one text node, and where all
   * text is normalized, throughout the node tree. Same as calling `coalesceAllAdjacentText` followed by `normalizeAllText`,
   * but more efficient.
   */
  def coalesceAndNormalizeAllText: Elem = {
    val newChildren = mutable.ArrayBuffer[Node]()

    // Recursive, but not tail-recursive

    def accumulate(childNodes: Seq[Node]) {
      if (!childNodes.isEmpty) {
        val head = childNodes.head

        head match {
          case t: Text =>
            val (textNodes, remainder) = childNodes span {
              case t: Text => true
              case _ => false
            }

            val combinedText: String = textNodes collect { case t: Text => t.text } mkString ""

            newChildren += Text(XmlStringUtils.normalizeString(combinedText))
            accumulate(remainder)
          case e: Elem =>
            newChildren += e
            accumulate(childNodes.tail)
        }
      }
    }

    accumulate(self.children)

    val resultChildren = newChildren.toIndexedSeq map { (n: Node) =>
      n match {
        case e: Elem => e.coalesceAndNormalizeAllText
        case n => n
      }
    }

    self.withChildren(resultChildren)
  }
}

final case class Text(text: String) extends Node {
  require(text ne null)

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

object Node {

  /**
   * Converts a yaidom `Node` to a "resolved" `Node`.
   * Note that the entity references, comments, processing instructions and top-level documents are lost.
   * All that remains are elements (without qualified names) and text nodes.
   * Losing the qualified names means that the prefixes are lost. Losing the prefixes not only affects serialization of
   * the `Node` to an XML string, but also affects attribute values and text nodes in which those prefixes are used.
   *
   * Note that if there are any unresolved entities in the yaidom `Node`, those entity references are silently ignored!
   * This is definitely something to keep in mind!
   */
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
