/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.queryapitests

import scala.Vector
import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.PathBuilder
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed.{ Elem => IndexedElem }
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.ElemBuilder
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi

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

  @Test def testChildElems(): Unit = {
    require(bookstore.localName == "Bookstore")

    val bookstoreChildElms = bookstore.findAllChildElems
    val magazineElms = bookstore \ (_.localName == "Magazine")
    val bookElms = bookstore \ (_.localName == "Book")
    val cheapBookElms =
      bookstore filterChildElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    assertResult(8) {
      bookstoreChildElms.size
    }
    assertResult(4) {
      magazineElms.size
    }
    assertResult(4) {
      bookElms.size
    }
    assertResult(2) {
      cheapBookElms.size
    }
    assertResult(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
      val result = bookstoreChildElms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    assertResult(bookstore.findAllChildElems filter (_.localName == "Magazine")) {
      bookstore \ (_.localName == "Magazine")
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    assertResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    assertResult("Hector and Jeff's Database Hints") {
      cheapBookElm.getChildElem(EName(ns, "Title")).trimmedText
    }
    assertResult("An indispensable companion to your textbook") {
      (cheapBookElm getChildElem (_.localName == "Remark")).trimmedText
    }

    assertResult(Set("Ullman", "Garcia-Molina")) {
      val authorsElm = cheapBookElm getChildElem (_.localName == "Authors")
      val authorElms = authorsElm \ (_.localName == "Author")
      val authorLastNameElms = authorElms map { e => e getChildElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    assertResult(Set("Ullman", "Garcia-Molina")) {
      val authorsElm = cheapBookElm getChildElem (_.localName == "Authors")
      val authorElms = authorsElm \ (_.localName == "Author")
      val authorLastNameElms = authorElms flatMap { e => e findChildElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
  }

  @Test def testCollectFromChildElems(): Unit = {
    require(bookstore.localName == "Bookstore")

    val bookstoreChildElms = bookstore.findAllChildElems
    val magazineElms = bookstore.findAllChildElems collect {
      case e if e.localName == "Magazine" => e
    }
    val bookElms = bookstore.findAllChildElems collect {
      case e if e.localName == "Book" => e
    }
    val cheapBookElms =
      bookstore.findAllChildElems collect {
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
      }
    val cheapBookPrices =
      bookstore.findAllChildElems collect {
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 =>
          e.attribute(EName("Price")).toInt
      }

    assertResult(8) {
      bookstoreChildElms.size
    }
    assertResult(4) {
      magazineElms.size
    }
    assertResult(4) {
      bookElms.size
    }
    assertResult(2) {
      cheapBookElms.size
    }
    assertResult(Set(25, 50)) {
      cheapBookPrices.toSet
    }
    assertResult(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
      val result = bookstoreChildElms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testElems(): Unit = {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElems
    val magazineElms = bookstore \\ (_.localName == "Magazine")
    val bookElms = bookstore.filterElems(EName(ns, "Book"))
    val cheapBookElms =
      bookstore filterElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    assertResult(46) {
      elms.size
    }
    assertResult(4) {
      magazineElms.size
    }
    assertResult(4) {
      bookElms.size
    }
    assertResult(2) {
      cheapBookElms.size
    }
    assertResult(Set(EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    assertResult(bookstore.findAllElems filter { _.localName != "Magazine" }) {
      bookstore filterElems { _.localName != "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    assertResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    assertResult("Hector and Jeff's Database Hints") {
      val result = cheapBookElm filterElems { _.localName == "Title" } map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    assertResult("An indispensable companion to your textbook") {
      val result = (cheapBookElm \\ (_.localName == "Remark")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    assertResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm \\ (_.localName == "Author")
      val authorLastNameElms = authorElms flatMap { e => e \\ (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    assertResult(bookstore.findAllChildElems flatMap (_.findAllElemsOrSelf)) {
      bookstore.findAllElems
    }
  }

  @Test def testCollectFromElems(): Unit = {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElems
    val magazineElms = bookstore.findAllElems collect {
      case e if e.localName == "Magazine" => e
    }
    val bookElms = bookstore.findAllElems collect {
      case e if e.localName == "Book" => e
    }
    val cheapBookElms =
      bookstore.findAllElems collect {
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
      }

    assertResult(46) {
      elms.size
    }
    assertResult(4) {
      magazineElms.size
    }
    assertResult(4) {
      bookElms.size
    }
    assertResult(2) {
      cheapBookElms.size
    }
    assertResult(Set(EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testElemsOrSelf(): Unit = {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElemsOrSelf
    val magazineElms = bookstore \\ (_.localName == "Magazine")
    val bookElms = bookstore \\ EName(ns, "Book")
    val cheapBookElms =
      bookstore \\ { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    assertResult(47) {
      elms.size
    }
    assertResult(47) {
      val elms = bookstore \\ { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
      elms.size
    }
    assertResult(4) {
      magazineElms.size
    }
    assertResult(4) {
      bookElms.size
    }
    assertResult(2) {
      cheapBookElms.size
    }
    assertResult(Set(EName(ns, "Bookstore"), EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    assertResult(bookstore.findAllElemsOrSelf filter { _.localName != "Magazine" }) {
      bookstore \\ { _.localName != "Magazine" }
    }

    val cheapBookElm: Elem = cheapBookElms(0)

    assertResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    assertResult("Hector and Jeff's Database Hints") {
      val result = (cheapBookElm \\ EName(ns, "Title")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Title"))
    }
    assertResult("An indispensable companion to your textbook") {
      val result = (cheapBookElm \\ (_.localName == "Remark")) map { _.trimmedText }
      result.headOption.getOrElse(sys.error("Missing Remark"))
    }

    assertResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).filterElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e \\ (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    assertResult(immutable.IndexedSeq(bookstore) ++ (bookstore.findAllChildElems flatMap (_.findAllElemsOrSelf))) {
      bookstore.findAllElemsOrSelf
    }
  }

  @Test def testCollectFromElemsOrSelf(): Unit = {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElemsOrSelf
    val magazineElms = bookstore.findAllElemsOrSelf collect {
      case e if e.localName == "Magazine" => e
    }
    val bookElms = bookstore.findAllElemsOrSelf collect {
      case e if e.localName == "Book" => e
    }
    val cheapBookElms =
      bookstore.findAllElemsOrSelf collect {
        case e if e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 => e
      }

    assertResult(47) {
      elms.size
    }
    assertResult(47) {
      val elms = bookstore \\ { e => e.resolvedName.namespaceUriOption == Some(ns.toString) }
      elms.size
    }
    assertResult(4) {
      magazineElms.size
    }
    assertResult(4) {
      bookElms.size
    }
    assertResult(2) {
      cheapBookElms.size
    }
    assertResult(Set(EName(ns, "Bookstore"), EName(ns, "Book"), EName(ns, "Magazine"), EName(ns, "Title"),
      EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "First_Name"), EName(ns, "Last_Name"),
      EName(ns, "Remark"))) {
      val result = elms map { _.resolvedName }
      result.toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))
  }

  @Test def testTopmostElems(): Unit = {
    require(bookstore.localName == "Bookstore")

    val elms = bookstore.findAllElems
    val magazineElms = bookstore \\! EName(ns, "Magazine")
    val bookElms = bookstore.findTopmostElems(EName(ns, "Book"))
    val cheapBookElms =
      bookstore findTopmostElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }

    assertResult(46) {
      elms.size
    }
    assertResult(4) {
      magazineElms.size
    }
    assertResult(4) {
      bookElms.size
    }
    assertResult(2) {
      cheapBookElms.size
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    assertResult("ISBN-0-11-222222-3") {
      cheapBookElm.attribute(EName("ISBN"))
    }
    assertResult("Hector and Jeff's Database Hints") {
      cheapBookElm.findElem(EName(ns, "Title")) map { _.trimmedText } getOrElse (sys.error("Missing Title"))
    }
    assertResult("An indispensable companion to your textbook") {
      cheapBookElm.findElem(EName(ns, "Remark")) map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    assertResult("An indispensable companion to your textbook") {
      cheapBookElm findElem { _.localName == "Remark" } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }
    assertResult("An indispensable companion to your textbook") {
      cheapBookElm findElem { e => e.localName == "Remark" && e.findAllChildElems.isEmpty } map { _.trimmedText } getOrElse (sys.error("Missing Remark"))
    }

    assertResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).findTopmostElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e findElem (_.localName == "Last_Name") }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }
    assertResult(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.getChildElem(EName(ns, "Authors")).findTopmostElems(EName(ns, "Author"))
      val authorLastNameElms = authorElms flatMap { e => e.findTopmostElems(EName(ns, "Last_Name")) }
      val result = authorLastNameElms map { e => e.trimmedText }
      result.toSet
    }

    val ullmanAncestors =
      cheapBookElm filterElems { e => e.findAllElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }
    val firstUllmanAncestors =
      cheapBookElm findTopmostElems { e => e.findAllElemsOrSelf exists { e2 => e2.trimmedText == "Ullman" } }

    assertResult(3) {
      ullmanAncestors.size
    }
    assertResult(1) {
      firstUllmanAncestors.size
    }
    assertResult(Set(EName(ns, "Authors"), EName(ns, "Author"), EName(ns, "Last_Name"))) {
      ullmanAncestors.map(_.resolvedName).toSet
    }
    assertResult(Set(EName(ns, "Authors"))) {
      val result = firstUllmanAncestors map { _.resolvedName }
      result.toSet
    }
    assert(firstUllmanAncestors.toSet.subsetOf(ullmanAncestors.toSet))
  }

  @Test def testFindParentInTree(): Unit = {
    require(bookstore.localName == "Bookstore")

    val bookElms = bookstore filterElems { _.localName == "Book" }

    assertResult(Set(bookstore)) {
      val paths =
        IndexedElem(bookstore) filterElemsOrSelf { e => bookElms.contains(bookstore.getElemOrSelfByPath(e.path)) } map (_.path)
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: Path) => bookstore.getElemOrSelfByPath(path) }
      result
    }

    val lastNameElms = bookstore filterElems { _.localName == "Last_Name" }

    assertResult(Set(EName(ns, "Author"))) {
      val paths =
        IndexedElem(bookstore) filterElemsOrSelf { e => lastNameElms.contains(bookstore.getElemOrSelfByPath(e.path)) } map (_.path)
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: Path) => bookstore.getElemOrSelfByPath(path) }
      result map { e => e.resolvedName }
    }

    val cheapBookElms =
      bookstore findTopmostElems { e => e.localName == "Book" && e.attribute(EName("Price")).toInt <= 50 }
    val cheapBookElm: Elem = cheapBookElms(0)
    val cheapBookAuthorElms = cheapBookElm filterElems { _.localName == "Author" }

    assertResult(cheapBookAuthorElms.toSet) {
      // Taking cheapBookElm as root! Finding parents of lastNameElms.
      val paths =
        IndexedElem(cheapBookElm) filterElemsOrSelf { e => lastNameElms.contains(cheapBookElm.getElemOrSelfByPath(e.path)) } map (_.path)
      val parentPaths = paths flatMap { _.parentPathOption }
      val result: Set[Elem] = parentPaths.toSet map { (path: Path) => cheapBookElm.getElemOrSelfByPath(path) }
      result
    }
  }

  @Test def testGetIndex(): Unit = {
    require(bookstore.localName == "Bookstore")

    val index: Map[EName, immutable.IndexedSeq[Elem]] = bookstore.findAllElemsOrSelf groupBy { _.resolvedName }

    assertResult {
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

  @Test def testFindByPath(): Unit = {
    require(bookstore.localName == "Bookstore")

    assertResult(Some(bookstore)) {
      bookstore.findElemOrSelfByPath(Path.Root)
    }

    val scope = Scope.from(Map("b" -> ns.toString))

    assertResult(Some(QName("Last_Name"))) {
      val path = PathBuilder.from(QName("b:Book") -> 0, QName("b:Authors") -> 0, QName("b:Author") -> 0, QName("b:Last_Name") -> 0).build(scope)
      bookstore.findElemOrSelfByPath(path) map { _.qname }
    }
    assertResult(Some("Ullman")) {
      val path = PathBuilder.from(QName("b:Book") -> 0, QName("b:Authors") -> 0, QName("b:Author") -> 0, QName("b:Last_Name") -> 0).build(scope)
      bookstore.findElemOrSelfByPath(path) map { _.trimmedText }
    }

    val bookstoreChildIndexes = bookstore.findAllChildElemsWithPathEntries.map(_._2)

    assertResult(8) {
      bookstoreChildIndexes.size
    }
    assertResult(Set(EName(ns, "Book"), EName(ns, "Magazine"))) {
      val result = bookstoreChildIndexes map { idx => idx.elementName }
      result.toSet
    }
    assertResult((0 to 3).toSet) {
      val result = bookstoreChildIndexes map { idx => idx.index }
      result.toSet
    }

    for (idx <- bookstoreChildIndexes) {
      assertResult(true) {
        bookstore.findElemOrSelfByPath(Path(immutable.IndexedSeq(idx))).isDefined
      }
    }
    assertResult(None) {
      val path = PathBuilder.from(QName("b:Book") -> 2, QName("b:Title") -> 2).build(scope)
      bookstore.findElemOrSelfByPath(path)
    }
  }

  @Test def testEqualities(): Unit = {
    require(bookstore.localName == "Bookstore")

    val allElms = bookstore.findAllElemsOrSelf

    assertResult(47) {
      allElms.size
    }

    val p = (e: Elem) => e.localName == "Last_Name"
    val pf: PartialFunction[Elem, String] = { case e: Elem if e.localName == "Last_Name" => e.trimmedText }

    assertResult(8) {
      val result = bookstore.filterElems(p)
      result.size
    }

    assertResult(8) {
      val result = bookstore.findAllElems.collect(pf)
      result.size
    }

    for (elm <- allElms) {
      assertResult((elm.findAllChildElems flatMap (_.findAllElemsOrSelf))) {
        elm.findAllElems
      }

      assertResult((immutable.IndexedSeq(elm) ++ (elm.findAllChildElems flatMap (_.findAllElemsOrSelf)))) {
        elm.findAllElemsOrSelf
      }

      assertResult(elm.findAllChildElems.filter(p)) {
        elm \ p
      }

      assertResult(elm.findAllElems.filter(p)) {
        elm.filterElems(p)
      }

      assertResult(elm.findAllElemsOrSelf.filter(p)) {
        elm \\ p
      }

      assertResult(elm.findAllChildElems.collect(pf)) {
        elm.findAllChildElems.collect(pf)
      }

      assertResult(elm.findAllElems.collect(pf)) {
        elm.findAllElems.collect(pf)
      }

      assertResult(elm.findAllElemsOrSelf.collect(pf)) {
        elm.findAllElemsOrSelf.collect(pf)
      }

      assertResult(elm.findAllChildElems flatMap (_ \\! p)) {
        elm.findTopmostElems(p)
      }

      assertResult(if (p(elm)) immutable.IndexedSeq(elm) else (elm.findAllChildElems flatMap (_ \\! p))) {
        elm \\! p
      }

      val expectedTopmostElems = {
        elm.filterElems(p) filter { e =>
          val hasNoMatchingAncestor = elm.filterElems(p) forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }

      assertResult(expectedTopmostElems) {
        elm.findTopmostElems(p)
      }

      val expectedTopmostElemsOrSelf = {
        elm \\ p filter { e =>
          val hasNoMatchingAncestor = elm \\ p forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }

      assertResult(expectedTopmostElemsOrSelf) {
        elm \\! p
      }

      assertResult(elm.filterElems(p)) {
        (elm.findTopmostElems(p) flatMap (_ \\ p))
      }

      assertResult(elm \\ p) {
        (elm \\! p flatMap (_ \\ p))
      }

      assertResult(elm.findAllChildElems flatMap (_ \\ p)) {
        elm.filterElems(p)
      }

      assertResult((immutable.IndexedSeq(elm).filter(p)) ++ (elm.findAllChildElems flatMap (_ \\ p))) {
        elm \\ p
      }

      val ename = EName("Last_Name")
      assertResult(elm \\ (_.resolvedName == ename)) {
        elm \\ ename
      }

      assertResult((elm \\ p).headOption) {
        elm.findElemOrSelf(p)
      }

      assertResult((elm \\! p).headOption) {
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
