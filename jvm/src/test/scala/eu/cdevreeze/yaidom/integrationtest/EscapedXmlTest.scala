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

import java.io.StringReader

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * Escaped XML within XML test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class EscapedXmlTest extends FunSuite {

  test("testRoundtripEscapedInnerXml") {
    val docPrinter = DocumentPrinterUsingDom.newInstance()

    val docParser = DocumentParserUsingDom.newInstance()

    import simple.Node._

    val scope = Scope.Empty

    val text1 = "This text contains no so-called predefined entities"
    val text2 = "This text does contain some of the 5 predefined entities: <, >, &"

    // Build the inner XML DOM tree, which will introduce the predefined entities for text2, once
    // we serialize the DOM tree

    val innerXmlElem =
      emptyElem(QName("ul"), scope).
        plusChild(textElem(QName("li"), scope, text1)).
        plusChild(textElem(QName("li"), scope, text2))

    val innerXmlString = docPrinter.print(innerXmlElem)

    assertResult(true) {
      innerXmlString.contains("""entities: &lt;, &gt;, &amp;""")
    }

    val outerXmlElem =
      emptyElem(QName("report"), scope).
        plusChild(textElem(QName("item"), scope, innerXmlString.trim))

    val outerXmlString = docPrinter.print(outerXmlElem)

    assertResult(true) {
      outerXmlString.contains("""&lt;li&gt;This text """)
    }
    assertResult(true) {
      outerXmlString.contains("""entities: &amp;lt;, &amp;gt;, &amp;amp;""")
    }

    // Let's deserialize and compare the results with the original

    val outerXmlElem2 =
      docParser.parse(new InputSource(new StringReader(outerXmlString))).documentElement

    def emptyItemElem(e: simple.Elem): simple.Elem = {
      if (e.localName == "item") e.withChildren(Vector()) else e
    }

    assertResult(resolved.Elem(outerXmlElem.transformElems(emptyItemElem)).removeAllInterElementWhitespace) {
      resolved.Elem(outerXmlElem2.transformElems(emptyItemElem).removeAllInterElementWhitespace)
    }

    val itemElem = outerXmlElem2.findElem(_.localName == "item").get

    val innerXmlElem2 =
      docParser.parse(new InputSource(new StringReader(itemElem.text))).documentElement

    assertResult(List(text1, text2)) {
      innerXmlElem.filterElems(_.localName == "li").map(_.text)
    }

    assertResult(resolved.Elem(innerXmlElem).removeAllInterElementWhitespace) {
      resolved.Elem(innerXmlElem2).removeAllInterElementWhitespace
    }
  }
}
