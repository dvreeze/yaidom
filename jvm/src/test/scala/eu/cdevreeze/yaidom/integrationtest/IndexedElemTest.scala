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

import java.io.StringReader

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.indexed.IndexedNode
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import org.scalatest.funsuite.AnyFunSuite
import org.xml.sax.InputSource

/**
 * Indexed element test.
 *
 * @author Chris de Vreeze
 */
class IndexedElemTest extends AnyFunSuite {

  test("testIndexedElem") {
    doTestIndexing(IndexedNode.Elem(docWithCommentAtEnd.documentElement))

    doTestIndexing(IndexedNode.Elem(docUsingXml11.documentElement))
  }

  test("testGetChildren") {
    val docChildren = docWithCommentAtEnd.children

    assertResult(2)(docChildren.size)

    val rootElem = docWithCommentAtEnd.documentElement.prettify(2)
    val indexedElem = IndexedNode.Elem(rootElem)

    val docElemChildren = indexedElem.children

    assertResult(indexedElem.findAllChildElems) {
      docElemChildren.collect { case che: IndexedNode.Elem => che }
    }

    assertResult(rootElem.text) {
      indexedElem.text
    }

    assertResult(indexedElem.text) {
      val textChildren = docElemChildren.collect { case ch: ClarkNodes.Text => ch }
      textChildren.map(_.text).mkString
    }
  }

  private def doTestIndexing(rootElem: IndexedNode.Elem): Unit = {
    assertResult(List("product", "number", "size")) {
      rootElem.findAllElemsOrSelf.map(_.localName)
    }

    assertResult(
      List(
        List(EName(ns, "product")),
        List(EName(ns, "product"), EName(ns, "number")),
        List(EName(ns, "product"), EName(ns, "size")))) {

      rootElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)
    }

    assertResult(rootElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames).map(_.init)) {
      rootElem.findAllElemsOrSelf.map(_.reverseAncestryENames)
    }

    assertResult(List(List(EName(ns, "product"), EName(ns, "size")))) {
      rootElem
        .filterElems(e => e.resolvedName == EName(ns, "size") && e.path == Path.from(EName(ns, "size") -> 0))
        .map(_.reverseAncestryOrSelfENames)
    }

    // Some general properties

    assertResult(rootElem.findAllElemsOrSelf.map(resolved.Elem.from)) {
      rootElem.findAllElemsOrSelf.map(e => e.underlyingRootElem.getElemOrSelfByPath(e.path)).map(resolved.Elem.from)
    }
  }

  private val ns = "http://datypic.com/prod"

  private val docWithCommentAtEnd: Document = {
    val docParser = DocumentParserUsingSax.newInstance()

    val xml =
      """|<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |<!-- Bogus comment at the end -->
         |""".stripMargin.trim

    val doc = docParser.parse(new InputSource(new StringReader(xml)))
    doc
  }

  private val docUsingXml11: Document = {
    val docParser = DocumentParserUsingSax.newInstance()

    val xml =
      """|<?xml version="1.1" encoding="iso-8859-1" standalone="no"?>
         |<!-- Bogus comment at the beginning -->
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |""".stripMargin.trim

    val doc = docParser.parse(new InputSource(new StringReader(xml)))
    doc
  }
}
