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

package eu.cdevreeze.yaidom.queryapitests.indexed

import java.net.URI

import scala.Vector
import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.ElemBuilder
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapitests.AbstractElemLikeQueryTest

/**
 * Query test case for indexed Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class QueryTest extends AbstractElemLikeQueryTest {

  final type E = Elem

  test("testInternalConsistency") {
    require(bookstore.localName == "Bookstore")

    val elems = bookstore.findAllElemsOrSelf

    elems.foreach(e => e.assertConsistency())
  }

  test("testQueryAll") {
    require(bookstore.localName == "Bookstore")

    val elems = bookstore.findAllElemsOrSelf

    assertResult(true) {
      elems.nonEmpty
    }

    assertResult(true) {
      elems forall { e =>
        val parentScope = e.path.parentPathOption.map(p => bookstore.getElemOrSelfByPath(p).scope).getOrElse(Scope.Empty)
        parentScope.resolve(e.namespaces) == e.scope
      }
    }

    assertResult(bookstore.underlyingElem.findAllElemsOrSelf) {
      elems map { e => e.underlyingElem }
    }
  }

  test("testQueryBookOrMagazineTitlesUsingParent") {
    // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

    require(bookstore.localName == "Bookstore")

    // Indexed Elems can be queried for the parent element!

    val bookOrMagazineTitles =
      for {
        title <- bookstore filterElems { _.resolvedName == EName("Title") }
        if (title.path.parentPath.elementNameOption.contains(EName("Book"))) ||
          (title.path.parentPath.elementNameOption.contains(EName("Magazine")))
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

  test("testQueryTitlesUsingPaths") {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        title <- bookstore findTopmostElems { _.resolvedName == EName("Title") }
        if title.path.entries.size == 2
      } yield title

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titles map { e => bookstore.underlyingElem.getElemOrSelfByPath(e.path).trimmedText }
      result.toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByUllmanUsingParent") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author/Last_Name = "Ullman"]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      bookstore findTopmostElems { e => e.localName == "Last_Name" && e.trimmedText == "Ullman" } flatMap { elm =>
        require(elm.resolvedName == EName("Last_Name"))
        val bookOption = elm.path findAncestorPath { p =>
          val e = bookstore.getElemOrSelfByPath(p)
          e.resolvedName == EName("Book") && e.attribute(EName("Price")).toInt < 90
        } map { p => bookstore.getElemOrSelfByPath(p) }
        val titleOption = bookOption flatMap { bookElm => bookElm findElem { e => e.resolvedName == EName("Title") } }
        titleOption
      }

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }
  }

  test("testQueryTitlesOfCheapBooksByJeffreyUllmanUsingParent") {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.localName == "Bookstore")

    def authorLastAndFirstName(authorElem: eu.cdevreeze.yaidom.simple.Elem): (String, String) = {
      val lastNames = authorElem.filterChildElems(EName("Last_Name")) map { _.text.trim }
      val firstNames = authorElem.filterChildElems(EName("First_Name")) map { _.text.trim }
      (lastNames.mkString, firstNames.mkString)
    }

    val bookTitles2 =
      for {
        authorElem <- bookstore filterElemsOrSelf { _.resolvedName == EName("Author") }
        (lastName, firstName) = authorLastAndFirstName(authorElem.underlyingElem)
        if lastName == "Ullman" && firstName == "Jeffrey"
        bookElem <- authorElem.path findAncestorPath { _.elementNameOption.contains(EName("Book")) } map { p =>
          bookstore.getElemOrSelfByPath(p)
        }
        if bookElem.attributeOption(EName("Price")).map(_.toInt).getOrElse(0) < 90
      } yield bookElem.getChildElem(EName("Title"))

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles2 map { _.trimmedText }
      result.toSet
    }

    val bookTitles3 =
      for {
        authorElem <- bookstore \\ EName("Author")
        (lastName, firstName) = authorLastAndFirstName(authorElem.underlyingElem)
        if lastName == "Ullman" && firstName == "Jeffrey"
        bookElem <- authorElem.path findAncestorPath { _.elementNameOption.contains(EName("Book")) } map { p =>
          bookstore.getElemOrSelfByPath(p)
        }
        if (bookElem \@ EName("Price")).map(_.toInt).getOrElse(0) < 90
      } yield (bookElem \ EName("Title")).head

    assertResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles3 map { _.underlyingElem.trimmedText }
      result.toSet
    }
  }

  test("testQueryBooksByJeffreyUllmanUsingParent") {
    // Own example

    require(bookstore.localName == "Bookstore")

    val ullmanBookElms =
      for {
        authorElm <- bookstore filterElems { e =>
          (e.localName == "Author") &&
            ((e.getChildElem(_.localName == "First_Name")).text == "Jeffrey") &&
            ((e.getChildElem(_.localName == "Last_Name")).text == "Ullman")
        }
        bookElmPath = authorElm.path.parentPath.parentPath
        bookElm = bookstore.getElemOrSelfByPath(bookElmPath)
      } yield {
        require(bookElm.localName == "Book")
        bookElm
      }

    assertResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = ullmanBookElms map { e => e.getChildElem(_.localName == "Title").text }
      result.toSet
    }
  }

  test("testQueryElementsWithParentNotBookOrBookstore") {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    require(bookstore.localName == "Bookstore")

    val elms =
      for {
        e <- bookstore.findAllElems
        parent = bookstore.getElemOrSelfByPath(e.path.parentPath)
        if parent.underlyingElem.qname != QName("Bookstore") && parent.underlyingElem.qname != QName("Book")
      } yield e

    assert(elms.size > 10, "Expected more than 10 matching elements")

    assertResult(Set(QName("Title"), QName("Author"), QName("First_Name"), QName("Last_Name"))) {
      val result = elms map { e => e.underlyingElem.qname }
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val titleAndFirstNames =
      for {
        book <- bookstore \ (_.localName == "Book")
        title = book.getChildElem(EName("Title"))
        authorFirstNames = {
          val result = book.filterElems(EName("Author")) map { _.getChildElem(EName("First_Name")).trimmedText }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames filter { firstName => title.trimmedText.indexOf(firstName) >= 0 }
        if searchedForFirstNames.nonEmpty
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(title.underlyingElem)(Scope.Empty),
          textElem(QName("First_Name"), searchedForFirstNames.head))).build()

    assertResult(2) {
      titleAndFirstNames.size
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val titleElms = titleAndFirstNames map { e => e.filterElems(EName("Title")) }
      val result = titleElms.flatten map { e => e.trimmedText }
      result.toSet
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore \ (_.localName == "Book")
        price = book.attribute(EName("Price")).toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- bookstore \ (_.localName == "Book")
        price = book.attribute(EName("Price")).toDouble
        if price < avg
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(book.getChildElem(EName("Title")).underlyingElem)(Scope.Empty),
          textElem(QName("Price"), price.toString))).build()

    assertResult(2) {
      cheapBooks.size
    }
    assertResult(Set(50, 25)) {
      val result = cheapBooks flatMap { e => e.filterElems(EName("Price")) } map { e => e.trimmedText.toDouble.intValue }
      result.toSet
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
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
  test("testQueryBooksOrderedByPrice") {
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attribute(EName("Price")).toInt
      val price2 = book2.attribute(EName("Price")).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- bookstore \ (_.localName == "Book") sortWith { cheaper _ }
        price = book.attribute(EName("Price")).toDouble
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(book.getChildElem(EName("Title")).underlyingElem)(Scope.Empty),
          textElem(QName("Price"), price.toString))).build()
    }

    assertResult(4) {
      books.size
    }
    assertResult(List(25, 50, 85, 100)) {
      books flatMap { e => e.filterElems(EName("Price")) } map { e => e.trimmedText.toDouble.intValue }
    }
    assertResult(List(
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def books(authorLastName: String) =
      for {
        book <- bookstore \ (_.localName == "Book")
        author <- book.filterElems(EName("Author"))
        if author.getChildElem(EName("Last_Name")).trimmedText == authorLastName
      } yield {
        val attrs = book.underlyingElem.attributes filter { case (qn, v) => Set(QName("ISBN"), QName("Price")).contains(qn) }

        val children = book.underlyingElem.filterChildElems(EName("Title")) map { e => NodeBuilder.fromElem(e)(book.underlyingElem.scope) }

        elem(
          qname = QName("Book"),
          attributes = attrs,
          children = children).build()
      }

    val authorsWithBooks =
      for {
        lastNameValue <- {
          val result = bookstore.filterElems(EName("Author")) map { e => e.getChildElem(EName("Last_Name")).trimmedText }
          result.distinct
        }
      } yield {
        val author: Elem = {
          val result = for {
            author <- bookstore.filterElems(EName("Author"))
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
      Elem(
        Some(bookstore.docUri),
        eu.cdevreeze.yaidom.simple.Elem(qname = QName("InvertedBookstore"), children = authorsWithBooks))

    assertResult(3) {
      invertedBookstore.findAllChildElems.size
    }
  }

  test("testTransformLeavingOutPrices") {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, leaving out book prices

    require(bookstore.localName == "Bookstore")

    def removePrice(book: eu.cdevreeze.yaidom.simple.Elem): eu.cdevreeze.yaidom.simple.Elem = {
      require(book.resolvedName == EName("Book"))
      eu.cdevreeze.yaidom.simple.Elem(
        qname = book.qname,
        attributes = book.attributes filter { case (qn, v) => qn != QName("Price") },
        scope = book.scope,
        children = book.children)
    }

    val bookstoreWithoutPrices: Elem = {
      val f: eu.cdevreeze.yaidom.simple.Elem => eu.cdevreeze.yaidom.simple.Elem = {
        case e: eu.cdevreeze.yaidom.simple.Elem if e.resolvedName == EName("Book") => removePrice(e)
        case e: eu.cdevreeze.yaidom.simple.Elem                                    => e
      }
      val result = bookstore.underlyingElem.transformElemsOrSelf(f)
      Elem(Some(bookstore.docUri), result)
    }

    assertResult(4) {
      bookstore.filterElems(EName("Book")) count { e => e.attributeOption(EName("Price")).isDefined }
    }
    assertResult(0) {
      bookstoreWithoutPrices.filterElems(EName("Book")) count { e => e.attributeOption(EName("Price")).isDefined }
    }
    assertResult(4) {
      val elms = bookstore findTopmostElems { e => (e.resolvedName == EName("Book")) && (e.attributeOption(EName("Price")).isDefined) }
      elms.size
    }
    assertResult(0) {
      val elms = bookstoreWithoutPrices findTopmostElems { e => (e.resolvedName == EName("Book")) && (e.attributeOption(EName("Price")).isDefined) }
      elms.size
    }
  }

  test("testAncestryQueryingConsistency") {
    val reverseAncestryOrSelfENames = bookstore.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)

    assertResult(bookstore.findAllElemsOrSelf.map(_.resolvedName)) {
      reverseAncestryOrSelfENames.map(_.last)
    }
    assertResult(List(bookstore.resolvedName)) {
      reverseAncestryOrSelfENames.map(_.head).distinct
    }

    val reverseAncestryOrSelf = bookstore.findAllElemsOrSelf.map(_.reverseAncestryOrSelf)

    assertResult(reverseAncestryOrSelfENames) {
      reverseAncestryOrSelf.map(_.map(_.resolvedName))
    }
  }

  private val book1Builder: ElemBuilder = {
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

  private val book2Builder: ElemBuilder = {
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

  private val book3Builder: ElemBuilder = {
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

  private val book4Builder: ElemBuilder = {
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

  private val magazine1Builder: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  private val magazine2Builder: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  private val magazine3Builder: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Newsweek")))
  }

  private val magazine4Builder: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Hector and Jeff's Database Hints")))
  }

  protected final val bookstore: Elem = {
    import NodeBuilder._

    val result =
      elem(
        qname = QName("Bookstore"),
        children = Vector(
          book1Builder, book2Builder, book3Builder, book4Builder,
          magazine1Builder, magazine2Builder, magazine3Builder, magazine4Builder)).build(Scope.Empty)
    Elem(Some(new URI("http://bookstore.xml")), result)
  }
}
