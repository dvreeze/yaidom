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

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName

/**
 * Context path, representing the ancestry(-or-self) path of an element. Although the context path of an element
 * contains the ancestry-or-self, it does not contain any siblings of the ancestry.
 *
 * WARNING: CONTEXT PATHS ARE CONSIDERED EXPERIMENTAL!
 *
 * @author Chris de Vreeze
 */
trait ContextPath {

  type EntryType <: ContextPath.Entry

  type SelfType <: ContextPath

  /** Returns all entries in the context path */
  def entries: immutable.IndexedSeq[EntryType]

  /** Returns true if this context path is empty */
  def isEmpty: Boolean

  /** Appends a given `Entry` to this `ContextPath` */
  def append(entry: EntryType): SelfType

  /** Appends a given relative `ContextPath` to this `ContextPath` */
  def append(other: SelfType): SelfType

  /** Appends a given relative `ContextPath` to this `ContextPath`. Alias for `append(other)`. */
  def ++(other: SelfType): SelfType

  /**
   * Gets the parent context path (if any, because the empty context path has no parent) wrapped in an `Option`.
   */
  def parentContextPathOption: Option[SelfType]

  /** Like `parentContextPathOption`, but unwrapping the result (or throwing an exception otherwise) */
  def parentContextPath: SelfType

  /** Returns the ancestor-or-self context paths, starting with this one, then the parent (if any), and ending with the empty context path */
  def ancestorOrSelfContextPaths: immutable.IndexedSeq[SelfType]

  /** Returns the ancestor context paths, starting with the parent context path (if any), and ending with the empty context path */
  def ancestorContextPaths: immutable.IndexedSeq[SelfType]

  /** Returns `ancestorOrSelfContextPaths find { contextPath => p(contextPath) }` */
  def findAncestorOrSelfContextPath(p: SelfType => Boolean): Option[SelfType]

  /** Returns `ancestorContextPaths find { contextPath => p(contextPath) }` */
  def findAncestorContextPath(p: SelfType => Boolean): Option[SelfType]

  /** Returns the last entry, if any, wrapped in an Option */
  def lastEntryOption: Option[EntryType]

  /** Like `lastEntryOption`, but unwrapping the result (or throwing an exception otherwise) */
  def lastEntry: EntryType
}

object ContextPath {

  /** An entry in a `ContextPath`, containing at least the (resolved) element name and (resolved) attributes. */
  trait Entry extends HasEName {

    def resolvedName: EName

    def resolvedAttributes: immutable.Iterable[(EName, String)]
  }
}
