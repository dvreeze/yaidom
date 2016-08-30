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
import scala.collection.mutable

import eu.cdevreeze.yaidom.core.Path

/**
 * API and implementation trait for functionally updatable elements. This trait extends trait [[eu.cdevreeze.yaidom.queryapi.IsNavigable]],
 * adding knowledge about child nodes in general, and about the correspondence between child path entries and child
 * indexes.
 *
 * More precisely, this trait adds the following abstract methods to the abstract methods required by its super-trait:
 * `children`, `withChildren` and `collectChildNodeIndexes`. Based on these abstract methods (and the super-trait), this
 * trait offers a rich API for functionally updating elements.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.UpdatableElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment.
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike extends IsNavigable with UpdatableElemApi {

  type ThisElemApi <: UpdatableElemLike with IsNavigableApi.Aux[ThisElem]

  def children: immutable.IndexedSeq[ThisNode]

  def withChildren(newChildren: immutable.IndexedSeq[ThisNode]): ThisElem

  def collectChildNodeIndexes(pathEntries: Set[Path.Entry]): Map[Path.Entry, Int]

  final def childNodeIndex(pathEntry: Path.Entry): Int = {
    collectChildNodeIndexes(Set(pathEntry)).getOrElse(pathEntry, -1)
  }

  final def withChildSeqs(newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[ThisNode]]): ThisElem = {
    withChildren(newChildSeqs.flatten)
  }

  final def withUpdatedChildren(index: Int, newChild: ThisNode): ThisElem =
    withChildren(children.updated(index, newChild))

  final def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[ThisNode], replace: Int): ThisElem =
    withChildren(children.patch(from, newChildren, replace))

  final def plusChild(index: Int, child: ThisNode): ThisElem = {
    require(
      index <= thisElem.children.size,
      s"Expected index $index to be at most the number of children: ${thisElem.children.size}")

    if (index == children.size) plusChild(child)
    else withPatchedChildren(index, Vector(child, children(index)), 1)
  }

  final def plusChild(child: ThisNode): ThisElem = withChildren(children :+ child)

  final def plusChildOption(index: Int, childOption: Option[ThisNode]): ThisElem = {
    if (childOption.isEmpty) thisElem else plusChild(index, childOption.get)
  }

  final def plusChildOption(childOption: Option[ThisNode]): ThisElem = {
    if (childOption.isEmpty) thisElem else plusChild(childOption.get)
  }

  final def plusChildren(childSeq: immutable.IndexedSeq[ThisNode]): ThisElem = {
    withChildren(children ++ childSeq)
  }

  final def minusChild(index: Int): ThisElem = {
    require(
      index < thisElem.children.size,
      s"Expected index $index to be less than the number of children: ${thisElem.children.size}")

    withPatchedChildren(index, Vector(), 1)
  }

  final def updateChildElem(pathEntry: Path.Entry)(f: ThisElem => ThisElem): ThisElem = {
    updateChildElems(Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElem(pathEntry: Path.Entry, newElem: ThisElem): ThisElem = {
    updateChildElem(pathEntry) { e => newElem }
  }

  final def updateChildElemWithNodeSeq(pathEntry: Path.Entry)(f: ThisElem => immutable.IndexedSeq[ThisNode]): ThisElem = {
    updateChildElemsWithNodeSeq(Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElemWithNodeSeq(pathEntry: Path.Entry, newNodes: immutable.IndexedSeq[ThisNode]): ThisElem = {
    updateChildElemWithNodeSeq(pathEntry) { e => newNodes }
  }

  final def updateElemOrSelf(path: Path)(f: ThisElem => ThisElem): ThisElem = {
    updateElemsOrSelf(Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemOrSelf(path: Path, newElem: ThisElem): ThisElem =
    updateElemOrSelf(path) { e => newElem }

  final def updateElemWithNodeSeq(path: Path)(f: ThisElem => immutable.IndexedSeq[ThisNode]): ThisElem = {
    updateElemsWithNodeSeq(Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[ThisNode]): ThisElem = {
    updateElemWithNodeSeq(path) { e => newNodes }
  }

  final def updateChildElems(pathEntries: Set[Path.Entry])(f: (ThisElem, Path.Entry) => ThisElem): ThisElem = {
    // For efficiency, not delegating to updateChildElemsWithNodeSeq

    if (pathEntries.isEmpty) thisElem
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(thisElem.children) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[ThisElem]
          accChildNodes.updated(idx, f(che, pathEntry))
      }
      thisElem.withChildren(newChildren)
    }
  }

  final def updateChildElemsWithNodeSeq(pathEntries: Set[Path.Entry])(f: (ThisElem, Path.Entry) => immutable.IndexedSeq[ThisNode]): ThisElem = {
    if (pathEntries.isEmpty) thisElem
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(thisElem.children) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[ThisElem]
          accChildNodes.patch(idx, f(che, pathEntry), 1)
      }
      thisElem.withChildren(newChildren)
    }
  }

  final def updateElemsOrSelf(paths: Set[Path])(f: (ThisElem, Path) => ThisElem): ThisElem = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    val descendantUpdateResult =
      updateChildElems(pathsByFirstEntry.keySet) {
        case (che, pathEntry) =>
          // Recursive (but non-tail-recursive) call
          che.updateElemsOrSelf(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }

    if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else descendantUpdateResult
  }

  final def updateElems(paths: Set[Path])(f: (ThisElem, Path) => ThisElem): ThisElem = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElems(pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        che.updateElemsOrSelf(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateElemsOrSelfWithNodeSeq(paths: Set[Path])(f: (ThisElem, Path) => immutable.IndexedSeq[ThisNode]): immutable.IndexedSeq[ThisNode] = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    val descendantUpdateResult =
      updateChildElemsWithNodeSeq(pathsByFirstEntry.keySet) {
        case (che, pathEntry) =>
          // Recursive (but non-tail-recursive) call
          che.updateElemsOrSelfWithNodeSeq(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }

    if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else Vector(descendantUpdateResult)
  }

  final def updateElemsWithNodeSeq(paths: Set[Path])(f: (ThisElem, Path) => immutable.IndexedSeq[ThisNode]): ThisElem = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElemsWithNodeSeq(pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        che.updateElemsOrSelfWithNodeSeq(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateChildElems(f: (ThisElem, Path.Entry) => Option[ThisElem]): ThisElem = {
    val editsByPathEntries: Map[Path.Entry, ThisElem] =
      findAllChildElemsWithPathEntries.flatMap({ case (che, pe) => f(che, pe).map(newE => (pe, newE)) }).toMap

    updateChildElems(editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, che)
    }
  }

  final def updateChildElemsWithNodeSeq(f: (ThisElem, Path.Entry) => Option[immutable.IndexedSeq[ThisNode]]): ThisElem = {
    val editsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[ThisNode]] =
      findAllChildElemsWithPathEntries.flatMap({ case (che, pe) => f(che, pe).map(newNodes => (pe, newNodes)) }).toMap

    updateChildElemsWithNodeSeq(editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, immutable.IndexedSeq(che))
    }
  }

  final def updateTopmostElemsOrSelf(f: (ThisElem, Path) => Option[ThisElem]): ThisElem = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, ThisElem]()

    val foundElems =
      ElemWithPath(thisElem) findTopmostElemsOrSelf { elm =>
        val optResult = f(elm.elem, elm.path)
        if (optResult.isDefined) {
          mutableEditsByPaths += (elm.path -> optResult.get)
        }
        optResult.isDefined
      }

    val editsByPaths = mutableEditsByPaths.toMap

    updateElemsOrSelf(editsByPaths.keySet) {
      case (elm, path) => editsByPaths.getOrElse(path, elm)
    }
  }

  final def updateTopmostElems(f: (ThisElem, Path) => Option[ThisElem]): ThisElem = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, ThisElem]()

    val foundElems =
      ElemWithPath(thisElem) findTopmostElems { elm =>
        val optResult = f(elm.elem, elm.path)
        if (optResult.isDefined) {
          mutableEditsByPaths += (elm.path -> optResult.get)
        }
        optResult.isDefined
      }

    val editsByPaths = mutableEditsByPaths.toMap

    updateElems(editsByPaths.keySet) {
      case (elm, path) => editsByPaths.getOrElse(path, elm)
    }
  }

  final def updateTopmostElemsOrSelfWithNodeSeq(f: (ThisElem, Path) => Option[immutable.IndexedSeq[ThisNode]]): immutable.IndexedSeq[ThisNode] = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[ThisNode]]()

    val foundElems =
      ElemWithPath(thisElem) findTopmostElemsOrSelf { elm =>
        val optResult = f(elm.elem, elm.path)
        if (optResult.isDefined) {
          mutableEditsByPaths += (elm.path -> optResult.get)
        }
        optResult.isDefined
      }

    val editsByPaths = mutableEditsByPaths.toMap

    updateElemsOrSelfWithNodeSeq(editsByPaths.keySet) {
      case (elm, path) => editsByPaths.getOrElse(path, immutable.IndexedSeq(elm))
    }
  }

  final def updateTopmostElemsWithNodeSeq(f: (ThisElem, Path) => Option[immutable.IndexedSeq[ThisNode]]): ThisElem = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[ThisNode]]()

    val foundElems =
      ElemWithPath(thisElem) findTopmostElems { elm =>
        val optResult = f(elm.elem, elm.path)
        if (optResult.isDefined) {
          mutableEditsByPaths += (elm.path -> optResult.get)
        }
        optResult.isDefined
      }

    val editsByPaths = mutableEditsByPaths.toMap

    updateElemsWithNodeSeq(editsByPaths.keySet) {
      case (elm, path) => editsByPaths.getOrElse(path, immutable.IndexedSeq(elm))
    }
  }
}

object UpdatableElemLike {

  /**
   * This query API type, fixing ThisNode, ThisElem and ThisElemApi to the passed type parameters.
   *
   * @tparam N The node self type
   * @tparam E The element self type
   */
  type Aux[N, E] = UpdatableElemLike { type ThisNode = N; type ThisElem = E; type ThisElemApi = E }
}
