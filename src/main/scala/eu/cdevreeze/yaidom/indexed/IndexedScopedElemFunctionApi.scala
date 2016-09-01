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
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemFunctionApi
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike

final class IndexedScopedElemFunctionApi[U <: ScopedElemApi.Aux[U]] extends BackingElemFunctionApi {

  type Elem = IndexedScopedNode.Elem[U]

  // ClarkElemApi

  // ClarkElemApi: ElemApi part

  def filterChildElems(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
    elem.filterChildElems(p)
  }

  def filterElems(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
    elem.filterElems(p)
  }

  def filterElemsOrSelf(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
    elem.filterElemsOrSelf(p)
  }

  def findAllChildElems(elem: Elem): immutable.IndexedSeq[Elem] = {
    elem.findAllChildElems
  }

  def findAllElems(elem: Elem): immutable.IndexedSeq[Elem] = {
    elem.findAllElems
  }

  def findAllElemsOrSelf(elem: Elem): immutable.IndexedSeq[Elem] = {
    elem.findAllElemsOrSelf
  }

  def findChildElem(elem: Elem, p: Elem => Boolean): Option[Elem] = {
    elem.findChildElem(p)
  }

  def findElem(elem: Elem, p: Elem => Boolean): Option[Elem] = {
    elem.findElem(p)
  }

  def findElemOrSelf(elem: Elem, p: Elem => Boolean): Option[Elem] = {
    elem.findElemOrSelf(p)
  }

  def findTopmostElems(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
    elem.findTopmostElems(p)
  }

  def findTopmostElemsOrSelf(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
    elem.findTopmostElemsOrSelf(p)
  }

  def getChildElem(elem: Elem, p: Elem => Boolean): Elem = {
    elem.getChildElem(p)
  }

  // ClarkElemApi: IsNavigableApi part

  def findAllChildElemsWithPathEntries(elem: Elem): immutable.IndexedSeq[(Elem, Path.Entry)] = {
    elem.findAllChildElemsWithPathEntries
  }

  def findChildElemByPathEntry(elem: Elem, entry: Path.Entry): Option[Elem] = {
    elem.findChildElemByPathEntry(entry)
  }

  def findElemOrSelfByPath(elem: Elem, path: Path): Option[Elem] = {
    elem.findElemOrSelfByPath(path)
  }

  def findReverseAncestryOrSelfByPath(elem: Elem, path: Path): Option[immutable.IndexedSeq[Elem]] = {
    elem.findReverseAncestryOrSelfByPath(path)
  }

  def getChildElemByPathEntry(elem: Elem, entry: Path.Entry): Elem = {
    elem.getChildElemByPathEntry(entry)
  }

  def getElemOrSelfByPath(elem: Elem, path: Path): Elem = {
    elem.getElemOrSelfByPath(path)
  }

  def getReverseAncestryOrSelfByPath(elem: Elem, path: Path): immutable.IndexedSeq[Elem] = {
    elem.getReverseAncestryOrSelfByPath(path)
  }

  // ClarkElemApi: HasENameApi part

  def resolvedName(elem: Elem): EName = {
    elem.resolvedName
  }

  def localName(elem: Elem): String = {
    elem.localName
  }

  def resolvedAttributes(elem: Elem): immutable.IndexedSeq[(EName, String)] = {
    elem.resolvedAttributes.toIndexedSeq
  }

  def attributeOption(elem: Elem, expandedName: EName): Option[String] = {
    elem.attributeOption(expandedName)
  }

  def attribute(elem: Elem, expandedName: EName): String = {
    elem.attribute(expandedName)
  }

  def findAttributeByLocalName(elem: Elem, localName: String): Option[String] = {
    elem.findAttributeByLocalName(localName)
  }

  // ClarkElemApi: HasTextApi part

  def text(elem: Elem): String = {
    elem.text
  }

  def trimmedText(elem: Elem): String = {
    elem.trimmedText
  }

  def normalizedText(elem: Elem): String = {
    elem.normalizedText
  }

  // ScopedElemApi, except for ClarkElemApi

  // ScopedElemApi: HasQNameApi part

  def qname(elem: Elem): QName = {
    elem.qname
  }

  def attributes(elem: Elem): immutable.IndexedSeq[(QName, String)] = {
    elem.attributes
  }

  // ScopedElemApi: HasScope part

  def scope(elem: Elem): Scope = {
    elem.scope
  }

  // ScopedElemApi: own methods

  def textAsQName(elem: Elem): QName = {
    elem.textAsQName
  }

  def textAsResolvedQName(elem: Elem): EName = {
    elem.textAsResolvedQName
  }

  def attributeAsQNameOption(elem: Elem, expandedName: EName): Option[QName] = {
    elem.attributeAsQNameOption(expandedName)
  }

  def attributeAsQName(elem: Elem, expandedName: EName): QName = {
    elem.attributeAsQName(expandedName)
  }

  def attributeAsResolvedQNameOption(elem: Elem, expandedName: EName): Option[EName] = {
    elem.attributeAsResolvedQNameOption(expandedName)
  }

  def attributeAsResolvedQName(elem: Elem, expandedName: EName): EName = {
    elem.attributeAsResolvedQName(expandedName)
  }

  // Other functions, from IndexedClarkElemApi, IndexedScopedElemApi, HasParentApi etc.

  def baseUriOption(elem: Elem): Option[URI] = {
    elem.baseUriOption
  }

  def baseUri(elem: Elem): URI = {
    elem.baseUri
  }

  def docUriOption(elem: Elem): Option[URI] = {
    elem.docUriOption
  }

  def docUri(elem: Elem): URI = {
    elem.docUri
  }

  def parentBaseUriOption(elem: Elem): Option[URI] = {
    elem.parentBaseUriOption
  }

  def path(elem: Elem): Path = {
    elem.path
  }

  def rootElem(elem: Elem): Elem = {
    elem.rootElem
  }

  def reverseAncestryOrSelf(elem: Elem): immutable.IndexedSeq[Elem] = {
    elem.reverseAncestryOrSelf
  }

  def reverseAncestry(elem: Elem): immutable.IndexedSeq[Elem] = {
    elem.reverseAncestry
  }

  def reverseAncestryOrSelfENames(elem: Elem): immutable.IndexedSeq[EName] = {
    elem.reverseAncestryOrSelfENames
  }

  def reverseAncestryENames(elem: Elem): immutable.IndexedSeq[EName] = {
    elem.reverseAncestryENames
  }

  def namespaces(elem: Elem): Declarations = {
    elem.namespaces
  }

  def parentOption(elem: Elem): Option[Elem] = {
    elem.parentOption
  }

  def parent(elem: Elem): Elem = {
    elem.parent
  }

  def ancestors(elem: Elem): immutable.IndexedSeq[Elem] = {
    elem.ancestors
  }

  def ancestorsOrSelf(elem: Elem): immutable.IndexedSeq[Elem] = {
    elem.ancestorsOrSelf
  }

  def findAncestor(elem: Elem, p: Elem => Boolean): Option[Elem] = {
    elem.findAncestor(p)
  }

  def findAncestorOrSelf(elem: Elem, p: Elem => Boolean): Option[Elem] = {
    elem.findAncestorOrSelf(p)
  }
}
