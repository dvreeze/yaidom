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
import scala.util.control.Exception._
import jinterop.StaxConversions._

/** StAX-based Document parser */
final class DocumentParserUsingStax(val xmlInputFactory: XMLInputFactory) extends DocumentParser {

  /** Parses the input stream into a yaidom Document. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    var xmlEventReader: XMLEventReader = null
    try {
      xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream)
      convertToDocument(xmlEventReader.toSeq)
    } finally {
      ignoring(classOf[Exception]) {
        if (xmlEventReader ne null) xmlEventReader.close()
      }
      ignoring(classOf[Exception]) {
        if (inputStream ne null) inputStream.close()
      }
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
