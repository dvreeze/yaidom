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
package print

import java.{ util => jutil, io => jio }
import org.w3c.dom.ls.{ DOMImplementationLS, LSSerializer, LSOutput }
import org.w3c.dom.bootstrap.DOMImplementationRegistry
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import scala.collection.immutable
import convert.DomConversions._
import DocumentPrinterUsingDomLS._

/**
 * DOM-LS-based `Document` printer.
 *
 * To pretty-print a `Document`, create a `DocumentPrinterUsingDomLS` instance as follows:
 * {{{
 * val printer = DocumentPrinterUsingDomLS.newInstance() withSerializerCreator { domImpl =>
 *   val writer = domImpl.createLSSerializer()
 *   writer.getDomConfig.setParameter("format-pretty-print", java.lang.Boolean.TRUE)
 *   writer
 * }
 * }}}
 *
 * If more flexibility is needed in configuring the `DocumentPrinter` than offered by this class, consider
 * writing a wrapper `DocumentPrinter` which wraps a `DocumentPrinterUsingDomLS`, but adapts the `print` method.
 * This would make it possible to adapt the serialization, for example.
 *
 * A `DocumentPrinterUsingDomLS` instance can be re-used multiple times, from the same thread.
 * If the `DocumentBuilderFactory` and `DOMImplementationLS` are thread-safe, it can even be re-used from multiple threads.
 * Typically a `DocumentBuilderFactory` or `DOMImplementationLS` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `DocumentBuilderFactory` and `DOMImplementationLS` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentPrinterUsingDomLS(
  val docBuilderFactory: DocumentBuilderFactory,
  val docBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
  val domImplementation: DOMImplementationLS,
  val serializerCreator: DOMImplementationLS => LSSerializer) extends DocumentPrinter { self =>

  def print(doc: Document, encoding: String): Array[Byte] = {
    val docBuilder = docBuilderCreator(docBuilderFactory)
    val domDocument: org.w3c.dom.Document = convertDocument(doc)(docBuilder.newDocument)

    val serializer: LSSerializer = serializerCreator(domImplementation)

    val output: LSOutput = domImplementation.createLSOutput
    val bos = new jio.ByteArrayOutputStream
    output.setEncoding(encoding)
    output.setByteStream(bos)

    val ok = serializer.write(domDocument, output)
    require(ok, "Expected successful serialization of Document %s".format(doc.documentElement.toString))

    val result = bos.toByteArray
    result
  }

  def print(doc: Document): String = {
    val docBuilder = docBuilderCreator(docBuilderFactory)
    val domDocument: org.w3c.dom.Document = convertDocument(doc)(docBuilder.newDocument)

    val serializer: LSSerializer = serializerCreator(domImplementation)

    val output: LSOutput = domImplementation.createLSOutput
    val sw = new jio.StringWriter
    output.setEncoding("utf-8")
    output.setCharacterStream(sw)

    val ok = serializer.write(domDocument, output)
    require(ok, "Expected successful serialization of Document %s".format(doc.documentElement.toString))

    val result = sw.toString
    result
  }

  def omittingXmlDeclaration: DocumentPrinterUsingDomLS = {
    val newSerializerCreator = { domImpl: DOMImplementationLS =>
      val serializer = self.serializerCreator(domImpl)
      val domConfig = serializer.getDomConfig
      domConfig.setParameter("xml-declaration", java.lang.Boolean.FALSE)
      serializer
    }

    withSerializerCreator(newSerializerCreator)
  }

  def withSerializerCreator(newSerializerCreator: DOMImplementationLS => LSSerializer): DocumentPrinterUsingDomLS = {
    new DocumentPrinterUsingDomLS(
      docBuilderFactory,
      docBuilderCreator,
      domImplementation,
      newSerializerCreator)
  }
}

object DocumentPrinterUsingDomLS {

  /** Returns `newInstance(DocumentBuilderFactory.newInstance, domImplLS)`, for an appropriate `DOMImplementationLS` */
  def newInstance(): DocumentPrinterUsingDomLS = {
    val registry = DOMImplementationRegistry.newInstance
    val domImpl = registry.getDOMImplementation("LS 3.0")
    require(domImpl ne null, "Expected non-null DOM Implementation for feature 'LS 3.0'")
    require(domImpl.hasFeature("LS", "3.0"), "Expected DOM Implementation to have feature 'LS 3.0'")
    require(domImpl.isInstanceOf[DOMImplementationLS], "Expected DOM Implementation of type DOMImplementationLS")
    val domImplLS = domImpl.asInstanceOf[DOMImplementationLS]

    val docBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    newInstance(docBuilderFactory, domImplLS)
  }

  /** Invokes the 4-arg `newInstance` method, with trivial "docBuilderCreator" and "serializerCreator" */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    domImplementation: DOMImplementationLS): DocumentPrinterUsingDomLS = {

    newInstance(
      docBuilderFactory,
      { dbf => dbf.newDocumentBuilder() },
      domImplementation,
      { domImpl => domImpl.createLSSerializer() })
  }

  /** Returns a new instance, by invoking the primary constructor, with output encoding UTF-8 */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    docBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
    domImplementation: DOMImplementationLS,
    serializerCreator: DOMImplementationLS => LSSerializer): DocumentPrinterUsingDomLS = {

    new DocumentPrinterUsingDomLS(docBuilderFactory, docBuilderCreator, domImplementation, serializerCreator)
  }
}
