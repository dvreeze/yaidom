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

package eu.cdevreeze.yaidom.queryapitests.docaware

import java.net.URI

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.docaware
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.simple

/**
 * Alternative XML Base test case for docaware Elems. This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
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
    val scope = Scope.from("xlink" -> XLinkNs)

    val docElem =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc = docaware.Document(new URI("http://www.somewhere.com/f2.xml"), simple.Document(docElem))

    assertResult(new URI("http://www.zvon.org/")) {
      doc.documentElement.baseUri
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      doc.documentElement.baseUri.resolve(href)
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

    val doc1 = docaware.Document(new URI(""), simple.Document(docElem1))

    val docElem2 =
      emptyElem(
        QName("reference"),
        Vector(QName("xml:base") -> "http://www.zvon.org/", QName("xlink:href") -> "a.xml", QName("xlink:type") -> "simple"),
        scope)

    val doc2 = docaware.Document(new URI(""), simple.Document(docElem2))

    assertResult(new URI("")) {
      doc1.documentElement.baseUri
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc1.documentElement.attribute(XLinkHrefEName))
      doc1.documentElement.baseUri.resolve(href)
    }

    assertResult(new URI("http://www.zvon.org/")) {
      doc2.documentElement.baseUri
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val href = new URI(doc2.documentElement.attribute(XLinkHrefEName))
      doc2.documentElement.baseUri.resolve(href)
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

    val doc = docaware.Document(new URI("http://www.somewhere.com/f1.xml"), simple.Document(docElem))

    assertResult(new URI("http://www.somewhere.com/f1.xml")) {
      doc.documentElement.baseUri
    }
    assertResult(new URI("http://www.somewhere.com/a.xml")) {
      val href = new URI(doc.documentElement.attribute(XLinkHrefEName))
      doc.documentElement.baseUri.resolve(href)
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

    val doc = docaware.Document(new URI(""), simple.Document(docElem))

    assertResult(new URI("http://www.zvon.org/")) {
      doc.documentElement.baseUri
    }
    assertResult(new URI("http://www.zvon.org/a.xml")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      referenceElem.baseUri.resolve(href)
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

    val doc = docaware.Document(new URI(""), simple.Document(docElem))

    assertResult(new URI("http://www.zvon.org/")) {
      doc.documentElement.baseUri
    }
    assertResult(new URI("http://www.zvon.org/zz/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      pElem.baseUri
    }
    assertResult(new URI("http://www.zvon.org/zz/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      referenceElem.baseUri.resolve(href)
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

    val doc = docaware.Document(new URI("http://www.zvon.org/a/b.xml"), simple.Document(docElem))

    assertResult(new URI("http://www.zvon.org/")) {
      doc.documentElement.baseUri
    }
    assertResult(new URI("http://www.zvon.org/a/")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      referenceElem.baseUri
    }
    assertResult(new URI("http://www.zvon.org/a/b.xml")) {
      val referenceElem = doc.documentElement.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      referenceElem.baseUri.resolve(href)
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

    val doc = docaware.Document(new URI(""), simple.Document(docElem))

    assertResult(new URI("http://www.zvon.org/")) {
      doc.documentElement.baseUri
    }
    assertResult(new URI("http://www.zvon.org/yy/")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      pElem.baseUri
    }
    assertResult(new URI("http://www.zvon.org/yy/a.xml")) {
      val pElem = doc.documentElement.getChildElem(_.localName == "p")
      val referenceElem = pElem.getChildElem(_.localName == "reference")
      val href = new URI(referenceElem.attribute(XLinkHrefEName))
      referenceElem.baseUri.resolve(href)
    }

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  private def testXmlBaseProperty1(elem: docaware.Elem): Unit = {
    val ancestorsOrSelf =
      elem.path.ancestorOrSelfPaths.reverse.map(p => elem.rootElem.getElemOrSelfByPath(p))

    val expectedBaseUri =
      ancestorsOrSelf.foldLeft(elem.docUri) {
        case (currBaseUri, e) =>
          e.attributeOption(docaware.Elem.XmlBaseEName).map(s => currBaseUri.resolve(new URI(s))).getOrElse(currBaseUri)
      }

    assertResult(expectedBaseUri) {
      elem.baseUri
    }
  }

  private def testXmlBaseProperty2(elem: docaware.Elem): Unit = {
    val parentBaseUri = elem.parentBaseUri
    val expectedBaseUri =
      elem.attributeOption(docaware.Elem.XmlBaseEName).map(s => parentBaseUri.resolve(new URI(s))).getOrElse(parentBaseUri)

    assertResult(expectedBaseUri) {
      elem.baseUri
    }
  }
}
