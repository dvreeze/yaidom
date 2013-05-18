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
import javax.xml.stream.{ XMLInputFactory, XMLOutputFactory, XMLEventFactory, XMLResolver, XMLStreamException }
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.stream.StreamSource
import org.xml.sax.{ EntityResolver, InputSource, ErrorHandler, SAXParseException }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingStax
import print.DocumentPrinterUsingStax
import convert.StaxConversions._

/**
 * StAX interoperability test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * To debug the StAX parsers, use JVM option -Djaxp.debug=1.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class StaxInteropTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance
    val is = classOf[StaxInteropTest].getResourceAsStream("books.xml")

    val root: Elem = staxParser.parse(is).documentElement

    expectResult(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElems map (e => e.localName)).toSet
    }
    expectResult(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(8) {
      root.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult(3) {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult((root.findAllElems map (e => e.localName)).toSet) {
      (root2.findAllElems map (e => e.localName)).toSet
    }
    expectResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root2.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root2.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root2 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expectResult((root.findAllElems map (e => e.localName)).toSet) {
      (root3.findAllElems map (e => e.localName)).toSet
    }
    expectResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root3.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root3.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root3 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 6. Print to XML and parse back, and check again

    val doc = Document(root3)

    val xmlString2 = printer.print(doc)

    val xmlString3 = printer.print(doc.documentElement)
    assert(xmlString2.startsWith("<?xml "))
    assert(!xmlString3.startsWith("<?xml "))
    assert(xmlString2.size >= xmlString3.size + "<?xml ".size)

    val doc2 = staxParser.parse(new jio.ByteArrayInputStream(xmlString2.getBytes("utf-8")))

    val root4 = doc2.documentElement

    expectResult((root.findAllElems map (e => e.localName)).toSet) {
      (root4.findAllElems map (e => e.localName)).toSet
    }
    expectResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root4.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root4.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root4 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 7. Convert to resolved.Elem, and check again

    val root5: resolved.Elem = resolved.Elem(doc.documentElement)

    expectResult((root.findAllElems map (e => e.localName)).toSet) {
      (root5.findAllElems map (e => e.localName)).toSet
    }
    expectResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root5.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root5.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root5 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  @Test def testParseStrangeXml() {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance
    val is = classOf[StaxInteropTest].getResourceAsStream("strangeXml.xml")

    val root: Elem = staxParser.parse(is).documentElement

    expectResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expectResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  @Test def testParseDefaultNamespaceXml() {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXml.xml")

    val document: Document = staxParser.parse(is)
    val root: Elem = document.documentElement

    expectResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(QName("root"), QName("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expectResult("This is trivial XML") {
      val result = document.comments map { com => com.text.trim }
      result.mkString
    }
    expectResult("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(document)

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val document2: Document = staxParser.parse(bis)
    val root2: Elem = document2.documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(QName("root"), QName("child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expectResult("This is trivial XML") {
      val result = document2.comments map { com => com.text.trim }
      result.mkString
    }
    expectResult("Trivial XML") {
      val result = root2.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 5. Convert to NodeBuilder and back, and check again

    val document3: Document = DocBuilder.fromDocument(document2).build()
    val root3: Elem = document3.documentElement

    expectResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(QName("root"), QName("child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expectResult("This is trivial XML") {
      val result = document3.comments map { com => com.text.trim }
      result.mkString
    }
    expectResult("Trivial XML") {
      val result = root3.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
    // 1. Parse XML file into Elem

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    xmlInputFactory.setXMLResolver(new DtdSuppressionResolver)

    val staxParser = new DocumentParserUsingStax(xmlInputFactory)

    val is = classOf[StaxInteropTest].getResourceAsStream("XMLSchema.xsd")

    val document: Document = staxParser.parse(is)
    val root: Elem = document.documentElement

    val ns = nsXmlSchema

    val xsElmENames: Set[EName] =
      Set(EName(ns, "schema"), EName(ns, "annotation"), EName(ns, "documentation"),
        EName(ns, "import"), EName(ns, "complexType"), EName(ns, "complexContent"),
        EName(ns, "extension"), EName(ns, "sequence"), EName(ns, "element"),
        EName(ns, "attribute"), EName(ns, "choice"), EName(ns, "group"),
        EName(ns, "simpleType"), EName(ns, "restriction"), EName(ns, "enumeration"),
        EName(ns, "list"), EName(ns, "union"), EName(ns, "key"),
        EName(ns, "selector"), EName(ns, "field"), EName(ns, "attributeGroup"),
        EName(ns, "anyAttribute"), EName(ns, "whiteSpace"), EName(ns, "fractionDigits"),
        EName(ns, "pattern"), EName(ns, "any"), EName(ns, "appinfo"),
        EName(ns, "minLength"), EName(ns, "maxInclusive"), EName(ns, "minInclusive"),
        EName(ns, "notation"))

    expectResult(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    // Remember, coalescing is set to true!
    expectResult(Set(0, 1)) {
      val result = root \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: Elem): Unit = {
      val forChoiceDefOption: Option[Elem] = {
        val result = rootElm filterChildElems { e => e.resolvedName == EName(ns, "simpleType") && e.attribute(EName("name")) == "formChoice" }
        result.headOption
      }

      expectResult(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.filterElems(EName(ns, "documentation")) flatMap { e => e.trimmedText } mkString ""

      expectResult("A utility type, not for public use") {
        forChoiceDefDocumentation.trim
      }
    }

    checkForChoiceDocumentation(root)

    def checkCommentWithEscapedChar(rootElm: Elem): Unit = {
      val documentationElms =
        for {
          annotationElm <- rootElm \ EName(ns, "annotation")
          documentationElm <- annotationElm \ EName(ns, "documentation")
        } yield documentationElm

      val documentationText = documentationElms.drop(1).headOption map { e => e.trimmedText } getOrElse ""

      // The XML string contains "&lt;", but the parsed text should contain an unescaped "<" instead
      expectResult(true) {
        documentationText.containsSlice("""XML Schema language.  The documentation (within <documentation> elements)""")
      }
    }

    checkCommentWithEscapedChar(root)

    def checkIdentityConstraintElm(rootElm: Elem): Unit = {
      val identityConstraintElms =
        for {
          schemaElm <- rootElm filterElems { e =>
            e.resolvedName == EName(ns, "element") &&
              e.attributeOption(EName("name")) == Some("schema") &&
              e.attributeOption(EName("id")) == Some("schema")
          }
          idConstraintElm <- schemaElm filterChildElems { e =>
            e.resolvedName == EName(ns, "key") &&
              e.attributeOption(EName("name")) == Some("identityConstraint")
          }
        } yield idConstraintElm

      expectResult(1) {
        identityConstraintElms.size
      }

      val selectorElms = identityConstraintElms.head \ EName(ns, "selector")

      expectResult(1) {
        selectorElms.size
      }

      expectResult(""".//xs:key|.//xs:unique|.//xs:keyref""") {
        selectorElms.head.attributeOption(EName("xpath")).getOrElse("")
      }
    }

    checkIdentityConstraintElm(root)

    def checkComplexTypeElm(rootElm: Elem): Unit = {
      val complexTypeElms =
        rootElm filterElems { e =>
          e.resolvedName == EName(ns, "complexType") &&
            e.attributeOption(EName("name")) == Some("element") &&
            e.attributeOption(EName("abstract")) == Some("true")
        }

      expectResult(1) {
        complexTypeElms.size
      }

      val extensionElms = complexTypeElms.head.filterElems(EName(ns, "extension"))
      val sequenceElms = complexTypeElms.head.filterElems(EName(ns, "sequence"))
      val choiceElms = complexTypeElms.head.filterElems(EName(ns, "choice"))
      val elementElms = complexTypeElms.head.filterElems(EName(ns, "element"))
      val groupElms = complexTypeElms.head.filterElems(EName(ns, "group"))
      val attributeElms = complexTypeElms.head.filterElems(EName(ns, "attribute"))
      val attributeGroupElms = complexTypeElms.head.filterElems(EName(ns, "attributeGroup"))

      expectResult(Set(EName("base"))) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }
      expectResult(Set("xs:annotated")) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.values }
        result.toSet
      }

      expectResult(Set()) {
        val result = sequenceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("minOccurs"))) {
        val result = choiceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("name"), EName("type"))) {
        val result = elementElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("ref"), EName("minOccurs"), EName("maxOccurs"))) {
        val result = groupElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("name"), EName("type"), EName("use"), EName("default"))) {
        val result = attributeElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("ref"))) {
        val result = attributeGroupElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }
    }

    checkComplexTypeElm(root)

    def checkFieldPattern(rootElm: Elem): Unit = {
      val fieldElms = rootElm filterElems { e =>
        e.resolvedName == EName(ns, "element") &&
          e.attributeOption(EName("name")) == Some("field") &&
          e.attributeOption(EName("id")) == Some("field")
      }

      val patternElms = fieldElms flatMap { e => e.filterElems(EName(ns, "pattern")) }

      expectResult(1) {
        patternElms.size
      }

      expectResult("""(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*""") {
        patternElms.head.attributeOption(EName("value")).getOrElse("")
      }
    }

    checkFieldPattern(root)

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult(xsElmENames) {
      val result = root2 \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(0, 1)) {
      val result = root2 \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    checkForChoiceDocumentation(root2)
    checkCommentWithEscapedChar(root2)
    checkIdentityConstraintElm(root2)
    checkComplexTypeElm(root2)
    checkFieldPattern(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expectResult(xsElmENames) {
      val result = root3 \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(0, 1)) {
      val result = root3 \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
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

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = staxParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: Elem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expectResult(true) {
        childOption.isDefined
      }
      // Remember, coalescing is set to true!
      expectResult(1) {
        childOption.get.textChildren.size
      }
      val text = "This text contains an entity reference, viz. hi"
      expectResult(text) {
        childOption.get.trimmedText.take(text.length)
      }
    }

    checkChildText(root)

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root3)
  }

  @Test def testParseXmlWithNonExpandedEntityRef() {
    // 1. Parse XML file into Elem

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, java.lang.Boolean.FALSE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = staxParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: Elem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expectResult(true) {
        childOption.isDefined
      }
      expectResult(2) {
        val result = childOption.get.textChildren filter { t => t.text.trim != "" }
        result.size
      }
      expectResult(1) {
        val result = childOption.get.children collect { case er: EntityRef => er }
        result.size
      }
      expectResult(EntityRef("hello")) {
        val entityRefs = childOption.get.children collect { case er: EntityRef => er }
        val entityRef: EntityRef = entityRefs.head
        entityRef
      }
      val s = "This text contains an entity reference, viz."
      expectResult(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)

    // 2. Write Elem to an XML string

    // The entity references are lost in the conversion within the print method!
    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // No check on entity references, because they were lost in the conversion back to StAX events

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithNamespaceUndeclarations() {
    // 1. Parse XML file into Elem

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val root: Elem = staxParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithEscapedChars() {
    // 1. Parse XML file into Elem

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val root: Elem = staxParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      expectResult(2) {
        childElms.size
      }

      val text = "Jansen & co"

      // Remember: we set the parser to coalescing!
      expectResult(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }

      expectResult(Set(text)) {
        val result = childElms map { e => e.attributeOption(EName("about")).getOrElse("Missing text") }
        result.toSet
      }

      expectResult(Set(text)) {
        val result = rootElm.commentChildren map { c => c.text.trim }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)
  }

  @Test def testParseXmlWithSpecialChars() {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance

    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val root: Elem = staxParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      expectResult(2) {
        childElms.size
      }

      val text = "\u20AC 200"

      expectResult(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Convert to NodeBuilder and back, and check again

    val root2: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 3. Show the output with different output encodings

    val printer = DocumentPrinterUsingStax.newInstance()

    val utf8Encoding = "utf-8"
    // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6452107
    // TODO Try to fix the test for ISO-8859-1.
    val iso8859_1Encoding = "utf-8"

    val utf8Output = printer.print(Document(root), utf8Encoding)
    val iso8859_1Output = printer.print(Document(root), iso8859_1Encoding)

    logger.info("UTF-8 output (with euro) converted to String:%n%s".format(new String(utf8Output, utf8Encoding)))
    logger.info("ISO 8859-1 output (with euro) converted to String:%n%s".format(new String(iso8859_1Output, iso8859_1Encoding)))

    val doc1 = staxParser.parse(new jio.ByteArrayInputStream(utf8Output))
    // val doc2 = staxParser.parse(new jio.ByteArrayInputStream(iso8859_1Output))

    doChecks(doc1.documentElement)
    // doChecks(doc2.documentElement)

    // logger.info(
    //   "ISO 8859-1 output (with euro) parsed and printed again, as UTF-8:%n%s".format(printer.print(doc2)))
  }

  @Test def testParseGeneratedHtml() {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance
    val is = classOf[StaxInteropTest].getResourceAsStream("books.xml")

    val root: Elem = staxParser.parse(is).documentElement

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
        bookElm.filterElems(EName("{http://bookstore}Author")) map { e =>
          "%s %s".format(
            e.getChildElem(EName("{http://bookstore}First_Name")).trimmedText,
            e.getChildElem(EName("{http://bookstore}Last_Name")).trimmedText)
        }

      val authors = authorNames.mkString(", ")

      val result = bookFormatString.format(
        bookElm.getChildElem(EName("{http://bookstore}Title")).trimmedText,
        bookElm.attributeOption(EName("ISBN")).getOrElse(""),
        bookElm.attributeOption(EName("Edition")).getOrElse(""),
        authors,
        bookElm.attributeOption(EName("Price")).getOrElse(""))
      result
    }

    val booksHtmlString = root.filterElems(EName("{http://bookstore}Book")) map { e => bookHtmlString(e) } mkString ("\n")
    val htmlString = htmlFormatString.format(booksHtmlString)

    // 3. Parse HTML string (which is also valid XML in this case) into Document

    val htmlRoot: Elem = staxParser.parse(new jio.ByteArrayInputStream(htmlString.getBytes("utf-8"))).documentElement

    // 4. Check the parsed HTML

    val tableRowElms = htmlRoot.filterElems(EName("tr")).drop(1)

    expectResult(4) {
      tableRowElms.size
    }

    val isbnElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(1).headOption }
    val isbns = isbnElms map { e => e.trimmedText }

    expectResult(Set("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-0-11-222222-3", "ISBN-9-88-777777-6")) {
      isbns.toSet
    }

    val authorsElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(3).headOption }
    val authors = authorsElms map { e => e.trimmedText }

    expectResult(Set(
      "Jeffrey Ullman, Jennifer Widom",
      "Hector Garcia-Molina, Jeffrey Ullman, Jennifer Widom",
      "Jeffrey Ullman, Hector Garcia-Molina",
      "Jennifer Widom")) {
      authors.toSet
    }
  }

  @Test def testParseBrokenXml() {
    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setXMLResolver(new LoggingResolver)

    val staxParser = new DocumentParserUsingStax(xmlInputFactory)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()

    val is = new jio.ByteArrayInputStream(brokenXmlString.getBytes("utf-8"))

    intercept[Exception] {
      staxParser.parse(is).documentElement
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testParseGroovyXmlExample() {
    val parser = DocumentParserUsingStax.newInstance

    val doc = parser.parse(classOf[StaxInteropTest].getResourceAsStream("cars.xml"))

    expectResult("records") {
      doc.documentElement.localName
    }

    val recordsElm = doc.documentElement

    expectResult(3) {
      (recordsElm \ (_.localName == "car")).size
    }

    expectResult(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ (_.localName == "car"))(0)

    expectResult("car") {
      firstRecordElm.localName
    }

    expectResult("Holden") {
      firstRecordElm.attribute(EName("make"))
    }

    expectResult("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    expectResult(2) {
      val carElms = recordsElm \ (_.localName == "car")
      val result = carElms filter { e => e.attributeOption(EName("make")).getOrElse("").contains('e') }
      result.size
    }

    expectResult(Set("Holden", "Peel")) {
      val carElms = recordsElm \ (_.localName == "car")
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute(EName("make")))).toSet
    }

    expectResult(Set("speed", "size", "price")) {
      val result = recordsElm.findAllElemsOrSelf collect { case e if e.attributeOption(EName("type")).isDefined => e.attribute(EName("type")) }
      result.toSet
    }

    import NodeBuilder._

    val countryPath = ElemPathBuilder.from(QName("car") -> 0, QName("country") -> 0).build(Scope.Empty)
    val updatedCountryElm = textElem(QName("country"), "New Zealand").build()
    val updatedDoc = doc.updated(countryPath, updatedCountryElm)

    expectResult("New Zealand") {
      updatedDoc.documentElement.filterChildElems(_.localName == "car")(0).getChildElem(_.localName == "country").trimmedText
    }

    expectResult(List("Royale", "P50", "HSV Maloo")) {
      val carElms = recordsElm \ (_.localName == "car")
      val resultElms = carElms sortBy { e => e.attributeOption(EName("year")).getOrElse("0").toInt }
      resultElms map { e => e.attribute(EName("name")) }
    }
  }

  @Test def testParseFileWithUtf8Bom() {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance

    val is = classOf[StaxInteropTest].getResourceAsStream("books.xml")
    val ba = Stream.continually(is.read()).takeWhile(b => b != -1).map(_.toByte).toArray
    val baWithBom = addUtf8Bom(ba)
    assert(baWithBom.size == ba.size + 3)
    assert(baWithBom.toSeq.drop(3) == ba.toSeq)

    val root: Elem = staxParser.parse(new jio.ByteArrayInputStream(baWithBom)).documentElement

    expectResult(4) {
      (root \\! (_.localName == "Book")).size
    }
    expectResult(4) {
      (root \\! (_.localName == "Magazine")).size
    }
  }

  private def addUtf8Bom(ba: Array[Byte]): Array[Byte] = Array[Byte](0xEF.toByte, 0xBB.toByte, 0xBF.toByte) ++ ba

  class LoggingResolver extends XMLResolver {
    override def resolveEntity(publicId: String, systemId: String, baseUri: String, namespace: String): AnyRef = {
      logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))
      // Default behaviour
      val is: jio.InputStream = null
      is
    }
  }

  class DtdSuppressionResolver extends XMLResolver {
    override def resolveEntity(publicId: String, systemId: String, baseUri: String, namespace: String): AnyRef = {
      logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))
      new java.io.StringReader("")
    }
  }
}
