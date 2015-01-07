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

package eu.cdevreeze.yaidom.queryapitests

import scala.Vector

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node

/**
 * Query test, using examples from http://www.w3.org/TR/xquery-30-use-cases that show uses cases for XQuery 3.0.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractXQuery3UseCasesTest extends Suite {

  type E <: ScopedElemLike[E] with IsNavigable[E]

  @Test def testQ1(): Unit = {
    require(salesElem.localName == "sales")

    val salesRecords = salesElem \ withLocalName("record")

    def productName(salesRecord: E): String = {
      require(salesRecord.localName == "record")
      salesRecord.getChildElem(withLocalName("product-name")).text
    }

    val groupedSalesRecords = salesRecords.groupBy(e => productName(e)).toVector.sortBy(_._1)

    val scope = Scope.Empty

    val productResults = groupedSalesRecords map {
      case (productName, salesRecordGroup) =>
        import Node._
        val sum = salesRecordGroup.map(_.getChildElem(withLocalName("qty")).text.toInt).sum
        textElem(QName("product"), Vector(QName("name") -> productName), scope, sum.toString)
    }

    val salesResult = {
      import Node._
      elem(QName("sales-qty-by-product"), scope, productResults)
    }

    val result = fromSimpleElem(salesResult)

    val expectedResult = {
      import Node._

      elem(QName("sales-qty-by-product"), scope, Vector(
        textElem(QName("product"), Vector(QName("name") -> "blender"), scope, "250"),
        textElem(QName("product"), Vector(QName("name") -> "broiler"), scope, "20"),
        textElem(QName("product"), Vector(QName("name") -> "shirt"), scope, "10"),
        textElem(QName("product"), Vector(QName("name") -> "socks"), scope, "510"),
        textElem(QName("product"), Vector(QName("name") -> "toaster"), scope, "200")))
    }

    assertResult(eu.cdevreeze.yaidom.resolved.Elem(expectedResult).removeAllInterElementWhitespace) {
      toResolvedElem(result).removeAllInterElementWhitespace
    }
  }

  @Test def testQ2(): Unit = {
    require(productsElem.localName == "products")
    require(salesElem.localName == "sales")
    require(storesElem.localName == "stores")

    val salesRecords = salesElem \ withLocalName("record")

    def findState(salesRecord: E): Option[String] = {
      require(salesRecord.localName == "record")
      val storeNumberElemOption = storesElem findChildElem { e =>
        e.localName == "store" &&
          e.getChildElem(withLocalName("store-number")).text == salesRecord.getChildElem(withLocalName("store-number")).text
      }
      storeNumberElemOption.map(_.getChildElem(withLocalName("state")).text)
    }

    def findCategory(salesRecord: E): Option[String] = {
      require(salesRecord.localName == "record")
      val productElemOption = productsElem findChildElem { e =>
        e.localName == "product" &&
          e.getChildElem(withLocalName("name")).text == salesRecord.getChildElem(withLocalName("product-name")).text
      }
      productElemOption.map(_.getChildElem(withLocalName("category")).text)
    }

    val groupedSalesRecords =
      salesRecords.groupBy(e => (findState(e).get, findCategory(e).get)).toVector.sortBy(_._1)

    val scope = Scope.Empty

    val groupResults = groupedSalesRecords map {
      case ((state, category), salesRecordGroup) =>
        import Node._
        val sum = salesRecordGroup.map(_.getChildElem(withLocalName("qty")).text.toInt).sum

        elem(QName("group"), scope, Vector(
          textElem(QName("state"), scope, state),
          textElem(QName("category"), scope, category),
          textElem(QName("total-qty"), scope, sum.toString)))
    }

    val salesResult = {
      import Node._
      elem(QName("result"), scope, groupResults)
    }

    val result = fromSimpleElem(salesResult)

    def group(state: String, category: String, quantity: Int): Elem = {
      import Node._

      elem(QName("group"), scope, Vector(
        textElem(QName("state"), scope, state),
        textElem(QName("category"), scope, category),
        textElem(QName("total-qty"), scope, quantity.toString)))
    }

    val expectedResult = {
      import Node._

      elem(QName("result"), scope, Vector(
        group("CA", "clothes", 510),
        group("CA", "kitchen", 170),
        group("MA", "clothes", 10),
        group("MA", "kitchen", 300)))
    }

    assertResult(eu.cdevreeze.yaidom.resolved.Elem(expectedResult).removeAllInterElementWhitespace) {
      toResolvedElem(result).removeAllInterElementWhitespace
    }
  }

  @Test def testQ3(): Unit = {
    require(productsElem.localName == "products")
    require(salesElem.localName == "sales")
    require(storesElem.localName == "stores")

    val salesRecords = salesElem \ withLocalName("record")

    def findState(salesRecord: E): Option[String] = {
      require(salesRecord.localName == "record")
      val storeNumberElemOption = storesElem findChildElem { e =>
        e.localName == "store" &&
          e.getChildElem(withLocalName("store-number")).text == salesRecord.getChildElem(withLocalName("store-number")).text
      }
      storeNumberElemOption.map(_.getChildElem(withLocalName("state")).text)
    }

    def findProductElem(salesRecord: E): Option[E] = {
      require(salesRecord.localName == "record")
      val productElemOption = productsElem findChildElem { e =>
        e.localName == "product" &&
          e.getChildElem(withLocalName("name")).text == salesRecord.getChildElem(withLocalName("product-name")).text
      }
      productElemOption
    }

    def findCategory(salesRecord: E): Option[String] = {
      findProductElem(salesRecord).map(_.getChildElem(withLocalName("category")).text)
    }

    val groupedSalesRecords =
      salesRecords.groupBy(e => (findState(e).get, findCategory(e).get)).toVector.sortBy(_._1)

    val scope = Scope.Empty

    val groupResults = groupedSalesRecords map {
      case ((state, category), salesRecordGroup) =>
        import Node._
        val qtys = salesRecordGroup.map(_.getChildElem(withLocalName("qty")).text.toInt)
        val prices = salesRecordGroup.map(e => findProductElem(e).get.getChildElem(withLocalName("price")).text.toInt)
        val revenues = qtys.zip(prices) map { case (q, p) => q * p }

        val revenue = revenues.sum

        elem(QName("group"), scope, Vector(
          textElem(QName("state"), scope, state),
          textElem(QName("category"), scope, category),
          textElem(QName("total-revenue"), scope, revenue.toString)))
    }

    val salesResult = {
      import Node._
      elem(QName("result"), scope, groupResults)
    }

    val result = fromSimpleElem(salesResult)

    def group(state: String, category: String, revenue: Int): Elem = {
      import Node._

      elem(QName("group"), scope, Vector(
        textElem(QName("state"), scope, state),
        textElem(QName("category"), scope, category),
        textElem(QName("total-revenue"), scope, revenue.toString)))
    }

    val expectedResult = {
      import Node._

      elem(QName("result"), scope, Vector(
        group("CA", "clothes", 2550),
        group("CA", "kitchen", 6500),
        group("MA", "clothes", 100),
        group("MA", "kitchen", 14000)))
    }

    assertResult(eu.cdevreeze.yaidom.resolved.Elem(expectedResult).removeAllInterElementWhitespace) {
      toResolvedElem(result).removeAllInterElementWhitespace
    }
  }

  protected val productsElem: E

  protected val salesElem: E

  protected val storesElem: E

  protected def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem

  protected def fromSimpleElem(elem: Elem): E
}
