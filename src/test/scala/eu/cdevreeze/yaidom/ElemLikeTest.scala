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
    require(bookstore.qname.localPart == "Bookstore")

    val bookstoreChildElms: immutable.IndexedSeq[Elem] = bookstore.allChildElems
    val magazineElms: immutable.IndexedSeq[Elem] = bookstore childElemsWhere { e => e.resolvedName == ns.ns.ename("Magazine") }
    val bookElms: immutable.IndexedSeq[Elem] = bookstore.childElems(ns.ns.ename("Book"))
    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore childElemsWhere { e => e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 }

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
    expect(Set(ns.ns.ename("Book"), ns.ns.ename("Magazine"))) {
      val result = bookstoreChildElms map { e => e.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.childElem(ns.ns.ename("Title")).trimmedText
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.childElem(ns.ns.ename("Remark")).trimmedText
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ns.ns.ename("Authors")).childElems(ns.ns.ename("Author"))
      val authorLastNameElms = authorElms map { e => e.childElem(ns.ns.ename("Last_Name")) }
      authorLastNameElms map { e => e.trimmedText } toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ns.ns.ename("Authors")).childElems(ns.ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e.childElemOption(ns.ns.ename("Last_Name")) }
      authorLastNameElms map { e => e.trimmedText } toSet
    }
  }

  @Test def testCollectFromChildElems() {
    require(bookstore.qname.localPart == "Bookstore")

    val bookstoreChildElms: immutable.IndexedSeq[Elem] = bookstore.allChildElems
    val magazineElms: immutable.IndexedSeq[Elem] = bookstore collectFromChildElems {
      case e if e.resolvedName == ns.ns.ename("Magazine") => e
    }
    val bookElms: immutable.IndexedSeq[Elem] = bookstore collectFromChildElems {
      case e if e.resolvedName == ns.ns.ename("Book") => e
    }
    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore collectFromChildElems {
        case e if e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 => e
      }
    val cheapBookPrices: immutable.IndexedSeq[Int] =
      bookstore collectFromChildElems {
        case e if e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 =>
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
    expect(Set(ns.ns.ename("Book"), ns.ns.ename("Magazine"))) {
      val result = bookstoreChildElms map { e => e.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testElems() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.IndexedSeq[Elem] = bookstore.allElems
    val magazineElms: immutable.IndexedSeq[Elem] = bookstore elemsWhere { e => e.resolvedName == ns.ns.ename("Magazine") }
    val bookElms: immutable.IndexedSeq[Elem] = bookstore.elems(ns.ns.ename("Book"))
    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore elemsWhere { e => e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 }

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
    expect(Set(ns.ns.ename("Book"), ns.ns.ename("Magazine"), ns.ns.ename("Title"),
      ns.ns.ename("Authors"), ns.ns.ename("Author"), ns.ns.ename("First_Name"), ns.ns.ename("Last_Name"),
      ns.ns.ename("Remark"))) {
      elms map { e => e.resolvedName } toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      val result = cheapBookElm.elems(ns.ns.ename("Title")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      val result = cheapBookElm.elems(ns.ns.ename("Remark")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ns.ns.ename("Authors")).elems(ns.ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e.elems(ns.ns.ename("Last_Name")) }
      authorLastNameElms map { e => e.trimmedText } toSet
    }
  }

  @Test def testCollectFromElems() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.IndexedSeq[Elem] = bookstore.allElems
    val magazineElms: immutable.IndexedSeq[Elem] = bookstore collectFromElems {
      case e if e.resolvedName == ns.ns.ename("Magazine") => e
    }
    val bookElms: immutable.IndexedSeq[Elem] = bookstore collectFromElems {
      case e if e.resolvedName == ns.ns.ename("Book") => e
    }
    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore collectFromElems {
        case e if e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 => e
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
    expect(Set(ns.ns.ename("Book"), ns.ns.ename("Magazine"), ns.ns.ename("Title"),
      ns.ns.ename("Authors"), ns.ns.ename("Author"), ns.ns.ename("First_Name"), ns.ns.ename("Last_Name"),
      ns.ns.ename("Remark"))) {
      elms map { e => e.resolvedName } toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testElemsOrSelf() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.IndexedSeq[Elem] = bookstore.allElemsOrSelf
    val magazineElms: immutable.IndexedSeq[Elem] = bookstore elemsOrSelfWhere { e => e.resolvedName == ns.ns.ename("Magazine") }
    val bookElms: immutable.IndexedSeq[Elem] = bookstore.elemsOrSelf(ns.ns.ename("Book"))
    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore elemsOrSelfWhere { e => e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 }

    expect(47) {
      elms.size
    }
    expect(47) {
      val elms = bookstore elemsOrSelfWhere { e => e.resolvedName.namespaceUriOption == Some(ns) }
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
    expect(Set(ns.ns.ename("Bookstore"), ns.ns.ename("Book"), ns.ns.ename("Magazine"), ns.ns.ename("Title"),
      ns.ns.ename("Authors"), ns.ns.ename("Author"), ns.ns.ename("First_Name"), ns.ns.ename("Last_Name"),
      ns.ns.ename("Remark"))) {
      elms map { e => e.resolvedName } toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      val result = cheapBookElm.elemsOrSelf(ns.ns.ename("Title")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      val result = cheapBookElm.elemsOrSelf(ns.ns.ename("Remark")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ns.ns.ename("Authors")).elems(ns.ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e.elemsOrSelf(ns.ns.ename("Last_Name")) }
      authorLastNameElms map { e => e.trimmedText } toSet
    }
  }

  @Test def testCollectFromElemsOrSelf() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.IndexedSeq[Elem] = bookstore.allElemsOrSelf
    val magazineElms: immutable.IndexedSeq[Elem] = bookstore collectFromElemsOrSelf {
      case e if e.resolvedName == ns.ns.ename("Magazine") => e
    }
    val bookElms: immutable.IndexedSeq[Elem] = bookstore collectFromElemsOrSelf {
      case e if e.resolvedName == ns.ns.ename("Book") => e
    }
    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore collectFromElemsOrSelf {
        case e if e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 => e
      }

    expect(47) {
      elms.size
    }
    expect(47) {
      val elms = bookstore elemsOrSelfWhere { e => e.resolvedName.namespaceUriOption == Some(ns) }
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
    expect(Set(ns.ns.ename("Bookstore"), ns.ns.ename("Book"), ns.ns.ename("Magazine"), ns.ns.ename("Title"),
      ns.ns.ename("Authors"), ns.ns.ename("Author"), ns.ns.ename("First_Name"), ns.ns.ename("Last_Name"),
      ns.ns.ename("Remark"))) {
      elms map { e => e.resolvedName } toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testFirstElems() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.IndexedSeq[Elem] = bookstore.allElems
    val magazineElms: immutable.IndexedSeq[Elem] = bookstore firstElemsWhere { e => e.resolvedName == ns.ns.ename("Magazine") }
    val bookElms: immutable.IndexedSeq[Elem] = bookstore.firstElems(ns.ns.ename("Book"))
    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore firstElemsWhere { e => e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 }

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
      cheapBookElm.firstElemOption(ns.ns.ename("Title")) map { _.trimmedText } getOrElse (sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.firstElemOption(ns.ns.ename("Remark")) map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm firstElemOptionWhere { e => e.resolvedName == ns.ns.ename("Remark") } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm firstElemOptionWhere { e => e.resolvedName == ns.ns.ename("Remark") && e.allChildElems.isEmpty } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ns.ns.ename("Authors")).firstElems(ns.ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e.firstElemOption(ns.ns.ename("Last_Name")) }
      authorLastNameElms map { e => e.trimmedText } toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ns.ns.ename("Authors")).firstElems(ns.ns.ename("Author"))
      val authorLastNameElms = authorElms flatMap { e => e.firstElems(ns.ns.ename("Last_Name")) }
      authorLastNameElms map { e => e.trimmedText } toSet
    }

    val ullmanAncestors: immutable.IndexedSeq[Elem] =
      cheapBookElm elemsWhere { e => e.allElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }
    val firstUllmanAncestors: immutable.IndexedSeq[Elem] =
      cheapBookElm firstElemsWhere { e => e.allElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }

    expect(3) {
      ullmanAncestors.size
    }
    expect(1) {
      firstUllmanAncestors.size
    }
    expect(Set(ns.ns.ename("Authors"), ns.ns.ename("Author"), ns.ns.ename("Last_Name"))) {
      ullmanAncestors.map(_.resolvedName).toSet
    }
    expect(Set(ns.ns.ename("Authors"))) {
      firstUllmanAncestors map { _.resolvedName } toSet
    }
    assert(firstUllmanAncestors.toSet.subsetOf(ullmanAncestors.toSet))
  }

  @Test def testFindParentInTree() {
    require(bookstore.qname.localPart == "Bookstore")

    val bookElms = bookstore.elems(ns.ns.ename("Book"))

    expect(Set(bookstore)) {
      val result = bookElms map { e => e.findParentInTree(bookstore) }
      result.flatten.toSet
    }

    val lastNameElms = bookstore.elems(ns.ns.ename("Last_Name"))

    expect(Set(ns.ns.ename("Author"))) {
      lastNameElms map { e => e.findParentInTree(bookstore) } flatMap { eOption => eOption map { _.resolvedName } } toSet
    }

    val cheapBookElms: immutable.IndexedSeq[Elem] =
      bookstore firstElemsWhere { e => e.resolvedName == ns.ns.ename("Book") && e.attribute("Price".ename).toInt <= 50 }
    val cheapBookElm: Elem = cheapBookElms(0)
    val cheapBookAuthorElms: immutable.IndexedSeq[Elem] = cheapBookElm.elems(ns.ns.ename("Author"))

    expect(cheapBookAuthorElms.toSet) {
      lastNameElms flatMap { e => e.findParentInTree(cheapBookElm) } toSet
    }
  }

  @Test def testGetIndex() {
    require(bookstore.qname.localPart == "Bookstore")

    val index: Map[ExpandedName, immutable.IndexedSeq[Elem]] = bookstore getIndex { e => e.resolvedName }

    expect {
      val result = bookstore.allElemsOrSelf map { _.resolvedName }
      result.toSet.size
    } {
      index.size
    }

    assert {
      index forall { kv =>
        val ename: ExpandedName = kv._1
        val elms: immutable.IndexedSeq[Elem] = kv._2
        elms forall { e => e.resolvedName == ename }
      }
    }
  }

  @Test def testGetIndexToParent() {
    require(bookstore.qname.localPart == "Bookstore")

    val index: Map[ExpandedName, immutable.IndexedSeq[Elem]] = bookstore getIndexToParent { e => e.resolvedName }

    expect {
      val result = bookstore.allElems map { _.resolvedName }
      result.toSet.size
    } {
      index.size
    }

    expect(true) {
      index forall { kv =>
        val ename: ExpandedName = kv._1
        val elms: immutable.IndexedSeq[Elem] = kv._2
        val childElms: immutable.IndexedSeq[Elem] = elms flatMap { e => e.allChildElems }
        val result = childElms exists { e => e.resolvedName == ename }
        result
      }
    }
  }

  @Test def testFindByElemPath() {
    require(bookstore.qname.localPart == "Bookstore")

    expect(Some(bookstore)) {
      bookstore.findWithElemPath(ElemPath.Root)
    }

    expect(Some("Last_Name".qname)) {
      val path = ElemPath(immutable.IndexedSeq(
        ElemPath.Entry(ns.ns.ename("Book"), 0),
        ElemPath.Entry(ns.ns.ename("Authors"), 0),
        ElemPath.Entry(ns.ns.ename("Author"), 0),
        ElemPath.Entry(ns.ns.ename("Last_Name"), 0)))
      bookstore.findWithElemPath(path) map { _.qname }
    }
    expect(Some("Ullman")) {
      val path = ElemPath(immutable.IndexedSeq(
        ElemPath.Entry(ns.ns.ename("Book"), 0),
        ElemPath.Entry(ns.ns.ename("Authors"), 0),
        ElemPath.Entry(ns.ns.ename("Author"), 0),
        ElemPath.Entry(ns.ns.ename("Last_Name"), 0)))
      bookstore.findWithElemPath(path) map { _.trimmedText }
    }
  }

  private val book1: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-0-13-713526-2", "Price".qname -> "85", "Edition".qname -> "3rd"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(
            text("A First Course in Database Systems"))),
        elem(
          qname = "Authors".qname,
          children = immutable.IndexedSeq(
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Jeffrey"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Ullman"))))),
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Jennifer"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Widom")))))))))
  }

  private val book2: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-0-13-815504-6", "Price".qname -> "100"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(
            text("Database Systems: The Complete Book"))),
        elem(
          qname = "Authors".qname,
          children = immutable.IndexedSeq(
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Hector"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Garcia-Molina"))))),
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Jeffrey"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Ullman"))))),
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Jennifer"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Widom"))))))),
        elem(
          qname = "Remark".qname,
          children = immutable.IndexedSeq(text("Buy this book bundled with \"A First Course\" - a great deal!")))))
  }

  private val book3: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-0-11-222222-3", "Price".qname -> "50"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(
            text("Hector and Jeff's Database Hints"))),
        elem(
          qname = "Authors".qname,
          children = immutable.IndexedSeq(
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Jeffrey"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Ullman"))))),
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Hector"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Garcia-Molina"))))))),
        elem(
          qname = "Remark".qname,
          children = immutable.IndexedSeq(text("An indispensable companion to your textbook")))))
  }

  private val book4: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Book".qname,
      attributes = Map("ISBN".qname -> "ISBN-9-88-777777-6", "Price".qname -> "25"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(
            text("Jennifer's Economical Database Hints"))),
        elem(
          qname = "Authors".qname,
          children = immutable.IndexedSeq(
            elem(
              qname = "Author".qname,
              children = immutable.IndexedSeq(
                elem(
                  qname = "First_Name".qname,
                  children = immutable.IndexedSeq(text("Jennifer"))),
                elem(
                  qname = "Last_Name".qname,
                  children = immutable.IndexedSeq(text("Widom")))))))))
  }

  private val magazine1: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "January", "Year".qname -> "2009"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(text("National Geographic")))))
  }

  private val magazine2: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "February", "Year".qname -> "2009"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(text("National Geographic")))))
  }

  private val magazine3: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "February", "Year".qname -> "2009"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(text("Newsweek")))))
  }

  private val magazine4: ElemBuilder = {
    import NodeBuilder._

    elem(
      qname = "Magazine".qname,
      attributes = Map("Month".qname -> "March", "Year".qname -> "2009"),
      children = immutable.IndexedSeq(
        elem(
          qname = "Title".qname,
          children = immutable.IndexedSeq(text("Hector and Jeff's Database Hints")))))
  }

  private val bookstore: Elem = {
    import NodeBuilder._

    val result: Elem =
      elem(
        qname = "books:Bookstore".qname,
        namespaces = Map("" -> ns, "books" -> ns).namespaces,
        children = immutable.IndexedSeq(
          book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)

    require {
      result.allElemsOrSelf forall { e => e.resolvedName.namespaceUriOption == Option(ns) }
    }
    require(result.qname.prefixOption == Some("books"))
    require {
      result.allElems forall { e => e.qname.prefixOption.isEmpty }
    }
    result
  }
}
