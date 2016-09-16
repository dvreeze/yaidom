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

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.resolved

/**
 * QName-based query test.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScopedQueryTest extends FunSuite with BeforeAndAfterAll {

  test("testQueryBooks") {
    val docParser = DocumentParserUsingSax.newInstance()

    val doc = docParser.parse(classOf[ScopedQueryTest].getResourceAsStream("books-with-strange-namespaces.xml"))

    val scope = Scope.from("books" -> "http://bookstore")
    import scope._

    val bookElems = doc.documentElement \\ withEName(QName("books", "Book").res)

    assertResult(2) {
      bookElems.size
    }

    val expectedBookElems = doc.documentElement \\ withEName("http://bookstore", "Book")

    assertResult(expectedBookElems.map(e => resolved.Elem(e))) {
      bookElems.map(e => resolved.Elem(e))
    }

    val scope2 = Scope.from("books" -> "http://books")

    val bookElems2 = {
      import scope2._

      doc.documentElement \\ withEName(QName("books", "Book").res)
    }

    assertResult(1) {
      bookElems2.size
    }

    val expectedBookElems2 = doc.documentElement \\ withEName("http://books", "Book")

    assertResult(expectedBookElems2.map(e => resolved.Elem(e))) {
      bookElems2.map(e => resolved.Elem(e))
    }
  }

  test("testQueryBookAuthors") {
    val docParser = DocumentParserUsingSax.newInstance()

    val doc = docParser.parse(classOf[ScopedQueryTest].getResourceAsStream("books-with-strange-namespaces.xml"))

    val scope = Scope.from("books" -> "http://bookstore")

    val authorElems = {
      import scope._

      for {
        bookElem <- doc.documentElement \\ (QName("books", "Book").res)
        authorElem <- bookElem \\ (QName("books", "Author").res)
        lastNameElem <- authorElem \ (QName("books", "Last_Name").res)
      } yield lastNameElem
    }

    val authors = authorElems.map(_.text).toSet

    assertResult(Set("Ullman", "Widom", "Garcia-Molina")) {
      authors
    }
  }

  test("testQueryBookAuthorsAgain") {
    val docParser = DocumentParserUsingSax.newInstance()

    val doc = docParser.parse(classOf[ScopedQueryTest].getResourceAsStream("books-with-strange-namespaces.xml"))

    val scope = Scope.from("" -> "http://bookstore")

    val authorElems = {
      import scope._

      for {
        bookElem <- doc.documentElement \\ (QName("Book").res)
        authorElem <- bookElem \\ (QName("Author").res)
        lastNameElem <- authorElem \ (QName("Last_Name").res)
      } yield lastNameElem
    }

    val authors = authorElems.map(_.text).toSet

    assertResult(Set("Ullman", "Widom", "Garcia-Molina")) {
      authors
    }
  }
}
