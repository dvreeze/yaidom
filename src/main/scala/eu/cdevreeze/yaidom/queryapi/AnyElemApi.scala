/*
 * Copyright 2011-2017 Chris de Vreeze
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

/**
 * Super-trait for all element query API traits, promising a self type.
 *
 * Simplicity and consistency of the entire query API are 2 important design considerations. For example, the query
 * API methods themselves use no generics.
 *
 * @author Chris de Vreeze
 */
trait AnyElemApi {

  // The type member below is used for implementing F-bounded polymorphism.
  // Note that we need no surrounding cake, and we need no types like ThisApi#ThisElem.

  // For F-bounded polymorphism in DOT, see http://www.cs.uwm.edu/~boyland/fool2012/papers/fool2012_submission_3.pdf.

  /**
   * The element type itself. It must be restricted to a sub-type of the query API trait in question.
   *
   * Concrete element classes will restrict this type to that element class itself.
   */
  type ThisElem <: AnyElemApi

  /**
   * This element itself.
   */
  def thisElem: ThisElem
}
