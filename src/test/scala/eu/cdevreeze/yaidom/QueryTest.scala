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
      for {
        book <- bookstore.childElements("Book".ename)
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      bookTitles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryBookOrMagazineTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookOrMagazineTitles: immutable.Seq[Element] =
      for {
        bookOrMagazine <- bookstore.childElements(e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName))
        title <- bookOrMagazine.childElements("Title".ename)
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      bookOrMagazineTitles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for {
        ch <- bookstore.childElements
        title <- ch.childElements("Title".ename)
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      titles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryAllTitles() {
    // XPath: doc("bookstore.xml")//Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for {
        title <- bookstore.descendants("Title".ename)
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      titles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryAllElements() {
    // XPath: doc("bookstore.xml")//*

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val elements: immutable.Seq[Element] = bookstore.descendants :+ bookstore

    assert(elements.contains(bookstore), "Expected element 'Bookstore', among others")
    assert(elements.size > 10, "Expected more than 10 elements")
    assert(elements.forall(e => {
      e.childElements.forall(ch => elements.contains(ch))
    }), "Expected child elements of each element also in the result")
  }

  @Test def testQueryBookIsbns() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val isbns: immutable.Seq[String] =
      for {
        book <- bookstore.childElements("Book".ename)
        isbn <- book.attribute("ISBN".ename)
      } yield isbn

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
        book <- bookstore.childElements("Book".ename)
        price <- book.attribute("Price".ename)
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
        book <- bookstore.childElements("Book".ename)
        if book.attribute("Price".ename).exists(_.toInt < 90)
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      titles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfBooksWithRemarks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Remark]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElements("Book".ename)
        if !book.childElements("Remark".ename).isEmpty
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set(
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      bookTitles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElements("Book".ename)
        if book.attribute("Price".ename).exists(_.toInt < 90)
        val lastNameOption = (for {
          authors <- book.childElements("Authors".ename)
          author <- authors.childElements("Author".ename)
          lastName <- author.childElements("Last_Name".ename)
          if lastName.firstTextValue.exists(_ == "Ullman")
        } yield lastName).headOption
        if lastNameOption.isDefined
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      bookTitles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElements("Book".ename)
        if book.attribute("Price".ename).exists(_.toInt < 90)
        val lastNameOption = (for {
          authors <- book.childElements("Authors".ename)
          author <- authors.childElements("Author".ename)
          lastName <- author.childElements("Last_Name".ename)
          firstName <- author.childElements("First_Name".ename)
          if lastName.firstTextValue.exists(_ == "Ullman")
          if firstName.firstTextValue.exists(_ == "Jeffrey")
        } yield lastName).headOption
        if lastNameOption.isDefined
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      bookTitles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryTitlesOfBooksByJeffreyUllmanButNotWidom() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val bookTitles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElements("Book".ename)
        val lastNameStrings: immutable.Seq[String] = (for {
          authors <- book.childElements("Authors".ename)
          author <- authors.childElements("Author".ename)
          lastName <- author.childElements("Last_Name".ename)
          lastNameString <- lastName.firstTextValue
        } yield lastNameString)
        if lastNameStrings.contains("Ullman") && !lastNameStrings.contains("Widom")
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      bookTitles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val secondAuthors: immutable.Seq[Element] =
      for {
        book <- bookstore.childElements("Book".ename)
        authors <- book.childElements("Authors".ename)
        val authorColl = authors.childElements("Author".ename)
        if authorColl.size >= 2
        secondAuthor <- authorColl.drop(1).headOption
      } yield secondAuthor

    val secondAuthorLastNames: immutable.Seq[Element] = secondAuthors.flatMap(e => e.childElements("Last_Name".ename).headOption)
    expect(Set(
      "Widom",
      "Ullman",
      "Garcia-Molina")) {
      secondAuthorLastNames.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryGreatBooks() {
    // XPath: doc("bookstore.xml")//Book[contains(Remark, "great")]/Title

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val titles: immutable.Seq[Element] =
      for {
        book <- bookstore.childElements("Book".ename)
        remark <- book.childElements("Remark".ename)
        if remark.firstTextValue.exists(s => s.indexOf("great") >= 0)
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set("Database Systems: The Complete Book")) {
      titles.flatMap(_.firstTextValue).toSet
    }
  }

  @Test def testQueryMagazinesWithSameNameAsBook() {
    // XPath: doc("bookstore.xml")//Magazine[Title = doc("bookstore.xml")//Book[Title]]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val magazines: immutable.Seq[Element] =
      for {
        magazine <- bookstore.childElements("Magazine".ename)
        val magazineTitle: String = magazine.childElements("Title".ename).flatMap(_.firstTextValue).head
        val booksWithSameName = for {
          book <- bookstore.childElements("Book".ename)
          val bookTitle: String = book.childElements("Title".ename).flatMap(_.firstTextValue).head
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

    val descendants: immutable.Seq[Element] =
      for {
        desc <- bookstore.descendants
        parent <- desc.parentIn(bookstore)
        if parent.qname != "Bookstore".qname && parent.qname != "Book".qname
      } yield desc

    assert(descendants.size > 10, "Expected more than 10 matching elements")
    val qnames: Set[QName] = descendants.map(_.qname).toSet
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
        bookOrMagazine <- bookstore.childElements(e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName))
        title <- bookOrMagazine.childElements("Title".ename)
        val titleString: String = title.firstTextValue.get
        val otherBooksAndMagazines = bookstore.childElements(e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName)).toSet -- Set(bookOrMagazine)
        val titles = otherBooksAndMagazines.flatMap(e => e.childElements("Title".ename))
        val titleStrings: Set[String] = titles.flatMap(_.firstTextValue).toSet
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
        bookOrMagazine <- bookstore.childElements(e => Set("Book".ename, "Magazine".ename).contains(e.resolvedName))
        title <- bookOrMagazine.childElements("Title".ename)
        val titleString: String = title.firstTextValue.get
        val otherBooks = bookstore.childElements("Book".ename).toSet -- Set(bookOrMagazine)
        val titles = otherBooks.flatMap(e => e.childElements("Title".ename))
        val titleStrings: Set[String] = titles.flatMap(_.firstTextValue).toSet
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expect(Set(magazine4)) {
      booksAndMagazines.toSet
    }
  }

  @Test def testQueryBooksWithAllAuthorFirstNamesWithLetterJ() {
    // XPath: doc("bookstore.xml")//Book[count(Authors/Author[contains(First_Name, "J") = count(Authors/Author/First_Name)])]

    val bookstore = sampleXml
    require(bookstore.qname.localPart == "Bookstore")

    val books: immutable.Seq[Element] =
      for {
        book <- bookstore.childElements("Book".ename)
        val authorNames: Set[String] = book.descendants("Author".ename).flatMap(_.childElements("First_Name".ename)).flatMap(_.firstTextValue).toSet
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
        book <- bookstore.childElements("Book".ename)
        val authorNames: Set[String] = book.descendants("Author".ename).flatMap(_.childElements("Last_Name".ename)).flatMap(_.firstTextValue).toSet
        if authorNames.contains("Ullman") && !authorNames.contains("Widom")
        title <- book.childElements("Title".ename)
      } yield title

    expect(Set(
      "Hector and Jeff's Database Hints")) {
      titles.flatMap(_.firstTextValue).toSet
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
        book <- bookstore.childElements("Book".ename)
        title <- book.childElements("Title".ename)
        val authorFirstNames: Set[String] = book.descendants("Author".ename).flatMap(_.childElements("First_Name".ename)).flatMap(_.firstTextValue).toSet
        val searchedForFirstNames: Set[String] = authorFirstNames.filter(firstName => title.firstTextValue.head.indexOf(firstName) >= 0)
        if !searchedForFirstNames.isEmpty
      } yield Element(
        qname = "Book".qname,
        children = immutable.Seq(
          title,
          Element(qname = "First_Name".qname, children = immutable.Seq(Text(searchedForFirstNames.head)))))

    expect(2) {
      titleAndFirstNames.size
    }
    expect(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      titleAndFirstNames.map(e => e.descendants("Title".ename)).flatten.map(e => e.firstTextValue.get).toSet
    }
  }

  private val book1: Element =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(
            Text("A First Course in Database Systems"))),
        Element(
          qname = QName("Authors"),
          children = immutable.IndexedSeq(
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Jeffrey"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Ullman"))))),
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Jennifer"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Widom")))))))))

  private val book2 =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(
            Text("Database Systems: The Complete Book"))),
        Element(
          qname = QName("Authors"),
          children = immutable.IndexedSeq(
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Hector"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Garcia-Molina"))))),
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Jeffrey"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Ullman"))))),
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Jennifer"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Widom"))))))),
        Element(
          qname = QName("Remark"),
          children = immutable.IndexedSeq(Text("Buy this book bundled with \"A First Course\" - a great deal!")))))

  private val book3 =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(
            Text("Hector and Jeff's Database Hints"))),
        Element(
          qname = QName("Authors"),
          children = immutable.IndexedSeq(
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Jeffrey"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Ullman"))))),
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Hector"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Garcia-Molina"))))))),
        Element(
          qname = QName("Remark"),
          children = immutable.IndexedSeq(Text("An indispensable companion to your textbook")))))

  private val book4 =
    Element(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(
            Text("Jennifer's Economical Database Hints"))),
        Element(
          qname = QName("Authors"),
          children = immutable.IndexedSeq(
            Element(
              qname = QName("Author"),
              children = immutable.IndexedSeq(
                Element(
                  qname = QName("First_Name"),
                  children = immutable.IndexedSeq(Text("Jennifer"))),
                Element(
                  qname = QName("Last_Name"),
                  children = immutable.IndexedSeq(Text("Widom")))))))))

  private val magazine1 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "January", QName("Year") -> "2009"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(Text("National Geographic")))))

  private val magazine2 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(Text("National Geographic")))))

  private val magazine3 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(Text("Newsweek")))))

  private val magazine4 =
    Element(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "March", QName("Year") -> "2009"),
      children = immutable.IndexedSeq(
        Element(
          qname = QName("Title"),
          children = immutable.IndexedSeq(Text("Hector and Jeff's Database Hints")))))

  private def sampleXml: Element =
    Element(
      qname = QName("Bookstore"),
      children = immutable.IndexedSeq(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4))
}
