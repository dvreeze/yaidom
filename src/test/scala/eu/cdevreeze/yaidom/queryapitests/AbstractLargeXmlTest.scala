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

package eu.cdevreeze.yaidom.queryapitests

import java.{ io => jio }
import java.{ util => jutil }

import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.PathBuilder
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi

/**
 * Large XML test case.
 *
 * Acknowledgments: The large XML files come from http://javakata6425.appspot.com/#!goToPageIIIarticleIIIOptimally%20parse%20humongous%20XML%20files%20with%20vanilla%20Java.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractLargeXmlTest extends FunSuite with BeforeAndAfterAll {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests")

  type E = BackingElemApi

  type D <: DocumentApi { type DocElemType <: E }

  @volatile private var doc: D = _

  val enames =
    Set(EName("contacts"), EName("contact"), EName("firstName"), EName("lastName"), EName("email"), EName("phone"))
  val qnames = enames.map(en => QName(en.localPart))

  ENameProvider.globalENameProvider.become(new ENameProvider.ENameProviderUsingImmutableCache(enames))
  QNameProvider.globalQNameProvider.become(new QNameProvider.QNameProviderUsingImmutableCache(qnames))

  protected def parseDocument(is: jio.InputStream): D

  protected override def beforeAll(): Unit = {
    val zipFileUrl = classOf[AbstractLargeXmlTest].getResource("/eu/cdevreeze/yaidom/integrationtest/veryBigFile.zip")
    val zipFile = new jutil.zip.ZipFile(new jio.File(zipFileUrl.toURI))

    val zipEntries = zipFile.entries()
    require(zipEntries.hasMoreElements())

    val zipEntry: jutil.zip.ZipEntry = zipEntries.nextElement()

    val is = new jio.BufferedInputStream(zipFile.getInputStream(zipEntry))

    this.doc = parseDocument(is)
  }

  /** A real stress test (disabled by default). When running it, use jvisualvm to check on the JVM behavior */
  ignore("testQueryLargeXmlRepeatedly") {
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

  test("testQueryElem") {
    val startMs = System.currentTimeMillis()

    assert(doc.documentElement.findAllElemsOrSelf.size >= 100000, "Expected at least 100000 elements in the XML")

    assertResult(Set(EName("contacts"), EName("contact"), EName("firstName"), EName("lastName"), EName("email"), EName("phone"))) {
      val result = doc.documentElement.findAllElemsOrSelf map { e => e.resolvedName }
      result.toSet
    }

    val s = "b" * (2000 + 46)
    val elms1 = doc.documentElement \\ { e => e.resolvedName == EName("phone") && e.trimmedText == s }
    assert(elms1.size >= 1, s"Expected at least one phone element with text value '${s}'")

    val endMs = System.currentTimeMillis()
    logger.info(s"The test (invoking findAllElemsOrSelf twice, and filterElemsOrSelf once) took ${endMs - startMs} ms")
  }

  test("testFind") {
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

    {
      val result = rootElm findElemOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end2Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the fast way (using findElemOrSelf) took ${end2Ms - start2Ms} ms")

    // Finding the fast way (again)
    val start3Ms = System.currentTimeMillis()

    {
      val result = rootElm findElem { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end3Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the fast way (using findElem) took ${end3Ms - start3Ms} ms")

    // Finding the slower way
    val start4Ms = System.currentTimeMillis()

    {
      val result = rootElm findTopmostElemsOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end4Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the slower way (using findTopmostElemsOrSelf) took ${end4Ms - start4Ms} ms")

    // Finding the still slower way (in theory)
    val start5Ms = System.currentTimeMillis()

    {
      val result = rootElm filterElemsOrSelf { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end5Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the (theoretically) still slower way (using filterElemsOrSelf) took ${end5Ms - start5Ms} ms")

    // Finding the slowest way (in theory)
    val start6Ms = System.currentTimeMillis()

    {
      val result = rootElm.findAllElemsOrSelf filter { e => e.resolvedName == EName("phone") && e.trimmedText == s }
      result.headOption.getOrElse(sys.error(s"Expected at least one phone element with text value '${s}'"))
    }
    val end6Ms = System.currentTimeMillis()
    logger.info(s"Finding an element the (theoretically) slowest way (using findAllElemsOrSelf) took ${end6Ms - start6Ms} ms")
  }

  test("testNavigation") {
    val startMs = System.currentTimeMillis()
    val rootElm = doc.documentElement

    val path = PathBuilder.from(QName("contact") -> 19500, QName("phone") -> 0).build(Scope.Empty)

    assertResult(true) {
      rootElm.findElemOrSelfByPath(path).isDefined
    }

    val otherPath = PathBuilder.from(QName("contact") -> 1000000, QName("phone") -> 0).build(Scope.Empty)

    assertResult(true) {
      rootElm.findElemOrSelfByPath(otherPath).isEmpty
    }

    val endMs = System.currentTimeMillis()
    logger.info(s"The navigation test (invoking findElemOrSelfByPath twice) took ${endMs - startMs} ms")
  }

  test("testPaths") {
    val startMs = System.currentTimeMillis()
    val rootElm = doc.documentElement

    val allElemsOrSelf = rootElm.findAllElemsOrSelf
    val someElms =
      (Vector(0, 1000, 2000, 3000, 8000, 12000, 18000, 19000) ++ (19300 to 19350).toVector).map(i => allElemsOrSelf(i))
    val someElmPaths = someElms.map(_.path)

    assertResult(someElms) {
      someElmPaths.map(path => rootElm.getElemOrSelfByPath(path))
    }

    val endMs = System.currentTimeMillis()
    logger.info(s"The path retrieval and navigation test took ${endMs - startMs} ms")
  }
}
