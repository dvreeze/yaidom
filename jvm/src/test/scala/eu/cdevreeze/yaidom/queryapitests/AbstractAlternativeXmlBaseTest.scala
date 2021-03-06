/*
 * Copyright 2011-2017 Chris de Vreeze
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

import scala.collection.immutable

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import org.scalatest.funsuite.AnyFunSuite

/**
 * Alternative XML Base test case. It tests some expectations about XML Base support for different element implementations.
 * Hence, for DOM and Scala XML wrapper elements, it tests expectations about XML Base support for the underlying libraries.
 *
 * This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
 *
 * Note the use of empty URIs in some places.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractAlternativeXmlBaseTest extends AnyFunSuite {

  import yaidom.simple.Node._

  private val XmlNs = "http://www.w3.org/XML/1998/namespace"
  private val XmlBaseEName = EName(XmlNs, "base")

  private val XLinkNs = "http://www.w3.org/1999/xlink"
  private val XLinkHrefEName = EName(XLinkNs, "href")

  type E <: ScopedElemApi.Aux[E]

  type D <: DocumentApi.Aux[D, E]

  protected def convertToDocument(elem: yaidom.simple.Elem, docUri: URI): DocumentApi.Aux[D, E]

  protected def getBaseUri(elem: E): URI

  protected def getParentBaseUri(elem: E): URI

  protected def getDocumentUri(elem: E): URI

  protected def getReverseAncestryOrSelf(elem: E): immutable.IndexedSeq[E]

  // Naive resolveUri method
  protected def resolveUri(uri: URI, baseUriOption: Option[URI]): URI = {
    val baseUri = baseUriOption.getOrElse(new URI(""))

    if (uri.toString.isEmpty) baseUri else baseUri.resolve(uri)
  }

  protected def nullUri: URI

  test("testXmlBaseAttributeOnElement") {
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
      resolveUri(href, Some(getBaseUri(doc.documentElement)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testTwoEquivalentHrefs") {
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem1 =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "http://www.zvon.org/a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc1 = convertToDocument(docElem1, nullUri)

    val docElem2 =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc2 = convertToDocument(docElem2, nullUri)

    assertResult(new URI("")) {
      getBaseUri(doc1.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc1.documentElement.attribute(XLinkHrefEName))
      resolveUri(href, Some(getBaseUri(doc1.documentElement)))
    }

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc2.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc2.documentElement.attribute(XLinkHrefEName))
      resolveUri(href, Some(getBaseUri(doc2.documentElement)))
    }

    doc1.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc1.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))

    doc2.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc2.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testMissingXmlBaseAttribute") {
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
      resolveUri(href, Some(getBaseUri(doc.documentElement)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testXmlBaseAttributeOnParent") {
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

    val doc = convertToDocument(docElem, nullUri)

    assertResult(new URI("http://www.zvon.org/")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(getBaseUri(referenceElem)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testNestedXmlBaseAttributes") {
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

    val doc = convertToDocument(docElem, nullUri)

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
      resolveUri(href, Some(getBaseUri(referenceElem)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testOtherNestedXmlBaseAttributes") {
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
      resolveUri(href, Some(getBaseUri(referenceElem)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testNestedAbsoluteXmlBaseAttributes") {
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

    val doc = convertToDocument(docElem, nullUri)

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
      resolveUri(href, Some(getBaseUri(referenceElem)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testEmptyDocUriAndXmlBaseAttribute") {
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc = convertToDocument(docElem, nullUri)

    assertResult(new URI("")) {
      getBaseUri(doc.documentElement)
    }
    assertResult(new URI("a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      resolveUri(href, Some(getBaseUri(doc.documentElement)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testEmptyXmlBaseAttribute") {
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
      resolveUri(href, Some(getBaseUri(doc.documentElement)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testNestedSometimesEmptyXmlBaseAttributes") {
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

    val doc = convertToDocument(docElem, nullUri)

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
      resolveUri(href, Some(getBaseUri(referenceElem)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  test("testNestedSometimesEmptyXmlBaseAttributesAgain") {
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

    val doc = convertToDocument(docElem, nullUri)

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
      resolveUri(href, Some(getBaseUri(referenceElem)))
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  /**
   * Tests an XML Base property relating it to the document URI and the ancestry-or-self.
   */
  private def testXmlBaseProperty1(elem: E): Unit = {
    val ancestorsOrSelf = getReverseAncestryOrSelf(elem)

    val expectedBaseUri =
      ancestorsOrSelf.foldLeft(getDocumentUri(elem)) {
        case (currBaseUri, e) =>
          e.attributeOption(XmlBaseEName).map(s => resolveUri(new URI(s), Some(currBaseUri))).getOrElse(currBaseUri)
      }

    assertResult(expectedBaseUri) {
      getBaseUri(elem)
    }
  }

  /**
   * Tests an XML Base property relating it to the parent base URI and the element itself.
   */
  private def testXmlBaseProperty2(elem: E): Unit = {
    val parentBaseUri = getParentBaseUri(elem)

    val expectedBaseUri =
      elem.attributeOption(XmlBaseEName).map(s => resolveUri(new URI(s), Some(parentBaseUri))).getOrElse(parentBaseUri)

    assertResult(expectedBaseUri) {
      getBaseUri(elem)
    }
  }
}
