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

import scala.collection.immutable

/**
 * Supertrait for [[eu.cdevreeze.yaidom.Elem]] and other element-like classes, such as [[eu.cdevreeze.yaidom.xlink.Elem]].
 * Below, we refer to these element-like objects as elements.
 *
 * The only abstract methods are <code>resolvedName</code>, <code>resolvedAttributes</code> and <code>allChildElems</code>.
 * Based on these methods alone, this trait offers a rich API for querying elements and attributes.
 *
 * This trait offers public element retrieval methods to obtain:
 * <ul>
 * <li>child elements</li>
 * <li>descendant elements</li>
 * <li>descendant or self elements</li>
 * <li>first found descendant elements obeying a predicate, meaning that
 * they have no ancestors obeying that predicate</li>
 * </ul>
 * In the method names, "elems" stands for descendant elements, and "first elems" stands for first found descendant
 * elements as explained above.
 *
 * There are also attribute retrieval methods, and methods for indexing the element tree and finding subtrees.
 *
 * These element retrieval methods each have up to 3 variants (returning collections of elements):
 * <ol>
 * <li>A no argument variant, if applicable (typically with prefix "all" in the method name)</li>
 * <li>A single <code>E => Boolean</code> predicate argument variant (with suffix "where" in the method name)</li>
 * <li>An expanded name argument variant</li>
 * </ol>
 * The latter variant is implemented in terms of the single predicate argument variant.
 * Some methods also have variants that return a single element or an element Option.
 *
 * These element finder methods process and return elements in the following (depth-first) order:
 * <ol>
 * <li>Parents are processed before their children</li>
 * <li>Children are processed before the next sibling</li>
 * <li>The first child element is processed before the next child element, and so on</li>
 * </ol>
 * assuming that the no-arg <code>allChildElems</code> method returns the child elements in the correct order.
 * Hence, the methods taking a predicate invoke that predicate on the elements in a predictable order.
 * Per visited element, the predicate is invoked only once. These properties are especially important
 * if the predicate has side-effects, which typically should not be the case.
 *
 * The type parameter is the type of the element, which is itself an ElemLike.
 *
 * @author Chris de Vreeze
 */
trait ElemLike[E <: ElemLike[E]] { self: E =>

  /** Resolved name of the element, as ExpandedName */
  def resolvedName: ExpandedName

  /** The attributes as a Map from ExpandedNames (instead of QNames) to values */
  def resolvedAttributes: Map[ExpandedName, String]

  /** Returns all child elements, in the correct order */
  def allChildElems: immutable.Seq[E]

