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

package eu.cdevreeze.yaidom.queryapi2

/**
 * Super-trait for all element query API traits, promising a self type.
 *
 * @author Chris de Vreeze
 */
trait AnyElemApi {

  // The 2 type members below are instrumental in implementing F-bounded polymorphism.
  // Type ThisElemApi must always be a sub-type of the API trait or concrete element type in question.
  // Type ThisElem (the "real self type") must be a sub-type of ThisElemApi everywhere.
  // Note that we need no surrounding cake, and we need no types like ThisApi#ThisElem.

  // For F-bounded polymorphism in DOT, see http://www.cs.uwm.edu/~boyland/fool2012/papers/fool2012_submission_3.pdf.

  type ThisElemApi <: AnyElemApi

  type ThisElem <: ThisElemApi

  def self: ThisElem
}
