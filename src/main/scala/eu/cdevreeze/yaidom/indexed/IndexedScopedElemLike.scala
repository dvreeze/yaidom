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

  def rootElem: U

  def path: Path

  def elem: U

  def findAllChildElems: immutable.IndexedSeq[E]

  final override def qname: QName = elem.qname

  final override def attributes: immutable.Iterable[(QName, String)] = elem.attributes

  final override def scope: Scope = this.elem.scope

  final def namespaces: Declarations = {
    val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
    parentScope.relativize(this.elem.scope)
  }
}
