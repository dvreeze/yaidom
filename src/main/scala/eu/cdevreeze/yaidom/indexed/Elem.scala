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
 * ==Elem more formally==
 *
 * The following (rather obvious) properties hold for indexed elements:
 * {{{
 * val elems = indexedRootElem.findAllElemsOrSelf
 * val elemPaths = indexedRooElem.elem.findAllElemOrSelfPaths
 * (elems map (_.elemPath)) == elemPaths
 *
 * elems forall { e => indexedRootElem.elem.findWithElemPath(e.elemPath) == Some(e.elem) }
 * }}}
 *
 * Analogous remarks apply to the other query methods. For example:
 * {{{
 * val elemsContainingPlus = indexedRootElem filterElems { e => e.attributeOption(EName("name")).getOrElse("").contains("Plus") }
 * val pathsOfElemsContainingPlus = indexedRootElem.elem filterElemPaths { e => e.attributeOption(EName("name")).getOrElse("").contains("Plus") }
 * (elemsContainingPlus map (_.elemPath)) == pathsOfElemsContainingPlus
 *
 * elemsContainingPlus forall { e => indexedRootElem.elem.findWithElemPath(e.elemPath) == Some(e.elem) }
 * }}}
 *
 * @author Chris de Vreeze
 */
final class Elem(
  val rootElem: eu.cdevreeze.yaidom.Elem,
  val elemPath: ElemPath) extends ElemLike[Elem] with HasText with Immutable {

  def elem: eu.cdevreeze.yaidom.Elem =
    rootElem.findWithElemPath(elemPath).getOrElse(sys.error("Path %s must exist".format(elemPath)))

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
  def parentOption: Option[Elem] =
    this.elemPath.parentPathOption map { path => Elem(this.rootElem, path) }

  /**
   * Returns `Elem(this.rootElem, this.elemPath.parentPath)`
   */
  def parent: Elem = Elem(this.rootElem, this.elemPath.parentPath)

  /**
   * Returns `this.elemPath.ancestorOrSelfPaths map { path => Elem(this.rootElem, path) }`
   */
  def ancestorsOrSelf: immutable.IndexedSeq[Elem] =
    this.elemPath.ancestorOrSelfPaths map { path => Elem(this.rootElem, path) }

  /**
   * Returns `this.elemPath.ancestorPaths map { path => Elem(this.rootElem, path) }`
   */
  def ancestors: immutable.IndexedSeq[Elem] =
    this.elemPath.ancestorPaths map { path => Elem(this.rootElem, path) }

  /**
   * Returns `ancestorsOrSelf find { e => p(e) }`
   */
  def findAncestorOrSelf(p: Elem => Boolean): Option[Elem] =
    ancestorsOrSelf find { e => p(e) }

  /**
   * Returns `ancestors find { e => p(e) }`
   */
  def findAncestor(p: Elem => Boolean): Option[Elem] =
    ancestors find { e => p(e) }
}

object Elem {

  def apply(rootElem: eu.cdevreeze.yaidom.Elem, elemPath: ElemPath): Elem = {
    new Elem(rootElem, elemPath)
  }

  def apply(rootElem: eu.cdevreeze.yaidom.Elem): Elem = {
    apply(rootElem, ElemPath.Root)
  }
}
