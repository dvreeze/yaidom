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

package eu.cdevreeze.yaidom.queryapi

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope

/**
 * The equivalent of BackingElemApi, but as a functional API where each function takes the element itself as first parameter.
 * It is meant to be used for backing element function abstractions in "XML dialect support", which are more type-safe
 * element class hierarchies (that themselves offer a yaidom query API, but more type-safe). This enables the same
 * XML dialect supporting type-safe element classes to be backed by pluggable backing element implementations.
 *
 * Efficient implementations are possible for indexed elements and Saxon NodeInfo objects (backed by native tiny trees).
 * Saxon-backed elements are not offered by core yaidom, however. Saxon tiny trees are attractive for their low memory
 * footprint.
 *
 * It is quite possible that for XML dialect support richer element function APIs are needed, adding methods like conversions
 * to simple elements, or exposing underlying elements (such as Saxon NodeInfo objects), etc.
 *
 * @author Chris de Vreeze
 */
trait BackingElemFunctionApi {

  type Elem

  // ClarkElemApi

  // ClarkElemApi: ElemApi part

  def filterChildElems(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem]

  def filterElems(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem]

  def filterElemsOrSelf(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem]

  def findAllChildElems(elem: Elem): immutable.IndexedSeq[Elem]

  def findAllElems(elem: Elem): immutable.IndexedSeq[Elem]

  def findAllElemsOrSelf(elem: Elem): immutable.IndexedSeq[Elem]

  def findChildElem(elem: Elem, p: Elem => Boolean): Option[Elem]

  def findElem(elem: Elem, p: Elem => Boolean): Option[Elem]

  def findElemOrSelf(elem: Elem, p: Elem => Boolean): Option[Elem]

  def findTopmostElems(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem]

  def findTopmostElemsOrSelf(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem]

  def getChildElem(elem: Elem, p: Elem => Boolean): Elem

  // ClarkElemApi: IsNavigableApi part

  def findAllChildElemsWithPathEntries(elem: Elem): immutable.IndexedSeq[(Elem, Path.Entry)]

  def findChildElemByPathEntry(elem: Elem, entry: Path.Entry): Option[Elem]

  def findElemOrSelfByPath(elem: Elem, path: Path): Option[Elem]

  def findReverseAncestryOrSelfByPath(elem: Elem, path: Path): Option[immutable.IndexedSeq[Elem]]

  def getChildElemByPathEntry(elem: Elem, entry: Path.Entry): Elem

  def getElemOrSelfByPath(elem: Elem, path: Path): Elem

  def getReverseAncestryOrSelfByPath(elem: Elem, path: Path): immutable.IndexedSeq[Elem]

  // ClarkElemApi: HasENameApi part

  def resolvedName(elem: Elem): EName

  def localName(elem: Elem): String

  def resolvedAttributes(elem: Elem): immutable.IndexedSeq[(EName, String)]

  def attributeOption(elem: Elem, expandedName: EName): Option[String]

  def attribute(elem: Elem, expandedName: EName): String

  def findAttributeByLocalName(elem: Elem, localName: String): Option[String]

  // ClarkElemApi: HasTextApi part

  def text(elem: Elem): String

  def trimmedText(elem: Elem): String

  def normalizedText(elem: Elem): String

  // ScopedElemApi, except for ClarkElemApi

  // ScopedElemApi: HasQNameApi part

  def qname(elem: Elem): QName

  def attributes(elem: Elem): immutable.IndexedSeq[(QName, String)]

  // ScopedElemApi: HasScope part

  def scope(elem: Elem): Scope

  // ScopedElemApi: own methods

  def textAsQName(elem: Elem): QName

  def textAsResolvedQName(elem: Elem): EName

  def attributeAsQNameOption(elem: Elem, expandedName: EName): Option[QName]

  def attributeAsQName(elem: Elem, expandedName: EName): QName

  def attributeAsResolvedQNameOption(elem: Elem, expandedName: EName): Option[EName]

  def attributeAsResolvedQName(elem: Elem, expandedName: EName): EName

  // Other functions, from IndexedClarkElemApi, IndexedScopedElemApi, HasParentApi etc.

  def baseUriOption(elem: Elem): Option[URI]

  def baseUri(elem: Elem): URI

  def docUriOption(elem: Elem): Option[URI]

  def docUri(elem: Elem): URI

  def parentBaseUriOption(elem: Elem): Option[URI]

  def path(elem: Elem): Path

  def rootElem(elem: Elem): Elem

  def reverseAncestryOrSelf(elem: Elem): immutable.IndexedSeq[Elem]

  def reverseAncestry(elem: Elem): immutable.IndexedSeq[Elem]

  def reverseAncestryOrSelfENames(elem: Elem): immutable.IndexedSeq[EName]

  def reverseAncestryENames(elem: Elem): immutable.IndexedSeq[EName]

  def namespaces(elem: Elem): Declarations

  def parentOption(elem: Elem): Option[Elem]

  def parent(elem: Elem): Elem

  def ancestors(elem: Elem): immutable.IndexedSeq[Elem]

  def ancestorsOrSelf(elem: Elem): immutable.IndexedSeq[Elem]

  def findAncestor(elem: Elem, p: Elem => Boolean): Option[Elem]

  def findAncestorOrSelf(elem: Elem, p: Elem => Boolean): Option[Elem]
}

object BackingElemFunctionApi {

  /**
   * This query function API type, restricting Elem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = BackingElemFunctionApi { type Elem = E }
}
