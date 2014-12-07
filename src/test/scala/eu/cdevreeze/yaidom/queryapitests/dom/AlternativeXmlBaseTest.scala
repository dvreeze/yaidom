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

import java.net.URI

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Node.emptyElem
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Alternative XML Base test case for DOM wrapper Elems. This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
 *
 * Note the use of empty URIs in some places.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AlternativeXmlBaseTest extends Suite {

  import simple.Node._

  private val XmlNs = "http://www.w3.org/XML/1998/namespace"
  private val XmlBaseEName = EName(XmlNs, "base")

  private val XLinkNs = "http://www.w3.org/1999/xlink"
  private val XLinkHrefEName = EName(XLinkNs, "href")

  @Test def testXmlBaseAttributeOnElement(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("http://www.somewhere.com/f2.xml").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.zvon.org/")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      (new URI(doc.documentElement.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testTwoEquivalentHrefs(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem1 =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "http://www.zvon.org/a.xml", QName("xlink:type") -> "simple"),
        scope)

    val d1 = db.newDocument()
    DomConversions.convertDocument(Document(docElem1))(d1)
    d1.setDocumentURI(new URI("").toString)
    val doc1 = DomDocument(d1)

    val docElem2 =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val d2 = db.newDocument()
    DomConversions.convertDocument(Document(docElem2))(d2)
    d2.setDocumentURI(new URI("").toString)
    val doc2 = DomDocument(d2)

    assertResult(true) {
      Option(doc1.documentElement.wrappedNode.getBaseURI).getOrElse("").isEmpty
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc1.documentElement.attribute(XLinkHrefEName))
      (new URI(Option(doc1.documentElement.wrappedNode.getBaseURI).getOrElse(""))).resolve(href)
    }

    assertResult(new URI("http://www.zvon.org/")) {
      new URI(doc2.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc2.documentElement.attribute(XLinkHrefEName))
      (new URI(doc2.documentElement.wrappedNode.getBaseURI)).resolve(href)
    }

    doc1.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc1.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))

    doc2.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc2.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testMissingXmlBaseAttribute(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("http://www.somewhere.com/f1.xml").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.somewhere.com/f1.xml")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.somewhere.com/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      (new URI(doc.documentElement.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBaseAttributeOnParent(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)
    val docElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(referenceElem)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.zvon.org/")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      (new URI(referenceElem.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedXmlBaseAttributes(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)
    val pElem =
      emptyElem(
        QName("p"),
        Vector(QName("xml:base") -> "zz/"),
        scope).plusChild(referenceElem)
    val docElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(pElem)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.zvon.org/")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/zz/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      new URI(pElem.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/zz/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      (new URI(referenceElem.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testOtherNestedXmlBaseAttributes(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "a/", QName("xlink:href") -> "b.xml", QName("xlink:type") -> "simple"),
        scope)
    val docElem =
      emptyElem(
        QName("document"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(referenceElem)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("http://www.zvon.org/a/b.xml").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.zvon.org/")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/a/")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      new URI(referenceElem.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/a/b.xml")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      (new URI(referenceElem.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedAbsoluteXmlBaseAttributes(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)
    val pElem =
      emptyElem(
        QName("p"),
        Vector(QName("xml:base") -> "http://www.zvon.org/yy/"),
        scope).plusChild(referenceElem)
    val docElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(pElem)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.zvon.org/")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      new URI(pElem.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      (new URI(referenceElem.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testEmptyDocUriAndXmlBaseAttribute(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("").toString)
    val doc = DomDocument(d)

    assertResult(new URI("")) {
      toUri(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      (new URI("")).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testEmptyXmlBaseAttribute(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("http://www.somewhere.com/f1.xml").toString)
    val doc = DomDocument(d)

    // Not the same as baseUri.resolve(emptyUri)!
    assertResult(new URI("http://www.somewhere.com/f1.xml")) {
      toUri(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.somewhere.com/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      resolveUri(toUri(doc.documentElement.wrappedNode.getBaseURI), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedSometimesEmptyXmlBaseAttributes(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)
    val pElem =
      emptyElem(
        QName("p"),
        Vector(QName("xml:base") -> ""),
        scope).plusChild(referenceElem)
    val docElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/yy/"),
        scope).plusChild(pElem)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.zvon.org/yy/")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      new URI(pElem.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      new URI(referenceElem.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      (new URI(referenceElem.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedSometimesEmptyXmlBaseAttributesAgain(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder

    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)
    val pElem =
      emptyElem(
        QName("p"),
        Vector(QName("xml:base") -> ""),
        scope).plusChild(referenceElem)
    val docElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/yy/f1.xml"),
        scope).plusChild(pElem)

    val d = db.newDocument()
    DomConversions.convertDocument(Document(docElem))(d)
    d.setDocumentURI(new URI("").toString)
    val doc = DomDocument(d)

    assertResult(new URI("http://www.zvon.org/yy/f1.xml")) {
      new URI(doc.documentElement.wrappedNode.getBaseURI)
    }
    // Not the same as baseUri.resolve(emptyUri)!
    assertResult(new URI("http://www.zvon.org/yy/f1.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      new URI(pElem.wrappedNode.getBaseURI)
    }
    // Not the same as baseUri.resolve(emptyUri)!
    assertResult(new URI("http://www.zvon.org/yy/f1.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      new URI(referenceElem.wrappedNode.getBaseURI)
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      (new URI(referenceElem.wrappedNode.getBaseURI)).resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
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
