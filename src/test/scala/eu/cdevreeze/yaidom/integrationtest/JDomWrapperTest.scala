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
import java.net.URI
import java.{ util => jutil }

import scala.collection.JavaConverters._
import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

import JDomWrapperTest._
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.ElemWithPath
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.resolved.ResolvedNodes
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * JDOM wrapper test case. It shows that we can easily create `ElemLike` wrappers around JDOM Elements.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class JDomWrapperTest extends FunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  test("testParse") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("books.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    assertResult(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      val elms = root.findAllElems
      (elms map (e => e.localName)).toSet
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

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseStrangeXml") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("strangeXml.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseDefaultNamespaceXml") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXml.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

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

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseSchemaXsd") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[JDomWrapperTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[JDomWrapperTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val is = classOf[JDomWrapperTest].getResourceAsStream("XMLSchema.xsd")

    val db = createDocumentBuilder(dbf)
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

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

    def checkForChoiceDocumentation(rootElm: JDomElem): Unit = {
      val forChoiceDefOption: Option[JDomElem] = {
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

    def checkCommentWithEscapedChar(rootElm: JDomElem): Unit = {
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

    def checkIdentityConstraintElm(rootElm: JDomElem): Unit = {
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

    def checkComplexTypeElm(rootElm: JDomElem): Unit = {
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

    def checkFieldPattern(rootElm: JDomElem): Unit = {
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

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseXmlWithExpandedEntityRef") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: JDomElem): Unit = {
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

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseXmlWithNonExpandedEntityRef") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    dbf.setExpandEntityReferences(false)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: JDomElem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      assertResult(true) {
        childOption.isDefined
      }
      assertResult(2) {
        childOption.get.textChildren.size
      }
      assertResult(1) {
        val result = childOption.get.children collect { case er: JDomEntityRef => er }
        result.size
      }
      assertResult("hello") {
        val entityRefs = childOption.get.children collect { case er: JDomEntityRef => er }
        val entityRef: JDomEntityRef = entityRefs.head
        entityRef.wrappedNode.getName
      }
      val s = "This text contains an entity reference, viz."
      assertResult(s) {
        childOption.get.trimmedText.take(s.length)
      }
    }

    checkChildTextAndEntityRef(root)

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseXmlWithNamespaceUndeclarations") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseXmlWithEscapedChars") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    dbf.setCoalescing(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: JDomElem): Unit = {
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

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseXmlWithSpecialChars") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: JDomElem): Unit = {
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

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testParseGeneratedHtml") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("books.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

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

    def bookHtmlString(bookElm: JDomElem): String = {
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

    val domBuilder2 = new org.jdom2.input.DOMBuilder
    val doc2 = domBuilder2.build(
      dbf.newDocumentBuilder.parse(
        new jio.ByteArrayInputStream(htmlString.getBytes("utf-8"))))
    val domDoc2: JDomDocument = JDomNode.wrapDocument(doc2)

    val htmlRoot: JDomElem = domDoc2.documentElement

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

    checkEqualityOfDomAndJDomElems(d)
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testParseGroovyXmlExample") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("cars.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

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

    checkEqualityOfDomAndJDomElems(d)
  }

  /**
   * Example of finding elements and their ancestors.
   */
  test("testParseSchemaExample") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("gaap.xsd")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val elementDecls = domDoc.documentElement filterElems { e =>
      e.resolvedName == EName(nsXmlSchema, "element")
    }

    val anElementDeclOption = elementDecls find { e => e.attributeOption(EName("name")).contains("AddressRecord") }

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

    checkEqualityOfDomAndJDomElems(d)
  }

  test("testPathsAndAncestries") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[JDomWrapperTest].getResourceAsStream("books.xml")
    val domBuilder = new org.jdom2.input.DOMBuilder
    val d = db.parse(is)
    val doc = domBuilder.build(d)
    val domDoc: JDomDocument = JDomNode.wrapDocument(doc)

    val root: JDomElem = domDoc.documentElement

    assertResult(ElemWithPath(root).findAllElemsOrSelf.map(_.path)) {
      root.findAllElemsOrSelf.map(_.path)
    }

    assertResult(root.findAllElemsOrSelf.map(_.path.entries.map(_.elementName)).map(names => root.resolvedName +: names)) {
      root.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)
    }

    assertResult(root.findAllElemsOrSelf.map(e => e.path.entries.scanLeft(root)({ case (accElem, entry) => accElem.getChildElemByPathEntry(entry) }))) {
      root.findAllElemsOrSelf.map(_.reverseAncestryOrSelf)
    }
  }

  private def checkEqualityOfDomAndJDomElems(d: org.w3c.dom.Document): Unit = {
    val rootElem1 = resolved.Elem(DomDocument(d).documentElement)
    val domBuilder = new org.jdom2.input.DOMBuilder
    val rootElem2 = resolved.Elem(JDomNode.wrapDocument(domBuilder.build(d)).documentElement)

    assertResult(rootElem1) {
      rootElem2
    }
  }

  class LoggingEntityResolver extends EntityResolver {
    override def resolveEntity(publicId: String, systemId: String): InputSource = {
      logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")
      // Default behaviour
      null
    }
  }
}

object JDomWrapperTest {

  sealed trait JDomNode extends ResolvedNodes.Node {

    type DomType <: org.jdom2.Content

    def wrappedNode: DomType

    final override def toString: String = wrappedNode.toString
  }

  final class JDomDocument(val wrappedNode: org.jdom2.Document) extends DocumentApi {
    require(wrappedNode ne null)

    type DocElemType = JDomElem

    def uriOption: Option[URI] = Option(wrappedNode.getBaseURI).map(s => URI.create(s))

    def documentElement: JDomElem = JDomNode.wrapElement(wrappedNode.getRootElement)

    def children: immutable.IndexedSeq[JDomNode] = {
      val childList = wrappedNode.getContent.asScala.toIndexedSeq
      childList flatMap { ch => JDomNode.wrapNodeOption(ch) }
    }
  }

  final class JDomElem(
    override val wrappedNode: org.jdom2.Element) extends JDomNode with ResolvedNodes.Elem with BackingElemApi with ScopedElemLike with HasParent {

    require(wrappedNode ne null)

    type ThisElem = JDomElem

    override type DomType = org.jdom2.Element

    def thisElem: ThisElem = this

    final def children: immutable.IndexedSeq[JDomNode] = {
      val childList = wrappedNode.getContent.asScala.toIndexedSeq
      childList flatMap { ch => JDomNode.wrapNodeOption(ch) }
    }

    override def findAllChildElems: immutable.IndexedSeq[JDomElem] = children collect { case e: JDomElem => e }

    override def resolvedName: EName = {
      val jdomNs = wrappedNode.getNamespace
      val nsOption: Option[String] = if (jdomNs == org.jdom2.Namespace.NO_NAMESPACE) None else Some(jdomNs.getURI)
      EName(nsOption, wrappedNode.getName)
    }

    override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
      val jdomAttrs: immutable.IndexedSeq[org.jdom2.Attribute] = wrappedNode.getAttributes.asScala.toIndexedSeq

      val result = jdomAttrs map { attr =>
        val jdomNs = attr.getNamespace
        val nsOption: Option[String] = if (jdomNs == org.jdom2.Namespace.NO_NAMESPACE) None else Some(jdomNs.getURI)
        val ename = EName(nsOption, attr.getName)

        (ename -> attr.getValue)
      }
      result.toIndexedSeq
    }

    override def qname: QName = QName(wrappedNode.getQualifiedName)

    override def attributes: immutable.IndexedSeq[(QName, String)] = {
      val jdomAttrs: immutable.IndexedSeq[org.jdom2.Attribute] = wrappedNode.getAttributes.asScala.toIndexedSeq

      val result = jdomAttrs map { attr =>
        (QName(attr.getQualifiedName) -> attr.getValue)
      }
      result.toIndexedSeq
    }

    override def scope: Scope = {
      val m: Map[String, String] = {
        val result = wrappedNode.getNamespacesInScope.asScala.toIndexedSeq map { (ns: org.jdom2.Namespace) => (ns.getPrefix -> ns.getURI) }
        result.toMap
      }
      Scope.from(m)
    }

    /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
    def attributeScope: Scope = Scope(scope.prefixNamespaceMap - "")

    def declarations: Declarations = {
      val m: Map[String, String] = {
        val result = wrappedNode.getNamespacesIntroduced.asScala.toIndexedSeq map { (ns: org.jdom2.Namespace) => (ns.getPrefix -> ns.getURI) }
        result.toMap
      }
      Declarations.from(m)
    }

    /** Returns the text children */
    def textChildren: immutable.IndexedSeq[JDomText] = children collect { case t: JDomText => t }

    /** Returns the comment children */
    def commentChildren: immutable.IndexedSeq[JDomComment] = children collect { case c: JDomComment => c }

    /**
     * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
     * If there are no text children, the empty string is returned.
     */
    override def text: String = {
      val textStrings = textChildren map { t => t.text }
      textStrings.mkString
    }

    override def parentOption: Option[JDomElem] =
      Option(wrappedNode.getParentElement) map { e => JDomNode.wrapElement(e) }

    override def namespaces: Declarations = {
      declarations
    }

    def baseUriOption: Option[URI] = {
      Option(wrappedNode.getXMLBaseURI)
    }

    def baseUri: URI = {
      baseUriOption.getOrElse(JDomNode.EmptyUri)
    }

    def docUriOption: Option[URI] = {
      Option(wrappedNode.getDocument).flatMap(d => Option(d.getBaseURI)).map(u => URI.create(u))
    }

    def docUri: URI = {
      docUriOption.getOrElse(JDomNode.EmptyUri)
    }

    def parentBaseUriOption: Option[URI] = {
      parentOption.flatMap(_.baseUriOption)
    }

    def path: Path = {
      // Expensive!

      val reverseAncestorOrSelfPairs = reverseAncestryOrSelf.sliding(2).toIndexedSeq.filter(_.size == 2)

      val pathEntries: immutable.IndexedSeq[Path.Entry] = reverseAncestorOrSelfPairs map { pair =>
        val fromElem = pair(0)
        val toElem = pair(1)

        val expandedName = toElem.resolvedName
        val sameNameChildElems = fromElem.filterChildElems(_.resolvedName == expandedName)
        val idxOption = sameNameChildElems.zipWithIndex.find(pair => pair._1 == toElem).map(_._2)
        require(idxOption.isDefined, s"Corrupt data. No Path found for element $wrappedNode")

        Path.Entry(expandedName, idxOption.get)
      }
      Path(pathEntries)
    }

    def reverseAncestry: immutable.IndexedSeq[JDomElem] = {
      reverseAncestryOrSelf.init
    }

    def reverseAncestryENames: immutable.IndexedSeq[EName] = {
      reverseAncestry.map(_.resolvedName)
    }

    def reverseAncestryOrSelf: immutable.IndexedSeq[JDomElem] = {
      underlyingAncestryOrSelf.reverse.map(n => JDomNode.wrapElement(n))
    }

    def reverseAncestryOrSelfENames: immutable.IndexedSeq[EName] = {
      reverseAncestryOrSelf.map(_.resolvedName)
    }

    def rootElem: JDomElem = {
      JDomNode.wrapElement(underlyingAncestryOrSelf.last)
    }

    override def equals(other: Any): Boolean = other match {
      case other: JDomElem => this.wrappedNode == other.wrappedNode
      case _               => false
    }

    override def hashCode: Int = {
      wrappedNode.hashCode
    }

    private def underlyingAncestryOrSelf: immutable.IndexedSeq[org.jdom2.Element] = {
      var currNodes = Vector(wrappedNode)
      while (currNodes.last.getParentElement ne null) {
        val last = currNodes.last
        currNodes = currNodes :+ last.getParentElement
      }
      currNodes
    }
  }

  final class JDomText(override val wrappedNode: org.jdom2.Text) extends JDomNode with ResolvedNodes.Text {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.Text

    def text: String = wrappedNode.getText

    def trimmedText: String = wrappedNode.getTextTrim

    def normalizedText: String = wrappedNode.getTextNormalize
  }

  final class JDomProcessingInstruction(override val wrappedNode: org.jdom2.ProcessingInstruction) extends JDomNode with Nodes.ProcessingInstruction {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.ProcessingInstruction

    def target: String = wrappedNode.getTarget

    def data: String = wrappedNode.getValue
  }

  final class JDomEntityRef(override val wrappedNode: org.jdom2.EntityRef) extends JDomNode with Nodes.EntityRef {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.EntityRef

    def entity: String = wrappedNode.getName
  }

  final class JDomComment(override val wrappedNode: org.jdom2.Comment) extends JDomNode with Nodes.Comment {
    require(wrappedNode ne null)

    override type DomType = org.jdom2.Comment

    def text: String = wrappedNode.getText
  }

  object JDomNode {

    val EmptyUri = URI.create("")

    def wrapNodeOption(node: org.jdom2.Content): Option[JDomNode] = {
      node match {
        case e: org.jdom2.Element                => Some(new JDomElem(e))
        case cdata: org.jdom2.CDATA              => Some(new JDomText(cdata))
        case t: org.jdom2.Text                   => Some(new JDomText(t))
        case pi: org.jdom2.ProcessingInstruction => Some(new JDomProcessingInstruction(pi))
        case er: org.jdom2.EntityRef             => Some(new JDomEntityRef(er))
        case c: org.jdom2.Comment                => Some(new JDomComment(c))
        case _                                   => None
      }
    }

    def wrapDocument(doc: org.jdom2.Document): JDomDocument = new JDomDocument(doc)

    def wrapElement(elm: org.jdom2.Element): JDomElem = new JDomElem(elm)
  }
}
