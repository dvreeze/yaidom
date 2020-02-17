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
 * All methods are overridable. Hence element implementations mixing in this partial implementation trait can change the
 * implementation without breaking its API, caused by otherwise needed removal of this mixin. Arguably this trait should not
 * exist as part of the public API, because implementation details should not be part of the public API. Such implementation details
 * may be subtle, such as the (runtime) boundary on the ElemType type member.
 *
 * @author Chris de Vreeze
 */
trait ElemTransformationLike extends ElemTransformationApi {

  type NodeType

  type ElemType <: NodeType

  def transformChildElems(elem: ElemType, f: ElemType => ElemType): ElemType

  def transformChildElemsToNodeSeq(elem: ElemType, f: ElemType => immutable.IndexedSeq[NodeType]): ElemType

  def transformElemsOrSelf(elem: ElemType, f: ElemType => ElemType): ElemType = {
    f(transformChildElems(elem, e => transformElemsOrSelf(e, f)))
  }

  def transformElems(elem: ElemType, f: ElemType => ElemType): ElemType = {
    transformChildElems(elem, e => transformElemsOrSelf(e, f))
  }

  def transformElemsOrSelfToNodeSeq(elem: ElemType, f: ElemType => immutable.IndexedSeq[NodeType]): immutable.IndexedSeq[NodeType] = {
    f(transformChildElemsToNodeSeq(elem, e => transformElemsOrSelfToNodeSeq(e, f)))
  }

  def transformElemsToNodeSeq(elem: ElemType, f: ElemType => immutable.IndexedSeq[NodeType]): ElemType = {
    transformChildElemsToNodeSeq(elem, e => transformElemsOrSelfToNodeSeq(e, f))
  }
}

object ElemTransformationLike {

  /**
   * This query API type, restricting NodeType and ElemType to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemTransformationLike { type NodeType = N; type ElemType = E }
}
