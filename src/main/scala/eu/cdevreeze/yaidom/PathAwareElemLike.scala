/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom

import scala.collection.{ immutable, mutable }

/**
 * API and implementation trait for elements as containers of elements, each having a name and possible attributes, as well
 * as having awareness of element paths. This trait extends trait [[eu.cdevreeze.yaidom.ElemLike]], adding knowledge about
 * element paths.
 *
 * More precisely, this trait adds the following abstract methods to the abstract methods required by its super-trait:
 * `findAllChildElemsWithPathEntries` and `findWithElemPathEntry`. Based on these abstract methods (and the super-trait), this
 * trait offers a rich API for querying elements and element paths.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.PathAwareElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait PathAwareElemLike[E <: PathAwareElemLike[E]] extends ElemLike[E] with PathAwareElemApi[E] { self: E =>

  def findWithElemPathEntry(entry: ElemPath.Entry): Option[E]

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, ElemPath.Entry)]

  final def findAllChildElemPaths: immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries map { case (e, pe) => ElemPath(Vector(pe)) }

  final def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries filter { case (e, pe) => p(e) } map { case (e, pe) => ElemPath(Vector(pe)) }

  final def findChildElemPath(p: E => Boolean): Option[ElemPath] = {
    findAllChildElemsWithPathEntries find { case (e, pe) => p(e) } map { case (e, pe) => ElemPath(Vector(pe)) }
  }

  final def getChildElemPath(p: E => Boolean): ElemPath = {
    val result = filterChildElemPaths(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  final def findAllElemOrSelfPaths: immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.findAllElemOrSelfPaths map { path => path.prepend(pe) }
    }

    ElemPath.Root +: remainder
  }

  final def filterElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.filterElemOrSelfPaths(p) map { path => path.prepend(pe) }
    }

    if (p(self)) (ElemPath.Root +: remainder) else remainder
  }

  final def findAllElemPaths: immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findAllElemOrSelfPaths map { path => path.prepend(pe) } }

  final def filterElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.filterElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  final def findTopmostElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    if (p(self)) immutable.IndexedSeq(ElemPath.Root) else {
      // Not tail-recursive, but the depth should typically be limited
      val result = findAllChildElemsWithPathEntries flatMap {
        case (e, pe) => e.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) }
      }
      result
    }
  }

  final def findTopmostElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  final def findElemOrSelfPath(p: E => Boolean): Option[ElemPath] = {
    // Not efficient
    filterElemOrSelfPaths(p).headOption
  }

  final def findElemPath(p: E => Boolean): Option[ElemPath] = {
    val elms = self.findAllChildElemsWithPathEntries.view flatMap { case (ch, pe) => ch.findElemOrSelfPath(p) map { path => path.prepend(pe) } }
    elms.headOption
  }

  /**
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   * This method must be very efficient, which depends on the efficiency of method `findWithElemPathEntry`.
   */
  final def findWithElemPath(path: ElemPath): Option[E] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    val entryCount = path.entries.size

    def findWithElemPath(currentRoot: E, entryIndex: Int): Option[E] = {
      assert(entryIndex >= 0 && entryIndex <= entryCount)

      if (entryIndex == entryCount) Some(currentRoot) else {
        val newRootOption: Option[E] = currentRoot.findWithElemPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot => findWithElemPath(newRoot, entryIndex + 1) }
      }
    }

    findWithElemPath(self, 0)
  }

  final def getWithElemPath(path: ElemPath): E =
    findWithElemPath(path).getOrElse(sys.error("Expected existing path %s from root %s".format(path, self)))

  final def findAllChildElemPathEntries: immutable.IndexedSeq[ElemPath.Entry] = {
    findAllChildElemsWithPathEntries map { _._2 }
  }
}
