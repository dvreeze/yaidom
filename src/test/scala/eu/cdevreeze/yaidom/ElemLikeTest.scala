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
 * ElemLike test case.
 *
 * Acknowledgments: The sample XML and original XPath and XQuery queries are part of the online course
 * "Introduction to Databases", by professor Widom at Stanford University. Many thanks for letting me use
 * this material.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ElemLikeTest extends Suite {

  private val ns = "http://bookstore".ns

  @Test def testChildElems() {
    require(bookstore.localName == "Bookstore")

    val bookstoreChildElms = bookstore.allChildElems
    val magazineElms = bookstore \ { _.localName == "Magazine" }
    val bookElms = bookstore \ { _.localName == "Book" }
    val cheapBookElms =
      bookstore filterChildElems { e => e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 }

    expect(8) {
      bookstoreChildElms.size
    }
    expect(4) {
      magazineElms.size
    }
    expect(4) {
      bookElms.size
    }
    expect(2) {
      cheapBookElms.size
    }
    expect(Set(ns.ename("Book"), ns.ename("Magazine"))) {
      val result = bookstoreChildElms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expect(bookstore.allChildElems filter (_.localName == "Magazine")) {
      bookstore \ { _.localName == "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.getChildElemNamed(ns.ename("Title")).trimmedText
    }
    expect("An indispensable companion to your textbook") {
      (cheapBookElm getChildElem (_.localName == "Remark")).trimmedText
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorsElm = cheapBookElm getChildElem (_.localName == "Authors")
      val authorElms = authorsElm \ (_.localName == "Author")
      val authorLastNameElms = authorElms map { e => e getChildElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
      val authorsElm = cheapBookElm getChildElem (_.localName == "Authors")
      val authorElms = authorsElm \ (_.localName == "Author")
      val authorLastNameElms = authorElms flatMap { e => e findChildElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
  }

  @Test def testCollectFromChildElems() {
    require(bookstore.localName == "Bookstore")

    val bookstoreChildElms = bookstore.allChildElems
    val magazineElms = bookstore collectFromChildElems {
      case e if e.localName == "Magazine" => e
    }
    val bookElms = bookstore collectFromChildElems {
      case e if e.localName == "Book" => e
    }
    val cheapBookElms =
      bookstore collectFromChildElems {
        case e if e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 => e
      }
    val cheapBookPrices =
      bookstore collectFromChildElems {
        case e if e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 =>
          e.attribute("Price".ename).toInt
      }

    expect(8) {
      bookstoreChildElms.size
    }
    expect(4) {
      magazineElms.size
    }
    expect(4) {
      bookElms.size
    }
    expect(2) {
      cheapBookElms.size
    }
    expect(Set(25, 50)) {
      cheapBookPrices.toSet
    }
    expect(Set(ns.ename("Book"), ns.ename("Magazine"))) {
      val result = bookstoreChildElms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testElems() {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElems
    val magazineElms = bookstore \\ { _.localName == "Magazine" }
    val bookElms = bookstore.filterElemsNamed(ns.ename("Book"))
    val cheapBookElms =
      bookstore filterElems { e => e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 }

    expect(46) {
      elms.size
    }
    expect(4) {
      magazineElms.size
    }
    expect(4) {
      bookElms.size
    }
    expect(2) {
      cheapBookElms.size
    }
    expect(Set(ns.ename("Book"), ns.ename("Magazine"), ns.ename("Title"),
      ns.ename("Authors"), ns.ename("Author"), ns.ename("First_Name"), ns.ename("Last_Name"),
      ns.ename("Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expect(bookstore.findAllElems filter { _.localName != "Magazine" }) {
      bookstore filterElems { _.localName != "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      val result = cheapBookElm filterElems { _.localName == "Title" } map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      val result = (cheapBookElm \\ (_.localName == "Remark")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm \\ (_.localName == "Author")
      val authorLastNameElms = authorElms flatMap { e => e \\ (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    expect(bookstore.allChildElems flatMap (_.findAllElemsOrSelf)) {
      bookstore.findAllElems
    }
  }

  @Test def testCollectFromElems() {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElems
    val magazineElms = bookstore collectFromElems {
      case e if e.localName == "Magazine" => e
    }
    val bookElms = bookstore collectFromElems {
      case e if e.localName == "Book" => e
    }
    val cheapBookElms =
      bookstore collectFromElems {
        case e if e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 => e
      }

    expect(46) {
      elms.size
    }
    expect(4) {
      magazineElms.size
    }
    expect(4) {
      bookElms.size
    }
    expect(2) {
      cheapBookElms.size
    }
    expect(Set(ns.ename("Book"), ns.ename("Magazine"), ns.ename("Title"),
      ns.ename("Authors"), ns.ename("Author"), ns.ename("First_Name"), ns.ename("Last_Name"),
      ns.ename("Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testElemsOrSelf() {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElemsOrSelf
    val magazineElms = bookstore filterElemsOrSelf { _.localName == "Magazine" }
    val bookElms = bookstore.filterElemsOrSelfNamed(ns.ename("Book"))
    val cheapBookElms =
      bookstore filterElemsOrSelf { e => e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 }

    expect(47) {
      elms.size
    }
    expect(47) {
      val elms = bookstore filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
      elms.size
    }
    expect(4) {
      magazineElms.size
    }
    expect(4) {
      bookElms.size
    }
    expect(2) {
      cheapBookElms.size
    }
    expect(Set(ns.ename("Bookstore"), ns.ename("Book"), ns.ename("Magazine"), ns.ename("Title"),
      ns.ename("Authors"), ns.ename("Author"), ns.ename("First_Name"), ns.ename("Last_Name"),
      ns.ename("Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expect(bookstore.findAllElemsOrSelf filter { _.localName != "Magazine" }) {
      bookstore filterElemsOrSelf { _.localName != "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      val result = cheapBookElm.filterElemsOrSelfNamed(ns.ename("Title")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      val result = cheapBookElm filterElemsOrSelf { _.localName == "Remark" } map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElemNamed(ns.ename("Authors")).filterElemsNamed(ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e filterElemsOrSelf (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    expect(immutable.IndexedSeq(bookstore) ++ (bookstore.allChildElems flatMap (_.findAllElemsOrSelf))) {
      bookstore.findAllElemsOrSelf
    }
  }

  @Test def testCollectFromElemsOrSelf() {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElemsOrSelf
    val magazineElms = bookstore collectFromElemsOrSelf {
      case e if e.localName == "Magazine" => e
    }
    val bookElms = bookstore collectFromElemsOrSelf {
      case e if e.localName == "Book" => e
    }
    val cheapBookElms =
      bookstore collectFromElemsOrSelf {
        case e if e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 => e
      }

    expect(47) {
      elms.size
    }
    expect(47) {
      val elms = bookstore filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
      elms.size
    }
    expect(4) {
      magazineElms.size
    }
    expect(4) {
      bookElms.size
    }
    expect(2) {
      cheapBookElms.size
    }
    expect(Set(ns.ename("Bookstore"), ns.ename("Book"), ns.ename("Magazine"), ns.ename("Title"),
      ns.ename("Authors"), ns.ename("Author"), ns.ename("First_Name"), ns.ename("Last_Name"),
      ns.ename("Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testTopmostElems() {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElems
    val magazineElms = bookstore \\! { _.localName == "Magazine" }
    val bookElms = bookstore.findTopmostElemsNamed(ns.ename("Book"))
    val cheapBookElms =
      bookstore findTopmostElems { e => e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 }

    expect(46) {
      elms.size
    }
    expect(4) {
      magazineElms.size
    }
    expect(4) {
      bookElms.size
    }
    expect(2) {
      cheapBookElms.size
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.findElemNamed(ns.ename("Title")) map { _.trimmedText } getOrElse (sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.findElemNamed(ns.ename("Remark")) map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm findElem { _.localName == "Remark" } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm findElem { e => e.localName == "Remark" && e.allChildElems.isEmpty } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElemNamed(ns.ename("Authors")).findTopmostElemsNamed(ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e findElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElemNamed(ns.ename("Authors")).findTopmostElemsNamed(ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e.findTopmostElemsNamed(ns.ename("Last_Name")) }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    val ullmanAncestors =
      cheapBookElm filterElems { e => e.findAllElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }
    val firstUllmanAncestors =
      cheapBookElm findTopmostElems { e => e.findAllElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }

    expect(3) {
      ullmanAncestors.size
    }
    expect(1) {
      firstUllmanAncestors.size
    }
    expect(Set(ns.ename("Authors"), ns.ename("Author"), ns.ename("Last_Name"))) {
      ullmanAncestors.map(_.resolvedName).toSet
    }
    expect(Set(ns.ename("Authors"))) {
      val result = firstUllmanAncestors map { _.resolvedName }
      result.toSet
    }
    assert(firstUllmanAncestors.toSet.subsetOf(ullmanAncestors.toSet))
  }

  @Test def testFindParentInTree() {
    require(bookstore.localName == "Bookstore")

    val bookElms = bookstore filterElems { _.localName == "Book" }

    val indexedBookstoreDoc = new IndexedDocument(Document(bookstore))

    expect(Set(bookstore)) {
      val result = bookElms map { e => indexedBookstoreDoc.findParent(e) }
      result.flatten.toSet
    }

    val lastNameElms = bookstore filterElems { _.localName == "Last_Name" }

    expect(Set(ns.ename("Author"))) {
      val result = lastNameElms map { e => indexedBookstoreDoc.findParent(e) } flatMap { eOption => eOption map { _.resolvedName } }
      result.toSet
    }

    val cheapBookElms =
      bookstore findTopmostElems { e => e.localName == "Book" && e.attribute("Price".ename).toInt <= 50 }
    val cheapBookElm: Elem = cheapBookElms(0)
    val cheapBookAuthorElms = cheapBookElm filterElems { _.localName == "Author" }

    expect(cheapBookAuthorElms.toSet) {
      val indexedCheapBookDoc = new IndexedDocument(Document(cheapBookElm))

      val result = lastNameElms flatMap { e => indexedCheapBookDoc.findParent(e) }
      result.toSet
    }
  }

  @Test def testGetIndex() {
    require(bookstore.localName == "Bookstore")

    val index: Map[ExpandedName, immutable.IndexedSeq[Elem]] = bookstore getIndex { _.resolvedName }

    expect {
      val result = bookstore.findAllElemsOrSelf map { _.resolvedName }
      result.toSet.size
    } {
      index.size
    }

    assert {
      index forall { kv =>
        val ename: ExpandedName = kv._1
        val elms: immutable.IndexedSeq[Elem] = kv._2
        elms forall { _.resolvedName == ename }
      }
    }
  }

  @Test def testGetIndexByElemPath() {
    require(bookstore.localName == "Bookstore")

    val index: Map[ElemPath, Elem] = bookstore.getIndexByElemPath

    expect(bookstore.findAllElemsOrSelf.size) {
      index.size
    }

    for ((path, elm) <- index) {
      expect(Some(elm)) {
        bookstore.findWithElemPath(path)
      }
    }
  }

  @Test def testFindByElemPath() {
    require(bookstore.localName == "Bookstore")

    expect(Some(bookstore)) {
      bookstore.findWithElemPath(ElemPath.Root)
    }

    val scope = Scope.fromMap(Map("b" -> ns.toString))

    expect(Some("Last_Name".qname)) {
      val path = ElemPath.fromXPaths(List("/b:Book[1]", "/b:Authors[1]", "/b:Author[1]", "/b:Last_Name[1]"))(scope)
      bookstore.findWithElemPath(path) map { _.qname }
    }
    expect(Some("Ullman")) {
      val path = ElemPath.fromXPaths(List("/b:Book[1]", "/b:Authors[1]", "/b:Author[1]", "/b:Last_Name[1]"))(scope)
      bookstore.findWithElemPath(path) map { _.trimmedText }
    }

    val bookstoreChildIndexes = bookstore.allChildElemPathEntries

    expect(8) {
      bookstoreChildIndexes.size
    }
    expect(Set(ns.ename("Book"), ns.ename("Magazine"))) {
      val result = bookstoreChildIndexes map { idx => idx.elementName }
      result.toSet
    }
    expect((0 to 3).toSet) {
      val result = bookstoreChildIndexes map { idx => idx.index }
      result.toSet
    }

    for (idx <- bookstoreChildIndexes) {
      expect(true) {
        bookstore.findWithElemPath(ElemPath(immutable.IndexedSeq(idx))).isDefined
      }
    }
    expect(None) {
      val path = ElemPath.fromXPaths(List("/b:Book[3]", "/b:Title[3]"))(scope)
      bookstore.findWithElemPath(path)
    }
  }

  @Test def testEqualities() {
    require(bookstore.localName == "Bookstore")

    val allElms = bookstore.findAllElemsOrSelf

    expect(47) {
      allElms.size
    }

    val p = (e: Elem) => e.localName == "Last_Name"
    val pf: PartialFunction[Elem, String] = { case e: Elem if e.localName == "Last_Name" => e.trimmedText }

    expect(8) {
      val result = bookstore.filterElems(p)
      result.size
    }

    expect(8) {
      val result = bookstore.collectFromElems(pf)
      result.size
    }

    for (elm <- allElms) {
      expect((elm.allChildElems flatMap (_.findAllElemsOrSelf))) {
        elm.findAllElems
      }

      expect((immutable.IndexedSeq(elm) ++ (elm.allChildElems flatMap (_.findAllElemsOrSelf)))) {
        elm.findAllElemsOrSelf
      }

      expect(elm.allChildElems.filter(p)) {
        elm.filterChildElems(p)
      }

      expect(elm.findAllElems.filter(p)) {
        elm.filterElems(p)
      }

      expect(elm.findAllElemsOrSelf.filter(p)) {
        elm.filterElemsOrSelf(p)
      }

      expect(elm.allChildElems.collect(pf)) {
        elm.collectFromChildElems(pf)
      }

      expect(elm.findAllElems.collect(pf)) {
        elm.collectFromElems(pf)
      }

      expect(elm.findAllElemsOrSelf.collect(pf)) {
        elm.collectFromElemsOrSelf(pf)
      }

      expect(elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p))) {
        elm.findTopmostElems(p)
      }

      expect(if (p(elm)) immutable.IndexedSeq(elm) else (elm.allChildElems flatMap (_.findTopmostElemsOrSelf(p)))) {
        elm.findTopmostElemsOrSelf(p)
      }

      val expectedTopmostElems = {
        elm.filterElems(p) filter { e =>
          val hasNoMatchingAncestor = elm.filterElems(p) forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }

      expect(expectedTopmostElems) {
        elm.findTopmostElems(p)
      }

      val expectedTopmostElemsOrSelf = {
        elm.filterElemsOrSelf(p) filter { e =>
          val hasNoMatchingAncestor = elm.filterElemsOrSelf(p) forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }

      expect(expectedTopmostElemsOrSelf) {
        elm.findTopmostElemsOrSelf(p)
      }

      expect(elm.filterElems(p)) {
        (elm.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p)))
      }

      expect(elm.filterElemsOrSelf(p)) {
        (elm.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p)))
      }

      expect(elm.allChildElems flatMap (_.filterElemsOrSelf(p))) {
        elm.filterElems(p)
      }

      expect((immutable.IndexedSeq(elm).filter(p)) ++ (elm.allChildElems flatMap (_.filterElemsOrSelf(p)))) {
        elm.filterElemsOrSelf(p)
      }

      val ename = "Last_Name".ename
      expect(elm filterElemsOrSelf (_.resolvedName == ename)) {
        elm.filterElemsOrSelfNamed(ename)
      }

      expect(elm.filterElemsOrSelf(p).headOption) {
        elm.findElemOrSelf(p)
      }

      expect(elm.findTopmostElemsOrSelf(p).headOption) {
        elm.findElemOrSelf(p)
      }
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

    val result: Elem =
      elem(
        qname = "books:Bookstore".qname,
        namespaces = Map("" -> ns.toString, "books" -> ns.toString).namespaces,
        children = List(
          book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)

    require {
      result.findAllElemsOrSelf forall { e => e.resolvedName.namespaceUriOption == Option(ns.toString) }
    }
    require(result.qname.prefixOption == Some("books"))
    require {
      result.findAllElems forall { e => e.qname.prefixOption.isEmpty }
    }
    result
  }
}
