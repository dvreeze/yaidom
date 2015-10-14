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
 * @author Chris de Vreeze
 */
final class ElemWithPath[E <: ClarkElemApi[E]](val elm: E, val path: Path) extends ElemLike[ElemWithPath[E]] {

  final override def findAllChildElems: immutable.IndexedSeq[ElemWithPath[E]] = {
    elm.findAllChildElemsWithPathEntries map {
      case (che, pathEntry) =>
        new ElemWithPath[E](che, path.append(pathEntry))
    }
  }
}

object ElemWithPath {

  def apply[E <: ClarkElemApi[E]](elm: E, path: Path): ElemWithPath[E] = new ElemWithPath[E](elm, path)

  def apply[E <: ClarkElemApi[E]](elm: E): ElemWithPath[E] = new ElemWithPath[E](elm, Path.Root)
}
