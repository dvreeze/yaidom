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

package eu.cdevreeze.yaidom.queryapi

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.AbsolutePath
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * Abstract API for "indexed elements".
 *
 * Note how this API removes the need for an API which is like the `ElemApi` API, but taking and returning pairs
 * of elements and paths.
 *
 * @author Chris de Vreeze
 */
trait IndexedClarkElemApi extends ClarkElemApi {

  type ThisElem <: IndexedClarkElemApi

  /**
   * The optional document URI of the containing document, if any
   */
  def docUriOption: Option[URI]

  /**
   * The document URI, defaulting to the empty URI if absent
   */
  def docUri: URI

  /**
   * The root element
   */
  def rootElem: ThisElem

  /**
   * The path of this element, relative to the root element
   */
  def path: Path

  /**
   * The absolute path of this element, starting with the root element
   */
  def absolutePath: AbsolutePath

  /**
   * Returns the optional base URI, computed from the document URI, if any, and the XML base attributes of the
   * ancestors, if any.
   */
  def baseUriOption: Option[URI]

  /**
   * The base URI, defaulting to the empty URI if absent
   */
  def baseUri: URI

  /**
   * Returns the optional parent element base URI, computed from the document URI, if any, and the XML base attributes of the
   * ancestors, if any.
   */
  def parentBaseUriOption: Option[URI]

  /**
   * Returns the ENames of the ancestry-or-self reversed, starting with the root element and ending with this element.
   *
   * That is, returns:
   * {{{
   * rootElem.resolvedName +: path.entries.map(_.elementName)
   * }}}
   *
   * This is equal to:
   * {{{
   * reverseAncestryOrSelf.map(_.resolvedName)
   * }}}
   */
  def reverseAncestryOrSelfENames: immutable.IndexedSeq[EName]

  /**
   * Returns the ENames of the ancestry reversed, starting with the root element and ending with the parent of this element, if any.
   *
   * That is, returns:
   * {{{
   * reverseAncestryOrSelfENames.dropRight(1)
   * }}}
   */
  def reverseAncestryENames: immutable.IndexedSeq[EName]

  /**
   * Returns the reversed ancestor-or-self elements. That is, returns:
   * {{{
   * rootElem.findReverseAncestryOrSelfByPath(path).get
   * }}}
   */
  def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem]

  /**
   * Returns the reversed ancestor elements. That is, returns:
   * {{{
   * reverseAncestryOrSelf.init
   * }}}
   */
  def reverseAncestry: immutable.IndexedSeq[ThisElem]
}

object IndexedClarkElemApi {

  /**
   * This query API type, restricting ThisElem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = IndexedClarkElemApi { type ThisElem = E }
}
