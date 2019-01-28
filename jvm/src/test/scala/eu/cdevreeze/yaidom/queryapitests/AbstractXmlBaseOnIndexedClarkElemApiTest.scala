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

import org.scalatest.FunSuite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.queryapi.IndexedClarkElemApi
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * XML Base test case using an IndexedClarkElemApi.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractXmlBaseOnIndexedClarkElemApiTest extends FunSuite {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  type U <: ClarkElemApi.Aux[U]
  type E <: IndexedClarkElemApi.Aux[E]

  protected def getDocElem(path: String, docUri: URI): E

  protected def getDocElem(path: String): E

  // Naive resolveUri method
  protected def resolveUri(uri: URI, baseUriOption: Option[URI]): URI = {
    XmlBaseSupport.JdkUriResolver(uri, baseUriOption)
  }

  test("testXmlBase") {
    val docElem = getDocElem("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml")

    testXmlBase(docElem)

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  test("testXmlBase2") {
    val docElem = getDocElem("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml")

    val elem = docElem

    testXmlBase(elem)

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  test("testXmlBase3") {
    val docElem = getDocElem("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml", new URI("http://bogusBaseUri"))

    val elem = docElem

    testXmlBase(elem)

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  test("testXmlBase4") {
    val docElem = getDocElem("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml")

    val elem = docElem.findElem(_.resolvedName == EName("olist")).get

    testXmlBaseOfNonRootElem(elem)

    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    docElem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, docElem))
  }

  test("testOtherXmlBase") {
    val elem = getTestElem

    assertResult(new URI("http://example.org/wine/")) {
      elem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(new URI("http://example.org/wine/rose")) {
      val e = elem.getChildElem(_.localName == "e2")
      e.baseUriOption.getOrElse(URI.create(""))
    }

    elem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    elem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e, elem))
  }

  private def testXmlBase(elem: E): Unit = {
    assertResult(2) {
      elem.filterElemsOrSelf(e => e.attributeOption(XmlBaseEName).isDefined).size
    }
    assertResult(new URI("http://example.org/today/")) {
      elem.baseUriOption.getOrElse(URI.create(""))
    }
    assertResult(Set(new URI("http://example.org/hotpicks/"))) {
      elem.filterElems(EName("olist")).map(e => e.baseUriOption.getOrElse(URI.create(""))).toSet
    }
    assertResult(Set(
      new URI("http://example.org/today/new.xml"),
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          resolveUri(href, Some(e.baseUriOption.getOrElse(URI.create(""))))
        }
      uris.toSet
    }
  }

  private def testXmlBaseOfNonRootElem(elem: E): Unit = {
    require(elem.resolvedName == EName("olist"))

    assertResult(new URI("http://example.org/hotpicks/")) {
      elem.baseUriOption.getOrElse(URI.create(""))
    }

    assertResult(Set(
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          resolveUri(href, Some(e.baseUriOption.getOrElse(URI.create(""))))
        }
      uris.toSet
    }
  }

  private def getTestElem: E = {
    val docElem = getDocElem("/eu/cdevreeze/yaidom/queryapitests/miniXmlBaseTestFile.xml", new URI(""))
    docElem
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
