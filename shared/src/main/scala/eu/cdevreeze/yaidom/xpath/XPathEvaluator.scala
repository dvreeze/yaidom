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

import scala.collection.immutable

import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi
import eu.cdevreeze.yaidom.queryapi.BackingNodes

/**
 * A simple XPath evaluator abstraction. It has no knowledge about static and dynamic contexts (other than the
 * optional context item), etc. It also has no knowledge about specific implementations, such as Saxon. Moreover,
 * it has no knowledge about XPath versions.
 *
 * This trait looks a bit like the JAXP `XPath` interface. Like the `XPath` interface, this trait does not support the
 * XDM data types that succeeded XPath 1.0. Compared to the JAXP `XPath` interface this trait is more Scala-esque and type-safe.
 *
 * @author Chris de Vreeze
 */
trait XPathEvaluator {

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

  // Note that we have no evaluation methods that return XDM items.
  // Indeed, we have no XDM model in these XPath evaluation abstractions.
  // Hence, we do not have the modeling challenges related to the fact that in XDM everything is a sequence.

  def evaluateAsString(expr: XPathExpression, contextItemOption: Option[ContextItem]): String

  def evaluateAsNode(expr: XPathExpression, contextItemOption: Option[ContextItem]): Node

  def evaluateAsNodeSeq(expr: XPathExpression, contextItemOption: Option[ContextItem]): immutable.IndexedSeq[Node]

  def evaluateAsBackingDocument(expr: XPathExpression, contextItemOption: Option[ContextItem]): BackingDocumentApi

  def evaluateAsBackingElem(expr: XPathExpression, contextItemOption: Option[ContextItem]): BackingNodes.Elem

  def evaluateAsBackingElemSeq(expr: XPathExpression, contextItemOption: Option[ContextItem]): immutable.IndexedSeq[BackingNodes.Elem]

  def evaluateAsBigDecimal(expr: XPathExpression, contextItemOption: Option[ContextItem]): BigDecimal

  def evaluateAsBoolean(expr: XPathExpression, contextItemOption: Option[ContextItem]): Boolean

  /**
   * Creates an XPathExpression from the given expression string. Typically (but not necessarily) "compiles" the XPath string.
   * Make sure to pass only XPath strings for which all needed namespace bindings are known to the XPath evaluator.
   */
  def makeXPathExpression(xPathString: String): XPathExpression
}

object XPathEvaluator {

  type Aux[E, N, C] = XPathEvaluator {
    type XPathExpression = E
    type Node = N
    type ContextItem = C
  }
}
