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

package eu.cdevreeze.yaidom.queryapi2

import java.net.URI

import scala.annotation.elidable
import scala.annotation.elidable.ASSERTION
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * XML Base support, for elements implementing the `ClarkElemApi` query API.
 *
 * XML Base is very simple in its algorithm, given an optional start "document URI". Base URI computation for an element then starts
 * with the optional document URI, and processes all XML Base attributes in the reverse ancestry-or-self of the element,
 * resolving each XML Base attribute against the base URI computed so far. According to the XML Base specification,
 * same-document references do not alter this algorithm.
 *
 * What is sensitive in XML Base processing is the resolution of an URI against an optional base URI. For example, resolving
 * an empty URI using the `java.net.URI.resolve` method does not conform to RFC 3986
 * (see e.g. http://stackoverflow.com/questions/22203111/is-javas-uri-resolve-incompatible-with-rfc-3986-when-the-relative-uri-contains).
 *
 * This is why the user of this XML Base support must supply a strategy for resolving URIs against optional base URIs.
 *
 * Default attributes and entity resolution are out of scope for this XML Base support.
 *
 * @author Chris de Vreeze
 */
object XmlBaseSupport {

  /**
   * Resolver of a URI against an optional base URI, returning the resolved URI
   */
  type UriResolver = ((URI, Option[URI]) => URI)

  /**
   * Computes the optional base URI, given an optional document URI, the root element, and the Path from the root
   * element to "this" element. A URI resolution strategy must be provided as well.
   */
  def findBaseUriByDocUriAndPath(docUriOption: Option[URI], rootElem: ClarkElemApi, path: Path)(resolveUri: UriResolver): Option[URI] = {
    val reverseAncestryOrSelf: immutable.IndexedSeq[ClarkElemApi] =
      rootElem.findReverseAncestryOrSelfByPath(path).getOrElse(
        sys.error(s"Corrupt data. Could not get reverse ancestry-or-self of ${rootElem.getElemOrSelfByPath(path)}"))

    assert(reverseAncestryOrSelf.head == rootElem)

    reverseAncestryOrSelf.foldLeft(docUriOption) {
      case (parentBaseUriOption, elm) =>
        findBaseUriByParentBaseUri(parentBaseUriOption, elm)(resolveUri)
    }
  }

  /**
   * Returns the optional base URI, given an optional parent base URI, and "this" element. A URI resolution strategy must be provided as well.
   */
  def findBaseUriByParentBaseUri(parentBaseUriOption: Option[URI], elem: ClarkElemApi)(resolveUri: UriResolver): Option[URI] = {
    val baseUriAttributeOption = elem.attributeOption(XmlBaseEName).map(s => new URI(s))
    baseUriAttributeOption.map(bu => resolveUri(bu, parentBaseUriOption)).orElse(parentBaseUriOption)
  }

  /**
   * URI resolver using method `java.net.URI.resolve`. It does not conform to RFC 3986.
   */
  val JdkUriResolver: UriResolver = { (uri: URI, baseUriOption: Option[URI]) =>
    baseUriOption.map(bu => bu.resolve(uri)).getOrElse(uri)
  }

  val XmlNs = "http://www.w3.org/XML/1998/namespace"

  val XmlBaseEName = EName(XmlNs, "base")
}
