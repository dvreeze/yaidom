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

package eu.cdevreeze.notyaidom

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Query test case, using a very naive mini version of yaidom, thus showing how
 * yaidom on top of the Scala Collections API is really a low hanging fruit.
 *
 * Much of the code in this test class could be used in a presentation on yaidom that illustrates
 * how easy it is to get some (!) rudimentary XML processing capabilities on top of the Scala Collections
 * API, by defining only a small Elem class with only a few methods. The presentation could then
 * gradually move on to "real" yaidom examples, introducing namespaces, scopes, other node types,
 * parsing/printing/converting, etc.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class NonYaidomQueryTest extends Suite {

  /**
   * Naive node trait, with only subclasses for elements and text nodes.
   */
  sealed trait Node extends Immutable

  /**
   * Naive element class, which for example is not namespace-aware.
   *
   * This class shows how methods findAllElemsOrSelf and findAllElemsOrSelf, along with the Scala
   * Collections API, already provide a pretty powerful XML querying API.
   */
  final class Elem(
    val name: String,
    val attributes: Map[String, String],
    val children: immutable.IndexedSeq[Node]) extends Node {

    def this(name: String, children: immutable.IndexedSeq[Node]) =
      this(name, Map(), children)

    def allChildElems: immutable.IndexedSeq[Elem] =
      children collect { case e: Elem => e }

    /**
     * Finds all descendant elements and self.
     */
    def findAllElemsOrSelf: immutable.IndexedSeq[Elem] = {
      // Recursive, but not tail-recursive
      val self = Elem.this
      val elms = allChildElems flatMap { _.findAllElemsOrSelf }
      self +: elms
    }

    /**
     * Finds all topmost descendant elements and self, obeying the given predicate.
     * If a matching element has been found, its descendants are not searched for matches (hence "topmost").
     */
    def findTopmostElemsOrSelf(p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
      // Recursive, but not tail-recursive
      if (p(Elem.this)) {
        Vector(Elem.this)
      } else {
        allChildElems flatMap { _.findTopmostElemsOrSelf(p) }
      }
    }

    def text: String = {
      val textStrings = children collect { case t: Text => t.text }
      textStrings.mkString
    }
  }

  /**
   * Naive text node class.
   */
  final case class Text(val text: String) extends Node

  @Test def testQueryBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      bookstore.allChildElems filter { _.name == "Book" } map { e =>
        val result = e.allChildElems find (_.name == "Title")
        result.get
      }

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryBookOrMagazineTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.name == "Bookstore")

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore.allChildElems filter { e => Set("Book", "Magazine").contains(e.name) }
      } yield {
        val result = bookOrMagazine.allChildElems find { _.name == "Title" }
        result.get
      }

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.name == "Bookstore")

    val titles =
      for (ch <- bookstore.allChildElems) yield {
        val result = ch.allChildElems find { _.name == "Title" }
        result.get
      }

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryAllTitles() {
    // XPath: doc("bookstore.xml")//Title

    require(bookstore.name == "Bookstore")

    val titles =
      for (title <- bookstore.findAllElemsOrSelf filter (_.name == "Title")) yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryAllElements() {
    // XPath: doc("bookstore.xml")//*

    require(bookstore.name == "Bookstore")

    val elements = bookstore.findAllElemsOrSelf

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

    require(bookstore.name == "Bookstore")

    val isbns =
      for (book <- bookstore.allChildElems filter (_.name == "Book")) yield book.attributes("ISBN")

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

    require(bookstore.name == "Bookstore")

    val books =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        price = book.attributes("Price")
        if price.toInt < 90
      } yield book

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.allChildElems.find(_.name == "Title") map { _.text.trim } }
      result.toSet
    }
  }

  @Test def testQueryCheapBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.name == "Bookstore")

    val titles =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        if book.attributes("Price").toInt < 90
      } yield {
        val result = book.allChildElems find { _.name == "Title" }
        result.get
      }

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryCheapBookAuthors() {
    // Own example..

    require(bookstore.name == "Bookstore")

    val cheapBookElms =
      for {
        bookElm <- bookstore.allChildElems filter { _.name == "Book" }
        price = bookElm.attributes("Price")
        if price.toInt < 90
      } yield bookElm

    val cheapBookAuthors = {
      val result =
        for {
          cheapBookElm <- cheapBookElms
          authorElm <- cheapBookElm.findAllElemsOrSelf filter { _.name == "Author" }
        } yield {
          val firstNameElmOption = authorElm.allChildElems find { _.name == "First_Name" }
          val lastNameElmOption = authorElm.allChildElems find { _.name == "Last_Name" }

          val firstName = firstNameElmOption.map(_.text).getOrElse("")
          val lastName = lastNameElmOption.map(_.text).getOrElse("")
          (firstName + " " + lastName).trim
        }

      result.toSet
    }

    expect(Set(
      "Jeffrey Ullman",
      "Jennifer Widom",
      "Hector Garcia-Molina")) {
      cheapBookAuthors
    }
  }

  @Test def testQueryTitlesOfBooksWithRemarks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        if !book.allChildElems.filter(_.name == "Remark").isEmpty
      } yield book.allChildElems.filter(_.name == "Title").head

    expect(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        if book.attributes("Price").toInt < 90
        authors = book.allChildElems.filter(_.name == "Authors").head
        authorLastName <- authors.allChildElems filter { _.name == "Author" } flatMap { e => e.allChildElems filter (_.name == "Last_Name") } map { _.text.trim }
        if authorLastName == "Ullman"
      } yield book.allChildElems.find(_.name == "Title").get

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        if book.attributes("Price").toInt < 90
        authors = book.allChildElems.filter(_.name == "Authors").head
        authorLastName <- authors.allChildElems filter { _.name == "Author" } flatMap { e => e.allChildElems filter (_.name == "Last_Name") } map { _.text.trim }
        if authorLastName == "Ullman"
        authorFirstName <- authors.allChildElems filter { _.name == "Author" } flatMap { e => e.allChildElems filter (_.name == "First_Name") } map { _.text.trim }
        if authorFirstName == "Jeffrey"
      } yield book.allChildElems.find(_.name == "Title").get

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfBooksByJeffreyUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    require(bookstore.name == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        authors = book.allChildElems.find(_.name == "Authors").get
        lastNameStrings = for {
          author <- authors.allChildElems filter { _.name == "Author" }
          lastNameString = author.allChildElems.find(_.name == "Last_Name").get.text.trim
        } yield lastNameString
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.allChildElems.find(_.name == "Title").get

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstore.name == "Bookstore")

    val secondAuthors =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        authors = book.allChildElems.find(_.name == "Authors").get
        authorColl = authors.allChildElems filter { _.name == "Author" }
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames = secondAuthors map { e => e.allChildElems.find(_.name == "Last_Name").get }
    expect(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      val result = secondAuthorLastNames map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryGreatBooks() {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    require(bookstore.name == "Bookstore")

    val titles =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        remark <- book.allChildElems filter { _.name == "Remark" }
        if remark.text.trim.indexOf("great") >= 0
      } yield book.allChildElems.find(_.name == "Title").get

    expect(Set("Database Systems: The Complete Book")) {
      val result = titles map { _.text.trim }
      result.toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook() {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.name == "Bookstore")

    val magazines =
      for {
        magazine <- bookstore.allChildElems filter { _.name == "Magazine" }
        magazineTitle = magazine.allChildElems.find(_.name == "Title").get.text.trim
        booksWithSameName = for {
          book <- bookstore.allChildElems filter { _.name == "Book" }
          bookTitle = book.allChildElems.find(_.name == "Title").get.text.trim
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    expect(Set("Hector and Jeff's Database Hints")) {
      val result = magazines flatMap { mag => mag.findAllElemsOrSelf find (_.name == "Title") map { _.text.trim } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithNonUniqueTitles() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(bookstore.name == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore.allChildElems filter { e => Set("Book", "Magazine").contains(e.name) }
        titleString: String = bookOrMagazine.allChildElems.find(_.name == "Title").get.text.trim
        otherBooksAndMagazines = {
          val result = bookstore.allChildElems filter { e => Set("Book", "Magazine").contains(e.name) }
          result.toSet -- Set(bookOrMagazine)
        }
        titles = otherBooksAndMagazines map { e => e.allChildElems.find(_.name == "Title").get }
        titleStrings = {
          val result = titles map { _.text.trim }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = booksAndMagazines flatMap { mag => mag.findAllElemsOrSelf find (_.name == "Title") map { _.text.trim } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBook() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstore.name == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore.allChildElems filter { e => Set("Book", "Magazine").contains(e.name) }
        titleString: String = bookOrMagazine.allChildElems.find(_.name == "Title").get.text.trim
        otherBooks = bookstore.allChildElems.filter(_.name == "Book").toSet -- Set(bookOrMagazine)
        titles = otherBooks map { e => e.allChildElems.find(_.name == "Title").get }
        titleStrings = {
          val result = titles map { _.text.trim }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints")) {
      val result = booksAndMagazines flatMap { mag => mag.findAllElemsOrSelf find (_.name == "Title") map { _.text.trim } }
      result.toSet
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
    require(bookstore.name == "Bookstore")

    val books =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        authorNames = {
          val result = for {
            author <- book.findAllElemsOrSelf filter (_.name == "Author")
            firstName = author.allChildElems.find(_.name == "First_Name").get
          } yield firstName.text.trim
          result.toSet
        }
        if authorNames forall { name => name.indexOf("J") >= 0 }
      } yield book

    expect(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findAllElemsOrSelf.find(_.name == "Title") map { _.text.trim } }
      result.toSet
    }
  }

  @Test def testQueryBooksFromUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(bookstore.name == "Bookstore")

    val titles =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        authorNames = {
          val result = book.findAllElemsOrSelf filter { _.name == "Author" } map { _.allChildElems.find(_.name == "Last_Name").get.text.trim }
          result.toSet
        }
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.allChildElems.find(_.name == "Title").get

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      val result = titles map { _.text.trim }
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
  @Test def testQueryBooksWithAuthorInTitle() {
    require(bookstore.name == "Bookstore")

    val titleAndFirstNames =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        title = book.allChildElems.find(_.name == "Title").get
        authorFirstNames = {
          val result = book.findAllElemsOrSelf filter { _.name == "Author" } map { _.allChildElems.find(_.name == "First_Name").get.text.trim }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames filter { firstName => title.text.trim.indexOf(firstName) >= 0 }
        if !searchedForFirstNames.isEmpty
      } yield new Elem(
        name = "Book",
        children = Vector(
          title,
          textElem("First_Name", searchedForFirstNames.head)))

    expect(2) {
      titleAndFirstNames.size
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val titleElms = titleAndFirstNames map { e => e.findAllElemsOrSelf filter { _.name == "Title" } }
      val result = titleElms.flatten map { e => e.text.trim }
      result.toSet
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
    require(bookstore.name == "Bookstore")

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        price = book.attributes("Price").toDouble
      } yield price
    val averagePrice =
      textElem("Average", (prices.sum.toDouble / prices.size).toString)

    expect(65) {
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
  @Test def testQueryBooksPricedBelowAverage() {
    require(bookstore.name == "Bookstore")

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        price = book.attributes("Price").toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        price = book.attributes("Price").toDouble
        if price < avg
      } yield new Elem(
        name = "Book",
        children = Vector(
          book.allChildElems.find(_.name == "Title").get,
          textElem("Price", price.toString)))

    expect(2) {
      cheapBooks.size
    }
    expect(Set(50, 25)) {
      val result = cheapBooks flatMap { e => e.findAllElemsOrSelf filter { _.name == "Price" } } map { e => e.text.trim.toDouble.intValue }
      result.toSet
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val result = cheapBooks flatMap { e => e.findAllElemsOrSelf filter { _.name == "Title" } } map { e => e.text.trim }
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
  @Test def testQueryBooksOrderedByPrice() {
    require(bookstore.name == "Bookstore")

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attributes("Price").toInt
      val price2 = book2.attributes("Price").toInt
      price1 < price2
    }

    val books = {
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" } sortWith { cheaper _ }
        price = book.attributes("Price").toDouble
      } yield new Elem(
        name = "Book",
        children = Vector(
          book.allChildElems.find(_.name == "Title").get,
          textElem("Price", price.toString)))
    }

    expect(4) {
      books.size
    }
    expect(List(25, 50, 85, 100)) {
      books flatMap { e => e.findAllElemsOrSelf filter { _.name == "Price" } } map { e => e.text.trim.toDouble.intValue }
    }
    expect(List(
      "Jennifer's Economical Database Hints",
      "Hector and Jeff's Database Hints",
      "A First Course in Database Systems",
      "Database Systems: The Complete Book")) {
      books flatMap { e => e.findAllElemsOrSelf filter { _.name == "Title" } } map { e => e.text.trim }
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
    require(bookstore.name == "Bookstore")

    val lastNameValues: immutable.IndexedSeq[String] =
      for {
        lastName <- (bookstore.findAllElemsOrSelf filter { _.name == "Last_Name" } map (e => e.text.trim)).distinct
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
    require(bookstore.name == "Bookstore")

    def bookAuthorLastNames(book: Elem): Set[String] = {
      val authors = book.allChildElems.find(_.name == "Authors").get
      val result = for {
        author <- authors.allChildElems filter { _.name == "Author" }
        lastName = author.allChildElems.find(_.name == "Last_Name").get
        lastNameValue: String = lastName.text.trim
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: Elem): String = book.allChildElems.find(_.name == "Title").get.text.trim

    val pairs =
      for {
        book1 <- bookstore.allChildElems filter { _.name == "Book" }
        book2 <- bookstore.allChildElems filter { _.name == "Book" }
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield new Elem(
        name = "BookPair",
        children = Vector(
          textElem("Title1", bookTitle(book1)),
          textElem("Title2", bookTitle(book2))))

    expect(5) {
      pairs.size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.allChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book1) ||
          pair.allChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book1)).size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.allChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book2) ||
          pair.allChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book2)).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.allChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book3) ||
          pair.allChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book3)).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.allChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book4) ||
          pair.allChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book4)).size
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
    require(bookstore.name == "Bookstore")

    def books(authorLastName: String) =
      for {
        book <- bookstore.allChildElems filter { _.name == "Book" }
        author <- book.findAllElemsOrSelf filter { _.name == "Author" }
        if author.allChildElems.find(_.name == "Last_Name").get.text.trim == authorLastName
      } yield {
        val attrs = book.attributes filterKeys { a => Set("ISBN", "Price").contains(a) }
        new Elem(
          name = "Book",
          attributes = attrs,
          children = book.allChildElems filter (_.name == "Title"))
      }

    val authorsWithBooks =
      for {
        lastNameValue <- {
          val result = bookstore.findAllElemsOrSelf filter { _.name == "Author" } map { e => e.allChildElems.find(_.name == "Last_Name").get.text.trim }
          result.distinct
        }
      } yield {
        val author: Elem = {
          val result = for {
            author <- bookstore.findAllElemsOrSelf filter { _.name == "Author" }
            if author.allChildElems.find(_.name == "Last_Name").get.text.trim == lastNameValue
          } yield author
          result.head
        }
        val firstNameValue: String = author.allChildElems.find(_.name == "First_Name").get.text.trim

        val foundBooks = books(lastNameValue)

        new Elem(
          name = "Author",
          children = Vector(
            textElem("First_Name", firstNameValue),
            textElem("Last_Name", lastNameValue)) ++ foundBooks)
      }

    val invertedBookstore: Elem = new Elem(name = "InvertedBookstore", children = authorsWithBooks)

    expect(3) {
      invertedBookstore.allChildElems.size
    }
  }

  @Test def testQueryBookAndMagazineTitlesRelabeled() {
    // Taken from the XSLT demo
    require(bookstore.name == "Bookstore")

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore.allChildElems filter { e => Set("Book", "Magazine").contains(e.name) }
      } yield {
        val titleString = bookOrMagazine.allChildElems.find(_.name == "Title").get.text.trim

        if (bookOrMagazine.name == "Book") {
          textElem("BookTitle", titleString)
        } else {
          textElem("MagazineTitle", titleString)
        }
      }

    expect(Set("BookTitle", "MagazineTitle")) {
      bookOrMagazineTitles.map(e => e.name).toSet
    }
    val ngCount = bookOrMagazineTitles count { e => e.text.trim == "National Geographic" }
    assert(ngCount == 2, "Expected 'National Geographic' twice")
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
