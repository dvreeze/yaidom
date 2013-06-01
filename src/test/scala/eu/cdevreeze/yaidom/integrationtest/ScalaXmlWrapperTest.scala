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
import org.xml.sax.{ EntityResolver, InputSource, ErrorHandler, SAXParseException }
import org.w3c.dom
import javax.xml.transform.stream.StreamSource
import javax.xml.parsers.{ SAXParserFactory, SAXParser }
import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.xml.parsing.ConstructingParser
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions._
import ScalaXmlWrapperTest._

/**
 * Scala XML wrapper test case. It shows that we can easily create `ElemLike` wrappers around Scala XML Elems.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScalaXmlWrapperTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  private val preserveWS = true

  @Test def testParse() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("books.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    expectResult(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      val elms = root.findAllElems
      (elms map (e => e.localName)).toSet
    }
    expectResult(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root.findAllElemsOrSelf map (e => e.localName)).toSet
    }
    expectResult(8) {
      root.filterElemsOrSelf(EName(nsBookstore, "Title")).size
    }
    expectResult(3) {
      val result = root \\ { e => e.resolvedName == EName(nsBookstore, "Last_Name") && e.trimmedText == "Ullman" }
      result.size
    }
  }

  /**
   * Currently this test is ignored because of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
   * (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858).
   */
  @Ignore @Test def testParseStrangeXml() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("strangeXml.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    expectResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseDefaultNamespaceXml() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXml.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    expectResult(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(QName("root"), QName("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expectResult("Trivial XML") {
      val result = root.findAllElemsOrSelf flatMap { e => e.commentChildren.map(_.text.trim) }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
    // See http://richard.dallaway.com/2013-02-06

    val resolvingXmlLoader = new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def adapter: scala.xml.parsing.FactoryAdapter = new scala.xml.parsing.NoBindingFactoryAdapter() {
        override def resolveEntity(publicId: String, systemId: String): org.xml.sax.InputSource = {
          logger.info("Trying to resolve entity. Public ID: %s. System ID: %s".format(publicId, systemId))

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

    val root: ScalaXmlElem = ScalaXmlNode.wrapElement(resolvingXmlLoader.load(is))

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

    expectResult(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expectResult(Set(0, 1)) {
      val result = root \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: ScalaXmlElem): Unit = {
      val forChoiceDefOption: Option[ScalaXmlElem] = {
        val result = rootElm filterChildElems { e => e.resolvedName == EName(ns, "simpleType") && e.attribute(EName("name")) == "formChoice" }
        result.headOption
      }

      expectResult(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.filterElems(EName(ns, "documentation")) flatMap { e => e.trimmedText } mkString ""

      expectResult("A utility type, not for public use") {
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
      expectResult(true) {
        documentationText.containsSlice("""XML Schema language.  The documentation (within <documentation> elements)""")
      }
    }

    checkCommentWithEscapedChar(root)

    def checkIdentityConstraintElm(rootElm: ScalaXmlElem): Unit = {
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

      expectResult(1) {
        identityConstraintElms.size
      }

      val selectorElms = identityConstraintElms.head \ EName(ns, "selector")

      expectResult(1) {
        selectorElms.size
      }

      expectResult(""".//xs:key|.//xs:unique|.//xs:keyref""") {
        selectorElms.head.attributeOption(EName("xpath")).getOrElse("")
      }
    }

    checkIdentityConstraintElm(root)

    def checkComplexTypeElm(rootElm: ScalaXmlElem): Unit = {
      val complexTypeElms =
        rootElm filterElems { e =>
          e.resolvedName == EName(ns, "complexType") &&
            e.attributeOption(EName("name")) == Some("element") &&
            e.attributeOption(EName("abstract")) == Some("true")
        }

      expectResult(1) {
        complexTypeElms.size
      }

      val extensionElms = complexTypeElms.head.filterElems(EName(ns, "extension"))
      val sequenceElms = complexTypeElms.head.filterElems(EName(ns, "sequence"))
      val choiceElms = complexTypeElms.head.filterElems(EName(ns, "choice"))
      val elementElms = complexTypeElms.head.filterElems(EName(ns, "element"))
      val groupElms = complexTypeElms.head.filterElems(EName(ns, "group"))
      val attributeElms = complexTypeElms.head.filterElems(EName(ns, "attribute"))
      val attributeGroupElms = complexTypeElms.head.filterElems(EName(ns, "attributeGroup"))

      expectResult(Set(EName("base"))) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }
      expectResult(Set("xs:annotated")) {
        val result = extensionElms flatMap { e => e.resolvedAttributes.toMap.values }
        result.toSet
      }

      expectResult(Set()) {
        val result = sequenceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("minOccurs"))) {
        val result = choiceElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("name"), EName("type"))) {
        val result = elementElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("ref"), EName("minOccurs"), EName("maxOccurs"))) {
        val result = groupElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("name"), EName("type"), EName("use"), EName("default"))) {
        val result = attributeElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }

      expectResult(Set(EName("ref"))) {
        val result = attributeGroupElms flatMap { e => e.resolvedAttributes.toMap.keySet }
        result.toSet
      }
    }

    checkComplexTypeElm(root)

    def checkFieldPattern(rootElm: ScalaXmlElem): Unit = {
      val fieldElms = rootElm filterElems { e =>
        e.resolvedName == EName(ns, "element") &&
          e.attributeOption(EName("name")) == Some("field") &&
          e.attributeOption(EName("id")) == Some("field")
      }

      val patternElms = fieldElms flatMap { e => e.filterElems(EName(ns, "pattern")) }

      expectResult(1) {
        patternElms.size
      }

      expectResult("""(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*))))(\|(\.//)?((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)/)*((((child::)?((\i\c*:)?(\i\c*|\*)))|\.)|((attribute::|@)((\i\c*:)?(\i\c*|\*)))))*""") {
        patternElms.head.attributeOption(EName("value")).getOrElse("")
      }
    }

    checkFieldPattern(root)
  }

  @Test def testParseXmlWithExpandedEntityRef() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: ScalaXmlElem): Unit = {
      val childOption = rootElm.findElem(EName(ns, "child"))
      expectResult(true) {
        childOption.isDefined
      }
      val text = "This text contains an entity reference, viz. hi"
      expectResult(text) {
        val txt = childOption.get.trimmedText
        txt.take(text.length)
      }
    }

    checkChildText(root)
  }

  /**
   * Currently this test is ignored because of bug: SI 6939: Namespace binding (xmlns) is duplicated if a child redefines a prefix.
   * (see https://issues.scala-lang.org/browse/SI-6939 and https://github.com/scala/scala/pull/1858).
   */
  @Ignore @Test def testParseXmlWithNamespaceUndeclarations() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseXmlWithSpecialChars() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("trivialXmlWithEuro.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    expectResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: ScalaXmlElem): Unit = {
      val childElms = rootElm.findTopmostElems(EName(ns, "child"))
      expectResult(2) {
        childElms.size
      }

      val text = "\u20AC 200"

      expectResult(Set(text)) {
        val result = childElms map { e => e.trimmedText }
        result.toSet
      }
    }

    doChecks(root)
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testParseGroovyXmlExample() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("cars.xml")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val root: ScalaXmlElem = domDoc.documentElement

    expectResult("records") {
      domDoc.documentElement.localName
    }

    val recordsElm = domDoc.documentElement

    expectResult(3) {
      (recordsElm \ (_.localName == "car")).size
    }

    expectResult(10) {
      recordsElm.findAllElemsOrSelf.size
    }

    val firstRecordElm = (recordsElm \ (_.localName == "car"))(0)

    expectResult("car") {
      firstRecordElm.localName
    }

    expectResult("Holden") {
      firstRecordElm.attribute(EName("make"))
    }

    expectResult("Australia") {
      firstRecordElm.getChildElem(_.localName == "country").trimmedText
    }

    expectResult(2) {
      val carElms = recordsElm \ (_.localName == "car")
      val result = carElms filter { e => e.attributeOption(EName("make")).getOrElse("").contains('e') }
      result.size
    }

    expectResult(Set("Holden", "Peel")) {
      val carElms = recordsElm \ (_.localName == "car")
      val pattern = ".*s.*a.*".r.pattern

      val resultElms = carElms filter { e =>
        val s = e.getChildElem(_.localName == "country").trimmedText
        pattern.matcher(s).matches
      }

      (resultElms map (e => e.attribute(EName("make")))).toSet
    }

    expectResult(Set("speed", "size", "price")) {
      val result = recordsElm.findAllElemsOrSelf collect { case e if e.attributeOption(EName("type")).isDefined => e.attribute(EName("type")) }
      result.toSet
    }
  }

  /**
   * Example of finding elements and their ancestors.
   */
  @Test def testParseSchemaExample() {
    val is = classOf[ScalaXmlWrapperTest].getResourceAsStream("gaap.xsd")
    val parser = ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), preserveWS)
    val doc = parser.document
    val domDoc: ScalaXmlDocument = ScalaXmlNode.wrapDocument(doc)

    val elementDecls = domDoc.documentElement filterElems { e =>
      e.resolvedName == EName(nsXmlSchema, "element")
    }

    val anElementDeclOption = elementDecls find { e => e.attributeOption(EName("name")) == Some("AddressRecord") }

    expectResult(Some("AddressRecord")) {
      anElementDeclOption flatMap { e => (e \@ EName("name")) }
    }

    val tnsOption = domDoc.documentElement \@ EName("targetNamespace")

    expectResult(Some("http://xasb.org/gaap")) {
      tnsOption
    }
  }
}

