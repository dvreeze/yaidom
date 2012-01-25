package eu.cdevreeze.yaidom
package print

import java.{ util => jutil, io => jio }
import javax.xml.stream._
import javax.xml.stream.events.XMLEvent
import scala.collection.immutable
import jinterop.StaxConversions._

/** StAX-based Document printer */
final class DocumentPrinterUsingStax(
  val eventFactory: XMLEventFactory,
  val outputFactory: XMLOutputFactory) {

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
