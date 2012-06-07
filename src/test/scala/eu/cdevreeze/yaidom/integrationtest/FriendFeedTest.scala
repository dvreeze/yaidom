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
package integrationtest

import java.{ util => jutil, io => jio }
import javax.xml.parsers._
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.Predef._
import NodeBuilder._
import parse._
import print._

/**
 * Test case using yaidom on the FriendFeed example, used in https://www.ibm.com/developerworks/library/x-scalaxml/.
 *
 * Acknowledgments: the author of the above-mentioned developerWorks article, Michael Galpin, kindly permitted the use of his FriendFeed example.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class FriendFeedTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @Test def testFilterFeedProcessing() {
    // 1. Parse a sample feed into a Document

    val saxParser = DocumentParserUsingSax.newInstance

    val doc: Document = {
      val is = classOf[FriendFeedTest].getResourceAsStream("friend-feed.xml")
      saxParser.parse(is)
    }
    val feedElm = doc.documentElement

    // 2. Check document structure

    expect("feed") {
      feedElm.localName
    }
    expect(Set("entry")) {
      val childNames = feedElm.allChildElems map { _.localName }
      childNames.toSet
    }

    // 3. Create service summaries, and check them against expected XML

    val twitterSummaryElm: Elem = createServiceSummary(feedElm, "twitter")

    val expectedTwitterSummaryElm: resolved.Elem =
      resolved.Elem(
        "Service".ename,
        Map("id".ename -> "twitter"),
        immutable.IndexedSeq(
          resolved.Elem(
            "UserList".ename,
            Map(),
            immutable.IndexedSeq(
              resolved.Elem("nickname".ename, Map(), immutable.IndexedSeq(resolved.Text("karlerikson"))),
              resolved.Elem("nickname".ename, Map(), immutable.IndexedSeq(resolved.Text("asfaq"))),
              resolved.Elem("nickname".ename, Map(), immutable.IndexedSeq(resolved.Text("chrisjlee")))))))

    expect(expectedTwitterSummaryElm) {
      // There is no inter-element whitespace in this case, but removing it is a good habit before making equality comparisons
      resolved.Elem(twitterSummaryElm).removeAllInterElementWhitespace
    }
  }

  private def filterFeedOnServiceName(feedElm: Elem, serviceName: String): immutable.IndexedSeq[Elem] = {
    require(feedElm.localName == "feed")

    val entryElms = feedElm \ { _.localName == "entry" }

    entryElms filter { e =>
      // Assuming precisely 1 "service" child elem with precisely 1 "id" child elem
      val serviceIdElm = e getChildElem { _.localName == "service" } getChildElem { _.localName == "id" }
      serviceIdElm.text.trim == serviceName
    }
  }

  private def getUserNickNameOfEntry(entryElm: Elem): String = {
    require(entryElm.localName == "entry")

    // Assuming precisely 1 "user" child elem with precisely 1 "nickname" child elem
    val nickNameElm = entryElm getChildElem { _.localName == "user" } getChildElem { _.localName == "nickname" }
    nickNameElm.text.trim
  }

  private def createUserList(nickNames: immutable.Seq[String]): Elem = {
    val userElmBuilders = nickNames map { name =>
      elem(
        qname = "nickname".qname,
        children = List(text(name)))
    }

    val userListElmBuilder =
      elem(
        qname = "UserList".qname,
        children = userElmBuilders)
    userListElmBuilder.build()
  }

  private def createServiceSummary(feedElm: Elem, serviceName: String): Elem = {
    require(feedElm.localName == "feed")

    val nickNames = filterFeedOnServiceName(feedElm, serviceName) map { entryElm => getUserNickNameOfEntry(entryElm) }
    val userListElm = createUserList(nickNames)

    Elem(
      qname = "Service".qname,
      attributes = Map("id".qname -> serviceName),
      scope = Scope.Empty,
      children = immutable.IndexedSeq(userListElm))
  }

  private def createStatisticsForService(feedElm: Elem, serviceName: String): Elem = {
    require(feedElm.localName == "feed")

    val entryElms = filterFeedOnServiceName(feedElm, serviceName)

    val elmBuilder =
      elem(
        qname = "Service".qname,
        attributes = Map("cnt".qname -> entryElms.size.toString, "id".qname -> serviceName))

    elmBuilder.build()
  }

  private def createStatistics(feedElm: Elem, serviceNames: immutable.Seq[String]): Elem = {
    require(feedElm.localName == "feed")

    val serviceStatisticsElms = serviceNames map { serviceName => createStatisticsForService(feedElm, serviceName) }

    Elem(
      qname = "Stats".qname,
      attributes = Map(),
      scope = Scope.Empty,
      children = serviceStatisticsElms.toIndexedSeq)
  }
}
