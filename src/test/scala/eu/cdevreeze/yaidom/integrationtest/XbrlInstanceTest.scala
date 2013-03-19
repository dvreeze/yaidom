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
import parse.{ DocumentParserUsingSax, DocumentParserUsingDom }
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

    expectResult(1) {
      xbrlInstance.schemaRefs.size
    }
    expectResult(1) {
      xbrlInstance.linkbaseRefs.size
    }
    expectResult(0) {
      xbrlInstance.roleRefs.size
    }
    expectResult(0) {
      xbrlInstance.arcroleRefs.size
    }
    expectResult(2) {
      xbrlInstance.footnoteLinks.size
    }

    // Check units

    expectResult(3) {
      xbrlInstance.units.size
    }
    expectResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      val result = xbrlInstance.units map { _.id }
      result.toSet
    }

    // Check contexts

    val iContexts = xbrlInstance.contexts filter { _.id.startsWith("I-") }
    val dContexts = xbrlInstance.contexts filter { _.id.startsWith("D-") }

    assert(iContexts.size >= 30, "Expected at least 30 'instant' contexts")
    assert(dContexts.size >= 30, "Expected at least 30 'start-end-date' contexts")

    expectResult((xbrlInstance.contexts).toSet) {
      (iContexts ++ dContexts).toSet
    }

    expectResult(Set(EName(nsXbrli, "instant"))) {
      val result = iContexts flatMap { (ctx: XbrlContext) => ctx.period.allChildElems map { e => e.resolvedName } }
      result.toSet
    }
    expectResult(Set(EName(nsXbrli, "startDate"), EName(nsXbrli, "endDate"))) {
      val result = dContexts flatMap { (ctx: XbrlContext) => ctx.period.allChildElems map { e => e.resolvedName } }
      result.toSet
    }

    // Check facts

    assert(xbrlInstance.topLevelFacts.size >= 50)

    expectResult(0) {
      xbrlInstance.topLevelTuples.size
    }
    expectResult(xbrlInstance.topLevelFacts.size) {
      xbrlInstance.topLevelItems.size
    }

    expectResult(Set(Some("http://xasb.org/gaap"))) {
      val topLevelFacts = xbrlInstance.topLevelFacts
      val result = topLevelFacts map { fact => fact.wrappedElem.resolvedName.namespaceUriOption }
      result.toSet
    }

    assert(
      xbrlInstance.topLevelItems forall { item => xbrlInstance.contexts.map(_.id).contains(item.contextRef) },
      "All contextRefs must be resolved")

    assert(
      xbrlInstance.topLevelItems forall { item => xbrlInstance.units.map(_.id).contains(item.unitRefOption.getOrElse("U-Monetary")) },
      "All unitRefs must be resolved")

    val txt = "The following is an example/sample of the target use case for narratives."
    assert(
      xbrlInstance.topLevelItems exists { item => item.wrappedElem.trimmedText.startsWith(txt) },
      "Expected an item with a value starting with '%s'".format(txt))
  }

  @Test def testBasicRules() {
    // See The Guide & Workbook for Understanding XBRL, 2nd Edition (by Clinton White, Jr.)

    val parser = DocumentParserUsingDom.newInstance()
    val doc: Document = parser.parse(classOf[XbrlInstanceTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val xbrlInstance = XbrlInstance.fromElem(doc.documentElement)

    val xbrlElm: Elem = xbrlInstance.toElem

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(xbrlElm).removeAllInterElementWhitespace
    }

    // XBRL rule 1

    expectResult(EName(nsXbrli, "xbrl")) {
      xbrlElm.resolvedName
    }

    // XBRL rule 2

    expectResult(EName(nsLink, "schemaRef")) {
      xbrlElm.allChildElems.headOption.getOrElse(sys.error("First xbrl child must be schemaRef")).resolvedName
    }

    val schemaRefElms = xbrlElm.filterChildElems(EName(nsLink, "schemaRef"))

    expectResult(true) {
      schemaRefElms forall { e =>
        e.resolvedAttributes.toMap.keySet == Set(EName(nsXLink, "type"), EName(nsXLink, "href"))
      }
    }
    expectResult(true) {
      schemaRefElms forall { e =>
        e.attribute(EName(nsXLink, "type")) == "simple"
      }
    }

    // XBRL rule 3

    expectResult(true) {
      val contextElms = xbrlElm filterChildElems { e => XbrlInstance.mustBeContext(e)(xbrlElm) }
      !contextElms.isEmpty
    }

    // ... more checks ...

    // XBRL rule 4

    expectResult(true) {
      val unitElms = xbrlElm filterChildElems { e => XbrlInstance.mustBeUnit(e)(xbrlElm) }
      !unitElms.isEmpty
    }

    // ... more checks ...

    // XBRL rule 5

    expectResult(true) {
      val topLevelFactElms = xbrlElm filterChildElems { e => XbrlInstance.mustBeTopLevelFact(e)(xbrlElm) }
      !topLevelFactElms.isEmpty
    }

    // ... more checks ...
  }
}

object XbrlInstanceTest {

  val nsXbrli = "http://www.xbrl.org/2003/instance"
  val nsLink = "http://www.xbrl.org/2003/linkbase"
  val nsXLink = "http://www.w3.org/1999/xlink"

  import XbrlInstance._

