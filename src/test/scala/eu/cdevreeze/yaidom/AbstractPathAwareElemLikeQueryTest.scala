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

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import ElemApi._

/**
 * PathAwareElemLike-based query test case, extending AbstractElemLikeQueryTest.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractPathAwareElemLikeQueryTest extends AbstractElemLikeQueryTest {

  override type E <: PathAwareElemLike[E] with HasText

  @Test def testQueryBookTitlesUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstore.localName == "Bookstore")

    val bookTitlePaths =
      bookstore findTopmostElemPaths { _.localName == "Title" } filter { path => path.containsName(EName("Book")) }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBookTitlesUsingPathsAgain(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title
    // This time using the ElemApi companion object

    require(bookstore.localName == "Bookstore")

    val bookTitlePaths =
      bookstore findTopmostElemPaths withLocalName("Title") filter { path => path.containsName(EName("Book")) }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBookOrMagazineTitlesUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.localName == "Bookstore")

    val bookOrMagazineTitlePaths =
      for {
        titlePath <- bookstore filterElemPaths { _.resolvedName == EName("Title") }
        if titlePath.parentPath.endsWithName(EName("Book")) || titlePath.parentPath.endsWithName(EName("Magazine"))
      } yield titlePath

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBookOrMagazineTitlesUsingPathsAgain(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title
    // This time using the ElemApi companion object

    require(bookstore.localName == "Bookstore")

    val bookOrMagazineTitlePaths =
      for {
        titlePath <- bookstore filterElemPaths withNoNsEName("Title")
        if titlePath.parentPath.endsWithName(EName("Book")) || titlePath.parentPath.endsWithName(EName("Magazine"))
      } yield titlePath

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.localName == "Bookstore")

    val titlePaths =
      for {
        titlePath <- bookstore findTopmostElemPaths { _.resolvedName == EName("Title") }
        if titlePath.entries.size == 2
      } yield titlePath

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryAllTitlesUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")//Title

    require(bookstore.localName == "Bookstore")

    val titlePaths =
      for {
        titlePath <- bookstore filterElemPaths { _.localName == "Title" }
      } yield titlePath

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryAllElementsUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")//*

    require(bookstore.localName == "Bookstore")

    val elements = bookstore.findAllElemsOrSelf

    assert(elements.contains(bookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")

    val childrenAlsoIncluded =
      elements forall { e =>
        e.findAllChildElems forall { ch => elements.contains(ch) }
      }
    assert(childrenAlsoIncluded, "Expected child elements of each element also in the result")

    val paths = bookstore.findAllElemOrSelfPaths

    expectResult(elements.size) {
      paths.size
    }

    expectResult(elements map (e => toResolvedElem(e))) {
      paths map { path => bookstore.getElemOrSelfByPath(path) } map { e => toResolvedElem(e) }
    }
  }

  @Test def testQueryBookIsbnsUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(bookstore.localName == "Bookstore")

    expectResult(Set(
      "ISBN-0-13-713526-2",
      "ISBN-0-13-815504-6",
      "ISBN-0-11-222222-3",
      "ISBN-9-88-777777-6")) {
      val result =
        for (bookPath <- bookstore filterChildElemPaths (e => e.localName == "Book")) yield bookstore.getElemOrSelfByPath(bookPath).attribute(EName("ISBN"))
      result.toSet
    }
  }

  @Test def testQueryCheapBooksUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ (_.localName == "Book")
        price <- book \@ EName("Price")
        if price.toInt < 90
      } yield book

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book =>
        book findElemPath (e => e.resolvedName == EName("Title")) map
          { path => book.getElemOrSelfByPath(path).trimmedText }
      }
      result.toSet
    }
  }

  @Test def testQueryCheapBooksUsingPathsAgain(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]
    // This time using the ElemApi companion object

    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ withLocalName("Book")
        price <- book \@ EName("Price")
        if price.toInt < 90
      } yield book

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book =>
        book findElemPath withNoNsEName("Title") map
          { path => book.getElemOrSelfByPath(path).trimmedText }
      }
      result.toSet
    }
  }

  @Test def testQueryCheapBookTitlesUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.localName == "Bookstore")

    val titlePaths = bookstore.findAllElemPaths filter { path =>
      path.endsWithName(EName("Title")) && {
        val parentElm = bookstore.getElemOrSelfByPath(path.parentPath)
        parentElm.localName == "Book" && parentElm.attribute(EName("Price")).toInt < 90
      }
    }

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllmanUsingPaths(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitlePaths =
      bookstore findTopmostElemPaths { e => e.localName == "Last_Name" && e.trimmedText == "Ullman" } filter { path =>
        require(path.endsWithName(EName("Last_Name")))
        path.containsName(EName("Book")) && {
          val bookPath = path.ancestorPaths.filter(_.endsWithName(EName("Book"))).head
          val bookElm = bookstore.getElemOrSelfByPath(bookPath)
          bookElm.attribute(EName("Price")).toInt < 90
        }
      } flatMap { path =>
        require(path.endsWithName(EName("Last_Name")))
        val bookPath = path.ancestorPaths.filter(_.endsWithName(EName("Book"))).head
        val bookElm = bookstore.getElemOrSelfByPath(bookPath)
        val titlePathOption = bookElm findElemPath { e => e.resolvedName == EName("Title") } map
          { relativeTitlePath => bookPath ++ relativeTitlePath }
        titlePathOption
      }

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitlePaths map { path => bookstore.getElemOrSelfByPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBooksByJeffreyUllmanUsingPaths(): Unit = {
    // Own example

    require(bookstore.localName == "Bookstore")

    val ullmanBookElms =
      for {
        authorPath <- bookstore filterElemPaths { e =>
          (e.localName == "Author") &&
            ((e.getChildElem(_.localName == "First_Name")).text == "Jeffrey") &&
            ((e.getChildElem(_.localName == "Last_Name")).text == "Ullman")
        }
        bookPath = authorPath.parentPath.parentPath
      } yield {
        require(bookPath.lastEntry.elementName.localPart == "Book")
        bookstore.getElemOrSelfByPath(bookPath)
      }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = ullmanBookElms map { e => e.getChildElem(_.localName == "Title").text }
      result.toSet
    }
  }

  @Test def testQueryBooksByJeffreyUllmanUsingPathsAgain(): Unit = {
    // Own example
    // This time using the ElemApi companion object

    require(bookstore.localName == "Bookstore")

    val ullmanBookElms =
      for {
        authorPath <- bookstore filterElemPaths { e =>
          (e.localName == "Author") &&
            (e.getChildElem(withLocalName("First_Name")).text == "Jeffrey") &&
            (e.getChildElem(withLocalName("Last_Name")).text == "Ullman")
        }
        bookPath = authorPath.parentPath.parentPath
      } yield {
        require(bookPath.lastEntry.elementName.localPart == "Book")
        bookstore.getElemOrSelfByPath(bookPath)
      }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = ullmanBookElms map { e => e.getChildElem(withLocalName("Title")).text }
      result.toSet
    }
  }
}
