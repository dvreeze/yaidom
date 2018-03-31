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

package eu.cdevreeze.yaidom.utils

import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.ClarkElemNodeApi
import eu.cdevreeze.yaidom.queryapi.ElemCreationApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.TransformableElemLike
import eu.cdevreeze.yaidom.queryapi.UpdatableElemLike

/**
 * Node hierarchy of "Clark" nodes that are useful for element creation.
 * They are like "resolved" elements except that they know about other nodes than
 * element and text nodes alone. Also, the retain attribute order (although semantically not important).
 *
 * @author Chris de Vreeze
 */
object ClarkNode {

  /**
   * Node super-type of "Clark" elements.
   *
   * @author Chris de Vreeze
   */
  sealed trait Node extends Nodes.Node

  sealed trait CanBeDocumentChild extends Node with Nodes.CanBeDocumentChild

  /**
   * "Clark element". Like resolved elements in the API, but adding more node types and keeping
   * order of attributes (although the latter is semantically not important).
   *
   * The purpose of this element implementation is element creation and transformation/update,
   * without having to worry about namespace prefixes and any default namespaces.
   *
   * In other words, using this element implementation it is possible to localize concerns about
   * namespace prefixes, instead of having to worry about them all the time during element creation.
   *
   * @author Chris de Vreeze
   */
  final class Elem(
    val ename:      EName,
    val attributes: immutable.IndexedSeq[(EName, String)],
    val children:   immutable.IndexedSeq[Node])
    extends CanBeDocumentChild
    with Nodes.Elem
    with ClarkElemNodeApi
    with ClarkElemLike
    with UpdatableElemLike
    with TransformableElemLike {

    require(ename ne null) // scalastyle:off null
    require(attributes ne null) // scalastyle:off null
    require(children ne null) // scalastyle:off null

    require(attributes.toMap.size == attributes.size, s"There are duplicate attribute names: $attributes")

    type ThisNode = Node

    type ThisElem = Elem

    def thisElem: ThisElem = this

    override def resolvedName: EName = ename

    override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = attributes

    override def collectChildNodeIndexes(pathEntries: Set[Path.Entry]): Map[Path.Entry, Int] = {
      filterChildElemsWithPathEntriesAndNodeIndexes(pathEntries).map(triple => (triple._2, triple._3)).toMap
    }

    /** Returns the element children */
    override def findAllChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

    /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
    override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
      new Elem(ename, attributes, newChildren)
    }

    override def transformChildElems(f: Elem => Elem): Elem = {
      val newChildren =
        children map {
          case e: Elem => f(e)
          case n: Node => n
        }
      withChildren(newChildren)
    }

    override def transformChildElemsToNodeSeq(f: Elem => immutable.IndexedSeq[Node]): Elem = {
      val newChildren =
        children flatMap {
          case e: Elem => f(e)
          case n: Node => Vector(n)
        }
      withChildren(newChildren)
    }

    /**
     * Creates a copy, altered with the explicitly passed parameters (for ename, attributes and children).
     */
    def copy(
      ename:      EName                                 = this.ename,
      attributes: immutable.IndexedSeq[(EName, String)] = this.attributes,
      children:   immutable.IndexedSeq[Node]            = this.children): Elem = {

      new Elem(ename, attributes, children)
    }

    def withEName(newEName: EName): Elem = {
      copy(ename = newEName)
    }

    def filteringAttributes(p: (EName, String) => Boolean): Elem = {
      withAttributes(attributes filter { case (en, v) => p(en, v) })
    }

    /** Creates a copy, but with the attributes passed as parameter `newAttributes` */
    def withAttributes(newAttributes: immutable.IndexedSeq[(EName, String)]): Elem = {
      copy(attributes = newAttributes)
    }

