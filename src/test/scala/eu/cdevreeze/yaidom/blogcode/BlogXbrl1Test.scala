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
package blogcode

import java.net.URI
import java.util.Properties
import scala.collection.immutable
import org.joda.time.LocalDate
import org.joda.time.format.ISODateTimeFormat
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Code of yaidom blog "XBRL 1". It shows how yaidom, with its precision, can help in XBRL processing. In this example we only
 * look at XBRL instances, without looking at taxonomies. That is a major simplification, of course.
 *
 * The blog first refers to preceding blogs about yaidom, next explains the basics of XBRL (instances), then goes on to
 * build a yaidom wrapper for XBRL instances, and then shows simple NL-FRIS and KVK-FRIS validation checks using the yaidom
 * wrapper class for XBRL instances. Some queries are handy without "XBRL instance" wrapper, and some are not.
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class BlogXbrl1Test extends Suite {

  import Xbrl._

  private val pathToParentDir: java.io.File =
    (new java.io.File(classOf[BlogXbrl1Test].getResource("kvk-rpt-grote-rechtspersoon-geconsolideerd-model-b-e-indirect-2013.xbrl").toURI)).getParentFile

  private val clazz = classOf[BlogXbrl1Test]

  /**
   * The code in this test can be copied to the content in the first article on yaidom XBRL processing.
   */
  @Test def testXbrlProcessing(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._
    import Xbrl._

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val doc: Document =
      docParser.parse(new File(parentDir, "kvk-rpt-grote-rechtspersoon-geconsolideerd-model-b-e-indirect-2013.xbrl"))

    val xbrlInstanceDoc: XbrlInstanceDoc = new XbrlInstanceDoc(indexed.Document(doc))

    import ElemApi._

    require {
      xbrlInstanceDoc.xbrlInstance.topLevelItems.size >= 20
    }

    val bw2iNs = xbrlInstanceDoc.xbrlInstance.wrappedElem.elem.scope.prefixNamespaceMap("bw2-i")

    val entityFacts = xbrlInstanceDoc.xbrlInstance.filterFacts(EName(bw2iNs, "EntityName"))
    require(entityFacts.forall(e => !e.isTopLevel))

    assertResult(entityFacts.map(_.wrappedElem)) {
      xbrlInstanceDoc.xbrlInstance.topLevelTuples.flatMap(e => e.filterFacts(EName(bw2iNs, "EntityName"))).map(_.wrappedElem)
    }

    // NL-FRIS 8.0 checks.

    require(hasLanguageInRootElem(xbrlInstanceDoc))

    require(elementNamesOnlyUseReservedPrefixes(xbrlInstanceDoc))

    require(hasNoCData(xbrlInstanceDoc))

    require(hasNoXsiNilIsTrue(xbrlInstanceDoc))

    require(hasAtMostOneSchemaRef(xbrlInstanceDoc))

    require(hasNoLinkbaseRef(xbrlInstanceDoc))

    require(hasNoUnusedContexts(xbrlInstanceDoc))

    require(hasNoStartEndDateOverlaps(xbrlInstanceDoc))

    require(hasNoPeriodWithTime(xbrlInstanceDoc))

    require(hasNoPeriodForever(xbrlInstanceDoc))

    require(hasNoFootnotes(xbrlInstanceDoc))
  }

  /** Checks NL-FRIS 8.0, rule 2.1.1. */
  private def hasLanguageInRootElem(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.attributeOption(XmlLangEName).isDefined
  }

  /** Checks NL-FRIS 8.0, rule 2.1.2. */
  private def elementNamesOnlyUseReservedPrefixes(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    import scala.collection.JavaConverters._

    val props = new Properties
    props.load(clazz.getResourceAsStream("reserved-namespaces-and-prefixes.properties"))
    val namespacePrefixMap: Map[String, String] = props.asScala.toMap

    elementNamesOnlyUseReservedPrefixes(xbrlInstanceDoc, namespacePrefixMap)
  }

  /** Checks NL-FRIS 8.0, rule 2.1.2. */
  private def elementNamesOnlyUseReservedPrefixes(xbrlInstanceDoc: XbrlInstanceDoc, namespacePrefixMap: Map[String, String]): Boolean = {
    xbrlInstanceDoc.xbrlInstance.findAllElemsOrSelf forall { e =>
      val expectedPrefixOption = e.resolvedName.namespaceUriOption.flatMap(ns => namespacePrefixMap.get(ns))

      (expectedPrefixOption.isEmpty) || (e.wrappedElem.elem.qname.prefixOption == expectedPrefixOption)
    }
  }

  /** Checks NL-FRIS 8.0, rule 2.1.4. */
  private def hasNoCData(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.findAllElemsOrSelf.forall(e => !e.wrappedElem.elem.textChildren.exists(_.isCData))
  }

  /** Checks NL-FRIS 8.0, rule 2.1.5. */
  private def hasNoXsiNilIsTrue(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.findAllElemsOrSelf.forall(e => e.attributeOption(XsiNilEName) != Some("true"))
  }

  /** Checks NL-FRIS 8.0, rule 2.2.1. */
  private def hasAtMostOneSchemaRef(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.schemaRefs.size <= 1
  }

  /** Checks NL-FRIS 8.0, rule 2.2.1. */
  private def hasNoLinkbaseRef(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.linkbaseRefs.isEmpty
  }

  /** Checks NL-FRIS 8.0, rule 2.4.1. */
  private def hasNoUnusedContexts(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    val usedContextIds = xbrlInstanceDoc.xbrlInstance.items.map(_.contextRef).toSet

    val allContextIds = xbrlInstanceDoc.xbrlInstance.contextsById.keySet

    val unusedContextIds = allContextIds diff usedContextIds
    unusedContextIds.isEmpty
  }

  /** Checks NL-FRIS 8.0, rule 2.5.1. */
  private def hasNoStartEndDateOverlaps(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    val dateFormatter = ISODateTimeFormat.date()

    val startDatesByContextId: Map[String, LocalDate] =
      xbrlInstanceDoc.xbrlInstance.contextsById filter { case (id, ctx) => ctx.period.isFiniteDuration } mapValues { ctx =>
        val s = ctx.period.getChildElem(XbrliStartDateEName).wrappedElem.text
        dateFormatter.parseLocalDate(s)
      }

    val endDatesByContextId: Map[String, LocalDate] =
      xbrlInstanceDoc.xbrlInstance.contextsById filter { case (id, ctx) => ctx.period.isFiniteDuration } mapValues { ctx =>
        val s = ctx.period.getChildElem(XbrliEndDateEName).wrappedElem.text
        dateFormatter.parseLocalDate(s)
      }

    val contextIdsByEndDates: Map[LocalDate, immutable.IndexedSeq[String]] =
      endDatesByContextId.toVector.groupBy(_._2) mapValues { case idDateSeq => idDateSeq.map(_._1) }

    val offendingStartDatesByContextId: Map[String, LocalDate] =
      startDatesByContextId filter {
        case (id, startDate) =>
          val idsWithSameEndDate = contextIdsByEndDates.getOrElse(startDate, Vector())
          !(idsWithSameEndDate.toSet - id).isEmpty
      }

    offendingStartDatesByContextId.isEmpty
  }

  /** Checks NL-FRIS 8.0, rule 2.5.2. */
  private def hasNoPeriodWithTime(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.contexts.filter(e => !e.period.isForever) forall {
      case e: XbrliContext if e.period.isInstant =>
        val instant = e.period.getChildElem(XbrliInstantEName).wrappedElem.text
        !instant.contains("T")
      case e: XbrliContext if e.period.isFiniteDuration =>
        val startDate = e.period.getChildElem(XbrliStartDateEName).wrappedElem.text
        val endDate = e.period.getChildElem(XbrliEndDateEName).wrappedElem.text
        !startDate.contains("T") && !endDate.contains("T")
      case _ =>
        true
    }
  }

  /** Checks NL-FRIS 8.0, rule 2.5.3. */
  private def hasNoPeriodForever(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.contexts.filter(_.period.isForever).isEmpty
  }

  /** Checks NL-FRIS 8.0, rule 2.9. */
  private def hasNoFootnotes(xbrlInstanceDoc: XbrlInstanceDoc): Boolean = {
    xbrlInstanceDoc.xbrlInstance.footnoteLinks.isEmpty
  }

  /**
   * "Namespace" for XBRL instance support. This code can be copied as is into the blog, or into a document to which the
   * blog has a hyperlink.
   */
  object Xbrl {

    import ElemApi._

    val XbrliNs = "http://www.xbrl.org/2003/instance"
    val LinkNs = "http://www.xbrl.org/2003/linkbase"
    val XmlNs = "http://www.w3.org/XML/1998/namespace"
    val XsiNs = "http://www.w3.org/2001/XMLSchema-instance"

    val XbrliXbrlEName = EName(XbrliNs, "xbrl")
    val XbrliContextEName = EName(XbrliNs, "context")
    val XbrliUnitEName = EName(XbrliNs, "unit")
    val XbrliEntityEName = EName(XbrliNs, "entity")
    val XbrliPeriodEName = EName(XbrliNs, "period")
    val XbrliScenarioEName = EName(XbrliNs, "scenario")
    val XbrliIdentifierEName = EName(XbrliNs, "identifier")
    val XbrliSegmentEName = EName(XbrliNs, "segment")
    val XbrliInstantEName = EName(XbrliNs, "instant")
    val XbrliStartDateEName = EName(XbrliNs, "startDate")
    val XbrliEndDateEName = EName(XbrliNs, "endDate")
    val XbrliForeverEName = EName(XbrliNs, "forever")
    val XbrliMeasureEName = EName(XbrliNs, "measure")
    val XbrliDivideEName = EName(XbrliNs, "divide")
    val XbrliUnitNominatorEName = EName(XbrliNs, "unitNominator")
    val XbrliUnitDenominatorEName = EName(XbrliNs, "unitDenominator")

    val LinkSchemaRefEName = EName(LinkNs, "schemaRef")
    val LinkLinkbaseRefEName = EName(LinkNs, "linkbaseRef")
    val LinkRoleRefEName = EName(LinkNs, "roleRef")
    val LinkArcroleRefEName = EName(LinkNs, "arcroleRef")
    val LinkFootnoteLinkEName = EName(LinkNs, "footnoteLink")

    val XmlLangEName = EName(XmlNs, "lang")

    val XsiNilEName = EName(XsiNs, "nil")

    val IdEName = EName("id")
    val ContextRefEName = EName("contextRef")
    val UnitRefEName = EName("unitRef")

    import XbrliElem._

    /**
     * XBRL instance document. Expensive to create, because of the cached XBRL instance element.
     */
    final class XbrlInstanceDoc(val wrappedDoc: indexed.Document) {

      /** The document element, as XbrlInstance */
      val xbrlInstance: XbrlInstance = XbrliElem(wrappedDoc.documentElement).asInstanceOf[XbrlInstance]

      /** URI of the document. Useful for resolving relative URIs of schemaRefs, etc. */
      def uriOption: Option[URI] = wrappedDoc.uriOption
    }

    /**
     * XML element inside XBRL instance (or the entire XBRL instance itself), offering the ElemApi query API itself.
     */
    sealed abstract class XbrliElem private[Xbrl] (
      val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends ElemLike[XbrliElem] {

      assert(childElems.map(_.wrappedElem) == wrappedElem.findAllChildElems)
      assert(wrappedElem.rootElem.resolvedName == XbrliXbrlEName)

      final override def findAllChildElems: immutable.IndexedSeq[XbrliElem] = childElems

      final override def resolvedName: EName = wrappedElem.resolvedName

      final override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = wrappedElem.resolvedAttributes
    }

    /**
     * XBRL instance. Expensive to create, because of the cached contexts, units and facts.
     */
    final class XbrlInstance private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == XbrliXbrlEName)

      val contextsById: Map[String, XbrliContext] = {
        val contextsGrouped =
          filterChildElems(withEName(XbrliContextEName)).groupBy(_.attribute(IdEName))
        require(contextsGrouped.values.forall(_.size == 1), s"All context @id attributes must be unique inside the XBRL instance")

        contextsGrouped.mapValues(e => e.head.asInstanceOf[XbrliContext])
      }

      def contexts: immutable.IndexedSeq[XbrliContext] = contextsById.values.toVector

      val unitsById: Map[String, XbrliUnit] = {
        val unitsGrouped =
          filterChildElems(withEName(XbrliUnitEName)).groupBy(_.attribute(IdEName))
        require(unitsGrouped.values.forall(_.size == 1), s"All unit @id attributes must be unique inside the XBRL instance")

        unitsGrouped.mapValues(e => e.head.asInstanceOf[XbrliUnit])
      }

      def units: immutable.IndexedSeq[XbrliUnit] = unitsById.values.toVector

      val facts: immutable.IndexedSeq[Fact] =
        filterElems(e => canBeFact(e.wrappedElem)) collect { case e: Fact => e }

      val factsByEName: Map[EName, immutable.IndexedSeq[Fact]] =
        facts.groupBy(_.resolvedName)

      def items: immutable.IndexedSeq[ItemFact] =
        facts collect { case e: ItemFact => e }

      def tuples: immutable.IndexedSeq[TupleFact] =
        facts collect { case e: TupleFact => e }

      def topLevelFacts: immutable.IndexedSeq[Fact] =
        facts filter { e => e.wrappedElem.path.entries.size == 1 }

      def topLevelItems: immutable.IndexedSeq[ItemFact] =
        items filter { e => e.wrappedElem.path.entries.size == 1 }

      def topLevelTuples: immutable.IndexedSeq[TupleFact] =
        tuples filter { e => e.wrappedElem.path.entries.size == 1 }

      def filterFacts(ename: EName): immutable.IndexedSeq[Fact] =
        factsByEName.getOrElse(ename, Vector())

      def filterItems(ename: EName): immutable.IndexedSeq[ItemFact] =
        filterFacts(ename) collect { case e: ItemFact => e }

      def filterTuples(ename: EName): immutable.IndexedSeq[TupleFact] =
        filterFacts(ename) collect { case e: TupleFact => e }

      def filterTopLevelFacts(ename: EName): immutable.IndexedSeq[Fact] =
        filterFacts(ename) filter (_.isTopLevel)

      def filterTopLevelItems(ename: EName): immutable.IndexedSeq[ItemFact] =
        filterItems(ename) filter (_.isTopLevel)

      def filterTopLevelTuples(ename: EName): immutable.IndexedSeq[TupleFact] =
        filterTuples(ename) filter (_.isTopLevel)

      def schemaRefs: immutable.IndexedSeq[SchemaRef] =
        filterChildElems(withEName(LinkSchemaRefEName)) collect { case e: SchemaRef => e }

      def linkbaseRefs: immutable.IndexedSeq[LinkbaseRef] =
        filterChildElems(withEName(LinkLinkbaseRefEName)) collect { case e: LinkbaseRef => e }

      def roleRefs: immutable.IndexedSeq[RoleRef] =
        filterChildElems(withEName(LinkRoleRefEName)) collect { case e: RoleRef => e }

      def arcroleRefs: immutable.IndexedSeq[ArcroleRef] =
        filterChildElems(withEName(LinkArcroleRefEName)) collect { case e: ArcroleRef => e }

      def footnoteLinks: immutable.IndexedSeq[FootnoteLink] =
        filterChildElems(withEName(LinkFootnoteLinkEName)) collect { case e: FootnoteLink => e }
    }

    /**
     * SchemaRef in an XBRL instance
     */
    final class SchemaRef private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == LinkSchemaRefEName)
    }

    /**
     * LinkbaseRef in an XBRL instance
     */
    final class LinkbaseRef private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == LinkLinkbaseRefEName)
    }

    /**
     * RoleRef in an XBRL instance
     */
    final class RoleRef private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == LinkRoleRefEName)
    }

    /**
     * ArcroleRef in an XBRL instance
     */
    final class ArcroleRef private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == LinkArcroleRefEName)
    }

    /**
     * Context in an XBRL instance
     */
    final class XbrliContext private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == XbrliContextEName)
      require(wrappedElem.attributeOption(IdEName).isDefined, s"An xbrli:context must have attribute @id")

      def entity: XbrliEntity = getChildElem(XbrliEntityEName).asInstanceOf[XbrliEntity]

      def period: XbrliPeriod = getChildElem(XbrliPeriodEName).asInstanceOf[XbrliPeriod]

      def scenarioOption: Option[XbrliScenario] =
        findChildElem(XbrliScenarioEName) collect { case e: XbrliScenario => e }
    }

    /**
     * Unit in an XBRL instance
     */
    final class XbrliUnit private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == XbrliUnitEName)
      require(wrappedElem.attributeOption(IdEName).isDefined, s"An xbrli:unit must have attribute @id")
    }

    /**
     * Item or tuple fact in an XBRL instance, either top-level or nested
     */
    abstract class Fact private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(canBeFact(wrappedElem))

      final def isTopLevel: Boolean = wrappedElem.path.entries.size == 1
    }

    /**
     * Item fact in an XBRL instance, either top-level or nested
     */
    final class ItemFact private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends Fact(wrappedElem, childElems) {

      assert(attributeOption(ContextRefEName).isDefined)

      def contextRef: String = attribute(ContextRefEName)
    }

    /**
     * Tuple fact in an XBRL instance, either top-level or nested
     */
    final class TupleFact private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends Fact(wrappedElem, childElems) {

      assert(!attributeOption(ContextRefEName).isDefined)

      def facts: immutable.IndexedSeq[Fact] =
        wrappedElem.filterElems(e => canBeFact(e)) map { e => XbrliElem(e) } collect { case e: Fact => e }

      def childFacts: immutable.IndexedSeq[Fact] =
        wrappedElem.filterChildElems(e => canBeFact(e)) map { e => XbrliElem(e) } collect { case e: Fact => e }

      def filterFacts(ename: EName): immutable.IndexedSeq[Fact] =
        facts filter (_.resolvedName == ename)

      def filterChildFacts(ename: EName): immutable.IndexedSeq[Fact] =
        childFacts filter (_.resolvedName == ename)
    }

    /**
     * FootnoteLink in an XBRL instance
     */
    final class FootnoteLink private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == LinkFootnoteLinkEName)
    }

    /**
     * Entity in an XBRL instance context
     */
    final class XbrliEntity private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == XbrliEntityEName)
    }

    /**
     * Period in an XBRL instance context
     */
    final class XbrliPeriod private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == XbrliPeriodEName)

      def isInstant: Boolean = findChildElem(XbrliInstantEName).isDefined

      def isFiniteDuration: Boolean =
        findChildElem(XbrliStartDateEName).isDefined && findChildElem(XbrliEndDateEName).isDefined

      def isForever: Boolean = findChildElem(XbrliForeverEName).isDefined
    }

    /**
     * Scenario in an XBRL instance context
     */
    final class XbrliScenario private[Xbrl] (
      override val wrappedElem: indexed.Elem,
      childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(wrappedElem, childElems) {

      assert(wrappedElem.resolvedName == XbrliScenarioEName)
    }

    object XbrliElem {

      /**
       * Returns true if the element can be a top-level fact, without consulting the taxonomy, but just by looking at the
       * structure of the XBRL instance itself. Only the Path of the element is taken into account.
       */
      def canBeTopLevelFact(wrappedElem: indexed.Elem): Boolean = {
        require(
          wrappedElem.rootElem.resolvedName == XbrliXbrlEName,
          s"The root element must be $XbrliXbrlEName but found ${wrappedElem.rootElem.resolvedName} instead")

        canBeFact(wrappedElem) && (wrappedElem.path.entries.size == 1)
      }

      /**
       * Returns true if the element can be a fact, without consulting the taxonomy, but just by looking at the
       * structure of the XBRL instance itself. Only the Path of the element is taken into account.
       */
      def canBeFact(wrappedElem: indexed.Elem): Boolean = {
        require(
          wrappedElem.rootElem.resolvedName == XbrliXbrlEName,
          s"The root element must be $XbrliXbrlEName but found ${wrappedElem.rootElem.resolvedName} instead")

        val path = wrappedElem.path

        (path.entries.size >= 1) && {
          val topLevelAncestorPath = path.findAncestorOrSelfPath(_.entries.size == 1).get
          val namespaceUriOption = topLevelAncestorPath.elementNameOption.get.namespaceUriOption

          (namespaceUriOption != Some(LinkNs)) && (namespaceUriOption != Some(XbrliNs))
        }
      }

      /**
       * Expensive recursive factory method for XbrliElem instances.
       */
      def apply(wrappedElem: indexed.Elem): XbrliElem = {
        require(
          wrappedElem.rootElem.resolvedName == XbrliXbrlEName,
          s"The root element must be $XbrliXbrlEName but found ${wrappedElem.rootElem.resolvedName} instead")

        // Recursive calls
        val childElems = wrappedElem.findAllChildElems.map(e => apply(e))

        wrappedElem.resolvedName match {
          case XbrliXbrlEName => new XbrlInstance(wrappedElem, childElems)
          case LinkSchemaRefEName => new SchemaRef(wrappedElem, childElems)
          case LinkLinkbaseRefEName => new LinkbaseRef(wrappedElem, childElems)
          case LinkRoleRefEName => new RoleRef(wrappedElem, childElems)
          case LinkArcroleRefEName => new ArcroleRef(wrappedElem, childElems)
          case XbrliContextEName => new XbrliContext(wrappedElem, childElems)
          case XbrliUnitEName => new XbrliUnit(wrappedElem, childElems)
          case LinkFootnoteLinkEName => new FootnoteLink(wrappedElem, childElems)
          case XbrliEntityEName => new XbrliEntity(wrappedElem, childElems)
          case XbrliPeriodEName => new XbrliPeriod(wrappedElem, childElems)
          case XbrliScenarioEName => new XbrliScenario(wrappedElem, childElems)
          case _ => wrappedElem match {
            case e if canBeFact(e) =>
              if (e.attributeOption(ContextRefEName).isDefined) new ItemFact(e, childElems)
              else new TupleFact(e, childElems)
            case _ => new XbrliElem(wrappedElem, childElems) {}
          }
        }
      }
    }
  }
}
