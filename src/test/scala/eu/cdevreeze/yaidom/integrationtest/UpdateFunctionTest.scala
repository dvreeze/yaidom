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

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.Path
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

  test("testShowVisitedPaths") {
    // Showing that paths are visited in reverse document order, in order not to break them during transformations

    val is = classOf[UpdateFunctionTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    var visitedPaths: immutable.IndexedSeq[Path] = Vector()

    def accumulatePaths(elm: indexed.Elem): indexed.Elem = {
      visitedPaths = visitedPaths :+ elm.path
      elm
    }

    visitedPaths = Vector()

    transformElems(doc.documentElement, accumulatePaths)

    assertResult(doc.documentElement.findAllElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    transformElemsOrSelf(doc.documentElement, accumulatePaths)

    assertResult(doc.documentElement.findAllElemsOrSelf.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    transformChildElems(doc.documentElement, accumulatePaths)

    assertResult(doc.documentElement.findAllChildElems.map(_.path).reverse) {
      visitedPaths
    }
  }

  test("testShowVisitedPathsForNodeSeqTransformations") {
    // Showing that paths are visited in reverse document order, in order not to break them during transformations

    val is = classOf[UpdateFunctionTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    var visitedPaths: immutable.IndexedSeq[Path] = Vector()

    def accumulatePaths(elm: indexed.Elem): immutable.IndexedSeq[indexed.IndexedScopedNode.Node] = {
      visitedPaths = visitedPaths :+ elm.path
      immutable.IndexedSeq(elm)
    }

    visitedPaths = Vector()

    transformElemsToNodeSeq(doc.documentElement, accumulatePaths)

    assertResult(doc.documentElement.findAllElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    transformElemsOrSelfToNodeSeq(doc.documentElement, accumulatePaths)

    assertResult(doc.documentElement.findAllElemsOrSelf.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    transformChildElemsToNodeSeq(doc.documentElement, accumulatePaths)

    assertResult(doc.documentElement.findAllChildElems.map(_.path).reverse) {
      visitedPaths
    }
  }

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

  test("testUpdateAttributeNamesReturningNodeSeqs") {
    val is = classOf[UpdateFunctionTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 = transformElemsToNodeSeq(doc.documentElement, updateNameAttributeNameReturningNodeSeq)

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    val docElems2 = transformElemsOrSelfToNodeSeq(doc.documentElement, updateNameAttributeNameReturningNodeSeq)

    assertResult(1) {
      docElems2.size
    }
    assertResult(true) {
      docElems2.forall(_.isInstanceOf[indexed.IndexedScopedNode.Elem[_]])
    }

    val docElem2 = docElems2.head.asInstanceOf[indexed.Elem]

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val unchangedDocElem = transformChildElemsToNodeSeq(doc.documentElement, updateNameAttributeNameReturningNodeSeq)

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(unchangedDocElem.underlyingElem)
    }

    val docElem3 = transformChildElems(doc.documentElement, { che =>
      transformElemsToNodeSeq(che, updateNameAttributeNameReturningNodeSeq)
    })

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    val docElem4 = transformChildElemsToNodeSeq(doc.documentElement, { che =>
      transformElemsOrSelfToNodeSeq(che, updateNameAttributeNameReturningNodeSeq)
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

  private def updateNameAttributeNameReturningNodeSeq(elm: indexed.Elem): immutable.IndexedSeq[indexed.IndexedScopedNode.Node] = {
    elm.qname match {
      case qn @ UnprefixedName("First_Name") =>
        immutable.IndexedSeq(indexed.Elem(elm.underlyingElem.copy(qname = QName("FirstName"))))
      case qn @ UnprefixedName("Last_Name") =>
        immutable.IndexedSeq(indexed.Elem(elm.underlyingElem.copy(qname = QName("LastName"))))
      case qn =>
        immutable.IndexedSeq(elm)
    }
  }
}
