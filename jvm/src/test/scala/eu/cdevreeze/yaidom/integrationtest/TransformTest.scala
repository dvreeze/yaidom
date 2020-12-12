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

package eu.cdevreeze.yaidom.integrationtest

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import org.scalatest.funsuite.AnyFunSuite

/**
 * XML transformation test case.
 *
 * @author Chris de Vreeze
 */
class TransformTest extends AnyFunSuite {

  test("testNestedTransformation") {
    // See http://stackoverflow.com/questions/4313032/scala-xml-transform-throws-away-transformed-elements-with-multiple-attributes.
    // Also see http://scala-scales.googlecode.com/svn/sites/scales/scales-xml_2.9.1/0.2/ScalesXmlIntro.html,
    // on nested transformations.

    val scope = Scope.Empty

    import Node._

    val emptyScope: Scope = Scope.Empty

    def fooIdBits(i: Int): Iterator[Elem] = {
      Iterator.from(i).map { idx =>
        val currentFoo =
          elem(
            QName("foo"),
            Vector(QName("id") -> idx.toString),
            emptyScope,
            Vector(
              elem(
                QName("bar"),
                Vector(QName("id") -> "0"),
                emptyScope,
                Vector(
                  elem(
                    QName("baz"),
                    Vector(QName("id") -> "0", QName("blah") -> "blah", QName("etc") -> "etc"),
                    emptyScope,
                    Vector(emptyElem(QName("buz"), Vector(QName("id") -> "0"), emptyScope))
                  ),
                  emptyElem(
                    QName("buz"),
                    Vector(QName("id") -> "0", QName("blah") -> "blah", QName("etc") -> "etc"),
                    emptyScope)
                )
              ))
          )

        currentFoo
      }
    }

    val fooIdElem = Node.elem(QName("root"), scope, fooIdBits(1).take(50).toVector)

    def updateId(elem: Elem): Elem = {
      require(elem.localName == "foo")
      val id = elem.attribute(EName("id")).toInt
      elem.transformElems { e =>
        e.plusAttribute(QName("id"), id.toString)
      }
    }

    val updatedFooIdElem = fooIdElem.transformChildElems(e => updateId(e))

    val foundIds = updatedFooIdElem.findAllElemsOrSelf.flatMap(_.attributeOption(EName("id")))

    assertResult((1 to 50).toSet) {
      foundIds.map(_.toInt).toSet
    }
  }

  test("testTransformAttributeValues") {
    // See http://stackoverflow.com/questions/2569580/how-to-change-attribute-on-scala-xml-element/2581712#2581712.

    import Node._

    def b(attr1Value: Int, attr2Value: Int): Elem = {
      emptyElem(
        QName("b"),
        Vector(QName("attr1") -> attr1Value.toString, QName("attr2") -> attr2Value.toString),
        Scope.Empty)
    }

    val a =
      elem(QName("a"), Scope.Empty, Vector(b(100, 50), b(300, 40), b(20, 50)))

    def doubleAttributes(elem: Elem): Elem = {
      require(elem.localName == "b")
      val attr1 = elem.attribute(EName("attr1")).toInt
      val attr2 = elem.attribute(EName("attr2")).toInt
      elem.plusAttribute(QName("attr1"), (attr1 * 2).toString).plusAttribute(QName("attr2"), (attr2 * 2).toString)
    }

    val updatedA = a.transformChildElems(e => doubleAttributes(e))

    assertResult(List((200, 100), (600, 80), (40, 100))) {
      updatedA.findAllChildElems.map(e => (e.attribute(EName("attr1")).toInt, e.attribute(EName("attr2")).toInt))
    }
  }
}
