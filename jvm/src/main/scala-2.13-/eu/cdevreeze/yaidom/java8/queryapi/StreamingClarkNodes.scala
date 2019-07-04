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

package eu.cdevreeze.yaidom.java8.queryapi

/**
 * Equivalent of `ClarkNodes`.
 *
 * @author Chris de Vreeze
 */
object StreamingClarkNodes {

  /**
   * Arbitrary node
   */
  trait Node extends StreamingNodes.Node

  /**
   * Potential document child, so an element, processing instruction or comment
   */
  trait CanBeDocumentChild extends Node with StreamingNodes.CanBeDocumentChild

  /**
   * Arbitrary element node, offering the `StreamingClarkElemApi with StreamingHasChildNodesApi` element query API
   */
  trait Elem[N, E <: N with Elem[N, E]]
    extends CanBeDocumentChild with StreamingNodes.Elem with StreamingClarkElemApi[E] with StreamingHasChildNodesApi[N, E]

  /**
   * Arbitrary text node
   */
  trait Text extends Node with StreamingNodes.Text

  /**
   * Arbitrary comment node
   */
  trait Comment extends CanBeDocumentChild with StreamingNodes.Comment

  /**
   * Arbitrary processing instruction node
   */
  trait ProcessingInstruction extends CanBeDocumentChild with StreamingNodes.ProcessingInstruction

  /**
   * Arbitrary entity reference node
   */
  trait EntityRef extends Node with StreamingNodes.EntityRef
}