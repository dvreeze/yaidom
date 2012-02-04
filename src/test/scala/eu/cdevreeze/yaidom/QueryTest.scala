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
import ExpandedName._
import QName._

/**
 * Query test case. This test case shows how XPath and XQuery queries can be written in this API, be it somewhat
 * verbosely. More than anything else, it demonstrates some of the expressive power of Scala's excellent Collections API.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class QueryTest extends Suite {

  @Test def testQueryBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Elem] =
      bookstore.childElems("Book".ename) map { e => e.childElem("Title".ename) }

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      bookTitles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryBookOrMagazineTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.qname.localPart == "Bookstore")

    val bookOrMagazineTitles: immutable.Seq[Elem] =
      for {
        bookOrMagazine <- bookstore childElemsWhere { e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName) }
      } yield bookOrMagazine.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      bookOrMagazineTitles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Elem] =
      for (ch <- bookstore.allChildElems) yield ch.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      titles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryAllTitles() {
    // XPath: doc("bookstore.xml")//Title

    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Elem] =
      for (title <- bookstore.elems("Title".ename)) yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      titles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryAllElements() {
    // XPath: doc("bookstore.xml")//*

    require(bookstore.qname.localPart == "Bookstore")

    val elements: immutable.Seq[Elem] = bookstore.allElems :+ bookstore

    assert(elements.contains(bookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")

    val childrenAlsoIncluded =
      elements forall { e =>
        e.allChildElems forall { ch => elements.contains(ch) }
      }
    assert(childrenAlsoIncluded, "Expected child elements of each element also in the result")
  }

  @Test def testQueryBookIsbns() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(bookstore.qname.localPart == "Bookstore")

    val isbns: immutable.Seq[String] =
      for (book <- bookstore.childElems("Book".ename)) yield book.attribute("ISBN".ename)

    expect(Set(
      "ISBN-0-13-713526-2",
      "ISBN-0-13-815504-6",
      "ISBN-0-11-222222-3",
      "ISBN-9-88-777777-6")) {
      isbns.toSet
    }
  }

  @Test def testQueryCheapBooks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

    require(bookstore.qname.localPart == "Bookstore")

    val books: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename)
        if price.toInt < 90
      } yield book

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      books flatMap { book => book.firstElemOption("Title".ename) map { _.trimmedText } } toSet
    }
  }

  @Test def testQueryCheapBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        if book.attribute("Price".ename).toInt < 90
      } yield book.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      titles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryTitlesOfBooksWithRemarks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        if !book.childElems("Remark".ename).isEmpty
      } yield book.childElem("Title".ename)

    expect(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      bookTitles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        if book.attribute("Price".ename).toInt < 90
        val authors = book.childElem("Authors".ename)
        val lastNameOption = {
          val result =
            for {
              author <- authors.childElems("Author".ename)
              val lastName = author.childElem("Last_Name".ename)
              if lastName.trimmedText == "Ullman"
            } yield lastName
          result.headOption
        }
        if lastNameOption.isDefined
      } yield book.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      bookTitles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        if book.attribute("Price".ename).toInt < 90
        val authors = book.childElem("Authors".ename)
        val lastNameOption = {
          val result =
            for {
              author <- authors.childElems("Author".ename)
              val lastName = author.childElem("Last_Name".ename)
              val firstName = author.childElem("First_Name".ename)
              if lastName.trimmedText == "Ullman"
              if firstName.trimmedText == "Jeffrey"
            } yield lastName
          result.headOption
        }
        if lastNameOption.isDefined
      } yield book.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      bookTitles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryTitlesOfBooksByJeffreyUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authors = book.childElem("Authors".ename)
        val lastNameStrings: immutable.Seq[String] = for {
          author <- authors.childElems("Author".ename)
          val lastNameString = author.childElem("Last_Name".ename).trimmedText
        } yield lastNameString
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.childElem("Title".ename)

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      bookTitles map { _.trimmedText } toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstore.qname.localPart == "Bookstore")

    val secondAuthors: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authors = book.childElem("Authors".ename)
        val authorColl = authors.childElems("Author".ename)
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames: immutable.Seq[Elem] = secondAuthors map { e => e.childElem("Last_Name".ename) }
    expect(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      secondAuthorLastNames map { _.trimmedText } toSet
    }
  }

  @Test def testQueryGreatBooks() {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        remark <- book.childElems("Remark".ename)
        if remark.trimmedText.indexOf("great") >= 0
      } yield book.childElem("Title".ename)

    expect(Set("Database Systems: The Complete Book")) {
      titles map { _.trimmedText } toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook() {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.qname.localPart == "Bookstore")

    val magazines: immutable.Seq[Elem] =
      for {
        magazine <- bookstore.childElems("Magazine".ename)
        val magazineTitle: String = magazine.childElem("Title".ename).trimmedText
        val booksWithSameName = for {
          book <- bookstore.childElems("Book".ename)
          val bookTitle: String = book.childElem("Title".ename).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    expect(Set("Hector and Jeff's Database Hints")) {
      magazines flatMap { mag => mag.firstElemOption("Title".ename) map { _.trimmedText } } toSet
    }
  }

  @Test def testQueryElementsWithParentNotBookOrBookstore() {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    // TODO What we need is some index to parent nodes, e.g. from "path expressions" to parent nodes...

    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.Seq[Elem] =
      for {
        desc <- bookstore.allElems
        parent <- desc.findParentInTree(bookstore) // Inefficient
        if parent.qname != "Bookstore".qname && parent.qname != "Book".qname
      } yield desc

    assert(elms.size > 10, "Expected more than 10 matching elements")
    val qnames: Set[QName] = {
      val result = elms map { _.qname }
      result.toSet
    }
    expect(Set("Title".qname, "Author".qname, "First_Name".qname, "Last_Name".qname)) {
      qnames
    }
  }

  @Test def testQueryBooksOrMagazinesWithNonUniqueTitles() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(bookstore.qname.localPart == "Bookstore")

    val booksAndMagazines: immutable.Seq[Elem] =
      for {
        bookOrMagazine <- bookstore childElemsWhere { e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName) }
        val titleString: String = bookOrMagazine.childElem("Title".ename).trimmedText
        val otherBooksAndMagazines = {
          val result = bookstore childElemsWhere { e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName) }
          result.toSet -- Set(bookOrMagazine)
        }
        val titles = otherBooksAndMagazines map { e => e.childElem("Title".ename) }
        val titleStrings: Set[String] = {
          val result = titles map { _.trimmedText }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      booksAndMagazines flatMap { mag => mag.firstElemOption("Title".ename) map { _.trimmedText } } toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBook() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstore.qname.localPart == "Bookstore")

    val booksAndMagazines: immutable.Seq[Elem] =
      for {
        bookOrMagazine <- bookstore childElemsWhere { e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName) }
        val titleString: String = bookOrMagazine.childElem("Title".ename).trimmedText
        val otherBooks = bookstore.childElems("Book".ename).toSet -- Set(bookOrMagazine)
        val titles = otherBooks map { e => e.childElem("Title".ename) }
        val titleStrings: Set[String] = {
          val result = titles map { _.trimmedText }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints")) {
      booksAndMagazines flatMap { mag => mag.firstElemOption("Title".ename) map { _.trimmedText } } toSet
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
  @Test def testQueryBooksWithAllAuthorFirstNamesWithLetterJ() {
    require(bookstore.qname.localPart == "Bookstore")

    val books: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authorNames: Set[String] = {
          val result = for {
            author <- book.elems("Author".ename)
            val firstName = author.childElem("First_Name".ename)
          } yield firstName.trimmedText
          result.toSet
        }
        if authorNames forall { name => name.indexOf("J") >= 0 }
      } yield book

    expect(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      books flatMap { book => book.firstElemOption("Title".ename) map { _.trimmedText } } toSet
    }
  }

  @Test def testQueryBooksFromUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authorNames: Set[String] = {
          val result = book.elems("Author".ename) map { _.childElem("Last_Name".ename).trimmedText }
          result.toSet
        }
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.childElem("Title".ename)

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      titles map { _.trimmedText } toSet
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
  @Test def testQueryBooksWithAuthorInTitle() {
    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    val titleAndFirstNames: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        val title = book.childElem("Title".ename)
        val authorFirstNames: Set[String] = {
          val result = book.elems("Author".ename) map { _.childElem("First_Name".ename).trimmedText }
          result.toSet
        }
        val searchedForFirstNames: Set[String] = authorFirstNames filter { firstName => title.trimmedText.indexOf(firstName) >= 0 }
        if !searchedForFirstNames.isEmpty
      } yield elem(
        qname = "Book".qname,
        children = List(
          fromElem(title)(Scope.Empty),
          elem(qname = "First_Name".qname, children = List(text(searchedForFirstNames.head))))).build()

    expect(2) {
      titleAndFirstNames.size
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val result = titleAndFirstNames map { e => e.elems("Title".ename) }
      result.flatten map { e => e.trimmedText } toSet
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
  @Test def testQueryAverageBookPrice() {
    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    val prices: immutable.Seq[Double] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename).toDouble
      } yield price
    val averagePrice =
      elem(qname = "Average".qname,
        children = List(text((prices.sum.toDouble / prices.size).toString))).build()

    expect(65) {
      averagePrice.trimmedText.toDouble.intValue
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
  @Test def testQueryBooksPricedBelowAverage() {
    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    val prices: immutable.Seq[Double] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename).toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks: immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename).toDouble
        if price < avg
      } yield elem(
        qname = "Book".qname,
        children = List(
          fromElem(book.childElem("Title".ename))(Scope.Empty),
          elem(
            qname = "Price".qname,
            children = List(text(price.toString))))).build()

    expect(2) {
      cheapBooks.size
    }
    expect(Set(50, 25)) {
      cheapBooks flatMap { e => e.elems("Price".ename) } map { e => e.trimmedText.toDouble.intValue } toSet
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      cheapBooks flatMap { e => e.elems("Title".ename) } map { e => e.trimmedText } toSet
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
  @Test def testQueryBooksOrderedByPrice() {
    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attribute("Price".ename).toInt
      val price2 = book2.attribute("Price".ename).toInt
      price1 < price2
    }

    val books: immutable.Seq[Elem] = {
      for {
        book <- bookstore.childElems("Book".ename) sortWith { cheaper _ }
        val price = book.attribute("Price".ename).toDouble
      } yield elem(
        qname = "Book".qname,
        children = List(
          fromElem(book.childElem("Title".ename))(Scope.Empty),
          elem(
            qname = "Price".qname,
            children = List(text(price.toString))))).build()
    }

    expect(4) {
      books.size
    }
    expect(List(25, 50, 85, 100)) {
      books flatMap { e => e.elems("Price".ename) } map { e => e.trimmedText.toDouble.intValue }
    }
    expect(List(
      "Jennifer's Economical Database Hints",
      "Hector and Jeff's Database Hints",
      "A First Course in Database Systems",
      "Database Systems: The Complete Book")) {
      books flatMap { e => e.elems("Title".ename) } map { e => e.trimmedText }
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
  @Test def testQueryLastNames() {
    require(bookstore.qname.localPart == "Bookstore")

    val lastNameValues: immutable.Seq[String] =
      for {
        lastName <- bookstore.elems("Last_Name".ename) map { e => e.trimmedText } distinct
      } yield lastName

    expect(Set(
      "Ullman",
      "Widom",
      "Garcia-Molina")) {
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
  @Test def testQueryBookPairsFromSameAuthor() {
    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    def bookAuthorLastNames(book: Elem): Set[String] = {
      val authors = book.childElem("Authors".ename)
      val result = for {
        author <- authors.childElems("Author".ename)
        val lastName = author.childElem("Last_Name".ename)
        val lastNameValue: String = lastName.trimmedText
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: Elem): String = book.childElem("Title".ename).trimmedText

    val pairs: immutable.Seq[Elem] =
      for {
        book1 <- bookstore.childElems("Book".ename)
        book2 <- bookstore.childElems("Book".ename)
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield elem(
        qname = "BookPair".qname,
        children = List(
          elem(
            qname = "Title1".qname,
            children = List(text(bookTitle(book1)))),
          elem(
            qname = "Title2".qname,
            children = List(text(bookTitle(book2)))))).build()

    expect(5) {
      pairs.size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).trimmedText == bookTitle(book1.build()) ||
          pair.childElem("Title2".ename).trimmedText == bookTitle(book1.build())).size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).trimmedText == bookTitle(book2.build()) ||
          pair.childElem("Title2".ename).trimmedText == bookTitle(book2.build())).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).trimmedText == bookTitle(book3.build()) ||
          pair.childElem("Title2".ename).trimmedText == bookTitle(book3.build())).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).trimmedText == bookTitle(book4.build()) ||
          pair.childElem("Title2".ename).trimmedText == bookTitle(book4.build())).size
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
  @Test def testQueryInvertedBookstore() {
    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    def books(authorLastName: String): immutable.Seq[Elem] =
      for {
        book <- bookstore.childElems("Book".ename)
        author <- book.elems("Author".ename)
        if author.childElem("Last_Name".ename).trimmedText == authorLastName
      } yield {
        val attrs = book.attributes filterKeys { a => Set("ISBN".qname, "Price".qname).contains(a) }
        elem(
          qname = "Book".qname,
          attributes = attrs).
          withChildNodes(book.childElems("Title".ename))(Scope.Empty).build()
      }

    val authorsWithBooks: immutable.Seq[Elem] =
      for {
        lastNameValue <- bookstore.elems("Author".ename) map { e => e.childElem("Last_Name".ename).trimmedText } distinct
      } yield {
        val author: Elem = {
          val result = for {
            author <- bookstore.elems("Author".ename)
            if author.childElem("Last_Name".ename).trimmedText == lastNameValue
          } yield author
          result.head
        }
        val firstNameValue: String = author.childElem("First_Name".ename).trimmedText

        val foundBooks = books(lastNameValue)
        val bookBuilders = foundBooks map { book => fromElem(book)(Scope.Empty) }

        elem(
          qname = "Author".qname,
          children = List(
            elem(
              qname = "First_Name".qname,
              children = List(text(firstNameValue))),
            elem(
              qname = "Last_Name".qname,
              children = List(text(lastNameValue)))) ++ bookBuilders).build()
      }

    val invertedBookstore: Elem = Elem(qname = "InvertedBookstore".qname, children = authorsWithBooks)

    expect(3) {
      invertedBookstore.allChildElems.size
    }
  }

  @Test def testQueryBookAndMagazineTitlesRelabeled() {
    // Taken from the XSLT demo
    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    val bookOrMagazineTitles: immutable.Seq[Elem] =
      for {
        bookOrMagazine <- bookstore childElemsWhere { e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName) }
      } yield {
        val titleString = bookOrMagazine.childElem("Title".ename).trimmedText

        if (bookOrMagazine.resolvedName == "Book".ename) {
          elem(
            qname = "BookTitle".qname,
            children = List(text(titleString))).build()
        } else {
          elem(
            qname = "MagazineTitle".qname,
            children = List(text(titleString))).build()
        }
      }

    expect(Set("BookTitle".ename, "MagazineTitle".ename)) {
      bookOrMagazineTitles.map(e => e.resolvedName).toSet
    }
    val ngCount = bookOrMagazineTitles count { e => e.trimmedText == "National Geographic" }
    assert(ngCount == 2, "Expected 'National Geographic' twice")
  }

  @Test def testTransformLeavingOutPrices() {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, leaving out book prices

    require(bookstore.qname.localPart == "Bookstore")

    def removePrice(book: Elem): Elem = {
      require(book.resolvedName == "Book".ename)
      Elem(
        qname = book.qname,
        attributes = book.attributes filterKeys { a => a != "Price".qname },
        scope = book.scope,
        children = book.children)
    }

    val bookstoreWithoutPrices: Elem =
      bookstore.copyAndTransform(
        bookstore,
        { case Elem.RootAndElem(root, elem) if elem.resolvedName == "Book".ename => removePrice(elem) })

    expect(4) {
      bookstore.elems("Book".ename) count { e => e.attributeOption("Price".ename).isDefined }
    }
    expect(0) {
      bookstoreWithoutPrices.elems("Book".ename) count { e => e.attributeOption("Price".ename).isDefined }
    }
  }

  @Test def testTransformCombiningFirstAndLastName() {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, combining first and last names into Name elements

    require(bookstore.qname.localPart == "Bookstore")

    import NodeBuilder._

    def combineName(author: Elem): Elem = {
      require(author.resolvedName == "Author".ename)

      val firstNameValue: String = author.childElem("First_Name".ename).trimmedText
      val lastNameValue: String = author.childElem("Last_Name".ename).trimmedText
      val nameValue: String = "%s %s".format(firstNameValue, lastNameValue)
      val name: ElemBuilder = elem(qname = "Name".qname, children = List(text(nameValue)))

      elem(
        qname = author.qname,
        attributes = author.attributes,
        namespaces = Scope.Empty.relativize(author.scope),
        children = List(name)).build()
    }

    val bookstoreWithCombinedNames: Elem =
      bookstore.copyAndTransform(
        bookstore,
        { case Elem.RootAndElem(root, elem) if elem.resolvedName == "Author".ename => combineName(elem) })

    expect(Set("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina")) {
      bookstoreWithCombinedNames.elems("Name".ename) map { _.trimmedText } toSet
    }
  }

  private val book1: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-0-13-713526-2", "Price".qname -> "85", "Edition".qname -> "3rd"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(
            text("A First Course in Database Systems"))),
        elem(
          qname = "Authors".qname,
          children = List(
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Jeffrey"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Ullman"))))),
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Jennifer"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Widom")))))))))
  }

  private val book2: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-0-13-815504-6", "Price".qname -> "100"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(
            text("Database Systems: The Complete Book"))),
        elem(
          qname = "Authors".qname,
          children = List(
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Hector"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Garcia-Molina"))))),
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Jeffrey"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Ullman"))))),
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Jennifer"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Widom"))))))),
        elem(
          qname = "Remark".qname,
          children = List(text("Buy this book bundled with \"A First Course\" - a great deal!")))))
  }

  private val book3: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-0-11-222222-3", "Price".qname -> "50"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(
            text("Hector and Jeff's Database Hints"))),
        elem(
          qname = "Authors".qname,
          children = List(
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Jeffrey"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Ullman"))))),
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Hector"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Garcia-Molina"))))))),
        elem(
          qname = "Remark".qname,
          children = List(text("An indispensable companion to your textbook")))))
  }

  private val book4: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-9-88-777777-6", "Price".qname -> "25"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(
            text("Jennifer's Economical Database Hints"))),
        elem(
          qname = "Authors".qname,
          children = List(
            elem(
              qname = "Author".qname,
              children = List(
                elem(
                  qname = "First_Name".qname,
                  children = List(text("Jennifer"))),
                elem(
                  qname = "Last_Name".qname,
                  children = List(text("Widom")))))))))
  }

  private val magazine1: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "January", "Year".qname -> "2009"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(text("National Geographic")))))
  }

  private val magazine2: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "February", "Year".qname -> "2009"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(text("National Geographic")))))
  }

  private val magazine3: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "February", "Year".qname -> "2009"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(text("Newsweek")))))
  }

  private val magazine4: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "March", "Year".qname -> "2009"),
      children = List(
        elem(
          qname = "Title".qname,
          children = List(text("Hector and Jeff's Database Hints")))))
  }

  private val bookstore: Elem = {
    import NodeBuilder._

    elem(
      qname = "Bookstore".qname,
      children = List(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)
  }
}
