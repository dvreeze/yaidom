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
 * Obviously methods ``children``, ``withChildren`` and ``childNodeIndexesByPathEntries`` must be consistent with
 * methods such as ``findAllChildElems`` and ``findAllChildElemsWithPathEntries``, if the corresponding traits are
 * mixed in.
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
 * Suppose this XML has been parsed into [[eu.cdevreeze.yaidom.defaultelem.Elem]] variable named ``bookstoreElem``. Then we can add a book
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
 *     authorsPath <- bookstoreElem filterElemPaths { e => e.resolvedName == EName(bookstoreNamespace, "Authors") }
 *     if authorsPath.findAncestorPath(path => path.endsWithName(EName(bookstoreNamespace, "Book")) &&
 *       bookstoreElem.getElemOrSelfByPath(path).attribute(EName("ISBN")) == "978-1617290657").isDefined
 *   } yield authorsPath
 *
 * require(fpBookAuthorsPaths.size == 1)
 * val fpBookAuthorsPath = fpBookAuthorsPaths.head
 *
 * bookstoreElem = bookstoreElem.updated(fpBookAuthorsPath) { elem =>
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
 * val bookPaths = bookstoreElem filterElemPaths (_.resolvedName == EName(bookstoreNamespace, "Book"))
 *
 * bookstoreElem = bookstoreElem.updatedWithNodeSeqAtPaths(bookPaths.toSet) { (elem, path) =>
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
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemApi[N, E <: N with UpdatableElemApi[N, E]] extends IsNavigableApi[E] { self: E =>

  /** Returns the child nodes of this element, in the correct order */
  def children: immutable.IndexedSeq[N]

  /** Returns an element with the same name, attributes and scope as this element, but with the given child nodes */
  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  /**
   * Returns a Map from path entries (with respect to this element as parent element) to child node indexes.
   * The faster this method is, the better.
   */
  def childNodeIndexesByPathEntries: Map[Path.Entry, Int]

  /**
   * Shorthand for `childNodeIndexesByPathEntries.getOrElse(childPathEntry, -1)`.
   * The faster this method is, the better.
   */
  def childNodeIndex(childPathEntry: Path.Entry): Int

  /** Shorthand for `withChildren(children.updated(index, newChild))` */
  def withUpdatedChildren(index: Int, newChild: N): E

  /** Shorthand for `withChildren(children.patch(from, newChildren, replace))` */
  def withPatchedChildren(from: Int, newChildren: immutable.IndexedSeq[N], replace: Int): E

  /** Returns a copy in which the given child has been inserted at the given position (0-based) */
  def plusChild(index: Int, child: N): E

  /** Returns a copy in which the given child has been inserted at the end */
  def plusChild(child: N): E

  /** Returns a copy in which the child at the given position (0-based) has been removed */
  def minusChild(index: Int): E

  /**
   * '''Core method''' that "functionally updates" the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.Path.Entry]] (compared to this element as root).
   *
   * The method throws an exception if no element is found with the given path entry.
   *
   * It can be defined as follows:
   * {{{
   * val idx = self.childNodeIndex(pathEntry)
   * self.withUpdatedChildren(idx, f(children(idx).asInstanceOf[E]))
   * }}}
   */
  def updated(pathEntry: Path.Entry)(f: E => E): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all child elements with the given path entries (compared to this element as root).
   *
   * It can be defined as follows (ignoring exceptions):
   * {{{
   * val newChildren = childNodeIndexesByPathEntries.filterKeys(pathEntries).toSeq.sortBy(_._2).reverse.foldLeft(children) {
   *   case (acc, (pathEntry, idx)) =>
   *     acc.updated(idx, f(acc(idx).asInstanceOf[E], pathEntry))
   * }
   * withChildren(newChildren)
   * }}}
   */
  def updatedAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => E): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to the element that has the given [[eu.cdevreeze.yaidom.Path]] (compared to this element as root).
   *
   * The method throws an exception if no element is found with the given path.
   *
   * It can be defined (recursively) as follows:
   * {{{
   * if (path == Path.Root) f(self)
   * else updated(path.firstEntry) { e => e.updated(path.withoutFirstEntry)(f) }
   * }}}
   */
  def updated(path: Path)(f: E => E): E

  /** Returns `updated(path) { e => newElem }` */
  def updated(path: Path, newElem: E): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all descendant-or-self elements with the given paths (compared to this element as root).
   *
   * It can be defined (recursively) as follows (ignoring exceptions):
   * {{{
   * def updatedAtPaths(paths: Set[Path])(f: (E, Path) => E): E = {
   *   val pathsByPathEntries = paths.filter(path => !path.isRoot).groupBy(path => path.firstEntry)
   *   val resultWithoutSelf = self.updatedAtPathEntries(pathsByPathEntries.keySet) { (che, pathEntry) =>
   *     val newChe = che.updatedAtPaths(paths.map(_.withoutFirstEntry)) { (elem, relativePath) =>
   *       f(elem, relativePath.prepend(pathEntry))
   *     }
   *     newChe
   *   }
   *   if (paths.contains(Path.Root)) f(resultWithoutSelf, Path.Root) else resultWithoutSelf
   * }
   * }}}
   *
   * It is also equivalent to:
   * {{{
   * val pathsReversed = findAllElemOrSelfPaths.filter(p => paths.contains(p)).reverse
   * pathsReversed.foldLeft(self) { case (acc, path) => acc.updated(path) { e => f(e, path) } }
   * }}}
   */
  def updatedAtPaths(paths: Set[Path])(f: (E, Path) => E): E

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.Path]] (compared to this element as root). If the given path is the
   * root path, this element itself is returned unchanged.
   *
   * This function could be defined as follows:
   * {{{
   * // First define function g as follows:
   *
   * def g(e: Elem): Elem = {
   *   if (path == Path.Root) e
   *   else {
   *     e.withPatchedChildren(
   *       e.childNodeIndex(path.lastEntry),
   *       f(e.findChildElemByPathEntry(path.lastEntry).get),
   *       1)
   *   }
   * }
   *
   * // Then the function updatedWithNodeSeq(path)(f) could be defined as:
   *
   * updated(path.parentPathOption.getOrElse(Path.Root))(g)
   * }}}
   * After all, this is just a functional update that replaces the parent element, if it exists.
   *
   * The method throws an exception if no element is found with the given path.
   */
  def updatedWithNodeSeq(path: Path)(f: E => immutable.IndexedSeq[N]): E

  /** Returns `updatedWithNodeSeq(path) { e => newNodes }` */
  def updatedWithNodeSeq(path: Path, newNodes: immutable.IndexedSeq[N]): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all child elements with the given path entries (compared to this element as root).
   *
   * It can be defined as follows (ignoring exceptions):
   * {{{
   * val indexesByPathEntries = childNodeIndexesByPathEntries.filterKeys(pathEntries).toSeq.sortBy(_._2).reverse
   * val newChildGroups =
   *   indexesByPathEntries.foldLeft(self.children.map(n => immutable.IndexedSeq(n))) {
   *     case (acc, (pathEntry, idx)) =>
   *       acc.updated(idx, f(acc(idx).head.asInstanceOf[E], pathEntry))
   *   }
   * withChildren(newChildGroups.flatten)
   * }}}
   */
  def updatedWithNodeSeqAtPathEntries(pathEntries: Set[Path.Entry])(f: (E, Path.Entry) => immutable.IndexedSeq[N]): E

  /**
   * Method that "functionally updates" the tree with this element as root element, by applying the passed function
   * to all descendant elements with the given paths (compared to this element as root), but ignoring the root path.
   *
   * It can be defined as follows (ignoring exceptions):
   * {{{
   * val pathsByParentPaths = paths.filter(path => !path.isRoot).groupBy(path => path.parentPath)
   * self.updatedAtPaths(pathsByParentPaths.keySet) { (elem, path) =>
   *   val childPathEntries = pathsByParentPaths(path).map(_.lastEntry)
   *   elem.updatedWithNodeSeqAtPathEntries(childPathEntries) { (che, pathEntry) =>
   *     f(che, path.append(pathEntry))
   *   }
   * }
   * }}}
   */
  def updatedWithNodeSeqAtPaths(paths: Set[Path])(f: (E, Path) => immutable.IndexedSeq[N]): E
}
