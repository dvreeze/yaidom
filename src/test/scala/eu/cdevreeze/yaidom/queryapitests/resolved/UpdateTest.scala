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

package eu.cdevreeze.yaidom.queryapitests.resolved

import scala.reflect.classTag
import scala.reflect.ClassTag

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.queryapitests.AbstractUpdateTest
import eu.cdevreeze.yaidom.resolved.Elem
import eu.cdevreeze.yaidom.resolved.Node
import eu.cdevreeze.yaidom.resolved.Text

/**
 * Update test case for resolved Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class UpdateTest extends AbstractUpdateTest {

  final type N = Node
  final type E = Elem

  implicit val clsTag: ClassTag[E] = classTag[Elem]

  protected def fromSimpleElem(e: eu.cdevreeze.yaidom.simple.Elem): E = Elem(e)

  protected def updateMeasure(e: E): E = {
    if (e.localName == "measure" && e.text.indexOf(':') < 0) {
      val newQName = QName("xbrli", QName(e.text).localPart)
      e.copy(children = Vector(Text(newQName.toString)))
    } else {
      e
    }
  }

  protected def updateUnitId(e: E): E = {
    if (e.localName == "unit" && e.attribute(EName("id")) == "U-Monetary") {
      e.copy(resolvedAttributes = e.resolvedAttributes + (EName("id") -> "U-USD"))
    } else {
      e
    }
  }

  protected def updateUnitRef(e: E): E = {
    if (e.attributeOption(EName("unitRef")).contains("U-Monetary")) {
      e.copy(resolvedAttributes = e.resolvedAttributes + (EName("unitRef") -> "U-USD"))
    } else {
      e
    }
  }

  protected def updateContextId(e: E): E = {
    if (e.localName == "context" && e.attribute(EName("id")) == "D-2007-PPE-Other") {
      e.copy(resolvedAttributes = e.resolvedAttributes + (EName("id") -> "D-2007-Other-PPE"))
    } else {
      e
    }
  }

  protected def updateContextRef(e: E): E = {
    if (localNamesForContextUpdate.contains(e.localName) && e.attributeOption(EName("contextRef")).contains("D-2007-PPE-Other")) {
      e.copy(resolvedAttributes = e.resolvedAttributes + (EName("contextRef") -> "D-2007-Other-PPE"))
    } else {
      e
    }
  }

  protected def reorderSegmentChildren(e: E): E = {
    require(e.localName == "segment")

    e.withChildren(e.findAllChildElems.sortBy(_.attributeOption(EName("dimension")).getOrElse("")))
  }

  protected def updateFactValue(e: E): E = {
    e.withChildren(Vector(Text("235")))
  }
}
