/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package integrationtest

import java.{ util => jutil, io => jio }
import org.xml.sax.{ EntityResolver, InputSource, ErrorHandler, SAXParseException }
import javax.xml.transform.stream.StreamSource
import javax.xml.parsers.{ SAXParserFactory, SAXParser }
import org.xml.sax.helpers.DefaultHandler
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.{ DocumentParserUsingSax, DefaultElemProducingSaxHandler, SaxHandlerWithLocator }
import print.DocumentPrinterUsingSax

/**
 * SAX interoperability test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * To debug the SAX parsers, use JVM option -Djaxp.debug=1.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SaxInteropTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsYahoo = "http://www.yahoo.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("books.xml")

    val root: Elem = saxParser.parse(is).documentElement

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.allElems map (e => e.localName)).toSet
    }
    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.allElemsOrSelf map (e => e.localName)).toSet
    }
    expect(8) {
      root.elemsOrSelf(nsBookstore.ns.ename("Title")).size
    }
    expect(3) {
      val result = root elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingSax.newInstance

    val xmlString = printer.print(Document(root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = saxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect((root.allElems map (e => e.localName)).toSet) {
      (root2.allElems map (e => e.localName)).toSet
    }
    expect((root.allElemsOrSelf map (e => e.localName)).toSet) {
      (root2.allElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.elemsOrSelf(nsBookstore.ns.ename("Title")).size) {
      root2.elemsOrSelf(nsBookstore.ns.ename("Title")).size
    }
    expect {
      val result = root elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root2 elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect((root.allElems map (e => e.localName)).toSet) {
      (root3.allElems map (e => e.localName)).toSet
    }
    expect((root.allElemsOrSelf map (e => e.localName)).toSet) {
      (root3.allElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.elemsOrSelf(nsBookstore.ns.ename("Title")).size) {
      root3.elemsOrSelf(nsBookstore.ns.ename("Title")).size
    }
    expect {
      val result = root elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root3 elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 6. Print to XML and parse back, and check again

    val doc = Document(root3)

    val xmlString2 = printer.print(doc)

    val doc2 = saxParser.parse(new jio.ByteArrayInputStream(xmlString2.getBytes("utf-8")))

    val root4 = doc2.documentElement

    expect((root.allElems map (e => e.localName)).toSet) {
      (root4.allElems map (e => e.localName)).toSet
    }
    expect((root.allElemsOrSelf map (e => e.localName)).toSet) {
      (root4.allElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.elemsOrSelf(nsBookstore.ns.ename("Title")).size) {
      root4.elemsOrSelf(nsBookstore.ns.ename("Title")).size
    }
    expect {
      val result = root elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root4 elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  @Test def testParseStrangeXml() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("strangeXml.xml")

    val root: Elem = saxParser.parse(is).documentElement

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingSax.newInstance

    val xmlString = printer.print(Document(root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = saxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  @Test def testParseDefaultNamespaceXml() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("trivialXml.xml")

    val document: Document = saxParser.parse(is)
    val root: Elem = document.documentElement

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root.allElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("This is trivial XML") {
      val result = document.comments map { com => com.text.trim }
      result.mkString
    }
    expect("Trivial XML") {
      val result = root.allElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingSax.newInstance

    val xmlString = printer.print(document)

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val document2: Document = saxParser.parse(bis)
    val root2: Elem = document2.documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root2.allElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("This is trivial XML") {
      val result = document2.comments map { com => com.text.trim }
      result.mkString
    }
    expect("Trivial XML") {
      val result = root2.allElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 5. Convert to NodeBuilder and back, and check again

    val document3: Document = NodeBuilder.fromDocument(document2)(Scope.Empty).build()
    val root3: Elem = document3.documentElement

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root3.allElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("This is trivial XML") {
      val result = document3.comments map { com => com.text.trim }
      result.mkString
    }
    expect("Trivial XML") {
      val result = root3.allElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
    // 1. Parse XML file into Elem

    val spf = SAXParserFactory.newInstance

    // One possible EntityResolver, namely one that tries to find the needed DTDs locally
    trait EntityResolverUsingLocalDtds extends EntityResolver {
      override def resolveEntity(publicId: String, systemId: String): InputSource = {
        logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))

        if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
          new InputSource(classOf[SaxInteropTest].getResourceAsStream("XMLSchema.dtd"))
        } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
          new InputSource(classOf[SaxInteropTest].getResourceAsStream("datatypes.dtd"))
        } else {
          // Default behaviour
          null
        }
      }
    }

    // Another possible EntityResolver, namely one that suppresses DTD resolution
    trait SuppressingEntityResolver extends EntityResolver {
      override def resolveEntity(publicId: String, systemId: String): InputSource = {
        logger.info("Trying to resolve entity (but suppressing it). Public ID: %s. System ID: %s".format(publicId, systemId))

        // See http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds
        new InputSource(new jio.StringReader(""))
      }
    }

    // We use the SuppressingEntityResolver
    val saxParser = DocumentParserUsingSax.newInstance(spf, () => new DefaultElemProducingSaxHandler with SuppressingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("XMLSchema.xsd")

    val root: Elem = saxParser.parse(is).documentElement

    val ns = nsXmlSchema.ns

    val xsElmENames: Set[ExpandedName] =
      Set(ns.ename("schema"), ns.ename("annotation"), ns.ename("documentation"),
        ns.ename("import"), ns.ename("complexType"), ns.ename("complexContent"),
        ns.ename("extension"), ns.ename("sequence"), ns.ename("element"),
        ns.ename("attribute"), ns.ename("choice"), ns.ename("group"),
        ns.ename("simpleType"), ns.ename("restriction"), ns.ename("enumeration"),
        ns.ename("list"), ns.ename("union"), ns.ename("key"),
        ns.ename("selector"), ns.ename("field"), ns.ename("attributeGroup"),
        ns.ename("anyAttribute"), ns.ename("whiteSpace"), ns.ename("fractionDigits"),
        ns.ename("pattern"), ns.ename("any"), ns.ename("appinfo"),
        ns.ename("minLength"), ns.ename("maxInclusive"), ns.ename("minInclusive"),
        ns.ename("notation"))

    expect(xsElmENames) {
      val result = root elemsOrSelfWhere { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: Elem): Unit = {
      val forChoiceDefOption: Option[Elem] =
        rootElm childElemsWhere { e => e.resolvedName == ns.ename("simpleType") && e.attribute("name".ename) == "formChoice" } headOption

      expect(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.elems(ns.ename("documentation")) flatMap { e => e.trimmedText } mkString

      expect("A utility type, not for public use") {
        forChoiceDefDocumentation.trim
      }
    }

    checkForChoiceDocumentation(root)

    def checkCommentWithEscapedChar(rootElm: Elem): Unit = {
      val documentationElms =
        for {
          annotationElm <- rootElm.childElems(ns.ename("annotation"))
          documentationElm <- annotationElm.childElems(ns.ename("documentation"))
        } yield documentationElm

      val documentationText = documentationElms.drop(1).headOption map { e => e.trimmedText } getOrElse ""

      // The XML string contains "&lt;", but the parsed text should contain an unescaped "<" instead
      expect(true) {
        documentationText.containsSlice("""XML Schema language.  The documentation (within <documentation> elements)""")
      }
    }

    checkCommentWithEscapedChar(root)

    def checkIdentityConstraintElm(rootElm: Elem): Unit = {
      val identityConstraintElms =
        for {
          schemaElm <- rootElm elemsWhere { e =>
            e.resolvedName == ns.ename("element") &&
              e.attributeOption("name".ename) == Some("schema") &&
              e.attributeOption("id".ename) == Some("schema")
          }
          idConstraintElm <- schemaElm childElemsWhere { e =>
            e.resolvedName == ns.ename("key") &&
              e.attributeOption("name".ename) == Some("identityConstraint")
          }
        } yield idConstraintElm

      expect(1) {
        identityConstraintElms.size
      }

      val selectorElms = identityConstraintElms.head.childElems(ns.ename("selector"))

      expect(1) {
        selectorElms.size
      }

      expect(""".//xs:key|.//xs:unique|.//xs:keyref""") {
        selectorElms.head.attributeOption("xpath".ename).getOrElse("")
      }
    }

    checkIdentityConstraintElm(root)

    def checkComplexTypeElm(rootElm: Elem): Unit = {
      val complexTypeElms =
        rootElm elemsWhere { e =>
          e.resolvedName == ns.ename("complexType") &&
            e.attributeOption("name".ename) == Some("element") &&
            e.attributeOption("abstract".ename) == Some("true")
        }

      expect(1) {
        complexTypeElms.size
      }

      val extensionElms = complexTypeElms.head.elems(ns.ename("extension"))
      val sequenceElms = complexTypeElms.head.elems(ns.ename("sequence"))
      val choiceElms = complexTypeElms.head.elems(ns.ename("choice"))
      val elementElms = complexTypeElms.head.elems(ns.ename("element"))
      val groupElms = complexTypeElms.head.elems(ns.ename("group"))
      val attributeElms = complexTypeElms.head.elems(ns.ename("attribute"))
      val attributeGroupElms = complexTypeElms.head.elems(ns.ename("attributeGroup"))

      expect(Set("base".ename)) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }
      expect(Set("xs:annotated")) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.values }
        result.toSet
      }

      expect(Set()) {
        val result = sequenceElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set("minOccurs".ename)) {
        val result = choiceElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set("name".ename, "type".ename)) {
        val result = elementElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set("ref".ename, "minOccurs".ename, "maxOccurs".ename)) {
        val result = groupElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set("name".ename, "type".ename, "use".ename, "default".ename)) {
        val result = attributeElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set("ref".ename)) {
        val result = attributeGroupElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }
    }

    checkComplexTypeElm(root)

    def checkFieldPattern(rootElm: Elem): Unit = {
      val fieldElms = rootElm elemsWhere { e =>
        e.resolvedName == ns.ename("element") &&
          e.attributeOption("name".ename) == Some("field") &&
          e.attributeOption("id".ename) == Some("field")
      }

      val patternElms = fieldElms flatMap { e => e.elems(ns.ename("pattern")) }

      expect(1) {
        patternElms.size
      }

      expect("""(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*""") {
        patternElms.head.attributeOption("value".ename).getOrElse("")
      }
    }

    checkFieldPattern(root)

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingSax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = saxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect(xsElmENames) {
      val result = root2 elemsOrSelfWhere { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }

    checkForChoiceDocumentation(root2)
    checkCommentWithEscapedChar(root2)
    checkIdentityConstraintElm(root2)
    checkComplexTypeElm(root2)
    checkFieldPattern(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(xsElmENames) {
      val result = root3 elemsOrSelfWhere { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }

    checkForChoiceDocumentation(root3)
    checkCommentWithEscapedChar(root3)
    checkIdentityConstraintElm(root3)
    checkComplexTypeElm(root3)
    checkFieldPattern(root3)
  }

  @Test def testParseXmlWithExpandedEntityRef() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = saxParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: Elem): Unit = {
      val childOption = rootElm.firstElemOption(ns.ename("child"))
      expect(true) {
        childOption.isDefined
      }
      val text = "This text contains an entity reference, viz. hi"
      expect(text) {
        childOption.get.trimmedText.take(text.length)
      }
    }

    checkChildText(root)

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingSax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = saxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root3)
  }

  @Test def testParseXmlWithNamespaceUndeclarations() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val root: Elem = saxParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingSax.newInstance

    val xmlString = printer.print(Document(root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = saxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithEscapedChars() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val root: Elem = saxParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.topmostElems(ns.ename("child"))
      expect(2) {
        childElms.size
      }

      val text = "Jansen & co"

      expect(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }

      expect(Set(text)) {
        val result = childElms map { e => e.attributeOption("about".ename).getOrElse("Missing text") }
        result.toSet
      }

      expect(Set(text)) {
        val result = rootElm.commentChildren map { c => c.text.trim }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingSax.newInstance

    val xmlString = printer.print(Document(root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = saxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)
  }

  @Test def testParseXmlWithSpecialChars() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val root: Elem = saxParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.topmostElems(ns.ename("child"))
      expect(2) {
        childElms.size
      }

      val text = "\u20AC 200"

      expect(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Convert to NodeBuilder and back, and check again

    val root2: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)
  }

  @Test def testParseGeneratedHtml() {
    // 1. Parse XML file into Elem

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      () => new DefaultElemProducingSaxHandler with LoggingEntityResolver)

    val is = classOf[SaxInteropTest].getResourceAsStream("books.xml")

    val root: Elem = saxParser.parse(is).documentElement

    require(root.localName == "Bookstore")

    // 2. Create HTML string

    val htmlFormatString =
      """|<html>
         |  <body>
         |    <h1>Bookstore</h1>
         |    <table>
         |      <tr>
         |        <th>Title</th>
         |        <th>ISBN</th>
         |        <th>Edition</th>
         |        <th>Authors</th>
         |        <th>Price</th>
         |      </tr>
         |%s
         |    </table>
         |  </body>
         |</html>""".stripMargin

    val bookFormatString =
      """|      <tr>
         |        <td>%s</td>
         |        <td>%s</td>
         |        <td>%s</td>
         |        <td>%s</td>
         |        <td>%s</td>
         |      </tr>""".stripMargin

    def bookHtmlString(bookElm: Elem): String = {
      val authorNames: immutable.IndexedSeq[String] =
        bookElm.elems("{http://bookstore}Author".ename) map { e =>
          "%s %s".format(e.singleChildElem("{http://bookstore}First_Name".ename).trimmedText, e.singleChildElem("{http://bookstore}Last_Name".ename).trimmedText)
        }

      val authors = authorNames.mkString(", ")

      val result = bookFormatString.format(
        bookElm.singleChildElem("{http://bookstore}Title".ename).trimmedText,
        bookElm.attributeOption("ISBN".ename).getOrElse(""),
        bookElm.attributeOption("Edition".ename).getOrElse(""),
        authors,
        bookElm.attributeOption("Price".ename).getOrElse(""))
      result
    }

    val booksHtmlString = root.elems("{http://bookstore}Book".ename) map { e => bookHtmlString(e) } mkString ("\n")
    val htmlString = htmlFormatString.format(booksHtmlString)

    // 3. Parse HTML string (which is also valid XML in this case) into Document

    val htmlRoot: Elem = saxParser.parse(new jio.ByteArrayInputStream(htmlString.getBytes("utf-8"))).documentElement

    // 4. Check the parsed HTML

    val tableRowElms = htmlRoot.elems("tr".ename).drop(1)

    expect(4) {
      tableRowElms.size
    }

    val isbnElms = tableRowElms flatMap { rowElm => rowElm.childElems("td".ename).drop(1).headOption }
    val isbns = isbnElms map { e => e.trimmedText }

    expect(Set("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-0-11-222222-3", "ISBN-9-88-777777-6")) {
      isbns.toSet
    }

    val authorsElms = tableRowElms flatMap { rowElm => rowElm.childElems("td".ename).drop(3).headOption }
    val authors = authorsElms map { e => e.trimmedText }

    expect(Set(
      "Jeffrey Ullman, Jennifer Widom",
      "Hector Garcia-Molina, Jeffrey Ullman, Jennifer Widom",
      "Jeffrey Ullman, Hector Garcia-Molina",
      "Jennifer Widom")) {
      authors.toSet
    }
  }

  @Test def testParseBrokenXml() {
    var errorCount = 0
    var fatalErrorCount = 0
    var warningCount = 0
    var lineNumber = 0
    var columnNumber = 0

    trait MyErrorHandler extends ErrorHandler { self: SaxHandlerWithLocator =>

      override def error(exc: SAXParseException) {
        errorCount += 1
        lineNumber = locator.getLineNumber
        columnNumber = locator.getColumnNumber
      }

      override def fatalError(exc: SAXParseException) {
        fatalErrorCount += 1
        lineNumber = locator.getLineNumber
        columnNumber = locator.getColumnNumber
      }

      override def warning(exc: SAXParseException) {
        warningCount += 1
        lineNumber = locator.getLineNumber
        columnNumber = locator.getColumnNumber
      }
    }

    val handlerCreator = () => new DefaultElemProducingSaxHandler with LoggingEntityResolver with SaxHandlerWithLocator with MyErrorHandler

    val saxParser = DocumentParserUsingSax.newInstance(
      SAXParserFactory.newInstance,
      handlerCreator)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()

    val is = new jio.ByteArrayInputStream(brokenXmlString.getBytes("utf-8"))

    intercept[SAXParseException] {
      saxParser.parse(is).documentElement
    }
    expect(1) {
      fatalErrorCount
    }
    expect(0) {
      errorCount
    }
    expect(0) {
      warningCount
    }
    expect(2) {
      lineNumber
    }
    assert(columnNumber >= 16, "Expected the column number to be 16 or larger")
  }

  trait LoggingEntityResolver extends EntityResolver {
    override def resolveEntity(publicId: String, systemId: String): InputSource = {
      logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))
      // Default behaviour
      null
    }
  }
}
