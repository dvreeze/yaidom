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

package eu.cdevreeze.yaidom.queryapitests.resolved

import java.net.URI

import scala.reflect.classTag

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.indexed.IndexedClarkElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapitests.AbstractXmlBaseOnIndexedClarkElemApiTest
import eu.cdevreeze.yaidom.resolved.Elem

/**
 * XML Base test case for indexed Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlBaseOnIndexedClarkElemApiTest extends AbstractXmlBaseOnIndexedClarkElemApiTest {

  type U = Elem
  type E = IndexedClarkElem[U]

  protected def getDocElem(path: String, docUri: URI): E = {
    val docParser = DocumentParserUsingSax.newInstance
    val parsedDocUri = classOf[XmlBaseOnIndexedClarkElemApiTest].getResource(path).toURI
    val doc = docParser.parse(parsedDocUri)

    IndexedClarkElem(Some(docUri), Elem(doc.documentElement))
  }

  protected def getDocElem(path: String): E = {
    getDocElem(path, classOf[XmlBaseOnIndexedClarkElemApiTest].getResource(path).toURI)
  }
}
