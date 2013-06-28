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
 * "Transformable" element. It defines a contract for transformations, applying an element transforming function to all elements in
 * an element tree. See [[eu.cdevreeze.yaidom.TransformableElemLike]].
 *
 * The big conceptual difference with "updatable" elements (in trait `UpdatableElemLike[N, E]`) is that "transformations" are
 * about applying some transforming function to all descendant-or-self elements, while "(functional) updates" are about
 * "updates" at a given element path. So "transformations" are bulk updates, not using element paths, and "(functional) updates"
 * are single element updates, at a given element path.
 *
 * In spite of these differences, "updates" can be understood in terms of equivalent "transformations". Concerning performance,
 * as a rule of thumb it is best to prefer "updates" (`UpdatableElemApi`) if only relatively few element paths are involved,
 * and to prefer "transformations" (`TransformableElemApi`) otherwise.
 *
 * This purely abstract API leaves the implementation completely open.
 *
 * Note that in a way this API can be seen as the "update" counterpart of query API `ParentElemApi`, in that the 3 "axes"
 * of child elements, descendant elements, and descendant-or-self elements can be recognized.
 *
 * ==TransformableElemApi more formally==
 *
 * The `TransformableElemApi` trait obeys an interesting property about `transform` in terms of `UpdatableElemApi.updated`.
 *
 * That is:
 * {{{
 * resolved.Elem(elem.transform(f)) ==
 *   resolved.Elem(elem.findAllElemOrSelfPaths.reverse.foldLeft(elem) { (acc, path) => acc.updated(path)(f) })
 * }}}
 *
 * This property can be proven by structural induction as follows:
 *
 * __Base case__
 *
 * If `elem` has no child elements, then the LHS can be rewritten as follows (modulo resolved.Elem equality):
 * {{{
 * elem.transform(f)
 * f(elem.withMappedChildElems (e => e.transform(f))) // by definition
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
 * elem.transform(f)
 * f(elem.withMappedChildElems (e => e.transform(f))) // by definition
 *
 * // induction hypothesis
 * f(elem.withMappedChildElems { e =>
 *   e.findAllElemOrSelfPaths.reverse.foldLeft(e) { (acc, path) => acc.updated(path)(f) }
 * })
 *
 * // by definition of withMappedChildElems
 * f(elem.withChildren(elem.children map {
 *   case e: Elem => e.findAllElemOrSelfPaths.reverse.foldLeft(e) { (acc, path) => acc.updated(path)(f) }
 *   case n: Node => n
 * }))
 *
 * // rewriting the functional update of the children, and reversing (without affecting the result)
 * f(elem.findAllChildElemPathEntries.reverse.foldLeft(elem) { case (acc, pathEntry) =>
 *   acc.updated(pathEntry) { che =>
 *     che.findAllElemOrSelfPaths.reverse.foldLeft(che) { (acc2, path) => acc2.updated(path)(f) }
 *   }
 * })
 *
 * // by definition of updated (minding the order of functional updates)
 * f(elem.findAllElemPaths.reverse.foldLeft(elem) { case (acc, path) => acc.updated(path)(f) })
 *
 * // by definition of findAllElemOrSelfPaths, and because elem.updated(ElemPath.Root)(f) returns f(elem)
 * elem.findAllElemOrSelfPaths.reverse.foldLeft(elem) { case (acc, path) => acc.updated(path)(f) }
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
  def withMappedChildElems(f: E => E): E

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
  def withFlatMappedChildElems(f: E => immutable.IndexedSeq[N]): E

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withMappedChildElems (e => e.transform(f)))
   * }}}
   */
  def transform(f: E => E): E

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, in a bottom-up manner,
   * passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withMappedChildElems(e => e.transform(f, (ancestry :+ self))), ancestry)
   * }}}
   */
  def transform(f: (E, immutable.IndexedSeq[E]) => E, ancestry: immutable.IndexedSeq[E]): E

  /**
   * Transforms the element to a node sequence by applying the given function to all its descendant-or-self elements,
   * in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withFlatMappedChildElems (e => e.transformToNodeSeq(f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElemsToNodeSeq(f))
   * }}}
   */
  def transformToNodeSeq(f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N]

  /**
   * Transforms the element to a node sequence by applying the given function to all its descendant-or-self elements,
   * in a bottom-up manner, passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withFlatMappedChildElems(e => e.transformToNodeSeq(f, (ancestry :+ self))), ancestry)
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElemsToNodeSeq(f, ancestry), ancestry)
   * }}}
   */
  def transformToNodeSeq(
    f: (E, immutable.IndexedSeq[E]) => immutable.IndexedSeq[N],
    ancestry: immutable.IndexedSeq[E]): immutable.IndexedSeq[N]

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant elements,
   * in a bottom-up manner. The function is not applied to this element itself.
   *
   * That is, returns the equivalent of:
   * {{{
   * withFlatMappedChildElems(e => e.transformToNodeSeq(f))
   * }}}
   */
  def transformElemsToNodeSeq(f: E => immutable.IndexedSeq[N]): E

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant elements,
   * in a bottom-up manner, passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   * The function is not applied to this element itself.
   *
   * That is, returns the equivalent of:
   * {{{
   * withFlatMappedChildElems(e => e.transformToNodeSeq(f, (ancestry :+ self)))
   * }}}
   */
  def transformElemsToNodeSeq(
    f: (E, immutable.IndexedSeq[E]) => immutable.IndexedSeq[N],
    ancestry: immutable.IndexedSeq[E]): E
}
