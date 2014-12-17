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

package eu.cdevreeze.yaidom.integrationtest

import java.{ io => jio }
import java.{ util => jutil }

import scala.collection.JavaConverters.asScalaIteratorConverter
import scala.collection.mutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.StaxConversions.convertToElem
import eu.cdevreeze.yaidom.convert.StaxConversions.convertToEventStateIterator
import eu.cdevreeze.yaidom.convert.StaxEventsToYaidomConversions.EventState
import eu.cdevreeze.yaidom.core.Scope
import javax.xml.stream.XMLEventReader
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.stream.StreamSource

/**
 * Large XML test case, using streaming, thus keeping the memory footprint low.
 *
 * Acknowledgments: The large XML files come from http://javakata6425.appspot.com/#!goToPageIIIarticleIIIOptimally%20parse%20humongous%20XML%20files%20with%20vanilla%20Java.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class StreamingLargeXmlTest extends Suite with BeforeAndAfterAll {

  @volatile private var xmlBytes: Array[Byte] = _

  override def beforeAll(configMap: ConfigMap): Unit = {
    val zipFileUrl = classOf[StreamingLargeXmlTest].getResource("bigFile.zip")
    val zipFile = new jutil.zip.ZipFile(new jio.File(zipFileUrl.toURI))

    val zipEntries = zipFile.entries()
    require(zipEntries.hasMoreElements())

    val zipEntry: jutil.zip.ZipEntry = zipEntries.nextElement()

    val is = new jio.BufferedInputStream(zipFile.getInputStream(zipEntry))

    val bos = new jio.ByteArrayOutputStream
    var b: Int = -1
    while ({ b = is.read(); b >= 0 }) {
      bos.write(b)
    }
    is.close()

    this.xmlBytes = bos.toByteArray
  }

  /**
   * Test showing how StAX can help process very large XML inputs in many situations.
   * It is neither elegant nor fast code, but chunks of the input XML are processed by yaidom.
   *
   * This test example is simple, and does not use any namespaces.
   */
  @Test def testProcessLargeXmlUsingStreaming(): Unit = {
    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new jio.ByteArrayInputStream(this.xmlBytes))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    val eventStateIterator =
      convertToEventStateIterator(asIterator(xmlEventReader)).buffered

    var contactCount = 0
    var elemCount = 0

    collectUntilNextIsContactOrEnd(eventStateIterator)

    while (!stop(eventStateIterator)) {
      val eventStates =
        trimContactEvents((eventStateIterator.next()) +: collectUntilNextIsContactOrEnd(eventStateIterator))

      val contactElem =
        convertToElem(eventStates.map(_.event), eventStates.head.state.parentScope)

      assert(contactElem.localName == "contact")
      contactCount += 1
      elemCount += contactElem.findAllElemsOrSelf.size

      assertResult(true) {
        Set("firstName", "lastName").subsetOf(contactElem.findAllElems.map(_.localName).toSet)
      }
    }

    assertResult(true) {
      contactCount >= 1000
    }
    assertResult(true) {
      elemCount >= 10000
    }
  }

  private def isStartContact(xmlEvent: XMLEvent): Boolean = {
    xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "contact"
  }

  private def isEndContact(xmlEvent: XMLEvent): Boolean = {
    xmlEvent.isEndElement() && xmlEvent.asEndElement().getName.getLocalPart == "contact"
  }

  private def stop(it: BufferedIterator[EventState]): Boolean =
    (!it.hasNext) || (!isStartContact(it.head.event))

  private def collectUntilNextIsContactOrEnd(it: BufferedIterator[EventState]): Vector[EventState] = {
    var result = mutable.ArrayBuffer[EventState]()

    while (it.hasNext && !isStartContact(it.head.event)) {
      result += it.next()
    }

    // Did not work on Scala 2.9.X ...
    result.toVector
  }

  private def trimContactEvents(eventStates: Vector[EventState]): Vector[EventState] = {
    eventStates.dropWhile(ev => !isStartContact(ev.event)).reverse.dropWhile(ev => !isEndContact(ev.event)).reverse
  }

  private def asIterator(xmlEventReader: XMLEventReader): BufferedIterator[XMLEvent] = {
    val it = xmlEventReader.asInstanceOf[jutil.Iterator[XMLEvent]]
    it.asScala.buffered
  }
}
