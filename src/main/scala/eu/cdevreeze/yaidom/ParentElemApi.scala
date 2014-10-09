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

package eu.cdevreeze.yaidom

import scala.collection.immutable

/**
 * This is the <em>foundation</em> of the yaidom <em>uniform query API</em>. Many DOM-like element implementations in
 * yaidom mix in this trait (indirectly, because some implementing sub-trait is mixed in), thus sharing this query API.
 *
 * '''This trait typically does not show up in application code using yaidom, yet its (uniform) API does. Hence, it makes sense
 * to read the documentation of this trait, knowing that the API is offered by multiple element implementations.'''
 *
 * This trait is purely <em>abstract</em>. The most common implementation of this trait is [[eu.cdevreeze.yaidom.ParentElemLike]].
 * That trait only knows about elements (and not about other nodes), and only knows that elements can <em>have child elements</em>
 * (again not knowing about other child nodes). Using this minimal knowledge alone, it offers methods to query for
 * <em>descendant</em> elements, <em>descendant-or-self</em> methods, or sub-collections thereof. It is this minimal knowledge that
 * makes this API uniform.
 *
 * This query API leverages the Scala Collections API. Query results can be manipulated using the Collections API, and the
 * query API implementation (in ``ParentElemLike``) uses the Collections API internally.
 *
 * ==ParentElemApi examples==
 *
 * To illustrate usage of this API, consider the following example. Let's say we want to determine if some XML has its namespace
 * declarations (if any) only at the root element level. We show the query code for several yaidom DOM-like element implementations.
 *
 * Note that it depends on the DOM-like element implementation how to query for namespace declarations, but the code to
 * query for descendant or descendant-or-self elements remains the same. The method to retrieve all descendant elements
 * is called ``findAllElems``, and the method to retrieve all descendant-or-self elements is called ``findAllElemsOrSelf``.
 * The corresponding "filtering" methods are called ``filterElems`` and ``filterElemsOrSelf``, respectively. Knowing this,
 * it is easy to guess the other API method names.
 *
 * Let's start with a yaidom DOM wrapper, named ``rootElem``, of type [[eu.cdevreeze.yaidom.dom.DomElem]], and query for the
 * "offending" descendant elements:
 * {{{
 * rootElem filterElems (elem => !convert.DomConversions.extractNamespaceDeclarations(elem.wrappedNode.getAttributes).isEmpty)
 * }}}
 * This returns all offending elements, that is, all descendant elements of the root element (excluding the root element itself)
 * that have at least one namespace declaration.
 *
 * Now let's use an [[eu.cdevreeze.yaidom.ElemBuilder]], again named ``rootElem``:
 * {{{
 * rootElem filterElems (elem => !elem.namespaces.isEmpty)
 * }}}
 * The query is the same as the preceding one, except for the retrieval of namespace declarations of an element. (It should be
 * noted that class ``ElemBuilder`` already has a method ``allDeclarationsAreAtTopLevel``.)
 *
 * Finally, let's use a ``rootElem`` of type [[eu.cdevreeze.yaidom.indexed.Elem]], which is immutable, but knows its ancestry:
 * {{{
 * rootElem filterElems (elem => !elem.namespaces.isEmpty)
 * }}}
 * This is exactly the same code as for ``ElemBuilder``, because namespace declarations happen to be retrieved in the same way.
 *
 * If we want to query for all elements with namespace declarations, including the root element itself, we could write:
 * {{{
 * rootElem filterElemsOrSelf (elem => !elem.namespaces.isEmpty)
 * }}}
 *
 * In summary, the extremely simple ``ParentElemApi`` query API is indeed a uniform query API, offered by many different
 * yaidom DOM-like element implementations. It should be noted that most of these element implementations offer the
 * ``ElemApi`` query API, which extends the ``ParentElemApi`` query API.
 *
 * ==ParentElemApi more formally==
 *
 * '''In order to get started using the API, this more formal section can safely be skipped. On the other hand, this section
 * may provide a deeper understanding of the API.'''
 *
 * The ``ParentElemApi`` trait can be understood in a precise <em>mathematical</em> sense, as shown below.
 *
 * The most <em>fundamental method</em> of this trait is ``findAllChildElems``. The semantics of the other methods can be defined
 * directly or indirectly in terms of this method.
 *
 * The <em>basic operations</em> definable in terms of that method are ``\`` (alias for ``filterChildElems``), ``\\`` (alias for ``filterElemsOrSelf``)
 * and ``\\!`` (alias for ``findTopmostElemsOrSelf``). Their semantics must be as if they had been defined as follows:
 * {{{
 * def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E] =
 *   this.findAllChildElems.filter(p)
 *
 * def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
 *   Vector(this).filter(p) ++ (this.findAllChildElems flatMap (_.filterElemsOrSelf(p)))
 *
 * def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
 *   if (p(this)) Vector(this)
 *   else (this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p)))
 * }}}
 *
 * Moreover, we could have defined:
 * {{{
 * def filterElems(p: E => Boolean): immutable.IndexedSeq[E] =
 *   this.findAllChildElems flatMap (_.filterElemsOrSelf(p))
 *
 * def findTopmostElems(p: E => Boolean): immutable.IndexedSeq[E] =
 *   this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p))
 * }}}
 * and:
 * {{{
 * def findAllElemsOrSelf: immutable.IndexedSeq[E] = filterElemsOrSelf(e => true)
 *
 * def findAllElems: immutable.IndexedSeq[E] = filterElems(e => true)
 * }}}
 *
 * The following properties must hold (in the absence of side-effects), and can indeed be proven (given the documented
 * "definitions" of these operations):
 * {{{
 * // Filtering
 *
 * elem.filterElems(p) == elem.findAllElems.filter(p)
 *
 * elem.filterElemsOrSelf(p) == elem.findAllElemsOrSelf.filter(p)
 *
 * // Finding topmost
 *
 * elem.findTopmostElems(p) == {
 *   elem.filterElems(p) filter { e =>
 *     val hasNoMatchingAncestor = elem.filterElems(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 *
 * elem.findTopmostElemsOrSelf(p) == {
 *   elem.filterElemsOrSelf(p) filter { e =>
 *     val hasNoMatchingAncestor = elem.filterElemsOrSelf(p) forall { _.findElem(_ == e).isEmpty }
 *     hasNoMatchingAncestor
 *   }
 * }
 *
 * (elem.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p))) == (elem.filterElems(p))
 *
 * (elem.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) == (elem.filterElemsOrSelf(p))
 * }}}
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ParentElemApi[E <: ParentElemApi[E]] { self: E =>

  /**
   * '''Core method''' that returns '''all child elements''', in the correct order.
   * Other operations can be defined in terms of this one.
   */
  def findAllChildElems: immutable.IndexedSeq[E]

  /**
   * Returns all descendant elements (not including this element). This method could be defined as `filterElems { e => true }`.
   * Equivalent to `findAllElemsOrSelf.drop(1)`.
   */
  def findAllElems: immutable.IndexedSeq[E]

  /**
   * Returns this element followed by all descendant elements (that is, the descendant-or-self elements).
   * This method could be defined as `filterElemsOrSelf { e => true }`.
   */
  def findAllElemsOrSelf: immutable.IndexedSeq[E]

  /**
   * Returns the child elements obeying the given predicate. This method could be defined as:
   * {{{
   * def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E] =
   *   this.findAllChildElems.filter(p)
   * }}}
   */
  def filterChildElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the descendant elements obeying the given predicate.
   * This method could be defined as:
   * {{{
   * this.findAllChildElems flatMap (_.filterElemsOrSelf(p))
   * }}}
   */
  def filterElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the descendant-or-self elements obeying the given predicate. This method could be defined as:
   * {{{
   * def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
   *   Vector(this).filter(p) ++ (this.findAllChildElems flatMap (_.filterElemsOrSelf(p)))
   * }}}
   *
   * It can be proven that the result is equivalent to `findAllElemsOrSelf filter p`.
   */
  def filterElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the first found child element obeying the given predicate, if any, wrapped in an `Option`.
   * This method could be defined as `filterChildElems(p).headOption`.
   */
  def findChildElem(p: E => Boolean): Option[E]

  /**
   * Returns the first found (topmost) descendant element obeying the given predicate, if any, wrapped in an `Option`.
   * This method could be defined as `filterElems(p).headOption`.
   */
  def findElem(p: E => Boolean): Option[E]

  /**
   * Returns the first found (topmost) descendant-or-self element obeying the given predicate, if any, wrapped in an `Option`.
   * This method could be defined as `filterElemsOrSelf(p).headOption`.
   */
  def findElemOrSelf(p: E => Boolean): Option[E]

  /**
   * Returns the descendant elements obeying the given predicate that have no ancestor obeying the predicate.
   * This method could be defined as:
   * {{{
   * this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p))
   * }}}
   */
  def findTopmostElems(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the descendant-or-self elements obeying the given predicate, such that no ancestor obeys the predicate.
   * This method could be defined as:
   * {{{
   * def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E] =
   *   if (p(this)) Vector(this)
   *   else (this.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p)))
   * }}}
   */
  def findTopmostElemsOrSelf(p: E => Boolean): immutable.IndexedSeq[E]

  /**
   * Returns the single child element obeying the given predicate, and throws an exception otherwise.
   * This method could be defined as `findChildElem(p).get`.
   */
  def getChildElem(p: E => Boolean): E

  /** Shorthand for `filterChildElems(p)`. Use this shorthand only if the predicate is a short expression. */
  def \(p: E => Boolean): immutable.IndexedSeq[E]

  /** Shorthand for `filterElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  def \\(p: E => Boolean): immutable.IndexedSeq[E]

  /** Shorthand for `findTopmostElemsOrSelf(p)`. Use this shorthand only if the predicate is a short expression. */
  def \\!(p: E => Boolean): immutable.IndexedSeq[E]
}
