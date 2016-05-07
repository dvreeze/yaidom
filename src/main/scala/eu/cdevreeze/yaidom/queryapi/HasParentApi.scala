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

/**
 * API trait for elements that can be asked for the ancestor elements, if any.
 *
 * This trait only knows about elements, not about documents as root element parents.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait HasParentApi[E <: HasParentApi[E]] { self: E =>

  /**
   * Returns the parent element, if any, wrapped in an Option
   */
  def parentOption: Option[E]

  /**
   * Returns the equivalent `parentOption.get`, throwing an exception if this is the root element
   */
  def parent: E

  /**
   * Returns all ancestor elements or self, starting with this element, then the parent, if any, and ending with
   * the root element.
   */
  def ancestorsOrSelf: immutable.IndexedSeq[E]

  /**
   * Returns `ancestorsOrSelf.drop(1)`
   */
  def ancestors: immutable.IndexedSeq[E]

  /**
   * Returns the first found ancestor-or-self element obeying the given predicate, if any, wrapped in an Option.
   * Searching starts with this element, then the parent, if applicable, and so on.
   */
  def findAncestorOrSelf(p: E => Boolean): Option[E]

  /**
   * Returns the first found ancestor element obeying the given predicate, if any, wrapped in an Option.
   * Searching starts with the parent of this element, if applicable, then the grandparent, if applicable, and so on.
   */
  def findAncestor(p: E => Boolean): Option[E]
}

object HasParentApi {

  /**
   * The `HasParentApi` as type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] {

    def parentOption(thisElem: E): Option[E]

    def parent(thisElem: E): E

    def ancestorsOrSelf(thisElem: E): immutable.IndexedSeq[E]

    def ancestors(thisElem: E): immutable.IndexedSeq[E]

    def findAncestorOrSelf(thisElem: E, p: E => Boolean): Option[E]

    def findAncestor(thisElem: E, p: E => Boolean): Option[E]
  }
}
