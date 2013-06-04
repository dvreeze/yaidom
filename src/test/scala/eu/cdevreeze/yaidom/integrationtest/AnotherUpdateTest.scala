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
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import javax.xml.transform.{ TransformerFactory, Transformer }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingDom
import print.DocumentPrinterUsingDom
import NodeBuilder._

/**
 * Another XML functional update test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AnotherUpdateTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val docParser = DocumentParserUsingDom.newInstance()

  @Test def testDeleteMagazines() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val deleteMags: PartialFunction[Elem, Vector[Node]] = {
      case elem: Elem if elem.localName == "Magazine" => Vector[Node]()
    }

    val docWithoutMags: Document = doc.updatedWithNodeSeq(deleteMags)

    expectResult(doc.documentElement.findAllChildElems.size - 4) {
      docWithoutMags.documentElement.findAllChildElems.size
    }

    expectResult(doc.documentElement.filterElemsOrSelf(_.localName == "Book").size) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Book").size
    }

    expectResult(0) {
      docWithoutMags.documentElement.filterElemsOrSelf(_.localName == "Magazine").size
    }

    val deleteMagsResolved: PartialFunction[resolved.Elem, Vector[resolved.Node]] = {
      case elem: resolved.Elem if elem.resolvedName.localPart == "Magazine" => Vector[resolved.Node]()
    }

    expectResult(resolved.Elem(docWithoutMags.documentElement)) {
      resolved.Elem(doc.documentElement).updatedWithNodeSeq(deleteMagsResolved)
    }
  }

  @Test def testInsertAfter() {
    val is = classOf[AnotherUpdateTest].getResourceAsStream("books.xml")

    val doc: Document = docParser.parse(is)

    val newBook = {
      import NodeBuilder._

      val eb =
        elem(
          qname = QName("Book"),
          attributes = Vector(QName("ISBN") -> "978-0981531649", QName("Price") -> "37", QName("Edition") -> "2nd"),
          namespaces = Declarations.from("" -> "http://bookstore"),
          children = Vector(
            textElem(QName("Title"), "Programming in Scala: A Comprehensive Step-by-Step Guide"),
            elem(
              qname = QName("Authors"),
              children = Vector(
                elem(
                  qname = QName("Author"),
                  children = Vector(
                    textElem(QName("First_Name"), "Martin"),
                    textElem(QName("Last_Name"), "Odersky"))),
                elem(
                  qname = QName("Author"),
                  children = Vector(
                    textElem(QName("First_Name"), "Lex"),
                    textElem(QName("Last_Name"), "Spoon"))),
                elem(
                  qname = QName("Author"),
                  children = Vector(
                    textElem(QName("First_Name"), "Bill"),
                    textElem(QName("Last_Name"), "Venners")))))))

      eb.build(doc.documentElement.scope)
    }

    val lastBookPath: ElemPath = doc.documentElement.filterChildElemPaths(_.localName == "Book").last

    val docWithScalaBook: Document = {
      val result = doc.updatedWithNodeSeq(lastBookPath) { e => Vector(e, newBook) }
      result.withDocumentElement(result.documentElement.prettify(4))
    }

    val newLastBookPath: ElemPath = docWithScalaBook.documentElement.filterChildElemPaths(_.localName == "Book").last
    val newLastBook: Elem = docWithScalaBook.documentElement.getWithElemPath(newLastBookPath)

    expectResult("Programming in Scala") {
      (newLastBook \ (_.localName == "Title")).head.text.take("Programming in Scala".length)
    }

    val docWithoutNonDatabaseBook: Document = {
      val deleteNonDatabaseBook: PartialFunction[Elem, Vector[Node]] = {
        case e: Elem if e.localName == "Book" && (e \ (_.localName == "Title")).head.text.contains("Scala") => Vector[Node]()
      }

      docWithScalaBook.updatedWithNodeSeq(deleteNonDatabaseBook)
    }

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(docWithoutNonDatabaseBook.documentElement).removeAllInterElementWhitespace
    }
  }
}
