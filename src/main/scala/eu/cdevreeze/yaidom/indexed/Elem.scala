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

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.simple

/**
 * An element within its context. In other words, an element as a pair containing the root element (as [[eu.cdevreeze.yaidom.simple.Elem]])
 * and a path (from that root element) to this element.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * An `indexed.Elem(rootElem)` can be seen as one '''immutable snapshot''' of an XML tree. All queries (using the `ElemApi` uniform
 * query API) on that snapshot return results within the same snapshot. Take care not to mix up query results from different
 * snapshots. (This could have been modeled in an alternative design of the class, using a member type, but such a design has
 * not been chosen.)
 *
 * ==Example==
 *
 * Below follows an example. This example queries for all book elements having at least Jeffrey Ullman as author. It can be written as follows,
 * assuming a book store `Document` with the appropriate structure:
 * {{{
 * val bookstoreElm = indexed.Document(doc).documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val ullmanBookElms =
 *   for {
 *     authorElm <- bookstoreElm filterElems { e =>
 *       (e.localName == "Author") &&
 *       ((e.getChildElem(_.localName == "First_Name")).text == "Jeffrey") &&
 *       ((e.getChildElem(_.localName == "Last_Name")).text == "Ullman")
 *     }
 *     bookElm <- authorElm.path findAncestorPath { _.elementNameOption == Some(EName("Book")) } map { path =>
 *       bookstoreElm.getElemOrSelfByPath(path)
 *     }
 *   } yield bookElm
 * }}}
 *
 * Note how we found an ancestor (Book) element of an Author element by first finding the appropriate ancestor path, and then
 * querying the bookstore element for the element at that path. So we remembered the document element (as indexed element),
 * and used that "snapshot" to navigate to elements at given ancestor paths of other elements. This is certainly more efficient
 * than re-indexing (using an indexed element factory method).
 *
 * ==Elem more formally==
 *
 * '''In order to get started using the class, this more formal section can safely be skipped. On the other hand, this section
 * may provide a deeper understanding of the class.'''
 *
 * Let `indexedRootElem` be a root element, so `indexedRootElem.path == Path.Root`.
 *
 * Then, first of all, we have:
 * {{{
 * indexedRootElem.elem == indexedRootElem.rootElem
 * }}}
 *
 * Given:
 * {{{
 * val elems = indexedRootElem.findAllElemsOrSelf
 * val paths = indexedRootElem.findAllElemsOrSelf.map(_.path)
 * }}}
 * the following (rather obvious) properties hold for indexed elements:
 * {{{
 * elems forall (e => e.rootElem == indexedRootElem.rootElem)
 *
 * elems forall { e => e.rootElem.findElemOrSelfByPath(e.path) == Some(e.elem) }
 * }}}
 *
 * Analogous remarks apply to the other query methods. For example, given:
 * {{{
 * // Let p be a predicate of type (simple.Elem => Boolean)
 *
 * val elems = indexedRootElem filterElems { e => p(e.elem) }
 * val paths = indexedRootElem filterElems { e => p(e.elem) } map (_.path)
 * }}}
 * we have:
 * {{{
 * elems forall (e => e.rootElem == indexedRootElem.rootElem)
 *
 * elems forall { e => e.rootElem.findElemOrSelfByPath(e.path) == Some(e.elem) }
 * }}}
 *
 * @author Chris de Vreeze
 */
final class Elem private[indexed] (
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
  }

  /**
   * Map from child node indexes to child elem indexes, for speeding up lookups of child elements
   */
  private val elemIndexesByNodeIndex: Map[Int, Int] = {
    (elem.children.zipWithIndex collect { case (e: simple.Elem, idx) => idx }).zipWithIndex.toMap
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
    val nodeIdx = elem.childNodeIndex(entry)

    if (nodeIdx < 0) None
    else {
      val elemIdx = elemIndexesByNodeIndex(nodeIdx)
      val resultElem = childElems(elemIdx)

      assert(resultElem.resolvedName == entry.elementName)
      Some(resultElem)
    }
  }

  override def qname: QName = elem.qname

  override def attributes: immutable.IndexedSeq[(QName, String)] = elem.attributes

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem => (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  override def hashCode: Int = (rootElem, path).hashCode

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
}

object Elem {

  /**
   * Calls `apply(rootElem, Path.Root)`
   */
  def apply(rootElem: simple.Elem): Elem = {
    apply(rootElem, Path.Root)
  }

  /**
   * Expensive recursive factory method for "indexed elements".
   */
  def apply(rootElem: simple.Elem, path: Path): Elem = {
    // Expensive call, so invoked only once
    val elem = rootElem.getElemOrSelfByPath(path)

    apply(rootElem, path, elem)
  }

  private def apply(rootElem: simple.Elem, path: Path, elem: simple.Elem): Elem = {
    // Recursive calls
    val childElems = elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        apply(rootElem, path.append(entry), e)
    }

    new Elem(rootElem, childElems, path, elem)
  }
}
