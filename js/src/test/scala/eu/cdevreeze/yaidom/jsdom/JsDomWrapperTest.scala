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

package eu.cdevreeze.yaidom.jsdom

import eu.cdevreeze.yaidom.convert.JsDomConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple
import org.scalajs.dom.experimental.domparser.DOMParser
import org.scalajs.dom.experimental.domparser.SupportedType
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite

/**
 * DOM wrapper test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
class JsDomWrapperTest extends AnyFunSuite with BeforeAndAfterAll {

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"

  protected override def afterAll(): Unit = {
    // See https://github.com/orbeon/orbeon-forms/issues/2743, and the reason for the Travis build
    // to fail if we do not close the window afterwards.

    org.scalajs.dom.window.close()
  }

  test("testParse") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(booksXml, SupportedType.`text/xml`))

    val root: JsDomElem = domDoc.documentElement

    assertResult(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElems map (e => e.localName)).toSet
    }
    assertResult(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(8) {
      root.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult(3) {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  test("testParseStrangeXml") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(strangeXml, SupportedType.`text/xml`))

    val root: JsDomElem = domDoc.documentElement

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  test("testParseDefaultNamespaceXml") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(trivialXml, SupportedType.`text/xml`))

    val root: JsDomElem = domDoc.documentElement

    assertResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(QName("root"), QName("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    assertResult("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.commentChildren.map(_.text.trim) }
      result.mkString
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testParseGroovyXmlExample") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(carsXml, SupportedType.`text/xml`))

    assertResult("records") {
      domDoc.documentElement.localName
    }

    val recordsElm = domDoc.documentElement

    assertResult(3) {
      (recordsElm \ (_.localName == "car")).size
    }

    assertResult(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ (_.localName == "car")) (0)

    assertResult("car") {
      firstRecordElm.localName
    }

    assertResult("Holden") {
      firstRecordElm.attribute(EName("make"))
    }

    assertResult("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    assertResult(2) {
      val carElms = recordsElm \ (_.localName == "car")
      val result = carElms filter { e => e.attributeOption(EName("make")).getOrElse("").contains('e') }
      result.size
    }

    assertResult(Set("Holden", "Peel")) {
      val carElms = recordsElm \ (_.localName == "car")
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute(EName("make")))).toSet
    }

    assertResult(Set("speed", "size", "price")) {
      val result = recordsElm.findAllElemsOrSelf collect { case e if e.attributeOption(EName("type")).isDefined => e.attribute(EName("type")) }
      result.toSet
    }
  }

  // TODO Larger XML example, such as gaap.xsd

  /**
   * Example of parsing a document with multiple kinds of nodes.
   */
  test("testParseMultipleNodeKinds") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(
      db.parseFromString(trivialXmlWithDifferentKindsOfNodes, SupportedType.`text/xml`))

    assertResult(2) {
      domDoc.comments.size
    }
    assertResult(1) {
      domDoc.processingInstructions.size
    }

    assertResult("Some comment") {
      domDoc.comments(1).text.trim
    }
    assertResult("pi") {
      domDoc.processingInstructions.head.wrappedNode.target
    }

    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.children collect { case c: JsDomComment if c.text.trim == "Another comment" => c }).size
    }
    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.children collect { case pi: JsDomProcessingInstruction if pi.wrappedNode.target == "some_pi" => pi }).size
    }
    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.children collect { case t: JsDomText if t.text.trim.contains("Some Text") => t }).size
    }
    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.textChildren.filter(_.text.trim.contains("Some Text"))).size
    }
  }

  // Now add conversions to simple and indexed elements into the mix

  test("testParseAndConvert") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(booksXml, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val root: simple.Elem = JsDomConversions.convertToElem(domRoot.wrappedNode, Scope.Empty)

    assertResult(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElems map (e => e.localName)).toSet
    }
    assertResult(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(8) {
      root.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult(3) {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(root)
    }

    val iroot = indexed.Elem(root)

    assertResult((root.findAllElems map (e => e.localName)).toSet) {
      (iroot.findAllElems map (e => e.localName)).toSet
    }
    assertResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (iroot.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      iroot.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult((root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }).size) {
      val result = iroot \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(iroot.underlyingElem)
    }
  }

  test("testParseAndConvertStrangeXml") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(strangeXml, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val root: simple.Elem = JsDomConversions.convertToElem(domRoot.wrappedNode, Scope.Empty)
    val iroot: indexed.Elem = indexed.Elem(root)

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = iroot.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(iroot.underlyingElem)
    }
  }

  test("testParseAndConvertDefaultNamespaceXml") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(trivialXml, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val root: simple.Elem = JsDomConversions.convertToElem(domRoot.wrappedNode, Scope.Empty)
    val iroot: indexed.Elem = indexed.Elem(root)

    assertResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = iroot.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(QName("root"), QName("child"))) {
      val result = iroot.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    assertResult("Trivial XML") {
      val result = iroot.findAllElemsOrSelf flatMap { e => e.underlyingElem.commentChildren.map(_.text.trim) }
      result.mkString
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(iroot.underlyingElem)
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testParseAndConvertGroovyXmlExample") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(carsXml, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val root: simple.Elem = JsDomConversions.convertToElem(domRoot.wrappedNode, Scope.Empty)
    val iroot: indexed.Elem = indexed.Elem(root)

    assertResult("records") {
      domDoc.documentElement.localName
    }

    assertResult(3) {
      (iroot \ (_.localName == "car")).size
    }

    assertResult(10) {
      iroot.findAllElemsOrSelf.size
    }

    val firstRecordElm = (iroot \ (_.localName == "car")) (0)

    assertResult("car") {
      firstRecordElm.localName
    }

    assertResult("Holden") {
      firstRecordElm.attribute(EName("make"))
    }

    assertResult("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    assertResult(2) {
      val carElms = iroot \ (_.localName == "car")
      val result = carElms filter { e => e.attributeOption(EName("make")).getOrElse("").contains('e') }
      result.size
    }

    assertResult(Set("Holden", "Peel")) {
      val carElms = iroot \ (_.localName == "car")
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute(EName("make")))).toSet
    }

    assertResult(Set("speed", "size", "price")) {
      val result = iroot.findAllElemsOrSelf collect { case e if e.attributeOption(EName("type")).isDefined => e.attribute(EName("type")) }
      result.toSet
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(iroot.underlyingElem)
    }
  }

  /**
   * Example of parsing a document with multiple kinds of nodes.
   */
  test("testParseAndConvertMultipleNodeKinds") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(
      db.parseFromString(trivialXmlWithDifferentKindsOfNodes, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val doc: simple.Document = JsDomConversions.convertToDocument(domDoc.wrappedDocument)
    val root: simple.Elem = doc.documentElement
    val iroot: indexed.Elem = indexed.Elem(root)

    assertResult(2) {
      doc.comments.size
    }
    assertResult(1) {
      doc.processingInstructions.size
    }

    assertResult("Some comment") {
      doc.comments(1).text.trim
    }
    assertResult("pi") {
      doc.processingInstructions.head.target
    }

    assertResult(1) {
      root.findAllElemsOrSelf.
        flatMap(_.children collect { case c: simple.Comment if c.text.trim == "Another comment" => c }).size
    }
    assertResult(1) {
      root.findAllElemsOrSelf.
        flatMap(_.children collect { case pi: simple.ProcessingInstruction if pi.target == "some_pi" => pi }).size
    }
    assertResult(1) {
      root.findAllElemsOrSelf.
        flatMap(_.children collect { case t: simple.Text if t.text.trim.contains("Some Text") => t }).size
    }
    assertResult(1) {
      root.findAllElemsOrSelf.
        flatMap(_.textChildren.filter(_.text.trim.contains("Some Text"))).size
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(iroot.underlyingElem)
    }
  }

  test("testParseAndConvertLinkbase") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(linkbaseXml, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val root: simple.Elem = JsDomConversions.convertToElem(domRoot.wrappedNode, Scope.Empty)
    val iroot: indexed.Elem = indexed.Elem(root)

    val xlinkNs = "http://www.w3.org/1999/xlink"
    val linkNs = "http://www.xbrl.org/2003/linkbase"
    val genNs = "http://xbrl.org/2008/generic"
    val labelNs = "http://xbrl.org/2008/label"
    val xmlNs = "http://www.w3.org/XML/1998/namespace"
    val xsiNs = "http://www.w3.org/2001/XMLSchema-instance"

    assertResult(Set(EName(linkNs, "linkbase"), EName(linkNs, "roleRef"), EName(linkNs, "arcroleRef"),
      EName(genNs, "link"), EName(genNs, "arc"), EName(labelNs, "label"), EName(linkNs, "loc"))) {

      domRoot.findAllElemsOrSelf.map(_.resolvedName).toSet
    }
    assertResult(domRoot.findAllElemsOrSelf.map(_.resolvedName).toSet) {
      iroot.findAllElemsOrSelf.map(_.resolvedName).toSet
    }

    assertResult(Set(EName("roleURI"), EName("arcroleURI"), EName("id"), EName(xmlNs, "lang"), EName(xlinkNs, "href"), EName(xlinkNs, "type"),
      EName(xlinkNs, "role"), EName(xlinkNs, "arcrole"), EName(xlinkNs, "from"), EName(xlinkNs, "to"), EName(xlinkNs, "label"),
      EName(xsiNs, "schemaLocation"))) {

      domRoot.findAllElemsOrSelf.flatMap(_.resolvedAttributes.toMap.keySet).toSet
    }
    assertResult(domRoot.findAllElemsOrSelf.flatMap(_.resolvedAttributes.toMap.keySet).toSet) {
      iroot.findAllElemsOrSelf.flatMap(_.resolvedAttributes.toMap.keySet).toSet
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(iroot.underlyingElem)
    }
  }

  // Querying for ancestry

  test("testAncestry") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(booksXml, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val root: simple.Elem = JsDomConversions.convertToElem(domRoot.wrappedNode, Scope.Empty)
    val iroot: indexed.Elem = indexed.Elem(root)

    assertResult(false) {
      val firstDescendant = domRoot.findAllElems.head
      resolved.Elem.from(firstDescendant.parent) == resolved.Elem.from(firstDescendant)
    }

    assertResult(resolved.Elem.from(domRoot)) {
      resolved.Elem.from(domRoot.findAllElems.head.parent)
    }

    assertResult(domRoot.findAllElems.map(_.parent).map(e => resolved.Elem.from(e))) {
      iroot.findAllElems.map(_.parent).map(e => resolved.Elem.from(e.underlyingElem))
    }

    assertResult(List(root.resolvedName)) {
      domRoot.findAllElemsOrSelf.map(_.ancestorsOrSelf.last.resolvedName).distinct
    }
  }

  // Testing navigation and paths

  test("testNavigation") {
    val db = new DOMParser()
    val domDoc: JsDomDocument = JsDomDocument.wrapDocument(db.parseFromString(booksXml, SupportedType.`text/xml`))

    val domRoot: JsDomElem = domDoc.documentElement
    val root: simple.Elem = JsDomConversions.convertToElem(domRoot.wrappedNode, Scope.Empty)
    val iroot: indexed.Elem = indexed.Elem(root)

    val paths = iroot.findAllElemsOrSelf.map(_.path).ensuring(_.head.isEmpty).ensuring(_.tail.head.nonEmpty)

    assertResult(paths.map(path => resolved.Elem.from(domRoot.getElemOrSelfByPath(path)))) {
      paths.map(path => resolved.Elem.from(root.getElemOrSelfByPath(path)))
    }

    assertResult(iroot.findAllElemsOrSelf.map(_.path)) {
      domRoot.findAllElemsOrSelf.map(_.path)
    }

    assertResult(List(resolved.Elem.from(root))) {
      domRoot.findAllElemsOrSelf.map(_.rootElem).map(e => resolved.Elem.from(e)).distinct
    }
  }

  private val booksXml =
    """<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
	<Book ISBN="ISBN-0-13-713526-2" Price="85" Edition="3rd">
		<Title>A First Course in Database Systems</Title>
		<Authors>
			<Author>
				<First_Name>Jeffrey</First_Name>
				<Last_Name>Ullman</Last_Name>
			</Author>
			<Author>
				<First_Name>Jennifer</First_Name>
				<Last_Name>Widom</Last_Name>
			</Author>
		</Authors>
	</Book>
	<Book ISBN="ISBN-0-13-815504-6" Price="100">
		<Title>Database Systems: The Complete Book</Title>
		<Authors>
			<Author>
				<First_Name>Hector</First_Name>
				<Last_Name>Garcia-Molina</Last_Name>
			</Author>
			<Author>
				<First_Name>Jeffrey</First_Name>
				<Last_Name>Ullman</Last_Name>
			</Author>
			<Author>
				<First_Name>Jennifer</First_Name>
				<Last_Name>Widom</Last_Name>
			</Author>
		</Authors>
		<Remark>Buy this book bundled with "A First Course" - a great deal!
		</Remark>
	</Book>
	<Book ISBN="ISBN-0-11-222222-3" Price="50">
		<Title>Hector and Jeff's Database Hints</Title>
		<Authors>
			<Author>
				<First_Name>Jeffrey</First_Name>
				<Last_Name>Ullman</Last_Name>
			</Author>
			<Author>
				<First_Name>Hector</First_Name>
				<Last_Name>Garcia-Molina</Last_Name>
			</Author>
		</Authors>
		<Remark>An indispensable companion to your textbook</Remark>
	</Book>
	<Book ISBN="ISBN-9-88-777777-6" Price="25">
		<Title>Jennifer's Economical Database Hints</Title>
		<Authors>
			<Author>
				<First_Name>Jennifer</First_Name>
				<Last_Name>Widom</Last_Name>
			</Author>
		</Authors>
	</Book>
	<Magazine Month="January" Year="2009">
		<Title>National Geographic</Title>
	</Magazine>
	<Magazine Month="February" Year="2009">
		<Title>National Geographic</Title>
	</Magazine>
	<Magazine Month="February" Year="2009">
		<Title>Newsweek</Title>
	</Magazine>
	<Magazine Month="March" Year="2009">
		<Title>Hector and Jeff's Database Hints</Title>
	</Magazine>
</books:Bookstore>"""

  private val strangeXml =
    """<bar xmlns:ns="http://www.yahoo.com">
	<ns:foo xmlns:ns="http://www.google.com" />
</bar>"""

  private val trivialXml =
    """<!-- This is trivial XML -->
<root xmlns="urn:foo:bar">
	<!-- Trivial XML -->
	<child />
</root>"""

  private val carsXml =
    """<!-- Copied from http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser -->
<records>
	<car name='HSV Maloo' make='Holden' year='2006'>
		<country>Australia</country>
		<record type='speed'>Production Pickup Truck with speed of 271kph
		</record>
	</car>
	<car name='P50' make='Peel' year='1962'>
		<country>Isle of Man</country>
		<record type='size'>Smallest Street-Legal Car at 99cm wide and 59 kg
			in weight</record>
	</car>
	<car name='Royale' make='Bugatti' year='1931'>
		<country>France</country>
		<record type='price'>Most Valuable Car at $15 million</record>
	</car>
</records>"""

  private val trivialXmlWithDifferentKindsOfNodes =
    """<!DOCTYPE root [
  <!ENTITY hello "hi there">
  <!ELEMENT RootElement (FirstElement, SecondElement, ThirdElement, FourthElement)>
  <!ELEMENT FirstElement (#PCDATA)>
  <!ELEMENT SecondElement (#PCDATA|Inline)*>
  <!ELEMENT ThirdElement (#PCDATA)>
  <!ELEMENT FourthElement (#PCDATA)>
  <!ELEMENT Inline (#PCDATA)>
  <!ATTLIST RootElement xmlns CDATA #REQUIRED>
  <!ATTLIST RootElement param CDATA #REQUIRED>
  <!ATTLIST SecondElement param2 CDATA #REQUIRED>
]>
<!-- See http://en.wikipedia.org/wiki/Simple_API_for_XML -->
<!-- Some comment -->
<?pi some_value?>
<RootElement param="value" xmlns="http://bla">
	<!-- Another comment -->
	<FirstElement>
		Some Text
	</FirstElement>
    <?some_pi some_value?>
	<SecondElement param2="something">
		Pre-Text <Inline>Inlined text</Inline> Post-text.
	</SecondElement>
	<ThirdElement>
		<![CDATA[Piet & co]]>
	</ThirdElement>
	<FourthElement>
		This text contains an entity reference, viz. &hello;.
	The entity is defined in the included DTD.
	</FourthElement>
</RootElement>"""

  private val linkbaseXml =
    """<!--
  This file is part of the Dutch Taxonomy (Nederlandse Taxonomie; NT)
  Intellectual Property of the State of the Netherlands
  Architecture: NT11
  Version: 20161214
  Release date: Wed Dec 14 09:00:00 2016
-->
<link:linkbase xmlns:gen="http://xbrl.org/2008/generic" xmlns:label="http://xbrl.org/2008/label" xmlns:link="http://www.xbrl.org/2003/linkbase" xmlns:xlink="http://www.w3.org/1999/xlink" xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance" xsi:schemaLocation="http://xbrl.org/2008/label http://www.xbrl.org/2008/generic-label.xsd http://xbrl.org/2008/generic http://www.xbrl.org/2008/generic-link.xsd">
  <link:roleRef roleURI="http://www.xbrl.org/2008/role/label" xlink:href="http://www.xbrl.org/2008/generic-label.xsd#standard-label" xlink:type="simple"/>
  <link:roleRef roleURI="http://www.xbrl.org/2008/role/link" xlink:href="http://www.xbrl.org/2008/generic-link.xsd#standard-link-role" xlink:type="simple"/>
  <link:arcroleRef arcroleURI="http://xbrl.org/arcrole/2008/element-label" xlink:href="http://www.xbrl.org/2008/generic-label.xsd#element-label" xlink:type="simple"/>
  <gen:link xlink:role="http://www.xbrl.org/2008/role/link" xlink:type="extended">
    <gen:arc xlink:arcrole="http://xbrl.org/arcrole/2008/element-label" xlink:from="ez-ncgc-lr_DutchCorporateGovernanceCode_loc" xlink:to="ez-ncgc-lr_DutchCorporateGovernanceCode_label_en" xlink:type="arc"/>
    <label:label id="ez-ncgc-lr_DutchCorporateGovernanceCode_label_en" xlink:label="ez-ncgc-lr_DutchCorporateGovernanceCode_label_en" xlink:role="http://www.xbrl.org/2008/role/label" xlink:type="resource" xml:lang="en">Dutch Corporate Governance Code</label:label>
    <link:loc xlink:href="ez-ncgc-linkroles.xsd#ez-ncgc-lr_DutchCorporateGovernanceCode" xlink:label="ez-ncgc-lr_DutchCorporateGovernanceCode_loc" xlink:type="locator"/>
  </gen:link>
</link:linkbase>"""
}
