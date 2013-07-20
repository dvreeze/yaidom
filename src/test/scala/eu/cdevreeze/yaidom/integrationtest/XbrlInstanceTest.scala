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
 * XBRL instances and parts thereof are represented as wrappers around Elems, and these wrappers themselves mix in the
 * ElemApi trait!
 *
 * This test case also tests Elem methods retrieving QName/EName-valued attributes and element text.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceTest extends Suite {

  def testQueryXbrlInstance() {
    val parser = DocumentParserUsingSax.newInstance()
    val doc: Document = parser.parse(classOf[XbrlInstanceTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val xbrlInstance = new XbrlInstance(doc.documentElement)

    // Check "references"

    expectResult(1) {
      xbrlInstance.findAllSchemaRefs.size
    }
    expectResult(1) {
      xbrlInstance.findAllLinkbaseRefs.size
    }
    expectResult(0) {
      xbrlInstance.findAllRoleRefs.size
    }
    expectResult(0) {
      xbrlInstance.findAllArcroleRefs.size
    }
    expectResult(2) {
      xbrlInstance.findAllFootnoteLinks.size
    }

    // Check units

    expectResult(3) {
      xbrlInstance.findAllUnits.size
    }
    expectResult(Set("U-Monetary", "U-Shares", "U-Pure")) {
      val result = xbrlInstance.findAllUnits map { _.id }
      result.toSet
    }

    // Check contexts

    val iContexts = xbrlInstance.findAllContexts filter { _.id.startsWith("I-") }
    val dContexts = xbrlInstance.findAllContexts filter { _.id.startsWith("D-") }

    assert(iContexts.size >= 30, "Expected at least 30 'instant' contexts")
    assert(dContexts.size >= 30, "Expected at least 30 'start-end-date' contexts")

    expectResult((xbrlInstance.findAllContexts.map(_.asResolvedElem)).toSet) {
      (iContexts ++ dContexts).map(_.asResolvedElem).toSet
    }

    expectResult(Set(EName(XbrliNamespace, "instant"))) {
      val result = iContexts flatMap { (ctx: XbrlContext) => ctx.period.findAllChildElems map { e => e.resolvedName } }
      result.toSet
    }
    expectResult(Set(EName(XbrliNamespace, "startDate"), EName(XbrliNamespace, "endDate"))) {
      val result = dContexts flatMap { (ctx: XbrlContext) => ctx.period.findAllChildElems map { e => e.resolvedName } }
      result.toSet
    }

    // Check facts

    assert(xbrlInstance.findAllTopLevelFacts.size >= 50)

    expectResult(0) {
      xbrlInstance.findAllTopLevelTuples.size
    }
    expectResult(xbrlInstance.findAllTopLevelFacts.size) {
      xbrlInstance.findAllTopLevelItems.size
    }

    expectResult(Set(Some("http://xasb.org/gaap"))) {
      val topLevelFacts = xbrlInstance.findAllTopLevelFacts
      val result = topLevelFacts map { fact => fact.wrappedElem.resolvedName.namespaceUriOption }
      result.toSet
    }

    val allContextIds = xbrlInstance.findAllContexts.map(_.id).toSet
    val allUnitIds = xbrlInstance.findAllUnits.map(_.id).toSet

    assert(
      xbrlInstance.findAllTopLevelItems forall { item => allContextIds.contains(item.contextRef) },
      "All contextRefs must be resolved")

    assert(
      xbrlInstance.findAllTopLevelItems forall { item => allUnitIds.contains(item.unitRefOption.getOrElse("U-Monetary")) },
      "All unitRefs must be resolved")

    val txt = "The following is an example/sample of the target use case for narratives."
    assert(
      xbrlInstance.findAllTopLevelItems exists { item => item.wrappedElem.trimmedText.startsWith(txt) },
      "Expected an item with a value starting with '%s'".format(txt))
  }

  @Test def testBasicRules() {
    // See The Guide & Workbook for Understanding XBRL, 2nd Edition (by Clinton White, Jr.)

    val parser = DocumentParserUsingDom.newInstance()
    val doc: Document = parser.parse(classOf[XbrlInstanceTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val xbrlInstance = new XbrlInstance(doc.documentElement)

    val xbrlElm: Elem = xbrlInstance.wrappedElem

    expectResult(resolved.Elem(doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(xbrlElm).removeAllInterElementWhitespace
    }

    // XBRL rule 1

    expectResult(EName(XbrliNamespace, "xbrl")) {
      xbrlElm.resolvedName
    }

    // XBRL rule 2

    expectResult(EName(LinkNamespace, "schemaRef")) {
      xbrlElm.findAllChildElems.headOption.getOrElse(sys.error("First xbrl child must be schemaRef")).resolvedName
    }

    val schemaRefElms = xbrlElm.filterChildElems(EName(LinkNamespace, "schemaRef"))

    expectResult(true) {
      schemaRefElms forall { e =>
        e.resolvedAttributes.toMap.keySet == Set(EName(XLinkNamespace, "type"), EName(XLinkNamespace, "href"))
      }
    }
    expectResult(true) {
      schemaRefElms forall { e =>
        e.attribute(EName(XLinkNamespace, "type")) == "simple"
      }
    }

    // XBRL rule 3

    expectResult(true) {
      val contextElms = xbrlElm filterChildElems { e => XbrlInstancePart.canBeValidContext(e) }
      !contextElms.isEmpty
    }

    // ... more checks ...

    // XBRL rule 4

    expectResult(true) {
      val unitElms = xbrlElm filterChildElems { e => XbrlInstancePart.canBeValidUnit(e) }
      !unitElms.isEmpty
    }

    // ... more checks ...

    // XBRL rule 5

    expectResult(true) {
      val topLevelFactElms = xbrlElm filterChildElems { e => XbrlInstancePart.mustBeFact(e) }
      !topLevelFactElms.isEmpty
    }

    // ... more checks ...
  }

  @Test def testQueryDimensions() {
    // Testing the retrieval of attributes and element text as QNames, resolved as Elems ...

    val parser = DocumentParserUsingDom.newInstance()
    val doc: Document = parser.parse(classOf[XbrlInstanceTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val xbrlInstance = new XbrlInstance(doc.documentElement)

    val i2007Context = xbrlInstance.findAllContexts.find(e => e.id == "I-2007").getOrElse(sys.error("Missing context I-2007"))

    val dimensionMembers: Map[EName, EName] = {
      val result =
        i2007Context.filterElems(_.wrappedElem.localName == "explicitMember") map { e =>
          val dimension = e.wrappedElem.attributeAsResolvedQName(EName("dimension"))
          val member = e.wrappedElem.textAsResolvedQName
          (dimension -> member)
        }
      result.toMap
    }

    val gaapNamespace = "http://xasb.org/gaap"

    val expectedDimensionMembers = Map(
      EName(gaapNamespace, "EntityAxis") -> EName(gaapNamespace, "ABCCompanyDomain"),
      EName(gaapNamespace, "BusinessSegmentAxis") -> EName(gaapNamespace, "ConsolidatedGroupDomain"),
      EName(gaapNamespace, "VerificationAxis") -> EName(gaapNamespace, "UnqualifiedOpinionMember"),
      EName(gaapNamespace, "PremiseAxis") -> EName(gaapNamespace, "ActualMember"),
      EName(gaapNamespace, "ReportDateAxis") -> EName(gaapNamespace, "ReportedAsOfMarch182008Member"))

    expectResult(expectedDimensionMembers) {
      dimensionMembers
    }
  }

  @Test def testQueryUnits() {
    // Testing the retrieval of element text (without prefix, but using the default namespace!) as QNames, resolved as Elems ...

    val parser = DocumentParserUsingDom.newInstance()
    val doc: Document = parser.parse(classOf[XbrlInstanceTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val xbrlInstance = new XbrlInstance(doc.documentElement)

    val allUnits = xbrlInstance.findAllUnits
    val allMeasures = allUnits.map(e => e.getChildElem(EName(XbrliNamespace, "measure")))

    val measureENames = allMeasures.map(e => e.wrappedElem.textAsResolvedQName).toSet

    expectResult(Set(
      EName("http://www.xbrl.org/2003/iso4217", "USD"),
      EName(XbrliNamespace, "shares"),
      EName(XbrliNamespace, "pure"))) {

      measureENames
    }
  }
}

object XbrlInstanceTest {

  val XbrliNamespace = "http://www.xbrl.org/2003/instance"
  val LinkNamespace = "http://www.xbrl.org/2003/linkbase"
  val XLinkNamespace = "http://www.w3.org/1999/xlink"

  import XbrlInstancePart._

  sealed trait XbrlInstancePart extends ElemLike[XbrlInstancePart] with Immutable {

    def wrappedElem: Elem

    final override def findAllChildElems: immutable.IndexedSeq[XbrlInstancePart] =
      wrappedElem.findAllChildElems map { e => XbrlInstancePart(e) }

    final override def resolvedName: EName = wrappedElem.resolvedName

    final override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = wrappedElem.resolvedAttributes

    final override def toString: String = wrappedElem.toString

    final def asResolvedElem: resolved.Elem = resolved.Elem(wrappedElem)
  }

  final class XbrlInstance(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeInstance(wrappedElem))

    def findAllTopLevelFacts: immutable.IndexedSeq[XbrlFact] =
      wrappedElem.filterChildElems(e => mustBeFact(e)) collect { case e => XbrlFact(e) }

    def findAllTopLevelItems: immutable.IndexedSeq[XbrlItem] =
      findAllTopLevelFacts collect { case item: XbrlItem => item }

    def findAllTopLevelTuples: immutable.IndexedSeq[XbrlTuple] =
      findAllTopLevelFacts collect { case tuple: XbrlTuple => tuple }

    def findAllContexts: immutable.IndexedSeq[XbrlContext] =
      wrappedElem.filterChildElems(e => mustBeContext(e)) collect { case e => new XbrlContext(e) }

    def findAllUnits: immutable.IndexedSeq[XbrlUnit] =
      wrappedElem.filterChildElems(e => mustBeUnit(e)) collect { case e => new XbrlUnit(e) }

    def findAllSchemaRefs: immutable.IndexedSeq[SchemaRef] =
      wrappedElem.filterChildElems(e => mustBeSchemaRef(e)) collect { case e => new SchemaRef(e) }

    def findAllLinkbaseRefs: immutable.IndexedSeq[LinkbaseRef] =
      wrappedElem.filterChildElems(e => mustBeLinkbaseRef(e)) collect { case e => new LinkbaseRef(e) }

    def findAllRoleRefs: immutable.IndexedSeq[RoleRef] =
      wrappedElem.filterChildElems(e => mustBeRoleRef(e)) collect { case e => new RoleRef(e) }

    def findAllArcroleRefs: immutable.IndexedSeq[ArcroleRef] =
      wrappedElem.filterChildElems(e => mustBeArcroleRef(e)) collect { case e => new ArcroleRef(e) }

    def findAllFootnoteLinks: immutable.IndexedSeq[FootnoteLink] =
      wrappedElem.filterChildElems(e => mustBeFootnoteLink(e)) collect { case e => new FootnoteLink(e) }
  }

  final class XbrlContext(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeContext(wrappedElem))
    require(
      wrappedElem.attributeOption(EName("id")).isDefined,
      "Expected @id in xbrli:context %s".format(wrappedElem))
    require(
      wrappedElem.findChildElem(EName(XbrliNamespace, "entity")).isDefined,
      "Expected xbrli:entity in xbrli:context %s".format(wrappedElem))
    require(
      wrappedElem.findChildElem(EName(XbrliNamespace, "period")).isDefined,
      "Expected xbrli:period in xbrli:context %s".format(wrappedElem))

    def id: String = wrappedElem.attribute(EName("id"))

    def entity: Elem = wrappedElem.getChildElem(EName(XbrliNamespace, "entity"))

    def period: Elem = wrappedElem.getChildElem(EName(XbrliNamespace, "period"))

    def scenarioOption: Option[Elem] = wrappedElem.findChildElem(EName(XbrliNamespace, "scenario"))
  }

  final class XbrlUnit(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeUnit(wrappedElem))
    require(
      wrappedElem.attributeOption(EName("id")).isDefined,
      "Expected @id in xbrli:unit %s".format(wrappedElem))

    def id: String = wrappedElem.attribute(EName("id"))
  }

  abstract class XbrlFact(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeFact(wrappedElem))
  }

  final class XbrlItem(override val wrappedElem: Elem) extends XbrlFact(wrappedElem) {
    require(mustBeItem(wrappedElem))

    def contextRef: String = wrappedElem.attribute(EName("contextRef"))

    def unitRefOption: Option[String] = wrappedElem.attributeOption(EName("unitRef"))

    def isNumeric: Boolean = unitRefOption.isDefined

    def precisionOption: Option[String] = wrappedElem.attributeOption(EName("precision"))

    def decimalsOption: Option[String] = wrappedElem.attributeOption(EName("decimals"))
  }

  final class XbrlTuple(override val wrappedElem: Elem) extends XbrlFact(wrappedElem) {
    require(mustBeTuple(wrappedElem))

    def findAllChildFacts: immutable.IndexedSeq[XbrlFact] =
      wrappedElem.filterChildElems(e => mustBeFact(e)) map { e => XbrlFact(e) }
  }

  final class SchemaRef(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeSchemaRef(wrappedElem))
    require(
      xlink.XLink.mustBeSimpleLink(wrappedElem),
      "Expected XLink simple link, but found %s".format(wrappedElem))

    def asSimpleLink: xlink.SimpleLink = xlink.SimpleLink(wrappedElem)
  }

  final class LinkbaseRef(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeLinkbaseRef(wrappedElem))
    require(
      xlink.XLink.mustBeSimpleLink(wrappedElem),
      "Expected XLink simple link, but found %s".format(wrappedElem))

    def asSimpleLink: xlink.SimpleLink = xlink.SimpleLink(wrappedElem)
  }

  final class RoleRef(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeRoleRef(wrappedElem))
    require(
      xlink.XLink.mustBeSimpleLink(wrappedElem),
      "Expected XLink simple link, but found %s".format(wrappedElem))

    def asSimpleLink: xlink.SimpleLink = xlink.SimpleLink(wrappedElem)
  }

  final class ArcroleRef(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeArcroleRef(wrappedElem))
    require(
      xlink.XLink.mustBeSimpleLink(wrappedElem),
      "Expected XLink simple link, but found %s".format(wrappedElem))

    def asSimpleLink: xlink.SimpleLink = xlink.SimpleLink(wrappedElem)
  }

  final class FootnoteLink(override val wrappedElem: Elem) extends XbrlInstancePart {
    require(mustBeFootnoteLink(wrappedElem))
    require(
      xlink.XLink.mustBeExtendedLink(wrappedElem),
      "Expected XLink extended link, but found %s".format(wrappedElem))

    def asExtendedLink: xlink.ExtendedLink = xlink.ExtendedLink(wrappedElem)
  }

  object XbrlInstancePart {

    def mustBeInstance(e: Elem): Boolean = {
      e.resolvedName == EName(XbrliNamespace, "xbrl")
    }

    def mustBeFact(e: Elem): Boolean = {
      // Approximation
      val notAFact =
        Set("http://www.xbrl.org/", "http://xbrl.org", "http://www.w3.org/") exists { s =>
          e.resolvedName.namespaceUriOption.getOrElse("").startsWith(s)
        }
      !notAFact
    }

    def mustBeItem(e: Elem): Boolean = {
      mustBeFact(e) && (e.attributeOption(EName("contextRef")).isDefined)
    }

    def mustBeTuple(e: Elem): Boolean = {
      mustBeFact(e) && (e.attributeOption(EName("contextRef")).isEmpty)
    }

    def mustBeContext(e: Elem): Boolean = {
      e.resolvedName == EName(XbrliNamespace, "context")
    }

    def canBeValidContext(e: Elem): Boolean = {
      mustBeContext(e) && {
        e.attributeOption(EName("id")).isDefined &&
          e.findChildElem(EName(XbrliNamespace, "entity")).isDefined &&
          e.findChildElem(EName(XbrliNamespace, "period")).isDefined
      }
    }

    def mustBeUnit(e: Elem): Boolean = {
      e.resolvedName == EName(XbrliNamespace, "unit")
    }

    def canBeValidUnit(e: Elem): Boolean = {
      mustBeUnit(e) &&
        e.attributeOption(EName("id")).isDefined
    }

    def mustBeSchemaRef(e: Elem): Boolean = {
      e.resolvedName == EName(LinkNamespace, "schemaRef")
    }

    def mustBeLinkbaseRef(e: Elem): Boolean = {
      e.resolvedName == EName(LinkNamespace, "linkbaseRef")
    }

    def mustBeRoleRef(e: Elem): Boolean = {
      e.resolvedName == EName(LinkNamespace, "roleRef")
    }

    def mustBeArcroleRef(e: Elem): Boolean = {
      e.resolvedName == EName(LinkNamespace, "arcroleRef")
    }

    def mustBeFootnoteLink(e: Elem): Boolean = {
      e.resolvedName == EName(LinkNamespace, "footnoteLink")
    }

    def apply(elem: Elem): XbrlInstancePart = elem match {
      case e if mustBeInstance(e) => new XbrlInstance(e)
      case e if mustBeContext(e) => new XbrlContext(e)
      case e if mustBeUnit(e) => new XbrlUnit(e)
      case e if mustBeFact(e) => XbrlFact(e)
      case e if mustBeSchemaRef(e) => new SchemaRef(e)
      case e if mustBeLinkbaseRef(e) => new LinkbaseRef(e)
      case e if mustBeRoleRef(e) => new RoleRef(e)
      case e if mustBeArcroleRef(e) => new ArcroleRef(e)
      case e if mustBeFootnoteLink(e) => new FootnoteLink(e)
      case e => new XbrlInstancePart {
        override def wrappedElem: Elem = e
      }
    }
  }

  object XbrlFact {

    def apply(e: Elem): XbrlFact = {
      require(mustBeFact(e))
      if (mustBeItem(e)) new XbrlItem(e) else new XbrlTuple(e)
    }
  }
}
