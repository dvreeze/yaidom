/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.queryapi

import scala.collection.immutable
import scala.collection.mutable

/**
 * API and implementation trait for elements as containers of elements, as element nodes in a node tree. This trait knows very little
 * about elements. It does not know about names, attributes, etc. All it knows about elements is that elements can have element children (other
 * node types are entirely out of scope in this trait).
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.ElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment. Below follows an even more formal treatment, with proofs by induction
 * of important properties obeyed by methods of this API. It shows the <em>mathematical</em> rigor of the yaidom query API.
 * API users that are only interested in how to use the API can safely skip that formal treatment.
 *
 * ==ElemLike more formally==
 *
 * '''In order to get started using the API, this more formal section can safely be skipped. On the other hand, this section
 * may provide a deeper understanding of the API.'''
 *
 * The only abstract method is `findAllChildElems`. Based on this method alone, this trait offers a rich API for querying elements.
 * This is entirely consistent with the semantics defined in the `ElemApi` trait. Indeed, the implementation of the methods
 * follows the semantics defined there.
 *
 * In the `ElemApi` trait, some (simple) provable laws were mentioned. Some proofs follow below.
 *
 * ===1. Proving property about filterElemsOrSelf===
 *
 * Below follows a proof by structural induction of one of the laws mentioned in the documentation of trait `ElemApi`.
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
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.findAllChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filterElemsOrSelf
 * immutable.IndexedSeq(elm).filter(p) ++ (Seq() flatMap (_.filterElemsOrSelf(p))) // there are no child elements
 * immutable.IndexedSeq(elm).filter(p) ++ Seq() // flatMap on empty sequence returns empty sequence
 * immutable.IndexedSeq(elm).filter(p) // property of concatenation: xs ++ Seq() == xs
 * (immutable.IndexedSeq(elm) ++ Seq()).filter(p) // property of concatenation: xs ++ Seq() == xs
 * (immutable.IndexedSeq(elm) ++ (elm.findAllChildElems flatMap (_ filterElemsOrSelf (e => true)))) filter p // flatMap on empty sequence (of child elements) returns empty sequence
 * (immutable.IndexedSeq(elm).filter(e => true) ++ (elm.findAllChildElems flatMap (_ filterElemsOrSelf (e => true)))) filter p // filtering with predicate that is always true
 * elm.filterElemsOrSelf(e => true) filter p // definition of filterElemsOrSelf
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
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.findAllChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filterElemsOrSelf
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.findAllChildElems flatMap (ch => ch.findAllElemsOrSelf filter p)) // induction hypothesis
 * immutable.IndexedSeq(elm).filter(p) ++ ((elm.findAllChildElems.flatMap(ch => ch.findAllElemsOrSelf)) filter p) // property (b)
 * (immutable.IndexedSeq(elm) ++ (elm.findAllChildElems flatMap (_.findAllElemsOrSelf))) filter p // property (a)
 * (immutable.IndexedSeq(elm) ++ (elm.findAllChildElems flatMap (_ filterElemsOrSelf (e => true)))) filter p // definition of findAllElemsOrSelf
 * (immutable.IndexedSeq(elm).filter(e => true) ++ (elm.findAllChildElems flatMap (_ filterElemsOrSelf (e => true)))) filter p // filtering with predicate that is always true
 * elm.filterElemsOrSelf(e => true) filter p // definition of filterElemsOrSelf
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
 * (elm.findAllChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filterElems
 * (elm.findAllChildElems flatMap (e => e.findAllElemsOrSelf.filter(p))) // using the property proven above
 * (elm.findAllChildElems flatMap (_.findAllElemsOrSelf)) filter p // using property (b) above
 * (elm.findAllChildElems flatMap (_ filterElemsOrSelf (e => true))) filter p // definition of findAllElemsOrSelf
 * elm.filterElems(e => true) filter p // definition of filterElems
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
 * This is also known as the "associativity law for monads". (Monadic types obey 3 laws: associativity, left unit and right unit.)
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
 * (elm.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p))) flatMap (_.filterElemsOrSelf(p)) // definition of findTopmostElemsOrSelf, knowing that p(elm) does not hold
 * elm.findAllChildElems flatMap (ch => ch.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) // property (c)
 * elm.findAllChildElems flatMap (_.filterElemsOrSelf(p)) // induction hypothesis
 * immutable.IndexedSeq() ++ (elm.findAllChildElems flatMap (_.filterElemsOrSelf(p))) // definition of concatenation
 * immutable.IndexedSeq(elm).filter(p) ++ (elm.findAllChildElems flatMap (_.filterElemsOrSelf(p))) // definition of filter, knowing that p(elm) does not hold
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
 * (elm.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p))) flatMap (_.filterElemsOrSelf(p)) // definition of findTopmostElems
 * elm.findAllChildElems flatMap (ch => ch.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) // property (c)
 * elm.findAllChildElems flatMap (_.filterElemsOrSelf(p)) // using the property proven above
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
 * // Associativity law for monads
 * ((xs flatMap f) flatMap g) == (xs flatMap (x => f(x) flatMap g)) // property (c)
 * }}}
 *
 * Property (a) is obvious, and stated without proof. Property (c) is known as the "associativity law for monads".
 * Property (b) is proven below.
 *
 * To prove property (b), we use property (c), as well as the following property (d):
 * {{{
 * (xs filter p) == (xs flatMap (y => if (p(y)) List(y) else Nil)) // property (d)
 * }}}
 * Then property (b) can be proven as follows:
 * {{{
 * xs flatMap (x => f(x) filter p)
 * xs flatMap (x => f(x) flatMap (y => if (p(y)) List(y) else Nil))
 * (xs flatMap f) flatMap (y => if (p(y)) List(y) else Nil) // property (c)
 * (xs flatMap f) filter p
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
 * @author Chris de Vreeze
 */
