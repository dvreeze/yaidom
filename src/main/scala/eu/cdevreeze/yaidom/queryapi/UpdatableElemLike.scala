/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.queryapi

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path

/**
 * API and implementation trait for functionally updatable elements. This trait extends trait [[eu.cdevreeze.yaidom.queryapi.IsNavigable]],
 * adding knowledge about child nodes in general, and about the correspondence between child path entries and child
 * indexes.
 *
 * More precisely, this trait adds the following abstract methods to the abstract methods required by its super-trait:
 * `children`, `withChildren` and `childNodeIndexesByPathEntries`. Based on these abstract methods (and the super-trait), this
 * trait offers a rich API for functionally updating elements.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.UpdatableElemApi]]. See the documentation of that trait
 * for examples of usage, and for a more formal treatment.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemLike[N, E <: N with UpdatableElemLike[N, E]] extends IsNavigable[E] with UpdatableElemApi[N, E] { self: E =>

  // See https://pario.zendesk.com/entries/20124208-lesson-6-complex-xslt-example for an example of what we must be able to express easily.

  def children: immutable.IndexedSeq[N]

  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  def childNodeIndex(childPathEntry: Path.Entry): Int

  final def withUpdatedChildren(index: Int, newChild: N): E =
    withChildren(children.updated(index, newChild))

  final def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E =
    withChildren(children.patch(from, newChildren, replace))

  final def plusChild(index: Int, child: N): E = withPatchedChildren(index, Vector(child, children(index)), 1)

  final def plusChild(child: N): E = withChildren(children :+ child)

  final def plusChildOption(index: Int, childOption: Option[N]): E = {
    if (childOption.isEmpty) self else plusChild(index, childOption.get)
  }

  final def plusChildOption(childOption: Option[N]): E = {
    if (childOption.isEmpty) self else plusChild(childOption.get)
  }

  final def minusChild(index: Int): E = withPatchedChildren(index, Vector(), 1)

  final def updated(pathEntry: Path.Entry)(f: E => E): E = {
    val idx = self.childNodeIndex(pathEntry)
    require(idx >= 0, "Expected non-negative child node index")

    self.withUpdatedChildren(idx, f(children(idx).asInstanceOf[E]))
  }

  final def updatedAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => E): E = {
    if (pathEntries.isEmpty) self
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        pathEntries.toSeq.map(entry => (entry -> childNodeIndex(entry))).sortBy(_._2)

      require(indexesByPathEntries.size == pathEntries.size, "Expected only non-negative child node indexes")

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildren = indexesByPathEntries.reverse.foldLeft(self.children) {
        case (acc, (pathEntry, idx)) =>
          val che = acc(idx).asInstanceOf[E]
          assert(findChildElemByPathEntry(pathEntry) == Some(che))
          val updatedChe = f(che, pathEntry)
          acc.updated(idx, updatedChe)
      }
      self.withChildren(newChildren)
    }
  }

  final def updated(path: Path)(f: E => E): E = {
    if (path == Path.Root) f(self)
    else {
      // Recursive, but not tail-recursive

      updated(path.firstEntry) { e => e.updated(path.withoutFirstEntry)(f) }
    }
  }

  final def updated(path: Path, newElem: E): E = updated(path) { e => newElem }

  final def updatedAtPaths(paths: Set[Path])(f: (E, Path) => E): E = {
    if (paths.isEmpty) self
    else {
      val pathsByPathEntries: Map[Path.Entry, Set[Path]] =
        paths.filter(path => !path.isRoot).groupBy(path => path.firstEntry)

      val resultsByPathEntries: Map[Path.Entry, E] =
        pathsByPathEntries map {
          case (pathEntry, paths) =>
            val che = self.findChildElemByPathEntry(pathEntry).getOrElse(sys.error(s"Incorrect path entry $pathEntry"))

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

      if (paths.contains(Path.Root)) f(resultWithoutSelf, Path.Root) else resultWithoutSelf
    }
  }

  final def updatedWithNodeSeq(path: Path)(f: E => immutable.IndexedSeq[N]): E = {
    if (path == Path.Root) self
    else {
      assert(path.parentPathOption.isDefined)
      val parentPath = path.parentPath
      val parentElem = findElemOrSelfByPath(parentPath).getOrElse(sys.error(s"Incorrect parent path $parentPath"))

      val lastEntry = path.lastEntry
      val childNodeIndex = parentElem.childNodeIndex(lastEntry)
      require(childNodeIndex >= 0, s"Incorrect path entry $lastEntry")

      val childElemOption = parentElem.findChildElemByPathEntry(lastEntry)
      assert(childElemOption.isDefined)
      val childElem = childElemOption.get

      updated(parentPath, parentElem.withPatchedChildren(childNodeIndex, f(childElem), 1))
    }
  }

  final def updatedWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[N]): E =
    updatedWithNodeSeq(path) { e => newNodes }

  final def updatedWithNodeSeqAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => immutable.IndexedSeq[N]): E = {
    if (pathEntries.isEmpty) self
    else {
      val indexesByPathEntries: Seq[(Path.Entry, Int)] =
        pathEntries.toSeq.map(entry => (entry -> childNodeIndex(entry))).sortBy(_._2)

      require(indexesByPathEntries.size == pathEntries.size, "Expected only non-negative child node indexes")

      // Updating in reverse order of indexes, in order not to invalidate the path entries
      val newChildGroups: immutable.IndexedSeq[immutable.IndexedSeq[N]] =
        indexesByPathEntries.reverse.foldLeft(self.children.map(n => immutable.IndexedSeq(n))) {
          case (acc, (pathEntry, idx)) =>
            val nodesAtIdx = acc(idx)
            assert(nodesAtIdx.size == 1)
            val che = nodesAtIdx.head.asInstanceOf[E]
            assert(findChildElemByPathEntry(pathEntry) == Some(che))
            val newNodes = f(che, pathEntry)
            acc.updated(idx, newNodes)
        }
      self.withChildren(newChildGroups.flatten)
    }
  }

  final def updatedWithNodeSeqAtPaths(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E = {
    if (paths.isEmpty) self
    else {
      val pathsByParentPaths: Map[Path, Set[Path]] =
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
