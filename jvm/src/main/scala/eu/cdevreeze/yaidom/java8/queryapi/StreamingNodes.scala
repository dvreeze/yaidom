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
 * Equivalent of `Nodes`.
 *
 * @author Chris de Vreeze
 */
object StreamingNodes {

  /**
   * Arbitrary node
   */
  trait Node

  /**
   * Potential document child, so an element, processing instruction or comment
   */
  trait CanBeDocumentChild extends Node

  /**
   * Arbitrary element node
   */
  trait Elem extends CanBeDocumentChild

  /**
   * Arbitrary text node
   */
  trait Text extends Node {

    def text: String
  }

  /**
   * Arbitrary comment node
   */
  trait Comment extends CanBeDocumentChild {

    def text: String
  }

  /**
   * Arbitrary processing instruction node
   */
  trait ProcessingInstruction extends CanBeDocumentChild {

    def target: String

    def data: String
  }

  /**
   * Arbitrary entity reference node
   */
  trait EntityRef extends Node {

    def entity: String
  }
}
