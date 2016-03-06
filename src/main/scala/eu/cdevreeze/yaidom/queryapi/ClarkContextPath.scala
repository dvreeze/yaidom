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
import scala.annotation.tailrec
import scala.collection.immutable
import scala.collection.mutable
import eu.cdevreeze.yaidom.core.EName

/**
 * Context path, representing the ancestry(-or-self) path of an element. Although the context path of an element
 * contains the ancestry-or-self, it does not contain any siblings of the ancestry.
 * 
 * The entries know about ENames and ENames of attributes, and not about QNames.
 *
 * @author Chris de Vreeze
 */
final case class ClarkContextPath(val entries: immutable.IndexedSeq[ClarkContextPath.Entry]) extends ContextPath with Immutable { self =>
  require(entries ne null)

  type EntryType = ClarkContextPath.Entry

  type SelfType = ClarkContextPath

  def isEmpty: Boolean = entries.isEmpty

  def append(entry: EntryType): SelfType = ClarkContextPath(entries :+ entry)

  def append(other: SelfType): SelfType = ClarkContextPath(entries ++ other.entries)

  def ++(other: SelfType): SelfType = append(other)

  def parentContextPathOption: Option[SelfType] = {
    if (isEmpty) None
    else Some(ClarkContextPath(entries.dropRight(1)))
  }

  def parentContextPath: SelfType = {
    parentContextPathOption.getOrElse(sys.error("The empty context path has no parent context path"))
  }

  def ancestorOrSelfContextPaths: immutable.IndexedSeq[SelfType] = {
    @tailrec
    def accumulate(contextPath: ClarkContextPath, acc: mutable.ArrayBuffer[ClarkContextPath]): mutable.ArrayBuffer[ClarkContextPath] = {
      acc += contextPath
      if (contextPath.isEmpty) acc else accumulate(contextPath.parentContextPath, acc)
    }

    accumulate(self, mutable.ArrayBuffer[ClarkContextPath]()).toIndexedSeq
  }

  def ancestorContextPaths: immutable.IndexedSeq[SelfType] = {
    ancestorOrSelfContextPaths.drop(1)
  }

  def findAncestorOrSelfContextPath(p: SelfType => Boolean): Option[SelfType] = {
    ancestorOrSelfContextPaths find { contextPath => p(contextPath) }
  }

  def findAncestorContextPath(p: SelfType => Boolean): Option[SelfType] = {
    ancestorContextPaths find { contextPath => p(contextPath) }
  }

  def lastEntryOption: Option[EntryType] = entries.lastOption

  def lastEntry: EntryType = {
    lastEntryOption.getOrElse(sys.error("The empty context path has no last entry"))
  }
}

object ClarkContextPath {

  val Empty = apply(Vector())

  /** An entry in a `ClarkContextPath`, containing the (resolved) element name and (resolved) attributes. */
  final case class Entry(val resolvedName: EName, val resolvedAttributes: Map[EName, String]) extends ContextPath.Entry with Immutable {
    require(resolvedName ne null)
    require(resolvedAttributes ne null)
  }

  def apply(rootEntry: ClarkContextPath.Entry): ClarkContextPath = ClarkContextPath(Vector(rootEntry))
}
