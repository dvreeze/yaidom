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
import convert.StaxConversions._

/**
 * StAX-based `Document` printer.
 *
 * Note: this XML printer does not pretty-print!
 *
 * A `DocumentPrinterUsingStax` instance can be re-used multiple times, from the same thread.
 * If the `XMLEventFactory` and `XMLOutputFactory` are thread-safe, it can even be re-used from multiple threads.
 *
 * @author Chris de Vreeze
 */
sealed class DocumentPrinterUsingStax(
  val eventFactory: XMLEventFactory,
  val outputFactory: XMLOutputFactory) extends DocumentPrinter {

  val omitXmlDeclaration: Boolean = false

  def print(doc: Document): String = {
    val events: immutable.IndexedSeq[XMLEvent] = convertDocument(doc)(eventFactory)

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

    if (firstLine.trim.startsWith("<?xml")) {
      xmlString.drop(firstLine.size).trim
    } else xmlString
  }
}

object DocumentPrinterUsingStax {

  /** Returns `newInstance(XMLEventFactory.newFactory, XMLOutputFactory.newFactory)` */
  def newInstance(): DocumentPrinterUsingStax = {
    val eventFactory: XMLEventFactory = XMLEventFactory.newFactory
    val outputFactory: XMLOutputFactory = XMLOutputFactory.newFactory
    newInstance(eventFactory, outputFactory)
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    eventFactory: XMLEventFactory,
    outputFactory: XMLOutputFactory): DocumentPrinterUsingStax = {

    new DocumentPrinterUsingStax(eventFactory, outputFactory)
  }
}
