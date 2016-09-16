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

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ FunSuite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import org.ccil.cowan.tagsoup.jaxp.SAXFactoryImpl
import eu.cdevreeze.yaidom.queryapi.HasENameApi._
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions._
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.resolved

/**
 * HTML support test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class HtmlTest extends FunSuite with BeforeAndAfterAll {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  test("testParseHtml") {
    val docParser = DocumentParserUsingSax.newInstance(new SAXFactoryImpl)

    val doc = docParser.parse(classOf[HtmlTest].getResourceAsStream("badHtmlExample.html"))

    assertResult(4) {
      (doc.documentElement \\! withNoNsEName("li")).size
    }

    val firstLiOption = (doc.documentElement \\! withNoNsEName("li")).headOption

    assertResult(true) {
      firstLiOption.isDefined
    }

    val expectedLi = convertToElem(<li><a href="index.html">Home page</a></li>)

    def keepHref(elem: Elem): Elem = {
      require(elem.localName == "li")
      elem transformChildElems { e =>
        e.copy(attributes = e.attributes.toMap.filterKeys(Set(QName("href"))).toVector)
      }
    }

    assertResult(resolved.Elem(expectedLi).removeAllInterElementWhitespace) {
      resolved.Elem(keepHref(firstLiOption.get)).removeAllInterElementWhitespace
    }
  }

  test("testRoundtripHtml") {
    val docParser = DocumentParserUsingSax.newInstance(new SAXFactoryImpl)

    val doc = docParser.parse(classOf[HtmlTest].getResourceAsStream("badHtmlExample.html"))

    val docPrinter = DocumentPrinterUsingDom.newInstance().withTransformerCreatorForHtml

    val htmlString = docPrinter.print(doc)

    logger.info(s"HTML after parsing and printing:\n$htmlString")

    val doc2 = docParser.parse(new jio.ByteArrayInputStream(htmlString.getBytes("UTF-8")))

    assertResult(4) {
      (doc2.documentElement \\! withNoNsEName("li")).size
    }

    assertResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace.findTopmostElemsOrSelf(withNoNsEName("li"))) {
      resolved.Elem(doc2.documentElement).removeAllInterElementWhitespace.findTopmostElemsOrSelf(withNoNsEName("li"))
    }
  }
}