object ScalaXmlWrapperTest {

  sealed trait ScalaXmlNode {

    type DomType <: scala.xml.Node

    def wrappedNode: DomType

    final override def toString: String = wrappedNode.toString
  }

  trait ScalaXmlParentNode extends ScalaXmlNode {

    final def children: immutable.IndexedSeq[ScalaXmlNode] = {
      wrappedNode.child.toIndexedSeq flatMap { n: scala.xml.Node => ScalaXmlNode.wrapNodeOption(n) }
    }
  }

  final class ScalaXmlDocument(
    val wrappedNode: scala.xml.Document) {

    require(wrappedNode ne null)

    def documentElement: ScalaXmlElem = ScalaXmlNode.wrapElement(wrappedNode.docElem.asInstanceOf[scala.xml.Elem])
  }

  final class ScalaXmlElem(
    override val wrappedNode: scala.xml.Elem) extends ScalaXmlParentNode with ElemLike[ScalaXmlElem] with HasText { self =>

    require(wrappedNode ne null)

    override type DomType = scala.xml.Elem

    override def findAllChildElems: immutable.IndexedSeq[ScalaXmlElem] = children collect { case e: ScalaXmlElem => e }

    override def resolvedName: EName = {
      val scope = extractScope(wrappedNode.scope)
      val qname = toQName(wrappedNode)
      scope.resolveQNameOption(qname).getOrElse(
        sys.error("Could not resolve QName from prefix %s and label %s".format(Option(wrappedNode.prefix).getOrElse(""), wrappedNode.label)))
    }

    override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
      val attrScope = extractScope(wrappedNode.scope).withoutDefaultNamespace
      attributes map {
        case (attrName, attrValue) =>
          val ename = attrScope.resolveQNameOption(attrName).getOrElse(
            sys.error("Could not resolve attribute name %s".format(attrName)))
          (ename, attrValue)
      }
    }

