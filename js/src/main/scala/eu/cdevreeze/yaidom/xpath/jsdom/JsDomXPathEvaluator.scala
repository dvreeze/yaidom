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

package eu.cdevreeze.yaidom.xpath.jsdom

import scala.collection.immutable

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.xpath.XPathEvaluator
import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi
import eu.cdevreeze.yaidom.queryapi.BackingElemNodeApi
import org.scalajs.dom.{ raw => sjsdom }

/**
 * XPathEvaluator for JS-DOM XML (not HTML). It does not support compilation of XPath expressions and re-use of the compilation results.
 * Therefore, method `makeXPathExpression` is a no-op, returning the same expression string.
 *
 * See for example https://developer.mozilla.org/en-US/docs/Web/JavaScript/Introduction_to_using_XPath_in_JavaScript.
 *
 * @author Chris de Vreeze
 */
// scalastyle:off null
final class JsDomXPathEvaluator(val doc: sjsdom.Document, val namespaceResolverOption: Option[sjsdom.XPathNSResolver]) extends XPathEvaluator {

  type XPathExpression = String

  type Node = sjsdom.Node

  type ContextItem = sjsdom.Node

  def evaluateAsString(expr: XPathExpression, contextItemOption: Option[ContextItem]): String = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    val contextItem = contextItemOption.getOrElse(doc)

    val xpathResult = doc.evaluate(expr, contextItem, namespaceResolverOption.orNull, sjsdom.XPathResult.STRING_TYPE, null)
    xpathResult.stringValue
  }

  def evaluateAsNode(expr: XPathExpression, contextItemOption: Option[ContextItem]): Node = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    val contextItem = contextItemOption.getOrElse(doc)

    val xpathResult = doc.evaluate(expr, contextItem, namespaceResolverOption.orNull, sjsdom.XPathResult.FIRST_ORDERED_NODE_TYPE, null)
    xpathResult.singleNodeValue
  }

  def evaluateAsNodeSeq(expr: XPathExpression, contextItemOption: Option[ContextItem]): immutable.IndexedSeq[Node] = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    val contextItem = contextItemOption.getOrElse(doc)

    val xpathResult = doc.evaluate(expr, contextItem, namespaceResolverOption.orNull, sjsdom.XPathResult.ORDERED_NODE_SNAPSHOT_TYPE, null)

    (0 until xpathResult.snapshotLength).toIndexedSeq.map(i => xpathResult.snapshotItem((i)))
  }

  def evaluateAsBackingDocument(expr: XPathExpression, contextItemOption: Option[ContextItem]): BackingDocumentApi = {
    val nodeResult = evaluateAsNode(expr, contextItemOption)

    val docResult: sjsdom.Document = nodeResult match {
      case d: sjsdom.Document => d
      case n                  => sys.error(s"Expected document result but got ${n.getClass} instead")
    }

    yaidom.jsdom.JsDomDocument.wrapDocument(docResult)
  }

  def evaluateAsBackingElem(expr: XPathExpression, contextItemOption: Option[ContextItem]): BackingElemNodeApi = {
    val nodeResult = evaluateAsNode(expr, contextItemOption)

    val elemResult: sjsdom.Element = nodeResult match {
      case d: sjsdom.Document => d.documentElement
      case e: sjsdom.Element  => e
      case n                  => sys.error(s"Expected element or document result but got ${n.getClass} instead")
    }

    yaidom.jsdom.JsDomElem(elemResult)
  }

  def evaluateAsBackingElemSeq(expr: XPathExpression, contextItemOption: Option[ContextItem]): immutable.IndexedSeq[BackingElemNodeApi] = {
    val nodeSeqResult = evaluateAsNodeSeq(expr, contextItemOption)

    nodeSeqResult map { n =>
      val elemResult: sjsdom.Element = n match {
        case d: sjsdom.Document => d.documentElement
        case e: sjsdom.Element  => e
        case n                  => sys.error(s"Expected element or document result but got ${n.getClass} instead")
      }

      yaidom.jsdom.JsDomElem(elemResult)
    }
  }

  def evaluateAsBigDecimal(expr: XPathExpression, contextItemOption: Option[ContextItem]): BigDecimal = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    val contextItem = contextItemOption.getOrElse(doc)

    val xpathResult = doc.evaluate(expr, contextItem, namespaceResolverOption.orNull, sjsdom.XPathResult.NUMBER_TYPE, null)
    BigDecimal(xpathResult.numberValue)
  }

  def evaluateAsBoolean(expr: XPathExpression, contextItemOption: Option[ContextItem]): Boolean = {
    require(!contextItemOption.contains(null), s"Null context not allowed. Use empty Option instead.")

    val contextItem = contextItemOption.getOrElse(doc)

    val xpathResult = doc.evaluate(expr, contextItem, namespaceResolverOption.orNull, sjsdom.XPathResult.BOOLEAN_TYPE, null)
    xpathResult.booleanValue
  }

  def makeXPathExpression(xpathString: String): XPathExpression = {
    xpathString
  }
}
