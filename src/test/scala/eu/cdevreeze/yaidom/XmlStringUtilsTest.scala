/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Test case for [[eu.cdevreeze.yaidom.XmlStringUtils]].
 *
 * The example test strings have been taken from http://docstore.mik.ua/orelly/xml/xmlnut/ch02_04.htm.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlStringUtilsTest extends Suite {

  @Test def testNameValidity() {
    import XmlStringUtils._

    expect(true) {
      isAllowedElementLocalName("Drivers_License_Number")
    }
    expect(false) {
      isAllowedElementLocalName("Driver's_License_Number")
    }

    expect(true) {
      isAllowedElementLocalName("month-day-year")
    }
    expect(false) {
      isAllowedElementLocalName("month/day/year")
    }

    expect(true) {
      isAllowedElementLocalName("first_name")
    }
    expect(false) {
      isAllowedElementLocalName("first name")
    }

    expect(true) {
      isAllowedElementLocalName("_4-lane")
    }
    expect(false) {
      isAllowedElementLocalName("4-lane")
    }

    expect(true) {
      isProbablyValidXmlName("xmlns")
    }
    expect(true) {
      isAllowedElementLocalName("xmlns")
    }

    expect(true) {
      isProbablyValidXmlName("cars:tire")
    }
    expect(false) {
      isAllowedElementLocalName("cars:tire")
    }

    expect(false) {
      isProbablyValidXmlName("")
    }
    expect(false) {
      isProbablyValidXmlName("<")
    }
    expect(false) {
      isProbablyValidXmlName("&")
    }
  }

  @Test def testXPathNotValidName() {
    import XmlStringUtils._

    // To parse simple XPath expressions, we want to establish that "/", "*", "[" and "]" are never themselves
    // part of qualified names.

    expect(true) {
      isProbablyValidXmlName("tire")
    }
    expect(false) {
      isProbablyValidXmlName("/tire")
    }

    expect(true) {
      isProbablyValidXmlName("cars:tire")
    }
    expect(false) {
      isProbablyValidXmlName("/cars:tire")
    }

    expect(true) {
      isProbablyValidXmlName("tire")
    }
    expect(false) {
      isProbablyValidXmlName("tire[1]")
    }

    expect(true) {
      isProbablyValidXmlName("cars:tire")
    }
    expect(false) {
      isProbablyValidXmlName("cars:tire[1]")
    }

    expect(false) {
      isProbablyValidXmlName("*")
    }
  }
}
