/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package integrationtest

import java.{ util => jutil, io => jio }
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import javax.xml.transform.{ TransformerFactory, Transformer }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingDom
import print.DocumentPrinterUsingDom
import NodeBuilder._

/**
 * XML functional update test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class UpdateTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val scope = Scope.from("" -> "http://bookstore", "books" -> "http://bookstore")

  private val docParser = DocumentParserUsingDom.newInstance()

  private val docPrinter = {
    val dbf = DocumentBuilderFactory.newInstance
    val tf = TransformerFactory.newInstance

    try {
      tf.getAttribute("indent-number") // Throws an exception if "indent-number" is not supported
      tf.setAttribute("indent-number", java.lang.Integer.valueOf(4))
    } catch {
      case e: Exception => () // Ignore
    }

    DocumentPrinterUsingDom.newInstance(dbf, tf)
  }

  @Test def testUpdateUsingPaths(): Unit = {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    assertResult(Set(EName("Price"), EName("Edition"))) {
      attrNames[Node, Elem](doc1.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    assertResult(Set()) {
      elemNames[Node, Elem](doc1.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val updElem = { (e: Elem, attr: String) => updateBook(e, attr) }
    val doc2 = Document(
      turnBookAttributeIntoElem[Node, Elem](
        turnBookAttributeIntoElem[Node, Elem](doc1.documentElement, "Price", updElem), "Edition", updElem).removeAllInterElementWhitespace)

    assertResult(Set()) {
      attrNames[Node, Elem](doc2.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    assertResult(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Node, Elem](doc2.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val resolvedOriginalElm = resolved.Elem(doc1.documentElement)
    val resolvedUpdatedElm = resolved.Elem(doc2.documentElement)

    val updResolvedElem = { (e: resolved.Elem, attr: String) => updateBook(e, attr) }
    val updatedResolvedElm =
      turnBookAttributeIntoElem[resolved.Node, resolved.Elem](
        turnBookAttributeIntoElem[resolved.Node, resolved.Elem](resolvedOriginalElm, "Price", updResolvedElem), "Edition", updResolvedElem).removeAllInterElementWhitespace

    assertResult(false) {
      resolvedOriginalElm == resolvedUpdatedElm
    }

    assertResult(true) {
      resolvedUpdatedElm == updatedResolvedElm
    }
  }

  @Test def testUpdateUsingPathSet(): Unit = {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    assertResult(Set(EName("Price"), EName("Edition"))) {
      attrNames[Node, Elem](doc1.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    assertResult(Set()) {
      elemNames[Node, Elem](doc1.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val updElem = { (e: Elem, attr: String) => updateBook(e, attr) }
    val doc2 = Document(
      turnBookAttributeIntoElemUsingPathSet[Node, Elem](
        turnBookAttributeIntoElemUsingPathSet[Node, Elem](doc1.documentElement, "Price", updElem), "Edition", updElem).removeAllInterElementWhitespace)

    assertResult(Set()) {
      attrNames[Node, Elem](doc2.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    assertResult(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Node, Elem](doc2.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val resolvedOriginalElm = resolved.Elem(doc1.documentElement)
    val resolvedUpdatedElm = resolved.Elem(doc2.documentElement)

    val updResolvedElem = { (e: resolved.Elem, attr: String) => updateBook(e, attr) }
    val updatedResolvedElm =
      turnBookAttributeIntoElemUsingPathSet[resolved.Node, resolved.Elem](
        turnBookAttributeIntoElemUsingPathSet[resolved.Node, resolved.Elem](resolvedOriginalElm, "Price", updResolvedElem), "Edition", updResolvedElem).removeAllInterElementWhitespace

    assertResult(false) {
      resolvedOriginalElm == resolvedUpdatedElm
    }

    assertResult(true) {
      resolvedUpdatedElm == updatedResolvedElm
    }
  }

  @Test def testUpdateUsingTransform(): Unit = {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    assertResult(Set(EName("Price"), EName("Edition"))) {
      attrNames[Node, Elem](doc1.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    assertResult(Set()) {
      elemNames[Node, Elem](doc1.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val updElem = { (e: Elem, attr: String) => updateBook(e, attr) }
    val doc2 = Document(
      turnBookAttributeIntoElem(
        turnBookAttributeIntoElem(doc1.documentElement, "Price", updElem), "Edition", updElem).removeAllInterElementWhitespace)

    assertResult(Set()) {
      attrNames[Node, Elem](doc2.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    assertResult(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Node, Elem](doc2.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val resolvedOriginalElm = resolved.Elem(doc1.documentElement)
    val resolvedUpdatedElm = resolved.Elem(doc2.documentElement)

    val updResolvedElem = { (e: resolved.Elem, attr: String) => updateBook(e, attr) }
    val updatedResolvedElm =
      turnBookAttributeIntoElem(
        turnBookAttributeIntoElem(resolvedOriginalElm, "Price", updResolvedElem), "Edition", updResolvedElem).removeAllInterElementWhitespace

    assertResult(false) {
      resolvedOriginalElm == resolvedUpdatedElm
    }

    assertResult(true) {
      resolvedUpdatedElm == updatedResolvedElm
    }
  }

  /** Same example as http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser, but now using yaidom functional updates */
  @Test def testAnotherUpdate(): Unit = {
    val is = classOf[UpdateTest].getResourceAsStream("employee.xml")

    val doc: Document = docParser.parse(is)

    import NodeBuilder._

    // Updates on Employee elements:
    // 1. Id attribute prefix with (one char) gender
    // 2. Name element made uppercase. This is coded as a separate case in the partial function below!
    // 3. Element gender removed
    // 4. Element salary added (with value 10000)

    val f: Elem => Elem = {
      case e: Elem if e.localName == "Employee" =>
        val gender = (e \ (_.localName == "gender")) map (_.text) mkString ""
        val genderPrefix = if (gender == "Male") "M" else "F"
        val newId = genderPrefix + (e \@ EName("id")).head

        val scope = e.scope ++ Scope.from("" -> "http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser")
        val salaryElem = textElem(QName("salary"), "10000").build(scope)
        val newChildren = (e.children collect { case che: Elem if che.localName != "gender" => che }) :+ salaryElem
        e.plusAttribute(QName("id"), newId).withChildren(newChildren)
      case e: Elem if e.localName == "name" =>
        e.withChildren(Vector(Text(e.text.toUpperCase, false)))
      case e: Elem => e
    }

    val updatedDoc = doc.transformElemsOrSelf(f)
    val formattedUpdatedDoc = updatedDoc.withDocumentElement(updatedDoc.documentElement.prettify(4))

    logger.info("Result of update (using function updated):%n%s".format(docPrinter.print(formattedUpdatedDoc.documentElement)))

    // Parse and check the file with the expected updated result

    val expectedNewDoc = docParser.parse(classOf[UpdateTest].getResourceAsStream("updatedEmployee.xml"))
    val expectedResolvedNewRoot = resolved.Elem(expectedNewDoc.documentElement.removeAllInterElementWhitespace)

    // Is the parsed expected update result indeed as expected?

    assertResult(Seq("M1", "F2")) {
      (expectedResolvedNewRoot \\ (_.localName == "Employee")) flatMap (_ \@ EName("id"))
    }
    assertResult(Seq("PANKAJ", "LISA")) {
      (expectedResolvedNewRoot \\ (_.localName == "name")) map (_.text)
    }
    assertResult(Seq()) {
      (expectedResolvedNewRoot \\ (_.localName == "gender"))
    }
    assertResult(Seq("10000", "10000")) {
      (expectedResolvedNewRoot \\ (_.localName == "salary")) map (_.text)
    }

    // Finally we check the result of the functional update against this parsed expected update result

    assertResult(expectedResolvedNewRoot) {
      resolved.Elem(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace)
    }

    // Same check, but invoking plusAttribute and minusAttribute as well.
    assertResult(expectedResolvedNewRoot) {
      resolved.Elem(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace.
        minusAttribute(QName("x")).plusAttribute(QName("x"), "v").minusAttribute(QName("x")))
    }
  }

  /** Same example as http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser, but now using yaidom function topmostUpdated */
  @Test def testAnotherUpdateUsingTransformTopmost(): Unit = {
    val is = classOf[UpdateTest].getResourceAsStream("employee.xml")

    val doc: Document = docParser.parse(is)

    import NodeBuilder._

    // Updates on Employee elements:
    // 1. Id attribute prefix with (one char) gender
    // 2. Tried but not picked up: name element made uppercase. This is coded as a separate case in the partial function below!
    // 3. Element gender removed
    // 4. Element salary added (with value 10000)

    val f: Elem => Elem = {
      case e: Elem if e.localName == "Employee" =>
        val gender = (e \ (_.localName == "gender")) map (_.text) mkString ""
        val genderPrefix = if (gender == "Male") "M" else "F"
        val newId = genderPrefix + (e \@ EName("id")).head

        val scope = e.scope ++ Scope.from("" -> "http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser")
        val salaryElem = textElem(QName("salary"), "10000").build(scope)
        val newChildren = (e.children collect { case che: Elem if che.localName != "gender" => che }) :+ salaryElem
        e.plusAttribute(QName("id"), newId).withChildren(newChildren)
      case e: Elem if e.localName == "name" =>
        e.withChildren(Vector(Text(e.text.toUpperCase, false)))
      case e: Elem => e
    }

    // The name update is also picked up.

    val updatedDoc = doc.transformElemsOrSelf(f)
    val formattedUpdatedDoc = updatedDoc.withDocumentElement(updatedDoc.documentElement.prettify(4))

    logger.info("Result of update (using function topmostUpdated):%n%s".format(docPrinter.print(formattedUpdatedDoc.documentElement)))

    // Parse and check the file with the expected updated result

    val expectedNewDoc = docParser.parse(classOf[UpdateTest].getResourceAsStream("updatedEmployee.xml"))
    val expectedResolvedNewRoot = resolved.Elem(expectedNewDoc.documentElement.removeAllInterElementWhitespace)

    // Is the parsed expected update result indeed as expected?

    assertResult(Seq("M1", "F2")) {
      (expectedResolvedNewRoot \\ (_.localName == "Employee")) flatMap (_ \@ EName("id"))
    }
    assertResult(Seq("PANKAJ", "LISA")) {
      (expectedResolvedNewRoot \\ (_.localName == "name")) map (_.text)
    }
    assertResult(Seq()) {
      (expectedResolvedNewRoot \\ (_.localName == "gender"))
    }
    assertResult(Seq("10000", "10000")) {
      (expectedResolvedNewRoot \\ (_.localName == "salary")) map (_.text)
    }

    // Finally we check the result of the functional update against this parsed expected update result

    assertResult(expectedResolvedNewRoot.findAllElemsOrSelf.map(_.resolvedName)) {
      resolved.Elem(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace).findAllElemsOrSelf.map(_.resolvedName)
    }
    assertResult(expectedResolvedNewRoot.findAllElemsOrSelf.flatMap(_.resolvedAttributes)) {
      resolved.Elem(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace).findAllElemsOrSelf.flatMap(_.resolvedAttributes)
    }

    assertResult(expectedResolvedNewRoot) {
      resolved.Elem(formattedUpdatedDoc.documentElement.removeAllInterElementWhitespace)
    }
  }

  private def attrNames[N, E <: N with UpdatableElemLike[N, E]](rootElm: E): Set[EName] = {
    val result = rootElm.findAllElemsOrSelf flatMap { e => e.resolvedAttributes.toMap.keySet }
    result.toSet
  }

  private def elemNames[N, E <: N with UpdatableElemLike[N, E]](rootElm: E): Set[EName] = {
    val result = rootElm.findAllElemsOrSelf map { e => e.resolvedName }
    result.toSet
  }

  private def turnBookAttributeIntoElem[N, E <: N with UpdatableElemLike[N, E]](rootElm: E, attrName: String, upd: (E, String) => E): E = {
    val matchingPaths = rootElm filterElemPaths { e => e.attributeOption(EName(attrName)).isDefined } filter { path =>
      path.endsWithName(EName("{http://bookstore}Book"))
    }

    matchingPaths.reverse.foldLeft(rootElm) { (acc, path) =>
      require(rootElm.findElemOrSelfByPath(path).isDefined)

      acc.updated(path) { case e => upd(e, attrName) }
    }
  }

  private def turnBookAttributeIntoElemUsingPathSet[N, E <: N with UpdatableElemLike[N, E]](rootElm: E, attrName: String, upd: (E, String) => E): E = {
    val matchingPaths = rootElm filterElemPaths { e => e.attributeOption(EName(attrName)).isDefined } filter { path =>
      path.endsWithName(EName("{http://bookstore}Book"))
    }

    rootElm.updatedAtPaths(matchingPaths.toSet) { (elem, path) =>
      require(rootElm.findElemOrSelfByPath(path).isDefined)
      upd(elem, attrName)
    }
  }

  private def turnBookAttributeIntoElem(rootElm: Elem, attrName: String, upd: (Elem, String) => Elem): Elem = {
    val f: Elem => Elem = {
      case e: Elem if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined => upd(e, attrName)
      case e => e
    }

    rootElm.transformElemsOrSelf(f)
  }

  private def turnBookAttributeIntoElem(rootElm: resolved.Elem, attrName: String, upd: (resolved.Elem, String) => resolved.Elem): resolved.Elem = {
    val f: resolved.Elem => resolved.Elem = {
      case e: resolved.Elem if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined => upd(e, attrName)
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
      attributes = bookElm.attributes filterNot { case (qn, v) => qn == QName(attrName) },
      scope = bookElm.scope,
      children = bookElm.children :+ textElem(
        qname = QName(attrName),
        scope = bookElm.scope,
        txt = attrValue))
  }

  def updateBook(bookElm: resolved.Elem, attrName: String): resolved.Elem = {
    require(bookElm.localName == "Book")
    require(bookElm.attributeOption(EName(attrName)).isDefined)

    val attrValue = bookElm.attribute(EName(attrName))

    resolved.Elem(
      resolvedName = bookElm.resolvedName,
      resolvedAttributes = bookElm.resolvedAttributes filterNot { case (en, v) => en == EName(attrName) },
      children = bookElm.children :+ resolved.Elem(
        resolvedName = EName("http://bookstore", attrName),
        resolvedAttributes = Map(),
        children = Vector(resolved.Text(attrValue))))
  }
}
