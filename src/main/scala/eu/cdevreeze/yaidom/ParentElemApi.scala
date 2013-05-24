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
 * This purely abstract query API trait leaves the implementation (but not the semantics) completely open. For example,
 * an implementation backed by an XML database would not use the ``ParentElemLike`` implementation, for reasons of efficiency.
 *
 * The basic operations of this trait are ``\`` (alias for ``filterChildElems``), ``\\`` (alias for ``filterElemsOrSelf``)
 * and ``\\!`` (alias for ``findTopmostElemsOrSelf``). Their semantics must be as if they had been defined as follows:
 * {{{
 * def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E] =
 *   this.findAllChildElems.filter(p)
 *
 * def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
 *   Vector(this).filter(p) ++ (this.findAllChildElems flatMap (_.filterElemsOrSelf(p)))
 *
 * def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
 *   if (p(this)) Vector(this)
 *   else (this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p)))
 * }}}
 *
 * Similarly, we could have defined:
 * {{{
 * def filterElems(p: E => Boolean): immutable.IndexedSeq[E] =
 *   this.findAllChildElems flatMap (_.filterElemsOrSelf(p))
 *
 * def findTopmostElems(p: E => Boolean): immutable.IndexedSeq[E] =
 *   this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p))
 * }}}
 *
 * The following properties must hold (in the absence of side-effects), and can indeed be proven (given the documented
 * "definitions" of these operations):
 * {{{
 * // Filtering
 *
 * elem.filterElems(p) == elem.findAllElems.filter(p)
 *
 * elem.filterElemsOrSelf(p) == elem.findAllElemsOrSelf.filter(p)
 *
 * // Finding topmost
 *
 * elem.findTopmostElems(p) == {
 *   elem.filterElems(p) filter { e =>
 *     val hasNoMatchingAncestor = elem.filterElems(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 *
 * elem.findTopmostElemsOrSelf(p) == {
 *   elem.filterElemsOrSelf(p) filter { e =>
 *     val hasNoMatchingAncestor = elem.filterElemsOrSelf(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 *
 * (elem.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p))) == (elem.filterElems(p))
 *
 * (elem.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) == (elem.filterElemsOrSelf(p))
 * }}}
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ParentElemApi[E <: ParentElemApi[E]] { self: E =>

  /**
   * Returns '''all child elements''', in the correct order. Other operations can be defined in terms of this one.
   */
  def findAllChildElems: immutable.IndexedSeq[E]

  /**
   * Returns all descendant elements (not including this element). This method could be defined as `filterElems { e => true }`.
   * Equivalent to `findAllElemsOrSelf.drop(1)`.
   */
  def findAllElems: immutable.IndexedSeq[E]

  /**
   * Returns this element followed by all descendant elements (that is, the descendant-or-self elements).
   * This method could be defined as `filterElemsOrSelf { e => true }`.
   */
  def findAllElemsOrSelf: immutable.IndexedSeq[E]

  /**
   * '''Core method''' that returns the child elements obeying the given predicate. This method could be defined as:
   * {{{
   * def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E] =
   *   this.findAllChildElems.filter(p)
   * }}}
   */
  def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the descendant elements obeying the given predicate.
   * This method could be defined as:
   * {{{
   * this.findAllChildElems flatMap (_.filterElemsOrSelf(p))
   * }}}
   */
  def filterElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * '''Core method''' that returns the descendant-or-self elements obeying the given predicate. This method could be defined as:
   * {{{
   * def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
   *   Vector(this).filter(p) ++ (this.findAllChildElems flatMap (_.filterElemsOrSelf(p)))
   * }}}
   *
   * It can be proven that the result is equivalent to `findAllElemsOrSelf filter p`.
   */
  def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the first found child element obeying the given predicate, if any, wrapped in an `Option`.
   * This method could be defined as `filterChildElems(p).headOption`.
   */
  def findChildElem(p: E => Boolean): Option[E]

  /**
   * Returns the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option`.
   * This method could be defined as `filterElems(p).headOption`.
   */
  def findElem(p: E => Boolean): Option[E]

  /**
   * Returns the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option`.
   * This method could be defined as `filterElemsOrSelf(p).headOption`.
   */
  def findElemOrSelf(p: E => Boolean): Option[E]

  /**
   * Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate.
   * This method could be defined as:
   * {{{
   * this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p))
   * }}}
   */
  def findTopmostElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * '''Core method''' that returns the descendant-or-self elements obeying the given predicate, such that no ancestor obeys the predicate.
   * This method could be defined as:
   * {{{
   * def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
   *   if (p(this)) Vector(this)
   *   else (this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p)))
   * }}}
   */
  def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the single child element obeying the given predicate, and throws an exception otherwise.
   * This method could be defined as `findChildElem(p).get`.
   */
  def getChildElem(p: E => Boolean): E

  /** Shorthand for `filterChildElems(p)`. Use this shorthand only if the predicate is a short expression. */
  def \(p: E => Boolean): immutable.IndexedSeq[E]

  /** Shorthand for `filterElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  def \\(p: E => Boolean): immutable.IndexedSeq[E]

  /** Shorthand for `findTopmostElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  def \\!(p: E => Boolean): immutable.IndexedSeq[E]
}
