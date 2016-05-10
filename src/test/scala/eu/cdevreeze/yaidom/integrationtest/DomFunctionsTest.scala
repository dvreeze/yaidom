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

import java.{ io => jio }
import java.{ util => jutil }

import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.EntityResolver
import org.xml.sax.ErrorHandler
import org.xml.sax.InputSource
import org.xml.sax.SAXParseException

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.HasParentApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * DOM functions test case. It mirrors the DomWrapperTest, but uses type classes instead.
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
class DomFunctionsTest extends Suite {

  import DomFunctionsTest.DomLikeElem
  import DomFunctionsTest.DomLikeFunctionApi

  // Why do we still need to explicitly create this "implicit"?
  implicit val ev = DomLikeFunctionApi.DomFunctions

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("books.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

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

  @Test def testParseStrangeXml(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("strangeXml.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseDefaultNamespaceXml(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("trivialXml.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

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

  @Test def testParseSchemaXsd(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[DomFunctionsTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[DomFunctionsTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val is = classOf[DomFunctionsTest].getResourceAsStream("XMLSchema.xsd")

    val db = createDocumentBuilder(dbf)
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

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
      val result = root \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(0, 1)) {
      val result = root \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
      val forChoiceDefOption: Option[DomLikeElem[org.w3c.dom.Element]] = {
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

    def checkCommentWithEscapedChar(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
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

    def checkIdentityConstraintElm(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
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

    def checkComplexTypeElm(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
      val complexTypeElms =
        rootElm filterElems { e =>
          e.resolvedName == EName(ns, "complexType") &&
            e.attributeOption(EName("name")) == Some("element") &&
            e.attributeOption(EName("abstract")) == Some("true")
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

    def checkFieldPattern(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
      val fieldElms = rootElm filterElems { e =>
        e.resolvedName == EName(ns, "element") &&
          e.attributeOption(EName("name")) == Some("field") &&
          e.attributeOption(EName("id")) == Some("field")
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
  }

  @Test def testParseXmlWithExpandedEntityRef(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
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
  }

  @Test def testParseXmlWithNonExpandedEntityRef(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      assertResult(true) {
        childOption.isDefined
      }
      assertResult(2) {
        childOption.get.textChildren.size
      }
      assertResult(1) {
        val result =
          DomConversions.nodeListToIndexedSeq(childOption.get.wrappedElem.getChildNodes) collect { case er: org.w3c.dom.EntityReference => er }
        result.size
      }
      assertResult("hello") {
        val entityRefs =
          DomConversions.nodeListToIndexedSeq(childOption.get.wrappedElem.getChildNodes) collect { case er: org.w3c.dom.EntityReference => er }
        val entityRef: org.w3c.dom.EntityReference = entityRefs.head
        entityRef.getNodeName
      }
      val s = "This text contains an entity reference, viz."
      assertResult(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)
  }

  @Test def testParseXmlWithNamespaceUndeclarations(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithEscapedChars(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setCoalescing(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
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
  }

  @Test def testParseXmlWithSpecialChars(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: DomLikeElem[org.w3c.dom.Element]): Unit = {
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
  }

  @Test def testParseGeneratedHtml(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("books.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

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

    def bookHtmlString(bookElm: DomLikeElem[_]): String = {
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

    val htmlRoot: DomLikeElem[org.w3c.dom.Element] =
      new DomLikeElem(dbf.newDocumentBuilder.parse(new jio.ByteArrayInputStream(htmlString.getBytes("utf-8"))).getDocumentElement)

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

  @Test def testParseBrokenXml(): Unit = {
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

    val db = createDocumentBuilder(dbf)

    val brokenXmlString = """<?xml version="1.0" encoding="UTF-8"?>%n<a><b><c>broken</b></c></a>""".format()
    val is = new jio.ByteArrayInputStream(brokenXmlString.getBytes("utf-8"))

    intercept[SAXParseException] {
      db.parse(is)
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
  @Test def testParseGroovyXmlExample(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("cars.xml")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    assertResult("records") {
      root.localName
    }

    val recordsElm = root

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
  }

  /**
   * Example of finding elements and their ancestors.
   */
  @Test def testParseSchemaExample(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomFunctionsTest].getResourceAsStream("gaap.xsd")
    val doc = db.parse(is)

    val root: DomLikeElem[org.w3c.dom.Element] = new DomLikeElem(doc.getDocumentElement)

    val elementDecls = root filterElems { e =>
      e.resolvedName == EName(nsXmlSchema, "element")
    }

    val anElementDeclOption = elementDecls find { e => e.attributeOption(EName("name")) == Some("AddressRecord") }

    assertResult(Some("AddressRecord")) {
      anElementDeclOption flatMap { e => (e \@ EName("name")) }
    }

    val tnsOption = anElementDeclOption flatMap { e =>
      val ancestorOption = e findAncestor (ancestorElm => ancestorElm.resolvedName == EName(nsXmlSchema, "schema"))
      ancestorOption flatMap { e => (e \@ EName("targetNamespace")) }
    }

    assertResult(Some("http://xasb.org/gaap")) {
      tnsOption
    }
  }

  // No test method testParseMultipleNodeKinds. The type-class approach is functional, not OO, and
  // in particular does not easily support node type hierarchies.

  class LoggingEntityResolver extends EntityResolver {
    override def resolveEntity(publicId: String, systemId: String): InputSource = {
      logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")
      // Default behaviour
      null
    }
  }
}

object DomFunctionsTest {

  /**
   * DomLikeFunctionApi type class trait.
   */
  trait DomLikeFunctionApi[E] extends ScopedElemApi.FunctionApi[E] with HasParentApi.FunctionApi[E] {

    def textChildren(thisElem: E): immutable.IndexedSeq[Nodes.Text]

    def commentChildren(thisElem: E): immutable.IndexedSeq[Nodes.Comment]
  }

  /**
   * Partial implementation of DomLikeFunctionApi.
   */
  trait DomLikeFunctions[E] extends DomLikeFunctionApi[E] with ScopedElemLike.FunctionApi[E] with HasParent.FunctionApi[E]

  object DomLikeFunctionApi {

    implicit object DomFunctions extends DomLikeFunctions[org.w3c.dom.Element] {

      def findAllChildElems(thisElem: org.w3c.dom.Element): immutable.IndexedSeq[org.w3c.dom.Element] = {
        children(thisElem) collect { case e: org.w3c.dom.Element => e }
      }

      def resolvedName(thisElem: org.w3c.dom.Element): EName = {
        // Not efficient, because of expensive Scope computation

        scope(thisElem).resolveQNameOption(qname(thisElem)).getOrElse(sys.error(s"Element name '${qname(thisElem)}' should resolve to an EName in scope [${scope(thisElem)}]"))
      }

      def resolvedAttributes(thisElem: org.w3c.dom.Element): immutable.Iterable[(EName, String)] = {
        val attrScope = scope(thisElem).withoutDefaultNamespace

        attributes(thisElem) map { kv =>
          val attName = kv._1
          val attValue = kv._2
          val expandedName = attrScope.resolveQNameOption(attName).getOrElse(sys.error(s"Attribute name '${attName}' should resolve to an EName in scope [${attrScope}]"))
          (expandedName -> attValue)
        }
      }

      def text(thisElem: org.w3c.dom.Element): String = {
        val textChildren = children(thisElem) collect { case t: org.w3c.dom.Text => t }
        val textStrings = textChildren map (_.getData)
        textStrings.mkString
      }

      def qname(thisElem: org.w3c.dom.Element): QName = {
        DomConversions.toQName(thisElem)
      }

      def attributes(thisElem: org.w3c.dom.Element): immutable.Iterable[(QName, String)] = {
        DomConversions.extractAttributes(thisElem.getAttributes)
      }

      def scope(thisElem: org.w3c.dom.Element): Scope = {
        val ancestryOrSelf = getAncestorsOrSelf(thisElem)

        val resultScope =
          ancestryOrSelf.foldRight(Scope.Empty) {
            case (wrappedElem, accScope) =>
              val decls = DomConversions.extractNamespaceDeclarations(wrappedElem.getAttributes)
              accScope.resolve(decls)
          }
        resultScope
      }

      def parentOption(thisElem: org.w3c.dom.Element): Option[org.w3c.dom.Element] = {
        val parentNodeOption = Option(thisElem.getParentNode)
        val parentElemOption = parentNodeOption collect { case e: org.w3c.dom.Element => e }
        parentElemOption
      }

      def textChildren(thisElem: org.w3c.dom.Element): immutable.IndexedSeq[Nodes.Text] = {
        children(thisElem) collect { case t: org.w3c.dom.Text => t } map (t => eu.cdevreeze.yaidom.simple.Text(t.getData, false))
      }

      def commentChildren(thisElem: org.w3c.dom.Element): immutable.IndexedSeq[Nodes.Comment] = {
        children(thisElem) collect { case c: org.w3c.dom.Comment => c } map (c => eu.cdevreeze.yaidom.simple.Comment(c.getData))
      }

      private def children(thisElem: org.w3c.dom.Element): immutable.IndexedSeq[org.w3c.dom.Node] = {
        DomConversions.nodeListToIndexedSeq(thisElem.getChildNodes)
      }

      private def getAncestorsOrSelf(thisElem: org.w3c.dom.Element): List[org.w3c.dom.Element] = {
        val parentElement: org.w3c.dom.Element = thisElem.getParentNode match {
          case e: org.w3c.dom.Element => e
          case _                      => null
        }

        if (parentElement eq null) List(thisElem)
        else {
          // Recursive call
          thisElem :: getAncestorsOrSelf(parentElement)
        }
      }
    }
  }

  /**
   * Wrapper DOM-like element using the type class above.
   */
  final class DomLikeElem[U](val wrappedElem: U)(implicit ev: DomLikeFunctionApi[U]) extends ScopedElemLike[DomLikeElem[U]] with HasParent[DomLikeElem[U]] {

    def findAllChildElems: immutable.IndexedSeq[DomLikeElem[U]] = {
      ev.findAllChildElems(wrappedElem).map(e => new DomLikeElem(e))
    }

    def resolvedName: EName = {
      ev.resolvedName(wrappedElem)
    }

    def resolvedAttributes: immutable.Iterable[(EName, String)] = {
      ev.resolvedAttributes(wrappedElem)
    }

    def text: String = {
      ev.text(wrappedElem)
    }

    def qname: QName = {
      ev.qname(wrappedElem)
    }

    def attributes: immutable.Iterable[(QName, String)] = {
      ev.attributes(wrappedElem)
    }

    def scope: Scope = {
      ev.scope(wrappedElem)
    }

    def parentOption: Option[DomLikeElem[U]] = {
      ev.parentOption(wrappedElem).map(e => new DomLikeElem(e))
    }

    def textChildren: immutable.IndexedSeq[Nodes.Text] = {
      ev.textChildren(wrappedElem)
    }

    def commentChildren: immutable.IndexedSeq[Nodes.Comment] = {
      ev.commentChildren(wrappedElem)
    }
  }
}
