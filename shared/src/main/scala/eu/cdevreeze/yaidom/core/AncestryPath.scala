/*
 * Copyright 2011-2017 Chris de Vreeze
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
 * The ancestry path of an element in an element tree. The first entry represents the "current" element, and the last one the "root" element.
 * Each entry is like an element without its children, but with the element QName, Scope and attributes (mapping QNames to attribute values).
 *
 * An `AncestryPath` is quite different from a `Path`. The former represents the ancestry-or-self of an element in an element tree, while
 * the latter represents a series of steps to navigate through an element tree, to descendant-or-self elements. Ancestry paths have no knowledge
 * of the siblings of "this" element or of its ancestors, while (navigation) paths can be used to navigate to any descendant-or-self element.
 * Hence an ancestry path is a property of an element in a tree, whereas a (navigation) path is a navigation notion (through an element tree).
 * The latter should really be called NavigationPath. Both concepts have in common that they only know about element nodes.
 *
 * An AncestryPath can be thought of as a stack where the head is the "current" element and the tail is the ancestor elements.
 *
 * @author Chris de Vreeze
 */
final case class AncestryPath(val ancestorOrSelfEntries: List[AncestryPath.Entry]) extends Immutable { self =>
  require(ancestorOrSelfEntries ne null) // scalastyle:off null
  require(ancestorOrSelfEntries.size >= 1, s"Ancestry paths must have at least one entry")

  /**
   * Returns true if this ancestry path has only one entry.
   */
  def isRoot: Boolean = (ancestorOrSelfEntries.size == 1)

  /**
   * The ancestry path entry pointing to the "root element", which is the last entry.
   */
  def rootEntry: AncestryPath.Entry = ancestorOrSelfEntries.last

  /**
   * The ancestry path entry pointing to "this element", which is the first entry.
   */
  def firstEntry: AncestryPath.Entry = ancestorOrSelfEntries.head

  /**
   * Returns `AncestryPath.fromEntry(rootEntry)`.
   */
  def root: AncestryPath = AncestryPath.fromEntry(rootEntry)

  /**
   * Returns `AncestryPath.fromEntry(firstEntry)`.
   */
  def asRoot: AncestryPath = AncestryPath.fromEntry(firstEntry)

  /**
   * Gets the parent ancestry path (if any, because a root ancestry path has no parent) wrapped in an `Option`.
   */
  def parentOption: Option[AncestryPath] = {
    ancestorOrSelfEntries match {
      case Nil       => sys.error(s"Corrupt data. An AncestryPath can not be empty.")
      case hd :: Nil => None
      case hd :: tl  => Some(AncestryPath(tl))
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
   * Prepends a new first entry to this ancestry path.
   */
  def prepend(entry: AncestryPath.Entry): AncestryPath = {
    AncestryPath(entry :: self.ancestorOrSelfEntries)
  }

  /**
   * Returns the QNames, from the last entry to the root entry.
   */
  def qnames: List[QName] = {
    ancestorOrSelfEntries.map(_.qname)
  }

  /**
   * Returns the ENames, from the last entry to the root entry.
   */
  def enames: List[EName] = {
    ancestorOrSelfEntries.map(_.ename)
  }
}

object AncestryPath {

  def fromEntry(entry: AncestryPath.Entry): AncestryPath = {
    AncestryPath(List(entry))
  }

  /** An entry in an `AncestryPath`, as an element without children. */
  final case class Entry(val qname: QName, val attributes: immutable.IndexedSeq[(QName, String)], val scope: Scope) extends Immutable {
    require(qname ne null) // scalastyle:off null
    require(attributes ne null) // scalastyle:off null
    require(scope ne null) // scalastyle:off null

    def ename: EName = {
      scope.resolveQNameOption(qname).getOrElse(sys.error(s"Corrupt element. QName $qname could not be resolved in scope $scope"))
    }
  }
}
