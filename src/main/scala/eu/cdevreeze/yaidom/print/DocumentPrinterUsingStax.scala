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
  val outputFactory: XMLOutputFactory) extends DocumentPrinter {

  val omitXmlDeclaration: Boolean = false

  def print(doc: Document, encoding: String): Array[Byte] = {
    // The following conversion (causing a CreateStartDocument event with encoding) is need to hopefully prevent the following exception:
    // javax.xml.stream.XMLStreamException: Underlying stream encoding 'ISO8859_1' and input parameter for writeStartDocument() method 'UTF-8' do not match.
    // See http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6452107.
    // Alas, it still does not work, leading to an empty byte array below.

    val enc = encoding
    val converterToStax = new YaidomToStaxEventsConversions {
      override def encoding: String = enc
    }
    val events: immutable.IndexedSeq[XMLEvent] = converterToStax.convertDocument(doc)(eventFactory)

    val bos = new jio.ByteArrayOutputStream
    var xmlEventWriter: XMLEventWriter = null

    val bytes =
      try {
        xmlEventWriter = outputFactory.createXMLEventWriter(bos, encoding)
        for (ev <- events) {
          xmlEventWriter.add(ev)
        }
        val result = bos.toByteArray
        result
      } finally {
        if (xmlEventWriter ne null) xmlEventWriter.close()
      }

    // Inefficient, of course
    val xmlString = new String(bytes, encoding)
    val editedString = if (omitXmlDeclaration) removeXmlDeclaration(xmlString) else xmlString
    editedString.getBytes(encoding)
  }

  def print(doc: Document): String = {
    val converterToStax = new YaidomToStaxEventsConversions {}
    val events: immutable.IndexedSeq[XMLEvent] = converterToStax.convertDocument(doc)(eventFactory)

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

    if (omitXmlDeclaration) removeXmlDeclaration(xmlString) else xmlString
  }

  def omittingXmlDeclaration: DocumentPrinterUsingStax = {
    new DocumentPrinterUsingStax(eventFactory, outputFactory) {
      override val omitXmlDeclaration: Boolean = true
    }
  }

  /** Low tech solution for removing the XML declaration, if any */
  private def removeXmlDeclaration(xmlString: String): String = {
    val linesIterator = xmlString.linesWithSeparators
    require(linesIterator.hasNext, "Expected at least one line")
    val firstLine = linesIterator.next()

    if (firstLine.trim.startsWith("<?xml") && firstLine.trim.contains("?>")) {
      xmlString.dropWhile(c => c != '>').drop(1).trim
    } else xmlString
  }
}

object DocumentPrinterUsingStax {

  /** Returns `newInstance(XMLEventFactory.newFactory, XMLOutputFactory.newFactory)` */
  def newInstance(): DocumentPrinterUsingStax = {
    // Although the factory methods newFactory should be used instead of newInstance,
    // to stay out of "XML JAR-hell" (especially with Java 5, which does not ship with StAX), the newInstance methods
    // were used.

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
