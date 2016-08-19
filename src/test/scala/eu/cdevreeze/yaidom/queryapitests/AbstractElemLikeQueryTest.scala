/*
 * Copyright 2011-2014 Chris de Vreeze
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

import scala.Vector
import scala.collection.immutable

import org.junit.Test
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName

/**
 * ElemLike-based query test case. This test case shows how XPath and XQuery queries can be written in this API, be it somewhat
 * verbosely. More than anything else, it demonstrates some of the expressive power of Scala's excellent Collections API.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractElemLikeQueryTest extends Suite {

  type E <: ClarkElemLike.Aux[E]

  @Test def testQueryBookTitles(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      (bookstore \ (_.localName == "Book")) map { e => e getChildElem (_.localName == "Title") }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBookTitlesUsingQNames(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(bookstore.localName == "Bookstore")

    val scope = Scope.Empty

    val bookTitles = {
      import scope._

      (bookstore \ (QName("Book").res)) map { e => e getChildElem (QName("Title").res) }
    }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBookTitlesAgain(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title
    // This time using the HasENameApi companion object

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      (bookstore \ withLocalName("Book")) map { e => e getChildElem (withLocalName("Title")) }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBookOrMagazineTitles(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.localName == "Bookstore")

    // Using only the ElemLike API (except for method localName)...

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore filterChildElems { e => Set("Book", "Magazine").contains(e.localName) }
        title <- bookOrMagazine findChildElem { _.localName == "Title" }
      } yield title

    assertResult(Set(
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

  @Test def testQueryTitles(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for (ch <- bookstore.findAllChildElems) yield ch.getChildElem(EName("Title"))

    assertResult(Set(
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

  @Test def testQueryAllTitles(): Unit = {
    // XPath: doc("bookstore.xml")//Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for (title <- bookstore filterElems (_.localName == "Title")) yield title

    assertResult(Set(
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

  @Test def testQueryAllElements(): Unit = {
    // XPath: doc("bookstore.xml")//*

    require(bookstore.localName == "Bookstore")

    val elements = bookstore.findAllElemsOrSelf

    assert(elements.contains(bookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")

    val childrenAlsoIncluded =
      elements forall { e =>
        e.findAllChildElems forall { ch => elements.map(che => toResolvedElem(che)).contains(toResolvedElem(ch)) }
      }
    assert(childrenAlsoIncluded, "Expected child elements of each element also in the result")
  }

  @Test def testQueryBookIsbns(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(bookstore.localName == "Bookstore")

    // Using only the ElemLike API (except for method localName)...

    val isbns =
      for (book <- bookstore filterChildElems (_.localName == "Book")) yield book.attribute(EName("ISBN"))

    assertResult(Set(
      "ISBN-0-13-713526-2",
      "ISBN-0-13-815504-6",
      "ISBN-0-11-222222-3",
      "ISBN-9-88-777777-6")) {
      isbns.toSet
    }
  }

  @Test def testQueryCheapBooks(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ (_.localName == "Book")
        price <- book \@ EName("Price")
        if price.toInt < 90
      } yield book

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryCheapBooksUsingQNames(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

    require(bookstore.localName == "Bookstore")

    val scope = Scope.Empty

    val books = {
      import scope._

      for {
        book <- bookstore \ (QName("Book").res)
        price <- book \@ (QName("Price").res)
        if price.toInt < 90
      } yield book
    }

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryCheapBookTitles(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ (_.localName == "Book")
        price <- book \@ EName("Price")
        if price.toInt < 90
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }

    // Now the verbose way, using the ElemLike API and no operator notation...

    val titles2 =
      for {
        book <- bookstore filterChildElems { _.localName == "Book" }
        price <- book.attributeOption(EName("Price"))
        if price.toInt < 90
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles2 map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryCheapBookAuthors(): Unit = {
    // Own example..

    require(bookstore.localName == "Bookstore")

    val cheapBookElms =
      for {
        bookElm <- bookstore \ (_.localName == "Book")
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

    assertResult(Set(
      "Jeffrey Ullman",
      "Jennifer Widom",
      "Hector Garcia-Molina")) {
      cheapBookAuthors
    }
  }

  @Test def testQueryCheapBookAuthorsAgain(): Unit = {
    // Own example..
    // This time using the HasENameApi companion object

    require(bookstore.localName == "Bookstore")

    val cheapBookElms =
      for {
        bookElm <- bookstore \ withLocalName("Book")
        price <- bookElm \@ EName("Price")
        if price.toInt < 90
      } yield bookElm

    val cheapBookAuthors = {
      val result =
        for {
          cheapBookElm <- cheapBookElms
          authorElm <- cheapBookElm \\ withLocalName("Author")
        } yield {
          val firstNameElmOption = authorElm.findChildElem(withLocalName("First_Name"))
          val lastNameElmOption = authorElm.findChildElem(withLocalName("Last_Name"))

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

  @Test def testQueryTitlesOfBooksWithRemarks(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ (_.localName == "Book")
        if !book.filterChildElems(EName("Remark")).isEmpty
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllman(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ (_.localName == "Book")
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ (_.localName == "Last_Name") } map { _.trimmedText }
        if authorLastName == "Ullman"
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ (_.localName == "Book")
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ (_.localName == "Last_Name") } map { _.trimmedText }
        if authorLastName == "Ullman"
        authorFirstName <- authors \ { _.localName == "Author" } flatMap { e => e \ (_.localName == "First_Name") } map { _.trimmedText }
        if authorFirstName == "Jeffrey"
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfBooksByJeffreyUllmanButNotWidom(): Unit = {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ (_.localName == "Book")
        authors = book.getChildElem(EName("Authors"))
        lastNameStrings = for {
          author <- authors \ (_.localName == "Author")
          lastNameString = author.getChildElem(EName("Last_Name")).trimmedText
        } yield lastNameString
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBooksByJeffreyUllman(): Unit = {
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

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookElms map { e => e.getChildElem(_.localName == "Title").text }
      result.toSet
    }
  }

  @Test def testQuerySecondAuthors(): Unit = {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstore.localName == "Bookstore")

    val secondAuthors =
      for {
        book <- bookstore \ (_.localName == "Book")
        authors = book.getChildElem(EName("Authors"))
        authorColl = authors \ (_.localName == "Author")
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames = secondAuthors map { e => e getChildElem { _.localName == "Last_Name" } }
    assertResult(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      val result = secondAuthorLastNames map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryGreatBooks(): Unit = {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ (_.localName == "Book")
        remark <- book \ (_.localName == "Remark")
        if remark.trimmedText.indexOf("great") >= 0
      } yield book.getChildElem(EName("Title"))

    assertResult(Set("Database Systems: The Complete Book")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook(): Unit = {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.localName == "Bookstore")

    val magazines =
      for {
        magazine <- bookstore \ (_.localName == "Magazine")
        magazineTitle = magazine.getChildElem(EName("Title")).trimmedText
        booksWithSameName = for {
          book <- bookstore \ (_.localName == "Book")
          bookTitle = book.getChildElem(EName("Title")).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    assertResult(Set("Hector and Jeff's Database Hints")) {
      val result = magazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBookUsingQNames(): Unit = {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.localName == "Bookstore")

    val scope = Scope.Empty
    import scope._

    val magazines =
      for {
        magazine <- bookstore \ (QName("Magazine").res)
        magazineTitle = magazine.getChildElem(QName("Title").res).trimmedText
        booksWithSameName = for {
          book <- bookstore \ (QName("Book").res)
          bookTitle = book.getChildElem(QName("Title").res).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    assertResult(Set("Hector and Jeff's Database Hints")) {
      val result = magazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithNonUniqueTitles(): Unit = {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(bookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        titleString: String = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        otherBooksAndMagazines = {
          val result = bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
          result filter (e => toResolvedElem(e) != toResolvedElem(bookOrMagazine))
        }
        titles = otherBooksAndMagazines map { e => e getChildElem { _.localName == "Title" } }
        titleStrings = {
          val result = titles map { _.trimmedText }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    assertResult(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = booksAndMagazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBook(): Unit = {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        titleString: String = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        otherBooks = bookstore.filterChildElems(EName("Book")) filter (e => toResolvedElem(e) != toResolvedElem(bookOrMagazine))
        titles = otherBooks map { e => e getChildElem { _.localName == "Title" } }
        if titles.map(_.trimmedText).contains(titleString)
      } yield bookOrMagazine

    assertResult(Set("Hector and Jeff's Database Hints")) {
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
  @Test def testQueryBooksWithAllAuthorFirstNamesWithLetterJ(): Unit = {
    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ (_.localName == "Book")
        authorNames = {
          val result = for {
            author <- book.filterElems(EName("Author"))
            firstName = author.getChildElem(EName("First_Name"))
          } yield firstName.trimmedText
          result.toSet
        }
        if authorNames forall { name => name.indexOf("J") >= 0 }
      } yield book

    assertResult(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksFromUllmanButNotWidom(): Unit = {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ (_.localName == "Book")
        authorNames = {
          val result = book.filterElems(EName("Author")) map { _.getChildElem(EName("Last_Name")).trimmedText }
          result.toSet
        }
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.getChildElem(EName("Title"))

    assertResult(Set(
      "Hector and Jeff's Database Hints")) {
      val result = titles map { _.trimmedText }
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
  @Test def testQueryAverageBookPrice(): Unit = {
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore \ (_.localName == "Book")
        price <- book \@ EName("Price")
      } yield price.toDouble
    val averagePrice =
      textElem(QName("Average"), (prices.sum.toDouble / prices.size).toString).build()

    assertResult(65) {
      averagePrice.trimmedText.toDouble.intValue
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
  @Test def testQueryLastNames(): Unit = {
    require(bookstore.localName == "Bookstore")

    val lastNameValues: immutable.IndexedSeq[String] =
      for {
        lastName <- (bookstore.filterElems(EName("Last_Name")) map (e => e.trimmedText)).distinct
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
  @Test def testQueryBookPairsFromSameAuthor(): Unit = {
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def bookAuthorLastNames(book: E): Set[String] = {
      val authors = book.getChildElem(EName("Authors"))
      val result = for {
        author <- authors \ (_.localName == "Author")
        lastName = author getChildElem { _.localName == "Last_Name" }
        lastNameValue: String = lastName.trimmedText
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: E): String = book.getChildElem(EName("Title")).trimmedText

    val pairs =
      for {
        book1 <- bookstore \ (_.localName == "Book")
        book2 <- bookstore \ (_.localName == "Book")
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield elem(
        qname = QName("BookPair"),
        children = Vector(
          textElem(QName("Title1"), bookTitle(book1)),
          textElem(QName("Title2"), bookTitle(book2)))).build()

    assertResult(5) {
      pairs.size
    }
    assertResult(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book1) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book1)).size
    }
    assertResult(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book2) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book2)).size
    }
    assertResult(2) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book3) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book3)).size
    }
    assertResult(2) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book4) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book4)).size
    }
  }

  @Test def testQueryBookAndMagazineTitlesRelabeled(): Unit = {
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

    assertResult(Set(EName("BookTitle"), EName("MagazineTitle"))) {
      bookOrMagazineTitles.map(e => e.resolvedName).toSet
    }
    val ngCount = bookOrMagazineTitles count { e => e.trimmedText == "National Geographic" }
    assert(ngCount == 2, "Expected 'National Geographic' twice")
  }

  @Test def testDepthFirst(): Unit = {
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

    assertResult(depthFirstElmNames) {
      elms map { _.resolvedName }
    }
  }

  @Test def testQueryBookWithGivenIsbn(): Unit = {
    // See http://kousenit.wordpress.com/2008/03/12/nothing-makes-you-want-groovy-more-than-xml/,
    // but taking "our" bookstore as input XML. Somewhat more verbose than the Groovy example, but also more
    // explicit (about elements, expanded names, etc.).

    require(bookstore.localName == "Bookstore")

    val isbn = "ISBN-0-11-222222-3"

    val bookElmOption = bookstore findElem { e => e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some(isbn) }
    val bookElm = bookElmOption.getOrElse(sys.error(s"Expected Book with ISBN $isbn"))

    val title = bookElm.getChildElem(_.localName == "Title").text

    val authorLastNames =
      for {
        authorsElm <- bookElm \ (_.localName == "Authors")
        lastNameElm <- authorsElm \\ (_.localName == "Last_Name")
      } yield lastNameElm.text
    val firstAuthorLastName = authorLastNames.head

    assertResult("Hector and Jeff's Database Hints") {
      title
    }
    assertResult("Ullman") {
      firstAuthorLastName
    }
  }

  protected val bookstore: E

  protected def book1: E =
    bookstore.findElem(e => e.localName == "Book" && e.findAttributeByLocalName("ISBN") == Some("ISBN-0-13-713526-2")).get

  protected def book2: E =
    bookstore.findElem(e => e.localName == "Book" && e.findAttributeByLocalName("ISBN") == Some("ISBN-0-13-815504-6")).get

  protected def book3: E =
    bookstore.findElem(e => e.localName == "Book" && e.findAttributeByLocalName("ISBN") == Some("ISBN-0-11-222222-3")).get

  protected def book4: E =
    bookstore.findElem(e => e.localName == "Book" && e.findAttributeByLocalName("ISBN") == Some("ISBN-9-88-777777-6")).get

  protected def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem
}
