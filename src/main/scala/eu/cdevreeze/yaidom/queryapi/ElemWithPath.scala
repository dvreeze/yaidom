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
 * Pair of an element and a Path. These pairs themselves offer the ElemApi query API, so they can be seen as
 * "element implementations" themselves. They are like very light-weight "indexed" elements.
 *
 * These "elements" are used in the implementation of bulk update methods in trait ``UpdatableElemLike``, but they
 * can also be used in application code.
 *
 * Note that this class renders a separate query API for element-path pairs obsolete. It takes a `IsNavigableApi`, using
 * its `findAllChildElemsWithPathEntries` method, and offers the equivalent of an `ElemApi` for element-path pairs.
 *
 * @author Chris de Vreeze
 */
final class ElemWithPath[E <: IsNavigableApi[E]](val elem: E, val path: Path) extends ElemLike[ElemWithPath[E]] {

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding potential type class.
  // Yet I did not want to depend on a val or def returning the appropriate type class instance, so chose for code repetition.

  final override def findAllChildElems: immutable.IndexedSeq[ElemWithPath[E]] = {
    elem.findAllChildElemsWithPathEntries map {
      case (che, pathEntry) =>
        new ElemWithPath[E](che, path.append(pathEntry))
    }
  }
}

object ElemWithPath {

  def apply[E <: IsNavigableApi[E]](elem: E, path: Path): ElemWithPath[E] = new ElemWithPath[E](elem, path)

  def apply[E <: IsNavigableApi[E]](elem: E): ElemWithPath[E] = new ElemWithPath[E](elem, Path.Empty)

  /**
   * The `ElemWithPath` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] extends ElemLike.FunctionApi[(E, Path)] {

    def underlyingElementFunctionApi: IsNavigableApi.FunctionApi[E]

    final override def findAllChildElems(thisElem: (E, Path)): immutable.IndexedSeq[(E, Path)] = {
      underlyingElementFunctionApi.findAllChildElemsWithPathEntries(thisElem._1) map {
        case (che, pathEntry) =>
          (che, thisElem._2.append(pathEntry))
      }
    }
  }
}
