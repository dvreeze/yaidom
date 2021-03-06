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

import scala.reflect.ClassTag

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.queryapi.ElemWithPath
import eu.cdevreeze.yaidom.queryapi.TransformableElemApi
import eu.cdevreeze.yaidom.queryapi.UpdatableElemApi
import eu.cdevreeze.yaidom.simple.Elem
import org.scalatest.funsuite.AnyFunSuite

/**
 * Update test, using different yaidom methods of updating.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractUpdateTest extends AnyFunSuite {

  // Note below how we prevent the following error: "inferred type ... contains type selection from volatile type ..."
  // We do this by type E no longer extending type N.

  type E <: ClarkNodes.Elem.Aux[N, E] with UpdatableElemApi.Aux[N, E] with TransformableElemApi.Aux[N, E]
  type N >: E <: ClarkNodes.Node

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
      yaidom.resolved.Elem.from(elm)
    }
  }

  private def checkElemAfterUnitUpdate(elm: E): Unit = {
    assertResult(resolvedExpectedRootElemAfterUnitUpdate) {
      yaidom.resolved.Elem.from(elm)
    }
  }

  private def checkElemAfterContextUpdate(elm: E): Unit = {
    assertResult(resolvedExpectedRootElemAfterContextUpdate) {
      yaidom.resolved.Elem.from(elm)
    }
  }

  private val rootElem: E = {
    val docParser = DocumentParserUsingStax.newInstance()
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
    yaidom.resolved.Elem.from(rootElem.transformElems(updateMeasure))

  assert(resolvedExpectedRootElemAfterMeasureUpdate != yaidom.resolved.Elem.from(rootElem))

  private val resolvedExpectedRootElemAfterUnitUpdate: yaidom.resolved.Elem =
    yaidom.resolved.Elem.from(rootElem.transformElems((updateUnitId _) andThen (updateUnitRef _)))

  assert(resolvedExpectedRootElemAfterUnitUpdate != yaidom.resolved.Elem.from(rootElem))

  private val resolvedExpectedRootElemAfterContextUpdate: yaidom.resolved.Elem =
    yaidom.resolved.Elem.from(rootElem.transformElems((updateContextId _) andThen (updateContextRef _)))

  assert(resolvedExpectedRootElemAfterContextUpdate != yaidom.resolved.Elem.from(rootElem))

  protected def fromSimpleElem(e: Elem): E

  protected def updateMeasure(e: E): E

  protected def updateUnitId(e: E): E

  protected def updateUnitRef(e: E): E

  protected def updateContextId(e: E): E

  protected def updateContextRef(e: E): E

  protected def reorderSegmentChildren(e: E): E

  protected def updateFactValue(e: E): E
}
