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
 * API and implementation trait for elements as containers of elements, as indexed element nodes in a node tree. This trait
 * offers query methods for "indexes", as `ElemPath` instances.
 *
 * This trait to a large extent mirrors the `ElemNodeLike` trait. This trait knows more about elements, though. It knows about
 * `ElemPath` indexes, and therefore about elements having resolved names. Other node types than elements are not known to this API.
 *
 * Based on abstract methods `allChildElemsWithPathEntries` and `findWithElemPath`, this trait offers a rich `ElemPath` query API.
 *
 * This trait is mixed in by trait `ElemLike`, and therefore by [[eu.cdevreeze.yaidom.Elem]] and [[eu.cdevreeze.yaidom.resolved.Elem]].
 *
 * Example usage:
 * {{{
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val bookAuthorElms =
 *   for {
 *     authorPath <- bookstoreElm findTopmostElemPaths { _.localName == "Author" }
 *     if authorPath.containsName(EName("Book"))
 *   } yield bookstoreElm.getWithElemPath(authorPath)
 * }}}
 *
 * The above example shows how we can use the results of queries for `ElemPath`s, if we are interested in the ancestors of the
 * elements at those paths. Of course, using the `ElemNodeLike` API, this example could have been written simply as:
 * {{{
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val bookAuthorElms =
 *   for {
 *     bookElm <- bookstoreElm \\ EName("Book")
 *     authorElm <- bookElm \\! "Author"
 *   } yield bookElm
 * }}}
 *
 * Indeed, the query methods of the `ElemNodeLike` API should often be preferred to those of this `IndexedElemNodeLike` API.
 * After all, `ElemPath`s are relative to one specific root element, they are "volatile" (in that "functional updates" may render them
 * useless), and they are rather slow indexes. Moreover, the `ElemNodeLike` query methods tend to be faster than those of this trait.
 *
 * On the other hand, it is often the combination of `ElemNodeLike` API query methods and `IndexedElemNodeLike` API query methods
 * that offer interesting querying possibilities. After all, sometimes it is handy to formulate a query in such a way that ancestors
 * are retrieved in at least one of the intermediate steps.
 *
 * Another use for `ElemPath` queries is functional updates. See [[eu.cdevreeze.yaidom.UpdatableElemLike]] for the "update"
 * methods. Some `updated` methods take an `ElemPath`, and another `updated` method is implemented using an `ElemPath` query offered
 * by this API.
 *
 * ==IndexedElemNodeLike more formally==
 *
 * Analogously to the `ElemNodeLike` API, there are 3 '''core''' element path retrieval methods:
 * <ul>
 * <li>Method `allChildElemPaths`, returning the paths (relative to this element) of all '''child''' elements</li>
 * <li>Method `findAllElemPaths`, finding the paths (relative to this element) of all '''descendant''' elements</li>
 * <li>Method `findAllElemOrSelfPaths`, finding the paths (relative to this element) of all '''descendant''' elements '''or self'''</li>
 * </ul>
 *
 * For example, instead of:
 * {{{
 * val titlePaths = elm filterElemOrSelfPaths { e => e.resolvedName == EName("Title") }
 * }}}
 * we could write (more verbosely):
 * {{{
 * val titlePaths = elm.findAllElemOrSelfPaths filter { path => elm.getWithElemPath(path).resolvedName == EName("Title") }
 * }}}
 * The second statement is far less efficient, due to repeated calls to method `getWithElemPath`. In this case, we could instead
 * have written:
 * {{{
 * val titlePaths = elm.findAllElemOrSelfPaths filter { path => path.endsWithName(EName("Title")) }
 * }}}
 *
 * Assuming correct implementations of the abstract methods, and using "resolved" (!) [[eu.cdevreeze.yaidom.resolved.Elem]] instances
 * (which have structural equality defined), this trait obeys some obvious properties, expressed using equality:
 * {{{
 * (elm.findAllElemOrSelfPaths map (path => elm.getWithElemPath(path))) == elm.findAllElemsOrSelf
 * (elm.findAllElemPaths map (path => elm.getWithElemPath(path))) == elm.findAllElems
 * (elm.allChildElemPaths map (path => elm.getWithElemPath(path))) == elm.allChildElems
 *
 * (elm.filterElemOrSelfPaths(p) map (path => elm.getWithElemPath(path))) == elm.filterElemsOrSelf(p)
 * (elm.filterElemPaths(p) map (path => elm.getWithElemPath(path))) == elm.filterElems(p)
 *
 * (elm.findTopmostElemOrSelfPaths(p) map (path => elm.getWithElemPath(path))) == elm.findTopmostElemsOrSelf(p)
 * (elm.findTopmostElemPaths(p) map (path => elm.getWithElemPath(path))) == elm.findTopmostElems(p)
 * }}}
 * etc.
 *
 * We offer no proofs of these properties.
 *
 * ==Implementation notes==
 *
 * Like trait `ElemNodeLike`, some query methods use recursion in their implementations, but no tail recursion. See [[eu.cdevreeze.yaidom.ElemNodeLike]]
 * for a motivation.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait IndexedElemNodeLike[E <: IndexedElemNodeLike[E]] { self: E =>

  /** Returns all child elements with their `ElemPath` entries, in the correct order */
  def allChildElemsWithPathEntries: immutable.IndexedSeq[(E, ElemPath.Entry)]

  /**
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   */
  def findWithElemPath(path: ElemPath): Option[E]

  /** Returns (the equivalent of) `findWithElemPath(path).get` */
  final def getWithElemPath(path: ElemPath): E =
    findWithElemPath(path).getOrElse(sys.error("Expected existing path %s from root %s".format(path, self)))

  /** Returns `allChildElemsWithPathEntries map { case (e, pe) => ElemPath.from(pe) }` */
  final def allChildElemPaths: immutable.IndexedSeq[ElemPath] =
    allChildElemsWithPathEntries map { case (e, pe) => ElemPath.from(pe) }

  /** Returns the paths of child elements obeying the given predicate */
  final def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    allChildElemsWithPathEntries filter { case (e, pe) => p(e) } map { case (e, pe) => ElemPath.from(pe) }

  /** Returns the path of the first found child element obeying the given predicate, if any, wrapped in an `Option` */
  final def findChildElemPath(p: E => Boolean): Option[ElemPath] = {
    allChildElemsWithPathEntries find { case (e, pe) => p(e) } map { case (e, pe) => ElemPath.from(pe) }
  }

  /** Returns the path of the single child element obeying the given predicate, and throws an exception otherwise */
  final def getChildElemPath(p: E => Boolean): ElemPath = {
    val result = filterChildElemPaths(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  /** Returns the path of this element followed by the paths of all descendant elements (that is, the descendant-or-self elements) */
  final def findAllElemOrSelfPaths: immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = allChildElemsWithPathEntries flatMap {
      case (e, pe) => e.findAllElemOrSelfPaths map { path => path.prepend(pe) }
    }

    ElemPath.Root +: remainder
  }

  /**
   * Returns the paths of descendant-or-self elements that obey the given predicate.
   * That is, the result is equivalent to the paths of `findAllElemsOrSelf filter p`.
   */
  final def filterElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = allChildElemsWithPathEntries flatMap {
      case (e, pe) => e.filterElemOrSelfPaths(p) map { path => path.prepend(pe) }
    }

    if (p(self)) (ElemPath.Root +: remainder) else remainder
  }

  /** Returns the paths of all descendant elements (not including this element). Equivalent to `findAllElemOrSelfPaths.drop(1)` */
  final def findAllElemPaths: immutable.IndexedSeq[ElemPath] =
    allChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findAllElemOrSelfPaths map { path => path.prepend(pe) } }

  /** Returns the paths of descendant elements obeying the given predicate, that is, the paths of `findAllElems filter p` */
  final def filterElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    allChildElemsWithPathEntries flatMap { case (ch, pe) => ch.filterElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  /**
   * Returns the paths of the descendant-or-self elements that obey the given predicate, such that no ancestor obeys the predicate.
   */
  final def findTopmostElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    if (p(self)) immutable.IndexedSeq(ElemPath.Root) else {
      // Not tail-recursive, but the depth should typically be limited
      val result = allChildElemsWithPathEntries flatMap {
        case (e, pe) => e.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) }
      }
      result
    }
  }

  /** Returns the paths of the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def findTopmostElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    allChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  /** Returns the path of the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option` */
  final def findElemOrSelfPath(p: E => Boolean): Option[ElemPath] = {
    // Not efficient
    filterElemOrSelfPaths(p).headOption
  }

  /** Returns the path of the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option` */
  final def findElemPath(p: E => Boolean): Option[ElemPath] = {
    val elms = self.allChildElemsWithPathEntries.view flatMap { case (ch, pe) => ch.findElemOrSelfPath(p) map { path => path.prepend(pe) } }
    elms.headOption
  }
}
