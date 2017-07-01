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

package eu.cdevreeze.yaidom.parse

import java.{ io => jio }

import scala.collection.Iterator
import scala.util.control.Exception.ignoring

import eu.cdevreeze.yaidom.convert.StaxConversions
import eu.cdevreeze.yaidom.simple.ConverterToDocument
import eu.cdevreeze.yaidom.simple.Document
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.stream.StreamSource

/**
 * StAX-based `Document` parser.
 *
 * Typical non-trivial creation is as follows, assuming a class `MyXmlResolver`, which extends `XMLResolver`,
 * and a class `MyXmlReporter`, which extends `XMLReporter`:
 * {{{
 * val xmlInputFactory = XMLInputFactory.newFactory()
 * xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
 * xmlInputFactory.setXMLResolver(new MyXmlResolver)
 * xmlInputFactory.setXMLReporter(new MyXmlReporter)
 *
 * val docParser = DocumentParserUsingStax.newInstance(xmlInputFactory)
 * }}}
 *
 * A custom `XMLResolver` could be used to retrieve DTDs locally, or even to suppress DTD resolution.
 * The latter can be coded as follows (compare with http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds),
 * risking some loss of information:
 * {{{
 * class MyXmlResolver extends XMLResolver {
 *   override def resolveEntity(publicId: String, systemId: String, baseUri: String, namespace: String): Any = {
 *     // This dirty hack may not work on IBM JVMs
 *     new java.io.StringReader("")
 *   }
 * }
 * }}}
 *
 * A trivial `XMLReporter` could look like this:
 * {{{
 * class MyXmlReporter extends XMLReporter {
 *   override def report(message: String, errorType: String, relatedInformation: AnyRef, location: Location): Unit = {
 *     println("Location: %s. Error type: %s. Message: %s.".format(location, errorType, message))
 *   }
 * }
 * }}}
 *
 * If more flexibility is needed in configuring the `DocumentParser` than offered by this class, consider
 * writing a wrapper `DocumentParser` which wraps a `DocumentParserUsingStax`, but adapts the `parse` method.
 * This would make it possible to adapt the conversion from StAX events to yaidom `Document`, for example.
 *
 * A `DocumentParserUsingStax` instance can be re-used multiple times, from the same thread.
 * If the `XMLInputFactory` is thread-safe, it can even be re-used from multiple threads.
 * Typically a `XMLInputFactory` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `XMLInputFactory` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentParserUsingStax(
    val inputFactory: XMLInputFactory,
    val converterToDocument: ConverterToDocument[Iterator[XMLEvent]]) extends AbstractDocumentParser {

  /**
   * Returns an adapted copy having the passed ConverterToDocument. This method makes it possible to use an adapted
   * converter, which may be needed depending on the JAXP implementation used.
   */
  def withConverterToDocument(newConverterToDocument: ConverterToDocument[Iterator[XMLEvent]]): DocumentParserUsingStax = {
    new DocumentParserUsingStax(
      inputFactory,
      newConverterToDocument)
  }

  /** Parses the input stream into a yaidom `Document`. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    var xmlEventReader: XMLEventReader = null
    try {
      val streamSource = new StreamSource(inputStream)
      xmlEventReader = inputFactory.createXMLEventReader(streamSource)

      converterToDocument.convertToDocument(StaxConversions.asIterator(xmlEventReader))
    } finally {
      ignoring(classOf[Exception]) {
        if (xmlEventReader ne null) xmlEventReader.close() // scalastyle:off null
      }
      ignoring(classOf[Exception]) {
        if (inputStream ne null) inputStream.close() // scalastyle:off null
      }
    }
  }
}

object DocumentParserUsingStax {

  /** Returns a new instance, configured to coalesce CDATA sections. */
  def newInstance(): DocumentParserUsingStax = {
    // Although the factory method newFactory should be used instead of newInstance,
    // to stay out of "XML JAR-hell", the newInstance method was used.

    val xmlInputFactory = XMLInputFactory.newInstance
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    newInstance(xmlInputFactory)
  }

  /**
   * Returns a new instance, by invoking the primary constructor.
   * Do not turn off namespace awareness on the `XMLInputFactory` (by default, it is on).
   */
  def newInstance(inputFactory: XMLInputFactory): DocumentParserUsingStax =
    new DocumentParserUsingStax(inputFactory, StaxConversions)
}
