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

package eu.cdevreeze.yaidom.utils

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi.withEName
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple
import org.scalatest.funsuite.AnyFunSuite

/**
 * ClarkElem test case.
 *
 * @author Chris de Vreeze
 */
class ClarkElemTest extends AnyFunSuite {

  private val docParser = DocumentParserUsingSax.newInstance

  test("testCreateXbrlInstance") {
    val instance = createInstance()

    import scope._

    assertResult(QName("xbrl").res) {
      instance.resolvedName
    }
    assertResult(Some("1000")) {
      instance.findElem(withEName(QName("gaap:CashAndCashEquivalents").res)).map(_.text)
    }

    val uri = classOf[ClarkElemTest].getResource("sample-xbrl-instance.xml").toURI

    val parsedInstance = ClarkNode.Elem.from(docParser.parse(uri).documentElement)

    val filteredInstance = parsedInstance.minusAttribute(QName("xsi:schemaLocation").res) transformChildElemsToNodeSeq {
      case e@ClarkNode.Elem(EName(_, "schemaRef"), _, _) =>
        Vector(e)
      case e@ClarkNode.Elem(EName(_, "context"), _, _) if e.attributeOption(EName("id")).contains("I-2007") =>
        Vector(e)
      case e@ClarkNode.Elem(EName(_, "unit"), _, _) if e.attributeOption(EName("id")).contains("U-Monetary") =>
        Vector(e)
      case e@ClarkNode.Elem(EName(_, "CashAndCashEquivalents"), _, _) if e.attributeOption(EName("contextRef")).contains("I-2007") &&
        e.attributeOption(EName("unitRef")).contains("U-Monetary") =>

        Vector(e)
      case e =>
        Vector()
    }

    assertResult(resolved.Elem.from(filteredInstance).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem.from(instance).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }
  }

  test("testConvertXbrlInstance") {
    val uri = classOf[ClarkElemTest].getResource("sample-xbrl-instance.xml").toURI

    val parsedInstance = docParser.parse(uri).documentElement

    val clarkInstance = ClarkNode.Elem.from(parsedInstance)

    val adaptedScope =
      scope.withoutDefaultNamespace ++ Scope.from("xbrli" -> "http://www.xbrl.org/2003/instance")

    assertResult(adaptedScope) {
      adaptedScope.withoutDefaultNamespace.makeInvertible
    }

    val convertedInstance = simple.Elem.from(clarkInstance, adaptedScope)

    assertResult(resolved.Elem.from(parsedInstance)) {
      resolved.Elem.from(clarkInstance)
    }

    assertResult(resolved.Elem.from(convertedInstance)) {
      resolved.Elem.from(clarkInstance)
    }
  }

  private def createContext(): ClarkNode.Elem = {
    import ClarkNode.Node._
    import scope._

    val identifier =
      textElem(QName("identifier").res, Vector(EName("scheme") -> "http://www.sec.gov/CIK"), "1234567890")

    val segment =
      emptyElem(QName("segment").res)
        .plusChildren(
          Vector(
            textElem(QName("xbrldi:explicitMember").res, Vector(EName("dimension") -> "gaap:EntityAxis"), "gaap:ABCCompanyDomain"),
            textElem(QName("xbrldi:explicitMember").res, Vector(EName("dimension") -> "gaap:BusinessSegmentAxis"), "gaap:ConsolidatedGroupDomain"),
            textElem(QName("xbrldi:explicitMember").res, Vector(EName("dimension") -> "gaap:VerificationAxis"), "gaap:UnqualifiedOpinionMember"),
            textElem(QName("xbrldi:explicitMember").res, Vector(EName("dimension") -> "gaap:PremiseAxis"), "gaap:ActualMember"),
            textElem(QName("xbrldi:explicitMember").res, Vector(EName("dimension") -> "gaap:ReportDateAxis"), "gaap:ReportedAsOfMarch182008Member")))

    val entity = elem(QName("entity").res, Vector(identifier, segment))

    val period =
      emptyElem(QName("period").res)
        .plusChild(textElem(QName("instant").res, "2007-12-31"))

    emptyElem(QName("context").res)
      .plusAttribute(EName("id"), "I-2007")
      .plusChildren(Vector(entity, period))
  }

  private def createUnit(): ClarkNode.Elem = {
    import ClarkNode.Node._
    import scope._

    // Mind the prefix 'iso427' below. If we convert this to a simple Elem,
    // we need a Scope that includes that prefix!

    emptyElem(QName("unit").res)
      .plusAttribute(EName("id"), "U-Monetary")
      .plusChild(
        textElem(QName("measure").res, "iso4217:USD"))
  }

  private def createFact(): ClarkNode.Elem = {
    import ClarkNode.Node._
    import scope._

    textElem(QName("gaap:CashAndCashEquivalents").res, "1000")
      .plusAttribute(EName("id"), "Item-01")
      .plusAttribute(EName("contextRef"), "I-2007")
      .plusAttribute(EName("unitRef"), "U-Monetary")
      .plusAttribute(EName("decimals"), "INF")
  }

  private def createInstance(): ClarkNode.Elem = {
    import ClarkNode.Node._
    import scope._

    val schemaRef =
      emptyElem(QName("link:schemaRef").res)
        .plusAttribute(QName("xlink:type").res, "simple")
        .plusAttribute(QName("xlink:href").res, "gaap.xsd")

    emptyElem(QName("xbrl").res)
      .plusChild(schemaRef)
      .plusChild(createContext())
      .plusChild(createUnit())
      .plusChild(createFact())
  }

  private val scope = Scope.from(
    "" -> "http://www.xbrl.org/2003/instance",
    "xlink" -> "http://www.w3.org/1999/xlink",
    "link" -> "http://www.xbrl.org/2003/linkbase",
    "gaap" -> "http://xasb.org/gaap",
    "xsi" -> "http://www.w3.org/2001/XMLSchema-instance",
    "iso4217" -> "http://www.xbrl.org/2003/iso4217",
    "xbrldi" -> "http://xbrl.org/2006/xbrldi")
}
