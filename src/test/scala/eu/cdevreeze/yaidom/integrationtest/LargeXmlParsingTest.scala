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
import java.{ util => jutil }

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.resolved

/**
 * Large XML parsing test case.
 *
 * Acknowledgments: The large XML files come from http://javakata6425.appspot.com/#!goToPageIIIarticleIIIOptimally%20parse%20humongous%20XML%20files%20with%20vanilla%20Java.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class LargeXmlParsingTest extends FunSuite with BeforeAndAfterAll {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @volatile private var xmlBytes: Array[Byte] = _

  val enames =
    Set(EName("contacts"), EName("contact"), EName("firstName"), EName("lastName"), EName("email"), EName("phone"))
  val qnames = enames.map(en => QName(en.localPart))

  ENameProvider.globalENameProvider.become(new ENameProvider.ENameProviderUsingImmutableCache(enames))
  QNameProvider.globalQNameProvider.become(new QNameProvider.QNameProviderUsingImmutableCache(qnames))

  protected override def beforeAll(): Unit = {
    val zipFileUrl = classOf[LargeXmlParsingTest].getResource("veryBigFile.zip")
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

  test("testParseLargeXmlUsingSax") {
    val parser = DocumentParserUsingSax.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info(s"[testParseLargeXmlUsingSax] Parsing (into a Document) took ${endMs - startMs} ms")

    doQueryTest(doc.documentElement)
  }

  test("testParseLargeXmlIntoResolvedElemUsingSax") {
    val parser = DocumentParserUsingSax.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info(s"[testParseLargeXmlIntoResolvedElemUsingSax] Parsing (into a Document) took ${endMs - startMs} ms")

    val resolvedRoot = resolved.Elem(doc.documentElement)
    doQueryTest(resolvedRoot)

    val emailElms = resolvedRoot findTopmostElems { e => e.localName == "email" } take (10)

    assertResult(10) {
      emailElms.size
    }

    assertResult(doc.documentElement.findAllElemsOrSelf.size) {
      resolvedRoot.findAllElemsOrSelf.size
    }
  }

  test("testParseLargeXmlUsingStax") {
    val parser = DocumentParserUsingStax.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info(s"[testParseLargeXmlUsingStax] Parsing (into a Document) took ${endMs - startMs} ms")

    doQueryTest(doc.documentElement)
  }

  test("testParseLargeXmlUsingDom") {
    val parser = DocumentParserUsingDom.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info(s"[testParseLargeXmlUsingDom] Parsing (into a Document) took ${endMs - startMs} ms")

    doQueryTest(doc.documentElement)
  }

  test("testParseLargeXmlUsingDomLS") {
    val parser = DocumentParserUsingDomLS.newInstance

    val startMs = System.currentTimeMillis()
    val doc = parser.parse(new jio.ByteArrayInputStream(xmlBytes))
    val endMs = System.currentTimeMillis()
    logger.info(s"[testParseLargeXmlUsingDomLS] Parsing (into a Document) took ${endMs - startMs} ms")

    doQueryTest(doc.documentElement)
  }

  private def doQueryTest[E <: ClarkElemLike.Aux[E]](elm: E): Unit = {
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
    logger.info(s"The test (invoking findAllElemsOrSelf twice, and filterElemsOrSelf once) took ${endMs - startMs} ms")
  }
}
