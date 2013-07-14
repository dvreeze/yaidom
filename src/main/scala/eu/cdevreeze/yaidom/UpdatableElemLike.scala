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
 * It also requires concrete implementations for abstract methods `children`, `withChildren` and `childNodeIndex`.
 * Based on these 4 methods, and super-trait `PathAwareElemLike`, this trait offers a reasonably rich API for "functionally updating" elements.
 *
 * This trait adds the following groups of methods to the methods offered by the supertrait `PathAwareElemLike`:
 * <ul>
 * <li>Convenience methods for functional updates given a child node index (range)</li>
 * <li>Methods for functional updates given an `ElemPath`</li>
 * </ul>
 *
 * It is important that the abstract methods are mutually consistent in their implementations. For example, the following equality
 * must hold (for some concrete class `E` that mixes in this trait):
 * {{{
 * e.findAllChildElems == (e.children collect { case e: E => e })
 * }}}
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends PathAwareElemLike[E] with UpdatableElemApi[N, E] { self: E =>

  // See https://pario.zendesk.com/entries/20124208-lesson-6-complex-xslt-example for an example of what we must be able to express easily.

  def children: immutable.IndexedSeq[N]

  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  def childNodeIndexesByPathEntries: Map[ElemPath.Entry, Int]

  final def childNodeIndex(childPathEntry: ElemPath.Entry): Int = {
    childNodeIndexesByPathEntries.getOrElse(childPathEntry, -1)
  }

  final def withUpdatedChildren(index: Int, newChild: N): E =
    withChildren(children.updated(index, newChild))

  final def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E =
    withChildren(children.patch(from, newChildren, replace))

  final def plusChild(index: Int, child: N): E = withPatchedChildren(index, Vector(child, children(index)), 1)

  final def plusChild(child: N): E = withChildren(children :+ child)

  final def minusChild(index: Int): E = withPatchedChildren(index, Vector(), 1)

  final def updated(pathEntry: ElemPath.Entry)(f: E => E): E = {
    val idx = self.childNodeIndex(pathEntry)
    require(idx >= 0, "Expected non-negative child node index")

    self.withUpdatedChildren(idx, f(children(idx).asInstanceOf[E]))
  }

  final def updatedAtPathEntries(pathEntries: Set[ElemPath.Entry])(f: (E, ElemPath.Entry) => E): E = {
    if (pathEntries.isEmpty) self
    else {
      val indexesByPathEntries: Map[ElemPath.Entry, Int] = self.childNodeIndexesByPathEntries.filterKeys(pathEntries)
      require(indexesByPathEntries.size == pathEntries.size, "Expected only non-negative child node indexes")

      val newChildren = indexesByPathEntries.foldLeft(self.children) {
        case (acc, (pathEntry, idx)) =>
          val che = acc(idx).asInstanceOf[E]
          val updatedChe = f(che, pathEntry)
          acc.updated(idx, updatedChe)
      }
      self.withChildren(newChildren)
    }
  }

  final def updated(path: ElemPath)(f: E => E): E = {
    if (path == ElemPath.Root) f(self)
    else {
      // Recursive, but not tail-recursive

      updated(path.firstEntry) { e => e.updated(path.withoutFirstEntry)(f) }
    }
  }

  final def updated(path: ElemPath, newElem: E): E = updated(path) { e => newElem }

  final def updatedAtPaths(paths: Set[ElemPath])(f: (E, ElemPath) => E): E = {
    if (paths.isEmpty) self
    else {
      val pathsByPathEntries: Map[ElemPath.Entry, Set[ElemPath]] =
        paths.filter(path => !path.isRoot).groupBy(path => path.firstEntry)

      val resultsByPathEntries: Map[ElemPath.Entry, E] =
        pathsByPathEntries map {
          case (pathEntry, paths) =>
            val che = self.findWithElemPathEntry(pathEntry).getOrElse(sys.error("Incorrect path entry %s".format(pathEntry)))

            val relativePaths = paths.map(_.withoutFirstEntry)

            // Recursive call, but not tail-recursive

            val newChe = che.updatedAtPaths(relativePaths) { (elem, relativePath) =>
              val path = relativePath.prepend(pathEntry)
              f(elem, path)
            }
            (pathEntry -> newChe)
        }

      val resultWithoutSelf = self.updatedAtPathEntries(resultsByPathEntries.keySet) { (che, pathEntry) =>
        assert(resultsByPathEntries.contains(pathEntry))
        resultsByPathEntries(pathEntry)
      }

      if (paths.contains(ElemPath.Root)) f(resultWithoutSelf, ElemPath.Root) else resultWithoutSelf
    }
  }

  final def updatedWithNodeSeq(path: ElemPath)(f: E => immutable.IndexedSeq[N]): E = {
    if (path == ElemPath.Root) self
    else {
      assert(path.parentPathOption.isDefined)
      val parentPath = path.parentPath
      val parentElem = findWithElemPath(parentPath).getOrElse(sys.error("Incorrect parent path %s".format(parentPath)))

      val lastEntry = path.lastEntry
      val childNodeIndex = parentElem.childNodeIndex(lastEntry)
      require(childNodeIndex >= 0, "Incorrect path entry %s".format(lastEntry))

      val childElemOption = parentElem.findWithElemPathEntry(lastEntry)
      assert(childElemOption.isDefined)
      val childElem = childElemOption.get

      updated(parentPath, parentElem.withPatchedChildren(childNodeIndex, f(childElem), 1))
    }
  }

  final def updatedWithNodeSeq(path: ElemPath, newNodes: immutable.IndexedSeq[N]): E =
    updatedWithNodeSeq(path) { e => newNodes }

  final def updatedWithNodeSeqAtPathEntries(pathEntries: Set[ElemPath.Entry])(f: (E, ElemPath.Entry) => immutable.IndexedSeq[N]): E = {
    if (pathEntries.isEmpty) self
    else {
      val indexesByPathEntries: Map[ElemPath.Entry, Int] = self.childNodeIndexesByPathEntries.filterKeys(pathEntries)
      require(indexesByPathEntries.size == pathEntries.size, "Expected only non-negative child node indexes")

      val newChildGroups: immutable.IndexedSeq[immutable.IndexedSeq[N]] =
        indexesByPathEntries.foldLeft(self.children.map(n => immutable.IndexedSeq(n))) {
          case (acc, (pathEntry, idx)) =>
            val nodesAtIdx = acc(idx)
            assert(nodesAtIdx.size == 1)
            val che = nodesAtIdx.head.asInstanceOf[E]
            val newNodes = f(che, pathEntry)
            acc.updated(idx, newNodes)
        }
      self.withChildren(newChildGroups.flatten)
    }
  }

  final def updatedWithNodeSeqAtPaths(paths: Set[ElemPath])(f: (E, ElemPath) => immutable.IndexedSeq[N]): E = {
    if (paths.isEmpty) self
    else {
      val pathsByParentPaths: Map[ElemPath, Set[ElemPath]] =
        paths.filter(path => !path.isRoot).groupBy(path => path.parentPath)

      self.updatedAtPaths(pathsByParentPaths.keySet) { (elem, path) =>
        val childPathsOption = pathsByParentPaths.get(path)
        assert(childPathsOption.isDefined)
        val childPaths = childPathsOption.get
        assert(childPaths.forall(p => p.parentPathOption == Some(path)))
        val childPathEntries = childPaths.map(_.lastEntry)

        elem.updatedWithNodeSeqAtPathEntries(childPathEntries) { (che, pathEntry) =>
          f(che, path.append(pathEntry))
        }
      }
    }
  }
}
