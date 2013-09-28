/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package indexed

import scala.collection.immutable

/**
 * An element within its context. In other words, an element as a pair containing the root element (as [[eu.cdevreeze.yaidom.Elem]])
 * and an element path (from that root element) to this element.
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
 *     bookElm <- authorElm.elemPath findAncestorPath { _.elementNameOption == Some(EName("Book")) } map { path =>
 *       authorElm.rootElem.getWithElemPath(path)
 *     }
 *   } yield bookElm
 * }}}
 *
 * ==Elem more formally==
 *
 * '''In order to get started using the class, this more formal section can safely be skipped. On the other hand, this section
 * may provide a deeper understanding of the class.'''
 *
 * Let `indexedRootElem` be a root element, so `indexedRootElem.elemPath == ElemPath.Root`.
 *
 * Then, first of all, we have:
 * {{{
 * indexedRootElem.elem == indexedRootElem.rootElem
 * }}}
 *
 * Given:
 * {{{
 * val elems = indexedRootElem.findAllElemsOrSelf
 * val elemPaths = indexedRootElem.elem.findAllElemOrSelfPaths
 * }}}
 * the following (rather obvious) properties hold for indexed elements:
 * {{{
 * elems forall (e => e.rootElem == indexedRootElem.rootElem)
 *
 * (elems map (_.elemPath)) == elemPaths
 *
 * elems forall { e => e.rootElem.findWithElemPath(e.elemPath) == Some(e.elem) }
 * }}}
 *
 * Analogous remarks apply to the other query methods. For example, given:
 * {{{
 * // Let p be a predicate of type (yaidom.Elem => Boolean)
 *
 * val elems = indexedRootElem filterElems { e => p(e.elem) }
 * val elemPaths = indexedRootElem.elem filterElemPaths p
 * }}}
 * we have:
 * {{{
 * elems forall (e => e.rootElem == indexedRootElem.rootElem)
 *
 * (elems map (_.elemPath)) == elemPaths
 *
 * elems forall { e => e.rootElem.findWithElemPath(e.elemPath) == Some(e.elem) }
 * }}}
 *
 * @author Chris de Vreeze
 */
final class Elem private[indexed] (
  val rootElem: eu.cdevreeze.yaidom.Elem,
  childElems: immutable.IndexedSeq[Elem],
  val elemPath: ElemPath) extends ElemLike[Elem] with HasText with Immutable {

  /**
   * The yaidom Elem itself, stored as a val
   */
  val elem: eu.cdevreeze.yaidom.Elem =
    rootElem.findWithElemPath(elemPath).getOrElse(sys.error("Path %s must exist".format(elemPath)))

  require(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the element paths, which have one more
   * "path entry".
   */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = childElems

  override def resolvedName: EName = elem.resolvedName

  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem => (other.rootElem == this.rootElem) && (other.elemPath == this.elemPath)
    case _ => false
  }

  override def hashCode: Int = (rootElem, elemPath).hashCode

  /**
   * Returns `this.elem.scope`
   */
  final def scope: Scope = this.elem.scope

  /**
   * Returns the namespaces declared in this element.
   *
   * If the original parsed XML document contained duplicate namespace declarations (i.e. namespace declarations that are the same
   * as some namespace declarations in their context), these duplicate namespace declarations were lost during parsing of the
   * XML into an `Elem` tree. They therefore do not occur in the namespace declarations returned by this method.
   */
  final def namespaces: Declarations = {
    val parentScope = this.elemPath.parentPathOption map { path => rootElem.getWithElemPath(path).scope } getOrElse (Scope.Empty)
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
}

object Elem {

  /**
   * Calls `apply(rootElem, ElemPath.Root)`
   */
  def apply(rootElem: eu.cdevreeze.yaidom.Elem): Elem = {
    apply(rootElem, ElemPath.Root)
  }

  /**
   * Expensive recursive factory method for "indexed elements".
   */
  def apply(rootElem: eu.cdevreeze.yaidom.Elem, elemPath: ElemPath): Elem = {
    val elem = rootElem.getWithElemPath(elemPath)

    // Recursive calls
    val childElems = elem.findAllChildElemPathEntries.map(entry => apply(rootElem, elemPath.append(entry)))

    new Elem(rootElem, childElems, elemPath)
  }
}
