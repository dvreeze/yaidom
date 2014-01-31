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
package integrationtest

import java.{ util => jutil, io => jio }
import javax.xml.stream.{ XMLInputFactory, XMLOutputFactory, XMLEventFactory, XMLEventReader }
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.{ StreamSource, StreamResult }
import scala.collection.{ immutable, mutable }
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore, ConfigMap }
import org.scalatest.junit.JUnitRunner
import parse._
import print._

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

    val outputFactory = XMLOutputFactory.newInstance

    val docParser = parse.DocumentParserUsingSax.newInstance

    var contactCount = 0
    var elemCount = 0

    collectUntilNextIsContactOrEnd(xmlEventReader)

    while (!stop(xmlEventReader)) {
      val events =
        trimContactEvents((xmlEventReader.nextTag().asStartElement) +: collectUntilNextIsContactOrEnd(xmlEventReader))

      val bos = new jio.ByteArrayOutputStream()
      val xmlEventWriter = outputFactory.createXMLEventWriter(bos, "UTF-8")
      events foreach (ev => xmlEventWriter.add(ev))
      xmlEventWriter.flush() // Needed on IBM JDK
      val contactBytes = bos.toByteArray

      val contactElem = docParser.parse(new jio.ByteArrayInputStream(contactBytes)).documentElement

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

  private def stop(xmlEventReader: XMLEventReader): Boolean =
    (!xmlEventReader.hasNext) || (!isStartContact(xmlEventReader.peek()))

  private def collectUntilNextIsContactOrEnd(xmlEventReader: XMLEventReader): Vector[XMLEvent] = {
    var result = mutable.ArrayBuffer[XMLEvent]()

    while (xmlEventReader.hasNext() && !isStartContact(xmlEventReader.peek())) {
      result += xmlEventReader.nextEvent()
    }

    // ArrayBuffer.toVector not working on Scala 2.9.X ...
    Vector(result.toIndexedSeq: _*)
  }

  private def trimContactEvents(events: Vector[XMLEvent]): Vector[XMLEvent] = {
    events.dropWhile(ev => !isStartContact(ev)).reverse.dropWhile(ev => !isEndContact(ev)).reverse
  }
}
