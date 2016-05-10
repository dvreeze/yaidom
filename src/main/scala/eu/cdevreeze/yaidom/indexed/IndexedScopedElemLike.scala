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
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike

/**
 * Partial implementation of the abstract API for "indexed Scoped elements".
 *
 * @tparam E The element type itself
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
trait IndexedScopedElemLike[E <: IndexedScopedElemLike[E, U], U <: ScopedElemApi[U]] extends IndexedScopedElemApi[E, U] with IndexedClarkElemLike[E, U] with ScopedElemLike[E] { self: E =>

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding potential type class.
  // Yet I did not want to depend on a val or def returning the appropriate type class instance, so chose for code repetition.

  def docUriOption: Option[URI]

  def rootElem: U

  def path: Path

  def elem: U

  def baseUriOption: Option[URI]

  def findAllChildElems: immutable.IndexedSeq[E]

  final override def qname: QName = elem.qname

  final override def attributes: immutable.Iterable[(QName, String)] = elem.attributes

  final override def scope: Scope = this.elem.scope

  final def namespaces: Declarations = {
    val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
    parentScope.relativize(this.elem.scope)
  }
}

object IndexedScopedElemLike {

  /**
   * The `IndexedScopedElemLike` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E, U] extends IndexedScopedElemApi.FunctionApi[E, U] with IndexedClarkElemLike.FunctionApi[E, U] with ScopedElemLike.FunctionApi[E] {

    def underlyingElementFunctionApi: ScopedElemApi.FunctionApi[U]

    def docUriOption(thisElem: E): Option[URI]

    def rootElem(thisElem: E): U

    def path(thisElem: E): Path

    def elem(thisElem: E): U

    def baseUriOption(thisElem: E): Option[URI]

    def findAllChildElems(thisElem: E): immutable.IndexedSeq[E]

    final override def qname(thisElem: E): QName =
      underlyingElementFunctionApi.qname(elem(thisElem))

    final override def attributes(thisElem: E): immutable.Iterable[(QName, String)] =
      underlyingElementFunctionApi.attributes(elem(thisElem))

    final override def scope(thisElem: E): Scope =
      underlyingElementFunctionApi.scope(this.elem(thisElem))

    final def namespaces(thisElem: E): Declarations = {
      val parentScope =
        this.path(thisElem).parentPathOption map { path =>
          underlyingElementFunctionApi.scope(
            underlyingElementFunctionApi.getElemOrSelfByPath(rootElem(thisElem), path))
        } getOrElse (Scope.Empty)
      parentScope.relativize(underlyingElementFunctionApi.scope(this.elem(thisElem)))
    }
  }
}
