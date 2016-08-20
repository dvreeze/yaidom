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

package eu.cdevreeze.yaidom.queryapitests.indexed

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.indexed.IndexedScopedElem
import eu.cdevreeze.yaidom.queryapitests.AbstractXQuery3UseCasesTest
import eu.cdevreeze.yaidom.simple

/**
 * AbstractXQuery3UseCasesTest for indexed Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XQuery3UseCasesTest extends AbstractXQuery3UseCasesTest {

  final type E = Elem

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

    Elem(convertToElem(xml))
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

    Elem(convertToElem(xml))
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

    Elem(convertToElem(xml))
  }

  protected def toResolvedElem(elem: E): eu.cdevreeze.yaidom.resolved.Elem =
    eu.cdevreeze.yaidom.resolved.Elem(elem.underlyingElem)

  protected def fromSimpleElem(elem: simple.Elem): E = Elem(elem)
}
