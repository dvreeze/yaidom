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

import scala.Vector
import scala.reflect.ClassTag

import org.scalatest.FunSuite

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.indexed.IndexedClarkElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ElemWithPath
import eu.cdevreeze.yaidom.queryapi.UpdatableElemApi
import eu.cdevreeze.yaidom.queryapi.TransformableElemApi
import eu.cdevreeze.yaidom.resolved.ResolvedNodes
import eu.cdevreeze.yaidom.simple.Elem

/**
 * Update test, using different yaidom methods of updating.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractUpdateTest extends FunSuite {

  type N <: ResolvedNodes.Node
  type E <: N with ResolvedNodes.Elem with ClarkElemApi.Aux[E] with UpdatableElemApi.Aux[N, E] with TransformableElemApi.Aux[N, E]

  implicit val clsTag: ClassTag[E]

  // Does not compile in a Scala 2.13.0-M3 build, due to regression:
  // "inferred type ... contains type selection from volatile type ..."

  // Below, we update the measure elements, replacing the unprefixed measures with prefixed ones (using prefix xbrli)

  test("testTransformElems") {
    val newRootElem = rootElem transformElems {
      case e if e.resolvedName == EName(XbrliNs, "measure") =>
        updateMeasure(e)
      case e => e
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  test("testTransformElemsToNodeSeq") {
    val newRootElem = rootElem transformElemsToNodeSeq {
      case e if e.resolvedName == EName(XbrliNs, "measure") =>
        Vector(updateMeasure(e))
      case e => Vector(e)
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  test("testTransformElemsOrSelfToNodeSeq") {
    val newRootElems = rootElem transformElemsOrSelfToNodeSeq {
      case e if e.resolvedName == EName(XbrliNs, "measure") =>
        Vector(updateMeasure(e))
      case e => Vector(e)
    }

    assertResult(1) {
      newRootElems.size
    }
    checkElemAfterMeasureUpdate(newRootElems.head.asInstanceOf[E])
  }

  test("testUpdateTopmostElemsWithNodeSeq") {
    val newRootElem = rootElem updateTopmostElemsWithNodeSeq { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "measure") =>
          Some(Vector(updateMeasure(e)))
        case (e, p) => None
      }
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  test("testUpdateElemsOrSelf") {
    val pathAwareClarkElem = IndexedClarkElem(yaidom.resolved.Elem(rootElem))

    val paths: Set[Path] =
      pathAwareClarkElem.filterElems(_.resolvedName == EName(XbrliNs, "measure")).map(_.path).toSet

    val newRootElem = rootElem.updateElemsOrSelf(paths) {
      case (e, p) => updateMeasure(e)
    }

    assertResult(newRootElem) {
      IndexedClarkElem(newRootElem).underlyingElem
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  test("testUpdateElemsWithNodeSeqOnLazyIndexedElem") {
    val pathAwareClarkElem = IndexedClarkElem(yaidom.resolved.Elem(rootElem))

    val paths: Set[Path] =
      pathAwareClarkElem.filterElems(_.resolvedName == EName(XbrliNs, "measure")).map(_.path).toSet

    val newRootElem = rootElem.updateElemsWithNodeSeq(paths) {
      case (e, p) => Vector(updateMeasure(e))
    }

    assertResult(newRootElem) {
      IndexedClarkElem(newRootElem).underlyingElem
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  // Below, we update the unit IDs and references for unit U-Monetary.
  // There are many of those, so updates take time.

  test("testTransformElemsForUnitUpdate") {
    val newRootElem = rootElem transformElems {
      case e if e.resolvedName == EName(XbrliNs, "unit") =>
        updateUnitId(e)
      case e if e.attributeOption(EName("unitRef")).contains("U-Monetary") =>
        updateUnitRef(e)
      case e => e
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  test("testTransformElemsOrSelfForUnitUpdate") {
    val newRootElem = rootElem transformElemsOrSelf {
      case e => updateUnitRef(updateUnitId(e))
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  test("testUpdateTopmostElemsWithNodeSeqForUnitUpdate") {
    val newRootElem = rootElem updateTopmostElemsWithNodeSeq { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "unit") =>
          Some(Vector(updateUnitId(e)))
        case (e, p) if e.attributeOption(EName("unitRef")).contains("U-Monetary") =>
          Some(Vector(updateUnitRef(e)))
        case (e, p) => None
      }
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  test("testUpdateElemsWithNodeSeqAgainForUnitUpdate") {
    val pathAwareClarkElem = ElemWithPath(rootElem)

    val elems =
      pathAwareClarkElem filterElems {
        case e if e.elem.resolvedName == EName(XbrliNs, "unit") => true
        case e if e.elem.attributeOption(EName("unitRef")).contains("U-Monetary") => true
        case e => false
      }
    val paths: Set[Path] = elems.map(_.path).toSet

    val newRootElem = rootElem.updateElemsOrSelf(paths) {
      case (e, p) if e.resolvedName == EName(XbrliNs, "unit") => updateUnitId(e)
      case (e, p) if e.attributeOption(EName("unitRef")).contains("U-Monetary") => updateUnitRef(e)
      case (e, p) => e
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  test("testUpdateTopmostElemsOrSelfForUnitUpdate") {
    val newRootElem = rootElem updateTopmostElemsOrSelf { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "unit") =>
          Some(updateUnitId(e))
        case (e, p) if e.attributeOption(EName("unitRef")).contains("U-Monetary") =>
          Some(updateUnitRef(e))
        case (e, p) => None
      }
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  // Below, we update the context IDs and references for context D-2007-PPE-Other.
  // There are only a few of those, so updates are fast.

  test("testTransformElemsForContextUpdate") {
    val newRootElem = rootElem transformElems {
      case e if e.resolvedName == EName(XbrliNs, "context") =>
        updateContextId(e)
      case e if localNamesForContextUpdate.contains(e.localName) =>
        updateContextRef(e)
      case e => e
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  test("testTransformElemsOrSelfForContextUpdate") {
    val newRootElem = rootElem transformElemsOrSelf {
      case e => updateContextRef(updateContextId(e))
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  test("testUpdateTopmostElemsWithNodeSeqForContextUpdate") {
    val newRootElem = rootElem updateTopmostElemsWithNodeSeq { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "context") =>
          Some(Vector(updateContextId(e)))
        case (e, p) if localNamesForContextUpdate.contains(e.localName) =>
          Some(Vector(updateContextRef(e)))
        case (e, p) => None
      }
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  test("testUpdateElemsWithNodeSeqAgainForContextUpdate") {
    val pathAwareClarkElem = ElemWithPath(rootElem)

    val elems =
      pathAwareClarkElem filterElems {
        case e if e.elem.resolvedName == EName(XbrliNs, "context") => true
        case e if localNamesForContextUpdate.contains(e.elem.localName) => true
        case e => false
      }
    val paths: Set[Path] = elems.map(_.path).toSet

    val newRootElem = rootElem.updateElemsOrSelf(paths) {
      case (e, p) if e.resolvedName == EName(XbrliNs, "context") => updateContextId(e)
      case (e, p) if e.attributeOption(EName("contextRef")).contains("D-2007-PPE-Other") => updateContextRef(e)
      case (e, p) => e
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  test("testUpdateTopmostElemsOrSelfForContextUpdate") {
    val newRootElem = rootElem updateTopmostElemsOrSelf { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "context") =>
          Some(updateContextId(e))
        case (e, p) if localNamesForContextUpdate.contains(e.localName) =>
          Some(updateContextRef(e))
        case (e, p) => None
      }
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  // Reorder all explicit members in context entities.
  // This example shows how method updateTopmostElems helps in updating elements with specific ancestries.
  // It appears that the updateTopmostXXX methods can do a lot that otherwise could be done using XSLT.

  test("testReorderExplicitMembers") {
    // Easy to express using updateTopmostElems method

    val newRootElem = rootElem updateTopmostElems { (e, path) =>
      if (e.localName == "segment" &&
        path.entries.map(_.elementName.localPart) == List("context", "entity", "segment")) {

        Some(reorderSegmentChildren(e))
      } else {
        None
      }
    }

    assertResult(false) {
      rootElem.filterElems(_.localName == "explicitMember") ==
        newRootElem.filterElems(_.localName == "explicitMember")
    }

    assertResult(rootElem.filterElems(_.localName == "explicitMember").toSet) {
      newRootElem.filterElems(_.localName == "explicitMember").toSet
    }

    def makeSegmentEmpty(elm: E): E = {
      elm.transformElems(e => if (e.localName == "segment") e.withChildren(Vector()) else e)
    }

    assertResult(yaidom.resolved.Elem(makeSegmentEmpty(rootElem))) {
      yaidom.resolved.Elem(makeSegmentEmpty(newRootElem))
    }
  }

  // Update the value of a specific fact

  test("testUpdateFact") {
    val gaapNs = "http://xasb.org/gaap"

    val elems =
      ElemWithPath(rootElem) filterChildElems { e =>
        e.elem.resolvedName == EName(gaapNs, "AverageNumberEmployees") &&
          e.elem.attributeOption(EName("contextRef")).contains("D-2003") &&
          e.elem.attributeOption(EName("unitRef")).contains("U-Pure")
      }
    val paths = elems.map(_.path)

    assertResult(List(Path.from(EName(gaapNs, "AverageNumberEmployees") -> 2))) {
      paths
    }

    val newRootElem = rootElem.updateElemsOrSelf(paths.toSet) { (e, p) =>
      require(e.resolvedName == EName(gaapNs, "AverageNumberEmployees"))

      updateFactValue(e)
    }

    assertResult(Set("100", "200", "235", "240", "250", "300")) {
      newRootElem.filterChildElems(_.resolvedName == EName(gaapNs, "AverageNumberEmployees")).map(_.text).toSet
    }
  }

  // Helper methods

  private def checkElemAfterMeasureUpdate(elm: E): Unit = {
    assertResult(resolvedExpectedRootElemAfterMeasureUpdate) {
      yaidom.resolved.Elem(elm)
    }
  }

  private def checkElemAfterUnitUpdate(elm: E): Unit = {
    assertResult(resolvedExpectedRootElemAfterUnitUpdate) {
      yaidom.resolved.Elem(elm)
    }
  }

  private def checkElemAfterContextUpdate(elm: E): Unit = {
    assertResult(resolvedExpectedRootElemAfterContextUpdate) {
      yaidom.resolved.Elem(elm)
    }
  }

  private val rootElem: E = {
    val docParser = DocumentParserUsingStax.newInstance
    val uri = classOf[AbstractUpdateTest].getResource("sample-xbrl-instance.xml").toURI
    val doc = docParser.parse(uri)
    fromSimpleElem(doc.documentElement)
  }

  private val XbrliNs = "http://www.xbrl.org/2003/instance"

  protected val localNamesForContextUpdate: Set[String] =
    Set(
      "PropertyPlantAndEquipmentMeasurementBasis",
      "PropertyPlantAndEquipmentDepreciationMethod",
      "PropertyPlantAndEquipmentEstimatedUsefulLife")

  private val resolvedExpectedRootElemAfterMeasureUpdate: yaidom.resolved.Elem =
    yaidom.resolved.Elem(rootElem.transformElems(updateMeasure))

  assert(resolvedExpectedRootElemAfterMeasureUpdate != yaidom.resolved.Elem(rootElem))

  private val resolvedExpectedRootElemAfterUnitUpdate: yaidom.resolved.Elem =
    yaidom.resolved.Elem(rootElem.transformElems((updateUnitId _) andThen (updateUnitRef _)))

  assert(resolvedExpectedRootElemAfterUnitUpdate != yaidom.resolved.Elem(rootElem))

  private val resolvedExpectedRootElemAfterContextUpdate: yaidom.resolved.Elem =
    yaidom.resolved.Elem(rootElem.transformElems((updateContextId _) andThen (updateContextRef _)))

  assert(resolvedExpectedRootElemAfterContextUpdate != yaidom.resolved.Elem(rootElem))

  protected def fromSimpleElem(e: Elem): E

  protected def updateMeasure(e: E): E

  protected def updateUnitId(e: E): E

  protected def updateUnitRef(e: E): E

  protected def updateContextId(e: E): E

  protected def updateContextRef(e: E): E

  protected def reorderSegmentChildren(e: E): E

  protected def updateFactValue(e: E): E
}
