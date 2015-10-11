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
import eu.cdevreeze.yaidom.indexed.PathAwareClarkElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
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
  type E <: N with ResolvedNodes.Elem with UpdatableElemApi[N, E] with TransformableElemApi[N, E]

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

  @Test def testWithUpdatedElems(): Unit = {
    val newRootElem = rootElem updateElemsWithNodeSeq { (e: E, p: Path) =>
      (e, p) match {
        case (e, p) if e.resolvedName == EName(XbrliNs, "measure") =>
          Some(Vector(updateMeasure(e)))
        case (e, p) => None
      }
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  @Test def testUpdatedAtPaths(): Unit = {
    val pathAwareClarkElem = PathAwareClarkElem(yaidom.resolved.Elem(rootElem))

    val paths: Set[Path] =
      pathAwareClarkElem.filterElems(_.resolvedName == EName(XbrliNs, "measure")).map(_.path).toSet

    val newRootElem = rootElem.updatedAtPaths(paths) {
      case (e, p) => updateMeasure(e)
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  @Test def testUpdatedWithNodeSeqAtNonEmptyPaths(): Unit = {
    val pathAwareClarkElem = PathAwareClarkElem(yaidom.resolved.Elem(rootElem))

    val paths: Set[Path] =
      pathAwareClarkElem.filterElems(_.resolvedName == EName(XbrliNs, "measure")).map(_.path).toSet

    val newRootElem = rootElem.updatedWithNodeSeqAtNonEmptyPaths(paths) {
      case (e, p) => Vector(updateMeasure(e))
    }

    checkElemAfterMeasureUpdate(newRootElem)
  }

  private def checkElemAfterMeasureUpdate(elm: E): Unit = {
    assertResult(resolvedExpectedRootElemAfterMeasureUpdate) {
      yaidom.resolved.Elem(elm)
    }
  }

  private val rootElem: E = {
    val docParser = DocumentParserUsingStax.newInstance
    val uri = classOf[AbstractUpdateTest].getResource("sample-xbrl-instance.xml").toURI
    val doc = docParser.parse(uri)
    fromSimpleElem(doc.documentElement)
  }

  private val resolvedExpectedRootElemAfterMeasureUpdate: yaidom.resolved.Elem =
    yaidom.resolved.Elem(rootElem.transformElems(updateMeasure))

  private val XbrliNs = "http://www.xbrl.org/2003/instance"

  protected def fromSimpleElem(e: Elem): E

  protected def updateMeasure(e: E): E
}
