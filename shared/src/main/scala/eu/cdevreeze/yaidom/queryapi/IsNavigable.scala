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

import scala.Vector
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
 * All methods are overridable. Hence element implementations mixing in this partial implementation trait can change the
 * implementation without breaking its API, caused by otherwise needed removal of this mixin. Arguably this trait should not
 * exist as part of the public API, because implementation details should not be part of the public API. Such implementation details
 * may be subtle, such as the (runtime) boundary on the ThisElem type member.
 *
 * @author Chris de Vreeze
 */
trait IsNavigable extends IsNavigableApi {

  type ThisElem <: IsNavigable.Aux[ThisElem]

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(ThisElem, Path.Entry)]

  def findChildElemByPathEntry(entry: Path.Entry): Option[ThisElem]

  def getChildElemByPathEntry(entry: Path.Entry): ThisElem = {
    findChildElemByPathEntry(entry).getOrElse(sys.error(s"Expected existing path entry $entry from root $thisElem"))
  }

  def findElemOrSelfByPath(path: Path): Option[ThisElem] = {
    findReverseAncestryOrSelfByPath(path).map(_.last)
  }

  def getElemOrSelfByPath(path: Path): ThisElem = {
    findElemOrSelfByPath(path).getOrElse(sys.error(s"Expected existing path $path from root $thisElem"))
  }

  def findReverseAncestryOrSelfByPath(path: Path): Option[immutable.IndexedSeq[ThisElem]] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    val entryCount = path.entries.size

    def findReverseAncestryOrSelfByPath(
      currentRoot: ThisElem,
      entryIndex: Int,
      reverseAncestry: immutable.IndexedSeq[ThisElem]): Option[immutable.IndexedSeq[ThisElem]] = {

      assert(entryIndex >= 0 && entryIndex <= entryCount)

      if (entryIndex == entryCount) Some(reverseAncestry :+ currentRoot) else {
        val newRootOption: Option[ThisElem] = currentRoot.findChildElemByPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot =>
          findReverseAncestryOrSelfByPath(newRoot, entryIndex + 1, reverseAncestry :+ currentRoot)
        }
      }
    }

    findReverseAncestryOrSelfByPath(thisElem, 0, Vector())
  }

  def getReverseAncestryOrSelfByPath(path: Path): immutable.IndexedSeq[ThisElem] = {
    findReverseAncestryOrSelfByPath(path).getOrElse(sys.error(s"Expected existing path $path from root $thisElem"))
  }
}

object IsNavigable {

  /**
   * This query API type, restricting ThisElem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = IsNavigable { type ThisElem = E }
}
