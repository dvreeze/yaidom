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
import javax.xml.stream.{ XMLInputFactory, XMLOutputFactory, XMLEventFactory }
import javax.xml.stream.events.XMLEvent
import javax.xml.transform.TransformerFactory
import javax.xml.transform.stream.{ StreamSource, StreamResult }
import scala.collection.immutable
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import parse._
import print._

/**
 * Large XML test case.
 *
 * Acknowledgments: The large XML files come from http://javakata6425.appspot.com/#!goToPageIIIarticleIIIOptimally%20parse%20humongous%20XML%20files%20with%20vanilla%20Java.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class LargeXmlTest extends Suite with BeforeAndAfterAll {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @volatile private var xmlBytes: Array[Byte] = _

  override def beforeAll(configMap: Map[String, Any]) {
    val zipFileUrl = classOf[LargeXmlTest].getResource("veryBigFile.zip")
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

  @Test def testProcessLargeXmlUsingSax() {
    val parser = DocumentParserUsingSax.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testProcessLargeXmlUsingSax] Parsing (into a Document) took %d ms".format(endMs - startMs))

    doTest(doc.documentElement)
  }

  @Test def testProcessLargeXmlIntoResolvedElemUsingSax() {
    val parser = DocumentParserUsingSax.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testProcessLargeXmlIntoResolvedElemUsingSax] Parsing (into a Document) took %d ms".format(endMs - startMs))

    val resolvedRoot = resolved.Elem(doc.documentElement)
    doTest(resolvedRoot)

    val emailPaths = resolvedRoot findTopmostElemPaths { e => e.localName == "email" } take (10)
    val emailElms = resolvedRoot findTopmostElems { e => e.localName == "email" } take (10)

    expectResult(10) {
      emailPaths.size
    }
    expectResult(10) {
      emailElms.size
    }
    expectResult(emailElms) {
      emailPaths map { path => resolvedRoot.getWithElemPath(path) }
    }
  }

  /** A real stress test (disabled by default). When running it, use jvisualvm to check on the JVM behavior */
  @Ignore @Test def testParseLargeXmlRepeatedly() {
    val FirstNameEName = EName("firstName")
    val LastNameEName = EName("lastName")
    val ContactEName = EName("contact")
    val EmailEName = EName("email")

    for (i <- (0 until 200).par) {
      val parser = DocumentParserUsingSax.newInstance

      val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
      logger.info("Parsed Document (%d) in thread %s".format(i + 1, Thread.currentThread.getName))

      (i % 5) match {
        case 0 =>
          val firstNameElms = doc.documentElement.filterElems(FirstNameEName)
          logger.info("Number of first names: %d. Thread %s".format(firstNameElms.size, Thread.currentThread.getName))
        case 1 =>
          val lastNameElms = doc.documentElement.filterElems(LastNameEName)
          logger.info("Number of last names: %d. Thread %s".format(lastNameElms.size, Thread.currentThread.getName))
        case 2 =>
          val contactElms = doc.documentElement \\ ContactEName
          logger.info("Number of contacts: %d. Thread %s".format(contactElms.size, Thread.currentThread.getName))
        case 3 =>
          val emails = {
            val result = doc.documentElement.findAllElemsOrSelf collect {
              case e if e.resolvedName == EmailEName => e.trimmedText
            }
            result.toSet
          }
          logger.info("Different e-mails (%d): %s. Thread %s".format(emails.size, emails, Thread.currentThread.getName))
        case 4 =>
          val firstNameElms = doc.documentElement \\ FirstNameEName
          logger.info("Number of first names: %d. Thread %s".format(firstNameElms.size, Thread.currentThread.getName))
      }
    }
  }

  @Test def testProcessLargeXmlUsingStax() {
    val parser = DocumentParserUsingStax.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testProcessLargeXmlUsingStax] Parsing (into a Document) took %d ms".format(endMs - startMs))

    doTest(doc.documentElement)
  }

  @Test def testProcessLargeXmlUsingDom() {
    val parser = DocumentParserUsingDom.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testProcessLargeXmlUsingDom] Parsing (into a Document) took %d ms".format(endMs - startMs))

    doTest(doc.documentElement)
  }

  @Test def testProcessLargeXmlIntoIndexedElem() {
    val parser = DocumentParserUsingSax.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testProcessLargeXmlIntoIndexedElem] Parsing (into a Document) took %d ms".format(endMs - startMs))

    val indexedDoc = indexed.Document(doc)

    doTest(indexedDoc.documentElement)
  }

  /** A heavy test (now disabled) printing/parsing using the tree representation DSL. When running it, consider using jvisualvm to check on the JVM behavior */
  @Ignore @Test def testProcessLargeTreeRepr() {
    val parser = DocumentParserUsingSax.newInstance

    val startMs1 = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs1 = System.currentTimeMillis()
    logger.info("[testProcessLargeTreeRepr] Parsing (into a Document) took %d ms".format(endMs1 - startMs1))

    val startMs2 = System.currentTimeMillis()
    val treeRepr: String = doc.toString
    val endMs2 = System.currentTimeMillis()
    logger.info("[testProcessLargeTreeRepr] Calling toString took %d ms".format(endMs2 - startMs2))

    expectResult("document(") {
      treeRepr.take("document(".length)
    }

    val startMs3 = System.currentTimeMillis()
    val doc2: Document = {
      import TreeReprParsers._

      val parseResult = parseAll(TreeReprParsers.document, treeRepr)
      parseResult.get.build()
    }
    val endMs3 = System.currentTimeMillis()
    logger.info("[testProcessLargeTreeRepr] Parsing the tree representation took %d ms".format(endMs3 - startMs3))

    doTest(doc2.documentElement)
  }

  @Ignore @Test def testSerializeLargeNodeBuilder() {
    val parser = DocumentParserUsingSax.newInstance

    val startMs1 = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs1 = System.currentTimeMillis()
    logger.info("[testSerializeLargeNodeBuilder] Parsing (into a Document) took %d ms".format(endMs1 - startMs1))

    val startMs2 = System.currentTimeMillis()

    val docBuilder = DocBuilder.fromDocument(doc)
    val bos = new jio.ByteArrayOutputStream
    val oos = new jio.ObjectOutputStream(bos)

    oos.writeObject(docBuilder)

    val objectBytes = bos.toByteArray

    val endMs2 = System.currentTimeMillis()
    logger.info("[testSerializeLargeNodeBuilder] Serializing took %d ms".format(endMs2 - startMs2))

    val startMs3 = System.currentTimeMillis()

    val bis = new jio.ByteArrayInputStream(objectBytes)
    val ois = new jio.ObjectInputStream(bis)

    val doc2Builder = ois.readObject().asInstanceOf[DocBuilder]
    val doc2 = doc2Builder.build()

    val endMs3 = System.currentTimeMillis()
    logger.info("[testSerializeLargeNodeBuilder] Deserializing took %d ms".format(endMs3 - startMs3))

    doTest(doc2.documentElement)
  }

  @Ignore @Test def testSerializeLargeNode() {
    val parser = DocumentParserUsingSax.newInstance

    val startMs1 = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs1 = System.currentTimeMillis()
    logger.info("[testSerializeLargeNode] Parsing (into a Document) took %d ms".format(endMs1 - startMs1))

    val startMs2 = System.currentTimeMillis()

    val bos = new jio.ByteArrayOutputStream
    val oos = new jio.ObjectOutputStream(bos)

    oos.writeObject(doc)

    val objectBytes = bos.toByteArray

    val endMs2 = System.currentTimeMillis()
    logger.info("[testSerializeLargeNode] Serializing took %d ms".format(endMs2 - startMs2))

    val startMs3 = System.currentTimeMillis()

    val bis = new jio.ByteArrayInputStream(objectBytes)
    val ois = new jio.ObjectInputStream(bis)

    val doc2 = ois.readObject().asInstanceOf[Document]

    val endMs3 = System.currentTimeMillis()
    logger.info("[testSerializeLargeNode] Deserializing took %d ms".format(endMs3 - startMs3))

    doTest(doc2.documentElement)
  }

  @Test def testFind() {
    val parser = DocumentParserUsingDom.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testFind] Parsing (into a Document) took %d ms".format(endMs - startMs))

    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    expectResult(true) {
      val phoneElms = (rootElm \\ (_.localName == "phone")) filter { e => e.text.size == 1000 }
      phoneElms.size < 4000
    }
    expectResult(true) {
      val phoneElms = (rootElm \\ (_.localName == "phone")) filter { e => e.text.size == 2046 }
      phoneElms.size > 15000
    }

    val s = "b" * (1000)

    // Note: Do not take the durations logged below too literally. This is not a properly set up performance test in any way!

    rootElm findElemOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
    rootElm findElem { e => e.resolvedName == EName("phone") && e.trimmedText == s }
    (rootElm findTopmostElemsOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }).headOption
    (rootElm \\ { e => e.resolvedName == EName("phone") && e.trimmedText == s }).headOption
    (rootElm.findAllElemsOrSelf filter { e => e.resolvedName == EName("phone") && e.trimmedText == s }).headOption

    // Finding the fast way
    val start2Ms = System.currentTimeMillis()
    val foundElm2 = {
      val result = rootElm findElemOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.getOrElse(sys.error("Expected at least one phone element with text value '%s'".format(s)))
    }
    val end2Ms = System.currentTimeMillis()
    logger.info("Finding an element the fast way (using findElemOrSelf) took %d ms".format(end2Ms - start2Ms))

    // Finding the fast way (again)
    val start3Ms = System.currentTimeMillis()
    val foundElm3 = {
      val result = rootElm findElem { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.getOrElse(sys.error("Expected at least one phone element with text value '%s'".format(s)))
    }
    val end3Ms = System.currentTimeMillis()
    logger.info("Finding an element the fast way (using findElem) took %d ms".format(end3Ms - start3Ms))

    // Finding the slower way
    val start4Ms = System.currentTimeMillis()
    val foundElm4 = {
      val result = rootElm findTopmostElemsOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error("Expected at least one phone element with text value '%s'".format(s)))
    }
    val end4Ms = System.currentTimeMillis()
    logger.info("Finding an element the slower way (using findTopmostElemsOrSelf) took %d ms".format(end4Ms - start4Ms))

    // Finding the still slower way (in theory)
    val start5Ms = System.currentTimeMillis()
    val foundElm5 = {
      val result = rootElm filterElemsOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error("Expected at least one phone element with text value '%s'".format(s)))
    }
    val end5Ms = System.currentTimeMillis()
    logger.info("Finding an element the (theoretically) still slower way (using filterElemsOrSelf) took %d ms".format(end5Ms - start5Ms))

    // Finding the slowest way (in theory)
    val start6Ms = System.currentTimeMillis()
    val foundElm6 = {
      val result = rootElm.findAllElemsOrSelf filter { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error("Expected at least one phone element with text value '%s'".format(s)))
    }
    val end6Ms = System.currentTimeMillis()
    logger.info("Finding an element the (theoretically) slowest way (using findAllElemsOrSelf) took %d ms".format(end6Ms - start6Ms))
  }

  @Test def testUpdate() {
    val parser = DocumentParserUsingDom.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testUpdate] Parsing (into a Document) took %d ms".format(endMs - startMs))

    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    val path = ElemPathBuilder.from(QName("contact") -> 2499, QName("phone") -> 0).build(Scope.Empty)

    val newPhone = "012-34567890"

    val oldPhoneElm: Elem = doc.documentElement.findWithElemPath(path).getOrElse(sys.error("Expected element at path: " + path))

    expectResult(false) {
      oldPhoneElm.text == newPhone
    }

    // Update, using a fixed path.

    val start2Ms = System.currentTimeMillis()
    val updatedDoc: Document = doc.updated(path) { e =>
      e.withChildren(Vector(Text(newPhone, false)))
    }
    val end2Ms = System.currentTimeMillis()
    logger.info("Updating an element in the document took %d ms".format(end2Ms - start2Ms))

    val newPhoneElm: Elem = updatedDoc.documentElement.findWithElemPath(path).getOrElse(sys.error("Expected element at path: " + path))

    expectResult(true) {
      newPhoneElm.text == newPhone
    }

    // Comparing the corresponding resolved elements

    val resolvedElm1: resolved.Elem = resolved.Elem(doc.documentElement)

    val resolvedDocElm = resolved.Elem(doc.documentElement)
    val resolvedElm2: resolved.Elem = resolvedDocElm.updated(path) { e =>
      e.withChildren(Vector(resolved.Text(newPhone)))
    }

    val resolvedElm3: resolved.Elem = resolved.Elem(updatedDoc.documentElement)

    expectResult(false) {
      resolvedElm1 == resolvedElm2
    }
    expectResult(false) {
      resolvedElm1 == resolvedElm3
    }

    expectResult(true) {
      resolvedElm2 == resolvedElm3
    }
  }

  @Test def testUpdateAgain() {
    val parser = DocumentParserUsingDom.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info("[testUpdateAgain] Parsing (into a Document) took %d ms".format(endMs - startMs))

    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    val path = ElemPathBuilder.from(QName("contact") -> 2499, QName("phone") -> 0).build(Scope.Empty)

    val newPhone = "012-34567890"

    val oldPhoneElm: Elem = doc.documentElement.findWithElemPath(path).getOrElse(sys.error("Expected element at path: " + path))

    expectResult(false) {
      oldPhoneElm.text == newPhone
    }

    // Update, using a partial function. Note that this is probably inefficient for very large XML documents.

    val start2Ms = System.currentTimeMillis()

    val updatedDoc: Document = doc updated {
      case e if (e.localName == "phone") && (e == oldPhoneElm) => e.withChildren(Vector(Text(newPhone, false)))
    }

    val end2Ms = System.currentTimeMillis()
    logger.info("Updating an element in the document (using a partial function) took %d ms".format(end2Ms - start2Ms))

    val newPhoneElm: Elem = updatedDoc.documentElement.findWithElemPath(path).getOrElse(sys.error("Expected element at path: " + path))

    expectResult(newPhone) {
      newPhoneElm.text
    }
  }

  private def doTest[E <: ElemLike[E] with HasText](elm: E) {
    val startMs = System.currentTimeMillis()

    assert(elm.findAllElemsOrSelf.size >= 100000, "Expected at least 100000 elements in the XML")

    expectResult(Set(EName("contacts"), EName("contact"), EName("firstName"), EName("lastName"), EName("email"), EName("phone"))) {
      val result = elm.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    val s = "b" * (2000 + 46)
    val elms1 = elm \\ { e => e.resolvedName == EName("phone") && e.trimmedText == s }
    assert(elms1.size >= 1, "Expected at least one phone element with text value '%s'".format(s))

    val endMs = System.currentTimeMillis()
    logger.info("The test (invoking findAllElemsOrSelf twice, and filterElemsOrSelf once) took %d ms".format(endMs - startMs))
  }
}
