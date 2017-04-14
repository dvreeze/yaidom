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

import java.{ util => jutil }

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax

/**
 * CD store test case, using yaidom instead of XPath.
 *
 * Acknowledgments: The example comes from http://www.java-only.com/LoadTutorial.javaonly?id=60
 *
 * The original example uses Java, DOM and the standard Java XPath API. The corresponding yaidom code is far more
 * verbose in its expressions that replace the XPath expressions. Yet in total the yaidom examples are far more concise,
 * the yaidom Elem expressions are very easy to understand semantically, the yaidom Elems are immutable and thread-safe,
 * and the yaidom examples are straightforward and contain far less cruft (like setting up factory objects, compiling expressions,
 * obtaining and iterating over result sets, etc.).
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class CdStoreTest extends FunSuite with BeforeAndAfterAll {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  test("testQueryArtistElems") {
    val parser = DocumentParserUsingSax.newInstance

    val doc = parser.parse(classOf[CdStoreTest].getResourceAsStream("cdstore.xml"))

    // Instead of XPath: //cd[@genre='metal']/artist

    val artistElms =
      for {
        cdElm <- doc.documentElement \\ { e => e.localName == "cd" && e.attributeOption(EName("genre")).contains("metal") }
        artistElm <- cdElm \ (_.localName == "artist")
      } yield artistElm

    val artists = artistElms map { _.text }

    assertResult(List("An other artist")) {
      artists
    }

    // The same for-comprehension written in a slightly different way

    val artistElms2 =
      for {
        cdElm <- (doc.documentElement \\ (_.localName == "cd")) filter { _.attributeOption(EName("genre")).contains("metal") }
        artistElm <- cdElm \ (_.localName == "artist")
      } yield artistElm

    val artists2 = artistElms2 map { _.text }

    assertResult(List("An other artist")) {
      artists2
    }
  }

  test("testQueryArtistAsText") {
    val parser = DocumentParserUsingSax.newInstance

    val doc = parser.parse(classOf[CdStoreTest].getResourceAsStream("cdstore.xml"))

    // Instead of XPath: //cd[@genre='metal']/artist/text()

    val artists =
      for {
        cdElm <- doc.documentElement \\ { e => e.localName == "cd" && e.attributeOption(EName("genre")).contains("metal") }
        artistElm <- cdElm \ (_.localName == "artist")
      } yield artistElm.text

    val artistsConcatenated = artists.mkString

    assertResult("An other artist") {
      artistsConcatenated
    }

    // The same for-comprehension written in a slightly different way

    val artists2 =
      for {
        cdElm <- (doc.documentElement \\ (_.localName == "cd")) filter { _.attributeOption(EName("genre")).contains("metal") }
        artistElm <- cdElm \ (_.localName == "artist")
      } yield artistElm.text

    val artistsConcatenated2 = artists2.mkString

    assertResult("An other artist") {
      artistsConcatenated2
    }

    // Or, knowing that there is precisely one such artist

    val artists3 =
      for {
        cdElm <- (doc.documentElement \\ (_.localName == "cd")) filter { _.attributeOption(EName("genre")).contains("metal") }
        artistElm <- cdElm \ (_.localName == "artist")
      } yield artistElm.text

    val firstArtist = artists3.headOption.getOrElse("")

    assertResult("An other artist") {
      firstArtist
    }
  }

  test("testQueryPrice") {
    val parser = DocumentParserUsingSax.newInstance

    val doc = parser.parse(classOf[CdStoreTest].getResourceAsStream("cdstore.xml"))

    // Instead of XPath: //cd[@genre='metal']/price/text()

    val prices =
      for {
        cdElm <- doc.documentElement \\ { e => e.localName == "cd" && e.attributeOption(EName("genre")).contains("metal") }
        artistElm <- cdElm \ (_.localName == "price")
      } yield artistElm.text

    val price = prices.headOption.getOrElse(sys.error("Expected price")).toDouble

    assertResult(10) {
      price.toInt
    }
  }
}
