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
import eu.cdevreeze.yaidom.resolved

/**
 * Node equality test case.
 *
 * To debug the DOM parsers, use JVM option -Djaxp.debug=1.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class EqualityTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testBookstoreEquality() {
    // 1. Parse XML file into Document

    val parser = parse.DocumentParserUsingDom.newInstance
    val is1 = classOf[EqualityTest].getResourceAsStream("books.xml")

    val doc1: Document = parser.parse(is1)
    val root1 = doc1.documentElement

    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root1.findAllElemsOrSelf map (e => e.localName)).toSet
    }

    // 2. Now remove all element content whitespace

    val root2 = root1.removeAllInterElementWhitespace

    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      (root2.findAllElemsOrSelf map (e => e.localName)).toSet
    }

    // 3. Check these yaidom trees are not "equal"

    assert(root1.children.size > root2.children.size)
    assert(root1.textChildren.size > root2.textChildren.size)
    expect(root1.allChildElems.size) {
      root2.allChildElems.size
    }

    // 4. Check that the `resolved` trees (after removing element content whitespace) are equal

    val resolvedRoot1 = resolved.Elem(root1)
    val resolvedRoot2 = resolved.Elem(root2)

    assert(resolvedRoot1.children.size > resolvedRoot2.children.size)
    assert(resolvedRoot1.textChildren.size > resolvedRoot2.textChildren.size)
    expect(resolvedRoot1.allChildElems.size) {
      resolvedRoot2.allChildElems.size
    }

    val adaptedResolvedRoot1 = resolvedRoot1.removeAllInterElementWhitespace

    expect(adaptedResolvedRoot1) {
      resolvedRoot2
    }

    expect(adaptedResolvedRoot1) {
      resolvedRoot1.removeAllInterElementWhitespace
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  @Test def testStrangeXmlDescendantsOrSelf() {
    // 1. Parse XML file into Document

    val parser = parse.DocumentParserUsingDom.newInstance
    val is = classOf[EqualityTest].getResourceAsStream("strangeXml.xml")

    val doc: Document = parser.parse(is)
    val root = doc.documentElement

    expect(Set(EName("bar"), EName(nsGoogle, "foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = resolved.Elem(root)

    expect(Set(EName("bar"), EName("http://www.google.com", "foo"))) {
      val result = resolvedRoot.findAllElemsOrSelf map { _.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  @Test def testDefaultNamespaceXmlDescendantsOrSelf() {
    // 1. Parse XML file into Document

    val parser = parse.DocumentParserUsingDom.newInstance
    val is = classOf[EqualityTest].getResourceAsStream("trivialXml.xml")

    val doc: Document = parser.parse(is)
    val root = doc.documentElement

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = resolved.Elem(root)

    expect(Set(EName(nsFooBar, "root"), EName(nsFooBar, "child"))) {
      val result = resolvedRoot.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testSchemaXsdDescendantsOrSelf() {
    // 1. Parse XML file into Document

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

    val parser = parse.DocumentParserUsingDom.newInstance(dbf, createDocumentBuilder _)

    val is = classOf[EqualityTest].getResourceAsStream("XMLSchema.xsd")

    val doc: Document = parser.parse(is)
    val root = doc.documentElement

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

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = resolved.Elem(root)

    expect(xsElmENames) {
      val result = resolvedRoot \\ { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testEqualityForXmlWithEntityRefs() {
    // 1. Parse XML file into Document, twice

    val parser1 = parse.DocumentParserUsingDom.newInstance
    val is1 = classOf[EqualityTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc1: Document = parser1.parse(is1)
    val root1 = doc1.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root1.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val parser2 = parse.DocumentParserUsingDom.newInstance(dbf)
    val is2 = classOf[EqualityTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc2: Document = parser2.parse(is2)
    val root2 = doc2.documentElement

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root2.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    val entityRefs = {
      val result = root2.findAllElemsOrSelf flatMap { e => e.children.filter(_.isInstanceOf[EntityRef]) }
      result
    }
    expect(1) {
      entityRefs.size
    }

    // 2. Now check equalities for `resolved` counterparts

    val resolvedRoot1 = resolved.Elem(root1)
    val resolvedRoot2 = resolved.Elem(root2)

    expect(false) {
      resolvedRoot1 == resolvedRoot2
    }

    val scope = Scope.fromMap(Map("foobar" -> "urn:foo:bar"))

    import ElemPathBuilder.comp

    val path = ElemPathBuilder.from(comp(QName("foobar:child"), 0)).build(scope)

    val root3 = root2.updated(path) {
      e =>
        val newChildren: immutable.IndexedSeq[Node] = e.children flatMap { (n: Node) =>
          n match {
            case er: EntityRef if er.entity == "hello" => Some(Text("hi", false))
            // This is very counter-intuitive, but it seems that the entity reference is there as well as (!) the expansion
            case t: Text if t.text.startsWith("hi") => Some(Text(t.text.drop(2), t.isCData))
            case n: Node => Some(n)
          }
        }
        e.withChildren(newChildren)
    }

    val resolvedRoot3 = resolved.Elem(root3)

    val adaptedResolvedRoot1 = resolvedRoot1.removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    val adaptedResolvedRoot3 = resolvedRoot3.removeAllInterElementWhitespace.coalesceAndNormalizeAllText

    assert(adaptedResolvedRoot1.getChildElem(_.localName == "child").text.contains(" hi."))
    assert(adaptedResolvedRoot3.getChildElem(_.localName == "child").text.contains(" hi."))

    expect(adaptedResolvedRoot1) {
      adaptedResolvedRoot3
    }
  }

  @Test def testXmlWithNSUndeclarationsDescendantsOrSelf() {
    // 1. Parse XML file into Document

    val parser = parse.DocumentParserUsingDom.newInstance
    val is = classOf[EqualityTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val doc: Document = parser.parse(is)
    val root = doc.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = resolved.Elem(root)

    expect(Set(EName(ns, "root"), EName(ns, "a"), EName("b"), EName("c"), EName(ns, "d"))) {
      val result = resolvedRoot.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testEqualityForXmlWithEscapedCharacters() {
    // 1. Parse XML file into Document, and create an equivalent document

    val parser1 = parse.DocumentParserUsingDom.newInstance
    val is1 = classOf[EqualityTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val doc1: Document = parser1.parse(is1)
    val root1 = doc1.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root1.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    import NodeBuilder._

    val scope = Scope.fromMap(Map("" -> "urn:foo:bar"))
    val root2 =
      elem(
        qname = QName("root"),
        children = Vector(
          textElem(
            qname = QName("child"),
            attributes = Map(QName("about") -> "Jansen & co"),
            txt = "Jansen & co"),
          textElem(
            qname = QName("child"),
            attributes = Map(QName("about") -> "Jansen & co"),
            txt = "Jansen & co"))).build(scope)

    // Check equalities

    expect(false) {
      resolved.Elem(root1) == resolved.Elem(root2)
    }

    expect(resolved.Elem(root1).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem(root2).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }
  }

  @Test def testEqualityForXmlWithEuro() {
    // 1. Parse XML file into Document, and create an equivalent document

    val parser1 = parse.DocumentParserUsingDom.newInstance
    val is1 = classOf[EqualityTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val doc1: Document = parser1.parse(is1)
    val root1 = doc1.documentElement

    val ns = "urn:foo:bar"

    expect(Set(EName(ns, "root"), EName(ns, "child"))) {
      val result = root1.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    import NodeBuilder._

    val scope = Scope.fromMap(Map("" -> "urn:foo:bar"))
    val txt = "\u20AC 200"
    val root2 =
      elem(
        qname = QName("root"),
        children = Vector(
          textElem(QName("child"), txt),
          textElem(QName("child"), txt))).build(scope)

    // Check equalities

    expect(false) {
      resolved.Elem(root1) == resolved.Elem(root2)
    }

    expect(resolved.Elem(root1).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem(root2).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }

    expect(resolved.Elem(root1).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem(root2).removeAllInterElementWhitespace.coalesceAllAdjacentText.normalizeAllText
    }
  }
}
