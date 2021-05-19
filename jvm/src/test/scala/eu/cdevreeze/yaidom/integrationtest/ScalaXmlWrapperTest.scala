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

import java.{util => jutil}

import scala.xml.parsing.ConstructingParser

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions._
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml._
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory
import org.scalatest.funsuite.AnyFunSuite
import org.xml.sax.InputSource

/**
 * Scala XML wrapper test case. It shows that we can easily create `ElemLike` wrappers around Scala XML Elems.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
class ScalaXmlWrapperTest extends AnyFunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  private val preserveWS = true

  test("testParse") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("books.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

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

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  test("testParseStrangeXml") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("strangeXml.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    // This test works in spite of of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
    // (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858).
    // See method ScalaXmlToYaidomConversions.extractScope for the reason why. That method works around the bug.

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  test("testParseDefaultNamespaceXml") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXml.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

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

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  test("testParseSchemaXsd") {
    // See http://richard.dallaway.com/2013-02-06

    val resolvingXmlLoader = new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def adapter: scala.xml.parsing.FactoryAdapter = new scala.xml.parsing.NoBindingFactoryAdapter() {
        override def resolveEntity(publicId: String, systemId: String): org.xml.sax.InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[ScalaXmlInteropTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[ScalaXmlInteropTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      }
    }

    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("XMLSchema.xsd")

    val parserFactory: SAXParserFactory = SAXParserFactory.newInstance()
    parserFactory.setFeature("http://apache.org/xml/features/disallow-doctype-decl", false)
    val parser: SAXParser = parserFactory.newSAXParser()

    val root: ScalaXmlElem = ScalaXmlNode.wrapElement(resolvingXmlLoader.loadXML(new InputSource(is), parser))

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

    def checkForChoiceDocumentation(rootElm: ScalaXmlElem): Unit = {
      val forChoiceDefOption: Option[ScalaXmlElem] = {
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

    def checkCommentWithEscapedChar(rootElm: ScalaXmlElem): Unit = {
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

    def checkIdentityConstraintElm(rootElm: ScalaXmlElem): Unit = {
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

    def checkComplexTypeElm(rootElm: ScalaXmlElem): Unit = {
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

    def checkFieldPattern(rootElm: ScalaXmlElem): Unit = {
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

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  test("testParseXmlWithExpandedEntityRef") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: ScalaXmlElem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      assertResult(true) {
        childOption.isDefined
      }
      val text = "This text contains an entity reference, viz. hi"
      assertResult(text) {
        val txt = childOption.get.trimmedText
        txt.take(text.length)
      }
    }

    checkChildText(root)

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  test("testParseXmlWithNamespaceUndeclarations") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    // This test works in spite of of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
    // (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858).
    // See method ScalaXmlToYaidomConversions.extractScope for the reason why. That method works around the bug.

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  test("testParseXmlWithSpecialChars") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: ScalaXmlElem): Unit = {
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

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testParseGroovyXmlExample") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("cars.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

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

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  /**
   * Example of finding elements and their ancestors.
   */
  test("testParseSchemaExample") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("gaap.xsd")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    val elementDecls = domDoc.documentElement filterElems { e =>
      e.resolvedName == EName(nsXmlSchema, "element")
    }

    val anElementDeclOption = elementDecls find { e => e.attributeOption(EName("name")).contains("AddressRecord") }

    assertResult(Some("AddressRecord")) {
      anElementDeclOption flatMap { e => (e \@ EName("name")) }
    }

    val tnsOption = domDoc.documentElement \@ EName("targetNamespace")

    assertResult(Some("http://xasb.org/gaap")) {
      tnsOption
    }

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedNode)

    val root2 = new ScalaXmlElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedNode)

    assertResult(resolved.Elem.from(newRootElem1)) {
      resolved.Elem.from(newRootElem2)
    }
  }

  /**
   * Example of parsing a document with multiple kinds of nodes.
   */
  test("testParseMultipleNodeKinds") {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXmlWithDifferentKindsOfNodes.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document()

    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    assertResult(2) {
      domDoc.comments.size
    }
    assertResult(1) {
      domDoc.processingInstructions.size
    }

    assertResult("Some comment") {
      domDoc.comments(1).text.trim
    }
    assertResult("pi") {
      domDoc.processingInstructions.head.wrappedNode.target
    }

    assertResult(true) {
      domDoc.documentElement.findAllElemsOrSelf.flatMap(_.children).exists({
        case t: ScalaXmlCData if t.text == "Piet & co" => true
        case _ => false
      })
    }

    assertResult(true) {
      !domDoc.documentElement.findAllElemsOrSelf.flatMap(_.children).exists({
        case t: ScalaXmlAtom => true
        case _ => false
      })
    }

    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.children collect { case c: ScalaXmlComment if c.text.trim == "Another comment" => c }).size
    }
    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.children collect { case pi: ScalaXmlProcessingInstruction if pi.wrappedNode.target == "some_pi" => pi }).size
    }
    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.children collect { case t: ScalaXmlText if t.text.trim.contains("Some Text") => t }).size
    }
    assertResult(1) {
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.textChildren.filter(_.text.trim.contains("Some Text"))).size
    }

    val hasEntityRef =
      domDoc.documentElement.findAllElemsOrSelf.
        flatMap(_.children collect { case er: ScalaXmlEntityRef if er.wrappedNode.entityName == "hello" => er }).size >= 1
    val hasExpandedEntity =
      domDoc.documentElement.findAllElemsOrSelf.
        filter(e => e.text.contains("This text contains an entity reference, viz. hi there.")).size >= 1

    assertResult(true) {
      hasEntityRef || hasExpandedEntity
    }
  }

  private def testEquality(wrapperElem1: ScalaXmlElem, wrapperElem2: ScalaXmlElem): Unit = {
    assertResult(true) {
      ScalaXmlWrapperTest.areEqual(wrapperElem1, wrapperElem2)
    }
  }
}

object ScalaXmlWrapperTest {

  case class NameAttributesAndText(elementName: EName, attributes: Map[EName, String], text: String)

  /**
   * Tests 2 ScalaXmlElem instances for equality. The equality is based on finding the same descendant-or-self elements
   * in the same order, where a pair of elements is considered equal if they have the same element EName, the same
   * attribute map (from ENames to values), and the same text. Prefixes and scopes are not compared for equality.
   */
  private def areEqual(wrapperElem1: ScalaXmlElem, wrapperElem2: ScalaXmlElem): Boolean = {
    val elems1 = wrapperElem1.findAllElemsOrSelf
    val elems2 = wrapperElem2.findAllElemsOrSelf

    val namesAndAttrs1 = elems1 map (e => NameAttributesAndText(e.resolvedName, e.resolvedAttributes.toMap, e.text))
    val namesAndAttrs2 = elems2 map (e => NameAttributesAndText(e.resolvedName, e.resolvedAttributes.toMap, e.text))

    namesAndAttrs1 == namesAndAttrs2
  }
}
