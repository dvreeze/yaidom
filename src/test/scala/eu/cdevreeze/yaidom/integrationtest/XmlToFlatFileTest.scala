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

package eu.cdevreeze.yaidom
package integrationtest

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Test case that "ports" http://www.jroller.com/jurberg/entry/converting_xml_to_flat_files to Scala.
 * Here we do not need any dynamic language with lambdas. A static language with lambdas, such as Scala, will do just fine.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlToFlatFileTest extends Suite {

  @Test def testConvertXmlToFlatFile(): Unit = {
    val docParser = parse.DocumentParserUsingStax.newInstance

    val xmlData = """<?xml version="1.0"?>
<catalog>
   <book id="bk101">
      <author>Gambardella, Matthew</author>
      <title>XML Developer's Guide</title>
      <genre>Computer</genre>
      <price>44.95</price>
      <publish_date>2000-10-01</publish_date>
   </book>
   <book id="bk102">
      <author>Ralls, Kim</author>
      <title>Midnight Rain</title>
      <genre>Fantasy</genre>
      <price>5.95</price>
      <publish_date>2000-12-16</publish_date>
   </book>
   <book id="bk103">
      <author>Corets, Eva</author>
      <title>Maeve Ascendant</title>
      <genre>Fantasy</genre>
      <price>5.95</price>
      <publish_date>2000-11-17</publish_date>
   </book>
</catalog>
      """

    val rootElem = docParser.parse(new jio.ByteArrayInputStream(xmlData.getBytes("utf-8"))).documentElement

    val columnMapping = Vector(
      { bookElem: Elem =>
        val id = (bookElem \@ EName("id")).mkString.trim
        id.padTo(5, ' ').take(5)
      },
      { bookElem: Elem =>
        val lastName = (bookElem \ (_.localName == "author")).map(_.text).mkString.split(',').apply(0).trim
        lastName.padTo(15, ' ').take(15)
      },
      { bookElem: Elem =>
        val firstName = (bookElem \ (_.localName == "author")).map(_.text).mkString.split(',').apply(1).trim
        firstName.padTo(10, ' ').take(10)
      })

    val separator = System.getProperty("line.separator")

    val rows = (rootElem \ (_.localName == "book")) map { e =>
      val columns = columnMapping map { f => f(e) }
      columns.mkString + separator
    }

    val expected =
      "bk101Gambardella    Matthew   " + separator +
        "bk102Ralls          Kim       " + separator +
        "bk103Corets         Eva       " + separator

    assertResult(expected) {
      rows.mkString
    }
  }
}
