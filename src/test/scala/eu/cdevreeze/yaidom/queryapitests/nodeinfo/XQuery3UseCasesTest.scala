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

package eu.cdevreeze.yaidom.queryapitests.nodeinfo

import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.StringReader

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner
import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapitests.AbstractXQuery3UseCasesTest
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.testsupport.SaxonTestSupport
import javax.xml.transform.sax.SAXSource
import net.sf.saxon.lib.ParseOptions
import net.sf.saxon.s9api.XdmNode

/**
 * AbstractXQuery3UseCasesTest for Saxon wrapper Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XQuery3UseCasesTest extends AbstractXQuery3UseCasesTest with SaxonTestSupport {

  final type E = DomElem

  protected val productsElem: E = {
    val xml =
      <products xmlns="http://www.w3.org/TR/xquery-30-use-cases/">
        <product>
          <name>broiler</name>
          <category>kitchen</category>
          <price>100</price>
          <cost>70</cost>
        </product>
        <product>
          <name>toaster</name>
          <category>kitchen</category>
          <price>30</price>
          <cost>10</cost>
        </product>
        <product>
          <name>blender</name>
          <category>kitchen</category>
          <price>50</price>
          <cost>25</cost>
        </product>
        <product>
          <name>socks</name>
          <category>clothes</category>
          <price>5</price>
          <cost>2</cost>
        </product>
        <product>
          <name>shirt</name>
          <category>clothes</category>
          <price>10</price>
          <cost>3</cost>
        </product>
      </products>

    fromSimpleElem(convertToElem(xml))
  }

  protected val salesElem: E = {
    val xml =
      <sales xmlns="http://www.w3.org/TR/xquery-30-use-cases/">
        <record>
          <product-name>broiler</product-name>
          <store-number>1</store-number>
          <qty>20</qty>
        </record>
        <record>
          <product-name>toaster</product-name>
          <store-number>2</store-number>
          <qty>100</qty>
        </record>
        <record>
          <product-name>toaster</product-name>
          <store-number>2</store-number>
          <qty>50</qty>
        </record>
        <record>
          <product-name>toaster</product-name>
          <store-number>3</store-number>
          <qty>50</qty>
        </record>
        <record>
          <product-name>blender</product-name>
          <store-number>3</store-number>
          <qty>100</qty>
        </record>
        <record>
          <product-name>blender</product-name>
          <store-number>3</store-number>
          <qty>150</qty>
        </record>
        <record>
          <product-name>socks</product-name>
          <store-number>1</store-number>
          <qty>500</qty>
        </record>
        <record>
          <product-name>socks</product-name>
          <store-number>2</store-number>
          <qty>10</qty>
        </record>
        <record>
          <product-name>shirt</product-name>
          <store-number>3</store-number>
          <qty>10</qty>
        </record>
      </sales>

    fromSimpleElem(convertToElem(xml))
  }

  protected val storesElem: E = {
    val xml =
      <stores xmlns="http://www.w3.org/TR/xquery-30-use-cases/">
        <store>
          <store-number>1</store-number>
          <state>CA</state>
        </store>
        <store>
          <store-number>2</store-number>
          <state>CA</state>
        </store>
        <store>
          <store-number>3</store-number>
          <state>MA</state>
        </store>
        <store>
          <store-number>4</store-number>
          <state>WA</state>
        </store>
      </stores>

    fromSimpleElem(convertToElem(xml))
  }

  protected def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem = {
    val bos = new ByteArrayOutputStream
    val serializer = processor.newSerializer(bos)
    serializer.serializeNode(new XdmNode(elem.asInstanceOf[DomElem].wrappedNode))
    val xmlBytes = bos.toByteArray

    val docParser = DocumentParserUsingStax.newInstance
    eu.cdevreeze.yaidom.resolved.Elem(docParser.parse(new ByteArrayInputStream(xmlBytes)).documentElement)
  }

  protected def fromSimpleElem(elem: Elem): E =
    fromSimpleDocument(Document(elem)).documentElement

  private def fromSimpleDocument(d: Document): DomDocument = {
    val parseOptions = new ParseOptions
    val docPrinter = DocumentPrinterUsingDom.newInstance
    val xmlString = docPrinter.print(d)

    val is = new InputSource(new StringReader(xmlString))
    is.setSystemId(d.uriOption.map(_.toString).getOrElse(""))

    val doc: DomDocument =
      DomNode.wrapDocument(
        processor.getUnderlyingConfiguration.buildDocumentTree(
          new SAXSource(is), parseOptions))
    doc
  }
}
