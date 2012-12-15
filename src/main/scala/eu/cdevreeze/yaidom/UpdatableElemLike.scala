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
 * '''Most users of the yaidom API do not use this trait directly, so may skip the documentation of this trait.'''
 *
 * This trait is a sub-trait of [[eu.cdevreeze.yaidom.PathAwareElemLike]]. It adds a type parameter for (arbitrary) nodes.
 * It also requires concrete implementations for abstract methods `children`, `withChildren`, `childNodeIndex` and `findChildPathEntry`.
 * Based on these 4 methods, and super-trait `PathAwareElemLike`, this trait offers a reasonably rich API for "functionally updating" elements.
 *
 * This trait adds the following groups of methods to the methods offered by the supertrait `PathAwareElemLike`:
 * <ul>
 * <li>Convenience methods for functional updates given a child node index (range)</li>
 * <li>Methods for functional updates given an `ElemPath`</li>
 * <li>A method for functional updates, given a partial function from elements to node collections</li>
 * </ul>
 *
 * It is important that the abstract methods are mutually consistent in their implementations. For example, the following equality
 * must hold (for some concrete class `E` that mixes in this trait):
 * {{{
 * e.allChildElems == (e.children collect { case e: E => e })
 * }}}
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends PathAwareElemLike[E] { self: E =>

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

  /** Returns a copy in which the given child has been inserted at the given position (0-based) */
  final def plusChild(index: Int, child: N): E = withPatchedChildren(index, Vector(child, children(index)), 1)

  /** Returns a copy in which the child at the given position (0-based) has been removed */
  final def minusChild(index: Int): E = withPatchedChildren(index, Vector(), 1)

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root).
   *
   * The method throws an exception if no element is found with the given path.
   */
  final def updated(path: ElemPath)(f: E => E): E = {
    // This implementation has been inspired by Scala's immutable Vector, which offers efficient
    // "functional updates" (among other efficient operations).

    if (path == ElemPath.Root) f(self)
    else {
      val foundChildElm: E = self.findWithElemPathEntry(path.firstEntry).getOrElse(sys.error("No child element found with path %s".format(path)))
      val foundChildElmNodeIdx = self.childNodeIndex(path.firstEntry)
      assert(foundChildElmNodeIdx >= 0, "Expected non-negative child node index")

      // Recursive, but not tail-recursive
      val updatedChildTree: E = foundChildElm.updated(path.withoutFirstEntry)(f)

      val updatedChildren: immutable.IndexedSeq[N] =
        self.children.updated(foundChildElmNodeIdx, updatedChildTree)

      self.withChildren(updatedChildren)
    }
  }

  /** Returns `updated(path) { e => newElem }` */
  final def updated(path: ElemPath, newElem: E): E = updated(path) { e => newElem }

  /**
   * Functionally updates topmost descendant-or-self elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   */
  final def updated(pf: PartialFunction[E, E]): E = {
    val p = { e: E => pf.isDefinedAt(e) }
    // Very important to process paths in reverse order, because ElemPaths can become invalid during (functional) updates!!
    val paths = findTopmostElemOrSelfPaths(p).reverse

    val result: E = paths.foldLeft(self) {
      case (acc, path) =>
        val e = acc.findWithElemPath(path).getOrElse(sys.error("Path %s not existing in root %s".format(path, acc)))
        assert(pf.isDefinedAt(e))

        acc.updated(path, pf(e))
    }
    result
  }
}
