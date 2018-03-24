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

import org.scalajs.dom.experimental.domparser.DOMParser
import org.scalajs.dom.experimental.domparser.SupportedType
import org.scalatest.FunSuite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom
import org.scalajs.dom.{ raw => sjsdom }

/**
 * XPath test case for JS-DOM.
 *
 * @author Chris de Vreeze
 */
class XPathTest extends FunSuite {

  private val XbrliNs = "http://www.xbrl.org/2003/instance"

  private val doc: yaidom.jsdom.JsDomDocument = getDocument()

  private val rootElem: yaidom.jsdom.JsDomElem = {
    doc.documentElement
      .ensuring(_.wrappedNode != null, "Corrupt document element (missing underlying node)")
      .ensuring(_.resolvedName.namespaceUriOption.contains(XbrliNs))
  }

  private val xpathEvaluator =
    JsDomXPathEvaluatorFactoryBuilder(doc.wrappedDocument)
      .withScope(rootElem.scope ++ Scope.from("ns" -> "http://www.xbrl.org/2003/instance"))
      .build()
      .newXPathEvaluator()

  test("testVerySimpleXPathWithoutContextItem") {
    val exprString = "string(2 * 3)"

    val result = xpathEvaluator.evaluateAsString(exprString, None)

    assertResult("6") {
      result
    }
  }

  test("testVerySimpleXPathWithContextItem") {
    val exprString = "count(//*)"

    val result = xpathEvaluator.evaluateAsBigDecimal(exprString, Some(rootElem.wrappedNode))

    assertResult(true) {
      result.intValue >= 10
    }
  }

  test("testSimpleXPathWithContextItem") {
    val exprString = "//*[local-name(.) = 'explicitMember']"

    val result = xpathEvaluator.evaluateAsNode(exprString, Some(rootElem.wrappedNode))

    assertResult("gaap:ABCCompanyDomain") {
      yaidom.jsdom.JsDomElem(result.asInstanceOf[sjsdom.Element]).text.trim
    }
  }

  test("testVerboseNonNamespaceAwareNodeXPath") {
    val exprString =
      "//*[local-name(.) = 'context'][1]/*[local-name(.) = 'entity'][1]" +
        "/*[local-name(.) = 'segment'][1]/*[local-name(.) = 'explicitMember'][1]"

    val result = xpathEvaluator.evaluateAsNode(exprString, Some(rootElem.wrappedNode))

    assertResult("gaap:ABCCompanyDomain") {
      yaidom.jsdom.JsDomElem(result.asInstanceOf[sjsdom.Element]).text.trim
    }
  }

  // Ignored, because function namespace-uri has not yet been implemented
  ignore("testVerboseNamespaceAwareNodeXPath") {
    val exprString =
      "//*[local-name(.) = 'context' and namespace-uri(.) = 'http://www.xbrl.org/2003/instance'][1]" +
        "/*[local-name(.) = 'entity' and namespace-uri(.) = 'http://www.xbrl.org/2003/instance'][1]" +
        "/*[local-name(.) = 'segment' and namespace-uri(.) = 'http://www.xbrl.org/2003/instance'][1]" +
        "/*[local-name(.) = 'explicitMember' and namespace-uri(.) = 'http://www.xbrl.org/2003/instance'][1]"

    val result = xpathEvaluator.evaluateAsNode(exprString, Some(rootElem.wrappedNode))

    assertResult("gaap:ABCCompanyDomain") {
      yaidom.jsdom.JsDomElem(result.asInstanceOf[sjsdom.Element]).text.trim
    }
  }

  test("testSimpleNodeXPath") {
    val exprString = "//xbrli:context[1]/xbrli:entity/xbrli:segment/xbrldi:explicitMember[1]"

    val result = xpathEvaluator.evaluateAsNode(exprString, Some(rootElem.wrappedNode))

    assertResult("gaap:ABCCompanyDomain") {
      yaidom.jsdom.JsDomElem(result.asInstanceOf[sjsdom.Element]).text.trim
    }
  }

