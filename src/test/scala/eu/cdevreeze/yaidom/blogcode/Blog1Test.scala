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
 * Code of yaidom blog 1 ("yaidom querying"). The blog uses examples from the coursera course Introduction to Databases,
 * by Jennifer Widom.
 *
 * The blog should show yaidom's strengths (such as leveraging Scala Collections, and namespace handling),
 * and be accessible and interesting to the readers. So it should respect the limited time of the readers.
 *
 * It is assumed that the reader knows the basics of XML (in particular namespaces), knows a bit of Scala
 * (in particular the Scala Collections API), and has some experience with Java XML processing (in particular JAXP).
 *
 * The (code in this) blog shows some yaidom queries, using different element representations. It also shows namespace handling
 * in yaidom, and XML comparisons based on "resolved" elements.
 *
 * Yaidom's namespace support and multiple element representations sharing the same element query API make yaidom
 * unique as a Scala XML library. As for namespaces, the article http://www.lenzconsulting.com/namespaces/ can be
 * illustrated by yaidom examples that tell the same story (since yaidom distinguishes between qualified names and expanded
 * names, just like the article does).
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class Blog1Test extends Suite {

  private val pathToParentDir: java.io.File =
    (new java.io.File(classOf[Blog1Test].getResource("books.xml").toURI)).getParentFile

  /**
   * The code in this test can be copied to the "introduction to yaidom querying" section in the first article on yaidom.
   */
  @Test def testIntroductionToYaidomQuerying(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    val ns = "http://bookstore"

    // Using a yaidom DocumentParser that used DOM internally
    val docParser = parse.DocumentParserUsingDom.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val doc: Document =
      docParser.parse(new File(parentDir, "books.xml"))

    val docElem = doc.documentElement

    val magazineElems =
      docElem.filterChildElems(e => e.localName == "Magazine")

    val bookElems1 =
      docElem.filterChildElems(e => e.localName == "Book")

    val bookElems2 =
      docElem.filterElems(e => e.localName == "Book")

    val bookElems3 =
      docElem.filterElemsOrSelf(e => e.localName == "Book")

    val bookElems4 = docElem \ (e => e.localName == "Book")

    val bookElems5 = docElem \\ (e => e.localName == "Book")
  }

  /**
   * The code in this test can be copied to the "uniform element querying API" section in the first article on yaidom.
   */
  @Test def testUniformElementQueryingApi(): Unit = {
    // Start of section that does not need to be copied again

    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    val ns = "http://bookstore"

    // Using a yaidom DocumentParser that used DOM internally
    val docParser = parse.DocumentParserUsingDom.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val doc: Document =
      docParser.parse(new File(parentDir, "books.xml"))

    val docElem = doc.documentElement

    // End of section that does not need to be copied again

    def findAllBookAuthors[E <: ElemApi[E] with HasText](docElem: E): immutable.IndexedSeq[String] = {
      import ElemApi._
      val result =
        for {
          bookElem <- docElem \ withEName(ns, "Book")
          authorElem <- bookElem \\ withEName(ns, "Author")
          firstNameElem <- authorElem \ withEName(ns, "First_Name")
          lastNameElem <- authorElem \ withEName(ns, "Last_Name")
        } yield s"${firstNameElem.text} ${lastNameElem.text}"
      result.distinct.sorted
    }

    // "Default" elements

    val bookAuthors1 =
      findAllBookAuthors(doc.documentElement)

    val doc2: Document =
      docParser.parse(new File(parentDir, "books2.xml"))

    val rootElem = doc.documentElement
    val rootElem2 = doc2.documentElement

    // Method removeAllInterElementWhitespace makes the equality comparison
    // more robust, because it removes whitespace used for formatting

    require(resolved.Elem(rootElem).removeAllInterElementWhitespace ==
      resolved.Elem(rootElem2).removeAllInterElementWhitespace)

    // "Resolved" elements

    val resolvedDocElem =
      resolved.Elem(doc.documentElement)

    val bookAuthors2 =
      findAllBookAuthors(resolvedDocElem)

    // "DOM wrapper" elements

    // Using a JAXP (DOM) DocumentBuilderFactory
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val d = db.parse(new File(parentDir, "books.xml"))

    val wrapperDoc = dom.DomDocument(d)

    val bookAuthors3 =
      findAllBookAuthors(wrapperDoc.documentElement)

    // "Indexed" elements

    val indexedDoc = indexed.Document(doc)

    val bookAuthors4 =
      findAllBookAuthors(indexedDoc.documentElement)
  }
}
