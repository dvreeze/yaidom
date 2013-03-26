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
 * API for elements as containers of elements, as element nodes in a node tree. See [[eu.cdevreeze.yaidom.ParentElemLike]].
 *
 * This purely abstract query API trait leaves the implementation completely open. For example, an implementation backed by
 * an XML database would not use the ``ParentElemLike`` implementation, for reasons of efficiency.
 *
 * The following properties must hold (in the absence of side-effects):
 * {{{
 * // Filtering
 *
 * elem.filterChildElems(p) == elem.findAllChildElems.filter(p)
 *
 * elem.filterElems(p) == elem.findAllElems.filter(p)
 *
 * elem.filterElemsOrSelf(p) == elem.findAllElemsOrSelf.filter(p)
 *
 * // Finding
 *
 * elem.findChildElem(p) == elem.filterChildElems(p).headOption
 *
 * elem.findElem(p) == elem.filterElems(p).headOption
 *
 * elem.findElemOrSelf(p) == elem.filterElemsOrSelf(p).headOption
 *
 * // Finding topmost
 *
 * elem.findTopmostElems(p) == (elem.filterElems(p) filter (e1 => e1.filterElems(p) forall (e2 => !p(e2))))
 *
 * elem.findTopmostElemsOrSelf(p) == (elem.filterElemsOrSelf(p) filter (e1 => e1.filterElems(p) forall (e2 => !p(e2))))
 *
 * // Getting single result
 *
 * elem.getChildElem(p) == elem.findChildElem(p).get
 * }}}
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ParentElemApi[E <: ParentElemApi[E]] { self: E =>

  /**
   * Returns all child elements, in the correct order.
   */
  def findAllChildElems: immutable.IndexedSeq[E]

  /** Returns all descendant elements (not including this element). Equivalent to `findAllElemsOrSelf.drop(1)` */
  def findAllElems: immutable.IndexedSeq[E]

  /** Returns this element followed by all descendant elements (that is, the descendant-or-self elements) */
  def findAllElemsOrSelf: immutable.IndexedSeq[E]

  /** Returns the child elements obeying the given predicate */
  def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E]

  /** Returns the descendant elements obeying the given predicate, that is, `findAllElems filter p` */
  def filterElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the descendant-or-self elements that obey the given predicate.
   * That is, the result is equivalent to `findAllElemsOrSelf filter p`.
   */
  def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E]

  /** Returns the first found child element obeying the given predicate, if any, wrapped in an `Option` */
  def findChildElem(p: E => Boolean): Option[E]

  /** Returns the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option` */
  def findElem(p: E => Boolean): Option[E]

  /** Returns the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option` */
  def findElemOrSelf(p: E => Boolean): Option[E]

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  def findTopmostElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the descendant-or-self elements that obey the given predicate, such that no ancestor obeys the predicate.
   */
  def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E]

  /** Returns the single child element obeying the given predicate, and throws an exception otherwise */
  def getChildElem(p: E => Boolean): E

  /** Shorthand for `filterChildElems(p)`. Use this shorthand only if the predicate is a short expression. */
  def \(p: E => Boolean): immutable.IndexedSeq[E]

  /** Shorthand for `filterElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  def \\(p: E => Boolean): immutable.IndexedSeq[E]

  /** Shorthand for `findTopmostElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  def \\!(p: E => Boolean): immutable.IndexedSeq[E]
}
