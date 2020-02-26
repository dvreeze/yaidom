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

package eu.cdevreeze.yaidom.xpath.saxon

import eu.cdevreeze.yaidom.core.EName
import net.sf.saxon.s9api.Processor
import org.scalatest.funsuite.AnyFunSuite

/**
 * JSON XPath test case using JAXP backed by Saxon.
 *
 * @author Chris de Vreeze
 */
class JsonXPathTest extends AnyFunSuite {

  private val processor = new Processor(false)

  private val xpathEvaluatorFactory =
    SaxonJaxpXPathEvaluatorFactory(processor.getUnderlyingConfiguration)

  private val xpathEvaluator: SaxonJaxpXPathEvaluator =
    xpathEvaluatorFactory.newXPathEvaluator()

  test("testSimpleJson") {
    val exprString = """map { "foo": map { "bar": "baz" } } ? foo ? bar"""

    val expr = xpathEvaluator.makeXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsString(expr, None)

    assertResult("baz") {
      result
    }
  }

  test("testSimpleJsonWithArray") {
    val exprString =
      """map { "foo": [ map { "bar": "baz1" }, map { "bar": "baz2" } ] } ? foo ? 2 ? bar"""

    val expr = xpathEvaluator.makeXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsString(expr, None)

    assertResult("baz2") {
      result
    }
  }

  test("testJsonFromFile") {
    // This example data comes from https://www.sitepoint.com/json-server-example/.

    val uri = classOf[JsonXPathTest].getResource("db.json").toURI

    val exprString = s"""array:size(json-doc('$uri') ? clients)"""

    val expr = xpathEvaluator.makeXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBigDecimal(expr, None)

    assertResult(3) {
      result.toInt
    }
  }

  test("testJsonFromFileConvertedToXml") {
    // This example data comes from https://www.sitepoint.com/json-server-example/.

    val uri = classOf[JsonXPathTest].getResource("db.json").toURI

    val exprString = s"""json-to-xml(unparsed-text('$uri'))"""

    val expr = xpathEvaluator.makeXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBackingDocument(expr, None)

    val ns = "http://www.w3.org/2005/xpath-functions"

    assertResult(Set("map", "array", "string", "boolean", "number").map(nm => EName(ns, nm))) {
      result.documentElement.findAllElemsOrSelf.map(_.resolvedName).toSet
    }
  }

  test("testJsonFromFileConvertedToXmlElement") {
    // This example data comes from https://www.sitepoint.com/json-server-example/.

    val uri = classOf[JsonXPathTest].getResource("db.json").toURI

    val exprString = s"""json-to-xml(unparsed-text('$uri'))/*"""

    val expr = xpathEvaluator.makeXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsBackingElem(expr, None)

    val ns = "http://www.w3.org/2005/xpath-functions"

    assertResult(Set("map", "array", "string", "boolean", "number").map(nm => EName(ns, nm))) {
      result.findAllElemsOrSelf.map(_.resolvedName).toSet
    }
  }

  test("testJsonFromFileSerializedAsXml") {
    // This example data comes from https://www.sitepoint.com/json-server-example/.

    val uri = classOf[JsonXPathTest].getResource("db.json").toURI

    val exprString = s"""serialize(json-to-xml(unparsed-text('$uri'), map { "indent": true() }))"""

    val expr = xpathEvaluator.makeXPathExpression(exprString)
    val result = xpathEvaluator.evaluateAsString(expr, None)

    val exprString2 = s"""parse-xml('$result')"""

    val expr2 = xpathEvaluator.makeXPathExpression(exprString2)
    val result2 = xpathEvaluator.evaluateAsBackingDocument(expr2, None)

    val ns = "http://www.w3.org/2005/xpath-functions"

    assertResult(Set("map", "array", "string", "boolean", "number").map(nm => EName(ns, nm))) {
      result2.documentElement.findAllElemsOrSelf.map(_.resolvedName).toSet
    }
  }
}
