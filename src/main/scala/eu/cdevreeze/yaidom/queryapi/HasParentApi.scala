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
   * Returns all ancestor elements or self
   */
  def ancestorsOrSelf: immutable.IndexedSeq[E]

  /**
   * Returns `ancestorsOrSelf.drop(1)`
   */
  def ancestors: immutable.IndexedSeq[E]

  /**
   * Returns the first found ancestor-or-self element obeying the given predicate, if any, wrapped in an Option
   */
  def findAncestorOrSelf(p: E => Boolean): Option[E]

  /**
   * Returns the first found ancestor element obeying the given predicate, if any, wrapped in an Option
   */
  def findAncestor(p: E => Boolean): Option[E]
}