    /**
     * Functionally adds or updates the given attribute.
     *
     * More precisely, if an attribute with the same name exists at position `idx` (0-based),
     * `withAttributes(attributes.updated(idx, (attributeName -> attributeValue)))` is returned.
     * Otherwise, `withAttributes(attributes :+ (attributeName -> attributeValue))` is returned.
     */
    def plusAttribute(attributeName: EName, attributeValue: String): Elem = {
      val idx = attributes indexWhere { case (attr, value) => attr == attributeName }

      if (idx < 0) {
        withAttributes(attributes :+ (attributeName -> attributeValue))
      } else {
        withAttributes(attributes.updated(idx, (attributeName -> attributeValue)))
      }
    }

    /**
     * Functionally adds or updates the given attribute, if a value is given.
     * That is, returns `if (attributeValueOption.isEmpty) self else plusAttribute(attributeName, attributeValueOption.get)`.
     */
    def plusAttributeOption(attributeName: EName, attributeValueOption: Option[String]): Elem = {
      if (attributeValueOption.isEmpty) thisElem else plusAttribute(attributeName, attributeValueOption.get)
    }

    def filteringChildren(p: Node => Boolean): Elem = {
      withChildren(children.filter(p))
    }

    /**
     * Functionally removes the given attribute, if present.
     *
     * More precisely, returns `withAttributes(thisElem.attributes filterNot (_._1 == attributeName))`.
     */
    def minusAttribute(attributeName: EName): Elem = {
      val newAttributes = thisElem.attributes filterNot { case (attrName, attrValue) => attrName == attributeName }
      withAttributes(newAttributes)
    }

    /** Returns the text children */
    def textChildren: immutable.IndexedSeq[Text] = children collect { case t: Text => t }

    /** Returns the comment children */
    def commentChildren: immutable.IndexedSeq[Comment] = children collect { case c: Comment => c }

    /** Returns the processing instruction children */
    def processingInstructionChildren: immutable.IndexedSeq[ProcessingInstruction] =
      children collect { case pi: ProcessingInstruction => pi }

    /**
     * Returns the concatenation of the texts of text children, including whitespace. Non-text children are ignored.
     * If there are no text children, the empty string is returned.
     */
    override def text: String = {
      val textStrings = textChildren map { t => t.text }
      textStrings.mkString
    }

    /**
     * Returns a copy where inter-element whitespace has been removed, throughout the node tree.
     *
     * That is, for each descendant-or-self element determines if it has at least one child element and no non-whitespace
     * text child nodes, and if so, removes all (whitespace) text children.
     *
     * This method is useful if it is known that whitespace around element nodes is used for formatting purposes, and (in
     * the absence of an XML Schema or DTD) can therefore be treated as "ignorable whitespace". In the case of "mixed content"
     * (if text around element nodes is not all whitespace), this method will not remove any text children of the parent element.
     *
     * XML space attributes (xml:space) are not respected by this method. If such whitespace preservation functionality is needed,
     * it can be written as a transformation where for specific elements this method is not called.
     */
    def removeAllInterElementWhitespace: Elem = {
      def isWhitespaceText(n: Node): Boolean = n match {
        case t: Text if t.trimmedText.isEmpty => true
        case _                                => false
      }

      def isNonTextNode(n: Node): Boolean = n match {
        case t: Text => false
        case n       => true
      }

      val doStripWhitespace = (findChildElem(_ => true).nonEmpty) && (children forall (n => isWhitespaceText(n) || isNonTextNode(n)))

      // Recursive, but not tail-recursive

      val newChildren = {
        val remainder = if (doStripWhitespace) children.filter(n => isNonTextNode(n)) else children

        remainder map {
          case e: Elem =>
            // Recursive call
            e.removeAllInterElementWhitespace
          case n =>
            n
        }
      }

      thisElem.withChildren(newChildren)
    }

