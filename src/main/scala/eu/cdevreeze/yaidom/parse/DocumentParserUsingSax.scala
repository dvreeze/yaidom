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

package eu.cdevreeze.yaidom.parse

import java.{ io => jio }

import org.xml.sax.InputSource
import org.xml.sax.ext.LexicalHandler
import org.xml.sax.helpers.DefaultHandler

import eu.cdevreeze.yaidom.simple.Document
import javax.xml.parsers.SAXParser
import javax.xml.parsers.SAXParserFactory

/**
 * SAX-based `Document` parser.
 *
 * Typical non-trivial creation is as follows, assuming a trait `MyEntityResolver`, which extends `EntityResolver`,
 * and a trait `MyErrorHandler`, which extends `ErrorHandler`:
 * {{{
 * val spf = SAXParserFactory.newInstance().makeNamespaceAndPrefixAware
 *
 * val parser = DocumentParserUsingSax.newInstance(
 *   spf,
 *   () => new DefaultElemProducingSaxHandler with MyEntityResolver with MyErrorHandler
 * )
 * }}}
 *
 * If we want the `SAXParserFactory` to be a validating one, using an XML Schema, we could obtain the `SAXParserFactory` as follows:
 * {{{
 * val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
 * val schemaSource = new StreamSource(new File(pathToSchema))
 * val schema = schemaFactory.newSchema(schemaSource)
 *
 * val spf = {
 *   val result = SAXParserFactory.newInstance().makeNamespaceAndPrefixAware
 *   result.setSchema(schema)
 *   result
 * }
 * }}}
 *
 * A custom `EntityResolver` could be used to retrieve DTDs locally, or even to suppress DTD resolution.
 * The latter can be coded as follows (see http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds),
 * risking some loss of information:
 * {{{
 * trait MyEntityResolver extends EntityResolver {
 *   override def resolveEntity(publicId: String, systemId: String): InputSource = {
 *     // This dirty hack may not work on IBM JVMs
 *     new InputSource(new java.io.StringReader(""))
 *   }
 * }
 * }}}
 *
 * For completeness, a custom `ErrorHandler` trait that simply prints parse exceptions to standard output:
 * {{{
 * trait MyErrorHandler extends ErrorHandler {
 *   override def warning(exc: SAXParseException): Unit = { println(exc) }
 *   override def error(exc: SAXParseException): Unit = { println(exc) }
 *   override def fatalError(exc: SAXParseException): Unit = { println(exc) }
 * }
 * }}}
 *
 * It is even possible to parse HTML (including very poor HTML) into well-formed Documents by using a `SAXParserFactory` from the TagSoup library.
 * For example:
 * {{{
 * val parser = DocumentParserUsingSax.newInstance(new org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl)
 * }}}
 *
 * If more flexibility is needed in configuring the `DocumentParser` than offered by this class, consider
 * writing a wrapper `DocumentParser` which wraps a `DocumentParserUsingSax`, but adapts the `parse` method.
 * This would make it possible to set additional properties on the XML Reader, for example.
 *
 * As can be seen above, parsing is based on the JAXP `SAXParserFactory` instead of the SAX 2.0 `XMLReaderFactory`.
 *
 * A `DocumentParserUsingSax` instance can be re-used multiple times, from the same thread.
 * If the `SAXParserFactory` is thread-safe, it can even be re-used from multiple threads.
 * Typically a `SAXParserFactory` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `SAXParserFactory` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentParserUsingSax(
  val parserFactory: SAXParserFactory,
  val parserCreator: SAXParserFactory => SAXParser,
  val handlerCreator: () => ElemProducingSaxHandler) extends AbstractDocumentParser {

  /**
   * Parses the input stream into a yaidom `Document`. Closes the input stream afterwards.
   *
   * If the created `DefaultHandler` is a `LexicalHandler`, this `LexicalHandler` is registered. In practice all SAX parsers
   * should support LexicalHandlers.
   */
  def parse(inputStream: jio.InputStream): Document = {
    try {
      val sp: SAXParser = parserCreator(parserFactory)
      val handler = handlerCreator()

      if (handler.isInstanceOf[LexicalHandler]) {
        // Property "http://xml.org/sax/properties/lexical-handler" registers a LexicalHandler. See the corresponding API documentation.
        // It is assumed here that in practice all SAX parsers support LexicalHandlers.
        sp.getXMLReader().setProperty("http://xml.org/sax/properties/lexical-handler", handler)
      }

      // See http://docs.oracle.com/cd/E13222_01/wls/docs90/xml/best.html
      val inputSource = new InputSource(inputStream)
      sp.parse(inputSource, handler)

      val doc: Document = handler.resultingDocument
      doc
    } finally {
      if (inputStream ne null) inputStream.close()
    }
  }
}

