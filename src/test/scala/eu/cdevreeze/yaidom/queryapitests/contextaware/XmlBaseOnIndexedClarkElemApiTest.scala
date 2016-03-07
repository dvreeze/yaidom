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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.contextaware.Document
import eu.cdevreeze.yaidom.contextaware.Elem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseOnContextAwareClarkElemApiTest
import eu.cdevreeze.yaidom.simple

/**
 * XML Base test case for contextaware Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseOnContextAwareClarkElemApiTest extends AbstractXmlBaseOnContextAwareClarkElemApiTest {

  type U = simple.Elem
  type E = Elem

  protected def getDocElem(path: String, docUri: URI): E = {
    val docParser = DocumentParserUsingSax.newInstance
    val parsedDocUri = classOf[XmlBaseOnContextAwareClarkElemApiTest].getResource(path).toURI
    val doc = docParser.parse(parsedDocUri)

    Document.from(doc.withUriOption(Some(docUri)), resolveUri _).documentElement
  }

  protected def getDocElem(path: String): E = {
    getDocElem(path, classOf[XmlBaseOnContextAwareClarkElemApiTest].getResource(path).toURI)
  }
}