    /**
     * Returns a copy where adjacent text nodes have been combined into one text node, throughout the node tree.
     * After combining the adjacent text nodes, all text nodes are transformed by calling the passed function.
     */
    def coalesceAllAdjacentTextAndPostprocess(f: Text => Text): Elem = {
      // Recursive, but not tail-recursive

      def accumulate(childNodes: Seq[Node], newChildrenBuffer: mutable.ArrayBuffer[Node]): Unit = {
        if (childNodes.nonEmpty) {
          val head = childNodes.head

          head match {
            case t: Text =>
              val (textNodes, remainder) = childNodes span {
                case t: Text => true
                case _       => false
              }

              val combinedText: String = textNodes collect { case t: Text => t.text } mkString ""

              newChildrenBuffer += f(Text(combinedText, false)) // No CDATA?

              // Recursive call
              accumulate(remainder, newChildrenBuffer)
            case n: Node =>
              newChildrenBuffer += n

              // Recursive call
              accumulate(childNodes.tail, newChildrenBuffer)
          }
        }
      }

      thisElem transformElemsOrSelf { elm =>
        val newChildrenBuffer = mutable.ArrayBuffer[Node]()

        accumulate(elm.children, newChildrenBuffer)

        elm.withChildren(newChildrenBuffer.toIndexedSeq)
      }
    }

    /** Returns a copy where adjacent text nodes have been combined into one text node, throughout the node tree */
    def coalesceAllAdjacentText: Elem = {
      coalesceAllAdjacentTextAndPostprocess(t => t)
    }

    /**
     * Returns a copy where adjacent text nodes have been combined into one text node, and where all
     * text is normalized, throughout the node tree. Same as calling `coalesceAllAdjacentText` followed by `normalizeAllText`,
     * but more efficient.
     */
    def coalesceAndNormalizeAllText: Elem = {
      coalesceAllAdjacentTextAndPostprocess(t => Text(XmlStringUtils.normalizeString(t.text), t.isCData))
    }

    /**
     * Returns a copy where text nodes have been transformed, throughout the node tree.
     */
    def transformAllText(f: Text => Text): Elem = {
      thisElem transformElemsOrSelf { elm =>
        val newChildren: immutable.IndexedSeq[Node] = {
          elm.children map { (n: Node) =>
            n match {
              case t: Text => f(t)
              case n       => n
            }
          }
        }

        elm.withChildren(newChildren)
      }
    }

    /**
     * Returns a copy where text nodes have been normalized, throughout the node tree.
     * Note that it makes little sense to call this method before `coalesceAllAdjacentText`.
     */
    def normalizeAllText: Elem = {
      transformAllText(t => Text(t.normalizedText, false))
    }

