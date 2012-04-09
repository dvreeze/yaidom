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
import org.w3c.dom.{ Element }
import org.xml.sax.{ EntityResolver, InputSource, ErrorHandler, SAXParseException }
import javax.xml.transform.stream.StreamSource
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingDom
import print.DocumentPrinterUsingDom
import jinterop.DomConversions._

/**
 * DOM interoperability test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * To debug the DOM parsers, use JVM option -Djaxp.debug=1.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DomInteropTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore".ns
  private val nsGoogle = "http://www.google.com".ns
  private val nsFooBar = "urn:foo:bar".ns
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema".ns

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[DomInteropTest].getResourceAsStream("books.xml")

    val root: Elem = domParser.parse(is).documentElement

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElems map (e => e.localName)).toSet
    }
    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(8) {
      root.filterElemsOrSelfNamed(nsBookstore.ename("Title")).size
    }
    expect(3) {
      val result = root filterElemsOrSelf { e => e.resolvedName == nsBookstore.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val domDoc2: org.w3c.dom.Document = db2.newDocument
    val element = convertElem(root)(domDoc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(domDoc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root2.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root2.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelfNamed(nsBookstore.ename("Title")).size) {
      root2.filterElemsOrSelfNamed(nsBookstore.ename("Title")).size
    }
    expect {
      val result = root filterElemsOrSelf { e => e.resolvedName == nsBookstore.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root2 filterElemsOrSelf { e => e.resolvedName == nsBookstore.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root3.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root3.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelfNamed(nsBookstore.ename("Title")).size) {
      root3.filterElemsOrSelfNamed(nsBookstore.ename("Title")).size
    }
    expect {
      val result = root filterElemsOrSelf { e => e.resolvedName == nsBookstore.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root3 filterElemsOrSelf { e => e.resolvedName == nsBookstore.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 6. Print to XML and parse back, and check again

    val doc = Document(root3)

    val printer = DocumentPrinterUsingDom.newInstance
    val xmlString2 = printer.print(doc)

    val xmlString3 = printer.omittingXmlDeclaration.print(doc.documentElement)
    assert(xmlString2.startsWith("<?xml "))
    assert(!xmlString3.startsWith("<?xml "))
    assert(xmlString2.size >= xmlString3.size + "<?xml ".size)

    val bis = new jio.ByteArrayInputStream(xmlString2.getBytes("utf-8"))
    val doc2 = domParser.parse(bis)

    val root4 = doc2.documentElement

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root4.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root4.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelfNamed(nsBookstore.ename("Title")).size) {
      root4.filterElemsOrSelfNamed(nsBookstore.ename("Title")).size
    }
    expect {
      val result = root filterElemsOrSelf { e => e.resolvedName == nsBookstore.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root4 filterElemsOrSelf { e => e.resolvedName == nsBookstore.ename("Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  @Test def testParseStrangeXml() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[DomInteropTest].getResourceAsStream("strangeXml.xml")

    val root: Elem = domParser.parse(is).documentElement

    expect(Set("bar".ename, nsGoogle.ename("foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set("bar".ename, nsGoogle.ename("foo"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set("bar".ename, nsGoogle.ename("foo"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  @Test def testParseDefaultNamespaceXml() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXml.xml")

    val document: Document = domParser.parse(is)
    val root: Elem = document.documentElement

    expect(Set(nsFooBar.ename("root"), nsFooBar.ename("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("This is trivial XML") {
      val result = document.comments map { com => com.text.trim }
      result.mkString
    }
    expect("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = convertDocument(document)(db2.newDocument)

    // 3. Convert DOM element into Elem

    val document2: eu.cdevreeze.yaidom.Document = convertToDocument(doc2)
    val root2: Elem = document2.documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(nsFooBar.ename("root"), nsFooBar.ename("child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root2.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("This is trivial XML") {
      val result = document2.comments map { com => com.text.trim }
      result.mkString
    }
    expect("Trivial XML") {
      val result = root2.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 5. Convert to NodeBuilder and back, and check again

    val document3: eu.cdevreeze.yaidom.Document = NodeBuilder.fromDocument(document2)(Scope.Empty).build()
    val root3: Elem = document3.documentElement

    expect(Set(nsFooBar.ename("root"), nsFooBar.ename("child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root3.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("This is trivial XML") {
      val result = document3.comments map { com => com.text.trim }
      result.mkString
    }
    expect("Trivial XML") {
      val result = root3.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[DomInteropTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[DomInteropTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val domParser = new DocumentParserUsingDom(dbf, createDocumentBuilder _)

    val is = classOf[DomInteropTest].getResourceAsStream("XMLSchema.xsd")

    val root: Elem = domParser.parse(is).documentElement

    val ns = nsXmlSchema

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
      val result = root filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema.toString) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root filterElemsOrSelf { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: Elem): Unit = {
      val forChoiceDefOption: Option[Elem] =
        rootElm filterChildElems { e => e.resolvedName == ns.ename("simpleType") && e.attribute("name".ename) == "formChoice" } headOption

      expect(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.filterElemsNamed(ns.ename("documentation")) flatMap { e => e.trimmedText } mkString

      expect("A utility type, not for public use") {
        forChoiceDefDocumentation.trim
      }
    }

    checkForChoiceDocumentation(root)

    def checkCommentWithEscapedChar(rootElm: Elem): Unit = {
      val documentationElms =
        for {
          annotationElm <- rootElm.filterChildElemsNamed(ns.ename("annotation"))
          documentationElm <- annotationElm.filterChildElemsNamed(ns.ename("documentation"))
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
          schemaElm <- rootElm filterElems { e =>
            e.resolvedName == ns.ename("element") &&
              e.attributeOption("name".ename) == Some("schema") &&
              e.attributeOption("id".ename) == Some("schema")
          }
          idConstraintElm <- schemaElm filterChildElems { e =>
            e.resolvedName == ns.ename("key") &&
              e.attributeOption("name".ename) == Some("identityConstraint")
          }
        } yield idConstraintElm

      expect(1) {
        identityConstraintElms.size
      }

      val selectorElms = identityConstraintElms.head.filterChildElemsNamed(ns.ename("selector"))

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
        rootElm filterElems { e =>
          e.resolvedName == ns.ename("complexType") &&
            e.attributeOption("name".ename) == Some("element") &&
            e.attributeOption("abstract".ename) == Some("true")
        }

      expect(1) {
        complexTypeElms.size
      }

      val extensionElms = complexTypeElms.head.filterElemsNamed(ns.ename("extension"))
      val sequenceElms = complexTypeElms.head.filterElemsNamed(ns.ename("sequence"))
      val choiceElms = complexTypeElms.head.filterElemsNamed(ns.ename("choice"))
      val elementElms = complexTypeElms.head.filterElemsNamed(ns.ename("element"))
      val groupElms = complexTypeElms.head.filterElemsNamed(ns.ename("group"))
      val attributeElms = complexTypeElms.head.filterElemsNamed(ns.ename("attribute"))
      val attributeGroupElms = complexTypeElms.head.filterElemsNamed(ns.ename("attributeGroup"))

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
      val fieldElms = rootElm filterElems { e =>
        e.resolvedName == ns.ename("element") &&
          e.attributeOption("name".ename) == Some("field") &&
          e.attributeOption("id".ename) == Some("field")
      }

      val patternElms = fieldElms flatMap { e => e.filterElemsNamed(ns.ename("pattern")) }

      expect(1) {
        patternElms.size
      }

      expect("""(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*""") {
        patternElms.head.attributeOption("value".ename).getOrElse("")
      }
    }

    checkFieldPattern(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(xsElmENames) {
      val result = root2 filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema.toString) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root2 filterElemsOrSelf { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
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
      val result = root3 filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema.toString) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root3 filterElemsOrSelf { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
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

    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: Elem): Unit = {
      val childOption = rootElm.findElemNamed(ns.ename("child"))
      expect(true) {
        childOption.isDefined
      }
      expect(1) {
        childOption.get.textChildren.size
      }
      val text = "This text contains an entity reference, viz. hi"
      expect(text) {
        childOption.get.trimmedText.take(text.length)
      }
    }

    checkChildText(root)

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root3)
  }

  @Test def testParseXmlWithNonExpandedEntityRef() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: Elem): Unit = {
      val childOption = rootElm.findElemNamed(ns.ename("child"))
      expect(true) {
        childOption.isDefined
      }
      expect(2) {
        childOption.get.textChildren.size
      }
      expect(1) {
        val result = childOption.get.children collect { case er: EntityRef => er }
        result.size
      }
      expect(EntityRef("hello")) {
        val entityRefs = childOption.get.children collect { case er: EntityRef => er }
        val entityRef: EntityRef = entityRefs.head
        entityRef
      }
      val s = "This text contains an entity reference, viz."
      expect(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root3)
  }

  @Test def testParseXmlWithNamespaceUndeclarations() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithEscapedChars() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    dbf.setCoalescing(true)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElemsNamed(ns.ename("child"))
      expect(2) {
        childElms.size
      }

      val text = "Jansen & co"

      // Remember: we set the parser to coalescing!
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

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)
  }

  @Test def testParseXmlWithSpecialChars() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance

    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElemsNamed(ns.ename("child"))
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
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)
  }

  @Test def testParseGeneratedHtml() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[DomInteropTest].getResourceAsStream("books.xml")

    val root: Elem = domParser.parse(is).documentElement

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
        bookElm.filterElemsNamed("{http://bookstore}Author".ename) map { e =>
          "%s %s".format(e.getChildElemNamed("{http://bookstore}First_Name".ename).trimmedText, e.getChildElemNamed("{http://bookstore}Last_Name".ename).trimmedText)
        }

      val authors = authorNames.mkString(", ")

      val result = bookFormatString.format(
        bookElm.getChildElemNamed("{http://bookstore}Title".ename).trimmedText,
        bookElm.attributeOption("ISBN".ename).getOrElse(""),
        bookElm.attributeOption("Edition".ename).getOrElse(""),
        authors,
        bookElm.attributeOption("Price".ename).getOrElse(""))
      result
    }

    val booksHtmlString = root.filterElemsNamed("{http://bookstore}Book".ename) map { e => bookHtmlString(e) } mkString ("\n")
    val htmlString = htmlFormatString.format(booksHtmlString)

    // 3. Parse HTML string (which is also valid XML in this case) into Document

    val htmlRoot: Elem = domParser.parse(new jio.ByteArrayInputStream(htmlString.getBytes("utf-8"))).documentElement

    // 4. Check the parsed HTML

    val tableRowElms = htmlRoot.filterElemsNamed("tr".ename).drop(1)

    expect(4) {
      tableRowElms.size
    }

    val isbnElms = tableRowElms flatMap { rowElm => rowElm.filterChildElemsNamed("td".ename).drop(1).headOption }
    val isbns = isbnElms map { e => e.trimmedText }

    expect(Set("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-0-11-222222-3", "ISBN-9-88-777777-6")) {
      isbns.toSet
    }

    val authorsElms = tableRowElms flatMap { rowElm => rowElm.filterChildElemsNamed("td".ename).drop(3).headOption }
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

    class MyErrorHandler extends ErrorHandler {

      override def error(exc: SAXParseException) { errorCount += 1 }

      override def fatalError(exc: SAXParseException) { fatalErrorCount += 1 }

      override def warning(exc: SAXParseException) { warningCount += 1 }
    }

    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new LoggingEntityResolver)
      db.setErrorHandler(new MyErrorHandler)
      db
    }

    val domParser = new DocumentParserUsingDom(dbf, createDocumentBuilder _)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()

    val is = new jio.ByteArrayInputStream(brokenXmlString.getBytes("utf-8"))

    intercept[SAXParseException] {
      domParser.parse(is).documentElement
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
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testParseGroovyXmlExample() {
    val parser = DocumentParserUsingDom.newInstance

    val doc = parser.parse(classOf[DomInteropTest].getResourceAsStream("cars.xml"))

    expect("records") {
      doc.documentElement.localName
    }

    val recordsElm = doc.documentElement

    expect(3) {
      (recordsElm \ (_.localName == "car")).size
    }

    expect(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ (_.localName == "car"))(0)

    expect("car") {
      firstRecordElm.localName
    }

    expect("Holden") {
      firstRecordElm.attribute("make".ename)
    }

    expect("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    expect(2) {
      val carElms = recordsElm \ { _.localName == "car" }
      val result = carElms filter { e => e.attributeOption("make".ename).getOrElse("").contains('e') }
      result.size
    }

    expect(Set("Holden", "Peel")) {
      val carElms = recordsElm \ { _.localName == "car" }
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute("make".ename))).toSet
    }

    expect(Set("speed", "size", "price")) {
      val result = recordsElm collectFromElemsOrSelf { case e if e.attributeOption("type".ename).isDefined => e.attribute("type".ename) }
      result.toSet
    }

    import NodeBuilder._

    val countryPath = ElemPath.fromCanonicalXPath("/*/car[1]/country[1]")(Scope.Empty)
    val updatedCountryElm = elem(qname = "country".qname, children = List(text("New Zealand"))).build()
    val updatedDoc = doc.updated(countryPath, updatedCountryElm)

    expect("New Zealand") {
      updatedDoc.documentElement.filterChildElems(_.localName == "car")(0).getChildElem(_.localName == "country").trimmedText
    }

    expect(List("Royale", "P50", "HSV Maloo")) {
      val carElms = recordsElm \ { _.localName == "car" }
      val resultElms = carElms sortBy { e => e.attributeOption("year".ename).getOrElse("0").toInt }
      resultElms map { e => e.attribute("name".ename) }
    }
  }

  class LoggingEntityResolver extends EntityResolver {
    override def resolveEntity(publicId: String, systemId: String): InputSource = {
      logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))
      // Default behaviour
      null
    }
  }
}
