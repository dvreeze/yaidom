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
import java.net.URI
import java.{ util => jutil }

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.Ignore
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.PathBuilder
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.simple.DocBuilder
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Text
import javax.xml.parsers.DocumentBuilderFactory

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

  @volatile private var doc: Document = _
  @volatile private var domDoc: org.w3c.dom.Document = _
  @volatile private var scalaElem: scala.xml.Elem = _

  val enames =
    Set(EName("contacts"), EName("contact"), EName("firstName"), EName("lastName"), EName("email"), EName("phone"))
  val qnames = enames.map(en => QName(en.localPart))

  ENameProvider.globalENameProvider.become(new ENameProvider.ENameProviderUsingImmutableCache(enames))
  QNameProvider.globalQNameProvider.become(new QNameProvider.QNameProviderUsingImmutableCache(qnames))

  protected override def beforeAll(): Unit = {
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

    val xmlBytes = bos.toByteArray

    val docParser = DocumentParserUsingSax.newInstance
    this.doc = docParser.parse(new jio.ByteArrayInputStream(xmlBytes))

    val db = DocumentBuilderFactory.newInstance().newDocumentBuilder()
    this.domDoc = db.newDocument()
    this.domDoc = convert.DomConversions.convertDocument(this.doc)(this.domDoc)

    this.scalaElem = convert.ScalaXmlConversions.convertElem(this.doc.documentElement)
  }

  /** A real stress test (disabled by default). When running it, use jvisualvm to check on the JVM behavior */
  @Ignore @Test def testQueryLargeXmlRepeatedly(): Unit = {
    val FirstNameEName = EName("firstName")
    val LastNameEName = EName("lastName")
    val ContactEName = EName("contact")
    val EmailEName = EName("email")

    for (i <- (0 until 200).par) {
      logger.info(s"Queried Document (run ${i + 1}) in thread ${Thread.currentThread.getName}")

      (i % 5) match {
        case 0 =>
          val firstNameElms = doc.documentElement.filterElems(FirstNameEName)
          logger.info(s"Number of first names: ${firstNameElms.size}. Thread ${Thread.currentThread.getName}")
        case 1 =>
          val lastNameElms = doc.documentElement.filterElems(LastNameEName)
          logger.info(s"Number of last names: ${lastNameElms.size}. Thread ${Thread.currentThread.getName}")
        case 2 =>
          val contactElms = doc.documentElement \\ ContactEName
          logger.info(s"Number of contacts: ${contactElms.size}. Thread ${Thread.currentThread.getName}")
        case 3 =>
          val emails = {
            val result = doc.documentElement.findAllElemsOrSelf collect {
              case e if e.resolvedName == EmailEName => e.trimmedText
            }
            result.toSet
          }
          logger.info(s"Different e-mails (${emails.size}). Thread ${Thread.currentThread.getName}")
        case 4 =>
          val firstNameElms = doc.documentElement \\ FirstNameEName
          logger.info(s"Number of first names: ${firstNameElms.size}. Thread ${Thread.currentThread.getName}")
      }
    }
  }

  @Test def testQuerySimpleElem(): Unit = {
    doQueryTest(doc.documentElement, "simple.Elem")
  }

  @Test def testQueryIndexedElem(): Unit = {
    val indexedDoc = indexed.Document(doc)

    doQueryTest(indexedDoc.documentElement, "indexed.Elem")
  }

  @Test def testQueryResolvedElem(): Unit = {
    val resolvedElem = resolved.Elem(doc.documentElement)

    doQueryTest(resolvedElem, "resolved.Elem")
  }

  @Test def testQueryDomElem(): Unit = {
    doQueryTest(DomDocument(domDoc).documentElement, "dom.DomElem")
  }

  @Test def testQueryScalaXmlElem(): Unit = {
    doQueryTest(ScalaXmlElem(scalaElem), "scalaxml.ScalaXmlElem")
  }

  /** A heavy test (now disabled) printing/parsing using the tree representation DSL. When running it, consider using jvisualvm to check on the JVM behavior */
  @Ignore @Test def testProcessLargeTreeRepr(): Unit = {
    val startMs2 = System.currentTimeMillis()
    val treeRepr: String = doc.toString
    val endMs2 = System.currentTimeMillis()
    logger.info(s"[testProcessLargeTreeRepr] Calling toString took ${endMs2 - startMs2} ms")

    assertResult("document(") {
      treeRepr.take("document(".length)
    }

    doQueryTest(doc.documentElement, "testProcessLargeTreeRepr: simple.Elem")
  }

  @Ignore @Test def testSerializeLargeNodeBuilder(): Unit = {
    val startMs2 = System.currentTimeMillis()

    val docBuilder = DocBuilder.fromDocument(doc)
    val bos = new jio.ByteArrayOutputStream
    val oos = new jio.ObjectOutputStream(bos)

    oos.writeObject(docBuilder)

    val objectBytes = bos.toByteArray

    val endMs2 = System.currentTimeMillis()
    logger.info(s"[testSerializeLargeNodeBuilder] Serializing took ${endMs2 - startMs2} ms")

    val startMs3 = System.currentTimeMillis()

    val bis = new jio.ByteArrayInputStream(objectBytes)
    val ois = new jio.ObjectInputStream(bis)

    val doc2Builder = ois.readObject().asInstanceOf[DocBuilder]
    val doc2 = doc2Builder.build()

    val endMs3 = System.currentTimeMillis()
    logger.info(s"[testSerializeLargeNodeBuilder] Deserializing took ${endMs3 - startMs3} ms")

    doQueryTest(doc2.documentElement, "testSerializeLargeNodeBuilder: simple.Elem")
  }

  @Ignore @Test def testSerializeLargeNode(): Unit = {
    val startMs2 = System.currentTimeMillis()

    val bos = new jio.ByteArrayOutputStream
    val oos = new jio.ObjectOutputStream(bos)

    oos.writeObject(doc)

    val objectBytes = bos.toByteArray

    val endMs2 = System.currentTimeMillis()
    logger.info(s"[testSerializeLargeNode] Serializing took ${endMs2 - startMs2} ms")

    val startMs3 = System.currentTimeMillis()

    val bis = new jio.ByteArrayInputStream(objectBytes)
    val ois = new jio.ObjectInputStream(bis)

    val doc2 = ois.readObject().asInstanceOf[Document]

    val endMs3 = System.currentTimeMillis()
    logger.info(s"[testSerializeLargeNode] Deserializing took ${endMs3 - startMs3} ms")

    doQueryTest(doc2.documentElement, "testSerializeLargeNode: simple.Elem")
  }

  @Test def testFind(): Unit = {
    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    assertResult(true) {
      val phoneElms = (rootElm \\ (_.localName == "phone")) filter { e => e.text.size == 1000 }
      phoneElms.size < 4000
    }
    assertResult(true) {
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
      result.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end2Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the fast way (using findElemOrSelf) took ${end2Ms - start2Ms} ms")

    // Finding the fast way (again)
    val start3Ms = System.currentTimeMillis()
    val foundElm3 = {
      val result = rootElm findElem { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end3Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the fast way (using findElem) took ${end3Ms - start3Ms} ms")

    // Finding the slower way
    val start4Ms = System.currentTimeMillis()
    val foundElm4 = {
      val result = rootElm findTopmostElemsOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end4Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the slower way (using findTopmostElemsOrSelf) took ${end4Ms - start4Ms} ms")

    // Finding the still slower way (in theory)
    val start5Ms = System.currentTimeMillis()
    val foundElm5 = {
      val result = rootElm filterElemsOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end5Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the (theoretically) still slower way (using filterElemsOrSelf) took ${end5Ms - start5Ms} ms")

    // Finding the slowest way (in theory)
    val start6Ms = System.currentTimeMillis()
    val foundElm6 = {
      val result = rootElm.findAllElemsOrSelf filter { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end6Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the (theoretically) slowest way (using findAllElemsOrSelf) took ${end6Ms - start6Ms} ms")
  }

  @Test def testUpdate(): Unit = {
    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    val path = PathBuilder.from(QName("contact") -> 19500, QName("phone") -> 0).build(Scope.Empty)

    val newPhone = "012-34567890"

    val oldPhoneElm: Elem = doc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(false) {
      oldPhoneElm.text == newPhone
    }

    // Update, using a fixed path.

    val start2Ms = System.currentTimeMillis()
    val updatedDoc: Document = doc.updateElemOrSelf(path) { e =>
      e.withChildren(Vector(Text(newPhone, false)))
    }
    val end2Ms = System.currentTimeMillis()
    logger.info(s"Updating an element in the document took ${end2Ms - start2Ms} ms")

    val newPhoneElm: Elem = updatedDoc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(true) {
      newPhoneElm.text == newPhone
    }

    // Comparing the corresponding resolved elements

    val resolvedElm1: resolved.Elem = resolved.Elem(doc.documentElement)

    val resolvedDocElm = resolved.Elem(doc.documentElement)
    val resolvedElm2: resolved.Elem = resolvedDocElm.updateElemOrSelf(path) { e =>
      e.withChildren(Vector(resolved.Text(newPhone)))
    }

    val resolvedElm3: resolved.Elem = resolved.Elem(updatedDoc.documentElement)

    assertResult(false) {
      resolvedElm1 == resolvedElm2
    }
    assertResult(false) {
      resolvedElm1 == resolvedElm3
    }

    assertResult(true) {
      resolvedElm2 == resolvedElm3
    }
  }

  @Test def testUpdateUsingPaths(): Unit = {
    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    val path = PathBuilder.from(QName("contact") -> 19500, QName("phone") -> 0).build(Scope.Empty)
    // Arbitrarily adding root path as extra (ignored) update path
    val paths = Set(path, Path.Empty)

    val newPhone = "012-34567890"

    val oldPhoneElm: Elem = doc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(false) {
      oldPhoneElm.text == newPhone
    }

    // Update, using a fixed path.

    val start2Ms = System.currentTimeMillis()
    val updatedDoc: Document = doc.updateElemsOrSelf(paths) { (e, p) =>
      if (p == path) e.withChildren(Vector(Text(newPhone, false)))
      else e
    }
    val end2Ms = System.currentTimeMillis()
    logger.info(s"Updating an element in the document (using paths) took ${end2Ms - start2Ms} ms")

    val newPhoneElm: Elem = updatedDoc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(true) {
      newPhoneElm.text == newPhone
    }

    // Comparing the corresponding resolved elements

    val resolvedElm1: resolved.Elem = resolved.Elem(doc.documentElement)

    val resolvedDocElm = resolved.Elem(doc.documentElement)
    val resolvedElm2: resolved.Elem = resolvedDocElm.updateElemsOrSelf(paths) { (e, p) =>
      if (p == path) e.withChildren(Vector(resolved.Text(newPhone)))
      else e
    }

    val resolvedElm3: resolved.Elem = resolved.Elem(updatedDoc.documentElement)

    assertResult(false) {
      resolvedElm1 == resolvedElm2
    }
    assertResult(false) {
      resolvedElm1 == resolvedElm3
    }

    assertResult(true) {
      resolvedElm2 == resolvedElm3
    }
  }

  @Test def testUpdateUsingBulkUpdate(): Unit = {
    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    val path = PathBuilder.from(QName("contact") -> 19500, QName("phone") -> 0).build(Scope.Empty)
    // Arbitrarily adding root path as extra (ignored) update path
    val paths = Set(path, Path.Empty)

    val newPhone = "012-34567890"

    val oldPhoneElm: Elem = doc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(false) {
      oldPhoneElm.text == newPhone
    }

    // Update, using a fixed path.

    val start2Ms = System.currentTimeMillis()
    val newDocElem = doc.documentElement updateTopmostElemsWithNodeSeq { (elem, p) =>
      if (p == path) Some(Vector(elem.withChildren(Vector(Text(newPhone, false))))) else None
    }
    val updatedDoc: Document = doc.withDocumentElement(newDocElem)
    val end2Ms = System.currentTimeMillis()
    logger.info(s"Updating an element in the document (using bulk updates) took ${end2Ms - start2Ms} ms")

    val newPhoneElm: Elem = updatedDoc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(true) {
      newPhoneElm.text == newPhone
    }

    // Comparing the corresponding resolved elements

    val resolvedElm1: resolved.Elem = resolved.Elem(doc.documentElement)

    val resolvedDocElm = resolved.Elem(doc.documentElement)
    val resolvedElm2: resolved.Elem = resolvedDocElm updateTopmostElemsWithNodeSeq { (elem, p) =>
      if (p == path) Some(Vector(elem.withChildren(Vector(resolved.Text(newPhone))))) else None
    }

    val resolvedElm3: resolved.Elem = resolved.Elem(updatedDoc.documentElement)

    assertResult(false) {
      resolvedElm1 == resolvedElm2
    }
    assertResult(false) {
      resolvedElm1 == resolvedElm3
    }

    assertResult(true) {
      resolvedElm2 == resolvedElm3
    }
  }

  @Test def testTransform(): Unit = {
    val rootElm = doc.documentElement
    val allElms = rootElm.findAllElemsOrSelf
    assert(allElms.size >= 100000, "Expected at least 100000 elements in the XML")

    val path = PathBuilder.from(QName("contact") -> 19500, QName("phone") -> 0).build(Scope.Empty)

    val newPhone = "012-34567890"

    val oldPhoneElm: Elem = doc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(false) {
      oldPhoneElm.text == newPhone
    }

    // Transform, using a function (updating many phone elements). Note that this is probably inefficient for very large XML documents.

    val start2Ms = System.currentTimeMillis()

    def doUpdate(e: Elem): Elem = e match {
      case e if (e.localName == "phone") && (e.text == oldPhoneElm.text) =>
        e.withChildren(Vector(Text(newPhone, false)))
      case e => e
    }

    var updatedDoc: Document = doc.transformElemsOrSelf(doUpdate _)

    val end2Ms = System.currentTimeMillis()
    logger.info(s"Transforming an element in the document (using method transformElemsOrSelf) took ${end2Ms - start2Ms} ms")

    var newPhoneElm: Elem = updatedDoc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(newPhone) {
      newPhoneElm.text
    }

    val start3Ms = System.currentTimeMillis()

    updatedDoc = doc.transformElemsToNodeSeq(e => Vector(doUpdate(e)))

    val end3Ms = System.currentTimeMillis()
    logger.info(s"Transforming an element in the document (using method transformElemsToNodeSeq) took ${end3Ms - start3Ms} ms")

    newPhoneElm = updatedDoc.documentElement.findElemOrSelfByPath(path).getOrElse(sys.error("Expected element at path: " + path))

    assertResult(newPhone) {
      newPhoneElm.text
    }
  }

  @Test def testNavigation(): Unit = {
    doNavigationTest(doc.documentElement, "simple.Elem")
  }

  @Test def testNavigationForIndexedElem(): Unit = {
    val indexedDoc = indexed.Document(doc)

    doNavigationTest(indexedDoc.documentElement, "indexed.Elem")
  }

  @Test def testNavigationForResolvedElem(): Unit = {
    val resolvedElem = resolved.Elem(doc.documentElement)

    doNavigationTest(resolvedElem, "resolved.Elem")
  }

  @Test def testNavigationForDomElem(): Unit = {
    doNavigationTest(DomDocument(domDoc).documentElement, "dom.DomElem")
  }

  @Test def testNavigationForScalaXmlElem(): Unit = {
    doNavigationTest(ScalaXmlElem(scalaElem), "scalaxml.ScalaXmlElem")
  }

  private def doQueryTest[E <: ClarkElemLike.Aux[E]](elm: E, msg: String): Unit = {
    val startMs = System.currentTimeMillis()

    assert(elm.findAllElemsOrSelf.size >= 100000, "Expected at least 100000 elements in the XML")

    assertResult(Set(EName("contacts"), EName("contact"), EName("firstName"), EName("lastName"), EName("email"), EName("phone"))) {
      val result = elm.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    val s = "b" * (2000 + 46)
    val elms1 = elm \\ { e => e.resolvedName == EName("phone") && e.trimmedText == s }
    assert(elms1.size >= 1, s"Expected at least one phone element with text value '${s}'")

    val endMs = System.currentTimeMillis()
    logger.info(s"The test (invoking findAllElemsOrSelf twice, and filterElemsOrSelf once) took ${endMs - startMs} ms ($msg)")
  }

  private def doNavigationTest[E <: ClarkElemLike.Aux[E]](elm: E, msg: String): Unit = {
    val startMs = System.currentTimeMillis()

    val path = PathBuilder.from(QName("contact") -> 19500, QName("phone") -> 0).build(Scope.Empty)

    assertResult(true) {
      elm.findElemOrSelfByPath(path).isDefined
    }

    val otherPath = PathBuilder.from(QName("contact") -> 1000000, QName("phone") -> 0).build(Scope.Empty)

    assertResult(true) {
      elm.findElemOrSelfByPath(otherPath).isEmpty
    }

    val endMs = System.currentTimeMillis()
    logger.info(s"The navigation test (invoking findElemOrSelfByPath twice) took ${endMs - startMs} ms ($msg)")
  }
}
