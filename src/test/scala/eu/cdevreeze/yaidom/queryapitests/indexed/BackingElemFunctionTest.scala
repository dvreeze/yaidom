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

package eu.cdevreeze.yaidom.queryapitests.indexed

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.indexed.Document
import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.indexed.IndexedScopedElemFunctionApi
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapitests.AbstractBackingElemFunctionTest
import eu.cdevreeze.yaidom.queryapi.BackingElemFunctionApi

/**
 * Backing element function test for indexed Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class BackingElemFunctionTest extends AbstractBackingElemFunctionTest {

  private val docParser = DocumentParserUsingStax.newInstance()

  type E = Elem

  val docElem: E = {
    val docUri = classOf[AbstractBackingElemFunctionTest].getResource("some-data.xsd").toURI
    val doc = docParser.parse(docUri).withUriOption(Some(docUri))
    Document(doc).documentElement
  }

  val ops: BackingElemFunctionApi.Aux[E] = new IndexedScopedElemFunctionApi[simple.Elem]()
}
