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
package jinterop

import java.{ util => jutil }
import javax.xml.stream.XMLEventReader
import javax.xml.stream.events.XMLEvent
import scala.collection.immutable
import scala.collection.JavaConverters._

/**
 * Conversions between [[eu.cdevreeze.yaidom.Elem]]s and StAX events.
 *
 * Example usage for parsing an XML file into an [[eu.cdevreeze.yaidom.Elem]] using StAX:
 * {{{
 * import StaxConversions._
 *
 * val xmlInputFactory = XMLInputFactory.newFactory
 * val xmlEventReader = xmlInputFactory.createXMLEventReader(inputStream)
 * val root: Elem = convertToElem(xmlEventReader.toSeq)
 *
 * xmlEventReader.close()
 * }}}
 * Class [[eu.cdevreeze.yaidom.parse.DocumentParserUsingStax]] makes this a lot easier, though.
 *
 * A somewhat involved example for writing an [[eu.cdevreeze.yaidom.Elem]] to an XML file using StAX:
 * {{{
 * import StaxConversions._
 *
 * val xmlEventFactory = XMLEventFactory.newFactory
 * val events = convertElem(root)(xmlEventFactory)
 *
 * val xmlOutputFactory = XMLOutputFactory.newFactory
 * val xmlEventWriter = xmlOutputFactory.createXMLEventWriter(outputStream)
 * events.foreach(ev => xmlEventWriter.add(ev))
 *
 * xmlEventWriter.close()
 * }}}
 * Class [[eu.cdevreeze.yaidom.print.DocumentPrinterUsingStax]] makes this a lot easier, though.
 *
 * @author Chris de Vreeze
 */
object StaxConversions extends ElemToStaxEventsConverter with StaxEventsToElemConverter {

  /** "Implicit" class containing the toSeq method for XMLEventReaders. */
  final class ToXmlEventSeq(xmlEventReader: XMLEventReader) {
    def toSeq: immutable.Seq[XMLEvent] = {
      xmlEventReader.asInstanceOf[jutil.Iterator[XMLEvent]].asScala.toIndexedSeq
    }
  }

  /**
   * Adds a toSeq method that implicitly converts an <code>XMLEventReader</code> to a <code>immutable.Seq[XMLEvent]</code>.
   */
  implicit def toXmlEventSeq(xmlEventReader: XMLEventReader): ToXmlEventSeq = new ToXmlEventSeq(xmlEventReader)
}
