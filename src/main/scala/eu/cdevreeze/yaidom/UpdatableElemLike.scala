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
 * API and implementation trait for functionally updatable elements. This trait extends trait [[eu.cdevreeze.yaidom.PathAwareElemLike]],
 * adding knowledge about child nodes in general, and about the correspondence between child element path entries and child
 * indexes.
 *
 * More precisely, this trait adds the following abstract methods to the abstract methods required by its super-trait:
 * `children`, `withChildren` and `childNodeIndexesByPathEntries`. Based on these abstract methods (and the super-trait), this
 * trait offers a rich API for querying elements and element paths, and for functionally updating elements.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.UpdatableElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment.
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
      val indexesByPathEntries: Seq[(ElemPath.Entry, Int)] =
        self.childNodeIndexesByPathEntries.filterKeys(pathEntries).toSeq.sortBy(_._2)
      require(indexesByPathEntries.size == pathEntries.size, "Expected only non-negative child node indexes")

      // Updating in reverse order of indexes, in order not to invalidate the element path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(self.children) {
        case (acc, (pathEntry, idx)) =>
          val che = acc(idx).asInstanceOf[E]
          assert(findWithElemPathEntry(pathEntry) == Some(che))
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
      val indexesByPathEntries: Seq[(ElemPath.Entry, Int)] =
        self.childNodeIndexesByPathEntries.filterKeys(pathEntries).toSeq.sortBy(_._2)
      require(indexesByPathEntries.size == pathEntries.size, "Expected only non-negative child node indexes")

      // Updating in reverse order of indexes, in order not to invalidate the element path entries
      val newChildGroups: immutable.IndexedSeq[immutable.IndexedSeq[N]] =
        indexesByPathEntries.reverse.foldLeft(self.children.map(n => immutable.IndexedSeq(n))) {
          case (acc, (pathEntry, idx)) =>
            val nodesAtIdx = acc(idx)
            assert(nodesAtIdx.size == 1)
            val che = nodesAtIdx.head.asInstanceOf[E]
            assert(findWithElemPathEntry(pathEntry) == Some(che))
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
