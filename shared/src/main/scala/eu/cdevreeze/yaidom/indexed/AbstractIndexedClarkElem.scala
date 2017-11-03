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

package eu.cdevreeze.yaidom.indexed

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.IndexedClarkElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * Common super-class for IndexedClarkElem and IndexedScopedElem.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
abstract class AbstractIndexedClarkElem[U <: ClarkElemApi.Aux[U]](
    val docUriOption: Option[URI],
    val parentBaseUriOption: Option[URI],
    val underlyingRootElem: U,
    val path: Path,
    val underlyingElem: U) extends Nodes.Elem with IndexedClarkElemApi with ClarkElemLike {

  type ThisElem <: AbstractIndexedClarkElem.Aux[ThisElem, U]

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(underlyingElem == underlyingRootElem.getElemOrSelfByPath(path), "Corrupt element!")
    assert(parentBaseUriOption == AbstractIndexedClarkElem.computeParentBaseUriOption(this), "Corrupt element!")
  }

  def findAllChildElems: immutable.IndexedSeq[ThisElem]

  def rootElem: ThisElem

  def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem]

  /**
   * Returns the optional base URI. This method is fast, due to the use of the optional base URI of
   * the parent element, if any.
   */
  final def baseUriOption: Option[URI] = {
    XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, underlyingElem)(XmlBaseSupport.JdkUriResolver)
  }

  /**
   * Returns the base URI, falling back to the empty URI if absent.
   */
  final def baseUri: URI = baseUriOption.getOrElse(new URI(""))

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  final def docUri: URI = docUriOption.getOrElse(new URI(""))

  final def resolvedName: EName = underlyingElem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    underlyingElem.resolvedAttributes

  final def text: String = underlyingElem.text

  final def reverseAncestryOrSelfENames: immutable.IndexedSeq[EName] = {
    rootElem.resolvedName +: path.entries.map(_.elementName)
  }

  final def reverseAncestryENames: immutable.IndexedSeq[EName] = {
    reverseAncestryOrSelfENames.dropRight(1)
  }

  final def reverseAncestry: immutable.IndexedSeq[ThisElem] = {
    reverseAncestryOrSelf.init
  }
}

object AbstractIndexedClarkElem {

  /**
   * This query API type, restricting ThisElem to the first type parameter.
   *
   * @tparam E The element self type
   * @tparam U The underlying element type
   */
  type Aux[E, U <: ClarkElemApi.Aux[U]] = AbstractIndexedClarkElem[U] { type ThisElem = E }

  private[yaidom] def computeParentBaseUriOption(elm: AbstractIndexedClarkElem[_ <: ClarkElemApi]): Option[URI] = {
    if (elm.path.isEmpty) {
      elm.docUriOption
    } else {
      XmlBaseSupport.findBaseUriByDocUriAndPath(elm.docUriOption, elm.underlyingRootElem, elm.path.parentPath)(XmlBaseSupport.JdkUriResolver)
    }
  }
}