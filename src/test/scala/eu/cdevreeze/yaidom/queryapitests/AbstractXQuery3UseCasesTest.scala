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
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node

/**
 * Query test, using examples from http://www.w3.org/TR/xquery-30-use-cases that show uses cases for XQuery 3.0.
 *
 * This time, however, namespaces are used, in order to show yaidom's strengths w.r.t. precise namespace support.
 *
 * Note that using yaidom, like when using Scala in general, it is needed to think about the efficiency of the
 * query algorithm. The upside is that it is clear from the code how efficient the query is. Moreover, chances are
 * that XQuery is not really accessible to non-programmmers in the first place.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractXQuery3UseCasesTest extends Suite {

  type E <: ScopedElemLike[E]

  protected val ns = "http://www.w3.org/TR/xquery-30-use-cases/"

  @Test def testQ1(): Unit = {
    import Node._

    require(salesElem.localName == "sales")

    val allSalesByProduct: Vector[(String, immutable.IndexedSeq[E])] =
      salesElem.filterChildElems(withEName(ns, "record")).groupBy(_.getChildElem(withEName(ns, "product-name")).text).toVector.sortBy(_._1)

    val scope = Scope.from("" -> ns)

    val resultElem =
      emptyElem(QName("sales-qty-by-product"), scope) withChildren {
        for {
          (productName, productSales) <- allSalesByProduct
        } yield {
          textElem(
            QName("product"),
            Vector(QName("name") -> productName),
            scope,
            productSales.map(_.getChildElem(withEName(ns, "qty")).text.toInt).sum.toString)
        }
      }

    val result = fromSimpleElem(resultElem)

    // Compare with expected result

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
    import Node._

    require(productsElem.localName == "products")
    require(salesElem.localName == "sales")
    require(storesElem.localName == "stores")

    val storeElemsByStoreNumber: Map[String, E] = {
      val result = storesElem.filterChildElems(withEName(ns, "store")) groupBy { elem =>
        elem.getChildElem(withEName(ns, "store-number")).text
      }
      result.toMap.mapValues(_.head)
    }

    val productElemsByName: Map[String, E] = {
      val result = productsElem.filterChildElems(withEName(ns, "product")) groupBy { elem =>
        elem.getChildElem(withEName(ns, "name")).text
      }
      result.toMap.mapValues(_.head)
    }

    val allSalesByStateAndCategory: Vector[((String, String), immutable.IndexedSeq[E])] = {
      val result =
        salesElem.filterChildElems(withEName(ns, "record")) groupBy { e =>
          val storeNumber = e.getChildElem(withEName(ns, "store-number")).text
          val productName = e.getChildElem(withEName(ns, "product-name")).text

          val state = storeElemsByStoreNumber(storeNumber).getChildElem(withEName(ns, "state")).text
          val category = productElemsByName(productName).getChildElem(withEName(ns, "category")).text

          (state, category)
        }
      result.toVector.sortBy(_._1)
    }

    val scope = Scope.from("" -> ns)

    val resultElem =
      emptyElem(QName("result"), scope) withChildren {
        for {
          ((state, category), salesGroup) <- allSalesByStateAndCategory
        } yield {
          val sum = salesGroup.map(_.getChildElem(withEName(ns, "qty")).text.toInt).sum

          emptyElem(QName("group"), scope).
            plusChild(textElem(QName("state"), scope, state)).
            plusChild(textElem(QName("category"), scope, category)).
            plusChild(textElem(QName("total-qty"), scope, sum.toString))
        }
      }

    val result = fromSimpleElem(resultElem)

    // Compare with expected result

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
    import Node._

    require(productsElem.localName == "products")
    require(salesElem.localName == "sales")
    require(storesElem.localName == "stores")

    val storeElemsByStoreNumber: Map[String, E] = {
      val result = storesElem.filterChildElems(withEName(ns, "store")) groupBy { elem =>
        elem.getChildElem(withEName(ns, "store-number")).text
      }
      result.toMap.mapValues(_.head)
    }

    val productElemsByName: Map[String, E] = {
      val result = productsElem.filterChildElems(withEName(ns, "product")) groupBy { elem =>
        elem.getChildElem(withEName(ns, "name")).text
      }
      result.toMap.mapValues(_.head)
    }

    val allSalesByStateAndCategory: Vector[((String, String), immutable.IndexedSeq[E])] = {
      val result =
        salesElem.filterChildElems(withEName(ns, "record")) groupBy { e =>
          val storeNumber = e.getChildElem(withEName(ns, "store-number")).text
          val productName = e.getChildElem(withEName(ns, "product-name")).text

          val state = storeElemsByStoreNumber(storeNumber).getChildElem(withEName(ns, "state")).text
          val category = productElemsByName(productName).getChildElem(withEName(ns, "category")).text

          (state, category)
        }
      result.toVector.sortBy(_._1)
    }

    val scope = Scope.from("" -> ns)

    val resultElem =
      emptyElem(QName("result"), scope) withChildren {
        for {
          ((state, category), salesGroup) <- allSalesByStateAndCategory
        } yield {
          val qtys = salesGroup.map(_.getChildElem(withEName(ns, "qty")).text.toInt)
          val prices =
            salesGroup.map(e => productElemsByName(e.getChildElem(withEName(ns, "product-name")).text).getChildElem(withEName(ns, "price")).text.toInt)
          val revenues = qtys.zip(prices) map { case (q, p) => q * p }
          val revenue = revenues.sum

          emptyElem(QName("group"), scope).
            plusChild(textElem(QName("state"), scope, state)).
            plusChild(textElem(QName("category"), scope, category)).
            plusChild(textElem(QName("total-revenue"), scope, revenue.toString))
        }
      }

    val result = fromSimpleElem(resultElem)

    // Compare with expected result

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
   *
   * As compared to Scala XML, yaidom regards namespaces as first-class citizens. The Scala XML equivalent of
   * this query may be less verbose, but querying for child/descendant elements on names with namespaces requires
   * querying on local name, followed by filtering on namespace. See also http://stackoverflow.com/questions/20135832/how-to-get-namespaced-nodes-from-scala-xml.
   * That's not first class namespace support.
   *
   * In the Scala XML equivalent, the operations such as groupByOrderBy are not namespace-aware either.
   *
   * So, when querying documents with namespaces in a namespace-aware manner, yaidom's precision and namespace support
   * really start to shine.
   */
  @Test def testQ4(): Unit = {
    import Node._

    require(productsElem.localName == "products")
    require(salesElem.localName == "sales")
    require(storesElem.localName == "stores")

    val allStoresByState: Vector[(String, immutable.IndexedSeq[E])] =
      storesElem.filterChildElems(withEName(ns, "store")).groupBy(_.getChildElem(withEName(ns, "state")).text).toVector.sortBy(_._1)

    val allProductsByCategory: Vector[(String, immutable.IndexedSeq[E])] =
      productsElem.filterChildElems(withEName(ns, "product")).groupBy(_.getChildElem(withEName(ns, "category")).text).toVector.sortBy(_._1)

    val allSalesByProduct: Map[String, immutable.IndexedSeq[E]] =
      salesElem.filterChildElems(withEName(ns, "record")).groupBy(_.getChildElem(withEName(ns, "product-name")).text)

    val scope = Scope.from("" -> ns)

    val unfilteredResultElem =
      emptyElem(QName("result"), scope) withChildren {
        for {
          (state, stateStores) <- allStoresByState
          storeNumbers = stateStores.flatMap(_ \ withEName(ns, "store-number")).map(_.text).toSet
        } yield {
          emptyElem(QName("state"), Vector(QName("name") -> state), scope) withChildren {
            for {
              (category, products) <- allProductsByCategory
              productRecords = allSalesByProduct.filterKeys(products.flatMap(_ \ withEName(ns, "name")).map(_.text).toSet)
            } yield {
              emptyElem(QName("category"), Vector(QName("name") -> category), scope) withChildren {
                for {
                  (productName, productSales) <- productRecords.toVector.sortBy(_._1)
                  filteredSales = productSales.filter(e => storeNumbers(e.getChildElem(withEName(ns, "store-number")).text))
                  if !filteredSales.isEmpty
                  totalQty = filteredSales.flatMap(_ \ withEName(ns, "qty")).map(_.text.toInt).sum
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
      case e if e.localName == "state" && e.filterElems(withEName(ns, "product")).isEmpty => Vector()
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

  @Test def testQ5(): Unit = {
    import Node._

    require(productsElem.localName == "products")
    require(salesElem.localName == "sales")
    require(storesElem.localName == "stores")

    val allSalesByStoreNumber: Vector[(String, immutable.IndexedSeq[E])] =
      salesElem.filterChildElems(withEName(ns, "record")).groupBy(_.getChildElem(withEName(ns, "store-number")).text).toVector.sortBy(_._1)

    val scope = Scope.from("" -> ns)

    val resultElem =
      emptyElem(QName("result"), scope) withChildren {
        for {
          (storeNumber, storeSales) <- allSalesByStoreNumber
        } yield {
          emptyElem(QName("store"), Vector(QName("number") -> storeNumber), scope) withChildren {
            for {
              sales <- storeSales.sortBy(_.getChildElem(withEName(ns, "qty")).text.toInt).reverse
            } yield {
              emptyElem(
                QName("product"),
                Vector(
                  QName("name") -> sales.getChildElem(withEName(ns, "product-name")).text,
                  QName("qty") -> sales.getChildElem(withEName(ns, "qty")).text),
                scope)
            }
          }
        }
      }

    val result = fromSimpleElem(resultElem)

    // Compare with expected result

    val expectedResult = {
      import Node._

      elem(QName("result"), scope, Vector(
        elem(QName("store"), Vector(QName("number") -> "1"), scope, Vector(
          emptyElem(QName("product"), Vector(QName("name") -> "socks", QName("qty") -> "500"), scope),
          emptyElem(QName("product"), Vector(QName("name") -> "broiler", QName("qty") -> "20"), scope))),
        elem(QName("store"), Vector(QName("number") -> "2"), scope, Vector(
          emptyElem(QName("product"), Vector(QName("name") -> "toaster", QName("qty") -> "100"), scope),
          emptyElem(QName("product"), Vector(QName("name") -> "toaster", QName("qty") -> "50"), scope),
          emptyElem(QName("product"), Vector(QName("name") -> "socks", QName("qty") -> "10"), scope))),
        elem(QName("store"), Vector(QName("number") -> "3"), scope, Vector(
          emptyElem(QName("product"), Vector(QName("name") -> "blender", QName("qty") -> "150"), scope),
          emptyElem(QName("product"), Vector(QName("name") -> "blender", QName("qty") -> "100"), scope),
          emptyElem(QName("product"), Vector(QName("name") -> "toaster", QName("qty") -> "50"), scope),
          emptyElem(QName("product"), Vector(QName("name") -> "shirt", QName("qty") -> "10"), scope)))))
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
