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

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi

/**
 * Abstract API for "indexed elements".
 *
 * Note how this API removes the need for an API which is like the `ElemApi` API, but taking and returning pairs
 * of elements and paths. This could be seen as that API, re-using `ElemApi` instead of adding an extra API similar to it.
 * These `IndexedClarkElemApi` objects "are" the above-mentioned pairs of elements and paths.
 *
 * @tparam E The element type itself
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
trait IndexedClarkElemApi[E <: IndexedClarkElemApi[E, U], U <: ClarkElemApi[U]] extends ClarkElemApi[E] { self: E =>

  /**
   * The optional document URI of the containing document, if any
   */
  def docUriOption: Option[URI]

  /**
   * The root element of the underlying element type
   */
  def rootElem: U

  /**
   * The path of this element, relative to the root element
   */
  def path: Path

  /**
   * The underlying element, of the underlying element type. It must be equal to:
   * {{{
   * rootElem.getElemOrSelfByPath(path)
   * }}}
   */
  def elem: U

  /**
   * Returns the ENames of the ancestry-or-self, starting with the root element and ending with this element.
   *
   * That is, returns:
   * {{{
   * rootElem.resolvedName +: path.entries.map(_.elementName)
   * }}}
   */
  def ancestryOrSelfENames: immutable.IndexedSeq[EName]

  /**
   * Returns the ENames of the ancestry, starting with the root element and ending with the parent of this element, if any.
   *
   * That is, returns:
   * {{{
   * ancestryOrSelfENames.dropRight(1)
   * }}}
   */
  def ancestryENames: immutable.IndexedSeq[EName]
}
