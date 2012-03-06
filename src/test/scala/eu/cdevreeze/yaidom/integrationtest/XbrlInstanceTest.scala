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

    val xbrlInstance = XbrlInstance.fromElem(doc.documentElement)

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

    val root = xbrlInstance.toElem
    val remaining = root childElemsWhere { e =>
      !XbrlInstance.mustBeTopLevelFact(e)(root) && !XbrlInstance.mustBeContext(e)(root) && !XbrlInstance.mustBeUnit(e)(root)
    }
    expect(4) {
      remaining.size
    }
    expect(Set(nsLink.ename("schemaRef"), nsLink.ename("linkbaseRef"), nsLink.ename("footnoteLink"))) {
      val result = remaining map { _.resolvedName }
      result.toSet
    }

    expect(1) {
      xbrlInstance.schemaRefs.size
    }
    expect(1) {
      xbrlInstance.linkbaseRefs.size
    }
    expect(2) {
      xbrlInstance.footnoteLinks.size
    }
  }
}

object XbrlInstanceTest {

  val nsXbrli = "http://www.xbrl.org/2003/instance".ns
  val nsLink = "http://www.xbrl.org/2003/linkbase".ns
  val nsXLink = "http://www.w3.org/1999/xlink".ns

  import XbrlInstance._

  final class XbrlInstance(
    val contexts: Map[String, XbrlContext],
    val units: Map[String, XbrlUnit],
    val topLevelFacts: immutable.IndexedSeq[Elem],
    val schemaRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val linkbaseRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val roleRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val arcroleRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val footnoteLinks: immutable.IndexedSeq[xlink.ExtendedLink]) extends Immutable { self =>

    require(contexts ne null)
    require(units ne null)
    require(topLevelFacts ne null)
    require(schemaRefs ne null)
    require(linkbaseRefs ne null)
    require(roleRefs ne null)
    require(arcroleRefs ne null)
    require(footnoteLinks ne null)

    def toElem: Elem = {
      val children = {
        val contextElms = contexts.values map { _.wrappedElm }
        val unitElms = units.values map { _.wrappedElm }
        val schemaRefElms = schemaRefs map { _.wrappedElem }
        val linkbaseRefElms = linkbaseRefs map { _.wrappedElem }
        val roleRefElms = roleRefs map { _.wrappedElem }
        val arcroleRefElms = arcroleRefs map { _.wrappedElem }
        val footnoteLinkElms = footnoteLinks map { _.wrappedElem }

        schemaRefElms ++ linkbaseRefElms ++ roleRefElms ++ arcroleRefElms ++
          contextElms.toIndexedSeq ++ unitElms.toIndexedSeq ++
          topLevelFacts ++ footnoteLinkElms
      }

      Elem(
        qname = "xbrli:xbrl".qname,
        attributes = Map(),
        scope = Map("xbrli" -> "http://www.xbrl.org/2003/instance").scope,
        children = children)
    }
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

    def mustBeTopLevelFact(e: Elem)(root: Elem): Boolean = {
      // Approximation
      val childElms = root.allChildElems

      if (!childElms.contains(e)) false else {
        !Set(nsXbrli.toString, nsLink.toString).contains(e.resolvedName.namespaceUriOption.getOrElse(nsXbrli.toString))
      }
    }

    def mustBeContext(e: Elem)(root: Elem): Boolean = e.resolvedName == nsXbrli.ename("context")

    def mustBeUnit(e: Elem)(root: Elem): Boolean = e.resolvedName == nsXbrli.ename("unit")

    def mustBeSchemaRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == nsLink.ename("schemaRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeLinkbaseRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == nsLink.ename("linkbaseRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeRoleRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == nsLink.ename("roleRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeArcroleRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == nsLink.ename("arcroleRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeFootnoteLink(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == nsLink.ename("footnoteLink")
      if (result) {
        require(xlink.XLink.mustBeExtendedLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def fromElem(root: Elem): XbrlInstance = {
      require(mustBeInstance(root))

      val contexts: Map[String, XbrlContext] = {
        val result = root collectFromChildElems { case e if mustBeContext(e)(root) => (e.attribute("id".ename) -> new XbrlContext(e)) }
        result.toMap
      }

      val units: Map[String, XbrlUnit] = {
        val result = root collectFromChildElems { case e if mustBeUnit(e)(root) => (e.attribute("id".ename) -> new XbrlUnit(e)) }
        result.toMap
      }

      val topLevelFacts: immutable.IndexedSeq[Elem] =
        root childElemsWhere { e => mustBeTopLevelFact(e)(root) }

      val schemaRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root collectFromChildElems { case e if mustBeSchemaRef(e)(root) => xlink.SimpleLink(e) }
      val linkbaseRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root collectFromChildElems { case e if mustBeLinkbaseRef(e)(root) => xlink.SimpleLink(e) }
      val roleRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root collectFromChildElems { case e if mustBeRoleRef(e)(root) => xlink.SimpleLink(e) }
      val arcroleRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root collectFromChildElems { case e if mustBeArcroleRef(e)(root) => xlink.SimpleLink(e) }
      val footnoteLinks: immutable.IndexedSeq[xlink.ExtendedLink] =
        root collectFromChildElems { case e if mustBeFootnoteLink(e)(root) => xlink.ExtendedLink(e) }

      new XbrlInstance(
        contexts = contexts,
        units = units,
        topLevelFacts = topLevelFacts,
        schemaRefs = schemaRefs,
        linkbaseRefs = linkbaseRefs,
        roleRefs = roleRefs,
        arcroleRefs = arcroleRefs,
        footnoteLinks = footnoteLinks)
    }
  }
}
