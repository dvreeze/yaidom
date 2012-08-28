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
 * "Updatable" element. It defines a contract for "functional updates".
 *
 * This trait is a sub-trait of [[eu.cdevreeze.yaidom.ElemLike]]. It adds a type parameter for (arbitrary) nodes.
 * It also requires concrete implementations for abstract methods `children`, `withChildren`, `childNodeIndex` and `findChildPathEntry`.
 * Based on these 4 methods, and super-trait `ElemLike`, this trait offers a reasonably rich API for "functionally updating" elements.
 *
 * This trait adds the following groups of methods to the methods offered by the supertrait `ElemLike`:
 * <ul>
 * <li>Convenience methods for functional updates given a child node index (range)</li>
 * <li>Methods for functional updates given an `ElemPath`</li>
 * <li>A method for functional updates, given a partial function from elements to node collections</li>
 * </ul>
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends ElemLike[E] { self: E =>

  // See https://pario.zendesk.com/entries/20124208-lesson-6-complex-xslt-example for an example of what we must be able to express easily.

  /** Returns the child nodes of this element, in the correct order */
  def children: immutable.IndexedSeq[N]

  /** Returns an element with the same name, attributes and scope as this element, but with the given child nodes */
  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  /**
   * Returns the child node index of the given `ElemPath.Entry` with respect to this element as parent element.
   * If the path entry is not found, -1 is returned.
   *
   * Methods `updated` (taking `ElemPath`s) heavily use this method to turn `ElemPath`s into child node indexes.
   * This makes indexing using `ElemPath`s slow, because this method is O(n).
   */
  def childNodeIndex(childPathEntry: ElemPath.Entry): Int

  /**
   * The inverse of `childNodeIndex`, wrapped in an Option. If the child node at the given index is not an element, None is returned.
   */
  def findChildPathEntry(idx: Int): Option[ElemPath.Entry]

  /** Shorthand for `withChildren(children.updated(index, newChild))` */
  final def withUpdatedChildren(index: Int, newChild: N): E =
    withChildren(children.updated(index, newChild))

  /** Shorthand for `withChildren(children.patch(from, newChildren, replace))` */
  final def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E =
    withChildren(children.patch(from, newChildren, replace))

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root).
   *
   * The root element must remain the same, except for (one of its) children. Hence the root path is not allowed as parameter.
   *
   * The method throws an exception if no element is found with the given path, or if the root path is passed.
   */
  final def updated(path: ElemPath)(f: E => immutable.IndexedSeq[N]): E = {
    // This implementation has been inspired by Scala's immutable Vector, which offers efficient
    // "functional updates" (among other efficient operations).

    require(!path.isRoot, "We cannot update the root element itself (except for its children) within the tree with that root element")

    val foundChildElm: E = self.findWithElemPathEntry(path.firstEntry).getOrElse(sys.error("No child element found with path %s".format(path)))
    val foundChildElmNodeIdx = self.childNodeIndex(path.firstEntry)
    assert(foundChildElmNodeIdx >= 0, "Expected non-negative child node index")

    val updatedChildren: immutable.IndexedSeq[N] =
      if (path.entries.length == 1) {
        val newNodes: immutable.IndexedSeq[N] = f(foundChildElm)

        self.children.patch(foundChildElmNodeIdx, newNodes, 1)
      } else {
        assert(path.entries.length >= 2)

        // Recursive, but not tail-recursive
        val updatedTree: E = foundChildElm.updated(path.withoutFirstEntry)(f)

        self.children.updated(foundChildElmNodeIdx, updatedTree)
      }

    self.withChildren(updatedChildren)
  }

  /** Returns `updated(path) { e => nodes }` */
  final def updated(path: ElemPath, nodes: immutable.IndexedSeq[N]): E = updated(path) { e => nodes }

  /**
   * Functionally updates topmost descendant elements (but not self!) for which the partial function is defined,
   * within the tree of which this element is the root element.
   */
  final def updated(pf: PartialFunction[E, immutable.IndexedSeq[N]]): E = {
    val p = { e: E => pf.isDefinedAt(e) }
    val paths = findTopmostElemPaths(p).reverse

    val result: E = paths.foldLeft(self) {
      case (acc, path) =>
        val e = acc.findWithElemPath(path).getOrElse(sys.error("Path %s not existing in root %s".format(path, acc)))
        assert(pf.isDefinedAt(e))

        acc.updated(path, pf(e))
    }
    result
  }
}
