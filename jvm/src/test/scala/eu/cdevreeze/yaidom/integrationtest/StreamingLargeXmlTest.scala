/*
 * Copyright 2011-2017 Chris de Vreeze
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
import java.net.URI
import java.{ util => jutil }

import scala.collection.immutable
import scala.collection.mutable

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.convert.EventWithAncestry
import eu.cdevreeze.yaidom.convert.StaxConversions.asIterator
import eu.cdevreeze.yaidom.convert.StaxConversions.convertToEventWithAncestryIterator
import eu.cdevreeze.yaidom.convert.StaxConversions.takeElem
import eu.cdevreeze.yaidom.convert.StaxConversions.takeElemsUntil
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.simple.Elem
import javax.xml.stream.XMLInputFactory
import javax.xml.stream.events.{ ProcessingInstruction => StaxProcessingInstruction }
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

  import EventWithAncestry.dropWhileNot

  @volatile private var xmlBytes: Array[Byte] = _

  protected override def beforeAll(): Unit = {
    val zipFileUrl = classOf[StreamingLargeXmlTest].getResource("veryBigFile.zip")
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

    val it = convertToEventWithAncestryIterator(asIterator(xmlEventReader)).buffered

    var contactCount = 0
    var elemCount = 0

    def isStartContact(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "contact"

    def dropWhileNotContact(): Unit = {
      dropWhileNot(it, e => isStartContact(e.event))
    }

    dropWhileNotContact()

    while (it.hasNext) {
      val contactElem = takeElem(it)

      assert(contactElem.localName == "contact")
      contactCount += 1
      elemCount += contactElem.findAllElemsOrSelf.size

      assertResult(true) {
        Set("firstName", "lastName").subsetOf(contactElem.findAllElems.map(_.localName).toSet)
      }

      dropWhileNotContact()
    }

    assertResult(true) {
      contactCount >= 10000
    }
    assertResult(true) {
      elemCount >= 100000
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

    val it = convertToEventWithAncestryIterator(asIterator(xmlEventReader)).buffered

    var contactCount = 0
    var elemCount = 0

    def isStartContact(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "contact"

    def dropWhileNotContact(): Unit = {
      dropWhileNot(it, e => isStartContact(e.event))
    }

    def take10Contacts(): immutable.IndexedSeq[Elem] = {
      takeElemsUntil(it, (elms, nextEvOption) => elms.size == 10)
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
      contactCount >= 10000
    }
    assertResult(true) {
      elemCount >= 100000
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

    val it = convertToEventWithAncestryIterator(asIterator(xmlEventReader)).buffered

    var enterpriseCount = 0

    def isEnterprise(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "Enterprise"

    def dropWhileNotEnterprise(): Unit = {
      dropWhileNot(it, e => isEnterprise(e.event))
    }

    dropWhileNotEnterprise()

    while (it.hasNext) {
      val enterpriseElem = takeElem(it)

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

  /**
   * Test method finding a schemaRef element in an XBRL instance. StAX streaming is used here to keep memory usage (and CPU cycles)
   * to a minimum when finding a schemaRef element high in the document. This is another use case of StAX streaming: the XBRL instance
   * may or may not be large, but we can get the required result (a schemaRef element) with very little processing (as compared to building
   * parsing an entire DOM tree). This shows that the combination of StAX and yaidom can be used for many scenarios, and that there
   * can be much freedom in how much of the StAX stream is processed, and how many yaidom elements are created.
   */
  test("testFindEntrypoint") {
    val fileUri = classOf[StreamingLargeXmlTest].getResource("sample-xbrl-instance.xml").toURI

    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new FileInputStream(new File(fileUri)))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    val it = convertToEventWithAncestryIterator(asIterator(xmlEventReader)).buffered

    def isSchemaRef(ev: EventWithAncestry): Boolean =
      ev.event.isStartElement() && ev.enames.headOption.contains(EName(LinkNamespace, "schemaRef"))

    def dropWhileNotSchemaRef(): Unit = {
      dropWhileNot(it, e => isSchemaRef(e))
    }

    dropWhileNotSchemaRef()

    require(it.hasNext, s"Expected a link:schemaRef element but there are no more elements in the StAX stream")

    // Before materializing the next (schemaRef) element, we peek into the next event and have a look at the ancestry path.
    val event = it.head

    assertResult(Some(EName(LinkNamespace, "schemaRef"))) {
      event.enames.headOption
    }
    assertResult(List(EName(LinkNamespace, "schemaRef"), EName(XbrliNamespace, "xbrl"))) {
      event.enames
    }

    val baseUri = event.ancestryPathAfterEventOption.map(_.ancestorOrSelfEntries.reverse).getOrElse(Nil).foldLeft(fileUri) {
      case (accUri, elemEntry) =>
        val elem = Elem(elemEntry.qname, elemEntry.attributes, elemEntry.scope, Vector())
        XmlBaseSupport.findBaseUriByParentBaseUri(Some(accUri), elem)(XmlBaseSupport.JdkUriResolver).getOrElse(accUri)
    }

    assertResult(fileUri) {
      baseUri
    }

    // Now materialize the next (schemaRef) element.
    val schemaRef = takeElem(it)

    assertResult(EName(LinkNamespace, "schemaRef")) {
      schemaRef.resolvedName
    }

    assertResult(Some(URI.create("gaap.xsd"))) {
      schemaRef.attributeOption(EName(XLinkNamespace, "href")).map(s => URI.create(s))
    }
    assertResult(Some(URI.create("gaap.xsd")).map(u => fileUri.resolve(u))) {
      val schemaRefUriOption = schemaRef.attributeOption(EName(XLinkNamespace, "href")).map(s => URI.create(s))
      schemaRefUriOption.map(u => baseUri.resolve(u))
    }

    dropWhileNot(it, ev => ev.event.isStartElement)

    assertResult(true) {
      it.hasNext
    }
    assertResult(Some(EName(LinkNamespace, "linkbaseRef"))) {
      val nextEv = it.next
      nextEv.enames.headOption
    }
  }

  /**
   * Test method performing non-standard two-pass XBRL streaming, where the first pass accumulates all contexts and
   * units, and where the second pass materializes and processes one fact at a time. The idea is here that the
   * XBRL instance may be too large to keep entirely in memory, but that this is not the case for the contexts and
   * units.
   */
  test("testNonStandardTwoPassXbrlStreaming") {
    val fileUri = classOf[StreamingLargeXmlTest].getResource("sample-xbrl-instance.xml").toURI

    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new FileInputStream(new File(fileUri)))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    val it = convertToEventWithAncestryIterator(asIterator(xmlEventReader)).buffered

    val contextBuffer = mutable.ArrayBuffer[Elem]()
    val unitBuffer = mutable.ArrayBuffer[Elem]()

    def isContextOrUnit(ev: EventWithAncestry): Boolean = {
      ev.event.isStartElement && Set(Option(XbrliContextEName), Option(XbrliUnitEName)).contains(ev.enames.headOption)
    }

    while (it.hasNext) {
      dropWhileNot(it, isContextOrUnit _)

      if (it.hasNext) {
        val elem = takeElem(it)

        if (elem.resolvedName == XbrliContextEName) {
          contextBuffer += elem
        } else {
          assert(elem.resolvedName == XbrliUnitEName)
          unitBuffer += elem
        }
      }
    }

    // We keep all contexts and units in memory

    val contexts = contextBuffer.toIndexedSeq
    val units = unitBuffer.toIndexedSeq

    val contextsById = contexts.groupBy(_.attribute(EName("id"))).mapValues(_.head)
    val unitsById = units.groupBy(_.attribute(EName("id"))).mapValues(_.head)

    // Let's do the second pass, taking one fact element at a time

    val streamSource2 = new StreamSource(new FileInputStream(new File(fileUri)))
    val xmlEventReader2 = inputFactory.createXMLEventReader(streamSource2)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    val it2 = convertToEventWithAncestryIterator(asIterator(xmlEventReader2)).buffered

    def isTopLevelFact(ev: EventWithAncestry): Boolean = {
      ev.event.isStartElement &&
        ev.enames.headOption.exists(en => !Set(Option(XbrliNamespace), Option(LinkNamespace)).contains(en.namespaceUriOption)) &&
        ev.enames.size == 2
    }

    var allContextsFound = true
    var allUnitsFound = true

    dropWhileNot(it2, isTopLevelFact _)

    while (it2.hasNext) {
      val fact = takeElem(it2)
      require(fact.attributeOption(ContextRefEName).isDefined, s"Missing @contextRef in ${fact.resolvedName}")

      allContextsFound = allContextsFound && contextsById.contains(fact.attribute(ContextRefEName))

      if (fact.attributeOption(UnitRefEName).isDefined) {
        allUnitsFound = allUnitsFound && unitsById.contains(fact.attribute(UnitRefEName))
      }

      assert(fact.resolvedName.namespaceUriOption != Some(XbrliNamespace))
      assert(fact.resolvedName.namespaceUriOption != Some(LinkNamespace))

      dropWhileNot(it2, isTopLevelFact _)
    }

    assertResult(true) {
      allContextsFound
    }
    assertResult(true) {
      allUnitsFound
    }
  }

  /**
   * Test method performing one-pass XBRL streaming, where contexts and units are accumulated and kept in memory,
   * and where each encountered fact must reference a context and if numeric a unit that have already been read.
   * The idea is that the XBRL instance may be too large to keep entirely in memory, but that this is not the case
   * for the contexts and units, which must precede that facts using them.
   *
   * See http://www.xbrl.org/Specification/streaming-extensions-module/CR-2015-12-09/streaming-extensions-module-CR-2015-12-09.html.
   *
   * The processing in this test conforms to the XBRL Stream Extensions Module.
   *
   * It is easy to turn this into a full XBRL streaming implementation, by first supporting parsing of the streaming
   * processing instruction, then respect the context and unit buffer sizes, then turn each fact with its context
   * (and unit, if numeric) into a small XBRL instance. From there is it normal XBRL instance validation (or other
   * XBRL instance processing) per such instance, and then somehow accumulating or aggregating the results.
   */
  test("testOnePassXbrlStreaming") {
    val fileUri = classOf[StreamingLargeXmlTest].getResource("sample-xbrl-instance-for-streaming.xml").toURI

    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(new FileInputStream(new File(fileUri)))
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    val it = convertToEventWithAncestryIterator(asIterator(xmlEventReader)).buffered

    var contextMap = Map[String, Elem]()
    var unitMap = Map[String, Elem]()

    var allContextsFound = true
    var allUnitsFound = true

    def isStreamingPI(ev: EventWithAncestry): Boolean = {
      ev.event.isProcessingInstruction && ev.event.asInstanceOf[StaxProcessingInstruction].getTarget == "xbrl-streamable-instance"
    }

    def isContextOrUnit(ev: EventWithAncestry): Boolean = {
      ev.event.isStartElement && Set(Option(XbrliContextEName), Option(XbrliUnitEName)).contains(ev.enames.headOption)
    }

    def isTopLevelFact(ev: EventWithAncestry): Boolean = {
      ev.event.isStartElement &&
        ev.enames.headOption.exists(en => !Set(Option(XbrliNamespace), Option(LinkNamespace)).contains(en.namespaceUriOption)) &&
        ev.enames.size == 2
    }

    dropWhileNot(it, isStreamingPI _)

    require(it.hasNext)
    val piEvent = it.next

    // The following check is a bit sensitive, but good enough for this test (because we know the input file)

    assertResult("""version="1.0" contextBuffer="INF" unitBuffer="INF"""") {
      piEvent.event.asInstanceOf[StaxProcessingInstruction].getData.trim
    }

    while (it.hasNext) {
      dropWhileNot(it, ev => isContextOrUnit(ev) || isTopLevelFact(ev))

      if (it.hasNext) {
        val elem = takeElem(it)

        if (elem.resolvedName == XbrliContextEName) {
          contextMap += (elem.attribute(EName("id")) -> elem)
        } else if (elem.resolvedName == XbrliUnitEName) {
          unitMap += (elem.attribute(EName("id")) -> elem)
        } else {
          val factElem = elem

          allContextsFound = allContextsFound && contextMap.contains(factElem.attribute(ContextRefEName))

          if (factElem.attributeOption(UnitRefEName).isDefined) {
            allUnitsFound = allUnitsFound && unitMap.contains(factElem.attribute(UnitRefEName))
          }
        }
      }
    }

    // The following assertions must be true if the XBRL instance is to be valid w.r.t. the XBRL streaming specification.

    assertResult(true) {
      allContextsFound
    }
    assertResult(true) {
      allUnitsFound
    }
  }

  /**
   * Streaming test reading Wikipedia. See http://blog.korny.info/2014/03/08/xml-for-fun-and-profit.html.
   * Just run this test once, to show that StAX-based usage of yaidom for very large XML documents works in practice.
   *
   * The last run processed 5277000 document abstracts (doc elements) in 2703 seconds, so with a speed of 1952
   * doc elements per second, taking merely about 1.5 GiB in memory! No fewer than 101570334 elements were found.
   */
  ignore("testProcessWikipediaUsingStreaming") {
    // External file, and a very large one as well. This really proves that StAX-based streaming in yaidom works, where
    // descendant elements with a given name are loaded entirely in memory as yaidom Elem, one Elem at the time.

    val uri = java.net.URI.create("https://dumps.wikimedia.org/enwiki/latest/enwiki-latest-abstract.xml")

    val inputFactory = XMLInputFactory.newInstance

    val streamSource = new StreamSource(uri.toURL.openStream())
    val xmlEventReader = inputFactory.createXMLEventReader(streamSource)

    // Turn the Java iterator of StAX events into a Scala buffered iterator of enriched StAX events.
    // Creating this buffered iterator is done only once! Low level methods hasNext, head and next are
    // called to advance the iterator.

    val it = convertToEventWithAncestryIterator(asIterator(xmlEventReader)).buffered

    var docCount = 0
    var elemCount = 0

    def isDoc(xmlEvent: XMLEvent): Boolean =
      xmlEvent.isStartElement() && xmlEvent.asStartElement().getName.getLocalPart == "doc"

    def dropWhileNotDoc(): Unit = {
      dropWhileNot(it, e => isDoc(e.event))
    }

    dropWhileNotDoc()

    while (it.hasNext) {
      val docElem = takeElem(it)

      assert(docElem.localName == "doc")
      docCount += 1

      val allElems = docElem.findAllElemsOrSelf

      assertResult(true) {
        Set("doc", "title", "abstract", "url").subsetOf(allElems.map(_.localName).toSet)
      }

      elemCount += allElems.size

      if (docCount % 1000 == 0) {
        println(s"Found $docCount docs so far")
        println(s"\tFound $elemCount elements so far")
      }

      dropWhileNotDoc()
    }

    assertResult(true) {
      docCount >= 1000000
    }
  }

  private val XbrliNamespace = "http://www.xbrl.org/2003/instance"
  private val LinkNamespace = "http://www.xbrl.org/2003/linkbase"
  private val XLinkNamespace = "http://www.w3.org/1999/xlink"

  private val XbrliContextEName = EName(XbrliNamespace, "context")
  private val XbrliUnitEName = EName(XbrliNamespace, "unit")

  private val ContextRefEName = EName("contextRef")
  private val UnitRefEName = EName("unitRef")
}
