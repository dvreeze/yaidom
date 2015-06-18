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
 * API and implementation trait for elements that can be navigated using paths.
 *
 * More precisely, this trait has the following abstract methods: `findChildElemByPathEntry` and `findAllChildElemsWithPathEntries`.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.IsNavigableApi]]. See the documentation of that trait
 * for more information.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait IsNavigable[E <: IsNavigable[E]] extends IsNavigableApi[E] { self: E =>

  def findChildElemByPathEntry(entry: Path.Entry): Option[E]

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, Path.Entry)]

  final def getChildElemByPathEntry(entry: Path.Entry): E =
    findChildElemByPathEntry(entry).getOrElse(sys.error(s"Expected existing path entry $entry from root $self"))

  /**
   * Finds the element with the given `Path` (where this element is the root), if any, wrapped in an `Option`.
   * This method must be very efficient, which depends on the efficiency of method `findChildElemByPathEntry`.
   */
  final def findElemOrSelfByPath(path: Path): Option[E] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    val entryCount = path.entries.size

    def findElemOrSelfByPath(currentRoot: E, entryIndex: Int): Option[E] = {
      assert(entryIndex >= 0 && entryIndex <= entryCount)

      if (entryIndex == entryCount) Some(currentRoot) else {
        val newRootOption: Option[E] = currentRoot.findChildElemByPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot => findElemOrSelfByPath(newRoot, entryIndex + 1) }
      }
    }

    findElemOrSelfByPath(self, 0)
  }

  final def getElemOrSelfByPath(path: Path): E =
    findElemOrSelfByPath(path).getOrElse(sys.error(s"Expected existing path $path from root $self"))

  final def findAllElemsOrSelfWithPaths: immutable.IndexedSeq[(E, Path)] = {
    (self, Path.Root) +: {
      findAllChildElemsWithPathEntries flatMap {
        case (elem, entry) =>
          // Recursive calls
          elem.findAllElemsOrSelfWithPaths map { case (e, p) => (e, p.prepend(entry)) }
      }
    }
  }

  final def filterElemsOrSelfByPaths(paths: Set[Path]): immutable.IndexedSeq[E] = {
    val selfOption: Option[E] = if (paths.contains(Path.Root)) Some(this) else None

    val remainingPathsByFirstEntries: Map[Path.Entry, Set[Path]] =
      (paths - Path.Root).groupBy(_.firstEntry).mapValues(paths => paths.map(_.withoutFirstEntry))
    val firstEntries = remainingPathsByFirstEntries.keySet

    val descendants =
      findAllChildElemsWithPathEntries filter { case (che, entry) => firstEntries.contains(entry) } flatMap {
        case (che, entry) =>
          // Recursive call
          che.filterElemsOrSelfByPaths(remainingPathsByFirstEntries(entry))
      }

    selfOption.toIndexedSeq ++ descendants
  }
}
