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

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi.withLocalName
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.simple.Node
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.immutable

/**
 * Alternative query test, with yaidom queries ported from XQuery examples.
 *
 * Acknowledgments: The XQuery examples come from the book "XQuery by Priscilla Walmsley. Copyright 2007 Priscilla Walmsley, 978-0-596-00634-1".
 * I am grateful for being allowed to use these XQuery examples as inspiration for the yaidom queries in this test.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractAlternativeQueryTest extends AnyFunSuite {

  type E <: ClarkNodes.Elem with ClarkElemApi.Aux[E]

  protected val catalogElem: E

  protected val pricesElem: E

  protected val orderElem: E

  protected final def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem = {
    eu.cdevreeze.yaidom.resolved.Elem.from(elem)
  }

  protected def fromScalaElem(elem: scala.xml.Elem): E

  test("testFindAllCatalogProducts") {
    // See example 1-4.

    require(catalogElem.localName == "catalog")

    val productElems =
      catalogElem.filterChildElems(withLocalName("product"))

    assertResult(List("557", "563", "443", "784")) {
      productElems.map(_.getChildElem(withLocalName("number"))).map(_.text)
    }

    val expectedFirstProd =
      <product dept="WMN">
        <number>557</number>
        <name language="en">Fleece Pullover</name>
        <colorChoices>navy black</colorChoices>
      </product>

    assertResult(
      eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(expectedFirstProd)).removeAllInterElementWhitespace) {
      toResolvedElem(productElems.head).removeAllInterElementWhitespace
    }

    val expectedLastProd =
      <product dept="MEN">
        <number>784</number>
        <name language="en">Cotton Dress Shirt</name>
        <colorChoices>white gray</colorChoices>
        <desc>Our <i>favorite</i> shirt!</desc>
      </product>

    assertResult(
      eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(expectedLastProd)).removeAllInterElementWhitespace) {
      toResolvedElem(productElems.last).removeAllInterElementWhitespace
    }
  }

  test("testFindAllDepartments") {
    // XPath: doc("catalog.xml")/*/product/@dept
    // Or: doc("catalog.xml")//product/@dept

    require(catalogElem.localName == "catalog")

    val depts =
      for {
        productElem <- catalogElem \ withLocalName("product")
        dept <- productElem \@ EName("dept")
      } yield dept

    assertResult(List("WMN", "ACC", "ACC", "MEN")) {
      depts
    }
  }

  test("testFindAllAccProducts") {
    // XPath: doc("catalog.xml")/catalog/product[@dept = "ACC"]

    require(catalogElem.localName == "catalog")

    val productElems =
      catalogElem.filterChildElems { elem =>
        elem.localName == "product" && elem.attributeOption(EName("dept")).contains("ACC")
      }

    assertResult(List("563", "443")) {
      productElems.map(_.getChildElem(withLocalName("number"))).map(_.text)
    }

    val expectedFirstProd =
      <product dept="ACC">
        <number>563</number>
        <name language="en">Floppy Sun Hat</name>
      </product>

    assertResult(
      eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(expectedFirstProd)).removeAllInterElementWhitespace) {
      toResolvedElem(productElems.head).removeAllInterElementWhitespace
    }

    val expectedLastProd =
      <product dept="ACC">
        <number>443</number>
        <name language="en">Deluxe Travel Bag</name>
      </product>

    assertResult(
      eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(expectedLastProd)).removeAllInterElementWhitespace) {
      toResolvedElem(productElems.last).removeAllInterElementWhitespace
    }
  }

  test("testFindSecondProduct") {
    // XPath: doc("catalog.xml")/catalog/product[2]

    require(catalogElem.localName == "catalog")

    val productElem =
      catalogElem.filterChildElems(withLocalName("product")).take(2).last

    val expectedProd =
      <product dept="ACC">
        <number>563</number>
        <name language="en">Floppy Sun Hat</name>
      </product>

    assertResult(eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(expectedProd)).removeAllInterElementWhitespace) {
      toResolvedElem(productElem).removeAllInterElementWhitespace
    }
  }

  test("testFindAllAccProductNames") {
    // See example 1-5.

    require(catalogElem.localName == "catalog")

    val productNameElems =
      for {
        productElem <- catalogElem \ withLocalName("product")
        if (productElem \@ EName("dept")).contains("ACC")
        nameElem <- productElem \ withLocalName("name")
      } yield nameElem

    val sortedProductNameElems = productNameElems.sortBy(e => e.text)

    val expectedNameElems =
      List(<name language="en">Deluxe Travel Bag</name>, <name language="en">Floppy Sun Hat</name>).map(e =>
        eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(e)))

    assertResult(expectedNameElems) {
      sortedProductNameElems.map(e => toResolvedElem(e))
    }
  }

  test("testListAllAccProducts") {
    // See example 1-10.

    require(catalogElem.localName == "catalog")

    val productNameElems: immutable.IndexedSeq[scala.xml.Elem] =
      for {
        productElem <- (catalogElem \ withLocalName("product")).sortBy(e => e.getChildElem(withLocalName("name")).text)
        if (productElem \@ EName("dept")).contains("ACC")
        dept <- productElem \@ EName("dept")
        productName = productElem.getChildElem(withLocalName("name")).text
      } yield <li class={ dept }>{ productName }</li>

    val resultElem: E = fromScalaElem(<ul type="square">
        {
          productNameElems
        }
      </ul>)

    val expectedResult =
      <ul type="square">
        <li class="ACC">Deluxe Travel Bag</li>
        <li class="ACC">Floppy Sun Hat</li>
      </ul>

    assertResult(eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(expectedResult)).removeAllInterElementWhitespace) {
      toResolvedElem(resultElem).removeAllInterElementWhitespace
    }
  }

  test("testJoin") {
    // See example 1-11.

    require(catalogElem.localName == "catalog")
    require(orderElem.localName == "order")

    val itemElems =
      for {
        itemElem <- orderElem \\ withLocalName("item")
        productElem <- catalogElem \\ { elem =>
          elem.localName == "product" &&
          elem.getChildElem(withLocalName("number")).text == itemElem.attribute(EName("num"))
        }
        nameElem <- productElem \ withLocalName("name")
      } yield {
        import Node._

        emptyElem(
          qname = QName("item"),
          scope = Scope.Empty,
          attributes = Vector(
            QName("num") -> itemElem.attribute(EName("num")),
            QName("name") -> nameElem.text,
            QName("quan") -> itemElem.attribute(EName("quantity")))
        )
      }

    val expectedResults =
      Vector(
        <item num="557" name="Fleece Pullover" quan="1"/>,
        <item num="563" name="Floppy Sun Hat" quan="1"/>,
        <item num="443" name="Deluxe Travel Bag" quan="2"/>,
        <item num="784" name="Cotton Dress Shirt" quan="1"/>,
        <item num="784" name="Cotton Dress Shirt" quan="1"/>,
        <item num="557" name="Fleece Pullover" quan="1"/>
      )

    assertResult(expectedResults.map(e =>
      eu.cdevreeze.yaidom.resolved.Elem.from(convertToElem(e)).removeAllInterElementWhitespace)) {
      itemElems.map(e => eu.cdevreeze.yaidom.resolved.Elem.from(e).removeAllInterElementWhitespace)
    }
  }
}
