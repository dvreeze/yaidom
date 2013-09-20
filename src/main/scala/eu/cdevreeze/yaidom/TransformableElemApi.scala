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

import scala.collection.immutable

/**
 * This is the <em>element transformation</em> part of the yaidom query and update API. Only a few DOM-like element implementations
 * in yaidom mix in this trait (indirectly, because some implementing sub-trait is mixed in), thus sharing this API.
 *
 * This trait is purely <em>abstract</em>. The most common implementation of this trait is [[eu.cdevreeze.yaidom.TransformableElemLike]].
 * That trait only knows how to transform child elements. Using this minimal knowledge, the trait offers methods to transform
 * descendant elements and descendant-or-self elements. Indeed, the trait is similar to `ParentElemLike`, except that it
 * transforms elements instead of querying for elements.
 *
 * The big conceptual difference with "updatable" elements (in trait `UpdatableElemLike[N, E]`) is that "transformations" are
 * about applying some transforming function to an element tree, while "(functional) updates" are about "updates" at given
 * element paths.
 *
 * ==TransformableElemApi examples==
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
 * Suppose this XML has been parsed into [[eu.cdevreeze.yaidom.Elem]] variable named ``bookstoreElem``. Then we can combine
 * author first and last names as follows:
 * {{{
 * val authorNamespace = "http://bookstore/author"
 *
 * bookstoreElem = bookstoreElem transformElems {
 *   case elem: Elem if elem.resolvedName == EName(authorNamespace, "Author") =>
 *     val firstName = (elem \ (_.localName == "First_Name")).headOption.map(_.text).getOrElse("")
 *     val lastName = (elem \ (_.localName == "Last_Name")).headOption.map(_.text).getOrElse("")
 *     val name = (firstName + " " + lastName).trim
 *     Node.textElem(QName("auth:Author"), elem.scope ++ Scope.from("auth" -> authorNamespace), name)
 *   case elem: Elem => elem
 * }
 * bookstoreElem = bookstoreElem.prettify(2)
 * }}}
 *
 * When using the `TransformableElemApi` API, keep the following in mind:
 * <ul>
 * <li>The `transformElems` and `transformElemsOrSelf` methods (and their Node sequence producing counterparts)
 * may produce a lot of "garbage". If only a small portion of an element tree needs to be updated, the "update" methods in trait
 * `UpdatableElemApi` may be a better fit.</li>
 * <li>Transformations operate in a bottom-up manner. This implies that parent scopes cannot be used for transforming child
 * elements. Hence, namespace undeclarations may result, which are not allowed in XML 1.0 (except for the default namespace).</li>
 * </ul>
 *
 * Top-down transformations are still possible, by combining recursion with method `transformChildElems` (or
 * `transformChildElemsToNodeSeq`). For example:
 * {{{
 * def pushUpPrefixedNamespaceDeclarations(elem: Elem): Elem = {
 *   elem transformChildElems { e =>
 *     val newE = e.copy(scope = elem.scope.withoutDefaultNamespace ++ e.scope)
 *     pushUpPrefixedNamespaceDeclarations(newE)
 *   }
 * }
 * }}}
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait TransformableElemApi[N, E <: N with TransformableElemApi[N, E]] { self: E =>

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   *
   * That is, returns the equivalent of:
   * {{{
   * val newChildren =
   *   children map {
   *     case e: E => f(e)
   *     case n: N => n
   *   }
   * withChildren(newChildren)
   * }}}
   */
  def transformChildElems(f: E => E): E

  /**
   * Returns the same element, except that child elements have been replaced by applying the given function. Non-element
   * child nodes occur in the result element unaltered.
   *
   * That is, returns the equivalent of:
   * {{{
   * val newChildren =
   *   children flatMap {
   *     case e: E => f(e)
   *     case n: N => Vector(n)
   *   }
   * withChildren(newChildren)
   * }}}
   */
  def transformChildElemsToNodeSeq(f: E => immutable.IndexedSeq[N]): E

  /**
   * Transforms the element by applying the given function to all its descendant-or-self elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(transformChildElems (e => e.transformElemsOrSelf(f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElems(f))
   * }}}
   */
  def transformElemsOrSelf(f: E => E): E

  /**
   * Transforms the element by applying the given function to all its descendant elements, in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * transformChildElems (e => e.transformElemsOrSelf(f))
   * }}}
   */
  def transformElems(f: E => E): E

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant-or-self elements,
   * in a bottom-up manner.
   *
   * That is, returns the equivalent of:
   * {{{
   * f(transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f)))
   * }}}
   *
   * In other words, returns the equivalent of:
   * {{{
   * f(transformElemsToNodeSeq(f))
   * }}}
   */
  def transformElemsOrSelfToNodeSeq(f: E => immutable.IndexedSeq[N]): immutable.IndexedSeq[N]

  /**
   * Transforms each descendant element to a node sequence by applying the given function to all its descendant elements,
   * in a bottom-up manner. The function is not applied to this element itself.
   *
   * That is, returns the equivalent of:
   * {{{
   * transformChildElemsToNodeSeq(e => e.transformElemsOrSelfToNodeSeq(f))
   * }}}
   *
   * It is equivalent to the following expression:
   * {{{
   * transformElemsOrSelf { e => e.transformChildElemsToNodeSeq(che => f(che)) }
   * }}}
   */
  def transformElemsToNodeSeq(f: E => immutable.IndexedSeq[N]): E
}
