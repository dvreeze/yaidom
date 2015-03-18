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

package eu.cdevreeze.yaidom.docaware.alt

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.docaware.AbstractDocawareElem
import eu.cdevreeze.yaidom.simple

/**
 * Like a "docaware" element, except that the implementation prefers fast element tree creation over fast querying.
 *
 * @author Chris de Vreeze
 */
final class Elem private[docaware] (
  val docUri: URI,
  val parentBaseUri: URI,
  val rootElem: simple.Elem,
  val path: Path,
  val elem: simple.Elem) extends AbstractDocawareElem[Elem] {

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(elem == rootElem.getElemOrSelfByPath(path), "Corrupt element!")
    assert(findAllChildElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
    assert(findAllChildElems.forall(_.docUri eq this.docUri), "Corrupt element!")
  }

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the paths, which have one more
   * "path entry".
   */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = {
    val explicitBaseUriOption = elem.attributeOption(Elem.XmlBaseEName).map(s => new URI(s))
    val baseUri = explicitBaseUriOption.map(u => parentBaseUri.resolve(u)).getOrElse(parentBaseUri)

    val childElems = elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        new Elem(docUri, baseUri, rootElem, path.append(entry), e)
    }
    childElems
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem =>
      (other.docUri == this.docUri) && (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  override def hashCode: Int = (docUri, rootElem, path).hashCode
}

object Elem {

  private val XmlNs = "http://www.w3.org/XML/1998/namespace"

  private val XmlBaseEName = EName(XmlNs, "base")

  /**
   * Calls `apply(docUri, rootElem, Path.Root)`
   */
  def apply(docUri: URI, rootElem: simple.Elem): Elem = {
    apply(docUri, rootElem, Path.Root)
  }

  /**
   * Calls `apply(docUri, parentBaseUri, rootElem, Path.Root)`
   */
  def apply(docUri: URI, parentBaseUri: URI, rootElem: simple.Elem): Elem = {
    apply(docUri, parentBaseUri, rootElem, Path.Root)
  }

  /**
   * Factory method for "docaware2 elements", which first computes the parent base URI.
   */
  def apply(docUri: URI, rootElem: simple.Elem, path: Path): Elem = {
    // Expensive call, so invoked only once
    val elems = path.entries.scanLeft(rootElem) {
      case (accElem, entry) =>
        accElem.getChildElemByPathEntry(entry)
    }

    val parentBaseUri = getBaseUri(docUri, elems.init)
    val elem = elems.last

    new Elem(docUri, parentBaseUri, rootElem, path, elem)
  }

  /**
   * Factory method for "docaware2 elements", which is explicitly passed the parent base URI.
   */
  def apply(docUri: URI, parentBaseUri: URI, rootElem: simple.Elem, path: Path): Elem = {
    val elem = rootElem.getElemOrSelfByPath(path)

    new Elem(docUri, parentBaseUri, rootElem, path, elem)
  }

  /**
   * Computes the base URI of `elems.last`, if any, from the passed document URI and the passed ancestor-or-self `elems`
   * (starting with the root element, and ending with "this" element). If `elems` is empty, `docUri` is returned.
   */
  private def getBaseUri(docUri: URI, elems: immutable.IndexedSeq[simple.Elem]): URI = {
    elems.foldLeft(docUri) {
      case (accUri, elem) =>
        val explicitBaseUriOption = elem.attributeOption(Elem.XmlBaseEName).map(s => new URI(s))
        explicitBaseUriOption.map(u => accUri.resolve(u)).getOrElse(accUri)
    }
  }
}
