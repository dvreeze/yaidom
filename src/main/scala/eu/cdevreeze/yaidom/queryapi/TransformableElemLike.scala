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

/**
 * API and implementation trait for transformable elements.
 *
 * More precisely, this trait has abstract methods `transformChildElems` and `transformChildElemsToNodeSeq`. Based on these
 * abstract methods, this trait offers a rich API for transforming descendant elements or descendant-or-self elements.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.TransformableElemApi]]. See the documentation of that trait
 * for examples of usage.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait TransformableElemLike[N, E <: N with TransformableElemLike[N, E]] extends TransformableElemApi[N, E] { self: E =>

  def transformChildElems(f: E => E): E

  def transformChildElemsToNodeSeq(f: E => immutable.IndexedSeq[N]): E

  final def transformElemsOrSelf(f: E => E): E = {
    f(transformChildElems(e => e.transformElemsOrSelf(f)))
  }

  final def transformElems(f: E => E): E =
    transformChildElems(e => e.transformElemsOrSelf(f))

  final def transformElemsOrSelfToNodeSeq(f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N] = {
    f(transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f)))
  }

  final def transformElemsToNodeSeq(f: E => immutable.IndexedSeq[N]): E = {
    transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f))
  }
}
