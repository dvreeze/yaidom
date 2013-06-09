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
import convert.ScalaXmlConversions._

/**
 * Another XML functional update test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AnotherUpdateTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val docParser = DocumentParserUsingDom.newInstance()

  @Test def testDeleteMagazines() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val deleteMags: PartialFunction[Elem, Vector[Node]] = {
      case elem: Elem if elem.localName == "Magazine" => Vector[Node]()
    }

    val docWithoutMags: Document = doc.updatedWithNodeSeq(deleteMags)

    expectResult(doc.documentElement.findAllChildElems.size - 4) {
      docWithoutMags.documentElement.findAllChildElems.size
    }

    expectResult(doc.documentElement.filterElemsOrSelf(_.localName == "Book").size) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Book").size
    }

    expectResult(0) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Magazine").size
    }

    val deleteMagsResolved: PartialFunction[resolved.Elem, Vector[resolved.Node]] = {
      case elem: resolved.Elem if elem.resolvedName.localPart == "Magazine" => Vector[resolved.Node]()
    }

    expectResult(resolved.Elem(docWithoutMags.documentElement)) {
      resolved.Elem(doc.documentElement).updatedWithNodeSeq(deleteMagsResolved)
    }

    testPropertyAboutTopmostUpdatedWithNodeSeq(doc.documentElement, deleteMags)

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, deleteMags)
  }

  @Test def testInsertAfter() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val newBook = {
      val scalaElem =
        <Book xmlns="http://bookstore" ISBN="978-0981531649" Price="37" Edition="2nd">
          <Title>Programming in Scala: A Comprehensive Step-by-Step Guide</Title>
          <Authors>
            <Author>
              <First_Name>Martin</First_Name>
              <Last_Name>Odersky</Last_Name>
            </Author>
            <Author>
              <First_Name>Lex</First_Name>
              <Last_Name>Spoon</Last_Name>
            </Author>
            <Author>
              <First_Name>Bill</First_Name>
              <Last_Name>Venners</Last_Name>
            </Author>
          </Authors>
        </Book>

      convertToElem(scalaElem).notUndeclaringPrefixes(doc.documentElement.scope)
    }

    val lastBookPath: ElemPath = doc.documentElement.filterChildElemPaths(_.localName == "Book").last

    val docWithScalaBook: Document = {
      val result = doc.updatedWithNodeSeq(lastBookPath) { e => Vector(e, newBook) }
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newLastBookPath: ElemPath = docWithScalaBook.documentElement.filterChildElemPaths(_.localName == "Book").last
    val newLastBook: Elem = docWithScalaBook.documentElement.getWithElemPath(newLastBookPath)

    expectResult("Programming in Scala") {
      (newLastBook \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: PartialFunction[Elem, Vector[Node]] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
      }

      docWithScalaBook.updatedWithNodeSeq(deleteNonDatabaseBook)
    }

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    val insertBook: PartialFunction[Elem, immutable.IndexedSeq[Node]] = {
      case e: Elem if e == doc.documentElement.findWithElemPath(lastBookPath).get => Vector(e, newBook)
    }
    testPropertyAboutTopmostUpdatedWithNodeSeq(doc.documentElement, insertBook)

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, lastBookPath, { e => Vector(e, newBook) })

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, insertBook)
  }

  @Test def testInsertBefore() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val newBook = {
      val scalaElem =
        <Book xmlns="http://bookstore" ISBN="978-0981531649" Price="37" Edition="2nd">
          <Title>Programming in Scala: A Comprehensive Step-by-Step Guide</Title>
          <Authors>
            <Author>
              <First_Name>Martin</First_Name>
              <Last_Name>Odersky</Last_Name>
            </Author>
            <Author>
              <First_Name>Lex</First_Name>
              <Last_Name>Spoon</Last_Name>
            </Author>
            <Author>
              <First_Name>Bill</First_Name>
              <Last_Name>Venners</Last_Name>
            </Author>
          </Authors>
        </Book>

      convertToElem(scalaElem).notUndeclaringPrefixes(doc.documentElement.scope)
    }

    val lastBookPath: ElemPath = doc.documentElement.filterChildElemPaths(_.localName == "Book").last

    val docWithScalaBook: Document = {
      val result = doc.updatedWithNodeSeq(lastBookPath) { e => Vector(newBook, e) }
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newLastBookPathButOne: ElemPath = docWithScalaBook.documentElement.filterChildElemPaths(_.localName == "Book").init.last
    val newLastBookButOne: Elem = docWithScalaBook.documentElement.getWithElemPath(newLastBookPathButOne)

    expectResult("Programming in Scala") {
      (newLastBookButOne \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: PartialFunction[Elem, Vector[Node]] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
      }

      docWithScalaBook.updatedWithNodeSeq(deleteNonDatabaseBook)
    }

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    val insertBook: PartialFunction[Elem, immutable.IndexedSeq[Node]] = {
      case e: Elem if e == doc.documentElement.findWithElemPath(lastBookPath).get => Vector(newBook, e)
    }
    testPropertyAboutTopmostUpdatedWithNodeSeq(doc.documentElement, insertBook)

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, lastBookPath, { e => Vector(newBook, e) })

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, insertBook)
  }

  @Test def testInsertAsFirstInto() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val newBook = {
      val scalaElem =
        <Book xmlns="http://bookstore" ISBN="978-0981531649" Price="37" Edition="2nd">
          <Title>Programming in Scala: A Comprehensive Step-by-Step Guide</Title>
          <Authors>
            <Author>
              <First_Name>Martin</First_Name>
              <Last_Name>Odersky</Last_Name>
            </Author>
            <Author>
              <First_Name>Lex</First_Name>
              <Last_Name>Spoon</Last_Name>
            </Author>
            <Author>
              <First_Name>Bill</First_Name>
              <Last_Name>Venners</Last_Name>
            </Author>
          </Authors>
        </Book>

      convertToElem(scalaElem).notUndeclaringPrefixes(doc.documentElement.scope)
    }

    val insertBook: PartialFunction[Elem, Elem] = {
      case e: Elem if e.localName == "Bookstore" => e.plusChild(0, newBook)
    }

    val docWithScalaBook: Document = {
      val result = doc.updated(insertBook)
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newFirstBookPath: ElemPath = docWithScalaBook.documentElement.filterChildElemPaths(_.localName == "Book").head
    val newFirstBook: Elem = docWithScalaBook.documentElement.getWithElemPath(newFirstBookPath)

    expectResult("Programming in Scala") {
      (newFirstBook \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: PartialFunction[Elem, Vector[Node]] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
      }

      docWithScalaBook.updatedWithNodeSeq(deleteNonDatabaseBook)
    }

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    testPropertyAboutTopmostUpdated(doc.documentElement, insertBook)
  }

  @Test def testUpdate() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val updateMag: PartialFunction[Elem, Elem] = {
      case e: Elem if (e \ (_.localName == "Title")).map(_.text).mkString == "National Geographic" =>
        e.plusAttribute(QName("Month"), "January").plusAttribute(QName("Year"), "2010")
    }

    val updatedDoc: Document = {
      val result = doc.updated(updateMag)
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val updateMag2: PartialFunction[Elem, Vector[Node]] = {
      case e: Elem if updateMag.isDefinedAt(e) => Vector(updateMag(e))
    }

    val updatedDoc2: Document = {
      val result = doc.updatedWithNodeSeq(updateMag2)
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    expectResult(resolved.Elem(updatedDoc.documentElement)) {
      resolved.Elem(updatedDoc2.documentElement)
    }

    val newMag = {
      val results =
        (updatedDoc.documentElement \ (che =>
          che.localName == "Magazine" && (che \ (_.localName == "Title")).head.text == "National Geographic"))
      results.head
    }

    expectResult("2010") {
      (newMag \@ EName("Year")).getOrElse("")
    }

    testPropertyAboutTopmostUpdated(doc.documentElement, updateMag)

    testPropertyAboutTopmostUpdatedWithNodeSeq(doc.documentElement, updateMag2)

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, updateMag2)
  }

  private def testPropertyAboutTopmostUpdated(elem: Elem, pf: PartialFunction[Elem, Elem]): Unit = {
    val pf2: PartialFunction[Elem, Elem] = {
      case e: Elem if elem.findTopmostElemsOrSelf(e2 => pf.isDefinedAt(e2)).contains(e) => pf(e)
    }

    expectResult(resolved.Elem(elem.updated(pf2))) {
      resolved.Elem(elem.topmostUpdated(pf))
    }
  }

  private def testPropertyAboutTopmostUpdatedWithNodeSeq(elem: Elem, pf: PartialFunction[Elem, immutable.IndexedSeq[Node]]): Unit = {
    val pf2: PartialFunction[Elem, immutable.IndexedSeq[Node]] = {
      case e: Elem if elem.findTopmostElemsOrSelf(e2 => pf.isDefinedAt(e2)).contains(e) => pf(e)
    }

    expectResult(resolved.Elem(elem.updatedWithNodeSeq(pf2))) {
      resolved.Elem(elem.topmostUpdatedWithNodeSeq(pf))
    }
  }

  private def testPropertyAboutUpdatedWithNodeSeq(elem: Elem, path: ElemPath, f: Elem => immutable.IndexedSeq[Node]): Unit = {
    def g(e: Elem): Elem = {
      if (path == ElemPath.Root) e
      else {
        e.withPatchedChildren(
          e.childNodeIndex(path.lastEntry),
          f(e.findWithElemPathEntry(path.lastEntry).get),
          1)
      }
    }

    val updatedElem = elem.updated(path.parentPathOption.getOrElse(ElemPath.Root))(g)

    expectResult(resolved.Elem(updatedElem)) {
      resolved.Elem(elem.updatedWithNodeSeq(path)(f))
    }
  }

  private def testPropertyAboutUpdatedWithNodeSeq(elem: Elem, pf: PartialFunction[Elem, immutable.IndexedSeq[Node]]): Unit = {
    val pf2: PartialFunction[Elem, Elem] = {
      case e: Elem if !e.filterChildElems(che => pf.isDefinedAt(che)).isEmpty =>
        val childElemsWithPathEntries =
          e.findAllChildElemsWithPathEntries.filter(elemPathPair => pf.isDefinedAt(elemPathPair._1)).reverse

        childElemsWithPathEntries.foldLeft(e) {
          case (acc, (che, pathEntry)) =>
            acc.withPatchedChildren(acc.childNodeIndex(pathEntry), pf(che), 1)
        }
    }

    expectResult(resolved.Elem(elem.updated(pf2))) {
      resolved.Elem(elem.updatedWithNodeSeq(pf))
    }
  }
}
