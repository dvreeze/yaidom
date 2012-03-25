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

    // Check "references"

    expect(1) {
      xbrlInstance.schemaRefs.size
    }
    expect(1) {
      xbrlInstance.linkbaseRefs.size
    }
    expect(0) {
      xbrlInstance.roleRefs.size
    }
    expect(0) {
      xbrlInstance.arcroleRefs.size
    }
    expect(2) {
      xbrlInstance.footnoteLinks.size
    }

    // Check units

    expect(3) {
      xbrlInstance.units.size
    }
    expect(Set("U-Monetary", "U-Shares", "U-Pure")) {
      val result = xbrlInstance.units.values map { _.id }
      result.toSet
    }
    expect(Set("U-Monetary", "U-Shares", "U-Pure")) {
      xbrlInstance.units.keySet
    }

    // Check contexts

    val iContexts = xbrlInstance.contexts filterKeys { id => id.startsWith("I-") }
    val dContexts = xbrlInstance.contexts filterKeys { id => id.startsWith("D-") }

    assert(iContexts.size >= 30, "Expected at least 30 'instant' contexts")
    assert(dContexts.size >= 30, "Expected at least 30 'start-end-date' contexts")

    expect(xbrlInstance.contexts) {
      iContexts ++ dContexts
    }

    expect(Set(nsXbrli.ename("instant"))) {
      val result = iContexts.values flatMap { (ctx: XbrlContext) => ctx.period.allChildElems map { e => e.resolvedName } }
      result.toSet
    }
    expect(Set(nsXbrli.ename("startDate"), nsXbrli.ename("endDate"))) {
      val result = dContexts.values flatMap { (ctx: XbrlContext) => ctx.period.allChildElems map { e => e.resolvedName } }
      result.toSet
    }

    // Check facts

    assert(xbrlInstance.topLevelFacts.size >= 50)

    expect(0) {
      xbrlInstance.topLevelTuples.size
    }
    expect(xbrlInstance.topLevelFacts.size) {
      xbrlInstance.topLevelItems.size
    }

    expect(Set(Some("http://xasb.org/gaap"))) {
      val topLevelFacts = xbrlInstance.topLevelFacts
      val result = topLevelFacts map { fact => fact.wrappedElem.resolvedName.namespaceUriOption }
      result.toSet
    }

    assert(
      xbrlInstance.topLevelItems forall { item => xbrlInstance.contexts.contains(item.contextRef) },
      "All contextRefs must be resolved")

    assert(
      xbrlInstance.topLevelItems forall { item => xbrlInstance.units.contains(item.unitRefOption.getOrElse("U-Monetary")) },
      "All unitRefs must be resolved")

    val txt = "The following is an example/sample of the target use case for narratives."
    assert(
      xbrlInstance.topLevelItems exists { item => item.wrappedElem.trimmedText.startsWith(txt) },
      "Expected an item with a value starting with '%s'".format(txt))
  }
}

object XbrlInstanceTest {

  val nsXbrli = "http://www.xbrl.org/2003/instance".ns
  val nsLink = "http://www.xbrl.org/2003/linkbase".ns
  val nsXLink = "http://www.w3.org/1999/xlink".ns

  import XbrlInstance._

