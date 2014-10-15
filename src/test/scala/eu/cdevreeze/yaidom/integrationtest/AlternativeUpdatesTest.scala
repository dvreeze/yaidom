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

package eu.cdevreeze.yaidom.integrationtest

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions._
import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.dom
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.defaultelem.Elem

/**
 * Alternative updates test case. It demonstrates many ways of performing functional updates.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AlternativeUpdatesTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @Test def testRetainFirstAuthorsUsingTransformElemsOrSelf(): Unit = {
    val updatedElem = bookstore transformElemsOrSelf {
      case e: Elem if e.localName == "Authors" =>
        val authors = e.filterChildElems(che => che.localName == "Author")
        e.withChildren(authors.take(1))
      case e: Elem => e
    }

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
    }
  }

  @Test def testRetainFirstAuthorsUsingTransformElems(): Unit = {
    val updatedElem = bookstore transformElems {
      case e: Elem if e.localName == "Authors" =>
        val authors = e.filterChildElems(che => che.localName == "Author")
        e.withChildren(authors.take(1))
      case e: Elem => e
    }

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
    }
  }

  @Test def testRetainFirstAuthorsUsingUpdatedAtPathsPassingAllPaths(): Unit = {
    val paths = bookstore.findAllElemOrSelfPaths.toSet

    val updatedElem = bookstore.updatedAtPaths(paths) { (elem, path) =>
      elem match {
        case e: Elem if e.localName == "Authors" =>
          val authors = e.filterChildElems(che => che.localName == "Author")
          e.withChildren(authors.take(1))
        case e: Elem => e
      }
    }

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
    }
  }

  @Test def testRetainFirstAuthorsUsingUpdatedAtPathsPassingSomePaths(): Unit = {
    val paths = bookstore.filterElemPaths(e => e.localName == "Authors").toSet

    val updatedElem = bookstore.updatedAtPaths(paths) { (elem, path) =>
      elem match {
        case e: Elem if e.localName == "Authors" =>
          val authors = e.filterChildElems(che => che.localName == "Author")
          e.withChildren(authors.take(1))
        case e: Elem => e
      }
    }

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
    }
  }

  @Test def testRetainFirstAuthorsUsingUpdatedAtPaths(): Unit = {
    val paths = bookstore.filterElemPaths(e => e.localName == "Authors").toSet

    val updatedElem = bookstore.updatedAtPaths(paths) { (elem, path) =>
      require(elem.localName == "Authors")
      val authors = elem.filterChildElems(che => che.localName == "Author")
      elem.withChildren(authors.take(1))
    }

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
    }
  }

  @Test def testRetainFirstAuthorsUsingUpdatedWithNodeSeqAtPaths(): Unit = {
    val paths = bookstore.filterElemPaths(e => e.localName == "Author").toSet

    val updatedElem = bookstore.updatedWithNodeSeqAtPaths(paths) { (elem, path) =>
      require(elem.localName == "Author" && path.lastEntry.elementName.localPart == "Author")
      if (path.lastEntry.index == 0) Vector(elem) else Vector()
    }

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
    }
  }

  @Test def testRetainFirstAuthorsUsingDom(): Unit = {
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

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
    }
  }

  @Test def testUpdatedAtPathsInternally(): Unit = {
    val paths = bookstore.findAllElemOrSelfPaths.toSet

    var foundPaths = Vector[Path]()
    var foundElemsWithoutChildren = Vector[Elem]()

    val updatedElem = bookstore.updatedAtPaths(paths) { (elem, path) =>
      foundPaths = foundPaths :+ path
      foundElemsWithoutChildren = foundElemsWithoutChildren :+ elem.withChildren(Vector())

      elem match {
        case e: Elem if e.localName == "Authors" =>
          val authors = e.filterChildElems(che => che.localName == "Author")
          e.withChildren(authors.take(1))
        case e: Elem => e
      }
    }

    assertResult(resolved.Elem(bookstoreWithOnlyFirstAuthors.prettify(2))) {
      resolved.Elem(updatedElem.prettify(2))
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
          resolved.Elem(bookstore.getElemOrSelfByPath(path).withChildren(Vector())) == resolved.Elem(elem)
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
