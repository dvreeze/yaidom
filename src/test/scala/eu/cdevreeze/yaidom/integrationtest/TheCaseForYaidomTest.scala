/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.integrationtest

import java.io.ByteArrayInputStream

import scala.Vector
import scala.xml.NodeSeq.seqToNodeSeq

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.scalatest.FunSuite
import org.w3c.dom.DOMException
import org.xml.sax.SAXParseException

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.NodeBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.SAXParserFactory

/**
 * Test case showing why preferring yaidom over the standard Scala XML library makes sense.
 *
 * Yaidom does not like non-namespace-well-formed XML, unlike Scala XML. This is one of
 * the reasons that yaidom is a better precision tool for XML processing than Scala XML.
 * Yaidom does not try to be as concise as possible, but precise namespace support is
 * considered more important. Yaidom is (now and always) meant to be a good basis for the
 * kind of XML processing performed in XBRL processing and validation.
 *
 * This test case also shows how yaidom's namespace support is consistent with the existence
 * of multiple (native) element implementations, such as simple elements, resolved elements
 * (which represent James Clark's minimal element representation) and element builders
 * (which are not XML representations themselves, but make creation of XML possible without
 * passing in-scope namespaces around).
 *
 * In short, if you care about the test results of this test, showing some differences
 * between yaidom and Scala XML with respect to namespaces, consider using yaidom.
 * If not, by all means, use Scala XML.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class TheCaseForYaidomTest extends FunSuite {

  /**
   * Our XML, which is not namespace-well-formed.
   * See http://stackoverflow.com/questions/14871752/is-xml-document-with-undeclared-prefix-well-formed.
   */
  private val wrongXml =
    """<root><prefix:element></prefix:element></root>"""

  private val wrongXml2 =
    """<link:linkbaseRef xlink:arcrole="http://www.w3.org/1999/xlink/properties/linkbase" xlink:href="my-lab-en.xml" xlink:role="http://www.xbrl.org/2003/role/labelLinkbaseRef" xlink:type="simple"/>"""

  // Some test methods for the first non-namespace-well-formed XML.
  // They show that yaidom fails faster on non-namespace-well-formed XML than Scala XML.

  /**
   * Tries to create a DOM tree for the (wrong) XML. It fails, as expected.
   */
  test("testTryCreatingDomForWrongXml") {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val d = db.newDocument()

    val rootElm = d.createElementNS(null, "root")
    d.appendChild(rootElm)

    intercept[DOMException] {
      val elmElm = d.createElementNS(null, "prefix:element")
      rootElm.appendChild(elmElm)
    }
  }

  /**
   * Tries to parse the XML into a yaidom Elem (via SAX), but fails, as expected.
   */
  test("testTryParsingWrongXmlViaSax") {
    val parser = DocumentParserUsingSax.newInstance

    intercept[SAXParseException] {
      parser.parse(new ByteArrayInputStream(wrongXml.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the XML into a yaidom Elem (via StAX), but fails, as expected.
   */
  test("testTryParsingWrongXmlViaStax") {
    val parser = DocumentParserUsingStax.newInstance

    intercept[Exception] {
      parser.parse(new ByteArrayInputStream(wrongXml.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the XML into a yaidom Elem (via DOM), but fails, as expected.
   */
  test("testTryParsingWrongXmlViaDom") {
    val parser = DocumentParserUsingDom.newInstance

    intercept[SAXParseException] {
      parser.parse(new ByteArrayInputStream(wrongXml.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the XML into a yaidom Elem (via DOM LS), but fails, as expected.
   */
  test("testTryParsingWrongXmlViaDomLS") {
    val parser = DocumentParserUsingDomLS.newInstance

    intercept[RuntimeException] {
      parser.parse(new ByteArrayInputStream(wrongXml.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the XML into a yaidom Elem again, but fails, as expected.
   * This time the underlying parser is not namespace-aware, but still yaidom Elem
   * creation fails.
   */
  test("testTryParsingWrongXmlAgain") {
    val spf = SAXParserFactory.newInstance
    spf.setNamespaceAware(false)
    val parser = DocumentParserUsingSax.newInstance(spf)

    // SAX parsing succeeds, but yaidom Elem creation does not
    intercept[RuntimeException] {
      parser.parse(new ByteArrayInputStream(wrongXml.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the XML using scala.xml.XML, but succeeds, unexpectedly.
   *
   * A fatal error is reported, but the exception is "eaten", returning a scala.xml.Elem, which is
   * not namespace-well-formed.
   */
  test("testTryParsingWrongXmlUsingScalaXml") {
    val scalaElem = scala.xml.XML.load(new ByteArrayInputStream(wrongXml.getBytes("UTF-8")))

    // Why null? Why not an Option?
    assertResult(null) {
      scalaElem.prefix
    }
    assertResult("root") {
      scalaElem.label
    }

    assertResult("prefix") {
      scalaElem.child.head.prefix
    }
    assertResult("element") {
      scalaElem.child.head.label
    }
  }

  /**
   * Creates an ElemBuilder for the XML, which succeeds. After all, ElemBuilders are
   * only builders of XML, and do not claim to be valid namespace-well-formed XML
   * themselves. Indeed, ElemBuilders can only be queried for qualified names, but not
   * for expanded names.
   */
  test("testCreateElemBuilderForWrongXml") {
    import NodeBuilder._

    val elemBuilder =
      elem(QName("root"), Vector(emptyElem(QName("prefix:element"))))

    assertResult(false) {
      elemBuilder.canBuild(Scope.Empty)
    }

    val ns = "http://namespace"

    assertResult(true) {
      elemBuilder.canBuild(Scope.from("prefix" -> ns))
    }
  }

  /**
   * Tries to create an Elem for the XML, without passing the
   * correct parent scope. This fails, as expected.
   */
  test("testTryCreatingElemForWrongXml") {
    import Node._

    intercept[RuntimeException] {
      elem(
        QName("root"),
        Scope.Empty,
        Vector(emptyElem(QName("prefix:element"), Scope.Empty)))
    }
  }

  /**
   * Tries to convert an ElemBuilder for the XML into an Elem, this time passing the
   * correct parent scope. This succeeds, as expected, and so do the subsequent queries.
   */
  test("testQueryElemForFixedWrongXml") {
    import NodeBuilder._

    val elemBuilder =
      elem(QName("root"), Vector(emptyElem(QName("prefix:element"))))

    val ns = "http://namespace"

    val elm = elemBuilder.build(Scope.from("prefix" -> ns))

    val elemENames = elm.findAllElemsOrSelf.map(_.resolvedName)

    assertResult(List(EName("root"), EName(ns, "element"))) {
      elemENames
    }

    assertResult(elm.findAllElemsOrSelf) {
      elm.filterElemsOrSelf(e => e.localName == "root" || e.localName == "element")
    }

    assertResult(List(elm)) {
      elm.filterElemsOrSelf(withEName(None, "root"))
    }
    assertResult(Nil) {
      elm.filterElemsOrSelf(withEName(ns, "root"))
    }

    assertResult(elm.findAllElems) {
      elm.filterElemsOrSelf(withEName(ns, "element"))
    }
    assertResult(Nil) {
      elm.filterElemsOrSelf(withEName(None, "element"))
    }

    assertResult(elm.findAllElemsOrSelf) {
      elm.filterElemsOrSelf(e => e.resolvedName == EName("root") || e.resolvedName == EName(ns, "element"))
    }
  }

  /**
   * Tries to create a Scala XML Elem for the XML, which succeeds (although it should not succeed).
   */
  test("testCreateScalaXmlElemForWrongXml") {
    val scalaElem =
      <root><prefix:element></prefix:element></root>

    assertResult(true) {
      scalaElem.child.size == 1
    }
  }

  /**
   * Queries the Scala XML Elem. Here we see that Scala XML happily allows for querying
   * a non-namespace-well-formed XML tree. It allows for querying the child element,
   * returning the correct prefix, but returning null for the namespace. Indeed, the
   * namespace is absent, due to the non-namespace-well-formedness, but this is not
   * "XML querying", if we consider namespace-well-formedness essential for XML.
   */
  test("testQueryScalaXmlElemForWrongXml") {
    val scalaElem =
      <root><prefix:element></prefix:element></root>

    assertResult("root") {
      scalaElem.label
    }
    assertResult(null) {
      scalaElem.prefix
    }
    assertResult(null) {
      scalaElem.namespace
    }

    val child = scalaElem.child.head.asInstanceOf[scala.xml.Elem]

    assertResult("element") {
      child.label
    }
    assertResult("prefix") {
      child.prefix
    }
    assertResult(null) {
      child.namespace
    }
  }

  /**
   * Tries to query a wrapper around the Scala XML Elem, which only fails once an incorrect
   * Scope is used under the hood.
   */
  test("testQueryWrappedScalaXmlElemForWrongXml") {
    val scalaElem =
      <root><prefix:element></prefix:element></root>
    val elm = ScalaXmlElem(scalaElem)

    assertResult(QName("root")) {
      elm.qname
    }
    assertResult(EName("root")) {
      elm.resolvedName
    }

    val childElem = elm.findAllChildElems.head

    assertResult(QName("prefix", "element")) {
      childElem.qname
    }
    intercept[RuntimeException] {
      childElem.resolvedName
    }
  }

  /**
   * Tries to convert the Scala XML Elem to a yaidom Elem, which fails, as expected.
   */
  test("testTryToConvertScalaXmlElemForWrongXml") {
    val scalaElem =
      <root><prefix:element></prefix:element></root>

    intercept[RuntimeException] {
      ScalaXmlConversions.convertToElem(scalaElem)
    }
  }

  // The same test methods for the second non-namespace-well-formed XML.

  /**
   * Tries to create a DOM tree for the (wrong) 2nd XML. It fails, as expected.
   */
  test("testTryCreatingDomForWrongXml2") {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder()
    val d = db.newDocument()

    intercept[DOMException] {
      val linkbaseRefElm = d.createElementNS(null, "link:linkbaseRef")
      d.appendChild(linkbaseRefElm)
    }
  }

  /**
   * Tries to parse the 2nd XML into a yaidom Elem (via SAX), but fails, as expected.
   */
  test("testTryParsingWrongXml2ViaSax") {
    val parser = DocumentParserUsingSax.newInstance

    intercept[SAXParseException] {
      parser.parse(new ByteArrayInputStream(wrongXml2.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the 2nd XML into a yaidom Elem (via StAX), but fails, as expected.
   */
  test("testTryParsingWrongXml2ViaStax") {
    val parser = DocumentParserUsingStax.newInstance

    intercept[Exception] {
      parser.parse(new ByteArrayInputStream(wrongXml2.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the 2nd XML into a yaidom Elem (via DOM), but fails, as expected.
   */
  test("testTryParsingWrongXml2ViaDom") {
    val parser = DocumentParserUsingDom.newInstance

    intercept[SAXParseException] {
      parser.parse(new ByteArrayInputStream(wrongXml2.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the 2nd XML into a yaidom Elem (via DOM LS), but fails, as expected.
   */
  test("testTryParsingWrongXml2ViaDomLS") {
    val parser = DocumentParserUsingDomLS.newInstance

    intercept[RuntimeException] {
      parser.parse(new ByteArrayInputStream(wrongXml2.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the 2nd XML into a yaidom Elem again, but fails, as expected.
   * This time the underlying parser is not namespace-aware, but still yaidom Elem
   * creation fails.
   */
  test("testTryParsingWrongXml2Again") {
    val spf = SAXParserFactory.newInstance
    spf.setNamespaceAware(false)
    val parser = DocumentParserUsingSax.newInstance(spf)

    // SAX parsing succeeds, but yaidom Elem creation does not
    intercept[RuntimeException] {
      parser.parse(new ByteArrayInputStream(wrongXml2.getBytes("UTF-8")))
    }
  }

  /**
   * Tries to parse the 2nd XML using scala.xml.XML, but succeeds, unexpectedly.
   *
   * A fatal error is reported, but the exception is "eaten", returning a scala.xml.Elem, which is
   * not namespace-well-formed.
   */
  test("testTryParsingWrongXml2UsingScalaXml") {
    val scalaElem = scala.xml.XML.load(new ByteArrayInputStream(wrongXml2.getBytes("UTF-8")))

    assertResult("link") {
      scalaElem.prefix
    }
    assertResult("linkbaseRef") {
      scalaElem.label
    }

    assertResult(4) {
      scalaElem.attributes.size
    }
    // Now try to query for the non-namespace-well-formed XLink attributes ...
    // Also: The MetaData API is quite cumbersome, just like the Node and NodeSeq API
  }

  /**
   * Creates an ElemBuilder for the 2nd XML, which succeeds. After all, ElemBuilders are
   * only builders of XML, and do not claim to be valid namespace-well-formed XML
   * themselves. Indeed, ElemBuilders can only be queried for qualified names, but not
   * for expanded names.
   */
  test("testCreateElemBuilderForWrongXml2") {
    import NodeBuilder._

    val elemBuilder =
      emptyElem(
        QName("link:linkbaseRef"),
        Vector(
          QName("xlink:arcrole") -> "http://www.w3.org/1999/xlink/properties/linkbase",
          QName("xlink:href") -> "my-lab-en.xml",
          QName("xlink:role") -> "http://www.xbrl.org/2003/role/labelLinkbaseRef",
          QName("xlink:type") -> "simple"))

    assertResult(false) {
      elemBuilder.canBuild(Scope.Empty)
    }

    val nsXLink = "http://www.w3.org/1999/xlink"
    val nsLink = "http://www.xbrl.org/2003/linkbase"

    assertResult(false) {
      elemBuilder.canBuild(Scope.from("xlink" -> nsXLink))
    }
    assertResult(true) {
      elemBuilder.canBuild(Scope.from("xlink" -> nsXLink, "link" -> nsLink))
    }
  }

  /**
   * Tries to create an Elem for the 2nd XML, without passing the
   * correct parent scope. This fails, as expected.
   */
  test("testTryCreatingElemForWrongXml2") {
    import Node._

    intercept[RuntimeException] {
      emptyElem(
        QName("link:linkbaseRef"),
        Vector(
          QName("xlink:arcrole") -> "http://www.w3.org/1999/xlink/properties/linkbase",
          QName("xlink:href") -> "my-lab-en.xml",
          QName("xlink:role") -> "http://www.xbrl.org/2003/role/labelLinkbaseRef",
          QName("xlink:type") -> "simple"),
        Scope.Empty)
    }
  }

  /**
   * Tries to convert an ElemBuilder for the 2nd XML into an Elem, this time passing the
   * correct parent scope. This succeeds, as expected, and so do the subsequent queries.
   */
  test("testQueryElemForFixedWrongXml2") {
    import NodeBuilder._

    val elemBuilder =
      emptyElem(
        QName("link:linkbaseRef"),
        Vector(
          QName("xlink:arcrole") -> "http://www.w3.org/1999/xlink/properties/linkbase",
          QName("xlink:href") -> "my-lab-en.xml",
          QName("xlink:role") -> "http://www.xbrl.org/2003/role/labelLinkbaseRef",
          QName("xlink:type") -> "simple"))

    val nsXLink = "http://www.w3.org/1999/xlink"
    val nsLink = "http://www.xbrl.org/2003/linkbase"

    val elm = elemBuilder.build(Scope.from("xlink" -> nsXLink, "link" -> nsLink))

    val elemENames = elm.findAllElemsOrSelf.map(_.resolvedName)
    val rootAttrENames = elm.resolvedAttributes.map(_._1)

    assertResult(List(EName(nsLink, "linkbaseRef"))) {
      elemENames
    }
    assertResult(List(EName(nsXLink, "arcrole"), EName(nsXLink, "href"), EName(nsXLink, "role"), EName(nsXLink, "type"))) {
      rootAttrENames
    }

    assertResult(true) {
      elm.attributeOption(EName(nsXLink, "type")).isDefined
    }
    assertResult(false) {
      elm.attributeOption(EName(None, "type")).isDefined
    }
  }

  /**
   * Tries to create a Scala XML Elem for the 2nd XML, which succeeds (although it should not succeed).
   */
  test("testCreateScalaXmlElemForWrongXml2") {
    val scalaElem =
      <link:linkbaseRef xlink:arcrole="http://www.w3.org/1999/xlink/properties/linkbase" xlink:href="my-lab-en.xml" xlink:role="http://www.xbrl.org/2003/role/labelLinkbaseRef" xlink:type="simple"/>

    assertResult(true) {
      scalaElem.child.size == 0
    }
  }

  /**
   * Queries the 2nd Scala XML Elem. Here we see that Scala XML happily allows for querying
   * a non-namespace-well-formed XML tree. It allows for querying the child element,
   * returning the correct prefix, but returning null for the namespace. Indeed, the
   * namespace is absent, due to the non-namespace-well-formedness, but this is not
   * "XML querying", if we consider namespace-well-formedness essential for XML.
   */
  test("testQueryScalaXmlElemForWrongXml2") {
    val scalaElem =
      <link:linkbaseRef xlink:arcrole="http://www.w3.org/1999/xlink/properties/linkbase" xlink:href="my-lab-en.xml" xlink:role="http://www.xbrl.org/2003/role/labelLinkbaseRef" xlink:type="simple"/>

    assertResult("linkbaseRef") {
      scalaElem.label
    }
    assertResult("link") {
      scalaElem.prefix
    }
    assertResult(null) {
      scalaElem.namespace
    }

    val nsXLink = "http://www.w3.org/1999/xlink"
    val nsLink = "http://www.xbrl.org/2003/linkbase"

    assertResult(None) {
      scalaElem.attribute(nsXLink, "arcrole")
    }
    assertResult(Some("http://www.w3.org/1999/xlink/properties/linkbase")) {
      scalaElem.attribute(null, "arcrole").map(_.text)
    }
  }

  /**
   * Tries to query a wrapper around the 2nd Scala XML Elem, which only fails once an incorrect
   * Scope is used under the hood.
   */
  test("testQueryWrappedScalaXmlElemForWrongXml2") {
    val scalaElem =
      <link:linkbaseRef xlink:arcrole="http://www.w3.org/1999/xlink/properties/linkbase" xlink:href="my-lab-en.xml" xlink:role="http://www.xbrl.org/2003/role/labelLinkbaseRef" xlink:type="simple"/>
    val elm = ScalaXmlElem(scalaElem)

    val nsXLink = "http://www.w3.org/1999/xlink"

    assertResult(QName("link:linkbaseRef")) {
      elm.qname
    }

    intercept[RuntimeException] {
      elm.resolvedName
    }

    intercept[RuntimeException] {
      elm.attributeOption(EName(nsXLink, "type"))
    }
  }

  /**
   * Tries to convert the 2nd Scala XML Elem to a yaidom Elem, which fails, as expected.
   */
  test("testTryToConvertScalaXmlElemForWrongXml2") {
    val scalaElem =
      <link:linkbaseRef xlink:arcrole="http://www.w3.org/1999/xlink/properties/linkbase" xlink:href="my-lab-en.xml" xlink:role="http://www.xbrl.org/2003/role/labelLinkbaseRef" xlink:type="simple"/>

    intercept[RuntimeException] {
      ScalaXmlConversions.convertToElem(scalaElem)
    }
  }

  // For comparing XML, yaidom's resolved elements are a good basis. For example, attribute
  // order is non-existing in resolved elements. Scala XML does not consider two elements
  // the same if only the attribute order is different.
  // See http://stackoverflow.com/questions/4401702/scala-xml-loadstring-vs-literal-expression.

  /**
   * For comparing XML, yaidom offers resolved elements as a basis. In resolved elements,
   * attribute order does not play any role.
   */
  test("testEqualityIfAttributeOrderDiffers") {
    import Node._

    val ns = "http://www.w3.org/1999/xhtml"

    val elm1 =
      emptyElem(
        QName("link"),
        Vector(QName("href") -> "/css/main.css", QName("rel") -> "stylesheet", QName("type") -> "text/css"),
        Scope.from("" -> ns))

    val elm2 =
      emptyElem(
        QName("link"),
        Vector(QName("rel") -> "stylesheet", QName("href") -> "/css/main.css", QName("type") -> "text/css"),
        Scope.from("" -> ns))

    assertResult(false) {
      elm1 == elm2
    }
    assertResult(true) {
      resolved.Elem(elm1) == resolved.Elem(elm2)
    }
  }

  /**
   * In Scala XML, prefixes are relevant for XML equality comparisons.
   */
  test("testScalaXmlEqualityIfPrefixDiffers") {
    val ns = "http://www.w3.org/1999/xhtml"

    val xml1 =
      <link xmlns={ ns } href="/css/main.css" rel="stylesheet" type="text/css"/>
    val xml2 =
      <link xmlns={ ns } href="/css/main.css" rel="stylesheet" type="text/css"/>
    val xml3 =
      <h:link xmlns:h={ ns } href="/css/main.css" rel="stylesheet" type="text/css"/>

    assertResult(true) {
      xml1 == xml2
    }
    assertResult(false) {
      xml1 == xml3
    }
  }

  /**
   * For comparing XML, yaidom offers resolved elements as a basis. In resolved elements,
   * prefixes are irrelevant, in contrast to namespace URIs.
   */
  test("testEqualityIfPrefixDiffers") {
    import Node._

    val nsXLink = "http://www.w3.org/1999/xlink"
    val nsLink = "http://www.xbrl.org/2003/linkbase"

    val scope1 = Scope.from("xlink" -> nsXLink, "link" -> nsLink)

    val elm1 =
      emptyElem(
        QName("link:linkbaseRef"),
        Vector(
          QName("xlink:arcrole") -> "http://www.w3.org/1999/xlink/properties/linkbase",
          QName("xlink:href") -> "my-lab-en.xml",
          QName("xlink:role") -> "http://www.xbrl.org/2003/role/labelLinkbaseRef",
          QName("xlink:type") -> "simple"),
        scope1)

    val scope2 = Scope.from("xl" -> nsXLink, "lb" -> nsLink)

    val elm2 =
      emptyElem(
        QName("lb:linkbaseRef"),
        Vector(
          QName("xl:arcrole") -> "http://www.w3.org/1999/xlink/properties/linkbase",
          QName("xl:href") -> "my-lab-en.xml",
          QName("xl:role") -> "http://www.xbrl.org/2003/role/labelLinkbaseRef",
          QName("xl:type") -> "simple"),
        scope2)

    val scope3 = Scope.from("xl" -> nsXLink, "" -> nsLink)

    val elm3 =
      emptyElem(
        QName("linkbaseRef"),
        Vector(
          QName("xl:arcrole") -> "http://www.w3.org/1999/xlink/properties/linkbase",
          QName("xl:href") -> "my-lab-en.xml",
          QName("xl:role") -> "http://www.xbrl.org/2003/role/labelLinkbaseRef",
          QName("xl:type") -> "simple"),
        scope3)

    assertResult(false) {
      elm1 == elm2
    }
    assertResult(true) {
      resolved.Elem(elm1) == resolved.Elem(elm2)
    }

    assertResult(false) {
      elm1 == elm3
    }
    assertResult(true) {
      resolved.Elem(elm1) == resolved.Elem(elm3)
    }
  }

  // TODO Pattern matching. See http://www.codecommit.com/blog/scala/working-with-scalas-xml-support.
  // Also see discussion http://scala-language.1934581.n4.nabble.com/Namespace-support-in-XML-patterns-td2006894.html.
  // Or see http://alvinalexander.com/scala/using-match-expressions-with-xml-in-scala.
}
