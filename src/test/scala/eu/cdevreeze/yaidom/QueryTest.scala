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
 * verbosely. More than anything else, it demonstrates the expressive power of Scala's excellent Collections API.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material.
 */
@RunWith(classOf[JUnitRunner])
class QueryTest extends Suite {

  @Test def testQueryBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      bookstore.childElems("Book".ename) map { e => e.childElem("Title".ename) }

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      bookTitles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryBookOrMagazineTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookOrMagazineTitles: immutable.Seq[Element] =
      for {
        bookOrMagazine <- bookstore childElems { e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName) }
      } yield bookOrMagazine.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      bookOrMagazineTitles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for (ch <- bookstore.childElems) yield ch.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      titles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryAllTitles() {
    // XPath: doc("bookstore.xml")//Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for (title <- bookstore.elems("Title".ename)) yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      titles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryAllElements() {
    // XPath: doc("bookstore.xml")//*

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val elements: immutable.Seq[Element] = bookstore.elems :+ bookstore

    assert(elements.contains(bookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")
    assert(elements.forall(e => {
      e.childElems.forall(ch => elements.contains(ch))
    }), "Expected child elements of each element also in the result")
  }

  @Test def testQueryBookIsbns() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    val bookstore = sampleXml
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

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val books: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename)
        if price.toInt < 90
      } yield book

    expect(Set(
      book1, book3, book4)) {
      books.toSet
    }
  }

  @Test def testQueryCheapBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        if book.attribute("Price".ename).toInt < 90
      } yield book.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      titles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfBooksWithRemarks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        if !book.childElems("Remark".ename).isEmpty
      } yield book.childElem("Title".ename)

    expect(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      bookTitles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        if book.attribute("Price".ename).toInt < 90
        val authors = book.childElem("Authors".ename)
        val lastNameOption = (for {
          author <- authors.childElems("Author".ename)
          val lastName = author.childElem("Last_Name".ename)
          if lastName.firstTextValue == "Ullman"
        } yield lastName).headOption
        if lastNameOption.isDefined
      } yield book.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      bookTitles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        if book.attribute("Price".ename).toInt < 90
        val authors = book.childElem("Authors".ename)
        val lastNameOption = (for {
          author <- authors.childElems("Author".ename)
          val lastName = author.childElem("Last_Name".ename)
          val firstName = author.childElem("First_Name".ename)
          if lastName.firstTextValue == "Ullman"
          if firstName.firstTextValue == "Jeffrey"
        } yield lastName).headOption
        if lastNameOption.isDefined
      } yield book.childElem("Title".ename)

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      bookTitles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfBooksByJeffreyUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authors = book.childElem("Authors".ename)
        val lastNameStrings: immutable.Seq[String] = (for {
          author <- authors.childElems("Author".ename)
          val lastNameString = author.childElem("Last_Name".ename).firstTextValue
        } yield lastNameString)
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
      } yield book.childElem("Title".ename)

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      bookTitles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val secondAuthors: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authors = book.childElem("Authors".ename)
        val authorColl = authors.childElems("Author".ename)
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames: immutable.Seq[Element] = secondAuthors.map(e => e.childElem("Last_Name".ename))
    expect(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      secondAuthorLastNames.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryGreatBooks() {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        remark <- book.childElems("Remark".ename)
        if remark.firstTextValue.indexOf("great") >= 0
      } yield book.childElem("Title".ename)

    expect(Set("Database Systems: The Complete Book")) {
      titles.map(_.firstTextValue).toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook() {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val magazines: immutable.Seq[Element] =
      for {
        magazine <- bookstore.childElems("Magazine".ename)
        val magazineTitle: String = magazine.childElem("Title".ename).firstTextValue
        val booksWithSameName = for {
          book <- bookstore.childElems("Book".ename)
          val bookTitle: String = book.childElem("Title".ename).firstTextValue
          if magazineTitle == bookTitle
        } yield book
        if !booksWithSameName.isEmpty
      } yield magazine

    expect(Set(magazine4)) {
      magazines.toSet
    }
  }

  @Test def testQueryElementsWithParentNotBookOrBookstore() {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val elems: immutable.Seq[Element] =
      for {
        desc <- bookstore.elems
        parent <- desc.parentInTreeOption(bookstore)
        if parent.qname != "Bookstore".qname && parent.qname != "Book".qname
      } yield desc

    assert(elems.size > 10, "Expected more than 10 matching elements")
    val qnames: Set[QName] = elems.map(_.qname).toSet
    expect(Set("Title".qname, "Author".qname, "First_Name".qname, "Last_Name".qname)) {
      qnames
    }
  }

  @Test def testQueryBooksOrMagazinesWithNonUniqueTitles() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val booksAndMagazines: immutable.Seq[Element] =
      for {
        bookOrMagazine <- bookstore.childElems(e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName))
        val titleString: String = bookOrMagazine.childElem("Title".ename).firstTextValue
        val otherBooksAndMagazines = bookstore.childElems(e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName)).toSet -- Set(bookOrMagazine)
        val titles = otherBooksAndMagazines.map(e => e.childElem("Title".ename))
        val titleStrings: Set[String] = titles.map(_.firstTextValue).toSet
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set(book3, magazine4, magazine1, magazine2)) {
      booksAndMagazines.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBook() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val booksAndMagazines: immutable.Seq[Element] =
      for {
        bookOrMagazine <- bookstore.childElems(e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName))
        val titleString: String = bookOrMagazine.childElem("Title".ename).firstTextValue
        val otherBooks = bookstore.childElems("Book".ename).toSet -- Set(bookOrMagazine)
        val titles = otherBooks.map(e => e.childElem("Title".ename))
        val titleStrings: Set[String] = titles.map(_.firstTextValue).toSet
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set(magazine4)) {
      booksAndMagazines.toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * <pre>
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * where every $fn in $b/Authors/Author/First_Name satisfies contains($fn, "J")
   * return $b
   * </pre>
   * or XPath:
   * <pre>
   * doc("bookstore.xml")//Book[count(Authors/Author[contains(First_Name, "J"]) = count(Authors/Author/First_Name)]
   * </pre>
   */
  @Test def testQueryBooksWithAllAuthorFirstNamesWithLetterJ() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val books: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authorNames: Set[String] = (for {
          author <- book.elems("Author".ename)
          val firstName = author.childElem("First_Name".ename)
        } yield firstName.firstTextValue).toSet
        if authorNames.forall(name => name.indexOf("J") >= 0)
      } yield book

    expect(Set(book1, book4)) {
      books.toSet
    }
  }

  @Test def testQueryBooksFromUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        val authorNames: Set[String] = book.elems("Author".ename).map(_.childElem("Last_Name".ename).firstTextValue).toSet
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
      } yield book.childElem("Title".ename)

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      titles.map(_.firstTextValue).toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * <pre>
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * where some $fm in $b/Authors/Author/First_Name satisfies contains($b/Title, $fn)
   * return &lt;Book&gt;
   *          { $b/Title }
   *          { for $fm in $b/Authors/Author/First_Name where contains($b/Title, $fn) return $fn }
   *        &lt;/Book&gt;
   * </pre>
   */
  @Test def testQueryBooksWithAuthorInTitle() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titleAndFirstNames: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        val title = book.childElem("Title".ename)
        val authorFirstNames: Set[String] = book.elems("Author".ename).map(_.childElem("First_Name".ename).firstTextValue).toSet
        val searchedForFirstNames: Set[String] = authorFirstNames.filter(firstName => title.firstTextValue.indexOf(firstName) >= 0)
        if !searchedForFirstNames.isEmpty
      } yield Element(
        qname = "Book".qname,
        children = List(
          title,
          Element(qname = "First_Name".qname, children = List(Text(searchedForFirstNames.head)))))

    expect(2) {
      titleAndFirstNames.size
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      titleAndFirstNames.map(e => e.elems("Title".ename)).flatten.map(e => e.firstTextValue).toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * <pre>
   * &lt;Average&gt;
   * { let $plist := doc("bookstore.xml")/Bookstore/Book/@Price
   *   return avg($plist) }
   * &lt;/Average&gt;
   * </pre>
   */
  @Test def testQueryAverageBookPrice() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val prices: immutable.Seq[Double] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename).toDouble
      } yield price
    val averagePrice =
      Element(qname = "Average".qname,
        children = List(Text((prices.sum.toDouble / prices.size).toString)))

    expect(65) {
      averagePrice.firstTextValue.toDouble.intValue
    }
  }

  /**
   * The equivalent of XQuery:
   * <pre>
   * let $a := avg(doc("bookstore.xml")/Bookstore/Book/@Price)
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * where $b/@Price < $a
   * return &lt;Book&gt;
   *          { $b/Title }
   *          &lt;Price&gt; { $b/data(@Price) } &lt;/Price&gt;
   *        &lt;/Book&gt;
   * </pre>
   */
  @Test def testQueryBooksPricedBelowAverage() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val prices: immutable.Seq[Double] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename).toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks: immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        val price = book.attribute("Price".ename).toDouble
        if price < avg
      } yield Element(
        qname = "Book".qname,
        children = List(
          book.childElem("Title".ename),
          Element(
            qname = "Price".qname,
            children = List(Text(price.toString)))))

    expect(2) {
      cheapBooks.size
    }
    expect(Set(50, 25)) {
      cheapBooks.flatMap(e => e.elems("Price".ename)).map(e => e.firstTextValue.toDouble.intValue).toSet
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      cheapBooks.flatMap(e => e.elems("Title".ename)).map(e => e.firstTextValue).toSet
    }
  }

  /**
   * The equivalent of XQuery:
   * <pre>
   * for $b in doc("bookstore.xml")/Bookstore/Book
   * order by $b/@Price
   * return &lt;Book&gt;
   *          { $b/Title }
   *          &lt;Price&gt; { $b/data(@Price) } &lt;/Price&gt;
   *        &lt;/Book&gt;
   * </pre>
   */
  @Test def testQueryBooksOrderedByPrice() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    def cheaper(book1: Element, book2: Element): Boolean = {
      val price1 = book1.attribute("Price".ename).toInt
      val price2 = book2.attribute("Price".ename).toInt
      price1 < price2
    }

    val books: immutable.Seq[Element] = {
      for {
        book <- bookstore.childElems("Book".ename).sortWith(cheaper _)
        val price = book.attribute("Price".ename).toDouble
      } yield Element(
        qname = "Book".qname,
        children = List(
          book.childElem("Title".ename),
          Element(
            qname = "Price".qname,
            children = List(Text(price.toString)))))
    }

    expect(4) {
      books.size
    }
    expect(List(25, 50, 85, 100)) {
      books.flatMap(e => e.elems("Price".ename)).map(e => e.firstTextValue.toDouble.intValue)
    }
    expect(List(
      "Jennifer's Economical Database Hints",
      "Hector and Jeff's Database Hints",
      "A First Course in Database Systems",
      "Database Systems: The Complete Book")) {
      books.flatMap(e => e.elems("Title".ename)).map(e => e.firstTextValue)
    }
  }

  /**
   * The equivalent of XQuery:
   * <pre>
   * for $n in distinct-values(doc("bookstore.xml")//Last_Name)
   * return &lt;Last_Name&gt;
   *          { $n }
   *        &lt;/Last_Name&gt;
   * </pre>
   */
  @Test def testQueryLastNames() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val lastNameValues: immutable.Seq[String] =
      for {
        lastName <- bookstore.elems("Last_Name".ename).map(e => e.firstTextValue).distinct
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
   * <pre>
   * for $b1 in doc("bookstore.xml")/Bookstore/Book
   * for $b2 in doc("bookstore.xml")/Bookstore/Book
   * where $b1/Authors/Author/Last_Name = $b2/Authors/Author/Last_Name
   * and $b1/Title < $b2/Title
   * return &lt;BookPair&gt;
   *          &lt;Title1&gt;{ data($b1/Title) }&lt;/Title1&gt;
   *          &lt;Title2&gt;{ data($b2/Title) }&lt;/Title2&gt;
   *        &lt;/BookPair&gt;
   * </pre>
   */
  @Test def testQueryBookPairsFromSameAuthor() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    def bookAuthorLastNames(book: Element): Set[String] = {
      val authors = book.childElem("Authors".ename)
      (for {
        author <- authors.childElems("Author".ename)
        val lastName = author.childElem("Last_Name".ename)
        val lastNameValue: String = lastName.firstTextValue
      } yield lastNameValue).toSet
    }

    def bookTitle(book: Element): String = book.childElem("Title".ename).firstTextValue

    val pairs: immutable.Seq[Element] =
      for {
        book1 <- bookstore.childElems("Book".ename)
        book2 <- bookstore.childElems("Book".ename)
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield Element(
        qname = "BookPair".qname,
        children = List(
          Element(
            qname = "Title1".qname,
            children = List(Text(bookTitle(book1)))),
          Element(
            qname = "Title2".qname,
            children = List(Text(bookTitle(book2))))))

    expect(5) {
      pairs.size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).firstTextValue == bookTitle(book1) ||
          pair.childElem("Title2".ename).firstTextValue == bookTitle(book1)).size
    }
    expect(3) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).firstTextValue == bookTitle(book2) ||
          pair.childElem("Title2".ename).firstTextValue == bookTitle(book2)).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).firstTextValue == bookTitle(book3) ||
          pair.childElem("Title2".ename).firstTextValue == bookTitle(book3)).size
    }
    expect(2) {
      pairs.filter(pair =>
        pair.childElem("Title1".ename).firstTextValue == bookTitle(book4) ||
          pair.childElem("Title2".ename).firstTextValue == bookTitle(book4)).size
    }
  }

  /**
   * The equivalent of XQuery:
   * <pre>
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
   * </pre>
   */
  @Test def testQueryInvertedBookstore() {
    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    def books(authorLastName: String): immutable.Seq[Element] =
      for {
        book <- bookstore.childElems("Book".ename)
        author <- book.elems("Author".ename)
        if author.childElem("Last_Name".ename).firstTextValue == authorLastName
      } yield Element(
        qname = "Book".qname,
        attributes = book.attributes.filterKeys(a => Set("ISBN".qname, "Price".qname).contains(a)),
        children = book.childElems("Title".ename))

    val authorsWithBooks: immutable.Seq[Element] =
      for {
        lastNameValue <- bookstore.elems("Author".ename).map(e => e.childElem("Last_Name".ename).firstTextValue).distinct
      } yield {
        val author: Element =
          (for {
            author <- bookstore.elems("Author".ename)
            if author.childElem("Last_Name".ename).firstTextValue == lastNameValue
          } yield author).head
        val firstNameValue: String = author.childElem("First_Name".ename).firstTextValue

        Element(
          qname = "Author".qname,
          children = List(
            Element(
              qname = "First_Name".qname,
              children = List(Text(firstNameValue))),
            Element(
              qname = "Last_Name".qname,
              children = List(Text(lastNameValue)))) ++ books(lastNameValue))
      }

    val invertedBookstore: Element = Element(qname = "InvertedBookstore".qname, children = authorsWithBooks)

    expect(3) {
      invertedBookstore.childElems.size
    }
  }

  private val book1: Element =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(
            Text("A First Course in Database Systems"))),
        Element(
          qname = QName("Authors"),
          children = List(
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Jeffrey"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Ullman"))))),
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Jennifer"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Widom")))))))))

  private val book2 =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(
            Text("Database Systems: The Complete Book"))),
        Element(
          qname = QName("Authors"),
          children = List(
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Hector"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Garcia-Molina"))))),
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Jeffrey"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Ullman"))))),
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Jennifer"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Widom"))))))),
        Element(
          qname = QName("Remark"),
          children = List(Text("Buy this book bundled with \"A First Course\" - a great deal!")))))

  private val book3 =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(
            Text("Hector and Jeff's Database Hints"))),
        Element(
          qname = QName("Authors"),
          children = List(
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Jeffrey"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Ullman"))))),
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Hector"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Garcia-Molina"))))))),
        Element(
          qname = QName("Remark"),
          children = List(Text("An indispensable companion to your textbook")))))

  private val book4 =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(
            Text("Jennifer's Economical Database Hints"))),
        Element(
          qname = QName("Authors"),
          children = List(
            Element(
              qname = QName("Author"),
              children = List(
                Element(
                  qname = QName("First_Name"),
                  children = List(Text("Jennifer"))),
                Element(
                  qname = QName("Last_Name"),
                  children = List(Text("Widom")))))))))

  private val magazine1 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "January", QName("Year") -> "2009"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(Text("National Geographic")))))

  private val magazine2 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(Text("National Geographic")))))

  private val magazine3 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(Text("Newsweek")))))

  private val magazine4 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "March", QName("Year") -> "2009"),
      children = List(
        Element(
          qname = QName("Title"),
          children = List(Text("Hector and Jeff's Database Hints")))))

  private def sampleXml: Element =
    Element(
      qname = QName("Bookstore"),
      children = List(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4))
}
