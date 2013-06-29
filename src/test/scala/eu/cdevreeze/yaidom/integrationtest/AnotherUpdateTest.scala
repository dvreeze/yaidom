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
 * Another XML functional update test case. More precisely, mainly a transformation test case.
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

    val deleteMags: Elem => Vector[Node] = {
      case elem: Elem if elem.localName == "Magazine" => Vector[Node]()
      case elem: Elem => Vector(elem)
    }

    val docWithoutMags: Document = doc.transformElemsToNodeSeq(deleteMags)

    expectResult(doc.documentElement.findAllChildElems.size - 4) {
      docWithoutMags.documentElement.findAllChildElems.size
    }

    expectResult(doc.documentElement.filterElemsOrSelf(_.localName == "Book").size) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Book").size
    }

    expectResult(0) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Magazine").size
    }

    val deleteMagsResolved: resolved.Elem => Vector[resolved.Node] = {
      case elem: resolved.Elem if elem.resolvedName.localPart == "Magazine" => Vector[resolved.Node]()
      case elem: resolved.Elem => Vector(elem)
    }

    expectResult(resolved.Elem(docWithoutMags.documentElement)) {
      resolved.Elem(doc.documentElement).transformElemsToNodeSeq(deleteMagsResolved)
    }

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, deleteMags)
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
      val deleteNonDatabaseBook: Elem => Vector[Node] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
        case e: Elem => Vector(e)
      }

      docWithScalaBook.transformElemsToNodeSeq(deleteNonDatabaseBook)
    }

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    val insertBook: Elem => immutable.IndexedSeq[Node] = {
      case e: Elem if e == doc.documentElement.findWithElemPath(lastBookPath).get => Vector(e, newBook)
      case e: Elem => Vector(e)
    }

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, lastBookPath, { e => Vector(e, newBook) })

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, insertBook)
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
      val deleteNonDatabaseBook: Elem => Vector[Node] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
        case e: Elem => Vector(e)
      }

      docWithScalaBook.transformElemsToNodeSeq(deleteNonDatabaseBook)
    }

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    val insertBook: Elem => immutable.IndexedSeq[Node] = {
      case e: Elem if e == doc.documentElement.findWithElemPath(lastBookPath).get => Vector(newBook, e)
      case e: Elem => Vector(e)
    }

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, lastBookPath, { e => Vector(newBook, e) })

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, insertBook)
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

    val insertBook: Elem => Elem = {
      case e: Elem if e.localName == "Bookstore" => e.plusChild(0, newBook)
      case e: Elem => e
    }

    val docWithScalaBook: Document = {
      val result = doc.transformElemsOrSelf(insertBook)
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newFirstBookPath: ElemPath = docWithScalaBook.documentElement.filterChildElemPaths(_.localName == "Book").head
    val newFirstBook: Elem = docWithScalaBook.documentElement.getWithElemPath(newFirstBookPath)

    expectResult("Programming in Scala") {
      (newFirstBook \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: Elem => Vector[Node] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
        case e: Elem => Vector(e)
      }

      docWithScalaBook.transformElemsToNodeSeq(deleteNonDatabaseBook)
    }

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, { e => Vector(insertBook(e)) })
  }

  @Test def testUpdate() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val updateMag: Elem => Elem = {
      case e: Elem if (e \ (_.localName == "Title")).map(_.text).mkString == "National Geographic" =>
        e.plusAttribute(QName("Month"), "January").plusAttribute(QName("Year"), "2010")
      case e: Elem => e
    }

    val updatedDoc: Document = {
      val result = doc.transformElemsOrSelf(updateMag)
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val updateMag2: Elem => Vector[Node] = {
      case e: Elem if (e \ (_.localName == "Title")).map(_.text).mkString == "National Geographic" => Vector(updateMag(e))
      case e: Elem => Vector(e)
    }

    val updatedDoc2: Document = {
      val result = doc.transformElemsToNodeSeq(updateMag2)
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

    testPropertyAboutTransformChildElemsInTermsOfUpdated(doc.documentElement, updateMag)

    testPropertyAboutTransformElemsOrSelfInTermsOfUpdated(doc.documentElement, updateMag)

    testPropertyAboutTransformElemsInTermsOfUpdated(doc.documentElement, updateMag)

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, { e => Vector(updateMag(e)) })
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

  private def testPropertyAboutTransformChildElemsInTermsOfUpdated(elem: Elem, f: Elem => Elem): Unit = {
    val expectedResult = elem.findAllChildElemPaths.reverse.foldLeft(elem) { (acc, path) =>
      acc.updated(path)(f)
    }

    expectResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformChildElems(f))
    }

    val expectedResult2 = elem.findAllChildElemPathEntries.reverse.foldLeft(elem) { (acc, pathEntry) =>
      acc.updated(pathEntry)(f)
    }

    expectResult(resolved.Elem(expectedResult2)) {
      resolved.Elem(elem.transformChildElems(f))
    }
  }

  private def testPropertyAboutTransformElemsOrSelfInTermsOfUpdated(elem: Elem, f: Elem => Elem): Unit = {
    val expectedResult = elem.findAllElemOrSelfPaths.reverse.foldLeft(elem) { (acc, path) =>
      acc.updated(path)(f)
    }

    expectResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformElemsOrSelf(f))
    }
  }

  private def testPropertyAboutTransformElemsInTermsOfUpdated(elem: Elem, f: Elem => Elem): Unit = {
    val expectedResult = elem.findAllElemPaths.reverse.foldLeft(elem) { (acc, path) =>
      acc.updated(path)(f)
    }

    expectResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformElems(f))
    }
  }

  private def testPropertyAboutTransformElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Unit = {
    def g(elem: Elem): Elem = elem.transformChildElemsToNodeSeq(che => f(che))

    val expectedResult = elem.transformElemsOrSelf(g)

    expectResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformElemsToNodeSeq(f))
    }
  }
}