  /** Returns the value of the attribute with the given expanded name, if any, wrapped in an Option */
  final def attributeOption(expandedName: ExpandedName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  final def attribute(expandedName: ExpandedName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements obeying the given predicate */
  final def childElemsWhere(p: E => Boolean): immutable.Seq[E] = allChildElems filter p

  /** Returns the child elements with the given expanded name */
  final def childElems(expandedName: ExpandedName): immutable.Seq[E] = childElemsWhere { e => e.resolvedName == expandedName }

  /** Returns the single child element with the given expanded name, if any, wrapped in an Option */
  final def childElemOption(expandedName: ExpandedName): Option[E] = {
    val result = childElems(expandedName)
    require(result.size <= 1, "Expected at most 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.headOption
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  final def childElem(expandedName: ExpandedName): E = {
    val result = childElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns this element followed by all descendant elements */
  final def allElemsOrSelf: immutable.Seq[E] = allElemsOrSelfList.toIndexedSeq

  /**
   * Returns those elements among this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to <code>allElemsOrSelf filter p</code>.
   */
  final def elemsOrSelfWhere(p: E => Boolean): immutable.Seq[E] = elemsOrSelfListWhere(p).toIndexedSeq

  /** Returns those elements among this element and its descendant elements that have the given expanded name */
  final def elemsOrSelf(expandedName: ExpandedName): immutable.Seq[E] = elemsOrSelfWhere { e => e.resolvedName == expandedName }

  /** Returns all descendant elements (not including this element). Same as <code>allElemsOrSelf.drop(1)</code> */
  final def allElems: immutable.Seq[E] = allChildElems flatMap { ch => ch.allElemsOrSelf }

  /** Returns the descendant elements obeying the given predicate, that is, allElems filter p */
  final def elemsWhere(p: E => Boolean): immutable.Seq[E] = allChildElems flatMap { ch => ch elemsOrSelfWhere p }

  /** Returns the descendant elements with the given expanded name */
  final def elems(expandedName: ExpandedName): immutable.Seq[E] = elemsWhere { e => e.resolvedName == expandedName }

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def firstElemsWhere(p: E => Boolean): immutable.Seq[E] =
    allChildElems flatMap { ch => ch firstElemsOrSelfListWhere p toIndexedSeq }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def firstElems(expandedName: ExpandedName): immutable.Seq[E] = firstElemsWhere { e => e.resolvedName == expandedName }

  /** Returns the first found descendant element obeying the given predicate, if any, wrapped in an Option */
  final def firstElemOptionWhere(p: E => Boolean): Option[E] = {
    self.allChildElems.view flatMap { ch => ch firstElemOrSelfOptionWhere p } headOption
  }

  /** Returns the first found descendant element with the given expanded name, if any, wrapped in an Option */
  final def firstElemOption(expandedName: ExpandedName): Option[E] = firstElemOptionWhere { e => e.resolvedName == expandedName }

  /**
   * Finds the parent element, if any, searching in the tree with the given root element.
   * The implementation uses the equals method on the self type, and uses no index. Typically rather expensive.
   */
  final def findParentInTree(root: E): Option[E] = {
    root firstElemOrSelfOptionWhere { e => e.allChildElems exists { ch => ch == self } }
  }

  /** Computes an index on the given function taking an element, for example a function returning some unique Elem "identifier" */
  final def getIndex[K](f: E => K): Map[K, immutable.Seq[E]] = allElemsOrSelf groupBy f

  /** Computes an index to parent elements, on the given function applied to the child elements */
  final def getIndexToParent[K](f: E => K): Map[K, immutable.Seq[E]] = {
    val parentChildPairs = allElemsOrSelf flatMap { e => e.allChildElems map { ch => (e -> ch) } }
    parentChildPairs groupBy { pair => f(pair._2) } mapValues { pairs => pairs map { _._1 } } mapValues { _.distinct }
  }

  /** Finds the element with the given ElemPath (where this element is the root), if any, wrapped in an Option. */
  final def findWithElemPath(path: ElemPath): Option[E] = path.entries match {
    case Nil => Some(self)
    case _ =>
      val hd = path.entries.head
      val tl = path.entries.tail

      val relevantChildElms = self.childElems(hd.elementName)

      if (hd.index >= relevantChildElms.size) None else {
        val newRoot = relevantChildElms(hd.index)

        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRoot.findWithElemPath(path.skipEntry)
      }
  }

  /** Returns the ElemPath entries of all child elements, in the correct order */
  final def allChildElemPathEntries: immutable.Seq[ElemPath.Entry] = {
    val startAcc = immutable.IndexedSeq[ElemPath.Entry]()

    allChildElems.foldLeft(startAcc) { (acc, elm) =>
      val countForName = acc count { entry => entry.elementName == elm.resolvedName }
      val entry = ElemPath.Entry(elm.resolvedName, countForName)
      acc :+ entry
    }
  }

  /** Returns a List of this element followed by all descendant elements */
  private final def allElemsOrSelfList: List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    self :: {
      self.allChildElems.toList flatMap { ch => ch.allElemsOrSelfList }
    }
  }

  /**
   * Returns a List of those of this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to <code>allElemsOrSelfList filter p</code>.
   */
  private final def elemsOrSelfListWhere(p: E => Boolean): List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    val includesSelf = p(self)
    val resultWithoutSelf = self.allChildElems.toList flatMap { ch => ch elemsOrSelfListWhere p }
    if (includesSelf) self :: resultWithoutSelf else resultWithoutSelf
  }

  /**
   * Returns a List of those of this element and its descendant elements that obey the given predicate,
   * such that no ancestor obeys the predicate.
   */
  private final def firstElemsOrSelfListWhere(p: E => Boolean): List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) List(self) else self.allChildElems.toList flatMap { ch => ch firstElemsOrSelfListWhere p }
  }

  /** Returns the first found descendant element or self obeying the given predicate, if any, wrapped in an Option */
  private final def firstElemOrSelfOptionWhere(p: E => Boolean): Option[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) Some(self) else self.allChildElems.view flatMap { ch => ch firstElemOrSelfOptionWhere p } headOption
  }
}
