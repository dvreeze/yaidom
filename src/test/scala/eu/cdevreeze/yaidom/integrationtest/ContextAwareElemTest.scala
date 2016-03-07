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

import scala.reflect.classTag
import scala.reflect.ClassTag

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.contextaware
import eu.cdevreeze.yaidom.contextaware.ContextAwareClarkElem
import eu.cdevreeze.yaidom.contextaware.ContextAwareScopedElem
import eu.cdevreeze.yaidom.contextaware.LazyContextAwareClarkElem
import eu.cdevreeze.yaidom.contextaware.LazyContextAwareScopedElem
import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.dom.DomDocument
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ContextAwareClarkElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.resolved.ResolvedNodes
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem

/**
 * ContextAware element test.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ContextAwareElemTest extends Suite {

  @Test def testIndexingForSimpleElem(): Unit = {
    doTestIndexing[Elem, contextaware.Elem](ContextAwareScopedElem(docWithCommentAtEnd.documentElement))

    doTestIndexing[Elem, contextaware.Elem](ContextAwareScopedElem(docUsingXml11.documentElement))
  }

  @Test def testIndexingForDomWrapperElem(): Unit = {
    val d1 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, ContextAwareScopedElem[DomElem]](
      ContextAwareScopedElem(DomDocument.wrapDocument(DomConversions.convertDocument(docWithCommentAtEnd)(d1)).documentElement))

    val d2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, ContextAwareScopedElem[DomElem]](
      ContextAwareScopedElem(DomDocument.wrapDocument(DomConversions.convertDocument(docUsingXml11)(d2)).documentElement))
  }

  @Test def testClarkElemIndexingForDomWrapperElem(): Unit = {
    val d1 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, ContextAwareClarkElem[DomElem]](
      ContextAwareClarkElem(DomDocument.wrapDocument(DomConversions.convertDocument(docWithCommentAtEnd)(d1)).documentElement))

    val d2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, ContextAwareClarkElem[DomElem]](
      ContextAwareClarkElem(DomDocument.wrapDocument(DomConversions.convertDocument(docUsingXml11)(d2)).documentElement))
  }

  @Test def testLazyContextAwareScopedElemIndexingForDomWrapperElem(): Unit = {
    val d1 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, LazyContextAwareScopedElem[DomElem]](
      LazyContextAwareScopedElem(DomDocument.wrapDocument(DomConversions.convertDocument(docWithCommentAtEnd)(d1)).documentElement))

    val d2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, LazyContextAwareScopedElem[DomElem]](
      LazyContextAwareScopedElem(DomDocument.wrapDocument(DomConversions.convertDocument(docUsingXml11)(d2)).documentElement))
  }

  @Test def testLazyContextAwareClarkElemIndexingForDomWrapperElem(): Unit = {
    val d1 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, LazyContextAwareClarkElem[DomElem]](
      LazyContextAwareClarkElem(DomDocument.wrapDocument(DomConversions.convertDocument(docWithCommentAtEnd)(d1)).documentElement))

    val d2 = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument()
    doTestIndexing[DomElem, LazyContextAwareClarkElem[DomElem]](
      LazyContextAwareClarkElem(DomDocument.wrapDocument(DomConversions.convertDocument(docUsingXml11)(d2)).documentElement))
  }

  @Test def testIndexingForScalaXmlWrapperElem(): Unit = {
    doTestIndexing[ScalaXmlElem, ContextAwareScopedElem[ScalaXmlElem]](
      ContextAwareScopedElem(ScalaXmlElem(ScalaXmlConversions.convertElem(docWithCommentAtEnd.documentElement))))

    doTestIndexing[ScalaXmlElem, ContextAwareScopedElem[ScalaXmlElem]](
      ContextAwareScopedElem(ScalaXmlElem(ScalaXmlConversions.convertElem(docUsingXml11.documentElement))))
  }

  @Test def testLazyContextAwareScopedElemIndexingForScalaXmlWrapperElem(): Unit = {
    doTestIndexing[ScalaXmlElem, LazyContextAwareScopedElem[ScalaXmlElem]](
      LazyContextAwareScopedElem(ScalaXmlElem(ScalaXmlConversions.convertElem(docWithCommentAtEnd.documentElement))))

    doTestIndexing[ScalaXmlElem, LazyContextAwareScopedElem[ScalaXmlElem]](
      LazyContextAwareScopedElem(ScalaXmlElem(ScalaXmlConversions.convertElem(docUsingXml11.documentElement))))
  }

  @Test def testDoubleIndexing(): Unit = {
    val rootElem = docWithCommentAtEnd.documentElement
    val strangeElem = ContextAwareScopedElem(ContextAwareScopedElem(rootElem))

    assertResult(ContextAwareScopedElem(rootElem).findAllElemsOrSelf.map(e => resolved.Elem(e.elem))) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem))
    }

    assertResult(ContextAwareClarkElem(resolved.Elem(rootElem)).findAllElemsOrSelf.map(_.elem)) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem))
    }

    assertResult(resolved.Elem(rootElem).findAllElemsOrSelf) {
      strangeElem.findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem))
    }

    assertResult(resolved.Elem(rootElem).findAllElemsOrSelf) {
      ContextAwareScopedElem(strangeElem).findAllElemsOrSelf.map(e => resolved.Elem(e.elem.elem.elem))
    }
  }

  private def doTestIndexing[U <: ResolvedNodes.Elem with ClarkElemApi[U], E <: ContextAwareClarkElemApi[E]](rootElem: E)(implicit clsTag: ClassTag[E]): Unit = {
    assertResult(List("product", "number", "size")) {
      rootElem.findAllElemsOrSelf.map(_.localName)
    }

    assertResult(List(
      List(EName(ns, "product")),
      List(EName(ns, "product"), EName(ns, "number")),
      List(EName(ns, "product"), EName(ns, "size")))) {

      rootElem.findAllElemsOrSelf.map(_.contextPath.entries.map(_.resolvedName))
    }

    assertResult(rootElem.findAllElemsOrSelf.map(_.contextPath.entries.map(_.resolvedName)).map(_.init)) {
      rootElem.findAllElemsOrSelf.map(_.parentContextPath.entries.map(_.resolvedName))
    }

    val indexedClarkElem = ContextAwareClarkElem[E](rootElem)

    assertResult(rootElem.findAllElemsOrSelf.map(_.contextPath.entries.map(_.resolvedName))) {
      indexedClarkElem.findAllElemsOrSelf.map(_.contextPath.entries.map(_.resolvedName))
    }

    assertResult(rootElem.findAllElemsOrSelf.map(_.parentContextPath.entries.map(_.resolvedName))) {
      indexedClarkElem.findAllElemsOrSelf.map(_.parentContextPath.entries.map(_.resolvedName))
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
