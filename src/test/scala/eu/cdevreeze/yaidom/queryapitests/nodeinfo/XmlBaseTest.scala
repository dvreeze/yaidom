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

import java.io.File
import java.io.FileInputStream
import java.net.URI

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseTest
import eu.cdevreeze.yaidom.testsupport.SaxonTestSupport
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.lib.ParseOptions

/**
 * XML Base test case for Saxon wrapper Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseTest extends AbstractXmlBaseTest with SaxonTestSupport {

  type D = DomDocument

  type E = DomElem

  protected def getDocument(path: String, docUri: URI): DomDocument = {
    val parsedDocUri = classOf[XmlBaseTest].getResource(path).toURI
    val parseOptions = new ParseOptions
    val is = new FileInputStream(new File(parsedDocUri))
    val doc: DomDocument =
      DomNode.wrapDocument(
        processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is, docUri.toString), parseOptions))
    doc
  }

  protected def getDocument(path: String): DomDocument = {
    getDocument(path, classOf[XmlBaseTest].getResource(path).toURI)
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

  private def toUri(s: String): URI =
    Option(s).map(s => new URI(s)).getOrElse(new URI(""))
}
