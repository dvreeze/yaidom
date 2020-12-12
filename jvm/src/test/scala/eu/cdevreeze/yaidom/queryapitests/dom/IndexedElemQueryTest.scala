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

package eu.cdevreeze.yaidom.queryapitests.dom

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.indexed.IndexedClarkElem
import eu.cdevreeze.yaidom.queryapitests.AbstractIndexedElemLikeQueryTest
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Query test case for DOM wrapper elements wrapped in an IndexedClarkElem.
 *
 * @author Chris de Vreeze
 */
class IndexedElemQueryTest extends AbstractIndexedElemLikeQueryTest {

  final type U = DomElem

  private val book1Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "A First Course in Database Systems"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            )
          )
        )
      )
    )
  }

  private val book2Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Database Systems: The Complete Book"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Hector"),
                textElem(QName("Last_Name"), Scope.Empty, "Garcia-Molina"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            )
          )
        ),
        textElem(QName("Remark"), Scope.Empty, "Buy this book bundled with \"A First Course\" - a great deal!")
      )
    )
  }

  private val book3Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Hector and Jeff's Database Hints"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jeffrey"),
                textElem(QName("Last_Name"), Scope.Empty, "Ullman"))
            ),
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Hector"),
                textElem(QName("Last_Name"), Scope.Empty, "Garcia-Molina"))
            )
          )
        ),
        textElem(QName("Remark"), Scope.Empty, "An indispensable companion to your textbook")
      )
    )
  }

  private val book4Builder: Elem = {
    import Node._

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
      scope = Scope.Empty,
      children = Vector(
        textElem(QName("Title"), Scope.Empty, "Jennifer's Economical Database Hints"),
        elem(
          qname = QName("Authors"),
          scope = Scope.Empty,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = Scope.Empty,
              children = Vector(
                textElem(QName("First_Name"), Scope.Empty, "Jennifer"),
                textElem(QName("Last_Name"), Scope.Empty, "Widom"))
            ))
        )
      )
    )
  }

  private val magazine1Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "National Geographic"))
    )
  }

  private val magazine2Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "National Geographic"))
    )
  }

  private val magazine3Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "Newsweek"))
    )
  }

  private val magazine4Builder: Elem = {
    import Node._

    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
      scope = Scope.Empty,
      children = Vector(textElem(QName("Title"), Scope.Empty, "Hector and Jeff's Database Hints"))
    )
  }

  protected final val bookstore: E = {
    import Node._

    val resultElem: Elem =
      elem(
        qname = QName("Bookstore"),
        scope = Scope.Empty,
        children = Vector(
          book1Builder,
          book2Builder,
          book3Builder,
          book4Builder,
          magazine1Builder,
          magazine2Builder,
          magazine3Builder,
          magazine4Builder)
      )
    val resultDoc: Document = Document(resultElem)

    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val domDoc = db.newDocument()

    val indexedElemBuilder = IndexedClarkElem

    indexedElemBuilder(DomElem(convert.DomConversions.convertElem(resultDoc.documentElement)(domDoc)))
  }
}