object DocumentParserUsingSax {

  /**
   * Returns a new instance. Same as:
   * {{{
   * newInstance(SAXParserFactory.newInstance.makeNamespaceAndPrefixAware)
   * }}}
   * Calling the `setNamespaceAware` method instead does not suffice, and is not needed.
   * See http://www.cafeconleche.org/slides/xmlone/london2002/namespaces/36.html.
   */
  def newInstance(): DocumentParserUsingSax = {
    val spf = SAXParserFactory.newInstance.makeNamespaceAndPrefixAware
    newInstance(spf)
  }

  /**
   * Returns `newInstance(parserFactory, new DefaultElemProducingSaxHandler {})`.
   *
   * Do not forget to configure namespace handling, by calling `makeNamespaceAndPrefixAware`
   * on the `SAXParserFactory`.
   */
  def newInstance(parserFactory: SAXParserFactory): DocumentParserUsingSax = {
    newInstance(parserFactory, () => new DefaultElemProducingSaxHandler {})
  }

  /**
   * Invokes the 3-arg `newInstance` method on `parserFactory`, a `SAXParserFactory => SAXParser` "SAX parser creator", and
   * `handlerCreator`. The "SAX parser creator" invokes `parserFactory.newSAXParser()`.
   *
   * Do not forget to configure namespace handling, by calling `makeNamespaceAndPrefixAware`
   * on the `SAXParserFactory`.
   */
  def newInstance(parserFactory: SAXParserFactory, handlerCreator: () => ElemProducingSaxHandler): DocumentParserUsingSax = {
    newInstance(
      parserFactory = parserFactory,
      parserCreator = { (spf: SAXParserFactory) =>
        val parser = spf.newSAXParser()
        parser
      },
      handlerCreator = handlerCreator)
  }

  /**
   * Returns a new instance, by invoking the primary constructor.
   *
   * Do not forget to configure namespace handling, by calling `makeNamespaceAndPrefixAware`
   * on the `SAXParserFactory`.
   */
  def newInstance(
    parserFactory: SAXParserFactory,
    parserCreator: SAXParserFactory => SAXParser,
    handlerCreator: () => ElemProducingSaxHandler): DocumentParserUsingSax = {

    new DocumentParserUsingSax(parserFactory, parserCreator, handlerCreator)
  }

  /**
   * Enriches a `SAXParserFactory` with method `makeNamespaceAndPrefixAware`.
   */
  final implicit class RichSAXParserFactory(val saxParserFactory: SAXParserFactory) {

    /**
     * Returns the SAXParserFactory, after configuring it as follows:
     * {{{
     * setFeature("http://xml.org/sax/features/namespaces", true)
     * setFeature("http://xml.org/sax/features/namespace-prefixes", true)
     * }}}
     */
    def makeNamespaceAndPrefixAware: SAXParserFactory = {
      saxParserFactory.setFeature("http://xml.org/sax/features/namespaces", true)
      saxParserFactory.setFeature("http://xml.org/sax/features/namespace-prefixes", true)
      saxParserFactory
    }
  }
}
