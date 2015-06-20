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
import scala.collection.mutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.queryapi.NavigableClarkElemApi

/**
 * Element implementation that contains an underlying root element, a Path, and an underlying element
 * found from the root element following the Path. It can be used for temporarily indexing underlying
 * element trees (or parts thereof) with Paths, relative to the root element. Therefore these
 * IndexedElem objects can be used for retrieving ancestor or sibling elements. They can also
 * be used to easily find elements that have a given ancestry.
 *
 * Having an IndexedElem, it is always possible to re-create the root element as IndexedElem, because
 * the underlying root element is always available. On the other hand, creating an IndexedElem
 * is expensive. Class IndexedElem is optimized for fast querying, at the expense of
 * expensive recursive creation.
 *
 * ==IndexedElem examples==
 *
 * The following example code shows how to query for elements with a known ancestry,
 * regardless of the element implementation, if efficiency is not important:
 *
 * {{{
 * val iBookstore = IndexedElem(bookstore)
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
 * ==IndexedElem more formally==
 *
 * '''In order to use this class, this more formal section can safely be skipped.'''
 *
 * The ``IndexedElem`` class can be understood in a precise <em>mathematical</em> sense, as shown below.
 *
 * Some properties of IndexedElems are as follows:
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
 * The correspondence between queries on IndexedElems and the same queries on the underlying elements is as follows:
 * {{{
 * // Let p be a function from underlying element type E to Boolean
 *
 * IndexedElem(rootElem).filterElemsOrSelf(e => p(e.elem)).map(_.elem) ==
 *   rootElem.filterElemsOrSelf(p)
 * }}}
 *
 * Analogous properties hold for the other query methods.
 *
 * @tparam E The underlying element type
 *
 * @author Chris de Vreeze
 */
final class IndexedElem[E <: NavigableClarkElemApi[E]] private (
  val rootElem: E,
  childElems: immutable.IndexedSeq[IndexedElem[E]],
  val path: Path,
  val elem: E) extends ClarkElemLike[IndexedElem[E]] with IsNavigable[IndexedElem[E]] {

  final def findAllChildElems: immutable.IndexedSeq[IndexedElem[E]] = childElems

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    elem.resolvedAttributes

  final def text: String = elem.text

  final def findChildElemByPathEntry(entry: Path.Entry): Option[IndexedElem[E]] = {
    findAllChildElems.find(_.path.lastEntry == entry)
  }
}

object IndexedElem {

  /**
   * Returns the same as `apply(rootElem, Path.Root)`.
   */
  def apply[E <: NavigableClarkElemApi[E]](rootElem: E): IndexedElem[E] =
    apply(rootElem, Path.Root)

  /**
   * Expensive recursive factory method for "indexed elements".
   */
  def apply[E <: NavigableClarkElemApi[E]](rootElem: E, path: Path): IndexedElem[E] = {
    // Expensive call, so invoked only once
    val elem = rootElem.findElemOrSelfByPath(path).getOrElse(
      sys.error(s"Could not find the element with path $path from root ${rootElem.resolvedName}"))

    apply(rootElem, path, elem)
  }

  private def apply[E <: NavigableClarkElemApi[E]](rootElem: E, path: Path, elem: E): IndexedElem[E] = {
    // Recursive calls
    val childElems = findAllChildElemsWithPathEntries(elem) map {
      case (e, entry) =>
        apply(rootElem, path.append(entry), e)
    }

    new IndexedElem(rootElem, childElems, path, elem)
  }

  private def findAllChildElemsWithPathEntries[E <: NavigableClarkElemApi[E]](elem: E): immutable.IndexedSeq[(E, Path.Entry)] = {
    val nextEntries = mutable.Map[EName, Int]()

    elem.findAllChildElems map { e =>
      val ename = e.resolvedName
      val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
      nextEntries.put(ename, entry.index + 1)
      (e, entry)
    }
  }
}
