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
 * @author Chris de Vreeze
 */
trait IndexedClarkElemLike extends IndexedClarkElemApi with ClarkElemLike {

  type ThisElemApi <: IndexedClarkElemLike

  type UnderlyingElemApi <: ClarkElemApi.Aux[UnderlyingElem]

  def docUriOption: Option[URI]

  def rootElem: UnderlyingElem

  def path: Path

  def elem: UnderlyingElem

  def baseUriOption: Option[URI]

  def findAllChildElems: immutable.IndexedSeq[ThisElem]

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

  final def reverseAncestryOrSelf: immutable.IndexedSeq[UnderlyingElem] = {
    val resultOption = rootElem.findReverseAncestryOrSelfByPath(path)

    assert(resultOption.isDefined, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
    assert(!resultOption.get.isEmpty, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
    assert(resultOption.get.last == thisElem.elem)

    resultOption.get
  }
}

object IndexedClarkElemLike {

  type Aux[A, B] = IndexedClarkElemLike {
    type ThisElem = A
    type UnderlyingElem = B
  }
}
