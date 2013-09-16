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
  def transformChildElems(f: E => E): E

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
  def transformChildElemsToNodeSeq(f: E => immutable.IndexedSeq[N]): E

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(transformChildElems (e => e.transformElemsOrSelf(f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElems(f))
   * }}}
   */
  def transformElemsOrSelf(f: E => E): E

  /**
   * Transforms the element by applying the given function to all its descendant elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * transformChildElems (e => e.transformElemsOrSelf(f))
   * }}}
   */
  def transformElems(f: E => E): E

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant-or-self elements,
   * in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElemsToNodeSeq(f))
   * }}}
   */
  def transformElemsOrSelfToNodeSeq(f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N]

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant elements,
   * in a bottom-up manner. The function is not applied to this element itself.
   *
   * That is, returns the equivalent of:
   * {{{
   * transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f))
   * }}}
   *
   * It is equivalent to the following expression:
   * {{{
   * transformElemsOrSelf { e => e.transformChildElemsToNodeSeq(che => f(che)) }
   * }}}
   */
  def transformElemsToNodeSeq(f: E => immutable.IndexedSeq[N]): E
}
