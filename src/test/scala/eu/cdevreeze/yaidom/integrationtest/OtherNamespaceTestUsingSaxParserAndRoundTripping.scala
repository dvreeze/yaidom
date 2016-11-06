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

import java.io.InputStream

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertElem
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.parse.AbstractDocumentParser
import eu.cdevreeze.yaidom.parse.DocumentParser
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.resolved

/**
 * See AbstractOtherNamespaceTest.
 *
 * Acknowledgments: This test uses the examples in http://www.datypic.com/books/defxmlschema/chapter03.html, that are also used
 * in the excellent book Definitive XML Schema.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class OtherNamespaceTestUsingSaxParserAndRoundTripping extends AbstractOtherNamespaceTest {

  val documentParser: DocumentParser = new AbstractDocumentParser {

    private val wrappedDocumentParser: DocumentParser = DocumentParserUsingSax.newInstance

    override def parse(inputStream: InputStream): Document = {
      val doc = wrappedDocumentParser.parse(inputStream)

      val result = doc.withDocumentElement(convertToElem(convertElem(doc.documentElement)))

      require(
        resolved.Elem(doc.documentElement) == resolved.Elem(result.documentElement),
        "Data loss during roundtripping")

      result
    }
  }

  val documentParserForXml11: DocumentParser = new AbstractDocumentParser {

    private val wrappedDocumentParser: DocumentParser = DocumentParserUsingSax.newInstance

    override def parse(inputStream: InputStream): Document = {
      val doc = wrappedDocumentParser.parse(inputStream)

      val result = doc.withDocumentElement(convertToElem(convertElem(doc.documentElement)))

      require(
        resolved.Elem(doc.documentElement) == resolved.Elem(result.documentElement),
        "Data loss during roundtripping")

      result
    }
  }
}
