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
import eu.cdevreeze.yaidom.Predef._
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.resolved.Predef._

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

  private val nsBookstore = "http://bookstore".ns
  private val nsGoogle = "http://www.google.com".ns
  private val nsFooBar = "urn:foo:bar".ns
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema".ns

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

    val resolvedRoot1 = root1.resolvedElem
    val resolvedRoot2 = root2.resolvedElem

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

    expect(Set("bar".ename, nsGoogle.ename("foo"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = root.resolvedElem

    expect(Set("bar".ename, "http://www.google.com".ns.ename("foo"))) {
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

    expect(Set(nsFooBar.ename("root"), nsFooBar.ename("child"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = root.resolvedElem

    expect(Set(nsFooBar.ename("root"), nsFooBar.ename("child"))) {
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
      Set(ns.ename("schema"), ns.ename("annotation"), ns.ename("documentation"),
        ns.ename("import"), ns.ename("complexType"), ns.ename("complexContent"),
        ns.ename("extension"), ns.ename("sequence"), ns.ename("element"),
        ns.ename("attribute"), ns.ename("choice"), ns.ename("group"),
        ns.ename("simpleType"), ns.ename("restriction"), ns.ename("enumeration"),
        ns.ename("list"), ns.ename("union"), ns.ename("key"),
        ns.ename("selector"), ns.ename("field"), ns.ename("attributeGroup"),
        ns.ename("anyAttribute"), ns.ename("whiteSpace"), ns.ename("fractionDigits"),
        ns.ename("pattern"), ns.ename("any"), ns.ename("appinfo"),
        ns.ename("minLength"), ns.ename("maxInclusive"), ns.ename("minInclusive"),
        ns.ename("notation"))

    expect(xsElmENames) {
      val result = root filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema.toString) } map { e => e.resolvedName }
      result.toSet
    }

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = root.resolvedElem

    expect(xsElmENames) {
      val result = resolvedRoot filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsXmlSchema.toString) } map { e => e.resolvedName }
      result.toSet
    }
  }

  @Test def testEqualityForXmlWithEntityRefs() {
    // 1. Parse XML file into Document, twice

    val parser1 = parse.DocumentParserUsingDom.newInstance
    val is1 = classOf[EqualityTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc1: Document = parser1.parse(is1)
    val root1 = doc1.documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root1.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val parser2 = parse.DocumentParserUsingDom.newInstance(dbf)
    val is2 = classOf[EqualityTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc2: Document = parser2.parse(is2)
    val root2 = doc2.documentElement

    expect(Set(ns.ename("root"), ns.ename("child"))) {
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

    val resolvedRoot1 = root1.resolvedElem
    val resolvedRoot2 = root2.resolvedElem

    expect(false) {
      resolvedRoot1 == resolvedRoot2
    }

    val scope = Scope.fromMap(Map("foobar" -> "urn:foo:bar"))
    val path = ElemPath.fromCanonicalXPath("/*/foobar:child[1]")(scope)
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

    val resolvedRoot3 = root3.resolvedElem

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

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Check descendant elements of `resolved` tree

    val resolvedRoot = root.resolvedElem

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
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

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root1.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    import NodeBuilder._

    val scope = Scope.fromMap(Map("" -> "urn:foo:bar"))
    val root2 =
      elem(
        qname = "root".qname,
        children = List(
          elem(
            qname = "child".qname,
            attributes = Map("about".qname -> "Jansen & co"),
            children = List(text("Jansen & co"))),
          elem(
            qname = "child".qname,
            attributes = Map("about".qname -> "Jansen & co"),
            children = List(text("Jansen & co"))))).build(scope)

    // Check equalities

    expect(false) {
      root1.resolvedElem == root2.resolvedElem
    }

    expect(root1.resolvedElem.removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      root2.resolvedElem.removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }
  }

  @Test def testEqualityForXmlWithEuro() {
    // 1. Parse XML file into Document, and create an equivalent document

    val parser1 = parse.DocumentParserUsingDom.newInstance
    val is1 = classOf[EqualityTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val doc1: Document = parser1.parse(is1)
    val root1 = doc1.documentElement

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root1.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    import NodeBuilder._

    val scope = Scope.fromMap(Map("" -> "urn:foo:bar"))
    val txt = "\u20AC 200"
    val root2 =
      elem(
        qname = "root".qname,
        children = List(
          elem(
            qname = "child".qname,
            children = List(text(txt))),
          elem(
            qname = "child".qname,
            children = List(text(txt))))).build(scope)

    // Check equalities

    expect(false) {
      root1.resolvedElem == root2.resolvedElem
    }

    expect(root1.resolvedElem.removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      root2.resolvedElem.removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }

    expect(root1.resolvedElem.removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      root2.resolvedElem.removeAllInterElementWhitespace.coalesceAllAdjacentText.normalizeAllText
    }
  }
}
