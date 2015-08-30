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

package eu.cdevreeze.yaidom.queryapitests.indexed

import java.net.URI

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.queryapitests.AbstractAlternativeXmlBaseTest
import eu.cdevreeze.yaidom

/**
 * Alternative XML Base test case for indexed Elems. This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
 *
 * Note the use of empty URIs in some places.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AlternativeXmlBaseTest extends AbstractAlternativeXmlBaseTest {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")

  type E = yaidom.indexed.Elem

  type E2 = yaidom.simple.Elem

  protected def convertToDocument(elem: yaidom.simple.Elem, docUri: URI): DocumentApi[E] = {
    val doc = yaidom.indexed.Document.from(yaidom.simple.Document(Some(docUri), elem), resolveUri _)
    doc
  }

  protected def getBaseUri(elem: E): URI = {
    elem.baseUri
  }

  protected def getParentBaseUri(elem: E): URI = {
    elem.parentBaseUriOption.getOrElse(new URI(""))
  }

  protected def getDocumentUri(elem: E): URI = {
    elem.docUri
  }

  protected def getReverseAncestryOrSelf(elem: E): immutable.IndexedSeq[E2] = {
    elem.reverseAncestryOrSelf
  }

  // Naive resolveUri method
  protected override def resolveUri(uri: URI, baseUriOption: Option[URI]): URI = {
    // Note the different behavior for resolving the empty URI!
    XmlBaseSupport.JdkUriResolver(uri, baseUriOption)
  }

  protected def nullUri: URI = new URI("")
}
