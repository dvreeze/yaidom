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
package blogcode

import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Code of yaidom blog 3 ("advanced yaidom querying"). The blog uses examples from the coursera course Introduction to Databases,
 * by Jennifer Widom.
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class Blog3Test extends Suite {

  private val pathToParentDir: java.io.File =
    (new java.io.File(classOf[Blog3Test].getResource("books.xml").toURI)).getParentFile

  /**
   * The code in this test can be copied to the "introductory example" section in the third article on yaidom.
   */
  @Test def testIntroductoryExample(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    val ns = "http://bookstore"

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val doc: Document =
      docParser.parse(new File(parentDir, "books.xml"))

    val docElem = doc.documentElement

    // The example, showing (bottom-up) queries using attributes, paths, and XML literals

    import ElemApi._

    def findIsbnsOfBooksBy(firstName: String, lastName: String): immutable.IndexedSeq[String] = {
      for {
        authorPath <- docElem filterElemPaths withEName(ns, "Author")
        authorElem = docElem.getElemOrSelfByPath(authorPath)
        if authorElem.getChildElem(EName(ns, "First_Name")).text == firstName &&
          authorElem.getChildElem(EName(ns, "Last_Name")).text == lastName
        bookPath <- authorPath findAncestorPath (path => path.endsWithName(EName(ns, "Book")))
        bookElem = docElem.getElemOrSelfByPath(bookPath)
        isbn <- bookElem \@ EName("ISBN")
      } yield isbn
    }

    require(
      findIsbnsOfBooksBy("Jeffrey", "Ullman") ==
        List("ISBN-0-13-713526-2", "ISBN-0-13-815504-6", "ISBN-0-11-222222-3"))

    def summarizeBook(isbn: String): Elem = {
      val bookElem =
        (docElem findElem { e => e.resolvedName == EName(ns, "Book") && (e \@ EName("ISBN")) == Some(isbn) }).get

      val xml =
        <Book xmlns="http://bookstore" isbn={ bookElem.attribute(EName("ISBN")) } title={ bookElem.getChildElem(EName(ns, "Title")).text }>
          <Authors>{
            val authorElems = bookElem.filterElems(withEName(ns, "Author"))
            authorElems.map(e => s"${e.getChildElem(withLocalName("First_Name")).text} ${e.getChildElem(withLocalName("Last_Name")).text}").mkString(", ")
          }</Authors>
        </Book>
      convert.ScalaXmlConversions.convertToElem(xml)
    }

    require(
      summarizeBook("ISBN-0-13-713526-2").getChildElem(withLocalName("Authors")).text ==
        "Jeffrey Ullman, Jennifer Widom")
  }
}
