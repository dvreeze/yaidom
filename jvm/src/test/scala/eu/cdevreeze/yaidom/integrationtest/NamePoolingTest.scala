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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.ENameProvider
import eu.cdevreeze.yaidom.core.QNameProvider
import eu.cdevreeze.yaidom.core.jvm.CaffeineENameProvider
import eu.cdevreeze.yaidom.core.jvm.CaffeineQNameProvider
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi

/**
 * Name pooling test, using Google Guava cache based EName and QName providers.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class NamePoolingTest extends FunSuite {

  import NamePoolingTest._
  import ClarkElemApi._

  test("testNamePooling") {
    ENameProvider.globalENameProvider.become(enameProvider)
    QNameProvider.globalQNameProvider.become(qnameProvider)

    val docParser = DocumentParserUsingDom.newInstance()

    val is = classOf[NamePoolingTest].getResourceAsStream("gaap.xsd")

    val doc: Document = docParser.parse(is)

    val xsElementElems = doc.documentElement \\ withLocalName("element")

    require(xsElementElems.size >= 200)

    assertResult(true) {
      println(s"EName cache hitCount: ${enameCache.stats().hitCount()}")
      println(s"EName cache missCount: ${enameCache.stats().missCount()}")

      enameCache.stats().hitCount() >= 1000
    }

    assertResult(true) {
      println(s"QName cache hitCount: ${qnameCache.stats().hitCount()}")
      println(s"QName cache missCount: ${qnameCache.stats().missCount()}")

      qnameCache.stats().hitCount() >= 1000
    }

    ENameProvider.globalENameProvider.reset()
    QNameProvider.globalQNameProvider.reset()
  }
}

object NamePoolingTest {

  val enameCache = CaffeineENameProvider.createCache(1000, true)
  val enameProvider = new CaffeineENameProvider(enameCache)

  val qnameCache = CaffeineQNameProvider.createCache(1000, true)
  val qnameProvider = new CaffeineQNameProvider(qnameCache)
}
