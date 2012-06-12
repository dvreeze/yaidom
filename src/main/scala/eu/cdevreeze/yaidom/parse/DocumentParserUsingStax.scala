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

import java.{ io => jio, util => jutil }
import javax.xml.stream.{ XMLInputFactory, XMLEventReader }
import javax.xml.stream.events.XMLEvent
import scala.collection.immutable
import scala.collection.JavaConverters._
import scala.util.control.Exception._
import convert.StaxConversions._

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
 *   override def resolveEntity(publicId: String, systemId: String, baseUri: String, namespace: String): InputSource = {
 *     new InputSource(new java.io.StringReader(""))
 *   }
 * }
 * }}}
 *
 * A trivial `XMLReporter` could look like this:
 * {{{
 * class MyXmlReporter extends XMLReporter {
 *   override def report(message: String, errorType: String, relatedInformation: AnyRef, location: Location) {
 *     println("Location: %s. Error type: %s. Message: %s.".format(location, errorType, message))
 *   }
 * }
 * }}}
 *
 * A `DocumentParserUsingStax` instance can be re-used multiple times, from the same thread.
 * If the `XMLInputFactory` is thread-safe, it can even be re-used from multiple threads.
 */
final class DocumentParserUsingStax(val inputFactory: XMLInputFactory) extends DocumentParser {

  /** Parses the input stream into a yaidom `Document`. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    var xmlEventReader: XMLEventReader = null
    try {
      xmlEventReader = inputFactory.createXMLEventReader(inputStream)
      convertToDocument(toIndexedSeq(xmlEventReader))
    } finally {
      ignoring(classOf[Exception]) {
        if (xmlEventReader ne null) xmlEventReader.close()
      }
      ignoring(classOf[Exception]) {
        if (inputStream ne null) inputStream.close()
      }
    }
  }

  private def toIndexedSeq(xmlEventReader: XMLEventReader): immutable.IndexedSeq[XMLEvent] = {
    xmlEventReader.asInstanceOf[jutil.Iterator[XMLEvent]].asScala.toIndexedSeq
  }
}

object DocumentParserUsingStax {

  /** Returns a new instance, configured to coalesce whitespace */
  def newInstance(): DocumentParserUsingStax = {
    val xmlInputFactory = XMLInputFactory.newFactory
    xmlInputFactory.setProperty(XMLInputFactory.IS_COALESCING, java.lang.Boolean.TRUE)
    newInstance(xmlInputFactory)
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(inputFactory: XMLInputFactory): DocumentParserUsingStax = new DocumentParserUsingStax(inputFactory)
}
