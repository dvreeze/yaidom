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

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path

/**
 * API and implementation trait for elements as containers of elements, each having a name and possible attributes, as well
 * as having awareness of paths. This trait extends trait [[eu.cdevreeze.yaidom.NavigableElemLike]], adding knowledge about paths.
 *
 * More precisely, this trait adds the following abstract method to the abstract methods required by its super-trait:
 * `findAllChildElemsWithPathEntries`. Based on this abstract method (and the super-trait), this trait offers a rich API for
 * querying elements and paths.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.PathAwareElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait PathAwareElemLike[E <: PathAwareElemLike[E]] extends NavigableElemLike[E] with PathAwareElemApi[E] { self: E =>

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, Path.Entry)]

  final def findAllChildElemPaths: immutable.IndexedSeq[Path] =
    findAllChildElemsWithPathEntries map { case (e, pe) => Path(Vector(pe)) }

  final def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[Path] =
    findAllChildElemsWithPathEntries filter { case (e, pe) => p(e) } map { case (e, pe) => Path(Vector(pe)) }

  final def findChildElemPath(p: E => Boolean): Option[Path] = {
    findAllChildElemsWithPathEntries find { case (e, pe) => p(e) } map { case (e, pe) => Path(Vector(pe)) }
  }

  final def getChildElemPath(p: E => Boolean): Path = {
    val result = filterChildElemPaths(p)
    require(result.size == 1, s"Expected exactly 1 matching child element, but found ${result.size} of them")
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

  final def findAllChildElemPathEntries: immutable.IndexedSeq[Path.Entry] = {
    findAllChildElemsWithPathEntries map { _._2 }
  }
}
