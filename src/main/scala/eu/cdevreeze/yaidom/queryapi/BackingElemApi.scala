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

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * Shorthand for `IndexedScopedElemApi with HasParentApi`. In other words, this is an ancestry-aware "scoped element"
 * query API.
 *
 * Efficient implementations are possible for indexed elements and Saxon NodeInfo objects (backed by native tiny trees).
 * Saxon-backed elements are not offered by core yaidom, however. Saxon tiny trees are attractive for their low memory
 * footprint.
 *
 * It is possible to offer implementations by combining the partial implementation traits (XXXLike), or by entirely
 * custom and efficient "backend-aware" implementations.
 *
 * @author Chris de Vreeze
 */
trait BackingElemApi extends IndexedScopedElemApi with HasParentApi {

  type ThisElemApi <: BackingElemApi

  // Restricting AnyElemApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  def thisElem: ThisElem

  // Restricting ElemApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  def findAllChildElems: immutable.IndexedSeq[ThisElem]

  def findAllElems: immutable.IndexedSeq[ThisElem]

  def findAllElemsOrSelf: immutable.IndexedSeq[ThisElem]

  def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  def filterElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  def findChildElem(p: ThisElem => Boolean): Option[ThisElem]

  def findElem(p: ThisElem => Boolean): Option[ThisElem]

  def findElemOrSelf(p: ThisElem => Boolean): Option[ThisElem]

  def findTopmostElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  def findTopmostElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  def getChildElem(p: ThisElem => Boolean): ThisElem

  def \(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  def \\(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  def \\!(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  // Restricting IsNavigableApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(ThisElem, Path.Entry)]

  def findChildElemByPathEntry(entry: Path.Entry): Option[ThisElem]

  def getChildElemByPathEntry(entry: Path.Entry): ThisElem

  def findElemOrSelfByPath(path: Path): Option[ThisElem]

  def getElemOrSelfByPath(path: Path): ThisElem

  def findReverseAncestryOrSelfByPath(path: Path): Option[immutable.IndexedSeq[ThisElem]]

  def getReverseAncestryOrSelfByPath(path: Path): immutable.IndexedSeq[ThisElem]

  // Restricting IndexedClarkElemApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem]

  def reverseAncestry: immutable.IndexedSeq[ThisElem]

  // Restricting HasParentApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  def parentOption: Option[ThisElem]

  def parent: ThisElem

  def ancestorsOrSelf: immutable.IndexedSeq[ThisElem]

  def ancestors: immutable.IndexedSeq[ThisElem]

  def findAncestorOrSelf(p: ThisElem => Boolean): Option[ThisElem]

  def findAncestor(p: ThisElem => Boolean): Option[ThisElem]
}

object BackingElemApi {

  /**
   * This query API type, restricting ThisElem and ThisElemApi to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = BackingElemApi { type ThisElem = E; type ThisElemApi = E }
}
