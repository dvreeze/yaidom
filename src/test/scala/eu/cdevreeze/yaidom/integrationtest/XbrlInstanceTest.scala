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
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse.DocumentParserUsingSax
import XbrlInstanceTest._

/**
 * Test case using yaidom for XBRL instance processing.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceTest extends Suite {

  def testQueryXbrlInstance() {
    val parser = DocumentParserUsingSax.newInstance()
    val doc: Document = parser.parse(classOf[XbrlInstanceTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val xbrlInstance = new XbrlInstance(doc.documentElement)

    assert(xbrlInstance.topLevelFacts.size >= 10)
    assert(xbrlInstance.contexts.size >= 30)
    expect(3) {
      xbrlInstance.units.size
    }

    expect(Set(Some("http://xasb.org/gaap"))) {
      val topLevelFacts = xbrlInstance.topLevelFacts
      val result = topLevelFacts map { fact => fact.resolvedName.namespaceUriOption }
      result.toSet
    }

    val remaining = xbrlInstance.wrappedElm childElemsWhere { e =>
      !XbrlInstance.mustBeTopLevelFact(e)(xbrlInstance) && !XbrlInstance.mustBeContext(e)(xbrlInstance) && !XbrlInstance.mustBeUnit(e)(xbrlInstance)
    }
    expect(4) {
      remaining.size
    }
    expect(Set(nsLink.ename("schemaRef"), nsLink.ename("linkbaseRef"), nsLink.ename("footnoteLink"))) {
      val result = remaining map { _.resolvedName }
      result.toSet
    }
  }
}

object XbrlInstanceTest {

  val nsXbrli = "http://www.xbrl.org/2003/instance".ns
  val nsLink = "http://www.xbrl.org/2003/linkbase".ns
  val nsXLink = "http://www.w3.org/1999/xlink".ns

  final class XbrlInstance(val wrappedElm: Elem) extends Immutable { self =>
    import XbrlInstance._

    require(wrappedElm ne null)
    require(mustBeInstance(wrappedElm))

    def contexts: Map[String, XbrlContext] = {
      val result = wrappedElm collectFromChildElems { case e if mustBeContext(e)(self) => (e.attribute("id".ename) -> new XbrlContext(e)) }
      result.toMap
    }

    def units: Map[String, XbrlUnit] = {
      val result = wrappedElm collectFromChildElems { case e if mustBeUnit(e)(self) => (e.attribute("id".ename) -> new XbrlUnit(e)) }
      result.toMap
    }

    def topLevelFacts: immutable.IndexedSeq[Elem] =
      wrappedElm childElemsWhere { e => mustBeTopLevelFact(e)(self) }
  }

  final class XbrlContext(val wrappedElm: Elem) extends Immutable {
    require(wrappedElm ne null)
    require(wrappedElm.resolvedName == nsXbrli.ename("context"))
    require(wrappedElm.attributeOption("id".ename).isDefined)
  }

  final class XbrlUnit(val wrappedElm: Elem) extends Immutable {
    require(wrappedElm ne null)
    require(wrappedElm.resolvedName == nsXbrli.ename("unit"))
    require(wrappedElm.attributeOption("id".ename).isDefined)
  }

  object XbrlInstance {

    def mustBeInstance(e: Elem): Boolean = e.resolvedName == nsXbrli.ename("xbrl")

    def mustBeTopLevelFact(e: Elem)(xbrlInstance: XbrlInstance): Boolean = {
      // Approximation
      val childElms = xbrlInstance.wrappedElm.allChildElems

      if (!childElms.contains(e)) false else {
        !Set(nsXbrli.toString, nsLink.toString, nsXLink.toString).contains(e.resolvedName.namespaceUriOption.getOrElse(nsXbrli.toString)) &&
          !xlink.XLink.mustBeXLink(e)
      }
    }

    def mustBeContext(e: Elem)(xbrlInstance: XbrlInstance): Boolean = e.resolvedName == nsXbrli.ename("context")

    def mustBeUnit(e: Elem)(xbrlInstance: XbrlInstance): Boolean = e.resolvedName == nsXbrli.ename("unit")
  }
}
