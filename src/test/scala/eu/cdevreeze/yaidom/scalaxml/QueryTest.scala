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
package scalaxml

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import ElemFunctions._

/**
 * Query test case. This test case is much like the "original" QueryTest, yet taking Scala XML wrapper elements instead of standard yaidom
 * Elems.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class QueryTest extends Suite {

  @Test def testQueryAll() {
    require(bookstore.localName == "Bookstore")

    val elems = bookstore.findAllElemsOrSelf

    expectResult(true) {
      !elems.isEmpty
    }
  }

  @Test def testQueryBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      (bookstore \ havingLocalName("Book")) map { e => e getChildElem (_.localName == "Title") }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBookOrMagazineTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.localName == "Bookstore")

    // Using only the ParentElemLike API (except for method localName)...

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore filterChildElems { e => Set("Book", "Magazine").contains(e.localName) }
        title <- bookOrMagazine findChildElem { _.localName == "Title" }
      } yield title

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for (ch <- bookstore.findAllChildElems) yield ch.getChildElem(EName("Title"))

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryAllTitles() {
    // XPath: doc("bookstore.xml")//Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for (title <- bookstore filterElems (_.localName == "Title")) yield title

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryAllElements() {
    // XPath: doc("bookstore.xml")//*

    require(bookstore.localName == "Bookstore")

    val elements = bookstore.findAllElemsOrSelf

    assert(elements.contains(bookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")

    val childrenAlsoIncluded =
      elements forall { e =>
        e.findAllChildElems forall { ch => elements.map(_.wrappedNode).contains(ch.wrappedNode) }
      }
    assert(childrenAlsoIncluded, "Expected child elements of each element also in the result")
  }

  @Test def testQueryBookIsbns() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(bookstore.localName == "Bookstore")

    // Using only the ParentElemLike API (except for method localName)...

    val isbns =
      for (book <- bookstore filterChildElems havingLocalName("Book")) yield book.attribute(EName("ISBN"))

    expectResult(Set(
      "ISBN-0-13-713526-2",
      "ISBN-0-13-815504-6",
      "ISBN-0-11-222222-3",
      "ISBN-9-88-777777-6")) {
      isbns.toSet
    }
  }

  @Test def testQueryCheapBooks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ havingLocalName("Book")
        price <- book \@ EName("Price")
        if price.toInt < 90
      } yield book

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryCheapBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ havingLocalName("Book")
        price <- book \@ EName("Price")
        if price.toInt < 90
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }

    // Now the verbose way, using the ParentElemLike API and no operator notation...

    val titles2 =
      for {
        book <- bookstore filterChildElems { _.localName == "Book" }
        price <- book.attributeOption(EName("Price"))
        if price.toInt < 90
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles2 map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryCheapBookAuthors() {
    // Own example..

    require(bookstore.localName == "Bookstore")

    val cheapBookElms =
      for {
        bookElm <- bookstore \ havingLocalName("Book")
        price <- bookElm \@ EName("Price")
        if price.toInt < 90
      } yield bookElm

    val cheapBookAuthors = {
      val result =
        for {
          cheapBookElm <- cheapBookElms
          authorElm <- cheapBookElm \\ (_.localName == "Author")
        } yield {
          val firstNameElmOption = authorElm findChildElem { _.localName == "First_Name" }
          val lastNameElmOption = authorElm findChildElem { _.localName == "Last_Name" }

          val firstName = firstNameElmOption.map(_.text).getOrElse("")
          val lastName = lastNameElmOption.map(_.text).getOrElse("")
          (firstName + " " + lastName).trim
        }

      result.toSet
    }

    expectResult(Set(
      "Jeffrey Ullman",
      "Jennifer Widom",
      "Hector Garcia-Molina")) {
      cheapBookAuthors
    }
  }

  @Test def testQueryTitlesOfBooksWithRemarks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ havingLocalName("Book")
        if !book.filterChildElems(EName("Remark")).isEmpty
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ havingLocalName("Book")
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ (_.localName == "Last_Name") } map { _.trimmedText }
        if authorLastName == "Ullman"
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ havingLocalName("Book")
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ (_.localName == "Last_Name") } map { _.trimmedText }
        if authorLastName == "Ullman"
        authorFirstName <- authors \ { _.localName == "Author" } flatMap { e => e \ (_.localName == "First_Name") } map { _.trimmedText }
        if authorFirstName == "Jeffrey"
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfBooksByJeffreyUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ havingLocalName("Book")
        authors = book.getChildElem(EName("Authors"))
        lastNameStrings = for {
          author <- authors \ (_.localName == "Author")
          lastNameString = author.getChildElem(EName("Last_Name")).trimmedText
        } yield lastNameString
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBooksByJeffreyUllman() {
    // Own example

    require(bookstore.localName == "Bookstore")

    val bookElms =
      for {
        bookElm <- bookstore filterChildElems { _.localName == "Book" }
        if (bookElm \\ (_.localName == "Author")) exists { e =>
          ((e.getChildElem(_.localName == "First_Name")).text == "Jeffrey") &&
            ((e.getChildElem(_.localName == "Last_Name")).text == "Ullman")
        }
      } yield bookElm

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookElms map { e => e.getChildElem(_.localName == "Title").text }
      result.toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstore.localName == "Bookstore")

    val secondAuthors =
      for {
        book <- bookstore \ havingLocalName("Book")
        authors = book.getChildElem(EName("Authors"))
        authorColl = authors \ (_.localName == "Author")
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames = secondAuthors map { e => e getChildElem { _.localName == "Last_Name" } }
    expectResult(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      val result = secondAuthorLastNames map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryGreatBooks() {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ havingLocalName("Book")
        remark <- book \ (_.localName == "Remark")
        if remark.trimmedText.indexOf("great") >= 0
      } yield book.getChildElem(EName("Title"))

    expectResult(Set("Database Systems: The Complete Book")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook() {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.localName == "Bookstore")

    val magazines =
      for {
        magazine <- bookstore \ (_.localName == "Magazine")
        magazineTitle = magazine.getChildElem(EName("Title")).trimmedText
        booksWithSameName = for {
          book <- bookstore \ havingLocalName("Book")
          bookTitle = book.getChildElem(EName("Title")).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    expectResult(Set("Hector and Jeff's Database Hints")) {
      val result = magazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithNonUniqueTitles() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(bookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        titleString: String = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        otherBooksAndMagazines = {
          val result = bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
          result filterNot { e => e.wrappedNode == bookOrMagazine.wrappedNode }
        }
        titles = otherBooksAndMagazines map { e => e getChildElem { _.localName == "Title" } }
        if titles.map(_.trimmedText).contains(titleString)
      } yield bookOrMagazine

    expectResult(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = booksAndMagazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBook() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        titleString: String = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        otherBooks = bookstore.filterChildElems(EName("Book")) filterNot { e => e.wrappedNode == bookOrMagazine.wrappedNode }
        titles = otherBooks map { e => e getChildElem { _.localName == "Title" } }
        if titles.map(_.trimmedText).contains(titleString)
      } yield bookOrMagazine

    expectResult(Set("Hector and Jeff's Database Hints")) {
      val result = booksAndMagazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
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
    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ havingLocalName("Book")
        authorNames = {
          val result = for {
            author <- book.filterElems(EName("Author"))
            firstName = author.getChildElem(EName("First_Name"))
          } yield firstName.trimmedText
          result.toSet
        }
        if authorNames forall { name => name.indexOf("J") >= 0 }
      } yield book

    expectResult(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksFromUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ havingLocalName("Book")
        authorNames = {
          val result = book.filterElems(EName("Author")) map { _.getChildElem(EName("Last_Name")).trimmedText }
          result.toSet
        }
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "Hector and Jeff's Database Hints")) {
      val result = titles map { _.trimmedText }
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val titleAndFirstNames =
      for {
        book <- bookstore \ havingLocalName("Book")
        title = book.getChildElem(EName("Title"))
        authorFirstNames = {
          val result = book.filterElems(EName("Author")) map { _.getChildElem(EName("First_Name")).trimmedText }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames filter { firstName => title.trimmedText.indexOf(firstName) >= 0 }
        if !searchedForFirstNames.isEmpty
      } yield {
        val titleElem = convert.ScalaXmlConversions.convertToElem(title.wrappedNode)

        elem(
          qname = QName("Book"),
          children = Vector(
            fromElem(titleElem)(Scope.Empty),
            textElem(QName("First_Name"), searchedForFirstNames.head))).build()
      }

    expectResult(2) {
      titleAndFirstNames.size
    }
    expectResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val titleElms = titleAndFirstNames map { e => e.filterElems(EName("Title")) }
      val result = titleElms.flatten map { e => e.trimmedText }
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore \ havingLocalName("Book")
        price <- book \@ EName("Price")
      } yield price.toDouble
    val averagePrice =
      textElem(QName("Average"), (prices.sum.toDouble / prices.size).toString).build()

    expectResult(65) {
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore \ havingLocalName("Book")
        price = book.attribute(EName("Price")).toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- bookstore \ havingLocalName("Book")
        price = book.attribute(EName("Price")).toDouble
        if price < avg
      } yield {
        val title = book.getChildElem(EName("Title"))
        val titleElem = convert.ScalaXmlConversions.convertToElem(title.wrappedNode)

        elem(
          qname = QName("Book"),
          children = Vector(
            fromElem(titleElem)(Scope.Empty),
            textElem(QName("Price"), price.toString))).build()
      }

    expectResult(2) {
      cheapBooks.size
    }
    expectResult(Set(50, 25)) {
      val result = cheapBooks flatMap { e => e.filterElems(EName("Price")) } map { e => e.trimmedText.toDouble.intValue }
      result.toSet
    }
    expectResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val result = cheapBooks flatMap { e => e.filterElems(EName("Title")) } map { e => e.trimmedText }
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def cheaper(book1: ScalaXmlElem, book2: ScalaXmlElem): Boolean = {
      val price1 = book1.attribute(EName("Price")).toInt
      val price2 = book2.attribute(EName("Price")).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- bookstore \ havingLocalName("Book") sortWith { cheaper _ }
        price = book.attribute(EName("Price")).toDouble
      } yield {
        val title = book.getChildElem(EName("Title"))
        val titleElem = convert.ScalaXmlConversions.convertToElem(title.wrappedNode)

        elem(
          qname = QName("Book"),
          children = Vector(
            fromElem(titleElem)(Scope.Empty),
            textElem(QName("Price"), price.toString))).build()
      }
    }

    expectResult(4) {
      books.size
    }
    expectResult(List(25, 50, 85, 100)) {
      books flatMap { e => e.filterElems(EName("Price")) } map { e => e.trimmedText.toDouble.intValue }
    }
    expectResult(List(
      "Jennifer's Economical Database Hints",
      "Hector and Jeff's Database Hints",
      "A First Course in Database Systems",
      "Database Systems: The Complete Book")) {
      books flatMap { e => e.filterElems(EName("Title")) } map { e => e.trimmedText }
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
    require(bookstore.localName == "Bookstore")

    val lastNameValues: immutable.IndexedSeq[String] =
      for {
        lastName <- (bookstore.filterElems(EName("Last_Name")) map (e => e.trimmedText)).distinct
      } yield lastName

    expectResult(Set(
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def bookAuthorLastNames(book: ScalaXmlElem): Set[String] = {
      val authors = book.getChildElem(EName("Authors"))
      val result = for {
        author <- authors \ (_.localName == "Author")
        lastName = author getChildElem { _.localName == "Last_Name" }
        lastNameValue: String = lastName.trimmedText
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: ScalaXmlElem): String = book.getChildElem(EName("Title")).trimmedText

    val pairs =
      for {
        book1 <- bookstore \ havingLocalName("Book")
        book2 <- bookstore \ havingLocalName("Book")
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield elem(
        qname = QName("BookPair"),
        children = Vector(
          textElem(QName("Title1"), bookTitle(book1)),
          textElem(QName("Title2"), bookTitle(book2)))).build()

    val books = bookstore \ havingLocalName("Book")
    val book1 = books(0)
    val book2 = books(1)
    val book3 = books(2)
    val book4 = books(3)

    expectResult(5) {
      pairs.size
    }
    expectResult(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book1) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book1)).size
    }
    expectResult(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book2) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book2)).size
    }
    expectResult(2) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book3) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book3)).size
    }
    expectResult(2) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book4) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book4)).size
    }
  }

  @Test def testQueryBookAndMagazineTitlesRelabeled() {
    // Taken from the XSLT demo
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
      } yield {
        val titleString = bookOrMagazine.getChildElem(EName("Title")).trimmedText

        if (bookOrMagazine.resolvedName == EName("Book")) {
          textElem(QName("BookTitle"), titleString).build()
        } else {
          textElem(QName("MagazineTitle"), titleString).build()
        }
      }

    expectResult(Set(EName("BookTitle"), EName("MagazineTitle"))) {
      bookOrMagazineTitles.map(e => e.resolvedName).toSet
    }
    val ngCount = bookOrMagazineTitles count { e => e.trimmedText == "National Geographic" }
    assert(ngCount == 2, "Expected 'National Geographic' twice")
  }

  @Test def testDepthFirst() {
    require(bookstore.localName == "Bookstore")

    // Returns descendant-or-self elements in depth-first order, that is, in document order
    val elms = bookstore.findAllElemsOrSelf

    val depthFirstElmNames = List(
      EName("Bookstore"),
      EName("Book"),
      EName("Title"),
      EName("Authors"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Book"),
      EName("Title"),
      EName("Authors"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Remark"),
      EName("Book"),
      EName("Title"),
      EName("Authors"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Remark"),
      EName("Book"),
      EName("Title"),
      EName("Authors"),
      EName("Author"),
      EName("First_Name"),
      EName("Last_Name"),
      EName("Magazine"),
      EName("Title"),
      EName("Magazine"),
      EName("Title"),
      EName("Magazine"),
      EName("Title"),
      EName("Magazine"),
      EName("Title"))

    expectResult(depthFirstElmNames) {
      elms map { _.resolvedName }
    }
  }

  @Test def testQueryBookWithGivenIsbn() {
    // See http://kousenit.wordpress.com/2008/03/12/nothing-makes-you-want-groovy-more-than-xml/,
    // but taking "our" bookstore as input XML. Somewhat more verbose than the Groovy example, but also more
    // explicit (about elements, expanded names, etc.).

    require(bookstore.localName == "Bookstore")

    val isbn = "ISBN-0-11-222222-3"

    val bookElmOption = bookstore findElem { e => e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some(isbn) }
    val bookElm = bookElmOption.getOrElse(sys.error("Expected Book with ISBN %s".format(isbn)))

    val title = bookElm.getChildElem(_.localName == "Title").text

    val authorLastNames =
      for {
        authorsElm <- bookElm \ (_.localName == "Authors")
        lastNameElm <- authorsElm \\ (_.localName == "Last_Name")
      } yield lastNameElm.text
    val firstAuthorLastName = authorLastNames.head

    expectResult("Hector and Jeff's Database Hints") {
      title
    }
    expectResult("Ullman") {
      firstAuthorLastName
    }
  }

  private val bookstore: ScalaXmlElem = {
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
}
