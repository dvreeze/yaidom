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
import org.xml.sax.ext.LexicalHandler
import jinterop.{ ElemProducingSaxContentHandler, DefaultElemProducingSaxContentHandler }

/**
 * SAX-based Document parser.
 *
 * Typical non-trivial creation is as follows, assuming a trait <code>MyEntityHandler</code>, which extends <code>EntityHandler</code>,
 * and a trait <code>MyErrorHandler</code>, which extends <code>ErrorHandler</code>:
 * {{{
 * val parser = DocumentParserUsingSax.newInstance(
 *   SAXParserFactory.newInstance,
 *   new DefaultElemProducingSaxContentHandler with MyEntityResolver with MyErrorHandler
 * )
 * }}}
 *
 * A custom <code>EntityHandler</code> could be used to retrieve DTDs locally, or even to suppress DTD resolution.
 * The latter can be coded as follows (see http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds):
 * {{{
 * trait MyEntityHandler extends EntityHandler {
 *   override def resolveEntity(publicId: String, systemId: String): InputSource = {
 *     new InputSource(new java.io.StringReader(""))
 *   }
 * }
 * }}}
 */
final class DocumentParserUsingSax(
  val saxParserFactory: SAXParserFactory,
  val saxParserCreator: SAXParserFactory => SAXParser,
  val defaultHandler: ElemProducingSaxContentHandler) extends DocumentParser {

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

  /** Returns a new instance. Same as <code>newInstance(SAXParserFactory.newInstance)</code>. */
  def newInstance(): DocumentParserUsingSax = {
    val spf = SAXParserFactory.newInstance
    DocumentParserUsingSax.newInstance(spf)
  }

  /** Returns <code>newInstance(spf, new DefaultElemProducingSaxContentHandler {}))</code>. */
  def newInstance(spf: SAXParserFactory): DocumentParserUsingSax =
    newInstance(spf, new DefaultElemProducingSaxContentHandler {})

  /**
   * Invokes the constructur on <code>spf</code>, a <code>SAXParserFactory => SAXParser</code> "SAX parser creator", and
   * <code>handler</code>. The "SAX parser creator" invokes <code>spf.newSAXParser()</code>, but it also recognizes if the handler is a
   * <code>LexicalHandler</code>, and, if so, registers that handler as <code>LexicalHandler</code>. The underlying assumption
   * is that in practice all SAX parsers support LexicalHandlers.
   */
  def newInstance(spf: SAXParserFactory, handler: ElemProducingSaxContentHandler): DocumentParserUsingSax = {
    new DocumentParserUsingSax(
      saxParserFactory = spf,
      saxParserCreator = { spf =>
        val parser = spf.newSAXParser()

        if (handler.isInstanceOf[LexicalHandler]) {
          // Property "http://xml.org/sax/properties/lexical-handler" registers a LexicalHandler. See the corresponding API documentation.
          // It is assumed here that in practice all SAX parsers support LexicalHandlers.
          parser.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", handler)
        }

        parser
      },
      defaultHandler = handler)
  }
}
