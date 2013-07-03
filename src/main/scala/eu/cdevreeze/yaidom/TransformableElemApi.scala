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
 * "Transformable" element. It defines a contract for transformations, applying an element transforming function to some or all
 * elements in an element tree. See [[eu.cdevreeze.yaidom.TransformableElemLike]].
 *
 * The big conceptual difference with "updatable" elements (in trait `UpdatableElemLike[N, E]`) is that "transformations" are
 * about applying some transforming function to some or all descendant-or-self elements, while "(functional) updates" are about
 * "updates" at a given element path. So "transformations" are bulk updates, not using element paths, and "(functional) updates"
 * are single element updates, at a given element path.
 *
 * In spite of these differences, "transformations" can be understood in terms of equivalent repeated "updates". Concerning
 * performance, it is best to avoid "updates" (`UpdatableElemApi`) if many element paths are involved. Those "updates" are
 * not meant for bulk updates, whereas "transformations" are.
 *
 * This purely abstract API leaves the implementation completely open.
 *
 * Note that in a way this API can be seen as the "update" counterpart of query API `ParentElemApi`, in that the 3 "axes"
 * of child elements, descendant elements, and descendant-or-self elements can be recognized.
 *
 * ==TransformableElemApi more formally==
 *
 * The `TransformableElemApi` trait obeys some interesting properties.
 *
 * First of all, we have the following property about '''replaceAllChildElems in terms of updated''':
 * {{{
 * resolved.Elem(elem.replaceAllChildElems(f)) ==
 *   resolved.Elem(elem.findAllChildElemPathEntries.foldLeft(elem) { (acc, pathEntry) => acc.updated(pathEntry)(f) })
 * }}}
 *
 * After all, the LHS can be rewritten as follows (modulo resolved.Elem equality):
 * {{{
 * elem.replaceAllChildElems(f)
 *
 * // by definition
 * elem.withChildren(elem.children map {
 *   case e: E => f(e)
 *   case n: N => n
 * })
 *
 * // rewriting the child element update, using foldLeft and element path entries
 * elem.findAllChildElemPathEntries.foldLeft(elem) { case (acc, pathEntry) =>
 *   acc.updated(pathEntry)(f)
 * }
 * }}}
 * which is the RHS. This completes the proof.
 *
 * If we reverse the child element paths, the property still holds:
 * {{{
 * resolved.Elem(elem.replaceAllChildElems(f)) ==
 *   resolved.Elem(elem.findAllChildElemPathEntries.reverse.foldLeft(elem) { (acc, pathEntry) => acc.updated(pathEntry)(f) })
 * }}}
 * After all, each element is replaced by 1 element, so the child element path entries remain valid during the update.
 *
 * It easily follows that the same property holds for (child element) paths instead of path entries:
 * {{{
 * resolved.Elem(elem.replaceAllChildElems(f)) ==
 *   resolved.Elem(elem.findAllChildElemPaths.foldLeft(elem) { (acc, path) => acc.updated(path)(f) })
 * }}}
 * or:
 * {{{
 * resolved.Elem(elem.replaceAllChildElems(f)) ==
 *   resolved.Elem(elem.findAllChildElemPaths.reverse.foldLeft(elem) { (acc, path) => acc.updated(path)(f) })
 * }}}
 *
 * Furthermore, the following property about '''replaceAllElemsOrSelf in terms of updated''' holds:
 * {{{
 * resolved.Elem(elem.replaceAllElemsOrSelf(f)) ==
 *   resolved.Elem(elem.findAllElemOrSelfPaths.reverse.foldLeft(elem) { (acc, path) => acc.updated(path)(f) })
 * }}}
 *
 * This property can be proven by structural induction as follows:
 *
 * __Base case__
 *
 * If `elem` has no child elements, then the LHS can be rewritten as follows (modulo resolved.Elem equality):
 * {{{
 * elem.replaceAllElemsOrSelf(f)
 * f(elem.replaceAllChildElems (e => e.replaceAllElemsOrSelf(f))) // by definition
 * f(elem) // there are no child elements
 * elem.updated(ElemPath.Root)(f) // by definition
 * Vector(ElemPath.Root).reverse.foldLeft(elem) { (acc, path) => acc.updated(path)(f) } // by definition of foldLeft
 * elem.findAllElemOrSelfPaths.reverse.foldLeft(elem) { (acc, path) => acc.updated(path)(f) } // there are no child elements
 * }}}
 * which is the RHS.
 *
 * __Inductive step__
 *
 * If `elem` does have child elements, the LHS can be rewritten as:
 * {{{
 * elem.replaceAllElemsOrSelf(f)
 * f(elem.replaceAllChildElems (e => e.replaceAllElemsOrSelf(f))) // by definition
 *
 * // property about replaceAllChildElems in terms of updated
 * f(elem.findAllChildElemPathEntries.reverse.foldLeft(elem) { case (acc, pathEntry) =>
 *   acc.updated(pathEntry) { che => che.replaceAllElemsOrSelf(f) }
 * })
 *
 * // induction hypothesis
 * f(elem.findAllChildElemPathEntries.reverse.foldLeft(elem) { case (acc, pathEntry) =>
 *   acc.updated(pathEntry) { che => che.findAllElemOrSelfPaths.reverse.foldLeft(acc) { (acc2, path) => acc2.updated(path)(f) } }
 * })
 *
 * // put differently, keeping order in mind
 * f(elem.findAllElemPaths.reverse.foldLeft(elem) { case (acc, path) =>
 *   acc.updated(path.firstEntry) { che => che.updated(path.withoutFirstEntry)(f) }
 * })
 *
 * // recursive definition of updated
 * f(elem.findAllElemPaths.reverse.foldleft(elem) { case (acc, path) => acc.updated(path)(f) })
 *
 * // by definition of findAllElemOrSelfPaths, and because elem.updated(ElemPath.Root)(f) returns f(elem)
 * elem.findAllElemOrSelfPaths.reverse.foldLeft(elem) { case (acc, path) => acc.updated(path)(f) }
 * }}}
 * which is the RHS.
 *
 * This completes the proof.
 *
 * It easily follows that the following property about '''replaceAllElems in terms of updated''' holds:
 * {{{
 * resolved.Elem(elem.replaceAllElems(f)) ==
 *   resolved.Elem(elem.findAllElemPaths.reverse.foldLeft(elem) { (acc, path) => acc.updated(path)(f) })
 * }}}
 *
 * Another property is about '''replaceAllElemsByNodeSeq in terms of replaceAllElemsOrSelf''':
 * {{{
 * resolved.Elem(elem.replaceAllElemsByNodeSeq(f)) ==
 *   resolved.Elem(elem.replaceAllElemsOrSelf { e => e.replaceAllChildElemsByNodeSeq(che => f(che)) })
 * }}}
 *
 * First, define function `g` as follows:
 * {{{
 * def g(e: Elem): Elem = e.replaceAllChildElemsByNodeSeq(che => f(che))
 * }}}
 *
 * This property can be proven by structural induction as follows:
 *
 * __Base case__
 *
 * If `elem` has no child elements, then the LHS can be rewritten as follows (modulo resolved.Elem equality):
 * {{{
 * elem.replaceAllElemsByNodeSeq(f)
 * elem.replaceAllChildElemsByNodeSeq(e => e.replaceAllElemsOrSelfByNodeSeq(f)) // by definition
 * elem // there are no child elements
 * elem.replaceAllElems(g) // there are no child elements
 * g(elem.replaceAllElems(g)) // g(elem) is the same as elem, since there are no child elements
 * elem.replaceAllElemsOrSelf(g)
 * }}}
 * which is the RHS.
 *
 * __Inductive step__
 *
 * If `elem` does have child elements, the LHS can be rewritten as:
 * {{{
 * elem.replaceAllElemsByNodeSeq(f)
 * elem.replaceAllChildElemsByNodeSeq(e => e.replaceAllElemsOrSelfByNodeSeq(f)) // by definition
 * elem.replaceAllChildElemsByNodeSeq(e => f(e.replaceAllElemsByNodeSeq(f))) // by definition of replaceAllElemsOrSelfByNodeSeq
 * elem.replaceAllChildElemsByNodeSeq(e => f(e.replaceAllElemsOrSelf(g))) // induction hypothesis
 * g(elem.replaceAllChildElems(e => e.replaceAllElemsOrSelf(g))) // definition of g
 * g(elem.replaceAllElems(g)) // definition of replaceAllElems
 * elem.replaceAllElemsOrSelf(g) // definition of replaceAllElemsOrSelf
 * }}}
 * which is the RHS.
 *
 * This completes the proof.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait TransformableElemApi[N, E <: N with TransformableElemApi[N, E]] { self: E =>

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   *
   * That is, returns the equivalent of:
   * {{{
   * val newChildren =
   *   children map {
   *     case e: E => f(e)
   *     case n: N => n
   *   }
   * withChildren(newChildren)
   * }}}
   */
  def replaceAllChildElems(f: E => E): E

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   *
   * That is, returns the equivalent of:
   * {{{
   * val newChildren =
   *   children flatMap {
   *     case e: E => f(e)
   *     case n: N => Vector(n)
   *   }
   * withChildren(newChildren)
   * }}}
   */
  def replaceAllChildElemsByNodeSeq(f: E => immutable.IndexedSeq[N]): E

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(replaceAllChildElems (e => e.replaceAllElemsOrSelf(f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(replaceAllElems(f))
   * }}}
   */
  def replaceAllElemsOrSelf(f: E => E): E

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, in a bottom-up manner,
   * passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(replaceAllChildElems(e => e.replaceAllElemsOrSelf(f, (ancestry :+ self))), ancestry)
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(replaceAllElems(f, ancestry), ancestry)
   * }}}
   */
  def replaceAllElemsOrSelf(f: (E, immutable.IndexedSeq[E]) => E, ancestry: immutable.IndexedSeq[E]): E

  /**
   * Transforms the element by applying the given function to all its descendant elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * replaceAllChildElems (e => e.replaceAllElemsOrSelf(f))
   * }}}
   */
  def replaceAllElems(f: E => E): E

  /**
   * Transforms the element by applying the given function to all its descendant elements, in a bottom-up manner,
   * passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   *
   * That is, returns the equivalent of:
   * {{{
   * replaceAllChildElems(e => e.replaceAllElemsOrSelf(f, (ancestry :+ self)))
   * }}}
   */
  def replaceAllElems(f: (E, immutable.IndexedSeq[E]) => E, ancestry: immutable.IndexedSeq[E]): E

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant-or-self elements,
   * in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(replaceAllChildElemsByNodeSeq(e => e.replaceAllElemsOrSelfByNodeSeq(f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(replaceAllElemsByNodeSeq(f))
   * }}}
   */
  def replaceAllElemsOrSelfByNodeSeq(f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N]

  /**
   * Transforms the element to a node sequence by applying the given function to all its descendant-or-self elements,
   * in a bottom-up manner, passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(replaceAllChildElemsByNodeSeq(e => e.replaceAllElemsOrSelfByNodeSeq(f, (ancestry :+ self))), ancestry)
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(replaceAllElemsByNodeSeq(f, ancestry), ancestry)
   * }}}
   */
  def replaceAllElemsOrSelfByNodeSeq(
    f: (E, immutable.IndexedSeq[E]) => immutable.IndexedSeq[N],
    ancestry: immutable.IndexedSeq[E]): immutable.IndexedSeq[N]

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant elements,
   * in a bottom-up manner. The function is not applied to this element itself.
   *
   * That is, returns the equivalent of:
   * {{{
   * replaceAllChildElemsByNodeSeq(e => e.replaceAllElemsOrSelfByNodeSeq(f))
   * }}}
   *
   * It is equivalent to the following expression:
   * {{{
   * replaceAllElemsOrSelf { e => e.replaceAllChildElemsByNodeSeq(che => f(che)) }
   * }}}
   */
  def replaceAllElemsByNodeSeq(f: E => immutable.IndexedSeq[N]): E

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant elements,
   * in a bottom-up manner, passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   * The function is not applied to this element itself.
   *
   * That is, returns the equivalent of:
   * {{{
   * replaceAllChildElemsByNodeSeq(e => e.replaceAllElemsOrSelfByNodeSeq(f, (ancestry :+ self)))
   * }}}
   */
  def replaceAllElemsByNodeSeq(
    f: (E, immutable.IndexedSeq[E]) => immutable.IndexedSeq[N],
    ancestry: immutable.IndexedSeq[E]): E
}
