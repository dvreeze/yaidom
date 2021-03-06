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
 * This is the element (functional) update API, as function API instead of OO API. That is, this is the function API corresponding to
 * trait [[eu.cdevreeze.yaidom.queryapi.UpdatableElemApi]]. A few methods, like `updateTopmostElemsOrSelf`, are missing, though.
 *
 * See trait `UpdatableElemApi` for more info about (functional) element updates in yaidom, and their properties.
 *
 * This functional API is more widely applicable than trait `UpdatableElemApi`. First, it can be implemented for arbitrary
 * element types, even non-yaidom ones. Second, implementations can easily carry state that is shared by update functions, such
 * as a Saxon `Processor` in the case of a Saxon implementation of this API.
 *
 * Below, for most functions that take Paths or that take functions that take Paths the Paths are relative to the first argument
 * element, so they must not be interpreted as the Paths of the elements themselves (relative to their root elements).
 *
 * @author Chris de Vreeze
 */
trait ElemUpdateApi {

  type NodeType

  type ElemType <: NodeType

  /** Returns the child nodes of this element, in the correct order */
  def children(elem: ElemType): immutable.IndexedSeq[NodeType]

  /** Returns an element with the same name, attributes and scope as this element, but with the given child nodes */
  def withChildren(elem: ElemType, newChildren: immutable.IndexedSeq[NodeType]): ElemType

  /**
   * Filters the child elements with the given path entries, and returns a Map from the path entries of those filtered
   * elements to the child node indexes. The result Map has no entries for path entries that cannot be resolved.
   * This method should be fast, especially if the passed path entry set is small.
   */
  def collectChildNodeIndexes(elem: ElemType, pathEntries: Set[Path.Entry]): Map[Path.Entry, Int]

  /**
   * Returns all child elements paired with their path entries.
   */
  def findAllChildElemsWithPathEntries(elem: ElemType): immutable.IndexedSeq[(ElemType, Path.Entry)]

  /**
   * Finds the child node index of the given path entry, or -1 if not found. More precisely, returns:
   * {{{
   * collectChildNodeIndexes(elem, Set(pathEntry)).getOrElse(pathEntry, -1)
   * }}}
   */
  def childNodeIndex(elem: ElemType, pathEntry: Path.Entry): Int

