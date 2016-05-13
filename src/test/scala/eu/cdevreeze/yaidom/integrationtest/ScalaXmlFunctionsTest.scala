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

import scala.collection.JavaConverters._
import scala.collection.immutable
import scala.xml.parsing.ConstructingParser

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions._
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml._

/**
 * Scala XML functions test case. It mirrors the ScalaXmlWrapperTest, but uses type classes instead.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScalaXmlFunctionsTest extends Suite {

  import ScalaXmlFunctionsTest.ScalaXmlLikeElem
  import ScalaXmlFunctionsTest.ScalaXmlLikeFunctionApi

  // Why do we still need to explicitly create this "implicit"?
  implicit val ev = ScalaXmlLikeFunctionApi.ScalaXmlFunctions

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  private val preserveWS = true

  @Test def testParse(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("books.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

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

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  @Test def testParseStrangeXml(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("strangeXml.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

    // This test works in spite of of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
    // (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858).
    // See method ScalaXmlToYaidomConversions.extractScope for the reason why. That method works around the bug.

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  @Test def testParseDefaultNamespaceXml(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("trivialXml.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

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

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  @Test def testParseSchemaXsd(): Unit = {
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

    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("XMLSchema.xsd")

    val root: ScalaXmlLikeElem[scala.xml.Elem] =
      new ScalaXmlLikeElem(ScalaXmlNode.wrapElement(resolvingXmlLoader.load(is)).wrappedNode)

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

    def checkForChoiceDocumentation(rootElm: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
      val forChoiceDefOption: Option[ScalaXmlLikeElem[scala.xml.Elem]] = {
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

    def checkCommentWithEscapedChar(rootElm: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
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

    def checkIdentityConstraintElm(rootElm: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
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

    def checkComplexTypeElm(rootElm: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
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

    def checkFieldPattern(rootElm: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
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

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  @Test def testParseXmlWithExpandedEntityRef(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
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

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  @Test def testParseXmlWithNamespaceUndeclarations(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

    val ns = "urn:foo:bar"

    // This test works in spite of of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
    // (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858).
    // See method ScalaXmlToYaidomConversions.extractScope for the reason why. That method works around the bug.

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  @Test def testParseXmlWithSpecialChars(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
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

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testParseGroovyXmlExample(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("cars.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

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

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  /**
   * Example of finding elements and their ancestors.
   */
  @Test def testParseSchemaExample(): Unit = {
    val is = classOf[ScalaXmlFunctionsTest].getResourceAsStream("gaap.xsd")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlDocument.wrapDocument(doc)

    val root: ScalaXmlLikeElem[scala.xml.Elem] = new ScalaXmlLikeElem(domDoc.documentElement.wrappedNode)

    val elementDecls = root filterElems { e =>
      e.resolvedName == EName(nsXmlSchema, "element")
    }

    val anElementDeclOption = elementDecls find { e => e.attributeOption(EName("name")) == Some("AddressRecord") }

    assertResult(Some("AddressRecord")) {
      anElementDeclOption flatMap { e => (e \@ EName("name")) }
    }

    val tnsOption = root \@ EName("targetNamespace")

    assertResult(Some("http://xasb.org/gaap")) {
      tnsOption
    }

    // Convert to yaidom and back, and back to yaidom, and check for equalities

    val newRootElem1 = convertToElem(root.wrappedElem)

    val root2 = new ScalaXmlLikeElem(convertElem(newRootElem1))

    testEquality(root, root2)

    val newRootElem2 = convertToElem(root2.wrappedElem)

    assertResult(resolved.Elem(newRootElem1)) {
      resolved.Elem(newRootElem2)
    }
  }

  // No test method testParseMultipleNodeKinds. The type-class approach is functional, not OO, and
  // in particular does not easily support node type hierarchies.

  private def testEquality(wrapperElem1: ScalaXmlLikeElem[scala.xml.Elem], wrapperElem2: ScalaXmlLikeElem[scala.xml.Elem]): Unit = {
    assertResult(true) {
      ScalaXmlFunctionsTest.areEqual(wrapperElem1, wrapperElem2)
    }
  }
}

object ScalaXmlFunctionsTest {

  case class NameAttributesAndText(elementName: EName, attributes: Map[EName, String], text: String)

  /**
   * Tests 2 ScalaXmlLikeElem instances for equality. The equality is based on finding the same descendant-or-self elements
   * in the same order, where a pair of elements is considered equal if they have the same element EName, the same
   * attribute map (from ENames to values), and the same text. Prefixes and scopes are not compared for equality.
   */
  private def areEqual(wrapperElem1: ScalaXmlLikeElem[scala.xml.Elem], wrapperElem2: ScalaXmlLikeElem[scala.xml.Elem]): Boolean = {
    val elems1 = wrapperElem1.findAllElemsOrSelf
    val elems2 = wrapperElem2.findAllElemsOrSelf

    val namesAndAttrs1 = elems1 map (e => NameAttributesAndText(e.resolvedName, e.resolvedAttributes.toMap, e.text))
    val namesAndAttrs2 = elems2 map (e => NameAttributesAndText(e.resolvedName, e.resolvedAttributes.toMap, e.text))

    namesAndAttrs1 == namesAndAttrs2
  }

  /**
   * ScalaXmlLikeFunctionApi type class trait.
   */
  trait ScalaXmlLikeFunctionApi[E] extends ScopedElemApi.FunctionApi[E] {

    type Node
    type Text <: Node
    type Comment <: Node

    def textChildren(thisElem: E): immutable.IndexedSeq[Text]

    def commentChildren(thisElem: E): immutable.IndexedSeq[Comment]

    def textData(thisText: Text): String

    def commentData(thisComment: Comment): String
  }

  object ScalaXmlLikeFunctionApi {

    implicit object ScalaXmlFunctions extends ScalaXmlLikeFunctionApi[scala.xml.Elem] with ScalaXmlElem.FunctionApi {

      type Node = scala.xml.Node
      type Text = scala.xml.Text
      type Comment = scala.xml.Comment

      def textChildren(thisElem: scala.xml.Elem): immutable.IndexedSeq[Text] = {
        children(thisElem) collect { case t: scala.xml.Text => t }
      }

      def commentChildren(thisElem: scala.xml.Elem): immutable.IndexedSeq[Comment] = {
        children(thisElem) collect { case c: scala.xml.Comment => c }
      }

      def textData(thisText: Text): String = thisText.text

      def commentData(thisComment: Comment): String = thisComment.commentText
    }
  }

  /**
   * Wrapper DOM-like element using the type class above.
   */
  final class ScalaXmlLikeElem[U](val wrappedElem: U)(implicit ev: ScalaXmlLikeFunctionApi[U]) extends ScopedElemLike[ScalaXmlLikeElem[U]] {

    def findAllChildElems: immutable.IndexedSeq[ScalaXmlLikeElem[U]] = {
      ev.findAllChildElems(wrappedElem).map(e => new ScalaXmlLikeElem(e))
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

    def textChildren: immutable.IndexedSeq[Nodes.Text] = {
      ev.textChildren(wrappedElem) map (t => eu.cdevreeze.yaidom.simple.Text(ev.textData(t), false))
    }

    def commentChildren: immutable.IndexedSeq[Nodes.Comment] = {
      ev.commentChildren(wrappedElem) map (c => eu.cdevreeze.yaidom.simple.Comment(ev.commentData(c)))
    }
  }
}
