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
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike

/**
 * Element implementation that contains an underlying element as well as a Path.
 * It can be used for temporarily indexing underlying element trees (or parts thereof)
 * with Paths, relative to the root element. Therefore these IndexedElem objects
 * can be used for retrieving ancestor or sibling elements, provided the root element
 * is retained. They can also be used to easily find elements that have a given
 * ancestry.
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
 * For example:
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
final class IndexedElem[E <: ClarkElemApi[E]](val elem: E, val path: Path) extends ClarkElemLike[IndexedElem[E]] {

  /**
   * '''Core method''' that returns '''all child elements''', in the correct order.
   * Other operations can be defined in terms of this one.
   *
   * Typically this method is slower than the same method for the underlying element
   * implementation. Therefore querying these elements is slower than querying the
   * underlying elements. Hence, use class IndexedElem only if the benefits are
   * greater than the costs.
   *
   * This method is a lot slower than the same method on the underlying element
   * implementation if getting the expanded name of the underlying element is costly.
   */
  final def findAllChildElems: immutable.IndexedSeq[IndexedElem[E]] = {
    val nextEntries = mutable.Map[EName, Int]()

    elem.findAllChildElems map { elem =>
      val ename = elem.resolvedName
      val idx = nextEntries.getOrElse(ename, 0)
      val pathEntry = Path.Entry(ename, idx)
      nextEntries.put(ename, idx + 1)

      new IndexedElem(elem, path.append(pathEntry))
    }
  }

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] =
    elem.resolvedAttributes

  final def text: String = elem.text
}

object IndexedElem {

  /**
   * Creates an IndexedElem with the given underlying element and path.
   */
  def apply[E <: ClarkElemApi[E]](elem: E, path: Path): IndexedElem[E] =
    new IndexedElem[E](elem, path)

  /**
   * Returns `apply(elem, Path.Root)`.
   */
  def apply[E <: ClarkElemApi[E]](elem: E): IndexedElem[E] =
    apply(elem, Path.Root)
}
