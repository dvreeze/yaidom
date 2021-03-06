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

package eu.cdevreeze.yaidom.integrationtest

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions._
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Elem
import org.scalatest.funsuite.AnyFunSuite

/**
 * Alternative updates test case. It demonstrates many ways of performing functional updates.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
class AlternativeUpdatesTest extends AnyFunSuite {

  test("testRetainFirstAuthorsUsingTransformElemsOrSelf") {
    val updatedElem = bookstore transformElemsOrSelf {
      case e: Elem if e.localName == "Authors" =>
        val authors = e.filterChildElems(che => che.localName == "Author")
        e.withChildren(authors.take(1))
      case e: Elem => e
    }

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }
  }

  test("testRetainFirstAuthorsUsingTransformElems") {
    val updatedElem = bookstore transformElems {
      case e: Elem if e.localName == "Authors" =>
        val authors = e.filterChildElems(che => che.localName == "Author")
        e.withChildren(authors.take(1))
      case e: Elem => e
    }

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }
  }

  test("testRetainFirstAuthorsUsingUpdatedAtPathsPassingAllPaths") {
    val paths = indexed.Elem(bookstore).findAllElemsOrSelf.map(_.path).toSet

    val updatedElem = bookstore.updateElemsOrSelf(paths) { (elem, path) =>
      elem match {
        case e: Elem if e.localName == "Authors" =>
          val authors = e.filterChildElems(che => che.localName == "Author")
          e.withChildren(authors.take(1))
        case e: Elem => e
      }
    }

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }
  }

  test("testRetainFirstAuthorsUsingUpdatedAtPathsPassingSomePaths") {
    // Regression in Scala 2.13.0-M3:
    // Cannot construct a collection of type That with elements of type eu.cdevreeze.yaidom.core.Path based on
    // a collection of type scala.collection.immutable.IndexedSeq[eu.cdevreeze.yaidom.indexed.IndexedScopedNode.Elem[eu.cdevreeze.yaidom.simple.Elem]].
    // Circumventing this compilation error by introducing an extra variable for the indexed.Elem.

    val indexedBookstoreElem = indexed.Elem(bookstore)
    val paths = indexedBookstoreElem.filterElems(e => e.localName == "Authors").map(_.path).toSet

    val updatedElem = bookstore.updateElemsOrSelf(paths) { (elem, path) =>
      elem match {
        case e: Elem if e.localName == "Authors" =>
          val authors = e.filterChildElems(che => che.localName == "Author")
          e.withChildren(authors.take(1))
        case e: Elem => e
      }
    }

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }
  }

  test("testRetainFirstAuthorsUsingUpdatedAtPaths") {
    // Regression in Scala 2.13.0-M3:
    // Cannot construct a collection of type That with elements of type eu.cdevreeze.yaidom.core.Path based on
    // a collection of type scala.collection.immutable.IndexedSeq[eu.cdevreeze.yaidom.indexed.IndexedScopedNode.Elem[eu.cdevreeze.yaidom.simple.Elem]].
    // Circumventing this compilation error by introducing an extra variable for the indexed.Elem.

    val indexedBookstoreElem = indexed.Elem(bookstore)
    val paths = indexedBookstoreElem.filterElems(e => e.localName == "Authors").map(_.path).toSet

    val updatedElem = bookstore.updateElemsOrSelf(paths) { (elem, path) =>
      require(elem.localName == "Authors")
      val authors = elem.filterChildElems(che => che.localName == "Author")
      elem.withChildren(authors.take(1))
    }

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }
  }

  test("testRetainFirstAuthorsUsingUpdatedWithNodeSeqAtPaths") {
    // Regression in Scala 2.13.0-M3:
    // Cannot construct a collection of type That with elements of type eu.cdevreeze.yaidom.core.Path based on
    // a collection of type scala.collection.immutable.IndexedSeq[eu.cdevreeze.yaidom.indexed.IndexedScopedNode.Elem[eu.cdevreeze.yaidom.simple.Elem]].
    // Circumventing this compilation error by introducing an extra variable for the indexed.Elem.

    val indexedBookstoreElem = indexed.Elem(bookstore)
    val paths = indexedBookstoreElem.filterElems(e => e.localName == "Author").map(_.path).toSet

    val updatedElem = bookstore.updateElemsWithNodeSeq(paths) { (elem, path) =>
      require(elem.localName == "Author" && path.lastEntry.elementName.localPart == "Author")
      if (path.lastEntry.index == 0) Vector(elem) else Vector()
    }

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }
  }

  test("testRetainFirstAuthorsUsingDom") {
    val domDoc = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    val domBookstore = convert.DomConversions.convertElem(bookstore, domDoc, Scope.Empty)
    require(domBookstore.getOwnerDocument == domDoc, "Expected the owner document to be the created DOM Document")

    val bookstoreWrapper = new dom.DomElem(domBookstore)

    val nonFirstAuthorWrappers =
      for {
        authorsWrapper <- bookstoreWrapper.filterElems(_.localName == "Authors")
        nonFirstAuthorWrapper <- authorsWrapper.filterChildElems(_.localName == "Author").drop(1)
      } yield nonFirstAuthorWrapper

    for (domAuthor <- nonFirstAuthorWrappers.map(_.wrappedNode)) {
      val domParent = domAuthor.getParentNode
      domParent.removeChild(domAuthor)
    }

    // The state of bookstoreWrapper is its wrapped DOM element, which has been updated
    val updatedElem = convert.DomConversions.convertToElem(bookstoreWrapper.wrappedNode, Scope.Empty)

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }
  }

  test("testUpdatedAtPathsInternally") {
    val paths = indexed.Elem(bookstore).findAllElemsOrSelf.map(_.path).toSet

    var foundPaths = Vector[Path]()
    var foundElemsWithoutChildren = Vector[Elem]()

    val updatedElem = bookstore.updateElemsOrSelf(paths) { (elem, path) =>
      foundPaths = foundPaths :+ path
      foundElemsWithoutChildren = foundElemsWithoutChildren :+ elem.withChildren(Vector())

      elem match {
        case e: Elem if e.localName == "Authors" =>
          val authors = e.filterChildElems(che => che.localName == "Author")
          e.withChildren(authors.take(1))
        case e: Elem => e
      }
    }

    assertResult(resolved.Elem.from(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem.from(updatedElem.prettify(2))
    }

    assertResult(paths) {
      foundPaths.toSet
    }
    assertResult(foundPaths.size) {
      foundElemsWithoutChildren.size
    }
    assertResult(true) {
      foundPaths.zip(foundElemsWithoutChildren) forall {
        case (path, elem) =>
          resolved.Elem.from(bookstore.getElemOrSelfByPath(path).withChildren(Vector())) == resolved.Elem.from(elem)
      }
    }
  }

  private val bookstore: Elem = {
    val scalaXmlElem =
      <books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
        <Book ISBN="ISBN-0-13-713526-2" Price="85" Edition="3rd">
          <Title>A First Course in Database Systems</Title>
          <Authors>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
              <First_Name>Jennifer</First_Name>
              <Last_Name>Widom</Last_Name>
            </Author>
          </Authors>
        </Book>
        <Book ISBN="ISBN-0-13-815504-6" Price="100">
          <Title>Database Systems: The Complete Book</Title>
          <Authors>
            <Author>
              <First_Name>Hector</First_Name>
              <Last_Name>Garcia-Molina</Last_Name>
            </Author>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
              <First_Name>Jennifer</First_Name>
              <Last_Name>Widom</Last_Name>
            </Author>
          </Authors>
          <Remark>
            Buy this book bundled with "A First Course" - a great deal!
          </Remark>
        </Book>
        <Book ISBN="ISBN-0-11-222222-3" Price="50">
          <Title>Hector and Jeff's Database Hints</Title>
          <Authors>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
            <Author>
              <First_Name>Hector</First_Name>
              <Last_Name>Garcia-Molina</Last_Name>
            </Author>
          </Authors>
          <Remark>An indispensable companion to your textbook</Remark>
        </Book>
        <Book ISBN="ISBN-9-88-777777-6" Price="25">
          <Title>Jennifer's Economical Database Hints</Title>
          <Authors>
            <Author>
              <First_Name>Jennifer</First_Name>
              <Last_Name>Widom</Last_Name>
            </Author>
          </Authors>
        </Book>
        <Magazine Month="January" Year="2009">
          <Title>National Geographic</Title>
        </Magazine>
        <Magazine Month="February" Year="2009">
          <Title>National Geographic</Title>
        </Magazine>
        <Magazine Month="February" Year="2009">
          <Title>Newsweek</Title>
        </Magazine>
        <Magazine Month="March" Year="2009">
          <Title>Hector and Jeff's Database Hints</Title>
        </Magazine>
      </books:Bookstore>

    convertToElem(scalaXmlElem)
  }

  private val bookstoreWithOnlyFirstAuthors: Elem = {
    val scalaXmlElem =
      <books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
        <Book ISBN="ISBN-0-13-713526-2" Price="85" Edition="3rd">
          <Title>A First Course in Database Systems</Title>
          <Authors>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
          </Authors>
        </Book>
        <Book ISBN="ISBN-0-13-815504-6" Price="100">
          <Title>Database Systems: The Complete Book</Title>
          <Authors>
            <Author>
              <First_Name>Hector</First_Name>
              <Last_Name>Garcia-Molina</Last_Name>
            </Author>
          </Authors>
          <Remark>
            Buy this book bundled with "A First Course" - a great deal!
          </Remark>
        </Book>
        <Book ISBN="ISBN-0-11-222222-3" Price="50">
          <Title>Hector and Jeff's Database Hints</Title>
          <Authors>
            <Author>
              <First_Name>Jeffrey</First_Name>
              <Last_Name>Ullman</Last_Name>
            </Author>
          </Authors>
          <Remark>An indispensable companion to your textbook</Remark>
        </Book>
        <Book ISBN="ISBN-9-88-777777-6" Price="25">
          <Title>Jennifer's Economical Database Hints</Title>
          <Authors>
            <Author>
              <First_Name>Jennifer</First_Name>
              <Last_Name>Widom</Last_Name>
            </Author>
          </Authors>
        </Book>
        <Magazine Month="January" Year="2009">
          <Title>National Geographic</Title>
        </Magazine>
        <Magazine Month="February" Year="2009">
          <Title>National Geographic</Title>
        </Magazine>
        <Magazine Month="February" Year="2009">
          <Title>Newsweek</Title>
        </Magazine>
        <Magazine Month="March" Year="2009">
          <Title>Hector and Jeff's Database Hints</Title>
        </Magazine>
      </books:Bookstore>

    convertToElem(scalaXmlElem)
  }
}
