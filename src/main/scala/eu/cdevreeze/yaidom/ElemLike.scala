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
 * It could in principle also be used for implementing parts of other "element-like" classes, other than [[eu.cdevreeze.yaidom.Elem]].
 *
 * The only abstract methods are `resolvedName`, `resolvedAttributes` and `allChildElems`.
 * Based on these methods alone, this trait offers a rich API for querying elements and attributes.
 *
 * This trait only knows about elements, not about nodes in general. Hence this trait has no knowledge about child nodes in
 * general. Hence the single type parameter, for the captured element type itself.
 *
 * Trait `ElemLike` has many methods for retrieving elements, but they are pretty easy to remember. First of all, an `ElemLike`
 * has 3 '''core''' element collection retrieval methods. These 3 methods retrieve the following collections of elements, respectively:
 * <ul>
 * <li>Child elements</li>
 * <li>Descendant elements</li>
 * <li>Descendant elements or self</li>
 * </ul>
 * Obviously, the set of child elements is a subset of the set of descendant elements, and the set of descendant elements is a
 * proper subset of the set of "descendant elements or self". Many element retrieval methods have counterparts for all core
 * element collection retrieval methods, filtering the results.
 *
 * Below follows a summary of the groups of `ElemLike` element collection retrieval methods:
 * <ul>
 * <li>'''Core''' (see above): `allChildElems`, `allElems` and `allElemsOrSelf`</li>
 * <li>'''Obeying some predicate''': `childElemsWhere`, `elemsWhere` and `elemsOrSelfWhere`</li>
 * <li>'''Having some ExpandedName''' (special case of the former): `childElems`, `elems` and `elemsOrSelf`</li>
 * <li>'''Collecting data''': `collectFromChildElems`, `collectFromElems` and `collectFromElemsOrSelf`</li>
 * <li>'''Topmost obeying some predicate''' (not for child elements): `topmostElemsWhere` and `topmostElemsOrSelfWhere`</li>
 * <li>'''Topmost having some ExpandedName''' (special case of the former; not for child elements): `topmostElems` and `topmostElemsOrSelf`</li>
 * </ul>
 *
 * Note that above the only abstract method is `allChildElems` and the other methods are defined in terms of that method (and `resolvedName`).
 * Also note that "elems" stands for "descendant elements". The method names are quite predictable. The method names are derived
 * from the core retrieval method names, stripping prefix "all", and each of the above groups of (non-core) methods correspond to some prefix and/or suffix
 * added in the method names.
 *
 * Often it is appropriate to query for collections of elements, but sometimes it is appropriate to query for individual elements.
 * Therefore there are also some `ElemLike` methods returning at most one element. These methods are as follows:
 * <ul>
 * <li>'''Obeying some predicate''' (only for child elements): `singleChildElemOptionWhere` and `singleChildElemWhere`</li>
 * <li>'''Having some ExpandedName''' (special case of the former; only for child elements): `singleChildElemOption` and `singleChildElem`</li>
 * <li>'''First (depth-first) obeying some predicate''' (not for child elements): `firstElemOptionWhere` and `firstElemOrSelfOptionWhere`</li>
 * <li>'''First (depth-first) having some ExpandedName''' (special case of the former; not for child elements): `firstElemOption` and `firstElemOrSelfOption`</li>
 * </ul>
 *
 * These element (collection) retrieval methods process and return elements in the following (depth-first) order:
 * <ol>
 * <li>Parents are processed before their children</li>
 * <li>Children are processed before the next sibling</li>
 * <li>The first child element is processed before the next child element, and so on</li>
 * </ol>
 * assuming that the no-arg `allChildElems` method returns the child elements in the correct order.
 * Hence, the methods taking a predicate invoke that predicate on the elements in a predictable order.
 * Per visited element, the predicate is invoked only once. These properties are especially important
 * if the predicate has side-effects, which typically should not be the case.
 *
 * There are some obvious equalities, such as:
 * {{{
 * e.childElemsWhere(p) == e.allChildElems.filter(p)
 * e.elemsWhere(p) == e.allElems.filter(p)
 * e.elemsOrSelfWhere(p) == e.allElemsOrSelf.filter(p)
 *
 * e.collectFromChildElems(pf) == e.allChildElems.collect(pf)
 * e.collectFromElems(pf) == e.allElems.collect(pf)
 * e.collectFromElemsOrSelf(pf) == e.allElemsOrSelf.collect(pf)
 * }}}
 * Moreover, each method taking an ExpandedName trivially corresponds to a call to a method taking a predicate. For example:
 * {{{
 * e.elemsOrSelf(ename) == (e elemsOrSelfWhere (_.resolvedName == ename))
 * }}}
 * Finally, the methods returning at most one element trivially correspond to expressions containing calls to element collection
 * retrieval methods. For example:
 * {{{
 * e.firstElemOrSelfOptionWhere(p) == e.elemsOrSelfWhere(p).headOption
 * e.firstElemOrSelfOptionWhere(p) == e.topmostElemsOrSelfWhere(p).headOption
 * }}}
 *
 * Besides element (collection) retrieval methods, there are also attribute retrieval methods, methods for indexing the element tree and
 * finding subtrees, and methods dealing with `ElemPath`s.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ElemLike[E <: ElemLike[E]] { self: E =>

  /** Resolved name of the element, as `ExpandedName` */
  def resolvedName: ExpandedName

  /** The attributes as a `Map` from `ExpandedName`s (instead of `QName`s) to values */
  def resolvedAttributes: Map[ExpandedName, String]

  /** Returns all child elements, in the correct order. The faster this method is, the faster the other `ElemLike` methods will be. */
  def allChildElems: immutable.IndexedSeq[E]

  /** Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option` */
  final def attributeOption(expandedName: ExpandedName): Option[String] = resolvedAttributes.get(expandedName)

  /** Returns the value of the attribute with the given expanded name, and throws an exception otherwise */
  final def attribute(expandedName: ExpandedName): String = attributeOption(expandedName).getOrElse(sys.error("Missing attribute %s".format(expandedName)))

  /** Returns the child elements obeying the given predicate */
  final def childElemsWhere(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems filter p

  /** Returns the child elements with the given expanded name */
  final def childElems(expandedName: ExpandedName): immutable.IndexedSeq[E] = childElemsWhere { e => e.resolvedName == expandedName }

  /** Returns `allChildElems collect pf` */
  final def collectFromChildElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] = allChildElems collect pf

  /** Returns the single child element obeying the given predicate, if any, wrapped in an `Option` */
  final def singleChildElemOptionWhere(p: E => Boolean): Option[E] = {
    val result = childElemsWhere(p)
    require(result.size <= 1, "Expected at most 1 matching child element, but found %d of them".format(result.size))
    result.headOption
  }

  /** Returns the single child element obeying the given predicate, and throws an exception otherwise */
  final def singleChildElemWhere(p: E => Boolean): E = {
    val result = childElemsWhere(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  /** Returns the single child element with the given expanded name, if any, wrapped in an `Option` */
  final def singleChildElemOption(expandedName: ExpandedName): Option[E] = {
    val result = childElems(expandedName)
    require(result.size <= 1, "Expected at most 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.headOption
  }

  /** Returns the single child element with the given expanded name, and throws an exception otherwise */
  final def singleChildElem(expandedName: ExpandedName): E = {
    val result = childElems(expandedName)
    require(result.size == 1, "Expected exactly 1 child element %s, but found %d of them".format(expandedName, result.size))
    result.head
  }

  /** Returns this element followed by all descendant elements (that is, the descendant-or-self elements) */
  final def allElemsOrSelf: immutable.IndexedSeq[E] = {
    var result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulateElemsOrSelf(elm: E) {
      result += elm
      elm.allChildElems foreach { e => accumulateElemsOrSelf(e) }
    }

    accumulateElemsOrSelf(self)
    result.toIndexedSeq
  }

  /**
   * Returns the descendant-or-self elements that obey the given predicate.
   * That is, the result is equivalent to `allElemsOrSelf filter p`.
   */
  final def elemsOrSelfWhere(p: E => Boolean): immutable.IndexedSeq[E] = {
    var result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulateMatchingElemsOrSelf(elm: E) {
      if (p(elm)) result += elm
      elm.allChildElems foreach { e => accumulateMatchingElemsOrSelf(e) }
    }

    accumulateMatchingElemsOrSelf(self)
    result.toIndexedSeq
  }

  /** Returns the descendant-or-self elements that have the given expanded name */
  final def elemsOrSelf(expandedName: ExpandedName): immutable.IndexedSeq[E] = elemsOrSelfWhere { e => e.resolvedName == expandedName }

  /** Returns (the equivalent of) `allElemsOrSelf collect pf` */
  final def collectFromElemsOrSelf[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    elemsOrSelfWhere { e => pf.isDefinedAt(e) } collect pf

  /** Returns all descendant elements (not including this element). Same as `allElemsOrSelf.drop(1)` */
  final def allElems: immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch.allElemsOrSelf }

  /** Returns the descendant elements obeying the given predicate, that is, `allElems filter p` */
  final def elemsWhere(p: E => Boolean): immutable.IndexedSeq[E] = allChildElems flatMap { ch => ch elemsOrSelfWhere p }

  /** Returns the descendant elements with the given expanded name */
  final def elems(expandedName: ExpandedName): immutable.IndexedSeq[E] = elemsWhere { e => e.resolvedName == expandedName }

  /** Returns (the equivalent of) `allElems collect pf` */
  final def collectFromElems[B](pf: PartialFunction[E, B]): immutable.IndexedSeq[B] =
    elemsWhere { e => pf.isDefinedAt(e) } collect pf

  /**
   * Returns the descendant-or-self elements that obey the given predicate, such that no ancestor obeys the predicate.
   */
  final def topmostElemsOrSelfWhere(p: E => Boolean): immutable.IndexedSeq[E] = {
    var result = mutable.ArrayBuffer[E]()

    // Not tail-recursive, but the depth should typically be limited
    def accumulateMatchingTopmostElemsOrSelf(elm: E) {
      if (p(elm)) result += elm else {
        elm.allChildElems foreach { e => accumulateMatchingTopmostElemsOrSelf(e) }
      }
    }

    accumulateMatchingTopmostElemsOrSelf(self)
    result.toIndexedSeq
  }

  /** Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate */
  final def topmostElemsWhere(p: E => Boolean): immutable.IndexedSeq[E] =
    allChildElems flatMap { ch => ch topmostElemsOrSelfWhere p }

  /** Returns the descendant-or-self elements with the given expanded name that have no ancestor with the same name */
  final def topmostElemsOrSelf(expandedName: ExpandedName): immutable.IndexedSeq[E] =
    topmostElemsOrSelfWhere { e => e.resolvedName == expandedName }

  /** Returns the descendant elements with the given expanded name that have no ancestor with the same name */
  final def topmostElems(expandedName: ExpandedName): immutable.IndexedSeq[E] =
    topmostElemsWhere { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option` */
  final def firstElemOrSelfOptionWhere(p: E => Boolean): Option[E] = {
    // Not tail-recursive, but the depth should typically be limited
    def findMatchingFirstElemOrSelf(elm: E): Option[E] = {
      if (p(elm)) Some(elm) else {
        val childElms = elm.allChildElems

        var i = 0
        var result: Option[E] = None

        while ((result.isEmpty) && (i < childElms.size)) {
          result = findMatchingFirstElemOrSelf(childElms(i))
          i += 1
        }

        result
      }
    }

    findMatchingFirstElemOrSelf(self)
  }

  /** Returns the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option` */
  final def firstElemOptionWhere(p: E => Boolean): Option[E] = {
    self.allChildElems.view flatMap { ch => ch firstElemOrSelfOptionWhere p } headOption
  }

  /** Returns the first found (topmost) descendant-or-self element with the given expanded name, if any, wrapped in an `Option` */
  final def firstElemOrSelfOption(expandedName: ExpandedName): Option[E] =
    firstElemOrSelfOptionWhere { e => e.resolvedName == expandedName }

  /** Returns the first found (topmost) descendant element with the given expanded name, if any, wrapped in an `Option` */
  final def firstElemOption(expandedName: ExpandedName): Option[E] =
    firstElemOptionWhere { e => e.resolvedName == expandedName }

  /**
   * Finds the parent element, if any, searching in the tree with the given root element.
   * The implementation uses the `equals` method on the self type, and uses no index. Typically rather expensive.
   */
  final def findParentInTree(root: E): Option[E] = {
    root firstElemOrSelfOptionWhere { e => e.allChildElems exists { ch => ch == self } }
  }

  /** Computes an index on the given function taking an element, for example a function returning some unique element "identifier" */
  final def getIndex[K](f: E => K): Map[K, immutable.IndexedSeq[E]] = allElemsOrSelf groupBy f

  /** Computes an index to parent elements, on the given function applied to the child elements */
  final def getIndexToParent[K](f: E => K): Map[K, immutable.IndexedSeq[E]] = {
    val parentChildPairs = allElemsOrSelf flatMap { e => e.allChildElems map { ch => (e -> ch) } }
    parentChildPairs groupBy { pair => f(pair._2) } mapValues { pairs => pairs map { _._1 } } mapValues { _.distinct }
  }

  /**
   * Returns the equivalent of `findWithElemPath(ElemPath(immutable.IndexedSeq(entry)))`, but it should be more efficient.
   */
  final def findWithElemPathEntry(entry: ElemPath.Entry): Option[E] = {
    val relevantChildElms = self.childElems(entry.elementName)

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
    // This implementation is O(n), where n is the number of children, and uses mutable collections for speed

    val elementNameCounts = mutable.Map[ExpandedName, Int]()
    val acc = mutable.ArrayBuffer[ElemPath.Entry]()

    for (elm <- self.allChildElems) {
      val countForName = elementNameCounts.getOrElse(elm.resolvedName, 0)
      val entry = ElemPath.Entry(elm.resolvedName, countForName)
      elementNameCounts.update(elm.resolvedName, countForName + 1)
      acc += entry
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
    val idx = parent.childElems(self.resolvedName) indexWhere { e => e == self }
    require(idx >= 0, "Expected %s to have parent %s".format(self.toString, parent.toString))
    ElemPath.Entry(self.resolvedName, idx)
  }
}
