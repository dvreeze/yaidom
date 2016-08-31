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
 * Shorthand for `IndexedScopedElemApi with HasParentApi`. In other words, this is an ancestry-aware "scoped element"
 * query API. It is meant to be used for backing element abstractions in "XML dialect support", which are more type-safe
 * element class hierarchies (that themselves offer a yaidom query API, but more type-safe). This enables the same
 * XML dialect support code base to be backed by pluggable backing element implementations.
 *
 * Efficient implementations are possible for indexed elements and Saxon NodeInfo objects (backed by native tiny trees).
 * Saxon-backed elements are not offered by core yaidom, however. Saxon tiny trees are attractive for their low memory
 * footprint.
 *
 * It is possible to offer implementations by combining the partial implementation traits (XXXLike), or by entirely
 * custom and efficient "backend-aware" implementations.
 *
 * It is quite possible that for XML dialect support richer APIs are needed, adding methods like conversions to simple
 * elements, or exposing underlying elements (such as Saxon NodeInfo objects), etc. With a type class such query API
 * extensions are easy to implement.
 *
 * @author Chris de Vreeze
 */
trait BackingElemApi extends IndexedScopedElemApi with HasParentApi {

  type ThisElemApi <: BackingElemApi
}

object BackingElemApi {

  /**
   * This query API type, restricting ThisElem and ThisElemApi to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = BackingElemApi { type ThisElem = E; type ThisElemApi = E }
}
