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
 * `findAllChildElemsWithPathEntries` and `findChildElemByPathEntry`. Based on these abstract methods (and the super-trait), this
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

  def findChildElemByPathEntry(entry: ElemPath.Entry): Option[E]

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, ElemPath.Entry)]

  @deprecated(message = "Use findChildElemByPathEntry instead", since = "0.7.1")
  final def findWithElemPathEntry(entry: ElemPath.Entry): Option[E] = findChildElemByPathEntry(entry)

  final def findAllPathsOfChildElems: immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries map { case (e, pe) => ElemPath(Vector(pe)) }

  @deprecated(message = "Use findAllPathsOfChildElems instead", since = "0.7.1")
  final def findAllChildElemPaths: immutable.IndexedSeq[ElemPath] = findAllPathsOfChildElems

  final def filterPathsOfChildElems(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries filter { case (e, pe) => p(e) } map { case (e, pe) => ElemPath(Vector(pe)) }

  @deprecated(message = "Use filterPathsOfChildElems instead", since = "0.7.1")
  final def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = filterPathsOfChildElems(p)

  final def findPathOfChildElem(p: E => Boolean): Option[ElemPath] = {
    findAllChildElemsWithPathEntries find { case (e, pe) => p(e) } map { case (e, pe) => ElemPath(Vector(pe)) }
  }

  @deprecated(message = "Use findPathOfChildElem instead", since = "0.7.1")
  final def findChildElemPath(p: E => Boolean): Option[ElemPath] = findPathOfChildElem(p)

  final def getPathOfChildElem(p: E => Boolean): ElemPath = {
    val result = filterPathsOfChildElems(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  @deprecated(message = "Use getPathOfChildElem instead", since = "0.7.1")
  final def getChildElemPath(p: E => Boolean): ElemPath = getPathOfChildElem(p)

  final def findAllPathsOfElemsOrSelf: immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.findAllPathsOfElemsOrSelf map { path => path.prepend(pe) }
    }

    ElemPath.Root +: remainder
  }

  @deprecated(message = "Use findAllPathsOfElemsOrSelf instead", since = "0.7.1")
  final def findAllElemOrSelfPaths: immutable.IndexedSeq[ElemPath] = findAllPathsOfElemsOrSelf

  final def filterPathsOfElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.filterPathsOfElemsOrSelf(p) map { path => path.prepend(pe) }
    }

    if (p(self)) (ElemPath.Root +: remainder) else remainder
  }

  @deprecated(message = "Use filterPathsOfElemsOrSelf instead", since = "0.7.1")
  final def filterElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = filterPathsOfElemsOrSelf(p)

  final def findAllPathsOfElems: immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findAllPathsOfElemsOrSelf map { path => path.prepend(pe) } }

  @deprecated(message = "Use findAllPathsOfElems instead", since = "0.7.1")
  final def findAllElemPaths: immutable.IndexedSeq[ElemPath] = findAllPathsOfElems

  final def filterPathsOfElems(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.filterPathsOfElemsOrSelf(p) map { path => path.prepend(pe) } }

  @deprecated(message = "Use filterPathsOfElems instead", since = "0.7.1")
  final def filterElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = filterPathsOfElems(p)

  final def findPathsOfTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    if (p(self)) immutable.IndexedSeq(ElemPath.Root) else {
      // Not tail-recursive, but the depth should typically be limited
      val result = findAllChildElemsWithPathEntries flatMap {
        case (e, pe) => e.findPathsOfTopmostElemsOrSelf(p) map { path => path.prepend(pe) }
      }
      result
    }
  }

  @deprecated(message = "Use findPathsOfTopmostElemsOrSelf instead", since = "0.7.1")
  final def findTopmostElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findPathsOfTopmostElemsOrSelf(p)

  final def findPathsOfTopmostElems(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findPathsOfTopmostElemsOrSelf(p) map { path => path.prepend(pe) } }

  @deprecated(message = "Use findPathsOfTopmostElems instead", since = "0.7.1")
  final def findTopmostElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findPathsOfTopmostElems(p)

  final def findPathOfElemOrSelf(p: E => Boolean): Option[ElemPath] = {
    // Not efficient
    filterPathsOfElemsOrSelf(p).headOption
  }

  @deprecated(message = "Use findPathOfElemOrSelf instead", since = "0.7.1")
  final def findElemOrSelfPath(p: E => Boolean): Option[ElemPath] = findPathOfElemOrSelf(p)

  final def findPathOfElem(p: E => Boolean): Option[ElemPath] = {
    val elms = self.findAllChildElemsWithPathEntries.view flatMap { case (ch, pe) => ch.findPathOfElemOrSelf(p) map { path => path.prepend(pe) } }
    elms.headOption
  }

  @deprecated(message = "Use findPathOfElem instead", since = "0.7.1")
  final def findElemPath(p: E => Boolean): Option[ElemPath] = findPathOfElem(p)

  /**
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   * This method must be very efficient, which depends on the efficiency of method `findChildElemByPathEntry`.
   */
  final def findElemOrSelfByPath(path: ElemPath): Option[E] = {
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
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   * This method must be very efficient, which depends on the efficiency of method `findChildElemByPathEntry`.
   */
  @deprecated(message = "Use findElemOrSelfByPath instead", since = "0.7.1")
  final def findWithElemPath(path: ElemPath): Option[E] = findElemOrSelfByPath(path)

  final def getElemOrSelfByPath(path: ElemPath): E =
    findElemOrSelfByPath(path).getOrElse(sys.error("Expected existing path %s from root %s".format(path, self)))

  @deprecated(message = "Use getElemOrSelfByPath instead", since = "0.7.1")
  final def getWithElemPath(path: ElemPath): E = getElemOrSelfByPath(path)

  final def findAllPathEntriesOfChildElems: immutable.IndexedSeq[ElemPath.Entry] = {
    findAllChildElemsWithPathEntries map { _._2 }
  }

  @deprecated(message = "Use findAllPathEntriesOfChildElems instead", since = "0.7.1")
  final def findAllChildElemPathEntries: immutable.IndexedSeq[ElemPath.Entry] = findAllPathEntriesOfChildElems
}
