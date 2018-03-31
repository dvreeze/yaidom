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

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path

/**
 * This is the partially implemented (functional) element update API, as function API instead of OO API. That is, this is the function
 * API corresponding to trait [[eu.cdevreeze.yaidom.queryapi.UpdatableElemLike]].
 *
 * @author Chris de Vreeze
 */
trait ElemUpdateLike extends ElemUpdateApi {

  type NodeType

  type ElemType <: NodeType

  def children(elem: ElemType): immutable.IndexedSeq[NodeType]

  def withChildren(elem: ElemType, newChildren: immutable.IndexedSeq[NodeType]): ElemType

  def collectChildNodeIndexes(elem: ElemType, pathEntries: Set[Path.Entry]): Map[Path.Entry, Int]

  def findAllChildElemsWithPathEntries(elem: ElemType): immutable.IndexedSeq[(ElemType, Path.Entry)]

  final def childNodeIndex(elem: ElemType, pathEntry: Path.Entry): Int = {
    collectChildNodeIndexes(elem, Set(pathEntry)).getOrElse(pathEntry, -1)
  }

  final def withChildSeqs(elem: ElemType, newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[NodeType]]): ElemType = {
    withChildren(elem, newChildSeqs.flatten)
  }

  final def withUpdatedChildren(elem: ElemType, index: Int, newChild: NodeType): ElemType = {
    withChildren(elem, children(elem).updated(index, newChild))
  }

  final def withPatchedChildren(elem: ElemType, from: Int, newChildren: immutable.IndexedSeq[NodeType], replace: Int): ElemType = {
    withChildren(elem, children(elem).patch(from, newChildren, replace))
  }

  final def plusChild(elem: ElemType, index: Int, child: NodeType): ElemType = {
    val childNodes = children(elem)

    require(
      index <= childNodes.size,
      s"Expected index $index to be at most the number of children: ${childNodes.size}")

    if (index == childNodes.size) {
      plusChild(elem, child)
    } else {
      withPatchedChildren(elem, index, Vector(child, childNodes(index)), 1)
    }
  }

  final def plusChild(elem: ElemType, child: NodeType): ElemType = {
    withChildren(elem, children(elem) :+ child)
  }

  final def plusChildOption(elem: ElemType, index: Int, childOption: Option[NodeType]): ElemType = {
    if (childOption.isEmpty) elem else plusChild(elem, index, childOption.get)
  }

  final def plusChildOption(elem: ElemType, childOption: Option[NodeType]): ElemType = {
    if (childOption.isEmpty) elem else plusChild(elem, childOption.get)
  }

  final def plusChildren(elem: ElemType, childSeq: immutable.IndexedSeq[NodeType]): ElemType = {
    withChildren(elem, children(elem) ++ childSeq)
  }

  final def minusChild(elem: ElemType, index: Int): ElemType = {
    val childNodes = children(elem)

    require(
      index < childNodes.size,
      s"Expected index $index to be less than the number of children: ${childNodes.size}")

    withPatchedChildren(elem, index, Vector(), 1)
  }

  final def updateChildElem(elem: ElemType, pathEntry: Path.Entry)(f: ElemType => ElemType): ElemType = {
    updateChildElems(elem, Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElem(elem: ElemType, pathEntry: Path.Entry, newElem: ElemType): ElemType = {
    updateChildElem(elem, pathEntry) { e => newElem }
  }

  final def updateChildElemWithNodeSeq(elem: ElemType, pathEntry: Path.Entry)(f: ElemType => immutable.IndexedSeq[NodeType]): ElemType = {
    updateChildElemsWithNodeSeq(elem, Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElemWithNodeSeq(elem: ElemType, pathEntry: Path.Entry, newNodes: immutable.IndexedSeq[NodeType]): ElemType = {
    updateChildElemWithNodeSeq(elem, pathEntry) { e => newNodes }
  }

  final def updateElemOrSelf(elem: ElemType, path: Path)(f: ElemType => ElemType): ElemType = {
    updateElemsOrSelf(elem, Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemOrSelf(elem: ElemType, path: Path, newElem: ElemType): ElemType = {
    updateElemOrSelf(elem, path) { e => newElem }
  }

  final def updateElemWithNodeSeq(elem: ElemType, path: Path)(f: ElemType => immutable.IndexedSeq[NodeType]): ElemType = {
    updateElemsWithNodeSeq(elem, Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemWithNodeSeq(elem: ElemType, path: Path, newNodes: immutable.IndexedSeq[NodeType]): ElemType = {
    updateElemWithNodeSeq(elem, path) { e => newNodes }
  }

  final def updateChildElems(elem: ElemType, pathEntries: Set[Path.Entry])(f: (ElemType, Path.Entry) => ElemType): ElemType = {
    // For efficiency, not delegating to updateChildElemsWithNodeSeq

    if (pathEntries.isEmpty) {
      elem
    } else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(elem, pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(children(elem)) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[ElemType]
          accChildNodes.updated(idx, f(che, pathEntry))
      }
      withChildren(elem, newChildren)
    }
  }

  final def updateChildElemsWithNodeSeq(elem: ElemType, pathEntries: Set[Path.Entry])(f: (ElemType, Path.Entry) => immutable.IndexedSeq[NodeType]): ElemType = {
    if (pathEntries.isEmpty) {
      elem
    } else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(elem, pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(children(elem)) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[ElemType]
          accChildNodes.patch(idx, f(che, pathEntry), 1)
      }
      withChildren(elem, newChildren)
    }
  }

  final def updateElemsOrSelf(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => ElemType): ElemType = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    val descendantUpdateResult =
      updateChildElems(elem, pathsByFirstEntry.keySet) {
        case (che, pathEntry) =>
          // Recursive (but non-tail-recursive) call
          updateElemsOrSelf(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }

    if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else descendantUpdateResult
  }

  final def updateElems(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => ElemType): ElemType = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElems(elem, pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        updateElemsOrSelf(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateElemsOrSelfWithNodeSeq(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => immutable.IndexedSeq[NodeType]): immutable.IndexedSeq[NodeType] = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    val descendantUpdateResult =
      updateChildElemsWithNodeSeq(elem, pathsByFirstEntry.keySet) {
        case (che, pathEntry) =>
          // Recursive (but non-tail-recursive) call
          updateElemsOrSelfWithNodeSeq(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
            case (elm, path) =>
              f(elm, path.prepend(pathEntry))
          }
      }

    if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else Vector(descendantUpdateResult)
  }

  final def updateElemsWithNodeSeq(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => immutable.IndexedSeq[NodeType]): ElemType = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElemsWithNodeSeq(elem, pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        updateElemsOrSelfWithNodeSeq(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateChildElems(elem: ElemType, f: (ElemType, Path.Entry) => Option[ElemType]): ElemType = {
    val editsByPathEntries: Map[Path.Entry, ElemType] =
      findAllChildElemsWithPathEntries(elem).flatMap({ case (che, pe) => f(che, pe).map(newE => (pe, newE)) }).toMap

    updateChildElems(elem, editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, che)
    }
  }

  final def updateChildElemsWithNodeSeq(elem: ElemType, f: (ElemType, Path.Entry) => Option[immutable.IndexedSeq[NodeType]]): ElemType = {
    val editsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[NodeType]] =
      findAllChildElemsWithPathEntries(elem).flatMap({ case (che, pe) => f(che, pe).map(newNodes => (pe, newNodes)) }).toMap

    updateChildElemsWithNodeSeq(elem, editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, immutable.IndexedSeq(che))
    }
  }
}

object ElemUpdateLike {

  /**
   * This query API type, restricting NodeType and ElemType to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemUpdateLike { type NodeType = N; type ElemType = E }
}
