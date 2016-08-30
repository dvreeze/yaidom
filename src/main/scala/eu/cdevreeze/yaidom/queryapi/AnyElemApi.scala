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

/**
 * Super-trait for all element query API traits, promising a self type.
 *
 * @author Chris de Vreeze
 */
trait AnyElemApi {

  // The 2 type members below are instrumental in implementing F-bounded polymorphism.
  // Note that we need no surrounding cake, and we need no types like ThisApi#ThisElem.

  // For F-bounded polymorphism in DOT, see http://www.cs.uwm.edu/~boyland/fool2012/papers/fool2012_submission_3.pdf.

  /**
   * Upperbound on type ThisElem, which must be restricted to a sub-type of the query API trait in question.
   *
   * Concrete element classes will restrict this type to that element class itself.
   *
   * The purely abstract query API traits (XXXApi) should only restrict this type to a sub-type of the query API trait,
   * without putting any further restrictions on the type. This way these purely abstract traits are easy to use as "generic"
   * element contracts abstracting over multiple backing element implementations (for example, Saxon tiny trees and native
   * yaidom elements) without "infecting" the client code with generics.
   *
   * To a lesser extent, this should also be true for the partially implemented query API traits (XXXLike).
   */
  type ThisElemApi <: AnyElemApi

  /**
   * The ("stable") element type itself. It is not restricted in the query API traits (only indirectly, through ThisElemApi).
   *
   * Concrete element classes will restrict this type to that element class itself.
   */
  type ThisElem <: ThisElemApi

  /**
   * This element itself.
   */
  def thisElem: ThisElem
}
