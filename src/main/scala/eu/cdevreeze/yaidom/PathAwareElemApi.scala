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
 * This is the <em>ElemPath-aware</em> part of the yaidom <em>uniform query API</em>. It is a sub-trait of trait
 * [[eu.cdevreeze.yaidom.ElemApi]]. Only a few DOM-like element implementations in yaidom mix in this trait (indirectly,
 * because some implementing sub-trait is mixed in), thus sharing this query API.
 *
 * This trait is purely <em>abstract</em>. The most common implementation of this trait is [[eu.cdevreeze.yaidom.PathAwareElemLike]].
 * That trait only knows about elements (and not about other nodes), and only knows the following about elements:
 * <ul>
 * <li>elements can <em>have child elements</em> (as promised by ancestor trait ``ParentElemLike``)</li>
 * <li>elements have a so-called <em>"resolved name"</em> (as promised by parent trait ``ElemLike``)</li>
 * <li>elements have zero or more <em>"resolved attributes"</em> (as promised by parent trait ``ElemLike``)</li>
 * <li>elements have <em>element path entries</em> (as ``ElemPath.Entry`` objects) to their child elements,
 * and child elements can be found by their element path entries</li>
 * </ul>
 * Using this minimal knowledge alone, that trait not only offers the methods of its parent trait, but also:
 * <ul>
 * <li>methods mirroring the ``ParentElemLike`` query methods, but returning ``ElemPath`` objects instead of elements</li>
 * <li>a method to find an element given an ``ElemPath``</li>
 * </ul>
 * In other words, the ``PathAwareElemApi`` trait is quite a rich query API, considering the minimal knowledge it needs to
 * have about elements.
 *
 * This query API leverages the Scala Collections API. Query results can be manipulated using the Collections API, and the
 * query API implementation (in ``PathAwareElemLike``) uses the Collections API internally.
 *
 * ==PathAwareElemApi examples==
 *
 * To illustrate the use of this API, consider the following example XML:
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
 * following query, using only the ``ElemApi`` query API:
 * {{{
 * val bookstoreNamespace = "http://bookstore/book"
 * val authorNamespace = "http://bookstore/author"
 * require(bookstoreElem.resolvedName == EName(bookstoreNamespace, "Bookstore"))
 *
 * val scalaBookElems =
 *   for {
 *     bookElem <- bookstoreElem \\ EName(bookstoreNamespace, "Book")
 *     if (bookElem \ EName(bookstoreNamespace, "Title")).map(_.text).headOption.getOrElse("").contains("Scala")
 *   } yield bookElem
 * }}}
 *
 * This is a top-down approach for finding Scala books. A bottom-up approach, using the ``PathAwareElemApi`` query API,
 * could be coded as follows:
 * {{{
 * val scalaBookElems =
 *   for {
 *     titleElemPath <- bookstoreElem filterElemPaths (e => e.resolvedName == EName(bookstoreNamespace, "Title"))
 *     if bookstoreElem.getWithElemPath(titleElemPath).text.contains("Scala")
 *     bookElemPath <- titleElemPath.findAncestorPath(_.endsWithName(EName(bookstoreNamespace, "Book")))
 *   } yield bookstoreElem.getWithElemPath(bookElemPath)
 * }}}
 *
 * A few remarks are in order:
 * <ul>
 * <li>Be careful to invoke ``getWithElemPath`` (or ``findWithElemPath``) on the correct element</li>
 * <li>Invoking these methods in tight loops may harm performance</li>
 * <li>Note that element paths are not stable, when (functionally) updating elements</li>
 * <li>The "filtering" query methods specific to trait ``PathAwareElemApi`` take predicates on elements, and not on
 * paths or element-path-combinations. Not much would have been gained from that, because typical queries for paths contain
 * ``getWithElemPath`` calls anyway. After all, what else are element paths for?</li>
 * </ul>
 * In spite of these remarks, the methods specific to the ``PathAwareElemApi`` trait are a nice tool in the yaidom querying toolbox.
 *
 * ==PathAwareElemApi more formally==
 *
 * The ``PathAwareElemApi`` trait can be understood more formally, as shown below.
 *
 * The most <em>fundamental methods</em> of this trait are ``findAllChildElemsWithPathEntries`` and ``findWithElemPathEntry``.
 * The semantics of the other methods can be defined directly or indirectly in terms of method ``findAllChildElemsWithPathEntries``.
 *
 * The following must hold (for ``indexed.Elem``, which has structural equality defined):
 * {{{
 * elem.findAllChildElemsWithPathEntries.map(_._1) == elem.findAllChildElems
 *
 * elem.findAllChildElemsWithPathEntries forall { case (che, pe) => elem.findWithElemPathEntry(pe).get == che }
 * }}}
 *
 * The <em>basic operations</em> definable in terms of method ``findAllChildElemsWithPathEntries`` are ``filterChildElemPaths``,
 * ``filterElemOrSelfPaths`` and ``findTopmostElemOrSelfPaths``, analogous to trait ``ParentElemApi``. Their semantics must be
 * as if they had been defined as follows:
 * {{{
 * def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
 *   this.findAllChildElemsWithPathEntries collect { case (che, pe) if p(che) => ElemPath(Vector(pe)) }
 *
 * def filterElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
 *   (if (p(this)) Vector(ElemPath.Root) else Vector()) ++ {
 *     this.findAllChildElemsWithPathEntries flatMap { case (che, pe) =>
 *       che.filterElemOrSelfPaths(p).map(_.prepend(pe))
 *     }
 *   }
 *
 * def findTopmostElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
 *   if (p(this)) Vector(ElemPath.Root)
 *   else {
 *     this.findAllChildElemsWithPathEntries flatMap { case (che, pe) =>
 *       che.findTopmostElemOrSelfPaths(p).map(_.prepend(pe))
 *     }
 *   }
 * }}}
 *
 * Moreover, we could have defined:
 * {{{
 * def filterElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
 *   this.findAllChildElemsWithPathEntries flatMap { case (che, pe) =>
 *     che.filterElemOrSelfPaths(p).map(_.prepend(pe))
 *   }
 *
 * def findTopmostElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
 *   this.findAllChildElemsWithPathEntries flatMap { case (che, pe) =>
 *     che.findTopmostElemOrSelfPaths(p).map(_.prepend(pe))
 *   }
 * }}}
 * and:
 * {{{
 * def findAllElemOrSelfPaths: immutable.IndexedSeq[ElemPath] = filterElemOrSelfPaths(e => true)
 *
 * def findAllElemPaths: immutable.IndexedSeq[ElemPath] = filterElemPaths(e => true)
 * }}}
 *
 * Then, analogously to ``ParentElemApi``, the following properties hold:
 * {{{
 * elem.filterElemPaths(p) == elem.findAllElemPaths.filter(path => p(elem.findWithElemPath(path).get))
 *
 * elem.filterElemOrSelfPaths(p) == elem.findAllElemOrSelfPaths.filter(path => p(elem.findWithElemPath(path).get))
 * }}}
 * etc.
 *
 * Knowing that for ``resolved.Elem`` instance ``elem``, we have:
 * {{{
 * (elem.findAllChildElemsWithPathEntries map (_._1)) == elem.findAllChildElems
 * }}}
 * it follows that:
 * {{{
 * (elem.filterChildElemPaths(p) map (path => elem.findWithElemPath(path).get)) == elem.filterChildElems(p)
 *
 * (elem.filterElemOrSelfPaths(p) map (path => elem.findWithElemPath(path).get)) == elem.filterElemsOrSelf(p)
 *
 * (elem.filterElemPaths(p) map (path => elem.findWithElemPath(path).get)) == elem.filterElems(p)
 * }}}
 * etc., where ``findWithElemPath`` is defined recursively, using method ``findWithElemPathEntry``.
 *
 * No proofs are provided. Note that the similarities with trait ``ParentElemLike`` are striking.
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
