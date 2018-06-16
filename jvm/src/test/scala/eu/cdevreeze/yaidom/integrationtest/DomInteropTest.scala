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

package eu.cdevreeze.yaidom.integrationtest

import java.{ io => jio }
import java.{ util => jutil }

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.EntityResolver
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.convert.DomConversions.convertDocument
import eu.cdevreeze.yaidom.convert.DomConversions.convertElem
import eu.cdevreeze.yaidom.convert.DomConversions.convertToDocument
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.PathBuilder
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.DocBuilder
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.simple.NodeBuilder.textElem
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

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
class DomInteropTest extends FunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  test("testParse") {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[DomInteropTest].getResourceAsStream("books.xml")

    val root: Elem = domParser.parse(is).documentElement

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

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val domDoc2: org.w3c.dom.Document = db2.newDocument
    val element = convertElem(root)(domDoc2)
    domDoc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(domDoc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult((root.findAllElems map (e => e.localName)).toSet) {
      (root2.findAllElems map (e => e.localName)).toSet
    }
    assertResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root2.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root2.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root2 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    assertResult((root.findAllElems map (e => e.localName)).toSet) {
      (root3.findAllElems map (e => e.localName)).toSet
    }
    assertResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root3.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root3.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root3 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 6. Print to XML and parse back, and check again

    val doc = Document(root3)

    val printer = DocumentPrinterUsingDom.newInstance().withDocumentConverter(DomConversions)
    val xmlString2 = printer.print(doc)

    val xmlString3 = printer.print(doc.documentElement)
    assert(xmlString2.startsWith("<?xml "))
    assert(!xmlString3.startsWith("<?xml "))
    assert(xmlString2.size >= xmlString3.size + "<?xml ".size)

    val bis = new InputSource(new jio.StringReader(xmlString2))
    val doc2 = domParser.parse(bis)

    val root4 = doc2.documentElement

    assertResult((root.findAllElems map (e => e.localName)).toSet) {
      (root4.findAllElems map (e => e.localName)).toSet
    }
    assertResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root4.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root4.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root4 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 7. Convert to resolved.Elem, and check again

    val root5: resolved.Elem = resolved.Elem.from(doc.documentElement)

    assertResult((root.findAllElems map (e => e.localName)).toSet) {
      (root5.findAllElems map (e => e.localName)).toSet
    }
    assertResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root5.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root5.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root5 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }

    // 8. Serialize the corresponding NodeBuilder, deserialize it, and check again.

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