  test("testSimpleNodeSeqXPath") {
    val exprString = "//xbrli:context/xbrli:entity/xbrli:segment/xbrldi:explicitMember"

    val result = xpathEvaluator.evaluateAsNodeSeq(exprString, Some(rootElem.wrappedNode))

    assertResult(true) {
      result.nonEmpty
    }
  }

  test("testSimpleBackingElemXPath") {
    val exprString = "//xbrli:context[1]/xbrli:entity/xbrli:segment/xbrldi:explicitMember[1]"

    val resultElem = xpathEvaluator.evaluateAsBackingElem(exprString, Some(rootElem.wrappedNode))

    assertResult("gaap:ABCCompanyDomain") {
      resultElem.text.trim
    }
  }

  test("testSimpleBackingElemSeqXPath") {
    val exprString = "//xbrli:context/xbrli:entity/xbrli:segment/xbrldi:explicitMember"

    val resultElems = xpathEvaluator.evaluateAsBackingElemSeq(exprString, Some(rootElem.wrappedNode))

    assertResult(true) {
      resultElems.nonEmpty
    }
  }

  test("testYaidomQueryOnXPathBackingElemResults") {
    val exprString = "//xbrli:context/xbrli:entity/xbrli:segment/xbrldi:explicitMember"

    val resultElems = xpathEvaluator.evaluateAsBackingElemSeq(exprString, Some(rootElem.wrappedNode))

    // Use yaidom query API on results

    assertResult(true) {
      val someDimQNames =
        Set(QName("gaap:EntityAxis"), QName("gaap:VerificationAxis"), QName("gaap:PremiseAxis"), QName("gaap:ReportDateAxis"))

      val someDimENames = someDimQNames.map(qn => rootElem.scope.resolveQNameOption(qn).get)

      val foundDimensions =
        resultElems.flatMap(_.attributeAsResolvedQNameOption(EName("dimension"))).toSet

      someDimENames.subsetOf(foundDimensions)
    }

    // The Paths are not lost!

    val resultElemPaths = resultElems.map(_.path)

    assertResult(Set(List("context", "entity", "segment", "explicitMember"))) {
      resultElemPaths.map(_.entries.map(_.elementName.localPart)).toSet
    }
    assertResult(Set(EName(XbrliNs, "xbrl"))) {
      resultElems.map(_.rootElem.resolvedName).toSet
    }

    assertResult(resultElems) {
      resultElems.map(e => e.rootElem.getElemOrSelfByPath(e.path))
    }
  }

  // Below, we adapted the XBRL instance to not use the default namespace, but use the exact same
  // namespace prefix as in the XPath expressions. Otherwise the namespace-aware XPath queries do not work!
  // Is this a bug in js-dom? Or do we have to set namespace-awareness somewhere for XPath evaluation?

