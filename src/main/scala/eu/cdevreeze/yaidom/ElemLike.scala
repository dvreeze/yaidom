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

import scala.collection.{ immutable, mutable }

/**
 * API and implementation trait for elements as containers of elements. This trait extends trait [[eu.cdevreeze.yaidom.PathAwareElemLike]],
 * adding methods for querying attributes and elements with a given `EName`.
 *
 * '''Most users of the yaidom API do not use this trait directly, so may skip the documentation of this trait.'''
 *
 * Example usage (where the `text` method is not offered by the `ElemLike` API itself, but by trait `HasText` instead):
 * {{{
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val cheapBookElms =
 *   for {
 *     bookElm <- bookstoreElm \ "Book"
 *     price = bookElm.attribute(EName("Price"))
 *     if price.toInt < 90
 *   } yield bookElm
 *
 * val cheapBookAuthors = {
 *   val result =
 *     for {
 *       cheapBookElm <- cheapBookElms
 *       authorElm <- cheapBookElm \\ "Author"
 *     } yield {
 *       val firstNameElmOption = authorElm findChildElem { _.localName == "First_Name" }
 *       val lastNameElmOption = authorElm findChildElem { _.localName == "Last_Name" }
 *
 *       val firstName = firstNameElmOption.map(_.text).getOrElse("")
 *       val lastName = lastNameElmOption.map(_.text).getOrElse("")
 *       (firstName + " " + lastName).trim
 *     }
 *
 *   result.toSet
 * }
 * }}}
 *
 * Above, shorthand operator notation is used for querying child elements and descendant-or-self elements.
 * {{{
 * bookstoreElm \ "Book"
 * }}}
 * is equivalent to:
 * {{{
 * bookstoreElm filterChildElems { _.localName == "Book" }
 * }}}
 * Moreover:
 * {{{
 * cheapBookElm \\ "Author"
 * }}}
 * is equivalent to:
 * {{{
 * cheapBookElm filterElemsOrSelf { _.localName == "Author" }
 * }}}
 *
 * ==ElemLike methods==
 *
 * This trait adds the following abstract methods to the abstract methods required by its supertraits: `resolvedName` and `resolvedAttributes`.
 * Based on these abstract methods (and the supertraits), this trait offers a rich API for querying elements by expanded name, and for querying
 * attributes.
 *
 * This trait only knows about elements, not about nodes in general. Hence this trait has no knowledge about child nodes in
 * general. Hence the single type parameter, for the captured element type itself.
 *
 * The element query methods that need no knowledge about element names and attributes are implemented by supertrait
 * [[eu.cdevreeze.yaidom.ElemAwareElemLike]].
 *
 * This trait adds the following groups of methods to the methods offered by the supertraits `PathAwareElemLike` and `ElemAwareElemLike`:
 * <ul>
 * <li>Attribute query methods</li>
 * <li>Element query methods taking an `EName` or local name (which trivially correspond to method calls in the supertrait)</li>
 * <li>A method to get the local name of the element, without the namespace</li>
 * <li>Some query methods returning pairs of `ElemPath`s and elements</li>
 * </ul>
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ElemLike[E <: ElemLike[E]] extends PathAwareElemLike[E] { self: E =>

  /** Resolved name of the element, as `EName` */
  def resolvedName: EName

  /** The attributes as a `Map` from `EName`s (instead of `QName`s) to values */
  def resolvedAttributes: Map[EName, String]

  /** Returns all child elements, in the correct order. The faster this method is, the faster the other `ElemLike` methods will be. */
  override def allChildElems: immutable.IndexedSeq[E]

  /** The local name (or local part). Convenience method. */
  final def localName: String = resolvedName.localPart

  /** Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option` */
  final def attributeOption(expandedName: EName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  final def attribute(expandedName: EName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements with the given expanded name */
  final def filterChildElems(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems { e => e.resolvedName == expandedName }

  /** Shorthand for `filterChildElems(expandedName)`. */
  final def \(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems(expandedName)

  /** Shorthand for `filterChildElems { _.localName == localName }`. */
  final def \(localName: String): immutable.IndexedSeq[E] = filterChildElems { _.localName == localName }

  /** Returns the first found child element with the given expanded name, if any, wrapped in an `Option` */
  final def findChildElem(expandedName: EName): Option[E] = {
    findChildElem { e => e.resolvedName == expandedName }
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  final def getChildElem(expandedName: EName): E = {
    val result = filterChildElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns the descendant-or-self elements that have the given expanded name */
  final def filterElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf { e => e.resolvedName == expandedName }

  /** Shorthand for `filterElemsOrSelf(expandedName)`. */
  final def \\(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf(expandedName)

  /** Shorthand for `filterElemsOrSelf { _.localName == localName }`. */
  final def \\(localName: String): immutable.IndexedSeq[E] = filterElemsOrSelf { _.localName == localName }

  /** Returns the descendant elements with the given expanded name */
  final def filterElems(expandedName: EName): immutable.IndexedSeq[E] = filterElems { e => e.resolvedName == expandedName }

  /** Returns the descendant-or-self elements with the given expanded name that have no ancestor with the same name */
  final def findTopmostElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElemsOrSelf { e => e.resolvedName == expandedName }

  /** Shorthand for `findTopmostElemsOrSelf(expandedName)`. */
  final def \\!(expandedName: EName): immutable.IndexedSeq[E] = findTopmostElemsOrSelf(expandedName)

  /** Shorthand for `findTopmostElemsOrSelf { _.localName == localName }`. */
  final def \\!(localName: String): immutable.IndexedSeq[E] = findTopmostElemsOrSelf { _.localName == localName }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def findTopmostElems(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElems { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant-or-self element with the given expanded name, if any, wrapped in an `Option` */
  final def findElemOrSelf(expandedName: EName): Option[E] =
    findElemOrSelf { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant element with the given expanded name, if any, wrapped in an `Option` */
  final def findElem(expandedName: EName): Option[E] =
    findElem { e => e.resolvedName == expandedName }

  /**
   * Returns the equivalent of `findWithElemPath(ElemPath(immutable.IndexedSeq(entry)))`, but it should be more efficient.
   */
  final def findWithElemPathEntry(entry: ElemPath.Entry): Option[E] = {
    val relevantChildElms = self.filterChildElems(entry.elementName)

    if (entry.index >= relevantChildElms.size) None else Some(relevantChildElms(entry.index))
  }

  /**
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   */
  final override def findWithElemPath(path: ElemPath): Option[E] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    def findWithElemPath(currentRoot: E, entryIndex: Int): Option[E] = {
      assert(entryIndex >= 0 && entryIndex <= path.entries.size)

      if (entryIndex == path.entries.size) Some(currentRoot) else {
        val newRootOption: Option[E] = currentRoot.findWithElemPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot => findWithElemPath(newRoot, entryIndex + 1) }
      }
    }

    findWithElemPath(self, 0)
  }

  /** Returns the `ElemPath` entries of all child elements, in the correct order */
  final def allChildElemPathEntries: immutable.IndexedSeq[ElemPath.Entry] = {
    allChildElemsWithPathEntries map { elmPathPair => elmPathPair._2 }
  }

  /** Returns all child elements with their `ElemPath` entries, in the correct order */
  final override def allChildElemsWithPathEntries: immutable.IndexedSeq[(E, ElemPath.Entry)] = {
    // This implementation is O(n), where n is the number of children, and uses mutable collections for speed

    val elementNameCounts = mutable.Map[EName, Int]()
    val acc = mutable.ArrayBuffer[(E, ElemPath.Entry)]()

    for (elm <- self.allChildElems) {
      val ename = elm.resolvedName
      val countForName = elementNameCounts.getOrElse(ename, 0)
      val entry = ElemPath.Entry(ename, countForName)
      elementNameCounts.update(ename, countForName + 1)
      acc += (elm -> entry)
    }

    val result = acc.toIndexedSeq
    assert((result map (_._1)) == allChildElems)
    result
  }
}
