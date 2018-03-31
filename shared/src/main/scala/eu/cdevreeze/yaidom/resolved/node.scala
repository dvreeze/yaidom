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

package eu.cdevreeze.yaidom.resolved

import scala.Vector
import scala.collection.immutable
import scala.collection.mutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemNodeApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.ElemCreationApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.TransformableElemLike
import eu.cdevreeze.yaidom.queryapi.UpdatableElemLike

/**
 * Immutable "resolved" Node. It is called "resolved" because the element trees in this package only contain resolved element and
 * attribute names. Qualified names (and therefore prefixes) are gone in this representation.
 *
 * "Resolved" nodes can be compared for <em>equality</em>. This notion of equality only considers elements and text nodes.
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
 * <li>QNames in text or attribute values depend on in-scope namespaces for resolution</li>
 * <li>Differences in "ignorable whitespace", meant only for pretty-printing</li>
 * <li>Text that is possibly divided over several adjacent text nodes (possibly including CDATA text nodes), but should be "coalesced"</li>
 * <li>Text that is only equal after normalizing</li>
 * </ul>
 * Note that a validating parser knows the content model, so knows precisely which whitespace is "ignorable", for example, but once the parsed
 * XML is turned into untyped yaidom nodes, this information is lost. (Of course in principle PSVI data could be added to `Elem`s,
 * indexed by "paths", but that is beyond the scope of yaidom.)
 *
 * As mentioned above, QNames in text or attribute values depend on in-scope namespaces for resolution. Yet "resolved" nodes do
 * not keep track of in-scope namespaces, because QNames do not exist for "resolved" nodes. So, be extra careful when comparing
 * "resolved" elements containing QNames in text or attribute values. Either keep track of prefix bindings (scopes) outside the
 * "resolved" element, or convert the QNames before turning a normal Elem into a "resolved" Elem.
 *
 * Class [[eu.cdevreeze.yaidom.resolved.Elem]] has some methods to mitigate the above-mentioned small differences among elements (except
 * for the first difference, related to untyped data).
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Nodes.Node with Immutable

/**
 * Element as abstract data type. It contains only expanded names, not qualified names. This reminds of James Clark notation
 * for XML trees and expanded names, where qualified names are absent.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API, just like in the rest of yaidom.
 *
 * To illustrate <em>equality</em> comparisons in action, consider the following example yaidom `Elem`, named `schemaElem1`:
 * {{{
 * <xsd:schema xmlns:xsd="http://www.w3.org/2001/XMLSchema" targetNamespace="http://book" elementFormDefault="qualified">
 *   <xsd:element name="book">
 *     <xsd:complexType>
 *       <xsd:sequence>
 *         <xsd:element name="isbn" type="xsd:string" />
 *         <xsd:element name="title" type="xsd:string" />
 *         <xsd:element name="authors" type="xsd:string" />
 *       </xsd:sequence>
 *     </xsd:complexType>
 *   </xsd:element>
 * </xsd:schema>
 * }}}
 * Now consider the following equivalent yaidom `Elem`, named `schemaElem2`, differing only in namespace prefixes, and in
 * indentation:
 * {{{
 * <xs:schema xmlns:xs="http://www.w3.org/2001/XMLSchema" targetNamespace="http://book" elementFormDefault="qualified">
 *     <xs:element name="book">
 *         <xs:complexType>
 *             <xs:sequence>
 *                 <xs:element name="isbn" type="xs:string" />
 *                 <xs:element name="title" type="xs:string" />
 *                 <xs:element name="authors" type="xs:string" />
 *             </xs:sequence>
 *         </xs:complexType>
 *     </xs:element>
 * </xs:schema>
 * }}}
 *
 * These 2 XML trees can be considered equal, if we take indentation and namespace prefixes out of the equation. Note that
 * namespace prefixes also occur in the "type" attributes! The following equality comparison returns true:
 * {{{
 * def replaceTypeAttributes(elem: Elem): Elem = {
 *   elem transformElemsOrSelf { e =>
 *     e.plusAttributeOption(QName("type"), e.attributeAsResolvedQNameOption(EName("type")).map(_.toString))
 *   }
 * }
 *
 * resolved.Elem(replaceTypeAttributes(schemaElem1)).removeAllInterElementWhitespace ==
 *   resolved.Elem(replaceTypeAttributes(schemaElem2)).removeAllInterElementWhitespace
 * }}}
 */
