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

package eu.cdevreeze.yaidom.queryapitests.dom

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapitests.AbstractAlternativeXmlBaseTest
import eu.cdevreeze.yaidom.simple.Document
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Alternative XML Base test case for DOM wrapper Elems. This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
 *
 * Note the use of empty URIs in some places.
 *
 * @author Chris de Vreeze
 */
class AlternativeXmlBaseTest extends AbstractAlternativeXmlBaseTest {

  type D = DomDocument

  type E = DomElem

  protected def convertToDocument(elem: yaidom.simple.Elem, docUri: URI): DocumentApi.Aux[D, E] = {
    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder
    val d = db.newDocument()
    DomConversions.convertDocument(Document(elem))(d)
    d.setDocumentURI(docUri.toString)
    val doc = DomDocument(d)
    doc
  }

  protected def getBaseUri(elem: E): URI = {
    toUri(elem.wrappedNode.getBaseURI)
  }

  protected def getParentBaseUri(elem: E): URI = {
    elem.parentOption.map(e => toUri(e.wrappedNode.getBaseURI)).getOrElse(
      toUri(elem.wrappedNode.getOwnerDocument.getDocumentURI))
  }

  protected def getDocumentUri(elem: E): URI = {
    toUri(elem.wrappedNode.getOwnerDocument.getDocumentURI)
  }

  protected def getReverseAncestryOrSelf(elem: E): immutable.IndexedSeq[E] = {
    elem.ancestorsOrSelf.reverse
  }

  protected def nullUri: URI = new URI("")

  private def toUri(s: String): URI =
    Option(s).map(s => new URI(s)).getOrElse(new URI(""))
}
