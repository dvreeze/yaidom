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
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike

/**
 * Partial implementation of the abstract API for "indexed elements".
 *
 * @tparam E The element type itself
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
trait IndexedClarkElemLike[E <: IndexedClarkElemLike[E, U], U <: ClarkElemApi[U]] extends IndexedClarkElemApi[E, U] with ClarkElemLike[E] { self: E =>

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding potential type class.
  // Yet I did not want to depend on a val or def returning the appropriate type class instance, so chose for code repetition.

  def docUriOption: Option[URI]

  def rootElem: U

  def path: Path

  def elem: U

  def baseUriOption: Option[URI]

  def findAllChildElems: immutable.IndexedSeq[E]

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    elem.resolvedAttributes

  final def text: String = elem.text

  final def reverseAncestryOrSelfENames: immutable.IndexedSeq[EName] = {
    rootElem.resolvedName +: path.entries.map(_.elementName)
  }

  final def reverseAncestryENames: immutable.IndexedSeq[EName] = {
    reverseAncestryOrSelfENames.dropRight(1)
  }

  final def reverseAncestryOrSelf: immutable.IndexedSeq[U] = {
    val resultOption = rootElem.findReverseAncestryOrSelfByPath(path)

    assert(resultOption.isDefined, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
    assert(!resultOption.get.isEmpty, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
    assert(resultOption.get.last == self.elem)

    resultOption.get
  }
}

object IndexedClarkElemLike {

  /**
   * The `IndexedClarkElemLike` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E, U] extends IndexedClarkElemApi.FunctionApi[E, U] with ClarkElemLike.FunctionApi[E] {

    def underlyingElementFunctionApi: ClarkElemApi.FunctionApi[U]

    def docUriOption(thisElem: E): Option[URI]

    def rootElem(thisElem: E): U

    def path(thisElem: E): Path

    def elem(thisElem: E): U

    def baseUriOption(thisElem: E): Option[URI]

    def findAllChildElems(thisElem: E): immutable.IndexedSeq[E]

    final def resolvedName(thisElem: E): EName = underlyingElementFunctionApi.resolvedName(elem(thisElem))

    final def resolvedAttributes(thisElem: E): immutable.Iterable[(EName, String)] =
      underlyingElementFunctionApi.resolvedAttributes(elem(thisElem))

    final def text(thisElem: E): String = underlyingElementFunctionApi.text(elem(thisElem))

    final def reverseAncestryOrSelfENames(thisElem: E): immutable.IndexedSeq[EName] = {
      underlyingElementFunctionApi.resolvedName(rootElem(thisElem)) +: path(thisElem).entries.map(_.elementName)
    }

    final def reverseAncestryENames(thisElem: E): immutable.IndexedSeq[EName] = {
      reverseAncestryOrSelfENames(thisElem).dropRight(1)
    }

    final def reverseAncestryOrSelf(thisElem: E): immutable.IndexedSeq[U] = {
      val resultOption = underlyingElementFunctionApi.findReverseAncestryOrSelfByPath(rootElem(thisElem), path(thisElem))

      assert(resultOption.isDefined, s"Corrupt data! The reverse ancestry-or-self (of ${resolvedName(thisElem)} cannot be empty")
      assert(!resultOption.get.isEmpty, s"Corrupt data! The reverse ancestry-or-self (of ${resolvedName(thisElem)}) cannot be empty")
      assert(resultOption.get.last == elem(thisElem))

      resultOption.get
    }
  }
}
