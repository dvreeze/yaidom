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

import scala.util.control.Exception.ignoring

import org.w3c.dom.bootstrap.DOMImplementationRegistry
import org.w3c.dom.ls.DOMImplementationLS
import org.w3c.dom.ls.LSInput
import org.w3c.dom.ls.LSParser
import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.simple.ConverterToDocument
import eu.cdevreeze.yaidom.simple.Document

/**
 * DOM-LS-based `Document` parser.
 *
 * Typical non-trivial creation is as follows, assuming class `MyEntityResolver`, which extends `LSResourceResolver`,
 * and class `MyErrorHandler`, which extends `DOMErrorHandler`:
 * {{{
 * def createParser(domImplLS: DOMImplementationLS): LSParser = {
 *   val parser = domImplLS.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
 *   parser.getDomConfig.setParameter("resource-resolver", new MyEntityResolver)
 *   parser.getDomConfig.setParameter("error-handler", new MyErrorHandler)
 *   parser
 * }
 *
 * val domParser = DocumentParserUsingDomLS.newInstance().withParserCreator(createParser _)
 * }}}
 *
 * A custom `LSResourceResolver` could be used to retrieve DTDs locally, or even to suppress DTD resolution.
 * The latter can be coded as follows (see http://stuartsierra.com/2008/05/08/stop-your-java-sax-parser-from-downloading-dtds),
 * risking some loss of information:
 * {{{
 * class MyEntityResolver extends LSResourceResolver {
 *   override def resolveResource(tpe: String, namespaceURI: String, publicId: String, systemId: String, baseURI: String): LSInput = {
 *     val input = domImplLS.createLSInput()
 *     // This dirty hack may not work on IBM JVMs
 *     input.setCharacterStream(new jio.StringReader(""))
 *     input
 *   }
 * }
 * }}}
 *
 * For completeness, a custom `DOMErrorHandler` class that simply throws an exception:
 * {{{
 * class MyErrorHandler extends DOMErrorHandler {
 *   override def handleError(exc: DOMError): Boolean = {
 *     sys.error(exc.toString)
 *   }
 * }
 * }}}
 *
 * If more flexibility is needed in configuring the `DocumentParser` than offered by this class, consider
 * writing a wrapper `DocumentParser` which wraps a `DocumentParserUsingDomLS`, but adapts the `parse` method.
 *
 * A `DocumentParserUsingDomLS` instance can be re-used multiple times, from the same thread.
 * If the `DOMImplementationLS` is thread-safe, it can even be re-used from multiple threads.
 * Typically a `DOMImplementationLS` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `DOMImplementationLS` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentParserUsingDomLS(
    val domImplementation: DOMImplementationLS,
    val parserCreator: DOMImplementationLS => LSParser,
    val converterToDocument: ConverterToDocument[org.w3c.dom.Document]) extends AbstractDocumentParser {

  /**
   * Returns an adapted copy having the passed ConverterToDocument. This method makes it possible to use an adapted
   * converter, which may be needed depending on the JAXP implementation used.
   */
  def withConverterToDocument(newConverterToDocument: ConverterToDocument[org.w3c.dom.Document]): DocumentParserUsingDomLS = {
    new DocumentParserUsingDomLS(
      domImplementation,
      parserCreator,
      newConverterToDocument)
  }

  /** Parses the input source into a yaidom `Document`. Closes the input stream or reader afterwards. */
  def parse(inputSource: InputSource): Document = {
    try {
      val parser: LSParser = parserCreator(domImplementation)

      val input: LSInput = domImplementation.createLSInput

      if (Option(inputSource.getByteStream).nonEmpty) {
        input.setByteStream(inputSource.getByteStream)
      }

      if (Option(inputSource.getCharacterStream).nonEmpty) {
        input.setCharacterStream(inputSource.getCharacterStream)
      }

      if (Option(inputSource.getEncoding).nonEmpty) {
        input.setEncoding(inputSource.getEncoding)
      }

      if (Option(inputSource.getSystemId).nonEmpty) {
        input.setSystemId(inputSource.getSystemId)
      }

      if (Option(inputSource.getPublicId).nonEmpty) {
        input.setPublicId(inputSource.getPublicId)
      }

      val domDoc: org.w3c.dom.Document = parser.parse(input)

      converterToDocument.convertToDocument(domDoc)
    } finally {
      ignoring(classOf[Exception]) {
        Option(inputSource.getByteStream).foreach(bs => bs.close())
      }
      ignoring(classOf[Exception]) {
        Option(inputSource.getCharacterStream).foreach(cs => cs.close())
      }
    }
  }

  def withParserCreator(newParserCreator: DOMImplementationLS => LSParser): DocumentParserUsingDomLS = {
    new DocumentParserUsingDomLS(domImplementation, newParserCreator, converterToDocument)
  }
}

object DocumentParserUsingDomLS {

  /** Returns `newInstance(domImplLS)` for an appropriate `DOMImplementationLS` */
  def newInstance(): DocumentParserUsingDomLS = {
    val registry = DOMImplementationRegistry.newInstance
    val domImpl = registry.getDOMImplementation("LS 3.0")
    require(domImpl ne null, "Expected non-null DOM Implementation for feature 'LS 3.0'")
    require(domImpl.hasFeature("LS", "3.0"), "Expected DOM Implementation to have feature 'LS 3.0'")
    require(domImpl.isInstanceOf[DOMImplementationLS], "Expected DOM Implementation of type DOMImplementationLS")
    val domImplLS = domImpl.asInstanceOf[DOMImplementationLS]

    newInstance(domImplLS)
  }

  /**
   * Returns a new instance, using the given `DOMImplementationLS`, without any further configuration.
   */
  def newInstance(domImplementation: DOMImplementationLS): DocumentParserUsingDomLS = {
    val parserCreator = (domImpl: DOMImplementationLS) => domImpl.createLSParser(DOMImplementationLS.MODE_SYNCHRONOUS, null)
    newInstance(domImplementation, parserCreator)
  }

  /**
   * Returns a new instance, by invoking the primary constructor.
   */
  def newInstance(
    domImplementation: DOMImplementationLS,
    parserCreator: DOMImplementationLS => LSParser): DocumentParserUsingDomLS = {

    new DocumentParserUsingDomLS(domImplementation, parserCreator, DomConversions)
  }
}
