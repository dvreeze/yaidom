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

import eu.cdevreeze.yaidom.core.Path

/**
 * API and implementation trait for elements as containers of elements, each having a name and possible attributes, as well
 * as supporting navigation using paths. This trait extends trait [[eu.cdevreeze.yaidom.ElemLike]], adding navigation using paths.
 *
 * More precisely, this trait adds the following abstract method to the abstract methods required by its super-trait:
 * `findChildElemByPathEntry`.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.IsNavigableApi]]. See the documentation of that trait
 * for more information.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait IsNavigable[E <: IsNavigable[E]] extends IsNavigableApi[E] { self: E =>

  def findChildElemByPathEntry(entry: Path.Entry): Option[E]

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
}
