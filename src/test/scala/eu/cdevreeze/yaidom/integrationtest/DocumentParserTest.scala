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

import scala.io.Codec

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.parse.DocumentParser
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.simple.Document

/**
 * DocumentParser test.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class DocumentParserTest extends Suite {

  @Test def testParseWithEndingCommentsUsingSax(): Unit = {
    val parser = DocumentParserUsingSax.newInstance

    doTestParseWithEndingComments(parser)

    // No XML declaration stored (yet?)
  }

  @Test def testParseWithEndingCommentsUsingStax(): Unit = {
    val parser = DocumentParserUsingStax.newInstance

    val doc = doTestParseWithEndingComments(parser)

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Codec.UTF8.charSet)) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(true)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }
  }

  @Test def testParseWithEndingCommentsUsingDom(): Unit = {
    val parser = DocumentParserUsingDom.newInstance

    val doc = doTestParseWithEndingComments(parser)

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Codec.UTF8.charSet)) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(true)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }
  }

  @Test def testParseWithEndingCommentsUsingDomLS(): Unit = {
    val parser = DocumentParserUsingDomLS.newInstance

    val doc = doTestParseWithEndingComments(parser)

    val xmlDeclOption = doc.xmlDeclarationOption

    assertResult(Some("1.0")) {
      xmlDeclOption.map(_.version)
    }
    assertResult(Some(Codec.UTF8.charSet)) {
      xmlDeclOption.flatMap(_.encodingOption)
    }
    assertResult(Some(true)) {
      xmlDeclOption.flatMap(_.standaloneOption)
    }
  }

  private def doTestParseWithEndingComments(docParser: DocumentParser): Document = {
    val xml =
      """|<?xml version="1.0" encoding="UTF-8" standalone="yes"?>
         |<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |<!-- Bogus comment at the end -->
         |""".stripMargin.trim

    val doc = docParser.parse(new ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(List(QName("prod:product"), QName("prod:number"), QName("prod:size"))) {
      doc.documentElement.findAllElemsOrSelf.map(_.qname)
    }

    assertResult(List("Bogus comment at the end")) {
      doc.comments.map(_.text.trim)
    }

    doc
  }
}