    private def filterChildElemsWithPathEntriesAndNodeIndexes(pathEntries: Set[Path.Entry]): immutable.IndexedSeq[(Elem, Path.Entry, Int)] = {
      // Implementation inspired by findAllChildElemsWithPathEntries.
      // The fewer path entries passed, the more efficient this method is.

      var remainingPathEntries = pathEntries
      val nextEntries = mutable.Map[EName, Int]()

      children.zipWithIndex flatMap {
        case (n: Node, idx) if remainingPathEntries.isEmpty =>
          None
        case (e: Elem, idx) =>
          val ename = e.resolvedName

          if (remainingPathEntries.exists(_.elementName == ename)) {
            val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
            nextEntries.put(ename, entry.index + 1)

            if (pathEntries.contains(entry)) {
              remainingPathEntries -= entry
              Some((e, entry, idx))
            } else {
              None
            }
          } else {
            None
          }
        case (n: Node, idx) =>
          None
      }
    }
  }

  final case class Text(text: String, isCData: Boolean) extends Node with Nodes.Text {
    require(text ne null) // scalastyle:off null
    if (isCData) require(!text.containsSlice("]]>"))

    /** Returns `text.trim`. */
    def trimmedText: String = text.trim

    /** Returns `XmlStringUtils.normalizeString(text)` .*/
    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final case class ProcessingInstruction(target: String, data: String) extends CanBeDocumentChild with Nodes.ProcessingInstruction {
    require(target ne null) // scalastyle:off null
    require(data ne null) // scalastyle:off null
  }

  /**
   * An entity reference. For example:
   * {{{
   * &hello;
   * }}}
   * We obtain this entity reference as follows:
   * {{{
   * EntityRef("hello")
   * }}}
   */
  final case class EntityRef(entity: String) extends Node with Nodes.EntityRef {
    require(entity ne null) // scalastyle:off null
  }

  final case class Comment(text: String) extends CanBeDocumentChild with Nodes.Comment {
    require(text ne null) // scalastyle:off null
  }

  object Node extends ElemCreationApi {

    type NodeType = Node

    type ElemType = Elem

    /**
     * Converts any element or text `Nodes.Node` to a "Clark" `Node`. For other kinds of nodes, an exception is thrown.
     * All descendant-or-self elements must implement `ClarkElemNodeApi`, or else an exception is thrown.
     */
    def from(n: Nodes.Node): Node = n match {
      case e: Nodes.Elem with ClarkElemNodeApi => Elem.from(e)
      case e: Nodes.Elem                       => sys.error(s"Not an element that implements ClarkElemNodeApi")
      case t: Nodes.Text                       => Text(t.text, false)
      case c: Nodes.Comment                    => Comment(c.text)
      case pi: Nodes.ProcessingInstruction     => ProcessingInstruction(pi.target, pi.data)
      case er: Nodes.EntityRef                 => EntityRef(er.entity)
    }

    def elem(ename: EName, children: immutable.IndexedSeq[Node]): Elem = {
      elem(ename, immutable.IndexedSeq[(EName, String)](), children)
    }

    def elem(ename: EName, attributes: immutable.IndexedSeq[(EName, String)], children: immutable.IndexedSeq[Node]): Elem = {
      new Elem(ename, attributes, children)
    }

    def textElem(ename: EName, txt: String): Elem = {
      textElem(ename, immutable.IndexedSeq[(EName, String)](), txt)
    }

    def textElem(ename: EName, attributes: immutable.IndexedSeq[(EName, String)], txt: String): Elem = {
      new Elem(ename, attributes, immutable.IndexedSeq(text(txt)))
    }

    def emptyElem(ename: EName): Elem = {
      emptyElem(ename, immutable.IndexedSeq[(EName, String)]())
    }

    def emptyElem(ename: EName, attributes: immutable.IndexedSeq[(EName, String)]): Elem = {
      new Elem(ename, attributes, Vector())
    }

    def text(textValue: String): Text = Text(text = textValue, isCData = false)

    def cdata(textValue: String): Text = Text(text = textValue, isCData = true)

    def processingInstruction(target: String, data: String): ProcessingInstruction =
      ProcessingInstruction(target, data)

    def entityRef(entity: String): EntityRef = EntityRef(entity)

    def comment(textValue: String): Comment = Comment(textValue)
  }

  object Elem {

    /**
     * Extractor of Elems, to be used for pattern matching.
     */
    def unapply(e: Elem): Option[(EName, immutable.IndexedSeq[(EName, String)], immutable.IndexedSeq[Node])] = {
      Some((e.ename, e.attributes, e.children))
    }

    /**
     * Converts any `Nodes.Elem with ClarkElemNodeApi` element to a "Clark" `Elem`.
     * All descendant-or-self (`Nodes.Elem`) elements must implement `ClarkElemNodeApi`, or else an exception is thrown.
     */
    def from(e: Nodes.Elem with ClarkElemNodeApi): Elem = {
      val children = e.children collect {
        case childElm: Nodes.Elem with ClarkElemNodeApi => childElm
        case childElm: Nodes.Elem                       => sys.error(s"Not an element that implements ClarkElemNodeApi")
        case childText: Nodes.Text                      => childText
        case childComment: Nodes.Comment                => Comment(childComment.text)
        case childPi: Nodes.ProcessingInstruction       => ProcessingInstruction(childPi.target, childPi.data)
        case childEr: Nodes.EntityRef                   => EntityRef(childEr.entity)
      }
      // Recursion, with Node.apply and Elem.apply being mutually dependent
      val resolvedChildren = children map { node => Node.from(node) }

      new Elem(e.resolvedName, e.resolvedAttributes.toIndexedSeq, resolvedChildren)
    }
  }
}
