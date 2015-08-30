/*
 * Copyright 2011-2014 Chris de Vreeze
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

import java.io.ByteArrayInputStream
import java.nio.charset.Charset

import scala.io.Codec
import scala.reflect.classTag

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.indexed.IndexedClarkElem
import eu.cdevreeze.yaidom.indexed.IndexedScopedElem
import eu.cdevreeze.yaidom.parse.DocumentParser
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.DocumentApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
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
class IndexedElemTest extends Suite {

  @Test def testIndexingForSimpleElem(): Unit = {
    doTestIndexing(docWithCommentAtEnd.documentElement, indexedElemBuilder)

    doTestIndexing(docUsingXml11.documentElement, indexedElemBuilder)
  }

  @Test def testIndexingForDomWrapperElem(): Unit = {
    val d1 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing(
      DomDocument.wrapDocument(DomConversions.convertDocument(docWithCommentAtEnd)(d1)).documentElement,
      IndexedScopedElem.Builder(classTag[DomElem], uriResolver))

    val d2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing(
      DomDocument.wrapDocument(DomConversions.convertDocument(docUsingXml11)(d2)).documentElement,
      IndexedScopedElem.Builder(classTag[DomElem], uriResolver))
  }

  @Test def testIndexingForScalaXmlWrapperElem(): Unit = {
    doTestIndexing(
      ScalaXmlElem(ScalaXmlConversions.convertElem(docWithCommentAtEnd.documentElement)),
      IndexedScopedElem.Builder(classTag[ScalaXmlElem], uriResolver))

    doTestIndexing(
      ScalaXmlElem(ScalaXmlConversions.convertElem(docUsingXml11.documentElement)),
      IndexedScopedElem.Builder(classTag[ScalaXmlElem], uriResolver))
  }

  @Test def testDoubleIndexing(): Unit = {
    val rootElem = docWithCommentAtEnd.documentElement
    val strangeElem = indexedElemBuilder2.build(indexedElemBuilder.build(rootElem))

    assertResult(indexedElemBuilder.build(rootElem).findAllElemsOrSelf.map(e => resolved.Elem(e.elem))) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem))
    }

    assertResult(indexedClarkElemBuilder.build(resolved.Elem(rootElem)).findAllElemsOrSelf.map(_.elem)) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem))
    }

    assertResult(resolved.Elem(rootElem).findAllElemsOrSelf) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem))
    }

    assertResult(resolved.Elem(rootElem).findAllElemsOrSelf) {
      indexedElemBuilder3.build(strangeElem).findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem.elem))
    }
  }

  private def doTestIndexing[E <: ResolvedNodes.Elem with ScopedElemApi[E]](rootElem: E, iElemBuilder: IndexedScopedElem.Builder[E]): Unit = {
    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      rootElem.findAllElemsOrSelf.map(_.qname)
    }

    val indexedElem = iElemBuilder.build(rootElem)

    assertResult(List(
      List(EName(ns, "product")),
      List(EName(ns, "product"), EName(ns, "number")),
      List(EName(ns, "product"), EName(ns, "size")))) {

      indexedElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)
    }

    assertResult(indexedElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames).map(_.init)) {
      indexedElem.findAllElemsOrSelf.map(_.reverseAncestryENames)
    }

    val resolvedElem = resolved.Elem(rootElem)
    val indexedClarkElem = indexedClarkElemBuilder.build(resolvedElem)

    assertResult(indexedElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)) {
      indexedClarkElem.findAllElemsOrSelf.map(_.reverseAncestryOrSelfENames)
    }

    assertResult(indexedElem.findAllElemsOrSelf.map(_.reverseAncestryENames)) {
      indexedClarkElem.findAllElemsOrSelf.map(_.reverseAncestryENames)
    }

    assertResult(List(List(EName(ns, "product"), EName(ns, "size")))) {
      indexedElem.filterElems(e =>
        e.resolvedName == EName(ns, "size") && e.path == Path.from(EName(ns, "size") -> 0)).map(_.reverseAncestryOrSelfENames)
    }

    assertResult(indexedElem.filterElems(e =>
      e.resolvedName == EName(ns, "size") && e.path == Path.from(EName(ns, "size") -> 0)).map(_.reverseAncestryOrSelfENames)) {

      indexedClarkElem.filterElems(e =>
        e.resolvedName == EName(ns, "size") && e.path == Path.from(EName(ns, "size") -> 0)).map(_.reverseAncestryOrSelfENames)
    }

    // Some general properties

    assertResult(resolvedElem.findAllElemsOrSelf) {
      indexedClarkElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem))
    }

    assertResult(indexedClarkElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem))) {
      indexedElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem))
    }

    assertResult(resolvedElem.findAllElemsOrSelf) {
      indexedClarkElem.findAllElemsOrSelf.map(e => e.rootElem.getElemOrSelfByPath(e.path))
    }

    assertResult(resolvedElem.findAllElemsOrSelf) {
      indexedElem.findAllElemsOrSelf.map(e => resolved.Elem(e.rootElem.getElemOrSelfByPath(e.path)))
    }
  }

  private val ns = "http://datypic.com/prod"

  private val uriResolver = XmlBaseSupport.JdkUriResolver

  private val indexedElemBuilder =
    IndexedScopedElem.Builder(classTag[Elem], uriResolver)

  private val indexedClarkElemBuilder =
    IndexedClarkElem.Builder(classTag[resolved.Elem], uriResolver)

  private val indexedElemBuilder2 =
    IndexedScopedElem.Builder(classTag[IndexedScopedElem[Elem]], uriResolver)

  private val indexedElemBuilder3 =
    IndexedScopedElem.Builder(classTag[IndexedScopedElem[IndexedScopedElem[Elem]]], uriResolver)

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

    val doc = docParser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))
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

    val doc = docParser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))
    doc
  }
}
