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
import javax.xml.transform.stream.StreamSource
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingDom
import NodeBuilder._
import Node._

/**
 * XML creation test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class CreationTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"

  @Test def testCreation() {
    // 1. Parse XML file into Elem

    val docParser = DocumentParserUsingDom.newInstance()

    val is = classOf[CreationTest].getResourceAsStream("books-with-strange-namespaces.xml")

    val doc1: Document = docParser.parse(is)
    val resolvedRootElm1: resolved.Elem = resolved.Elem(doc1.documentElement)

    val expectedResolvedBookElm: resolved.Elem = {
      import resolved._

      Elem(
        EName("{http://books}Book"),
        Map(EName("ISBN") -> "ISBN-9-88-777777-6", EName("Price") -> "25"),
        Vector(
          Elem(
            EName("{http://books}Title"),
            Map(),
            Vector(Text("Jennifer's Economical Database Hints"))),
          Elem(
            EName("Authors"),
            Map(),
            Vector(
              Elem(
                EName("{http://bookstore}Author"),
                Map(),
                Vector(
                  Elem(
                    EName("{http://ns}First_Name"),
                    Map(),
                    Vector(Text("Jennifer"))),
                  Elem(
                    EName("{http://ns}Last_Name"),
                    Map(),
                    Vector(Text("Widom")))))))))
    }

    expect(Some(expectedResolvedBookElm)) {
      resolvedRootElm1.removeAllInterElementWhitespace findChildElem { e =>
        e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some("ISBN-9-88-777777-6")
      }
    }

    val elm2Builder: ElemBuilder =
      elem(
        qname = QName("books:Book"),
        attributes = Map(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
        namespaces = Scope.Declarations.from("books" -> "http://books", "names" -> "http://names"),
        children = Vector(
          textElem(QName("books:Title"), "Jennifer's Economical Database Hints"),
          elem(
            qname = QName("Authors"),
            namespaces = Scope.Declarations.from("books" -> "", "magazines" -> "http://magazines"),
            children = Vector(
              elem(
                qname = QName("books:Author"),
                namespaces = Scope.Declarations.from("books" -> "http://bookstore", "names" -> "http://ns"),
                children = Vector(
                  textElem(QName("names:First_Name"), "Jennifer"),
                  textElem(QName("names:Last_Name"), "Widom")))))))

    val elm2: Elem = elm2Builder.build()
    val resolvedElm2 = resolved.Elem(elm2)

    expect(expectedResolvedBookElm) {
      resolvedElm2
    }

    val elm3Builder: ElemBuilder =
      elem(
        qname = QName("books:Book"),
        attributes = Map(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
        children = Vector(
          textElem(QName("books:Title"), "Jennifer's Economical Database Hints"),
          elem(
            qname = QName("Authors"),
            namespaces = Scope.Declarations.from("books" -> "", "magazines" -> "http://magazines"),
            children = Vector(
              elem(
                qname = QName("books:Author"),
                namespaces = Scope.Declarations.from("books" -> "http://bookstore", "names" -> "http://ns"),
                children = Vector(
                  textElem(QName("names:First_Name"), "Jennifer"),
                  textElem(QName("names:Last_Name"), "Widom")))))))

    val elm3: Elem = elm3Builder.build(Scope.from("books" -> "http://books", "names" -> "http://names"))
    val resolvedElm3 = resolved.Elem(elm3)

    expect(expectedResolvedBookElm) {
      resolvedElm3
    }

    val elm4: Elem =
      mkElem(
        qname = QName("books:Book"),
        attributes = Map(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
        scope = Scope.from("books" -> "http://books", "names" -> "http://names"),
        children = Vector(
          mkTextElem(
            QName("books:Title"),
            Scope.from("books" -> "http://books", "names" -> "http://names"),
            "Jennifer's Economical Database Hints"),
          mkElem(
            qname = QName("Authors"),
            scope = Scope.from("magazines" -> "http://magazines"),
            children = Vector(
              mkElem(
                qname = QName("books:Author"),
                scope = Scope.from("books" -> "http://bookstore", "names" -> "http://ns", "magazines" -> "http://magazines"),
                children = Vector(
                  mkTextElem(
                    QName("names:First_Name"),
                    Scope.from("books" -> "http://bookstore", "names" -> "http://ns", "magazines" -> "http://magazines"),
                    "Jennifer"),
                  mkTextElem(
                    QName("names:Last_Name"),
                    Scope.from("books" -> "http://bookstore", "names" -> "http://ns", "magazines" -> "http://magazines"),
                    "Widom")))))))

    val resolvedElm4 = resolved.Elem(elm4)

    expect(expectedResolvedBookElm) {
      resolvedElm4
    }
  }
}
