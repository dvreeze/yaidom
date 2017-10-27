/*
 * Copyright 2011-2017 Chris de Vreeze
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
 * This is the partially implemented element transformation API, as function API instead of OO API. That is, this is the function
 * API corresponding to trait [[eu.cdevreeze.yaidom.queryapi.TransformableElemLike]].
 *
 * In other words, this trait has abstract methods `transformChildElems` and `transformChildElemsToNodeSeq`. Based on these
 * abstract methods, this trait offers a rich API for transforming descendant elements or descendant-or-self elements.
 *
 * @author Chris de Vreeze
 */
trait ElemTransformationLike extends ElemTransformationApi {

  type Node

  type Elem <: Node

  def transformChildElems(elem: Elem, f: Elem => Elem): Elem

  def transformChildElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Elem

  final def transformElemsOrSelf(elem: Elem, f: Elem => Elem): Elem = {
    f(transformChildElems(elem, e => transformElemsOrSelf(e, f)))
  }

  final def transformElems(elem: Elem, f: Elem => Elem): Elem = {
    transformChildElems(elem, e => transformElemsOrSelf(e, f))
  }

  final def transformElemsOrSelfToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): immutable.IndexedSeq[Node] = {
    f(transformChildElemsToNodeSeq(elem, e => transformElemsOrSelfToNodeSeq(e, f)))
  }

  final def transformElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Elem = {
    transformChildElemsToNodeSeq(elem, e => transformElemsOrSelfToNodeSeq(e, f))
  }
}

object ElemTransformationLike {

  /**
   * This query API type, restricting Node and Elem to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemTransformationLike { type Node = N; type Elem = E }
}
