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
import jinterop.StaxConversions._

/** StAX-based Document printer. Note: this XML printer does not pretty-print! */
final class DocumentPrinterUsingStax(
  val eventFactory: XMLEventFactory,
  val outputFactory: XMLOutputFactory) extends DocumentPrinter {

  def printXml(doc: Document): String = {
    val events: immutable.Seq[XMLEvent] = convertDocument(doc)(eventFactory)

    val sw = new jio.StringWriter
    var xmlEventWriter: XMLEventWriter = null
    try {
      xmlEventWriter = outputFactory.createXMLEventWriter(sw)
      for (ev <- events) xmlEventWriter.add(ev)
      val result = sw.toString
      result
    } finally {
      if (xmlEventWriter ne null) xmlEventWriter.close()
    }
  }
}

object DocumentPrinterUsingStax {

  def newInstance(): DocumentPrinterUsingStax = {
    val eventFactory: XMLEventFactory = XMLEventFactory.newFactory
    val outputFactory: XMLOutputFactory = XMLOutputFactory.newFactory
    new DocumentPrinterUsingStax(eventFactory, outputFactory)
  }
}
