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

package eu.cdevreeze.notyaidom

import scala.collection.immutable
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

/**
 * Query test case, using a very naive mini version of an alternative yaidom, inspired by the streaming API
 * of Saxon 9.9.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material. The API has been deeply inspired by Saxon 9.9.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SaxonInspiredQueryTest extends FunSuite {

  import SaxonInspiredQueryTest._

  import SaxonInspiredQueryTest.ElemStepFactory._

  test("testQueryBookTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstore.name == "Bookstore")

    val bookTitles = bookstore.select { childElems("Book") / childElems("Title").firstOption }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {

      bookTitles.map(_.text.trim).toSet
    }
  }

  test("testQueryBookOrMagazineTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.name == "Bookstore")

    val bookOrMagazineTitles =
      bookstore.select { childElems("Book").cat(childElems("Magazine")) / childElems("Title").firstOption }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {

      bookOrMagazineTitles.map(_.text.trim).toSet
    }
  }

  test("testQueryTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.name == "Bookstore")

    val titles = bookstore.select { childElems() / childElems("Title").firstOption }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {

      titles.map(_.text.trim).toSet
    }
  }

  test("testQueryAllTitles") {
    // XPath: doc("bookstore.xml")//Title

    require(bookstore.name == "Bookstore")

    val titles = bookstore.select { descendantElemsOrSelf("Title") }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {

      titles.map(_.text.trim).toSet
    }
  }

  test("testQueryAllElements") {
    // XPath: doc("bookstore.xml")//*

    require(bookstore.name == "Bookstore")

    val elements = bookstore.select { descendantElemsOrSelf() }

    assert(elements.contains(bookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")

    val childrenAlsoIncluded =
      elements.forall { e =>
        e.select { childElems() }.forall(ch => elements.contains(ch))
      }
    assert(childrenAlsoIncluded, "Expected child elements of each element also in the result")
  }

  test("testQueryBookIsbns") {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(bookstore.name == "Bookstore")

    val isbns = bookstore.select { childElems("Book") }.map(_.attributes("ISBN"))

    assertResult(Set(
      "ISBN-0-13-713526-2",
      "ISBN-0-13-815504-6",
      "ISBN-0-11-222222-3",
      "ISBN-9-88-777777-6")) {

      isbns.toSet
    }
  }

  test("testQueryCheapBooks") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

    require(bookstore.name == "Bookstore")

    val books = bookstore.select { childElems("Book").where(_.attributes("Price").toInt < 90) }

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {

      books.flatMap(_.select { childElems("Title").firstOption }.map(_.text.trim)).toSet
    }
  }

  test("testQueryCheapBookTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.name == "Bookstore")

    val titles =
      bookstore.select { childElems("Book").where(_.attributes("Price").toInt < 90) / childElems("Title").firstOption }

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {

      titles.map(_.text.trim).toSet
    }
  }

  test("testQueryCheapBookAuthors") {
    // Own example..

    require(bookstore.name == "Bookstore")

    val cheapBookAuthorElms: immutable.IndexedSeq[Elem] =
      bookstore.select { childElems("Book").where(_.attributes("Price").toInt < 90) / descendantElemsOrSelf("Author") }

    val cheapBookAuthors: immutable.IndexedSeq[String] =
      for {
        authorElm <- cheapBookAuthorElms
      } yield {
        val firstNameElmOption = authorElm.select { childElems("First_Name").firstOption }.headOption
        val lastNameElmOption = authorElm.select { childElems("Last_Name").firstOption }.headOption

        val firstName = firstNameElmOption.map(_.text).getOrElse("")
        val lastName = lastNameElmOption.map(_.text).getOrElse("")
        (firstName + " " + lastName).trim
      }

    assertResult(Set("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina")) {
      cheapBookAuthors.toSet
    }
  }

  test("testQueryTitlesOfBooksWithRemarks") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      bookstore.select { childElems("Book").where(_.select(childElems("Remark")).nonEmpty) / childElems("Title").first }

    assertResult(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {

      bookTitles.map(_.text.trim).toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByUllman") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      bookstore.select {
        childElems("Book").where { e =>
          e.attributes("Price").toInt < 90 &&
            e.select { childElems("Authors") / childElems("Author") / childElems("Last_Name").where(_.text.trim == "Ullman") }.nonEmpty
        } / childElems("Title").firstOption
      }

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {

      bookTitles.map(_.text.trim).toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByJeffreyUllman") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      bookstore.select {
        childElems("Book").where { e =>
          e.attributes("Price").toInt < 90 &&
            e.select {
              descendantElemsOrSelf("Author").where { e =>
                e.select { childElems("Last_Name") }.map(_.text.trim).contains("Ullman") &&
                  e.select { childElems("First_Name") }.map(_.text.trim).contains("Jeffrey")
              }
            }.nonEmpty
        } / childElems("Title").firstOption
      }

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {

      bookTitles.map(_.text.trim).toSet
    }
  }

  test("testQueryTitlesOfBooksByJeffreyUllmanButNotWidom") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      bookstore.select {
        childElems("Book")
          .where { e =>
            e.select { childElems("Authors") / childElems("Author") / childElems("Last_Name").where(_.text.trim == "Ullman") }.nonEmpty &&
              e.select { childElems("Authors") / childElems("Author") / childElems("Last_Name").where(_.text.trim == "Widom") }.isEmpty
          } / childElems("Title")
      }

    assertResult(Set(
      "Hector and Jeff's Database Hints")) {

      bookTitles.map(_.text.trim).toSet
    }
  }

  test("testQuerySecondAuthors") {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstore.name == "Bookstore")

    val secondAuthorLastNames =
      bookstore.select { descendantElemsOrSelf("Authors") / (_.select(childElems("Author")).slice(1, 2)) / childElems("Last_Name") }

    assertResult(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {

      secondAuthorLastNames.map(_.text.trim).toSet
    }
  }

  test("testQueryGreatBooks") {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    require(bookstore.name == "Bookstore")

    val titles = bookstore.select {
      descendantElemsOrSelf("Book")
        .where(_.select(childElems("Remark")).map(_.text.trim).exists(_.contains("great"))) / childElems("Title")
    }

    assertResult(Set("Database Systems: The Complete Book")) {
      titles.map(_.text.trim).toSet
    }
  }

  test("testQueryMagazinesWithSameNameAsBook") {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.name == "Bookstore")

    def title(bookOrMagazine: Elem): String = bookOrMagazine.select { childElems("Title") }.map(_.text.trim).mkString

    val magazines =
      bookstore.select { childElems("Magazine").where(e => bookstore.select(childElems("Book")).map(title(_)).contains(title(e))) }

    assertResult(Set("Hector and Jeff's Database Hints")) {
      magazines.flatMap { _.select(descendantElemsOrSelf("Title")).map(_.text.trim) }.toSet
    }
  }

  test("testQueryBooksOrMagazinesWithNonUniqueTitles") {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(bookstore.name == "Bookstore")

    def title(bookOrMagazine: Elem): String = bookOrMagazine.select { childElems("Title") }.map(_.text.trim).mkString

    val booksAndMagazines = bookstore.select {
      childElems("Book").cat(childElems("Magazine")).where { e =>
        bookstore.select(childElems().where(_ != e)).filter(title(_) == title(e)).nonEmpty
      }
    }

    assertResult(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      booksAndMagazines.flatMap { _.select(descendantElemsOrSelf("Title")).map(_.text.trim) }.toSet
    }
  }

  test("testQueryBooksOrMagazinesWithTitleAsOtherBook") {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstore.name == "Bookstore")

    def title(bookOrMagazine: Elem): String = bookOrMagazine.select { childElems("Title") }.map(_.text.trim).mkString

    val booksAndMagazines = bookstore.select {
      childElems("Book").cat(childElems("Magazine")).where { e =>
        bookstore.select(childElems("Book").where(_ != e)).filter(title(_) == title(e)).nonEmpty
      }
    }

    assertResult(Set("Hector and Jeff's Database Hints")) {
      booksAndMagazines.flatMap { _.select(descendantElemsOrSelf("Title")).map(_.text.trim) }.toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * where every $fn in $b/Authors/Author/First_Name satisfies contains($fn, "J")
   * return $b
   * </pre>
   * or XPath:
   * }}}
   * doc("bookstore.xml")//Book[count(Authors/Author[contains(First_Name, "J"]) = count(Authors/Author/First_Name)]
   * </pre>
   */
  test("testQueryBooksWithAllAuthorFirstNamesWithLetterJ") {
    require(bookstore.name == "Bookstore")

    val books =
      bookstore.select {
        childElems("Book").where { e =>
          e.select { descendantElemsOrSelf("Author").where(_.select(childElems("First_Name").where(_.text.contains("J"))).nonEmpty) }.size ==
            e.select { descendantElemsOrSelf("Author") / childElems("First_Name") }.size
        }
      }

    assertResult(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      books.flatMap { _.select(descendantElemsOrSelf("Title")).map(_.text.trim) }.toSet
    }
  }

  test("testQueryBooksFromUllmanButNotWidom") {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(bookstore.name == "Bookstore")

    def findAuthorNames(bookElem: Elem): immutable.IndexedSeq[String] = {
      bookElem.select { descendantElemsOrSelf("Author") / childElems("Last_Name") }.map(_.text.trim).distinct
    }

    val books =
      bookstore.select { childElems("Book").where(e => findAuthorNames(e).contains("Ullman") && !findAuthorNames(e).contains("Widom")) }

    assertResult(Set("Hector and Jeff's Database Hints")) {
      books.flatMap(_.select(childElems("Title"))).map(_.text.trim).toSet
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
    require(bookstore.name == "Bookstore")

    val titleAndFirstNames =
      for {
        book <- bookstore.select { childElems("Book") }
        title <- book.select { childElems("Title") }
        authorFirstNames = book.select { descendantElemsOrSelf("Author") / childElems("First_Name") }.map(_.text.trim).toSet
        searchedForFirstNames = authorFirstNames.filter(firstName => title.text.trim.indexOf(firstName) >= 0)
        if searchedForFirstNames.nonEmpty
      } yield {
        new Elem(
          name = "Book",
          children = Vector(
            title,
            textElem("First_Name", searchedForFirstNames.head)))
      }

    assertResult(2) {
      titleAndFirstNames.size
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val titleElms = titleAndFirstNames.flatMap { _.select(descendantElemsOrSelf("Title")) }
      titleElms.map(_.text.trim).toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * &lt;Average&gt;
   * { let $plist := doc("bookstore.xml")/Bookstore/Book/@Price
   *   return avg($plist) }
   * &lt;/Average&gt;
   * }}}
   */
  test("testQueryAverageBookPrice") {
    require(bookstore.name == "Bookstore")

    val prices: immutable.IndexedSeq[Double] = bookstore.select { childElems("Book") }.map(_.attributes("Price").toDouble)

    val averagePrice = textElem("Average", (prices.sum.toDouble / prices.size).toString)

    assertResult(65) {
      averagePrice.text.trim.toDouble.intValue
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
    require(bookstore.name == "Bookstore")

    val prices: immutable.IndexedSeq[Double] = bookstore.select { childElems("Book") }.map(_.attributes("Price").toDouble)
    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- bookstore.select { childElems("Book") }
        price = book.attributes("Price").toDouble
        if price < avg
      } yield {
        new Elem(
          name = "Book",
          children = Vector(
            book.select(childElems()).find(_.name == "Title").get,
            textElem("Price", price.toString)))
      }

    assertResult(2) {
      cheapBooks.size
    }
    assertResult(Set(50, 25)) {
      cheapBooks.flatMap { _.select(descendantElemsOrSelf("Price")) }.map(_.text.trim.toDouble.intValue).toSet
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      cheapBooks.flatMap { _.select(descendantElemsOrSelf("Title")) }.map(_.text.trim).toSet
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
    require(bookstore.name == "Bookstore")

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      book1.attributes("Price").toInt < book2.attributes("Price").toInt
    }

    val books = {
      for {
        book <- bookstore.select { childElems("Book") }.sortWith(cheaper _)
        price = book.attributes("Price").toDouble
      } yield {
        new Elem(
          name = "Book",
          children = Vector(
            book.select(childElems()).find(_.name == "Title").get,
            textElem("Price", price.toString)))
      }
    }

    assertResult(4) {
      books.size
    }
    assertResult(List(25, 50, 85, 100)) {
      books.flatMap { _.select(descendantElemsOrSelf("Price")) }.map(_.text.trim.toDouble.intValue)
    }
    assertResult(List(
      "Jennifer's Economical Database Hints",
      "Hector and Jeff's Database Hints",
      "A First Course in Database Systems",
      "Database Systems: The Complete Book")) {

      books.flatMap { _.select(descendantElemsOrSelf("Title")) }.map(_.text.trim)
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * for $n in distinct-values(doc("bookstore.xml")//Last_Name)
   * return &lt;Last_Name&gt;
   *          { $n }
   *        &lt;/Last_Name&gt;
   * }}}
   */
  test("testQueryLastNames") {
    require(bookstore.name == "Bookstore")

    val lastNameValues: immutable.IndexedSeq[String] =
      bookstore.select { descendantElemsOrSelf("Last_Name") }.map(_.text.trim).distinct

    assertResult(Set("Ullman", "Widom", "Garcia-Molina")) {
      lastNameValues.toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * for $b1 in doc("bookstore.xml")/Bookstore/Book
   * for $b2 in doc("bookstore.xml")/Bookstore/Book
   * where $b1/Authors/Author/Last_Name = $b2/Authors/Author/Last_Name
   * and $b1/Title < $b2/Title
   * return &lt;BookPair&gt;
   *          &lt;Title1&gt;{ data($b1/Title) }&lt;/Title1&gt;
   *          &lt;Title2&gt;{ data($b2/Title) }&lt;/Title2&gt;
   *        &lt;/BookPair&gt;
   * }}}
   */
  test("testQueryBookPairsFromSameAuthor") {
    require(bookstore.name == "Bookstore")

    def bookAuthorLastNames(book: Elem): Set[String] = {
      val lastNames = book.select { childElems("Authors") / childElems("Author") / childElems("Last_Name") }
      lastNames.map(_.text.trim).toSet
    }

    def bookTitle(book: Elem): String = book.select(childElems("Title")).headOption.map(_.text.trim).mkString

    val pairs =
      for {
        book1 <- bookstore.select { childElems("Book") }
        book2 <- bookstore.select { childElems("Book") }
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield {
        new Elem(
          name = "BookPair",
          children = Vector(
            textElem("Title1", bookTitle(book1)),
            textElem("Title2", bookTitle(book2))))
      }

    assertResult(5) {
      pairs.size
    }
    assertResult(3) {
      pairs.filter(pair =>
        pair.select(childElems("Title1")).head.text.trim == bookTitle(book1) ||
          pair.select(childElems("Title2")).head.text.trim == bookTitle(book1)).size
    }
    assertResult(3) {
      pairs.filter(pair =>
        pair.select(childElems("Title1")).head.text.trim == bookTitle(book2) ||
          pair.select(childElems("Title2")).head.text.trim == bookTitle(book2)).size
    }
    assertResult(2) {
      pairs.filter(pair =>
        pair.select(childElems("Title1")).head.text.trim == bookTitle(book3) ||
          pair.select(childElems("Title2")).head.text.trim == bookTitle(book3)).size
    }
    assertResult(2) {
      pairs.filter(pair =>
        pair.select(childElems("Title1")).head.text.trim == bookTitle(book4) ||
          pair.select(childElems("Title2")).head.text.trim == bookTitle(book4)).size
    }
  }

  /**
   * The equivalent of XQuery:
   * {{{
   * &lt;InvertedBookstore&gt;
   * {
   *   for $ln in distinct-values(doc("bookstore.xml")//Author/Last_Name)
   *   for $fn in distinct-values(doc("bookstore.xml")//Author[Last_Name = $ln]/First_Name)
   *   return
   *       &lt;Author&gt;
   *         &lt;First_Name&gt; { $fn } &lt;/First_Name&gt;
   *         &lt;Last_Name&gt; { $ln } &lt;/Last_Name&gt;
   *         { for $b in doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = $ln]
   *           return &lt;Book&gt; { $b/@ISBN } { $b/@Price } { $b/Title } &lt;/Book&gt; }
   *       &lt;/Author&gt; }
   * &lt;/InvertedBookstore&gt;
   * }}}
   */
  test("testQueryInvertedBookstore") {
    require(bookstore.name == "Bookstore")

    def books(authorLastName: String) =
      for {
        book <- bookstore.select { childElems("Book") }
        author <- book.select { descendantElemsOrSelf("Author") }
        if author.select(childElems("Last_Name")).head.text.trim == authorLastName
      } yield {
        // Method filterKeys deprecated since Scala 2.13.0.
        val attrs = book.attributes.filter { case (a, _) => Set("ISBN", "Price").contains(a) }.toMap
        new Elem(
          name = "Book",
          attributes = attrs,
          children = book.select(childElems("Title")))
      }

    val authorsWithBooks =
      for {
        lastNameValue <- bookstore.select { descendantElemsOrSelf("Author") / childElems("Last_Name") }.map(_.text.trim).distinct
      } yield {
        val authors: immutable.IndexedSeq[Elem] =
          bookstore.select { descendantElemsOrSelf("Author").where(_.select(childElems("Last_Name")).exists(_.text.trim == lastNameValue)) }

        val firstNameValue: String = authors.head.select(childElems("First_Name")).head.text.trim

        val foundBooks = books(lastNameValue)

        new Elem(
          name = "Author",
          children = Vector(
            textElem("First_Name", firstNameValue),
            textElem("Last_Name", lastNameValue)) ++ foundBooks)
      }

    val invertedBookstore: Elem = new Elem(name = "InvertedBookstore", children = authorsWithBooks)

    assertResult(3) {
      invertedBookstore.select(childElems()).size
    }
  }

  test("testQueryBookAndMagazineTitlesRelabeled") {
    // Taken from the XSLT demo
    require(bookstore.name == "Bookstore")

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore.select { childElems(e => Set("Book", "Magazine").contains(e.name)) }
      } yield {
        val titleString = bookOrMagazine.select { childElems("Title") }.head.text.trim

        if (bookOrMagazine.name == "Book") {
          textElem("BookTitle", titleString)
        } else {
          textElem("MagazineTitle", titleString)
        }
      }

    assertResult(Set("BookTitle", "MagazineTitle")) {
      bookOrMagazineTitles.map(_.name).toSet
    }
    val ngCount = bookOrMagazineTitles.count(_.text.trim == "National Geographic")
    assert(ngCount == 2, "Expected 'National Geographic' twice")
  }

  test("testQueryAuthorLastNamesShowingMonoidLaws") {
    // XPath: doc("bookstore.xml")//Book[@Price < 90]//Authors/Author/Last_Name

    require(bookstore.name == "Bookstore")

    val lastNames1 = bookstore.select {
      descendantElemsOrSelf("Book").where(_.attributes("Price").toInt < 90) / descendantElemsOrSelf("Authors") /
        childElems("Author") / childElems("Last_Name")
    }

    val lastNames2 = bookstore.select {
      descendantElemsOrSelf("Book").where(_.attributes("Price").toInt < 90) /
        (descendantElemsOrSelf("Authors") / (childElems("Author") / childElems("Last_Name")))
    }

    val lastNames3 = bookstore.select {
      (descendantElemsOrSelf("Book").where(_.attributes("Price").toInt < 90) / descendantElemsOrSelf("Authors")) /
        (childElems("Author") / childElems("Last_Name"))
    }

    val lastNames4 = bookstore.select {
      descendantElemsOrSelf("Book").where(_.attributes("Price").toInt < 90) / descendantElemsOrSelf("Authors") /
        childElems("Author") / childElems("Last_Name") / Step.empty
    }

    val lastNames5 = bookstore.select {
      descendantElemsOrSelf("Book").where(_.attributes("Price").toInt < 90) / descendantElemsOrSelf("Authors") /
        childElems("Author") / (Step.empty / childElems("Last_Name"))
    }

    assertResult(Set("Ullman", "Widom", "Garcia-Molina")) {
      lastNames1.map(_.text.trim).toSet
    }

    assertResult(lastNames1.map(_.text.trim).toSet) {
      lastNames2.map(_.text.trim).toSet
    }

    assertResult(lastNames1.map(_.text.trim).toSet) {
      lastNames3.map(_.text.trim).toSet
    }

    assertResult(lastNames1.map(_.text.trim).toSet) {
      lastNames4.map(_.text.trim).toSet
    }

    assertResult(lastNames1.map(_.text.trim).toSet) {
      lastNames5.map(_.text.trim).toSet
    }
  }

  private def textElem(elmName: String, txt: String): Elem =
    new Elem(
      name = elmName,
      attributes = Map(),
      children = Vector(Text(txt)))

  private val book1: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-0-13-713526-2", "Price" -> "85", "Edition" -> "3rd"),
      children = Vector(
        textElem("Title", "A First Course in Database Systems"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jeffrey"),
                textElem("Last_Name", "Ullman"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jennifer"),
                textElem("Last_Name", "Widom")))))))
  }

  private val book2: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-0-13-815504-6", "Price" -> "100"),
      children = Vector(
        textElem("Title", "Database Systems: The Complete Book"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Hector"),
                textElem("Last_Name", "Garcia-Molina"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jeffrey"),
                textElem("Last_Name", "Ullman"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jennifer"),
                textElem("Last_Name", "Widom"))))),
        textElem("Remark", "Buy this book bundled with \"A First Course\" - a great deal!")))
  }

  private val book3: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-0-11-222222-3", "Price" -> "50"),
      children = Vector(
        textElem("Title", "Hector and Jeff's Database Hints"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jeffrey"),
                textElem("Last_Name", "Ullman"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Hector"),
                textElem("Last_Name", "Garcia-Molina"))))),
        textElem("Remark", "An indispensable companion to your textbook")))
  }

  private val book4: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-9-88-777777-6", "Price" -> "25"),
      children = Vector(
        textElem("Title", "Jennifer's Economical Database Hints"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jennifer"),
                textElem("Last_Name", "Widom")))))))
  }

  private val magazine1: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "January", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "National Geographic")))
  }

  private val magazine2: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "February", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "National Geographic")))
  }

  private val magazine3: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "February", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "Newsweek")))
  }

  private val magazine4: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "March", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "Hector and Jeff's Database Hints")))
  }

  private val bookstore: Elem = {
    new Elem(
      name = "Bookstore",
      children = Vector(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4))
  }
}

