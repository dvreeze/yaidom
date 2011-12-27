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
import ExpandedName._
import DomConversions._

/**
 * DOM interoperability test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DomInteropTest extends Suite {

  private val ns = "http://bookstore"

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val is = classOf[DomInteropTest].getResourceAsStream("books.xml")
    val doc = db.parse(is)

    val root: Elem = convertToElem(doc.getDocumentElement)
    is.close()

    expect(Set("Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      root.elems.map(e => e.qname.localPart).toSet
    }
    expect(Set("Bookstore", "Book", "Title", "Authors", "Author", "First_Name", "Last_Name", "Remark", "Magazine")) {
      root.elemsOrSelf.map(e => e.qname.localPart).toSet
    }
    expect(8) {
      root.elemsOrSelf(ns.ns.ename("Title")).size
    }
    expect(3) {
      root.elemsOrSelf(ns.ns.ename("Last_Name"), e => e.firstTextValue == "Ullman").size
    }

    // 2. Convert Elem to a DOM element

    val db2 = dbf.newDocumentBuilder
    val doc2 = db2.newDocument
    val element = convertElem(root)(doc2)

    // 3. Convert DOM element into Elem

    val root2: Elem = convertToElem(doc2.getDocumentElement)

    // 4. Perform the checks of the converted DOM tree as Elem against the originally parsed XML file as Elem

    expect(root.elems.map(e => e.qname.localPart).toSet) {
      root2.elems.map(e => e.qname.localPart).toSet
    }
    expect(root.elemsOrSelf.map(e => e.qname.localPart).toSet) {
      root2.elemsOrSelf.map(e => e.qname.localPart).toSet
    }
    expect(root.elemsOrSelf(ns.ns.ename("Title")).size) {
      root2.elemsOrSelf(ns.ns.ename("Title")).size
    }
    expect(root.elemsOrSelf(ns.ns.ename("Last_Name"), e => e.firstTextValue == "Ullman").size) {
      root2.elemsOrSelf(ns.ns.ename("Last_Name"), e => e.firstTextValue == "Ullman").size
    }
  }
}
