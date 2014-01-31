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

  @Test def testNameValidity(): Unit = {
    import XmlStringUtils._

    assertResult(true) {
      isAllowedElementLocalName("Drivers_License_Number")
    }
    assertResult(false) {
      isAllowedElementLocalName("Driver's_License_Number")
    }

    assertResult(true) {
      isAllowedElementLocalName("month-day-year")
    }
    assertResult(false) {
      isAllowedElementLocalName("month/day/year")
    }

    assertResult(true) {
      isAllowedElementLocalName("first_name")
    }
    assertResult(false) {
      isAllowedElementLocalName("first name")
    }

    assertResult(true) {
      isAllowedElementLocalName("_4-lane")
    }
    assertResult(false) {
      isAllowedElementLocalName("4-lane")
    }

    assertResult(true) {
      isProbablyValidXmlName("xmlns")
    }
    assertResult(true) {
      isAllowedElementLocalName("xmlns")
    }

    assertResult(true) {
      isProbablyValidXmlName("cars:tire")
    }
    assertResult(false) {
      isAllowedElementLocalName("cars:tire")
    }

    assertResult(false) {
      isProbablyValidXmlName("")
    }
    assertResult(false) {
      isProbablyValidXmlName("<")
    }
    assertResult(false) {
      isProbablyValidXmlName("&")
    }
  }

  @Test def testXPathNotValidName(): Unit = {
    import XmlStringUtils._

    // To parse simple XPath expressions, we want to establish that "/", "*", "[" and "]" are never themselves
    // part of qualified names.

    assertResult(true) {
      isProbablyValidXmlName("tire")
    }
    assertResult(false) {
      isProbablyValidXmlName("/tire")
    }

    assertResult(true) {
      isProbablyValidXmlName("cars:tire")
    }
    assertResult(false) {
      isProbablyValidXmlName("/cars:tire")
    }

    assertResult(true) {
      isProbablyValidXmlName("tire")
    }
    assertResult(false) {
      isProbablyValidXmlName("tire[1]")
    }

    assertResult(true) {
      isProbablyValidXmlName("cars:tire")
    }
    assertResult(false) {
      isProbablyValidXmlName("cars:tire[1]")
    }

    assertResult(false) {
      isProbablyValidXmlName("*")
    }
  }
}
