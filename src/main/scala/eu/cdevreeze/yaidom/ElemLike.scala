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
 * Supertrait for Elems and other element-like objects.
 *
 * @author Chris de Vreeze
 */
trait ElemLike[E <: ElemLike[E]] { self: E =>

  /** Resolved name of the element, as ExpandedName */
  def resolvedName: ExpandedName

  /** Returns the child elements */
  def childElems: immutable.Seq[E]

  /** Returns the child elements obeying the given predicate */
  final def childElems(p: E => Boolean): immutable.Seq[E] = childElems.filter(p)

  /** Returns the child elements with the given expanded name */
  final def childElems(expandedName: ExpandedName): immutable.Seq[E] = childElems(e => e.resolvedName == expandedName)

  /** Returns the child elements with the given expanded name, obeying the given predicate */
  final def childElems(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    childElems(e => (e.resolvedName == expandedName) && p(e))

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

  /**
   * Returns this element followed by the descendant elements.
   * The result is in the same order as corresponding StAX "start" events.
   * This method is very inefficient.
   */
  final def elemsOrSelf: immutable.Seq[E] = elemsOrSelfList.toIndexedSeq

  /**
   * Returns those of this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to <code>elemsOrSelf.filter(p)</code>.
   * The result is in the same order as corresponding StAX "start" events.
   * This method is not efficient.
   */
  final def elemsOrSelf(p: E => Boolean): immutable.Seq[E] = elemsOrSelfList(p).toIndexedSeq

  /**
   * Returns those of this element and its descendant elements that have the given expanded name.
   * The result is in the same order as corresponding StAX "start" events.
   * This method is not efficient.
   */
  final def elemsOrSelf(expandedName: ExpandedName): immutable.Seq[E] = elemsOrSelf(e => e.resolvedName == expandedName)

  /**
   * Returns those of this element and its descendant elements that have the given expanded name
   * and obey the given predicate. The result is in the same order as corresponding StAX "start" events.
   * This method is not efficient.
   */
  final def elemsOrSelf(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    elemsOrSelf(e => e.resolvedName == expandedName && p(e))

  /** Returns the descendant elements (not including this element). Same as <code>elemsOrSelf.drop(1)</code>. */
  final def elems: immutable.Seq[E] = {
    val resultWithSelf = elemsOrSelf
    require(resultWithSelf.head == self)
    resultWithSelf.drop(1)
  }

  /** Returns the descendant elements obeying the given predicate, that is, elems.filter(p). Not efficient. */
  final def elems(p: E => Boolean): immutable.Seq[E] = {
    val resultWithSelf = elemsOrSelf(p)

    if (p(self)) {
      require(resultWithSelf.head == self)
      resultWithSelf.drop(1)
    } else {
      resultWithSelf
    }
  }

  /** Returns the descendant elements with the given expanded name. Not efficient. */
  final def elems(expandedName: ExpandedName): immutable.Seq[E] = elems(e => e.resolvedName == expandedName)

  /** Returns the descendant elements with the given expanded name, obeying the given predicate. Not efficient. */
  final def elems(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    elems(e => (e.resolvedName == expandedName) && p(e))

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def firstElems(p: E => Boolean): immutable.Seq[E] = {
    val resultWithSelf = firstElemsOrSelfList(p)

    if (p(self)) {
      require(resultWithSelf.head == self)
      resultWithSelf.drop(1).toIndexedSeq
    } else {
      resultWithSelf.toIndexedSeq
    }
  }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def firstElems(expandedName: ExpandedName): immutable.Seq[E] = firstElems(e => e.resolvedName == expandedName)

  /**
   * Returns the descendant elements with the given expanded name, obeying the given predicate, that have no ancestor
   * with the same name obeying the same predicate
   */
  final def firstElems(expandedName: ExpandedName, p: E => Boolean): immutable.Seq[E] =
    firstElems(e => (e.resolvedName == expandedName) && p(e))

  /** Returns the first found descendant element obeying the given predicate, if any, wrapped in an Option. */
  final def firstElemOption(p: E => Boolean): Option[E] = {
    self.childElems.view.flatMap(ch => ch.firstElemOrSelfOption(p)).headOption
  }

  /** Returns the first found descendant element with the given expanded name, if any, wrapped in an Option */
  final def firstElemOption(expandedName: ExpandedName): Option[E] = firstElemOption(e => e.resolvedName == expandedName)

  /** Returns the first found descendant element with the given expanded name, obeying the given predicate, if any, wrapped in an Option */
  final def firstElemOption(expandedName: ExpandedName, p: E => Boolean): Option[E] =
    firstElemOption(e => (e.resolvedName == expandedName) && p(e))

  /** Finds the parent element, if any, searching in the tree with the given root element. Typically rather expensive. */
  final def findParentInTree(root: E): Option[E] = {
    root.firstElemOrSelfOption(e => e.childElems.exists(ch => ch == self))
  }

  /** Computes an index on the given function taking an element, for example a function returning a UUID. Very inefficient. */
  final def getIndex[K](f: E => K): Map[K, immutable.Seq[E]] = (elemsOrSelf).groupBy(f)

  /** Computes an index to parent elements, on the given function applied to the child elements. Very inefficient. */
  final def getIndexToParent[K](f: E => K): Map[K, immutable.Seq[E]] = {
    val parentChildPairs = (elemsOrSelf).flatMap(e => e.childElems.map(ch => (e -> ch)))
    parentChildPairs.groupBy(pair => f(pair._2)).mapValues(pairs => pairs.map(pair => pair._1))
  }

  /**
   * Returns a List of this element followed by the descendant elements.
   * The result is in the same order as corresponding StAX "start" events.
   * This method is very inefficient.
   */
  private final def elemsOrSelfList: List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    self :: self.childElems.toList.flatMap(ch => ch.elemsOrSelfList)
  }

  /**
   * Returns a List of those of this element and its descendant elements that obey the given predicate.
   * That is, the result is equivalent to <code>elemsOrSelfList.filter(p)</code>.
   * The result is in the same order as corresponding StAX "start" events.
   * This method is not efficient.
   */
  private final def elemsOrSelfList(p: E => Boolean): List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    val resultWithoutSelf = self.childElems.toList.flatMap(ch => ch.elemsOrSelfList(p))
    if (p(self)) self :: resultWithoutSelf else resultWithoutSelf
  }

  /**
   * Returns a List of those of this element and its descendant elements that obey the given predicate,
   * such that no ancestor obeys the predicate. The result is in the same order as corresponding StAX "start" events.
   * This method is not efficient.
   */
  private final def firstElemsOrSelfList(p: E => Boolean): List[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) List(self) else self.childElems.toList.flatMap(ch => ch.firstElemsOrSelfList(p))
  }

  /** Returns the first found descendant element or self obeying the given predicate, if any, wrapped in an Option. */
  private final def firstElemOrSelfOption(p: E => Boolean): Option[E] = {
    // Not tail-recursive, but the depth should typically be limited
    if (p(self)) Some(self) else self.childElems.view.flatMap(ch => ch.firstElemOrSelfOption(p)).headOption
  }
}
