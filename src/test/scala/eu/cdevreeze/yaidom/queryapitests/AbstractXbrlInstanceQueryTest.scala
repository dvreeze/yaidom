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

import scala.reflect.ClassTag
import scala.reflect.classTag

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.indexed.IndexedClarkElem
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * ElemLike-based query test case, using an XBRL instance as sample data.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractXbrlInstanceQueryTest extends Suite {

  type E <: ClarkElemLike.Aux[E]

  implicit val ttag: ClassTag[E] = classTag[E]

  private val XbrliNs = "http://www.xbrl.org/2003/instance"
  private val LinkNs = "http://www.xbrl.org/2003/linkbase"
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  @Test def testSimpleQueries(): Unit = {
    require(xbrlInstance.resolvedName == EName(XbrliNs, "xbrl"))

    // Finding child elements (more verbose than XPath, but very precise)

    val contexts =
      xbrlInstance filterChildElems { e =>
        e.resolvedName == EName(XbrliNs, "context")
      }

    val units =
      xbrlInstance.filterChildElems(withEName(XbrliNs, "unit"))

    val optNamespaces = Set(Option(XbrliNs), Option(LinkNs))
    val topLevelFacts =
      xbrlInstance filterChildElems { e =>
        !optNamespaces.contains(e.resolvedName.namespaceUriOption)
      }

    // Finding descendant elements (more verbose than XPath, but very precise)
    // Takes fractions into account, as they are are not facts

    val nestedFacts =
      topLevelFacts.flatMap(e => e.filterElems(_.resolvedName.namespaceUriOption != Some(XbrliNs)))

    // Finding descendant-or-self elements (more verbose than XPath, but very precise)
    // Takes fractions into account, as they are are not facts

    val allFacts =
      topLevelFacts.flatMap(e => e.filterElemsOrSelf(_.resolvedName.namespaceUriOption != Some(XbrliNs)))

    // Assertions

    assertResult(true) {
      val ids = contexts.flatMap(_.attributeOption(EName("id"))).toSet
      Set("I-2003", "I-2004", "I-2005", "I-2006", "I-2007", "D-2007", "D-2007-PSA", "D-2007-CSA").subsetOf(ids)
    }

    // Safe: compares "resolved" elements

    assertResult(contexts.map(e => toResolvedElem(e))) {
      xbrlInstance.filterChildElems(withEName(XbrliNs, "context")).map(e => toResolvedElem(e))
    }

    // Less safe: works if no new element objects are created, or if newly created element objects are considered equal

    assertResult(contexts) {
      xbrlInstance.filterChildElems(withEName(XbrliNs, "context"))
    }

    val GaapNs = "http://xasb.org/gaap"

    assertResult(true) {
      val topLevelFactENames = topLevelFacts.map(_.resolvedName).toSet

      Set(
        EName(GaapNs, "CashFlowNet"),
        EName(GaapNs, "ProceedsFromSaleOfPropertyPlantAndEquipment"),
        EName(GaapNs, "PreferredStockShares"),
        EName(GaapNs, "InventoryCostMethod"),
        EName(GaapNs, "IncomeTaxesPolicy")).subsetOf(topLevelFactENames)
    }

    // No tuples
    assertResult(0) {
      nestedFacts.size
    }

    assertResult(allFacts.size) {
      topLevelFacts.size + nestedFacts.size
    }

    assertResult(xbrlInstance.findAllChildElems.size) {
      val topLevelNonFacts =
        xbrlInstance.filterChildElems(e => optNamespaces.contains(e.resolvedName.namespaceUriOption))

      topLevelNonFacts.size + topLevelFacts.size
    }

    // Checking properties about "resolved" elements

    assertResult(toResolvedElem(xbrlInstance).findAllChildElems) {
      xbrlInstance.findAllChildElems.map(e => toResolvedElem(e))
    }

    assertResult(toResolvedElem(xbrlInstance).findAllElemsOrSelf) {
      xbrlInstance.findAllElemsOrSelf.map(e => toResolvedElem(e))
    }

    // Checking equality for child element sets, using "resolved" elements

    assertResult(xbrlInstance.findAllChildElems.map(e => toResolvedElem(e)).toSet) {
      val topLevelNonFacts =
        xbrlInstance.filterChildElems(e => optNamespaces.contains(e.resolvedName.namespaceUriOption))

      topLevelNonFacts.map(e => toResolvedElem(e)).toSet.union(topLevelFacts.map(e => toResolvedElem(e)).toSet)
    }
  }

  @Test def testBulkNavigation(): Unit = {
    require(xbrlInstance.resolvedName == EName(XbrliNs, "xbrl"))

    val uriResolver = XmlBaseSupport.JdkUriResolver
    val indexedElemBuilder = IndexedClarkElem

    val indexedInstance = indexedElemBuilder(xbrlInstance)

    val elemsWithPaths =
      indexedInstance.findAllElemsOrSelf.map(p => (p.underlyingElem, p.path))
    val paths = elemsWithPaths.map(_._2)

    assertResult(xbrlInstance.findAllElemsOrSelf) {
      indexedInstance.filterElemsOrSelf(e => paths.toSet.contains(e.path)).map(_.underlyingElem)
    }

    def isIdentifierPath(p: Path): Boolean = {
      !p.isEmpty && (p.elementNameOption.get == EName(XbrliNs, "identifier"))
    }

    assertResult(xbrlInstance.filterElemsOrSelf(_.resolvedName == EName(XbrliNs, "identifier"))) {
      indexedInstance.filterElemsOrSelf(e => paths.filter(isIdentifierPath).toSet.contains(e.path)).map(_.underlyingElem)
    }
  }

  protected val xbrlInstance: E

  protected def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem
}
