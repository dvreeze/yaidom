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
   * Returns the optional base URI, given an optional document URI, the root element, and the Path from the root
   * element to "this" element.
   */
  def computeBaseUriOption[U <: ClarkElemApi[U]](docUriOption: Option[URI], rootElem: U, path: Path): Option[URI] = {
    val reverseAncestryOrSelf: immutable.IndexedSeq[U] =
      rootElem.findReverseAncestryOrSelfByPath(path).getOrElse(
        sys.error(s"Corrupt data. Could not get reverse ancestry-or-self of ${rootElem.getElemOrSelfByPath(path)}"))

    assert(reverseAncestryOrSelf.head == rootElem)

    reverseAncestryOrSelf.foldLeft(docUriOption) {
      case (parentBaseUriOption, elm) =>
        getNextBaseUriOption(parentBaseUriOption, elm)
    }
  }

  /**
   * Returns the optional base URI, given an optional parent base URI, and "this" element.
   */
  def getNextBaseUriOption[U <: ClarkElemApi[U]](parentBaseUriOption: Option[URI], elem: U): Option[URI] = {
    val explicitBaseUriOption = elem.attributeOption(IndexedClarkElemLike.XmlBaseEName).map(s => new URI(s))
    explicitBaseUriOption.map(u => parentBaseUriOption.map(_.resolve(u)).getOrElse(u)).orElse(parentBaseUriOption)
  }

  val XmlNs = "http://www.w3.org/XML/1998/namespace"

  val XmlBaseEName = EName(XmlNs, "base")
}
