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
 * "Updatable" element. It defines a contract for "functional updates". See [[eu.cdevreeze.yaidom.UpdatableElemLike]].
 *
 * For the conceptual difference with "transformable" elements, see trait [[eu.cdevreeze.yaidom.TransformableElemApi]].
 *
 * This purely abstract query API trait leaves the implementation completely open. For example, an implementation backed by
 * an XML database would not use the ``UpdatableElemLike`` implementation, for reasons of efficiency.
 *
 * There are 2 groups of "functional update" methods that work with `ElemPath` instances (implicitly or explicitly):
 * <ul>
 * <li>Overloaded `updated` methods. They use an "update function" from elements to elements, and call it on the root element as well.</li>
 * <li>Overloaded `updatedWithNodeSeq` methods. They use an "update function" from elements to node sequences, and do not call it on the root element.</li>
 * </ul>
 *
 * The second group of "functional update" methods can be implemented in terms of the first group of methods. The second group of
 * methods allow for flexible "functional updates", because an element can be "replaced" by an arbitrary sequence of nodes.
 * For example, with the `updatedWithNodeSeq` (and `topmostUpdatedWithNodeSeq`) functions (taking a partial function parameter),
 * it is easy to write functions to functionally delete elements, insert nodes before or after an element, etc.
 *
 * Below follow some formal properties that the "functional update" support obeys. We assume the use of side-effect-free functions
 * only.
 *
 * For example, the following property (trivially) holds:
 * {{{
 * // First define pf2 in terms of partial function pf, and let E be type Elem
 *
 * val pf2: PartialFunction[Elem, Elem] = {
 *   case e: Elem if elem.findTopmostElemsOrSelf(e2 => pf.isDefinedAt(e2)).contains(e) => pf(e)
 * }
 *
 * // Then the following holds (in terms of '=='):
 *
 * resolved.Elem(elem.topmostUpdated(pf)) == resolved.Elem(elem.updated(pf2))
 * }}}
 *
 * An analogous property holds for `topmostUpdatedWithNodeSeq` (taking a partial function) in terms of `updatedWithNodeSeq`.
 *
 * Another property reduces `updated` (taking a partial function) to `transform`:
 * {{{
 * val f: E => E = { e => if (pf.isDefinedAt(e)) pf(e) else e }
 *
 * resolved.Elem(elem.updated(pf)) == resolved.Elem(elem.transform(f))
 * }}}
 * Beware that this equality may not hold if pf depends on element object identities, because `transform` "changes" object
 * identities more aggressively than `updated`.
 *
 * Yet another property reduces `updatedWithNodeSeq` to `updated`:
 * {{{
 * // Let E be type Elem, and define pf2 as follows:
 *
 * val pf2: PartialFunction[Elem, Elem] = {
 *   case e: Elem if !e.filterChildElems(che => pf.isDefinedAt(che)).isEmpty =>
 *     val childElemsWithPathEntries =
 *       e.findAllChildElemsWithPathEntries.filter(elemPathPair => pf.isDefinedAt(elemPathPair._1)).reverse
 *
 *     childElemsWithPathEntries.foldLeft(e) { case (acc, (che, pathEntry)) =>
 *       acc.withPatchedChildren(acc.childNodeIndex(pathEntry), pf(che), 1)
 *     }
 * }
 *
 * // Then the following holds (in terms of '=='):
 *
 * resolved.Elem(elem.updatedWithNodeSeq(pf)) == resolved.Elem(elem.updated(pf2))
 * }}}
 *
 * It is easy to understand the subcase where partial function pf is defined only for one element. After all, in that case
 * the property reduces to the "definition" of method `updatedWithNodeSeq` (taking one ElemPath and a function) in terms of
 * method `updated` (taking the parent path and a function transforming the parent element).
 *
 * If the property holds if partial function pf is defined for only one element, it can also be understood to hold if pf
 * is defined only for (some or all) child elements of one given parent element. Note that the functional updates are
 * performed in reverse order of the child element path entries, so these path entries remain valid during the updates.
 *
 * The general case (where pf can be any partial function from elements to node sequences) needs to be proven by structural
 * induction.
 *
 * In summary, the functional update methods can all be understood (directly or indirectly) in terms of method
 * `updated(path)(f)`.
 *
 * ==UpdatableElemApi more formally==
 *
 * Below follows a proof by structural induction of the property that given:
 * {{{
 * val f: E => E = { e => if (pf.isDefinedAt(e)) pf(e) else e }
 * }}}
 * we have:
 * {{{
 * resolved.Elem(elem.updated(pf)) == resolved.Elem(elem.transform(f))
 * }}}
 *
 * For the proof, let:
 * {{{
 * val p = { e: E => pf.isDefinedAt(e) }
 *
 * val pathsReversed = elem.filterElemOrSelfPaths(p).reverse
 * }}}
 *
 * In the proof, we use the following (yet unproven) property, called property (1.):
 * {{{
 * resolved.Elem(elem.updated(pf)) == {
 *   resolved.Elem(f(elem withMappedChildElems (che => che.updated(pf))))
 * }
 * }}}
 *
 * __Base case__
 *
 * If `elem` has no child elements, and pf is defined at elem, then the LHS can be rewritten (modulo `resolved.Elem` equality)
 * as follows:
 * {{{
 * elem.updated(pf)
 *
 * // Definition of updated
 *
 * pathsReversed.foldLeft(elem) { case (acc, path) =>
 *   acc.updated(path, pf(acc.getWithElemPath(path)))
 * }
 *
 * // No child elements, and pf is defined at elem, so pathsReversed only contains ElemPath.Root
 *
 * elem.updated(ElemPath.Root, pf(elem.getWithElemPath(ElemPath.Root)))
 *
 * // Using the root path for getting an element
 *
 * elem.updated(ElemPath.Root, pf(elem))
 *
 * // In terms of another overloaded function called updated
 *
 * elem.updated(ElemPath.Root) { e => pf(elem) }
 *
 * // Definition of updated (taking path and function), applied to the root path. See implementation in UpdatableElemLike.
 *
 * pf(elem)
 *
 * // Definition of f
 *
 * f(elem)
 *
 * // Definitions of transform and withMappedChildElems, given that there are no child elements
 *
 * elem.transform(f)
 * }}}
 * which is the RHS.
 *
 * If `elem` has no child elements, and pf is not defined at elem, then the LHS can be rewritten (modulo `resolved.Elem` equality)
 * as follows:
 * {{{
 * elem.updated(pf)
 *
 * // Definition of updated
 *
 * pathsReversed.foldLeft(elem) { case (acc, path) =>
 *   acc.updated(path, pf(acc.getWithElemPath(path)))
 * }
 *
 * // No child elements, and pf is not defined at elem, so pathsReversed is empty, and the foldLeft just returns elem
 *
 * elem
 *
 * // Definition of f, given that in this case f(elem) is equal to elem
 *
 * f(elem)
 *
 * // Definitions of transform and withMappedChildElems, given that there are no child elements
 *
 * elem.transform(f)
 * }}}
 * which is the RHS.
 *
 * __Inductive step__
 *
 * If `elem` does have child elements, the LHS can be rewritten (modulo `resolved.Elem` equality) as:
 * {{{
 * elem.updated(pf)
 *
 * // Property (1.)
 *
 * f(elem withMappedChildElems (che => che.updated(pf)))
 *
 * // Induction hypothesis
 *
 * f(elem withMappedChildElems (che => che.transform(f)))
 *
 * // Definition of transform
 *
 * elem.transform(f)
 * }}}
 * which is the RHS.
 *
 * This completes the proof.
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait UpdatableElemApi[N, E <: N with UpdatableElemApi[N, E]] extends PathAwareElemApi[E] { self: E =>

  /** Returns the child nodes of this element, in the correct order */
  def children: immutable.IndexedSeq[N]

  /** Returns an element with the same name, attributes and scope as this element, but with the given child nodes */
  def withChildren(newChildren: immutable.IndexedSeq[N]): E

  /**
   * Returns the child node index of the given `ElemPath.Entry` with respect to this element as parent element.
   * If the path entry is not found, -1 is returned.
   */
  def childNodeIndex(childPathEntry: ElemPath.Entry): Int

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
   * to the element that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root).
   *
   * The method throws an exception if no element is found with the given path.
   */
  def updated(path: ElemPath)(f: E => E): E

  /** Returns `updated(path) { e => newElem }` */
  def updated(path: ElemPath, newElem: E): E

  /**
   * Functionally updates the descendant-or-self elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = filterElemOrSelfPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updated(path, pf(e))
   * }
   * }}}
   */
  def updated(pf: PartialFunction[E, E]): E

  /**
   * Functionally updates the topmost descendant-or-self elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = findTopmostElemOrSelfPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updated(path, pf(e))
   * }
   * }}}
   */
  def topmostUpdated(pf: PartialFunction[E, E]): E

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root). If the given path is the
   * root path, this element itself is returned unchanged.
   *
   * This function could be defined as follows:
   * {{{
   * // First define function g as follows:
   *
   * def g(e: Elem): Elem = {
   *   if (path == ElemPath.Root) e
   *   else {
   *     e.withPatchedChildren(
   *       e.childNodeIndex(path.lastEntry),
   *       f(e.findWithElemPathEntry(path.lastEntry).get),
   *       1)
   *   }
   * }
   *
   * // Then the function updatedWithNodeSeq(path)(f) could be defined as:
   *
   * updated(path.parentPathOption.getOrElse(ElemPath.Root))(g)
   * }}}
   * After all, this is just a functional update that replaces the parent element, if it exists.
   *
   * The method throws an exception if no element is found with the given path.
   */
  def updatedWithNodeSeq(path: ElemPath)(f: E => immutable.IndexedSeq[N]): E

  /** Returns `updatedWithNodeSeq(path) { e => newNodes }` */
  def updatedWithNodeSeq(path: ElemPath, newNodes: immutable.IndexedSeq[N]): E

  /**
   * Functionally updates the descendant elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = filterElemPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updatedWithNodeSeq(path, pf(e))
   * }
   * }}}
   */
  def updatedWithNodeSeq(pf: PartialFunction[E, immutable.IndexedSeq[N]]): E

  /**
   * Functionally updates the topmost descendant elements for which the partial function is defined,
   * within the tree of which this element is the root element.
   *
   * This function is equivalent to:
   * {{{
   * val p = { e: E => pf.isDefinedAt(e) }
   * val pathsReversed = findTopmostElemPaths(p).reverse
   *
   * pathsReversed.foldLeft(self) { case (acc, path) =>
   *   val e = acc.findWithElemPath(path).get
   *   acc.updatedWithNodeSeq(path, pf(e))
   * }
   * }}}
   */
  def topmostUpdatedWithNodeSeq(pf: PartialFunction[E, immutable.IndexedSeq[N]]): E
}
