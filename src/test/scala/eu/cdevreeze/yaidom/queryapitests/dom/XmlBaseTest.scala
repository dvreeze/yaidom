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

package eu.cdevreeze.yaidom.queryapitests.dom

import java.io.File
import java.io.FileInputStream
import java.net.URI

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseTest
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XML Base test case for DOM wrapper Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseTest extends AbstractXmlBaseTest {

  type E = DomElem

  type E2 = DomElem

  protected def getDocument(path: String, docUri: URI): DomDocument = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val docUri = classOf[XmlBaseTest].getResource(path).toURI

    val domDoc = db.parse(new FileInputStream(new File(docUri)))
    domDoc.setDocumentURI(docUri.toString)
    val doc = DomDocument(domDoc)
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
      toUri(elem.wrappedNode.getOwnerDocument.getDocumentURI))
  }

  protected def getDocumentUri(elem: E): URI = {
    toUri(elem.wrappedNode.getOwnerDocument.getDocumentURI)
  }

  protected def getAncestorsOrSelfReversed(elem: E): immutable.IndexedSeq[E2] = {
    elem.ancestorsOrSelf.reverse
  }
}
