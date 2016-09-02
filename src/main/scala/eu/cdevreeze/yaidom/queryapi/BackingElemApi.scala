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

  override def thisElem: ThisElem

  // Restricting ElemApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  override def findAllChildElems: immutable.IndexedSeq[ThisElem]

  override def findAllElems: immutable.IndexedSeq[ThisElem]

  override def findAllElemsOrSelf: immutable.IndexedSeq[ThisElem]

  override def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  override def filterElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  override def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  override def findChildElem(p: ThisElem => Boolean): Option[ThisElem]

  override def findElem(p: ThisElem => Boolean): Option[ThisElem]

  override def findElemOrSelf(p: ThisElem => Boolean): Option[ThisElem]

  override def findTopmostElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  override def findTopmostElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  override def getChildElem(p: ThisElem => Boolean): ThisElem

  override def \(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  override def \\(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  override def \\!(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

  // Restricting IsNavigableApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  override def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(ThisElem, Path.Entry)]

  override def findChildElemByPathEntry(entry: Path.Entry): Option[ThisElem]

  override def getChildElemByPathEntry(entry: Path.Entry): ThisElem

  override def findElemOrSelfByPath(path: Path): Option[ThisElem]

  override def getElemOrSelfByPath(path: Path): ThisElem

  override def findReverseAncestryOrSelfByPath(path: Path): Option[immutable.IndexedSeq[ThisElem]]

  override def getReverseAncestryOrSelfByPath(path: Path): immutable.IndexedSeq[ThisElem]

  // Restricting IndexedClarkElemApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  override def rootElem: ThisElem

  override def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem]

  override def reverseAncestry: immutable.IndexedSeq[ThisElem]

  // Restricting HasParentApi methods to use "this ThisElem", to prevent down-casts in code using this "raw" query API trait

  override def parentOption: Option[ThisElem]

  override def parent: ThisElem

  override def ancestorsOrSelf: immutable.IndexedSeq[ThisElem]

  override def ancestors: immutable.IndexedSeq[ThisElem]

  override def findAncestorOrSelf(p: ThisElem => Boolean): Option[ThisElem]

  override def findAncestor(p: ThisElem => Boolean): Option[ThisElem]
}

object BackingElemApi {

  /**
   * This query API type, restricting ThisElem and ThisElemApi to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = BackingElemApi { type ThisElem = E; type ThisElemApi = E }
}
