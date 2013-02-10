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
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root).
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
}
