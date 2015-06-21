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

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.queryapi.NavigableClarkElemApi

/**
 * Partial implementation of the abstract API for "indexed elements".
 *
 * @tparam E The element type itself
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
trait IndexedElemLike[E <: IndexedElemLike[E, U], U <: NavigableClarkElemApi[U]] extends IndexedElemApi[E, U] with ClarkElemLike[E] with IsNavigable[E] { self: E =>

  def rootElem: U

  def path: Path

  def elem: U

  def findAllChildElems: immutable.IndexedSeq[E]

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    elem.resolvedAttributes

  final def text: String = elem.text

  final def findChildElemByPathEntry(entry: Path.Entry): Option[E] = {
    findAllChildElems.find(_.path.lastEntry == entry)
  }
}
