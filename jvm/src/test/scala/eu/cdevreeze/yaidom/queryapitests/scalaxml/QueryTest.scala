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

package eu.cdevreeze.yaidom.queryapitests.scalaxml

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.queryapitests.AbstractElemLikeQueryTest
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.simple.Node

import scala.collection.immutable

/**
 * Query test case for Scala XML wrapper elements.
 *
 * @author Chris de Vreeze
 */
class QueryTest extends AbstractElemLikeQueryTest {

  final type E = ScalaXmlElem

  protected final val bookstore: ScalaXmlElem = {
    val scalaElem =
      <Bookstore>
        <Book ISBN="ISBN-0-13-713526-2" Price="85" Edition="3rd">
          <Title>A First Course in Database Systems</Title>
          <Authors>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
              <First_Name>Jennifer</First_Name>
              <Last_Name>Widom</Last_Name>
            </Author>
          </Authors>
        </Book>
        <Book ISBN="ISBN-0-13-815504-6" Price="100">
          <Title>Database Systems: The Complete Book</Title>
          <Authors>
            <Author>
              <First_Name>Hector</First_Name>
              <Last_Name>Garcia-Molina</Last_Name>
            </Author>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
              <First_Name>Jennifer</First_Name>
              <Last_Name>Widom</Last_Name>
            </Author>
          </Authors>
          <Remark>
            Buy this book bundled with "A First Course" - a great deal!
          </Remark>
        </Book>
        <Book ISBN="ISBN-0-11-222222-3" Price="50">
          <Title>Hector and Jeff's Database Hints</Title>
          <Authors>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
              <First_Name>Hector</First_Name>
              <Last_Name>Garcia-Molina</Last_Name>
            </Author>
          </Authors>
          <Remark>An indispensable companion to your textbook</Remark>
        </Book>
        <Book ISBN="ISBN-9-88-777777-6" Price="25">
          <Title>Jennifer's Economical Database Hints</Title>
          <Authors>
            <Author>
              <First_Name>Jennifer</First_Name>
              <Last_Name>Widom</Last_Name>
            </Author>
          </Authors>
        </Book>
        <Magazine Month="January" Year="2009">
          <Title>National Geographic</Title>
        </Magazine>
        <Magazine Month="February" Year="2009">
          <Title>National Geographic</Title>
        </Magazine>
        <Magazine Month="February" Year="2009">
          <Title>Newsweek</Title>
        </Magazine>
        <Magazine Month="March" Year="2009">
          <Title>Hector and Jeff's Database Hints</Title>
        </Magazine>
      </Bookstore>

    new ScalaXmlElem(scalaElem)
  }

  test("testQueryAll") {
    require(bookstore.localName == "Bookstore")

    val elems = bookstore.findAllElemsOrSelf

    assertResult(true) {
      elems.nonEmpty
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * where some $fm in $b/Authors/Author/First_Name satisfies contains($b/Title, $fn)
   * return &lt;Book&gt;
   *          { $b/Title }
   *          { for $fm in $b/Authors/Author/First_Name where contains($b/Title, $fn) return $fn }
   *        &lt;/Book&gt;
   * }}}
   */
  test("testQueryBooksWithAuthorInTitle") {
    require(bookstore.localName == "Bookstore")

    import Node._

    val titleAndFirstNames =
      for {
        book <- bookstore \ (_.localName == "Book")
        title = book.getChildElem(EName("Title"))
        authorFirstNames = {
          val result = book.filterElems(EName("Author")).map { _.getChildElem(EName("First_Name")).trimmedText }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames.filter { firstName =>
          title.trimmedText.indexOf(firstName) >= 0
        }
        if searchedForFirstNames.nonEmpty
      } yield {
        val titleElem = convert.ScalaXmlConversions.convertToElem(title.wrappedNode)

        elem(
          qname = QName("Book"),
          scope = Scope.Empty,
          children = Vector(titleElem, textElem(QName("First_Name"), Scope.Empty, searchedForFirstNames.head)))
      }

    assertResult(2) {
      titleAndFirstNames.size
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val titleElms = titleAndFirstNames.map { e =>
        e.filterElems(EName("Title"))
      }
      val result = titleElms.flatten.map { e =>
        e.trimmedText
      }
      result.toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * let $a := avg(doc("bookstore.xml")/Bookstore/Book/@Price)
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * where $b/@Price < $a
   * return &lt;Book&gt;
   *          { $b/Title }
   *          &lt;Price&gt; { $b/data(@Price) } &lt;/Price&gt;
   *        &lt;/Book&gt;
   * }}}
   */
  test("testQueryBooksPricedBelowAverage") {
    require(bookstore.localName == "Bookstore")

    import Node._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore \ (_.localName == "Book")
        price = book.attribute(EName("Price")).toDouble
      } yield price

    val avg: Double = prices.sum / prices.size

    val cheapBooks =
      for {
        book <- bookstore \ (_.localName == "Book")
        price = book.attribute(EName("Price")).toDouble
        if price < avg
      } yield {
        val title = book.getChildElem(EName("Title"))
        val titleElem = convert.ScalaXmlConversions.convertToElem(title.wrappedNode)

        elem(
          qname = QName("Book"),
          scope = Scope.Empty,
          children = Vector(titleElem, textElem(QName("Price"), Scope.Empty, price.toString)))
      }

    assertResult(2) {
      cheapBooks.size
    }
    assertResult(Set(50, 25)) {
      val result = cheapBooks
        .flatMap { e =>
          e.filterElems(EName("Price"))
        }
        .map { e =>
          e.trimmedText.toDouble.intValue
        }
      result.toSet
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val result = cheapBooks
        .flatMap { e =>
          e.filterElems(EName("Title"))
        }
        .map { e =>
          e.trimmedText
        }
      result.toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * order by $b/@Price
   * return &lt;Book&gt;
   *          { $b/Title }
   *          &lt;Price&gt; { $b/data(@Price) } &lt;/Price&gt;
   *        &lt;/Book&gt;
   * }}}
   */
  test("testQueryBooksOrderedByPrice") {
    require(bookstore.localName == "Bookstore")

    import Node._

    def cheaper(book1: ScalaXmlElem, book2: ScalaXmlElem): Boolean = {
      val price1 = book1.attribute(EName("Price")).toInt
      val price2 = book2.attribute(EName("Price")).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- (bookstore \ (_.localName == "Book")).sortWith { cheaper }
        price = book.attribute(EName("Price")).toDouble
      } yield {
        val title = book.getChildElem(EName("Title"))
        val titleElem = convert.ScalaXmlConversions.convertToElem(title.wrappedNode)

        elem(
          qname = QName("Book"),
          scope = Scope.Empty,
          children = Vector(titleElem, textElem(QName("Price"), Scope.Empty, price.toString)))
      }
    }

    assertResult(4) {
      books.size
    }
    assertResult(List(25, 50, 85, 100)) {
      books
        .flatMap { e =>
          e.filterElems(EName("Price"))
        }
        .map { e =>
          e.trimmedText.toDouble.intValue
        }
    }
    assertResult(
      List(
        "Jennifer's Economical Database Hints",
        "Hector and Jeff's Database Hints",
        "A First Course in Database Systems",
        "Database Systems: The Complete Book")) {
      books
        .flatMap { e =>
          e.filterElems(EName("Title"))
        }
        .map { e =>
          e.trimmedText
        }
    }
  }
}
