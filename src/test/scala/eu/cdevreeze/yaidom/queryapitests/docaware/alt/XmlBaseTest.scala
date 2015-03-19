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

package eu.cdevreeze.yaidom.queryapitests.docaware.alt

import java.net.URI

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.docaware
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseTest
import eu.cdevreeze.yaidom.simple

/**
 * XML Base test case for docaware.alt Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseTest extends AbstractXmlBaseTest {

  type E = docaware.alt.Elem

  type E2 = simple.Elem

  protected def getDocument(path: String, docUri: URI): docaware.alt.Document = {
    val docParser = DocumentParserUsingSax.newInstance
    val parsedDocUri = classOf[XmlBaseTest].getResource(path).toURI
    val doc = docParser.parse(parsedDocUri)

    docaware.alt.Document(docUri, doc)
  }

  protected def getDocument(path: String): docaware.alt.Document = {
    getDocument(path, classOf[XmlBaseTest].getResource(path).toURI)
  }

  protected def getBaseUri(elem: E): URI = {
    elem.baseUri
  }

  protected def getParentBaseUri(elem: E): URI = {
    elem.parentBaseUri
  }

  protected def getDocumentUri(elem: E): URI = {
    elem.docUri
  }

  protected def getAncestorsOrSelfReversed(elem: E): immutable.IndexedSeq[E2] = {
    elem.path.ancestorOrSelfPaths.reverse.map(p => elem.rootElem.getElemOrSelfByPath(p))
  }

  protected override def resolveUri(base: URI, uri: URI): URI = {
    // Note the different behavior for resolving the empty URI!
    base.resolve(uri)
  }
}