trait ElemLike extends ElemApi {

  type ThisElemApi <: ElemLike

  def findAllChildElems: immutable.IndexedSeq[ThisElem]

  final def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = findAllChildElems filter p

  final def \(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = filterChildElems(p)

  final def findChildElem(p: ThisElem => Boolean): Option[ThisElem] = {
    val result = filterChildElems(p)
    result.headOption
  }

  final def getChildElem(p: ThisElem => Boolean): ThisElem = {
    val result = filterChildElems(p)
    require(result.size == 1, s"Expected exactly 1 matching child element, but found ${result.size} of them")
    result.head
  }

  final def findAllElemsOrSelf: immutable.IndexedSeq[ThisElem] = {
    val result = mutable.ArrayBuffer[ThisElem]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: ThisElem): Unit = {
      result += elm
      elm.findAllChildElems foreach { e => accumulate(e) }
    }

    accumulate(thisElem)
    result.toIndexedSeq
  }

  final def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    val result = mutable.ArrayBuffer[ThisElem]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: ThisElem): Unit = {
      if (p(elm)) result += elm
      elm.findAllChildElems foreach { e => accumulate(e) }
    }

    accumulate(thisElem)
    result.toIndexedSeq
  }

  final def \\(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = filterElemsOrSelf(p)

  final def findAllElems: immutable.IndexedSeq[ThisElem] = findAllChildElems flatMap { ch => ch.findAllElemsOrSelf }

  final def filterElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = findAllChildElems flatMap { ch => ch filterElemsOrSelf p }

  final def findTopmostElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
    val result = mutable.ArrayBuffer[ThisElem]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: ThisElem): Unit = {
      if (p(elm)) result += elm else {
        elm.findAllChildElems foreach { e => accumulate(e) }
      }
    }

    accumulate(thisElem)
    result.toIndexedSeq
  }

  final def \\!(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = findTopmostElemsOrSelf(p)

  final def findTopmostElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] =
    findAllChildElems flatMap { ch => ch findTopmostElemsOrSelf p }

  final def findElemOrSelf(p: ThisElem => Boolean): Option[ThisElem] = {
    // Not tail-recursive, but the depth should typically be limited
    def findMatch(elm: ThisElem): Option[ThisElem] = {
      if (p(elm)) Some(elm) else {
        val childElms = elm.findAllChildElems

        var i = 0
        var result: Option[ThisElem] = None

        while ((result.isEmpty) && (i < childElms.size)) {
          result = findMatch(childElms(i))
          i += 1
        }

        result
      }
    }

    findMatch(thisElem)
  }

  final def findElem(p: ThisElem => Boolean): Option[ThisElem] = {
    val elms = thisElem.findAllChildElems.view flatMap { ch => ch findElemOrSelf p }
    elms.headOption
  }
}

object ElemLike {

  /**
   * This query API type, fixing ThisElem and ThisElemApi to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = ElemLike { type ThisElem = E; type ThisElemApi = E }
}
