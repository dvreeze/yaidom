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

package eu.cdevreeze.yaidom.xpath

/**
 * A simple XPath evaluator factory abstraction. The implementation may or may not be thread-safe.
 *
 * @author Chris de Vreeze
 */
trait XPathEvaluatorFactory {

  /**
   * XPath expression. Typically (but not necessarily) a "compiled" one.
   */
  type XPathExpression

  /**
   * The DOM node type in (DOM) evaluation results.
   */
  type Node

  /**
   * The context item type.
   */
  type ContextItem

  def newXPathEvaluator(): XPathEvaluator.Aux[XPathExpression, Node, ContextItem]
}

object XPathEvaluatorFactory {

  type Aux[E, N, C] = XPathEvaluatorFactory {
    type XPathExpression = E
    type Node = N
    type ContextItem = C
  }
}