  private def xmlString = """<?xml version="1.0" encoding="utf-8"?>
<!-- Created by Charles Hoffman, CPA, 2008-03-27 -->
<!-- See http://www.xbrlsite.com/examples/comprehensiveexample/2008-04-18/sample-Instance-Proof.xml. -->
<xbrli:xbrl xmlns='http://www.xbrl.org/2003/instance' xmlns:xbrli='http://www.xbrl.org/2003/instance'
	xmlns:link='http://www.xbrl.org/2003/linkbase' xmlns:xlink='http://www.w3.org/1999/xlink'
	xmlns:xs='http://www.w3.org/2001/XMLSchema'
	xmlns:xsi='http://www.w3.org/2001/XMLSchema-instance' xmlns:xbrldi="http://xbrl.org/2006/xbrldi"
	xmlns:iso4217='http://www.xbrl.org/2003/iso4217' xmlns:gaap='http://xasb.org/gaap'
	xmlns:company='http://www.example.com/company' xsi:schemaLocation="">


	<!-- Use to turn on/off formula validation. Copy OUTSIDE of CDATA section
		to turn on <![CDATA[ ]]> -->


	<link:schemaRef xlink:type="simple" xlink:href="gaap.xsd" />

	<link:linkbaseRef xlink:type="simple" xlink:href="gaap-formula.xml"
		xlink:arcrole="http://www.w3.org/1999/xlink/properties/linkbase" />


	<!-- General Contexts -->
	<xbrli:context id='I-2007'>
		<!-- Consolidated Group, Current Period, As of -->
		<xbrli:entity>
			<xbrli:identifier scheme='http://www.sec.gov/CIK'>1234567890</xbrli:identifier>
			<xbrli:segment>
				<xbrldi:explicitMember dimension="gaap:EntityAxis">gaap:ABCCompanyDomain
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:BusinessSegmentAxis">gaap:ConsolidatedGroupDomain
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:VerificationAxis">gaap:UnqualifiedOpinionMember
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:PremiseAxis">gaap:ActualMember
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:ReportDateAxis">gaap:ReportedAsOfMarch182008Member
				</xbrldi:explicitMember>
			</xbrli:segment>
		</xbrli:entity>
		<xbrli:period>
			<xbrli:instant>2007-12-31</xbrli:instant>
		</xbrli:period>
	</xbrli:context>
	<xbrli:context id='I-2006'>
		<!-- Consolodated Group, Prior Period, As of -->
		<xbrli:entity>
			<xbrli:identifier scheme='http://www.sec.gov/CIK'>1234567890</xbrli:identifier>
			<xbrli:segment>
				<xbrldi:explicitMember dimension="gaap:EntityAxis">gaap:ABCCompanyDomain
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:BusinessSegmentAxis">gaap:ConsolidatedGroupDomain
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:VerificationAxis">gaap:UnqualifiedOpinionMember
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:PremiseAxis">gaap:ActualMember
				</xbrldi:explicitMember>
				<xbrldi:explicitMember dimension="gaap:ReportDateAxis">gaap:ReportedAsOfMarch182008Member
				</xbrldi:explicitMember>
			</xbrli:segment>
		</xbrli:entity>
		<xbrli:period>
			<xbrli:instant>2006-12-31</xbrli:instant>
		</xbrli:period>
	</xbrli:context>

	<!-- Units -->
	<xbrli:unit id='U-Monetary'>
		<!-- US Dollars -->
		<xbrli:measure>iso4217:USD</xbrli:measure>
	</xbrli:unit>
	<xbrli:unit id="U-Shares">
		<!-- Shares -->
		<xbrli:measure>shares</xbrli:measure>
	</xbrli:unit>
	<xbrli:unit id="U-Pure">
		<!-- Pure; no measure, pure number -->
		<xbrli:measure>pure</xbrli:measure>
	</xbrli:unit>


	<!-- Balance Sheet -->
	<gaap:CashAndCashEquivalents id="Item-01"
		contextRef="I-2007" unitRef="U-Monetary" decimals="INF">1000
	</gaap:CashAndCashEquivalents>
	<gaap:ReceivablesNetCurrent id="Item-02"
		contextRef="I-2007" unitRef="U-Monetary" decimals="INF">1000
	</gaap:ReceivablesNetCurrent>

	<gaap:CashAndCashEquivalents contextRef="I-2006"
		unitRef="U-Monetary" decimals="INF">1000</gaap:CashAndCashEquivalents>
	<gaap:ReceivablesNetCurrent contextRef="I-2006"
		unitRef="U-Monetary" decimals="INF">1000</gaap:ReceivablesNetCurrent>

</xbrli:xbrl>
""".trim

  private def getDocument(): yaidom.jsdom.JsDomDocument = {
    val db = new DOMParser()

    val domDoc: yaidom.jsdom.JsDomDocument =
      yaidom.jsdom.JsDomDocument.wrapDocument(
        db.parseFromString(xmlString, SupportedType.`text/xml`))

    domDoc.ensuring(_.documentElement.findAllElemsOrSelf.size >= 10)
  }
}
