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

import org.scalatest.FunSuite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.PathBuilder
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedNodes

/**
 * ScopedElemLike-based query test case.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractScopedElemLikeQueryTest extends FunSuite {

  type E <: ScopedNodes.Elem with ScopedElemApi.Aux[E]

  val XsNamespace = "http://www.w3.org/2001/XMLSchema"

  test("testResolveQNamesInContent") {
    val elemElemDeclOption =
      xsdSchemaElem.findChildElem(e => e.localName == "element" && e.attributeOption(EName("name")).contains("schema"))

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
      elemElemDecl.findChildElem(e => e.qname == QName("xs:key") && (e \@ EName("name")).contains("element"))

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

  test("testNavigation") {
    val scope = Scope.from("xs" -> XsNamespace)
    val path1 = PathBuilder.from(QName("xs:element") -> 0, QName("xs:complexType") -> 0, QName("xs:complexContent") -> 0).build(scope)
    val path2 = PathBuilder.from(QName("xs:extension") -> 0, QName("xs:sequence") -> 0, QName("xs:choice") -> 0, QName("xs:element") -> 3).build(scope)
    val path = path1.append(path2)

    val complexContentElemOption = xsdSchemaElem.findElemOrSelfByPath(path1)

    assertResult(true) {
      complexContentElemOption.isDefined
    }

    val elemElemOption = complexContentElemOption.get.findElemOrSelfByPath(path2)

    assertResult(true) {
      elemElemOption.isDefined
    }
    assertResult(EName(XsNamespace, "element")) {
      elemElemOption.get.resolvedName
    }
    assertResult(Some("xs:annotation")) {
      elemElemOption.get \@ EName("ref")
    }

    assertResult(elemElemOption.map(e => toResolvedElem(e).removeAllInterElementWhitespace)) {
      val elemOption =
        xsdSchemaElem.findChildElem(withEName(XsNamespace, "element")).toVector.flatMap(_.filterElems(withEName(XsNamespace, "element"))).drop(3).headOption

      elemOption.map(e => toResolvedElem(e).removeAllInterElementWhitespace)
    }

    assertResult(elemElemOption.map(e => toResolvedElem(e).removeAllInterElementWhitespace)) {
      val elemOption = xsdSchemaElem.findElemOrSelfByPath(path)

      elemOption.map(e => toResolvedElem(e).removeAllInterElementWhitespace)
    }
  }

  protected val xsdSchemaElem: E

  protected final def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem = {
    eu.cdevreeze.yaidom.resolved.Elem.from(elem)
  }
}
