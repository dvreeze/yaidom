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

import org.scalatest.funsuite.AnyFunSuite

/**
 * Query test case, using a very naive mini version of yaidom, thus showing the core of yaidom,
 * as a generic element query API (with partial implementation traits), multiple element implementations
 * offering that element query API, and support for multiple XML dialects on top of the general element
 * query API.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material.
 *
 * @author Chris de Vreeze
 */
class CoreOfYaidomTest extends AnyFunSuite {

  import CoreOfYaidomTest._

  test("testQueryBookTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val bookTitles =
      bookstoreIndexedElem filterChildElems {
        _.name == "Book"
      } map { e =>
        val result = e.findAllChildElems find (_.name == "Title")
        result.get
      }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryBookOrMagazineTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstoreIndexedElem.findAllChildElems
        if Set("Book", "Magazine").contains(bookOrMagazine.name)
        title <- bookOrMagazine.findAllChildElems find {
          _.name == "Title"
        }
      } yield title

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val titles =
      for (ch <- bookstoreIndexedElem.findAllChildElems) yield {
        val result = ch.findAllChildElems find {
          _.name == "Title"
        }
        result.get
      }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryAllTitles") {
    // XPath: doc("bookstore.xml")//Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val titles =
      for (title <- bookstoreIndexedElem.findAllElemsOrSelf if title.name == "Title") yield title

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryAllElements") {
    // XPath: doc("bookstore.xml")//*

    require(bookstoreIndexedElem.name == "Bookstore")

    val elements = bookstoreIndexedElem.findAllElemsOrSelf

    assert(elements.contains(bookstoreIndexedElem), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")

    val childrenAlsoIncluded =
      elements forall { e =>
        e.findAllChildElems forall { ch => elements.contains(ch) }
      }
    assert(childrenAlsoIncluded, "Expected child elements of each element also in the result")
  }

