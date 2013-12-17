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
 * as having awareness of paths. This trait extends trait [[eu.cdevreeze.yaidom.ElemLike]], adding knowledge about paths.
 *
 * More precisely, this trait adds the following abstract methods to the abstract methods required by its super-trait:
 * `findAllChildElemsWithPathEntries` and `findChildElemByPathEntry`. Based on these abstract methods (and the super-trait), this
 * trait offers a rich API for querying elements and paths.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.PathAwareElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait PathAwareElemLike[E <: PathAwareElemLike[E]] extends ElemLike[E] with PathAwareElemApi[E] { self: E =>

  def findChildElemByPathEntry(entry: Path.Entry): Option[E]

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, Path.Entry)]

  @deprecated(message = "Use findChildElemByPathEntry instead", since = "0.7.1")
  final def findWithElemPathEntry(entry: Path.Entry): Option[E] = findChildElemByPathEntry(entry)

  final def getChildElemByPathEntry(entry: Path.Entry): E =
    findChildElemByPathEntry(entry).getOrElse(sys.error("Expected existing path entry %s from root %s".format(entry, self)))

  final def findAllChildElemPaths: immutable.IndexedSeq[Path] =
    findAllChildElemsWithPathEntries map { case (e, pe) => Path(Vector(pe)) }

  final def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[Path] =
    findAllChildElemsWithPathEntries filter { case (e, pe) => p(e) } map { case (e, pe) => Path(Vector(pe)) }

  final def findChildElemPath(p: E => Boolean): Option[Path] = {
    findAllChildElemsWithPathEntries find { case (e, pe) => p(e) } map { case (e, pe) => Path(Vector(pe)) }
  }

  final def getChildElemPath(p: E => Boolean): Path = {
    val result = filterChildElemPaths(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  final def findAllElemOrSelfPaths: immutable.IndexedSeq[Path] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.findAllElemOrSelfPaths map { path => path.prepend(pe) }
    }

    Path.Root +: remainder
  }

  final def filterElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[Path] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.filterElemOrSelfPaths(p) map { path => path.prepend(pe) }
    }

    if (p(self)) (Path.Root +: remainder) else remainder
  }

  final def findAllElemPaths: immutable.IndexedSeq[Path] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findAllElemOrSelfPaths map { path => path.prepend(pe) } }

  final def filterElemPaths(p: E => Boolean): immutable.IndexedSeq[Path] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.filterElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  final def findTopmostElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[Path] = {
    if (p(self)) immutable.IndexedSeq(Path.Root) else {
      // Not tail-recursive, but the depth should typically be limited
      val result = findAllChildElemsWithPathEntries flatMap {
        case (e, pe) => e.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) }
      }
      result
    }
  }

  final def findTopmostElemPaths(p: E => Boolean): immutable.IndexedSeq[Path] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  final def findElemOrSelfPath(p: E => Boolean): Option[Path] = {
    // Not efficient
    filterElemOrSelfPaths(p).headOption
  }

  final def findElemPath(p: E => Boolean): Option[Path] = {
    val elms = self.findAllChildElemsWithPathEntries.view flatMap { case (ch, pe) => ch.findElemOrSelfPath(p) map { path => path.prepend(pe) } }
    elms.headOption
  }

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

  /**
   * Finds the element with the given `Path` (where this element is the root), if any, wrapped in an `Option`.
   * This method must be very efficient, which depends on the efficiency of method `findChildElemByPathEntry`.
   */
  @deprecated(message = "Use findElemOrSelfByPath instead", since = "0.7.1")
  final def findWithElemPath(path: Path): Option[E] = findElemOrSelfByPath(path)

  final def getElemOrSelfByPath(path: Path): E =
    findElemOrSelfByPath(path).getOrElse(sys.error("Expected existing path %s from root %s".format(path, self)))

  @deprecated(message = "Use getElemOrSelfByPath instead", since = "0.7.1")
  final def getWithElemPath(path: Path): E = getElemOrSelfByPath(path)

  final def findAllChildElemPathEntries: immutable.IndexedSeq[Path.Entry] = {
    findAllChildElemsWithPathEntries map { _._2 }
  }
}
