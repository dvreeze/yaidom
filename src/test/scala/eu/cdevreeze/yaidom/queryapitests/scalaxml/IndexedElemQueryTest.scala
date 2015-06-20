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

package eu.cdevreeze.yaidom.queryapitests.scalaxml

import scala.Vector
import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed.IndexedElem
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapitests.AbstractElemLikeQueryTest
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem

/**
 * Query test case for Scala XML wrapper elements wrapped in an IndexedElem.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class IndexedElemQueryTest extends AbstractElemLikeQueryTest {

  final type E = IndexedElem[ScalaXmlElem]

  @Test def testQueryTheBookAuthors(): Unit = {
    require(bookstore.localName == "Bookstore")

    val theBookAuthors =
      for {
        author <- bookstore.filterElems(withLocalName("Author"))
        bookPath <- author.path.findAncestorPath(_.elementNameOption.map(_.localPart) == Some("Book"))
        book <- bookstore.findElem(_.path == bookPath)
        if book.getChildElem(withLocalName("Title")).elem.text.startsWith("A First Course in Database Systems")
      } yield author
    val elems = bookstore.findAllElemsOrSelf

    assertResult(List("Ullman", "Widom")) {
      theBookAuthors.map(_.getChildElem(withLocalName("Last_Name")).text)
    }

    assertResult(List("Jeffrey", "Jennifer")) {
      theBookAuthors.map(_.getChildElem(withLocalName("First_Name")).text)
    }
  }

  protected final val bookstore: E = {
    val scalaElem =
      <Bookstore>
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
      </Bookstore>

    IndexedElem(ScalaXmlElem(scalaElem))
  }

  protected final def toResolvedElem(elem: E): resolved.Elem =
    resolved.Elem(convert.ScalaXmlConversions.convertToElem(elem.elem.wrappedNode))
}