  test("testQueryBookIsbns") {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(bookstoreIndexedElem.name == "Bookstore")

    val isbns =
      for (book <- bookstoreIndexedElem.findAllChildElems if book.name == "Book") yield book.attributes("ISBN")

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

    require(bookstoreIndexedElem.name == "Bookstore")

    val books =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        price = book.attributes("Price")
        if price.toInt < 90
      } yield book

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findAllChildElems.find(_.name == "Title") map {
        _.text.trim
      }
      }
      result.toSet
    }
  }

  test("testQueryCheapBookTitles") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val titles =
      for {
        book <- bookstoreIndexedElem.findAllChildElems
        if (book.name == "Book") && (book.attributes("Price").toInt < 90)
        title <- book.findAllChildElems find {
          _.name == "Title"
        }
      } yield title

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryCheapBookAuthors") {
    // Own example..

    require(bookstoreIndexedElem.name == "Bookstore")

    val cheapBookElms =
      for {
        bookElm <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        price = bookElm.attributes("Price")
        if price.toInt < 90
      } yield bookElm

    val cheapBookAuthors = {
      val result =
        for {
          cheapBookElm <- cheapBookElms
          authorElm <- cheapBookElm filterElemsOrSelf {
            _.name == "Author"
          }
        } yield {
          val firstNameElmOption = authorElm.findAllChildElems find {
            _.name == "First_Name"
          }
          val lastNameElmOption = authorElm.findAllChildElems find {
            _.name == "Last_Name"
          }

          val firstName = firstNameElmOption.map(_.text).getOrElse("")
          val lastName = lastNameElmOption.map(_.text).getOrElse("")
          (firstName + " " + lastName).trim
        }

      result.toSet
    }

    assertResult(Set(
      "Jeffrey Ullman",
      "Jennifer Widom",
      "Hector Garcia-Molina")) {
      cheapBookAuthors
    }
  }

  test("testQueryTitlesOfBooksWithRemarks") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val bookTitles =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        if book.findAllChildElems.filter(_.name == "Remark").nonEmpty
      } yield book.findAllChildElems.filter(_.name == "Title").head

    assertResult(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByUllman") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val bookTitles =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        if book.attributes("Price").toInt < 90
        authors = book.findAllChildElems.filter(_.name == "Authors").head
        authorLastName <- authors filterChildElems {
          _.name == "Author"
        } flatMap { e => e filterChildElems (_.name == "Last_Name") } map {
          _.text.trim
        }
        if authorLastName == "Ullman"
      } yield book.findAllChildElems.find(_.name == "Title").get

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByJeffreyUllman") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    def authorLastAndFirstNames(bookElem: ElemApi): immutable.IndexedSeq[(String, String)] = {
      for {
        author <- bookElem.findAllElemsOrSelf
        if author.name == "Author"
      } yield {
        val lastNames = author filterChildElems {
          _.name == "Last_Name"
        } map {
          _.text.trim
        }
        val firstNames = author filterChildElems {
          _.name == "First_Name"
        } map {
          _.text.trim
        }
        (lastNames.mkString, firstNames.mkString)
      }
    }

    val bookTitles =
      for {
        book <- bookstoreIndexedElem.findAllChildElems
        if (book.name == "Book") &&
          (book.attributes("Price").toInt < 90 && authorLastAndFirstNames(book).contains(("Ullman", "Jeffrey")))
      } yield book.findAllChildElems.find(_.name == "Title").get

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryTitlesOfBooksByJeffreyUllmanButNotWidom") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val bookTitles =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        authors <- book.findAllChildElems.find(_.name == "Authors")
        lastNameStrings = for {
          author <- authors filterChildElems {
            _.name == "Author"
          }
          lastNameString = author.findAllChildElems.find(_.name == "Last_Name").get.text.trim
        } yield lastNameString
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.findAllChildElems.find(_.name == "Title").get

    assertResult(Set(
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQuerySecondAuthors") {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstoreIndexedElem.name == "Bookstore")

    val secondAuthors =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        authors <- book.findAllChildElems.find(_.name == "Authors").toSeq
        authorColl = authors filterChildElems {
          _.name == "Author"
        }
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames = secondAuthors map { e => e.findAllChildElems.find(_.name == "Last_Name").get }
    assertResult(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      val result = secondAuthorLastNames map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryGreatBooks") {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    require(bookstoreIndexedElem.name == "Bookstore")

    val titles =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        remark <- book filterChildElems {
          _.name == "Remark"
        }
        if remark.text.trim.indexOf("great") >= 0
      } yield book.findAllChildElems.find(_.name == "Title").get

    assertResult(Set("Database Systems: The Complete Book")) {
      val result = titles map {
        _.text.trim
      }
      result.toSet
    }
  }

  test("testQueryMagazinesWithSameNameAsBook") {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstoreIndexedElem.name == "Bookstore")

    val magazines =
      for {
        magazine <- bookstoreIndexedElem filterChildElems {
          _.name == "Magazine"
        }
        magazineTitle = magazine.findAllChildElems.find(_.name == "Title").get.text.trim
        booksWithSameName = for {
          book <- bookstoreIndexedElem filterChildElems {
            _.name == "Book"
          }
          bookTitle = book.findAllChildElems.find(_.name == "Title").get.text.trim
          if magazineTitle == bookTitle
        } yield book
        if booksWithSameName.nonEmpty
      } yield magazine

    assertResult(Set("Hector and Jeff's Database Hints")) {
      val result = magazines flatMap { mag => mag.findAllElemsOrSelf find (_.name == "Title") map {
        _.text.trim
      }
      }
      result.toSet
    }
  }

  test("testQueryBooksOrMagazinesWithNonUniqueTitles") {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(bookstoreIndexedElem.name == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstoreIndexedElem filterChildElems { e => Set("Book", "Magazine").contains(e.name) }
        titleString: String = bookOrMagazine.findAllChildElems.find(_.name == "Title").get.text.trim
        otherBooksAndMagazines = {
          val result = bookstoreIndexedElem filterChildElems { e => Set("Book", "Magazine").contains(e.name) }
          result.toSet.diff(Set(bookOrMagazine))
        }
        titles = otherBooksAndMagazines map { e => e.findAllChildElems.find(_.name == "Title").get }
        titleStrings = {
          val result = titles map {
            _.text.trim
          }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    assertResult(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = booksAndMagazines flatMap { mag => mag.findAllElemsOrSelf find (_.name == "Title") map {
        _.text.trim
      }
      }
      result.toSet
    }
  }

  test("testQueryBooksOrMagazinesWithTitleAsOtherBook") {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstoreIndexedElem.name == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstoreIndexedElem filterChildElems { e => Set("Book", "Magazine").contains(e.name) }
        titleString: String = bookOrMagazine.findAllChildElems.find(_.name == "Title").get.text.trim
        otherBooks = bookstoreIndexedElem.findAllChildElems.filter(_.name == "Book").toSet.diff(Set(bookOrMagazine))
        titles = otherBooks map { e => e.findAllChildElems.find(_.name == "Title").get }
        titleStrings = {
          val result = titles map {
            _.text.trim
          }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    assertResult(Set("Hector and Jeff's Database Hints")) {
      val result = booksAndMagazines flatMap { mag => mag.findAllElemsOrSelf find (_.name == "Title") map {
        _.text.trim
      }
      }
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
  test("testQueryBooksWithAllAuthorFirstNamesWithLetterJ") {
    require(bookstoreIndexedElem.name == "Bookstore")

    val books =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        authorNames = {
          val result = for {
            author <- book filterElemsOrSelf (_.name == "Author")
            firstName = author.findAllChildElems.find(_.name == "First_Name").get
          } yield firstName.text.trim
          result.toSet
        }
        if authorNames forall { name => name.indexOf("J") >= 0 }
      } yield book

    assertResult(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findAllElemsOrSelf.find(_.name == "Title") map {
        _.text.trim
      }
      }
      result.toSet
    }
  }

  test("testQueryBooksFromUllmanButNotWidom") {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(bookstoreIndexedElem.name == "Bookstore")

    def findAuthorNames(bookElem: ElemApi): immutable.IndexedSeq[String] = {
      for {
        author <- bookElem.findAllElemsOrSelf
        if author.name == "Author"
        lastName <- author.findAllChildElems
        if lastName.name == "Last_Name"
      } yield lastName.text.trim
    }

    val titles =
      for {
        book <- bookstoreIndexedElem.findAllChildElems
        if book.name == "Book"
        authorNames = findAuthorNames(book)
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.findAllChildElems.find(_.name == "Title").get

    assertResult(Set(
      "Hector and Jeff's Database Hints")) {
      val result = titles map {
        _.text.trim
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
    // Starting with the "normal" bookstore element
    require(bookstore.name == "Bookstore")

    val titleAndFirstNames =
      for {
        book <- bookstore filterChildElems {
          _.name == "Book"
        }
        title = book.findAllChildElems.find(_.name == "Title").get
        authorFirstNames = {
          val result = book filterElemsOrSelf {
            _.name == "Author"
          } map {
            _.findAllChildElems.find(_.name == "First_Name").get.text.trim
          }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames filter { firstName => title.text.trim.indexOf(firstName) >= 0 }
        if searchedForFirstNames.nonEmpty
      } yield new Elem(
        name = "Book",
        children = Vector(
          title,
          textElem("First_Name", searchedForFirstNames.head)))

    assertResult(2) {
      titleAndFirstNames.size
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val titleElms = titleAndFirstNames map { e => e filterElemsOrSelf {
        _.name == "Title"
      }
      }
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
  test("testQueryAverageBookPrice") {
    require(bookstoreIndexedElem.name == "Bookstore")

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        price = book.attributes("Price").toDouble
      } yield price
    val averagePrice =
      textElem("Average", (prices.sum.toDouble / prices.size).toString)

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
    // Starting with the "normal" bookstore element
    require(bookstore.name == "Bookstore")

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore filterChildElems {
          _.name == "Book"
        }
        price = book.attributes("Price").toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- bookstore filterChildElems {
          _.name == "Book"
        }
        price = book.attributes("Price").toDouble
        if price < avg
      } yield new Elem(
        name = "Book",
        children = Vector(
          book.findAllChildElems.find(_.name == "Title").get,
          textElem("Price", price.toString)))

    assertResult(2) {
      cheapBooks.size
    }
    assertResult(Set(50, 25)) {
      val result = cheapBooks flatMap { e => e filterElemsOrSelf {
        _.name == "Price"
      }
      } map { e => e.text.trim.toDouble.intValue }
      result.toSet
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val result = cheapBooks flatMap { e => e filterElemsOrSelf {
        _.name == "Title"
      }
      } map { e => e.text.trim }
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
    // Starting with the "normal" bookstore element
    require(bookstore.name == "Bookstore")

    def cheaper(book1: ElemApi, book2: ElemApi): Boolean = {
      val price1 = book1.attributes("Price").toInt
      val price2 = book2.attributes("Price").toInt
      price1 < price2
    }

    val books = {
      for {
        book <- bookstore filterChildElems {
          _.name == "Book"
        } sortWith {
          cheaper _
        }
        price = book.attributes("Price").toDouble
      } yield new Elem(
        name = "Book",
        children = Vector(
          book.findAllChildElems.find(_.name == "Title").get,
          textElem("Price", price.toString)))
    }

    assertResult(4) {
      books.size
    }
    assertResult(List(25, 50, 85, 100)) {
      books flatMap { e => e filterElemsOrSelf {
        _.name == "Price"
      }
      } map { e => e.text.trim.toDouble.intValue }
    }
    assertResult(List(
      "Jennifer's Economical Database Hints",
      "Hector and Jeff's Database Hints",
      "A First Course in Database Systems",
      "Database Systems: The Complete Book")) {
      books flatMap { e => e filterElemsOrSelf {
        _.name == "Title"
      }
      } map { e => e.text.trim }
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
    require(bookstoreIndexedElem.name == "Bookstore")

    val lastNameValues: immutable.IndexedSeq[String] =
      for {
        lastName <- (bookstoreIndexedElem filterElemsOrSelf {
          _.name == "Last_Name"
        } map (e => e.text.trim)).distinct
      } yield lastName

    assertResult(Set(
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
  test("testQueryBookPairsFromSameAuthor") {
    require(bookstoreIndexedElem.name == "Bookstore")

    def bookAuthorLastNames(book: ElemApi): Set[String] = {
      require(book.name == "Book", s"Expected 'Book' element but found ${book.name}")
      val authors = book.findAllChildElems.find(_.name == "Authors").get
      val result = for {
        author <- authors filterChildElems {
          _.name == "Author"
        }
        lastName = author.findAllChildElems.find(_.name == "Last_Name").get
        lastNameValue: String = lastName.text.trim
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: ElemApi): String = book.findAllChildElems.find(_.name == "Title").get.text.trim

    val pairs =
      for {
        book1 <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        book2 <- bookstoreIndexedElem filterChildElems {
          _.name == "Book"
        }
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield new Elem(
        name = "BookPair",
        children = Vector(
          textElem("Title1", bookTitle(book1)),
          textElem("Title2", bookTitle(book2))))

    assertResult(5) {
      pairs.size
    }
    assertResult(3) {
      pairs.filter(pair =>
        pair.findAllChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book1) ||
          pair.findAllChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book1)).size
    }
    assertResult(3) {
      pairs.filter(pair =>
        pair.findAllChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book2) ||
          pair.findAllChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book2)).size
    }
    assertResult(2) {
      pairs.filter(pair =>
        pair.findAllChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book3) ||
          pair.findAllChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book3)).size
    }
    assertResult(2) {
      pairs.filter(pair =>
        pair.findAllChildElems.filter(_.name == "Title1").head.text.trim == bookTitle(book4) ||
          pair.findAllChildElems.filter(_.name == "Title2").head.text.trim == bookTitle(book4)).size
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
    // Starting with the "normal" bookstore element
    require(bookstore.name == "Bookstore")

    def books(authorLastName: String) =
      for {
        book <- bookstore filterChildElems {
          _.name == "Book"
        }
        author <- book filterElemsOrSelf {
          _.name == "Author"
        }
        if author.findAllChildElems.find(_.name == "Last_Name").get.text.trim == authorLastName
      } yield {
        // Method filterKeys deprecated since Scala 2.13.0.
        val attrs = book.attributes.filter { case (a, _) => Set("ISBN", "Price").contains(a) }.toMap
        new Elem(
          name = "Book",
          attributes = attrs,
          children = book filterChildElems (_.name == "Title"))
      }

    val authorsWithBooks =
      for {
        lastNameValue <- {
          val result = bookstore filterElemsOrSelf {
            _.name == "Author"
          } map { e => e.findAllChildElems.find(_.name == "Last_Name").get.text.trim }
          result.distinct
        }
      } yield {
        val author: Elem = {
          val result = for {
            author <- bookstore filterElemsOrSelf {
              _.name == "Author"
            }
            if author.findAllChildElems.find(_.name == "Last_Name").get.text.trim == lastNameValue
          } yield author
          result.head
        }
        val firstNameValue: String = author.findAllChildElems.find(_.name == "First_Name").get.text.trim

        val foundBooks = books(lastNameValue)

        new Elem(
          name = "Author",
          children = Vector(
            textElem("First_Name", firstNameValue),
            textElem("Last_Name", lastNameValue)) ++ foundBooks)
      }

    val invertedBookstore: Elem = new Elem(name = "InvertedBookstore", children = authorsWithBooks)

    assertResult(3) {
      invertedBookstore.findAllChildElems.size
    }
  }

  test("testQueryBookAndMagazineTitlesRelabeled") {
    // Taken from the XSLT demo
    require(bookstoreIndexedElem.name == "Bookstore")

    // Below, had we written bookstoreIndexedElem.thisElem instead of just bookstoreIndexedElem,
    // the Scala 2.13.0-M3 compiler would not have compiled this code successfully. The Scala 2.12.X
    // and 2.11.X compilers have no issue with that code, so it seems that there is a regression in
    // Scala 2.13.0-M3 (not even in version 2.13.0-M2).

    // Had we stored bookstoreIndexedElem.thisElem in a variable first, and then used that variable,
    // the compiler error for Scala 2.13.0-M3 would disappear again.

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstoreIndexedElem filterChildElems { e => Set("Book", "Magazine").contains(e.name) }
      } yield {
        val titleString = bookOrMagazine.findAllChildElems.find(_.name == "Title").get.text.trim

        if (bookOrMagazine.name == "Book") {
          textElem("BookTitle", titleString)
        } else {
          textElem("MagazineTitle", titleString)
        }
      }

    assertResult(Set("BookTitle", "MagazineTitle")) {
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

  // Using the purely abstract element query API in all queries!!

  private val bookstoreIndexedElem: BackingElemApi = {
    new IndexedElem(bookstore)
  }

  // TODO Bookstore XML dialect, taking BackingElemApi as (abstract) backing element
}

object CoreOfYaidomTest {

  // 1. Query API consisting of some purely abstract XXXApi traits and their XXXLike partial
  // implementations (that may be mixed in by concrete element implementations).
  // Concrete element implementations mix in some of these query API traits, but at least the ElemApi one.
  // That is the idea: many element implementations can be created that offer (part of) the same element query API.

  trait AnyElemApi {

    // The type member below is used for implementing F-bounded polymorphism.
    // Note that we need no surrounding cake, and we need no types like ThisApi#ThisElem.

    type ThisElem <: AnyElemApi

    def thisElem: ThisElem

    def name: String

    def attributes: Map[String, String]

    def text: String
  }

  trait ElemApi extends AnyElemApi {

    type ThisElem <: ElemApi

    def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

    def findAllChildElems: immutable.IndexedSeq[ThisElem]

    def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

    def findAllElemsOrSelf: immutable.IndexedSeq[ThisElem]
  }

  trait ElemLike extends ElemApi {
    self =>

    type ThisElem <: ElemLike {type ThisElem = self.ThisElem}

    final def findAllChildElems: immutable.IndexedSeq[ThisElem] = {
      filterChildElems(_ => true)
    }

    // Note that ThisElem is used both in the filter functions and in the return type

    final def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
      // Recursion
      Vector(thisElem).filter(p) ++ findAllChildElems.flatMap(e => e.filterElemsOrSelf(p))
    }

    final def findAllElemsOrSelf: immutable.IndexedSeq[ThisElem] = {
      filterElemsOrSelf(_ => true)
    }
  }

  trait HasParentApi extends AnyElemApi {

    type ThisElem <: HasParentApi

    def parentOption: Option[ThisElem]

    def findAncestor(p: ThisElem => Boolean): Option[ThisElem]

    def findAncestorOrSelf(p: ThisElem => Boolean): Option[ThisElem]
  }

  trait HasParent extends HasParentApi {
    self =>

    type ThisElem <: HasParent {type ThisElem = self.ThisElem}

    final def findAncestor(p: ThisElem => Boolean): Option[ThisElem] = {
      parentOption.flatMap(_.findAncestorOrSelf(p))
    }

    final def findAncestorOrSelf(p: ThisElem => Boolean): Option[ThisElem] = {
      if (p(thisElem)) {
        Some(thisElem)
      } else {
        // Recursion
        parentOption.flatMap(_.findAncestorOrSelf(p))
      }
    }
  }

  // Purely abstract rich query API trait

  trait BackingElemApi extends ElemApi with HasParentApi {

    type ThisElem <: BackingElemApi
  }

  // 2. Concrete element implementation (one of many), mixing in part of the query API.

  /**
   * Naive node trait, with only subclasses for elements and text nodes.
   */
  sealed trait Node

  /**
   * Naive element class, which for example is not namespace-aware.
   *
   * This class shows how methods findAllChildElems and findAllElemsOrSelf, along with the Scala
   * Collections API, already provide a pretty powerful XML querying API.
   */
  final class Elem(
    val name: String,
    val attributes: Map[String, String],
    val children: immutable.IndexedSeq[Node]) extends Node with ElemLike {

    type ThisElem = Elem

    def this(name: String, children: immutable.IndexedSeq[Node]) =
      this(name, Map(), children)

    def thisElem: Elem = this

    def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[Elem] =
      children collect { case e: Elem if p(e) => e }

    def text: String = {
      val textStrings = children collect { case t: Text => t.text }
      textStrings.mkString
    }

    // Needed below

    def findElemOrSelfByNavigationPath(path: NavigationPath): Option[Elem] = {
      if (path.isEmpty) {
        Some(thisElem)
      } else {
        val childElemOption = findAllChildElems.drop(path.head).headOption

        // Recursion
        childElemOption.flatMap(che => che.findElemOrSelfByNavigationPath(path.tail))
      }
    }
  }

  /**
   * Naive text node class.
   */
  final case class Text(val text: String) extends Node

  // Navigation path, used to implement the next element implementation

  final case class NavigationPath(childElemIndices: List[Int]) {

    def isEmpty = childElemIndices.isEmpty

    def head: Int = childElemIndices.head

    def tail: NavigationPath = NavigationPath(childElemIndices.tail)

    def init: NavigationPath = NavigationPath(childElemIndices.init)

    def append(chi: Int): NavigationPath = NavigationPath(childElemIndices ::: List(chi))
  }

  // Next concrete element implementation, implementing all of API trait BackingElemApi

  /**
   * Naive (indexed) node trait, with only subclasses for elements and text nodes.
   */
  sealed trait IndexedNode

  /**
   * Naive (indexed) element class, which for example is not namespace-aware.
   *
   * This class shows how methods findAllChildElems and findAllElemsOrSelf, along with the Scala
   * Collections API, already provide a pretty powerful XML querying API.
   */
  final case class IndexedElem(
    val rootElem: Elem,
    val ownRelativeNavigationPath: NavigationPath) extends IndexedNode with BackingElemApi with ElemLike with HasParent {

    type ThisElem = IndexedElem

    def this(rootElem: Elem) = this(rootElem, NavigationPath(Nil))

    def thisElem: IndexedElem = this

    def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[IndexedElem] = {
      val allChildElems = underlyingElem.findAllChildElems.zipWithIndex map {
        case (_, chi) =>
          IndexedElem(rootElem, ownRelativeNavigationPath.append(chi))
      }

      allChildElems.filter(p)
    }

    def parentOption: Option[IndexedElem] = {
      if (ownRelativeNavigationPath.isEmpty) None else Some(IndexedElem(rootElem, ownRelativeNavigationPath.init))
    }

    def text: String = {
      val textStrings = underlyingElem.children collect { case t: Text => t.text }
      textStrings.mkString
    }

    def underlyingElem: Elem = {
      rootElem.findElemOrSelfByNavigationPath(ownRelativeNavigationPath).get
    }

    def name: String = underlyingElem.name

    def attributes: Map[String, String] = underlyingElem.attributes
  }

  /**
   * Naive (indexed) text node class.
   */
  final case class IndexedText(val text: String) extends IndexedNode

}
