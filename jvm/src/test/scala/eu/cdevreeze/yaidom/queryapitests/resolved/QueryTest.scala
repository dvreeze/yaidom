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

package eu.cdevreeze.yaidom.queryapitests.resolved

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi._
import eu.cdevreeze.yaidom.queryapitests.AbstractElemLikeQueryTest
import eu.cdevreeze.yaidom.resolved.Elem
import eu.cdevreeze.yaidom.resolved.Text
import eu.cdevreeze.yaidom.simple

import scala.collection.immutable

/**
 * Query test case for resolved Elems.
 *
 * @author Chris de Vreeze
 */
class QueryTest extends AbstractElemLikeQueryTest {

  final type E = Elem

  private val book1Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "A First Course in Database Systems"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            )
          )
        )
      )
    )
  }

  private val book2Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Database Systems: The Complete Book"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Hector"),
                textElem(QName("Last_Name"), Scope.Empty, "Garcia-Molina"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            )
          )
        ),
        textElem(QName("Remark"), Scope.Empty, "Buy this book bundled with \"A First Course\" - a great deal!")
      )
    )
  }

  private val book3Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Hector and Jeff's Database Hints"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Hector"),
                textElem(QName("Last_Name"), Scope.Empty, "Garcia-Molina"))
            )
          )
        ),
        textElem(QName("Remark"), Scope.Empty, "An indispensable companion to your textbook")
      )
    )
  }

  private val book4Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Jennifer's Economical Database Hints"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            ))
        )
      )
    )
  }

  private val magazine1Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "National Geographic"))
    )
  }

  private val magazine2Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "National Geographic"))
    )
  }

  private val magazine3Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "Newsweek"))
    )
  }

  private val magazine4Builder: simple.Elem = {
    import simple.Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "Hector and Jeff's Database Hints"))
    )
  }

  protected final val bookstore: Elem = {
    import simple.Node._

    val result =
      elem(
        qname = QName("Bookstore"),
        scope = Scope.Empty,
        children = Vector(
          book1Builder,
          book2Builder,
          book3Builder,
          book4Builder,
          magazine1Builder,
          magazine2Builder,
          magazine3Builder,
          magazine4Builder)
      )
    Elem.from(result)
  }

  test("testQueryElementsWithParentNotBookOrBookstoreUsingStoredElemPaths") {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    require(bookstore.localName == "Bookstore")

    def addElemPaths(e: Elem, path: Path, scope: Scope): Elem = {
      def getChildPathEntry(e: Elem, nodeIdx: Int): Path.Entry = {
        val includedChildren = e.children.take(nodeIdx + 1)
        val ename = includedChildren.last.asInstanceOf[Elem].resolvedName
        val childElmsWithSameName = includedChildren.collect { case che: Elem if che.resolvedName == ename => che }
        Path.Entry(ename, childElmsWithSameName.size - 1)
      }

      Elem(
        resolvedName = e.resolvedName,
        resolvedAttributes = e.resolvedAttributes + (EName("path") -> path.toResolvedCanonicalXPath),
        children = e.children.zipWithIndex.map {
          case (ch, idx) =>
            ch match {
              case che: Elem =>
                val childPath = path.append(getChildPathEntry(e, idx))
                // Recursive call
                addElemPaths(che, childPath, scope)
              case _ => ch
            }
        }
      )
    }

    // Not using method Elem.updated here
    val bookStoreWithPaths = addElemPaths(bookstore, Path.Empty, Scope.Empty)

    val elms =
      for {
        desc <- bookStoreWithPaths.findAllElems
        path = Path.fromResolvedCanonicalXPath(desc.attribute(EName("path")))
        parent <- bookstore.findElemOrSelfByPath(path.parentPath)
        if parent.resolvedName != EName("Bookstore") && parent.resolvedName != EName("Book")
      } yield desc

    assert(elms.size > 10, "Expected more than 10 matching elements")
    val enames: Set[EName] = {
      val result = elms.map { _.resolvedName }
      result.toSet
    }
    assertResult(Set(EName("Title"), EName("Author"), EName("First_Name"), EName("Last_Name"))) {
      enames
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

    val titleAndFirstNames =
      for {
        book <- bookstore \ (_.localName == "Book")
        title = book.getChildElem(EName("Title"))
        authorFirstNames = {
          val result = book.filterElems(EName("Author")).map { _.getChildElem(EName("First_Name")).trimmedText }
          result.toSet
        }
        searchedForFirstNames = authorFirstNames.filter { firstName =>
          title.trimmedText.indexOf(firstName) >= 0
        }
        if searchedForFirstNames.nonEmpty
      } yield
        Elem(
          resolvedName = EName("Book"),
          resolvedAttributes = Map(),
          children = Vector(title, Elem(EName("First_Name"), Map(), Vector(Text(searchedForFirstNames.head)))))

    assertResult(2) {
      titleAndFirstNames.size
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val titleElms = titleAndFirstNames.map { e =>
        e.filterElems(EName("Title"))
      }
      val result = titleElms.flatten.map { e =>
        e.trimmedText
      }
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

    val prices: immutable.IndexedSeq[Double] =
      for {
        book <- bookstore \ (_.localName == "Book")
        price = book.attribute(EName("Price")).toDouble
      } yield price

    val avg: Double = prices.sum / prices.size

    val cheapBooks =
      for {
        book <- bookstore \ (_.localName == "Book")
        price = book.attribute(EName("Price")).toDouble
        if price < avg
      } yield
        Elem(
          resolvedName = EName("Book"),
          resolvedAttributes = Map(),
          children =
            Vector(book.getChildElem(EName("Title")), Elem(EName("Price"), Map(), Vector(Text(price.toString))))
        )

    assertResult(2) {
      cheapBooks.size
    }
    assertResult(Set(50, 25)) {
      val result = cheapBooks
        .flatMap { e =>
          e.filterElems(EName("Price"))
        }
        .map { e =>
          e.trimmedText.toDouble.intValue
        }
      result.toSet
    }
    assertResult(Set("Hector and Jeff's Database Hints", "Jennifer's Economical Database Hints")) {
      val result = cheapBooks
        .flatMap { e =>
          e.filterElems(EName("Title"))
        }
        .map { e =>
          e.trimmedText
        }
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

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attribute(EName("Price")).toInt
      val price2 = book2.attribute(EName("Price")).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- (bookstore \ (_.localName == "Book")).sortWith { cheaper }
        price = book.attribute(EName("Price")).toDouble
      } yield
        Elem(
          resolvedName = EName("Book"),
          resolvedAttributes = Map(),
          children =
            Vector(book.getChildElem(EName("Title")), Elem(EName("Price"), Map(), Vector(Text(price.toString))))
        )
    }

    assertResult(4) {
      books.size
    }
    assertResult(List(25, 50, 85, 100)) {
      books
        .flatMap { e =>
          e.filterElems(EName("Price"))
        }
        .map { e =>
          e.trimmedText.toDouble.intValue
        }
    }
    assertResult(
      List(
        "Jennifer's Economical Database Hints",
        "Hector and Jeff's Database Hints",
        "A First Course in Database Systems",
        "Database Systems: The Complete Book")) {
      books
        .flatMap { e =>
          e.filterElems(EName("Title"))
        }
        .map { e =>
          e.trimmedText
        }
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

    def books(authorLastName: String) =
      for {
        book <- bookstore \ (_.localName == "Book")
        author <- book.filterElems(EName("Author"))
        if author.getChildElem(EName("Last_Name")).trimmedText == authorLastName
      } yield {
        val attrs = book.resolvedAttributes.filter { case (en, _) => Set(EName("ISBN"), EName("Price")).contains(en) }

        val children = book.filterChildElems(EName("Title"))

        Elem(resolvedName = EName("Book"), resolvedAttributes = attrs, children = children)
      }

    val authorsWithBooks =
      for {
        lastNameValue <- {
          val result = bookstore.filterElems(EName("Author")).map { e =>
            e.getChildElem(EName("Last_Name")).trimmedText
          }
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
            Elem(EName("Last_Name"), Map(), Vector(Text(lastNameValue)))) ++ foundBooks
        )
      }

    val invertedBookstore: Elem =
      Elem(resolvedName = EName("InvertedBookstore"), resolvedAttributes = Map(), children = authorsWithBooks)

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

    def removePrice(book: Elem): Elem = {
      require(book.resolvedName == EName("Book"))
      Elem(resolvedName = book.resolvedName, resolvedAttributes = book.resolvedAttributes.filter {
        case (en, _) => en != EName("Price")
      }, children = book.children)
    }

    val bookstoreWithoutPrices: Elem =
      bookstore.transformElemsOrSelf {
        case e if e.resolvedName == EName("Book") => removePrice(e)
        case e                                    => e
      }

    assertResult(4) {
      bookstore.filterElems(EName("Book")).count { e =>
        e.attributeOption(EName("Price")).isDefined
      }
    }
    assertResult(0) {
      bookstoreWithoutPrices.filterElems(EName("Book")).count { e =>
        e.attributeOption(EName("Price")).isDefined
      }
    }
  }

  test("testTransformCombiningFirstAndLastName") {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, combining first and last names into Name elements

    require(bookstore.localName == "Bookstore")

    def combineName(author: Elem): Elem = {
      require(author.resolvedName == EName("Author"))

      val firstNameValue: String = author.getChildElem(EName("First_Name")).trimmedText
      val lastNameValue: String = author.getChildElem(EName("Last_Name")).trimmedText
      val nameValue: String = s"$firstNameValue $lastNameValue"
      val name: Elem = Elem(EName("Name"), Map(), Vector(Text(nameValue)))

      Elem(resolvedName = author.resolvedName, resolvedAttributes = author.resolvedAttributes, children = Vector(name))
    }

    val bookstoreWithCombinedNames: Elem =
      bookstore.transformElemsOrSelf {
        case e if e.resolvedName == EName("Author") => combineName(e)
        case e                                      => e
      }

    assertResult(Set("Jeffrey Ullman", "Jennifer Widom", "Hector Garcia-Molina")) {
      val result = bookstoreWithCombinedNames.filterElems(EName("Name")).map { _.trimmedText }
      result.toSet
    }
  }

  test("testTransformAuthors") {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, giving a namespace to Author elements

    require(bookstore.localName == "Bookstore")

    val updatedBookstoreElm: Elem =
      bookstore.transformElemsOrSelf {
        case e if e.resolvedName == EName("Author") =>
          val newElmName = EName("http://def", e.localName)
          Elem(newElmName, e.resolvedAttributes, e.children)
        case e => e
      }

    // Only the Author elements are functionally changed!

    assertResult(
      Set(
        EName("Bookstore"),
        EName("Magazine"),
        EName("Title"),
        EName("Book"),
        EName("Authors"),
        EName("{http://def}Author"),
        EName("First_Name"),
        EName("Last_Name"),
        EName("Remark")
      )) {

      val result = updatedBookstoreElm.findAllElemsOrSelf.map { _.resolvedName }
      result.toSet
    }
  }
}