    def qname: QName = toQName(wrappedNode)

    def attributes: immutable.IndexedSeq[(QName, String)] = extractAttributes(wrappedNode.attributes)

    /** Returns the text children */
    def textChildren: immutable.IndexedSeq[ScalaXmlText] = children collect { case t: ScalaXmlText => t }

    /** Returns the comment children */
    def commentChildren: immutable.IndexedSeq[ScalaXmlComment] = children collect { case c: ScalaXmlComment => c }

    /**
     * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
     * If there are no text children, the empty string is returned.
     */
    override def text: String = {
      val textStrings = textChildren map { t => t.text }
      textStrings.mkString
    }
  }

  final class ScalaXmlText(override val wrappedNode: scala.xml.Text) extends ScalaXmlNode {
    require(wrappedNode ne null)

    override type DomType = scala.xml.Text

    def text: String = wrappedNode.text

    def trimmedText: String = text.trim

    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final class ScalaXmlCData(override val wrappedNode: scala.xml.PCData) extends ScalaXmlNode {
    require(wrappedNode ne null)

    override type DomType = scala.xml.PCData

    def text: String = wrappedNode.text

    def trimmedText: String = text.trim

    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final class ScalaXmlProcessingInstruction(override val wrappedNode: scala.xml.ProcInstr) extends ScalaXmlNode {
    require(wrappedNode ne null)

    override type DomType = scala.xml.ProcInstr
  }

  final class ScalaXmlComment(override val wrappedNode: scala.xml.Comment) extends ScalaXmlNode {
    require(wrappedNode ne null)

    override type DomType = scala.xml.Comment

    def text: String = wrappedNode.commentText
  }

  object ScalaXmlNode {

    def wrapNodeOption(node: scala.xml.Node): Option[ScalaXmlNode] = {
      node match {
        case e: scala.xml.Elem => Some(new ScalaXmlElem(e))
        case cdata: scala.xml.PCData => Some(new ScalaXmlCData(cdata))
        case t: scala.xml.Text => Some(new ScalaXmlText(t))
        case pi: scala.xml.ProcInstr => Some(new ScalaXmlProcessingInstruction(pi))
        case c: scala.xml.Comment => Some(new ScalaXmlComment(c))
        case _ => None
      }
    }

    def wrapDocument(doc: scala.xml.Document): ScalaXmlDocument = new ScalaXmlDocument(doc)

    def wrapElement(elm: scala.xml.Elem): ScalaXmlElem = new ScalaXmlElem(elm)
  }
}
