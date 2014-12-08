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

package eu.cdevreeze.yaidom.queryapitests

import java.net.URI

import scala.Vector
import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.IsNavigableApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom

/**
 * Alternative XML Base test case. This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
 *
 * Note the use of empty URIs in some places.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractAlternativeXmlBaseTest extends Suite {

  import yaidom.simple.Node._

  private val XmlNs = "http://www.w3.org/XML/1998/namespace"
  private val XmlBaseEName = EName(XmlNs, "base")

  private val XLinkNs = "http://www.w3.org/1999/xlink"
  private val XLinkHrefEName = EName(XLinkNs, "href")

  type E <: ScopedElemApi[E] with IsNavigableApi[E]

  type E2 <: ScopedElemApi[E2] with IsNavigableApi[E2]

  protected def convertToDocument(elem: yaidom.simple.Elem, docUri: URI): DocumentApi[E]

  protected def getBaseUri(elem: E): URI

  protected def getParentBaseUri(elem: E): URI

  protected def getDocumentUri(elem: E): URI

  protected def getAncestorsOrSelfReversed(elem: E): immutable.IndexedSeq[E2]

  protected def resolveUri(base: URI, uri: URI): URI = {
    if (uri.toString.isEmpty) base else base.resolve(uri)
  }

  protected def toUri(s: String): URI =
    Option(s).map(s => new URI(s)).getOrElse(new URI(""))

  @Test def testXmlBaseAttributeOnElement(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc = convertToDocument(docElem, new URI("http://www.somewhere.com/f2.xml"))

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(doc.documentElement), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testTwoEquivalentHrefs(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem1 =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "http://www.zvon.org/a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc1 = convertToDocument(docElem1, new URI(""))

    val docElem2 =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc2 = convertToDocument(docElem2, new URI(""))

    assertResult(new URI("")) {
      getBaseUri(doc1.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc1.documentElement.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(doc1.documentElement), href)
    }

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc2.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc2.documentElement.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(doc2.documentElement), href)
    }

    doc1.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc1.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))

    doc2.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc2.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testMissingXmlBaseAttribute(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc = convertToDocument(docElem, new URI("http://www.somewhere.com/f1.xml"))

    assertResult(new URI("http://www.somewhere.com/f1.xml")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.somewhere.com/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(doc.documentElement), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBaseAttributeOnParent(): Unit = {
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

    val doc = convertToDocument(docElem, new URI(""))

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(referenceElem), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedXmlBaseAttributes(): Unit = {
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

    val doc = convertToDocument(docElem, new URI(""))

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/zz/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      getBaseUri(pElem)
    }
    assertResult(new URI("http://www.zvon.org/zz/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(referenceElem), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testOtherNestedXmlBaseAttributes(): Unit = {
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

    val doc = convertToDocument(docElem, new URI("http://www.zvon.org/a/b.xml"))

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a/")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      getBaseUri(referenceElem)
    }
    assertResult(new URI("http://www.zvon.org/a/b.xml")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(referenceElem), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedAbsoluteXmlBaseAttributes(): Unit = {
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

    val doc = convertToDocument(docElem, new URI(""))

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      getBaseUri(pElem)
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(referenceElem), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testEmptyDocUriAndXmlBaseAttribute(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc = convertToDocument(docElem, new URI(""))

    assertResult(new URI("")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(doc.documentElement), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testEmptyXmlBaseAttribute(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc = convertToDocument(docElem, new URI("http://www.somewhere.com/f1.xml"))

    assertResult(true) {
      Set(
        new URI("http://www.somewhere.com/"),
        new URI("http://www.somewhere.com/f1.xml")).contains(getBaseUri(doc.documentElement))
    }
    assertResult(new URI("http://www.somewhere.com/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(doc.documentElement), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedSometimesEmptyXmlBaseAttributes(): Unit = {
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

    val doc = convertToDocument(docElem, new URI(""))

    assertResult(new URI("http://www.zvon.org/yy/")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      getBaseUri(pElem)
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      getBaseUri(referenceElem)
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(referenceElem), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testNestedSometimesEmptyXmlBaseAttributesAgain(): Unit = {
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

    val doc = convertToDocument(docElem, new URI(""))

    assertResult(new URI("http://www.zvon.org/yy/f1.xml")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(true) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      Set(
        new URI("http://www.zvon.org/yy/"),
        new URI("http://www.zvon.org/yy/f1.xml")).contains(getBaseUri(pElem))
    }
    assertResult(true) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      Set(
        new URI("http://www.zvon.org/yy/"),
        new URI("http://www.zvon.org/yy/f1.xml")).contains(getBaseUri(referenceElem))
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(getBaseUri(referenceElem), href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  private def testXmlBaseProperty1(elem: E): Unit = {
    val ancestorsOrSelf = getAncestorsOrSelfReversed(elem)

    val expectedBaseUri =
      ancestorsOrSelf.foldLeft(getDocumentUri(elem)) {
        case (currBaseUri, e) =>
          e.attributeOption(XmlBaseEName).map(s => resolveUri(currBaseUri, toUri(s))).getOrElse(currBaseUri)
      }

    assertResult(expectedBaseUri) {
      getBaseUri(elem)
    }
  }

  private def testXmlBaseProperty2(elem: E): Unit = {
    val parentBaseUri = getParentBaseUri(elem)

    val expectedBaseUri =
      elem.attributeOption(XmlBaseEName).map(s => resolveUri(parentBaseUri, toUri(s))).getOrElse(parentBaseUri)

    assertResult(expectedBaseUri) {
      getBaseUri(elem)
    }
  }
}
