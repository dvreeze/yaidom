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
 * Updatable ElemLike. It augments an `ElemLike` with "functional update" support.
 *
 * The only abstract methods (ignoring those required by the `ElemLike` trait) are `children` and `withChildren`.
 * Based on these methods alone, this trait offers a rich API for "functionally updating" elements.
 *
 * Typical element classes extend this trait.
 *
 * See method `children` for requirements on node type N and element type E that are not checked by the compiler
 * (because we do not tell the compiler), but must hold in order for this trait to work.
 *
 * @tparam N The node type at the top of the node type hierarchy
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends ElemLike[E] { self: E =>

  /**
   * Returns all child nodes (elements and other nodes) in the correct order.
   *
   * Each child node is either an element of type `ElemLike[_]`, or not. The following must hold:
   * if a child node is an element of type `ElemLike[_]`, it is also of self type `E` (and `ElemLike[E]`), and we can safely
   * cast the node to that self type.
   */
  def children: immutable.IndexedSeq[N]

  /** "Functionally updates" this element with the passed child nodes */
  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  /**
   * Returns all child elements, in the correct order (same order as method `children`, but filtering only elements).
   * See method `children` for remarks about child element types.
   *
   * Note that this method is required by the `ElemLike` trait.
   */
  final def allChildElems: immutable.IndexedSeq[E] = children collect {
    case e: ElemLike[_] =>
      // We can safely cast e to type E, which is the subtype of N for elements
      e.asInstanceOf[E]
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  final def plusChild(newChild: N): E = withChildren(self.children :+ newChild)

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed partial function to the elements
   * for which the partial function is defined. The partial function is defined for an element if that element has an [[eu.cdevreeze.yaidom.ElemPath]]
   * (w.r.t. this element as root) for which it is defined. Tree traversal is top-down.
   *
   * Only topmost elements for which the partial function is defined are "functionally updated", so their descendants, if any, are
   * determined by the result of the partial function application, not by their occurrence in the original tree.
   *
   * This is potentially an expensive method.
   */
  final def updated(pf: PartialFunction[ElemPath, E]): E = {
    def updated(currentPath: ElemPath, currentElm: E): E = {
      val childNodes = currentElm.children

      if (pf.isDefinedAt(currentPath)) {
        pf(currentPath)
      } else if (childNodes.isEmpty) {
        currentElm
      } else {
        val childElemsWithPaths: immutable.IndexedSeq[(E, ElemPath.Entry)] = currentElm.allChildElemsWithPathEntries
        var idx = 0

        // Recursive, but not tail-recursive
        val updatedChildNodes: immutable.IndexedSeq[N] = childNodes map { (n: N) =>
          n match {
            case e: ElemLike[_] =>
              val pathEntry = childElemsWithPaths(idx)._2
              assert(childElemsWithPaths(idx)._1 == e)
              idx += 1
              val newPath = currentPath.append(pathEntry)

              // We can safely cast e to type E, which is the subtype of N for elements
              updated(newPath, e.asInstanceOf[E])
            case n => n
          }
        }
        currentElm.withChildren(updatedChildNodes)
      }
    }

    updated(ElemPath.Root, self)
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

      val childNodes = children

      assert(childNodes(idx).isInstanceOf[ElemLike[_]])
      // We can safely cast children(idx) to type E, which is the subtype of N for elements
      val childElm = childNodes(idx).asInstanceOf[E]

      // Recursive, but not tail-recursive
      val updatedChildren: immutable.IndexedSeq[N] = childNodes.updated(idx, childElm.updated(path.withoutFirstEntry)(f))
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
    val childNodes = children

    var cnt = 0
    var idx = -1
    while (cnt <= pathEntry.index) {
      val newIdx = childNodes indexWhere ({
        case e: ElemLike[_] =>
          // We can safely cast e to type E, which is the subtype of N for elements
          e.asInstanceOf[E].resolvedName == pathEntry.elementName
        case _ => false
      }, idx + 1)

      idx = newIdx
      if (idx < 0) {
        assert(idx == -1)
        return idx
      }
      cnt += 1
    }
    idx
  }
}
