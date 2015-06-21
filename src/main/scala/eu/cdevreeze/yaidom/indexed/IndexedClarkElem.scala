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

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi

/**
 * Element implementation that contains an underlying root element, a Path, and an underlying element
 * found from the root element following the Path. It can be used for temporarily indexing underlying
 * element trees (or parts thereof) with Paths, relative to the root element. Therefore these
 * IndexedClarkElem objects can be used for retrieving ancestor or sibling elements. They can also
 * be used to easily find elements that have a given ancestry.
 *
 * Having an IndexedClarkElem, it is always possible to re-create the root element as IndexedClarkElem, because
 * the underlying root element is always available. On the other hand, creating an IndexedClarkElem
 * is expensive. Class IndexedClarkElem is optimized for fast querying, at the expense of
 * expensive recursive creation.
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
  val rootElem: U,
  childElems: immutable.IndexedSeq[IndexedClarkElem[U]],
  val path: Path,
  val elem: U) extends IndexedClarkElemLike[IndexedClarkElem[U], U] {

  private implicit val uTag: ClassTag[U] = classTag[U]

  final def findAllChildElems: immutable.IndexedSeq[IndexedClarkElem[U]] = childElems

  final override def equals(obj: Any): Boolean = obj match {
    case other: IndexedClarkElem[U] => (other.rootElem == this.rootElem) && (other.path == this.path)
    case _                          => false
  }

  final override def hashCode: Int = (rootElem, path).hashCode
}

object IndexedClarkElem {

  /**
   * Returns the same as `apply(rootElem, Path.Root)`.
   */
  def apply[U <: ClarkElemApi[U]](rootElem: U): IndexedClarkElem[U] =
    apply(rootElem, Path.Root)

  /**
   * Expensive recursive factory method for "indexed elements".
   */
  def apply[U <: ClarkElemApi[U]](rootElem: U, path: Path): IndexedClarkElem[U] = {
    // Expensive call, so invoked only once
    val elem = rootElem.findElemOrSelfByPath(path).getOrElse(
      sys.error(s"Could not find the element with path $path from root ${rootElem.resolvedName}"))

    apply(rootElem, path, elem)
  }

  private def apply[U <: ClarkElemApi[U]](rootElem: U, path: Path, elem: U): IndexedClarkElem[U] = {
    // Recursive calls
    val childElems = elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        apply(rootElem, path.append(entry), e)
    }

    new IndexedClarkElem(rootElem, childElems, path, elem)
  }
}
