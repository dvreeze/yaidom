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

package eu.cdevreeze.yaidom.resolved

import eu.cdevreeze.yaidom.queryapi.ClarkElemNodeApi
import eu.cdevreeze.yaidom.queryapi.Nodes

/**
 * Abstract node trait hierarchy. It offers a common minimal API for different kinds of nodes that can be converted
 * (without any loss of information) to resolved nodes, such that elements implement the `ClarkElemNodeApi`
 * at minimum.
 *
 * @author Chris de Vreeze
 */
object ResolvedNodes {

  /**
   * Arbitrary node that can be converted to a resolved node.
   */
  trait Node extends Nodes.Node

  /**
   * Arbitrary element node that offers the `ClarkElemNodeApi` query API and know its child nodes. Typical implementations
   * offer the `ScopedElemNodeApi` as well.
   */
  trait Elem extends Node with Nodes.Elem with ClarkElemNodeApi {

    type ThisElem <: Elem

    type ThisNode >: ThisElem <: Node
  }

  object Elem {

    /**
     * This query API type, restricting Node and Elem to the passed type parameters.
     *
     * @tparam N The node type
     * @tparam E The element type
     */
    type Aux[N, E] = Elem { type ThisNode = N; type ThisElem = E }
  }

  /**
   * Arbitrary text node
   */
  trait Text extends Node with Nodes.Text {

    def text: String
  }
}
