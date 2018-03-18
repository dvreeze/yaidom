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
 * Shorthand for `ClarkElemApi with HasChildNodesApi`. In other words, like `ClarkElemApi` but adding
 * knowledge about child nodes (of any node kind).
 * 
 * Important sub-types are `BackingElemNodeApi` (and its concrete implementations) and `resolved.Elem`.
 *
 * @author Chris de Vreeze
 */
trait ClarkElemNodeApi extends ClarkElemApi with HasChildNodesApi {

  type ThisElem <: ClarkElemNodeApi
}

object ClarkElemNodeApi {

  /**
   * This query API type, restricting Node and Elem to the passed type parameters.
   *
   * @tparam N The node type
   * @tparam E The element type
   */
  type Aux[N, E] = ClarkElemNodeApi { type ThisNode = N; type ThisElem = E }
}