object SaxonInspiredQueryTest {

  // Abstract Step and Step factory API

  /**
   * Step, which is a function from elements to collections of elements. This is a monoid. See
   * https://typelevel.org/cats/typeclasses/monoid.html.
   */
  trait Step[E] extends Function1[E, immutable.IndexedSeq[E]] {

    /**
     * Associative operation to combine 2 steps.
     */
    def combine(step: Step[E]): Step[E] = {
      { elem => this(elem).flatMap(step) }
    }

    /**
     * Alias for method combine.
     */
    def /(step: Step[E]): Step[E] = {
      combine(step)
    }

    def where(p: E => Boolean): Step[E] = {
      { elem => this(elem).filter(p) }
    }

    def cat(step: Step[E]): Step[E] = {
      { elem => this(elem) ++ step(elem) }
    }

    def first: Step[E] = {
      { elem => immutable.IndexedSeq(this(elem).head) }
    }

    def firstOption: Step[E] = {
      { elem => this(elem).headOption.toIndexedSeq }
    }
  }

  object Step {

    /**
     * The empty value of the Step monoid.
     */
    def empty[E]: Step[E] = {
      { elem => immutable.IndexedSeq(elem) }
    }
  }

  trait ElemStepFactoryApi[E, S <: Step[E]] {

