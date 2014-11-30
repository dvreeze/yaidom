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

package eu.cdevreeze.yaidom.docaware

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.simple

/**
 * An element just like `indexed.Elem`, but storing the containing document URI (and base URI) as well. See [[eu.cdevreeze.yaidom.indexed.Elem]]
 * for more details. These details apply to [[eu.cdevreeze.yaidom.docaware.Elem]] too, except that additionally the document and base URIs
 * are stored.
 *
 * If XML Base support is important, but the documents may have no document URI, still consider using `docaware` elements,
 * using the empty URI as document URI. Note that the empty URI is a relative URI, containing only an empty path, and
 * that resolving URIs against the empty URI leaves those URIs the same. XML Base processing is still possible if
 * the document URI is empty and the XML Base attributes also contain only relative URIs, as long as one is aware of it.
 * Of course, the user of the API is free to reject those relative base URIs.
 *
 * The base URI of an element `elem` can be defined as follows, given document URI `docUri`:
 * {{{
 * val ancestorsOrSelf = elem.path.ancestorOrSelfPaths.reverse.map(p => elem.rootElem.getElemOrSelfByPath(p))
 *
 * val baseUri =
 *   ancestorsOrSelf.foldLeft(docUri) {
 *     case (currBaseUri, e) =>
 *       e.attributeOption(Elem.XmlBaseEName).map(s => currBaseUri.resolve(new URI(s))).getOrElse(currBaseUri)
 *   }
 * }}}
 * although the implementation is not this inefficient.
 *
 * Given a parent base URI `parentBaseUri`, which for the root element is the document URI, the base URI of element `elem` is:
 * {{{
 * elem.attributeOption(Elem.XmlBaseEName).map(s => parentBaseUri.resolve(new URI(s))).getOrElse(parentBaseUri)
 * }}}
 *
 * Note the analogy with Scope resolution, where base URIs and parent base URIs map to scopes and parent scopes, and
 * where XML Base attributes map to namespace declarations.
 *
 * @author Chris de Vreeze
 */
final class Elem private[docaware] (
  val docUri: URI,
  val parentBaseUri: URI,
  val rootElem: simple.Elem,
  childElems: immutable.IndexedSeq[Elem],
  val path: Path,
  val elem: simple.Elem) extends ScopedElemLike[Elem] with IsNavigable[Elem] with Immutable {

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(elem == rootElem.getElemOrSelfByPath(path), "Corrupt element!")
    assert(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
    assert(childElems.forall(_.docUri eq this.docUri), "Corrupt element!")
  }

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the paths, which have one more
   * "path entry".
   */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = childElems

  override def resolvedName: EName = elem.resolvedName

  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes

  override def findChildElemByPathEntry(entry: Path.Entry): Option[Elem] = {
    val filteredChildElems = childElems.toStream filter { e => e.resolvedName == entry.elementName }

    val childElemOption = filteredChildElems.drop(entry.index).headOption
    assert(childElemOption.forall(_.resolvedName == entry.elementName))
    childElemOption
  }

  override def qname: QName = elem.qname

  override def attributes: immutable.IndexedSeq[(QName, String)] = elem.attributes

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem =>
      (other.docUri == this.docUri) && (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  override def hashCode: Int = (docUri, rootElem, path).hashCode

  /**
   * Returns `this.elem.scope`
   */
  final override def scope: Scope = this.elem.scope

  /**
   * Returns the namespaces declared in this element.
   *
   * If the original parsed XML document contained duplicate namespace declarations (i.e. namespace declarations that are the same
   * as some namespace declarations in their context), these duplicate namespace declarations were lost during parsing of the
   * XML into an `Elem` tree. They therefore do not occur in the namespace declarations returned by this method.
   */
  final def namespaces: Declarations = {
    val parentScope = this.path.parentPathOption map { path => rootElem.getElemOrSelfByPath(path).scope } getOrElse (Scope.Empty)
    parentScope.relativize(this.elem.scope)
  }

  /**
   * Returns the concatenation of the texts of text children, including whitespace. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = elem.textChildren map { t => t.text }
    textStrings.mkString
  }

  /**
   * Returns the ENames of the ancestry-or-self, starting with the root element and ending with this element.
   *
   * That is, returns:
   * {{{
   * rootElem.resolvedName +: path.entries.map(_.elementName)
   * }}}
   */
  final def ancestryOrSelfENames: immutable.IndexedSeq[EName] = {
    rootElem.resolvedName +: path.entries.map(_.elementName)
  }

  /**
   * Returns the ENames of the ancestry, starting with the root element and ending with the parent of this element, if any.
   *
   * That is, returns:
   * {{{
   * ancestryOrSelfENames.dropRight(1)
   * }}}
   */
  final def ancestryENames: immutable.IndexedSeq[EName] = {
    ancestryOrSelfENames.dropRight(1)
  }

  /**
   * Returns the base URI of the element. That is, returns:
   * {{{
   * attributeOption(Elem.XmlBaseEName).map(s => parentBaseUri.resolve(new URI(s))).getOrElse(parentBaseUri)
   * }}}
   */
  final def baseUri: URI = {
    val explicitBaseUriOption = attributeOption(Elem.XmlBaseEName).map(s => new URI(s))
    explicitBaseUriOption.map(u => parentBaseUri.resolve(u)).getOrElse(parentBaseUri)
  }
}

object Elem {

  private val XmlNs = "http://www.w3.org/XML/1998/namespace"

  val XmlBaseEName = EName(XmlNs, "base")

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
   * Expensive recursive factory method for "docaware elements", which first computes the parent base URI.
   */
  def apply(docUri: URI, rootElem: simple.Elem, path: Path): Elem = {
    // Expensive call, so invoked only once
    val elems = path.entries.scanLeft(rootElem) {
      case (accElem, entry) =>
        accElem.getChildElemByPathEntry(entry)
    }

    val parentBaseUri = getBaseUri(docUri, elems.init)
    val elem = elems.last

    apply(docUri, parentBaseUri, rootElem, path, elem)
  }

  /**
   * Expensive recursive factory method for "docaware elements", which is explicitly passed the parent base URI.
   */
  def apply(docUri: URI, parentBaseUri: URI, rootElem: simple.Elem, path: Path): Elem = {
    // Expensive call, so invoked only once
    val elem = rootElem.getElemOrSelfByPath(path)

    apply(docUri, parentBaseUri, rootElem, path, elem)
  }

  private def apply(docUri: URI, parentBaseUri: URI, rootElem: simple.Elem, path: Path, elem: simple.Elem): Elem = {
    val explicitBaseUriOption = elem.attributeOption(Elem.XmlBaseEName).map(s => new URI(s))
    val baseUri = explicitBaseUriOption.map(u => parentBaseUri.resolve(u)).getOrElse(parentBaseUri)

    // Recursive calls
    val childElems = elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        apply(docUri, baseUri, rootElem, path.append(entry), e)
    }

    new Elem(docUri, parentBaseUri, rootElem, childElems, path, elem)
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
