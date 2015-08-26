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

import java.net.URI
import java.{ util => jutil }

import scala.Vector
import scala.collection.immutable
import scala.collection.mutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.resolved.ResolvedNodes
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.`type`.Type
import net.sf.saxon.dom.NodeOverNodeInfo
import net.sf.saxon.lib.ParseOptions
import net.sf.saxon.om.AxisInfo
import net.sf.saxon.om.DocumentInfo
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.s9api.Processor

/**
 * Saxon DOM wrapper test case. It shows that we can easily create `ElemLike` wrappers around Saxon DOMNodeWrapper Elements.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SaxonDomWrapperTest extends Suite {

  import SaxonDomWrapperTest._

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val processor = new Processor(false)

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("books.xml")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

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
    assertResult(Set(Scope.from("" -> "http://bookstore", "books" -> "http://bookstore"))) {
      root.findAllElemsOrSelf.map(_.scope).toSet
    }

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  @Test def testParseStrangeXml(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("strangeXml.xml")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

    assertResult(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Scope.from("ns" -> "http://www.yahoo.com")) {
      root.scope
    }
    assertResult(Scope.from("ns" -> "http://www.google.com")) {
      root.getChildElem(HasENameApi.withLocalName("foo")).scope
    }

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  @Test def testParseDefaultNamespaceXml(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("trivialXml.xml")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

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

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  @Test def testParseSchemaXsd(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("XMLSchema.xsd")

    val parseOptions = new ParseOptions

    val resolver = new EntityResolver {
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
    }
    parseOptions.setEntityResolver(resolver)

    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

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

    assertResult(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    assertResult(Set(0, 1)) {
      val result = root \\ { e => e.findAllChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: DomElem): Unit = {
      val forChoiceDefOption: Option[DomElem] = {
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

    def checkCommentWithEscapedChar(rootElm: DomElem): Unit = {
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

    def checkComplexTypeElm(rootElm: DomElem): Unit = {
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

    def checkFieldPattern(rootElm: DomElem): Unit = {
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

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  @Test def testParseXmlWithExpandedEntityRef(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: DomElem): Unit = {
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

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  @Test def testParseXmlWithNamespaceUndeclarations(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  @Test def testParseXmlWithEscapedChars(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val parseOptions = new ParseOptions
    // No need to try to influence "coalescing" behavior?
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: DomElem): Unit = {
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

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  @Test def testParseXmlWithSpecialChars(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

    val ns = "urn:foo:bar"

    assertResult(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def doChecks(rootElm: DomElem): Unit = {
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

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  @Test def testParseGroovyXmlExample(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("cars.xml")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val root: DomElem = domDoc.documentElement

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

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  /**
   * Example of finding elements and their ancestors.
   */
  @Test def testParseSchemaExample(): Unit = {
    val is = classOf[SaxonDomWrapperTest].getResourceAsStream("gaap.xsd")

    val parseOptions = new ParseOptions
    val domDoc: DomDocument =
      DomNode.wrapDocument(processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))

    val elementDecls = domDoc.documentElement filterElems { e =>
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

    checkEqualityOfDomAndSaxonElems(domDoc)
  }

  private def checkEqualityOfDomAndSaxonElems(d: DomDocument): Unit = {
    val rootElem1 = resolved.Elem(d.documentElement)
    val rootElem2 =
      resolved.Elem(dom.DomDocument(NodeOverNodeInfo.wrap(d.wrappedNode).asInstanceOf[org.w3c.dom.Document]).documentElement)

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

object SaxonDomWrapperTest {

  import ENameProvider.globalENameProvider._
  import QNameProvider.globalQNameProvider._

  abstract class DomNode(val wrappedNode: NodeInfo) extends ResolvedNodes.Node {

    final override def toString: String = wrappedNode.toString

    protected def nodeInfo2EName(nodeInfo: NodeInfo): EName = {
      val ns: String = nodeInfo.getURI
      val nsOption: Option[String] = if (ns == "") None else Some(ns)
      getEName(nsOption, nodeInfo.getLocalPart)
    }

    protected def nodeInfo2QName(nodeInfo: NodeInfo): QName = {
      val pref: String = nodeInfo.getPrefix
      val prefOption: Option[String] = if (pref == "") None else Some(pref)
      getQName(prefOption, nodeInfo.getLocalPart)
    }
  }

  final class DomDocument(val wrappedNode: DocumentInfo) extends DocumentApi[DomElem] {
    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.DOCUMENT)

    def uriOption: Option[URI] = Option(wrappedNode.getBaseURI).map(s => URI.create(s))

    def documentElement: DomElem =
      (children collectFirst { case e: DomElem => e }).getOrElse(sys.error(s"Missing document element"))

    final def children: immutable.IndexedSeq[DomNode] = {
      if (!wrappedNode.hasChildNodes) Vector()
      else {
        val it = wrappedNode.iterateAxis(AxisInfo.CHILD)

        val nodes = Stream.continually(it.next).takeWhile(_ ne null).toVector

        nodes.flatMap(nodeInfo => DomNode.wrapNodeOption(nodeInfo))
      }
    }
  }

  final class DomElem(
    override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) with ResolvedNodes.Elem with ScopedElemLike[DomElem] with HasParent[DomElem] { self =>

    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.ELEMENT)

    override def findAllChildElems: immutable.IndexedSeq[DomElem] = children collect { case e: DomElem => e }

    override def resolvedName: EName = nodeInfo2EName(wrappedNode)

    override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
      val it = wrappedNode.iterateAxis(AxisInfo.ATTRIBUTE)

      val nodes = Stream.continually(it.next).takeWhile(_ ne null).toVector

      nodes map { nodeInfo => nodeInfo2EName(nodeInfo) -> nodeInfo.getStringValue }
    }

    override def qname: QName = nodeInfo2QName(wrappedNode)

    override def attributes: immutable.IndexedSeq[(QName, String)] = {
      val it = wrappedNode.iterateAxis(AxisInfo.ATTRIBUTE)

      val nodes = Stream.continually(it.next).takeWhile(_ ne null).toVector

      nodes map { nodeInfo => nodeInfo2QName(nodeInfo) -> nodeInfo.getStringValue }
    }

    override def children: immutable.IndexedSeq[DomNode] = {
      if (!wrappedNode.hasChildNodes) Vector()
      else {
        val it = wrappedNode.iterateAxis(AxisInfo.CHILD)

        val nodes = Stream.continually(it.next).takeWhile(_ ne null).toVector

        nodes.flatMap(nodeInfo => DomNode.wrapNodeOption(nodeInfo))
      }
    }

    /** Returns the text children */
    def textChildren: immutable.IndexedSeq[DomText] = children collect { case t: DomText => t }

    /** Returns the comment children */
    def commentChildren: immutable.IndexedSeq[DomComment] = children collect { case c: DomComment => c }

    /**
     * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
     * If there are no text children, the empty string is returned.
     */
    override def text: String = {
      val textStrings = textChildren map { t => t.text }
      textStrings.mkString
    }

    override def parentOption: Option[DomElem] = {
      val parentNodeOption = Option(wrappedNode.getParent)
      val parentElemOption = parentNodeOption collect { case e: NodeInfo if e.getNodeKind == Type.ELEMENT => e }
      parentElemOption map { e => DomNode.wrapElement(e) }
    }

    override def scope: Scope = {
      val it = wrappedNode.iterateAxis(AxisInfo.NAMESPACE)

      val nodes = Stream.continually(it.next).takeWhile(_ ne null).toVector

      val resultMap = {
        val result =
          nodes map { nodeInfo =>
            // Not very transparent: prefix is "display name" and namespace URI is "string value"
            val prefix = nodeInfo.getDisplayName
            val nsUri = nodeInfo.getStringValue
            (prefix -> nsUri)
          }
        result.toMap
      }

      Scope.from(resultMap - "xml")
    }
  }

  final class DomText(override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) with ResolvedNodes.Text {
    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.TEXT || wrappedNode.getNodeKind == Type.WHITESPACE_TEXT)

    def text: String = wrappedNode.getStringValue

    def trimmedText: String = text.trim

    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final class DomProcessingInstruction(override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) with Nodes.ProcessingInstruction {
    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.PROCESSING_INSTRUCTION)

    def target: String = wrappedNode.getDisplayName // ???

    def data: String = wrappedNode.getStringValue // ???
  }

  final class DomComment(override val wrappedNode: NodeInfo) extends DomNode(wrappedNode) with Nodes.Comment {
    require(wrappedNode ne null)
    require(wrappedNode.getNodeKind == Type.COMMENT)

    def text: String = wrappedNode.getStringValue
  }

  object DomNode {

    def wrapNodeOption(node: NodeInfo): Option[DomNode] = {
      node.getNodeKind match {
        case Type.ELEMENT                => Some(new DomElem(node))
        case Type.TEXT                   => Some(new DomText(node))
        case Type.WHITESPACE_TEXT        => Some(new DomText(node))
        case Type.PROCESSING_INSTRUCTION => Some(new DomProcessingInstruction(node))
        case Type.COMMENT                => Some(new DomComment(node))
        case _                           => None
      }
    }

    def wrapDocument(doc: DocumentInfo): DomDocument = new DomDocument(doc)

    def wrapElement(elm: NodeInfo): DomElem = new DomElem(elm)
  }
}
