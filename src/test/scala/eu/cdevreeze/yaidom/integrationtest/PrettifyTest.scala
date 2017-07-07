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

import java.{ util => jutil }

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDomLS
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.Text
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Prettify test case.
 *
 * Acknowledgments: The sample XML is part of the online course "Introduction to Databases", by professor Widom at
 * Stanford University. Many thanks for letting me use this material. Other sample XML files are taken from Anti-XML
 * issues.
 *
 * To debug the DOM parsers, use JVM option -Djaxp.debug=1.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class PrettifyTest extends FunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  test("testPrettifyBooks") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[PrettifyTest].getResourceAsStream("books.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/78 */
  test("testPrettifyStrangeXml") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[PrettifyTest].getResourceAsStream("strangeXml.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  /** See discussion on https://github.com/djspiewak/anti-xml/issues/79 */
  test("testPrettifyDefaultNamespaceXml") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[PrettifyTest].getResourceAsStream("trivialXml.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  test("testPrettifySchemaXsd") {
    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[PrettifyTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[PrettifyTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val domParser = new DocumentParserUsingDom(dbf, createDocumentBuilder _, DomConversions)

    val is = classOf[PrettifyTest].getResourceAsStream("XMLSchema.xsd")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  test("testPrettifyXmlWithExpandedEntityRef") {
    val domParser = DocumentParserUsingDom.newInstance().withConverterToDocument(DomConversions)
    val is = classOf[PrettifyTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  test("testPrettifyXmlWithNonExpandedEntityRef") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setExpandEntityReferences(false)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[PrettifyTest].getResourceAsStream("trivialXmlWithEntityRef.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  test("testPrettifyXmlWithNamespaceUndeclarations") {
    val domParser = DocumentParserUsingDom.newInstance
    val is = classOf[PrettifyTest].getResourceAsStream("trivialXmlWithNSUndeclarations.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  test("testPrettifyXmlWithEscapedChars") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setCoalescing(true)
    val domParser = DocumentParserUsingDom.newInstance(dbf)
    val is = classOf[PrettifyTest].getResourceAsStream("trivialXmlWithEscapedChars.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  test("testPrettifyXmlWithSpecialChars") {
    val domParser = DocumentParserUsingDom.newInstance

    val is = classOf[PrettifyTest].getResourceAsStream("trivialXmlWithEuro.xml")

    val doc: Document = domParser.parse(is)

    doTest(doc)
  }

  /**
   * See http://groovy.codehaus.org/Reading+XML+using+Groovy%27s+XmlParser. The Groovy example is less verbose.
   * The Scala counterpart is more type-safe.
   */
  test("testPrettifyGroovyXmlExample") {
    val parser = DocumentParserUsingDom.newInstance

    val doc = parser.parse(classOf[PrettifyTest].getResourceAsStream("cars.xml"))

    doTest(doc)
  }

  test("testPrettifyFileWithUtf8Bom") {
    val domParser = DocumentParserUsingDom.newInstance

    val is = classOf[PrettifyTest].getResourceAsStream("books.xml")
    val ba = Stream.continually(is.read()).takeWhile(b => b != -1).map(_.toByte).toArray
    val baWithBom = addUtf8Bom(ba)
    assert(baWithBom.size == ba.size + 3)
    assert(baWithBom.toSeq.drop(3) == ba.toSeq)

    val doc: Document = domParser.parse(new java.io.ByteArrayInputStream(baWithBom))

    doTest(doc)
  }

  /**
   * See https://github.com/dvreeze/yaidom/issues/5. Thanks to Matthias Hogerheijde for the bug report.
   */
  test("testPrettifyXmlFromBugReport") {
    val parser = DocumentParserUsingDom.newInstance

    val xmlString = """<?xml version="1.0" encoding="UTF-8"?>
<rootElem>
  <listing>
    <item />
    <item />
    <!-- some comment -->
  </listing>
</rootElem>
"""

    val doc = parser.parse(new InputSource(new java.io.StringReader(xmlString)))

    doTest(doc)
  }

  /**
   * See http://stackoverflow.com/questions/11703635/strip-whitespace-and-newlines-from-xml-in-java.
   */
  test("testRemoveIgnorableWhitespace") {
    val dbf = DocumentBuilderFactory.newInstance
    dbf.setCoalescing(false)
    val parser = DocumentParserUsingDom.newInstance(dbf)

    val xmlString = """<?xml version="1.0" encoding="UTF-8"?>
<tag1>
 <tag2>
    <![CDATA[  Some data ]]>
 </tag2>
</tag1>
"""

    val doc = parser.parse(new InputSource(new java.io.StringReader(xmlString)))

    assertResult(true) {
      doc.documentElement.filterElems(_.localName == "tag2").flatMap(_.textChildren).size >= 3
    }

    val editedDoc =
      doc.transformingDocumentElement(_.removeAllInterElementWhitespace) transformElemsOrSelf { elm =>
        if (elm.localName == "tag2") elm.withChildren(elm.textChildren.filter(_.text.trim.nonEmpty)) else elm
      }

    assertResult(1) {
      editedDoc.documentElement.findAllChildElems.flatMap(_.textChildren).filter(_.isCData).size
    }

    // Very sensitive. Only a (default) DocumentPrinterUsingDomLS seems to return the expected XML output.
    val docPrinter = DocumentPrinterUsingDomLS.newInstance()

    val editedXmlString = docPrinter.print(editedDoc.documentElement)

    assertResult("""<tag1><tag2><![CDATA[  Some data ]]></tag2></tag1>""") {
      editedXmlString
    }
  }

  private def doTest(doc: Document): Unit = {
    val prettifiedDoc = doc.transformingDocumentElement(_.prettify(2))

    // Call printNonFormattedTreeRepr to generate DSL code for creating DocBuilders in TreeReprTest!

    assertResult(true) {
      isPrettified(prettifiedDoc.documentElement, 2, 0)
    }

    assertResult(true) {
      hasNoInterElementWhitespace(prettifiedDoc.documentElement.removeAllInterElementWhitespace)
    }
  }

  private def isPrettified(elem: Elem, indentStep: Int, currentIndent: Int): Boolean = {
    val selfMustBePrettified =
      elem.findChildElem(_ => true).isDefined && elem.textChildren.filter(_.text.trim.nonEmpty).isEmpty

    !selfMustBePrettified || {
      assert(elem.children.nonEmpty)

      val nextIndent = currentIndent + indentStep
      val indentString = "\n" + (" " * nextIndent)
      val lastIndentString = "\n" + (" " * currentIndent)

      val currentElemPrettified =
        (elem.children.sliding(2, 2).filter(_.size == 2).forall(pair => isText(pair(0), indentString))) &&
          (isText(elem.children.last, lastIndentString))

      // Recursive calls

      val descendantsPrettified = elem.findAllChildElems.forall(e => isPrettified(e, indentStep, nextIndent))

      currentElemPrettified && descendantsPrettified
    }
  }

  private def hasNoInterElementWhitespace(elem: Elem): Boolean = {
    val selfMustBeProcessed =
      elem.findChildElem(_ => true).isDefined && elem.textChildren.filter(_.text.trim.nonEmpty).isEmpty

    !selfMustBeProcessed || {
      assert(elem.children.nonEmpty)

      // This check is a bit too strict, but ok for this test
      val currentElemHasNoInterElementWhitespace =
        elem.textChildren.filter(_.text.trim.isEmpty).isEmpty

      // Recursive calls

      val descendantsHaveNoInterElementWhitespace =
        elem.findAllChildElems.forall(e => hasNoInterElementWhitespace(e))

      currentElemHasNoInterElementWhitespace && descendantsHaveNoInterElementWhitespace
    }
  }

  private def isText(n: Node, text: String): Boolean = {
    n.isInstanceOf[Text] && n.asInstanceOf[Text].text == text
  }

  private def addUtf8Bom(ba: Array[Byte]): Array[Byte] = Array[Byte](0xEF.toByte, 0xBB.toByte, 0xBF.toByte) ++ ba
}
