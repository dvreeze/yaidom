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

package eu.cdevreeze.yaidom.core

import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable

/**
 * The ancestry path of an element in an element tree. The first entry represents the root element, and the last one the "current" element.
 * Each entry is like an element without its children, but with the element QName, Scope and attributes (mapping QNames to attribute values).
 *
 * An `AncestryPath` is quite different from a `Path`. The former represents the ancestry-or-self of an element in an element tree, while
 * the latter represents a series of steps to navigate through an element tree (to descendants-or-self). Ancestry paths have no knowledge of the
 * siblings of "this" element or of its ancestors, while (navigation) paths can be used to navigate to any descendant-or-self element.
 * Hence an ancestry path is a property of an element in a tree, whereas a (navigation) path is a navigation notion (through an element tree).
 * The latter should really be called NavigationPath. Both concepts have in common that they only know about element nodes.
 *
 * @author Chris de Vreeze
 */
final case class AncestryPath(val entries: immutable.IndexedSeq[AncestryPath.Entry]) extends Immutable { self =>
  require(entries ne null)
  require(entries.size >= 1, s"Ancestry paths must have at least one entry")

  /**
   * Returns true if this ancestry path has only one entry.
   */
  def isRoot: Boolean = (entries.size == 1)

  /**
   * The ancestry path entry pointing to the "root element", which is the first entry.
   */
  def rootEntry: AncestryPath.Entry = entries.head

  /**
   * The ancestry path entry pointing to "this element", which is the last entry.
   */
  def lastEntry: AncestryPath.Entry = entries.last

  /**
   * Returns `AncestryPath(rootEntry)`.
   */
  def root: AncestryPath = AncestryPath.fromEntry(rootEntry)

  /**
   * Returns `AncestryPath(lastEntry)`.
   */
  def asRoot: AncestryPath = AncestryPath.fromEntry(lastEntry)

  /**
   * Gets the parent ancestry path (if any, because a root ancestry path has no parent) wrapped in an `Option`.
   */
  def parentOption: Option[AncestryPath] = {
    entries match {
      case xs if xs.size == 1 => None
      case _                  => Some(AncestryPath(entries.dropRight(1)))
    }
  }

  /** Like `parentOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parent: AncestryPath = parentOption.getOrElse(sys.error("A singleton ancestry path has no parent"))

  /** Returns the ancestors-or-self, starting with this ancestry path, then the parent (if any), and ending with a root path */
  def ancestorsOrSelf: immutable.IndexedSeq[AncestryPath] = {
    @tailrec
    def accumulate(path: AncestryPath, acc: mutable.ArrayBuffer[AncestryPath]): mutable.ArrayBuffer[AncestryPath] = {
      acc += path
      if (path.isRoot) acc else accumulate(path.parent, acc)
    }

    accumulate(self, mutable.ArrayBuffer[AncestryPath]()).toIndexedSeq
  }

  /** Returns the ancestors, starting with the parent (if any), and ending with the root ancestry path */
  def ancestors: immutable.IndexedSeq[AncestryPath] = ancestorsOrSelf.drop(1)

  /** Returns `ancestorsOrSelf find { path => p(path) }` */
  def findAncestorsOrSelf(p: AncestryPath => Boolean): Option[AncestryPath] = {
    ancestorsOrSelf find { path => p(path) }
  }

  /** Returns `ancestors find { path => p(path) }` */
  def findAncestor(p: AncestryPath => Boolean): Option[AncestryPath] = {
    ancestors find { path => p(path) }
  }

  /**
   * Appends a new last entry to this ancestry path.
   */
  def append(entry: AncestryPath.Entry): AncestryPath = {
    AncestryPath(self.entries :+ entry)
  }

  /**
   * Returns the QNames, from root entry to last entry.
   */
  def qnames: immutable.IndexedSeq[QName] = {
    entries.map(_.qname)
  }

  /**
   * Returns the ENames, from root entry to last entry.
   */
  def enames: immutable.IndexedSeq[EName] = {
    entries.map(_.ename)
  }
}

object AncestryPath {

  def fromEntry(entry: AncestryPath.Entry): AncestryPath = {
    AncestryPath(immutable.IndexedSeq(entry))
  }

  /** An entry in an `AncestryPath`, as an element without children. */
  final case class Entry(val qname: QName, val attributes: immutable.IndexedSeq[(QName, String)], val scope: Scope) extends Immutable {
    require(qname ne null)
    require(attributes ne null)
    require(scope ne null)

    def ename: EName = {
      scope.resolveQNameOption(qname).getOrElse(sys.error(s"Corrupt element. QName $qname could not be resolved in scope $scope"))
    }
  }
}