final case class Elem(
  override val resolvedName:       EName,
  override val resolvedAttributes: Map[EName, String],
  override val children:           immutable.IndexedSeq[Node])
  extends Node
  with Nodes.Elem
  with ClarkElemNodeApi
  with ClarkElemLike
  with UpdatableElemLike
  with TransformableElemLike {

  require(resolvedName ne null) // scalastyle:off null
  require(resolvedAttributes ne null) // scalastyle:off null
  require(children ne null) // scalastyle:off null

  type ThisNode = Node

  type ThisElem = Elem

  def thisElem: ThisElem = this

  @throws(classOf[java.io.ObjectStreamException])
  private[resolved] def writeReplace(): Any = new Elem.ElemSerializationProxy(resolvedName, resolvedAttributes, children)

  override def collectChildNodeIndexes(pathEntries: Set[Path.Entry]): Map[Path.Entry, Int] = {
    filterChildElemsWithPathEntriesAndNodeIndexes(pathEntries).map(triple => (triple._2, triple._3)).toMap
  }

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(resolvedName, resolvedAttributes, newChildren)
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

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[Text] = children collect { case t: Text => t }

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

            newChildrenBuffer += f(Text(combinedText))

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
    coalesceAllAdjacentTextAndPostprocess(t => Text(XmlStringUtils.normalizeString(t.text)))
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
    transformAllText(t => Text(t.normalizedText))
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

final case class Text(text: String) extends Node with Nodes.Text {
  require(text ne null) // scalastyle:off null

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

object Node extends ElemCreationApi {

  type NodeType = Node

  type ElemType = Elem

  /**
   * Converts any element or text `Nodes.Node` to a "resolved" `Node`. For other kinds of nodes, an exception is thrown.
   * All descendant-or-self elements must implement `ClarkElemNodeApi`, or else an exception is thrown.
   *
   * Note that entity references, comments, processing instructions and top-level documents are lost.
   * All that remains are elements (without qualified names) and text nodes.
   * Losing the qualified names means that prefixes are lost. Losing the prefixes not only affects serialization of
   * the `Node` to an XML string, but also affects attribute values and text nodes in which those prefixes are used.
   *
   * Note that if there are any unresolved entities in the yaidom `Node`, those entity references are silently ignored!
   * This is definitely something to keep in mind!
   */
  def apply(n: Nodes.Node): Node = n match {
    case e: Nodes.Elem with ClarkElemNodeApi => Elem(e)
    case e: Nodes.Elem                       => sys.error(s"Not an element that implements ClarkElemNodeApi")
    case t: Nodes.Text                       => Text(t)
    case n                                   => sys.error(s"Not an element or text node: $n")
  }

  def elem(ename: EName, children: immutable.IndexedSeq[Node]): Elem = {
    elem(ename, Map[EName, String](), children)
  }

  def elem(ename: EName, attributes: Map[EName, String], children: immutable.IndexedSeq[Node]): Elem = {
    Elem(ename, attributes, children)
  }

  def elem(ename: EName, attributes: immutable.IndexedSeq[(EName, String)], children: immutable.IndexedSeq[Node]): Elem = {
    elem(ename, attributes.toMap, children)
  }

  def text(textValue: String): Text = Text(textValue)

  def textElem(ename: EName, txt: String): Elem = {
    textElem(ename, Map[EName, String](), txt)
  }

  def textElem(ename: EName, attributes: Map[EName, String], txt: String): Elem = {
    Elem(ename, attributes, Vector(text(txt)))
  }

  def textElem(ename: EName, attributes: immutable.IndexedSeq[(EName, String)], txt: String): Elem = {
    textElem(ename, attributes.toMap, txt)
  }

  def emptyElem(ename: EName): Elem = {
    emptyElem(ename, Map[EName, String]())
  }

  def emptyElem(ename: EName, attributes: Map[EName, String]): Elem = {
    Elem(ename, attributes, Vector())
  }

  def emptyElem(ename: EName, attributes: immutable.IndexedSeq[(EName, String)]): Elem = {
    emptyElem(ename, attributes.toMap)
  }
}

object Elem {

  private[resolved] final class ElemSerializationProxy(
    val resolvedName:       EName,
    val resolvedAttributes: Map[EName, String],
    val children:           immutable.IndexedSeq[Node]) extends Serializable {

    @throws(classOf[java.io.ObjectStreamException])
    def readResolve(): Any = new Elem(resolvedName, resolvedAttributes, children)
  }

  /**
   * Converts any `Nodes.Elem with ClarkElemNodeApi` element to a "resolved" `Elem`.
   * All descendant-or-self (`Nodes.Elem`) elements must implement `ClarkElemNodeApi`, or else an exception is thrown.
   */
  def apply(e: Nodes.Elem with ClarkElemNodeApi): Elem = {
    val children = e.children collect {
      case childElm: Nodes.Elem with ClarkElemNodeApi => childElm
      case childElm: Nodes.Elem                       => sys.error(s"Not an element that implements ClarkElemNodeApi")
      case childText: Nodes.Text                      => childText
    }
    // Recursion, with Node.apply and Elem.apply being mutually dependent
    val resolvedChildren = children map { node => Node(node) }

    Elem(e.resolvedName, e.resolvedAttributes.toMap, resolvedChildren)
  }
}

object Text {

  def apply(t: Nodes.Text): Text = Text(t.text)
}
