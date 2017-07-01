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
 * XML functional update function test case, using the ElemUpdateApi for indexed elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ElemUpdateTest extends FunSuite {

  private val docParser = DocumentParserUsingDom.newInstance()

  test("testShowVisitedPaths") {
    // Showing that paths are visited in reverse document order, in order not to break them during updates

    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemUpdates._

    var visitedPaths: immutable.IndexedSeq[Path] = Vector()

    def accumulatePaths(elm: indexed.Elem): indexed.Elem = {
      visitedPaths = visitedPaths :+ elm.path
      elm
    }

    val rootPath = doc.documentElement.path.ensuring(_.isEmpty)

    visitedPaths = Vector()

    updateElems(
      doc.documentElement,
      doc.documentElement.findAllElems.map(_.path.skippingPath(rootPath)).toSet)(toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.findAllElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateElemsOrSelf(
      doc.documentElement,
      doc.documentElement.findAllElemsOrSelf.map(_.path.skippingPath(rootPath)).toSet)(toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.findAllElemsOrSelf.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateChildElems(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath).lastEntry).toSet)(toFunctionTakingElemAndPathEntry(accumulatePaths))

    assertResult(doc.documentElement.findAllChildElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateElemsOrSelf(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath)).toSet)(toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.findAllChildElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateElemsOrSelf(
      doc.documentElement,
      doc.documentElement.filterElems(_.localName.startsWith("Author")).map(_.path.skippingPath(rootPath)).toSet)(
        toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.filterElems(_.localName.startsWith("Author")).map(_.path).reverse) {
      visitedPaths
    }
  }

  test("testShowVisitedPathsForNodeSeqUpdates") {
    // Showing that paths are visited in reverse document order, in order not to break them during updates

    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemUpdates._

    var visitedPaths: immutable.IndexedSeq[Path] = Vector()

    def accumulatePaths(elm: indexed.Elem): immutable.IndexedSeq[indexed.IndexedScopedNode.Node] = {
      visitedPaths = visitedPaths :+ elm.path
      immutable.IndexedSeq(elm)
    }

    val rootPath = doc.documentElement.path.ensuring(_.isEmpty)

    visitedPaths = Vector()

    updateElemsWithNodeSeq(
      doc.documentElement,
      doc.documentElement.findAllElems.map(_.path.skippingPath(rootPath)).toSet)(toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.findAllElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateElemsOrSelfWithNodeSeq(
      doc.documentElement,
      doc.documentElement.findAllElemsOrSelf.map(_.path.skippingPath(rootPath)).toSet)(toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.findAllElemsOrSelf.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateChildElemsWithNodeSeq(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath).lastEntry).toSet)(
        toFunctionTakingElemAndPathEntry(accumulatePaths))

    assertResult(doc.documentElement.findAllChildElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateElemsOrSelfWithNodeSeq(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath)).toSet)(toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.findAllChildElems.map(_.path).reverse) {
      visitedPaths
    }

    visitedPaths = Vector()

    updateElemsOrSelfWithNodeSeq(
      doc.documentElement,
      doc.documentElement.filterElems(_.localName.startsWith("Author")).map(_.path.skippingPath(rootPath)).toSet)(
        toFunctionTakingElemAndPath(accumulatePaths))

    assertResult(doc.documentElement.filterElems(_.localName.startsWith("Author")).map(_.path).reverse) {
      visitedPaths
    }
  }

  test("testUpdateNames") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemUpdates._

    // Clumsy ways of updating name element names

    val rootPath = doc.documentElement.path.ensuring(_.isEmpty)

    val docElem1 = updateElems(
      doc.documentElement,
      doc.documentElement.findAllElems.map(_.path.skippingPath(rootPath)).toSet)(toFunctionTakingElemAndPath(updateNameElementName))

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    val docElem2 = updateElemsOrSelf(
      doc.documentElement,
      doc.documentElement.findAllElemsOrSelf.map(_.path.skippingPath(rootPath)).toSet)(
        toFunctionTakingElemAndPath(updateNameElementName))

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val unchangedDocElem = updateChildElems(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath).lastEntry).toSet)(
        toFunctionTakingElemAndPathEntry(updateNameElementName))

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(unchangedDocElem.underlyingElem)
    }

    val unchangedDocElem2 = updateElems(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath)).toSet)(
        toFunctionTakingElemAndPath(updateNameElementName))

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(unchangedDocElem2.underlyingElem)
    }

    val docElem3 = updateElemsOrSelf(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath)).toSet) { (che, path) =>

        require(che.path == path)

        updateElems(
          che,
          che.findAllElems.map(_.path.skippingPath(che.path)).toSet)(toFunctionTakingElemAndPath(updateNameElementName))
      }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    val docElem4 = updateChildElems(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath).lastEntry).toSet) { (che, pathEntry) =>

        require(che.path.lastEntry == pathEntry)

        updateElemsOrSelf(
          che,
          che.findAllElemsOrSelf.map(_.path.skippingPath(che.path)).toSet)(
            toFunctionTakingElemAndPath(updateNameElementName))
      }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }

    // Finally a more natural way of updating name element names, limiting the paths for which to update

    val authorPaths =
      doc.documentElement.filterElems(e => e.localName == "First_Name" || e.localName == "Last_Name").map(_.path).toSet

    val docElem5 = updateElemsOrSelf(
      doc.documentElement,
      authorPaths.map(_.skippingPath(rootPath)))(toFunctionTakingElemAndPath(updateNameElementName))

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem5.underlyingElem)
    }
  }

  test("testUpdateNamesReturningNodeSeqs") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemUpdates._

    // Clumsy ways of updating name element names

    val rootPath = doc.documentElement.path.ensuring(_.isEmpty)

    val docElem1 = updateElemsWithNodeSeq(
      doc.documentElement,
      doc.documentElement.findAllElems.map(_.path.skippingPath(rootPath)).toSet)(
        toFunctionTakingElemAndPath(updateNameElementNameReturningNodeSeq))

    assertResult(Set("FirstName", "LastName")) {
      docElem1.findAllElemsOrSelf.map(_.resolvedName.localPart).toSet.filter(_.contains("Name"))
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    val docElems2 = updateElemsOrSelfWithNodeSeq(
      doc.documentElement,
      doc.documentElement.findAllElemsOrSelf.map(_.path.skippingPath(rootPath)).toSet)(
        toFunctionTakingElemAndPath(updateNameElementNameReturningNodeSeq))

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

    val unchangedDocElem = updateChildElemsWithNodeSeq(
      doc.documentElement,
      doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath).lastEntry).toSet)(
        toFunctionTakingElemAndPathEntry(updateNameElementNameReturningNodeSeq))

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(unchangedDocElem.underlyingElem)
    }

    val docElem3 =
      updateChildElems(
        doc.documentElement,
        doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath).lastEntry).toSet) { (che, pathEntry) =>

          require(che.path.lastEntry == pathEntry)

          updateElemsWithNodeSeq(
            che,
            che.findAllElems.map(_.path.skippingPath(che.path)).toSet)(
              toFunctionTakingElemAndPath(updateNameElementNameReturningNodeSeq))
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    val docElem4 =
      updateChildElemsWithNodeSeq(
        doc.documentElement,
        doc.documentElement.findAllChildElems.map(_.path.skippingPath(rootPath).lastEntry).toSet) { (che, pathEntry) =>

          require(che.path.lastEntry == pathEntry)

          updateElemsOrSelfWithNodeSeq(
            che,
            che.findAllElemsOrSelf.map(_.path.skippingPath(che.path)).toSet)(
              toFunctionTakingElemAndPath(updateNameElementNameReturningNodeSeq))
        }

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }

    // Finally a more natural way of updating name element names, limiting the paths for which to update

    val authorPaths =
      doc.documentElement.filterElems(e => e.localName == "First_Name" || e.localName == "Last_Name").map(_.path).toSet

    val docElem5 = updateElemsWithNodeSeq(
      doc.documentElement,
      authorPaths.map(_.skippingPath(rootPath)))(toFunctionTakingElemAndPath(updateNameElementNameReturningNodeSeq))

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem5.underlyingElem)
    }
  }

  test("testUpdateIsbnAndNames") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    // Using the transformXXX methods defined below, implemented in terms of ElemUpdateApi methods

    val docElem1 =
      transformElems(
        transformElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement)

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
        turnIsbnIntoElement)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val partiallyChangedDocElem =
      transformChildElems(
        transformChildElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement)

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(partiallyChangedDocElem.underlyingElem).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Changing the order of updates
    val docElem3 =
      transformElems(
        transformElems(doc.documentElement, turnIsbnIntoElement),
        updateNameElementName)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    // Changing the order of updates
    val docElem4 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, turnIsbnIntoElement),
        updateNameElementName)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }

    // Combining transformChildElems with transformElems
    val docElem5 =
      transformChildElems(
        transformElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem5.underlyingElem)
    }

    // Combining transformChildElems with transformElemsOrSelf
    val docElem6 =
      transformChildElems(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElement)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem6.underlyingElem)
    }
  }

  test("testUpdateIsbnAndNamesReturningNodeSeqs") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    // Using the transformXXX methods defined below, implemented in terms of ElemUpdateApi methods

    val docElem1 =
      transformElemsToNodeSeq(
        transformElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq),
        turnIsbnIntoElementReturningNodeSeq)

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
        turnIsbnIntoElementReturningNodeSeq).head.asInstanceOf[indexed.Elem]

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    val partiallyChangedDocElem =
      transformChildElemsToNodeSeq(
        transformChildElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq),
        turnIsbnIntoElementReturningNodeSeq)

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(partiallyChangedDocElem.underlyingElem).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Changing the order of updates
    val docElem3 =
      transformElemsToNodeSeq(
        transformElemsToNodeSeq(doc.documentElement, turnIsbnIntoElementReturningNodeSeq),
        updateNameElementNameReturningNodeSeq)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }

    // Changing the order of updates
    val docElem4 =
      transformElemsOrSelfToNodeSeq(
        transformElemsOrSelfToNodeSeq(doc.documentElement, turnIsbnIntoElementReturningNodeSeq).head.asInstanceOf[indexed.Elem],
        updateNameElementNameReturningNodeSeq).head.asInstanceOf[indexed.Elem]

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }

    // Combining transformChildElemsToNodeSeq with transformElemsToNodeSeq
    val docElem5 =
      transformChildElemsToNodeSeq(
        transformElemsToNodeSeq(doc.documentElement, updateNameElementNameReturningNodeSeq),
        turnIsbnIntoElementReturningNodeSeq)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem5.underlyingElem)
    }

    // Combining transformChildElemsToNodeSeq with transformElemsOrSelf
    val docElem6 =
      transformChildElemsToNodeSeq(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElementReturningNodeSeq)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem6.underlyingElem)
    }
  }

  test("testUpdateIsbnIfNamesUpdated") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    // Using the transformXXX methods defined below, implemented in terms of ElemUpdateApi methods

    // First update names, then update ISBN if names have been updated (which they have)
    val docElem1 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, updateNameElementName),
        turnIsbnIntoElementIfNamesUpdated)

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
    val docElem2 = transformElemsOrSelf(doc.documentElement, turnIsbnIntoElementIfNamesUpdated)

    assertResult(resolved.Elem(doc.documentElement.underlyingElem)) {
      resolved.Elem(docElem2.underlyingElem)
    }

    // Changing the order of updates, resulting in a name update, but no ISBN update
    val docElem3 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, turnIsbnIntoElementIfNamesUpdated),
        updateNameElementName)

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem3.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    // Doing less work
    val docElem4 =
      transformChildElems(
        transformElems(doc.documentElement, updateNameElementName),
        turnIsbnIntoElementIfNamesUpdated)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem4.underlyingElem)
    }
  }

  test("testUpdateIsbnIfNamesNotUpdated") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    // Using the transformXXX methods defined below, implemented in terms of ElemUpdateApi methods

    // First update ISBN if names have not been updated (which is indeed the case), then update names
    val docElem1 =
      transformElemsOrSelf(
        transformElemsOrSelf(doc.documentElement, turnIsbnIntoElementIfNamesNotUpdated),
        updateNameElementName)

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
        turnIsbnIntoElementIfNamesNotUpdated)

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem2.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    // Doing less work
    val docElem3 =
      transformElems(
        transformChildElems(doc.documentElement, turnIsbnIntoElementIfNamesNotUpdated),
        updateNameElementName)

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(docElem3.underlyingElem)
    }
  }

  test("testEffectivelyUpdateNamesAndIsbn") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemUpdates._

    val docElem1 = updateElems(
      doc.documentElement,
      doc.documentElement.findAllElems.map(_.path.skippingPath(doc.documentElement.path)).toSet)(
        toFunctionTakingElemAndPath(turnIsbnIntoElementIfNamesUpdatedAndUpdateNames))

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoIsbnUpdate).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).transformElems(undoIsbnUpdate).removeAllInterElementWhitespace
    }

    // Using the transformXXX methods defined below, implemented in terms of ElemUpdateApi methods

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(transformElems(doc.documentElement, turnIsbnIntoElementIfNamesUpdatedAndUpdateNames).underlyingElem)
    }
  }

  test("testEffectivelyUpdateNamesOnly") {
    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemUpdates._

    val docElem1 = updateElems(
      doc.documentElement,
      doc.documentElement.findAllElems.map(_.path.skippingPath(doc.documentElement.path)).toSet)(
        toFunctionTakingElemAndPath(turnIsbnIntoElementIfNamesNotUpdatedAndUpdateNames))

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).transformElems(undoNameUpdate).removeAllInterElementWhitespace
    }

    // Using the transformXXX methods defined below, implemented in terms of ElemUpdateApi methods

    assertResult(resolved.Elem(docElem1.underlyingElem)) {
      resolved.Elem(transformElems(doc.documentElement, turnIsbnIntoElementIfNamesNotUpdatedAndUpdateNames).underlyingElem)
    }
  }

  test("testFastUpdateOfSpecificBook") {
    // This shows a (sensitive!) technique of fast updates of specific elements (in potentially very large XML documents).
    // The idea is to first find the local element tree to update, and then update only that element and its descendants.

    val is = classOf[ElemUpdateTest].getResourceAsStream("books.xml")

    val doc: indexed.Document = indexed.Document(docParser.parse(is))

    import indexed.Elem.ElemUpdates._

    val bookElem =
      doc.documentElement.findElem(e => e.localName == "Book" && e.attribute(EName("ISBN")) == "ISBN-0-13-815504-6").get

    // Mind the Paths passed, as always. It is safer to use ElemTransformationApi.transformChildElems on bookElem.

    val updatedBookElem =
      updateChildElems(
        bookElem,
        bookElem.findAllChildElems.map(_.path.skippingPath(bookElem.path).lastEntry).toSet)(
          toFunctionTakingElemAndPathEntry(updateRemark)) ensuring { updatedBook =>

            resolved.Elem(updatedBook.underlyingElem) ==
              resolved.Elem(indexed.Elem.ElemTransformations.transformChildElems(
                bookElem,
                updateRemark).underlyingElem)
          }

    assertResult(bookElem.resolvedName) {
      updatedBookElem.resolvedName
    }

    val docElem1 = updatedBookElem.rootElem

    assertResult(resolved.Elem(doc.documentElement.underlyingElem).transformElems(updateRemark).removeAllInterElementWhitespace) {
      resolved.Elem(docElem1.underlyingElem).removeAllInterElementWhitespace
    }

    // Using the transformXXX methods defined below, implemented in terms of ElemUpdateApi methods

    assert(bookElem.path.nonEmpty)

    val updatedBookElem2 = transformElems(bookElem, updateRemark)

    assertResult(resolved.Elem(updatedBookElem.underlyingElem)) {
      resolved.Elem(updatedBookElem2.underlyingElem)
    }
  }

  private def toFunctionTakingElemAndPath[A](f: indexed.Elem => A): ((indexed.Elem, Path) => A) = {
    { (elm, path) =>
      // When called by functions like ElemUpdateApi.updateElemsOrSelf, we can trust the following property to hold
      require(Path(elm.path.entries.takeRight(path.entries.size)) == path)

      f(elm)
    }
  }

  private def toFunctionTakingElemAndPathEntry[A](f: indexed.Elem => A): ((indexed.Elem, Path.Entry) => A) = {
    { (elm, pathEntry) =>
      // When called by functions like ElemUpdateApi.updateChildElems, we can trust the following property to hold
      require(elm.path.lastEntryOption.contains(pathEntry))

      f(elm)
    }
  }

  private def updateNameElementName(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("First_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("FirstName")))
      case qn @ UnprefixedName("Last_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("LastName")))
      case qn =>
        elm
    }
  }

  private def updateNameElementNameReturningNodeSeq(elm: indexed.Elem): immutable.IndexedSeq[indexed.IndexedScopedNode.Node] = {
    elm.qname match {
      case qn @ UnprefixedName("First_Name") =>
        immutable.IndexedSeq(indexed.Elem(elm.underlyingElem.copy(qname = QName("FirstName"))))
      case qn @ UnprefixedName("Last_Name") =>
        immutable.IndexedSeq(indexed.Elem(elm.underlyingElem.copy(qname = QName("LastName"))))
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
        indexed.Elem(newElm)
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
        immutable.IndexedSeq(indexed.Elem(newElm))
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
        indexed.Elem(newElm)
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
        indexed.Elem(newElm)
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
        indexed.Elem(newElm)
      case qn @ UnprefixedName("First_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("FirstName")))
      case qn @ UnprefixedName("Last_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("LastName")))
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
        indexed.Elem(newElm)
      case qn @ UnprefixedName("First_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("FirstName")))
      case qn @ UnprefixedName("Last_Name") =>
        indexed.Elem(elm.underlyingElem.copy(qname = QName("LastName")))
      case qn =>
        elm
    }
  }

  private def updateRemark(elm: indexed.Elem): indexed.Elem = {
    elm.qname match {
      case qn @ UnprefixedName("Remark") if elm.parent.attribute(EName("ISBN")) == "ISBN-0-13-815504-6" =>
        val newRemark = "Get a discount on this book commbined with \"A First Course\""

        val newElm = elm.underlyingElem.copy(children = Vector(simple.Text(newRemark, false)))
        indexed.Elem(newElm)
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
        val newRemark = "Get a discount on this book commbined with \"A First Course\""

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

  // Redefining ElemTransformationApi methods in terms of ElemUpdateApi methods

  private def transformChildElems(elem: indexed.Elem, f: indexed.Elem => indexed.Elem): indexed.Elem = {
    import indexed.Elem.ElemUpdates._

    val pathEntries: Set[Path.Entry] = elem.findAllChildElems.map(_.path.lastEntry).toSet

    updateChildElems(elem, pathEntries)(toFunctionTakingElemAndPathEntry(f)) ensuring { resultElem =>
      resolved.Elem(resultElem.underlyingElem) ==
        resolved.Elem(indexed.Elem.ElemTransformations.transformChildElems(elem, f).underlyingElem)
    }
  }

  private def transformChildElemsToNodeSeq(
    elem: indexed.Elem,
    f: indexed.Elem => immutable.IndexedSeq[indexed.IndexedScopedNode.Node]): indexed.Elem = {

    import indexed.Elem.ElemUpdates._

    val pathEntries: Set[Path.Entry] = elem.findAllChildElems.map(_.path.lastEntry).toSet

    updateChildElemsWithNodeSeq(elem, pathEntries)(toFunctionTakingElemAndPathEntry(f)) ensuring { resultElem =>
      resolved.Elem(resultElem.underlyingElem) ==
        resolved.Elem(indexed.Elem.ElemTransformations.transformChildElemsToNodeSeq(elem, f).underlyingElem)
    }
  }

  private def transformElemsOrSelf(elem: indexed.Elem, f: indexed.Elem => indexed.Elem): indexed.Elem = {
    import indexed.Elem.ElemUpdates._

    val paths: Set[Path] = elem.findAllElemsOrSelf.map(_.path.skippingPath(elem.path)).toSet

    updateElemsOrSelf(elem, paths)(toFunctionTakingElemAndPath(f)) ensuring { resultElem =>
      resolved.Elem(resultElem.underlyingElem) ==
        resolved.Elem(indexed.Elem.ElemTransformations.transformElemsOrSelf(elem, f).underlyingElem)
    }
  }

  private def transformElems(elem: indexed.Elem, f: indexed.Elem => indexed.Elem): indexed.Elem = {
    import indexed.Elem.ElemUpdates._

    val paths: Set[Path] = elem.findAllElems.map(_.path.skippingPath(elem.path)).toSet

    updateElems(elem, paths)(toFunctionTakingElemAndPath(f)) ensuring { resultElem =>
      resolved.Elem(resultElem.underlyingElem) ==
        resolved.Elem(indexed.Elem.ElemTransformations.transformElems(elem, f).underlyingElem)
    }
  }

  private def transformElemsOrSelfToNodeSeq(
    elem: indexed.Elem,
    f: indexed.Elem => immutable.IndexedSeq[indexed.IndexedScopedNode.Node]): immutable.IndexedSeq[indexed.IndexedScopedNode.Node] = {

    import indexed.Elem.ElemUpdates._

    val paths: Set[Path] = elem.findAllElemsOrSelf.map(_.path.skippingPath(elem.path)).toSet

    updateElemsOrSelfWithNodeSeq(elem, paths)(toFunctionTakingElemAndPath(f)) ensuring { resultNodes =>
      val resultElems =
        resultNodes collect { case e: indexed.IndexedScopedNode.Elem[_] => e.asInstanceOf[indexed.Elem] }

      val expectedResultElems =
        indexed.Elem.ElemTransformations.transformElemsOrSelfToNodeSeq(elem, f) collect {
          case e: indexed.IndexedScopedNode.Elem[_] => e.asInstanceOf[indexed.Elem]
        }

      resultElems.map(e => resolved.Elem(e.underlyingElem)) == expectedResultElems.map(e => resolved.Elem(e.underlyingElem))
    }
  }

  private def transformElemsToNodeSeq(
    elem: indexed.Elem,
    f: indexed.Elem => immutable.IndexedSeq[indexed.IndexedScopedNode.Node]): indexed.Elem = {

    import indexed.Elem.ElemUpdates._

    val paths: Set[Path] = elem.findAllElems.map(_.path.skippingPath(elem.path)).toSet

    updateElemsWithNodeSeq(elem, paths)(toFunctionTakingElemAndPath(f)) ensuring { resultElem =>
      resolved.Elem(resultElem.underlyingElem) ==
        resolved.Elem(indexed.Elem.ElemTransformations.transformElemsToNodeSeq(elem, f).underlyingElem)
    }
  }
}
