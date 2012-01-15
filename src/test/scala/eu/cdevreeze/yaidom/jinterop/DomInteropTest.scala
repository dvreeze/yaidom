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

  private val ns = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsYahoo = "http://www.yahoo.com"
  private val nsFooBar = "urn:foo:bar"

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("books.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      root.elems map { e => e.qname.localPart } toSet
    }
    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      root.elemsOrSelf map { e => e.qname.localPart } toSet
    }
    expect(8) {
      root.elemsOrSelf(ns.ns.ename("Title")).size
    }
    expect(3) {
      root elemsOrSelf { e => e.resolvedName == ns.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    }

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(root.elems.map(e => e.qname.localPart).toSet) {
      root2.elems map { e => e.qname.localPart } toSet
    }
    expect(root.elemsOrSelf.map(e => e.qname.localPart).toSet) {
      root2.elemsOrSelf map { e => e.qname.localPart } toSet
    }
    expect(root.elemsOrSelf(ns.ns.ename("Title")).size) {
      root2.elemsOrSelf(ns.ns.ename("Title")).size
    }
    expect {
      root elemsOrSelf { e => e.resolvedName == ns.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    } {
      root2 elemsOrSelf { e => e.resolvedName == ns.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(root.elems.map(e => e.qname.localPart).toSet) {
      root3.elems map { e => e.qname.localPart } toSet
    }
    expect(root.elemsOrSelf.map(e => e.qname.localPart).toSet) {
      root3.elemsOrSelf map { e => e.qname.localPart } toSet
    }
    expect(root.elemsOrSelf(ns.ns.ename("Title")).size) {
      root3.elemsOrSelf(ns.ns.ename("Title")).size
    }
    expect {
      root elemsOrSelf { e => e.resolvedName == ns.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
    } {
      root3 elemsOrSelf { e => e.resolvedName == ns.ns.ename("Last_Name") && e.firstTextValue == "Ullman" } size
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
      val result = root.elemsOrSelf map { e => e.resolvedName }
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
      val result = root2.elemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root3.elemsOrSelf map { e => e.resolvedName }
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
      val result = root.elemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root.elemsOrSelf map { e => e.qname }
      result.toSet
    }

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root2.elemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root2.elemsOrSelf map { e => e.qname }
      result.toSet
    }

    // 5. Convert to NodeBuilder and back, and check again

    val root3: Elem = NodeBuilder.fromElem(root2)(Scope.Empty).build()

    expect(Set(nsFooBar.ns.ename("root"), nsFooBar.ns.ename("child"))) {
      val result = root3.elemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
    expect(Set("root".qname, "child".qname)) {
      val result = root3.elemsOrSelf map { e => e.qname }
      result.toSet
    }
  }
}
