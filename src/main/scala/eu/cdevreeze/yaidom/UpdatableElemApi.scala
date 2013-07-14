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

import scala.collection.immutable

/**
 * "Updatable" element. It defines a contract for "functional updates". See [[eu.cdevreeze.yaidom.UpdatableElemLike]].
 *
 * For the conceptual difference with "transformable" elements, see trait [[eu.cdevreeze.yaidom.TransformableElemApi]].
 *
 * This purely abstract query API trait leaves the implementation completely open. For example, an implementation backed by
 * an XML database would not use the ``UpdatableElemLike`` implementation, for reasons of efficiency.
 *
 * There are 2 groups of "functional update" methods that work with `ElemPath` instances:
 * <ul>
 * <li>Overloaded `updated` methods. They use an "update function" from elements to elements, and call it on the root element as well.</li>
 * <li>Overloaded `updatedWithNodeSeq` methods. They use an "update function" from elements to node sequences, and do not call it on the root element.</li>
 * </ul>
 *
 * The second group of "functional update" methods can be implemented in terms of the first group of methods. The second group of
 * methods allow for flexible "functional updates", because an element can be "replaced" by an arbitrary sequence of nodes.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemApi[N, E <: N with UpdatableElemApi[N, E]] extends PathAwareElemApi[E] { self: E =>

  /** Returns the child nodes of this element, in the correct order */
  def children: immutable.IndexedSeq[N]

  /** Returns an element with the same name, attributes and scope as this element, but with the given child nodes */
  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  /**
   * Returns a Map from path entries (with respect to this element as parent element) to child node indexes.
   * The faster this method is, the better.
   */
  def childNodeIndexesByPathEntries: Map[ElemPath.Entry, Int]

  /**
   * Shorthand for `childNodeIndexesByPathEntries.getOrElse(childPathEntry, -1)`.
   * The faster this method is, the better.
   */
  def childNodeIndex(childPathEntry: ElemPath.Entry): Int

  /** Shorthand for `withChildren(children.updated(index, newChild))` */
  def withUpdatedChildren(index: Int, newChild: N): E

  /** Shorthand for `withChildren(children.patch(from, newChildren, replace))` */
  def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E

  /** Returns a copy in which the given child has been inserted at the given position (0-based) */
  def plusChild(index: Int, child: N): E

  /** Returns a copy in which the given child has been inserted at the end */
  def plusChild(child: N): E

  /** Returns a copy in which the child at the given position (0-based) has been removed */
  def minusChild(index: Int): E

  /**
   * '''Core method''' that "functionally updates" the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.ElemPath.Entry]] (compared to this element as root).
   *
   * The method throws an exception if no element is found with the given path entry.
   *
   * It can be defined as follows:
   * {{{
   * val idx = self.childNodeIndex(pathEntry)
   * self.withUpdatedChildren(idx, f(children(idx).asInstanceOf[E]))
   * }}}
   */
  def updated(pathEntry: ElemPath.Entry)(f: E => E): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all child elements with the given path entries (compared to this element as root).
   *
   * It can be defined as follows (ignoring exceptions):
   * {{{
   * val newChildren = childNodeIndexesByPathEntries.filterKeys(pathEntries).foldLeft(children) {
   *   case (acc, (pathEntry, idx)) =>
   *     acc.updated(idx, f(acc(idx).asInstanceOf[E], pathEntry))
   * }
   * withChildren(newChildren)
   * }}}
   */
  def updatedAtPathEntries(pathEntries: Set[ElemPath.Entry])(f: (E, ElemPath.Entry) => E): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root).
   *
   * The method throws an exception if no element is found with the given path.
   *
   * It can be defined (recursively) as follows:
   * {{{
   * if (path == ElemPath.Root) f(self)
   * else updated(path.firstEntry) { e => e.updated(path.withoutFirstEntry)(f) }
   * }}}
   */
  def updated(path: ElemPath)(f: E => E): E

  /** Returns `updated(path) { e => newElem }` */
  def updated(path: ElemPath, newElem: E): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all descendant-or-self elements with the given paths (compared to this element as root).
   *
   * It can be defined (recursively) as follows (ignoring exceptions):
   * {{{
   * def updatedAtPaths(paths: Set[ElemPath])(f: (E, ElemPath) => E): E = {
   *   val pathsByPathEntries = paths.filter(path => !path.isRoot).groupBy(path => path.firstEntry)
   *   val resultWithoutSelf = self.updatedAtPathEntries(pathsByPathEntries.keySet) { (che, pathEntry) =>
   *     val che = findWithElemPathEntry(pathEntry).get
   *     val newChe = che.updatedAtPaths(paths.map(_.withoutFirstEntry)) { (elem, relativePath) =>
   *       f(elem, relativePath.prepend(pathEntry))
   *     }
   *     newChe
   *   }
   *   if (paths.contains(ElemPath.Root)) f(resultWithoutSelf, ElemPath.Root) else resultWithoutSelf
   * }
   * }}}
   *
   * It is also equivalent to:
   * {{{
   * val pathsReversed = findAllElemOrSelfPaths.filter(p => paths.contains(p)).reverse
   * pathsReversed.foldLeft(self) { case (acc, path) => acc.updated(path) { e => f(e, path) } }
   * }}}
   */
  def updatedAtPaths(paths: Set[ElemPath])(f: (E, ElemPath) => E): E

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root). If the given path is the
   * root path, this element itself is returned unchanged.
   *
   * This function could be defined as follows:
   * {{{
   * // First define function g as follows:
   *
   * def g(e: Elem): Elem = {
   *   if (path == ElemPath.Root) e
   *   else {
   *     e.withPatchedChildren(
   *       e.childNodeIndex(path.lastEntry),
   *       f(e.findWithElemPathEntry(path.lastEntry).get),
   *       1)
   *   }
   * }
   *
   * // Then the function updatedWithNodeSeq(path)(f) could be defined as:
   *
   * updated(path.parentPathOption.getOrElse(ElemPath.Root))(g)
   * }}}
   * After all, this is just a functional update that replaces the parent element, if it exists.
   *
   * The method throws an exception if no element is found with the given path.
   */
  def updatedWithNodeSeq(path: ElemPath)(f: E => immutable.IndexedSeq[N]): E

  /** Returns `updatedWithNodeSeq(path) { e => newNodes }` */
  def updatedWithNodeSeq(path: ElemPath, newNodes: immutable.IndexedSeq[N]): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all child elements with the given path entries (compared to this element as root).
   *
   * It can be defined as follows (ignoring exceptions):
   * {{{
   * val indexesByPathEntries = childNodeIndexesByPathEntries.filterKeys(pathEntries)
   * val newChildGroups =
   *   indexesByPathEntries.foldLeft(self.children.map(n => immutable.IndexedSeq(n))) {
   *     case (acc, (pathEntry, idx)) =>
   *       acc.updated(idx, f(acc(idx).head.asInstanceOf[E], pathEntry))
   *   }
   * withChildren(newChildGroups.flatten)
   * }}}
   */
  def updatedWithNodeSeqAtPathEntries(pathEntries: Set[ElemPath.Entry])(f: (E, ElemPath.Entry) => immutable.IndexedSeq[N]): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all descendant elements with the given paths (compared to this element as root), but ignoring the root path.
   *
   * It can be defined as follows (ignoring exceptions):
   * {{{
   * val pathsByParentPaths = paths.filter(path => !path.isRoot).groupBy(path => path.parentPath)
   * self.updatedAtPaths(pathsByParentPaths.keySet) { (elem, path) =>
   *   val childPathEntries = pathsByParentPaths(path).map(_.lastEntry)
   *   elem.updatedWithNodeSeqAtPathEntries(childPathEntries) { (che, pathEntry) =>
   *     f(che, path.append(pathEntry))
   *   }
   * }
   * }}}
   */
  def updatedWithNodeSeqAtPaths(paths: Set[ElemPath])(f: (E, ElemPath) => immutable.IndexedSeq[N]): E
}
