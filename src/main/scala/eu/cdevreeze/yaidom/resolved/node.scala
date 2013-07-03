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
 * <li>QNames in text or attribute values depend on in-scope namespaces for resolution</li>
 * <li>Differences in "ignorable whitespace", meant only for pretty-printing</li>
 * <li>Text that is possibly divided over several adjacent text nodes (possibly including CDATA text nodes), but should be "coalesced"</li>
 * <li>Text that is only equal after normalizing</li>
 * </ul>
 * Note that a validating parser knows the content model, so knows precisely which whitespace is "ignorable", for example, but once the parsed
 * XML is turned into untyped yaidom nodes, this information is lost. (Of course in principle PSVI data could be added to `Elem`s,
 * indexed by "element paths", but that is beyond the scope of yaidom.)
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
sealed trait Node extends Immutable

/**
 * Element as abstract data type. It contains only expanded names, not qualified names. This reminds of James Clark notation
 * for XML trees and expanded names, where qualified names are absent.
 *
 * Namespace declarations (and undeclarations) are not considered attributes in this API.
 */
final case class Elem(
  override val resolvedName: EName,
  override val resolvedAttributes: Map[EName, String],
  override val children: immutable.IndexedSeq[Node]) extends Node with UpdatableElemLike[Node, Elem] with TransformableElemLike[Node, Elem] with HasText { self =>

  require(resolvedName ne null)
  require(resolvedAttributes ne null)
  require(children ne null)

  @throws(classOf[java.io.ObjectStreamException])
  private[resolved] def writeReplace(): Any = new Elem.ElemSerializationProxy(resolvedName, resolvedAttributes, children)

  /** Cache for speeding up child element lookups by element path */
  private val childIndexesByPathEntries: Map[ElemPath.Entry, Int] = {
    // This implementation is O(n), where n is the number of children, and uses mutable collections for speed

    val elementNameCounts = mutable.Map[EName, Int]()
    val acc = mutable.ArrayBuffer[(ElemPath.Entry, Int)]()

    for ((node, idx) <- self.children.zipWithIndex) {
      node match {
        case elm: Elem =>
          val ename = elm.resolvedName
          val countForName = elementNameCounts.getOrElse(ename, 0)
          val entry = ElemPath.Entry(ename, countForName)
          elementNameCounts.update(ename, countForName + 1)
          acc += ((entry, idx))
        case _ => ()
      }
    }

    val result = acc.toMap
    result
  }

  /** Returns the element children */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  /**
   * Returns all child elements with their `ElemPath` entries, in the correct order.
   *
   * The implementation must be such that the following holds: `(findAllChildElemsWithPathEntries map (_._1)) == findAllChildElems`
   */
  override def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(Elem, ElemPath.Entry)] = {
    val childElms = findAllChildElems
    val entries = childIndexesByPathEntries.toSeq.sortBy(_._2).map(_._1)
    assert(childElms.size == entries.size)
    childElms.zip(entries)
  }

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(resolvedName, resolvedAttributes, newChildren)
  }

  override def childNodeIndex(childPathEntry: ElemPath.Entry): Int = {
    childIndexesByPathEntries.getOrElse(childPathEntry, -1)
  }

  override def findWithElemPathEntry(entry: ElemPath.Entry): Option[Elem] = {
    val idx = childNodeIndex(entry)
    if (idx < 0) None else Some(children(idx).asInstanceOf[Elem])
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

    val doStripWhitespace = (children forall (n => isWhitespaceText(n) || isElem(n))) && (!findAllChildElems.isEmpty)

    // Recursive, but not tail-recursive

    val newChildren = {
      val remainder = if (doStripWhitespace) findAllChildElems else children

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

  private[resolved] final class ElemSerializationProxy(
    val resolvedName: EName,
    val resolvedAttributes: Map[EName, String],
    val children: immutable.IndexedSeq[Node]) extends Serializable {

    @throws(classOf[java.io.ObjectStreamException])
    def readResolve(): Any = new Elem(resolvedName, resolvedAttributes, children)
  }

  def apply(e: eu.cdevreeze.yaidom.Elem): Elem = {
    val children = e.children collect {
      case childElm: eu.cdevreeze.yaidom.Elem => childElm
      case childText: eu.cdevreeze.yaidom.Text => childText
    }
    val resolvedChildren = children map { node => Node(node) }

    Elem(e.resolvedName, e.resolvedAttributes.toMap, resolvedChildren)
  }
}

object Text {

  def apply(t: eu.cdevreeze.yaidom.Text): Text = Text(t.text)
}
