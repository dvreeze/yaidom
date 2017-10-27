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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.indexed.AbstractIndexedClarkElem
import eu.cdevreeze.yaidom.indexed.IndexedClarkElem
import eu.cdevreeze.yaidom.indexed.IndexedScopedElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.resolved.ResolvedNodes
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem

/**
 * Indexed element test.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class IndexedElemTest extends FunSuite {

  test("testIndexingForSimpleElem") {
    doTestIndexing[Elem, indexed.Elem](IndexedScopedElem(docWithCommentAtEnd.documentElement))

    doTestIndexing[Elem, indexed.Elem](IndexedScopedElem(docUsingXml11.documentElement))
  }

  test("testIndexingForDomWrapperElem") {
    val d1 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, IndexedScopedElem[DomElem]](
      IndexedScopedElem(DomDocument.wrapDocument(DomConversions.convertDocument(docWithCommentAtEnd)(d1)).documentElement))

    val d2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, IndexedScopedElem[DomElem]](
      IndexedScopedElem(DomDocument.wrapDocument(DomConversions.convertDocument(docUsingXml11)(d2)).documentElement))
  }

  test("testClarkElemIndexingForDomWrapperElem") {
    val d1 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, IndexedClarkElem[DomElem]](
      IndexedClarkElem(DomDocument.wrapDocument(DomConversions.convertDocument(docWithCommentAtEnd)(d1)).documentElement))

    val d2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, IndexedClarkElem[DomElem]](
      IndexedClarkElem(DomDocument.wrapDocument(DomConversions.convertDocument(docUsingXml11)(d2)).documentElement))
  }

  test("testIndexingForScalaXmlWrapperElem") {
    doTestIndexing[ScalaXmlElem, IndexedScopedElem[ScalaXmlElem]](
      IndexedScopedElem(ScalaXmlElem(ScalaXmlConversions.convertElem(docWithCommentAtEnd.documentElement))))

    doTestIndexing[ScalaXmlElem, IndexedScopedElem[ScalaXmlElem]](
      IndexedScopedElem(ScalaXmlElem(ScalaXmlConversions.convertElem(docUsingXml11.documentElement))))
  }

  test("testDoubleIndexing") {
    val rootElem = docWithCommentAtEnd.documentElement
    val strangeElem = IndexedScopedElem(IndexedScopedElem(rootElem))

    assertResult(IndexedScopedElem(rootElem).findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingElem))) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingElem.underlyingElem))
    }

    assertResult(IndexedClarkElem(resolved.Elem(rootElem)).findAllElemsOrSelf.map(_.underlyingElem)) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingElem.underlyingElem))
    }

    assertResult(resolved.Elem(rootElem).findAllElemsOrSelf) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingElem.underlyingElem))
    }

    assertResult(resolved.Elem(rootElem).findAllElemsOrSelf) {
      IndexedScopedElem(strangeElem).findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingElem.underlyingElem.underlyingElem))
    }
  }

  test("testGetChildren") {
    val docChildren = docWithCommentAtEnd.children

    assertResult(2)(docChildren.size)

    val rootElem = docWithCommentAtEnd.documentElement.prettify(2)
    val indexedElem = IndexedScopedElem(rootElem)

    val docElemChildren = IndexedScopedElem.getChildren(indexedElem)

    assertResult(indexedElem.findAllChildElems) {
      docElemChildren collect { case che: IndexedScopedElem[_] => che }
    }

    assertResult(rootElem.text) {
      indexedElem.text
    }

    assertResult(indexedElem.text) {
      val textChildren = docElemChildren collect { case ch: Nodes.Text => ch }
      textChildren.map(_.text).mkString
    }
  }

  private def doTestIndexing[U <: ResolvedNodes.Elem with ClarkElemApi.Aux[U], E <: AbstractIndexedClarkElem[U]](rootElem: E): Unit = {
    assertResult(List("product", "number", "size")) {
      rootElem.findAllElemsOrSelf.map(_.localName)
    }

    assertResult(List(
      List(EName(ns, "product")),
      List(EName(ns, "product"), EName(ns, "number")),
      List(EName(ns, "product"), EName(ns, "size")))) {

      rootElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)
    }

    assertResult(rootElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames).map(_.init)) {
      rootElem.findAllElemsOrSelf.map(_.reverseAncestryENames)
    }

    val resolvedElem = resolved.Elem(rootElem.underlyingElem)
    val indexedClarkElem = IndexedClarkElem(resolvedElem)

    assertResult(rootElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)) {
      indexedClarkElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)
    }

    assertResult(rootElem.findAllElemsOrSelf.map(_.reverseAncestryENames)) {
      indexedClarkElem.findAllElemsOrSelf.map(_.reverseAncestryENames)
    }

    assertResult(List(List(EName(ns, "product"), EName(ns, "size")))) {
      rootElem.filterElems(e =>
        e.resolvedName == EName(ns, "size") && e.path == Path.from(EName(ns, "size") -> 0)).map(_.reverseAncestryOrSelfENames)
    }

    assertResult(rootElem.filterElems(e =>
      e.resolvedName == EName(ns, "size") && e.path == Path.from(EName(ns, "size") -> 0)).map(_.reverseAncestryOrSelfENames)) {

      indexedClarkElem.filterElems(e =>
        e.resolvedName == EName(ns, "size") && e.path == Path.from(EName(ns, "size") -> 0)).map(_.reverseAncestryOrSelfENames)
    }

    // Some general properties

    assertResult(resolvedElem.findAllElemsOrSelf) {
      indexedClarkElem.findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingElem))
    }

    assertResult(indexedClarkElem.findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingElem))) {
      rootElem.underlyingElem.findAllElemsOrSelf.map(e => resolved.Elem(e))
    }

    assertResult(resolvedElem.findAllElemsOrSelf) {
      indexedClarkElem.findAllElemsOrSelf.map(e => e.underlyingRootElem.getElemOrSelfByPath(e.path))
    }

    assertResult(resolvedElem.findAllElemsOrSelf) {
      rootElem.findAllElemsOrSelf.map(e => resolved.Elem(e.underlyingRootElem.getElemOrSelfByPath(e.path).asInstanceOf[ResolvedNodes.Elem]))
    }
  }

  private val ns = "http://datypic.com/prod"

  private val docWithCommentAtEnd: Document = {
    val docParser = DocumentParserUsingSax.newInstance

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
    val docParser = DocumentParserUsingSax.newInstance

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
