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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.UnprefixedName
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.resolved

/**
 * XML functional update function test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class UpdateFunctionTest extends FunSuite {

  private val docParser = DocumentParserUsingDom.newInstance()

  test("testUpdateAttributeNames") {
    val is = classOf[UpdateFunctionTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 = transformElems(doc.documentElement, updateNameAttributeName)

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    val docElem2 = transformElemsOrSelf(doc.documentElement, updateNameAttributeName)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val unchangedDocElem = transformChildElems(doc.documentElement, updateNameAttributeName)

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(unchangedDocElem.underlyingElem)
    }

    val docElem3 = transformChildElems(doc.documentElement, { che =>
      transformElems(che, updateNameAttributeName)
    })

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    val docElem4 = transformChildElems(doc.documentElement, { che =>
      transformElemsOrSelf(che, updateNameAttributeName)
    })

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }
  }

  private def updateNameAttributeName(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("First_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("FirstName")))
      case qn @ UnprefixedName("Last_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("LastName")))
      case qn =>
        elm
    }
  }
}
