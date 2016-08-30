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

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.resolved.ResolvedNodes

object IndexedClarkNode {

  /**
   * Node super-type of indexed Clark elements.
   *
   * @author Chris de Vreeze
   */
  sealed trait Node extends Nodes.Node

  /**
   * An element within its context. In other words, an element as a pair containing the root element (of an underlying element type)
   * and a path (from that root element) to this element. More precisely, this element implementation contains an underlying root element,
   * a Path, and an underlying element found from the root element following the Path. It also contains an optional URI
   * of the containing document, if any.
   *
   * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
   *
   * An `IndexedClarkElem(underlyingRootElem)` can be seen as one '''immutable snapshot''' of an XML tree. All queries (using the `ElemApi` uniform
   * query API) on that snapshot return results within the same snapshot. Take care not to mix up query results from different
   * snapshots. (This could have been modeled in an alternative design of the class, but such a design has not been chosen.)
   *
   * Using IndexedClarkElem objects, it is easy to get the ancestry or siblings of an element, as elements of the underlying element type.
   *
   * Be careful not to create any '''memory leaks'''. After all, an element, even a leaf element, typically keeps the entire underlying
   * document element tree as state. Hence the underlying document element tree will always remain in memory if at least
   * one indexed element contains it in its state. (Yet with mutable org.w3c.dom element trees, it is also easy to cause
   * memory leaks. See http://apmblog.compuware.com/2011/04/20/the-top-java-memory-problems-part-1/.)
   *
   * The optional parent base URI is stored for very fast (optional) base URI computation. This is helpful in
   * an XBRL context, where URI resolution against a base URI is typically a very frequent operation.
   *
   * ==IndexedClarkElem examples==
   *
   * The following example code shows how to query for elements with a known ancestry,
   * regardless of the element implementation, if efficiency is not important:
   *
   * {{{
   * val iBookstore = IndexedClarkElem(bookstore)
   *
   * val iTheBookAuthors =
   *   for {
   *     iAuthor <- iBookstore.filterElems(withLocalName("Author"))
   *     bookPath <- iAuthor.path.findAncestorPath(_.elementNameOption.map(_.localPart) == Some("Book"))
   *     iBook <- iBookstore.findElem(_.path == bookPath)
   *     if iBook.getChildElem(withLocalName("Title")).elem.text.startsWith("Programming in Scala")
   *   } yield iAuthor
   * }}}
   *
   * ==IndexedClarkElem more formally==
   *
   * '''In order to use this class, this more formal section can safely be skipped.'''
   *
   * The ``IndexedClarkElem`` class can be understood in a precise <em>mathematical</em> sense, as shown below.
   *
   * Some properties of IndexedClarkElems are as follows:
   * {{{
   * // All elements (obtained from querying other elements) have the same rootElem
   *
   * iElem.findAllElemsOrSelf.map(_.rootElem).distinct == List(iElem.rootElem)
   *
   * iElem.findAllElemsOrSelf.map(_.underlyingRootElem).distinct == List(iElem.underlyingRootElem)
   *
   * // The correspondence between rootElem, path and elem
   *
   * iElem.findAllElemsOrSelf.forall(e => e.rootElem.findElemOrSelfByPath(e.path).get == e)
   *
   * iElem.findAllElemsOrSelf.forall(e => e.underlyingRootElem.findElemOrSelfByPath(e.path).get == e.underlyingElem)
   * }}}
   *
   * The correspondence between queries on IndexedClarkElems and the same queries on the underlying elements is as follows:
   * {{{
   * // Let p be a function from underlying element type E to Boolean
   *
   * iElem.rootElem.filterElemsOrSelf(e => p(e.underlyingElem)).map(_.underlyingElem) ==
   *   iElem.underlyingRootElem.filterElemsOrSelf(p)
   * }}}
   *
   * Analogous properties hold for the other query methods.
   *
   * @author Chris de Vreeze
   */
  final class Elem[U <: ClarkElemApi.Aux[U]] private[IndexedClarkNode] (
    docUriOption: Option[URI],
    underlyingRootElem: U,
    path: Path,
    underlyingElem: U) extends AbstractIndexedClarkElem(docUriOption, underlyingRootElem, path, underlyingElem) with Node with Nodes.Elem {

    type ThisElemApi = Elem[U]

    type ThisElem = Elem[U]

    def thisElem: ThisElem = this

    final def findAllChildElems: immutable.IndexedSeq[ThisElem] = {
      underlyingElem.findAllChildElemsWithPathEntries map {
        case (e, entry) =>
          new Elem(docUriOption, underlyingRootElem, path.append(entry), e)
      }
    }

    final override def equals(obj: Any): Boolean = obj match {
      case other: Elem[U] =>
        (other.docUriOption == this.docUriOption) && (other.underlyingRootElem == this.underlyingRootElem) &&
          (other.path == this.path) && (other.underlyingElem == this.underlyingElem)
      case _ => false
    }

    final override def hashCode: Int = (docUriOption, underlyingRootElem, path, underlyingElem).hashCode

    final def rootElem: ThisElem = {
      new Elem[U](docUriOption, underlyingRootElem, Path.Empty, underlyingRootElem)
    }

    final def reverseAncestryOrSelf: immutable.IndexedSeq[ThisElem] = {
      val resultOption = rootElem.findReverseAncestryOrSelfByPath(path)

      assert(resultOption.isDefined, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
      assert(!resultOption.get.isEmpty, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
      assert(resultOption.get.last == thisElem)

      resultOption.get
    }
  }

  final case class Text(text: String, isCData: Boolean) extends Node with Nodes.Text {
    require(text ne null)
    if (isCData) require(!text.containsSlice("]]>"))

    /** Returns `text.trim`. */
    def trimmedText: String = text.trim

    /** Returns `XmlStringUtils.normalizeString(text)` .*/
    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  object Elem {

    def apply[U <: ClarkElemApi.Aux[U]](docUriOption: Option[URI], underlyingRootElem: U, path: Path): Elem[U] = {
      new Elem[U](docUriOption, underlyingRootElem, path, underlyingRootElem.getElemOrSelfByPath(path))
    }

    def apply[U <: ClarkElemApi.Aux[U]](docUri: URI, underlyingRootElem: U, path: Path): Elem[U] = {
      apply(Some(docUri), underlyingRootElem, path)
    }

    def apply[U <: ClarkElemApi.Aux[U]](underlyingRootElem: U, path: Path): Elem[U] = {
      new Elem[U](None, underlyingRootElem, path, underlyingRootElem.getElemOrSelfByPath(path))
    }

    def apply[U <: ClarkElemApi.Aux[U]](docUriOption: Option[URI], underlyingRootElem: U): Elem[U] = {
      apply(docUriOption, underlyingRootElem, Path.Empty)
    }

    def apply[U <: ClarkElemApi.Aux[U]](docUri: URI, underlyingRootElem: U): Elem[U] = {
      apply(Some(docUri), underlyingRootElem)
    }

    def apply[U <: ClarkElemApi.Aux[U]](underlyingRootElem: U): Elem[U] = {
      apply(None, underlyingRootElem, Path.Empty)
    }

    /**
     * Returns the child nodes of the given element, provided the underlying element knows its child nodes too
     * (which is assured by the type restriction on the underlying element).
     */
    def getChildren[U <: ClarkElemApi.Aux[U] with ResolvedNodes.Elem](elem: Elem[U]): immutable.IndexedSeq[Node] = {
      val childElems = elem.findAllChildElems
      var childElemIdx = 0

      val children =
        elem.underlyingElem.children flatMap {
          case che: Nodes.Elem =>
            val e = childElems(childElemIdx)
            childElemIdx += 1
            Some(e)
          case ch: Nodes.Text =>
            Some(Text(ch.text, false))
          case ch =>
            None
        }

      assert(childElemIdx == childElems.size)

      children
    }
  }
}
