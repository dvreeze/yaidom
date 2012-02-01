/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package parse

import java.{ io => jio }
import javax.xml.stream.{ XMLInputFactory, XMLEventReader }
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.Source
import jinterop.StaxConversions._

/** StAX-based Document parser */
final class DocumentParserUsingStax(val xmlInputFactory: XMLInputFactory) extends DocumentParser {

  def parse(source: Source): Document = {
    var xmlEventReader: XMLEventReader = null
    try {
      xmlEventReader = xmlInputFactory.createXMLEventReader(source)
      convertToDocument(xmlEventReader.toSeq)
    } finally {
      if (xmlEventReader ne null) xmlEventReader.close()
    }
  }
}

object DocumentParserUsingStax {

  /** Returns a new instance, configured to coalesce whitespace */
  def newInstance(): DocumentParserUsingStax = {
    val xmlInputFactory = XMLInputFactory.newFactory
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    new DocumentParserUsingStax(xmlInputFactory)
  }
}
