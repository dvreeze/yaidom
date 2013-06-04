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
 * API and implementation trait for elements as containers of elements, each having a name and possible attributes,
 * as well as an "element path" from the root element. This trait extends trait [[eu.cdevreeze.yaidom.ElemLike]], adding knowledge
 * about "element paths" of elements with respect to a root element.
 *
 * '''Most users of the yaidom API do not use this trait directly, so may skip the documentation of this trait.'''
 *
 * This trait to a large extent mirrors the `ParentElemLike` trait, with queries returning "element paths" instead of "elements".
 * As mentioned above, this trait knows more about elements than its supertraits, because it knows about "element paths". Still, it
 * knows only about element nodes, so other node types than elements are not known to this API.
 *
 * Based on its supertraits `ElemLike` and `ParentElemLike`, this trait offers a rich `ElemPath` query API. Two additional abstract methods
 * other than those required by the supertraits need to be implemented, viz. `findWithElemPathEntry` and `findAllChildElemsWithPathEntries`.
 *
 * This trait is extended by trait `UpdatableElemLike`, and therefore mixed in by [[eu.cdevreeze.yaidom.Elem]] and [[eu.cdevreeze.yaidom.resolved.Elem]].
 *
 * Example usage:
 * {{{
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val bookAuthorElms =
 *   for {
 *     authorPath <- bookstoreElm findTopmostElemPaths { _.localName == "Author" }
 *     if authorPath.containsName(EName("Book"))
 *   } yield bookstoreElm.getWithElemPath(authorPath)
 * }}}
 *
 * The above example shows how we can use the results of queries for `ElemPath`s, if we are interested in the ancestors of the
 * elements at those paths. Of course, using only the `ElemLike` API, this example could have been written simply as:
 * {{{
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val bookAuthorElms =
 *   for {
 *     bookElm <- bookstoreElm \\ EName("Book")
 *     authorElm <- bookElm findTopmostElems { _.localName == "Author" }
 *   } yield authorElm
 * }}}
 *
 * Indeed, the query methods of the `ParentElemLike` API (or `ElemLike` API) should often be preferred to those of this
 * `PathAwareElemLike` API. After all, `ElemPath`s are relative to one specific root element, they are "volatile" (in that
 * "functional updates" may render them useless), and they are rather slow indexes. Moreover, the `ParentElemLike` query methods
 * tend to be faster than those of this trait.
 *
 * On the other hand, it is often the combination of `ParentElemLike` API query methods and `PathAwareElemLike` API query methods
 * that offer interesting querying possibilities. After all, sometimes it is handy to formulate a query in such a way that ancestors
 * are retrieved in at least one of the intermediate steps.
 *
 * Another use for `ElemPath` queries is functional updates. See [[eu.cdevreeze.yaidom.UpdatableElemLike]] for the "update"
 * methods. Some `updated` methods take an `ElemPath`, and another `updated` method is implemented using an `ElemPath` query offered
 * by this API.
 *
 * Note that this API does not offer any query methods using a predicate on `ElemPath`s instead of on elements. Such queries
 * can always be rewritten to queries in which the Scala Collections API `filter` method is used for filtering on `ElemPath`s.
 *
 * ==PathAwareElemLike more formally==
 *
 * Analogously to the `ParentElemLike` API, there are 3 '''core''' element path retrieval methods:
 * <ul>
 * <li>Method `findAllChildElemPaths`, returning the paths (relative to this element) of all '''child''' elements</li>
 * <li>Method `findAllElemPaths`, finding the paths (relative to this element) of all '''descendant''' elements</li>
 * <li>Method `findAllElemOrSelfPaths`, finding the paths (relative to this element) of all '''descendant''' elements '''or self'''</li>
 * </ul>
 *
 * For example, instead of:
 * {{{
 * val titlePaths = elm filterElemOrSelfPaths { e => e.resolvedName == EName("Title") }
 * }}}
 * we could write (more verbosely):
 * {{{
 * val titlePaths = elm.findAllElemOrSelfPaths filter { path => elm.getWithElemPath(path).resolvedName == EName("Title") }
 * }}}
 * The second statement is far less efficient, due to repeated calls to method `getWithElemPath`. In this case, we could instead
 * have written:
 * {{{
 * val titlePaths = elm.findAllElemOrSelfPaths filter { path => path.endsWithName(EName("Title")) }
 * }}}
 *
 * Assuming correct implementations of the abstract methods, and using "resolved" (!) [[eu.cdevreeze.yaidom.resolved.Elem]] instances
 * (which have structural equality defined), this trait obeys some obvious properties, expressed using equality:
 * {{{
 * (elm.findAllElemOrSelfPaths map (path => elm.getWithElemPath(path))) == elm.findAllElemsOrSelf
 * (elm.findAllElemPaths map (path => elm.getWithElemPath(path))) == elm.findAllElems
 * (elm.findAllChildElemPaths map (path => elm.getWithElemPath(path))) == elm.findAllChildElems
 *
 * (elm.filterElemOrSelfPaths(p) map (path => elm.getWithElemPath(path))) == elm.filterElemsOrSelf(p)
 * (elm.filterElemPaths(p) map (path => elm.getWithElemPath(path))) == elm.filterElems(p)
 *
 * (elm.findTopmostElemOrSelfPaths(p) map (path => elm.getWithElemPath(path))) == elm.findTopmostElemsOrSelf(p)
 * (elm.findTopmostElemPaths(p) map (path => elm.getWithElemPath(path))) == elm.findTopmostElems(p)
 * }}}
 * etc.
 *
 * We offer no proofs of these properties.
 *
 * ==Implementation notes==
 *
 * Like trait `ParentElemLike`, some query methods use recursion in their implementations, but no tail recursion. See [[eu.cdevreeze.yaidom.ParentElemLike]]
 * for a motivation.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait PathAwareElemLike[E <: PathAwareElemLike[E]] extends ElemLike[E] with PathAwareElemApi[E] { self: E =>

  def findWithElemPathEntry(entry: ElemPath.Entry): Option[E]

  def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, ElemPath.Entry)]

  final def findAllChildElemPaths: immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries map { case (e, pe) => ElemPath.from(pe) }

  final def filterChildElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries filter { case (e, pe) => p(e) } map { case (e, pe) => ElemPath.from(pe) }

  final def findChildElemPath(p: E => Boolean): Option[ElemPath] = {
    findAllChildElemsWithPathEntries find { case (e, pe) => p(e) } map { case (e, pe) => ElemPath.from(pe) }
  }

  final def getChildElemPath(p: E => Boolean): ElemPath = {
    val result = filterChildElemPaths(p)
    require(result.size == 1, "Expected exactly 1 matching child element, but found %d of them".format(result.size))
    result.head
  }

  final def findAllElemOrSelfPaths: immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.findAllElemOrSelfPaths map { path => path.prepend(pe) }
    }

    ElemPath.Root +: remainder
  }

  final def filterElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    // Not tail-recursive, but the depth should typically be limited
    val remainder = findAllChildElemsWithPathEntries flatMap {
      case (e, pe) => e.filterElemOrSelfPaths(p) map { path => path.prepend(pe) }
    }

    if (p(self)) (ElemPath.Root +: remainder) else remainder
  }

  final def findAllElemPaths: immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findAllElemOrSelfPaths map { path => path.prepend(pe) } }

  final def filterElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.filterElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  final def findTopmostElemOrSelfPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] = {
    if (p(self)) immutable.IndexedSeq(ElemPath.Root) else {
      // Not tail-recursive, but the depth should typically be limited
      val result = findAllChildElemsWithPathEntries flatMap {
        case (e, pe) => e.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) }
      }
      result
    }
  }

  final def findTopmostElemPaths(p: E => Boolean): immutable.IndexedSeq[ElemPath] =
    findAllChildElemsWithPathEntries flatMap { case (ch, pe) => ch.findTopmostElemOrSelfPaths(p) map { path => path.prepend(pe) } }

  final def findElemOrSelfPath(p: E => Boolean): Option[ElemPath] = {
    // Not efficient
    filterElemOrSelfPaths(p).headOption
  }

  final def findElemPath(p: E => Boolean): Option[ElemPath] = {
    val elms = self.findAllChildElemsWithPathEntries.view flatMap { case (ch, pe) => ch.findElemOrSelfPath(p) map { path => path.prepend(pe) } }
    elms.headOption
  }

  /**
   * Finds the element with the given `ElemPath` (where this element is the root), if any, wrapped in an `Option`.
   * This method must be very efficient, which depends on the efficiency of method `findWithElemPathEntry`.
   */
  final def findWithElemPath(path: ElemPath): Option[E] = {
    // This implementation avoids "functional updates" on the path, and therefore unnecessary object creation

    val entryCount = path.entries.size

    def findWithElemPath(currentRoot: E, entryIndex: Int): Option[E] = {
      assert(entryIndex >= 0 && entryIndex <= entryCount)

      if (entryIndex == entryCount) Some(currentRoot) else {
        val newRootOption: Option[E] = currentRoot.findWithElemPathEntry(path.entries(entryIndex))
        // Recursive call. Not tail-recursive, but recursion depth should be limited.
        newRootOption flatMap { newRoot => findWithElemPath(newRoot, entryIndex + 1) }
      }
    }

    findWithElemPath(self, 0)
  }

  final def getWithElemPath(path: ElemPath): E =
    findWithElemPath(path).getOrElse(sys.error("Expected existing path %s from root %s".format(path, self)))

  final def findAllChildElemPathEntries: immutable.IndexedSeq[ElemPath.Entry] = {
    findAllChildElemsWithPathEntries map { _._2 }
  }
}
