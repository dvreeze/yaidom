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

import eu.cdevreeze.yaidom.queryapitests.AbstractXbrlInstanceQueryTest
import eu.cdevreeze.yaidom.saxon.SaxonDocument
import eu.cdevreeze.yaidom.saxon.SaxonElem
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.lib.ParseOptions
import net.sf.saxon.s9api.Processor

/**
 * XBRL instance query test case for Saxon wrapper elements.
 *
 * @author Chris de Vreeze
 */
class XbrlInstanceQueryTest extends AbstractXbrlInstanceQueryTest {

  final type E = SaxonElem

  private val processor = new Processor(false)

  protected final val xbrlInstance: SaxonElem = {
    val parseOptions = new ParseOptions

    val is = classOf[XbrlInstanceQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/sample-xbrl-instance.xml")

    val doc: SaxonDocument =
      SaxonDocument.wrapDocument(
        processor.getUnderlyingConfiguration.buildDocumentTree(new StreamSource(is), parseOptions))
    doc.documentElement
  }
}
