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
    expect(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
      val result = bookstoreChildElms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expect(bookstore.allChildElems filter (_.localName == "Magazine")) {
      bookstore \ "Magazine"
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.getChildElem(EName(ns, "Title")).trimmedText
    }
    expect("An indispensable companion to your textbook") {
      (cheapBookElm getChildElem (_.localName == "Remark")).trimmedText
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorsElm = cheapBookElm getChildElem (_.localName == "Authors")
      val authorElms = authorsElm \ "Author"
      val authorLastNameElms = authorElms map { e => e getChildElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
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
    expect(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
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
    expect(Set(EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
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
      cheapBookElm.attribute(EName("ISBN"))
    }
    expect("Hector and Jeff's Database Hints") {
      val result = cheapBookElm filterElems { _.localName == "Title" } map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      val result = (cheapBookElm \\ "Remark") map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm \\ "Author"
      val authorLastNameElms = authorElms flatMap { e => e \\ "Last_Name" }
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
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
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
    expect(Set(EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
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

    expect(47) {
      elms.size
    }
    expect(47) {
      val elms = bookstore \\ { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
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
    expect(Set(EName(ns, "Bookstore"), EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    expect(bookstore.findAllElemsOrSelf filter { _.localName != "Magazine" }) {
      bookstore \\ { _.localName != "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    expect("Hector and Jeff's Database Hints") {
      val result = (cheapBookElm \\ EName(ns, "Title")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      val result = (cheapBookElm \\ "Remark") map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).filterElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e \\ "Last_Name" }
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
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
      }

    expect(47) {
      elms.size
    }
    expect(47) {
      val elms = bookstore \\ { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
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
    expect(Set(EName(ns, "Bookstore"), EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
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
      cheapBookElm.attribute(EName("ISBN"))
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.findElem(EName(ns, "Title")) map { _.trimmedText } getOrElse (sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.findElem(EName(ns, "Remark")) map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm findElem { _.localName == "Remark" } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm findElem { e => e.localName == "Remark" && e.allChildElems.isEmpty } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).findTopmostElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e findElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).findTopmostElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e.findTopmostElems(EName(ns, "Last_Name")) }
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
    expect(Set(EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "Last_Name"))) {
      ullmanAncestors.map(_.resolvedName).toSet
    }
    expect(Set(EName(ns, "Authors"))) {
      val result = firstUllmanAncestors map { _.resolvedName }
      result.toSet
    }
    assert(firstUllmanAncestors.toSet.subsetOf(ullmanAncestors.toSet))
  }

  @Test def testFindParentInTree() {
    // Actually a test of sub-trait PathAwareElemLike...

    require(bookstore.localName == "Bookstore")

    val bookElms = bookstore filterElems { _.localName == "Book" }

    expect(Set(bookstore)) {
      val paths = bookstore.findAllElemOrSelfPaths filter { path => bookElms.contains(bookstore.getWithElemPath(path)) }
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: ElemPath) => bookstore.getWithElemPath(path) }
      result
    }

    val lastNameElms = bookstore filterElems { _.localName == "Last_Name" }

    expect(Set(EName(ns, "Author"))) {
      val paths = bookstore.findAllElemOrSelfPaths filter { path => lastNameElms.contains(bookstore.getWithElemPath(path)) }
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: ElemPath) => bookstore.getWithElemPath(path) }
      result map { e => e.resolvedName }
    }

    val cheapBookElms =
      bookstore findTopmostElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }
    val cheapBookElm: Elem = cheapBookElms(0)
    val cheapBookAuthorElms = cheapBookElm filterElems { _.localName == "Author" }

    expect(cheapBookAuthorElms.toSet) {
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

    expect {
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

    expect(Some(bookstore)) {
      bookstore.findWithElemPath(ElemPath.Root)
    }

    val scope = Scope.from(Map("b" -> ns.toString))

    expect(Some(QName("Last_Name"))) {
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
    expect(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
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
        elm \ p
      }

      expect(elm.findAllElems.filter(p)) {
        elm.filterElems(p)
      }

      expect(elm.findAllElemsOrSelf.filter(p)) {
        elm \\ p
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

      expect(elm.allChildElems flatMap (_ \\! p)) {
        elm.findTopmostElems(p)
      }

      expect(if (p(elm)) immutable.IndexedSeq(elm) else (elm.allChildElems flatMap (_ \\! p))) {
        elm \\! p
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
        elm \\ p filter { e =>
          val hasNoMatchingAncestor = elm \\ p forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }

      expect(expectedTopmostElemsOrSelf) {
        elm \\! p
      }

      expect(elm.filterElems(p)) {
        (elm.findTopmostElems(p) flatMap (_ \\ p))
      }

      expect(elm \\ p) {
        (elm \\! p flatMap (_ \\ p))
      }

      expect(elm.allChildElems flatMap (_ \\ p)) {
        elm.filterElems(p)
      }

      expect((immutable.IndexedSeq(elm).filter(p)) ++ (elm.allChildElems flatMap (_ \\ p))) {
        elm \\ p
      }

      val ename = EName("Last_Name")
      expect(elm \\ (_.resolvedName == ename)) {
        elm \\ ename
      }

      expect((elm \\ p).headOption) {
        elm.findElemOrSelf(p)
      }

      expect((elm \\! p).headOption) {
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
