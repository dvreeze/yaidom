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
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * XML Base test case. It tests some expectations about XML Base support for different element implementations.
 * Hence, for DOM and Scala XML wrapper elements, it tests expectations about XML Base support for the underlying libraries.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractXmlBaseTest extends Suite {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  type E <: ScopedElemApi[E]

  type E2 <: ScopedElemApi[E2]

  protected def getDocument(path: String, docUri: URI): DocumentApi[E]

  protected def getDocument(path: String): DocumentApi[E]

  protected def getBaseUri(elem: E): URI

  protected def getParentBaseUri(elem: E): URI

  protected def getDocumentUri(elem: E): URI

  protected def getReverseAncestryOrSelf(elem: E): immutable.IndexedSeq[E2]

  // Naive resolveUri method
  protected def resolveUri(uri: URI, baseUriOption: Option[URI]): URI = {
    val baseUri = baseUriOption.getOrElse(new URI(""))

    if (uri.toString.isEmpty) baseUri else baseUri.resolve(uri)
  }

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
    val elem = getTestElem

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
          resolveUri(href, Some(getBaseUri(e)))
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
          resolveUri(href, Some(getBaseUri(e)))
        }
      uris.toSet
    }
  }

  private def getTestElem: E = {
    val doc = getDocument("/eu/cdevreeze/yaidom/queryapitests/miniXmlBaseTestFile.xml", new URI(""))
    doc.documentElement
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
