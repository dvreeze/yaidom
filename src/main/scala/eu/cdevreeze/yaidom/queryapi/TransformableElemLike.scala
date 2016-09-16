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
 * @author Chris de Vreeze
 */
trait TransformableElemLike extends TransformableElemApi {

  type ThisElem <: TransformableElemLike.Aux[ThisNode, ThisElem]

  def transformChildElems(f: ThisElem => ThisElem): ThisElem

  def transformChildElemsToNodeSeq(f: ThisElem => immutable.IndexedSeq[ThisNode]): ThisElem

  final def transformElemsOrSelf(f: ThisElem => ThisElem): ThisElem = {
    f(transformChildElems(e => e.transformElemsOrSelf(f)))
  }

  final def transformElems(f: ThisElem => ThisElem): ThisElem =
    transformChildElems(e => e.transformElemsOrSelf(f))

  final def transformElemsOrSelfToNodeSeq(f: ThisElem => immutable.IndexedSeq[ThisNode]): immutable.IndexedSeq[ThisNode] = {
    f(transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f)))
  }

  final def transformElemsToNodeSeq(f: ThisElem => immutable.IndexedSeq[ThisNode]): ThisElem = {
    transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f))
  }
}

object TransformableElemLike {

  /**
   * This query API type, restricting ThisNode and ThisElem to the passed type parameters.
   *
   * @tparam N The node self type
   * @tparam E The element self type
   */
  type Aux[N, E] = TransformableElemLike { type ThisNode = N; type ThisElem = E }
}
