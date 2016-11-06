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

package eu.cdevreeze.yaidom.print

import java.{ io => jio }

import org.xml.sax.ContentHandler

import eu.cdevreeze.yaidom.convert.YaidomToSaxEventsConversions
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.DocumentConverter
import javax.xml.transform.OutputKeys
import javax.xml.transform.TransformerFactory
import javax.xml.transform.sax.SAXTransformerFactory
import javax.xml.transform.sax.TransformerHandler
import javax.xml.transform.stream.StreamResult
import DocumentPrinterUsingSax.SaxEventsProducer

/**
 * SAX-based `Document` printer. It should be the fastest of the `DocumentPrinter` implementations, and use the least memory.
 *
 * See http://blogger.ziesemer.com/2007/06/xml-generation-in-java.html#SAXMethod.
 *
 * If more flexibility is needed in configuring the `DocumentPrinter` than offered by this class, consider
 * writing a wrapper `DocumentPrinter` which wraps a `DocumentPrinterUsingSax`, but adapts the `print` method.
 * This would make it possible to adapt the generation of SAX events, for example.
 *
 * A `DocumentPrinterUsingSax` instance can be re-used multiple times, from the same thread.
 * If the `SAXTransformerFactory` is thread-safe, it can even be re-used from multiple threads.
 * Typically a `SAXTransformerFactory` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `SAXTransformerFactory` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentPrinterUsingSax(
  val saxTransformerFactory: SAXTransformerFactory,
  val transformerHandlerCreator: SAXTransformerFactory => TransformerHandler,
  val documentConverter: DocumentConverter[SaxEventsProducer]) extends AbstractDocumentPrinter {

  /**
   * Returns an adapted copy having the passed DocumentConverter. This method makes it possible to use an adapted
   * document converter, which may be needed depending on the JAXP implementation used.
   */
  def withDocumentConverter(newDocumentConverter: DocumentConverter[SaxEventsProducer]): DocumentPrinterUsingSax = {
    new DocumentPrinterUsingSax(
      saxTransformerFactory,
      transformerHandlerCreator,
      newDocumentConverter)
  }

  def print(doc: Document, encoding: String, outputStream: jio.OutputStream): Unit = {
    val handler = transformerHandlerCreator(saxTransformerFactory)

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val streamResult = new StreamResult(outputStream)

    try {
      handler.getTransformer.setOutputProperty(OutputKeys.ENCODING, encoding)
      handler.setResult(streamResult)
      generateEventsForDocument(doc, handler)
    } finally {
      outputStream.close()
    }
  }

  def print(doc: Document): String = {
    val handler = transformerHandlerCreator(saxTransformerFactory)

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val sw = new jio.StringWriter
    val streamResult = new StreamResult(sw)

    handler.setResult(streamResult)
    generateEventsForDocument(doc, handler)

    val result = sw.toString
    result
  }

  def omittingXmlDeclaration: DocumentPrinterUsingSax = {
    val newTransformerHandlerCreator = { tf: SAXTransformerFactory =>
      val transformerHandler = transformerHandlerCreator(tf)
      transformerHandler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
      transformerHandler
    }

    new DocumentPrinterUsingSax(
      saxTransformerFactory,
      newTransformerHandlerCreator,
      documentConverter)
  }

  private def generateEventsForDocument(doc: Document, handler: TransformerHandler): Unit = {
    documentConverter.convertDocument(doc)(handler)
  }
}

object DocumentPrinterUsingSax {

  /** Producer of SAX events, given a `ContentHandler` on which the SAX event handlers are invoked */
  type SaxEventsProducer = (ContentHandler => Unit)

  /** Returns `newInstance(TransformerFactory.newInstance().asInstanceOf[SAXTransformerFactory])` */
  def newInstance(): DocumentPrinterUsingSax = {
    val tf = TransformerFactory.newInstance()

    assert(
      tf.getFeature(SAXTransformerFactory.FEATURE),
      s"The TransformerFactory ${tf.getClass} is not a SAXTransformerFactory")
    val stf = tf.asInstanceOf[SAXTransformerFactory]

    newInstance(stf)
  }

  /** Invokes the 2-arg `newInstance` method, with trivial "transformerHandlerCreator" */
  def newInstance(saxTransformerFactory: SAXTransformerFactory): DocumentPrinterUsingSax =
    newInstance(
      saxTransformerFactory,
      { tf => tf.newTransformerHandler() })

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    saxTransformerFactory: SAXTransformerFactory,
    transformerHandlerCreator: SAXTransformerFactory => TransformerHandler): DocumentPrinterUsingSax = {

    new DocumentPrinterUsingSax(
      saxTransformerFactory,
      transformerHandlerCreator,
      new YaidomToSaxEventsConversions {})
  }
}
