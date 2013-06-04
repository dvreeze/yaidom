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
 * This purely abstract query API trait leaves the implementation completely open. For example, an implementation backed by
 * an XML database would not use the ``UpdatableElemLike`` implementation, for reasons of efficiency.
 *
 * There are 2 groups of "functional update" methods that work with `ElemPath` instances (implicitly or explicitly:
 * <ul>
 * <li>Overloaded `updated` methods. They use an "update function" from elements to elements, and call it on the root element as well.</li>
 * <li>Overloaded `updatedWithNodeSeq` methods. They use an "update function" from elements to node sequences, and do not call it on the root element.</li>
 * </ul>
 *
 * The second group of "functional update" methods can be implemented in terms of the first group of methods. The second group of
 * methods allow for flexible "functional updates", because an element can be "replaced" by an arbitrary sequence of nodes.
 * For example, with the `updatedWithNodeSeq` (and `topmostUpdatedWithNodeSeq`) functions, it is easy to write functions to
 * functionally delete elements, insert nodes before or after an element, etc.
 *
 * Below follow some formal properties that the "functional update" support obeys.
 *
 * For example, the following property (trivially) holds:
 * {{{
 * // First define pf2, and let E be type Elem (it could be another element type as well, of course)
 *
 * val pf2: PartialFunction[Elem, Elem] = {
 *   case e: Elem if pf.isDefinedAt(e) && !elem.filterElemsOrSelf(e2 => pf.isDefinedAt(e2)).contains(e) => pf(e)
 * }
 *
 * // Then the following holds (in terms of '=='):
 *
 * elem.topmostUpdated(pf) == elem.updated(pf2)
 * }}}
 * 
 * An analogous property holds for `topmostUpdatedWithNodeSeq` (taking a partial function) in terms of `updatedWithNodeSeq`.
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
   * Returns the child node index of the given `ElemPath.Entry` with respect to this element as parent element.
   * If the path entry is not found, -1 is returned.
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
   * to the element that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root).
   *
   * The method throws an exception if no element is found with the given path.
   */
  def updated(path: ElemPath)(f: E => E): E

  /** Returns `updated(path) { e => newElem }` */
  def updated(path: ElemPath, newElem: E): E

  /**
   * Functionally updates the descendant-or-self elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = filterElemOrSelfPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updated(path, pf(e))
   * }
   * }}}
   */
  def updated(pf: PartialFunction[E, E]): E

  /**
   * Functionally updates the topmost descendant-or-self elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = findTopmostElemOrSelfPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updated(path, pf(e))
   * }
   * }}}
   */
  def topmostUpdated(pf: PartialFunction[E, E]): E

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root). If the given path is the
   * root path, this element itself is returned unchanged.
   *
   * This function is equivalent to:
   * {{{
   * if (path == ElemPath.Root) self
   * else {
   *   val parentElem = getWithElemPath(path.parentPath)
   *   val childNodeIndex = parentElem.childNodeIndex(path.lastEntry)
   *   val childElem = parentElem.findWithElemPathEntry(path.lastEntry).get
   *
   *   updated(parentPath, parentElem.withPatchedChildren(childNodeIndex, f(childElem), 1))
   * }
   * }}}
   * After all, this is just a functional update that replaces the parent element.
   *
   * The method throws an exception if no element is found with the given path.
   */
  def updatedWithNodeSeq(path: ElemPath)(f: E => immutable.IndexedSeq[N]): E

  /** Returns `updatedWithNodeSeq(path) { e => newNodes }` */
  def updatedWithNodeSeq(path: ElemPath, newNodes: immutable.IndexedSeq[N]): E

  /**
   * Functionally updates the descendant elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = filterElemPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updatedWithNodeSeq(path, pf(e))
   * }
   * }}}
   */
  def updatedWithNodeSeq(pf: PartialFunction[E, immutable.IndexedSeq[N]]): E

  /**
   * Functionally updates the topmost descendant elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = findTopmostElemPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updatedWithNodeSeq(path, pf(e))
   * }
   * }}}
   */
  def topmostUpdatedWithNodeSeq(pf: PartialFunction[E, immutable.IndexedSeq[N]]): E
}
