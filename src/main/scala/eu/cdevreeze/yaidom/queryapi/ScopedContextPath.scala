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
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope

/**
 * Context path, representing the ancestry(-or-self) path of an element. Although the context path of an element
 * contains the ancestry-or-self, it does not contain any siblings of the ancestry.
 *
 * WARNING: CONTEXT PATHS ARE CONSIDERED EXPERIMENTAL!
 *
 * The entries know about ENames and ENames of attributes, but also about QNames and QNames of attributes.
 *
 * @author Chris de Vreeze
 */
final case class ScopedContextPath(val entries: immutable.IndexedSeq[ScopedContextPath.Entry]) extends ContextPath with Immutable { self =>
  require(entries ne null)

  type EntryType = ScopedContextPath.Entry

  type SelfType = ScopedContextPath

  def isEmpty: Boolean = entries.isEmpty

  def append(entry: EntryType): SelfType = ScopedContextPath(entries :+ entry)

  def append(other: SelfType): SelfType = ScopedContextPath(entries ++ other.entries)

  def ++(other: SelfType): SelfType = append(other)

  def parentContextPathOption: Option[SelfType] = {
    if (isEmpty) None
    else Some(ScopedContextPath(entries.dropRight(1)))
  }

  def parentContextPath: SelfType = {
    parentContextPathOption.getOrElse(sys.error("The empty context path has no parent context path"))
  }

  def ancestorOrSelfContextPaths: immutable.IndexedSeq[SelfType] = {
    @tailrec
    def accumulate(contextPath: ScopedContextPath, acc: mutable.ArrayBuffer[ScopedContextPath]): mutable.ArrayBuffer[ScopedContextPath] = {
      acc += contextPath
      if (contextPath.isEmpty) acc else accumulate(contextPath.parentContextPath, acc)
    }

    accumulate(self, mutable.ArrayBuffer[ScopedContextPath]()).toIndexedSeq
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

object ScopedContextPath {

  val Empty = apply(Vector())

  /**
   * An entry in a `ScopedContextPath`, containing the (resolved) element name and (resolved) attributes, as well
   * as the qualified name, attributes with QName keys, and scope.
   */
  final case class Entry(val qname: QName, val attributes: immutable.IndexedSeq[(QName, String)], val scope: Scope) extends ContextPath.Entry with HasQNameApi with HasScopeApi with Immutable {
    require(qname ne null)
    require(attributes ne null)
    require(scope ne null)

    /** The `Elem` name as `EName`, obtained by resolving the element `QName` against the `Scope` */
    val resolvedName: EName =
      scope.resolveQNameOption(qname).getOrElse(sys.error(s"Element name '${qname}' should resolve to an EName in scope [${scope}]"))

    /** The attributes as an ordered mapping from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against the attribute scope */
    val resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
      val attrScope = attributeScope

      attributes map { kv =>
        val attName = kv._1
        val attValue = kv._2
        val expandedName =
          attrScope.resolveQNameOption(attName).getOrElse(sys.error(s"Attribute name '${attName}' should resolve to an EName in scope [${attrScope}]"))
        (expandedName -> attValue)
      }
    }

    /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
    def attributeScope: Scope = scope.withoutDefaultNamespace
  }

  def apply(rootEntry: ScopedContextPath.Entry): ScopedContextPath = ScopedContextPath(Vector(rootEntry))
}
