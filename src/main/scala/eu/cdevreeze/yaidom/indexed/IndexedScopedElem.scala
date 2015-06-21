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

package eu.cdevreeze.yaidom.indexed

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi

/**
 * Indexed Scoped element. Like `IndexedClarkElem` but instead of being and indexing
 * a `ClarkElemApi`, it is and indexes a `ScopedElemApi`. Other than that, see the
 * documentation for `IndexedClarkElem`.
 *
 * @tparam E The underlying element type
 *
 * @author Chris de Vreeze
 */
final class IndexedScopedElem[E <: ScopedElemApi[E]] private (
  val rootElem: E,
  childElems: immutable.IndexedSeq[IndexedScopedElem[E]],
  val path: Path,
  val elem: E) extends IndexedScopedElemLike[IndexedScopedElem[E], E] {

  final def findAllChildElems: immutable.IndexedSeq[IndexedScopedElem[E]] = childElems
}

object IndexedScopedElem {

  /**
   * Returns the same as `apply(rootElem, Path.Root)`.
   */
  def apply[E <: ScopedElemApi[E]](rootElem: E): IndexedScopedElem[E] =
    apply(rootElem, Path.Root)

  /**
   * Expensive recursive factory method for "indexed elements".
   */
  def apply[E <: ScopedElemApi[E]](rootElem: E, path: Path): IndexedScopedElem[E] = {
    // Expensive call, so invoked only once
    val elem = rootElem.findElemOrSelfByPath(path).getOrElse(
      sys.error(s"Could not find the element with path $path from root ${rootElem.resolvedName}"))

    apply(rootElem, path, elem)
  }

  private def apply[E <: ScopedElemApi[E]](rootElem: E, path: Path, elem: E): IndexedScopedElem[E] = {
    // Recursive calls
    val childElems = elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        apply(rootElem, path.append(entry), e)
    }

    new IndexedScopedElem(rootElem, childElems, path, elem)
  }
}
