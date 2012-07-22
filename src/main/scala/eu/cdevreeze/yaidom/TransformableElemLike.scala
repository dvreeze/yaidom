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
 * Transformable element. It defines a contract for "functional updates".
 *
 * Implementation notes: We could provide implementations of the `updated` methods, but not without any costs.
 * I tried several options, such as introducing another type parameter for the node supertype, or using abstract type members,
 * or getting rid of nodes altogether in the "update" trait. No such attempt led to a sufficiently satisfactory result: either
 * casts deep inside the code were needed, or the solution was intrusive, or performance suffered. In the end, I accepted
 * some code duplication, hidden behind a common contract, namely this trait.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait TransformableElemLike[E <: TransformableElemLike[E]] {

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed partial function to the elements
   * for which the partial function is defined. The partial function is defined for an element if that element has an [[eu.cdevreeze.yaidom.ElemPath]]
   * (w.r.t. this element as root) for which it is defined. Tree traversal is top-down.
   *
   * Only topmost elements for which the partial function is defined are "functionally updated", so their descendants, if any, are
   * determined by the result of the partial function application, not by their occurrence in the original tree.
   *
   * This is potentially an expensive method.
   */
  def updated(pf: PartialFunction[ElemPath, E]): E

  /**
   * "Functionally updates" the tree with this element as root element, by applying the passed function to the element
   * that has the given [[eu.cdevreeze.yaidom.ElemPath]] (compared to this element as root). The method throws an exception
   * if no element is found with the given path.
   */
  def updated(path: ElemPath)(f: E => E): E

  /** Returns `updated(path) { e => elm }` */
  final def updated(path: ElemPath, elm: E): E = updated(path) { e => elm }
}
