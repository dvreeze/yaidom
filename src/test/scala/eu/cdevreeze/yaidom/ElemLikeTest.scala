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
import ExpandedName._
import QName._

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

    val bookstoreChildElms: immutable.Seq[Elem] = bookstore.childElems
    val magazineElms: immutable.Seq[Elem] = bookstore.childElems(e => e.resolvedName == ExpandedName(ns, "Magazine"))
    val bookElms: immutable.Seq[Elem] = bookstore.childElems(ExpandedName(ns, "Book"))
    val cheapBookElms: immutable.Seq[Elem] = bookstore.childElems(ExpandedName(ns, "Book"), e => e.attribute("Price".ename).toInt <= 50)

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
    expect(Set(ExpandedName(ns, "Book"), ExpandedName(ns, "Magazine"))) {
      bookstoreChildElms.map(e => e.resolvedName).toSet
    }
    assert(magazineElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(bookElms.toSet.subsetOf(bookstoreChildElms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.childElem(ExpandedName(ns, "Title")).firstTextValue
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.childElem(ExpandedName(ns, "Remark")).firstTextValue
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ExpandedName(ns, "Authors")).childElems(ExpandedName(ns, "Author"))
      val authorLastNameElms = authorElms.map(e => e.childElem(ExpandedName(ns, "Last_Name")))
      authorLastNameElms.map(e => e.firstTextValue).toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ExpandedName(ns, "Authors")).childElems(ExpandedName(ns, "Author"))
      val authorLastNameElms = authorElms.flatMap(e => e.childElemOption(ExpandedName(ns, "Last_Name")))
      authorLastNameElms.map(e => e.firstTextValue).toSet
    }
  }

  @Test def testElems() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.Seq[Elem] = bookstore.elems
    val magazineElms: immutable.Seq[Elem] = bookstore.elems(e => e.resolvedName == ExpandedName(ns, "Magazine"))
    val bookElms: immutable.Seq[Elem] = bookstore.elems(ExpandedName(ns, "Book"))
    val cheapBookElms: immutable.Seq[Elem] = bookstore.elems(ExpandedName(ns, "Book"), e => e.attribute("Price".ename).toInt <= 50)

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
    expect(Set(ExpandedName(ns, "Book"), ExpandedName(ns, "Magazine"), ExpandedName(ns, "Title"),
      ExpandedName(ns, "Authors"), ExpandedName(ns, "Author"), ExpandedName(ns, "First_Name"), ExpandedName(ns, "Last_Name"),
      ExpandedName(ns, "Remark"))) {
      elms.map(e => e.resolvedName).toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.elems(ExpandedName(ns, "Title")).map(_.firstTextValue).headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.elems(ExpandedName(ns, "Remark")).map(_.firstTextValue).headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ExpandedName(ns, "Authors")).elems(ExpandedName(ns, "Author"))
      val authorLastNameElms = authorElms.flatMap(e => e.elems(ExpandedName(ns, "Last_Name")))
      authorLastNameElms.map(e => e.firstTextValue).toSet
    }
  }

  @Test def testElemsOrSelf() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.Seq[Elem] = bookstore.elemsOrSelf
    val magazineElms: immutable.Seq[Elem] = bookstore.elemsOrSelf(e => e.resolvedName == ExpandedName(ns, "Magazine"))
    val bookElms: immutable.Seq[Elem] = bookstore.elemsOrSelf(ExpandedName(ns, "Book"))
    val cheapBookElms: immutable.Seq[Elem] = bookstore.elemsOrSelf(ExpandedName(ns, "Book"), e => e.attribute("Price".ename).toInt <= 50)

    expect(47) {
      elms.size
    }
    expect(47) {
      val elms = bookstore.elemsOrSelf(e => e.resolvedName.namespaceUri == Some(ns))
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
    expect(Set(ExpandedName(ns, "Bookstore"), ExpandedName(ns, "Book"), ExpandedName(ns, "Magazine"), ExpandedName(ns, "Title"),
      ExpandedName(ns, "Authors"), ExpandedName(ns, "Author"), ExpandedName(ns, "First_Name"), ExpandedName(ns, "Last_Name"),
      ExpandedName(ns, "Remark"))) {
      elms.map(e => e.resolvedName).toSet
    }
    assert(magazineElms.toSet.subsetOf(elms.toSet))
    assert(bookElms.toSet.subsetOf(elms.toSet))
    assert(cheapBookElms.toSet.subsetOf(bookElms.toSet))

    val cheapBookElm: Elem = cheapBookElms(0)

    expect("ISBN-0-11-222222-3") {
      cheapBookElm.attribute("ISBN".ename)
    }
    expect("Hector and Jeff's Database Hints") {
      cheapBookElm.elemsOrSelf(ExpandedName(ns, "Title")).map(_.firstTextValue).headOption.getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.elemsOrSelf(ExpandedName(ns, "Remark")).map(_.firstTextValue).headOption.getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ExpandedName(ns, "Authors")).elems(ExpandedName(ns, "Author"))
      val authorLastNameElms = authorElms.flatMap(e => e.elemsOrSelf(ExpandedName(ns, "Last_Name")))
      authorLastNameElms.map(e => e.firstTextValue).toSet
    }
  }

  @Test def testFirstElems() {
    require(bookstore.qname.localPart == "Bookstore")

    val elms: immutable.Seq[Elem] = bookstore.elems
    val magazineElms: immutable.Seq[Elem] = bookstore.firstElems(e => e.resolvedName == ExpandedName(ns, "Magazine"))
    val bookElms: immutable.Seq[Elem] = bookstore.firstElems(ExpandedName(ns, "Book"))
    val cheapBookElms: immutable.Seq[Elem] = bookstore.firstElems(ExpandedName(ns, "Book"), e => e.attribute("Price".ename).toInt <= 50)

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
      cheapBookElm.firstElemOption(ExpandedName(ns, "Title")).map(_.firstTextValue).getOrElse(sys.error("Missing Title"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.firstElemOption(ExpandedName(ns, "Remark")).map(_.firstTextValue).getOrElse(sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.firstElemOption(e => e.resolvedName == ExpandedName(ns, "Remark")).map(_.firstTextValue).getOrElse(sys.error("Missing Remark"))
    }
    expect("An indispensable companion to your textbook") {
      cheapBookElm.firstElemOption(ExpandedName(ns, "Remark"), e => e.childElems.isEmpty).map(_.firstTextValue).getOrElse(sys.error("Missing Remark"))
    }

    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ExpandedName(ns, "Authors")).firstElems(ExpandedName(ns, "Author"))
      val authorLastNameElms = authorElms.flatMap(e => e.firstElemOption(ExpandedName(ns, "Last_Name")))
      authorLastNameElms.map(e => e.firstTextValue).toSet
    }
    expect(Set("Ullman", "Garcia-Molina")) {
      val authorElms = cheapBookElm.childElem(ExpandedName(ns, "Authors")).firstElems(ExpandedName(ns, "Author"))
      val authorLastNameElms = authorElms.flatMap(e => e.firstElems(ExpandedName(ns, "Last_Name")))
      authorLastNameElms.map(e => e.firstTextValue).toSet
    }

    val ullmanAncestors: immutable.Seq[Elem] = cheapBookElm.elems(e => e.elemsOrSelf.exists(e2 => e2.firstTextValueOption == Some("Ullman")))
    val firstUllmanAncestors: immutable.Seq[Elem] = cheapBookElm.firstElems(e => e.elemsOrSelf.exists(e2 => e2.firstTextValueOption == Some("Ullman")))

    expect(3) {
      ullmanAncestors.size
    }
    expect(1) {
      firstUllmanAncestors.size
    }
    expect(Set(ExpandedName(ns, "Authors"), ExpandedName(ns, "Author"), ExpandedName(ns, "Last_Name"))) {
      ullmanAncestors.map(_.resolvedName).toSet
    }
    expect(Set(ExpandedName(ns, "Authors"))) {
      firstUllmanAncestors.map(_.resolvedName).toSet
    }
    assert(firstUllmanAncestors.toSet.subsetOf(ullmanAncestors.toSet))
  }

  @Test def testFindParentInTree() {
    require(bookstore.qname.localPart == "Bookstore")

    val bookElms = bookstore.elems(ExpandedName(ns, "Book"))

    expect(Set(bookstore)) {
      bookElms.map(e => e.findParentInTree(bookstore)).flatten.toSet
    }

    val lastNameElms = bookstore.elems(ExpandedName(ns, "Last_Name"))

    expect(Set(ExpandedName(ns, "Author"))) {
      lastNameElms.map(e => e.findParentInTree(bookstore)).flatMap(eOption => eOption.map(_.resolvedName)).toSet
    }

    val cheapBookElms: immutable.Seq[Elem] = bookstore.firstElems(ExpandedName(ns, "Book"), e => e.attribute("Price".ename).toInt <= 50)
    val cheapBookElm: Elem = cheapBookElms(0)
    val cheapBookAuthorElms: immutable.Seq[Elem] = cheapBookElm.elems(ExpandedName(ns, "Author"))

    expect(cheapBookAuthorElms.toSet) {
      lastNameElms.flatMap(e => e.findParentInTree(cheapBookElm)).toSet
    }
  }

  @Test def testGetIndex() {
    require(bookstore.qname.localPart == "Bookstore")

    val index: Map[ExpandedName, immutable.Seq[Elem]] = bookstore.getIndex(e => e.resolvedName)

    expect(bookstore.elemsOrSelf.map(_.resolvedName).toSet.size) {
      index.size
    }

    assert(index.forall(kv => {
      val ename: ExpandedName = kv._1
      val elms: immutable.Seq[Elem] = kv._2
      elms.forall(e => e.resolvedName == ename)
    }))
  }

  @Test def testGetIndexToParent() {
    require(bookstore.qname.localPart == "Bookstore")

    val index: Map[ExpandedName, immutable.Seq[Elem]] = bookstore.getIndexToParent(e => e.resolvedName)

    expect(bookstore.elems.map(_.resolvedName).toSet.size) {
      index.size
    }

    expect(true) {
      index.forall(kv => {
        val ename: ExpandedName = kv._1
        val elms: immutable.Seq[Elem] = kv._2
        val childElms: immutable.Seq[Elem] = elms.flatMap(e => e.childElems)
        val result = childElms.exists(e => e.resolvedName == ename)
        result
      })
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
        namespaces = Scope.Declarations.fromMap(Map("" -> ns, "books" -> ns)),
        children = List(
          book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)

    require(result.elemsOrSelf.forall(e => e.resolvedName.namespaceUri == Option(ns)))
    require(result.qname.prefixOption == Some("books"))
    require(result.elems.forall(e => e.qname.prefixOption.isEmpty))
    result
  }
}