  /** XBRL instance. The original XML can be reconstructed, modulo equality of the "resolved elements" */
  final class XbrlInstance(
    val rootQName: QName,
    val rootAttributes: immutable.IndexedSeq[(QName, String)],
    val rootScope: Scope,
    val schemaRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val linkbaseRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val roleRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val arcroleRefs: immutable.IndexedSeq[xlink.SimpleLink],
    val contexts: immutable.IndexedSeq[XbrlContext],
    val units: immutable.IndexedSeq[XbrlUnit],
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

    require(contexts.size == (contexts map (_.id)).size)
    require(units.size == (units map (_.id)).size)

    require(rootScope.resolveQNameOption(rootQName) == Some(EName(nsXbrli, "xbrl")))

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
        val contextElms = contexts map { _.wrappedElem }
        val unitElms = units map { _.wrappedElem }
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
    require(wrappedElem.resolvedName == EName(nsXbrli, "context"))
    require(wrappedElem.attributeOption(EName("id")).isDefined)
    require(wrappedElem.findChildElem(EName(nsXbrli, "entity")).isDefined)
    require(wrappedElem.findChildElem(EName(nsXbrli, "period")).isDefined)

    def id: String = wrappedElem.attribute(EName("id"))

    def entity: Elem = wrappedElem.getChildElem(EName(nsXbrli, "entity"))

    def period: Elem = wrappedElem.getChildElem(EName(nsXbrli, "period"))

    def scenarioOption: Option[Elem] = wrappedElem.findChildElem(EName(nsXbrli, "scenario"))
  }

  final class XbrlUnit(val wrappedElem: Elem) extends Immutable {
    require(wrappedElem ne null)
    require(wrappedElem.resolvedName == EName(nsXbrli, "unit"))
    require(wrappedElem.attributeOption(EName("id")).isDefined)

    def id: String = wrappedElem.attribute(EName("id"))
  }

  abstract class XbrlFact(val wrappedElem: Elem) extends Immutable {
    require(wrappedElem ne null)
    require(!Set(Option(nsXbrli), Option(nsLink)).contains(wrappedElem.resolvedName.namespaceUriOption))
  }

  object XbrlFact {

    def apply(e: Elem): XbrlFact =
      if (e.attributeOption(EName("contextRef")).isDefined) new XbrlItem(e) else new XbrlTuple(e)
  }

  final class XbrlItem(override val wrappedElem: Elem) extends XbrlFact(wrappedElem) {
    require(wrappedElem.attributeOption(EName("contextRef")).isDefined)

    def contextRef: String = wrappedElem.attribute(EName("contextRef"))

    def unitRefOption: Option[String] = wrappedElem.attributeOption(EName("unitRef"))

    def isNumeric: Boolean = unitRefOption.isDefined

    def precisionOption: Option[String] = wrappedElem.attributeOption(EName("precision"))

    def decimalsOption: Option[String] = wrappedElem.attributeOption(EName("decimals"))
  }

  final class XbrlTuple(override val wrappedElem: Elem) extends XbrlFact(wrappedElem) {
    require(wrappedElem.attributeOption(EName("contextRef")).isEmpty)

    def childFacts: immutable.IndexedSeq[XbrlFact] = wrappedElem.allChildElems map { e => XbrlFact(e) }
  }

  object XbrlInstance {

    def mustBeInstance(e: Elem): Boolean = e.resolvedName == EName(nsXbrli, "xbrl")

    def mustBeTopLevelFact(e: Elem)(root: Elem): Boolean = {
      // Approximation
      val childElms = root.allChildElems

      if (!childElms.contains(e)) false else {
        !Set(nsXbrli, nsLink).contains(e.resolvedName.namespaceUriOption.getOrElse(nsXbrli))
      }
    }

    def mustBeContext(e: Elem)(root: Elem): Boolean = e.resolvedName == EName(nsXbrli, "context")

    def mustBeUnit(e: Elem)(root: Elem): Boolean = e.resolvedName == EName(nsXbrli, "unit")

    def mustBeSchemaRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == EName(nsLink, "schemaRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeLinkbaseRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == EName(nsLink, "linkbaseRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeRoleRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == EName(nsLink, "roleRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeArcroleRef(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == EName(nsLink, "arcroleRef")
      if (result) {
        require(xlink.XLink.mustBeSimpleLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def mustBeFootnoteLink(e: Elem)(root: Elem): Boolean = {
      val result = e.resolvedName == EName(nsLink, "footnoteLink")
      if (result) {
        require(xlink.XLink.mustBeExtendedLink(e))
        require(root.allChildElems.contains(e))
      }
      result
    }

    def fromElem(root: Elem): XbrlInstance = {
      require(mustBeInstance(root))

      val contexts: immutable.IndexedSeq[XbrlContext] =
        root.allChildElems collect { case e if mustBeContext(e)(root) => new XbrlContext(e) }

      val units: immutable.IndexedSeq[XbrlUnit] =
        root.allChildElems collect { case e if mustBeUnit(e)(root) => new XbrlUnit(e) }

      val topLevelFacts: immutable.IndexedSeq[XbrlFact] =
        root.allChildElems collect { case e if mustBeTopLevelFact(e)(root) => XbrlFact(e) }

      val schemaRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root.allChildElems collect { case e if mustBeSchemaRef(e)(root) => xlink.SimpleLink(e) }
      val linkbaseRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root.allChildElems collect { case e if mustBeLinkbaseRef(e)(root) => xlink.SimpleLink(e) }
      val roleRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root.allChildElems collect { case e if mustBeRoleRef(e)(root) => xlink.SimpleLink(e) }
      val arcroleRefs: immutable.IndexedSeq[xlink.SimpleLink] =
        root.allChildElems collect { case e if mustBeArcroleRef(e)(root) => xlink.SimpleLink(e) }
      val footnoteLinks: immutable.IndexedSeq[xlink.ExtendedLink] =
        root.allChildElems collect { case e if mustBeFootnoteLink(e)(root) => xlink.ExtendedLink(e) }

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
