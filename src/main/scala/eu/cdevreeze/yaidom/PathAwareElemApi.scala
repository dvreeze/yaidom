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
 * API for elements as containers of elements, each having a name and possible attributes, as well as an "element path" from
 * the root element. See [[eu.cdevreeze.yaidom.PathAwareElemLike]].
 *
 * This purely abstract query API trait leaves the implementation completely open. For example, an implementation backed by
 * an XML database would not use the ``PathAwareElemLike`` implementation, for reasons of efficiency.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait PathAwareElemApi[E <: PathAwareElemApi[E]] extends ElemApi[E] { self: E =>

  /**
   * Returns the equivalent of `findWithElemPath(ElemPath(immutable.IndexedSeq(entry)))`, but it should be very efficient.
   *
   * Indeed, it is function `findWithElemPath` that is defined in terms of this function, `findWithElemPathEntry`, and not
   * the other way around.
   */
  def findWithElemPathEntry(entry: ElemPath.Entry): Option[E]

  /**
   * Returns all child elements with their `ElemPath` entries, in the correct order. This method should be very efficient.
   *
   * The implementation must be such that the following holds: `(findAllChildElemsWithPathEntries map (_._1)) == findAllChildElems`
   */
  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, ElemPath.Entry)]

  /** Returns `findAllChildElemsWithPathEntries map { case (e, pe) => ElemPath.from(pe) }` */
  def findAllChildElemPaths: immutable.IndexedSeq[ElemPath]

  /** Returns the paths of child elements obeying the given predicate */
  def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath]

  /** Returns the path of the first found child element obeying the given predicate, if any, wrapped in an `Option` */
  def findChildElemPath(p: E => Boolean): Option[ElemPath]

  /** Returns the path of the single child element obeying the given predicate, and throws an exception otherwise */
  def getChildElemPath(p: E => Boolean): ElemPath

  /** Returns the path of this element followed by the paths of all descendant elements (that is, the descendant-or-self elements) */
  def findAllElemOrSelfPaths: immutable.IndexedSeq[ElemPath]

  /**
   * Returns the paths of descendant-or-self elements that obey the given predicate.
   * That is, the result is equivalent to the paths of `findAllElemsOrSelf filter p`.
   */
  def filterElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath]

  /** Returns the paths of all descendant elements (not including this element). Equivalent to `findAllElemOrSelfPaths.drop(1)` */
  def findAllElemPaths: immutable.IndexedSeq[ElemPath]

  /** Returns the paths of descendant elements obeying the given predicate, that is, the paths of `findAllElems filter p` */
  def filterElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath]

  /**
   * Returns the paths of the descendant-or-self elements that obey the given predicate, such that no ancestor obeys the predicate.
   */
  def findTopmostElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath]

  /** Returns the paths of the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  def findTopmostElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath]

  /** Returns the path of the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option` */
  def findElemOrSelfPath(p: E => Boolean): Option[ElemPath]

  /** Returns the path of the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option` */
  def findElemPath(p: E => Boolean): Option[ElemPath]

  /**
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   */
  def findWithElemPath(path: ElemPath): Option[E]

  /** Returns (the equivalent of) `findWithElemPath(path).get` */
  def getWithElemPath(path: ElemPath): E

  /**
   * Returns the `ElemPath` entries of all child elements, in the correct order.
   * Equivalent to `findAllChildElemsWithPathEntries map { _._2 }`.
   */
  def findAllChildElemPathEntries: immutable.IndexedSeq[ElemPath.Entry]
}
