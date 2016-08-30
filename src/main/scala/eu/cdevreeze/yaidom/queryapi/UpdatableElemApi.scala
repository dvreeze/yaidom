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

package eu.cdevreeze.yaidom.queryapi

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path

/**
 * This is the <em>functional update</em> part of the yaidom <em>uniform query API</em>. It is a sub-trait of trait
 * [[eu.cdevreeze.yaidom.queryapi.IsNavigableApi]]. Only a few DOM-like element implementations in yaidom mix in this trait (indirectly,
 * because some implementing sub-trait is mixed in), thus sharing this query API.
 *
 * '''This trait typically does not show up in application code using yaidom, yet its (uniform) API does. Hence, it makes sense
 * to read the documentation of this trait, knowing that the API is offered by multiple element implementations.'''
 *
 * This trait is purely <em>abstract</em>. The most common implementation of this trait is [[eu.cdevreeze.yaidom.queryapi.UpdatableElemLike]].
 * The trait has all the knowledge of its super-trait, but in addition to that knows the following:
 * <ul>
 * <li>An element has <em>child nodes</em>, which may or may not be elements. Hence the extra type parameter for nodes.</li>
 * <li>An element knows the <em>child node indexes</em> of the path entries of the child elements.</li>
 * </ul>
 * Obviously methods ``children``, ``withChildren`` and ``collectChildNodeIndexes`` must be consistent with
 * methods such as ``findAllChildElems``.
 *
 * Using this minimal knowledge alone, trait ``UpdatableElemLike`` not only offers the methods of its parent trait, but also:
 * <ul>
 * <li>methods to <em>functionally update</em> an element by replacing, adding or deleting child nodes</li>
 * <li>methods to <em>functionally update</em> an element by replacing descendant-or-self elements at specified paths</li>
 * </ul>
 *
 * For the conceptual difference with "transformable" elements, see trait [[eu.cdevreeze.yaidom.queryapi.TransformableElemApi]].
 *
 * This query API leverages the Scala Collections API. Query results can be manipulated using the Collections API, and the
 * query API implementation (in ``UpdatableElemLike``) uses the Collections API internally.
 *
 * ==UpdatableElemApi examples==
 *
 * To illustrate the use of this API, consider the following example XML:
 * {{{
 * <book:Bookstore xmlns:book="http://bookstore/book" xmlns:auth="http://bookstore/author">
 *   <book:Book ISBN="978-0321356680" Price="35" Edition="2">
 *     <book:Title>Effective Java (2nd Edition)</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Joshua</auth:First_Name>
 *         <auth:Last_Name>Bloch</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 *   <book:Book ISBN="978-0981531649" Price="35" Edition="2">
 *     <book:Title>Programming in Scala: A Comprehensive Step-by-Step Guide, 2nd Edition</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Martin</auth:First_Name>
 *         <auth:Last_Name>Odersky</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Lex</auth:First_Name>
 *         <auth:Last_Name>Spoon</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Bill</auth:First_Name>
 *         <auth:Last_Name>Venners</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 * </book:Bookstore>
 * }}}
 *
 * Suppose this XML has been parsed into [[eu.cdevreeze.yaidom.simple.Elem]] variable named ``bookstoreElem``. Then we can add a book
 * as follows, where we "forget" the 2nd author for the moment:
 * {{{
 * import convert.ScalaXmlConversions._
 *
 * val bookstoreNamespace = "http://bookstore/book"
 * val authorNamespace = "http://bookstore/author"
 *
 * val fpBookXml =
 *   <book:Book xmlns:book="http://bookstore/book" xmlns:auth="http://bookstore/author" ISBN="978-1617290657" Price="33">
 *     <book:Title>Functional Programming in Scala</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Paul</auth:First_Name>
 *         <auth:Last_Name>Chiusano</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 * val fpBookElem = convertToElem(fpBookXml)
 *
 * bookstoreElem = bookstoreElem.plusChild(fpBookElem)
 * }}}
 * Note that the namespace declarations for prefixes ``book`` and ``auth`` had to be repeated in the Scala XML literal
 * for the added book, because otherwise the ``convertToElem`` method would throw an exception (since ``Elem`` instances
 * cannot be created unless all element and attribute QNames can be resolved as ENames).
 *
 * The resulting bookstore seems ok, but if we print ``convertElem(bookstoreElem)``, the result does not look pretty.
 * This can be fixed if the last assignment is replaced by:
 * {{{
 * bookstoreElem = bookstoreElem.plusChild(fpBookElem).prettify(2)
 * }}}
 * knowing that an indentation of 2 spaces has been used throughout the original XML. Method ``prettify`` is expensive, so it
 * is best not to invoke it within a tight loop. As an alternative, formatting can be left to the ``DocumentPrinter``, of
 * course.
 *
 * The assignment above is the same as the following one:
 * {{{
 * bookstoreElem = bookstoreElem.withChildren(bookstoreElem.children :+ fpBookElem).prettify(2)
 * }}}
 *
 * There are several methods to functionally update the children of an element. For example, method ``plusChild`` is overloaded,
 * and the other variant can insert a child at a given 0-based position. Other "children update" methods are ``minusChild``,
 * ``withPatchedChildren`` and ``withUpdatedChildren``.
 *
 * Let's now turn to functional update methods that take ``Path`` instances or collections thereof. In the example above
 * the second author of the added book is missing. Let's fix that:
 * {{{
 * val secondAuthorXml =
 *   <auth:Author xmlns:auth="http://bookstore/author">
 *     <auth:First_Name>Runar</auth:First_Name>
 *     <auth:Last_Name>Bjarnason</auth:Last_Name>
 *   </auth:Author>
 * val secondAuthorElem = convertToElem(secondAuthorXml)
 *
 * val fpBookAuthorsPaths =
 *   for {
 *     authorsPath <- indexed.Elem(bookstoreElem) filterElems { e => e.resolvedName == EName(bookstoreNamespace, "Authors") } map (_.path)
 *     if authorsPath.findAncestorPath(path => path.endsWithName(EName(bookstoreNamespace, "Book")) &&
 *       bookstoreElem.getElemOrSelfByPath(path).attribute(EName("ISBN")) == "978-1617290657").isDefined
 *   } yield authorsPath
 *
 * require(fpBookAuthorsPaths.size == 1)
 * val fpBookAuthorsPath = fpBookAuthorsPaths.head
 *
 * bookstoreElem = bookstoreElem.updateElemOrSelf(fpBookAuthorsPath) { elem =>
 *   require(elem.resolvedName == EName(bookstoreNamespace, "Authors"))
 *   val rawResult = elem.plusChild(secondAuthorElem)
 *   rawResult transformElemsOrSelf (e => e.copy(scope = elem.scope.withoutDefaultNamespace ++ e.scope))
 * }
 * bookstoreElem = bookstoreElem.prettify(2)
 * }}}
 *
 * Clearly the resulting bookstore element is nicely formatted, but there was another possible issue that was taken into
 * account. See the line of code transforming the "raw result". That line was added in order to prevent namespace undeclarations,
 * which for XML version 1.0 are not allowed (with the exception of the default namespace). After all, the XML for the second
 * author was created with only the ``auth`` namespace declared. Without the above-mentioned line of code, a namespace
 * undeclaration for prefix ``book`` would have occurred in the resulting XML, thus leading to an invalid XML 1.0 element tree.
 *
 * To illustrate functional update methods taking collections of paths, let's remove the added book from the book store.
 * Here is one (somewhat inefficient) way to do that:
 * {{{
 * val bookPaths = indexed.Elem(bookstoreElem) filterElems (_.resolvedName == EName(bookstoreNamespace, "Book")) map (_.path)
 *
 * bookstoreElem = bookstoreElem.updateElemsWithNodeSeq(bookPaths.toSet) { (elem, path) =>
 *   if ((elem \@ EName("ISBN")) == Some("978-1617290657")) Vector() else Vector(elem)
 * }
 * bookstoreElem = bookstoreElem.prettify(2)
 * }}}
 * There are very many ways to write this functional update, using different functional update methods in trait ``UpdatableElemApi``,
 * or even only using transformation methods in trait ``TransformableElemApi`` (thus not using paths).
 *
 * The example code above is enough to get started using the ``UpdatableElemApi`` methods, but it makes sense to study the
 * entire API, and practice with it. Always keep in mind that functional updates typically mess up formatting and/or namespace
 * (un)declarations, unless these aspects are taken into account.
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemApi extends AnyElemNodeApi with IsNavigableApi {

  type ThisElemApi <: UpdatableElemApi

  /** Returns the child nodes of this element, in the correct order */
  def children: immutable.IndexedSeq[ThisNode]

  /** Returns an element with the same name, attributes and scope as this element, but with the given child nodes */
  def withChildren(newChildren: immutable.IndexedSeq[ThisNode]): ThisElem

  /**
   * Filters the child elements with the given path entries, and returns a Map from the path entries of those filtered
   * elements to the child node indexes. The result Map has no entries for path entries that cannot be resolved.
   * This method should be fast, especially if the passed path entry set is small.
   */
  def collectChildNodeIndexes(pathEntries: Set[Path.Entry]): Map[Path.Entry, Int]

  /**
   * Finds the child node index of the given path entry, or -1 if not found. More precisely, returns:
   * {{{
   * collectChildNodeIndexes(Set(pathEntry)).getOrElse(pathEntry, -1)
   * }}}
   */
  def childNodeIndex(pathEntry: Path.Entry): Int

  /** Shorthand for `withChildren(newChildSeqs.flatten)` */
  def withChildSeqs(newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[ThisNode]]): ThisElem

  /** Shorthand for `withChildren(children.updated(index, newChild))` */
  def withUpdatedChildren(index: Int, newChild: ThisNode): ThisElem

  /** Shorthand for `withChildren(children.patch(from, newChildren, replace))` */
  def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[ThisNode], replace: Int): ThisElem

  /**
   * Returns a copy in which the given child has been inserted at the given position (0-based).
   * If `index == children.size`, adds the element at the end. If `index > children.size`, throws an exception.
   *
   * Afterwards, the resulting element indeed has the given child at position `index` (0-based).
   */
  def plusChild(index: Int, child: ThisNode): ThisElem

  /** Returns a copy in which the given child has been inserted at the end */
  def plusChild(child: ThisNode): ThisElem

  /**
   * Returns a copy in which the given child, if any, has been inserted at the given position (0-based).
   * That is, returns `plusChild(index, childOption.get)` if the given optional child element is non-empty.
   */
  def plusChildOption(index: Int, childOption: Option[ThisNode]): ThisElem

  /**
   * Returns a copy in which the given child, if any, has been inserted at the end.
   * That is, returns `plusChild(childOption.get)` if the given optional child element is non-empty.
   */
  def plusChildOption(childOption: Option[ThisNode]): ThisElem

  /** Returns a copy in which the given children have been inserted at the end */
  def plusChildren(childSeq: immutable.IndexedSeq[ThisNode]): ThisElem

  /**
   * Returns a copy in which the child at the given position (0-based) has been removed.
   * Throws an exception if `index >= children.size`.
   */
  def minusChild(index: Int): ThisElem

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.core.Path.Entry]] (compared to this element as root).
   *
   * It can be defined as follows:
   * {{{
   * updateChildElems(Set(pathEntry)) { case (che, pe) => f(che) }
   * }}}
   */
  def updateChildElem(pathEntry: Path.Entry)(f: ThisElem => ThisElem): ThisElem

  /** Returns `updateChildElem(pathEntry) { e => newElem }` */
  def updateChildElem(pathEntry: Path.Entry, newElem: ThisElem): ThisElem

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.core.Path.Entry]] (compared to this element as root).
   *
   * It can be defined as follows:
   * {{{
   * updateChildElemsWithNodeSeq(Set(pathEntry)) { case (che, pe) => f(che) }
   * }}}
   */
  def updateChildElemWithNodeSeq(pathEntry: Path.Entry)(f: ThisElem => immutable.IndexedSeq[ThisNode]): ThisElem

  /** Returns `updateChildElemWithNodeSeq(pathEntry) { e => newNodes }` */
  def updateChildElemWithNodeSeq(pathEntry: Path.Entry, newNodes: immutable.IndexedSeq[ThisNode]): ThisElem

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.core.Path]] (compared to this element as root).
   *
   * It can be defined as follows:
   * {{{
   * updateElemsOrSelf(Set(path)) { case (e, path) => f(e) }
   * }}}
   */
  def updateElemOrSelf(path: Path)(f: ThisElem => ThisElem): ThisElem

  /** Returns `updateElemOrSelf(path) { e => newElem }` */
  def updateElemOrSelf(path: Path, newElem: ThisElem): ThisElem

  /**
   * Functionally updates the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.core.Path]] (compared to this element as root). If the given path is the
   * root path, this element itself is returned unchanged.
   *
   * This function could be defined as follows:
   * {{{
   * updateElemsWithNodeSeq(Set(path)) { case (e, path) => f(e) }
   * }}}
   */
  def updateElemWithNodeSeq(path: Path)(f: ThisElem => immutable.IndexedSeq[ThisNode]): ThisElem

  /** Returns `updateElemWithNodeSeq(path) { e => newNodes }` */
  def updateElemWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[ThisNode]): ThisElem

  /**
   * Updates the child elements with the given path entries, applying the passed update function.
   *
   * That is, returns the equivalent of:
   * {{{
   * updateChildElemsWithNodeSeq(pathEntries) { case (che, pe) => Vector(f(che, pe)) }
   * }}}
   *
   * If the set of path entries is small, this method is rather efficient.
   */
  def updateChildElems(pathEntries: Set[Path.Entry])(f: (ThisElem, Path.Entry) => ThisElem): ThisElem

  /**
   * '''Updates the child elements with the given path entries''', applying the passed update function.
   * This is the '''core''' method of the update API, and the other methods have implementations that
   * directly or indirectly depend on this method.
   *
   * That is, returns:
   * {{{
   * if (pathEntries.isEmpty) self
   * else {
   *   val indexesByPathEntries: Seq[(Path.Entry, Int)] =
   *     collectChildNodeIndexes(pathEntries).toSeq.sortBy(_._2)
   *
   *   // Updating in reverse order of indexes, in order not to invalidate the path entries
   *   val newChildren = indexesByPathEntries.reverse.foldLeft(self.children) {
   *     case (accChildNodes, (pathEntry, idx)) =>
   *       val che = accChildNodes(idx).asInstanceOf[ThisElem]
   *       accChildNodes.patch(idx, f(che, pathEntry), 1)
   *   }
   *   self.withChildren(newChildren)
   * }
   * }}}
   *
   * If the set of path entries is small, this method is rather efficient.
   */
  def updateChildElemsWithNodeSeq(pathEntries: Set[Path.Entry])(f: (ThisElem, Path.Entry) => immutable.IndexedSeq[ThisNode]): ThisElem

  /**
   * Updates the descendant-or-self elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * val descendantUpdateResult =
   *   updateChildElems(pathsByFirstEntry.keySet) {
   *     case (che, pathEntry) =>
   *       // Recursive (but non-tail-recursive) call
   *       che.updateElemsOrSelf(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *         case (elm, path) =>
   *           f(elm, path.prepend(pathEntry))
   *       }
   *   }
   *
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else descendantUpdateResult
   * }}}
   *
   * In other words, returns:
   * {{{
   * val descendantUpdateResult = updateElems(paths)(f)
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else descendantUpdateResult
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElemsOrSelf(paths: Set[Path])(f: (ThisElem, Path) => ThisElem): ThisElem

  /**
   * Updates the descendant elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * updateChildElems(pathsByFirstEntry.keySet) {
   *   case (che, pathEntry) =>
   *     che.updateElemsOrSelf(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *       case (elm, path) =>
   *         f(elm, path.prepend(pathEntry))
   *     }
   * }
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElems(paths: Set[Path])(f: (ThisElem, Path) => ThisElem): ThisElem

  /**
   * Updates the descendant-or-self elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * val descendantUpdateResult =
   *   updateChildElemsWithNodeSeq(pathsByFirstEntry.keySet) {
   *     case (che, pathEntry) =>
   *       // Recursive (but non-tail-recursive) call
   *       che.updateElemsOrSelfWithNodeSeq(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *         case (elm, path) =>
   *           f(elm, path.prepend(pathEntry))
   *       }
   *   }
   *
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else Vector(descendantUpdateResult)
   * }}}
   *
   * In other words, returns:
   * {{{
   * val descendantUpdateResult = updateElemsWithNodeSeq(paths)(f)
   * if (paths.contains(Path.Empty)) f(descendantUpdateResult, Path.Empty) else Vector(descendantUpdateResult)
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElemsOrSelfWithNodeSeq(paths: Set[Path])(f: (ThisElem, Path) => immutable.IndexedSeq[ThisNode]): immutable.IndexedSeq[ThisNode]

  /**
   * Updates the descendant elements with the given paths, applying the passed update function.
   *
   * That is, returns:
   * {{{
   * val pathsByFirstEntry: Map[Path.Entry, Set[Path]] = paths.filterNot(_.isEmpty).groupBy(_.firstEntry)
   *
   * updateChildElemsWithNodeSeq(pathsByFirstEntry.keySet) {
   *   case (che, pathEntry) =>
   *     che.updateElemsOrSelfWithNodeSeq(pathsByFirstEntry(pathEntry).map(_.withoutFirstEntry)) {
   *       case (elm, path) =>
   *         f(elm, path.prepend(pathEntry))
   *     }
   * }
   * }}}
   *
   * If the set of paths is small, this method is rather efficient.
   */
  def updateElemsWithNodeSeq(paths: Set[Path])(f: (ThisElem, Path) => immutable.IndexedSeq[ThisNode]): ThisElem

  /**
   * Invokes `updateChildElems`, passing the path entries for which the passed function is defined. It is equivalent to:
   * {{{
   * val editsByPathEntries: Map[Path.Entry, ThisElem] =
   *   findAllChildElemsWithPathEntries.flatMap({ case (che, pe) => f(che, pe).map(newE => (pe, newE)) }).toMap
   *
   * updateChildElems(editsByPathEntries.keySet) { case (che, pe) => editsByPathEntries.getOrElse(pe, che) }
   * }}}
   */
  def updateChildElems(f: (ThisElem, Path.Entry) => Option[ThisElem]): ThisElem

  /**
   * Invokes `updateChildElemsWithNodeSeq`, passing the path entries for which the passed function is defined. It is equivalent to:
   * {{{
   * val editsByPathEntries: Map[Path.Entry, immutable.IndexedSeq[ThisNode]] =
   *   findAllChildElemsWithPathEntries.flatMap({ case (che, pe) => f(che, pe).map(newNodes => (pe, newNodes)) }).toMap
   *
   * updateChildElemsWithNodeSeq(editsByPathEntries.keySet) { case (che, pe) =>
   *   editsByPathEntries.getOrElse(pe, immutable.IndexedSeq(che))
   * }
   * }}}
   */
  def updateChildElemsWithNodeSeq(f: (ThisElem, Path.Entry) => Option[immutable.IndexedSeq[ThisNode]]): ThisElem

  /**
   * Invokes `updateElemsOrSelf`, passing the topmost paths for which the passed function is defined. It is equivalent to:
   * {{{
   * val mutableEditsByPaths = mutable.Map[Path, ThisElem]()
   *
   * val foundElems =
   *   ElemWithPath(self) findTopmostElemsOrSelf { elm =>
   *     val optResult = f(elm.elem, elm.path)
   *     if (optResult.isDefined) {
   *       mutableEditsByPaths += (elm.path -> optResult.get)
   *     }
   *     optResult.isDefined
   *   }
   *
   * val editsByPaths = mutableEditsByPaths.toMap
   *
   * updateElemsOrSelf(editsByPaths.keySet) {
   *   case (elm, path) => editsByPaths.getOrElse(path, elm)
   * }
   * }}}
   */
  def updateTopmostElemsOrSelf(f: (ThisElem, Path) => Option[ThisElem]): ThisElem

  /**
   * Invokes `updateElems`, passing the topmost non-empty paths for which the passed function is defined. It is equivalent to:
   * {{{
   * val mutableEditsByPaths = mutable.Map[Path, ThisElem]()
   *
   * val foundElems =
   *   ElemWithPath(self) findTopmostElems { elm =>
   *     val optResult = f(elm.elem, elm.path)
   *     if (optResult.isDefined) {
   *       mutableEditsByPaths += (elm.path -> optResult.get)
   *     }
   *     optResult.isDefined
   *   }
   *
   * val editsByPaths = mutableEditsByPaths.toMap
   *
   * updateElems(editsByPaths.keySet) {
   *   case (elm, path) => editsByPaths.getOrElse(path, elm)
   * }
   * }}}
   */
  def updateTopmostElems(f: (ThisElem, Path) => Option[ThisElem]): ThisElem

  /**
   * Invokes `updateElemsOrSelfWithNodeSeq`, passing the topmost paths for which the passed function is defined. It is equivalent to:
   * {{{
   * val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[ThisNode]]()
   *
   * val foundElems =
   *   ElemWithPath(self) findTopmostElemsOrSelf { elm =>
   *     val optResult = f(elm.elem, elm.path)
   *     if (optResult.isDefined) {
   *       mutableEditsByPaths += (elm.path -> optResult.get)
   *     }
   *     optResult.isDefined
   *   }
   *
   * val editsByPaths = mutableEditsByPaths.toMap
   *
   * updateElemsOrSelfWithNodeSeq(editsByPaths.keySet) {
   *   case (elm, path) => editsByPaths.getOrElse(path, immutable.IndexedSeq(elm))
   * }
   * }}}
   */
  def updateTopmostElemsOrSelfWithNodeSeq(f: (ThisElem, Path) => Option[immutable.IndexedSeq[ThisNode]]): immutable.IndexedSeq[ThisNode]

  /**
   * Invokes `updateElemsWithNodeSeq`, passing the topmost non-empty paths for which the passed function is defined. It is equivalent to:
   * {{{
   * val mutableEditsByPaths = mutable.Map[Path, immutable.IndexedSeq[ThisNode]]()
   *
   * val foundElems =
   *   ElemWithPath(self) findTopmostElems { elm =>
   *     val optResult = f(elm.elem, elm.path)
   *     if (optResult.isDefined) {
   *       mutableEditsByPaths += (elm.path -> optResult.get)
   *     }
   *     optResult.isDefined
   *   }
   *
   * val editsByPaths = mutableEditsByPaths.toMap
   *
   * updateElemsWithNodeSeq(editsByPaths.keySet) {
   *   case (elm, path) => editsByPaths.getOrElse(path, immutable.IndexedSeq(elm))
   * }
   * }}}
   */
  def updateTopmostElemsWithNodeSeq(f: (ThisElem, Path) => Option[immutable.IndexedSeq[ThisNode]]): ThisElem
}

object UpdatableElemApi {

  /**
   * This query API type, restricting ThisNode, ThisElem and ThisElemApi to the passed type parameters.
   *
   * @tparam N The node self type
   * @tparam E The element self type
   */
  type Aux[N, E] = UpdatableElemApi { type ThisNode = N; type ThisElem = E; type ThisElemApi = E }
}
