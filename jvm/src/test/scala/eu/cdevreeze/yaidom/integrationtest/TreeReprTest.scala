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

package eu.cdevreeze.yaidom.integrationtest

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.simple.DocBuilder
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.EntityRef
import javax.xml.parsers.DocumentBuilderFactory
import org.scalatest.funsuite.AnyFunSuite
import org.xml.sax.InputSource

/**
 * Tree representation test case. The code to create DocBuilder objects had come verbatim from the toString results
 * of the corresponding test methods in test PrettifyTest, except for leaving out a test method parsing the schema
 * for XML Schema.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * To debug the DOM parsers, use JVM option -Djaxp.debug=1.
 *
 * @author Chris de Vreeze
 */
class TreeReprTest extends AnyFunSuite {

  import eu.cdevreeze.yaidom.simple.DocBuilder._
  import eu.cdevreeze.yaidom.simple.NodeBuilder._

  test("testCreateTreeForBooks") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[TreeReprTest].getResourceAsStream("books.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("books:Bookstore"),
            namespaces = Declarations.from("" -> "http://bookstore", "books" -> "http://bookstore"),
            children = Vector(
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("Edition") -> "3rd", QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""A First Course in Database Systems"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jeffrey"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Ullman"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jennifer"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Widom"))))))))),
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Database Systems: The Complete Book"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Hector"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Garcia-Molina"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jeffrey"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Ullman"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jennifer"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Widom"))))))),
                  elem(
                    qname = QName("Remark"),
                    children = Vector(
                      text(
                        """Buy this book bundled with "A First Course" - a great deal!
                        """))))),
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Hector and Jeff's Database Hints"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jeffrey"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Ullman"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Hector"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Garcia-Molina"))))))),
                  elem(
                    qname = QName("Remark"),
                    children = Vector(
                      text("""An indispensable companion to your textbook"""))))),
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Jennifer's Economical Database Hints"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jennifer"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Widom"))))))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""National Geographic"""))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""National Geographic"""))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("Newsweek"))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Hector and Jeff's Database Hints""")))))))))

    doTest(doc, docBuilder)
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  test("testCreateTreeForStrangeXml") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[TreeReprTest].getResourceAsStream("strangeXml.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("bar"),
            namespaces = Declarations.from("ns" -> "http://www.yahoo.com"),
            children = Vector(
              emptyElem(
                qname = QName("ns:foo"),
                namespaces = Declarations.from("ns" -> "http://www.google.com"))))))

    doTest(doc, docBuilder)
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  test("testCreateTreeForDefaultNamespaceXml") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[TreeReprTest].getResourceAsStream("trivialXml.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          comment(""" This is trivial XML """),
          elem(
            qname = QName("root"),
            namespaces = Declarations.from("" -> "urn:foo:bar"),
            children = Vector(
              comment(""" Trivial XML """),
              emptyElem(
                qname = QName("child"))))))

    doTest(doc, docBuilder)
  }

  test("testCreateTreeForXmlWithExpandedEntityRef") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[TreeReprTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("root"),
            namespaces = Declarations.from("" -> "urn:foo:bar"),
            children = Vector(
              elem(
                qname = QName("child"),
                children = Vector(
                  text(
                    """This text contains an entity reference, viz. hi.
  The entity is defined in the included DTD.
  """)))))))

    doTest(doc, docBuilder)
  }

  test("testCreateTreeForXmlWithNonExpandedEntityRef") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[TreeReprTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("root"),
            namespaces = Declarations.from("" -> "urn:foo:bar"),
            children = Vector(
              elem(
                qname = QName("child"),
                children = Vector(
                  text("""This text contains an entity reference, viz. """),
                  entityRef("hello"),
                  text(
                    """hi.
  The entity is defined in the included DTD.
  """)))))))

    // We only check the entity reference, for consistent results between Oracle and IBM J9.
    // Note that above the string "hi" should not be there in the first place in the second text node.

    def transform(d: Document): Document = {
      d.transformElemsOrSelf {
        case e if e.localName == "child" =>
          e.withChildren(e.children collect { case er: EntityRef => er })
            .ensuring(_.findAllElemsOrSelf.flatMap(_.children.collect { case er: EntityRef => er }).nonEmpty)
        case e => e
      }
    }

    doTest(
      transform(doc),
      DocBuilder.fromDocument(transform(docBuilder.build())))
  }

  test("testCreateTreeForXmlWithNamespaceUndeclarations") {
    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[TreeReprTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("root"),
            namespaces = Declarations.from("" -> "urn:foo:bar"),
            children = Vector(
              elem(
                qname = QName("a"),
                children = Vector(
                  elem(
                    qname = QName("b"),
                    namespaces = Declarations.from("" -> ""),
                    children = Vector(
                      elem(
                        qname = QName("c"),
                        children = Vector(
                          elem(
                            qname = QName("d"),
                            namespaces = Declarations.from("" -> "urn:foo:bar"),
                            children = Vector(
                              text("text")))))))))))))

    doTest(doc, docBuilder)
  }

  test("testCreateTreeForXmlWithEscapedChars") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setCoalescing(true)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[TreeReprTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("root"),
            namespaces = Declarations.from("" -> "urn:foo:bar"),
            children = Vector(
              comment(""" Jansen & co """),
              elem(
                qname = QName("child"),
                attributes = Vector(QName("about") -> """Jansen & co"""),
                children = Vector(
                  text(
                    """
    Jansen & co
  """))),
              elem(
                qname = QName("child"),
                attributes = Vector(QName("about") -> """Jansen & co"""),
                children = Vector(
                  text(
                    """
  Jansen & co
  """)))))))

    doTest(doc, docBuilder)
  }

  test("testCreateTreeForXmlWithSpecialChars") {
    val domParser = DocumentParserUsingDom.newInstance

    val is = classOf[TreeReprTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val doc: Document = domParser.parse(is)

    val docBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("root"),
            namespaces = Declarations.from("" -> "urn:foo:bar"),
            children = Vector(
              elem(
                qname = QName("child"),
                children = Vector(
                  text("""€ 200"""))),
              elem(
                qname = QName("child"),
                children = Vector(
                  text("""€ 200""")))))))

    doTest(doc, docBuilder)
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testCreateTreeForGroovyXmlExample") {
    val parser = DocumentParserUsingDom.newInstance

    val doc = parser.parse(classOf[TreeReprTest].getResourceAsStream("cars.xml"))

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          comment(""" Copied from http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser """),
          elem(
            qname = QName("records"),
            children = Vector(
              elem(
                qname = QName("car"),
                attributes = Vector(QName("make") -> "Holden", QName("name") -> """HSV Maloo""", QName("year") -> "2006"),
                children = Vector(
                  elem(
                    qname = QName("country"),
                    children = Vector(
                      text("Australia"))),
                  elem(
                    qname = QName("record"),
                    attributes = Vector(QName("type") -> "speed"),
                    children = Vector(
                      text(
                        """Production Pickup Truck with speed of 271kph
                        """))))),
              elem(
                qname = QName("car"),
                attributes = Vector(QName("make") -> "Peel", QName("name") -> "P50", QName("year") -> "1962"),
                children = Vector(
                  elem(
                    qname = QName("country"),
                    children = Vector(
                      text("""Isle of Man"""))),
                  elem(
                    qname = QName("record"),
                    attributes = Vector(QName("type") -> "size"),
                    children = Vector(
                      text(
                        """Smallest Street-Legal Car at 99cm wide and 59 kg
      in weight"""))))),
              elem(
                qname = QName("car"),
                attributes = Vector(QName("make") -> "Bugatti", QName("name") -> "Royale", QName("year") -> "1931"),
                children = Vector(
                  elem(
                    qname = QName("country"),
                    children = Vector(
                      text("France"))),
                  elem(
                    qname = QName("record"),
                    attributes = Vector(QName("type") -> "price"),
                    children = Vector(
                      text("""Most Valuable Car at $15 million""")))))))))

    doTest(doc, docBuilder)
  }

  test("testCreateTreeForFileWithUtf8Bom") {
    val domParser = DocumentParserUsingDom.newInstance

    val is = classOf[TreeReprTest].getResourceAsStream("books.xml")
    val ba = Iterator.continually(is.read()).takeWhile(b => b != -1).map(_.toByte).toArray
    val baWithBom = addUtf8Bom(ba)
    assert(baWithBom.size == ba.size + 3)
    assert(baWithBom.toSeq.drop(3) == ba.toSeq)

    val doc: Document = domParser.parse(new java.io.ByteArrayInputStream(baWithBom))

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("books:Bookstore"),
            namespaces = Declarations.from("" -> "http://bookstore", "books" -> "http://bookstore"),
            children = Vector(
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("Edition") -> "3rd", QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""A First Course in Database Systems"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jeffrey"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Ullman"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jennifer"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Widom"))))))))),
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Database Systems: The Complete Book"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Hector"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Garcia-Molina"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jeffrey"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Ullman"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jennifer"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Widom"))))))),
                  elem(
                    qname = QName("Remark"),
                    children = Vector(
                      text(
                        """Buy this book bundled with "A First Course" - a great deal!
                        """))))),
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Hector and Jeff's Database Hints"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jeffrey"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Ullman"))))),
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Hector"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Garcia-Molina"))))))),
                  elem(
                    qname = QName("Remark"),
                    children = Vector(
                      text("""An indispensable companion to your textbook"""))))),
              elem(
                qname = QName("Book"),
                attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Jennifer's Economical Database Hints"""))),
                  elem(
                    qname = QName("Authors"),
                    children = Vector(
                      elem(
                        qname = QName("Author"),
                        children = Vector(
                          elem(
                            qname = QName("First_Name"),
                            children = Vector(
                              text("Jennifer"))),
                          elem(
                            qname = QName("Last_Name"),
                            children = Vector(
                              text("Widom"))))))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""National Geographic"""))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""National Geographic"""))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("Newsweek"))))),
              elem(
                qname = QName("Magazine"),
                attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
                children = Vector(
                  elem(
                    qname = QName("Title"),
                    children = Vector(
                      text("""Hector and Jeff's Database Hints""")))))))))

    doTest(doc, docBuilder)
  }

  /**
   * See https://github.com/dvreeze/yaidom/issues/5. Thanks to Matthias Hogerheijde for the bug report.
   */
  test("testCreateTreeForXmlFromBugReport") {
    val parser = DocumentParserUsingDom.newInstance

    val xmlString =
      """<?xml version="1.0" encoding="UTF-8"?>
<rootElem>
  <listing>
    <item />
    <item />
    <!-- some comment -->
  </listing>
</rootElem>
"""

    val doc = parser.parse(new InputSource(new java.io.StringReader(xmlString)))

    val docBuilder: DocBuilder =
      document(
        uriOption = None,
        children = Vector(
          elem(
            qname = QName("rootElem"),
            children = Vector(
              elem(
                qname = QName("listing"),
                children = Vector(
                  emptyElem(
                    qname = QName("item")),
                  emptyElem(
                    qname = QName("item")),
                  comment(""" some comment """)))))))

    doTest(doc, docBuilder)
  }

  private def doTest(doc: Document, docBuilder: DocBuilder): Unit = {
    val doc2 = docBuilder.build().transformingDocumentElement(e => e.removeAllInterElementWhitespace.coalesceAndNormalizeAllText)

    // Call printTreeReprs to compare the toString result pairs!

    assertResult(doc.transformingDocumentElement(e => e.removeAllInterElementWhitespace.coalesceAndNormalizeAllText).toString) {
      doc2.toString
    }
  }

  private def addUtf8Bom(ba: Array[Byte]): Array[Byte] = Array[Byte](0xEF.toByte, 0xBB.toByte, 0xBF.toByte) ++ ba
}
