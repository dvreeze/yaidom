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
import scala.reflect.ClassTag
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * An element within its context. In other words, an element as a pair containing the root element (of an underlying element type)
 * and a path (from that root element) to this element. More precisely, this element implementation contains an underlying root element,
 * a Path, and an underlying element found from the root element following the Path. It also contains an optional URI
 * of the containing document, if any.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * An `IndexedClarkElem(rootElem)` can be seen as one '''immutable snapshot''' of an XML tree. All queries (using the `ElemApi` uniform
 * query API) on that snapshot return results within the same snapshot. Take care not to mix up query results from different
 * snapshots. (This could have been modeled in an alternative design of the class, using a member type, but such a design has
 * not been chosen.)
 *
 * Using IndexedClarkElem objects, it is easy to get the ancestry or siblings of an element, as elements of the underlying element type.
 *
 * Be careful not to create any '''memory leaks'''. After all, an element, even a leaf element, typically keeps the entire underlying
 * document element tree as state. Hence the underlying document element tree will always remain in memory if at least
 * one indexed element contains it in its state. (Yet with mutable org.w3c.dom element trees, it is also easy to cause
 * memory leaks. See http://apmblog.compuware.com/2011/04/20/the-top-java-memory-problems-part-1/.)
 *
 * Having an IndexedClarkElem, it is always possible to re-create the root element as IndexedClarkElem, because
 * the underlying root element is always available. On the other hand, creating an IndexedClarkElem is expensive. Class
 * IndexedClarkElem is optimized for fast querying, at the expense of costly recursive creation.
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
 * // The correspondence between rootElem, path and elem
 *
 * iElem.findAllElemsOrSelf.forall(e => e.rootElem.findElemOrSelfByPath(e.path).get == e.elem)
 * }}}
 *
 * The correspondence between queries on IndexedClarkElems and the same queries on the underlying elements is as follows:
 * {{{
 * // Let p be a function from underlying element type E to Boolean
 *
 * IndexedClarkElem(rootElem).filterElemsOrSelf(e => p(e.elem)).map(_.elem) ==
 *   rootElem.filterElemsOrSelf(p)
 * }}}
 *
 * Analogous properties hold for the other query methods.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
final class IndexedClarkElem[U <: ClarkElemApi[U]] private (
  val docUriOption: Option[URI],
  val parentBaseUriOption: Option[URI],
  val rootElem: U,
  childElems: immutable.IndexedSeq[IndexedClarkElem[U]],
  val path: Path,
  val elem: U,
  val uriResolver: XmlBaseSupport.UriResolver) extends Nodes.Elem with IndexedClarkElemLike[IndexedClarkElem[U], U] {

  private implicit val uTag: ClassTag[U] = classTag[U]

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(elem == rootElem.getElemOrSelfByPath(path), "Corrupt element!")
    assert(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
    assert(childElems.forall(_.docUriOption eq this.docUriOption), "Corrupt element!")
  }

  final def findAllChildElems: immutable.IndexedSeq[IndexedClarkElem[U]] = childElems

  final override def equals(obj: Any): Boolean = obj match {
    case other: IndexedClarkElem[U] =>
      (other.docUriOption == this.docUriOption) && (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  final override def hashCode: Int = (docUriOption, rootElem, path).hashCode

  final def baseUriOption: Option[URI] = {
    XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, elem)(uriResolver)
  }

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  final def docUri: URI = docUriOption.getOrElse(new URI(""))

  /**
   * Returns the base URI, falling back to the empty URI if absent.
   */
  final def baseUri: URI = baseUriOption.getOrElse(new URI(""))
}

object IndexedClarkElem {

  /**
   * Builder of `IndexedClarkElem` objects. The builder has a chosen URI resolver strategy. Typically these
   * builders are long-lived global objects. Each element created with this builder will have the same URI resolver,
   * viz. the one passed as constructor argument of the builder.
   */
  final case class Builder[U <: ClarkElemApi[U]](
    val underlyingType: ClassTag[U],
    override val uriResolver: XmlBaseSupport.UriResolver) extends IndexedClarkElemApi.Builder[IndexedClarkElem[U], U] {

    override def build(rootElem: U): IndexedClarkElem[U] =
      build(None, rootElem)

    override def build(docUriOption: Option[URI], rootElem: U): IndexedClarkElem[U] =
      build(docUriOption, rootElem, Path.Root)

    override def build(rootElem: U, path: Path): IndexedClarkElem[U] = {
      build(None, rootElem, path)
    }

    /**
     * Expensive recursive factory method for "indexed elements".
     */
    override def build(docUriOption: Option[URI], rootElem: U, path: Path): IndexedClarkElem[U] = {
      // Expensive call, so invoked only once
      val elem = rootElem.findElemOrSelfByPath(path).getOrElse(
        sys.error(s"Could not find the element with path $path from root ${rootElem.resolvedName}"))

      val parentBaseUriOption: Option[URI] =
        path.parentPathOption.flatMap(pp => XmlBaseSupport.findBaseUriByDocUriAndPath(docUriOption, rootElem, pp)(uriResolver)).orElse(docUriOption)

      build(docUriOption, parentBaseUriOption, rootElem, path, elem)
    }

    private def build(
      docUriOption: Option[URI],
      parentBaseUriOption: Option[URI],
      rootElem: U,
      path: Path,
      elem: U): IndexedClarkElem[U] = {

      val baseUriOption =
        XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, elem)(uriResolver)

      // Recursive calls
      val childElems = elem.findAllChildElemsWithPathEntries map {
        case (e, entry) =>
          build(docUriOption, baseUriOption, rootElem, path.append(entry), e)
      }

      new IndexedClarkElem(docUriOption, parentBaseUriOption, rootElem, childElems, path, elem, uriResolver)
    }
  }
}
