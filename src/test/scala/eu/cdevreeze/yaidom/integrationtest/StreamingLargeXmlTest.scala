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

import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.StaxConversions.asIterator
import eu.cdevreeze.yaidom.convert.StaxConversions.convertToEventWithEndStateIterator
import eu.cdevreeze.yaidom.convert.StaxConversions.takeElem
import eu.cdevreeze.yaidom.convert.StaxConversions.takeElemSeqUntil
import eu.cdevreeze.yaidom.simple.Elem
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.stream.StreamSource

/**
 * Large XML test case, using streaming, thus keeping the memory footprint low. This test case shows how to code StAX-based streaming for yaidom, keeping the
 * memory footprint low. This approach must therefore work for XML files of multiple GiB.
 *
 * Acknowledgments: The large XML files come from http://javakata6425.appspot.com/#!goToPageIIIarticleIIIOptimally%20parse%20humongous%20XML%20files%20with%20vanilla%20Java.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class StreamingLargeXmlTest extends FunSuite with BeforeAndAfterAll {

  @volatile private var xmlBytes: Array[Byte] = _

  protected override def beforeAll(): Unit = {
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
  test("testProcessLargeXmlUsingStreaming") {
    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new jio.ByteArrayInputStream(this.xmlBytes))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    // In an earlier version of this test file buffered iterators were created while traversing the
    // originally created buffered iterator. This did not work on Scala 2.12.0-RC1, resulting in an infinite
    // loop in scala.collection.Iterator.hasNext, hopping between lines 800 and 1078. So we take care to
    // no longer create a buffered iterator more than once.

    var it = convertToEventWithEndStateIterator(asIterator(xmlEventReader)).buffered

    var contactCount = 0
    var elemCount = 0

    def isStartContact(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "contact"

    def dropWhileNotContact(): Unit = {
      while (it.hasNext && !isStartContact(it.head.event)) {
        it.next()
      }
    }

    dropWhileNotContact()

    while (it.hasNext) {
      val contactResult = takeElem(it)

      val contactElem = contactResult.elem

      assert(contactElem.localName == "contact")
      contactCount += 1
      elemCount += contactElem.findAllElemsOrSelf.size

      assertResult(true) {
        Set("firstName", "lastName").subsetOf(contactElem.findAllElems.map(_.localName).toSet)
      }

      dropWhileNotContact()
    }

    assertResult(true) {
      contactCount >= 1000
    }
    assertResult(true) {
      elemCount >= 10000
    }
  }

  /**
   * Test showing how StAX can help process very large XML inputs in many situations.
   * It is neither elegant nor fast code, but chunks of the input XML are processed by yaidom.
   *
   * This test example is simple, and does not use any namespaces.
   */
  test("testProcessLargeXmlUsingStreamingStoringMultipleElements") {
    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new jio.ByteArrayInputStream(this.xmlBytes))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    // In an earlier version of this test file buffered iterators were created while traversing the
    // originally created buffered iterator. This did not work on Scala 2.12.0-RC1, resulting in an infinite
    // loop in scala.collection.Iterator.hasNext, hopping between lines 800 and 1078. So we take care to
    // no longer create a buffered iterator more than once.

    var it = convertToEventWithEndStateIterator(asIterator(xmlEventReader)).buffered

    var contactCount = 0
    var elemCount = 0

    def isStartContact(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "contact"

    def dropWhileNotContact(): Unit = {
      while (it.hasNext && !isStartContact(it.head.event)) {
        it.next()
      }
    }

    def take10Contacts(): immutable.IndexedSeq[Elem] = {
      takeElemSeqUntil(it, result => result.elems.size == 10).elems
    }

    dropWhileNotContact()

    while (it.hasNext) {
      val contactElems = take10Contacts()

      assert(contactElems.forall(_.localName == "contact"))
      contactCount += contactElems.size
      elemCount += contactElems.flatMap(_.findAllElemsOrSelf).size

      assertResult(true) {
        Set("firstName", "lastName").subsetOf(contactElems.flatMap(_.findAllElems).map(_.localName).toSet)
      }

      dropWhileNotContact()
    }

    assertResult(true) {
      contactCount >= 1000
    }
    assertResult(true) {
      elemCount >= 10000
    }
  }

  test("testProcessAnotherXmlUsingStreaming") {
    val fileUri = classOf[StreamingLargeXmlTest].getResource("enterprise-info.xml").toURI

    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new FileInputStream(new File(fileUri)))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    // In an earlier version of this test file buffered iterators were created while traversing the
    // originally created buffered iterator. This did not work on Scala 2.12.0-RC1, resulting in an infinite
    // loop in scala.collection.Iterator.hasNext, hopping between lines 800 and 1078. So we take care to
    // no longer create a buffered iterator more than once.

    var it = convertToEventWithEndStateIterator(asIterator(xmlEventReader)).buffered

    var enterpriseCount = 0

    def isEnterprise(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "Enterprise"

    def dropWhileNotEnterprise(): Unit = {
      while (it.hasNext && !isEnterprise(it.head.event)) {
        it.next()
      }
    }

    dropWhileNotEnterprise()

    while (it.hasNext) {
      val enterpriseResult = takeElem(it)

      val enterpriseElem = enterpriseResult.elem

      assert(enterpriseElem.localName == "Enterprise")
      enterpriseCount += 1

      if (enterpriseCount % 100 == 0) {
        assertResult(true) {
          Set("Address", "LocalUnit").subsetOf(enterpriseElem.findAllChildElems.map(_.localName).toSet)
        }
      }

      dropWhileNotEnterprise()
    }

    assertResult(2000) {
      enterpriseCount
    }
  }
}
