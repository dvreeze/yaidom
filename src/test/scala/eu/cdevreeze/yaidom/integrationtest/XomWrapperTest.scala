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
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import scala.collection.immutable
import scala.collection.JavaConverters._
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import XomWrapperTest._

/**
 * XOM wrapper test case. It shows that we can easily create `ElemLike` wrappers around XOM Elements.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XomWrapperTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("books.xml")
    val doc = nu.xom.converters.DOMConverter.convert(db.parse(is))
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      val elms = root.findAllElems
      (elms map (e => e.localName)).toSet
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
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("strangeXml.xml")
    val doc = nu.xom.converters.DOMConverter.convert(db.parse(is))
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testParseDefaultNamespaceXml() {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder
    val is = classOf[XomWrapperTest].getResourceAsStream("trivialXml.xml")
    val doc = nu.xom.converters.DOMConverter.convert(db.parse(is))
    val domDoc: XomDocument = XomNode.wrapDocument(doc)

    val root: XomElem = domDoc.documentElement

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
    dbf.setNamespaceAware(true)

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

    val is = classOf[XomWrapperTest].getResourceAsStream("XMLSchema.xsd")

    val db = createDocumentBuilder(dbf)
    val doc = nu.xom.converters.DOMConverter.convert(db.parse(is))
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

    expect(xsElmENames) {
      val result = root \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root \\ { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: XomElem): Unit = {
      val forChoiceDefOption: Option[XomElem] = {
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

    def checkCommentWithEscapedChar(rootElm: XomElem): Unit = {
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

    def checkIdentityConstraintElm(rootElm: XomElem): Unit = {
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

    def checkComplexTypeElm(rootElm: XomElem): Unit = {
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

    def checkFieldPattern(rootElm: XomElem): Unit = {
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
}

object XomWrapperTest {

  sealed trait XomNode {

    type DomType <: nu.xom.Node

    def wrappedNode: DomType

    final override def toString: String = wrappedNode.toString
  }

  trait XomParentNode extends XomNode {

    override type DomType <: nu.xom.ParentNode

    final def children: immutable.IndexedSeq[XomNode] = {
      (0 until wrappedNode.getChildCount).toIndexedSeq flatMap { (idx: Int) => XomNode.wrapOption(wrappedNode.getChild(idx)) }
    }
  }

  final class XomDocument(
    val wrappedNode: nu.xom.Document) extends XomParentNode {

    require(wrappedNode ne null)

    override type DomType = nu.xom.Document

    def documentElement: XomElem = XomNode.wrapElement(wrappedNode.getRootElement)
  }

  final class XomElem(
    override val wrappedNode: nu.xom.Element) extends XomParentNode with ElemLike[XomElem] with HasText { self =>

    require(wrappedNode ne null)

    override type DomType = nu.xom.Element

    override def allChildElems: immutable.IndexedSeq[XomElem] = children collect { case e: XomElem => e }

    override def resolvedName: EName = {
      val ns: String = wrappedNode.getNamespaceURI
      val nsOption: Option[String] = if (ns == "") None else Some(ns)
      EName(nsOption, wrappedNode.getLocalName)
    }

    override def resolvedAttributes: Map[EName, String] = {
      val attrs: immutable.IndexedSeq[nu.xom.Attribute] =
        (0 until wrappedNode.getAttributeCount).toIndexedSeq map { (idx: Int) => wrappedNode.getAttribute(idx) }

      val result = attrs map { attr =>
        val ns: String = attr.getNamespaceURI
        val nsOption: Option[String] = if (ns == "") None else Some(ns)
        val ename = EName(nsOption, attr.getLocalName)

        (ename -> attr.getValue)
      }
      result.toMap
    }

    def qname: QName = QName(wrappedNode.getQualifiedName)

    def attributes: Map[QName, String] = {
      val attrs: immutable.IndexedSeq[nu.xom.Attribute] =
        (0 until wrappedNode.getAttributeCount).toIndexedSeq map { (idx: Int) => wrappedNode.getAttribute(idx) }

      val result = attrs map { attr => (QName(attr.getQualifiedName) -> attr.getValue) }
      result.toMap
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
  }

  final class XomText(override val wrappedNode: nu.xom.Text) extends XomNode {
    require(wrappedNode ne null)

    override type DomType = nu.xom.Text

    def text: String = wrappedNode.getValue

    def trimmedText: String = text.trim

    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final class XomProcessingInstruction(override val wrappedNode: nu.xom.ProcessingInstruction) extends XomNode {
    require(wrappedNode ne null)

    override type DomType = nu.xom.ProcessingInstruction
  }

  final class XomComment(override val wrappedNode: nu.xom.Comment) extends XomNode {
    require(wrappedNode ne null)

    override type DomType = nu.xom.Comment

    def text: String = wrappedNode.getValue
  }

  object XomNode {

    def wrapOption(node: nu.xom.Node): Option[XomNode] = {
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
  }
}
