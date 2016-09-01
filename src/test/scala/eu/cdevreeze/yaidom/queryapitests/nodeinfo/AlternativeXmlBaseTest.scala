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

package eu.cdevreeze.yaidom.queryapitests.nodeinfo

import java.io.ByteArrayInputStream
import java.net.URI

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapitests.AbstractAlternativeXmlBaseTest
import eu.cdevreeze.yaidom.testsupport.SaxonTestSupport
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.lib.ParseOptions

/**
 * Alternative XML Base test case for Saxon wrapper Elems. This test uses the XML Base tutorial at: http://zvon.org/comp/r/tut-XML_Base.html.
 *
 * Note the use of empty URIs in some places.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AlternativeXmlBaseTest extends AbstractAlternativeXmlBaseTest with SaxonTestSupport {

  type D = DomDocument

  type E = DomElem

  protected def convertToDocument(elem: yaidom.simple.Elem, docUri: URI): DocumentApi.Aux[D, E] = {
    val docPrinter = DocumentPrinterUsingDom.newInstance
    val xmlString = docPrinter.print(elem)

    val parseOptions = new ParseOptions
    val is = new ByteArrayInputStream(xmlString.getBytes("UTF-8"))
    val doc: DomDocument =
      DomNode.wrapDocument(
        processor.getUnderlyingConfiguration.buildDocument(
          new StreamSource(is, Option(docUri).map(_.toString).getOrElse(null)), parseOptions))
    doc
  }

  protected def getBaseUri(elem: E): URI = {
    toUri(elem.wrappedNode.getBaseURI)
  }

  protected def getParentBaseUri(elem: E): URI = {
    elem.parentOption.map(e => toUri(e.wrappedNode.getBaseURI)).getOrElse(
      toUri(elem.wrappedNode.getDocumentRoot.getSystemId))
  }

  protected def getDocumentUri(elem: E): URI = {
    toUri(elem.wrappedNode.getDocumentRoot.getSystemId)
  }

  protected def getReverseAncestryOrSelf(elem: E): immutable.IndexedSeq[E] = {
    elem.ancestorsOrSelf.reverse
  }

  protected def nullUri: URI = null

  private def toUri(s: String): URI =
    Option(s).map(s => new URI(s)).getOrElse(new URI(""))
}
