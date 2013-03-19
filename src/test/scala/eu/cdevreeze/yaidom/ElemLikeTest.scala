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

  private val ns = "http://bookstore"

  @Test def testChildElems() {
    require(bookstore.localName == "Bookstore")

    val bookstoreChildElms = bookstore.allChildElems
    val magazineElms = bookstore \ "Magazine"
    val bookElms = bookstore \ "Book"
    val cheapBookElms =
      bookstore filterChildElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    expectResult(8) {
      bookstoreChildElms.size
    }
    expectResult(4) {
      magazineElms.size
    }
    expectResult(4) {
      bookElms.size
    }
    expectResult(2) {
      cheapBookElms.size
    }
    expectResult(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
      val result = bookstoreChildElms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expectResult(bookstore.allChildElems filter (_.localName == "Magazine")) {
      bookstore \ "Magazine"
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expectResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    expectResult("Hector and Jeff's Database Hints") {
      cheapBookElm.getChildElem(EName(ns, "Title")).trimmedText
    }
    expectResult("An indispensable companion to your textbook") {
      (cheapBookElm getChildElem (_.localName == "Remark")).trimmedText
    }

    expectResult(Set("Ullman", "Garcia-Molina")) {
      val authorsElm = cheapBookElm getChildElem (_.localName == "Authors")
      val authorElms = authorsElm \ "Author"
      val authorLastNameElms = authorElms map { e => e getChildElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    expectResult(Set("Ullman", "Garcia-Molina")) {
      val authorsElm = cheapBookElm getChildElem (_.localName == "Authors")
      val authorElms = authorsElm \ "Author"
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
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
      }
    val cheapBookPrices =
      bookstore collectFromChildElems {
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 =>
          e.attribute(EName("Price")).toInt
      }

    expectResult(8) {
      bookstoreChildElms.size
    }
    expectResult(4) {
      magazineElms.size
    }
    expectResult(4) {
      bookElms.size
    }
    expectResult(2) {
      cheapBookElms.size
    }
    expectResult(Set(25, 50)) {
      cheapBookPrices.toSet
    }
    expectResult(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
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
    val magazineElms = bookstore \\ "Magazine"
    val bookElms = bookstore.filterElems(EName(ns, "Book"))
    val cheapBookElms =
      bookstore filterElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    expectResult(46) {
      elms.size
    }
    expectResult(4) {
      magazineElms.size
    }
    expectResult(4) {
      bookElms.size
    }
    expectResult(2) {
      cheapBookElms.size
    }
    expectResult(Set(EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expectResult(bookstore.findAllElems filter { _.localName != "Magazine" }) {
      bookstore filterElems { _.localName != "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expectResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    expectResult("Hector and Jeff's Database Hints") {
      val result = cheapBookElm filterElems { _.localName == "Title" } map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expectResult("An indispensable companion to your textbook") {
      val result = (cheapBookElm \\ "Remark") map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expectResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm \\ "Author"
      val authorLastNameElms = authorElms flatMap { e => e \\ "Last_Name" }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    expectResult(bookstore.allChildElems flatMap (_.findAllElemsOrSelf)) {
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
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
      }

    expectResult(46) {
      elms.size
    }
    expectResult(4) {
      magazineElms.size
    }
    expectResult(4) {
      bookElms.size
    }
    expectResult(2) {
      cheapBookElms.size
    }
    expectResult(Set(EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
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
    val magazineElms = bookstore \\ "Magazine"
    val bookElms = bookstore \\ EName(ns, "Book")
    val cheapBookElms =
      bookstore \\ { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    expectResult(47) {
      elms.size
    }
    expectResult(47) {
      val elms = bookstore \\ { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
      elms.size
    }
    expectResult(4) {
      magazineElms.size
    }
    expectResult(4) {
      bookElms.size
    }
    expectResult(2) {
      cheapBookElms.size
    }
    expectResult(Set(EName(ns, "Bookstore"), EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expectResult(bookstore.findAllElemsOrSelf filter { _.localName != "Magazine" }) {
      bookstore \\ { _.localName != "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expectResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    expectResult("Hector and Jeff's Database Hints") {
      val result = (cheapBookElm \\ EName(ns, "Title")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expectResult("An indispensable companion to your textbook") {
      val result = (cheapBookElm \\ "Remark") map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expectResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).filterElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e \\ "Last_Name" }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    expectResult(immutable.IndexedSeq(bookstore) ++ (bookstore.allChildElems flatMap (_.findAllElemsOrSelf))) {
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
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
      }

    expectResult(47) {
      elms.size
    }
    expectResult(47) {
      val elms = bookstore \\ { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
      elms.size
    }
    expectResult(4) {
      magazineElms.size
    }
    expectResult(4) {
      bookElms.size
    }
    expectResult(2) {
      cheapBookElms.size
    }
    expectResult(Set(EName(ns, "Bookstore"), EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
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
    val magazineElms = bookstore \\! "Magazine"
    val bookElms = bookstore.findTopmostElems(EName(ns, "Book"))
    val cheapBookElms =
      bookstore findTopmostElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    expectResult(46) {
      elms.size
    }
    expectResult(4) {
      magazineElms.size
    }
    expectResult(4) {
      bookElms.size
    }
    expectResult(2) {
      cheapBookElms.size
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expectResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    expectResult("Hector and Jeff's Database Hints") {
      cheapBookElm.findElem(EName(ns, "Title")) map { _.trimmedText } getOrElse (sys.error("Missing Title"))
    }
    expectResult("An indispensable companion to your textbook") {
      cheapBookElm.findElem(EName(ns, "Remark")) map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expectResult("An indispensable companion to your textbook") {
      cheapBookElm findElem { _.localName == "Remark" } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expectResult("An indispensable companion to your textbook") {
      cheapBookElm findElem { e => e.localName == "Remark" && e.allChildElems.isEmpty } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }

    expectResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).findTopmostElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e findElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    expectResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).findTopmostElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e.findTopmostElems(EName(ns, "Last_Name")) }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    val ullmanAncestors =
      cheapBookElm filterElems { e => e.findAllElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }
    val firstUllmanAncestors =
      cheapBookElm findTopmostElems { e => e.findAllElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }

    expectResult(3) {
      ullmanAncestors.size
    }
    expectResult(1) {
      firstUllmanAncestors.size
    }
    expectResult(Set(EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "Last_Name"))) {
      ullmanAncestors.map(_.resolvedName).toSet
    }
    expectResult(Set(EName(ns, "Authors"))) {
      val result = firstUllmanAncestors map { _.resolvedName }
      result.toSet
    }
    assert(firstUllmanAncestors.toSet.subsetOf(ullmanAncestors.toSet))
  }

  @Test def testFindParentInTree() {
    // Actually a test of sub-trait PathAwareElemLike...

    require(bookstore.localName == "Bookstore")

    val bookElms = bookstore filterElems { _.localName == "Book" }

    expectResult(Set(bookstore)) {
      val paths = bookstore.findAllElemOrSelfPaths filter { path => bookElms.contains(bookstore.getWithElemPath(path)) }
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: ElemPath) => bookstore.getWithElemPath(path) }
      result
    }

    val lastNameElms = bookstore filterElems { _.localName == "Last_Name" }

    expectResult(Set(EName(ns, "Author"))) {
      val paths = bookstore.findAllElemOrSelfPaths filter { path => lastNameElms.contains(bookstore.getWithElemPath(path)) }
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: ElemPath) => bookstore.getWithElemPath(path) }
      result map { e => e.resolvedName }
    }

    val cheapBookElms =
      bookstore findTopmostElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }
    val cheapBookElm: Elem = cheapBookElms(0)
    val cheapBookAuthorElms = cheapBookElm filterElems { _.localName == "Author" }

    expectResult(cheapBookAuthorElms.toSet) {
      // Taking cheapBookElm as root! Finding parents of lastNameElms.
      val paths = cheapBookElm.findAllElemOrSelfPaths filter { path => lastNameElms.contains(cheapBookElm.getWithElemPath(path)) }
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: ElemPath) => cheapBookElm.getWithElemPath(path) }
      result
    }
  }

  @Test def testGetIndex() {
    require(bookstore.localName == "Bookstore")

    val index: Map[EName, immutable.IndexedSeq[Elem]] = bookstore getIndex { _.resolvedName }

    expectResult {
      val result = bookstore.findAllElemsOrSelf map { _.resolvedName }
      result.toSet.size
    } {
      index.size
    }

    assert {
      index forall { kv =>
        val ename: EName = kv._1
        val elms: immutable.IndexedSeq[Elem] = kv._2
        elms forall { _.resolvedName == ename }
      }
    }
  }

  @Test def testFindByElemPath() {
    require(bookstore.localName == "Bookstore")

    expectResult(Some(bookstore)) {
      bookstore.findWithElemPath(ElemPath.Root)
    }

    val scope = Scope.from(Map("b" -> ns.toString))

    expectResult(Some(QName("Last_Name"))) {
      val path = ElemPathBuilder.from(QName("b:Book") -> 0, QName("b:Authors") -> 0, QName("b:Author") -> 0, QName("b:Last_Name") -> 0).build(scope)
      bookstore.findWithElemPath(path) map { _.qname }
    }
    expectResult(Some("Ullman")) {
      val path = ElemPathBuilder.from(QName("b:Book") -> 0, QName("b:Authors") -> 0, QName("b:Author") -> 0, QName("b:Last_Name") -> 0).build(scope)
      bookstore.findWithElemPath(path) map { _.trimmedText }
    }

    val bookstoreChildIndexes = bookstore.allChildElemPathEntries

    expectResult(8) {
      bookstoreChildIndexes.size
    }
    expectResult(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
      val result = bookstoreChildIndexes map { idx => idx.elementName }
      result.toSet
    }
    expectResult((0 to 3).toSet) {
      val result = bookstoreChildIndexes map { idx => idx.index }
      result.toSet
    }

    for (idx <- bookstoreChildIndexes) {
      expectResult(true) {
        bookstore.findWithElemPath(ElemPath(immutable.IndexedSeq(idx))).isDefined
      }
    }
    expectResult(None) {
      val path = ElemPathBuilder.from(QName("b:Book") -> 2, QName("b:Title") -> 2).build(scope)
      bookstore.findWithElemPath(path)
    }
  }

  @Test def testEqualities() {
    require(bookstore.localName == "Bookstore")

    val allElms = bookstore.findAllElemsOrSelf

    expectResult(47) {
      allElms.size
    }

    val p = (e: Elem) => e.localName == "Last_Name"
    val pf: PartialFunction[Elem, String] = { case e: Elem if e.localName == "Last_Name" => e.trimmedText }

    expectResult(8) {
      val result = bookstore.filterElems(p)
      result.size
    }

    expectResult(8) {
      val result = bookstore.collectFromElems(pf)
      result.size
    }

    for (elm <- allElms) {
      expectResult((elm.allChildElems flatMap (_.findAllElemsOrSelf))) {
        elm.findAllElems
      }

      expectResult((immutable.IndexedSeq(elm) ++ (elm.allChildElems flatMap (_.findAllElemsOrSelf)))) {
        elm.findAllElemsOrSelf
      }

      expectResult(elm.allChildElems.filter(p)) {
        elm \ p
      }

      expectResult(elm.findAllElems.filter(p)) {
        elm.filterElems(p)
      }

      expectResult(elm.findAllElemsOrSelf.filter(p)) {
        elm \\ p
      }

      expectResult(elm.allChildElems.collect(pf)) {
        elm.collectFromChildElems(pf)
      }

      expectResult(elm.findAllElems.collect(pf)) {
        elm.collectFromElems(pf)
      }

      expectResult(elm.findAllElemsOrSelf.collect(pf)) {
        elm.collectFromElemsOrSelf(pf)
      }

      expectResult(elm.allChildElems flatMap (_ \\! p)) {
        elm.findTopmostElems(p)
      }

      expectResult(if (p(elm)) immutable.IndexedSeq(elm) else (elm.allChildElems flatMap (_ \\! p))) {
        elm \\! p
      }

      val expectedTopmostElems = {
        elm.filterElems(p) filter { e =>
          val hasNoMatchingAncestor = elm.filterElems(p) forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }

      expectResult(expectedTopmostElems) {
        elm.findTopmostElems(p)
      }

      val expectedTopmostElemsOrSelf = {
        elm \\ p filter { e =>
          val hasNoMatchingAncestor = elm \\ p forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }

      expectResult(expectedTopmostElemsOrSelf) {
        elm \\! p
      }

      expectResult(elm.filterElems(p)) {
        (elm.findTopmostElems(p) flatMap (_ \\ p))
      }

      expectResult(elm \\ p) {
        (elm \\! p flatMap (_ \\ p))
      }

      expectResult(elm.allChildElems flatMap (_ \\ p)) {
        elm.filterElems(p)
      }

      expectResult((immutable.IndexedSeq(elm).filter(p)) ++ (elm.allChildElems flatMap (_ \\ p))) {
        elm \\ p
      }

      val ename = EName("Last_Name")
      expectResult(elm \\ (_.resolvedName == ename)) {
        elm \\ ename
      }

      expectResult((elm \\ p).headOption) {
        elm.findElemOrSelf(p)
      }

      expectResult((elm \\! p).headOption) {
        elm.findElemOrSelf(p)
      }
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

    val result: Elem =
      elem(
        qname = QName("books:Bookstore"),
        namespaces = Declarations.from("" -> ns.toString, "books" -> ns.toString),
        children = Vector(
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
