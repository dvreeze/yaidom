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
 * '''Core API''' for element nodes that offer the central `ScopedElemApi with HasChildNodesApi` query API. Each element implementation that
 * knows about expanded names as well as qualified name should directly or indirectly implement this API.
 *
 * This API is directly implemented by elements that know about expanded names and qualified names,
 * but that do not know about their ancestor elements.
 *
 * @author Chris de Vreeze
 */
object ScopedNodes {

  /**
   * Arbitrary node
   */
  trait Node extends ClarkNodes.Node

  /**
   * Potential document child, so an element, processing instruction or comment
   */
  trait CanBeDocumentChild extends Node with ClarkNodes.CanBeDocumentChild

  /**
   * Arbitrary element node, offering the `ScopedElemApi with HasChildNodesApi` element query API
   */
  trait Elem extends CanBeDocumentChild with ClarkNodes.Elem with ScopedElemApi with HasChildNodesApi {

    type ThisElem <: Elem

    type ThisNode >: ThisElem <: Node
  }

  /**
   * Arbitrary text node
   */
  trait Text extends Node with ClarkNodes.Text

  /**
   * Arbitrary comment node
   */
  trait Comment extends CanBeDocumentChild with ClarkNodes.Comment

  /**
   * Arbitrary processing instruction node
   */
  trait ProcessingInstruction extends CanBeDocumentChild with ClarkNodes.ProcessingInstruction

  /**
   * Arbitrary entity reference node
   */
  trait EntityRef extends Node with ClarkNodes.EntityRef

  object Elem {

    /**
     * This query API type, restricting Node and Elem to the passed type parameters.
     *
     * @tparam N The node type
     * @tparam E The element type
     */
    type Aux[N, E] = Elem { type ThisNode = N; type ThisElem = E }
  }
}
