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
import javax.xml.stream._
import javax.xml.stream.events.XMLEvent
import scala.collection.immutable
import convert.YaidomToStaxEventsConversions

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
  val outputFactory: XMLOutputFactory) extends AbstractDocumentPrinter {

  val omitXmlDeclaration: Boolean = false

  def print(doc: Document, encoding: String, outputStream: jio.OutputStream): Unit = {
    // The following conversion (causing a CreateStartDocument event with encoding) is needed to hopefully prevent the following exception:
    // javax.xml.stream.XMLStreamException: Underlying stream encoding 'ISO8859_1' and input parameter for writeStartDocument() method 'UTF-8' do not match.
    // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6452107.
    // Alas, it still does not work, leading to an empty byte array below.

    // See http://docs.oracle.com/cd/E13222_01/wls/docs90/xml/stax.html#1105345 for a nice StAX tutorial.

    val enc = encoding
    val converterToStax = new YaidomToStaxEventsConversions {
      override def encoding: String = enc
    }
    val events: immutable.IndexedSeq[XMLEvent] = {
      val result = converterToStax.convertDocument(doc)(eventFactory)

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
    val converterToStax = new YaidomToStaxEventsConversions {}
    val events: immutable.IndexedSeq[XMLEvent] = {
      val result = converterToStax.convertDocument(doc)(eventFactory)

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
    new DocumentPrinterUsingStax(eventFactory, outputFactory) {
      override val omitXmlDeclaration: Boolean = true
    }
  }
}

object DocumentPrinterUsingStax {

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

    new DocumentPrinterUsingStax(eventFactory, outputFactory)
  }
}
