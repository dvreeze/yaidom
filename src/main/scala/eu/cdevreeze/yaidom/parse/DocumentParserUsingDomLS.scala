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
import org.w3c.dom.Element
import org.w3c.dom.ls.{ DOMImplementationLS, LSParser, LSInput }
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import eu.cdevreeze.yaidom.convert.DomConversions

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
 * This would make it possible to set an encoding on the `LSInput`, for example. As another example, this would
 * allow for adapting the conversion from a DOM `Document` to yaidom `Document`.
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
  val parserCreator: DOMImplementationLS => LSParser) extends AbstractDocumentParser {

  /** Parses the input stream into a yaidom `Document`. Closes the input stream afterwards. */
  def parse(inputStream: jio.InputStream): Document = {
    try {
      val parser: LSParser = parserCreator(domImplementation)

      val input: LSInput = domImplementation.createLSInput
      input.setByteStream(inputStream)

      val domDoc: org.w3c.dom.Document = parser.parse(input)

      val domConversions = new DomConversions(ENameProvider.newSimpleCachingInstance, QNameProvider.newSimpleCachingInstance)
      domConversions.convertToDocument(domDoc)
    } finally {
      if (inputStream ne null) inputStream.close()
    }
  }

  def withParserCreator(newParserCreator: DOMImplementationLS => LSParser): DocumentParserUsingDomLS = {
    new DocumentParserUsingDomLS(domImplementation, newParserCreator)
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

    new DocumentParserUsingDomLS(domImplementation, parserCreator)
  }
}
