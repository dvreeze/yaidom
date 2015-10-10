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

import scala.collection.immutable

import eu.cdevreeze.yaidom.convert.StaxConversions
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.DocumentConverter
import javax.xml.stream.XMLEventFactory
import javax.xml.stream.XMLEventWriter
import javax.xml.stream.XMLOutputFactory
import javax.xml.stream.events.XMLEvent
import DocumentPrinterUsingStax.XmlEventsProducer

/**
 * StAX-based `Document` printer.
 *
 * Note: this XML printer does not pretty-print!
 *
 * If more flexibility is needed in configuring the `DocumentPrinter` than offered by this class, consider
 * writing a wrapper `DocumentPrinter` which wraps a `DocumentPrinterUsingStax`, but adapts the `print` method.
 * This would make it possible to adapt the conversion from a yaidom `Document` to StAX events, for example.
 *
 * A `DocumentPrinterUsingStax` instance can be re-used multiple times, from the same thread.
 * If the `XMLEventFactory` and `XMLOutputFactory` are thread-safe, it can even be re-used from multiple threads.
 * Typically these objects cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `XMLEventFactory` and `XMLOutputFactory` instance per request.
 *
 * @author Chris de Vreeze
 */
sealed class DocumentPrinterUsingStax(
  val eventFactory: XMLEventFactory,
  val outputFactory: XMLOutputFactory,
  val documentConverter: DocumentConverter[XmlEventsProducer]) extends AbstractDocumentPrinter {

  val omitXmlDeclaration: Boolean = false

  /**
   * Returns an adapted copy having the passed DocumentConverter. This method makes it possible to use an adapted
   * document converter, which may be needed depending on the JAXP implementation used.
   *
   * Depending on the StAX implementation used, it may be needed to use a specific document converter for a given
   * output encoding. See for example http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6452107.
   *
   * In that case, consider passing the following document converter, for encoding `enc`:
   * {{{
   * new YaidomToStaxEventsConversions {
   *   override def encoding: String = enc
   * }
   * }}}
   *
   * For a nice StAX tutorial, see http://docs.oracle.com/cd/E13222_01/wls/docs90/xml/stax.html.
   */
  def withDocumentConverter(newDocumentConverter: DocumentConverter[XmlEventsProducer]): DocumentPrinterUsingStax = {
    new DocumentPrinterUsingStax(
      eventFactory,
      outputFactory,
      newDocumentConverter)
  }

  def print(doc: Document, encoding: String, outputStream: jio.OutputStream): Unit = {
    val events: immutable.IndexedSeq[XMLEvent] = {
      val result = documentConverter.convertDocument(doc)(eventFactory)

      if (omitXmlDeclaration) result.filterNot(ev => ev.isStartDocument() || ev.isEndDocument)
      else result
    }

    var xmlEventWriter: XMLEventWriter = null

    try {
      xmlEventWriter = outputFactory.createXMLEventWriter(outputStream, encoding)
      for (ev <- events) {
        xmlEventWriter.add(ev)
      }
    } finally {
      if (xmlEventWriter ne null) xmlEventWriter.close()
      outputStream.close()
    }
  }

  def print(doc: Document): String = {
    val events: immutable.IndexedSeq[XMLEvent] = {
      val result = documentConverter.convertDocument(doc)(eventFactory)

      if (omitXmlDeclaration) result.filterNot(ev => ev.isStartDocument() || ev.isEndDocument)
      else result
    }

    val sw = new jio.StringWriter
    var xmlEventWriter: XMLEventWriter = null

    val xmlString =
      try {
        xmlEventWriter = outputFactory.createXMLEventWriter(sw)
        for (ev <- events) xmlEventWriter.add(ev)
        val result = sw.toString
        result
      } finally {
        if (xmlEventWriter ne null) xmlEventWriter.close()
      }

    xmlString
  }

  def omittingXmlDeclaration: DocumentPrinterUsingStax = {
    new DocumentPrinterUsingStax(eventFactory, outputFactory, documentConverter) {
      override val omitXmlDeclaration: Boolean = true
    }
  }
}

object DocumentPrinterUsingStax {

  /** Producer of an `IndexedSeq[XMLEvent]`, given a `XMLEventFactory` as factory of StAX events */
  type XmlEventsProducer = (XMLEventFactory => immutable.IndexedSeq[XMLEvent])

  /** Returns `newInstance(XMLEventFactory.newFactory, XMLOutputFactory.newFactory)` */
  def newInstance(): DocumentPrinterUsingStax = {
    // Although the factory methods newFactory should be used instead of newInstance,
    // to stay out of "XML JAR-hell", the newInstance methods were used.

    val eventFactory: XMLEventFactory = XMLEventFactory.newInstance
    val outputFactory: XMLOutputFactory = XMLOutputFactory.newInstance
    newInstance(eventFactory, outputFactory)
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    eventFactory: XMLEventFactory,
    outputFactory: XMLOutputFactory): DocumentPrinterUsingStax = {

    new DocumentPrinterUsingStax(eventFactory, outputFactory, StaxConversions)
  }
}
