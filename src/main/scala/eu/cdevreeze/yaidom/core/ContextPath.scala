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

import scala.Vector
import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable

/**
 * Context path, representing the ancestry(-or-self) path of an element. Although the context path of an element
 * contains the ancestry-or-self, it does not contain any siblings of the ancestry.
 *
 * Each context path has at least one entry, namely a root entry.
 *
 * @author Chris de Vreeze
 */
final case class ContextPath(val rootEntry: ContextPath.Entry, val nonRootEntries: immutable.IndexedSeq[ContextPath.Entry]) extends Immutable { self =>
  require(rootEntry ne null)
  require(nonRootEntries ne null)

  def entries: immutable.IndexedSeq[ContextPath.Entry] = rootEntry +: nonRootEntries

  /** Returns true if this context path has only 1 entry */
  def isRootContextPath: Boolean = nonRootEntries.isEmpty

  /** Appends a given `Entry` to this `ContextPath` */
  def append(entry: ContextPath.Entry): ContextPath = ContextPath(rootEntry, self.nonRootEntries :+ entry)

  /** Appends a given relative `ContextPath` to this `ContextPath` */
  def append(other: ContextPath): ContextPath = {
    ContextPath(self.rootEntry, self.nonRootEntries ++ other.entries)
  }

  /** Appends a given relative `ContextPath` to this `ContextPath`. Alias for `append(other)`. */
  def ++(other: ContextPath): ContextPath = append(other)

  /**
   * Gets the parent context path (if any, because a single entry context path has no parent) wrapped in an `Option`.
   */
  def parentContextPathOption: Option[ContextPath] = {
    if (isRootContextPath) None
    else Some(ContextPath(rootEntry, nonRootEntries.dropRight(1)))
  }

  /** Like `parentContextPathOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parentContextPath: ContextPath =
    parentContextPathOption.getOrElse(sys.error("A single entry context path has no parent context path"))

  /** Returns the ancestor-or-self context paths, starting with this one, then the parent (if any), and ending with the root context path */
  def ancestorOrSelfContextPaths: immutable.IndexedSeq[ContextPath] = {
    @tailrec
    def accumulate(contextPath: ContextPath, acc: mutable.ArrayBuffer[ContextPath]): mutable.ArrayBuffer[ContextPath] = {
      acc += contextPath
      if (contextPath.isRootContextPath) acc else accumulate(contextPath.parentContextPath, acc)
    }

    accumulate(self, mutable.ArrayBuffer[ContextPath]()).toIndexedSeq
  }

  /** Returns the ancestor context paths, starting with the parent context path (if any), and ending with a root context path */
  def ancestorContextPaths: immutable.IndexedSeq[ContextPath] = ancestorOrSelfContextPaths.drop(1)

  /** Returns `ancestorOrSelfContextPaths find { contextPath => p(contextPath) }` */
  def findAncestorOrSelfContextPath(p: ContextPath => Boolean): Option[ContextPath] = {
    ancestorOrSelfContextPaths find { contextPath => p(contextPath) }
  }

  /** Returns `ancestorContextPaths find { contextPath => p(contextPath) }` */
  def findAncestorContextPath(p: ContextPath => Boolean): Option[ContextPath] = {
    ancestorContextPaths find { contextPath => p(contextPath) }
  }

  /** Returns the last entry */
  def lastEntry: ContextPath.Entry = entries.last
}

object ContextPath {

  /** An entry in a `ContextPath`, containing the (resolved) element name and (resolved) attributes. */
  final case class Entry(val resolvedName: EName, val resolvedAttributes: Map[EName, String]) extends Immutable {
    require(resolvedName ne null)
    require(resolvedAttributes ne null)
  }

  def apply(rootEntry: ContextPath.Entry): ContextPath = ContextPath(rootEntry, Vector())
}
