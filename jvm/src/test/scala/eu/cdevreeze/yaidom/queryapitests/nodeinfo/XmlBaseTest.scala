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

package eu.cdevreeze.yaidom.queryapitests.nodeinfo

import java.io.File
import java.io.FileInputStream
import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseTest
import eu.cdevreeze.yaidom.saxon.SaxonDocument
import eu.cdevreeze.yaidom.saxon.SaxonElem
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.lib.ParseOptions
import net.sf.saxon.s9api.Processor

/**
 * XML Base test case for Saxon wrapper Elems.
 *
 * @author Chris de Vreeze
 */
class XmlBaseTest extends AbstractXmlBaseTest {

  type D = SaxonDocument

  type E = SaxonElem

  private val processor = new Processor(false)

  protected def getDocument(path: String, docUri: URI): SaxonDocument = {
    val parsedDocUri = classOf[XmlBaseTest].getResource(path).toURI
    val parseOptions = new ParseOptions
    val is = new FileInputStream(new File(parsedDocUri))
    val doc: SaxonDocument =
      SaxonDocument.wrapDocument(
        processor.getUnderlyingConfiguration.buildDocumentTree(new StreamSource(is, docUri.toString), parseOptions))
    doc
  }

  protected def getDocument(path: String): SaxonDocument = {
    getDocument(path, classOf[XmlBaseTest].getResource(path).toURI)
  }

  protected def getBaseUri(elem: E): URI = {
    toUri(elem.wrappedNode.getBaseURI)
  }

  protected def getParentBaseUri(elem: E): URI = {
    elem.parentOption.map(e => toUri(e.wrappedNode.getBaseURI)).getOrElse(
      toUri(elem.wrappedNode.getSystemId))
  }

  protected def getDocumentUri(elem: E): URI = {
    toUri(elem.wrappedNode.getSystemId)
  }

  protected def getReverseAncestryOrSelf(elem: E): immutable.IndexedSeq[E] = {
    elem.ancestorsOrSelf.reverse
  }

  private def toUri(s: String): URI =
    Option(s).map(s => new URI(s)).getOrElse(new URI(""))
}
