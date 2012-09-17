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
import eu.cdevreeze.yaidom.dom._

/**
 * DOM wrapper test case.
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
class DomWrapperTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse() {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("books.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

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
  }

  @Test def testParseStrangeXml() {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("strangeXml.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseDefaultNamespaceXml() {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("trivialXml.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(QName("root"), QName("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.commentChildren.map(_.text.trim) }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
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

    val is = classOf[DomWrapperTest].getResourceAsStream("XMLSchema.xsd")

    val db = createDocumentBuilder(dbf)
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

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

    def checkForChoiceDocumentation(rootElm: DomElem): Unit = {
      val forChoiceDefOption: Option[DomElem] = {
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

    def checkCommentWithEscapedChar(rootElm: DomElem): Unit = {
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

    def checkIdentityConstraintElm(rootElm: DomElem): Unit = {
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

    def checkComplexTypeElm(rootElm: DomElem): Unit = {
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

      expect(Set(EName("minOccurs"))) {
        val result = choiceElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("name"), EName("type"))) {
        val result = elementElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("ref"), EName("minOccurs"), EName("maxOccurs"))) {
        val result = groupElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("name"), EName("type"), EName("use"), EName("default"))) {
        val result = attributeElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }

      expect(Set(EName("ref"))) {
        val result = attributeGroupElms flatMap { e => e.resolvedAttributes.keySet }
        result.toSet
      }
    }

    checkComplexTypeElm(root)

    def checkFieldPattern(rootElm: DomElem): Unit = {
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
  }

  @Test def testParseXmlWithExpandedEntityRef() {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: DomElem): Unit = {
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
  }

  @Test def testParseXmlWithNonExpandedEntityRef() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: DomElem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expect(true) {
        childOption.isDefined
      }
      expect(2) {
        childOption.get.textChildren.size
      }
      expect(1) {
        val result = childOption.get.children collect { case er: DomEntityRef => er }
        result.size
      }
      expect("hello") {
        val entityRefs = childOption.get.children collect { case er: DomEntityRef => er }
        val entityRef: DomEntityRef = entityRefs.head
        entityRef.wrappedNode.getNodeName
      }
      val s = "This text contains an entity reference, viz."
      expect(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)
  }

  @Test def testParseXmlWithNamespaceUndeclarations() {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithEscapedChars() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setCoalescing(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: DomElem): Unit = {
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
  }

  @Test def testParseXmlWithSpecialChars() {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: DomElem): Unit = {
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
  }

  @Test def testParseGeneratedHtml() {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("books.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    val root: DomElem = domDoc.documentElement

    require(root.localName == "Bookstore")

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

    def bookHtmlString(bookElm: DomElem): String = {
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

    val htmlRoot: DomElem =
      DomNode.wrapDocument(dbf.newDocumentBuilder.parse(new jio.ByteArrayInputStream(htmlString.getBytes("utf-8")))).documentElement

    val tableRowElms = htmlRoot.filterElems(EName("tr")).drop(1)

    expect(4) {
      tableRowElms.size
    }

    val isbnElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(1).headOption }
    val isbns = isbnElms map { e => e.trimmedText }

    expect(Set("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-0-11-222222-3", "ISBN-9-88-777777-6")) {
      isbns.toSet
    }

    val authorsElms = tableRowElms flatMap { rowElm => rowElm.filterChildElems(EName("td")).drop(3).headOption }
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

    val db = createDocumentBuilder(dbf)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()
    val is = new jio.ByteArrayInputStream(brokenXmlString.getBytes("utf-8"))

    intercept[SAXParseException] {
      db.parse(is)
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
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomWrapperTest].getResourceAsStream("cars.xml")
    val domDoc: DomDocument = DomNode.wrapDocument(db.parse(is))

    expect("records") {
      domDoc.documentElement.localName
    }

    val recordsElm = domDoc.documentElement

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
  }

  class LoggingEntityResolver extends EntityResolver {
    override def resolveEntity(publicId: String, systemId: String): InputSource = {
      logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))
      // Default behaviour
      null
    }
  }
}
