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
 * More precisely, this trait has only the following abstract methods: `findChildElemByPathEntry` and `findAllChildElemsWithPathEntries`.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.IsNavigableApi]]. See the documentation of that trait
 * for more information.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait IsNavigable[E <: IsNavigable[E]] extends IsNavigableApi[E] { self: E =>

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, Path.Entry)]

  def findChildElemByPathEntry(entry: Path.Entry): Option[E]

  final def getChildElemByPathEntry(entry: Path.Entry): E =
    findChildElemByPathEntry(entry).getOrElse(sys.error(s"Expected existing path entry $entry from root $self"))

  final def findElemOrSelfByPath(path: Path): Option[E] = {
    findReverseAncestryOrSelfByPath(path).map(_.last)
  }

  final def getElemOrSelfByPath(path: Path): E =
    findElemOrSelfByPath(path).getOrElse(sys.error(s"Expected existing path $path from root $self"))

  final def findReverseAncestryOrSelfByPath(path: Path): Option[immutable.IndexedSeq[E]] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    val entryCount = path.entries.size

    def findReverseAncestryOrSelfByPath(
      currentRoot: E,
      entryIndex: Int,
      reverseAncestry: immutable.IndexedSeq[E]): Option[immutable.IndexedSeq[E]] = {

      assert(entryIndex >= 0 && entryIndex <= entryCount)

      if (entryIndex == entryCount) Some(reverseAncestry :+ currentRoot) else {
        val newRootOption: Option[E] = currentRoot.findChildElemByPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot =>
          findReverseAncestryOrSelfByPath(newRoot, entryIndex + 1, reverseAncestry :+ currentRoot)
        }
      }
    }

    findReverseAncestryOrSelfByPath(self, 0, Vector())
  }

  final def getReverseAncestryOrSelfByPath(path: Path): immutable.IndexedSeq[E] = {
    findReverseAncestryOrSelfByPath(path).getOrElse(sys.error(s"Expected existing path $path from root $self"))
  }
}
