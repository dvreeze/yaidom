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
import java.io.File
import java.io.FileInputStream
import java.{ util => jutil }

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.ConfigMap
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.StaxConversions.asIterator
import eu.cdevreeze.yaidom.convert.StaxConversions.convertToEventWithEndStateIterator
import eu.cdevreeze.yaidom.convert.StaxConversions.takeElem
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

    var it = convertToEventWithEndStateIterator(asIterator(xmlEventReader)).buffered

    var contactCount = 0
    var elemCount = 0

    def isStartContact(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "contact"

    it = it.dropWhile(es => !isStartContact(es.event)).buffered

    while (it.hasNext) {
      val contactResult = takeElem(it)

      val contactElem = contactResult.elem
      it = contactResult.remainder

      assert(contactElem.localName == "contact")
      contactCount += 1
      elemCount += contactElem.findAllElemsOrSelf.size

      assertResult(true) {
        Set("firstName", "lastName").subsetOf(contactElem.findAllElems.map(_.localName).toSet)
      }

      it = it.dropWhile(es => !isStartContact(es.event)).buffered
    }

    assertResult(true) {
      contactCount >= 1000
    }
    assertResult(true) {
      elemCount >= 10000
    }
  }

  @Test def testProcessAnotherXmlUsingStreaming(): Unit = {
    val fileUri = classOf[StreamingLargeXmlTest].getResource("enterprise-info.xml").toURI

    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new FileInputStream(new File(fileUri)))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    var it = convertToEventWithEndStateIterator(asIterator(xmlEventReader)).buffered

    var enterpriseCount = 0

    def isEnterprise(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "Enterprise"

    it = it.dropWhile(es => !isEnterprise(es.event)).buffered

    while (it.hasNext) {
      val enterpriseResult = takeElem(it)

      val enterpriseElem = enterpriseResult.elem
      it = enterpriseResult.remainder

      assert(enterpriseElem.localName == "Enterprise")
      enterpriseCount += 1

      if (enterpriseCount % 100 == 0) {
        assertResult(true) {
          Set("Address", "LocalUnit").subsetOf(enterpriseElem.findAllChildElems.map(_.localName).toSet)
        }
      }

      it = it.dropWhile(es => !isEnterprise(es.event)).buffered
    }

    assertResult(2000) {
      enterpriseCount
    }
  }
}
