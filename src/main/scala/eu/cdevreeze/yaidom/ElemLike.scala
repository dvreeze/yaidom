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
 * Supertrait for <code>Elem</code>s and other element-like objects, such as <code>xlink.Elem</code>s.
 * Below, we refer to these element-like objects as elements.
 *
 * The only abstract methods are <code>resolvedName</code>, <code>resolvedAttributes</code> and <code>childElems</code> (without arguments).
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
 * There are also attribute retrieval methods, and methods for indexing the element tree.
 *
 * These element retrieval methods each have up to 3 variants (returning collections of elements):
 * <ol>
 * <li>A no argument variant, if applicable</li>
 * <li>A single <code>E => Boolean</code> predicate argument variant</li>
 * <li>An expanded name argument variant</li>
 * The latter variant is implemented in terms of the single predicate argument variant.
 * Some methods also have variants that return a single element or an element Option.
 *
 * These element finder methods process and return elements in the following (depth-first) order:
 * <ol>
 * <li>Parents are processed before their children</li>
 * <li>Children are processed before the next sibling</li>
 * <li>The first child element is processed before the next child element, and so on</li>
 * </ol>
 * assuming that the no-arg <code>childElems</code> methods returns the child elements in the correct order.
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

  /** Returns the child elements, in the correct order */
  def childElems: immutable.Seq[E]

  /** Returns the value of the attribute with the given expanded name, if any, and None otherwise */
  final def attributeOption(expandedName: ExpandedName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  final def attribute(expandedName: ExpandedName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements obeying the given predicate */
  final def childElems(p: E => Boolean): immutable.Seq[E] = childElems filter p

  /** Returns the child elements with the given expanded name */
  final def childElems(expandedName: ExpandedName): immutable.Seq[E] = childElems { e => e.resolvedName == expandedName }

  /** Returns the single child element with the given expanded name, if any, and None otherwise */
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

  /** Returns this element followed by the descendant elements */
  final def elemsOrSelf: immutable.Seq[E] = elemsOrSelfList.toIndexedSeq

  /**
   * Returns those elements among this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to <code>elemsOrSelf filter p</code>.
   */
  final def elemsOrSelf(p: E => Boolean): immutable.Seq[E] = elemsOrSelfList(p).toIndexedSeq

  /** Returns those of this element and its descendant elements that have the given expanded name */
  final def elemsOrSelf(expandedName: ExpandedName): immutable.Seq[E] = elemsOrSelf { e => e.resolvedName == expandedName }

  /** Returns the descendant elements (not including this element). Same as <code>elemsOrSelf.drop(1)</code> */
  final def elems: immutable.Seq[E] = childElems flatMap { ch => ch.elemsOrSelf }

  /** Returns the descendant elements obeying the given predicate, that is, elems filter p */
  final def elems(p: E => Boolean): immutable.Seq[E] = childElems flatMap { ch => ch elemsOrSelf p }

  /** Returns the descendant elements with the given expanded name */
  final def elems(expandedName: ExpandedName): immutable.Seq[E] = elems { e => e.resolvedName == expandedName }

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def firstElems(p: E => Boolean): immutable.Seq[E] =
    childElems flatMap { ch => ch firstElemsOrSelfList p toIndexedSeq }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def firstElems(expandedName: ExpandedName): immutable.Seq[E] = firstElems { e => e.resolvedName == expandedName }

  /** Returns the first found descendant element obeying the given predicate, if any, wrapped in an Option */
  final def firstElemOption(p: E => Boolean): Option[E] = {
    self.childElems.view flatMap { ch => ch firstElemOrSelfOption p } headOption
  }

  /** Returns the first found descendant element with the given expanded name, if any, wrapped in an Option */
  final def firstElemOption(expandedName: ExpandedName): Option[E] = firstElemOption { e => e.resolvedName == expandedName }

  /** Finds the parent element, if any, searching in the tree with the given root element. Typically rather expensive. */
  final def findParentInTree(root: E): Option[E] = {
    root firstElemOrSelfOption { e => e.childElems exists { ch => ch == self } }
  }

  /** Computes an index on the given function taking an element, for example a function returning a UUID */
  final def getIndex[K](f: E => K): Map[K, immutable.Seq[E]] = elemsOrSelf groupBy f

  /** Computes an index to parent elements, on the given function applied to the child elements */
  final def getIndexToParent[K](f: E => K): Map[K, immutable.Seq[E]] = {
    val parentChildPairs = elemsOrSelf flatMap { e => e.childElems map { ch => (e -> ch) } }
    parentChildPairs groupBy { pair => f(pair._2) } mapValues { pairs => pairs map { _._1 } } mapValues { _.distinct }
  }

  /** Returns a List of this element followed by the descendant elements */
  private final def elemsOrSelfList: List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    self :: {
      self.childElems.toList flatMap { ch => ch.elemsOrSelfList }
    }
  }

  /**
   * Returns a List of those of this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to <code>elemsOrSelfList filter p</code>.
   */
  private final def elemsOrSelfList(p: E => Boolean): List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    val includesSelf = p(self)
    val resultWithoutSelf = self.childElems.toList flatMap { ch => ch elemsOrSelfList p }
    if (includesSelf) self :: resultWithoutSelf else resultWithoutSelf
  }

  /**
   * Returns a List of those of this element and its descendant elements that obey the given predicate,
   * such that no ancestor obeys the predicate.
   */
  private final def firstElemsOrSelfList(p: E => Boolean): List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) List(self) else self.childElems.toList flatMap { ch => ch firstElemsOrSelfList p }
  }

  /** Returns the first found descendant element or self obeying the given predicate, if any, wrapped in an Option */
  private final def firstElemOrSelfOption(p: E => Boolean): Option[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) Some(self) else self.childElems.view flatMap { ch => ch firstElemOrSelfOption p } headOption
  }
}
