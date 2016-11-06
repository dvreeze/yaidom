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

package eu.cdevreeze.yaidom.queryapitests

import scala.collection.immutable

import org.junit.Test

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.indexed.IndexedClarkElem
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName

/**
 * ElemLike-based query test case, taking an IndexedClarkElem.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractIndexedElemLikeQueryTest extends AbstractElemLikeQueryTest {

  type U <: ClarkElemApi.Aux[U]

  final override type E = IndexedClarkElem[U]

  test("testQueryTheBookAuthors") {
    require(bookstore.localName == "Bookstore")

    val theBookAuthors =
      for {
        author <- bookstore.filterElems(withLocalName("Author"))
        bookPath <- author.path.findAncestorPath(_.elementNameOption.map(_.localPart) == Some("Book"))
        book <- bookstore.findElem(_.path == bookPath)
        if book.getChildElem(withLocalName("Title")).underlyingElem.text.startsWith("A First Course in Database Systems")
      } yield author
    val elems = bookstore.findAllElemsOrSelf

    assertResult(List("Ullman", "Widom")) {
      theBookAuthors.map(_.getChildElem(withLocalName("Last_Name")).text)
    }

    assertResult(List("Jeffrey", "Jennifer")) {
      theBookAuthors.map(_.getChildElem(withLocalName("First_Name")).text)
    }
  }

  test("testQueryTitlesOfCheapBooksByUllmanAgain") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.localName == "Bookstore")

    def getBookTitles[Elm <: ClarkElemApi.Aux[Elm]](store: Elm): immutable.IndexedSeq[Elm] =
      for {
        book <- store \ (_.localName == "Book")
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ (_.localName == "Last_Name") } map { _.trimmedText }
        if authorLastName == "Ullman"
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = getBookTitles(bookstore) map { _.trimmedText }
      result.toSet
    }

    assertResult(getBookTitles(bookstore.underlyingElem)) {
      getBookTitles(bookstore).map(_.underlyingElem)
    }
  }

  test("testQueryMagazinesWithSameNameAsBookAgain") {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.localName == "Bookstore")

    def getMagazines[Elm <: ClarkElemApi.Aux[Elm]](store: Elm): immutable.IndexedSeq[Elm] =
      for {
        magazine <- store \ (_.localName == "Magazine")
        magazineTitle = magazine.getChildElem(EName("Title")).trimmedText
        booksWithSameName = for {
          book <- bookstore \ (_.localName == "Book")
          bookTitle = book.getChildElem(EName("Title")).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    assertResult(Set("Hector and Jeff's Database Hints")) {
      val result = getMagazines(bookstore) flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }

    assertResult(getMagazines(bookstore.underlyingElem)) {
      getMagazines(bookstore).map(_.underlyingElem)
    }
  }
}
