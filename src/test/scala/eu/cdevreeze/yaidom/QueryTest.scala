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
      (bookstore \ "Book") map { e => e getChildElem (_.localName == "Title") }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }

    val bookTitlePaths =
      bookstore findTopmostElemPaths { _.localName == "Title" } filter { path => path.containsName(EName("Book")) }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = bookTitlePaths map { path => bookstore.getWithElemPath(path).trimmedText }
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

    val bookOrMagazineTitlePaths =
      for {
        titlePath <- bookstore filterElemPaths { _.resolvedName == EName("Title") }
        if titlePath.parentPath.endsWithName(EName("Book")) || titlePath.parentPath.endsWithName(EName("Magazine"))
      } yield titlePath

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = bookOrMagazineTitlePaths map { path => bookstore.getWithElemPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/*/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for (ch <- bookstore.allChildElems) yield ch.getChildElem(EName("Title"))

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

    val titlePaths =
      for {
        titlePath <- bookstore findTopmostElemPaths { _.resolvedName == EName("Title") }
        if titlePath.entries.size == 2
      } yield titlePath

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titlePaths map { path => bookstore.getWithElemPath(path).trimmedText }
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

    val titlePaths =
      for {
        titlePath <- bookstore filterElemPaths { _.localName == "Title" }
      } yield titlePath

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints",
      "National Geographic",
      "Newsweek")) {
      val result = titlePaths map { path => bookstore.getWithElemPath(path).trimmedText }
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

    val paths = bookstore.findAllElemOrSelfPaths

    expectResult(elements.size) {
      paths.size
    }

    expectResult(elements map (e => resolved.Elem(e))) {
      paths map { path => bookstore.getWithElemPath(path) } map { e => resolved.Elem(e) }
    }
  }

  @Test def testQueryBookIsbns() {
    // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

    require(bookstore.localName == "Bookstore")

    // Using only the ParentElemLike API (except for method localName)...

    val isbns =
      for (book <- bookstore filterChildElems (_.localName == "Book")) yield book.attribute(EName("ISBN"))

    expectResult(Set(
      "ISBN-0-13-713526-2",
      "ISBN-0-13-815504-6",
      "ISBN-0-11-222222-3",
      "ISBN-9-88-777777-6")) {
      isbns.toSet
    }

    expectResult(Set(
      "ISBN-0-13-713526-2",
      "ISBN-0-13-815504-6",
      "ISBN-0-11-222222-3",
      "ISBN-9-88-777777-6")) {
      val result =
        for (bookPath <- bookstore filterChildElemPaths (e => e.localName == "Book")) yield bookstore.getWithElemPath(bookPath).attribute(EName("ISBN"))
      result.toSet
    }
  }

  @Test def testQueryCheapBooks() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]

    require(bookstore.localName == "Bookstore")

    val books =
      for {
        book <- bookstore \ "Book"
        price <- book \@ "Price"
        if price.toInt < 90
      } yield book

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book => book.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = books flatMap { book =>
        book findElemPath (e => e.resolvedName == EName("Title")) map
          { path => book.getWithElemPath(path).trimmedText }
      }
      result.toSet
    }
  }

  @Test def testQueryCheapBookTitles() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

    require(bookstore.localName == "Bookstore")

    val titles =
      for {
        book <- bookstore \ "Book"
        price <- book \@ "Price"
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

    val titlePaths = bookstore.findAllElemPaths filter { path =>
      path.endsWithName(EName("Title")) && {
        val parentElm = bookstore.getWithElemPath(path.parentPath)
        parentElm.localName == "Book" && parentElm.attribute(EName("Price")).toInt < 90
      }
    }

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")) {
      val result = titlePaths map { path => bookstore.getWithElemPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryCheapBookAuthors() {
    // Own example..

    require(bookstore.localName == "Bookstore")

    val cheapBookElms =
      for {
        bookElm <- bookstore \ "Book"
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
        book <- bookstore \ "Book"
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
        book <- bookstore \ "Book"
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ "Last_Name" } map { _.trimmedText }
        if authorLastName == "Ullman"
      } yield book.getChildElem(EName("Title"))

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitles map { _.trimmedText }
      result.toSet
    }

    val bookTitlePaths =
      bookstore findTopmostElemPaths { e => e.localName == "Last_Name" && e.trimmedText == "Ullman" } filter { path =>
        require(path.endsWithName(EName("Last_Name")))
        path.containsName(EName("Book")) && {
          val bookPath = path.ancestorPaths.filter(_.endsWithName(EName("Book"))).head
          val bookElm = bookstore.getWithElemPath(bookPath)
          bookElm.attribute(EName("Price")).toInt < 90
        }
      } flatMap { path =>
        require(path.endsWithName(EName("Last_Name")))
        val bookPath = path.ancestorPaths.filter(_.endsWithName(EName("Book"))).head
        val bookElm = bookstore.getWithElemPath(bookPath)
        val titlePathOption = bookElm findElemPath { e => e.resolvedName == EName("Title") } map
          { relativeTitlePath => bookPath ++ relativeTitlePath }
        titlePathOption
      }

    expectResult(Set(
      "A First Course in Database Systems",
      "Hector and Jeff's Database Hints")) {
      val result = bookTitlePaths map { path => bookstore.getWithElemPath(path).trimmedText }
      result.toSet
    }
  }

  @Test def testQueryTitlesOfCheapBooksByJeffreyUllman() {
    // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

    require(bookstore.localName == "Bookstore")

    val bookTitles =
      for {
        book <- bookstore \ "Book"
        if book.attribute(EName("Price")).toInt < 90
        authors = book getChildElem { _.localName == "Authors" }
        authorLastName <- authors \ { _.localName == "Author" } flatMap { e => e \ "Last_Name" } map { _.trimmedText }
        if authorLastName == "Ullman"
        authorFirstName <- authors \ { _.localName == "Author" } flatMap { e => e \ "First_Name" } map { _.trimmedText }
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
        book <- bookstore \ "Book"
        authors = book.getChildElem(EName("Authors"))
        lastNameStrings = for {
          author <- authors \ "Author"
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
        if (bookElm \\ "Author") exists { e =>
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

    // Using paths ...

    val ullmanBookElms =
      for {
        authorPath <- bookstore filterElemPaths { e =>
          (e.localName == "Author") &&
            ((e.getChildElem(_.localName == "First_Name")).text == "Jeffrey") &&
            ((e.getChildElem(_.localName == "Last_Name")).text == "Ullman")
        }
        bookPath = authorPath.parentPath.parentPath
      } yield {
        require(bookPath.lastEntry.elementName.localPart == "Book")
        bookstore.getWithElemPath(bookPath)
      }

    expectResult(Set(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints")) {
      val result = ullmanBookElms map { e => e.getChildElem(_.localName == "Title").text }
      result.toSet
    }
  }

  @Test def testQuerySecondAuthors() {
    // XPath: doc("bookstore.xml")//Authors/Author[2]

    require(bookstore.localName == "Bookstore")

    val secondAuthors =
      for {
        book <- bookstore \ "Book"
        authors = book.getChildElem(EName("Authors"))
        authorColl = authors \ "Author"
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
        book <- bookstore \ "Book"
        remark <- book \ "Remark"
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
        magazine <- bookstore \ "Magazine"
        magazineTitle = magazine.getChildElem(EName("Title")).trimmedText
        booksWithSameName = for {
          book <- bookstore \ "Book"
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

  @Test def testQueryElementsWithParentNotBookOrBookstoreUsingStoredElemPaths() {
    // XPath: doc("bookstore.xml")//*[name(parent::*) != "Bookstore" and name(parent::*) != "Book"]

    require(bookstore.localName == "Bookstore")

    def addElemPaths(e: Elem, path: ElemPath, scope: Scope): Elem = {
      def getChildPathEntry(e: Elem, nodeIdx: Int): ElemPath.Entry = {
        val includedChildren = e.children.take(nodeIdx + 1)
        val ename = includedChildren.last.asInstanceOf[Elem].resolvedName
        val childElmsWithSameName = includedChildren collect { case che: Elem if che.resolvedName == ename => che }
        ElemPath.Entry(ename, childElmsWithSameName.size - 1)
      }

      Elem(
        qname = e.qname,
        attributes = e.attributes :+ (QName("elemPath") -> path.toCanonicalXPath(scope)),
        scope = e.scope,
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
    val bookStoreWithPaths = addElemPaths(bookstore, ElemPath.Root, Scope.Empty)

    val elms =
      for {
        desc <- bookStoreWithPaths.findAllElems
        path = ElemPath.fromCanonicalXPath(desc.attribute(EName("elemPath")))(Scope.Empty)
        parent <- bookstore.findWithElemPath(path.parentPath)
        if parent.qname != QName("Bookstore") && parent.qname != QName("Book")
      } yield desc

    assert(elms.size > 10, "Expected more than 10 matching elements")
    val qnames: Set[QName] = {
      val result = elms map { _.qname }
      result.toSet
    }
    expectResult(Set(QName("Title"), QName("Author"), QName("First_Name"), QName("Last_Name"))) {
      qnames
    }

    // We could do a lot better, using the PathAwareElemLike API...

    val paths =
      for {
        path <- bookstore.findAllElemPaths
        parentPath = path.parentPath
        parent = bookstore.getWithElemPath(parentPath)
        if parent.qname != QName("Bookstore") && parent.qname != QName("Book")
      } yield path

    assert(paths.size > 10, "Expected more than 10 matching element paths")

    expectResult(Set(QName("Title"), QName("Author"), QName("First_Name"), QName("Last_Name"))) {
      val result = paths map { path => bookstore.getWithElemPath(path).qname }
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
          result.toSet -- Set(bookOrMagazine)
        }
        titles = otherBooksAndMagazines map { e => e getChildElem { _.localName == "Title" } }
        titleStrings = {
          val result = titles map { _.trimmedText }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expectResult(Set("Hector and Jeff's Database Hints", "National Geographic")) {
      val result = booksAndMagazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }

    // And now using ElemPaths ...

    val bookAndMagazinePaths =
      for {
        bookOrMagazinePath <- bookstore filterChildElemPaths { e => Set("Book", "Magazine").contains(e.localName) }
        bookOrMagazine = bookstore.getWithElemPath(bookOrMagazinePath)
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
      val result = bookAndMagazinePaths flatMap { path => bookstore.getWithElemPath(path).findElem(EName("Title")) map { _.trimmedText } }
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
        otherBooks = bookstore.filterChildElems(EName("Book")).toSet -- Set(bookOrMagazine)
        titles = otherBooks map { e => e getChildElem { _.localName == "Title" } }
        titleStrings = {
          val result = titles map { _.trimmedText }
          result.toSet
        }
        if titleStrings.contains(titleString)
      } yield bookOrMagazine

    expectResult(Set("Hector and Jeff's Database Hints")) {
      val result = booksAndMagazines flatMap { mag => mag.findElem(EName("Title")) map { _.trimmedText } }
      result.toSet
    }

    // And now using ElemPaths ...

    val bookAndMagazinePaths =
      for {
        bookOrMagazinePath <- bookstore filterChildElemPaths { e => Set("Book", "Magazine").contains(e.localName) }
        bookOrMagazine = bookstore.getWithElemPath(bookOrMagazinePath)
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
      val result = bookAndMagazinePaths flatMap { path => bookstore.getWithElemPath(path).findElem(EName("Title")) map { _.trimmedText } }
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
        book <- bookstore \ "Book"
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
        book <- bookstore \ "Book"
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
        book <- bookstore \ "Book"
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
          fromElem(title)(Scope.Empty),
          textElem(QName("First_Name"), searchedForFirstNames.head))).build()

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
        book <- bookstore \ "Book"
        price <- book \@ "Price"
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
        book <- bookstore \ "Book"
        price = book.attribute(EName("Price")).toDouble
      } yield price

    val avg: Double = prices.sum.toDouble / prices.size

    val cheapBooks =
      for {
        book <- bookstore \ "Book"
        price = book.attribute(EName("Price")).toDouble
        if price < avg
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(book.getChildElem(EName("Title")))(Scope.Empty),
          textElem(QName("Price"), price.toString))).build()

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

    def cheaper(book1: Elem, book2: Elem): Boolean = {
      val price1 = book1.attribute(EName("Price")).toInt
      val price2 = book2.attribute(EName("Price")).toInt
      price1 < price2
    }

    val books = {
      for {
        book <- bookstore \ "Book" sortWith { cheaper _ }
        price = book.attribute(EName("Price")).toDouble
      } yield elem(
        qname = QName("Book"),
        children = Vector(
          fromElem(book.getChildElem(EName("Title")))(Scope.Empty),
          textElem(QName("Price"), price.toString))).build()
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

    def bookAuthorLastNames(book: Elem): Set[String] = {
      val authors = book.getChildElem(EName("Authors"))
      val result = for {
        author <- authors \ "Author"
        lastName = author getChildElem { _.localName == "Last_Name" }
        lastNameValue: String = lastName.trimmedText
      } yield lastNameValue
      result.toSet
    }

    def bookTitle(book: Elem): String = book.getChildElem(EName("Title")).trimmedText

    val pairs =
      for {
        book1 <- bookstore \ "Book"
        book2 <- bookstore \ "Book"
        if bookAuthorLastNames(book1).intersect(bookAuthorLastNames(book2)).size > 0
        if bookTitle(book1) < bookTitle(book2)
      } yield elem(
        qname = QName("BookPair"),
        children = Vector(
          textElem(QName("Title1"), bookTitle(book1)),
          textElem(QName("Title2"), bookTitle(book2)))).build()

    expectResult(5) {
      pairs.size
    }
    expectResult(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book1.build()) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book1.build())).size
    }
    expectResult(3) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book2.build()) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book2.build())).size
    }
    expectResult(2) {
      pairs.filter(pair =>
        pair.getChildElem(EName("Title1")).trimmedText == bookTitle(book3.build()) ||
          pair.getChildElem(EName("Title2")).trimmedText == bookTitle(book3.build())).size
    }
    expectResult(2) {
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
    require(bookstore.localName == "Bookstore")

    import NodeBuilder._

    def books(authorLastName: String) =
      for {
        book <- bookstore \ "Book"
        author <- book.filterElems(EName("Author"))
        if author.getChildElem(EName("Last_Name")).trimmedText == authorLastName
      } yield {
        val attrs = book.attributes filter { case (qn, v) => Set(QName("ISBN"), QName("Price")).contains(qn) }

        val children = book.filterChildElems(EName("Title")) map { e => NodeBuilder.fromElem(e)(book.scope) }

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

    val invertedBookstore: Elem = Elem(qname = QName("InvertedBookstore"), children = authorsWithBooks)

    expectResult(3) {
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

  @Test def testTransformLeavingOutPrices() {
    // Made up example. Here the focus is different: not querying and explicitly mentioning the structure
    // of the query result, but just transforming parts of the XML tree, leaving the remainder of the tree like it is,
    // without having to know about what the rest of the tree exactly looks like. Think XSLT, rather than XQuery.

    // Transforms the XML tree, leaving out book prices

    require(bookstore.localName == "Bookstore")

    def removePrice(book: Elem): Elem = {
      require(book.resolvedName == EName("Book"))
      Elem(
        qname = book.qname,
        attributes = book.attributes filter { case (qn, v) => qn != QName("Price") },
        scope = book.scope,
        children = book.children)
    }

    val bookstoreWithoutPrices: Elem =
      bookstore updated {
        case e if e.resolvedName == EName("Book") => removePrice(e)
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

    import NodeBuilder._

    def combineName(author: Elem): Elem = {
      require(author.resolvedName == EName("Author"))

      val firstNameValue: String = author.getChildElem(EName("First_Name")).trimmedText
      val lastNameValue: String = author.getChildElem(EName("Last_Name")).trimmedText
      val nameValue: String = "%s %s".format(firstNameValue, lastNameValue)
      val name: ElemBuilder = textElem(QName("Name"), nameValue)

      elem(
        qname = author.qname,
        attributes = author.attributes,
        namespaces = Scope.Empty.relativize(author.scope),
        children = Vector(name)).build()
    }

    val bookstoreWithCombinedNames: Elem =
      bookstore updated {
        case e if e.resolvedName == EName("Author") => combineName(e)
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

    import NodeBuilder._

    val updatedBookstoreElm: Elem =
      bookstore updated {
        case e if e.resolvedName == EName("Author") =>
          val newScope = e.scope.resolve(Declarations.from("abc" -> "http://def"))
          val newElmName = QName("abc", e.localName)
          Elem(newElmName, e.attributes, newScope, e.children)
      }

    // Although the partial function is defined for any path containing an Author, only the Author elements are functionally updated!

    expectResult(Set(EName("Bookstore"), EName("Magazine"), EName("Title"), EName("Book"), EName("Authors"),
      EName("{http://def}Author"), EName("First_Name"), EName("Last_Name"), EName("Remark"))) {

      val result = updatedBookstoreElm.findAllElemsOrSelf map { _.resolvedName }
      result.toSet
    }
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
        authorsElm <- bookElm \ "Authors"
        lastNameElm <- authorsElm \\ "Last_Name"
      } yield lastNameElm.text
    val firstAuthorLastName = authorLastNames.head

    expectResult("Hector and Jeff's Database Hints") {
      title
    }
    expectResult("Ullman") {
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

  private val bookstore: Elem = {
    import NodeBuilder._

    elem(
      qname = QName("Bookstore"),
      children = Vector(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)
  }
}
