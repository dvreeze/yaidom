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

import scala.Vector
import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.resolved

/**
 * Another XML functional update test case. More precisely, mainly a transformation test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AnotherUpdateTest extends FunSuite {

  private val docParser = DocumentParserUsingDom.newInstance()

  test("testDeleteMagazines") {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val deleteMags: Elem => Vector[Node] = {
      case Elem(QName(_, "Magazine"), _, _, _) => Vector[Node]()
      case elem: Elem                          => Vector(elem)
    }

    val docWithoutMags: Document = doc.transformElemsToNodeSeq(deleteMags)

    assertResult(doc.documentElement.findAllChildElems.size - 4) {
      docWithoutMags.documentElement.findAllChildElems.size
    }

    assertResult(doc.documentElement.filterElemsOrSelf(_.localName == "Book").size) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Book").size
    }

    assertResult(0) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Magazine").size
    }

    val deleteMagsResolved: resolved.Elem => Vector[resolved.Node] = {
      case resolved.Elem(EName(_, "Magazine"), _, _) => Vector[resolved.Node]()
      case elem: resolved.Elem                       => Vector(elem)
    }

    assertResult(resolved.Elem(docWithoutMags.documentElement)) {
      resolved.Elem(doc.documentElement).transformElemsToNodeSeq(deleteMagsResolved)
    }

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, deleteMags)

    testSymmetryPropertyAboutTransformElemsToNodeSeq(doc.documentElement, deleteMags, deleteMagsResolved)
  }

  test("testInsertAfter") {
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

    val lastBookPath: Path =
      indexed.Elem(doc.documentElement).filterChildElems(_.localName == "Book").map(_.path).last

    val docWithScalaBook: Document = {
      val result = doc.updateElemWithNodeSeq(lastBookPath) { e => Vector(e, newBook) }
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newLastBookPath: Path =
      indexed.Elem(docWithScalaBook.documentElement).filterChildElems(_.localName == "Book").map(_.path).last
    val newLastBook: Elem = docWithScalaBook.documentElement.getElemOrSelfByPath(newLastBookPath)

    assertResult("Programming in Scala") {
      (newLastBook \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: Elem => Vector[Node] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
        case e: Elem => Vector(e)
      }

      docWithScalaBook.transformElemsToNodeSeq(deleteNonDatabaseBook)
    }

    assertResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    val insertBook: Elem => immutable.IndexedSeq[Node] = {
      case e: Elem if resolved.Elem(e) == resolved.Elem(doc.documentElement.getElemOrSelfByPath(lastBookPath)) =>
        Vector(e, newBook)
      case e: Elem => Vector(e)
    }

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, lastBookPath, { e => Vector(e, newBook) })

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, insertBook)

    testSymmetryPropertyAboutTransformElemsToNodeSeq(doc.documentElement, insertBook, {
      case e: resolved.Elem if e == resolved.Elem(doc.documentElement.getElemOrSelfByPath(lastBookPath)) =>
        Vector(e, resolved.Elem(newBook))
      case e: resolved.Elem => Vector(e)
    })
  }

  test("testInsertBefore") {
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

    val lastBookPath: Path =
      indexed.Elem(doc.documentElement).filterChildElems(_.localName == "Book").map(_.path).last

    val docWithScalaBook: Document = {
      val result = doc.updateElemWithNodeSeq(lastBookPath) { e => Vector(newBook, e) }
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newLastBookPathButOne: Path =
      indexed.Elem(docWithScalaBook.documentElement).filterChildElems(_.localName == "Book").map(_.path).init.last
    val newLastBookButOne: Elem = docWithScalaBook.documentElement.getElemOrSelfByPath(newLastBookPathButOne)

    assertResult("Programming in Scala") {
      (newLastBookButOne \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: Elem => Vector[Node] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
        case e: Elem => Vector(e)
      }

      docWithScalaBook.transformElemsToNodeSeq(deleteNonDatabaseBook)
    }

    assertResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    val insertBook: Elem => immutable.IndexedSeq[Node] = {
      case e: Elem if resolved.Elem(e) == resolved.Elem(doc.documentElement.getElemOrSelfByPath(lastBookPath)) =>
        Vector(newBook, e)
      case e: Elem => Vector(e)
    }

    testPropertyAboutUpdatedWithNodeSeq(doc.documentElement, lastBookPath, { e => Vector(newBook, e) })

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, insertBook)

    testSymmetryPropertyAboutTransformElemsToNodeSeq(doc.documentElement, insertBook, {
      case e: resolved.Elem if e == resolved.Elem(doc.documentElement.getElemOrSelfByPath(lastBookPath)) =>
        Vector(resolved.Elem(newBook), e)
      case e: resolved.Elem => Vector(e)
    })
  }

  test("testInsertAsFirstInto") {
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
      case e: Elem                               => e
    }

    val docWithScalaBook: Document = {
      val result = doc.transformElemsOrSelf(insertBook)
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newFirstBookPath: Path =
      indexed.Elem(docWithScalaBook.documentElement).filterChildElems(_.localName == "Book").map(_.path).head
    val newFirstBook: Elem = docWithScalaBook.documentElement.getElemOrSelfByPath(newFirstBookPath)

    assertResult("Programming in Scala") {
      (newFirstBook \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: Elem => Vector[Node] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
        case e: Elem => Vector(e)
      }

      docWithScalaBook.transformElemsToNodeSeq(deleteNonDatabaseBook)
    }

    assertResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, { e => Vector(insertBook(e)) })
  }

  test("testUpdate") {
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

    assertResult(resolved.Elem(updatedDoc.documentElement)) {
      resolved.Elem(updatedDoc2.documentElement)
    }

    val newMag = {
      val results =
        (updatedDoc.documentElement \ (che =>
          che.localName == "Magazine" && (che \ (_.localName == "Title")).head.text == "National Geographic"))
      results.head
    }

    assertResult("2010") {
      (newMag \@ EName("Year")).getOrElse("")
    }

    testPropertyAboutTransformChildElemsInTermsOfUpdated(doc.documentElement, updateMag)

    testPropertyAboutTransformElemsOrSelfInTermsOfUpdated(doc.documentElement, updateMag)

    testPropertyAboutTransformElemsInTermsOfUpdated(doc.documentElement, updateMag)

    testPropertyAboutTransformElemsToNodeSeq(doc.documentElement, { e => Vector(updateMag(e)) })
  }

  private def testPropertyAboutUpdatedWithNodeSeq(elem: Elem, path: Path, f: Elem => immutable.IndexedSeq[Node]): Unit = {
    def g(e: Elem): Elem = {
      if (path == Path.Empty) e
      else {
        e.withPatchedChildren(
          e.childNodeIndex(path.lastEntry),
          f(e.findChildElemByPathEntry(path.lastEntry).get),
          1)
      }
    }

    val updatedElem = elem.updateElemOrSelf(path.parentPathOption.getOrElse(Path.Empty))(g)

    assertResult(resolved.Elem(updatedElem)) {
      resolved.Elem(elem.updateElemWithNodeSeq(path)(f))
    }
  }

  private def testPropertyAboutTransformChildElemsInTermsOfUpdated(elem: Elem, f: Elem => Elem): Unit = {
    val expectedResult =
      indexed.Elem(elem).findAllChildElems.map(_.path).reverse.foldLeft(elem) { (acc, path) =>
        acc.updateElemOrSelf(path)(f)
      }

    assertResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformChildElems(f))
    }

    val expectedResult2 = elem.findAllChildElemsWithPathEntries.map(_._2).reverse.foldLeft(elem) { (acc, pathEntry) =>
      acc.updateChildElem(pathEntry)(f)
    }

    assertResult(resolved.Elem(expectedResult2)) {
      resolved.Elem(elem.transformChildElems(f))
    }
  }

  private def testPropertyAboutTransformElemsOrSelfInTermsOfUpdated(elem: Elem, f: Elem => Elem): Unit = {
    val expectedResult =
      indexed.Elem(elem).findAllElemsOrSelf.map(_.path).reverse.foldLeft(elem) { (acc, path) =>
        acc.updateElemOrSelf(path)(f)
      }

    assertResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformElemsOrSelf(f))
    }
  }

  private def testPropertyAboutTransformElemsInTermsOfUpdated(elem: Elem, f: Elem => Elem): Unit = {
    val expectedResult =
      indexed.Elem(elem).findAllElems.map(_.path).reverse.foldLeft(elem) { (acc, path) =>
        acc.updateElemOrSelf(path)(f)
      }

    assertResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformElems(f))
    }
  }

  private def testPropertyAboutTransformElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Unit = {
    def g(elem: Elem): Elem = elem.transformChildElemsToNodeSeq(che => f(che))

    val expectedResult = elem.transformElemsOrSelf(g)

    assertResult(resolved.Elem(expectedResult)) {
      resolved.Elem(elem.transformElemsToNodeSeq(f))
    }
  }

  private def testSymmetryPropertyAboutTransformElemsToNodeSeq(
    elem: Elem,
    f: Elem => immutable.IndexedSeq[Node],
    g: resolved.Elem => immutable.IndexedSeq[resolved.Node]): Unit = {

    assertResult(resolved.Elem(elem).transformElemsToNodeSeq(g)) {
      resolved.Elem(elem.transformElemsToNodeSeq(f))
    }
  }
}
