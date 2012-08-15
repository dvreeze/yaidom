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
 * API and implementation trait for elements as containers of elements, as element nodes in a node tree. This trait knows very little
 * about elements. It does not know about names, attributes, etc. All it knows about elements is that elements can have element children (other
 * node types are entirely out of scope in this trait).
 *
 * Based on an abstract method returning the child elements, this trait offers query methods to find descendant-or-self elements,
 * topmost descendant-or-self elements obeying a predicate, and so on.
 *
 * This trait implements those methods of the `Elem` class, which mixes in this trait. It is also used for implementing parts of other
 * "element-like" classes, other than the "core" [[eu.cdevreeze.yaidom.Elem]] class, such as [[eu.cdevreeze.yaidom.resolved.Elem]] for
 * "resolved" elements.
 *
 * Subtrait [[eu.cdevreeze.yaidom.ElemLike]] implements many more methods on elements, based on more knowledge about elements, such
 * as element names and attributes. It is indeed the `ElemLike` trait that is mixed in by element classes. The distinction between
 * this trait and subtrait `ElemLike` is still useful, because this trait implements methods that only need knowledge about
 * elements as parent nodes of other elements. In an abstract sense, this trait even has nothing to do with elements in particular,
 * but it deals with trees (XML or not) in general (if we were to rename the trait, its methods and the type parameter).
 *
 * ==ElemNodeLike more formally==
 *
 * The only abstract method is `allChildElems`. Based on this method alone, this trait offers a rich API for querying elements.
 *
 * As said above, this trait only knows about elements, not about other node types. Hence this trait has no knowledge about child nodes in
 * general. Hence the single type parameter, for the captured element type itself.
 *
 * Trait `ElemNodeLike` has many methods for retrieving elements, but they are pretty easy to remember. First of all, an `ElemNodeLike`
 * has 3 '''core''' element collection retrieval methods. These 3 methods (in order of subset relation) are:
 * <ul>
 * <li>Abstract method `allChildElems`, returning all '''child''' elements</li>
 * <li>Method `findAllElems`, finding all '''descendant''' elements</li>
 * <li>Method `findAllElemsOrSelf`, finding all '''descendant''' elements '''or self'''</li>
 * </ul>
 * The latter 2 methods are implemented in terms of method `allChildElems`. The following equalities define their semantics more formally:
 * {{{
 * elm.findAllElems == (elm.allChildElems flatMap (_.findAllElemsOrSelf))
 *
 * elm.findAllElemsOrSelf == {
 *   elm +: (elm.allChildElems flatMap (_.findAllElemsOrSelf))
 * }
 * }}}
 * Strictly speaking, these '''core''' element collection retrieval methods, in combination with Scala's Collections API, should in theory
 * be enough for all element collection needs. For conciseness (and performance), there are more element (collection) retrieval methods.
 *
 * Below follows a summary of those groups of `ElemNodeLike` element collection retrieval methods:
 * <ul>
 * <li>'''Filtering''': `filterChildElems`, `filterElems` and `filterElemsOrSelf`</li>
 * <li>'''Collecting data''': `collectFromChildElems`, `collectFromElems` and `collectFromElemsOrSelf`</li>
 * <li>'''Finding topmost obeying some predicate''' (not for child elements): `findTopmostElems` and `findTopmostElemsOrSelf`</li>
 * </ul>
 *
 * Often it is appropriate to query for collections of elements, but sometimes it is appropriate to query for individual elements.
 * Therefore there are also some `ElemNodeLike` methods returning at most one element. These methods are as follows:
 * <ul>
 * <li>'''Finding first obeying some predicate''' (depth-first search): `findChildElem` and `getChildElem`, `findElem` and `findElemOrSelf`</li>
 * </ul>
 *
 * These element (collection) retrieval methods process and return elements in depth-first order
 * (see http://en.wikipedia.org/wiki/Depth-first_search).
 *
 * Assuming no side-effects, some equalities defining the semantics of the left-hand-side are:
 * {{{
 * e.filterChildElems(p) == e.allChildElems.filter(p)
 * e.filterElems(p) == e.findAllElems.filter(p)
 * e.filterElemsOrSelf(p) == e.findAllElemsOrSelf.filter(p)
 *
 * e.collectFromChildElems(pf) == e.allChildElems.collect(pf)
 * e.collectFromElems(pf) == e.findAllElems.collect(pf)
 * e.collectFromElemsOrSelf(pf) == e.findAllElemsOrSelf.collect(pf)
 *
 * elm.findTopmostElems(p) == {
 *   elm.filterElems(p) filter { e =>
 *     val hasNoMatchingAncestor = elm.filterElems(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 *
 * elm.findTopmostElemsOrSelf(p) == {
 *   elm.filterElemsOrSelf(p) filter { e =>
 *     val hasNoMatchingAncestor = elm.filterElemsOrSelf(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 * }}}
 * The latter put differently:
 * {{{
 * (elm.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p))) == (elm.filterElems(p))
 * (elm.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) == (elm.filterElemsOrSelf(p))
 * }}}
 *
 * The equalities above give semantics to the left-hand sides, but do not necessarily suggest how they are implemented.
 * Assuming no side-effects, the following (provable) equalities hint at possible implementations of the left-hand-sides, although
 * the real implementations may be (far) more efficient:
 * {{{
 * elm.filterElems(p) == (elm.allChildElems flatMap (_.filterElemsOrSelf(p)))
 *
 * elm.filterElemsOrSelf(p) == {
 *   (immutable.IndexedSeq(elm).filter(p)) ++ (elm.allChildElems flatMap (_.filterElemsOrSelf(p)))
 * }
 *
 * elm.findTopmostElems(p) == (elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p)))
 *
 * elm.findTopmostElemsOrSelf(p) == {
 *   if (p(elm))
 *     immutable.IndexedSeq(elm)
 *   else
 *     (elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p)))
 * }
 * }}}
 *
 * The methods returning at most one element trivially correspond to expressions containing calls to element collection
 * retrieval methods. For example (in the absence of side-effects) the following holds:
 * {{{
 * e.findElemOrSelf(p) == e.filterElemsOrSelf(p).headOption
 * e.findElemOrSelf(p) == e.findTopmostElemsOrSelf(p).headOption
 * }}}
 *
 * ==Implementation notes==
 *
 * Methods `findAllElemsOrSelf`, `filterElemsOrSelf`, `findTopmostElemsOrSelf` and `findElemOrSelf` use recursion in their
 * implementations, but not tail-recursion. The lack of tail-recursion should not be a problem, due to limited XML tree
 * depths in practice. It is comparable to an "idiomatic" Scala quicksort implementation in its lack of tail-recursion.
 * Also in the case of quicksort, the lack of tail-recursion is acceptable due to limited recursion depths. If we want tail-recursive
 * implementations of the above-mentioned methods (in particular the first 3 ones), we either lose the ordering of result elements
 * in document order (depth-first), or we lose performance and/or clarity. That just is not worth it.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ElemNodeLike[E <: ElemNodeLike[E]] { self: E =>

  /** Returns all child elements, in the correct order. The faster this method is, the faster the other `ElemNodeLike` methods will be. */
  def allChildElems: immutable.IndexedSeq[E]

  /** Returns the child elements obeying the given predicate */
  final def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems filter p

  /** Shorthand for `filterChildElems(p)`. Use this shorthand only if the predicate is a short expression. */
  final def \(p: E => Boolean): immutable.IndexedSeq[E] = filterChildElems(p)

  /** Returns `allChildElems collect pf` */
  final def collectFromChildElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] = allChildElems collect pf

  /** Returns the first found child element obeying the given predicate, if any, wrapped in an `Option` */
  final def findChildElem(p: E => Boolean): Option[E] = {
    val result = filterChildElems(p)
    result.headOption
  }

  /** Returns the single child element obeying the given predicate, and throws an exception otherwise */
  final def getChildElem(p: E => Boolean): E = {
    val result = filterChildElems(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  /** Returns this element followed by all descendant elements (that is, the descendant-or-self elements) */
  final def findAllElemsOrSelf: immutable.IndexedSeq[E] = {
    val result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: E) {
      result += elm
      elm.allChildElems foreach { e => accumulate(e) }
    }

    accumulate(self)
    result.toIndexedSeq
  }

  /**
   * Returns the descendant-or-self elements that obey the given predicate.
   * That is, the result is equivalent to `findAllElemsOrSelf filter p`.
   */
  final def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] = {
    val result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: E) {
      if (p(elm)) result += elm
      elm.allChildElems foreach { e => accumulate(e) }
    }

    accumulate(self)
    result.toIndexedSeq
  }

  /** Shorthand for `filterElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  final def \\(p: E => Boolean): immutable.IndexedSeq[E] = filterElemsOrSelf(p)

  /** Returns (the equivalent of) `findAllElemsOrSelf collect pf` */
  final def collectFromElemsOrSelf[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    filterElemsOrSelf { e => pf.isDefinedAt(e) } collect pf

  /** Returns all descendant elements (not including this element). Equivalent to `findAllElemsOrSelf.drop(1)` */
  final def findAllElems: immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch.findAllElemsOrSelf }

  /** Returns the descendant elements obeying the given predicate, that is, `findAllElems filter p` */
  final def filterElems(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch filterElemsOrSelf p }

  /** Returns (the equivalent of) `findAllElems collect pf` */
  final def collectFromElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    filterElems { e => pf.isDefinedAt(e) } collect pf

  /**
   * Returns the descendant-or-self elements that obey the given predicate, such that no ancestor obeys the predicate.
   */
  final def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] = {
    val result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: E) {
      if (p(elm)) result += elm else {
        elm.allChildElems foreach { e => accumulate(e) }
      }
    }

    accumulate(self)
    result.toIndexedSeq
  }

  /** Shorthand for `findTopmostElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  final def \\!(p: E => Boolean): immutable.IndexedSeq[E] = findTopmostElemsOrSelf(p)

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def findTopmostElems(p: E => Boolean): immutable.IndexedSeq[E] =
    allChildElems flatMap { ch => ch findTopmostElemsOrSelf p }

  /** Returns the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option` */
  final def findElemOrSelf(p: E => Boolean): Option[E] = {
    // Not tail-recursive, but the depth should typically be limited
    def findMatch(elm: E): Option[E] = {
      if (p(elm)) Some(elm) else {
        val childElms = elm.allChildElems

        var i = 0
        var result: Option[E] = None

        while ((result.isEmpty) && (i < childElms.size)) {
          result = findMatch(childElms(i))
          i += 1
        }

        result
      }
    }

    findMatch(self)
  }

  /** Returns the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option` */
  final def findElem(p: E => Boolean): Option[E] = {
    val elms = self.allChildElems.view flatMap { ch => ch findElemOrSelf p }
    elms.headOption
  }

  /** Computes an index on the given function taking an element, for example a function returning some unique element "identifier" */
  final def getIndex[K](f: E => K): Map[K, immutable.IndexedSeq[E]] = findAllElemsOrSelf groupBy f
}
