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
import javax.xml.parsers.{ SAXParserFactory, SAXParser }
import org.xml.sax.helpers.DefaultHandler
import jinterop.ElemProducingSaxContentHandler

/** SAX-based Document parser */
final class DocumentParserUsingSax(
  val saxParserFactory: SAXParserFactory,
  val saxParserCreator: SAXParserFactory => SAXParser,
  val defaultHandler: ElemProducingSaxContentHandler) extends DocumentParser {

  def this(spf: SAXParserFactory) = this(
    saxParserFactory = spf,
    saxParserCreator = { spf =>
      val parser = spf.newSAXParser()
      parser
    },
    defaultHandler = new DefaultHandler with ElemProducingSaxContentHandler)

  /** Parses the input stream into a yaidom Document. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    try {
      val sp: SAXParser = saxParserCreator(saxParserFactory)
      sp.parse(inputStream, defaultHandler)

      val doc: Document = defaultHandler.resultingDocument
      doc
    } finally {
      if (inputStream ne null) inputStream.close()
    }
  }
}

object DocumentParserUsingSax {

  /** Returns a new instance */
  def newInstance(): DocumentParserUsingSax = {
    val spf = SAXParserFactory.newInstance
    new DocumentParserUsingSax(spf)
  }
}
