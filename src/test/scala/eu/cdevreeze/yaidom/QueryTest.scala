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

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      (bookstore \ (_.localName == "Book")) map { e => e getChildElem (_.localName == "Title") }

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

    require(bookstore.localName == "Bookstore")

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
      } yield bookOrMagazine.getChildElemNamed("Title".ename)

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
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for (ch <- bookstore.allChildElems) yield ch.getChildElemNamed("Title".ename)

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
  }

  @Test def testQueryAllTitles() {
    // XPath: doc("bookstore.xml")//Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for (title <- bookstore filterElems (_.localName == "Title")) yield title

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
  }

  @Test def testQueryAllElements() {
    // XPath: doc("bookstore.xml")//*

    require(bookstore.localName == "Bookstore")

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

    require(bookstore.localName == "Bookstore")

    val isbns =
      for (book <- bookstore \ (_.localName == "Book")) yield book.attribute("ISBN".ename)

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

    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ { _.localName == "Book" }
        val price = book.attribute("Price".ename)
        if price.toInt < 90
      } yield book

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElemNamed("Title".ename) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryCheapBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ { _.localName == "Book" }
        if book.attribute("Price".ename).toInt < 90
      } yield book.getChildElemNamed("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfBooksWithRemarks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ { _.localName == "Book" }
        if !book.filterChildElemsNamed("Remark".ename).isEmpty
      } yield book.getChildElemNamed("Title".ename)

    expect(Set(
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
        book <- bookstore \ { _.localName == "Book" }
        if book.attribute("Price".ename).toInt < 90
        val authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors filterChildElems { _.localName == "Author" } flatMap { e => e \ { _.localName == "Last_Name" } } map { _.trimmedText }
        if authorLastName == "Ullman"
      } yield book.getChildElemNamed("Title".ename)

    expect(Set(
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
        book <- bookstore \ { _.localName == "Book" }
        if book.attribute("Price".ename).toInt < 90
        val authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors filterChildElems { _.localName == "Author" } flatMap { e => e \ { _.localName == "Last_Name" } } map { _.trimmedText }
        if authorLastName == "Ullman"
        authorFirstName <- authors filterChildElems { _.localName == "Author" } flatMap { e => e \ { _.localName == "First_Name" } } map { _.trimmedText }
        if authorFirstName == "Jeffrey"
      } yield book.getChildElemNamed("Title".ename)

    expect(Set(
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
        book <- bookstore \ { _.localName == "Book" }
        val authors = book.getChildElemNamed("Authors".ename)
        val lastNameStrings: immutable.IndexedSeq[String] = for {
          author <- authors \ { _.localName == "Author" }
          val lastNameString = author.getChildElemNamed("Last_Name".ename).trimmedText
        } yield lastNameString
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.getChildElemNamed("Title".ename)

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstore.localName == "Bookstore")

    val secondAuthors =
      for {
        book <- bookstore \ { _.localName == "Book" }
        val authors = book.getChildElemNamed("Authors".ename)
        val authorColl = authors \ { _.localName == "Author" }
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

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ { _.localName == "Book" }
        remark <- book \ { _.localName == "Remark" }
        if remark.trimmedText.indexOf("great") >= 0
      } yield book.getChildElemNamed("Title".ename)

    expect(Set("Database Systems: The Complete Book")) {
      val result = titles map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook() {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    require(bookstore.localName == "Bookstore")

    val magazines =
      for {
        magazine <- bookstore \ { _.localName == "Magazine" }
        val magazineTitle: String = magazine.getChildElemNamed("Title".ename).trimmedText
        val booksWithSameName = for {
          book <- bookstore \ { _.localName == "Book" }
          val bookTitle: String = book.getChildElemNamed("Title".ename).trimmedText
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    expect(Set("Hector and Jeff's Database Hints")) {
      val result = magazines flatMap { mag => mag.findElemNamed("Title".ename) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryElementsWithParentNotBookOrBookstoreUsingStoredElemPaths() {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    require(bookstore.localName == "Bookstore")

    def addElemPaths(e: Elem, path: ElemPath, scope: Scope): Elem = {
      Elem(
        qname = e.qname,
        attributes = e.attributes + ("elemPath".qname -> path.toCanonicalXPath(scope)),
        scope = e.scope,
        children = e.children map { ch =>
          ch match {
            case che: Elem =>
              val childPath = path.append(che.ownElemPathEntry(e))
              // Recursive call
              addElemPaths(che, childPath, scope)
            case _ => ch
          }
        })
    }

    // Not using method Elem.updated here
    val bookStoreWithPaths = addElemPaths(bookstore, ElemPath.Root, Scope.Empty)

    val elms =
      for {
        desc <- bookStoreWithPaths.findAllElems
        val path = ElemPath.fromCanonicalXPath(desc.attribute("elemPath".ename))(Scope.Empty)
        parent <- bookstore.findWithElemPath(path.parentPath)
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

  @Test def testQueryElementsWithParentNotBookOrBookstoreUsingIndexedDocument() {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    // This implementation is similar to the preceding one, except that the index on ElemPath is easily obtained, but not stored

    require(bookstore.localName == "Bookstore")

    val indexedDoc = new IndexedDocument(Document(bookstore))

    val elms =
      for {
        elm <- indexedDoc.document.documentElement.findAllElems
        parent <- indexedDoc.findParent(elm)
        if parent.qname != "Bookstore".qname && parent.qname != "Book".qname
      } yield elm

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

    require(bookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        val titleString: String = bookOrMagazine.getChildElemNamed("Title".ename).trimmedText
        val otherBooksAndMagazines = {
          val result = bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
          result.toSet -- Set(bookOrMagazine)
        }
        val titles = otherBooksAndMagazines map { e => e getChildElem { _.localName == "Title" } }
        val titleStrings: Set[String] = {
          val result = titles map { _.trimmedText }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = booksAndMagazines flatMap { mag => mag.findElemNamed("Title".ename) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBook() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstore.localName == "Bookstore")

    val booksAndMagazines =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
        val titleString: String = bookOrMagazine.getChildElemNamed("Title".ename).trimmedText
        val otherBooks = bookstore.filterChildElemsNamed("Book".ename).toSet -- Set(bookOrMagazine)
        val titles = otherBooks map { e => e getChildElem { _.localName == "Title" } }
        val titleStrings: Set[String] = {
          val result = titles map { _.trimmedText }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set("Hector and Jeff's Database Hints")) {
      val result = booksAndMagazines flatMap { mag => mag.findElemNamed("Title".ename) map { _.trimmedText } }
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
        book <- bookstore \ { _.localName == "Book" }
        val authorNames: Set[String] = {
          val result = for {
            author <- book.filterElemsNamed("Author".ename)
            val firstName = author.getChildElemNamed("First_Name".ename)
          } yield firstName.trimmedText
          result.toSet
        }
        if authorNames forall { name => name.indexOf("J") >= 0 }
      } yield book

    expect(Set("A First Course in Database Systems", "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElemNamed("Title".ename) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksFromUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ { _.localName == "Book" }
        val authorNames: Set[String] = {
          val result = book.filterElemsNamed("Author".ename) map { _.getChildElemNamed("Last_Name".ename).trimmedText }
          result.toSet
        }
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.getChildElemNamed("Title".ename)

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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val titleAndFirstNames =
      for {
        book <- bookstore \ { _.localName == "Book" }
        val title = book.getChildElemNamed("Title".ename)
        val authorFirstNames: Set[String] = {
          val result = book.filterElemsNamed("Author".ename) map { _.getChildElemNamed("First_Name".ename).trimmedText }
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
      val titleElms = titleAndFirstNames map { e => e.filterElemsNamed("Title".ename) }
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
        book <- bookstore \ { _.localName == "Book" }
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore \ { _.localName == "Book" }
        val price = book.attribute("Price".ename).toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- bookstore \ { _.localName == "Book" }
        val price = book.attribute("Price".ename).toDouble
        if price < avg
      } yield elem(
        qname = "Book".qname,
        children = List(
          fromElem(book.getChildElemNamed("Title".ename))(Scope.Empty),
          elem(
            qname = "Price".qname,
            children = List(text(price.toString))))).build()

    expect(2) {
      cheapBooks.size
    }
    expect(Set(50, 25)) {
      val result = cheapBooks flatMap { e => e.filterElemsNamed("Price".ename) } map { e => e.trimmedText.toDouble.intValue }
      result.toSet
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val result = cheapBooks flatMap { e => e.filterElemsNamed("Title".ename) } map { e => e.trimmedText }
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

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attribute("Price".ename).toInt
      val price2 = book2.attribute("Price".ename).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- bookstore \ { _.localName == "Book" } sortWith { cheaper _ }
        val price = book.attribute("Price".ename).toDouble
      } yield elem(
        qname = "Book".qname,
        children = List(
          fromElem(book.getChildElemNamed("Title".ename))(Scope.Empty),
          elem(
            qname = "Price".qname,
            children = List(text(price.toString))))).build()
    }

    expect(4) {
      books.size
    }
    expect(List(25, 50, 85, 100)) {
      books flatMap { e => e.filterElemsNamed("Price".ename) } map { e => e.trimmedText.toDouble.intValue }
    }
    expect(List(
      "Jennifer's Economical Database Hints",
      "Hector and Jeff's Database Hints",
      "A First Course in Database Systems",
      "Database Systems: The Complete Book")) {
      books flatMap { e => e.filterElemsNamed("Title".ename) } map { e => e.trimmedText }
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
        lastName <- (bookstore.filterElemsNamed("Last_Name".ename) map (e => e.trimmedText)).distinct
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def bookAuthorLastNames(book: Elem): Set[String] = {
      val authors = book.getChildElemNamed("Authors".ename)
      val result = for {
        author <- authors \ { _.localName == "Author" }
        val lastName = author getChildElem { _.localName == "Last_Name" }
        val lastNameValue: String = lastName.trimmedText
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: Elem): String = book.getChildElemNamed("Title".ename).trimmedText

    val pairs =
      for {
        book1 <- bookstore \ { _.localName == "Book" }
        book2 <- bookstore \ { _.localName == "Book" }
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
        pair.getChildElemNamed("Title1".ename).trimmedText == bookTitle(book1.build()) ||
          pair.getChildElemNamed("Title2".ename).trimmedText == bookTitle(book1.build())).size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.getChildElemNamed("Title1".ename).trimmedText == bookTitle(book2.build()) ||
          pair.getChildElemNamed("Title2".ename).trimmedText == bookTitle(book2.build())).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.getChildElemNamed("Title1".ename).trimmedText == bookTitle(book3.build()) ||
          pair.getChildElemNamed("Title2".ename).trimmedText == bookTitle(book3.build())).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.getChildElemNamed("Title1".ename).trimmedText == bookTitle(book4.build()) ||
          pair.getChildElemNamed("Title2".ename).trimmedText == bookTitle(book4.build())).size
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def books(authorLastName: String) =
      for {
        book <- bookstore \ { _.localName == "Book" }
        author <- book.filterElemsNamed("Author".ename)
        if author.getChildElemNamed("Last_Name".ename).trimmedText == authorLastName
      } yield {
        val attrs = book.attributes filterKeys { a => Set("ISBN".qname, "Price".qname).contains(a) }
        elem(
          qname = "Book".qname,
          attributes = attrs).
          withChildNodes(book.filterChildElemsNamed("Title".ename))(Scope.Empty).build()
      }

    val authorsWithBooks =
      for {
        lastNameValue <- bookstore.filterElemsNamed("Author".ename) map { e => e.getChildElemNamed("Last_Name".ename).trimmedText } distinct
      } yield {
        val author: Elem = {
          val result = for {
            author <- bookstore.filterElemsNamed("Author".ename)
            if author.getChildElemNamed("Last_Name".ename).trimmedText == lastNameValue
          } yield author
          result.head
        }
        val firstNameValue: String = author.getChildElemNamed("First_Name".ename).trimmedText

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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val bookOrMagazineTitles =
      for {
        bookOrMagazine <- bookstore \ { e => Set("Book", "Magazine").contains(e.localName) }
      } yield {
        val titleString = bookOrMagazine.getChildElemNamed("Title".ename).trimmedText

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

    require(bookstore.localName == "Bookstore")

    def removePrice(book: Elem): Elem = {
      require(book.resolvedName == "Book".ename)
      Elem(
        qname = book.qname,
        attributes = book.attributes filterKeys { a => a != "Price".qname },
        scope = book.scope,
        children = book.children)
    }

    val bookstoreWithoutPrices: Elem =
      bookstore updated {
        case path if path.endsWithName("Book".ename) =>
          val elem = bookstore.findWithElemPath(path).get
          removePrice(elem)
      }

    expect(4) {
      bookstore.filterElemsNamed("Book".ename) count { e => e.attributeOption("Price".ename).isDefined }
    }
    expect(0) {
      bookstoreWithoutPrices.filterElemsNamed("Book".ename) count { e => e.attributeOption("Price".ename).isDefined }
    }
  }

  @Test def testTransformCombiningFirstAndLastName() {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, combining first and last names into Name elements

    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def combineName(author: Elem): Elem = {
      require(author.resolvedName == "Author".ename)

      val firstNameValue: String = author.getChildElemNamed("First_Name".ename).trimmedText
      val lastNameValue: String = author.getChildElemNamed("Last_Name".ename).trimmedText
      val nameValue: String = "%s %s".format(firstNameValue, lastNameValue)
      val name: ElemBuilder = elem(qname = "Name".qname, children = List(text(nameValue)))

      elem(
        qname = author.qname,
        attributes = author.attributes,
        namespaces = Scope.Empty.relativize(author.scope),
        children = List(name)).build()
    }

    val bookstoreWithCombinedNames: Elem =
      bookstore updated {
        case path if path.endsWithName("Author".ename) =>
          val elem = bookstore.findWithElemPath(path).get
          combineName(elem)
      }

    expect(Set("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina")) {
      val result = bookstoreWithCombinedNames.filterElemsNamed("Name".ename) map { _.trimmedText }
      result.toSet
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
