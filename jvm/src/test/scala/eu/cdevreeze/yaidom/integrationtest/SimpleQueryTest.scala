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

import scala.Vector

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertElem
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml

/**
 * Query test case, testing several example queries.
 *
 * Acknowledgments: The sample XML comes from the excellent book "XQuery", by Priscilla Walmsley (publisher: O'Reilly Media,
 * Ebook ISBN 978-0-596-15924-5). The queries are also "yaidom ports" of several XQuery examples in that book.
 *
 * Clearly the yaidom queries are more verbose than their XQuery counterparts, and yaidom lacks XML types. On the other hand,
 * yaidom leverages Scala (and the JVM), and Scala Collections in particular. Scala yaidom queries tend to be clear and precise.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SimpleQueryTest extends FunSuite {

  test("testVerySimpleProductQuery") {
    val productElems =
      for {
        productElem <- catalogDoc.documentElement \ EName("product")
      } yield productElem

    assertResult(List("WMN", "ACC", "ACC", "MEN")) {
      productElems flatMap (e => e \@ EName("dept"))
    }

    assertResult(catalogDoc.documentElement.findAllChildElems) {
      productElems
    }
  }

  test("testVerySimpleDeptQuery") {
    val depts =
      for {
        productElem <- catalogDoc.documentElement \\ EName("product")
        dept <- productElem \@ EName("dept")
      } yield dept

    assertResult(List("WMN", "ACC", "ACC", "MEN")) {
      depts
    }
  }

  test("testVerySimpleAccProductQuery") {
    val accProductElems =
      for {
        productElem <- catalogDoc.documentElement \ EName("product")
        dept <- productElem \@ EName("dept")
        if dept == "ACC"
      } yield productElem

    assertResult(List("ACC", "ACC")) {
      accProductElems flatMap (e => e \@ EName("dept"))
    }

    assertResult(catalogDoc.documentElement.filterChildElems(e => (e \@ EName("dept")).contains("ACC"))) {
      accProductElems
    }
  }

  test("testSimpleAccProductNameQuery") {
    // Simulating the ordering allowed by XQuery FLWOR expressions.

    val accProductNameElems =
      for {
        productElem <- (catalogDoc.documentElement \ EName("product")) sortBy (e => e.getChildElem(EName("name")).text)
        dept <- productElem \@ EName("dept")
        if dept == "ACC"
      } yield productElem.getChildElem(EName("name"))

    val expectedResult = List(
      convertToElem(<name language="en">Deluxe Travel Bag</name>),
      convertToElem(<name language="en">Floppy Sun Hat</name>))

    assertResult(expectedResult.map(e => resolved.Elem.from(e))) {
      accProductNameElems.map(e => resolved.Elem.from(e))
    }
  }

  test("testSimpleHtmlAccProductNameQuery") {
    // Simulating the ordering and element constructors allowed by XQuery FLWOR expressions.

    import Node._

    val accProductNames =
      for {
        productElem <- (catalogDoc.documentElement \ EName("product")) sortBy (e => e.getChildElem(EName("name")).text)
        dept <- productElem \@ EName("dept")
        if dept == "ACC"
      } yield productElem.getChildElem(EName("name")).text

    val accProductUlElem =
      elem(
        qname = QName("ul"),
        scope = Scope.Empty,
        children = accProductNames map { nm => textElem(QName("li"), Scope.Empty, nm) })

    val expectedResult = {
      val xml =
        <ul>
          <li>Deluxe Travel Bag</li>
          <li>Floppy Sun Hat</li>
        </ul>
      convertToElem(xml)
    }

    assertResult(resolved.Elem.from(expectedResult).removeAllInterElementWhitespace) {
      resolved.Elem.from(accProductUlElem).removeAllInterElementWhitespace
    }
  }

  test("testReasonablySimpleHtmlAccProductNameQuery") {
    // Simulating the ordering and direct constructors allowed by XQuery FLWOR expressions.

    import Node._

    val accProductElems =
      for {
        productElem <- (catalogDoc.documentElement \ EName("product")) sortBy (e => e.getChildElem(EName("name")).text)
        dept <- productElem \@ EName("dept")
        if dept == "ACC"
      } yield productElem

    val accProductUlElem =
      elem(
        qname = QName("ul"),
        attributes = Vector(QName("type") -> "square"),
        scope = Scope.Empty,
        children = accProductElems map { e =>
          val dept = (e \@ EName("dept")).getOrElse("")
          val productName = e.getChildElem(EName("name")).text

          textElem(
            qname = QName("li"),
            attributes = Vector(QName("class") -> dept),
            scope = Scope.Empty,
            txt = productName)
        })

    val expectedResult = {
      val xml =
        <ul type="square">
          <li class="ACC">Deluxe Travel Bag</li>
          <li class="ACC">Floppy Sun Hat</li>
        </ul>
      convertToElem(xml)
    }

    assertResult(resolved.Elem.from(expectedResult).removeAllInterElementWhitespace) {
      resolved.Elem.from(accProductUlElem).removeAllInterElementWhitespace
    }
  }

  test("testRewrittenHtmlAccProductNameQuery") {
    // Same as above, but rewritten using Scala XML literals.

    val catalogScalaXmlElem = new scalaxml.ScalaXmlElem(convertElem(catalogDoc.documentElement))

    val accProductScalaXmlElems =
      for {
        productElem <- (catalogScalaXmlElem \ EName("product")) sortBy (e => e.getChildElem(EName("name")).text)
        dept <- productElem \@ EName("dept")
        if dept == "ACC"
      } yield productElem

    val accProductUlElem: Elem = {
      val xml =
        <ul type="square">{
          accProductScalaXmlElems map { e =>
            val dept = (e \@ EName("dept")).getOrElse("")
            val productName = e.getChildElem(EName("name")).text

            <li class={ dept }>{ productName }</li>
          }
        }</ul>

      convertToElem(xml)
    }

    val expectedResult = {
      val xml =
        <ul type="square">
          <li class="ACC">Deluxe Travel Bag</li>
          <li class="ACC">Floppy Sun Hat</li>
        </ul>
      convertToElem(xml)
    }

    assertResult(resolved.Elem.from(expectedResult).removeAllInterElementWhitespace) {
      resolved.Elem.from(accProductUlElem).removeAllInterElementWhitespace
    }
  }

  test("testSimpleItemQuery") {
    // Simple "join"

    val itemElems =
      for {
        itemElem <- orderDoc.documentElement \\ EName("item")
        num <- (itemElem \@ EName("num")).toSeq
        productElem <- catalogDoc.documentElement \\! EName("product")
        numberElem <- productElem \ EName("number")
        if numberElem.text == num
        nameElem <- productElem \ EName("name")
      } yield {
        val xml =
          <item num={ itemElem.attribute(EName("num")) } name={ nameElem.text } quan={ itemElem.attribute(EName("quantity")) }/>
        convertToElem(xml)
      }

    val expectedResult = {
      List(
        <item num="557" name="Fleece Pullover" quan="1"/>,
        <item num="563" name="Floppy Sun Hat" quan="1"/>,
        <item num="443" name="Deluxe Travel Bag" quan="2"/>,
        <item num="784" name="Cotton Dress Shirt" quan="1"/>,
        <item num="784" name="Cotton Dress Shirt" quan="1"/>,
        <item num="557" name="Fleece Pullover" quan="1"/>) map (e => convertToElem(e))
    }

    assertResult(expectedResult.map(e => resolved.Elem.from(e).removeAllInterElementWhitespace)) {
      itemElems.map(e => resolved.Elem.from(e).removeAllInterElementWhitespace)
    }
  }

  test("testSimpleAggregationQuery") {
    // Simple "aggregation"

    val departmentElems =
      for {
        dept <- (orderDoc.documentElement \\ EName("item") flatMap (_ \@ EName("dept"))).distinct.sorted
        deptItems = orderDoc.documentElement \\ { e =>
          e.resolvedName == EName("item") && e.attribute(EName("dept")) == dept
        }
      } yield {
        val totQuantity = deptItems.map(_.attribute(EName("quantity")).toInt).sum

        val xml =
          <department name={ dept } totQuantity={ totQuantity.toString }/>
        convertToElem(xml)
      }

    val expectedResult = {
      List(
        <department name="ACC" totQuantity="3"/>,
        <department name="MEN" totQuantity="2"/>,
        <department name="WMN" totQuantity="2"/>) map (e => convertToElem(e))
    }

    assertResult(expectedResult.map(e => resolved.Elem.from(e).removeAllInterElementWhitespace)) {
      departmentElems.map(e => resolved.Elem.from(e).removeAllInterElementWhitespace)
    }
  }

  private val catalogDoc: Document = {
    val xml =
      <catalog>
        <product dept="WMN">
          <number>557</number>
          <name language="en">Fleece Pullover</name>
          <colorChoices>navy black</colorChoices>
        </product>
        <product dept="ACC">
          <number>563</number>
          <name language="en">Floppy Sun Hat</name>
        </product>
        <product dept="ACC">
          <number>443</number>
          <name language="en">Deluxe Travel Bag</name>
        </product>
        <product dept="MEN">
          <number>784</number>
          <name language="en">Cotton Dress Shirt</name>
          <colorChoices>white gray</colorChoices>
          <desc>Our <i>favorite</i> shirt!</desc>
        </product>
      </catalog>

    Document(convertToElem(xml))
  }

  private val orderDoc: Document = {
    val xml =
      <order num="00299432" date="2006-09-15" cust="0221A">
        <item dept="WMN" num="557" quantity="1" color="navy"/>
        <item dept="ACC" num="563" quantity="1"/>
        <item dept="ACC" num="443" quantity="2"/>
        <item dept="MEN" num="784" quantity="1" color="white"/>
        <item dept="MEN" num="784" quantity="1" color="gray"/>
        <item dept="WMN" num="557" quantity="1" color="black"/>
      </order>

    Document(convertToElem(xml))
  }
}
