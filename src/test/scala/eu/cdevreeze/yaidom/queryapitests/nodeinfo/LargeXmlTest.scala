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

import java.io.InputStream

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.queryapitests.AbstractLargeXmlTest
import eu.cdevreeze.yaidom.testsupport.SaxonTestSupport
import javax.xml.transform.stream.StreamSource

/**
 * Large XML test for Saxon-backed Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class LargeXmlTest extends AbstractLargeXmlTest with SaxonTestSupport {

  type D = DomDocument

  protected def parseDocument(is: InputStream): D = {
    val doc = processor.getUnderlyingConfiguration.buildDocumentTree(new StreamSource(is))
    DomNode.wrapDocument(doc)
  }
}
