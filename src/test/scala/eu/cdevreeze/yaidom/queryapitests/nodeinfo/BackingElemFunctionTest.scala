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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.queryapi.BackingElemFunctionApi
import eu.cdevreeze.yaidom.queryapitests.AbstractBackingElemFunctionTest
import eu.cdevreeze.yaidom.testsupport.SaxonTestSupport
import net.sf.saxon.om.DocumentInfo

/**
 * Backing element function test for Saxon Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class BackingElemFunctionTest extends AbstractBackingElemFunctionTest with SaxonTestSupport {

  private val docBuilder = processor.newDocumentBuilder()

  type E = DomElem

  val docElem: E = {
    val docUri = classOf[AbstractBackingElemFunctionTest].getResource("some-data.xsd").toURI
    val doc = docBuilder.build(new File(docUri)).getUnderlyingNode().asInstanceOf[DocumentInfo]
    DomNode.wrapDocument(doc).documentElement
  }

  val ops: BackingElemFunctionApi.Aux[E] = DomElemFunctionApi
}