    assertResult((root.findAllElems map (e => e.localName)).toSet) {
      (root7.findAllElems map (e => e.localName)).toSet
    }
    assertResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root7.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root7.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult {
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

    assertResult((root.findAllElems map (e => e.localName)).toSet) {
      (root8.findAllElems map (e => e.localName)).toSet
    }
    assertResult((root.findAllElemsOrSelf map (e => e.localName)).toSet) {
      (root8.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    assertResult(root.filterElemsOrSelf(EName(nsBookstore, "Title")).size) {
      root8.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    assertResult {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    } {
      val result = root8 \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  test("testParseStrangeXml") {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[DomInteropTest].getResourceAsStream("strangeXml.xml")

    val root: Elem = domParser.parse(is).documentElement

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  test("testParseDefaultNamespaceXml") {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXml.xml")

    val document: Document = domParser.parse(is)
    val root: Elem = document.documentElement

    assertResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(QName("root"), QName("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    assertResult("This is trivial XML") {
      val result = document.comments map { com => com.text.trim }
      result.mkString
    }
    assertResult("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = convertDocument(document)(db2.newDocument)

    // 3. Convert DOM element into Elem

    val document2: eu.cdevreeze.yaidom.simple.Document = convertToDocument(doc2)
    val root2: Elem = document2.documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(QName("root"), QName("child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    assertResult("This is trivial XML") {
      val result = document2.comments map { com => com.text.trim }
      result.mkString
    }
    assertResult("Trivial XML") {
      val result = root2.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 5. Convert to NodeBuilder and back, and check again

    val document3: eu.cdevreeze.yaidom.simple.Document = DocBuilder.fromDocument(document2).build()
    val root3: Elem = document3.documentElement

    assertResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(QName("root"), QName("child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    assertResult("This is trivial XML") {
      val result = document3.comments map { com => com.text.trim }
      result.mkString
    }
    assertResult("Trivial XML") {
      val result = root3.findAllElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }
  }

  test("testParseSchemaXsd") {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

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

    val domParser = new DocumentParserUsingDom(dbf, createDocumentBuilder _, DomConversions)

    val is = classOf[DomInteropTest].getResourceAsStream("XMLSchema.xsd")

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

    assertResult(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption.contains(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(0, 1)) {
      val result = root \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: Elem): Unit = {
      val forChoiceDefOption: Option[Elem] = {
        val result = rootElm filterChildElems { e => e.resolvedName == EName(ns, "simpleType") && e.attribute(EName("name")) == "formChoice" }
        result.headOption
      }

      assertResult(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.filterElems(EName(ns, "documentation")) flatMap { e => e.trimmedText } mkString ""

      assertResult("A utility type, not for public use") {
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
      assertResult(true) {
        documentationText.containsSlice("""XML Schema language.  The documentation (within <documentation> elements)""")
      }
    }

    checkCommentWithEscapedChar(root)

    def checkIdentityConstraintElm(rootElm: Elem): Unit = {
      val identityConstraintElms =
        for {
          schemaElm <- rootElm filterElems { e =>
            e.resolvedName == EName(ns, "element") &&
              e.attributeOption(EName("name")).contains("schema") &&
              e.attributeOption(EName("id")).contains("schema")
          }
          idConstraintElm <- schemaElm filterChildElems { e =>
            e.resolvedName == EName(ns, "key") &&
              e.attributeOption(EName("name")).contains("identityConstraint")
          }
        } yield idConstraintElm

      assertResult(1) {
        identityConstraintElms.size
      }

      val selectorElms = identityConstraintElms.head \ EName(ns, "selector")

      assertResult(1) {
        selectorElms.size
      }

      assertResult(""".//xs:key|.//xs:unique|.//xs:keyref""") {
        selectorElms.head.attributeOption(EName("xpath")).getOrElse("")
      }
    }

    checkIdentityConstraintElm(root)

    def checkComplexTypeElm(rootElm: Elem): Unit = {
      val complexTypeElms =
        rootElm filterElems { e =>
          e.resolvedName == EName(ns, "complexType") &&
            e.attributeOption(EName("name")).contains("element") &&
            e.attributeOption(EName("abstract")).contains("true")
        }

      assertResult(1) {
        complexTypeElms.size
      }

      val extensionElms = complexTypeElms.head.filterElems(EName(ns, "extension"))
      val sequenceElms = complexTypeElms.head.filterElems(EName(ns, "sequence"))
      val choiceElms = complexTypeElms.head.filterElems(EName(ns, "choice"))
      val elementElms = complexTypeElms.head.filterElems(EName(ns, "element"))
      val groupElms = complexTypeElms.head.filterElems(EName(ns, "group"))
      val attributeElms = complexTypeElms.head.filterElems(EName(ns, "attribute"))
      val attributeGroupElms = complexTypeElms.head.filterElems(EName(ns, "attributeGroup"))

      assertResult(Set(EName("base"))) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }
      assertResult(Set("xs:annotated")) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.values }
        result.toSet
      }

      assertResult(Set()) {
        val result = sequenceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      assertResult(Set(EName("minOccurs"))) {
        val result = choiceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      assertResult(Set(EName("name"), EName("type"))) {
        val result = elementElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      assertResult(Set(EName("ref"), EName("minOccurs"), EName("maxOccurs"))) {
        val result = groupElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      assertResult(Set(EName("name"), EName("type"), EName("use"), EName("default"))) {
        val result = attributeElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      assertResult(Set(EName("ref"))) {
        val result = attributeGroupElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }
    }

    checkComplexTypeElm(root)

    def checkFieldPattern(rootElm: Elem): Unit = {
      val fieldElms = rootElm filterElems { e =>
        e.resolvedName == EName(ns, "element") &&
          e.attributeOption(EName("name")).contains("field") &&
          e.attributeOption(EName("id")).contains("field")
      }

      val patternElms = fieldElms flatMap { e => e.filterElems(EName(ns, "pattern")) }

      assertResult(1) {
        patternElms.size
      }

      assertResult("""(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*""") {
        patternElms.head.attributeOption(EName("value")).getOrElse("")
      }
    }

    checkFieldPattern(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult(xsElmENames) {
      val result = root2 \\ { e => e.resolvedName.namespaceUriOption.contains(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(0, 1)) {
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

    assertResult(xsElmENames) {
      val result = root3 \\ { e => e.resolvedName.namespaceUriOption.contains(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(0, 1)) {
      val result = root3 \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    checkForChoiceDocumentation(root3)
    checkCommentWithEscapedChar(root3)
    checkIdentityConstraintElm(root3)
    checkComplexTypeElm(root3)
    checkFieldPattern(root3)
  }

  test("testParseXmlWithExpandedEntityRef") {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: Elem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      assertResult(true) {
        childOption.isDefined
      }
      assertResult(1) {
        childOption.get.textChildren.size
      }
      val text = "This text contains an entity reference, viz. hi"
      assertResult(text) {
        val txt = childOption.get.trimmedText
        txt.take(text.length)
      }
    }

    checkChildText(root)

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root3)
  }

  test("testParseXmlWithNonExpandedEntityRef") {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: Elem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      assertResult(true) {
        childOption.isDefined
      }
      assertResult(2) {
        childOption.get.textChildren.size
      }
      assertResult(1) {
        val result = childOption.get.children collect { case er: EntityRef => er }
        result.size
      }
      assertResult(EntityRef("hello")) {
        val entityRefs = childOption.get.children collect { case er: EntityRef => er }
        val entityRef: EntityRef = entityRefs.head
        entityRef
      }
      val s = "This text contains an entity reference, viz."
      assertResult(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root3)
  }

  test("testParseXmlWithNamespaceUndeclarations") {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val db2 = domParser.docBuilderCreator(domParser.docBuilderFactory)
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  test("testParseXmlWithEscapedChars") {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    dbf.setCoalescing(true)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      assertResult(2) {
        childElms.size
      }

      val text = "Jansen & co"

      // Remember: we set the parser to coalescing!
      assertResult(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }

      assertResult(Set(text)) {
        val result = childElms map { e => e.attributeOption(EName("about")).getOrElse("Missing text") }
        result.toSet
      }

      assertResult(Set(text)) {
        val result = rootElm.commentChildren map { c => c.text.trim }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)
    doc2.appendChild(element)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToDocument(doc2).documentElement

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)
  }

  test("testParseXmlWithSpecialChars") {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance

    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val root: Elem = domParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: Elem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      assertResult(2) {
        childElms.size
      }

      val text = "\u20AC 200"

      assertResult(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }
    }

    doChecks(root)

    // 2. Convert to NodeBuilder and back, and check again

    val root2: Elem = NodeBuilder.fromElem(root)(Scope.Empty).build()

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 3. Show the output with different output encodings

    val printer = DocumentPrinterUsingDom.newInstance()

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

  test("testParseGeneratedHtml") {
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

    val htmlRoot: Elem = domParser.parse(new InputSource(new jio.StringReader(htmlString))).documentElement

    // 4. Check the parsed HTML

    val tableRowElms = htmlRoot.filterElems(EName("tr")).drop(1)

    assertResult(4) {
      tableRowElms.size
    }

    val isbnElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(1).headOption }
    val isbns = isbnElms map { e => e.trimmedText }

    assertResult(Set("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-0-11-222222-3", "ISBN-9-88-777777-6")) {
      isbns.toSet
    }

    val authorsElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(3).headOption }
    val authors = authorsElms map { e => e.trimmedText }

    assertResult(Set(
      "Jeffrey Ullman, Jennifer Widom",
      "Hector Garcia-Molina, Jeffrey Ullman, Jennifer Widom",
      "Jeffrey Ullman, Hector Garcia-Molina",
      "Jennifer Widom")) {
      authors.toSet
    }
  }

  test("testParseBrokenXml") {
    var errorCount = 0
    var fatalErrorCount = 0
    var warningCount = 0

    class MyErrorHandler extends ErrorHandler {

      override def error(exc: SAXParseException): Unit = { errorCount += 1 }

      override def fatalError(exc: SAXParseException): Unit = { fatalErrorCount += 1 }

      override def warning(exc: SAXParseException): Unit = { warningCount += 1 }
    }

    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new LoggingEntityResolver)
      db.setErrorHandler(new MyErrorHandler)
      db
    }

    val domParser = new DocumentParserUsingDom(dbf, createDocumentBuilder _, DomConversions)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()

    val is = new InputSource(new jio.StringReader(brokenXmlString))

    intercept[SAXParseException] {
      domParser.parse(is).documentElement
    }
    assertResult(1) {
      fatalErrorCount
    }
    assertResult(0) {
      errorCount
    }
    assertResult(0) {
      warningCount
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testParseGroovyXmlExample") {
    val parser = DocumentParserUsingDom.newInstance

    val doc = parser.parse(classOf[DomInteropTest].getResourceAsStream("cars.xml"))

    assertResult("records") {
      doc.documentElement.localName
    }

    val recordsElm = doc.documentElement

    assertResult(3) {
      (recordsElm \ (_.localName == "car")).size
    }

    assertResult(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ (_.localName == "car"))(0)

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

    import NodeBuilder._

    val countryPath = PathBuilder.from(QName("car") -> 0, QName("country") -> 0).build(Scope.Empty)
    val updatedCountryElm = textElem(QName("country"), "New Zealand").build()
    val updatedDoc = doc.updateElemOrSelf(countryPath, updatedCountryElm)

    assertResult("New Zealand") {
      updatedDoc.documentElement.filterChildElems(_.localName == "car")(0).getChildElem(_.localName == "country").trimmedText
    }

    assertResult(List("Royale", "P50", "HSV Maloo")) {
      val carElms = recordsElm \ (_.localName == "car")
      val resultElms = carElms sortBy { e => e.attributeOption(EName("year")).getOrElse("0").toInt }
      resultElms map { e => e.attribute(EName("name")) }
    }
  }

  test("testParseFileWithUtf8Bom") {
    // 1. Parse XML file into Elem

    val domParser = DocumentParserUsingDom.newInstance

    val is = classOf[DomInteropTest].getResourceAsStream("books.xml")
    val ba = Iterator.continually(is.read()).takeWhile(b => b != -1).map(_.toByte).toArray
    val baWithBom = addUtf8Bom(ba)
    assert(baWithBom.size == ba.size + 3)
    assert(baWithBom.toSeq.drop(3) == ba.toSeq)

    val root: Elem = domParser.parse(new jio.ByteArrayInputStream(baWithBom)).documentElement

    assertResult(4) {
      (root \\! (_.localName == "Book")).size
    }
    assertResult(4) {
      (root \\! (_.localName == "Magazine")).size
    }
  }

  private def addUtf8Bom(ba: Array[Byte]): Array[Byte] = Array[Byte](0xEF.toByte, 0xBB.toByte, 0xBF.toByte) ++ ba

  class LoggingEntityResolver extends EntityResolver {
    override def resolveEntity(publicId: String, systemId: String): InputSource = {
      logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")
      // Default behaviour
      null
    }
  }
}
