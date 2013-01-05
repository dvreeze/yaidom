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

  @Test def testUpdateUsingPaths() {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    expect(Set(EName("Price"), EName("Edition"))) {
      attrNames[Node, Elem](doc1.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    expect(Set()) {
      elemNames[Node, Elem](doc1.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val updElem = { (e: Elem, attr: String) => updateBook(e, attr) }
    val doc2 = Document(
      turnBookAttributeIntoElem[Node, Elem](
        turnBookAttributeIntoElem[Node, Elem](doc1.documentElement, "Price", updElem), "Edition", updElem).removeAllInterElementWhitespace)

    expect(Set()) {
      attrNames[Node, Elem](doc2.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    expect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Node, Elem](doc2.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val resolvedOriginalElm = resolved.Elem(doc1.documentElement)
    val resolvedUpdatedElm = resolved.Elem(doc2.documentElement)

    val updResolvedElem = { (e: resolved.Elem, attr: String) => updateBook(e, attr) }
    val updatedResolvedElm =
      turnBookAttributeIntoElem[resolved.Node, resolved.Elem](
        turnBookAttributeIntoElem[resolved.Node, resolved.Elem](resolvedOriginalElm, "Price", updResolvedElem), "Edition", updResolvedElem).removeAllInterElementWhitespace

    expect(false) {
      resolvedOriginalElm == resolvedUpdatedElm
    }

    expect(true) {
      resolvedUpdatedElm == updatedResolvedElm
    }
  }

  @Test def testUpdateUsingUpdatedFunction() {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    expect(Set(EName("Price"), EName("Edition"))) {
      attrNames[Node, Elem](doc1.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    expect(Set()) {
      elemNames[Node, Elem](doc1.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val updElem = { (e: Elem, attr: String) => updateBook(e, attr) }
    val doc2 = Document(
      turnBookAttributeIntoElemUsingUpdatedFunction(
        turnBookAttributeIntoElemUsingUpdatedFunction(doc1.documentElement, "Price", updElem), "Edition", updElem).removeAllInterElementWhitespace)

    expect(Set()) {
      attrNames[Node, Elem](doc2.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    expect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Node, Elem](doc2.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val resolvedOriginalElm = resolved.Elem(doc1.documentElement)
    val resolvedUpdatedElm = resolved.Elem(doc2.documentElement)

    val updResolvedElem = { (e: resolved.Elem, attr: String) => updateBook(e, attr) }
    val updatedResolvedElm =
      turnBookAttributeIntoElemUsingUpdatedFunction(
        turnBookAttributeIntoElemUsingUpdatedFunction(resolvedOriginalElm, "Price", updResolvedElem), "Edition", updResolvedElem).removeAllInterElementWhitespace

    expect(false) {
      resolvedOriginalElm == resolvedUpdatedElm
    }

    expect(true) {
      resolvedUpdatedElm == updatedResolvedElm
    }
  }

  @Test def testUpdateUsingTopmostUpdatedFunction() {
    val is = classOf[UpdateTest].getResourceAsStream("books.xml")

    val doc1: Document = docParser.parse(is)

    expect(Set(EName("Price"), EName("Edition"))) {
      attrNames[Node, Elem](doc1.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    expect(Set()) {
      elemNames[Node, Elem](doc1.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val updElem = { (e: Elem, attr: String) => updateBook(e, attr) }
    val doc2 = Document(
      turnBookAttributeIntoElemUsingTopmostUpdatedFunction(
        turnBookAttributeIntoElemUsingTopmostUpdatedFunction(doc1.documentElement, "Price", updElem), "Edition", updElem).removeAllInterElementWhitespace)

    expect(Set()) {
      attrNames[Node, Elem](doc2.documentElement) intersect Set(EName("Price"), EName("Edition"))
    }
    expect(Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))) {
      elemNames[Node, Elem](doc2.documentElement) intersect Set(EName("{http://bookstore}Price"), EName("{http://bookstore}Edition"))
    }

    val resolvedOriginalElm = resolved.Elem(doc1.documentElement)
    val resolvedUpdatedElm = resolved.Elem(doc2.documentElement)

    val updResolvedElem = { (e: resolved.Elem, attr: String) => updateBook(e, attr) }
    val updatedResolvedElm =
      turnBookAttributeIntoElemUsingTopmostUpdatedFunction(
        turnBookAttributeIntoElemUsingTopmostUpdatedFunction(resolvedOriginalElm, "Price", updResolvedElem), "Edition", updResolvedElem).removeAllInterElementWhitespace

    expect(false) {
      resolvedOriginalElm == resolvedUpdatedElm
    }

    expect(true) {
      resolvedUpdatedElm == updatedResolvedElm
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
      require(rootElm.findWithElemPath(path).isDefined)

      acc.updated(path) { case e => upd(e, attrName) }
    }
  }

  private def turnBookAttributeIntoElemUsingUpdatedFunction(rootElm: Elem, attrName: String, upd: (Elem, String) => Elem): Elem = {
    val pf: PartialFunction[Elem, Elem] = {
      case e: Elem if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined => upd(e, attrName)
    }

    rootElm updated pf
  }

  private def turnBookAttributeIntoElemUsingUpdatedFunction(rootElm: resolved.Elem, attrName: String, upd: (resolved.Elem, String) => resolved.Elem): resolved.Elem = {
    val pf: PartialFunction[resolved.Elem, resolved.Elem] = {
      case e: resolved.Elem if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined => upd(e, attrName)
    }

    rootElm updated pf
  }

  private def turnBookAttributeIntoElemUsingTopmostUpdatedFunction(rootElm: Elem, attrName: String, upd: (Elem, String) => Elem): Elem = {
    val pf: PartialFunction[Elem, Elem] = {
      case e: Elem if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined => upd(e, attrName)
    }

    rootElm topmostUpdated pf
  }

  private def turnBookAttributeIntoElemUsingTopmostUpdatedFunction(rootElm: resolved.Elem, attrName: String, upd: (resolved.Elem, String) => resolved.Elem): resolved.Elem = {
    val pf: PartialFunction[resolved.Elem, resolved.Elem] = {
      case e: resolved.Elem if e.resolvedName == EName("{http://bookstore}Book") && e.attributeOption(EName(attrName)).isDefined => upd(e, attrName)
    }

    rootElm topmostUpdated pf
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
