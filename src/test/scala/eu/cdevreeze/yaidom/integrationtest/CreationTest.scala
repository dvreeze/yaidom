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

/**
 * XML creation test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class CreationTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  private val nsBookstore = "http://bookstore"

  @Test def testCreation(): Unit = {
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

    expectResult(Some(expectedResolvedBookElm)) {
      resolvedRootElm1.removeAllInterElementWhitespace findChildElem { e =>
        e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some("ISBN-9-88-777777-6")
      }
    }

    val elm2Builder: ElemBuilder =
      elem(
        qname = QName("books:Book"),
        attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
        namespaces = Declarations.from("books" -> "http://books", "names" -> "http://names"),
        children = Vector(
          textElem(QName("books:Title"), "Jennifer's Economical Database Hints"),
          elem(
            qname = QName("Authors"),
            namespaces = Declarations.from("books" -> "", "magazines" -> "http://magazines"),
            children = Vector(
              elem(
                qname = QName("books:Author"),
                namespaces = Declarations.from("books" -> "http://bookstore", "names" -> "http://ns"),
                children = Vector(
                  textElem(QName("names:First_Name"), "Jennifer"),
                  textElem(QName("names:Last_Name"), "Widom")))))))

    val elm2: Elem = elm2Builder.build()
    val resolvedElm2 = resolved.Elem(elm2)

    expectResult(expectedResolvedBookElm) {
      resolvedElm2
    }

    expectResult(Set()) {
      elm2Builder.nonDeclaredPrefixes(Scope.Empty)
    }
    assert(elm2Builder.canBuild(Scope.Empty))

    expectResult(false) {
      elm2Builder.allDeclarationsAreAtTopLevel
    }

    val elm3Builder: ElemBuilder =
      elem(
        qname = QName("books:Book"),
        attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
        children = Vector(
          textElem(QName("books:Title"), "Jennifer's Economical Database Hints"),
          elem(
            qname = QName("Authors"),
            namespaces = Declarations.from("books" -> "", "magazines" -> "http://magazines"),
            children = Vector(
              elem(
                qname = QName("books:Author"),
                namespaces = Declarations.from("books" -> "http://bookstore", "names" -> "http://ns"),
                children = Vector(
                  textElem(QName("names:First_Name"), "Jennifer"),
                  textElem(QName("names:Last_Name"), "Widom")))))))

    val prefixesUsed: Set[String] = {
      elm3Builder.findAllElemsOrSelf.foldLeft(Set[String]()) { (acc, elemBuilder) =>
        val qnames: Set[QName] = (elemBuilder.attributes.toMap.keySet) + elemBuilder.qname
        val prefixes: Set[String] = qnames flatMap { qname => qname.prefixOption }
        acc ++ prefixes.toSet
      }
    }

    expectResult(Set("books", "names")) {
      prefixesUsed
    }

    expectResult(Set("books")) {
      elm3Builder.nonDeclaredPrefixes(Scope.Empty)
    }
    expectResult(Set()) {
      elm3Builder.nonDeclaredPrefixes(Scope.from("books" -> "http://bookstore"))
    }
    assert(!elm3Builder.canBuild(Scope.Empty))
    assert(elm3Builder.canBuild(Scope.from("books" -> "http://bookstore")))

    expectResult(false) {
      elm3Builder.allDeclarationsAreAtTopLevel
    }

    val elm3: Elem = elm3Builder.build(Scope.from("books" -> "http://books"))
    val resolvedElm3 = resolved.Elem(elm3)

    expectResult(expectedResolvedBookElm) {
      resolvedElm3
    }

    val elm4: Elem = {
      import Node._

      elem(
        qname = QName("books:Book"),
        attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
        scope = Scope.from("books" -> "http://books", "names" -> "http://names"),
        children = Vector(
          textElem(
            QName("books:Title"),
            Scope.from("books" -> "http://books", "names" -> "http://names"),
            "Jennifer's Economical Database Hints"),
          elem(
            qname = QName("Authors"),
            scope = Scope.from("magazines" -> "http://magazines"),
            children = Vector(
              elem(
                qname = QName("books:Author"),
                scope = Scope.from("books" -> "http://bookstore", "names" -> "http://ns", "magazines" -> "http://magazines"),
                children = Vector(
                  textElem(
                    QName("names:First_Name"),
                    Scope.from("books" -> "http://bookstore", "names" -> "http://ns", "magazines" -> "http://magazines"),
                    "Jennifer"),
                  textElem(
                    QName("names:Last_Name"),
                    Scope.from("books" -> "http://bookstore", "names" -> "http://ns", "magazines" -> "http://magazines"),
                    "Widom")))))))
    }

    val resolvedElm4 = resolved.Elem(elm4)

    expectResult(expectedResolvedBookElm) {
      resolvedElm4
    }
  }

  @Test def testNotUndeclaringPrefixes(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance()

    val is = classOf[CreationTest].getResourceAsStream("books-with-strange-namespaces.xml")

    val doc1: Document = docParser.parse(is)
    val resolvedRootElm1: resolved.Elem = resolved.Elem(doc1.documentElement)

    val isbn = "ISBN-9-88-777777-6"
    val bookElm1 = doc1.documentElement.findElem(e =>
      e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some(isbn)).getOrElse(sys.error("No book with ISBN %s".format(isbn)))
    val authorsElm1 = bookElm1.getChildElem(_.localName == "Authors")

    val doc2: Document = Document(doc1.documentElement.notUndeclaringPrefixes(Scope.Empty))
    val bookElm2 = doc2.documentElement.findElem(e =>
      e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some(isbn)).getOrElse(sys.error("No book with ISBN %s".format(isbn)))
    val authorsElm2 = bookElm2.getChildElem(_.localName == "Authors")

    val doc3: Document = Document(doc1.documentElement.notUndeclaringPrefixes(Scope.from("books" -> "http://bookstore")))
    val bookElm3 = doc3.documentElement.findElem(e =>
      e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some(isbn)).getOrElse(sys.error("No book with ISBN %s".format(isbn)))
    val authorsElm3 = bookElm3.getChildElem(_.localName == "Authors")

    val doc4: Document = Document(doc1.documentElement.notUndeclaringPrefixes(Scope.from("books" -> "http://abc")))
    val bookElm4 = doc4.documentElement.findElem(e =>
      e.localName == "Book" && e.attributeOption(EName("ISBN")) == Some(isbn)).getOrElse(sys.error("No book with ISBN %s".format(isbn)))
    val authorsElm4 = bookElm4.getChildElem(_.localName == "Authors")

    expectResult((bookElm1.scope ++ Scope.from("magazines" -> "http://magazines")) -- Set("books")) {
      authorsElm1.scope
    }

    expectResult(bookElm1.scope ++ Scope.from("magazines" -> "http://magazines")) {
      authorsElm2.scope
    }

    expectResult(bookElm1.scope ++ Scope.from("magazines" -> "http://magazines")) {
      authorsElm3.scope
    }

    expectResult(bookElm1.scope ++ Scope.from("magazines" -> "http://magazines")) {
      authorsElm4.scope
    }

    val resolvedRoot = resolved.Elem(doc1.documentElement)

    expectResult(resolvedRoot) {
      resolved.Elem(doc2.documentElement)
    }

    expectResult(resolvedRoot) {
      resolved.Elem(doc3.documentElement)
    }

    expectResult(resolvedRoot) {
      resolved.Elem(doc4.documentElement)
    }
  }

  @Test def testNotUndeclaringPrefixesAgain(): Unit = {
    val docParser = DocumentParserUsingDom.newInstance()

    val is = classOf[CreationTest].getResourceAsStream("books-with-strange-namespaces.xml")

    val doc1: Document = docParser.parse(is)
    val resolvedRootElm1: resolved.Elem = resolved.Elem(doc1.documentElement)

    // First call notUndeclaringPrefixes with an empty Scope

    val parentScope2 = Scope.Empty
    val rootElem2 = doc1.documentElement.notUndeclaringPrefixes(parentScope2)

    expectResult(resolvedRootElm1) {
      resolved.Elem(rootElem2)
    }

    expectResult(Set(Declarations.Empty)) {
      NodeBuilder.fromElem(rootElem2)(parentScope2).findAllElemsOrSelf.
        map(_.namespaces.withoutDefaultNamespace.retainingUndeclarations).toSet
    }

    // Now call notUndeclaringPrefixes with Scope.from("books" -> "http://bookstore", "names" -> "http://xyz")

    val parentScope3 = Scope.from("books" -> "http://bookstore", "names" -> "http://xyz")
    val rootElem3 = doc1.documentElement.notUndeclaringPrefixes(parentScope3)

    expectResult(resolvedRootElm1) {
      resolved.Elem(rootElem3)
    }

    expectResult(Set(Declarations.Empty)) {
      NodeBuilder.fromElem(rootElem3)(parentScope3).findAllElemsOrSelf.
        map(_.namespaces.withoutDefaultNamespace.retainingUndeclarations).toSet
    }

    // Next call notUndeclaringPrefixes with Scope.from("abcde" -> "http://abcde")

    val parentScope4 = Scope.from("abcde" -> "http://abcde")
    val rootElem4 = doc1.documentElement.notUndeclaringPrefixes(parentScope4)

    expectResult(resolvedRootElm1) {
      resolved.Elem(rootElem4)
    }

    expectResult(Set(Declarations.Empty)) {
      NodeBuilder.fromElem(rootElem4)(parentScope4).findAllElemsOrSelf.
        map(_.namespaces.withoutDefaultNamespace.retainingUndeclarations).toSet
    }

    // Finally call notUndeclaringPrefixes with Scope.from("books" -> "http://bookstore", "names" -> "http://xyz", "abcde" -> "http://abcde")

    val parentScope5 = Scope.from("books" -> "http://bookstore", "names" -> "http://xyz", "abcde" -> "http://abcde")
    val rootElem5 = doc1.documentElement.notUndeclaringPrefixes(parentScope5)

    expectResult(resolvedRootElm1) {
      resolved.Elem(rootElem5)
    }

    expectResult(Set(Declarations.Empty)) {
      NodeBuilder.fromElem(rootElem5)(parentScope5).findAllElemsOrSelf.
        map(_.namespaces.withoutDefaultNamespace.retainingUndeclarations).toSet
    }
  }

  @Test def testInsertionWhileReusingPrefixes(): Unit = {
    val booksElmBuilder: ElemBuilder =
      elem(
        qname = QName("books:Book"),
        attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
        namespaces = Declarations.from("books" -> "http://bookstore"),
        children = Vector(
          textElem(QName("books:Title"), "Jennifer's Economical Database Hints"),
          elem(
            qname = QName("books:Authors"))))

    expectResult(true) {
      booksElmBuilder.canBuild(Scope.Empty)
    }
    expectResult(true) {
      booksElmBuilder.findAllElemsOrSelf forall (e => e.qname.prefixOption.isDefined)
    }
    expectResult(true) {
      booksElmBuilder.allDeclarationsAreAtTopLevel
    }

    val booksElm: Elem = booksElmBuilder.build(Scope.Empty)

    val prefixBooks = booksElm.scope.prefixesForNamespace("http://bookstore").headOption.getOrElse("bks")

    expectResult("books") {
      prefixBooks
    }

    // Building an "independent" author ElemBuilder, which reuses the "books" prefix of the intended parent tree.
    // "Independence" means: canBuild(Scope.Empty) && (findAllElemsOrSelf forall (e => e.qname.prefixOption.isDefined))
    // In other words, it does not care about the specific parent scope.

    val authorElmBuilder: ElemBuilder =
      elem(
        qname = QName(prefixBooks, "Author"),
        namespaces = Declarations.from(prefixBooks -> "http://bookstore"),
        children = Vector(
          textElem(QName(prefixBooks, "First_Name"), "Jennifer"),
          textElem(QName(prefixBooks, "Last_Name"), "Widom")))

    expectResult(true) {
      authorElmBuilder.canBuild(Scope.Empty)
    }
    expectResult(true) {
      authorElmBuilder.findAllElemsOrSelf forall (e => e.qname.prefixOption.isDefined)
    }
    expectResult(true) {
      authorElmBuilder.allDeclarationsAreAtTopLevel
    }

    val authorElm: Elem = authorElmBuilder.build(Scope.Empty)

    // Let's functionally insert the author

    val authorsPath = booksElm.findElemPath(_.resolvedName == EName("{http://bookstore}Authors")).
      getOrElse(sys.error("No 'Authors' element found"))

    val updatedBooksElm: Elem = booksElm.updated(authorsPath) {
      e => e.plusChild(authorElm)
    }

    expectResult(Some(authorsPath)) {
      updatedBooksElm findElemOrSelfPath { e => e.localName == "Author" } flatMap { path => path.parentPathOption }
    }
    expectResult(true) {
      updatedBooksElm.findAllElemsOrSelf forall { e => e.scope == Scope.from("books" -> "http://bookstore") }
    }

    // Although the "inserted" tree had its own namespace declarations, they are superfluous within the parent tree,
    // and are lost when converting the parent tree back to an ElemBuilder.

    expectResult(true) {
      NodeBuilder.fromElem(updatedBooksElm)(Scope.Empty).allDeclarationsAreAtTopLevel
    }
  }
}
