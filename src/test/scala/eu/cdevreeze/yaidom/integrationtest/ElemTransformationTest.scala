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

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.UnprefixedName
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * XML functional update function test case, using the ElemTransformationApi for indexed elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ElemTransformationTest extends FunSuite {

  private val docParser = DocumentParserUsingDom.newInstance()

  test("testShowVisitedPaths") {
    // Showing that paths are visited in reverse document order, in order not to break them during transformations

    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

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

    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

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

  test("testUpdateNames") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 = transformElems(doc.documentElement, updateNameElementName) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    val docElem2 = transformElemsOrSelf(doc.documentElement, updateNameElementName) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val unchangedDocElem = transformChildElems(doc.documentElement, updateNameElementName) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(unchangedDocElem.underlyingElem)
    }

    val docElem3 = transformChildElems(doc.documentElement, { che =>
      transformElems(che, updateNameElementName)
    }) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    val docElem4 = transformChildElems(doc.documentElement, { che =>
      transformElemsOrSelf(che, updateNameElementName)
    }) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }
  }

  test("testUpdateNamesReturningNodeSeqs") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 = transformElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    val docElems2 = transformElemsOrSelfToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq)

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

    val unchangedDocElem = transformChildElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(unchangedDocElem.underlyingElem)
    }

    val docElem3 = transformChildElems(doc.documentElement, { che =>
      transformElemsToNodeSeq(che, updateNameElementNameReturningNodeSeq)
    }) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    val docElem4 = transformChildElemsToNodeSeq(doc.documentElement, { che =>
      transformElemsOrSelfToNodeSeq(che, updateNameElementNameReturningNodeSeq)
    }) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }
  }

  test("testUpdateIsbnAndNames") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 =
      transformElems(
        transformElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoIsbnUpdate).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Now using transformElemsOrSelf twice
    val docElem2 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val partiallyChangedDocElem =
      transformChildElems(
        transformChildElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(partiallyChangedDocElem.underlyingElem).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Changing the order of updates
    val docElem3 =
      transformElems(
        transformElems(doc.documentElement, turnIsbnIntoElement),
        updateNameElementName) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    // Changing the order of updates
    val docElem4 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, turnIsbnIntoElement),
        updateNameElementName) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }

    // Combining transformChildElems with transformElems
    val docElem5 =
      transformChildElems(
        transformElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem5.underlyingElem)
    }

    // Combining transformChildElems with transformElemsOrSelf
    val docElem6 =
      transformChildElems(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem6.underlyingElem)
    }
  }

  test("testUpdateIsbnAndNamesReturningNodeSeqs") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 =
      transformElemsToNodeSeq(
        transformElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq),
        turnIsbnIntoElementReturningNodeSeq) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoIsbnUpdate).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Now using transformElemsOrSelfToNodeSeq twice
    val docElem2 =
      transformElemsOrSelfToNodeSeq(
        transformElemsOrSelfToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq).head.asInstanceOf[indexed.Elem],
        turnIsbnIntoElementReturningNodeSeq).head.asInstanceOf[indexed.Elem] ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val partiallyChangedDocElem =
      transformChildElemsToNodeSeq(
        transformChildElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq),
        turnIsbnIntoElementReturningNodeSeq) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(partiallyChangedDocElem.underlyingElem).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Changing the order of updates
    val docElem3 =
      transformElemsToNodeSeq(
        transformElemsToNodeSeq(doc.documentElement, turnIsbnIntoElementReturningNodeSeq),
        updateNameElementNameReturningNodeSeq) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    // Changing the order of updates
    val docElem4 =
      transformElemsOrSelfToNodeSeq(
        transformElemsOrSelfToNodeSeq(doc.documentElement, turnIsbnIntoElementReturningNodeSeq).head.asInstanceOf[indexed.Elem],
        updateNameElementNameReturningNodeSeq).head.asInstanceOf[indexed.Elem] ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }

    // Combining transformChildElemsToNodeSeq with transformElemsToNodeSeq
    val docElem5 =
      transformChildElemsToNodeSeq(
        transformElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq),
        turnIsbnIntoElementReturningNodeSeq) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem5.underlyingElem)
    }

    // Combining transformChildElemsToNodeSeq with transformElemsOrSelf
    val docElem6 =
      transformChildElemsToNodeSeq(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElementReturningNodeSeq) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem6.underlyingElem)
    }
  }

  test("testUpdateIsbnIfNamesUpdated") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    // First update names, then update ISBN if names have been updated (which they have)
    val docElem1 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElementIfNamesUpdated) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoIsbnUpdate).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Update ISBN if names have been updated (which they haven't)
    val docElem2 = transformElemsOrSelf(doc.documentElement, turnIsbnIntoElementIfNamesUpdated) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    // Changing the order of updates, resulting in a name update, but no ISBN update
    val docElem3 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, turnIsbnIntoElementIfNamesUpdated),
        updateNameElementName) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem3.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    // Doing less work
    val docElem4 =
      transformChildElems(
        transformElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElementIfNamesUpdated) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }
  }

  test("testUpdateIsbnIfNamesNotUpdated") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    // First update ISBN if names have not been updated (which is indeed the case), then update names
    val docElem1 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, turnIsbnIntoElementIfNamesNotUpdated),
        updateNameElementName) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoIsbnUpdate).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // First update names, then update ISBN if names have not been updated (which is not the case)
    val docElem2 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElementIfNamesNotUpdated) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem2.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    // Doing less work
    val docElem3 =
      transformElems(
        transformChildElems(doc.documentElement, turnIsbnIntoElementIfNamesNotUpdated),
        updateNameElementName) ensuring { e =>

          haveMatchingAncestry(e, doc.documentElement)
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }
  }

  test("testEffectivelyUpdateNamesAndIsbn") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 = transformElems(doc.documentElement, turnIsbnIntoElementIfNamesUpdatedAndUpdateNames) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoIsbnUpdate).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }
  }

  test("testEffectivelyUpdateNamesOnly") {
    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val docElem1 = transformElems(doc.documentElement, turnIsbnIntoElementIfNamesNotUpdatedAndUpdateNames) ensuring { e =>
      haveMatchingAncestry(e, doc.documentElement)
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }
  }

  test("testFastUpdateOfSpecificBook") {
    // This shows a technique of fast updates of specific elements (in potentially very large XML documents).
    // The idea is to first find the local element tree to update, and then transform only that element and its descendants.

    val is = classOf[ElemTransformationTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemTransformations._

    val bookElem =
      doc.documentElement.findElem(e => e.localName == "Book" && e.attribute(EName("ISBN")) == "ISBN-0-13-815504-6").get

    val updatedBookElem = transformChildElems(bookElem, updateRemark) ensuring { e =>
      haveMatchingAncestry(e, bookElem)
    }

    assertResult(bookElem.resolvedName) {
      updatedBookElem.resolvedName
    }

    val docElem1 = updatedBookElem.rootElem

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).transformElems(updateRemark).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).removeAllInterElementWhitespace
    }
  }

  private def haveMatchingAncestry(elem1: indexed.Elem, elem2: indexed.Elem): Boolean = {
    elem1.docUriOption == elem2.docUriOption && elem1.path.parentPathOption == elem2.path.parentPathOption
  }

  private def updateNameElementName(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("First_Name") =>
        indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("FirstName")))
      case qn @ UnprefixedName("Last_Name") =>
        indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("LastName")))
      case qn =>
        elm
    }
  }

  private def updateNameElementNameReturningNodeSeq(elm: indexed.Elem): immutable.IndexedSeq[indexed.IndexedScopedNode.Node] = {
    elm.qname match {
      case qn @ UnprefixedName("First_Name") =>
        immutable.IndexedSeq(indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("FirstName"))))
      case qn @ UnprefixedName("Last_Name") =>
        immutable.IndexedSeq(indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("LastName"))))
      case qn =>
        immutable.IndexedSeq(elm)
    }
  }

  private def turnIsbnIntoElement(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("Book") =>
        val isbn = elm.attribute(EName("ISBN"))

        val newElm =
          elm.underlyingElem.
            plusChild(0, simple.Node.textElem(QName("ISBN"), elm.scope, isbn)).
            minusAttribute(QName("ISBN"))
        indexed.Elem.ignoringPathAndDocUri(newElm)
      case qn =>
        elm
    }
  }

  private def turnIsbnIntoElementReturningNodeSeq(elm: indexed.Elem): immutable.IndexedSeq[indexed.IndexedScopedNode.Node] = {
    elm.qname match {
      case qn @ UnprefixedName("Book") =>
        val isbn = elm.attribute(EName("ISBN"))

        val newElm =
          elm.underlyingElem.
            plusChild(0, simple.Node.textElem(QName("ISBN"), elm.scope, isbn)).
            minusAttribute(QName("ISBN"))
        immutable.IndexedSeq(indexed.Elem.ignoringPathAndDocUri(newElm))
      case qn =>
        immutable.IndexedSeq(elm)
    }
  }

  private def turnIsbnIntoElementIfNamesUpdated(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("Book") if elm.filterElems(_.localName == "LastName").nonEmpty =>
        val isbn = elm.attribute(EName("ISBN"))

        val newElm =
          elm.underlyingElem.
            plusChild(0, simple.Node.textElem(QName("ISBN"), elm.scope, isbn)).
            minusAttribute(QName("ISBN"))
        indexed.Elem.ignoringPathAndDocUri(newElm)
      case qn =>
        elm
    }
  }

  private def turnIsbnIntoElementIfNamesNotUpdated(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("Book") if elm.filterElems(_.localName == "LastName").isEmpty =>
        val isbn = elm.attribute(EName("ISBN"))

        val newElm =
          elm.underlyingElem.
            plusChild(0, simple.Node.textElem(QName("ISBN"), elm.scope, isbn)).
            minusAttribute(QName("ISBN"))
        indexed.Elem.ignoringPathAndDocUri(newElm)
      case qn =>
        elm
    }
  }

  // Effectively update names and ISBN. After all, the nested elements are updated first.
  private def turnIsbnIntoElementIfNamesUpdatedAndUpdateNames(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("Book") if elm.filterElems(_.localName == "LastName").nonEmpty =>
        val isbn = elm.attribute(EName("ISBN"))

        val newElm =
          elm.underlyingElem.
            plusChild(0, simple.Node.textElem(QName("ISBN"), elm.scope, isbn)).
            minusAttribute(QName("ISBN"))
        indexed.Elem.ignoringPathAndDocUri(newElm)
      case qn @ UnprefixedName("First_Name") =>
        indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("FirstName")))
      case qn @ UnprefixedName("Last_Name") =>
        indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("LastName")))
      case qn =>
        elm
    }
  }

  // Effectively update names but not ISBN. After all, the nested elements are updated first.
  private def turnIsbnIntoElementIfNamesNotUpdatedAndUpdateNames(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("Book") if elm.filterElems(_.localName == "LastName").isEmpty =>
        val isbn = elm.attribute(EName("ISBN"))

        val newElm =
          elm.underlyingElem.
            plusChild(0, simple.Node.textElem(QName("ISBN"), elm.scope, isbn)).
            minusAttribute(QName("ISBN"))
        indexed.Elem.ignoringPathAndDocUri(newElm)
      case qn @ UnprefixedName("First_Name") =>
        indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("FirstName")))
      case qn @ UnprefixedName("Last_Name") =>
        indexed.Elem.ignoringPathAndDocUri(elm.underlyingElem.copy(qname = QName("LastName")))
      case qn =>
        elm
    }
  }

  private def updateRemark(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("Remark") if elm.parent.attribute(EName("ISBN")) == "ISBN-0-13-815504-6" =>
        val newRemark = "Get a discount on this book combined with \"A First Course\""

        val newElm = elm.underlyingElem.copy(children = Vector(simple.Text(newRemark, false)))
        indexed.Elem.ignoringPathAndDocUri(newElm)
      case qn =>
        elm
    }
  }

  private def undoIsbnUpdate(elm: resolved.Elem): resolved.Elem = {
    elm match {
      case elm @ resolved.Elem(EName(Some("http://bookstore"), "Book"), attrs, children) =>
        elm.
          copy(resolvedAttributes = elm.resolvedAttributes + (EName("ISBN") -> elm.getChildElem(_.resolvedName == EName("http://bookstore", "ISBN")).text)).
          transformChildElemsToNodeSeq(e => if (e.resolvedName == EName("http://bookstore", "ISBN")) Vector() else Vector(e))
      case elm =>
        elm
    }
  }

  private def undoNameUpdate(elm: resolved.Elem): resolved.Elem = {
    elm match {
      case elm @ resolved.Elem(EName(Some("http://bookstore"), "FirstName"), attrs, children) =>
        elm.copy(resolvedName = EName(elm.resolvedName.namespaceUriOption, "First_Name"))
      case elm @ resolved.Elem(EName(Some("http://bookstore"), "LastName"), attrs, children) =>
        elm.copy(resolvedName = EName(elm.resolvedName.namespaceUriOption, "Last_Name"))
      case elm =>
        elm
    }
  }

  private def updateRemark(elm: resolved.Elem): resolved.Elem = {
    elm match {
      case elm @ resolved.Elem(EName(Some("http://bookstore"), "Book"), attrs, children) if elm.attribute(EName("ISBN")) == "ISBN-0-13-815504-6" =>
        val newRemark = "Get a discount on this book combined with \"A First Course\""

        elm transformChildElems { che =>
          if (che.localName == "Remark") {
            che.copy(children = Vector(resolved.Text(newRemark)))
          } else {
            che
          }
        }
      case elm =>
        elm
    }
  }
}
