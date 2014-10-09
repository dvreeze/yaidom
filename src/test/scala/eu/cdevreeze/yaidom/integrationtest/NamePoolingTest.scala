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

package eu.cdevreeze.yaidom
package integrationtest

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingDom
import com.google.common.cache._

/**
 * Name pooling test, using Google Guava cache based EName and QName providers.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class NamePoolingTest extends Suite {

  import NamePoolingTest._
  import ElemApi._

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @Test def testNamePooling(): Unit = {
    ENameProvider.globalENameProvider.become(new GuavaENameProvider(1000))
    QNameProvider.globalQNameProvider.become(new GuavaQNameProvider(1000))

    val docParser = DocumentParserUsingDom.newInstance()

    val is = classOf[NamePoolingTest].getResourceAsStream("gaap.xsd")

    val doc: Document = docParser.parse(is)

    val xsElementElems = doc.documentElement \\ withLocalName("element")

    require(xsElementElems.size >= 50)

    assertResult(true) {
      val firstXsElementElem = xsElementElems(0)

      xsElementElems.drop(1).filter(e => e.resolvedName eq firstXsElementElem.resolvedName).size >= 30
    }

    assertResult(true) {
      val firstXsElementElem = xsElementElems(0)

      xsElementElems.drop(1).filter(e => e.qname eq firstXsElementElem.qname).size >= 30
    }

    // Reset EName/QName providers

    ENameProvider.globalENameProvider.reset()
    QNameProvider.globalQNameProvider.reset()

    val doc2: Document = docParser.parse(classOf[NamePoolingTest].getResourceAsStream("gaap.xsd"))

    val xsElementElems2 = doc2.documentElement \\ withLocalName("element")

    require(xsElementElems2.size >= 50)

    assertResult(true) {
      val firstXsElementElem = xsElementElems2(0)

      xsElementElems2.drop(1).filter(e => e.resolvedName eq firstXsElementElem.resolvedName).isEmpty
    }

    assertResult(true) {
      val firstXsElementElem = xsElementElems2(0)

      xsElementElems2.drop(1).filter(e => e.qname eq firstXsElementElem.qname).isEmpty
    }
  }
}

object NamePoolingTest {

  final class GuavaENameProvider(val cacheSize: Int) extends ENameProvider {

    private val cache: LoadingCache[(String, String), EName] = {
      val cacheLoader = new CacheLoader[(String, String), EName] {
        def load(key: (String, String)): EName =
          if (key._1.isEmpty) EName(None, key._2) else EName(Some(key._1), key._2)
      }
      val result = CacheBuilder.newBuilder().maximumSize(cacheSize).build(cacheLoader)
      result
    }

    def getEName(namespaceUriOption: Option[String], localPart: String): EName =
      cache.get((namespaceUriOption.getOrElse(""), localPart))

    def getEName(namespaceUri: String, localPart: String): EName =
      cache.get((namespaceUri, localPart))

    def getNoNsEName(localPart: String): EName =
      cache.get(("", localPart))

    def parseEName(s: String): EName = {
      val ename = EName.parse(s)
      cache.get((ename.namespaceUriOption.getOrElse(""), ename.localPart))
    }
  }

  final class GuavaQNameProvider(val cacheSize: Int) extends QNameProvider {

    private val cache: LoadingCache[(String, String), QName] = {
      val cacheLoader = new CacheLoader[(String, String), QName] {
        def load(key: (String, String)): QName =
          if (key._1.isEmpty) QName(None, key._2) else QName(Some(key._1), key._2)
      }
      val result = CacheBuilder.newBuilder().maximumSize(cacheSize).build(cacheLoader)
      result
    }

    def getQName(prefixOption: Option[String], localPart: String): QName =
      cache.get((prefixOption.getOrElse(""), localPart))

    def getQName(prefix: String, localPart: String): QName =
      cache.get((prefix, localPart))

    def getUnprefixedQName(localPart: String): QName =
      cache.get(("", localPart))

    def parseQName(s: String): QName = {
      val qname = QName.parse(s)
      cache.get((qname.prefixOption.getOrElse(""), qname.localPart))
    }
  }
}
