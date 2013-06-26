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
 * about applying some transforming function to all descendant-or-self elements, while "(functional) updates" are mainly about
 * "updates" at given element paths (either explicitly, by passing the path, or implicitly, by passing a partial function
 * from elements to elements/node collections).
 *
 * In spite of these differences, "updates" can be understood in terms of equivalent "transformations". Concerning performance,
 * as a rule of thumb it is best to prefer "updates" (`UpdatableElemApi`) if only relatively few element paths are involved,
 * and to prefer "transformations" (`TransformableElemApi`) otherwise.
 *
 * This purely abstract API leaves the implementation completely open.
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
   * Transforms the element by applying the given function to all its descendant-or-self elements.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withMappedChildElems (e => e.transform(f)))
   * }}}
   */
  def transform(f: E => E): E

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, passing its ancestors
   * (as sequence of elements, starting with the "root" element) as well.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withMappedChildElems(e => e.transform(f, (ancestry :+ self))), ancestry)
   * }}}
   */
  def transform(f: (E, immutable.IndexedSeq[E]) => E, ancestry: immutable.IndexedSeq[E]): E

  /**
   * Transforms the element to a node sequence by applying the given function to all its descendant-or-self elements.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withFlatMappedChildElems (e => e.transformToNodeSeq(f)))
   * }}}
   */
  def transformToNodeSeq(f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N]

  /**
   * Transforms the element to a node sequence by applying the given function to all its descendant-or-self elements,
   * passing its ancestors (as sequence of elements, starting with the "root" element) as well.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(withFlatMappedChildElems(e => e.transformToNodeSeq(f, (ancestry :+ self))), ancestry)
   * }}}
   */
  def transformToNodeSeq(
    f: (E, immutable.IndexedSeq[E]) => immutable.IndexedSeq[N],
    ancestry: immutable.IndexedSeq[E]): immutable.IndexedSeq[N]
}
