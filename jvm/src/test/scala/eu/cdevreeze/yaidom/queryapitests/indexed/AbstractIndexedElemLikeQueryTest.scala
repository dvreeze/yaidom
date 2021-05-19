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

package eu.cdevreeze.yaidom.queryapitests.indexed

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.PathConversions
import eu.cdevreeze.yaidom.indexed.IndexedNode
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.queryapitests.AbstractElemLikeQueryTest

/**
 * ElemLike-based query test case, taking an indexed element.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractIndexedElemLikeQueryTest extends AbstractElemLikeQueryTest {

  final override type E = IndexedNode.Elem

  test("testQueryTheBookAuthors") {
    require(bookstore.localName == "Bookstore")

    val theBookAuthors =
      for {
        author <- bookstore.filterElems(withLocalName("Author"))
        bookPath <- author.path.findAncestorPath(_.elementNameOption.map(_.localPart).contains("Book"))
        book <- bookstore.findElem(_.path == bookPath)
        if book
          .getChildElem(withLocalName("Title"))
          .underlyingElem
          .text
          .startsWith("A First Course in Database Systems")
      } yield author

    assertResult(List("Ullman", "Widom")) {
      theBookAuthors.map(_.getChildElem(withLocalName("Last_Name")).text)
    }

    assertResult(List("Jeffrey", "Jennifer")) {
      theBookAuthors.map(_.getChildElem(withLocalName("First_Name")).text)
    }

    val theBookAuthors2 =
      for {
        author <- bookstore.filterElems(withLocalName("Author"))
        bookAbsolutePath <- author.absolutePath.findAncestor(_.elementName.localPart == "Book")
        bookPath = PathConversions.convertAbsolutePathToPath(bookAbsolutePath)
        book <- bookstore.findElem(_.path == bookPath)
        if book
          .getChildElem(withLocalName("Title"))
          .underlyingElem
          .text
          .startsWith("A First Course in Database Systems")
      } yield author

    assertResult(List("Ullman", "Widom")) {
      theBookAuthors2.map(_.getChildElem(withLocalName("Last_Name")).text)
    }

    assertResult(List("Jeffrey", "Jennifer")) {
      theBookAuthors2.map(_.getChildElem(withLocalName("First_Name")).text)
    }
  }

  test("testQueryTitlesOfCheapBooksByUllmanAgain") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.localName == "Bookstore")

    def getBookTitles(store: ClarkNodes.Elem): IndexedSeq[ClarkNodes.Elem] =
      for {
        book <- store \ (_.localName == "Book")
        if book.attribute(EName("Price")).toInt < 90
        authors = book.getChildElem { _.localName == "Authors" }
        authorLastName <- (authors \ { _.localName == "Author" })
          .flatMap { e =>
            e \ (_.localName == "Last_Name")
          }
          .map { _.trimmedText }
        if authorLastName == "Ullman"
      } yield book.getChildElem(EName("Title"))

    assertResult(Set("A First Course in Database Systems", "Hector and Jeff's Database Hints")) {
      val result = getBookTitles(bookstore).map { _.trimmedText }
      result.toSet
    }

    assertResult(getBookTitles(bookstore.underlyingElem)) {
      getBookTitles(bookstore).map(_.asInstanceOf[IndexedNode.Elem].underlyingElem)
    }
  }

  test("testQueryMagazinesWithSameNameAsBookAgain") {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.localName == "Bookstore")

    def getMagazines(store: ClarkNodes.Elem): IndexedSeq[ClarkNodes.Elem] =
      for {
        magazine <- store \ (_.localName == "Magazine")
        magazineTitle = magazine.getChildElem(EName("Title")).trimmedText
        booksWithSameName = for {
          book <- bookstore \ (_.localName == "Book")
          bookTitle = book.getChildElem(EName("Title")).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if booksWithSameName.nonEmpty
      } yield magazine

    assertResult(Set("Hector and Jeff's Database Hints")) {
      val result = getMagazines(bookstore).flatMap { mag =>
        mag.findElem(EName("Title")).map { _.trimmedText }
      }
      result.toSet
    }

    assertResult(getMagazines(bookstore.underlyingElem)) {
      getMagazines(bookstore).map(_.asInstanceOf[IndexedNode.Elem].underlyingElem)
    }
  }
}
