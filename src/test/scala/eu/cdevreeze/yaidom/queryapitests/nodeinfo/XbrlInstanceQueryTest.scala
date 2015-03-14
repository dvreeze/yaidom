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

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapitests.AbstractXbrlInstanceQueryTest
import eu.cdevreeze.yaidom.resolved
import javax.xml.transform.stream.StreamSource
import net.sf.saxon.lib.ParseOptions
import net.sf.saxon.s9api.XdmNode

/**
 * XBRL instance query test case for Saxon wrapper elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceQueryTest extends AbstractXbrlInstanceQueryTest with SaxonTestSupport {

  final type E = DomElem

  protected final val xbrlInstance: DomElem = {
    val parseOptions = new ParseOptions

    val is = classOf[XbrlInstanceQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/sample-xbrl-instance.xml")

    val doc: DomDocument =
      DomNode.wrapDocument(
        processor.getUnderlyingConfiguration.buildDocument(new StreamSource(is), parseOptions))
    doc.documentElement
  }

  protected final def toResolvedElem(elem: E): resolved.Elem = {
    // Very inefficient!

    val bos = new ByteArrayOutputStream
    val serializer = processor.newSerializer(bos)
    serializer.serializeNode(new XdmNode(elem.wrappedNode))
    val xmlBytes = bos.toByteArray

    val docParser = DocumentParserUsingStax.newInstance
    eu.cdevreeze.yaidom.resolved.Elem(docParser.parse(new ByteArrayInputStream(xmlBytes)).documentElement)
  }
}
