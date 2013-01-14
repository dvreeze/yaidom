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
package indexed

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Query test case. This test case is much like the "original" QueryTest, yet taking indexed Elems instead of standard yaidom
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

  @Test def testQueryBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    require(indexedBookstore.localName == "Bookstore")

    val bookTitles =
      (indexedBookstore \ "Book") map { e => e getChildElem (_.localName == "Title") }

    expect(Set(
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

    require(indexedBookstore.localName == "Bookstore")

    // Using only the ParentElemLike API (except for method localName)...

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- indexedBookstore filterChildElems { e => Set("Book", "Magazine").contains(e.localName) }
        title <- bookOrMagazine findChildElem { _.localName == "Title" }
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitles map { _.trimmedText }
      result.toSet
    }

    // Indexed Elems can be queried for the parent element!

    val bookOrMagazineTitles2 =
      for {
        title <- indexedBookstore filterElems { _.resolvedName == EName("Title") }
        if (title.parent.resolvedName == EName("Book")) || (title.parent.resolvedName == EName("Magazine"))
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitles2 map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(indexedBookstore.localName == "Bookstore")

    val titles =
      for (ch <- indexedBookstore.allChildElems) yield ch.getChildElem(EName("Title"))

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }

    val titles2 =
      for {
        title <- indexedBookstore findTopmostElems { _.resolvedName == EName("Title") }
        if title.elemPath.entries.size == 2
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles2 map { e => indexedBookstore.elem.getWithElemPath(e.elemPath).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryAllTitles() {
    // XPath: doc("bookstore.xml")//Title

    require(indexedBookstore.localName == "Bookstore")

    val titles =
      for (title <- indexedBookstore filterElems (_.localName == "Title")) yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { e => indexedBookstore.elem.getWithElemPath(e.elemPath).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryAllElements() {
    // XPath: doc("bookstore.xml")//*

    require(indexedBookstore.localName == "Bookstore")

    val elements = indexedBookstore.findAllElemsOrSelf

    assert(elements.contains(indexedBookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")

    val childrenAlsoIncluded =
      elements forall { e =>
        e.allChildElems forall { ch => elements.contains(ch) }
      }
    assert(childrenAlsoIncluded, "Expected child elements of each element also in the result")

    val paths = indexedBookstore.elem.findAllElemOrSelfPaths

    expect(elements.size) {
      paths.size
    }

    expect(elements map (e => resolved.Elem(e.elem))) {
      paths map { path => indexedBookstore.elem.getWithElemPath(path) } map { e => resolved.Elem(e) }
    }
  }

  @Test def testQueryBookIsbns() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(indexedBookstore.localName == "Bookstore")

    // Using only the ParentElemLike API (except for method localName)...

    val isbns =
      for (book <- indexedBookstore filterChildElems (_.localName == "Book")) yield book.attribute(EName("ISBN"))

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

    require(indexedBookstore.localName == "Bookstore")

    val books =
      for {
        book <- indexedBookstore \ "Book"
        price <- book \@ "Price"
        if price.toInt < 90
      } yield book

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book =>
        book.elem findElemPath (e => e.resolvedName == EName("Title")) map
          { path => book.elem.getWithElemPath(path).trimmedText }
      }
      result.toSet
    }
  }

  @Test def testQueryCheapBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(indexedBookstore.localName == "Bookstore")

    val titles =
      for {
        book <- indexedBookstore \ "Book"
        price <- book \@ "Price"
        if price.toInt < 90
      } yield book.getChildElem(EName("Title"))

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }

    // Now the verbose way, using the ParentElemLike API and no operator notation...

    val titles2 =
      for {
        book <- indexedBookstore filterChildElems { _.localName == "Book" }
        price <- book.attributeOption(EName("Price"))
        if price.toInt < 90
      } yield book.getChildElem(EName("Title"))

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles2 map { _.trimmedText }
      result.toSet
    }

    val titles3 = indexedBookstore.findAllElems filter { e =>
      (e.resolvedName == EName("Title")) && {
        val parentElm = e.parent
        parentElm.localName == "Book" && parentElm.attribute(EName("Price")).toInt < 90
      }
    }

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles3 map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryCheapBookAuthors() {
    // Own example..

    require(indexedBookstore.localName == "Bookstore")

    val cheapBookElms =
      for {
        bookElm <- indexedBookstore \ "Book"
        price <- bookElm \@ "Price"
        if price.toInt < 90
      } yield bookElm

    val cheapBookAuthors = {
      val result =
        for {
          cheapBookElm <- cheapBookElms
          authorElm <- cheapBookElm \\ "Author"
        } yield {
          val firstNameElmOption = authorElm findChildElem { _.localName == "First_Name" }
          val lastNameElmOption = authorElm findChildElem { _.localName == "Last_Name" }

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

    require(indexedBookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- indexedBookstore \ "Book"
        if !book.filterChildElems(EName("Remark")).isEmpty
      } yield book.getChildElem(EName("Title"))

    expect(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(indexedBookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- indexedBookstore \ "Book"
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ "Last_Name" } map { _.trimmedText }
        if authorLastName == "Ullman"
      } yield book.getChildElem(EName("Title"))

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }

    val bookTitles2 =
      indexedBookstore findTopmostElems { e => e.localName == "Last_Name" && e.trimmedText == "Ullman" } flatMap { elm =>
        require(elm.resolvedName == EName("Last_Name"))
        val bookOption = elm findAncestor { e => e.resolvedName == EName("Book") && e.attribute(EName("Price")).toInt < 90 }
        val titleOption = bookOption flatMap { bookElm => bookElm findElem { e => e.resolvedName == EName("Title") } }
        titleOption
      }

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles2 map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(indexedBookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- indexedBookstore \ "Book"
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ "Last_Name" } map { _.trimmedText }
        if authorLastName == "Ullman"
        authorFirstName <- authors \ { _.localName == "Author" } flatMap { e => e \ "First_Name" } map { _.trimmedText }
        if authorFirstName == "Jeffrey"
      } yield book.getChildElem(EName("Title"))

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }

    def authorLastAndFirstName(authorElem: eu.cdevreeze.yaidom.Elem): (String, String) = {
      val lastNames = authorElem.filterChildElems(EName("Last_Name")) map { _.text.trim }
      val firstNames = authorElem.filterChildElems(EName("First_Name")) map { _.text.trim }
      (lastNames.mkString, firstNames.mkString)
    }

    val bookTitles2 =
      for {
        authorElem <- indexedBookstore filterElemsOrSelf { _.resolvedName == EName("Author") }
        if authorLastAndFirstName(authorElem.elem) == ("Ullman", "Jeffrey")
        bookElem <- authorElem findAncestor { _.resolvedName == EName("Book") }
        if bookElem.attributeOption(EName("Price")).map(_.toInt).getOrElse(0) < 90
      } yield bookElem.getChildElem(EName("Title"))

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles2 map { _.trimmedText }
      result.toSet
    }

    val bookTitles3 =
      for {
        authorElem <- indexedBookstore \\ EName("Author")
        if authorLastAndFirstName(authorElem.elem) == ("Ullman", "Jeffrey")
        bookElem <- authorElem findAncestor { _.resolvedName == EName("Book") }
        if (bookElem \@ EName("Price")).map(_.toInt).getOrElse(0) < 90
      } yield (bookElem \ EName("Title")).head

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles3 map { _.elem.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfBooksByJeffreyUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    require(indexedBookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- indexedBookstore \ "Book"
        authors = book.getChildElem(EName("Authors"))
        lastNameStrings = for {
          author <- authors \ "Author"
          lastNameString = author.getChildElem(EName("Last_Name")).trimmedText
        } yield lastNameString
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.getChildElem(EName("Title"))

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryBooksByJeffreyUllman() {
    // Own example

    require(indexedBookstore.localName == "Bookstore")

    val bookElms =
      for {
        bookElm <- indexedBookstore filterChildElems { _.localName == "Book" }
        if (bookElm \\ "Author") exists { e =>
          ((e.getChildElem(_.localName == "First_Name")).text == "Jeffrey") &&
            ((e.getChildElem(_.localName == "Last_Name")).text == "Ullman")
        }
      } yield bookElm

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = bookElms map { e => e.getChildElem(_.localName == "Title").text }
      result.toSet
    }

    val ullmanBookElms =
      for {
        authorElm <- indexedBookstore filterElems { e =>
          (e.localName == "Author") &&
            ((e.getChildElem(_.localName == "First_Name")).text == "Jeffrey") &&
            ((e.getChildElem(_.localName == "Last_Name")).text == "Ullman")
        }
        bookElm = authorElm.parent.parent
      } yield {
        require(bookElm.localName == "Book")
        bookElm
      }

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = ullmanBookElms map { e => e.getChildElem(_.localName == "Title").text }
      result.toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(indexedBookstore.localName == "Bookstore")

    val secondAuthors =
      for {
        book <- indexedBookstore \ "Book"
        authors = book.getChildElem(EName("Authors"))
        authorColl = authors \ "Author"
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames = secondAuthors map { e => e getChildElem { _.localName == "Last_Name" } }
    expect(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      val result = secondAuthorLastNames map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryGreatBooks() {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    require(indexedBookstore.localName == "Bookstore")

    val titles =
      for {
        book <- indexedBookstore \ "Book"
        remark <- book \ "Remark"
        if remark.trimmedText.indexOf("great") >= 0
      } yield book.getChildElem(EName("Title"))

    expect(Set("Database Systems: The Complete Book")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook() {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(indexedBookstore.localName == "Bookstore")

    val magazines =
      for {
        magazine <- indexedBookstore \ "Magazine"
        magazineTitle = magazine.getChildElem(EName("Title")).trimmedText
        booksWithSameName = for {
          book <- indexedBookstore \ "Book"
          bookTitle = book.getChildElem(EName("Title")).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    expect(Set("Hector and Jeff's Database Hints")) {
      val result = magazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryElementsWithParentNotBookOrBookstore() {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    require(indexedBookstore.localName == "Bookstore")

    val elms =
      for {
        e <- indexedBookstore.findAllElems
        parent = e.parent
        if parent.elem.qname != QName("Bookstore") && parent.elem.qname != QName("Book")
      } yield e

    assert(elms.size > 10, "Expected more than 10 matching elements")

    expect(Set(QName("Title"), QName("Author"), QName("First_Name"), QName("Last_Name"))) {
      val result = elms map { e => e.elem.qname }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithNonUniqueTitles() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(indexedBookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- indexedBookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        titleString: String = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        otherBooksAndMagazines = {
          val result = indexedBookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
          result.toSet -- Set(bookOrMagazine)
        }
        titles = otherBooksAndMagazines map { e => e getChildElem { _.localName == "Title" } }
        if titles.map(_.trimmedText).contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = booksAndMagazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBook() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(indexedBookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- indexedBookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        titleString: String = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        otherBooks = indexedBookstore.filterChildElems(EName("Book")).toSet -- Set(bookOrMagazine)
        titles = otherBooks map { e => e getChildElem { _.localName == "Title" } }
        if titles.map(_.trimmedText).contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints")) {
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
    require(indexedBookstore.localName == "Bookstore")

    val books =
      for {
        book <- indexedBookstore \ "Book"
        authorNames = {
          val result = for {
            author <- book.filterElems(EName("Author"))
            firstName = author.getChildElem(EName("First_Name"))
          } yield firstName.trimmedText
          result.toSet
        }
        if authorNames forall { name => name.indexOf("J") >= 0 }
      } yield book

    expect(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksFromUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(indexedBookstore.localName == "Bookstore")

    val titles =
      for {
        book <- indexedBookstore \ "Book"
        authorNames = {
          val result = book.filterElems(EName("Author")) map { _.getChildElem(EName("Last_Name")).trimmedText }
          result.toSet
        }
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.getChildElem(EName("Title"))

    expect(Set(
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
    require(indexedBookstore.localName == "Bookstore")

    import NodeBuilder._

    val titleAndFirstNames =
      for {
        book <- indexedBookstore \ "Book"
        title = book.getChildElem(EName("Title"))
        authorFirstNames = {
          val result = book.filterElems(EName("Author")) map { _.getChildElem(EName("First_Name")).trimmedText }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames filter { firstName => title.trimmedText.indexOf(firstName) >= 0 }
        if !searchedForFirstNames.isEmpty
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(title.elem)(Scope.Empty),
          textElem(QName("First_Name"), searchedForFirstNames.head))).build()

    expect(2) {
      titleAndFirstNames.size
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
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
    require(indexedBookstore.localName == "Bookstore")

    import NodeBuilder._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- indexedBookstore \ "Book"
        price <- book \@ "Price"
      } yield price.toDouble
    val averagePrice =
      textElem(QName("Average"), (prices.sum.toDouble / prices.size).toString).build()

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
    require(indexedBookstore.localName == "Bookstore")

    import NodeBuilder._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- indexedBookstore \ "Book"
        price = book.attribute(EName("Price")).toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- indexedBookstore \ "Book"
        price = book.attribute(EName("Price")).toDouble
        if price < avg
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(book.getChildElem(EName("Title")).elem)(Scope.Empty),
          textElem(QName("Price"), price.toString))).build()

    expect(2) {
      cheapBooks.size
    }
    expect(Set(50, 25)) {
      val result = cheapBooks flatMap { e => e.filterElems(EName("Price")) } map { e => e.trimmedText.toDouble.intValue }
      result.toSet
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
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
    require(indexedBookstore.localName == "Bookstore")

    import NodeBuilder._

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attribute(EName("Price")).toInt
      val price2 = book2.attribute(EName("Price")).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- indexedBookstore \ "Book" sortWith { cheaper _ }
        price = book.attribute(EName("Price")).toDouble
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(book.getChildElem(EName("Title")).elem)(Scope.Empty),
          textElem(QName("Price"), price.toString))).build()
    }

    expect(4) {
      books.size
    }
    expect(List(25, 50, 85, 100)) {
      books flatMap { e => e.filterElems(EName("Price")) } map { e => e.trimmedText.toDouble.intValue }
    }
    expect(List(
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
    require(indexedBookstore.localName == "Bookstore")

    val lastNameValues: immutable.IndexedSeq[String] =
      for {
        lastName <- (indexedBookstore.filterElems(EName("Last_Name")) map (e => e.trimmedText)).distinct
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
    require(indexedBookstore.localName == "Bookstore")

    import NodeBuilder._

    def bookAuthorLastNames(book: Elem): Set[String] = {
      val authors = book.getChildElem(EName("Authors"))
      val result = for {
        author <- authors \ "Author"
        lastName = author getChildElem { _.localName == "Last_Name" }
        lastNameValue: String = lastName.trimmedText
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: eu.cdevreeze.yaidom.Elem): String = book.getChildElem(EName("Title")).trimmedText

    val pairs =
      for {
        book1 <- indexedBookstore \ "Book"
        book2 <- indexedBookstore \ "Book"
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1.elem) < bookTitle(book2.elem)
      } yield elem(
        qname = QName("BookPair"),
        children = Vector(
          textElem(QName("Title1"), bookTitle(book1.elem)),
          textElem(QName("Title2"), bookTitle(book2.elem)))).build()

    expect(5) {
      pairs.size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book1.build()) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book1.build())).size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book2.build()) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book2.build())).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book3.build()) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book3.build())).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book4.build()) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book4.build())).size
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
    require(indexedBookstore.localName == "Bookstore")

    import NodeBuilder._

    def books(authorLastName: String) =
      for {
        book <- indexedBookstore \ "Book"
        author <- book.filterElems(EName("Author"))
        if author.getChildElem(EName("Last_Name")).trimmedText == authorLastName
      } yield {
        val attrs = book.elem.attributes filter { case (qn, v) => Set(QName("ISBN"), QName("Price")).contains(qn) }
        elem(
          qname = QName("Book"),
          attributes = attrs).
          withChildNodes(book.elem.filterChildElems(EName("Title")))(Scope.Empty).build()
      }

    val authorsWithBooks =
      for {
        lastNameValue <- {
          val result = indexedBookstore.filterElems(EName("Author")) map { e => e.getChildElem(EName("Last_Name")).trimmedText }
          result.distinct
        }
      } yield {
        val author: Elem = {
          val result = for {
            author <- indexedBookstore.filterElems(EName("Author"))
            if author.getChildElem(EName("Last_Name")).trimmedText == lastNameValue
          } yield author
          result.head
        }
        val firstNameValue: String = author.getChildElem(EName("First_Name")).trimmedText

        val foundBooks = books(lastNameValue)
        val bookBuilders = foundBooks map { book => fromElem(book)(Scope.Empty) }

        elem(
          qname = QName("Author"),
          children = Vector(
            textElem(QName("First_Name"), firstNameValue),
            textElem(QName("Last_Name"), lastNameValue)) ++ bookBuilders).build()
      }

    val invertedBookstore: Elem =
      Elem(eu.cdevreeze.yaidom.Elem(qname = QName("InvertedBookstore"), children = authorsWithBooks))

    expect(3) {
      invertedBookstore.allChildElems.size
    }
  }

  @Test def testQueryBookAndMagazineTitlesRelabeled() {
    // Taken from the XSLT demo
    require(indexedBookstore.localName == "Bookstore")

    import NodeBuilder._

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- indexedBookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
      } yield {
        val titleString = bookOrMagazine.getChildElem(EName("Title")).trimmedText

        if (bookOrMagazine.resolvedName == EName("Book")) {
          textElem(QName("BookTitle"), titleString).build()
        } else {
          textElem(QName("MagazineTitle"), titleString).build()
        }
      }

    expect(Set(EName("BookTitle"), EName("MagazineTitle"))) {
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

    require(indexedBookstore.localName == "Bookstore")

    def removePrice(book: eu.cdevreeze.yaidom.Elem): eu.cdevreeze.yaidom.Elem = {
      require(book.resolvedName == EName("Book"))
      eu.cdevreeze.yaidom.Elem(
        qname = book.qname,
        attributes = book.attributes filter { case (qn, v) => qn != QName("Price") },
        scope = book.scope,
        children = book.children)
    }

    val bookstoreWithoutPrices: Elem = {
      val pf: PartialFunction[eu.cdevreeze.yaidom.Elem, eu.cdevreeze.yaidom.Elem] = {
        case e: eu.cdevreeze.yaidom.Elem if e.resolvedName == EName("Book") => removePrice(e)
      }
      val result = indexedBookstore.elem updated pf
      Elem(result)
    }

    expect(4) {
      indexedBookstore.filterElems(EName("Book")) count { e => e.attributeOption(EName("Price")).isDefined }
    }
    expect(0) {
      bookstoreWithoutPrices.filterElems(EName("Book")) count { e => e.attributeOption(EName("Price")).isDefined }
    }
    expect(4) {
      val elms = indexedBookstore findTopmostElems { e => (e.resolvedName == EName("Book")) && (e.attributeOption(EName("Price")).isDefined) }
      elms.size
    }
    expect(0) {
      val elms = bookstoreWithoutPrices findTopmostElems { e => (e.resolvedName == EName("Book")) && (e.attributeOption(EName("Price")).isDefined) }
      elms.size
    }
  }

  @Test def testDepthFirst() {
    require(indexedBookstore.localName == "Bookstore")

    // Returns descendant-or-self elements in depth-first order, that is, in document order
    val elms = indexedBookstore.findAllElemsOrSelf

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

    expect(depthFirstElmNames) {
      elms map { _.resolvedName }
    }
  }

  @Test def testQueryBookWithGivenIsbn() {
    // See http://kousenit.wordpress.com/2008/03/12/nothing-makes-you-want-groovy-more-than-xml/,
    // but taking "our" bookstore as input XML. Somewhat more verbose than the Groovy example, but also more
    // explicit (about elements, expanded names, etc.).

    require(indexedBookstore.localName == "Bookstore")

    val isbn = "ISBN-0-11-222222-3"

    val bookElmOption = indexedBookstore findElem { e => e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some(isbn) }
    val bookElm = bookElmOption.getOrElse(sys.error("Expected Book with ISBN %s".format(isbn)))

    val title = bookElm.getChildElem(_.localName == "Title").text

    val authorLastNames =
      for {
        authorsElm <- bookElm \ "Authors"
        lastNameElm <- authorsElm \\ "Last_Name"
      } yield lastNameElm.text
    val firstAuthorLastName = authorLastNames.head

    expect("Hector and Jeff's Database Hints") {
      title
    }
    expect("Ullman") {
      firstAuthorLastName
    }
  }

  private val book1: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      children = Vector(
        textElem(QName("Title"), "A First Course in Database Systems"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jeffrey"),
                textElem(QName("Last_Name"), "Ullman"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jennifer"),
                textElem(QName("Last_Name"), "Widom")))))))
  }

  private val book2: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
      children = Vector(
        textElem(QName("Title"), "Database Systems: The Complete Book"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Hector"),
                textElem(QName("Last_Name"), "Garcia-Molina"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jeffrey"),
                textElem(QName("Last_Name"), "Ullman"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jennifer"),
                textElem(QName("Last_Name"), "Widom"))))),
        textElem(QName("Remark"), "Buy this book bundled with \"A First Course\" - a great deal!")))
  }

  private val book3: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
      children = Vector(
        textElem(QName("Title"), "Hector and Jeff's Database Hints"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jeffrey"),
                textElem(QName("Last_Name"), "Ullman"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Hector"),
                textElem(QName("Last_Name"), "Garcia-Molina"))))),
        textElem(QName("Remark"), "An indispensable companion to your textbook")))
  }

  private val book4: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
      children = Vector(
        textElem(QName("Title"), "Jennifer's Economical Database Hints"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jennifer"),
                textElem(QName("Last_Name"), "Widom")))))))
  }

  private val magazine1: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  private val magazine2: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  private val magazine3: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Newsweek")))
  }

  private val magazine4: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Hector and Jeff's Database Hints")))
  }

  private val indexedBookstore: Elem = {
    import NodeBuilder._

    val result =
      elem(
        qname = QName("Bookstore"),
        children = Vector(
          book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)
    indexed.Elem(result)
  }
}
