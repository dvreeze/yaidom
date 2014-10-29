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

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike

/**
 * ScopedElemLike-based query test case.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractScopedElemLikeQueryTest extends Suite {

  type E <: ScopedElemLike[E]

  val XsNamespace = "http://www.w3.org/2001/XMLSchema"

  @Test def testResolveQNamesInContent(): Unit = {
    val elemElemDeclOption =
      xsdSchemaElem.findChildElem(e => e.localName == "element" && e.attributeOption(EName("name")) == Some("schema"))

    assertResult(true) {
      elemElemDeclOption.isDefined
    }

    val elemElemDecl = elemElemDeclOption.get

    val nestedElemDecls = elemElemDecl.filterElems(withLocalName("element"))

    assertResult(5) {
      nestedElemDecls.size
    }

    assertResult(Set("xs:include", "xs:import", "xs:redefine", "xs:annotation")) {
      nestedElemDecls.flatMap(_ \@ EName("ref")).toSet
    }
    assertResult(Set("xs:include", "xs:import", "xs:redefine", "xs:annotation").map(s => QName(s))) {
      nestedElemDecls.flatMap(e => e.attributeAsQNameOption(EName("ref"))).toSet
    }
    assertResult(Set(
      EName(XsNamespace, "include"),
      EName(XsNamespace, "import"),
      EName(XsNamespace, "redefine"),
      EName(XsNamespace, "annotation"))) {
      nestedElemDecls.flatMap(e => e.attributeAsResolvedQNameOption(EName("ref"))).toSet
    }

    val elemKeyDefOption =
      elemElemDecl.findChildElem(e => e.qname == QName("xs:key") && (e \@ EName("name")) == Some("element"))

    assertResult(true) {
      elemKeyDefOption.isDefined
    }

    val elemKeyDef = elemKeyDefOption.get

    assertResult(List("xs:element", "@name")) {
      elemKeyDef.findAllElems.flatMap(_ \@ EName("xpath"))
    }
    assertResult(QName("xs:element")) {
      elemKeyDef.findElem(_.attributeOption(EName("xpath")).isDefined).head.attributeAsQName(EName("xpath"))
    }
    assertResult(EName(XsNamespace, "element")) {
      elemKeyDef.findElem(_.attributeOption(EName("xpath")).isDefined).head.attributeAsResolvedQName(EName("xpath"))
    }
  }

  protected val xsdSchemaElem: E

  protected def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem
}
