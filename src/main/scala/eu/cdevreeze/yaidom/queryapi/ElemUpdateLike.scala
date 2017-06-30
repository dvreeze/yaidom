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

  type Node

  type Elem <: Node

  def children(elem: Elem): immutable.IndexedSeq[Node]

  def withChildren(elem: Elem, newChildren: immutable.IndexedSeq[Node]): Elem

  def collectChildNodeIndexes(elem: Elem, pathEntries: Set[Path.Entry]): Map[Path.Entry, Int]

  def findAllChildElemsWithPathEntries(elem: Elem): immutable.IndexedSeq[(Elem, Path.Entry)]

  final def childNodeIndex(elem: Elem, pathEntry: Path.Entry): Int = {
    collectChildNodeIndexes(elem, Set(pathEntry)).getOrElse(pathEntry, -1)
  }

  final def withChildSeqs(elem: Elem, newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[Node]]): Elem = {
    withChildren(elem, newChildSeqs.flatten)
  }

  final def withUpdatedChildren(elem: Elem, index: Int, newChild: Node): Elem = {
    withChildren(elem, children(elem).updated(index, newChild))
  }

  final def withPatchedChildren(elem: Elem, from: Int, newChildren: immutable.IndexedSeq[Node], replace: Int): Elem = {
    withChildren(elem, children(elem).patch(from, newChildren, replace))
  }

  final def plusChild(elem: Elem, index: Int, child: Node): Elem = {
    val childNodes = children(elem)

    require(
      index <= childNodes.size,
      s"Expected index $index to be at most the number of children: ${childNodes.size}")

    if (index == childNodes.size) plusChild(elem, child)
    else withPatchedChildren(elem, index, Vector(child, childNodes(index)), 1)
  }

  final def plusChild(elem: Elem, child: Node): Elem = {
    withChildren(elem, children(elem) :+ child)
  }

  final def plusChildOption(elem: Elem, index: Int, childOption: Option[Node]): Elem = {
    if (childOption.isEmpty) elem else plusChild(elem, index, childOption.get)
  }

  final def plusChildOption(elem: Elem, childOption: Option[Node]): Elem = {
    if (childOption.isEmpty) elem else plusChild(elem, childOption.get)
  }

  final def plusChildren(elem: Elem, childSeq: immutable.IndexedSeq[Node]): Elem = {
    withChildren(elem, children(elem) ++ childSeq)
  }

  final def minusChild(elem: Elem, index: Int): Elem = {
    val childNodes = children(elem)

    require(
      index < childNodes.size,
      s"Expected index $index to be less than the number of children: ${childNodes.size}")

    withPatchedChildren(elem, index, Vector(), 1)
  }

  final def updateChildElem(elem: Elem, pathEntry: Path.Entry)(f: Elem => Elem): Elem = {
    updateChildElems(elem, Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElem(elem: Elem, pathEntry: Path.Entry, newElem: Elem): Elem = {
    updateChildElem(elem, pathEntry) { e => newElem }
  }

  final def updateChildElemWithNodeSeq(elem: Elem, pathEntry: Path.Entry)(f: Elem => immutable.IndexedSeq[Node]): Elem = {
    updateChildElemsWithNodeSeq(elem, Set(pathEntry)) { case (che, pe) => f(che) }
  }

  final def updateChildElemWithNodeSeq(elem: Elem, pathEntry: Path.Entry, newNodes: immutable.IndexedSeq[Node]): Elem = {
    updateChildElemWithNodeSeq(elem, pathEntry) { e => newNodes }
  }

  final def updateElemOrSelf(elem: Elem, path: Path)(f: Elem => Elem): Elem = {
    updateElemsOrSelf(elem, Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemOrSelf(elem: Elem, path: Path, newElem: Elem): Elem = {
    updateElemOrSelf(elem, path) { e => newElem }
  }

  final def updateElemWithNodeSeq(elem: Elem, path: Path)(f: Elem => immutable.IndexedSeq[Node]): Elem = {
    updateElemsWithNodeSeq(elem, Set(path)) { case (e, path) => f(e) }
  }

  final def updateElemWithNodeSeq(elem: Elem, path: Path, newNodes: immutable.IndexedSeq[Node]): Elem = {
    updateElemWithNodeSeq(elem, path) { e => newNodes }
  }

  final def updateChildElems(elem: Elem, pathEntries: Set[Path.Entry])(f: (Elem, Path.Entry) => Elem): Elem = {
    // For efficiency, not delegating to updateChildElemsWithNodeSeq

    if (pathEntries.isEmpty) elem
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(elem, pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(children(elem)) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[Elem]
          accChildNodes.updated(idx, f(che, pathEntry))
      }
      withChildren(elem, newChildren)
    }
  }

  final def updateChildElemsWithNodeSeq(elem: Elem, pathEntries: Set[Path.Entry])(f: (Elem, Path.Entry) => immutable.IndexedSeq[Node]): Elem = {
    if (pathEntries.isEmpty) elem
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        collectChildNodeIndexes(elem, pathEntries).toSeq.sortBy(_._2)

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(children(elem)) {
        case (accChildNodes, (pathEntry, idx)) =>
          val che = accChildNodes(idx).asInstanceOf[Elem]
          accChildNodes.patch(idx, f(che, pathEntry), 1)
      }
      withChildren(elem, newChildren)
    }
  }

  final def updateElemsOrSelf(elem: Elem, paths: Set[Path])(f: (Elem, Path) => Elem): Elem = {
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

  final def updateElems(elem: Elem, paths: Set[Path])(f: (Elem, Path) => Elem): Elem = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElems(elem, pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        updateElemsOrSelf(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateElemsOrSelfWithNodeSeq(elem: Elem, paths: Set[Path])(f: (Elem, Path) => immutable.IndexedSeq[Node]): immutable.IndexedSeq[Node] = {
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

  final def updateElemsWithNodeSeq(elem: Elem, paths: Set[Path])(f: (Elem, Path) => immutable.IndexedSeq[Node]): Elem = {
    val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)

    updateChildElemsWithNodeSeq(elem, pathsByFirstEntry.keySet) {
      case (che, pathEntry) =>
        updateElemsOrSelfWithNodeSeq(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
          case (elm, path) =>
            f(elm, path.prepend(pathEntry))
        }
    }
  }

  final def updateChildElems(elem: Elem, f: (Elem, Path.Entry) => Option[Elem]): Elem = {
    val editsByPathEntries: Map[Path.Entry, Elem] =
      findAllChildElemsWithPathEntries(elem).flatMap({ case (che, pe) => f(che, pe).map(newE => (pe, newE)) }).toMap

    updateChildElems(elem, editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, che)
    }
  }

  final def updateChildElemsWithNodeSeq(elem: Elem, f: (Elem, Path.Entry) => Option[immutable.IndexedSeq[Node]]): Elem = {
    val editsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[Node]] =
      findAllChildElemsWithPathEntries(elem).flatMap({ case (che, pe) => f(che, pe).map(newNodes => (pe, newNodes)) }).toMap

    updateChildElemsWithNodeSeq(elem, editsByPathEntries.keySet) {
      case (che, pe) =>
        editsByPathEntries.getOrElse(pe, immutable.IndexedSeq(che))
    }
  }
}

object ElemUpdateLike {

  /**
   * This query API type, restricting Node and Elem to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemUpdateLike { type Node = N; type Elem = E }
}
