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
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import org.w3c.dom.Element
import jinterop.DomConversions._

/**
 * DOM-based `Document` parser.
 *
 * Typical non-trivial creation is as follows, assuming class `MyEntityResolver`, which extends `EntityResolver`,
 * and class `MyErrorHandler`, which extends `ErrorHandler`:
 * {{{
 * val dbf = DocumentBuilderFactory.newInstance()
 *
 * def createDocumentBuilder(dbf: DocumentBuilderFactory): DocumentBuilder = {
 *   val db = dbf.newDocumentBuilder()
 *   db.setEntityResolver(new MyEntityResolver)
 *   db.setErrorHandler(new MyErrorHandler)
 *   db
 * }
 *
 * val docParser = DocumentParserUsingDom.newInstance(dbf, createDocumentBuilder _)
 * }}}
 *
 * If we want the `DocumentBuilderFactory` to be a validating one, using an XML Schema, we could obtain the `DocumentBuilderFactory` as follows:
 * {{{
 * val schemaFactory = SchemaFactory.newInstance(XMLConstants.W3C_XML_SCHEMA_NS_URI)
 * val schemaSource = new StreamSource(new File(pathToSchema))
 * val schema = schemaFactory.newSchema(schemaSource)
 *
 * val dbf = {
 *   val result = DocumentBuilderFactory.newInstance()
 *   result.setNamespaceAware(true)
 *   result.setSchema(schema)
 *   result
 * }
 * }}}
 *
 * A custom `EntityResolver` could be used to retrieve DTDs locally, or even to suppress DTD resolution.
 * The latter can be coded as follows (see http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds):
 * {{{
 * class MyEntityResolver extends EntityResolver {
 *   override def resolveEntity(publicId: String, systemId: String): InputSource = {
 *     new InputSource(new java.io.StringReader(""))
 *   }
 * }
 * }}}
 *
 * For completeness, a custom `ErrorHandler` class that simply prints parse exceptions to standard output:
 * {{{
 * class MyErrorHandler extends ErrorHandler {
 *   def warning(exc: SAXParseException) { println(exc) }
 *   def error(exc: SAXParseException) { println(exc) }
 *   def fatalError(exc: SAXParseException) { println(exc) }
 * }
 * }}}
 *
 * A `DocumentParserUsingDom` instance can be re-used multiple times, from the same thread.
 * If the `DocumentBuilderFactory` is thread-safe, it can even be re-used from multiple threads.
 */
final class DocumentParserUsingDom(
  val docBuilderFactory: DocumentBuilderFactory,
  val docBuilderCreator: DocumentBuilderFactory => DocumentBuilder) extends DocumentParser {

  /** Parses the input stream into a yaidom `Document`. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    try {
      val db: DocumentBuilder = docBuilderCreator(docBuilderFactory)
      val domDoc: org.w3c.dom.Document = db.parse(inputStream)

      convertToDocument(domDoc)
    } finally {
      if (inputStream ne null) inputStream.close()
    }
  }
}

object DocumentParserUsingDom {

  /** Returns `newInstance(DocumentBuilderFactory.newInstance)` */
  def newInstance(): DocumentParserUsingDom = {
    val dbf = DocumentBuilderFactory.newInstance()
    newInstance(dbf)
  }

  /** Returns a new instance, using the given `DocumentBuilderFactory`, without any further configuration */
  def newInstance(docBuilderFactory: DocumentBuilderFactory): DocumentParserUsingDom = {
    val dbc = (dbf: DocumentBuilderFactory) => dbf.newDocumentBuilder()
    newInstance(docBuilderFactory, dbc)
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    docBuilderCreator: DocumentBuilderFactory => DocumentBuilder): DocumentParserUsingDom = {

    new DocumentParserUsingDom(docBuilderFactory, docBuilderCreator)
  }
}
