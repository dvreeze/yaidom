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
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.TransformableElemApi]]. See the documentation of that trait
 * for examples of usage.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait TransformableElemLike[N, E <: N with TransformableElemLike[N, E]] extends TransformableElemApi[N, E] { self: E =>

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding potential type class.
  // Yet I did not want to depend on a val or def returning the appropriate type class instance, so chose for code repetition.

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

object TransformableElemLike {

  /**
   * The `TransformableElemLike` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[N, E <: N] extends TransformableElemApi.FunctionApi[N, E] {

    def transformChildElems(thisElem: E, f: E => E): E

    def transformChildElemsToNodeSeq(thisElem: E, f: E => immutable.IndexedSeq[N]): E

    final def transformElemsOrSelf(thisElem: E, f: E => E): E = {
      f(transformChildElems(thisElem, e => transformElemsOrSelf(e, f)))
    }

    final def transformElems(thisElem: E, f: E => E): E =
      transformChildElems(thisElem, e => transformElemsOrSelf(e, f))

    final def transformElemsOrSelfToNodeSeq(thisElem: E, f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N] = {
      f(transformChildElemsToNodeSeq(thisElem, e => transformElemsOrSelfToNodeSeq(e, f)))
    }

    final def transformElemsToNodeSeq(thisElem: E, f: E => immutable.IndexedSeq[N]): E = {
      transformChildElemsToNodeSeq(thisElem, e => transformElemsOrSelfToNodeSeq(e, f))
    }
  }
}
