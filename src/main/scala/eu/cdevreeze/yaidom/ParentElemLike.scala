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
 * '''Most users of the yaidom API do not use this trait directly, so may skip the documentation of this trait.'''
 *
 * Based on an abstract method returning the child elements, this trait offers query methods to find descendant-or-self elements,
 * topmost descendant-or-self elements obeying a predicate, and so on.
 *
 * Concrete element classes, such as [[eu.cdevreeze.yaidom.Elem]] and [[eu.cdevreeze.yaidom.resolved.Elem]] (and even [[eu.cdevreeze.yaidom.ElemBuilder]]),
 * indeed mix in this trait (directly or indirectly), thus getting an API and implementation of many such query methods.
 *
 * Subtraits like [[eu.cdevreeze.yaidom.ElemLike]] implement many more methods on elements, based on more knowledge about elements, such
 * as element names and attributes. It is subtrait `UpdatableElemLike` that is typically mixed in by element classes. The distinction between
 * `ParentElemLike` and its subtraits is still useful, because this trait implements methods that only need knowledge about elements
 * as parent nodes of other elements. In an abstract sense, this trait could even be seen as an API that has nothing to do with
 * elements in particular, but that deals with trees (XML or not) in general (if we were to rename the trait, its methods and the type
 * parameter).
 *
 * The concrete element classes that mix in this trait (or a sub-trait) have knowledge about all child nodes of an element, whether
 * these child nodes are elements or not (such as text, comments etc.). Hence, this simple element-centric `ParentElemLike` API can be seen
 * as a good basis for querying arbitrary nodes and their values, even if this trait itself knows only about element nodes.
 *
 * For example, using class [[eu.cdevreeze.yaidom.Elem]], which also mixes in trait [[eu.cdevreeze.yaidom.HasText]],
 * all normalized text in a tree with document element `root` can be found as follows:
 * {{{
 * root.findAllElemsOrSelf map { e => e.normalizedText }
 * }}}
 * or:
 * {{{
 * root collectFromElemsOrSelf { case e: Elem => e.normalizedText }
 * }}}
 * As another example (also using the `HasText` trait as mixin), all text containing the string "query" can be found as follows:
 * {{{
 * root filterElemsOrSelf { e => e.text.contains("query") } map { _.text }
 * }}}
 * or:
 * {{{
 * root collectFromElemsOrSelf { case e: Elem if e.text.contains("query") => e.text }
 * }}}
 *
 * ==ParentElemLike more formally==
 *
 * The only abstract method is `allChildElems`. Based on this method alone, this trait offers a rich API for querying elements.
 *
 * As said above, this trait only knows about elements, not about other node types. Hence this trait has no knowledge about child nodes in
 * general. Hence the single type parameter, for the captured element type itself.
 *
 * Trait `ParentElemLike` has many methods for retrieving elements, but they are pretty easy to remember. First of all, an `ParentElemLike`
 * has 3 '''core''' element collection retrieval methods. These 3 methods (in order of subset relation) are:
 * <ul>
 * <li>Abstract method `allChildElems`, returning all '''child''' elements</li>
 * <li>Method `findAllElems`, finding all '''descendant''' elements</li>
 * <li>Method `findAllElemsOrSelf`, finding all '''descendant''' elements '''or self'''</li>
 * </ul>
 *
 * We define method `findAllElems` and `findAllElemsOrSelf` (recursively) as follows (in terms of equality):
 * {{{
 * elm.findAllElems == { elm.allChildElems flatMap (_.findAllElemsOrSelf) }
 *
 * elm.findAllElemsOrSelf == { elm +: (elm.allChildElems flatMap (_.findAllElemsOrSelf)) }
 * }}}
 *
 * The actual implementations may be different and more efficient, but they are consistent with these definitions.
 *
 * Strictly speaking, these '''core''' element collection retrieval methods, in combination with Scala's Collections API, should in theory
 * be enough for all element collection needs. For conciseness (and performance), there are more element (collection) retrieval methods.
 *
 * Below follows a summary of those groups of `ParentElemLike` element collection retrieval methods:
 * <ul>
 * <li>'''Filtering''': `filterChildElems`, `filterElems` and `filterElemsOrSelf`</li>
 * <li>'''Collecting data''': `collectFromChildElems`, `collectFromElems` and `collectFromElemsOrSelf`</li>
 * <li>'''Finding topmost obeying some predicate''' (not for child elements): `findTopmostElems` and `findTopmostElemsOrSelf`</li>
 * </ul>
 *
 * Often it is appropriate to query for collections of elements, but sometimes it is appropriate to query for individual elements.
 * Therefore there are also some `ParentElemLike` methods returning at most one element. These methods are as follows:
 * <ul>
 * <li>'''Finding first obeying some predicate''' (depth-first search): `findChildElem` and `getChildElem`, `findElem` and `findElemOrSelf`</li>
 * </ul>
 *
 * These element (collection) retrieval methods process and return elements in depth-first order
 * (see http://en.wikipedia.org/wiki/Depth-first_search). In other words, they process and return elements in document order,
 * which is the order encountered when reading the XML string from top to bottom. (This happens to be consistent with XPath 2.0,
 * where path expression results must be in document order.)
 *
 * We define some of those methods as follows (in terms of equality):
 * {{{
 * elm.filterChildElems(p) == (elm.allChildElems filter p)
 *
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
 *
 * elm.collectFromChildElems(pf) == elm.allChildElems.collect(pf)
 * elm.collectFromElems(pf) == elm.findAllElems.collect(pf)
 * elm.collectFromElemsOrSelf(pf) == elm.findAllElemsOrSelf.collect(pf)
 * }}}
 *
 * Again, the actual implementations may be more efficient, but are consistent with these definitions.
 *
 * Given the definitions above, there are some provable properties about these methods that are also intuitively true, and give
 * semantics to these methods. Assuming no side-effects etc. (see below for the precise assumptions made), some of these equalities are:
 * {{{
 * elm.filterElems(p) == elm.findAllElems.filter(p)
 * elm.filterElemsOrSelf(p) == elm.findAllElemsOrSelf.filter(p)
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
 * The methods returning at most one element trivially correspond to expressions containing calls to element collection
 * retrieval methods. For example, in the absence of side-effects etc. (see below for the precise assumptions made), the following holds:
 * {{{
 * elm.findElemOrSelf(p) == elm.filterElemsOrSelf(p).headOption
 * elm.findElemOrSelf(p) == elm.findTopmostElemsOrSelf(p).headOption
 * }}}
 *
 * ==ParentElemLike even more formally==
 *
 * ===1. Proving property about filterElemsOrSelf===
 *
 * Below follows a proof by structural induction of one of the above-mentioned properties.
 *
 * First we make a few assumptions, for this proof, and (implicitly) for the other proofs:
 * <ul>
 * <li>The function literals used in the properties ("element predicates" in this case) have no side-effects</li>
 * <li>These function literals terminate normally, without throwing any exception</li>
 * <li>These function literals are "closed terms", so the function values that are instances of these function literals are not "true closures"</li>
 * <li>These function literals use "fresh" variables, thus avoiding shadowing of variables defined in the context of the function literal</li>
 * <li>Equality on the element type is an equivalence relation (reflexive, symmetric, transitive)</li>
 * </ul>
 *
 * Based on these assumptions, we prove by induction that:
 * {{{
 * elm.filterElemsOrSelf(p) == elm.findAllElemsOrSelf.filter(p)
 * }}}
 *
 * __Base case__
 *
 * If `elm` has no child elements, then the LHS can be rewritten as follows:
 * {{{
 * elm.filterElemsOrSelf(p)
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.allChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filterElemsOrSelf
 * immutable.IndexedSeq(elm).filter(p) ++ (Seq() flatMap (_.filterElemsOrSelf(p))) // there are no child elements
 * immutable.IndexedSeq(elm).filter(p) ++ Seq() // flatMap on empty sequence returns empty sequence
 * immutable.IndexedSeq(elm).filter(p) // property of concatenation: xs ++ Seq() == xs
 * (immutable.IndexedSeq(elm) ++ Seq()).filter(p) // property of concatenation: xs ++ Seq() == xs
 * (immutable.IndexedSeq(elm) ++ (elm.allChildElems flatMap (_.findAllElemsOrSelf))) filter p // flatMap on empty sequence (of child elements) returns empty sequence
 * elm.findAllElemsOrSelf filter p // definition of findAllElemsOrSelf
 * }}}
 * which is the RHS.
 *
 * __Inductive step__
 *
 * For the inductive step, we use the following (general) properties:
 * {{{
 * (xs.filter(p) ++ ys.filter(p)) == ((xs ++ ys) filter p) // referred to below as property (a)
 * }}}
 * and:
 * {{{
 * (xs flatMap (x => f(x) filter p)) == ((xs flatMap f) filter p) // referred to below as property (b)
 * }}}
 *
 * If `elm` does have child elements, the LHS can be rewritten as:
 * {{{
 * elm.filterElemsOrSelf(p)
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.allChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filterElemsOrSelf
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.allChildElems flatMap (ch => ch.findAllElemsOrSelf filter p)) // induction hypothesis
 * immutable.IndexedSeq(elm).filter(p) ++ ((elm.allChildElems.flatMap(ch => ch.findAllElemsOrSelf)) filter p) // property (b)
 * (immutable.IndexedSeq(elm) ++ (elm.allChildElems flatMap (_.findAllElemsOrSelf))) filter p // property (a)
 * elm.findAllElemsOrSelf filter p // definition of findAllElemsOrSelf
 * }}}
 * which is the RHS.
 *
 * This completes the proof. Other above-mentioned properties can be proven by induction in a similar way.
 *
 * ===2. Proving property about filterElems===
 *
 * From the preceding proven property it easily follows (without using a proof by induction) that:
 * {{{
 * elm.filterElems(p) == elm.findAllElems.filter(p)
 * }}}
 * After all, the LHS can be rewritten as follows:
 * {{{
 * elm.filterElems(p)
 * (elm.allChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filterElems
 * (elm.allChildElems flatMap (e => e.findAllElemsOrSelf.filter(p))) // using the property proven above
 * (elm.allChildElems flatMap (_.findAllElemsOrSelf)) filter p // using property (b) above
 * elm.findAllElems filter p // definition of findAllElems
 * }}}
 * which is the RHS.
 *
 * ===3. Proving property about findTopmostElemsOrSelf===
 *
 * Given the above-mentioned assumptions, we prove by structural induction that:
 * {{{
 * (elm.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) == (elm.filterElemsOrSelf(p))
 * }}}
 *
 * __Base case__
 *
 * If `elm` has no child elements, and `p(elm)` holds, then LHS and RHS evaluate to `immutable.IndexedSeq(elm)`.
 *
 * If `elm` has no child elements, and `p(elm)` does not hold, then LHS and RHS evaluate to `immutable.IndexedSeq()`.
 *
 * __Inductive step__
 *
 * For the inductive step, we introduce the following additional (general) property, if `f` and `g` have the same types:
 * {{{
 * ((xs flatMap f) flatMap g) == (xs flatMap (x => f(x) flatMap g)) // referred to below as property (c)
 * }}}
 *
 * If `elm` does have child elements, and `p(elm)` holds, the LHS can be rewritten as:
 * {{{
 * (elm.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p)))
 * immutable.IndexedSeq(elm) flatMap (_.filterElemsOrSelf(p)) // definition of findTopmostElemsOrSelf, knowing that p(elm) holds
 * elm.filterElemsOrSelf(p) // definition of flatMap, applied to singleton sequence
 * }}}
 * which is the RHS. In this case, we did not even need the induction hypothesis.
 *
 * If `elm` does have child elements, and `p(elm)` does not hold, the LHS can be rewritten as:
 * {{{
 * (elm.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p)))
 * (elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p))) flatMap (_.filterElemsOrSelf(p)) // definition of findTopmostElemsOrSelf, knowing that p(elm) does not hold
 * elm.allChildElems flatMap (ch => ch.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) // property (c)
 * elm.allChildElems flatMap (_.filterElemsOrSelf(p)) // induction hypothesis
 * immutable.IndexedSeq() ++ (elm.allChildElems flatMap (_.filterElemsOrSelf(p))) // definition of concatenation
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.allChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filter, knowing that p(elm) does not hold
 * elm.filterElemsOrSelf(p) // definition of filterElems
 * }}}
 * which is the RHS.
 *
 * ===4. Proving property about findTopmostElems===
 *
 * From the preceding proven property it easily follows (without using a proof by induction) that:
 * {{{
 * (elm.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p))) == (elm.filterElems(p))
 * }}}
 *
 * After all, the LHS can be rewritten to:
 * {{{
 * (elm.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p)))
 * (elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p))) flatMap (_.filterElemsOrSelf(p)) // definition of findTopmostElems
 * elm.allChildElems flatMap (ch => ch.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) // property (c)
 * elm.allChildElems flatMap (_.filterElemsOrSelf(p)) // using the property proven above
 * elm.filterElems(p) // definition of filterElems
 * }}}
 * which is the RHS.
 *
 * ===5. Properties used in the proofs above===
 *
 * There are several (unproven) properties that were used in the proofs above:
 * {{{
 * (xs.filter(p) ++ ys.filter(p)) == ((xs ++ ys) filter p) // property (a); filter distributes over concatenation
 *
 * (xs flatMap (x => f(x) filter p)) == ((xs flatMap f) filter p) // property (b)
 *
 * ((xs flatMap f) flatMap g) == (xs flatMap (x => f(x) flatMap g)) // if f and g have the same types; property (c)
 * }}}
 *
 * No proofs are offered here, but we could prove them ourselves. It would help to regard `xs` and `ys` above as `List` instances,
 * and use structural induction for Lists (!) as proof method. First we would need some defining clauses for `filter`, `++` and `flatMap`:
 * {{{
 * Nil.filter(p) == Nil
 * (x :: xs).filter(p) == if (p(x)) x :: xs.filter(p) else xs.filter(p)
 *
 * Nil ++ ys == ys
 * (x :: xs) ++ ys == x :: (xs ++ ys)
 *
 * Nil.flatMap(f) == Nil
 * (x :: xs).flatMap(f) == (f(x) ++ xs.flatMap(f))
 * }}}
 *
 * Property (a) could then be proven by structural induction (for lists), by using only defining clauses and no other proven properties.
 * Property (b) could then be proven by structural induction as well, but (possibly) requiring property (a) in its proof.
 * Property (c) could be proven by structural induction, if we would first prove the distribution law for `flatMap` over concatenation.
 * The proof by structural induction of the latter property would (possibly) depend on the property that concatenation is associative.
 * These proofs are all left as exercises for the reader, as they say. Yaidom considers these properties as theorems based on which "yaidom properties"
 * were proven above.
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
trait ParentElemLike[E <: ParentElemLike[E]] { self: E =>

  /** Returns all child elements, in the correct order. The faster this method is, the faster the other `ParentElemLike` methods will be. */
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
