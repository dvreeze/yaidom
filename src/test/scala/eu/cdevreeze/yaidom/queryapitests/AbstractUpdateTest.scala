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
import scala.reflect.ClassTag

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.indexed.LazyIndexedClarkElem
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
abstract class AbstractUpdateTest extends Suite {

  type N <: ResolvedNodes.Node
  type E <: N with ResolvedNodes.Elem with ClarkElemApi[E] with UpdatableElemApi[N, E] with TransformableElemApi[N, E]

  implicit val clsTag: ClassTag[E]

  // Below, we update the measure elements, replacing the unprefixed measures with prefixed ones (using prefix xbrli)

  @Test def testTransformElems(): Unit = {
    val newRootElem = rootElem transformElems {
      case e if e.resolvedName == EName(XbrliNs, "measure") =>
        updateMeasure(e)
      case e => e
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  @Test def testTransformElemsToNodeSeq(): Unit = {
    val newRootElem = rootElem transformElemsToNodeSeq {
      case e if e.resolvedName == EName(XbrliNs, "measure") =>
        Vector(updateMeasure(e))
      case e => Vector(e)
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  @Test def testTransformElemsOrSelfToNodeSeq(): Unit = {
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

  @Test def testUpdateTopmostElemsWithNodeSeq(): Unit = {
    val newRootElem = rootElem updateTopmostElemsWithNodeSeq { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "measure") =>
          Some(Vector(updateMeasure(e)))
        case (e, p) => None
      }
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  @Test def testUpdateElemsOrSelf(): Unit = {
    val pathAwareClarkElem = LazyIndexedClarkElem(yaidom.resolved.Elem(rootElem))

    val paths: Set[Path] =
      pathAwareClarkElem.filterElems(_.resolvedName == EName(XbrliNs, "measure")).map(_.path).toSet

    val newRootElem = rootElem.updateElemsOrSelf(paths) {
      case (e, p) => updateMeasure(e)
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  @Test def testUpdateElemsWithNodeSeqOnLazyIndexedElem(): Unit = {
    val pathAwareClarkElem = LazyIndexedClarkElem(yaidom.resolved.Elem(rootElem))

    val paths: Set[Path] =
      pathAwareClarkElem.filterElems(_.resolvedName == EName(XbrliNs, "measure")).map(_.path).toSet

    val newRootElem = rootElem.updateElemsWithNodeSeq(paths) {
      case (e, p) => Vector(updateMeasure(e))
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  // Below, we update the unit IDs and references for unit U-Monetary.
  // There are many of those, so updates take time.

  @Test def testTransformElemsForUnitUpdate(): Unit = {
    val newRootElem = rootElem transformElems {
      case e if e.resolvedName == EName(XbrliNs, "unit") =>
        updateUnitId(e)
      case e if e.attributeOption(EName("unitRef")) == Some("U-Monetary") =>
        updateUnitRef(e)
      case e => e
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  @Test def testTransformElemsOrSelfForUnitUpdate(): Unit = {
    val newRootElem = rootElem transformElemsOrSelf {
      case e => updateUnitRef(updateUnitId(e))
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  @Test def testUpdateTopmostElemsWithNodeSeqForUnitUpdate(): Unit = {
    val newRootElem = rootElem updateTopmostElemsWithNodeSeq { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "unit") =>
          Some(Vector(updateUnitId(e)))
        case (e, p) if e.attributeOption(EName("unitRef")) == Some("U-Monetary") =>
          Some(Vector(updateUnitRef(e)))
        case (e, p) => None
      }
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  @Test def testUpdateElemsWithNodeSeqAgainForUnitUpdate(): Unit = {
    val pathAwareClarkElem = ElemWithPath(rootElem)

    val elems =
      pathAwareClarkElem filterElems {
        case e if e.elem.resolvedName == EName(XbrliNs, "unit") => true
        case e if e.elem.attributeOption(EName("unitRef")) == Some("U-Monetary") => true
        case e => false
      }
    val paths: Set[Path] = elems.map(_.path).toSet

    val newRootElem = rootElem.updateElemsOrSelf(paths) {
      case (e, p) if e.resolvedName == EName(XbrliNs, "unit") => updateUnitId(e)
      case (e, p) if e.attributeOption(EName("unitRef")) == Some("U-Monetary") => updateUnitRef(e)
      case (e, p) => e
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  @Test def testUpdateTopmostElemsOrSelfForUnitUpdate(): Unit = {
    val newRootElem = rootElem updateTopmostElemsOrSelf { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "unit") =>
          Some(updateUnitId(e))
        case (e, p) if e.attributeOption(EName("unitRef")) == Some("U-Monetary") =>
          Some(updateUnitRef(e))
        case (e, p) => None
      }
    }

    checkElemAfterUnitUpdate(newRootElem)
  }

  // Below, we update the context IDs and references for context D-2007-PPE-Other.
  // There are only a few of those, so updates are fast.

  @Test def testTransformElemsForContextUpdate(): Unit = {
    val newRootElem = rootElem transformElems {
      case e if e.resolvedName == EName(XbrliNs, "context") =>
        updateContextId(e)
      case e if localNamesForContextUpdate.contains(e.localName) =>
        updateContextRef(e)
      case e => e
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  @Test def testTransformElemsOrSelfForContextUpdate(): Unit = {
    val newRootElem = rootElem transformElemsOrSelf {
      case e => updateContextRef(updateContextId(e))
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  @Test def testUpdateTopmostElemsWithNodeSeqForContextUpdate(): Unit = {
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

  @Test def testUpdateElemsWithNodeSeqAgainForContextUpdate(): Unit = {
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
      case (e, p) if e.attributeOption(EName("contextRef")) == Some("D-2007-PPE-Other") => updateContextRef(e)
      case (e, p) => e
    }

    checkElemAfterContextUpdate(newRootElem)
  }

  @Test def testUpdateTopmostElemsOrSelfForContextUpdate(): Unit = {
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
}
