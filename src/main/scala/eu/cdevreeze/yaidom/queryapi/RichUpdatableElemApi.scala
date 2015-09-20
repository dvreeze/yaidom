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

import eu.cdevreeze.yaidom.core.Path

/**
 * This is an easy to use <em>functional update</em> part of the yaidom <em>uniform query API</em>, leveraging
 * the ``UpdatableElemApi`` and ``ClarkElemApi`` super-traits.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait RichUpdatableElemApi[N, E <: N with RichUpdatableElemApi[N, E]] extends UpdatableElemApi[N, E] with ClarkElemApi[E] { self: E =>

  /**
   * Functionally updates the child elements for which the passed function returns a non-empty result.
   */
  def withUpdatedChildElems(f: (E, Path.Entry) => Option[immutable.IndexedSeq[N]]): E

  /**
   * Functionally updates the descendant elements for which the passed function returns a non-empty result.
   * Note that the passed function is not applied to this element itself!
   */
  def withUpdatedElems(f: (E, Path) => Option[immutable.IndexedSeq[N]]): E
}
