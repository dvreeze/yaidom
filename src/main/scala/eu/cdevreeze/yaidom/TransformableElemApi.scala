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
 * This purely abstract API leaves the implementation completely open.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait TransformableElemApi[E <: TransformableElemApi[E]] { self: E =>

  /**
   * Returns the equivalent of:
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
   * This function could be defined as:
   * {{{
   * f(withMappedChildElems (e => e.transform(f)))
   * }}}
   */
  def transform(f: E => E): E
}
