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
 * It is up to the user of the API to keep such state consistent during transformations, and to be careful when depending on state
 * that is volatile during transformations.
 *
 * Also note for `BackingElemApi` elements, if a transformation function alters "ancestry state" such as (base and document) URIs,
 * paths etc., these altered values may be ignored, depending on the API calls made.
 *
 * ==ElemTransformationApi more formally==
 *
 * '''In order to get started using the API, this more formal section can safely be skipped. On the other hand, this section
 * may provide a deeper understanding of the API.'''
 *
 * Some provable properties hold about this `ElemTransformationApi` API in terms of the more low level `ElemUpdateApi` API.
 *
 * Let's first try to define the methods of `ElemTransformationApi` in terms of the `ElemUpdateApi` API. Below their equivalence
 * will be proven. We define the following, assuming type `ElemType` to be a yaidom "indexed element" type:
 *
 * {{{
 * def addPathParameter[A](f: ElemType => A): ((ElemType, Path) => A) = {
 *   { (elm, path) => f(elm) } // Unused path
 * }
 *
 * def addPathEntryParameter[A](f: ElemType => A): ((ElemType, Path.Entry) => A) = {
 *   { (elm, pathEntry) => f(elm) } // Unused path entry
 * }
 *
 * def findAllChildPathEntries(elem: ElemType): Set[Path.Entry] = {
 *   elem.findAllChildElems.map(_.path.lastEntry).toSet
 * }
 *
 * def findAllRelativeElemOrSelfPaths(elem: ElemType): Set[Path] = {
 *   elem.findAllElemsOrSelf.map(_.path.skippingPath(elem.path)).toSet
 * }
 *
 * def findAllRelativeElemPaths(elem: ElemType): Set[Path] = {
 *   elem.findAllElems.map(_.path.skippingPath(elem.path)).toSet
 * }
 *
 * // The transformation functions, defined in terms of the ElemUpdateApi
 *
 * def transformChildElems2(elem: ElemType, f: ElemType => ElemType): ElemType = {
 *   updateChildElems(elem, findAllChildPathEntries(elem))(addPathEntryParameter(f))
 * }
 *
 * def transformElemsOrSelf2(elem: ElemType, f: ElemType => ElemType): ElemType = {
 *   updateElemsOrSelf(elem, findAllRelativeElemOrSelfPaths(elem))(addPathParameter(f))
 * }
 *
 * def transformElems2(elem: ElemType, f: ElemType => ElemType): ElemType = {
 *   updateElems(elem, findAllRelativeElemPaths(elem))(addPathParameter(f))
 * }
 * }}}
 *
 * ===1. Property about transformChildElems in terms of transformChildElems2===
 *
 * The following property must hold, for all elements and (pure) element transformation functions:
 * {{{
 * transformChildElems(elem, f) == transformChildElems2(elem, f)
 * }}}
 *
 * No proof is provided, but this property must obviously hold, since `transformChildElems` replaces
 * child element nodes by applying the given function, and leaves the other child nodes alone, and
 * method `transformChildElems2` does the same. The latter function does it via child path entries
 * (translated to child node indexes), iterating over child nodes in reverse order (in order not
 * to invalidate the next processed path entry), but the net effect is the same.
 *
 * ===2. Property about transformElemsOrSelf in terms of transformElemsOrSelf2===
 *
 * The following property holds, for all elements and (pure) element transformation functions:
 * {{{
 * transformElemsOrSelf(elem, f) == transformElemsOrSelf2(elem, f)
 * }}}
 *
 * Below follows a proof of this property by structural induction.
 *
 * __Base case__
 *
 * If `elem` has no child elements, then the LHS can be rewritten as follows:
 * {{{
 * transformElemsOrSelf(elem, f)
 * f(transformChildElems(elem, e => transformElemsOrSelf(e, f))) // definition of transformElemsOrSelf
 * f(elem) // there are no child element nodes, so transformChildElems is an identity function in this case
 * updateElemsOrSelf(elem, Set(Path.Empty))(addPathParameter(f)) // only updates elem
 * transformElemsOrSelf2(elem, f) // definition of transformElemsOrSelf2, and absence of descendant paths
 * }}}
 * which is the RHS.
 *
 * __Inductive step__
 *
 * If `elem` does have child elements, the LHS can be rewritten as:
 * {{{
 * transformElemsOrSelf(elem, f)
 * f(transformChildElems(elem, e => transformElemsOrSelf(e, f))) // definition of transformElemsOrSelf
 * f(transformChildElems(elem, e => transformElemsOrSelf2(e, f))) // induction hypothesis
 * f(transformChildElems2(elem, e => transformElemsOrSelf2(e, f))) // property above
 * f(transformChildElems2(elem, e => updateElemsOrSelf(e, findAllRelativeElemOrSelfPaths(e))(addPathParameter(f))))
 *   // definition of transformElemsOrSelf2
 *
 * f(updateChildElems(elem, findAllChildPathEntries(elem))(addPathEntryParameter(
 *   e => updateElemsOrSelf(e, findAllRelativeElemOrSelfPaths(e))(addPathParameter(f))))
 * ) // definition of transformChildElems2
 *
 * f(updateElems(elem, findAllRelativeElemOrSelfPaths(elem))(addPathParameter(f)))
 *   // property about updateElems, and knowing that the added path and path entry parameters do nothing here
 *
 * updateElemsOrSelf(elem, findAllRelativeElemOrSelfPaths(elem))(addPathParameter(f))
 *   // (indirect) definition of updateElemsOrSelf
 * transformElemsOrSelf2(elem, f) // definition of transformElemsOrSelf2
 * }}}
 * which is the RHS.
 *
 * This completes the proof. For the other `ElemTransformationApi` methods, analogous provable properties hold.
 *
 * @author Chris de Vreeze
 */
trait ElemTransformationApi {

  type NodeType

  type ElemType <: NodeType

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   */
  def transformChildElems(elem: ElemType, f: ElemType => ElemType): ElemType

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   */
  def transformChildElemsToNodeSeq(elem: ElemType, f: ElemType => immutable.IndexedSeq[NodeType]): ElemType

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
  def transformElemsOrSelf(elem: ElemType, f: ElemType => ElemType): ElemType

  /**
   * Transforms the element by applying the given function to all its descendant elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * transformChildElems(elem, e => transformElemsOrSelf(e, f))
   * }}}
   */
  def transformElems(elem: ElemType, f: ElemType => ElemType): ElemType

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
  def transformElemsOrSelfToNodeSeq(elem: ElemType, f: ElemType => immutable.IndexedSeq[NodeType]): immutable.IndexedSeq[NodeType]

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
  def transformElemsToNodeSeq(elem: ElemType, f: ElemType => immutable.IndexedSeq[NodeType]): ElemType
}

object ElemTransformationApi {

  /**
   * This query API type, restricting NodeType and ElemType to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemTransformationApi { type NodeType = N; type ElemType = E }
}
