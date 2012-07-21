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
 * Implementation trait for elements as containers of elements. This trait implements the corresponding `Elem` methods.
 * It is also used for implementing parts of other "element-like" classes, other than [[eu.cdevreeze.yaidom.Elem]], such as
 * [[eu.cdevreeze.yaidom.resolved.Elem]].
 *
 * Example usage (where the `text` method is not offered by the `ElemLike` API itself):
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
 * ==ElemLike more formally==
 *
 * The only abstract methods are `resolvedName`, `resolvedAttributes` and `allChildElems`.
 * Based on these methods alone, this trait offers a rich API for querying elements and attributes.
 *
 * This trait only knows about elements, not about nodes in general. Hence this trait has no knowledge about child nodes in
 * general. Hence the single type parameter, for the captured element type itself.
 *
 * Trait `ElemLike` has many methods for retrieving elements, but they are pretty easy to remember. First of all, an `ElemLike`
 * has 3 '''core''' element collection retrieval methods. These 3 methods (in order of subset relation) are:
 * <ul>
 * <li>Abstract method `allChildElems`, returning all '''child''' elements</li>
 * <li>Method `findAllElems`, finding all '''descendant''' elements</li>
 * <li>Method `findAllElemsOrSelf`, finding all '''descendant''' elements '''or self'''</li>
 * </ul>
 * The latter 2 methods are implemented in terms of method `allChildElems`. The following equalities define their semantics more formally:
 * {{{
 * elm.findAllElems == (elm.allChildElems flatMap (_.findAllElemsOrSelf))
 *
 * elm.findAllElemsOrSelf == {
 *   immutable.IndexedSeq(elm) ++ (elm.allChildElems flatMap (_.findAllElemsOrSelf))
 * }
 * }}}
 * Strictly speaking, these '''core''' element collection retrieval methods, in combination with Scala's Collections API, should in theory
 * be enough for all element collection needs. For conciseness (and performance), there are more element (collection) retrieval methods.
 *
 * Below follows a summary of those groups of `ElemLike` element collection retrieval methods:
 * <ul>
 * <li>'''Filtering''': `filterChildElems`, `filterElems` and `filterElemsOrSelf`</li>
 * <li>'''Collecting data''': `collectFromChildElems`, `collectFromElems` and `collectFromElemsOrSelf`</li>
 * <li>'''Finding topmost obeying some predicate''' (not for child elements): `findTopmostElems` and `findTopmostElemsOrSelf`</li>
 * </ul>
 *
 * Often it is appropriate to query for collections of elements, but sometimes it is appropriate to query for individual elements.
 * Therefore there are also some `ElemLike` methods returning at most one element. These methods are as follows:
 * <ul>
 * <li>'''Finding first obeying some predicate''' (depth-first search): `findChildElem` and `getChildElem`, `findElem` and `findElemOrSelf`</li>
 * </ul>
 *
 * These element (collection) retrieval methods process and return elements in depth-first order
 * (see http://en.wikipedia.org/wiki/Depth-first_search).
 *
 * Assuming no side-effects, some equalities defining the semantics of the left-hand-side are:
 * {{{
 * e.filterChildElems(p) == e.allChildElems.filter(p)
 * e.filterElems(p) == e.findAllElems.filter(p)
 * e.filterElemsOrSelf(p) == e.findAllElemsOrSelf.filter(p)
 *
 * e.collectFromChildElems(pf) == e.allChildElems.collect(pf)
 * e.collectFromElems(pf) == e.findAllElems.collect(pf)
 * e.collectFromElemsOrSelf(pf) == e.findAllElemsOrSelf.collect(pf)
 *
 * elm.findTopmostElems(p) == {
 *   elm.filterElems(p) filter { e =>
 *     val hasNoMatchingAncestor = elm.filterElems(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 *
 * elm.findTopmostElemsOrSelf(p) == {
 *   elm.filterElemsOrSelf(p) filter { e =>
 *     val hasNoMatchingAncestor = elm.filterElemsOrSelf(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 * }}}
 * The latter put differently:
 * {{{
 * (elm.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p))) == (elm.filterElems(p))
 * (elm.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) == (elm.filterElemsOrSelf(p))
 * }}}
 *
 * The equalities above give semantics to the left-hand sides, but do not necessarily suggest how they are implemented.
 * Assuming no side-effects, the following (provable) equalities hint at possible implementations of the left-hand-sides, although
 * the real implementations may be (far) more efficient:
 * {{{
 * elm.filterElems(p) == (elm.allChildElems flatMap (_.filterElemsOrSelf(p)))
 *
 * elm.filterElemsOrSelf(p) == {
 *   (immutable.IndexedSeq(elm).filter(p)) ++ (elm.allChildElems flatMap (_.filterElemsOrSelf(p)))
 * }
 *
 * elm.findTopmostElems(p) == (elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p)))
 *
 * elm.findTopmostElemsOrSelf(p) == {
 *   if (p(elm))
 *     immutable.IndexedSeq(elm)
 *   else
 *     (elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p)))
 * }
 * }}}
 *
 * Each method taking an EName trivially corresponds to a call to a method taking a predicate. For example:
 * {{{
 * e.filterElemsOrSelf(ename) == (e filterElemsOrSelf (_.resolvedName == ename))
 * }}}
 * Finally, the methods returning at most one element trivially correspond to expressions containing calls to element collection
 * retrieval methods. For example (in the absence of side-effects) the following holds:
 * {{{
 * e.findElemOrSelf(p) == e.filterElemsOrSelf(p).headOption
 * e.findElemOrSelf(p) == e.findTopmostElemsOrSelf(p).headOption
 * }}}
 *
 * Besides element (collection) retrieval methods, there are also attribute retrieval methods, methods dealing with `ElemPath`s, etc.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ElemLike[E <: ElemLike[E]] { self: E =>

  /** Resolved name of the element, as `EName` */
  def resolvedName: EName

  /** The attributes as a `Map` from `EName`s (instead of `QName`s) to values */
  def resolvedAttributes: Map[EName, String]

  /** Returns all child elements, in the correct order. The faster this method is, the faster the other `ElemLike` methods will be. */
  def allChildElems: immutable.IndexedSeq[E]

  /** The local name (or local part). Convenience method. */
  final def localName: String = resolvedName.localPart

  /** Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option` */
  final def attributeOption(expandedName: EName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  final def attribute(expandedName: EName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements obeying the given predicate */
  final def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems filter p

  /** Shorthand for `filterChildElems(p)`. Use this shorthand only if the predicate is a short expression. */
  final def \(p: E => Boolean): immutable.IndexedSeq[E] = filterChildElems(p)

  /** Returns the child elements with the given expanded name */
  final def filterChildElems(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems { e => e.resolvedName == expandedName }

  /** Shorthand for `filterChildElems(expandedName)`. */
  final def \(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems(expandedName)

  /** Shorthand for `filterChildElems { _.localName == localName }`. */
  final def \(localName: String): immutable.IndexedSeq[E] = filterChildElems { _.localName == localName }

  /** Returns `allChildElems collect pf` */
  final def collectFromChildElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] = allChildElems collect pf

  /** Returns the first found child element obeying the given predicate, if any, wrapped in an `Option` */
  final def findChildElem(p: E => Boolean): Option[E] = {
    val result = filterChildElems(p)
    result.headOption
  }

  /** Returns the first found child element with the given expanded name, if any, wrapped in an `Option` */
  final def findChildElem(expandedName: EName): Option[E] = {
    findChildElem { e => e.resolvedName == expandedName }
  }

  /** Returns the single child element obeying the given predicate, and throws an exception otherwise */
  final def getChildElem(p: E => Boolean): E = {
    val result = filterChildElems(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  final def getChildElem(expandedName: EName): E = {
    val result = filterChildElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns this element followed by all descendant elements (that is, the descendant-or-self elements) */
  final def findAllElemsOrSelf: immutable.IndexedSeq[E] = {
    var result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: E) {
      result += elm
      elm.allChildElems foreach { e => accumulate(e) }
    }

    accumulate(self)
    result.toIndexedSeq
  }

  /**
   * Returns the descendant-or-self elements that obey the given predicate.
   * That is, the result is equivalent to `findAllElemsOrSelf filter p`.
   */
  final def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] = {
    var result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: E) {
      if (p(elm)) result += elm
      elm.allChildElems foreach { e => accumulate(e) }
    }

    accumulate(self)
    result.toIndexedSeq
  }

  /** Shorthand for `filterElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  final def \\(p: E => Boolean): immutable.IndexedSeq[E] = filterElemsOrSelf(p)

  /** Returns the descendant-or-self elements that have the given expanded name */
  final def filterElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf { e => e.resolvedName == expandedName }

  /** Shorthand for `filterElemsOrSelf(expandedName)`. */
  final def \\(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf(expandedName)

  /** Shorthand for `filterElemsOrSelf { _.localName == localName }`. */
  final def \\(localName: String): immutable.IndexedSeq[E] = filterElemsOrSelf { _.localName == localName }

  /** Returns (the equivalent of) `findAllElemsOrSelf collect pf` */
  final def collectFromElemsOrSelf[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    filterElemsOrSelf { e => pf.isDefinedAt(e) } collect pf

  /** Returns all descendant elements (not including this element). Equivalent to `findAllElemsOrSelf.drop(1)` */
  final def findAllElems: immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch.findAllElemsOrSelf }

  /** Returns the descendant elements obeying the given predicate, that is, `findAllElems filter p` */
  final def filterElems(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch filterElemsOrSelf p }

  /** Returns the descendant elements with the given expanded name */
  final def filterElems(expandedName: EName): immutable.IndexedSeq[E] = filterElems { e => e.resolvedName == expandedName }

  /** Returns (the equivalent of) `findAllElems collect pf` */
  final def collectFromElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    filterElems { e => pf.isDefinedAt(e) } collect pf

  /**
   * Returns the descendant-or-self elements that obey the given predicate, such that no ancestor obeys the predicate.
   */
  final def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] = {
    var result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulate(elm: E) {
      if (p(elm)) result += elm else {
        elm.allChildElems foreach { e => accumulate(e) }
      }
    }

    accumulate(self)
    result.toIndexedSeq
  }

  /** Shorthand for `findTopmostElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  final def \\!(p: E => Boolean): immutable.IndexedSeq[E] = findTopmostElemsOrSelf(p)

  /** Returns the descendant-or-self elements with the given expanded name that have no ancestor with the same name */
  final def findTopmostElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElemsOrSelf { e => e.resolvedName == expandedName }

  /** Shorthand for `findTopmostElemsOrSelf(expandedName)`. */
  final def \\!(expandedName: EName): immutable.IndexedSeq[E] = findTopmostElemsOrSelf(expandedName)

  /** Shorthand for `findTopmostElemsOrSelf { _.localName == localName }`. */
  final def \\!(localName: String): immutable.IndexedSeq[E] = findTopmostElemsOrSelf { _.localName == localName }

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def findTopmostElems(p: E => Boolean): immutable.IndexedSeq[E] =
    allChildElems flatMap { ch => ch findTopmostElemsOrSelf p }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def findTopmostElems(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElems { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option` */
  final def findElemOrSelf(p: E => Boolean): Option[E] = {
    // Not tail-recursive, but the depth should typically be limited
    def findMatch(elm: E): Option[E] = {
      if (p(elm)) Some(elm) else {
        val childElms = elm.allChildElems

        var i = 0
        var result: Option[E] = None

        while ((result.isEmpty) && (i < childElms.size)) {
          result = findMatch(childElms(i))
          i += 1
        }

        result
      }
    }

    findMatch(self)
  }

  /** Returns the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option` */
  final def findElem(p: E => Boolean): Option[E] = {
    val elms = self.allChildElems.view flatMap { ch => ch findElemOrSelf p }
    elms.headOption
  }

  /** Returns the first found (topmost) descendant-or-self element with the given expanded name, if any, wrapped in an `Option` */
  final def findElemOrSelf(expandedName: EName): Option[E] =
    findElemOrSelf { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant element with the given expanded name, if any, wrapped in an `Option` */
  final def findElem(expandedName: EName): Option[E] =
    findElem { e => e.resolvedName == expandedName }

  /** Computes an index on the given function taking an element, for example a function returning some unique element "identifier" */
  final def getIndex[K](f: E => K): Map[K, immutable.IndexedSeq[E]] = findAllElemsOrSelf groupBy f

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
  final def findWithElemPath(path: ElemPath): Option[E] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    def findWithElemPath(currentRoot: E, entryIndex: Int): Option[E] = {
      require(entryIndex >= 0 && entryIndex <= path.entries.size)

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
  final def allChildElemsWithPathEntries: immutable.IndexedSeq[(E, ElemPath.Entry)] = {
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

    acc.toIndexedSeq
  }

  /**
   * Returns the `ElemPath` `Entry` of this element with respect to the given parent,
   * throwing an exception if this element is not a child of that parent.
   *
   * The implementation uses the equals method on the self type.
   */
  final def ownElemPathEntry(parent: E): ElemPath.Entry = {
    val idx = parent.filterChildElems(self.resolvedName) indexWhere { e => e == self }
    require(idx >= 0, "Expected %s to have parent %s".format(self.toString, parent.toString))
    ElemPath.Entry(self.resolvedName, idx)
  }
}
