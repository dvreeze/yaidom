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

package eu.cdevreeze.yaidom.indexed

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.IndexedScopedElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike

/**
 * Indexed Scoped element. Like `IndexedClarkElem` but instead of being and indexing
 * a `ClarkElemApi`, it is and indexes a `ScopedElemApi`. Other than that, see the
 * documentation for `IndexedClarkElem`.
 *
 * The optional parent base URI is stored for very fast (optional) base URI computation. This is helpful in
 * an XBRL context, where URI resolution against a base URI is typically a very frequent operation.
 *
 * @author Chris de Vreeze
 */
final class IndexedScopedElem[U <: ScopedElemApi.Aux[U]] private (
  docUriOption: Option[URI],
  underlyingRootElem: U,
  path: Path,
  underlyingElem: U) extends AbstractIndexedClarkElem(docUriOption, underlyingRootElem, path, underlyingElem) with Nodes.Elem with IndexedScopedElemApi with ScopedElemLike {

  type ThisElemApi = IndexedScopedElem[U]

  type ThisElem = IndexedScopedElem[U]

  def thisElem: ThisElem = this

  final def findAllChildElems: immutable.IndexedSeq[ThisElem] = {
    underlyingElem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        new IndexedScopedElem(docUriOption, underlyingRootElem, path.append(entry), e)
    }
  }

  final def qname: QName = underlyingElem.qname

  final def attributes: immutable.IndexedSeq[(QName, String)] = underlyingElem.attributes.toIndexedSeq

  final def scope: Scope = underlyingElem.scope

  final override def equals(obj: Any): Boolean = obj match {
    case other: IndexedScopedElem[U] =>
      (other.docUriOption == this.docUriOption) && (other.underlyingRootElem == this.underlyingRootElem) &&
        (other.path == this.path) && (other.underlyingElem == this.underlyingElem)
    case _ => false
  }

  final override def hashCode: Int = (docUriOption, underlyingRootElem, path, underlyingElem).hashCode

  final def rootElem: ThisElem = {
    new IndexedScopedElem[U](docUriOption, underlyingRootElem, Path.Empty, underlyingRootElem)
  }

  final def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem] = {
    val resultOption = rootElem.findReverseAncestryOrSelfByPath(path)

    assert(resultOption.isDefined, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
    assert(!resultOption.get.isEmpty, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
    assert(resultOption.get.last == thisElem)

    resultOption.get
  }

  final def namespaces: Declarations = {
    val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
    parentScope.relativize(this.scope)
  }
}

object IndexedScopedElem {

  def apply[U <: ScopedElemApi.Aux[U]](docUriOption: Option[URI], underlyingRootElem: U, path: Path): IndexedScopedElem[U] = {
    new IndexedScopedElem[U](docUriOption, underlyingRootElem, path, underlyingRootElem.getElemOrSelfByPath(path))
  }

  def apply[U <: ScopedElemApi.Aux[U]](docUri: URI, underlyingRootElem: U, path: Path): IndexedScopedElem[U] = {
    apply(Some(docUri), underlyingRootElem, path)
  }

  def apply[U <: ScopedElemApi.Aux[U]](underlyingRootElem: U, path: Path): IndexedScopedElem[U] = {
    new IndexedScopedElem[U](None, underlyingRootElem, path, underlyingRootElem.getElemOrSelfByPath(path))
  }

  def apply[U <: ScopedElemApi.Aux[U]](docUriOption: Option[URI], underlyingRootElem: U): IndexedScopedElem[U] = {
    apply(docUriOption, underlyingRootElem, Path.Empty)
  }

  def apply[U <: ScopedElemApi.Aux[U]](docUri: URI, underlyingRootElem: U): IndexedScopedElem[U] = {
    apply(Some(docUri), underlyingRootElem, Path.Empty)
  }

  def apply[U <: ScopedElemApi.Aux[U]](underlyingRootElem: U): IndexedScopedElem[U] = {
    apply(None, underlyingRootElem, Path.Empty)
  }
}
