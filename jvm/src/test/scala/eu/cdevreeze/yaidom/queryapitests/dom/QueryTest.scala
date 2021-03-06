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

package eu.cdevreeze.yaidom.queryapitests.dom

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.queryapitests.AbstractElemLikeQueryTest
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import javax.xml.parsers.DocumentBuilderFactory

import scala.collection.immutable

/**
 * Query test case for DOM wrapper elements.
 *
 * @author Chris de Vreeze
 */
class QueryTest extends AbstractElemLikeQueryTest {

  final type E = DomElem

  private val book1Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "A First Course in Database Systems"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            )
          )
        )
      )
    )
  }

  private val book2Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Database Systems: The Complete Book"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Hector"),
                textElem(QName("Last_Name"), Scope.Empty, "Garcia-Molina"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            )
          )
        ),
        textElem(QName("Remark"), Scope.Empty, "Buy this book bundled with \"A First Course\" - a great deal!")
      )
    )
  }

  private val book3Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Hector and Jeff's Database Hints"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Hector"),
                textElem(QName("Last_Name"), Scope.Empty, "Garcia-Molina"))
            )
          )
        ),
        textElem(QName("Remark"), Scope.Empty, "An indispensable companion to your textbook")
      )
    )
  }

  private val book4Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Jennifer's Economical Database Hints"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            ))
        )
      )
    )
  }

  private val magazine1Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "National Geographic"))
    )
  }

  private val magazine2Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "National Geographic"))
    )
  }

  private val magazine3Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "Newsweek"))
    )
  }

  private val magazine4Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "Hector and Jeff's Database Hints"))
    )
  }

  protected final val bookstore: DomElem = {
    import Node._

    val resultElem: Elem =
      elem(
        qname = QName("Bookstore"),
        scope = Scope.Empty,
        children = Vector(
          book1Builder,
          book2Builder,
          book3Builder,
          book4Builder,
          magazine1Builder,
          magazine2Builder,
          magazine3Builder,
          magazine4Builder)
      )
    val resultDoc: Document = Document(resultElem)

    val dbf = DocumentBuilderFactory.newInstance()
    val db = dbf.newDocumentBuilder
    val domDoc = db.newDocument()

    new DomElem(convert.DomConversions.convertElem(resultDoc.documentElement)(domDoc))
  }

  test("testQueryAll") {
    require(bookstore.localName == "Bookstore")

    val elems = bookstore.findAllElemsOrSelf

    assertResult(true) {
      elems.nonEmpty
    }
  }

  test("testQueryBookOrMagazineTitlesUsingParent") {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.localName == "Bookstore")

    val bookOrMagazineTitles =
      for {
        title <- bookstore.filterElems { _.resolvedName == EName("Title") }
        if (title.parent.resolvedName == EName("Book")) || (title.parent.resolvedName == EName("Magazine"))
      } yield title

    assertResult(
      Set(
        "A First Course in Database Systems",
        "Database Systems: The Complete Book",
        "Hector and Jeff's Database Hints",
        "Jennifer's Economical Database Hints",
        "National Geographic",
        "Newsweek"
      )) {
      val result = bookOrMagazineTitles.map { _.trimmedText }
      result.toSet
    }
  }

  test("testQueryTitlesUsingPaths") {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        title <- bookstore.findTopmostElems { _.resolvedName == EName("Title") }
        if title.parentOption.isDefined && title.parent.parentOption.map(_.localName).contains("Bookstore")
      } yield title

    assertResult(
      Set(
        "A First Course in Database Systems",
        "Database Systems: The Complete Book",
        "Hector and Jeff's Database Hints",
        "Jennifer's Economical Database Hints",
        "National Geographic",
        "Newsweek"
      )) {
      val result = titles.map { _.trimmedText }
      result.toSet
    }
  }

  test("testQueryCheapBookTitlesUsingParent") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.localName == "Bookstore")

    val titles = bookstore.findAllElems.filter { e =>
      (e.resolvedName == EName("Title")) && {
        val parentElm = e.parent
        parentElm.localName == "Book" && parentElm.attribute(EName("Price")).toInt < 90
      }
    }

    assertResult(
      Set(
        "A First Course in Database Systems",
        "Hector and Jeff's Database Hints",
        "Jennifer's Economical Database Hints")) {
      val result = titles.map { _.trimmedText }
      result.toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByUllmanUsingParent") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      bookstore
        .findTopmostElems { e =>
          e.localName == "Last_Name" && e.trimmedText == "Ullman"
        }
        .flatMap { elm =>
          require(elm.resolvedName == EName("Last_Name"))
          val bookOption = elm.findAncestor { e =>
            e.resolvedName == EName("Book") && e.attribute(EName("Price")).toInt < 90
          }
          val titleOption = bookOption.flatMap { bookElm =>
            bookElm.findElem { e =>
              e.resolvedName == EName("Title")
            }
          }
          titleOption
        }

    assertResult(Set("A First Course in Database Systems", "Hector and Jeff's Database Hints")) {
      val result = bookTitles.map { _.trimmedText }
      result.toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByJeffreyUllmanUsingParent") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.localName == "Bookstore")

    def authorLastAndFirstName(authorElem: DomElem): (String, String) = {
      val lastNames = authorElem.filterChildElems(EName("Last_Name")).map { _.text.trim }
      val firstNames = authorElem.filterChildElems(EName("First_Name")).map { _.text.trim }
      (lastNames.mkString, firstNames.mkString)
    }

    val bookTitles2 =
      for {
        authorElem <- bookstore.filterElemsOrSelf { _.resolvedName == EName("Author") }
        (lastName, firstName) = authorLastAndFirstName(authorElem)
        if lastName == "Ullman" && firstName == "Jeffrey"
        bookElem <- authorElem.findAncestor { _.resolvedName == EName("Book") }
        if bookElem.attributeOption(EName("Price")).map(_.toInt).getOrElse(0) < 90
      } yield bookElem.getChildElem(EName("Title"))

    assertResult(Set("A First Course in Database Systems", "Hector and Jeff's Database Hints")) {
      val result = bookTitles2.map { _.trimmedText }
      result.toSet
    }

    val bookTitles3 =
      for {
        authorElem <- bookstore \\ EName("Author")
        (lastName, firstName) = authorLastAndFirstName(authorElem)
        if lastName == "Ullman" && firstName == "Jeffrey"
        bookElem <- authorElem.findAncestor { _.resolvedName == EName("Book") }
        if (bookElem \@ EName("Price")).map(_.toInt).getOrElse(0) < 90
      } yield (bookElem \ EName("Title")).head

    assertResult(Set("A First Course in Database Systems", "Hector and Jeff's Database Hints")) {
      val result = bookTitles3.map { _.trimmedText }
      result.toSet
    }
  }

  test("testQueryBooksByJeffreyUllmanUsingParent") {
    // Own example

    require(bookstore.localName == "Bookstore")

    val ullmanBookElms =
      for {
        authorElm <- bookstore.filterElems { e =>
          (e.localName == "Author") &&
          (e.getChildElem(_.localName == "First_Name").text == "Jeffrey") &&
          (e.getChildElem(_.localName == "Last_Name").text == "Ullman")
        }
        bookElm = authorElm.parent.parent
      } yield {
        require(bookElm.localName == "Book")
        bookElm
      }

    assertResult(
      Set(
        "A First Course in Database Systems",
        "Database Systems: The Complete Book",
        "Hector and Jeff's Database Hints")) {
      val result = ullmanBookElms.map { e =>
        e.getChildElem(_.localName == "Title").text
      }
      result.toSet
    }
  }

  test("testQueryElementsWithParentNotBookOrBookstore") {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    require(bookstore.localName == "Bookstore")

    val elms =
      for {
        e <- bookstore.findAllElems
        parent = e.parent
        if parent.qname != QName("Bookstore") && parent.qname != QName("Book")
      } yield e

    assert(elms.size > 10, "Expected more than 10 matching elements")

    assertResult(Set(QName("Title"), QName("Author"), QName("First_Name"), QName("Last_Name"))) {
      val result = elms.map { e =>
        e.qname
      }
      result.toSet
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
        val titleElem = convert.DomConversions.convertToElem(title.wrappedNode, book.scope)

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
        val titleElem = convert.DomConversions.convertToElem(title.wrappedNode, book.scope)

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

    def cheaper(book1: DomElem, book2: DomElem): Boolean = {
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
        val titleElem = convert.DomConversions.convertToElem(title.wrappedNode, book.scope)

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
