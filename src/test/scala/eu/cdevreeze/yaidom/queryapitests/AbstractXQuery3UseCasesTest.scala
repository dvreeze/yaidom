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
import scala.collection.immutable

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
 * Note that using yaidom, like when using Scala in general, it is needed to think about the efficiency of the
 * query algorithm. The upside is that it is clear from the code how efficient the query is. Moreover, chances are
 * the XQuery is not really accessible to non-programmmers in the first place.
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

    val salesRecordsGroupedByProduct = salesRecords.groupBy(e => productName(e)).toVector.sortBy(_._1)

    val scope = Scope.Empty

    val productResults = salesRecordsGroupedByProduct map {
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

    // Build Maps for store and product elements, for efficient lookups

    val storeElemsByStoreNumber: Map[String, E] = {
      val result = storesElem.filterChildElems(withLocalName("store")) map { elem =>
        val storeNumber = elem.getChildElem(withLocalName("store-number")).text
        (storeNumber -> elem)
      }
      result.toMap
    }

    val productElemsByName: Map[String, E] = {
      val result = productsElem.filterChildElems(withLocalName("product")) map { elem =>
        val productName = elem.getChildElem(withLocalName("name")).text
        (productName -> elem)
      }
      result.toMap
    }

    // Use the Maps above

    def findState(salesRecord: E): Option[String] = {
      require(salesRecord.localName == "record")
      val storeNumber = salesRecord.getChildElem(withLocalName("store-number")).text
      storeElemsByStoreNumber.get(storeNumber).map(_.getChildElem(withLocalName("state")).text)
    }

    def findCategory(salesRecord: E): Option[String] = {
      require(salesRecord.localName == "record")
      val productName = salesRecord.getChildElem(withLocalName("product-name")).text
      productElemsByName.get(productName).map(_.getChildElem(withLocalName("category")).text)
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

    // Build Maps for store and product elements, for efficient lookups

    val storeElemsByStoreNumber: Map[String, E] = {
      val result = storesElem.filterChildElems(withLocalName("store")) map { elem =>
        val storeNumber = elem.getChildElem(withLocalName("store-number")).text
        (storeNumber -> elem)
      }
      result.toMap
    }

    val productElemsByName: Map[String, E] = {
      val result = productsElem.filterChildElems(withLocalName("product")) map { elem =>
        val productName = elem.getChildElem(withLocalName("name")).text
        (productName -> elem)
      }
      result.toMap
    }

    // Convenience method

    def productName(salesRecord: E): String = {
      require(salesRecord.localName == "record")
      salesRecord.getChildElem(withLocalName("product-name")).text
    }

    // Use the Maps above

    def findState(salesRecord: E): Option[String] = {
      require(salesRecord.localName == "record")
      val storeNumber = salesRecord.getChildElem(withLocalName("store-number")).text
      storeElemsByStoreNumber.get(storeNumber).map(_.getChildElem(withLocalName("state")).text)
    }

    def findCategory(salesRecord: E): Option[String] = {
      require(salesRecord.localName == "record")
      val productName = salesRecord.getChildElem(withLocalName("product-name")).text
      productElemsByName.get(productName).map(_.getChildElem(withLocalName("category")).text)
    }

    val groupedSalesRecords =
      salesRecords.groupBy(e => (findState(e).get, findCategory(e).get)).toVector.sortBy(_._1)

    val scope = Scope.Empty

    val groupResults = groupedSalesRecords map {
      case ((state, category), salesRecordGroup) =>
        import Node._
        val qtys = salesRecordGroup.map(_.getChildElem(withLocalName("qty")).text.toInt)
        val prices =
          salesRecordGroup.map(e => productElemsByName(productName(e)).getChildElem(withLocalName("price")).text.toInt)
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

  /**
   * See http://xmllondon.com/2014/xmllondon-2014-proceedings.pdf, on XML processing in Scala.
   */
  @Test def testQ4(): Unit = {
    import Node._

    require(productsElem.localName == "products")
    require(salesElem.localName == "sales")
    require(storesElem.localName == "stores")

    val allStoresByState: Vector[(String, immutable.IndexedSeq[E])] =
      storesElem.filterChildElems(withLocalName("store")).groupBy(_.getChildElem(withLocalName("state")).text).toVector.sortBy(_._1)

    val allProductsByCategory: Vector[(String, immutable.IndexedSeq[E])] =
      productsElem.filterChildElems(withLocalName("product")).groupBy(_.getChildElem(withLocalName("category")).text).toVector.sortBy(_._1)

    val allSalesByProduct: Map[String, immutable.IndexedSeq[E]] =
      salesElem.filterChildElems(withLocalName("record")).groupBy(_.getChildElem(withLocalName("product-name")).text)

    val scope = Scope.Empty

    val unfilteredResultElem =
      emptyElem(QName("result"), scope) withChildren {
        for {
          (state, stateStores) <- allStoresByState
          storeNumbers = stateStores.flatMap(_ \ withLocalName("store-number")).map(_.text).toSet
        } yield {
          emptyElem(QName("state"), Vector(QName("name") -> state), scope) withChildren {
            for {
              (category, products) <- allProductsByCategory
              productRecords = allSalesByProduct.filterKeys(products.flatMap(_ \ withLocalName("name")).map(_.text).toSet)
            } yield {
              emptyElem(QName("category"), Vector(QName("name") -> category), scope) withChildren {
                for {
                  (productName, productSales) <- productRecords.toVector.sortBy(_._1)
                  filteredSales = productSales.filter(e => storeNumbers(e.getChildElem(withLocalName("store-number")).text))
                  if !filteredSales.isEmpty
                  totalQty = filteredSales.flatMap(_ \ withLocalName("qty")).map(_.text.toInt).sum
                } yield {
                  emptyElem(
                    QName("product"),
                    Vector(QName("name") -> productName, QName("total-qty") -> totalQty.toString),
                    scope)
                }
              }
            }
          }
        }
      }
    val resultElem = unfilteredResultElem transformChildElemsToNodeSeq {
      case e if e.localName == "state" && e.filterElems(withLocalName("product")).isEmpty => Vector()
      case e => Vector(e)
    }

    val result = fromSimpleElem(resultElem)

    // Compare with expected result

    val expectedResult = {
      import Node._

      elem(QName("result"), scope, Vector(
        elem(QName("state"), Vector(QName("name") -> "CA"), scope, Vector(
          elem(QName("category"), Vector(QName("name") -> "clothes"), scope, Vector(
            emptyElem(QName("product"), Vector(QName("name") -> "socks", QName("total-qty") -> "510"), scope))),
          elem(QName("category"), Vector(QName("name") -> "kitchen"), scope, Vector(
            emptyElem(QName("product"), Vector(QName("name") -> "broiler", QName("total-qty") -> "20"), scope),
            emptyElem(QName("product"), Vector(QName("name") -> "toaster", QName("total-qty") -> "150"), scope))))),
        elem(QName("state"), Vector(QName("name") -> "MA"), scope, Vector(
          elem(QName("category"), Vector(QName("name") -> "clothes"), scope, Vector(
            emptyElem(QName("product"), Vector(QName("name") -> "shirt", QName("total-qty") -> "10"), scope))),
          elem(QName("category"), Vector(QName("name") -> "kitchen"), scope, Vector(
            emptyElem(QName("product"), Vector(QName("name") -> "blender", QName("total-qty") -> "250"), scope),
            emptyElem(QName("product"), Vector(QName("name") -> "toaster", QName("total-qty") -> "50"), scope)))))))
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
