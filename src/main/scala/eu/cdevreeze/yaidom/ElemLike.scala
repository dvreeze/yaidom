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

import scala.collection.{ immutable, mutable }

/**
 * API and implementation trait for elements as containers of elements, each having a name and possible attributes.
 * This trait extends trait [[eu.cdevreeze.yaidom.ParentElemLike]], adding knowledge about names of elements and of attributes.
 *
 * '''Most users of the yaidom API do not use this trait directly, so may skip the documentation of this trait.'''
 *
 * Example usage (where the `text` method is not offered by the `ElemLike` API itself, but by trait `HasText` instead):
 * {{{
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val cheapBookElms =
 *   for {
 *     bookElm <- bookstoreElm \ "Book"
 *     price = bookElm.attribute(EName("Price"))
 *     if price.toInt < 90
 *   } yield bookElm
 *
 * val cheapBookAuthors = {
 *   val result =
 *     for {
 *       cheapBookElm <- cheapBookElms
 *       authorElm <- cheapBookElm \\ "Author"
 *     } yield {
 *       val firstNameElmOption = authorElm findChildElem { _.localName == "First_Name" }
 *       val lastNameElmOption = authorElm findChildElem { _.localName == "Last_Name" }
 *
 *       val firstName = firstNameElmOption.map(_.text).getOrElse("")
 *       val lastName = lastNameElmOption.map(_.text).getOrElse("")
 *       (firstName + " " + lastName).trim
 *     }
 *
 *   result.toSet
 * }
 * }}}
 *
 * Above, shorthand operator notation is used for querying child elements and descendant-or-self elements.
 * {{{
 * bookstoreElm \ "Book"
 * }}}
 * is equivalent to:
 * {{{
 * bookstoreElm filterChildElems { _.localName == "Book" }
 * }}}
 * Moreover:
 * {{{
 * cheapBookElm \\ "Author"
 * }}}
 * is equivalent to:
 * {{{
 * cheapBookElm filterElemsOrSelf { _.localName == "Author" }
 * }}}
 *
 * ==ElemLike methods==
 *
 * This trait adds the following abstract methods to the abstract methods required by its supertrait: `resolvedName` and `resolvedAttributes`.
 * Based on these abstract methods (and the supertrait), this trait offers a rich API for querying elements by (expanded) name, and for querying
 * attributes.
 *
 * This trait only knows about elements, not about nodes in general. Hence this trait has no knowledge about child nodes in
 * general. Hence the single type parameter, for the captured element type itself.
 *
 * The element query methods that need no knowledge about element names and attributes are implemented by supertrait
 * [[eu.cdevreeze.yaidom.ParentElemLike]].
 *
 * This trait adds the following groups of methods to the methods offered by the supertrait `ParentElemLike`:
 * <ul>
 * <li>Attribute query methods</li>
 * <li>Element query methods taking an `EName` or local name (which trivially correspond to method calls in the supertrait)</li>
 * <li>A method to get the local name of the element, without the namespace</li>
 * </ul>
 *
 * Obviously, this API does not offer much to the parent trait API. After all, the element query methods taking a local name or
 * expanded name are only syntactic sugar for specific calls to the corresponding `ParentElemLike` methods taking an element predicate.
 * Had we defined implicit conversions from local/expanded names to element predicates, most of this API would have been superfluous.
 * On the other hand, the current `ElemLike` API, which requires no implicits, is trivial to use.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ElemLike[E <: ElemLike[E]] extends ParentElemLike[E] { self: E =>

  /** Resolved name of the element, as `EName` */
  def resolvedName: EName

  /**
   * The attributes as a mapping from `EName`s (instead of `QName`s) to values.
   *
   * The implementation must ensure that `resolvedAttributes.toMap.size == resolvedAttributes.size`.
   */
  def resolvedAttributes: immutable.Iterable[(EName, String)]

  /** The local name (or local part). Convenience method. */
  final def localName: String = resolvedName.localPart

  /** Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option` */
  final def attributeOption(expandedName: EName): Option[String] = resolvedAttributes.toMap.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  final def attribute(expandedName: EName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /**
   * Returns the first found attribute value of an attribute with the given local name, if any, wrapped in an `Option`.
   * Because of differing namespaces, it is possible that more than one such attribute exists, although this is not often the case.
   */
  final def findAttribute(localName: String): Option[String] = {
    val matchingAttrs = resolvedAttributes filter { case (en, v) => en.localPart == localName }
    matchingAttrs.map(_._2).headOption
  }

  /** Shorthand for `attributeOption(expandedName)` */
  final def \@(expandedName: EName): Option[String] = attributeOption(expandedName)

  /** Shorthand for `findAttribute(localName)` */
  final def \@(localName: String): Option[String] = findAttribute(localName)

  /** Returns the child elements with the given expanded name */
  final def filterChildElems(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems { e => e.resolvedName == expandedName }

  /** Shorthand for `filterChildElems(expandedName)`. */
  final def \(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems(expandedName)

  /** Shorthand for `filterChildElems { _.localName == localName }`. */
  final def \(localName: String): immutable.IndexedSeq[E] = filterChildElems { _.localName == localName }

  /** Returns the first found child element with the given expanded name, if any, wrapped in an `Option` */
  final def findChildElem(expandedName: EName): Option[E] = {
    findChildElem { e => e.resolvedName == expandedName }
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  final def getChildElem(expandedName: EName): E = {
    val result = filterChildElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns the descendant-or-self elements that have the given expanded name */
  final def filterElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf { e => e.resolvedName == expandedName }

  /** Shorthand for `filterElemsOrSelf(expandedName)`. */
  final def \\(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf(expandedName)

  /** Shorthand for `filterElemsOrSelf { _.localName == localName }`. */
  final def \\(localName: String): immutable.IndexedSeq[E] = filterElemsOrSelf { _.localName == localName }

  /** Returns the descendant elements with the given expanded name */
  final def filterElems(expandedName: EName): immutable.IndexedSeq[E] = filterElems { e => e.resolvedName == expandedName }

  /** Returns the descendant-or-self elements with the given expanded name that have no ancestor with the same name */
  final def findTopmostElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElemsOrSelf { e => e.resolvedName == expandedName }

  /** Shorthand for `findTopmostElemsOrSelf(expandedName)`. */
  final def \\!(expandedName: EName): immutable.IndexedSeq[E] = findTopmostElemsOrSelf(expandedName)

  /** Shorthand for `findTopmostElemsOrSelf { _.localName == localName }`. */
  final def \\!(localName: String): immutable.IndexedSeq[E] = findTopmostElemsOrSelf { _.localName == localName }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def findTopmostElems(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElems { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant-or-self element with the given expanded name, if any, wrapped in an `Option` */
  final def findElemOrSelf(expandedName: EName): Option[E] =
    findElemOrSelf { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant element with the given expanded name, if any, wrapped in an `Option` */
  final def findElem(expandedName: EName): Option[E] =
    findElem { e => e.resolvedName == expandedName }
}
