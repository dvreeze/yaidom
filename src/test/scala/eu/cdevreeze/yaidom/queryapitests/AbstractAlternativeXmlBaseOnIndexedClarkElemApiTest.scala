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

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed.IndexedClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.simple.Node.emptyElem

/**
 * Alternative XML Base test case.
 *
 * This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
 *
 * Note the use of empty URIs in some places.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractAlternativeXmlBaseOnIndexedClarkElemApiTest extends Suite {

  import yaidom.simple.Node._

  private val XmlNs = "http://www.w3.org/XML/1998/namespace"
  private val XmlBaseEName = EName(XmlNs, "base")

  private val XLinkNs = "http://www.w3.org/1999/xlink"
  private val XLinkHrefEName = EName(XLinkNs, "href")

  type U <: ClarkElemApi.Aux[U]
  type E <: IndexedClarkElemApi.Aux[E, U]

  // Naive resolveUri method
  protected def resolveUri(uri: URI, baseUriOption: Option[URI]): URI = {
    XmlBaseSupport.JdkUriResolver(uri, baseUriOption)
  }

  protected def convertToDocElem(elem: yaidom.simple.Elem, docUri: URI): E

  protected def nullUri: URI

  @Test def testXmlBaseAttributeOnElement(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val simpleDocElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val docElem = convertToDocElem(simpleDocElem, new URI("http://www.somewhere.com/f2.xml"))

    assertResult(new URI("http://www.zvon.org/")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(docElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(docElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  @Test def testTwoEquivalentHrefs(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val simpleDocElem1 =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "http://www.zvon.org/a.xml", QName("xlink:type") -> "simple"),
        scope)

    val docElem1 = convertToDocElem(simpleDocElem1, nullUri)

    val simpleDocElem2 =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val docElem2 = convertToDocElem(simpleDocElem2, nullUri)

    assertResult(new URI("")) {
      docElem1.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(docElem1.attribute(XLinkHrefEName))
      resolveUri(href, Some(docElem1.baseUriOption.getOrElse(URI.create(""))))
    }

    assertResult(new URI("http://www.zvon.org/")) {
      docElem2.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(docElem2.attribute(XLinkHrefEName))
      resolveUri(href, Some(docElem2.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem1.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem1.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem1))

    docElem2.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem2.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem2))
  }

  @Test def testMissingXmlBaseAttribute(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val simpleDocElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val docElem = convertToDocElem(simpleDocElem, new URI("http://www.somewhere.com/f1.xml"))

    assertResult(new URI("http://www.somewhere.com/f1.xml")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.somewhere.com/a.xml")) {
      val href = new URI(docElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(docElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  @Test def testXmlBaseAttributeOnParent(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)
    val simpleDocElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(referenceElem)

    val docElem = convertToDocElem(simpleDocElem, nullUri)

    assertResult(new URI("http://www.zvon.org/")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val referenceElem = docElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(referenceElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
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
    val simpleDocElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(pElem)

    val docElem = convertToDocElem(simpleDocElem, nullUri)

    assertResult(new URI("http://www.zvon.org/")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/zz/")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      pElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/zz/a.xml")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(referenceElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  @Test def testOtherNestedXmlBaseAttributes(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val referenceElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "a/", QName("xlink:href") -> "b.xml", QName("xlink:type") -> "simple"),
        scope)
    val simpleDocElem =
      emptyElem(
        QName("document"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(referenceElem)

    val docElem = convertToDocElem(simpleDocElem, new URI("http://www.zvon.org/a/b.xml"))

    assertResult(new URI("http://www.zvon.org/")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/a/")) {
      val referenceElem = docElem.getChildElem(_.localName == "reference")
      referenceElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/a/b.xml")) {
      val referenceElem = docElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(referenceElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
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
    val simpleDocElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/"),
        scope).plusChild(pElem)

    val docElem = convertToDocElem(simpleDocElem, nullUri)

    assertResult(new URI("http://www.zvon.org/")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      pElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(referenceElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  @Test def testEmptyDocUriAndXmlBaseAttribute(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val simpleDocElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val docElem = convertToDocElem(simpleDocElem, nullUri)

    assertResult(new URI("")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("a.xml")) {
      val href = new URI(docElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(docElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  @Test def testEmptyXmlBaseAttribute(): Unit = {
    val scope = Scope.from("xlink" -> XLinkNs)

    val simpleDocElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val docElem = convertToDocElem(simpleDocElem, new URI("http://www.somewhere.com/f1.xml"))

    assertResult(true) {
      Set(
        new URI("http://www.somewhere.com/"),
        new URI("http://www.somewhere.com/f1.xml")).contains(docElem.baseUriOption.getOrElse(URI.create("")))
    }
    assertResult(new URI("http://www.somewhere.com/a.xml")) {
      val href = new URI(docElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(docElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
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
    val simpleDocElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/yy/"),
        scope).plusChild(pElem)

    val docElem = convertToDocElem(simpleDocElem, nullUri)

    assertResult(new URI("http://www.zvon.org/yy/")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      pElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      referenceElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(referenceElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
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
    val simpleDocElem =
      emptyElem(
        QName("doc"),
        Vector(QName("xml:base") -> "http://www.zvon.org/yy/f1.xml"),
        scope).plusChild(pElem)

    val docElem = convertToDocElem(simpleDocElem, nullUri)

    assertResult(new URI("http://www.zvon.org/yy/f1.xml")) {
      docElem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(true) {
      val pElem = docElem.getChildElem(_.localName == "p")
      Set(
        new URI("http://www.zvon.org/yy/"),
        new URI("http://www.zvon.org/yy/f1.xml")).contains(pElem.baseUriOption.getOrElse(URI.create("")))
    }
    assertResult(true) {
      val pElem = docElem.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      Set(
        new URI("http://www.zvon.org/yy/"),
        new URI("http://www.zvon.org/yy/f1.xml")).contains(referenceElem.baseUriOption.getOrElse(URI.create("")))
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = docElem.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      resolveUri(href, Some(referenceElem.baseUriOption.getOrElse(URI.create(""))))
    }

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  /**
   * Tests an XML Base property relating it to the document URI and the ancestry-or-self.
   */
  private def testXmlBaseProperty1(elem: E): Unit = {
    require(elem.docUriOption.isDefined)

    val expectedBaseUri =
      XmlBaseSupport.findBaseUriByDocUriAndPath(elem.docUriOption, elem.rootElem, elem.path)(resolveUri).get

    assertResult(expectedBaseUri) {
      elem.baseUriOption.getOrElse(URI.create(""))
    }
  }

  /**
   * Tests an XML Base property relating it to the parent base URI and the element itself.
   */
  private def testXmlBaseProperty2(elem: E, docElem: E): Unit = {
    require(elem.docUriOption.isDefined)

    val parentBaseUriOption =
      elem.path.parentPathOption.map(pp => docElem.getElemOrSelfByPath(pp)).flatMap(_.baseUriOption).orElse(elem.docUriOption)

    val expectedBaseUri =
      XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, elem)(resolveUri).getOrElse(URI.create(""))

    assertResult(expectedBaseUri) {
      elem.baseUriOption.getOrElse(URI.create(""))
    }
  }
}
