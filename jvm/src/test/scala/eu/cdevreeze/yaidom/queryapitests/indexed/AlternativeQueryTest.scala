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

package eu.cdevreeze.yaidom.queryapitests.indexed

import java.net.URI

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.convertToElem
import eu.cdevreeze.yaidom.indexed.Document
import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.queryapitests.AbstractAlternativeQueryTest
import eu.cdevreeze.yaidom.resolved

/**
 * Alternative query test case for indexed Elems.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class AlternativeQueryTest extends AbstractAlternativeQueryTest {

  final type E = Elem

  private val indexedElemBuilder = Elem

  protected val catalogElem: E = {
    val xml =
      <catalog>
        <product dept="WMN">
          <number>557</number>
          <name language="en">Fleece Pullover</name>
          <colorChoices>navy black</colorChoices>
        </product>
        <product dept="ACC">
          <number>563</number>
          <name language="en">Floppy Sun Hat</name>
        </product>
        <product dept="ACC">
          <number>443</number>
          <name language="en">Deluxe Travel Bag</name>
        </product>
        <product dept="MEN">
          <number>784</number>
          <name language="en">Cotton Dress Shirt</name>
          <colorChoices>white gray</colorChoices>
          <desc>Our <i>favorite</i> shirt!</desc>
        </product>
      </catalog>

    val result = indexedElemBuilder(Some(new URI("http://catalog")), convertToElem(xml))

    // Invoking some Document methods, but without altering the result

    val doc = Document(result)
    require(result == doc.documentElement)
    require(result == doc.withDocumentElement(result).documentElement)
    result
  }

  protected val pricesElem: E = {
    val xml =
      <prices>
        <priceList effDate="2006-11-15">
          <prod num="557">
            <price currency="USD">29.99</price>
            <discount type="CLR">10.00</discount>
          </prod>
          <prod num="563">
            <price currency="USD">69.99</price>
          </prod>
          <prod num="443">
            <price currency="USD">39.99</price>
            <discount type="CLR">3.99</discount>
          </prod>
        </priceList>
      </prices>

    indexedElemBuilder(Some(new URI("http://prices")), convertToElem(xml))
  }

  protected val orderElem: E = {
    val xml =
      <order num="00299432" date="2006-09-15" cust="0221A">
        <item dept="WMN" num="557" quantity="1" color="navy"/>
        <item dept="ACC" num="563" quantity="1"/>
        <item dept="ACC" num="443" quantity="2"/>
        <item dept="MEN" num="784" quantity="1" color="white"/>
        <item dept="MEN" num="784" quantity="1" color="gray"/>
        <item dept="WMN" num="557" quantity="1" color="black"/>
      </order>

    indexedElemBuilder(Some(new URI("http://order")), convertToElem(xml))
  }

  protected final def toResolvedElem(elem: E): resolved.Elem = resolved.Elem.from(elem.underlyingElem)

  protected def fromScalaElem(elem: scala.xml.Elem): E = {
    indexedElemBuilder(Some(new URI("http://bogus-uri")), convertToElem(elem))
  }
}
