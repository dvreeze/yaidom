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

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.queryapitests.AbstractBackingElemTest
import eu.cdevreeze.yaidom.saxon.SaxonDocument
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.s9api.Processor

/**
 * Backing element test for Saxon Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class BackingElemTest extends AbstractBackingElemTest {

  private val processor = new Processor(false)

  val docElem: E = {
    val docUri = classOf[AbstractBackingElemTest].getResource("some-data.xsd").toURI
    val inputSource = new StreamSource(new File(docUri))
    val doc = processor.getUnderlyingConfiguration.buildDocumentTree(inputSource)
    SaxonDocument.wrapDocument(doc).documentElement
  }
}
