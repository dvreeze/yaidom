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
 * Updatable element. It defines a contract for "functional updates".
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends ElemLike[E] { self: E =>

  // See https://pario.zendesk.com/entries/20124208-lesson-6-complex-xslt-example for an example of what we must be able to express easily.

  def children: immutable.IndexedSeq[N]

  def ownChildIndex(parent: E): Int

  def withChildren(newChildren: immutable.IndexedSeq[N]): E

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
    val foundChildElmNodeIdx = foundChildElm.ownChildIndex(self)
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
}
