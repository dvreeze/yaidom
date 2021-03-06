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

import java.{util => jutil}

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.queryapi.UpdatableElemLike
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.Text
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import org.scalatest.funsuite.AnyFunSuite

import scala.collection.immutable

/**
 * XML functional update test case.
 *
 * @author Chris de Vreeze
 */
class UpdateTest extends AnyFunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val docParser = DocumentParserUsingDom.newInstance()

  private val docPrinter = {
    val dbf = DocumentBuilderFactory.newInstance
    val tf = TransformerFactory.newInstance

    try {
      tf.getAttribute("indent-number") // Throws an exception if "indent-number" is not supported
      tf.setAttribute("indent-number", java.lang.Integer.valueOf(4))
    } catch {
      case _: Exception => () // Ignore
    }

    DocumentPrinterUsingDom.newInstance(dbf, tf)
  }

  test("testUpdateUsingPaths") {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    assertResult(Set(EName("Price"), EName("Edition"))) {
      attrNames[Elem](doc1.documentElement).intersect(Set(EName("Price"), EName("Edition")))
    }
    assertResult(Set()) {
      elemNames[Elem](doc1.documentElement)
        .intersect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition")))
    }

    val updElem = { (e: Elem, attr: String) =>
      updateBook(e, attr)
    }
    val doc2 = Document(turnBookAttributeIntoElem(
      turnBookAttributeIntoElem(doc1.documentElement, "Price", updElem),
      "Edition",
      updElem).removeAllInterElementWhitespace)

    assertResult(Set()) {
      attrNames[Elem](doc2.documentElement).intersect(Set(EName("Price"), EName("Edition")))
    }
    assertResult(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Elem](doc2.documentElement)
        .intersect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition")))
    }
  }

  test("testUpdateUsingPathSet") {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    assertResult(Set(EName("Price"), EName("Edition"))) {
      attrNames[Elem](doc1.documentElement).intersect(Set(EName("Price"), EName("Edition")))
    }
    assertResult(Set()) {
      elemNames[Elem](doc1.documentElement)
        .intersect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition")))
    }

    val updElem = { (e: Elem, attr: String) =>
      updateBook(e, attr)
    }
    val doc2 = Document(
      turnBookAttributeIntoElemUsingPathSet(
        turnBookAttributeIntoElemUsingPathSet(doc1.documentElement, "Price", updElem),
        "Edition",
        updElem).removeAllInterElementWhitespace)

    assertResult(Set()) {
      attrNames[Elem](doc2.documentElement).intersect(Set(EName("Price"), EName("Edition")))
    }
    assertResult(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Elem](doc2.documentElement)
        .intersect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition")))
    }
  }

  test("testUpdateUsingTransform") {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    assertResult(Set(EName("Price"), EName("Edition"))) {
      attrNames[Elem](doc1.documentElement).intersect(Set(EName("Price"), EName("Edition")))
    }
    assertResult(Set()) {
      elemNames[Elem](doc1.documentElement)
        .intersect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition")))
    }

    val updElem = { (e: Elem, attr: String) =>
      updateBook(e, attr)
    }
    val doc2 = Document(
      turnBookAttributeIntoElemUsingTransform(
        turnBookAttributeIntoElemUsingTransform(doc1.documentElement, "Price", updElem),
        "Edition",
        updElem).removeAllInterElementWhitespace)

    assertResult(Set()) {
      attrNames[Elem](doc2.documentElement).intersect(Set(EName("Price"), EName("Edition")))
    }
    assertResult(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Elem](doc2.documentElement)
        .intersect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition")))
    }

    val resolvedOriginalElm = resolved.Elem.from(doc1.documentElement)
    val resolvedUpdatedElm = resolved.Elem.from(doc2.documentElement)

    val updResolvedElem = { (e: resolved.Elem, attr: String) =>
      updateBook(e, attr)
    }
    val updatedResolvedElm =
      turnBookAttributeIntoElemUsingTransform(
        turnBookAttributeIntoElemUsingTransform(resolvedOriginalElm, "Price", updResolvedElem),
        "Edition",
        updResolvedElem).removeAllInterElementWhitespace

    assertResult(false) {
      resolvedOriginalElm == resolvedUpdatedElm
    }

    assertResult(true) {
      resolvedUpdatedElm == updatedResolvedElm
    }
  }

  /** Same example as http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser, but now using yaidom functional updates */
  test("testAnotherUpdate") {
    val is = classOf[UpdateTest].getResourceAsStream("employee.xml")

    val doc: Document = docParser.parse(is)

    import Node._

    // Updates on Employee elements:
    // 1. Id attribute prefix with (one char) gender
    // 2. Name element made uppercase. This is coded as a separate case in the partial function below!
    // 3. Element gender removed
    // 4. Element salary added (with value 10000)

    val f: Elem => Elem = {
      case e @ Elem(QName(_, "Employee"), _, _, _) =>
        val gender = (e \ (_.localName == "gender")).map(_.text).mkString("")
        val genderPrefix = if (gender == "Male") "M" else "F"
        val newId = genderPrefix + (e \@ EName("id")).head

        val scope = e.scope ++ Scope.from("" -> "http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser")
        val salaryElem = textElem(QName("salary"), scope, "10000")
        val newChildren = e.children.collect { case che: Elem if che.localName != "gender" => che } :+ salaryElem
        e.plusAttribute(QName("id"), newId).withChildren(newChildren)
      case e @ Elem(QName(_, "name"), _, _, _) =>
        e.withChildren(Vector(Text(e.text.toUpperCase, isCData = false)))
      case e: Elem => e
    }

    val updatedDoc = doc.transformElemsOrSelf(f)
    val formattedUpdatedDoc = updatedDoc.withDocumentElement(updatedDoc.documentElement.prettify(4))

    logger.info(
      "Result of update (using function updated):%n%s".format(docPrinter.print(formattedUpdatedDoc.documentElement)))

    // Parse and check the file with the expected updated result

    val expectedNewDoc = docParser.parse(classOf[UpdateTest].getResourceAsStream("updatedEmployee.xml"))
    val expectedResolvedNewRoot = resolved.Elem.from(expectedNewDoc.documentElement.removeAllInterElementWhitespace)

    // Is the parsed expected update result indeed as expected?

    assertResult(Seq("M1", "F2")) {
      (expectedResolvedNewRoot \\ (_.localName == "Employee")).flatMap(_ \@ EName("id"))
    }
    assertResult(Seq("PANKAJ", "LISA")) {
      (expectedResolvedNewRoot \\ (_.localName == "name")).map(_.text)
    }
    assertResult(Seq()) {
      expectedResolvedNewRoot \\ (_.localName == "gender")
    }
    assertResult(Seq("10000", "10000")) {
      (expectedResolvedNewRoot \\ (_.localName == "salary")).map(_.text)
    }

    // Finally we check the result of the functional update against this parsed expected update result

    assertResult(expectedResolvedNewRoot) {
      resolved.Elem.from(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace)
    }

    // Same check, but invoking plusAttribute and minusAttribute as well.
    assertResult(expectedResolvedNewRoot) {
      resolved.Elem.from(
        formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace
          .minusAttribute(QName("x"))
          .plusAttribute(QName("x"), "v")
          .minusAttribute(QName("x")))
    }
  }

  /** Same example as http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser, but now using yaidom function topmostUpdated */
  test("testAnotherUpdateUsingTransformTopmost") {
    val is = classOf[UpdateTest].getResourceAsStream("employee.xml")

    val doc: Document = docParser.parse(is)

    import Node._

    // Updates on Employee elements:
    // 1. Id attribute prefix with (one char) gender
    // 2. Tried but not picked up: name element made uppercase. This is coded as a separate case in the partial function below!
    // 3. Element gender removed
    // 4. Element salary added (with value 10000)

    val f: Elem => Elem = {
      case e @ Elem(QName(_, "Employee"), _, _, _) =>
        val gender = (e \ (_.localName == "gender")).map(_.text).mkString("")
        val genderPrefix = if (gender == "Male") "M" else "F"
        val newId = genderPrefix + (e \@ EName("id")).head

        val scope = e.scope ++ Scope.from("" -> "http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser")
        val salaryElem = textElem(QName("salary"), scope, "10000")
        val newChildren = e.children.collect { case che: Elem if che.localName != "gender" => che } :+ salaryElem
        e.plusAttribute(QName("id"), newId).withChildren(newChildren)
      case e @ Elem(QName(_, "name"), _, _, _) =>
        e.withChildren(Vector(Text(e.text.toUpperCase, isCData = false)))
      case e: Elem => e
    }

    // The name update is also picked up.

    val updatedDoc = doc.transformElemsOrSelf(f)
    val formattedUpdatedDoc = updatedDoc.withDocumentElement(updatedDoc.documentElement.prettify(4))

    logger.info(
      "Result of update (using function topmostUpdated):%n%s".format(
        docPrinter.print(formattedUpdatedDoc.documentElement)))

    // Parse and check the file with the expected updated result

    val expectedNewDoc = docParser.parse(classOf[UpdateTest].getResourceAsStream("updatedEmployee.xml"))
    val expectedResolvedNewRoot = resolved.Elem.from(expectedNewDoc.documentElement.removeAllInterElementWhitespace)

    // Is the parsed expected update result indeed as expected?

    assertResult(Seq("M1", "F2")) {
      (expectedResolvedNewRoot \\ (_.localName == "Employee")).flatMap(_ \@ EName("id"))
    }
    assertResult(Seq("PANKAJ", "LISA")) {
      (expectedResolvedNewRoot \\ (_.localName == "name")).map(_.text)
    }
    assertResult(Seq()) {
      expectedResolvedNewRoot \\ (_.localName == "gender")
    }
    assertResult(Seq("10000", "10000")) {
      (expectedResolvedNewRoot \\ (_.localName == "salary")).map(_.text)
    }

    // Finally we check the result of the functional update against this parsed expected update result

    assertResult(expectedResolvedNewRoot.findAllElemsOrSelf.map(_.resolvedName)) {
      resolved.Elem
        .from(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace)
        .findAllElemsOrSelf
        .map(_.resolvedName)
    }
    assertResult(expectedResolvedNewRoot.findAllElemsOrSelf.flatMap(_.resolvedAttributes)) {
      resolved.Elem
        .from(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace)
        .findAllElemsOrSelf
        .flatMap(_.resolvedAttributes)
    }

    assertResult(expectedResolvedNewRoot) {
      resolved.Elem.from(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace)
    }
  }

  private def attrNames[E <: ClarkNodes.Elem.Aux[_, E] with UpdatableElemLike.Aux[_, E]](rootElm: E): Set[EName] = {
    val allElems = rootElm.findAllElemsOrSelf.asInstanceOf[immutable.IndexedSeq[ClarkElemApi]]
    val result = allElems.flatMap(e => e.resolvedAttributes.toMap.keySet)
    result.toSet
  }

  private def elemNames[E <: ClarkNodes.Elem.Aux[_, E] with UpdatableElemLike.Aux[_, E]](rootElm: E): Set[EName] = {
    val allElems = rootElm.findAllElemsOrSelf.asInstanceOf[immutable.IndexedSeq[ClarkElemApi]]
    val result = allElems.map { e =>
      e.resolvedName
    }
    result.toSet
  }

  private def turnBookAttributeIntoElem(rootElm: Elem, attrName: String, upd: (Elem, String) => Elem): Elem = {
    // Regression in Scala 2.13.0-M3:
    // Cannot construct a collection of type That with elements of type eu.cdevreeze.yaidom.core.Path based on
    // a collection of type scala.collection.immutable.IndexedSeq[eu.cdevreeze.yaidom.indexed.IndexedScopedNode.Elem[eu.cdevreeze.yaidom.simple.Elem]].
    // Circumventing this compilation error by introducing an extra variable for the indexed.Elem.

    val indexedRootElm = indexed.Elem(rootElm)
    val matchingPaths =
      indexedRootElm
        .filterElems { e =>
          e.attributeOption(EName(attrName)).isDefined && e.path.endsWithName(EName("{http://bookstore}Book"))
        }
        .map(_.path)

    matchingPaths.reverse.foldLeft(rootElm) { (acc, path) =>
      require(rootElm.findElemOrSelfByPath(path).isDefined)

      acc.updateElemOrSelf(path) { e =>
        upd(e, attrName)
      }
    }
  }

  private def turnBookAttributeIntoElemUsingPathSet(
      rootElm: Elem,
      attrName: String,
      upd: (Elem, String) => Elem): Elem = {
    // Regression in Scala 2.13.0-M3:
    // Cannot construct a collection of type That with elements of type eu.cdevreeze.yaidom.core.Path based on
    // a collection of type scala.collection.immutable.IndexedSeq[eu.cdevreeze.yaidom.indexed.IndexedScopedNode.Elem[eu.cdevreeze.yaidom.simple.Elem]].
    // Circumventing this compilation error by introducing an extra variable for the indexed.Elem.

    val indexedRootElm = indexed.Elem(rootElm)
    val matchingPaths =
      indexedRootElm
        .filterElems { e =>
          e.attributeOption(EName(attrName)).isDefined && e.path.endsWithName(EName("{http://bookstore}Book"))
        }
        .map(_.path)

    rootElm.updateElemsOrSelf(matchingPaths.toSet) { (elem, path) =>
      require(rootElm.findElemOrSelfByPath(path).isDefined)
      upd(elem, attrName)
    }
  }

  private def turnBookAttributeIntoElemUsingTransform(
      rootElm: Elem,
      attrName: String,
      upd: (Elem, String) => Elem): Elem = {
    val f: Elem => Elem = {
      case e: Elem
          if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined =>
        upd(e, attrName)
      case e => e
    }

    rootElm.transformElemsOrSelf(f)
  }

  private def turnBookAttributeIntoElemUsingTransform(
      rootElm: resolved.Elem,
      attrName: String,
      upd: (resolved.Elem, String) => resolved.Elem): resolved.Elem = {
    val f: resolved.Elem => resolved.Elem = {
      case e: resolved.Elem
          if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined =>
        upd(e, attrName)
      case e => e
    }

    rootElm.transformElemsOrSelf(f)
  }

  def updateBook(bookElm: Elem, attrName: String): Elem = {
    require(bookElm.localName == "Book")
    require(bookElm.attributeOption(EName(attrName)).isDefined)

    val attrValue = bookElm.attribute(EName(attrName))

    import Node._

    elem(
      qname = bookElm.qname,
      attributes = bookElm.attributes.filterNot { case (qn, _) => qn == QName(attrName) },
      scope = bookElm.scope,
      children = bookElm.children :+ textElem(qname = QName(attrName), scope = bookElm.scope, txt = attrValue)
    )
  }

  def updateBook(bookElm: resolved.Elem, attrName: String): resolved.Elem = {
    require(bookElm.localName == "Book")
    require(bookElm.attributeOption(EName(attrName)).isDefined)

    val attrValue = bookElm.attribute(EName(attrName))

    resolved.Elem(
      resolvedName = bookElm.resolvedName,
      resolvedAttributes = bookElm.resolvedAttributes.filterNot { case (en, _) => en == EName(attrName) },
      children = bookElm.children :+ resolved.Elem(
        resolvedName = EName("http://bookstore", attrName),
        resolvedAttributes = Map(),
        children = Vector(resolved.Text(attrValue)))
    )
  }
}
