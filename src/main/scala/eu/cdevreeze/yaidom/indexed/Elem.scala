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
 *     bookElm <- authorElm findAncestor { _.resolvedName == EName("Book") }
 *   } yield bookElm
 * }}}
 *
 * ==Design considerations==
 *
 * Conceptually, indexed elements are owned by their root element. This could have been expressed in the type system, by
 * introducing class "IndexedElem", having a member type "IndexedElem.Elem". In such a design, the rootElem would live only
 * in type "IndexedElem", and would be absent in the "Indexed.Elem" member type. Such a design would make it difficult to
 * erroneously mix indexed elements from different indexed trees, each having different root elements. On the other hand, such a
 * design would (intentionally) make each "Elem" a member of a specific "IndexedElem" instance, which could be somewhat cumbersome
 * to use.
 *
 * The design of this `indexed.Elem` class does not make the "owner" of each indexed element as explicit as in the alternative
 * design described above. Still, the API user can only create top-level indexed elements, so in practice "ownership" should
 * be somewhat less of an issue.
 *
 * In a way, there are similarities between these indexed elements and JAXP DOM trees. After all, when querying a DOM element
 * for other DOM elements, each such returned element knows its owning document and therefore all of its descendants. This is also
 * true for indexed elements. The big difference, of course, is that yaidom indexed elements are "immutable snapshots" of
 * XML data within its context. If they come from queries on the same explicitly created top-level indexed element "snapshot",
 * then they belong to that same (top-level) "immutable snapshot".
 *
 * ==Elem more formally==
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
  val elemPath: ElemPath) extends ElemLike[Elem] with HasParent[Elem] with HasText with Immutable {

  def elem: eu.cdevreeze.yaidom.Elem =
    rootElem.findWithElemPath(elemPath).getOrElse(sys.error("Path %s must exist".format(elemPath)))

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the element paths, which have one more
   * "path entry".
   */
  override def allChildElems: immutable.IndexedSeq[Elem] = {
    // Remember: this function must be as fast as possible!
    val childElemPathEntries = elem.allChildElemPathEntries
    childElemPathEntries map { entry => new Elem(rootElem, elemPath.append(entry)) }
  }

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
    val parentScope = this.parentOption map { e => e.elem.scope } getOrElse (Scope.Empty)
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
   * Returns `this.elemPath.parentPathOption map { path => Elem(this.rootElem, path) }`
   */
  override def parentOption: Option[Elem] =
    this.elemPath.parentPathOption map { path => new Elem(this.rootElem, path) }
}

object Elem {

  /**
   * Calls `new Elem(rootElem, ElemPath.Root)`
   */
  def apply(rootElem: eu.cdevreeze.yaidom.Elem): Elem = {
    new Elem(rootElem, ElemPath.Root)
  }
}
