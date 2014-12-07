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

package eu.cdevreeze.yaidom.queryapitests.dom

import java.io.File
import java.io.FileInputStream
import java.net.URI

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XML Base test case for DOM wrapper Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseTest extends Suite {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  @Test def testXmlBase(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI

    val domDoc = db.parse(new FileInputStream(new File(docUri)))
    domDoc.setDocumentURI(docUri.toString)
    val doc = DomDocument(domDoc)

    testXmlBase(doc.documentElement)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBase2(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI

    val domDoc = db.parse(new FileInputStream(new File(docUri)))
    domDoc.setDocumentURI(docUri.toString)
    val doc = DomDocument(domDoc)

    val elem = doc.documentElement

    testXmlBase(elem)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBase3(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI

    val domDoc = db.parse(new FileInputStream(new File(docUri)))
    domDoc.setDocumentURI("http://bogusBaseUri")
    val doc = DomDocument(domDoc)

    val elem = doc.documentElement

    testXmlBase(elem)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBase4(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI

    val domDoc = db.parse(new FileInputStream(new File(docUri)))
    domDoc.setDocumentURI(docUri.toString)
    val doc = DomDocument(domDoc)

    val elem = doc.documentElement.findElem(_.resolvedName == EName("olist")).get

    testXmlBaseOfNonRootElem(elem)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testOtherXmlBase(): Unit = {
    val elem = testElem

    assertResult(new URI("http://example.org/wine/")) {
      new URI(elem.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://example.org/wine/rose")) {
      val e = elem.getChildElem(_.localName == "e2")
      new URI(e.wrappedNode.getBaseURI)
    }

    elem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    elem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  private def testXmlBase(elem: DomElem): Unit = {
    assertResult(2) {
      elem.filterElemsOrSelf(e => e.attributeOption(XmlBaseEName).isDefined).size
    }
    assertResult(new URI("http://example.org/today/")) {
      new URI(elem.wrappedNode.getBaseURI)
    }
    assertResult(Set(new URI("http://example.org/hotpicks/"))) {
      elem.filterElems(EName("olist")).map(e => new URI(e.wrappedNode.getBaseURI)).toSet
    }
    assertResult(Set(
      new URI("http://example.org/today/new.xml"),
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          (new URI(e.wrappedNode.getBaseURI)).resolve(href)
        }
      uris.toSet
    }
  }

  private def testXmlBaseOfNonRootElem(elem: DomElem): Unit = {
    require(elem.resolvedName == EName("olist"))

    assertResult(new URI("http://example.org/hotpicks/")) {
      new URI(elem.wrappedNode.getBaseURI)
    }

    assertResult(Set(
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          (new URI(e.wrappedNode.getBaseURI)).resolve(href)
        }
      uris.toSet
    }
  }

  private def testElem: DomElem = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/miniXmlBaseTestFile.xml").toURI

    val domDoc = db.parse(new FileInputStream(new File(docUri)))
    domDoc.setDocumentURI("")
    val doc = DomDocument(domDoc)
    doc.documentElement
  }

  private def testXmlBaseProperty1(elem: DomElem): Unit = {
    val ancestorsOrSelf = elem.ancestorsOrSelf.reverse

    val expectedBaseUri =
      ancestorsOrSelf.foldLeft(toUri(elem.wrappedNode.getOwnerDocument.getDocumentURI)) {
        case (currBaseUri, e) =>
          e.attributeOption(XmlBaseEName).map(s => resolveUri(currBaseUri, toUri(s))).getOrElse(currBaseUri)
      }

    assertResult(expectedBaseUri) {
      toUri(elem.wrappedNode.getBaseURI)
    }
  }

  private def testXmlBaseProperty2(elem: DomElem): Unit = {
    val parentBaseUri =
      elem.parentOption.map(e => toUri(e.wrappedNode.getBaseURI)).getOrElse(
        toUri(elem.wrappedNode.getOwnerDocument.getDocumentURI))

    val expectedBaseUri =
      elem.attributeOption(XmlBaseEName).map(s => resolveUri(parentBaseUri, toUri(s))).getOrElse(parentBaseUri)

    assertResult(expectedBaseUri) {
      toUri(elem.wrappedNode.getBaseURI)
    }
  }

  private def resolveUri(base: URI, uri: URI): URI = {
    if (uri.toString.isEmpty) base else base.resolve(uri)
  }

  private def toUri(s: String): URI =
    Option(s).map(s => new URI(s)).getOrElse(new URI(""))
}