  final class XbrlInstance(
    val rootQName: QName,
    val rootAttributes: Map[QName, String],
    val rootScope: Scope,
    val schemaRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val linkbaseRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val roleRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val arcroleRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val contexts: Map[String, XbrlContext],
    val units: Map[String, XbrlUnit],
    val topLevelFacts: immutable.IndexedSeq[XbrlFact],
    val footnoteLinks: immutable.IndexedSeq[xlink.ExtendedLink]) extends Immutable { self =>

    require(rootQName ne null)
    require(rootAttributes ne null)
    require(rootScope ne null)
    require(schemaRefs ne null)
    require(linkbaseRefs ne null)
    require(roleRefs ne null)
    require(arcroleRefs ne null)
    require(contexts ne null)
    require(units ne null)
    require(topLevelFacts ne null)
    require(footnoteLinks ne null)

    require(rootScope.resolveQName(rootQName) == Some(nsXbrli.ename("xbrl")))

    require(schemaRefs forall { schemaRef => schemaRef.hrefOption.isDefined })
    require(linkbaseRefs forall { linkbaseRef => linkbaseRef.hrefOption.isDefined })
    require(linkbaseRefs forall { linkbaseRef => linkbaseRef.arcroleOption == Some("http://www.w3.org/1999/xlink/properties/linkbase") })

    def topLevelItems: immutable.IndexedSeq[XbrlItem] = topLevelFacts collect { case item: XbrlItem => item }

    def topLevelTuples: immutable.IndexedSeq[XbrlTuple] = topLevelFacts collect { case tuple: XbrlTuple => tuple }

    def toElem: Elem = {
      val children = {
        val schemaRefElms = schemaRefs map { _.wrappedElem }
        val linkbaseRefElms = linkbaseRefs map { _.wrappedElem }
        val roleRefElms = roleRefs map { _.wrappedElem }
        val arcroleRefElms = arcroleRefs map { _.wrappedElem }
        val contextElms = contexts.values map { _.wrappedElem }
        val unitElms = units.values map { _.wrappedElem }
        val topLevelFactElms = topLevelFacts map { _.wrappedElem }
        val footnoteLinkElms = footnoteLinks map { _.wrappedElem }

        schemaRefElms ++ linkbaseRefElms ++ roleRefElms ++ arcroleRefElms ++
          contextElms.toIndexedSeq ++ unitElms.toIndexedSeq ++
          topLevelFactElms ++ footnoteLinkElms
      }

      Elem(
        qname = rootQName,
        attributes = rootAttributes,
        scope = rootScope,
        children = children)
    }
  }

  final class XbrlContext(val wrappedElem: Elem) extends Immutable {
    require(wrappedElem ne null)
    require(wrappedElem.resolvedName == nsXbrli.ename("context"))
    require(wrappedElem.attributeOption("id".ename).isDefined)
    require(wrappedElem.singleChildElemOption(nsXbrli.ename("entity")).isDefined)
    require(wrappedElem.singleChildElemOption(nsXbrli.ename("period")).isDefined)

    def id: String = wrappedElem.attribute("id".ename)

    def entity: Elem = wrappedElem.singleChildElem(nsXbrli.ename("entity"))

    def period: Elem = wrappedElem.singleChildElem(nsXbrli.ename("period"))

    def scenarioOption: Option[Elem] = wrappedElem.singleChildElemOption(nsXbrli.ename("scenario"))
  }

  final class XbrlUnit(val wrappedElem: Elem) extends Immutable {
    require(wrappedElem ne null)
    require(wrappedElem.resolvedName == nsXbrli.ename("unit"))
    require(wrappedElem.attributeOption("id".ename).isDefined)

    def id: String = wrappedElem.attribute("id".ename)
  }

  abstract class XbrlFact(val wrappedElem: Elem) extends Immutable {
    require(wrappedElem ne null)
    require(!Set(Option(nsXbrli.toString), Option(nsLink.toString)).contains(wrappedElem.resolvedName.namespaceUriOption))
  }

  object XbrlFact {

    def apply(e: Elem): XbrlFact =
      if (e.attributeOption("contextRef".ename).isDefined) new XbrlItem(e) else new XbrlTuple(e)
  }

  final class XbrlItem(override val wrappedElem: Elem) extends XbrlFact(wrappedElem) {
    require(wrappedElem.attributeOption("contextRef".ename).isDefined)

    def contextRef: String = wrappedElem.attribute("contextRef".ename)

    def unitRefOption: Option[String] = wrappedElem.attributeOption("unitRef".ename)

    def isNumeric: Boolean = unitRefOption.isDefined

    def precisionOption: Option[String] = wrappedElem.attributeOption("precision".ename)

    def decimalsOption: Option[String] = wrappedElem.attributeOption("decimals".ename)
  }

  final class XbrlTuple(override val wrappedElem: Elem) extends XbrlFact(wrappedElem) {
    require(wrappedElem.attributeOption("contextRef".ename).isEmpty)

    def childFacts: immutable.IndexedSeq[XbrlFact] = wrappedElem.allChildElems map { e => XbrlFact(e) }
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

      val topLevelFacts: immutable.IndexedSeq[XbrlFact] =
        root collectFromChildElems { case e if mustBeTopLevelFact(e)(root) => XbrlFact(e) }

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
        rootQName = root.qname,
        rootAttributes = root.attributes,
        rootScope = root.scope,
        schemaRefs = schemaRefs,
        linkbaseRefs = linkbaseRefs,
        roleRefs = roleRefs,
        arcroleRefs = arcroleRefs,
        contexts = contexts,
        units = units,
        topLevelFacts = topLevelFacts,
        footnoteLinks = footnoteLinks)
    }
  }
}
