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

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.docaware
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi

/**
 * XML Base test case for docaware Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseTest extends Suite {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  @Test def testInternalConsistency(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance
    val docUri = classOf[XmlBaseTest].getResource("/eu/cdevreeze/yaidom/queryapitests/xmlBaseTestFile.xml").toURI
    val doc = docParser.parse(docUri)
    val docawareDoc = docaware.Document(docUri, doc)

    assertResult(2) {
      docawareDoc.documentElement.filterElemsOrSelf(e => e.attributeOption(XmlBaseEName).isDefined).size
    }
    assertResult(new URI("http://example.org/today/")) {
      docawareDoc.documentElement.baseUri
    }
    assertResult(Set(new URI("http://example.org/hotpicks/"))) {
      docawareDoc.documentElement.filterElems(EName("olist")).map(_.baseUri).toSet
    }
    assertResult(Set(
      new URI("http://example.org/today/new.xml"),
      new URI("http://example.org/hotpicks/pick1.xml"),
      new URI("http://example.org/hotpicks/pick2.xml"),
      new URI("http://example.org/hotpicks/pick3.xml"))) {

      val uris =
        docawareDoc.documentElement.filterElems(EName("link")) map { e =>
          val href = new URI(e.attribute(EName(XLinkNs, "href")))
          e.baseUri.resolve(href)
        }
      uris.toSet
    }
  }
}
