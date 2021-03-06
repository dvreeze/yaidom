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

package eu.cdevreeze.yaidom.queryapitests.indexed

import java.net.URI

import eu.cdevreeze.yaidom.indexed.Document
import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseOnIndexedClarkElemApiTest
import eu.cdevreeze.yaidom.simple

/**
 * XML Base test case for indexed Elems.
 *
 * @author Chris de Vreeze
 */
class XmlBaseOnIndexedClarkElemApiTest extends AbstractXmlBaseOnIndexedClarkElemApiTest {

  type U = simple.Elem
  type E = Elem

  protected def getDocElem(path: String, docUri: URI): E = {
    val docParser = DocumentParserUsingSax.newInstance()
    val parsedDocUri = classOf[XmlBaseOnIndexedClarkElemApiTest].getResource(path).toURI
    val doc = docParser.parse(parsedDocUri)

    Document.from(doc.withUriOption(Some(docUri))).documentElement
  }

  protected def getDocElem(path: String): E = {
    getDocElem(path, classOf[XmlBaseOnIndexedClarkElemApiTest].getResource(path).toURI)
  }
}
