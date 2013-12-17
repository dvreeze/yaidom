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
package resolved

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Query test case for resolved Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class QueryTest extends AbstractPathAwareElemLikeQueryTest {

  final type E = Elem

  @Test def testQueryElementsWithParentNotBookOrBookstoreUsingStoredElemPaths() {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    require(bookstore.localName == "Bookstore")

    def addElemPaths(e: Elem, path: Path, scope: Scope): Elem = {
      def getChildPathEntry(e: Elem, nodeIdx: Int): Path.Entry = {
        val includedChildren = e.children.take(nodeIdx + 1)
        val ename = includedChildren.last.asInstanceOf[Elem].resolvedName
        val childElmsWithSameName = includedChildren collect { case che: Elem if che.resolvedName == ename => che }
        Path.Entry(ename, childElmsWithSameName.size - 1)
      }

      Elem(
        resolvedName = e.resolvedName,
        resolvedAttributes = e.resolvedAttributes + (EName("path") -> path.toCanonicalXPath(scope)),
        children = e.children.zipWithIndex map {
          case (ch, idx) =>
            ch match {
              case che: Elem =>
                val childPath = path.append(getChildPathEntry(e, idx))
                // Recursive call
                addElemPaths(che, childPath, scope)
              case _ => ch
            }
        })
    }

    // Not using method Elem.updated here
    val bookStoreWithPaths = addElemPaths(bookstore, Path.Root, Scope.Empty)

    val elms =
      for {
        desc <- bookStoreWithPaths.findAllElems
        path = Path.fromCanonicalXPath(desc.attribute(EName("path")))(Scope.Empty)
        parent <- bookstore.findElemOrSelfByPath(path.parentPath)
        if parent.resolvedName != EName("Bookstore") && parent.resolvedName != EName("Book")
      } yield desc

    assert(elms.size > 10, "Expected more than 10 matching elements")
    val enames: Set[EName] = {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    expectResult(Set(EName("Title"), EName("Author"), EName("First_Name"), EName("Last_Name"))) {
      enames
    }

    // We could do a lot better, using the PathAwareElemLike API...

    val paths =
      for {
        path <- bookstore.findAllElemPaths
        parentPath = path.parentPath
        parent = bookstore.getElemOrSelfByPath(parentPath)
        if parent.resolvedName != EName("Bookstore") && parent.resolvedName != EName("Book")
      } yield path

    assert(paths.size > 10, "Expected more than 10 matching paths")

    expectResult(Set(EName("Title"), EName("Author"), EName("First_Name"), EName("Last_Name"))) {
      val result = paths map { path => bookstore.getElemOrSelfByPath(path).resolvedName }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithNonUniqueTitlesUsingPaths() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::*/Title or Title = preceding-sibling::*/Title]

    require(bookstore.localName == "Bookstore")

    val bookAndMagazinePaths =
      for {
        bookOrMagazinePath <- bookstore filterChildElemPaths { e => Set("Book", "Magazine").contains(e.localName) }
        bookOrMagazine = bookstore.getElemOrSelfByPath(bookOrMagazinePath)
        title = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        nodeIndex = bookstore.childNodeIndex(bookOrMagazinePath.lastEntry)
        nextChildren = bookstore.children.drop(nodeIndex + 1)
        prevChildren = bookstore.children.take(nodeIndex)
        nextOption = nextChildren find {
          case e: Elem if (e.getChildElem(EName("Title")).trimmedText == title) => true
          case n: Node => false
        } collect { case e: Elem => e }
        prevOption = prevChildren find {
          case e: Elem if (e.getChildElem(EName("Title")).trimmedText == title) => true
          case n: Node => false
        } collect { case e: Elem => e }
        if (nextOption.isDefined || prevOption.isDefined)
      } yield bookOrMagazinePath

    expectResult(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = bookAndMagazinePaths flatMap { path => bookstore.getElemOrSelfByPath(path).findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }
  }

  @Test def testQueryBooksOrMagazinesWithTitleAsOtherBookUsingPaths() {
    // XPath: doc("bookstore.xml")//(Book|Magazine)[Title = following-sibling::Book/Title or Title = preceding-sibling::Book/Title]

    require(bookstore.localName == "Bookstore")

    val bookAndMagazinePaths =
      for {
        bookOrMagazinePath <- bookstore filterChildElemPaths { e => Set("Book", "Magazine").contains(e.localName) }
        bookOrMagazine = bookstore.getElemOrSelfByPath(bookOrMagazinePath)
        title = bookOrMagazine.getChildElem(EName("Title")).trimmedText
        nodeIndex = bookstore.childNodeIndex(bookOrMagazinePath.lastEntry)
        nextChildren = bookstore.children.drop(nodeIndex + 1)
        prevChildren = bookstore.children.take(nodeIndex)
        nextBookOption = nextChildren find {
          case e: Elem if (e.localName == "Book") && (e.getChildElem(EName("Title")).trimmedText == title) => true
          case n: Node => false
        } collect { case e: Elem => e }
        prevBookOption = prevChildren find {
          case e: Elem if (e.localName == "Book") && (e.getChildElem(EName("Title")).trimmedText == title) => true
          case n: Node => false
        } collect { case e: Elem => e }
        if (nextBookOption.isDefined || prevBookOption.isDefined)
      } yield bookOrMagazinePath

    expectResult(Set("Hector and Jeff's Database Hints")) {
      val result = bookAndMagazinePaths flatMap { path => bookstore.getElemOrSelfByPath(path).findElem(EName("Title")) map { _.trimmedText } }
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

    val titleAndFirstNames =
      for {
        book <- bookstore \ (_.localName == "Book")
        title = book.getChildElem(EName("Title"))
        authorFirstNames = {
          val result = book.filterElems(EName("Author")) map { _.getChildElem(EName("First_Name")).trimmedText }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames filter { firstName => title.trimmedText.indexOf(firstName) >= 0 }
        if !searchedForFirstNames.isEmpty
      } yield Elem(
        resolvedName = EName("Book"),
        resolvedAttributes = Map(),
        children = Vector(
          title,
          Elem(EName("First_Name"), Map(), Vector(Text(searchedForFirstNames.head)))))

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
      } yield Elem(
        resolvedName = EName("Book"),
        resolvedAttributes = Map(),
        children = Vector(
          book.getChildElem(EName("Title")),
          Elem(EName("Price"), Map(), Vector(Text(price.toString)))))

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

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attribute(EName("Price")).toInt
      val price2 = book2.attribute(EName("Price")).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- bookstore \ (_.localName == "Book") sortWith { cheaper _ }
        price = book.attribute(EName("Price")).toDouble
      } yield Elem(
        resolvedName = EName("Book"),
        resolvedAttributes = Map(),
        children = Vector(
          book.getChildElem(EName("Title")),
          Elem(EName("Price"), Map(), Vector(Text(price.toString)))))
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

    def books(authorLastName: String) =
      for {
        book <- bookstore \ (_.localName == "Book")
        author <- book.filterElems(EName("Author"))
        if author.getChildElem(EName("Last_Name")).trimmedText == authorLastName
      } yield {
        val attrs = book.resolvedAttributes filter { case (en, v) => Set(EName("ISBN"), EName("Price")).contains(en) }

        val children = book.filterChildElems(EName("Title"))

        Elem(
          resolvedName = EName("Book"),
          resolvedAttributes = attrs,
          children = children)
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

        Elem(
          resolvedName = EName("Author"),
          resolvedAttributes = Map(),
          children = Vector(
            Elem(EName("First_Name"), Map(), Vector(Text(firstNameValue))),
            Elem(EName("Last_Name"), Map(), Vector(Text(lastNameValue)))) ++ foundBooks)
      }

    val invertedBookstore: Elem = Elem(resolvedName = EName("InvertedBookstore"), resolvedAttributes = Map(), children = authorsWithBooks)

    expectResult(3) {
      invertedBookstore.findAllChildElems.size
    }
  }

  @Test def testTransformLeavingOutPrices() {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, leaving out book prices

    require(bookstore.localName == "Bookstore")

    def removePrice(book: Elem): Elem = {
      require(book.resolvedName == EName("Book"))
      Elem(
        resolvedName = book.resolvedName,
        resolvedAttributes = book.resolvedAttributes filter { case (en, v) => en != EName("Price") },
        children = book.children)
    }

    val bookstoreWithoutPrices: Elem =
      bookstore transformElemsOrSelf {
        case e if e.resolvedName == EName("Book") => removePrice(e)
        case e => e
      }

    expectResult(4) {
      bookstore.filterElems(EName("Book")) count { e => e.attributeOption(EName("Price")).isDefined }
    }
    expectResult(0) {
      bookstoreWithoutPrices.filterElems(EName("Book")) count { e => e.attributeOption(EName("Price")).isDefined }
    }
    expectResult(4) {
      val paths = bookstore findTopmostElemPaths { e => (e.resolvedName == EName("Book")) && (e.attributeOption(EName("Price")).isDefined) }
      paths.size
    }
    expectResult(0) {
      val paths = bookstoreWithoutPrices findTopmostElemPaths { e => (e.resolvedName == EName("Book")) && (e.attributeOption(EName("Price")).isDefined) }
      paths.size
    }
  }

  @Test def testTransformCombiningFirstAndLastName() {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, combining first and last names into Name elements

    require(bookstore.localName == "Bookstore")

    def combineName(author: Elem): Elem = {
      require(author.resolvedName == EName("Author"))

      val firstNameValue: String = author.getChildElem(EName("First_Name")).trimmedText
      val lastNameValue: String = author.getChildElem(EName("Last_Name")).trimmedText
      val nameValue: String = "%s %s".format(firstNameValue, lastNameValue)
      val name: Elem = Elem(EName("Name"), Map(), Vector(Text(nameValue)))

      Elem(
        resolvedName = author.resolvedName,
        resolvedAttributes = author.resolvedAttributes,
        children = Vector(name))
    }

    val bookstoreWithCombinedNames: Elem =
      bookstore transformElemsOrSelf {
        case e if e.resolvedName == EName("Author") => combineName(e)
        case e => e
      }

    expectResult(Set("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina")) {
      val result = bookstoreWithCombinedNames.filterElems(EName("Name")) map { _.trimmedText }
      result.toSet
    }
  }

  @Test def testTransformAuthors() {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, giving a namespace to Author elements

    require(bookstore.localName == "Bookstore")

    val updatedBookstoreElm: Elem =
      bookstore transformElemsOrSelf {
        case e if e.resolvedName == EName("Author") =>
          val newElmName = EName("http://def", e.localName)
          Elem(newElmName, e.resolvedAttributes, e.children)
        case e => e
      }

    // Only the Author elements are functionally changed!

    expectResult(Set(EName("Bookstore"), EName("Magazine"), EName("Title"), EName("Book"), EName("Authors"),
      EName("{http://def}Author"), EName("First_Name"), EName("Last_Name"), EName("Remark"))) {

      val result = updatedBookstoreElm.findAllElemsOrSelf map { _.resolvedName }
      result.toSet
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

  protected final val bookstore: resolved.Elem = {
    import NodeBuilder._

    val result =
      elem(
        qname = QName("Bookstore"),
        children = Vector(
          book1Builder, book2Builder, book3Builder, book4Builder,
          magazine1Builder, magazine2Builder, magazine3Builder, magazine4Builder)).build(Scope.Empty)
    resolved.Elem(result)
  }

  protected final def toResolvedElem(elem: E): resolved.Elem = elem
}
