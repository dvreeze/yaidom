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
 * API for elements as containers of elements, each having a name and possible attributes.
 * See [[eu.cdevreeze.yaidom.ElemLike]].
 *
 * This purely abstract query API trait leaves the implementation completely open. For example, an implementation backed by
 * an XML database would not use the ``ElemLike`` implementation, for reasons of efficiency.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ElemApi[E <: ElemApi[E]] extends ParentElemApi[E] { self: E =>

  /** Resolved name of the element, as `EName` */
  def resolvedName: EName

  /**
   * The attributes as a mapping from `EName`s (instead of `QName`s) to values.
   *
   * The implementation must ensure that `resolvedAttributes.toMap.size == resolvedAttributes.size`.
   */
  def resolvedAttributes: immutable.Iterable[(EName, String)]

  /** The local name (or local part). Convenience method. */
  def localName: String

  /** Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option` */
  def attributeOption(expandedName: EName): Option[String]

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  def attribute(expandedName: EName): String

  /**
   * Returns the first found attribute value of an attribute with the given local name, if any, wrapped in an `Option`.
   * Because of differing namespaces, it is possible that more than one such attribute exists, although this is not often the case.
   */
  def findAttribute(localName: String): Option[String]

  /** Shorthand for `attributeOption(expandedName)` */
  def \@(expandedName: EName): Option[String]

  /** Shorthand for `findAttribute(localName)` */
  def \@(localName: String): Option[String]

  /** Returns the child elements with the given expanded name */
  def filterChildElems(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `filterChildElems(expandedName)`. */
  def \(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `filterChildElems { _.localName == localName }`. */
  def \(localName: String): immutable.IndexedSeq[E]

  /** Returns the first found child element with the given expanded name, if any, wrapped in an `Option` */
  def findChildElem(expandedName: EName): Option[E]

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  def getChildElem(expandedName: EName): E

  /** Returns the descendant-or-self elements that have the given expanded name */
  def filterElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `filterElemsOrSelf(expandedName)`. */
  def \\(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `filterElemsOrSelf { _.localName == localName }`. */
  def \\(localName: String): immutable.IndexedSeq[E]

  /** Returns the descendant elements with the given expanded name */
  def filterElems(expandedName: EName): immutable.IndexedSeq[E]

  /** Returns the descendant-or-self elements with the given expanded name that have no ancestor with the same name */
  def findTopmostElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `findTopmostElemsOrSelf(expandedName)`. */
  def \\!(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `findTopmostElemsOrSelf { _.localName == localName }`. */
  def \\!(localName: String): immutable.IndexedSeq[E]

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  def findTopmostElems(expandedName: EName): immutable.IndexedSeq[E]

  /** Returns the first found (topmost) descendant-or-self element with the given expanded name, if any, wrapped in an `Option` */
  def findElemOrSelf(expandedName: EName): Option[E]

  /** Returns the first found (topmost) descendant element with the given expanded name, if any, wrapped in an `Option` */
  def findElem(expandedName: EName): Option[E]
}
