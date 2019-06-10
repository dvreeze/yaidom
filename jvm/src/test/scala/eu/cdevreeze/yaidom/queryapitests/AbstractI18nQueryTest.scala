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

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi.withEName
import eu.cdevreeze.yaidom.queryapi.ScopedNodes
import org.scalatest.FunSuite

/**
 * ScopedElemLike-based I18N query test case. Make sure the encoding of this source file is UTF-8!
 *
 * @author Chris de Vreeze
 */
abstract class AbstractI18nQueryTest extends FunSuite {

  type E <: ScopedNodes.Elem

  test("testI18n") {
    val facts = rootElem.filterChildElems(e => e.qname.prefixOption.contains("tx"))

    assertResult(List("la_á", "la_é", "la_í", "ó", "la_ú", "España1").map(s => QName("tx", s))) {
      facts.map(_.qname)
    }

    val txNs = "http://www.reportingstandard.com/conformance/internatialization"

    assertResult(List("la_á", "la_é", "la_í", "ó", "la_ú", "España1").map(s => EName(txNs, s))) {
      facts.map(_.resolvedName)
    }

    assertResult(List("España")) {
      facts.flatMap(e => e.attributeOption(EName("contextRef"))).distinct
    }

    assertResult(List("ÑÁÉ")) {
      facts.flatMap(e => e.attributeOption(EName("unitRef"))).distinct
    }

    val xbrliNs = "http://www.xbrl.org/2003/instance"
    val contexts = rootElem.filterChildElems(withEName(xbrliNs, "context"))

    assertResult(contexts.map(_.attribute(EName("id"))).distinct) {
      facts.flatMap(e => e.attributeOption(EName("contextRef"))).distinct
    }

    val units = rootElem.filterChildElems(withEName(xbrliNs, "unit"))

    assertResult(units.map(_.attribute(EName("id"))).distinct) {
      facts.flatMap(e => e.attributeOption(EName("unitRef"))).distinct
    }

    assertResult(List("Pañuelos Co.")) {
      rootElem.filterElems(withEName(xbrliNs, "identifier")).map(_.text).distinct
    }

    assertResult(List("30")) {
      rootElem.filterElems(e => e.resolvedName == EName(txNs, "la_í") && e.attributeOption(EName("unitRef")).contains("ÑÁÉ")).map(_.text).distinct
    }
  }

  protected val rootElem: E

  protected final def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem = {
    eu.cdevreeze.yaidom.resolved.Elem.from(elem)
  }
}
