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
import org.w3c.dom.{ Element, DOMErrorHandler, DOMError }
import org.w3c.dom.ls.{ DOMImplementationLS, LSParser, LSInput, LSResourceResolver, LSException }
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.xml.sax.{ EntityResolver, InputSource, ErrorHandler, SAXParseException }
import javax.xml.transform.stream.StreamSource
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import NodeBuilder._
import parse.DocumentParserUsingDomLS
import print.DocumentPrinterUsingDomLS
import convert.DomConversions._

/**
 * DOM LS interoperability test case.
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
class DomLSInteropTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDomLS.newInstance
    val is = classOf[DomLSInteropTest].getResourceAsStream("books.xml")

    val root: Elem = domParser.parse(is).documentElement

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElems map (e => e.localName)).toSet
    }
    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(8) {
      root.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect(3) {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 2. Convert Elem to a DOM element

    val printer = DocumentPrinterUsingDomLS.newInstance

    val db2 = printer.docBuilderCreator(printer.docBuilderFactory)
    val domDoc2: org.w3c.dom.Document = db2.newDocument
    val element = convertElem(root)(domDoc2)
    domDoc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(domDoc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root2.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root2.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root2.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root2 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
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
    expect(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root3.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect {
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

    val bis = new jio.ByteArrayInputStream(xmlString2.getBytes("utf-8"))
    val doc2 = domParser.parse(bis)

    val root4 = doc2.documentElement

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root4.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root4.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root4.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root4 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 7. Convert to resolved.Elem, and check again

    val root5: resolved.Elem = resolved.Elem(doc.documentElement)

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root5.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root5.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root5.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root5 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 8. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = doc.toString

    assert(treeRepr.trim.startsWith("document("), "Expected the tree representation to start with 'document('")

    val doc6: Document = {
      import TreeReprParsers._

      val parseResult = parseAll(document, treeRepr)
      parseResult.get.build()
    }

    val root6 = doc6.documentElement

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root6.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root6.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root6.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root6 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 9. Serialize the corresponding NodeBuilder, deserialize it, and check again.

    val rootDocBuilder = DocBuilder.fromDocument(Document(root))
    val bos = new jio.ByteArrayOutputStream
    val oos = new jio.ObjectOutputStream(bos)

    oos.writeObject(rootDocBuilder)

    val objectBytes = bos.toByteArray

    assert(objectBytes.size >= 1000 && objectBytes.size <= 50000, "Expected the serialized document to be >= 1000 and <= 50000 bytes")

    val bis2 = new jio.ByteArrayInputStream(objectBytes)
    val ois = new jio.ObjectInputStream(bis2)

    val rootDocBuilder2 = ois.readObject().asInstanceOf[DocBuilder]
    val doc7 = rootDocBuilder2.build()
    val root7 = doc7.documentElement

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root7.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root7.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root7.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root7 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 10. Serialize the Document, deserialize it, and check again.

    val rootDoc = Document(root)
    val bos3 = new jio.ByteArrayOutputStream
    val oos3 = new jio.ObjectOutputStream(bos3)

    oos3.writeObject(rootDoc)

    val objectBytes3 = bos3.toByteArray

    assert(objectBytes3.size >= 1000 && objectBytes3.size <= 50000, "Expected the serialized document to be >= 1000 and <= 50000 bytes")

    val bis3 = new jio.ByteArrayInputStream(objectBytes3)
    val ois3 = new jio.ObjectInputStream(bis3)

    val doc8 = ois3.readObject().asInstanceOf[Document]
    val root8 = doc8.documentElement

    expect((root.findAllElems map (e => e.localName)).toSet) {
      (root8.findAllElems map (e => e.localName)).toSet
    }
    expect((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root8.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expect(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root8.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expect {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root8 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  @Test def testParseStrangeXml() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDomLS.newInstance
    val is = classOf[DomLSInteropTest].getResourceAsStream("strangeXml.xml")

    val root: Elem = domParser.parse(is).documentElement

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val dbf = DocumentBuilderFactory.newInstance
    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 6. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  @Test def testParseDefaultNamespaceXml() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDomLS.newInstance
    val is = classOf[DomLSInteropTest].getResourceAsStream("trivialXml.xml")

    val document: Document = domParser.parse(is)
    val root: Elem = document.documentElement

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(QName("root"), QName("child"))) {
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

    val dbf = DocumentBuilderFactory.newInstance
    val db2 = dbf.newDocumentBuilder
    val doc2 = convertDocument(document)(db2.newDocument)

    // 3. Convert DOM element into Elem

    val document2: eu.cdevreeze.yaidom.Document = convertToDocument(doc2)
    val root2: Elem = document2.documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(QName("root"), QName("child"))) {
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

    val document3: eu.cdevreeze.yaidom.Document = DocBuilder.fromDocument(document2).build()
    val root3: Elem = document3.documentElement

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(QName("root"), QName("child"))) {
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

    // 6. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = document.toString

    assert(treeRepr.trim.startsWith("document("), "Expected the tree representation to start with 'document('")

    val document4: Document = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.document, treeRepr)
      parseResult.get.build()
    }

    val root4 = document4.documentElement

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(QName("root"), QName("child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("This is trivial XML") {
      val result = document4.comments map { com => com.text.trim }
      result.mkString
    }
    expect("Trivial XML") {
      val result = root4.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
    // 1. Parse XML file into Elem

    def createParser(domImplLS: DOMImplementationLS): LSParser = {
      val parser = domImplLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      val resourceResolver = new LSResourceResolver {
        override def resolveResource(tpe: String, namespaceURI: String, publicId: String, systemId: String, baseURI: String): LSInput = {
          logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            val is = classOf[DomLSInteropTest].getResourceAsStream("XMLSchema.dtd")
            val input = domImplLS.createLSInput()
            input.setByteStream(is)
            input
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            val is = classOf[DomLSInteropTest].getResourceAsStream("datatypes.dtd")
            val input = domImplLS.createLSInput()
            input.setByteStream(is)
            input
          } else {
            // Default behaviour
            null
          }
        }
      }
      parser.getDomConfig.setParameter("resource-resolver", resourceResolver)
      parser
    }

    val domParser = DocumentParserUsingDomLS.newInstance() withParserCreator (createParser _)

    val is = classOf[DomLSInteropTest].getResourceAsStream("XMLSchema.xsd")

    val root: Elem = domParser.parse(is).documentElement

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

    expect(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root \\ { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: Elem): Unit = {
      val forChoiceDefOption: Option[Elem] = {
        val result = rootElm filterChildElems { e => e.resolvedName == EName(ns, "simpleType") && e.attribute(EName("name")) == "formChoice" }
        result.headOption
      }

      expect(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.filterElems(EName(ns, "documentation")) flatMap { e => e.trimmedText } mkString ""

      expect("A utility type, not for public use") {
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
      expect(true) {
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

      expect(1) {
        identityConstraintElms.size
      }

      val selectorElms = identityConstraintElms.head \ EName(ns, "selector")

      expect(1) {
        selectorElms.size
      }

      expect(""".//xs:key|.//xs:unique|.//xs:keyref""") {
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

      expect(1) {
        complexTypeElms.size
      }

      val extensionElms = complexTypeElms.head.filterElems(EName(ns, "extension"))
      val sequenceElms = complexTypeElms.head.filterElems(EName(ns, "sequence"))
      val choiceElms = complexTypeElms.head.filterElems(EName(ns, "choice"))
      val elementElms = complexTypeElms.head.filterElems(EName(ns, "element"))
      val groupElms = complexTypeElms.head.filterElems(EName(ns, "group"))
      val attributeElms = complexTypeElms.head.filterElems(EName(ns, "attribute"))
      val attributeGroupElms = complexTypeElms.head.filterElems(EName(ns, "attributeGroup"))

      expect(Set(EName("base"))) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }
      expect(Set("xs:annotated")) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.values }
        result.toSet
      }

      expect(Set()) {
        val result = sequenceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expect(Set(EName("minOccurs"))) {
        val result = choiceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expect(Set(EName("name"), EName("type"))) {
        val result = elementElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expect(Set(EName("ref"), EName("minOccurs"), EName("maxOccurs"))) {
        val result = groupElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expect(Set(EName("name"), EName("type"), EName("use"), EName("default"))) {
        val result = attributeElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expect(Set(EName("ref"))) {
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

      expect(1) {
        patternElms.size
      }

      expect("""(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*""") {
        patternElms.head.attributeOption(EName("value")).getOrElse("")
      }
    }

    checkFieldPattern(root)

    // 2. Convert Elem to a DOM element

    val dbf = DocumentBuilderFactory.newInstance
    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(xsElmENames) {
      val result = root2 \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root2 \\ { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
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
      val result = root3 \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root3 \\ { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    checkForChoiceDocumentation(root3)
    checkCommentWithEscapedChar(root3)
    checkIdentityConstraintElm(root3)
    checkComplexTypeElm(root3)
    checkFieldPattern(root3)

    // 6. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expect(xsElmENames) {
      val result = root4 \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root4 \\ { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    checkForChoiceDocumentation(root4)
    checkCommentWithEscapedChar(root4)
    checkIdentityConstraintElm(root4)
    checkComplexTypeElm(root4)
    checkFieldPattern(root4)
  }

  @Test def testParseXmlWithExpandedEntityRef() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDomLS.newInstance
    val is = classOf[DomLSInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: Elem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expect(true) {
        childOption.isDefined
      }
      expect(1) {
        childOption.get.textChildren.size
      }
      val text = "This text contains an entity reference, viz. hi"
      expect(text) {
        val txt = childOption.get.trimmedText
        txt.take(text.length)
      }
    }

    checkChildText(root)

    // 2. Convert Elem to a DOM element

    val dbf = DocumentBuilderFactory.newInstance
    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root3)

    // 6. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root4)
  }

  @Test def testParseXmlWithNonExpandedEntityRef() {
    // 1. Parse XML file into Elem

    def createParser(domImplLS: DOMImplementationLS): LSParser = {
      val parser = domImplLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("entities", java.lang.Boolean.TRUE)
      parser
    }

    val domParser = DocumentParserUsingDomLS.newInstance().withParserCreator(createParser _)
    val is = classOf[DomLSInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: Elem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
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

    val dbf = DocumentBuilderFactory.newInstance
    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root3)

    // 6. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root4)
  }

  @Test def testParseXmlWithNamespaceUndeclarations() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDomLS.newInstance
    val is = classOf[DomLSInteropTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val dbf = DocumentBuilderFactory.newInstance
    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 6. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithEscapedChars() {
    // 1. Parse XML file into Elem

    // We do not need to explicitly set coalescing to true
    val domParser = DocumentParserUsingDomLS.newInstance()
    val is = classOf[DomLSInteropTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
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
        val result = childElms map { e => e.attributeOption(EName("about")).getOrElse("Missing text") }
        result.toSet
      }

      expect(Set(text)) {
        val result = rootElm.commentChildren map { c => c.text.trim }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Convert Elem to a DOM element

    val dbf = DocumentBuilderFactory.newInstance
    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)

    // 6. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root4: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root4.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root4)
  }

  @Test def testParseXmlWithSpecialChars() {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDomLS.newInstance

    val is = classOf[DomLSInteropTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
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

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 3. Print to tree representation String and parse back that DSL String, and check again

    val treeRepr: String = root.toString

    assert(treeRepr.trim.startsWith("elem("), "Expected the tree representation to start with 'elem('")

    val root3: Elem = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.element, treeRepr)
      parseResult.get.build()
    }

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)

    // 4. Show the output with different output encodings

    val printer = DocumentPrinterUsingDomLS.newInstance() withSerializerCreator { domImpl =>
      val serializer = domImpl.createLSSerializer()
      // This configuration fixes the following exception:
      // org.w3c.dom.ls.LSException: Attribute "xmlns" was already specified for element "root"
      serializer.getDomConfig.setParameter("namespaces", java.lang.Boolean.FALSE)
      serializer
    }

    val utf8Encoding = "utf-8"
    val iso8859_1Encoding = "ISO-8859-1"

    val utf8Output = printer.print(Document(root), utf8Encoding)
    val iso8859_1Output = printer.print(Document(root), iso8859_1Encoding)

    logger.info("UTF-8 output (with euro) converted to String:%n%s".format(new String(utf8Output, utf8Encoding)))
    logger.info("ISO 8859-1 output (with euro) converted to String:%n%s".format(new String(iso8859_1Output, iso8859_1Encoding)))

    val doc1 = domParser.parse(new jio.ByteArrayInputStream(utf8Output))
    val doc2 = domParser.parse(new jio.ByteArrayInputStream(iso8859_1Output))

    doChecks(doc1.documentElement)
    doChecks(doc2.documentElement)

    logger.info(
      "ISO 8859-1 output (with euro) parsed and printed again, as UTF-8:%n%s".format(printer.print(doc2)))
  }

  @Test def testParseBrokenXml() {
    var errorCount = 0

    class MyErrorHandler extends DOMErrorHandler {

      override def handleError(exc: DOMError): Boolean = {
        errorCount += 1
        true
      }
    }

    def createParser(domImplLS: DOMImplementationLS): LSParser = {
      val parser = domImplLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("error-handler", new MyErrorHandler)
      parser
    }

    val domParser = DocumentParserUsingDomLS.newInstance().withParserCreator(createParser _)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()

    val is = new jio.ByteArrayInputStream(brokenXmlString.getBytes("utf-8"))

    intercept[LSException] {
      domParser.parse(is).documentElement
    }
    expect(1) {
      errorCount
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testParseGroovyXmlExample() {
    val parser = DocumentParserUsingDomLS.newInstance

    val doc = parser.parse(classOf[DomLSInteropTest].getResourceAsStream("cars.xml"))

    expect("records") {
      doc.documentElement.localName
    }

    val recordsElm = doc.documentElement

    expect(3) {
      (recordsElm \ "car").size
    }

    expect(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ "car")(0)

    expect("car") {
      firstRecordElm.localName
    }

    expect("Holden") {
      firstRecordElm.attribute(EName("make"))
    }

    expect("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    expect(2) {
      val carElms = recordsElm \ "car"
      val result = carElms filter { e => e.attributeOption(EName("make")).getOrElse("").contains('e') }
      result.size
    }

    expect(Set("Holden", "Peel")) {
      val carElms = recordsElm \ "car"
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute(EName("make")))).toSet
    }

    expect(Set("speed", "size", "price")) {
      val result = recordsElm collectFromElemsOrSelf { case e if e.attributeOption(EName("type")).isDefined => e.attribute(EName("type")) }
      result.toSet
    }

    import NodeBuilder._

    val countryPath = ElemPathBuilder.from(QName("car") -> 0, QName("country") -> 0).build(Scope.Empty)
    val updatedCountryElm = textElem(QName("country"), "New Zealand").build()
    val updatedDoc = doc.updated(countryPath, updatedCountryElm)

    expect("New Zealand") {
      updatedDoc.documentElement.filterChildElems(_.localName == "car")(0).getChildElem(_.localName == "country").trimmedText
    }

    expect(List("Royale", "P50", "HSV Maloo")) {
      val carElms = recordsElm \ "car"
      val resultElms = carElms sortBy { e => e.attributeOption(EName("year")).getOrElse("0").toInt }
      resultElms map { e => e.attribute(EName("name")) }
    }
  }

  @Test def testNoAdjacentTextNodes() {
    val parser = DocumentParserUsingDomLS.newInstance

    val xmlString = """<root><a> a <![CDATA[b]]> c </a></root>"""
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    val aElm = doc.documentElement.getChildElem(_.localName == "a")

    expect(1) {
      aElm.children.size
    }
    expect(1) {
      aElm.textChildren.size
    }
    expect(" a b c ") {
      aElm.textChildren(0).text
    }
    expect(" a b c ") {
      aElm.text
    }
  }

  @Test def testNoEmptyTextNodes() {
    val parser = DocumentParserUsingDomLS.newInstance

    val xmlString = """<root><a></a><b><![CDATA[]]></b><c/></root>"""
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    val aElm = doc.documentElement.getChildElem(_.localName == "a")
    val bElm = doc.documentElement.getChildElem(_.localName == "b")
    val cElm = doc.documentElement.getChildElem(_.localName == "c")

    expect(0) {
      aElm.children.size
    }
    expect(0) {
      bElm.children.size
    }
    expect(0) {
      cElm.children.size
    }
  }

  @Test def testKeepCData() {
    val parser1 = DocumentParserUsingDomLS.newInstance()

    val parser2 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("cdata-sections", java.lang.Boolean.TRUE)
      parser
    }

    val parser3 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("cdata-sections", java.lang.Boolean.FALSE)
      parser
    }

    val xmlString = """<root><a><![CDATA[abc def]]></a></root>"""

    val doc1 = parser1.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))
    val doc2 = parser2.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))
    val doc3 = parser3.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    val aElm1 = doc1.documentElement.getChildElem(_.localName == "a")
    val aElm2 = doc2.documentElement.getChildElem(_.localName == "a")
    val aElm3 = doc3.documentElement.getChildElem(_.localName == "a")

    expect(1) {
      aElm1.textChildren.size
    }
    expect(1) {
      aElm2.textChildren.size
    }
    expect(1) {
      aElm3.textChildren.size
    }

    expect("abc def") {
      aElm1.text
    }
    expect("abc def") {
      aElm2.text
    }
    expect("abc def") {
      aElm3.text
    }

    expect(true) {
      aElm2.textChildren(0).isCData
    }
    expect(false) {
      aElm3.textChildren(0).isCData
    }
  }

  @Test def testKeepComments() {
    val parser1 = DocumentParserUsingDomLS.newInstance()

    val parser2 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("comments", java.lang.Boolean.TRUE)
      parser
    }

    val parser3 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("comments", java.lang.Boolean.FALSE)
      parser
    }

    val xmlString = """<root><a>abc <!-- abc def --> def</a></root>"""

    val doc1 = parser1.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))
    val doc2 = parser2.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))
    val doc3 = parser3.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    val aElm1 = doc1.documentElement.getChildElem(_.localName == "a")
    val aElm2 = doc2.documentElement.getChildElem(_.localName == "a")
    val aElm3 = doc3.documentElement.getChildElem(_.localName == "a")

    expect(1) {
      doc1.allComments.size
    }
    expect(1) {
      doc2.allComments.size
    }
    expect(0) {
      doc3.allComments.size
    }

    expect("abc  def") {
      aElm1.text
    }
    expect("abc  def") {
      aElm2.text
    }
    expect("abc  def") {
      aElm3.text
    }

    expect(" abc def ") {
      aElm1.commentChildren.headOption.map(_.text).getOrElse("")
    }
    expect(" abc def ") {
      aElm2.commentChildren.headOption.map(_.text).getOrElse("")
    }
    expect("XXX") {
      aElm3.commentChildren.headOption.map(_.text).getOrElse("XXX")
    }
  }

  @Test def testElementContentWhitespace() {
    val parser1 = DocumentParserUsingDomLS.newInstance()

    val parser2 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("element-content-whitespace", java.lang.Boolean.TRUE)
      parser
    }

    val parser3 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      parser.getDomConfig.setParameter("element-content-whitespace", java.lang.Boolean.FALSE)
      parser
    }

    val xmlString =
      """|<?xml version="1.0"?>
         |<!DOCTYPE root [
         |  <!ELEMENT root (a)>
         |  <!ELEMENT a (b)>
         |]>
         |<root><a><b></b>   </a></root>
         |""".stripMargin

    val doc1 = parser1.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))
    val doc2 = parser2.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))
    val doc3 = parser3.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    val aElm1 = doc1.documentElement.getChildElem(_.localName == "a")
    val aElm2 = doc2.documentElement.getChildElem(_.localName == "a")
    val aElm3 = doc3.documentElement.getChildElem(_.localName == "a")

    expect(1) {
      aElm1.textChildren.size
    }
    expect(1) {
      aElm2.textChildren.size
    }
    expect(0) {
      aElm3.textChildren.size
    }

    expect("   ") {
      aElm1.text
    }
    expect("   ") {
      aElm2.text
    }
    expect("") {
      aElm3.text
    }
  }

  @Test def testValidate() {
    class MyErrorHandler extends DOMErrorHandler {

      override def handleError(exc: DOMError): Boolean = {
        sys.error(exc.toString)
      }
    }

    val parser1 = DocumentParserUsingDomLS.newInstance()

    val parser2 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/TR/REC-xml")
      parser.getDomConfig.setParameter("validate", java.lang.Boolean.TRUE)
      parser.getDomConfig.setParameter("error-handler", new MyErrorHandler)
      parser
    }

    val parser3 = DocumentParserUsingDomLS.newInstance() withParserCreator { domImpl =>
      val parser = domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, "http://www.w3.org/TR/REC-xml")
      parser.getDomConfig.setParameter("validate", java.lang.Boolean.FALSE)
      parser.getDomConfig.setParameter("error-handler", new MyErrorHandler)
      parser
    }

    val xmlString =
      """|<?xml version="1.0"?>
         |<!DOCTYPE root [
         |  <!ELEMENT root (a)>
         |  <!ELEMENT a (c)>
         |]>
         |<root><a><b></b></a></root>
         |""".stripMargin

    val doc1 = parser1.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    intercept[Exception] {
      parser2.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))
    }

    val doc3 = parser3.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    expect(3) {
      doc1.documentElement.findAllElemsOrSelf.size
    }
    expect(3) {
      doc3.documentElement.findAllElemsOrSelf.size
    }
  }

  @Test def testPrettyPrint() {
    val parser = DocumentParserUsingDomLS.newInstance()

    val xmlString =
      """|<?xml version="1.0"?>
         |<!DOCTYPE root [
         |  <!ELEMENT root (a)>
         |  <!ELEMENT a (b)>
         |]>
         |<root><a><b></b></a></root>
         |""".stripMargin

    val doc = parser.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    expect("") {
      doc.documentElement.findAllElemsOrSelf.map(_.text).mkString
    }

    val printer1 = DocumentPrinterUsingDomLS.newInstance withSerializerCreator { domImpl =>
      val writer = domImpl.createLSSerializer()
      writer.getDomConfig.setParameter("xml-declaration", java.lang.Boolean.FALSE)
      writer
    }

    val printer2 = DocumentPrinterUsingDomLS.newInstance() withSerializerCreator { domImpl =>
      val writer = domImpl.createLSSerializer()
      writer.getDomConfig.setParameter("format-pretty-print", java.lang.Boolean.TRUE)
      writer.getDomConfig.setParameter("xml-declaration", java.lang.Boolean.FALSE)
      writer
    }

    val xmlString1 = printer1.print(doc)
    val xmlString2 = printer2.print(doc)

    logger.info("Non-prettified xmlString1 (method testPrettyPrint):%n%s".format(xmlString1))
    logger.info("Prettified xmlString2 (method testPrettyPrint):%n%s".format(xmlString2))

    assert(xmlString1.size < xmlString2.size)
    assert(xmlString1.trim.size < xmlString2.trim.size)

    val doc1 = parser.parse(new jio.ByteArrayInputStream(xmlString1.getBytes("utf-8")))
    val doc2 = parser.parse(new jio.ByteArrayInputStream(xmlString2.getBytes("utf-8")))

    logger.info("Non-prettified xmlString1 after roundtripping (method testPrettyPrint):%n%s".format(printer1.print(doc1)))
    logger.info("Prettified xmlString2 after roundtripping (method testPrettyPrint):%n%s".format(printer1.print(doc2)))

    val text1 = doc1.documentElement.findAllElemsOrSelf.map(_.text).mkString
    val text2 = doc2.documentElement.findAllElemsOrSelf.map(_.text).mkString

    assert(text1.size < text2.size)
  }

  @Test def testPrettyPrintAgain() {
    val parser = DocumentParserUsingDomLS.newInstance()

    val xmlString =
      """|<?xml version="1.0"?>
         |<root><a><b></b></a></root>
         |""".stripMargin

    val doc = parser.parse(new jio.ByteArrayInputStream(xmlString.getBytes("utf-8")))

    expect("") {
      doc.documentElement.findAllElemsOrSelf.map(_.text).mkString
    }

    val printer1 = DocumentPrinterUsingDomLS.newInstance withSerializerCreator { domImpl =>
      val writer = domImpl.createLSSerializer()
      writer.getDomConfig.setParameter("xml-declaration", java.lang.Boolean.FALSE)
      writer
    }

    val printer2 = DocumentPrinterUsingDomLS.newInstance() withSerializerCreator { domImpl =>
      val writer = domImpl.createLSSerializer()
      writer.getDomConfig.setParameter("format-pretty-print", java.lang.Boolean.TRUE)
      writer.getDomConfig.setParameter("xml-declaration", java.lang.Boolean.FALSE)
      writer
    }

    val xmlString1 = printer1.print(doc)
    val xmlString2 = printer2.print(doc)

    logger.info("Non-prettified xmlString1 (method testPrettyPrintAgain):%n%s".format(xmlString1))
    logger.info("Prettified xmlString2 (method testPrettyPrintAgain):%n%s".format(xmlString2))

    assert(xmlString1.size < xmlString2.size)
    assert(xmlString1.trim.size < xmlString2.trim.size)

    val doc1 = parser.parse(new jio.ByteArrayInputStream(xmlString1.getBytes("utf-8")))
    val doc2 = parser.parse(new jio.ByteArrayInputStream(xmlString2.getBytes("utf-8")))

    logger.info("Non-prettified xmlString1 after roundtripping (method testPrettyPrintAgain):%n%s".format(printer1.print(doc1)))
    logger.info("Prettified xmlString2 after roundtripping (method testPrettyPrintAgain):%n%s".format(printer1.print(doc2)))

    val text1 = doc1.documentElement.findAllElemsOrSelf.map(_.text).mkString
    val text2 = doc2.documentElement.findAllElemsOrSelf.map(_.text).mkString

    assert(text1.size < text2.size)
  }

  @Test def testSuppressDtdResolution() {
    def createParser(domImplLS: DOMImplementationLS): LSParser = {
      val parser = domImplLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
      val resourceResolver = new LSResourceResolver {
        override def resolveResource(tpe: String, namespaceURI: String, publicId: String, systemId: String, baseURI: String): LSInput = {
          logger.info("Suppressing DTD resolution! Public ID: %s. System ID: %s".format(publicId, systemId))

          val input = domImplLS.createLSInput()
          input.setCharacterStream(new jio.StringReader(""))
          input
        }
      }
      parser.getDomConfig.setParameter("resource-resolver", resourceResolver)
      parser
    }

    val domParser = DocumentParserUsingDomLS.newInstance() withParserCreator (createParser _)

    val is = classOf[DomLSInteropTest].getResourceAsStream("XMLSchema.xsd")

    val root: Elem = domParser.parse(is).documentElement

    assert(root.findAllElemsOrSelf.size >= 100)
  }

  @Test def testParseFileWithUtf8Bom() {
    // 1. Parse XML file into Elem

    val parser = DocumentParserUsingDomLS.newInstance

    val is = classOf[DomLSInteropTest].getResourceAsStream("books.xml")
    val ba = Stream.continually(is.read()).takeWhile(b => b != -1).map(_.toByte).toArray
    val baWithBom = addUtf8Bom(ba)
    assert(baWithBom.size == ba.size + 3)

    val root: Elem = parser.parse(new jio.ByteArrayInputStream(baWithBom)).documentElement

    expect(4) {
      (root \\! "Book").size
    }
    expect(4) {
      (root \\! "Magazine").size
    }
  }

  private def addUtf8Bom(ba: Array[Byte]): Array[Byte] = Array[Byte](0xEF.toByte, 0xBB.toByte, 0xBF.toByte) ++ ba
}
