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
import javax.xml.stream.{ XMLInputFactory, XMLOutputFactory, XMLEventFactory }
import javax.xml.stream.events.XMLEvent
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import ExpandedName._
import StaxConversions._

/**
 * StAX interoperability test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class StaxInteropTest extends Suite {

  private val ns = "http://bookstore"
  private val nsGoogle = "http://www.google.com"
  private val nsYahoo = "http://www.yahoo.com"

  @Test def testParse() {
    // 1. Parse XML file into Elem

    val xmlInputFactory = XMLInputFactory.newFactory
    val is = classOf[StaxInteropTest].getResourceAsStream("books.xml")
    var eventReader = xmlInputFactory.createXMLEventReader(is)

    val root: Elem = convertToElem(eventReader.toSeq)
    eventReader.close()

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

    // 2. Write Elem to an XML string

    val bos = new jio.ByteArrayOutputStream

    val xmlEventFactory = XMLEventFactory.newFactory
    val events = convertElem(root)(xmlEventFactory)

    val xmlOutputFactory = XMLOutputFactory.newFactory
    val xmlEventWriter = xmlOutputFactory.createXMLEventWriter(bos)
    events.foreach(ev => xmlEventWriter.add(ev))

    xmlEventWriter.close()

    val xmlString = new String(bos.toByteArray, "utf-8")

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))
    eventReader = xmlInputFactory.createXMLEventReader(bis)

    val root2: Elem = convertToElem(eventReader.toSeq)
    eventReader.close()

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

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
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  @Test def testParseStrangeXml() {
    // 1. Parse XML file into Elem

    val xmlInputFactory = XMLInputFactory.newFactory
    val is = classOf[StaxInteropTest].getResourceAsStream("strangeXml.xml")
    var eventReader = xmlInputFactory.createXMLEventReader(is)

    val root: Elem = convertToElem(eventReader.toSeq)
    eventReader.close()

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root.elemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    // 2. Write Elem to an XML string

    val bos = new jio.ByteArrayOutputStream

    val xmlEventFactory = XMLEventFactory.newFactory
    val events = convertElem(root)(xmlEventFactory)

    val xmlOutputFactory = XMLOutputFactory.newFactory
    val xmlEventWriter = xmlOutputFactory.createXMLEventWriter(bos)
    events.foreach(ev => xmlEventWriter.add(ev))

    xmlEventWriter.close()

    val xmlString = new String(bos.toByteArray, "utf-8")

    // 3. Parse XML string into Elem

    val bis = new jio.ByteArrayInputStream(xmlString.getBytes("utf-8"))
    eventReader = xmlInputFactory.createXMLEventReader(bis)

    val root2: Elem = convertToElem(eventReader.toSeq)
    eventReader.close()

    // 4. Perform the checks of the parsed XML string as Elem against the originally parsed XML file as Elem

    expect(Set("bar".ename, nsGoogle.ns.ename("foo"))) {
      val result = root2.elemsOrSelf map { e => e.resolvedName }
      result.toSet
    }
  }
}
