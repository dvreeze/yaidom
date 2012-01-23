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
package jinterop

import java.{ util => jutil, io => jio }
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import org.w3c.dom.{ Element, Document }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import QName._
import ExpandedName._
import DomConversions._

/**
 * DOM interoperability test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DomInteropTest extends Suite {

  private val nsBookstore = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsYahoo = "http://www.yahoo.com"
  private val nsFooBar = "urn:foo:bar"
  private val nsXmlSchema = "http://www.w3.org/2001/XMLSchema"

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("books.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      root.allElems map { e => e.qname.localPart } toSet
    }
    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      root.allElemsOrSelf map { e => e.qname.localPart } toSet
    }
    expect(8) {
      root.elemsOrSelf(nsBookstore.ns.ename("Title")).size
    }
    expect(3) {
      root elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    }

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(root.allElems map { e => e.qname.localPart } toSet) {
      root2.allElems map { e => e.qname.localPart } toSet
    }
    expect(root.allElemsOrSelf map { e => e.qname.localPart } toSet) {
      root2.allElemsOrSelf map { e => e.qname.localPart } toSet
    }
    expect(root.elemsOrSelf(nsBookstore.ns.ename("Title")).size) {
      root2.elemsOrSelf(nsBookstore.ns.ename("Title")).size
    }
    expect {
      root elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    } {
      root2 elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(root.allElems map { e => e.qname.localPart } toSet) {
      root3.allElems map { e => e.qname.localPart } toSet
    }
    expect(root.allElemsOrSelf map { e => e.qname.localPart } toSet) {
      root3.allElemsOrSelf map { e => e.qname.localPart } toSet
    }
    expect(root.elemsOrSelf(nsBookstore.ns.ename("Title")).size) {
      root3.elemsOrSelf(nsBookstore.ns.ename("Title")).size
    }
    expect {
      root elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    } {
      root3 elemsOrSelfWhere { e => e.resolvedName == nsBookstore.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  @Test def testParseStrangeXml() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("strangeXml.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  @Test def testParseDefaultNamespaceXml() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXml.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root.allElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("Trivial XML") {
      val result = root.allElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root2.allElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("Trivial XML") {
      val result = root2.allElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root3.allElemsOrSelf map { e => e.qname }
      result.toSet
    }
    expect("Trivial XML") {
      val result = root3.allElemsOrSelf flatMap { e => e.children } collect { case c: Comment => c.text.trim }
      result.mkString
    }
  }

  @Test def testParseSchemaXsd() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("XMLSchema.xsd")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    val ns = nsXmlSchema.ns

    val xsElmENames: Set[ExpandedName] =
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
      val result = root elemsOrSelfWhere { e => e.resolvedName.namespaceUri == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root elemsOrSelfWhere { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    def checkForChoiceDocumentation(rootElm: Elem): Unit = {
      val forChoiceDefOption: Option[Elem] =
        rootElm childElemsWhere { e => e.resolvedName == ns.ename("simpleType") && e.attribute("name".ename) == "formChoice" } headOption

      expect(true) {
        forChoiceDefOption.isDefined
      }

      val forChoiceDefDocumentation: String =
        forChoiceDefOption.get.elems(ns.ename("documentation")) flatMap { e => e.firstTextValue } mkString

      expect("A utility type, not for public use") {
        forChoiceDefDocumentation.trim
      }
    }
    checkForChoiceDocumentation(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(xsElmENames) {
      val result = root2 elemsOrSelfWhere { e => e.resolvedName.namespaceUri == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root2 elemsOrSelfWhere { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    checkForChoiceDocumentation(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(xsElmENames) {
      val result = root3 elemsOrSelfWhere { e => e.resolvedName.namespaceUri == Some(nsXmlSchema) } map { e => e.resolvedName }
      result.toSet
    }
    expect(Set(0, 1)) {
      val result = root3 elemsOrSelfWhere { e => e.allChildElems.isEmpty } map { e => e.textChildren.size }
      result.toSet
    }

    checkForChoiceDocumentation(root3)
  }

  @Test def testParseXmlWithExpandedEntityRef() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildText(rootElm: Elem): Unit = {
      val childOption = rootElm.firstElemOption(ns.ename("child"))
      expect(true) {
        childOption.isDefined
      }
      expect(1) {
        childOption.get.textChildren.size
      }
      val text = "This text contains an entity reference, viz. hi"
      expect(text) {
        childOption.get.firstTextValue.trim.take(text.length)
      }
    }

    checkChildText(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildText(root3)
  }

  @Test def testParseXmlWithNonExpandedEntityRef() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithEntityRef.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    def checkChildTextAndEntityRef(rootElm: Elem): Unit = {
      val childOption = rootElm.firstElemOption(ns.ename("child"))
      expect(true) {
        childOption.isDefined
      }
      expect(2) {
        childOption.get.textChildren.size
      }
      expect(1) {
        val result = childOption.get.children collect { case er: EntityRef => er }
        result.size
      }
      expect(EntityRef("hello")) {
        val entityRefs = childOption.get.children collect { case er: EntityRef => er }
        val entityRef: EntityRef = entityRefs.head
        entityRef
      }
      expect("This text contains an entity reference, viz.") {
        childOption.get.firstTextValue.trim
      }
    }

    checkChildTextAndEntityRef(root)

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root2)

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("child"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    checkChildTextAndEntityRef(root3)
  }

  @Test def testParseXmlWithNamespaceUndeclarations() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    val ns = "urn:foo:bar".ns

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root2.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(ns.ename("root"), ns.ename("a"), "b".ename, "c".ename, ns.ename("d"))) {
      val result = root3.allElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }
}
