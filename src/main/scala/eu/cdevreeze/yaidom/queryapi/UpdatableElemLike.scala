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
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends IsNavigable[E] with UpdatableElemApi[N, E] { self: E =>

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding potential type class.
  // Yet I did not want to depend on a val or def returning the appropriate type class instance, so chose for code repetition.

  def children: immutable.IndexedSeq[N]

  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  def collectChildNodeIndexes(pathEntries: Set[Path.Entry]): Map[Path.Entry, Int]

  final def childNodeIndex(pathEntry: Path.Entry): Int = {
    collectChildNodeIndexes(Set(pathEntry)).getOrElse(pathEntry, -1)
  }

  final def withChildSeqs(newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): E = {
    withChildren(newChildSeqs.flatten)
  }

  final def withUpdatedChildren(index: Int, newChild: N): E =
    withChildren(children.updated(index, newChild))

  final def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E =
    withChildren(children.patch(from, newChildren, replace))

  final def plusChild(index: Int, child: N): E = {
    require(
      index <= self.children.size,
      s"Expected index $index to be at most the number of children: ${self.children.size}")

    if (index == children.size) plusChild(child)
    else withPatchedChildren(index, Vector(child, children(index)), 1)
  }

  final def plusChild(child: N): E = withChildren(children :+ child)

  final def plusChildOption(index: Int, childOption: Option[N]): E = {
    if (childOption.isEmpty) self else plusChild(index, childOption.get)
  }

  final def plusChildOption(childOption: Option[N]): E = {
    if (childOption.isEmpty) self else plusChild(childOption.get)
  }

  final def plusChildren(childSeq: immutable.IndexedSeq[N]): E = {
    withChildren(children ++ childSeq)
  }

  final def minusChild(index: Int): E = {
    require(
      index < self.children.size,
      s"Expected index $index to be less than the number of children: ${self.children.size}")

    withPatchedChildren(index, Vector(), 1)
  }

  final def updateChildElem(pathEntry: Path.Entry)(f: E => E): E = {
    updateChildElems(Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElem(pathEntry: Path.Entry, newElem: E): E = {
    updateChildElem(pathEntry) { e => newElem }
  }

  final def updateChildElemWithNodeSeq(pathEntry: Path.Entry)(f: E => immutable.IndexedSeq[N]): E = {
    updateChildElemsWithNodeSeq(Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElemWithNodeSeq(pathEntry: Path.Entry, newNodes: immutable.IndexedSeq[N]): E = {
    updateChildElemWithNodeSeq(pathEntry) { e => newNodes }
  }

  final def updateElemOrSelf(path: Path)(f: E => E): E = {
    updateElemsOrSelf(Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemOrSelf(path: Path, newElem: E): E =
    updateElemOrSelf(path) { e => newElem }

  final def updateElemWithNodeSeq(path: Path)(f: E => immutable.IndexedSeq[N]): E = {
    updateElemsWithNodeSeq(Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[N]): E = {
    updateElemWithNodeSeq(path) { e => newNodes }
  }

  final def updateChildElems(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => E): E = {
    // For efficiency, not delegating to updateChildElemsWithNodeSeq

    if (pathEntries.isEmpty) self
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(self.children) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[E]
          accChildNodes.updated(idx, f(che, pathEntry))
      }
      self.withChildren(newChildren)
    }
  }

  final def updateChildElemsWithNodeSeq(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => immutable.IndexedSeq[N]): E = {
    if (pathEntries.isEmpty) self
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(self.children) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[E]
          accChildNodes.patch(idx, f(che, pathEntry), 1)
      }
      self.withChildren(newChildren)
    }
  }

  final def updateElemsOrSelf(paths: Set[Path])(f: (E, Path) => E): E = {
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

  final def updateElems(paths: Set[Path])(f: (E, Path) => E): E = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElems(pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        che.updateElemsOrSelf(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateElemsOrSelfWithNodeSeq(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): immutable.IndexedSeq[N] = {
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

  final def updateElemsWithNodeSeq(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElemsWithNodeSeq(pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        che.updateElemsOrSelfWithNodeSeq(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateChildElems(f: (E, Path.Entry) => Option[E]): E = {
    val editsByPathEntries: Map[Path.Entry, E] =
      findAllChildElemsWithPathEntries.flatMap({ case (che, pe) => f(che, pe).map(newE => (pe, newE)) }).toMap

    updateChildElems(editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, che)
    }
  }

  final def updateChildElemsWithNodeSeq(f: (E, Path.Entry) => Option[immutable.IndexedSeq[N]]): E = {
    val editsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[N]] =
      findAllChildElemsWithPathEntries.flatMap({ case (che, pe) => f(che, pe).map(newNodes => (pe, newNodes)) }).toMap

    updateChildElemsWithNodeSeq(editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, immutable.IndexedSeq(che))
    }
  }

  final def updateTopmostElemsOrSelf(f: (E, Path) => Option[E]): E = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, E]()

    val foundElems =
      ElemWithPath(self) findTopmostElemsOrSelf { elm =>
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

  final def updateTopmostElems(f: (E, Path) => Option[E]): E = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, E]()

    val foundElems =
      ElemWithPath(self) findTopmostElems { elm =>
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

  final def updateTopmostElemsOrSelfWithNodeSeq(f: (E, Path) => Option[immutable.IndexedSeq[N]]): immutable.IndexedSeq[N] = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[N]]()

    val foundElems =
      ElemWithPath(self) findTopmostElemsOrSelf { elm =>
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

  final def updateTopmostElemsWithNodeSeq(f: (E, Path) => Option[immutable.IndexedSeq[N]]): E = {
    // Code repetition, but clear
    val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[N]]()

    val foundElems =
      ElemWithPath(self) findTopmostElems { elm =>
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

  @deprecated(message = "Renamed to 'updateChildElem'", since = "1.5.0")
  final def updated(pathEntry: Path.Entry)(f: E => E): E = {
    updateChildElem(pathEntry)(f)
  }

  @deprecated(message = "Renamed to 'updateChildElems'", since = "1.5.0")
  final def updatedAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => E): E = {
    updateChildElems(pathEntries)(f)
  }

  @deprecated(message = "Renamed to 'updateElemOrSelf'", since = "1.5.0")
  final def updated(path: Path)(f: E => E): E = {
    updateElemOrSelf(path)(f)
  }

  @deprecated(message = "Renamed to 'updateElemOrSelf'", since = "1.5.0")
  final def updated(path: Path, newElem: E): E = {
    updateElemOrSelf(path, newElem)
  }

  @deprecated(message = "Renamed to 'updateElemsOrSelf'", since = "1.5.0")
  final def updatedAtPaths(paths: Set[Path])(f: (E, Path) => E): E = {
    updateElemsOrSelf(paths)(f)
  }

  @deprecated(message = "Renamed to 'updateElemWithNodeSeq'", since = "1.5.0")
  final def updatedWithNodeSeq(path: Path)(f: E => immutable.IndexedSeq[N]): E = {
    updateElemWithNodeSeq(path)(f)
  }

  @deprecated(message = "Renamed to 'updateElemWithNodeSeq'", since = "1.5.0")
  final def updatedWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[N]): E = {
    updateElemWithNodeSeq(path, newNodes)
  }

  @deprecated(message = "Renamed to 'updateChildElemsWithNodeSeq'", since = "1.5.0")
  final def updatedWithNodeSeqAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => immutable.IndexedSeq[N]): E = {
    updateChildElemsWithNodeSeq(pathEntries)(f)
  }

  @deprecated(message = "Renamed to 'updateElemsWithNodeSeq'", since = "1.5.0")
  final def updatedWithNodeSeqAtPaths(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E = {
    updateElemsWithNodeSeq(paths)(f)
  }
}

object UpdatableElemLike {

  /**
   * The `UpdatableElemLike` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[N, E <: N] extends IsNavigable.FunctionApi[E] with UpdatableElemApi.FunctionApi[N, E] {

    def underlyingElementFunctionApi: ElemWithPath.FunctionApi[E]

    def children(thisElem: E): immutable.IndexedSeq[N]

    def withChildren(thisElem: E, newChildren: immutable.IndexedSeq[N]): E

    def collectChildNodeIndexes(thisElem: E, pathEntries: Set[Path.Entry]): Map[Path.Entry, Int]

    final def childNodeIndex(thisElem: E, pathEntry: Path.Entry): Int = {
      collectChildNodeIndexes(thisElem, Set(pathEntry)).getOrElse(pathEntry, -1)
    }

    final def withChildSeqs(thisElem: E, newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): E = {
      withChildren(thisElem, newChildSeqs.flatten)
    }

    final def withUpdatedChildren(thisElem: E, index: Int, newChild: N): E =
      withChildren(thisElem, children(thisElem).updated(index, newChild))

    final def withPatchedChildren(thisElem: E, from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E =
      withChildren(thisElem, children(thisElem).patch(from, newChildren, replace))

    final def plusChild(thisElem: E, index: Int, child: N): E = {
      require(
        index <= children(thisElem).size,
        s"Expected index $index to be at most the number of children: ${children(thisElem).size}")

      if (index == children(thisElem).size) plusChild(thisElem, child)
      else withPatchedChildren(thisElem, index, Vector(child, children(thisElem)(index)), 1)
    }

    final def plusChild(thisElem: E, child: N): E = withChildren(thisElem, children(thisElem) :+ child)

    final def plusChildOption(thisElem: E, index: Int, childOption: Option[N]): E = {
      if (childOption.isEmpty) thisElem else plusChild(thisElem, index, childOption.get)
    }

    final def plusChildOption(thisElem: E, childOption: Option[N]): E = {
      if (childOption.isEmpty) thisElem else plusChild(thisElem, childOption.get)
    }

    final def plusChildren(thisElem: E, childSeq: immutable.IndexedSeq[N]): E = {
      withChildren(thisElem, children(thisElem) ++ childSeq)
    }

    final def minusChild(thisElem: E, index: Int): E = {
      require(
        index < children(thisElem).size,
        s"Expected index $index to be less than the number of children: ${children(thisElem).size}")

      withPatchedChildren(thisElem, index, Vector(), 1)
    }

    final def updateChildElem(thisElem: E, pathEntry: Path.Entry)(f: E => E): E = {
      updateChildElems(thisElem, Set(pathEntry)) { case (che, pe) => f(che) }
    }

    final def updateChildElem(thisElem: E, pathEntry: Path.Entry, newElem: E): E = {
      updateChildElem(thisElem, pathEntry) { e => newElem }
    }

    final def updateChildElemWithNodeSeq(thisElem: E, pathEntry: Path.Entry)(f: E => immutable.IndexedSeq[N]): E = {
      updateChildElemsWithNodeSeq(thisElem, Set(pathEntry)) { case (che, pe) => f(che) }
    }

    final def updateChildElemWithNodeSeq(thisElem: E, pathEntry: Path.Entry, newNodes: immutable.IndexedSeq[N]): E = {
      updateChildElemWithNodeSeq(thisElem, pathEntry) { e => newNodes }
    }

    final def updateElemOrSelf(thisElem: E, path: Path)(f: E => E): E = {
      updateElemsOrSelf(thisElem, Set(path)) { case (e, path) => f(e) }
    }

    final def updateElemOrSelf(thisElem: E, path: Path, newElem: E): E =
      updateElemOrSelf(thisElem, path) { e => newElem }

    final def updateElemWithNodeSeq(thisElem: E, path: Path)(f: E => immutable.IndexedSeq[N]): E = {
      updateElemsWithNodeSeq(thisElem, Set(path)) { case (e, path) => f(e) }
    }

    final def updateElemWithNodeSeq(thisElem: E, path: Path, newNodes: immutable.IndexedSeq[N]): E = {
      updateElemWithNodeSeq(thisElem, path) { e => newNodes }
    }

    final def updateChildElems(thisElem: E, pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => E): E = {
      // For efficiency, not delegating to updateChildElemsWithNodeSeq

      if (pathEntries.isEmpty) thisElem
      else {
        val indexesByPathEntries: Seq[(Path.Entry, Int)] =
          collectChildNodeIndexes(thisElem, pathEntries).toSeq.sortBy(_._2)

        // Updating in reverse order of indexes, in order not to invalidate the path entries
        val newChildren = indexesByPathEntries.reverse.foldLeft(children(thisElem)) {
          case (accChildNodes, (pathEntry, idx)) =>
            val che = accChildNodes(idx).asInstanceOf[E]
            accChildNodes.updated(idx, f(che, pathEntry))
        }
        withChildren(thisElem, newChildren)
      }
    }

    final def updateChildElemsWithNodeSeq(thisElem: E, pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => immutable.IndexedSeq[N]): E = {
      if (pathEntries.isEmpty) thisElem
      else {
        val indexesByPathEntries: Seq[(Path.Entry, Int)] =
          collectChildNodeIndexes(thisElem, pathEntries).toSeq.sortBy(_._2)

        // Updating in reverse order of indexes, in order not to invalidate the path entries
        val newChildren = indexesByPathEntries.reverse.foldLeft(children(thisElem)) {
          case (accChildNodes, (pathEntry, idx)) =>
            val che = accChildNodes(idx).asInstanceOf[E]
            accChildNodes.patch(idx, f(che, pathEntry), 1)
        }
        withChildren(thisElem, newChildren)
      }
    }

    final def updateElemsOrSelf(thisElem: E, paths: Set[Path])(f: (E, Path) => E): E = {
      val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

      val descendantUpdateResult =
        updateChildElems(thisElem, pathsByFirstEntry.keySet) {
          case (che, pathEntry) =>
            // Recursive (but non-tail-recursive) call
            updateElemsOrSelf(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
              case (elm, path) =>
                f(elm, path.prepend(pathEntry))
            }
        }

      if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else descendantUpdateResult
    }

    final def updateElems(thisElem: E, paths: Set[Path])(f: (E, Path) => E): E = {
      val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

      updateChildElems(thisElem, pathsByFirstEntry.keySet) {
        case (che, pathEntry) =>
          updateElemsOrSelf(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }
    }

    final def updateElemsOrSelfWithNodeSeq(thisElem: E, paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): immutable.IndexedSeq[N] = {
      val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

      val descendantUpdateResult =
        updateChildElemsWithNodeSeq(thisElem, pathsByFirstEntry.keySet) {
          case (che, pathEntry) =>
            // Recursive (but non-tail-recursive) call
            updateElemsOrSelfWithNodeSeq(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
              case (elm, path) =>
                f(elm, path.prepend(pathEntry))
            }
        }

      if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else Vector(descendantUpdateResult)
    }

    final def updateElemsWithNodeSeq(thisElem: E, paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E = {
      val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

      updateChildElemsWithNodeSeq(thisElem, pathsByFirstEntry.keySet) {
        case (che, pathEntry) =>
          updateElemsOrSelfWithNodeSeq(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }
    }

    final def updateChildElems(thisElem: E, f: (E, Path.Entry) => Option[E]): E = {
      val editsByPathEntries: Map[Path.Entry, E] =
        findAllChildElemsWithPathEntries(thisElem).flatMap({ case (che, pe) => f(che, pe).map(newE => (pe, newE)) }).toMap

      updateChildElems(thisElem, editsByPathEntries.keySet) {
        case (che, pe) =>
          editsByPathEntries.getOrElse(pe, che)
      }
    }

    final def updateChildElemsWithNodeSeq(thisElem: E, f: (E, Path.Entry) => Option[immutable.IndexedSeq[N]]): E = {
      val editsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[N]] =
        findAllChildElemsWithPathEntries(thisElem).flatMap({ case (che, pe) => f(che, pe).map(newNodes => (pe, newNodes)) }).toMap

      updateChildElemsWithNodeSeq(thisElem, editsByPathEntries.keySet) {
        case (che, pe) =>
          editsByPathEntries.getOrElse(pe, immutable.IndexedSeq(che))
      }
    }

    final def updateTopmostElemsOrSelf(thisElem: E, f: (E, Path) => Option[E]): E = {
      // Code repetition, but clear
      val mutableEditsByPaths = mutable.Map[Path, E]()

      val foundElems =
        underlyingElementFunctionApi.findTopmostElemsOrSelf((thisElem, Path.Empty), { elm =>
          val optResult = f(elm._1, elm._2)
          if (optResult.isDefined) {
            mutableEditsByPaths += (elm._2 -> optResult.get)
          }
          optResult.isDefined
        })

      val editsByPaths = mutableEditsByPaths.toMap

      updateElemsOrSelf(thisElem, editsByPaths.keySet) {
        case (elm, path) => editsByPaths.getOrElse(path, elm)
      }
    }

    final def updateTopmostElems(thisElem: E, f: (E, Path) => Option[E]): E = {
      // Code repetition, but clear
      val mutableEditsByPaths = mutable.Map[Path, E]()

      val foundElems =
        underlyingElementFunctionApi.findTopmostElems((thisElem, Path.Empty), { elm =>
          val optResult = f(elm._1, elm._2)
          if (optResult.isDefined) {
            mutableEditsByPaths += (elm._2 -> optResult.get)
          }
          optResult.isDefined
        })

      val editsByPaths = mutableEditsByPaths.toMap

      updateElems(thisElem, editsByPaths.keySet) {
        case (elm, path) => editsByPaths.getOrElse(path, elm)
      }
    }

    final def updateTopmostElemsOrSelfWithNodeSeq(thisElem: E, f: (E, Path) => Option[immutable.IndexedSeq[N]]): immutable.IndexedSeq[N] = {
      // Code repetition, but clear
      val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[N]]()

      val foundElems =
        underlyingElementFunctionApi.findTopmostElemsOrSelf((thisElem, Path.Empty), { elm =>
          val optResult = f(elm._1, elm._2)
          if (optResult.isDefined) {
            mutableEditsByPaths += (elm._2 -> optResult.get)
          }
          optResult.isDefined
        })

      val editsByPaths = mutableEditsByPaths.toMap

      updateElemsOrSelfWithNodeSeq(thisElem, editsByPaths.keySet) {
        case (elm, path) => editsByPaths.getOrElse(path, immutable.IndexedSeq(elm))
      }
    }

    final def updateTopmostElemsWithNodeSeq(thisElem: E, f: (E, Path) => Option[immutable.IndexedSeq[N]]): E = {
      // Code repetition, but clear
      val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[N]]()

      val foundElems =
        underlyingElementFunctionApi.findTopmostElems((thisElem, Path.Empty), { elm =>
          val optResult = f(elm._1, elm._2)
          if (optResult.isDefined) {
            mutableEditsByPaths += (elm._2 -> optResult.get)
          }
          optResult.isDefined
        })

      val editsByPaths = mutableEditsByPaths.toMap

      updateElemsWithNodeSeq(thisElem, editsByPaths.keySet) {
        case (elm, path) => editsByPaths.getOrElse(path, immutable.IndexedSeq(elm))
      }
    }
  }
}
