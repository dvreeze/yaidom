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

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.parse.DocumentParser
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS

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
  }

  @Test def testParseWithEndingCommentsUsingStax(): Unit = {
    val parser = DocumentParserUsingStax.newInstance

    doTestParseWithEndingComments(parser)
  }

  @Test def testParseWithEndingCommentsUsingDom(): Unit = {
    val parser = DocumentParserUsingDom.newInstance

    doTestParseWithEndingComments(parser)
  }

  @Test def testParseWithEndingCommentsUsingDomLS(): Unit = {
    val parser = DocumentParserUsingDomLS.newInstance

    doTestParseWithEndingComments(parser)
  }

  private def doTestParseWithEndingComments(docParser: DocumentParser): Unit = {
    val xml =
      """|<?xml version="1.0" encoding="UTF-8"?>
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
  }
}
