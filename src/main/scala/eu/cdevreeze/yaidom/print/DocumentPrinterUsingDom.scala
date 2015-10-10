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

package eu.cdevreeze.yaidom.print

import java.{ io => jio }

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.DocumentConverter
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import DocumentPrinterUsingDom.transformerCreatorForHtml
import DocumentPrinterUsingDom.DocumentProducer

/**
 * DOM-based `Document` printer.
 *
 * It may be the case that the `DocumentPrinter` does not indent. See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446.
 * This problem appears especially when creating a `Document` from scratch, using `NodeBuilder`s.
 * A possible (implementation-specific!) workaround is to create the `DocumentPrinter` as follows:
 * {{{
 * val documentBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
 *
 * val transformerFactory: TransformerFactory = TransformerFactory.newInstance
 * transformerFactory.setAttribute("indent-number", java.lang.Integer.valueOf(2))
 *
 * val documentPrinter = DocumentPrinterUsingDom.newInstance(documentBuilderFactory, transformerFactory)
 * }}}
 *
 * If more flexibility is needed in configuring the `DocumentPrinter` than offered by this class, consider
 * writing a wrapper `DocumentPrinter` which wraps a `DocumentPrinterUsingDom`, but adapts the `print` method.
 * This would make it possible to adapt the conversion from a yaidom `Document` to DOM `Document`, for example.
 *
 * A `DocumentPrinterUsingDom` instance can be re-used multiple times, from the same thread.
 * If the `DocumentBuilderFactory` and `TransformerFactory` are thread-safe, it can even be re-used from multiple threads.
 * Typically a `DocumentBuilderFactory` or `TransformerFactory` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `DocumentBuilderFactory` and `TransformerFactory` instance per request.
 *
 * ==Notes on HTML==
 *
 * In order to serialize a yaidom Document as HTML (which could have been parsed using the TagSoup library),
 * a "transformer creator" can be coded as follows:
 * {{{
 * val trCreator = { (tf: TransformerFactory) =>
 *   val t = tf.newTransformer
 *   t.setOutputProperty(OutputKeys.METHOD, "html")
 *   t
 * }
 * }}}
 * This "transformer creator" is then passed as one of the arguments during `DocumentPrinter` construction.
 *
 * Using such a `DocumentPrinter` configured for "HTML serialization", the serialized HTML is hopefully better understood
 * by browsers than would have been the case with a `DocumentPrinter` configured for "XML serialization".
 * For example, script tags (with empty content) are typically not written as self-closing tags in the first case,
 * whereas they are in the second case.
 *
 * If a `DocumentPrinter` is not explicitly configured for either "HTML or XML serialization", it may still be the case
 * that HTML is serialized as HTML, depending on the TrAX implementation. Yet it is likely that in that case other elements
 * than the document element would be serialized as XML instead of HTML. After all, (immutable) yaidom elements do not know about
 * their ancestry.
 *
 * @author Chris de Vreeze
 */
