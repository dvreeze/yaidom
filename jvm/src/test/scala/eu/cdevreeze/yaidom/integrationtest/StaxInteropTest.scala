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

import java.{io => jio}
import java.{util => jutil}

import scala.collection.immutable

import eu.cdevreeze.yaidom.convert.StaxConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.PathBuilder
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingStax
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.Node
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.XMLResolver
import org.scalatest.funsuite.AnyFunSuite
import org.xml.sax.InputSource

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
class StaxInteropTest extends AnyFunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  private val testScope: Scope = Scope.from("test" -> "http://www.test.org/test")

  test("testParse") {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance().withConverterToDocument(StaxConversions)
    val is = classOf[StaxInteropTest].getResourceAsStream("books.xml")

    val root: Elem = staxParser.parse(is).documentElement

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

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance().withDocumentConverter(StaxConversions)

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

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

    // 5. Call method notUndeclaringPrefixes, and check again

    val root3: Elem = root2.notUndeclaringPrefixes(testScope)

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

    val xmlString2 = printer.print(doc)

    val xmlString3 = printer.print(doc.documentElement)
    assert(xmlString2.startsWith("<?xml "))
    assert(!xmlString3.startsWith("<?xml "))
    assert(xmlString2.size >= xmlString3.size + "<?xml ".size)

    val doc2 = staxParser.parse(new InputSource(new jio.StringReader(xmlString2)))

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
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  test("testParseStrangeXml") {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance().withConverterToDocument(StaxConversions)
    val is = classOf[StaxInteropTest].getResourceAsStream("strangeXml.xml")

    val root: Elem = staxParser.parse(is).documentElement

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance().withDocumentConverter(StaxConversions)

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Call method notUndeclaringPrefixes, and check again

    val root3: Elem = root2.notUndeclaringPrefixes(testScope)

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  test("testParseDefaultNamespaceXml") {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance().withConverterToDocument(StaxConversions)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXml.xml")

    val document: Document = staxParser.parse(is)
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

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance().withDocumentConverter(StaxConversions)

    val xmlString = printer.print(document)

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val document2: Document = staxParser.parse(bis)
    val root2: Elem = document2.documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

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

    // 5. Copy the document, and check again

    val document3: Document = Document.document(document2.uriOption.map(_.toString), document2.children)
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

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    // With an IBM JRE, the DtdSuppressionResolver as implemented below does not work
    xmlInputFactory.setXMLResolver(new EntityResolverUsingLocalDtds)

    val staxParser = new DocumentParserUsingStax(xmlInputFactory, StaxConversions)

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

    assertResult(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption.contains(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    // Remember, coalescing is set to true!
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

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance().withDocumentConverter(StaxConversions)

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

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

    // 5. Call method notUndeclaringPrefixes, and check again

    val root3: Elem = root2.notUndeclaringPrefixes(testScope)

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

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory, StaxConversions)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = staxParser.parse(is).documentElement

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
      // Remember, coalescing is set to true!
      assertResult(1) {
        childOption.get.textChildren.size
      }
      val text = "This text contains an entity reference, viz. hi"
      assertResult(text) {
        childOption.get.trimmedText.take(text.length)
      }
    }

    checkChildText(root)

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance()

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root2)

    // 5. Call method notUndeclaringPrefixes, and check again

    val root3: Elem = root2.notUndeclaringPrefixes(testScope)

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root3)
  }

  test("testParseXmlWithNonExpandedEntityRef") {
    // 1. Parse XML file into Elem

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_REPLACING_ENTITY_REFERENCES, java.lang.Boolean.FALSE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory, StaxConversions)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val root: Elem = staxParser.parse(is).documentElement

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
        val result = childOption.get.textChildren filter { t => t.text.trim != "" }
        result.size
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

    // 2. Write Elem to an XML string

    // The entity references are lost in the conversion within the print method!
    val printer = DocumentPrinterUsingStax.newInstance()

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // No check on entity references, because they were lost in the conversion back to StAX events

    // 5. Call method notUndeclaringPrefixes, and check again

    val root3: Elem = root2.notUndeclaringPrefixes(testScope)

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  test("testParseXmlWithNamespaceUndeclarations") {
    // 1. Parse XML file into Elem

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory, StaxConversions)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val root: Elem = staxParser.parse(is).documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance()

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Call method notUndeclaringPrefixes, and check again

    val root3: Elem = root2.notUndeclaringPrefixes(testScope)

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  test("testParseXmlWithEscapedChars") {
    // 1. Parse XML file into Elem

    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    val staxParser = new DocumentParserUsingStax(xmlInputFactory, StaxConversions)
    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val root: Elem = staxParser.parse(is).documentElement

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

    // 2. Write Elem to an XML string

    val printer = DocumentPrinterUsingStax.newInstance()

    val xmlString = printer.print(Document(None, root))

    // 3. Parse XML string into Elem

    val bis = new InputSource(new jio.StringReader(xmlString))

    val root2: Elem = staxParser.parse(bis).documentElement

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root2)

    // 5. Call method notUndeclaringPrefixes, and check again

    val root3: Elem = root2.notUndeclaringPrefixes(testScope)

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root3.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    doChecks(root3)
  }

  test("testParseXmlWithSpecialChars") {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance()

    val is = classOf[StaxInteropTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val root: Elem = staxParser.parse(is).documentElement

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

    // 2. Call method notUndeclaringPrefixes, and check again

    val root2: Elem = root.notUndeclaringPrefixes(testScope)

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
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

  test("testParseGeneratedHtml") {
    // 1. Parse XML file into Elem

    val staxParser = DocumentParserUsingStax.newInstance()
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

    val htmlRoot: Elem = staxParser.parse(new InputSource(new jio.StringReader(htmlString))).documentElement

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
    // Using method newInstance instead of newFactory to stay out of "XML JAR-hell".
    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setXMLResolver(new LoggingResolver)

    val staxParser = new DocumentParserUsingStax(xmlInputFactory, StaxConversions)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()

    val is = new InputSource(new jio.StringReader(brokenXmlString))

    intercept[Exception] {
      staxParser.parse(is).documentElement
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testParseGroovyXmlExample") {
    val parser = DocumentParserUsingStax.newInstance()

    val doc = parser.parse(classOf[StaxInteropTest].getResourceAsStream("cars.xml"))

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

    import Node._

    val countryPath = PathBuilder.from(QName("car") -> 0, QName("country") -> 0).build(Scope.Empty)
    val updatedCountryElm = textElem(QName("country"), Scope.Empty, "New Zealand")
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

    val staxParser = DocumentParserUsingStax.newInstance()

    val is = classOf[StaxInteropTest].getResourceAsStream("books.xml")
    val ba = Iterator.continually(is.read()).takeWhile(b => b != -1).map(_.toByte).toArray
    val baWithBom = addUtf8Bom(ba)
    assert(baWithBom.size == ba.size + 3)
    assert(baWithBom.toSeq.drop(3) == ba.toSeq)

    val root: Elem = staxParser.parse(new jio.ByteArrayInputStream(baWithBom)).documentElement

    assertResult(4) {
      (root \\! (_.localName == "Book")).size
    }
    assertResult(4) {
      (root \\! (_.localName == "Magazine")).size
    }
  }

  private def addUtf8Bom(ba: Array[Byte]): Array[Byte] = Array[Byte](0xEF.toByte, 0xBB.toByte, 0xBF.toByte) ++ ba

  class LoggingResolver extends XMLResolver {
    override def resolveEntity(publicId: String, systemId: String, baseUri: String, namespace: String): AnyRef = {
      logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")
      // Default behaviour
      val is: jio.InputStream = null
      is
    }
  }

  class DtdSuppressionResolver extends XMLResolver {
    override def resolveEntity(publicId: String, systemId: String, baseUri: String, namespace: String): AnyRef = {
      logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")
      new java.io.StringReader("")
    }
  }

  class EntityResolverUsingLocalDtds extends XMLResolver {
    override def resolveEntity(publicId: String, systemId: String, baseUri: String, namespace: String): AnyRef = {
      logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

      if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
        classOf[StaxInteropTest].getResourceAsStream("XMLSchema.dtd")
      } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
        classOf[StaxInteropTest].getResourceAsStream("datatypes.dtd")
      } else {
        // Default behaviour
        null
      }
    }
  }

}
