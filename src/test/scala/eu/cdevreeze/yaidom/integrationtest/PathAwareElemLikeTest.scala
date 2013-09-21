/*
 * Copyright 2011 Chris de Vreeze
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
import convert.ScalaXmlConversions._

/**
 * Test case testing some properties of trait PathAwareElemLike.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class PathAwareElemLikeTest extends Suite {

  private val rootElem: Elem = {
    // From: http://www.w3.org/TR/xmlschema-0/
    val xml =
      <purchaseOrder orderDate="1999-10-20">
        <shipTo country="US">
          <name>Alice Smith</name>
          <street>123 Maple Street</street>
          <city>Mill Valley</city>
          <state>CA</state>
          <zip>90952</zip>
        </shipTo>
        <billTo country="US">
          <name>Robert Smith</name>
          <street>8 Oak Avenue</street>
          <city>Old Town</city>
          <state>PA</state>
          <zip>95819</zip>
        </billTo>
        <comment>
          Hurry, my lawn is going wild!
        </comment>
        <items>
          <item partNum="872-AA">
            <productName>Lawnmower</productName>
            <quantity>1</quantity>
            <USPrice>148.95</USPrice>
            <comment>Confirm this is electric</comment>
          </item>
          <item partNum="926-AA">
            <productName>Baby Monitor</productName>
            <quantity>1</quantity>
            <USPrice>39.98</USPrice>
            <shipDate>1999-05-21</shipDate>
          </item>
        </items>
      </purchaseOrder>
    convertToElem(xml)
  }

  @Test def testFilterChildElemPaths() {
    val p = { e: Elem => Set("shipTo", "billTo").contains(e.localName) }
    val toPaths = rootElem.filterChildElemPaths(p)

    expectResult(2) {
      toPaths.size
    }

    expectResult(filterChildElemPaths(rootElem, p)) {
      rootElem.filterChildElemPaths(p)
    }

    expectResult(filterChildElemPaths(rootElem, (e => true))) {
      rootElem.filterChildElemPaths(e => true)
    }
  }

  @Test def testFilterElemOrSelfPaths() {
    val p = { e: Elem => !Set("comment").contains(e.localName) }
    val paths = rootElem.filterElemOrSelfPaths(p)

    expectResult(rootElem.findAllElemsOrSelf.size - 2) {
      paths.size
    }

    expectResult(filterElemOrSelfPaths(rootElem, p)) {
      rootElem.filterElemOrSelfPaths(p)
    }

    expectResult(filterElemOrSelfPaths(rootElem, (e => true))) {
      rootElem.filterElemOrSelfPaths(e => true)
    }
  }

  @Test def testFindTopmostElemOrSelfPaths() {
    val p = { e: Elem => Set("item").contains(e.localName) }
    val paths = rootElem.findTopmostElemOrSelfPaths(p)

    expectResult(2) {
      paths.size
    }

    expectResult(findTopmostElemOrSelfPaths(rootElem, p)) {
      rootElem.findTopmostElemOrSelfPaths(p)
    }

    expectResult(findTopmostElemOrSelfPaths(rootElem, (e => true))) {
      rootElem.findTopmostElemOrSelfPaths(e => true)
    }
  }

  @Test def testFilterElemPaths() {
    val p = { e: Elem => !Set("comment").contains(e.localName) }
    val paths = rootElem.filterElemPaths(p)

    expectResult(rootElem.findAllElems.size - 2) {
      paths.size
    }

    expectResult(filterElemPaths(rootElem, p)) {
      rootElem.filterElemPaths(p)
    }

    expectResult(filterElemPaths(rootElem, (e => true))) {
      rootElem.filterElemPaths(e => true)
    }
  }

  @Test def testFindTopmostElemPaths() {
    val p = { e: Elem => Set("item").contains(e.localName) }
    val paths = rootElem.findTopmostElemPaths(p)

    expectResult(2) {
      paths.size
    }

    expectResult(findTopmostElemPaths(rootElem, p)) {
      rootElem.findTopmostElemPaths(p)
    }

    expectResult(findTopmostElemPaths(rootElem, (e => true))) {
      rootElem.findTopmostElemPaths(e => true)
    }
  }

  @Test def testEqualities() {
    val p = { e: Elem => Set("item").contains(e.localName) }
    val paths = rootElem.filterElemOrSelfPaths(p)

    expectResult(rootElem.filterElemsOrSelf(p).map(e => resolved.Elem(e))) {
      rootElem.filterElemOrSelfPaths(p) map (path => rootElem.getWithElemPath(path)) map (e => resolved.Elem(e))
    }

    expectResult(rootElem.filterElems(p).map(e => resolved.Elem(e))) {
      rootElem.filterElemPaths(p) map (path => rootElem.getWithElemPath(path)) map (e => resolved.Elem(e))
    }

    expectResult(rootElem.filterChildElems(p).map(e => resolved.Elem(e))) {
      rootElem.filterChildElemPaths(p) map (path => rootElem.getWithElemPath(path)) map (e => resolved.Elem(e))
    }
  }

  @Test def testOtherEqualities() {
    val p = { e: Elem => Set("item").contains(e.localName) }
    val paths = rootElem.filterElemOrSelfPaths(p)

    expectResult(rootElem.findAllElemOrSelfPaths.filter(path => p(rootElem.getWithElemPath(path)))) {
      rootElem.filterElemOrSelfPaths(p)
    }

    expectResult(rootElem.findAllElemPaths.filter(path => p(rootElem.getWithElemPath(path)))) {
      rootElem.filterElemPaths(p)
    }

    expectResult(rootElem.findAllChildElemPaths.filter(path => p(rootElem.getWithElemPath(path)))) {
      rootElem.filterChildElemPaths(p)
    }
  }

  // Semantical definitions

  private def filterChildElemPaths(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[ElemPath] =
    elem.findAllChildElemsWithPathEntries collect { case (che, pe) if p(che) => ElemPath(Vector(pe)) }

  private def filterElemOrSelfPaths(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[ElemPath] = {
    (if (p(elem)) Vector(ElemPath.Root) else Vector()) ++ {
      elem.findAllChildElemsWithPathEntries flatMap {
        case (che, pe) =>
          filterElemOrSelfPaths(che, p).map(_.prepend(pe))
      }
    }
  }

  private def findTopmostElemOrSelfPaths(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[ElemPath] = {
    if (p(elem)) Vector(ElemPath.Root)
    else {
      elem.findAllChildElemsWithPathEntries flatMap {
        case (che, pe) =>
          findTopmostElemOrSelfPaths(che, p).map(_.prepend(pe))
      }
    }
  }

  private def filterElemPaths(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[ElemPath] =
    elem.findAllChildElemsWithPathEntries flatMap {
      case (che, pe) =>
        filterElemOrSelfPaths(che, p).map(_.prepend(pe))
    }

  private def findTopmostElemPaths(elem: Elem, p: Elem => Boolean): immutable.IndexedSeq[ElemPath] =
    elem.findAllChildElemsWithPathEntries flatMap {
      case (che, pe) =>
        findTopmostElemOrSelfPaths(che, p).map(_.prepend(pe))
    }

  private def findAllElemOrSelfPaths(elem: Elem): immutable.IndexedSeq[ElemPath] = filterElemOrSelfPaths(elem, e => true)

  private def findAllElemPaths(elem: Elem): immutable.IndexedSeq[ElemPath] = filterElemPaths(elem, e => true)
}