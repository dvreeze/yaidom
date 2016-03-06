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

package eu.cdevreeze.yaidom.queryapitests.contextaware

import java.net.URI

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.contextaware
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ContextPath
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseTest
import eu.cdevreeze.yaidom.simple

/**
 * XML Base test case for contextaware Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseTest extends AbstractXmlBaseTest {

  private val XmlBaseEName = EName("http://www.w3.org/XML/1998/namespace", "base")

  type E = contextaware.Elem

  type E2 = ContextPath.Entry

  protected def getDocument(path: String, docUri: URI): contextaware.Document = {
    val docParser = DocumentParserUsingSax.newInstance
    val parsedDocUri = classOf[XmlBaseTest].getResource(path).toURI
    val doc = docParser.parse(parsedDocUri)

    contextaware.Document.from(doc.withUriOption(Some(docUri)), resolveUri _)
  }

  protected def getDocument(path: String): contextaware.Document = {
    getDocument(path, classOf[XmlBaseTest].getResource(path).toURI)
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
    elem.contextPath.entries
  }

  // Naive resolveUri method
  protected override def resolveUri(uri: URI, baseUriOption: Option[URI]): URI = {
    // Note the different behavior for resolving the empty URI!
    XmlBaseSupport.JdkUriResolver(uri, baseUriOption)
  }
}
