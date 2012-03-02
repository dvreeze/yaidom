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
 * An [[eu.cdevreeze.yaidom.ElemAwareElemLike]] that is aware of its child nodes (which need not be elements).
 *
 * The only abstract methods (added to those in `ElemAwareElemLike`) are `children` and `withChildren`.
 * Based on these methods alone, this trait offers a rich API for "functional updates" (transformations) of elements.
 *
 * @tparam N the node type of the children of this element
 * @tparam E the captured element subtype, which is the (self) type of the element, so the type of the `NodeAwareElemLike[E]` itself
 *
 * @author Chris de Vreeze
 */
trait NodeAwareElemLike[N, E <: N with NodeAwareElemLike[N, E]] extends ElemAwareElemLike[E] { self: E =>

  /**
   * Returns all child nodes, in the correct order.
   * If a child is an element, it must be of type `E`.
   */
  def children: immutable.IndexedSeq[N]

  /** Creates a copy, but with (only) the children passed as parameter newChildren */
  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  /** Returns `withChildren(self.children :+ newChild)`. */
  final def plusChild(newChild: N): E = withChildren(self.children :+ newChild)

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed partial function to the elements
   * for which the partial function is defined. The partial function is defined for an element if that element has an [[eu.cdevreeze.yaidom.ElemPath]]
   * (w.r.t. this element as root) for which it is defined. Tree traversal is top-down.
   *
   * This is an expensive method.
   */
  final def updated(pf: PartialFunction[ElemPath, E]): E = {
    def updated(currentPath: ElemPath): E = {
      val elm: E = self.findWithElemPath(currentPath).getOrElse(sys.error("Undefined path %s for root element %s".format(currentPath, self)))

      currentPath match {
        case p if pf.isDefinedAt(p) => pf(p)
        case p =>
          val childResults: immutable.IndexedSeq[N] = elm.children map {
            case e: NodeAwareElemLike[_, _] =>
              // Safe cast if method children obeys its contract
              val ownPathEntry = e.asInstanceOf[E].ownElemPathEntry(elm)
              val ownPath = currentPath.append(ownPathEntry)

              // Recursive call, but not tail-recursive
              val updatedElm = updated(ownPath)
              updatedElm
            case n => n
          }

          elm.withChildren(childResults)
      }
    }

    updated(ElemPath.Root)
  }

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root). The method throws an exception
   * if no element is found with the given path.
   */
  final def updated(path: ElemPath)(f: E => E): E = {
    // This implementation has been inspired by Scala's immutable Vector, which offers efficient
    // "functional updates" (among other efficient operations).

    if (path.entries.isEmpty) f(self) else {
      val firstEntry = path.firstEntry
      val idx = childIndexOf(firstEntry)
      require(idx >= 0, "The path %s does not exist".format(path))
      val childElm = children(idx).asInstanceOf[E]

      // Recursive, but not tail-recursive
      val updatedChildren = children.updated(idx, childElm.updated(path.withoutFirstEntry)(f))
      self.withChildren(updatedChildren)
    }
  }

  /** Returns `updated(path) { e => elm }` */
  final def updated(path: ElemPath, elm: E): E = updated(path) { e => elm }

  /**
   * Returns the index of the child with the given `ElemPath` `Entry` (taking this element as parent), or -1 if not found.
   * Must be fast.
   */
  final def childIndexOf(pathEntry: ElemPath.Entry): Int = {
    var cnt = 0
    var idx = -1
    while (cnt <= pathEntry.index) {
      val newIdx = children indexWhere ({
        case e: NodeAwareElemLike[_, _] if e.resolvedName == pathEntry.elementName => true
        case _ => false
      }, idx + 1)
      idx = newIdx
      cnt += 1
    }
    idx
  }
}
