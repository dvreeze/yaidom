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

import scala.collection.immutable

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapi.IsNavigableApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi

/**
 * XML Base test case.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractXmlBaseTest extends Suite {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  type E <: ScopedElemApi[E] with IsNavigableApi[E]

  type E2 <: ScopedElemApi[E2] with IsNavigableApi[E2]

  protected def getDocument(path: String, docUri: URI): DocumentApi[E]

  protected def getDocument(path: String): DocumentApi[E]

  protected def getBaseUri(elem: E): URI

  protected def getParentBaseUri(elem: E): URI

  protected def getDocumentUri(elem: E): URI

  protected def getAncestorsOrSelfReversed(elem: E): immutable.IndexedSeq[E2]

  protected def resolveUri(base: URI, uri: URI): URI = {
    if (uri.toString.isEmpty) base else base.resolve(uri)
  }

  protected def toUri(s: String): URI =
    Option(s).map(s => new URI(s)).getOrElse(new URI(""))

  @Test def testXmlBase(): Unit = {
    val doc = getDocument("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml")

    testXmlBase(doc.documentElement)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBase2(): Unit = {
    val doc = getDocument("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml")

    val elem = doc.documentElement

    testXmlBase(elem)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBase3(): Unit = {
    val doc = getDocument("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml", new URI("http://bogusBaseUri"))

    val elem = doc.documentElement

    testXmlBase(elem)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testXmlBase4(): Unit = {
    val doc = getDocument("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml")

    val elem = doc.documentElement.findElem(_.resolvedName == EName("olist")).get

    testXmlBaseOfNonRootElem(elem)

    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    doc.documentElement.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  @Test def testOtherXmlBase(): Unit = {
    val elem = testElem

    assertResult(new URI("http://example.org/wine/")) {
      getBaseUri(elem)
    }
    assertResult(new URI("http://example.org/wine/rose")) {
      val e = elem.getChildElem(_.localName == "e2")
      getBaseUri(e)
    }

    elem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty1(e))
    elem.findAllElemsOrSelf.foreach(e => testXmlBaseProperty2(e))
  }

  private def testXmlBase(elem: E): Unit = {
    assertResult(2) {
      elem.filterElemsOrSelf(e => e.attributeOption(XmlBaseEName).isDefined).size
    }
    assertResult(new URI("http://example.org/today/")) {
      getBaseUri(elem)
    }
    assertResult(Set(new URI("http://example.org/hotpicks/"))) {
      elem.filterElems(EName("olist")).map(e => getBaseUri(e)).toSet
    }
    assertResult(Set(
      new URI("http://example.org/today/new.xml"),
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          getBaseUri(e).resolve(href)
        }
      uris.toSet
    }
  }

  private def testXmlBaseOfNonRootElem(elem: E): Unit = {
    require(elem.resolvedName == EName("olist"))

    assertResult(new URI("http://example.org/hotpicks/")) {
      getBaseUri(elem)
    }

    assertResult(Set(
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        elem.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          getBaseUri(e).resolve(href)
        }
      uris.toSet
    }
  }

  private def testElem: E = {
    val doc = getDocument("/eu/cdevreeze/yaidom/queryapitests/miniXmlBaseTestFile.xml", new URI(""))
    doc.documentElement
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