final class DocumentPrinterUsingDom(
  val docBuilderFactory: DocumentBuilderFactory,
  val docBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
  val transformerFactory: TransformerFactory,
  val transformerCreator: TransformerFactory => Transformer,
  val documentConverter: DocumentConverter[DocumentProducer]) extends AbstractDocumentPrinter {

  /**
   * Returns an adapted copy having the passed DocumentConverter. This method makes it possible to use an adapted
   * document converter, which may be needed depending on the JAXP implementation used.
   */
  def withDocumentConverter(newDocumentConverter: DocumentConverter[DocumentProducer]): DocumentPrinterUsingDom = {
    new DocumentPrinterUsingDom(
      docBuilderFactory,
      docBuilderCreator,
      transformerFactory,
      transformerCreator,
      newDocumentConverter)
  }

  def print(doc: Document, encoding: String, outputStream: jio.OutputStream): Unit = {
    val docBuilder = docBuilderCreator(docBuilderFactory)
    val domDocument: org.w3c.dom.Document = documentConverter.convertDocument(doc)(docBuilder.newDocument)

    val transformer = transformerCreator(transformerFactory)
    transformer.setOutputProperty(OutputKeys.ENCODING, encoding)

    // TODO TrAX, DOMSource and namespaces are a very troublesome combination. See for example http://java.net/jira/browse/JAXP-46.
    val domSource = new DOMSource(domDocument)

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val streamResult = new StreamResult(outputStream)

    try {
      transformer.transform(domSource, streamResult)
    } finally {
      outputStream.close()
    }
  }

  def print(doc: Document): String = {
    val docBuilder = docBuilderCreator(docBuilderFactory)
    val domDocument: org.w3c.dom.Document = documentConverter.convertDocument(doc)(docBuilder.newDocument)

    val transformer = transformerCreator(transformerFactory)

    // TODO TrAX, DOMSource and namespaces are a very troublesome combination. See for example http://java.net/jira/browse/JAXP-46.
    val domSource = new DOMSource(domDocument)

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val sw = new jio.StringWriter
    val streamResult = new StreamResult(sw)

    transformer.transform(domSource, streamResult)

    val result = sw.toString
    result
  }

  def omittingXmlDeclaration: DocumentPrinterUsingDom = {
    val newTransformerCreator = { tf: TransformerFactory =>
      val transformer = transformerCreator(tf)
      transformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
      transformer
    }

    new DocumentPrinterUsingDom(
      docBuilderFactory,
      docBuilderCreator,
      transformerFactory,
      newTransformerCreator,
      documentConverter)
  }

  def withDocumentBuilderCreator(newDocBuilderCreator: DocumentBuilderFactory => DocumentBuilder): DocumentPrinterUsingDom = {
    new DocumentPrinterUsingDom(
      docBuilderFactory,
      newDocBuilderCreator,
      transformerFactory,
      transformerCreator,
      documentConverter)
  }

  def withTransformerCreator(newTransformerCreator: TransformerFactory => Transformer): DocumentPrinterUsingDom = {
    new DocumentPrinterUsingDom(
      docBuilderFactory,
      docBuilderCreator,
      transformerFactory,
      newTransformerCreator,
      documentConverter)
  }

  def withTransformerCreatorForHtml: DocumentPrinterUsingDom = withTransformerCreator(transformerCreatorForHtml)
}

object DocumentPrinterUsingDom {

  /** Producer of a DOM `Document`, given the DOM `Document` as factory of DOM objects */
  type DocumentProducer = (org.w3c.dom.Document => org.w3c.dom.Document)

  /** Returns `newInstance(DocumentBuilderFactory.newInstance, TransformerFactory.newInstance)` */
  def newInstance(): DocumentPrinterUsingDom = {
    val docBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    val transformerFactory: TransformerFactory = TransformerFactory.newInstance
    newInstance(docBuilderFactory, transformerFactory)
  }

  /** Invokes the 4-arg `newInstance` method, with trivial "docBuilderCreator" and (indenting) "transformerCreator" */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    transformerFactory: TransformerFactory): DocumentPrinterUsingDom = {

    newInstance(
      docBuilderFactory,
      { dbf => dbf.newDocumentBuilder() },
      transformerFactory,
      { tf =>
        val t = tf.newTransformer()
        t.setOutputProperty(OutputKeys.INDENT, "yes")
        t
      })
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    docBuilderFactory: DocumentBuilderFactory,
    docBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
    transformerFactory: TransformerFactory,
    transformerCreator: TransformerFactory => Transformer): DocumentPrinterUsingDom = {

    new DocumentPrinterUsingDom(
      docBuilderFactory,
      docBuilderCreator,
      transformerFactory,
      transformerCreator,
      DomConversions)
  }

  val transformerCreatorForHtml: (TransformerFactory => Transformer) = { (tf: TransformerFactory) =>
    val t = tf.newTransformer
    t.setOutputProperty(OutputKeys.METHOD, "html")
    t
  }
}
