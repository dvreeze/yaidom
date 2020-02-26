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

import java.net.URI
import java.{io => jio}
import java.{util => jutil}

import scala.collection.immutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.ScopedNodes
import eu.cdevreeze.yaidom.resolved
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import org.scalatest.funsuite.AnyFunSuite
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

/**
 * XOM wrapper test case. It shows that we can easily create `ElemLike` wrappers around XOM Elements.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
class XomWrapperTest extends AnyFunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  import XomWrapperTest._

  test("testParse") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("books.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

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

    checkEqualityOfDomAndXomElems(d)
  }

  test("testParseStrangeXml") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("strangeXml.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkEqualityOfDomAndXomElems(d)
  }

  test("testParseDefaultNamespaceXml") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("trivialXml.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

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

    checkEqualityOfDomAndXomElems(d)
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
            new InputSource(classOf[XomWrapperTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[XomWrapperTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val is = classOf[XomWrapperTest].getResourceAsStream("XMLSchema.xsd")

    val db = createDocumentBuilder(dbf)
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

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

    def checkForChoiceDocumentation(rootElm: XomElem): Unit = {
      val forChoiceDefOption: Option[XomElem] = {
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

    def checkCommentWithEscapedChar(rootElm: XomElem): Unit = {
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

    def checkIdentityConstraintElm(rootElm: XomElem): Unit = {
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

    def checkComplexTypeElm(rootElm: XomElem): Unit = {
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

    def checkFieldPattern(rootElm: XomElem): Unit = {
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

    checkEqualityOfDomAndXomElems(d)
  }

  test("testParseXmlWithExpandedEntityRef") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: XomElem): Unit = {
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

    checkEqualityOfDomAndXomElems(d)
  }

  test("testParseXmlWithNamespaceUndeclarations") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkEqualityOfDomAndXomElems(d)
  }

  test("testParseXmlWithEscapedChars") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    dbf.setCoalescing(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: XomElem): Unit = {
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

    checkEqualityOfDomAndXomElems(d)
  }

  test("testParseXmlWithSpecialChars") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: XomElem): Unit = {
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

    checkEqualityOfDomAndXomElems(d)
  }

  test("testParseGeneratedHtml") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("books.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

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

    def bookHtmlString(bookElm: XomElem): String = {
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

    val htmlRoot: XomElem =
      XomNode.wrapDocument(
        nu.xom.converters.DOMConverter.convert(
          dbf.newDocumentBuilder.parse(new InputSource(new jio.StringReader(htmlString))))).documentElement

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

    checkEqualityOfDomAndXomElems(d)
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testParseGroovyXmlExample") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("cars.xml")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

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

    checkEqualityOfDomAndXomElems(d)
  }

  /**
   * Example of finding elements and their ancestors.
   */
  test("testParseSchemaExample") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("gaap.xsd")
    val d = db.parse(is)
    val doc = nu.xom.converters.DOMConverter.convert(d)
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

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

    checkEqualityOfDomAndXomElems(d)
  }

  private def checkEqualityOfDomAndXomElems(d: org.w3c.dom.Document): Unit = {
    val rootElem1 = resolved.Elem.from(DomDocument(d).documentElement)
    val rootElem2 = resolved.Elem.from(XomNode.wrapDocument(nu.xom.converters.DOMConverter.convert(d)).documentElement)

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

object XomWrapperTest {

  sealed trait XomNode extends ScopedNodes.Node {

    type DomType <: nu.xom.Node

    def wrappedNode: DomType

    final override def toString: String = wrappedNode.toString
  }

  final class XomDocument(val wrappedNode: nu.xom.Document) extends DocumentApi {
    require(wrappedNode ne null)

    type DocElemType = XomElem

    def uriOption: Option[URI] = Option(wrappedNode.getBaseURI).map(s => URI.create(s))

    def documentElement: XomElem = XomNode.wrapElement(wrappedNode.getRootElement)

    final def children: immutable.IndexedSeq[XomNode] = {
      (0 until wrappedNode.getChildCount).toIndexedSeq flatMap { (idx: Int) => XomNode.wrapNodeOption(wrappedNode.getChild(idx)) }
    }
  }

  final class XomElem(
    override val wrappedNode: nu.xom.Element) extends XomNode with ScopedNodes.Elem with ScopedElemLike with HasParent {

    require(wrappedNode ne null)

    type ThisElem = XomElem

    type ThisNode = XomNode

    override type DomType = nu.xom.Element

    def thisElem: ThisElem = this

    override def findAllChildElems: immutable.IndexedSeq[XomElem] = children collect { case e: XomElem => e }

    override def resolvedName: EName = {
      val ns: String = wrappedNode.getNamespaceURI
      val nsOption: Option[String] = if (ns == "") None else Some(ns)
      EName(nsOption, wrappedNode.getLocalName)
    }

    override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
      val attrs: immutable.IndexedSeq[nu.xom.Attribute] =
        (0 until wrappedNode.getAttributeCount).toIndexedSeq map { (idx: Int) => wrappedNode.getAttribute(idx) }

      val result = attrs map { attr =>
        val ns: String = attr.getNamespaceURI
        val nsOption: Option[String] = if (ns == "") None else Some(ns)
        val ename = EName(nsOption, attr.getLocalName)

        (ename -> attr.getValue)
      }
      result.toIndexedSeq
    }

    override def qname: QName = QName(wrappedNode.getQualifiedName)

    override def attributes: immutable.IndexedSeq[(QName, String)] = {
      val attrs: immutable.IndexedSeq[nu.xom.Attribute] =
        (0 until wrappedNode.getAttributeCount).toIndexedSeq map { (idx: Int) => wrappedNode.getAttribute(idx) }

      val result = attrs map { attr => (QName(attr.getQualifiedName) -> attr.getValue) }
      result.toIndexedSeq
    }

    override def children: immutable.IndexedSeq[XomNode] = {
      (0 until wrappedNode.getChildCount).toIndexedSeq flatMap { (idx: Int) => XomNode.wrapNodeOption(wrappedNode.getChild(idx)) }
    }

    /** Returns the text children */
    def textChildren: immutable.IndexedSeq[XomText] = children collect { case t: XomText => t }

    /** Returns the comment children */
    def commentChildren: immutable.IndexedSeq[XomComment] = children collect { case c: XomComment => c }

    /**
     * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
     * If there are no text children, the empty string is returned.
     */
    override def text: String = {
      val textStrings = textChildren map { t => t.text }
      textStrings.mkString
    }

    override def parentOption: Option[XomElem] = {
      val parentNodeOption = Option(wrappedNode.getParent)
      val parentElemOption = parentNodeOption collect { case e: nu.xom.Element => e }
      parentElemOption map { e => XomNode.wrapElement(e) }
    }

    override def scope: Scope = {
      val ancestryOrSelf = XomNode.getAncestorsOrSelf(this.wrappedNode)

      val resultScope =
        ancestryOrSelf.foldRight(Scope.Empty) {
          case (wrappedElem, accScope) =>
            val declsMap =
              (0 until wrappedElem.getNamespaceDeclarationCount) map { i =>
                val pref = wrappedElem.getNamespacePrefix(i)
                pref -> wrappedElem.getNamespaceURI(pref)
              }
            val decls = Declarations.from(declsMap.toMap)
            accScope.resolve(decls)
        }
      resultScope
    }
  }

  final class XomText(override val wrappedNode: nu.xom.Text) extends XomNode with ScopedNodes.Text {
    require(wrappedNode ne null)

    override type DomType = nu.xom.Text

    def text: String = wrappedNode.getValue

    def trimmedText: String = text.trim

    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final class XomProcessingInstruction(override val wrappedNode: nu.xom.ProcessingInstruction) extends XomNode with ScopedNodes.ProcessingInstruction {
    require(wrappedNode ne null)

    override type DomType = nu.xom.ProcessingInstruction

    def target: String = wrappedNode.getTarget

    def data: String = wrappedNode.getValue
  }

  final class XomComment(override val wrappedNode: nu.xom.Comment) extends XomNode with ScopedNodes.Comment {
    require(wrappedNode ne null)

    override type DomType = nu.xom.Comment

    def text: String = wrappedNode.getValue
  }

  object XomNode {

    def wrapNodeOption(node: nu.xom.Node): Option[XomNode] = {
      node match {
        case e: nu.xom.Element => Some(new XomElem(e))
        case t: nu.xom.Text => Some(new XomText(t))
        case pi: nu.xom.ProcessingInstruction => Some(new XomProcessingInstruction(pi))
        case c: nu.xom.Comment => Some(new XomComment(c))
        case _ => None
      }
    }

    def wrapDocument(doc: nu.xom.Document): XomDocument = new XomDocument(doc)

    def wrapElement(elm: nu.xom.Element): XomElem = new XomElem(elm)

    def getAncestorsOrSelf(elem: nu.xom.Element): List[nu.xom.Element] = {
      val parentElement: nu.xom.Element = elem.getParent match {
        case e: nu.xom.Element => e
        case _ => null
      }

      if (parentElement eq null) List(elem)
      else {
        // Recursive call
        elem :: getAncestorsOrSelf(parentElement)
      }
    }
  }

}
