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
 * This is the element transformation API, as function API instead of OO API. That is, this is the function API corresponding to
 * trait [[eu.cdevreeze.yaidom.queryapi.TransformableElemApi]].
 *
 * See trait `TransformableElemApi` for more info about element transformations in yaidom, and their properties.
 *
 * This functional API is more widely applicable than trait `TransformableElemApi`. First, it can be implemented for arbitrary
 * element types, even non-yaidom ones. Second, implementations can easily carry state that is shared by update functions, such
 * as a Saxon `Processor` in the case of a Saxon implementation of this API.
 *
 * When using this API for elements that carry context such as "ancestry state", be careful when writing transformation functions
 * that are passed to the functions of this API. For example, if the element type is `BackingElemApi` or a sub-type, such sensitive
 * state includes the base URI, document URI, the `Path` relative to the root element, and most important of all, the root element itself.
 * During transformations, the `Path` of an element is very volatile, so depending on the `Path` in a transformation function may
 * affect the result in unexpected ways.
 *
 * Also note for `BackingElemApi` elements, if a transformation function alters "ancestry state" such as (base and document) URIs,
 * paths etc., these altered values will be ignored. After all, it is the functions of this API that will transform the entire underlying
 * root element tree, and that will determine the `Path` and (document and base) URI of each element after the transformation.
 *
 * @author Chris de Vreeze
 */
trait ElemTransformationApi {

  type Node

  type Elem <: Node

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   */
  def transformChildElems(elem: Elem, f: Elem => Elem): Elem

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   */
  def transformChildElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Elem

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(transformChildElems(elem, e => transformElemsOrSelf(e, f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElems(elem, f))
   * }}}
   */
  def transformElemsOrSelf(elem: Elem, f: Elem => Elem): Elem

  /**
   * Transforms the element by applying the given function to all its descendant elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * transformChildElems(elem, e => transformElemsOrSelf(e, f))
   * }}}
   */
  def transformElems(elem: Elem, f: Elem => Elem): Elem

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant-or-self elements,
   * in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(transformChildElemsToNodeSeq(elem, e => transformElemsOrSelfToNodeSeq(e, f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElemsToNodeSeq(elem, f))
   * }}}
   */
  def transformElemsOrSelfToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): immutable.IndexedSeq[Node]

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant elements,
   * in a bottom-up manner. The function is not applied to this element itself.
   *
   * That is, returns the equivalent of:
   * {{{
   * transformChildElemsToNodeSeq(elem, e => transformElemsOrSelfToNodeSeq(e, f))
   * }}}
   *
   * It is equivalent to the following expression:
   * {{{
   * transformElemsOrSelf(elem, { e => transformChildElemsToNodeSeq(e, f) })
   * }}}
   */
  def transformElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Elem
}

object ElemTransformationApi {

  /**
   * This query API type, restricting Node and Elem to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemTransformationApi { type Node = N; type Elem = E }
}