    def childElems(): S

    def childElems(name: String): S

    def childElems(p: E => Boolean): S

    def descendantElemsOrSelf(): S

    def descendantElemsOrSelf(name: String): S

    def descendantElemsOrSelf(p: E => Boolean): S
  }

  // Concrete Step and Step factory API implementation

  type ElemStep = Step[Elem]

  object ElemStepFactory extends ElemStepFactoryApi[Elem, ElemStep] {

    def childElems(): ElemStep = childElems(_ => true)

    def childElems(name: String): ElemStep = childElems(_.name == name)

    def childElems(p: Elem => Boolean): ElemStep = {
      { elem => elem.filterChildElems(p) }
    }

    def descendantElemsOrSelf(): ElemStep = descendantElemsOrSelf(_ => true)

    def descendantElemsOrSelf(name: String): ElemStep = descendantElemsOrSelf(_.name == name)

    def descendantElemsOrSelf(p: Elem => Boolean): ElemStep = {
      { elem => elem.filterDescendantElemsOrSelf(p) }
    }
  }

  /**
   * Naive node trait, with only subclasses for elements and text nodes.
   */
  sealed trait Node

  /**
   * Naive element class, which for example is not namespace-aware.
   *
   * This class shows how method calls select(childElems()) and select(descendantElemsOrSelf()), along with the Scala
   * Collections API, already provide a pretty powerful XML querying API.
   */
  final class Elem(
    val name: String,
    val attributes: Map[String, String],
    val children: immutable.IndexedSeq[Node]) extends Node {

    def this(name: String, children: immutable.IndexedSeq[Node]) =
      this(name, Map(), children)

    def select(step: Step[Elem]): immutable.IndexedSeq[Elem] = {
      step(this)
    }

    def text: String = {
      val textStrings = children collect { case t: Text => t.text }
      textStrings.mkString
    }

    private[notyaidom] def filterChildElems(p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
      this.children.collect { case e: Elem if p(e) => e }
    }

    private[notyaidom] def filterDescendantElemsOrSelf(p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
      // Recursive call
      Vector(this).filter(p) ++ filterChildElems(_ => true).flatMap(e => e.filterDescendantElemsOrSelf(p))
    }
  }

  /**
   * Naive text node class.
   */
  final case class Text(val text: String) extends Node
}