  /** Shorthand for `withChildren(elem, newChildSeqs.flatten)` */
  def withChildSeqs(elem: ElemType, newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[NodeType]]): ElemType

  /** Shorthand for `withChildren(elem, children(elem).updated(index, newChild))` */
  def withUpdatedChildren(elem: ElemType, index: Int, newChild: NodeType): ElemType

  /** Shorthand for `withChildren(elem, children(elem).patch(from, newChildren, replace))` */
  def withPatchedChildren(elem: ElemType, from: Int, newChildren: immutable.IndexedSeq[NodeType], replace: Int): ElemType

  /**
   * Returns a copy in which the given child has been inserted at the given position (0-based).
   * If `index == children(elem).size`, adds the element at the end. If `index > children(elem).size`, throws an exception.
   *
   * Afterwards, the resulting element indeed has the given child at position `index` (0-based).
   */
  def plusChild(elem: ElemType, index: Int, child: NodeType): ElemType

  /** Returns a copy in which the given child has been inserted at the end */
  def plusChild(elem: ElemType, child: NodeType): ElemType

  /**
   * Returns a copy in which the given child, if any, has been inserted at the given position (0-based).
   * That is, returns `plusChild(elem, index, childOption.get)` if the given optional child element is non-empty.
   */
  def plusChildOption(elem: ElemType, index: Int, childOption: Option[NodeType]): ElemType

  /**
   * Returns a copy in which the given child, if any, has been inserted at the end.
   * That is, returns `plusChild(elem, childOption.get)` if the given optional child element is non-empty.
   */
  def plusChildOption(elem: ElemType, childOption: Option[NodeType]): ElemType

  /** Returns a copy in which the given children have been inserted at the end */
  def plusChildren(elem: ElemType, childSeq: immutable.IndexedSeq[NodeType]): ElemType

  /**
   * Returns a copy in which the child at the given position (0-based) has been removed.
   * Throws an exception if `index >= children(elem).size`.
   */
  def minusChild(elem: ElemType, index: Int): ElemType

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.core.Path.Entry]] (compared to this element as root).
   *
   * It can be defined as follows:
   * {{{
   * updateChildElems(elem, Set(pathEntry)) { case (che, pe) => f(che) }
   * }}}
   */
  def updateChildElem(elem: ElemType, pathEntry: Path.Entry)(f: ElemType => ElemType): ElemType

  /** Returns `updateChildElem(elem, pathEntry) { e => newElem }` */
  def updateChildElem(elem: ElemType, pathEntry: Path.Entry, newElem: ElemType): ElemType

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.core.Path.Entry]] (compared to this element as root).
   *
   * It can be defined as follows:
   * {{{
   * updateChildElemsWithNodeSeq(elem, Set(pathEntry)) { case (che, pe) => f(che) }
   * }}}
   */
  def updateChildElemWithNodeSeq(elem: ElemType, pathEntry: Path.Entry)(f: ElemType => immutable.IndexedSeq[NodeType]): ElemType

  /** Returns `updateChildElemWithNodeSeq(elem, pathEntry) { e => newNodes }` */
  def updateChildElemWithNodeSeq(elem: ElemType, pathEntry: Path.Entry, newNodes: immutable.IndexedSeq[NodeType]): ElemType

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.core.Path]] (compared to this element as root).
   *
   * It can be defined as follows:
   * {{{
   * updateElemsOrSelf(elem, Set(path)) { case (e, path) => f(e) }
   * }}}
   */
  def updateElemOrSelf(elem: ElemType, path: Path)(f: ElemType => ElemType): ElemType

  /** Returns `updateElemOrSelf(elem, path) { e => newElem }` */
  def updateElemOrSelf(elem: ElemType, path: Path, newElem: ElemType): ElemType

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.core.Path]] (compared to this element as root). If the given path is the
   * root path, this element itself is returned unchanged.
   *
   * This function could be defined as follows:
   * {{{
   * updateElemsWithNodeSeq(elem, Set(path)) { case (e, path) => f(e) }
   * }}}
   */
  def updateElemWithNodeSeq(elem: ElemType, path: Path)(f: ElemType => immutable.IndexedSeq[NodeType]): ElemType

  /** Returns `updateElemWithNodeSeq(elem, path) { e => newNodes }` */
  def updateElemWithNodeSeq(elem: ElemType, path: Path, newNodes: immutable.IndexedSeq[NodeType]): ElemType

  /**
   * Updates the child elements with the given path entries, applying the passed update function.
   *
   * That is, returns the equivalent of:
   * {{{
   * updateChildElemsWithNodeSeq(elem, pathEntries) { case (che, pe) => Vector(f(che, pe)) }
   * }}}
   *
   * If the set of path entries is small, this method is rather efficient.
   */
  def updateChildElems(elem: ElemType, pathEntries: Set[Path.Entry])(f: (ElemType, Path.Entry) => ElemType): ElemType

  /**
   * '''Updates the child elements with the given path entries''', applying the passed update function.
   * This is the '''core''' method of the update API, and the other methods have implementations that
   * directly or indirectly depend on this method.
   *
   * That is, returns:
   * {{{
   * if (pathEntries.isEmpty) elem
   * else {
   *   val indexesByPathEntries: Seq[(Path.Entry, Int)] =
   *     collectChildNodeIndexes(elem, pathEntries).toSeq.sortBy(_._2)
   *
   *   // Updating in reverse order of indexes, in order not to invalidate the path entries
   *   val newChildren = indexesByPathEntries.reverse.foldLeft(children(elem)) {
   *     case (accChildNodes, (pathEntry, idx)) =>
   *       val che = accChildNodes(idx).asInstanceOf[ElemType]
   *       accChildNodes.patch(idx, f(che, pathEntry), 1)
   *   }
   *   withChildren(elem, newChildren)
   * }
   * }}}
   *
   * If the set of path entries is small, this method is rather efficient.
   */
  def updateChildElemsWithNodeSeq(elem: ElemType, pathEntries: Set[Path.Entry])(f: (ElemType, Path.Entry) => immutable.IndexedSeq[NodeType]): ElemType

  /**
   * Updates the descendant-or-self elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] =
   *   paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * val descendantUpdateResult =
   *   updateChildElems(elem, pathsByFirstEntry.keySet) {
   *     case (che, pathEntry) =>
   *       // Recursive (but non-tail-recursive) call
   *       updateElemsOrSelf(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *         case (elm, path) =>
   *           f(elm, path.prepend(pathEntry))
   *       }
   *   }
   *
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty)
   * else descendantUpdateResult
   * }}}
   *
   * In other words, returns:
   * {{{
   * val descendantUpdateResult = updateElems(elem, paths)(f)
   *
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty)
   * else descendantUpdateResult
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElemsOrSelf(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => ElemType): ElemType

  /**
   * Updates the descendant elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] =
   *   paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * updateChildElems(elem, pathsByFirstEntry.keySet) {
   *   case (che, pathEntry) =>
   *     updateElemsOrSelf(che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *       case (elm, path) =>
   *         f(elm, path.prepend(pathEntry))
   *     }
   * }
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElems(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => ElemType): ElemType

  /**
   * Updates the descendant-or-self elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] =
   *   paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * val descendantUpdateResult =
   *   updateChildElemsWithNodeSeq(elem, pathsByFirstEntry.keySet) {
   *     case (che, pathEntry) =>
   *       // Recursive (but non-tail-recursive) call
   *       updateElemsOrSelfWithNodeSeq(
   *         che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *         case (elm, path) =>
   *           f(elm, path.prepend(pathEntry))
   *       }
   *   }
   *
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty)
   * else Vector(descendantUpdateResult)
   * }}}
   *
   * In other words, returns:
   * {{{
   * val descendantUpdateResult = updateElemsWithNodeSeq(elem, paths)(f)
   *
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty)
   * else Vector(descendantUpdateResult)
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElemsOrSelfWithNodeSeq(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => immutable.IndexedSeq[NodeType]): immutable.IndexedSeq[NodeType]

  /**
   * Updates the descendant elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] =
   *   paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * updateChildElemsWithNodeSeq(elem, pathsByFirstEntry.keySet) {
   *   case (che, pathEntry) =>
   *     updateElemsOrSelfWithNodeSeq(
   *       che, pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *       case (elm, path) =>
   *         f(elm, path.prepend(pathEntry))
   *     }
   * }
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElemsWithNodeSeq(elem: ElemType, paths: Set[Path])(f: (ElemType, Path) => immutable.IndexedSeq[NodeType]): ElemType

  /**
   * Invokes `updateChildElems`, passing the path entries for which the passed function is defined. It is equivalent to:
   * {{{
   * val editsByPathEntries: Map[Path.Entry, ElemType] =
   *   findAllChildElemsWithPathEntries(elem).flatMap({ case (che, pe) =>
   *     f(che, pe).map(newE => (pe, newE)) }).toMap
   *
   * updateChildElems(elem, editsByPathEntries.keySet) { case (che, pe) =>
   *   editsByPathEntries.getOrElse(pe, che) }
   * }}}
   */
  def updateChildElems(elem: ElemType, f: (ElemType, Path.Entry) => Option[ElemType]): ElemType

  /**
   * Invokes `updateChildElemsWithNodeSeq`, passing the path entries for which the passed function is defined. It is equivalent to:
   * {{{
   * val editsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[NodeType]] =
   *   findAllChildElemsWithPathEntries(elem).flatMap({ case (che, pe) =>
   *     f(che, pe).map(newNodes => (pe, newNodes)) }).toMap
   *
   * updateChildElemsWithNodeSeq(elem, editsByPathEntries.keySet) { case (che, pe) =>
   *   editsByPathEntries.getOrElse(pe, immutable.IndexedSeq(che))
   * }
   * }}}
   */
  def updateChildElemsWithNodeSeq(elem: ElemType, f: (ElemType, Path.Entry) => Option[immutable.IndexedSeq[NodeType]]): ElemType
}

object ElemUpdateApi {

  /**
   * This query API type, restricting NodeType and ElemType to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ElemUpdateApi { type NodeType = N; type ElemType = E }
}
