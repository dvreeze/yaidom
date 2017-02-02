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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.dom.DomDocument
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Newline handling test case, checking that the XML parser normalizes line endings to Unix newlines.
 * This is not so much a yaidom test case. It is more a newline DOM parsing test case where yaidom is
 * used for its query API.
 *
 * Acknowledgments: The example XML comes from http://www.java-only.com/LoadTutorial.javaonly?id=60
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class NewlineTest extends FunSuite {

  test("testNormalizeNewlineInPrettifiedXml") {
    val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()

    val is1 = classOf[NewlineTest].getResourceAsStream("cdstore.xml")

    val doc1 = DomDocument(docBuilder.parse(is1))

    // File cdstore-dos.xml is like cdstore.xml, but after invoking the unix2dos command

    val is2 = classOf[NewlineTest].getResourceAsStream("cdstore-dos.xml")

    val doc2 = DomDocument(docBuilder.parse(is2))

    // The yaidom DOM wrappers above contain the unchanged underlying DOM documents!
    // Yaidom (as DOM wrappers) is used here only for easy querying.

    assertResult(doc1.documentElement.findAllElemsOrSelf.map(_.resolvedName)) {
      doc2.documentElement.findAllElemsOrSelf.map(_.resolvedName)
    }

    // Note that DomText.text only invokes wrappedNode.getData

    val whitespaceTextNodes1 =
      doc1.documentElement.findAllElemsOrSelf.flatMap(_.textChildren).filter(_.text.trim.isEmpty)

    val whitespaceTextNodes2 =
      doc2.documentElement.findAllElemsOrSelf.flatMap(_.textChildren).filter(_.text.trim.isEmpty)

    assertResult(true)(whitespaceTextNodes1.size >= 10)

    assertResult(true) {
      whitespaceTextNodes1.filter(_.text.contains('\n')).size >= 10
    }
    assertResult(true) {
      whitespaceTextNodes2.filter(_.text.contains('\n')).size >= 10
    }

    // Counting text nodes is sensitive, so we try not to depend on precise counts of text nodes

    assertResult(whitespaceTextNodes1.map(_.text.count(Set('\n'))).sum) {
      whitespaceTextNodes2.map(_.text.count(Set('\n'))).sum
    }

    // No CR in cdstore.xml (of course), but also no more CR in the DOM tree for cdstore-dos.xml

    assertResult(true) {
      whitespaceTextNodes1.filter(_.text.contains('\r')).isEmpty
    }

    assertResult(true) {
      whitespaceTextNodes2.filter(_.text.contains('\r')).isEmpty
    }

    assertResult(whitespaceTextNodes1.mkString) {
      whitespaceTextNodes2.mkString
    }
  }

  // TODO Test intra-element Unix/Windows newlines, and test mixing both Unix and Windows newlines.
}
