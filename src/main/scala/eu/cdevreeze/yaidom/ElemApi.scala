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
 * This is the <em>best known</em> part of the yaidom <em>uniform query API</em>. It is a sub-trait of trait
 * [[eu.cdevreeze.yaidom.ParentElemApi]]. Many DOM-like element implementations in yaidom mix in this trait (indirectly,
 * because some implementing sub-trait is mixed in), thus sharing this query API.
 *
 * '''This trait typically does not show up in application code using yaidom, yet its (uniform) API does. Hence, it makes sense
 * to read the documentation of this trait, knowing that the API is offered by multiple element implementations.'''
 *
 * This trait is purely <em>abstract</em>. The most common implementation of this trait is [[eu.cdevreeze.yaidom.ElemLike]].
 * That trait only knows about elements (and not about other nodes), and only knows the following about elements:
 * <ul>
 * <li>elements can <em>have child elements</em> (as promised by the trait's super-trait)</li>
 * <li>elements have a so-called <em>"resolved name"</em>, which is an [[eu.cdevreeze.yaidom.EName]]</li>
 * <li>elements have zero or more <em>"resolved attributes"</em>, mapping attribute names (as ``EName``s) to attribute values</li>
 * </ul>
 * Using this minimal knowledge alone, that trait offers methods to query for <em>descendant</em> elements,
 * <em>descendant-or-self</em> methods, or sub-collections thereof. Element sub-collections can be queried by passing a
 * predicate (as offered by the super-trait), or simply by passing an element ``EName``.
 *
 * It is this minimal knowledge that makes this API uniform. On the one hand, that minimal knowledge is enough knowledge for
 * providing a rather rich ``ElemApi`` query API, and on the other hand, that minimal knowledge is so fundamental to DOM-like elements
 * that most yaidom DOM-like element implementations indeed offer this API.
 *
 * This query API leverages the Scala Collections API. Query results can be manipulated using the Collections API, and the
 * query API implementation (in ``ElemLike``) uses the Collections API internally.
 *
 * ==ElemApi examples==
 *
 * It is easy to show that this small query API is already very useful. Consider the following example XML:
 * {{{
 * <book:Bookstore xmlns:book="http://bookstore/book" xmlns:auth="http://bookstore/author">
 *   <book:Book ISBN="978-0321356680" Price="35" Edition="2">
 *     <book:Title>Effective Java (2nd Edition)</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Joshua</auth:First_Name>
 *         <auth:Last_Name>Bloch</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 *   <book:Book ISBN="978-0981531649" Price="35" Edition="2">
 *     <book:Title>Programming in Scala: A Comprehensive Step-by-Step Guide, 2nd Edition</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Martin</auth:First_Name>
 *         <auth:Last_Name>Odersky</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Lex</auth:First_Name>
 *         <auth:Last_Name>Spoon</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Bill</auth:First_Name>
 *         <auth:Last_Name>Venners</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 * </book:Bookstore>
 * }}}
 *
 * Suppose this XML has been parsed into [[eu.cdevreeze.yaidom.Elem]] instance ``bookstoreElem``. Then we can perform the
 * following queries:
 * {{{
 * val bookstoreNamespace = "http://bookstore/book"
 * val authorNamespace = "http://bookstore/author"
 * require(bookstoreElem.resolvedName == EName(bookstoreNamespace, "Bookstore"))
 *
 * val cheapBookElems =
 *   for {
 *     bookElem <- bookstoreElem \ EName(bookstoreNamespace, "Book")
 *     price <- bookElem \@ EName("Price")
 *     if price.toInt < 90
 *   } yield bookElem
 *
 * val cheapBookAuthors = {
 *   val result =
 *     for {
 *       cheapBookElem <- cheapBookElems
 *       authorElem <- cheapBookElem \\ EName(authorNamespace, "Author")
 *     } yield {
 *       val firstName = authorElem \ EName(authorNamespace, "First_Name") map (_.text) mkString ""
 *       val lastName = authorElem \ EName(authorNamespace, "Last_Name") map (_.text) mkString ""
 *       (firstName + " " + lastName).trim
 *     }
 *   result.toSet
 * }
 * }}}
 *
 * Using more ``ParentElemApi`` query methods, we could instead have written:
 * {{{
 * val cheapBookElems =
 *   for {
 *     bookElem <- bookstoreElem \ (e => e.resolvedName == EName(bookstoreNamespace, "Book"))
 *     price <- bookElem \@ EName("Price")
 *     if price.toInt < 90
 *   } yield bookElem
 *
 * val cheapBookAuthors = {
 *   val result =
 *     for {
 *       cheapBookElem <- cheapBookElems
 *       authorElem <- cheapBookElem \\ (e => e.resolvedName == EName(authorNamespace, "Author"))
 *     } yield {
 *       val firstName =
 *         authorElem \ (e => e.resolvedName == EName(authorNamespace, "First_Name")) map (_.text) mkString ""
 *       val lastName =
 *         authorElem \ (e => e.resolvedName == EName(authorNamespace, "Last_Name")) map (_.text) mkString ""
 *       (firstName + " " + lastName).trim
 *     }
 *   result.toSet
 * }
 * }}}
 *
 * By replacing operator notation, we get the following equivalent queries:
 * {{{
 * val cheapBookElems =
 *   for {
 *     bookElem <- bookstoreElem filterChildElems (e => e.resolvedName == EName(bookstoreNamespace, "Book"))
 *     price <- bookElem.attributeOption(EName("Price"))
 *     if price.toInt < 90
 *   } yield bookElem
 *
 * val cheapBookAuthors = {
 *   val result =
 *     for {
 *       cheapBookElem <- cheapBookElems
 *       authorElem <- cheapBookElem filterElemsOrSelf (e => e.resolvedName == EName(authorNamespace, "Author"))
 *     } yield {
 *       val firstName =
 *         authorElem filterChildElems (e => e.resolvedName == EName(authorNamespace, "First_Name")) map (_.text) mkString ""
 *       val lastName =
 *         authorElem filterChildElems (e => e.resolvedName == EName(authorNamespace, "Last_Name")) map (_.text) mkString ""
 *       (firstName + " " + lastName).trim
 *     }
 *   result.toSet
 * }
 * }}}
 *
 * The queries above only use the following knowledge about the DOM-like elements: they offer the ``ElemApi`` and ``HasText``
 * APIs. As a consequence, the exact same queries work for other DOM-like element implementations as well. That is, ``bookstoreElem``
 * could instead have been of type [[eu.cdevreeze.yaidom.indexed.Elem]], [[eu.cdevreeze.yaidom.resolved.Elem]],
 * [[eu.cdevreeze.yaidom.dom.DomElem]] or [[eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem]]. Hence the ``ElemApi`` trait indeed
 * offers a <em>uniform</em> element query API.
 *
 * ==ElemApi more formally==
 *
 * From a formal point of view, ``ElemApi`` offers little of interest. After all, given super-trait ``ParentElemApi``, as well
 * as methods ``resolvedName`` and ``resolvedAttributes``, the other methods are trivial to implement.
 *
 * For example, the semantics of method ``filterChildElems`` (taking an ``EName``) is trivially defined as follows:
 * {{{
 * elem.filterChildElems(ename) == elem.filterChildElems(e => e.resolvedName == ename)
 * }}}
 *
 * Other ``ParentElemApi`` methods taking a predicate also have a counterpart in ``ElemApi`` taking just an ``EName``, and
 * the latter ones are trivially defined in terms of the former ones, just like ``filterChildElems`` (taking an ``EName``) above.
 * After all, parent trait ``ParentElemApi`` is the foundation of the yaidom query API, yet sub-trait ``ElemApi`` makes it
 * much more useful in practice, by adding some knowledge about element names and attributes.
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
   *
   * Namespace declarations are not considered attributes in yaidom, so are not included in the result.
   */
  def resolvedAttributes: immutable.Iterable[(EName, String)]

  /** The local name (or local part). Convenience method. */
  def localName: String

  /**
   * Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option`.
   */
  def attributeOption(expandedName: EName): Option[String]

  /**
   * Returns the value of the attribute with the given expanded name, and throws an exception otherwise.
   */
  def attribute(expandedName: EName): String

  /**
   * Returns the first found attribute value of an attribute with the given local name, if any, wrapped in an `Option`.
   * Because of differing namespaces, it is possible that more than one such attribute exists, although this is not often the case.
   */
  def findAttributeByLocalName(localName: String): Option[String]

  /** Shorthand for `attributeOption(expandedName)` */
  def \@(expandedName: EName): Option[String]

  /** Returns the child elements with the given expanded name */
  def filterChildElems(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `filterChildElems(expandedName)`. */
  def \(expandedName: EName): immutable.IndexedSeq[E]

  /** Returns the first found child element with the given expanded name, if any, wrapped in an `Option` */
  def findChildElem(expandedName: EName): Option[E]

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  def getChildElem(expandedName: EName): E

  /** Returns the descendant-or-self elements that have the given expanded name */
  def filterElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `filterElemsOrSelf(expandedName)`. */
  def \\(expandedName: EName): immutable.IndexedSeq[E]

  /** Returns the descendant elements with the given expanded name */
  def filterElems(expandedName: EName): immutable.IndexedSeq[E]

  /** Returns the descendant-or-self elements with the given expanded name that have no ancestor with the same name */
  def findTopmostElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E]

  /** Shorthand for `findTopmostElemsOrSelf(expandedName)`. */
  def \\!(expandedName: EName): immutable.IndexedSeq[E]

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  def findTopmostElems(expandedName: EName): immutable.IndexedSeq[E]

  /** Returns the first found (topmost) descendant-or-self element with the given expanded name, if any, wrapped in an `Option` */
  def findElemOrSelf(expandedName: EName): Option[E]

  /** Returns the first found (topmost) descendant element with the given expanded name, if any, wrapped in an `Option` */
  def findElem(expandedName: EName): Option[E]
}
