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

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.simple.ConverterToDocument
import eu.cdevreeze.yaidom.simple.Document
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * DOM-based `Document` parser.
 *
 * Typical non-trivial creation is as follows, assuming class `MyEntityResolver`, which extends `EntityResolver`,
 * and class `MyErrorHandler`, which extends `ErrorHandler`:
 * {{{
 * val dbf = DocumentBuilderFactory.newInstance()
 * dbf.setNamespaceAware(true)
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
 * The latter can be coded as follows (see http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds),
 * risking some loss of information:
 * {{{
 * class MyEntityResolver extends EntityResolver {
 *   override def resolveEntity(publicId: String, systemId: String): InputSource = {
 *     // This dirty hack may not work on IBM JVMs
 *     new InputSource(new java.io.StringReader(""))
 *   }
 * }
 * }}}
 *
 * For completeness, a custom `ErrorHandler` class that simply prints parse exceptions to standard output:
 * {{{
 * class MyErrorHandler extends ErrorHandler {
 *   def warning(exc: SAXParseException): Unit = { println(exc) }
 *   def error(exc: SAXParseException): Unit = { println(exc) }
 *   def fatalError(exc: SAXParseException): Unit = { println(exc) }
 * }
 * }}}
 *
 * If more flexibility is needed in configuring the `DocumentParser` than offered by this class, consider
 * writing a wrapper `DocumentParser` which wraps a `DocumentParserUsingDom`, but adapts the `parse` method.
 * This would make it possible to adapt the conversion from a DOM `Document` to yaidom `Document`, for example.
 *
 * A `DocumentParserUsingDom` instance can be re-used multiple times, from the same thread.
 * If the `DocumentBuilderFactory` is thread-safe, it can even be re-used from multiple threads.
 * Typically a `DocumentBuilderFactory` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `DocumentBuilderFactory` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentParserUsingDom(
  val docBuilderFactory: DocumentBuilderFactory,
  val docBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
  val converterToDocument: ConverterToDocument[org.w3c.dom.Document]) extends AbstractDocumentParser {

  /**
   * Returns an adapted copy having the passed ConverterToDocument. This method makes it possible to use an adapted
   * converter, which may be needed depending on the JAXP implementation used.
   */
  def withConverterToDocument(newConverterToDocument: ConverterToDocument[org.w3c.dom.Document]): DocumentParserUsingDom = {
    new DocumentParserUsingDom(
      docBuilderFactory,
      docBuilderCreator,
      newConverterToDocument)
  }

  /** Parses the input stream into a yaidom `Document`. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    try {
      val db: DocumentBuilder = docBuilderCreator(docBuilderFactory)

      // See http://docs.oracle.com/cd/E13222_01/wls/docs90/xml/best.html
      val inputSource = new InputSource(inputStream)
      val domDoc: org.w3c.dom.Document = db.parse(inputSource)

      converterToDocument.convertToDocument(domDoc)
    } finally {
      if (inputStream ne null) inputStream.close()
    }
  }
}

object DocumentParserUsingDom {

  /** Returns `newInstance(DocumentBuilderFactory.newInstance)`, except that namespace awareness is set to true */
  def newInstance(): DocumentParserUsingDom = {
    val dbf = DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    newInstance(dbf)
  }

  /**
   * Returns a new instance, using the given `DocumentBuilderFactory`, without any further configuration.
   * Do not forget to set namespace awareness to true on the `DocumentBuilderFactory`!
   */
  def newInstance(docBuilderFactory: DocumentBuilderFactory): DocumentParserUsingDom = {
    val dbc = (dbf: DocumentBuilderFactory) => dbf.newDocumentBuilder()
    newInstance(docBuilderFactory, dbc)
  }

  /**
   * Returns a new instance, by invoking the primary constructor
   * Do not forget to set namespace awareness to true on the `DocumentBuilderFactory`!
   */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    docBuilderCreator: DocumentBuilderFactory => DocumentBuilder): DocumentParserUsingDom = {

    new DocumentParserUsingDom(docBuilderFactory, docBuilderCreator, DomConversions)
  }
}
