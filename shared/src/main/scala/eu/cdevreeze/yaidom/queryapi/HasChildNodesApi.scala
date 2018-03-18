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

import scala.collection.immutable

/**
 * API trait for elements that can be asked for their child nodes, of any node kind.
 *
 * @author Chris de Vreeze
 */
trait HasChildNodesApi extends AnyElemNodeApi {

  type ThisElem <: HasChildNodesApi

  /**
   * Returns all child nodes, of any kind of node (element node, text node etc.).
   */
  def children: immutable.IndexedSeq[ThisNode]
}

object HasChildNodesApi {

  /**
   * This query API type, restricting ThisNode and ThisElem to the type parameters.
   *
   * @tparam N The node self type
   * @tparam E The element self type
   */
  type Aux[N, E] = HasChildNodesApi { type ThisNode = N; type ThisElem = E }
}
