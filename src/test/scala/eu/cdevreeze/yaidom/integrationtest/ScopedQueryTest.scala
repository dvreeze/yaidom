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
package integrationtest

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll, Ignore }
import org.scalatest.junit.JUnitRunner
import ElemApi._
import convert.ScalaXmlConversions._
import parse._
import print._

/**
 * QName-based query test.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScopedQueryTest extends Suite with BeforeAndAfterAll {

  @Test def testQueryBooks(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance()

    val doc = docParser.parse(classOf[ScopedQueryTest].getResourceAsStream("books-with-strange-namespaces.xml"))

    val usedScope = Scope.from("books" -> "http://bookstore")

    val scoped = Scoped(usedScope)
    import scoped._

    val bookElems = doc.documentElement \\ withEName(QName("books", "Book").e)

    expectResult(2) {
      bookElems.size
    }

    val expectedBookElems = doc.documentElement \\ withEName("http://bookstore", "Book")

    expectResult(expectedBookElems.map(e => resolved.Elem(e))) {
      bookElems.map(e => resolved.Elem(e))
    }

    val usedScope2 = Scope.from("books" -> "http://books")

    val scoped2 = Scoped(usedScope2)

    val bookElems2 = {
      import scoped2._

      doc.documentElement \\ withEName(QName("books", "Book").e)
    }

    expectResult(1) {
      bookElems2.size
    }

    val expectedBookElems2 = doc.documentElement \\ withEName("http://books", "Book")

    expectResult(expectedBookElems2.map(e => resolved.Elem(e))) {
      bookElems2.map(e => resolved.Elem(e))
    }
  }
}
