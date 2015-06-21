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

package eu.cdevreeze.yaidom.blogcode

import scala.BigDecimal
import scala.collection.immutable
import scala.reflect.classTag
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ElemApi.anyElem
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike
import eu.cdevreeze.yaidom.indexed
import java.net.URI
import AbstractBlogXbrlTestSupport._

/**
 * Super-trait of BlogXbrlTest, containing an XBRL instance model offering the yaidom query API.
 *
 * @author Chris de Vreeze
 */
trait AbstractBlogXbrlTestSupport {

  /**
   * XML element inside XBRL instance (or the entire XBRL instance itself). This API is immutable.
   *
   * The `SubtypeAwareElemApi` API is offered.
   *
   * Also note that the package-private constructor contains redundant data, in order to speed up (yaidom-based) querying.
   *
   * These XBRL instance elements are just an XBRL instance view on the underlying "indexed" element tree, and
   * therefore do not know about the taxonomy describing the XBRL instance (other than the href to the DTS entrypoint).
   *
   * As a consequence, this model must recognize facts by only looking at the elements and their ancestry, without knowing
   * anything about the substitution groups of the corresponding concept declarations. Fortunately, the XBRL instance
   * schema (xbrl-instance-2003-12-31.xsd) and the specification of allowed XBRL tuple content are (almost) restrictive enough
   * in order to recognize facts.
   *
   * It is even possible to easily distinguish between item facts and tuple facts, based on the presence or absence of the
   * contextRef attribute. There is one complication, though, and that is nil item and tuple facts. Unfortunately, concept
   * declarations in taxonomy schemas may have the nillable attribute set to true. This led to some clutter in the
   * inheritance hierarchy for numeric item facts.
   *
   * Hence, regarding nil facts, the user of the API is responsible for keeping in mind that facts can indeed be nil facts
   * (which facts are easy to filter away).
   *
   * @author Chris de Vreeze
   */
  sealed class XbrliElem private[blogcode] (
    val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends ScopedElemLike[XbrliElem] with SubtypeAwareElemLike[XbrliElem] {

    require(childElems.map(_.indexedElem) == indexedElem.findAllChildElems)

    /**
     * Very fast implementation of findAllChildElems, for fast querying
     */
    final def findAllChildElems: immutable.IndexedSeq[XbrliElem] = childElems

    final def resolvedName: EName = indexedElem.resolvedName

    final def resolvedAttributes: immutable.Iterable[(EName, String)] = indexedElem.resolvedAttributes

    final def qname: QName = indexedElem.qname

    final def attributes: immutable.Iterable[(QName, String)] = indexedElem.attributes

    final def scope: Scope = indexedElem.scope

    final def text: String = indexedElem.text

    final override def equals(other: Any): Boolean = other match {
      case e: XbrliElem => indexedElem == e.indexedElem
      case _            => false
    }

    final override def hashCode: Int = indexedElem.hashCode
  }

  /**
   * XBRL instance.
   *
   * It does not check validity of the XBRL instance. Neither does it know about the DTS describing the XBRL instance.
   * It does, however, contain the entrypoint URI(s) to the DTS.
   *
   * Without any knowledge about the DTS, this class only recognizes (item and tuple) facts by looking at the
   * structure of the element and its ancestry. Attribute @contextRef is only allowed for item facts, and tuple facts can be
   * recognized by looking at the "path" of the element.
   *
   * @author Chris de Vreeze
   */
  final class XbrlInstance private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliXbrlEName, s"Expected EName $XbrliXbrlEName but found $resolvedName")

    val allContexts: immutable.IndexedSeq[XbrliContext] =
      findAllChildElemsOfType(classTag[XbrliContext])

    val allContextsById: Map[String, XbrliContext] =
      allContexts.groupBy(_.id) mapValues (_.head)

    val allUnits: immutable.IndexedSeq[XbrliUnit] =
      findAllChildElemsOfType(classTag[XbrliUnit])

    val allUnitsById: Map[String, XbrliUnit] =
      allUnits.groupBy(_.id) mapValues (_.head)

    val allTopLevelFacts: immutable.IndexedSeq[Fact] =
      findAllChildElemsOfType(classTag[Fact])

    val allTopLevelItems: immutable.IndexedSeq[ItemFact] =
      findAllChildElemsOfType(classTag[ItemFact])

    val allTopLevelTuples: immutable.IndexedSeq[TupleFact] =
      findAllChildElemsOfType(classTag[TupleFact])

    val allTopLevelFactsByEName: Map[EName, immutable.IndexedSeq[Fact]] =
      allTopLevelFacts groupBy (_.resolvedName)

    val allTopLevelItemsByEName: Map[EName, immutable.IndexedSeq[ItemFact]] =
      allTopLevelItems groupBy (_.resolvedName)

    val allTopLevelTuplesByEName: Map[EName, immutable.IndexedSeq[TupleFact]] =
      allTopLevelTuples groupBy (_.resolvedName)

    def filterTopLevelFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
      filterChildElemsOfType(classTag[Fact])(p)
    }

    def filterTopLevelItems(p: ItemFact => Boolean): immutable.IndexedSeq[ItemFact] = {
      filterChildElemsOfType(classTag[ItemFact])(p)
    }

    def filterTopLevelTuples(p: TupleFact => Boolean): immutable.IndexedSeq[TupleFact] = {
      filterChildElemsOfType(classTag[TupleFact])(p)
    }

    def findAllFacts: immutable.IndexedSeq[Fact] = {
      findAllElemsOfType(classTag[Fact])
    }

    def findAllItems: immutable.IndexedSeq[ItemFact] = {
      findAllElemsOfType(classTag[ItemFact])
    }

    def findAllTuples: immutable.IndexedSeq[TupleFact] = {
      findAllElemsOfType(classTag[TupleFact])
    }

    def filterFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
      filterElemsOfType(classTag[Fact])(p)
    }

    def filterItems(p: ItemFact => Boolean): immutable.IndexedSeq[ItemFact] = {
      filterElemsOfType(classTag[ItemFact])(p)
    }

    def filterTuples(p: TupleFact => Boolean): immutable.IndexedSeq[TupleFact] = {
      filterElemsOfType(classTag[TupleFact])(p)
    }

    def findAllSchemaRefs: immutable.IndexedSeq[SchemaRef] = {
      findAllChildElemsOfType(classTag[SchemaRef])
    }

    def findAllLinkbaseRefs: immutable.IndexedSeq[LinkbaseRef] = {
      findAllChildElemsOfType(classTag[LinkbaseRef])
    }

    def findAllRoleRefs: immutable.IndexedSeq[RoleRef] = {
      findAllChildElemsOfType(classTag[RoleRef])
    }

    def findAllArcroleRefs: immutable.IndexedSeq[ArcroleRef] = {
      findAllChildElemsOfType(classTag[ArcroleRef])
    }

    def findAllFootnoteLinks: immutable.IndexedSeq[FootnoteLink] = {
      findAllChildElemsOfType(classTag[FootnoteLink])
    }
  }

  /**
   * SchemaRef in an XBRL instance
   *
   * @author Chris de Vreeze
   */
  final class SchemaRef private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == LinkSchemaRefEName, s"Expected EName $LinkSchemaRefEName but found $resolvedName")
  }

  /**
   * LinkbaseRef in an XBRL instance
   *
   * @author Chris de Vreeze
   */
  final class LinkbaseRef private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == LinkLinkbaseRefEName, s"Expected EName $LinkLinkbaseRefEName but found $resolvedName")
  }

  /**
   * RoleRef in an XBRL instance
   *
   * @author Chris de Vreeze
   */
  final class RoleRef private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == LinkRoleRefEName, s"Expected EName $LinkRoleRefEName but found $resolvedName")
  }

  /**
   * ArcroleRef in an XBRL instance
   *
   * @author Chris de Vreeze
   */
  final class ArcroleRef private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == LinkArcroleRefEName, s"Expected EName $LinkArcroleRefEName but found $resolvedName")
  }

  /**
   * Context in an XBRL instance
   *
   * @author Chris de Vreeze
   */
  final class XbrliContext private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliContextEName, s"Expected EName $XbrliContextEName but found $resolvedName")

    def id: String = attribute(IdEName)

    def entity: Entity = {
      getChildElemOfType(classTag[Entity])(anyElem)
    }

    def period: Period = {
      getChildElemOfType(classTag[Period])(anyElem)
    }

    def scenarioOption: Option[Scenario] = {
      findChildElemOfType(classTag[Scenario])(anyElem)
    }
  }

  /**
   * Unit in an XBRL instance
   *
   * @author Chris de Vreeze
   */
  final class XbrliUnit private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliUnitEName, s"Expected EName $XbrliUnitEName but found $resolvedName")

    def id: String = attribute(IdEName)

    def measures: immutable.IndexedSeq[EName] = {
      filterChildElems(XbrliMeasureEName) map (e => e.textAsResolvedQName)
    }

    def divide: Divide = {
      getChildElemOfType(classTag[Divide])(anyElem)
    }
  }

  /**
   * Item or tuple fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
   *
   * @author Chris de Vreeze
   */
  abstract class Fact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    def isTopLevel: Boolean = indexedElem.path.entries.size == 1

    def isNil: Boolean = attributeOption(XsiNilEName) == Some("true")
  }

  /**
   * Item fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
   *
   * @author Chris de Vreeze
   */
  abstract class ItemFact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends Fact(indexedElem, childElems) {

    require(attributeOption(ContextRefEName).isDefined, s"Expected attribute $ContextRefEName")

    def contextRef: String = attribute(ContextRefEName)
  }

  /**
   * Non-numeric item fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
   *
   * @author Chris de Vreeze
   */
  final class NonNumericItemFact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends ItemFact(indexedElem, childElems) {

    require(attributeOption(UnitRefEName).isEmpty, s"Expected no attribute $UnitRefEName")
  }

  /**
   * Numeric item fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
   *
   * @author Chris de Vreeze
   */
  abstract class NumericItemFact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends ItemFact(indexedElem, childElems) {

    require(attributeOption(UnitRefEName).isDefined, s"Expected attribute $UnitRefEName")

    def unitRef: String = attribute(UnitRefEName)
  }

  /**
   * Nil numeric item fact in an XBRL instance, either top-level or nested
   *
   * @author Chris de Vreeze
   */
  final class NilNumericItemFact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends NumericItemFact(indexedElem, childElems) {

    require(isNil, s"Expected nil numeric item fact")
  }

  /**
   * Non-nil non-fraction numeric item fact in an XBRL instance, either top-level or nested
   *
   * @author Chris de Vreeze
   */
  final class NonNilNonFractionNumericItemFact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends NumericItemFact(indexedElem, childElems) {

    require(!isNil, s"Expected non-nil numeric item fact")

    def precisionOption: Option[String] = attributeOption(PrecisionEName)

    def decimalsOption: Option[String] = attributeOption(DecimalsEName)
  }

  /**
   * Non-nil fraction item fact in an XBRL instance, either top-level or nested
   *
   * @author Chris de Vreeze
   */
  final class NonNilFractionItemFact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends NumericItemFact(indexedElem, childElems) {

    require(!isNil, s"Expected non-nil numeric item fact")

    require(findAllChildElems.map(_.resolvedName).toSet == Set(XbrliNumeratorEName, XbrliDenominatorEName))

    def numerator: BigDecimal = {
      val s = getChildElem(XbrliNumeratorEName).text
      BigDecimal(s)
    }

    def denominator: BigDecimal = {
      val s = getChildElem(XbrliDenominatorEName).text
      BigDecimal(s)
    }
  }

  /**
   * Tuple fact in an XBRL instance, either top-level or nested (and either non-nil or nil)
   *
   * @author Chris de Vreeze
   */
  final class TupleFact private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends Fact(indexedElem, childElems) {

    def findAllChildFacts: immutable.IndexedSeq[Fact] = {
      findAllChildElemsOfType(classTag[Fact])
    }

    def findAllFacts: immutable.IndexedSeq[Fact] = {
      findAllElemsOfType(classTag[Fact])
    }

    def filterChildFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
      filterChildElemsOfType(classTag[Fact])(p)
    }

    def filterFacts(p: Fact => Boolean): immutable.IndexedSeq[Fact] = {
      filterElemsOfType(classTag[Fact])(p)
    }
  }

  /**
   * FootnoteLink in an XBRL instance
   *
   * @author Chris de Vreeze
   */
  final class FootnoteLink private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == LinkFootnoteLinkEName, s"Expected EName $LinkFootnoteLinkEName but found $resolvedName")
  }

  /**
   * Entity in an XBRL instance context
   *
   * @author Chris de Vreeze
   */
  final class Entity private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliEntityEName, s"Expected EName $XbrliEntityEName but found $resolvedName")

    def identifier: Identifier = {
      getChildElemOfType(classTag[Identifier])(anyElem)
    }

    def segmentOption: Option[Segment] = {
      findChildElemOfType(classTag[Segment])(anyElem)
    }
  }

  /**
   * Period in an XBRL instance context
   *
   * TODO sub-traits
   *
   * @author Chris de Vreeze
   */
  final class Period private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliPeriodEName, s"Expected EName $XbrliPeriodEName but found $resolvedName")

    def isInstant: Boolean = {
      findChildElem(XbrliInstantEName).isDefined
    }

    def isFiniteDuration: Boolean = {
      findChildElem(XbrliStartDateEName).isDefined
    }

    def isForever: Boolean = {
      findChildElem(XbrliForeverEName).isDefined
    }
  }

  /**
   * Scenario in an XBRL instance context
   *
   * @author Chris de Vreeze
   */
  final class Scenario private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliScenarioEName, s"Expected EName $XbrliScenarioEName but found $resolvedName")
  }

  /**
   * Segment in an XBRL instance context entity
   *
   * @author Chris de Vreeze
   */
  final class Segment private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliSegmentEName, s"Expected EName $XbrliSegmentEName but found $resolvedName")
  }

  /**
   * Identifier in an XBRL instance context entity
   *
   * @author Chris de Vreeze
   */
  final class Identifier private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliIdentifierEName, s"Expected EName $XbrliIdentifierEName but found $resolvedName")
  }

  /**
   * Divide in an XBRL instance unit
   *
   * @author Chris de Vreeze
   */
  final class Divide private[blogcode] (
    override val indexedElem: indexed.Elem,
    childElems: immutable.IndexedSeq[XbrliElem]) extends XbrliElem(indexedElem, childElems) {

    require(resolvedName == XbrliDivideEName, s"Expected EName $XbrliDivideEName but found $resolvedName")

    def numerator: immutable.IndexedSeq[EName] = {
      val unitNumerator = getChildElem(XbrliUnitNumeratorEName)
      val result = unitNumerator.filterChildElems(XbrliMeasureEName).map(e => e.textAsResolvedQName)
      result
    }

    def denominator: immutable.IndexedSeq[EName] = {
      val unitDenominator = getChildElem(XbrliUnitDenominatorEName)
      val result = unitDenominator.filterChildElems(XbrliMeasureEName).map(e => e.textAsResolvedQName)
      result
    }
  }

  object XbrliElem {

    /**
     * Expensive method to create an XbrliElem tree
     */
    def apply(elem: indexed.Elem): XbrliElem = {
      // Recursive calls
      val childElems = elem.findAllChildElems.map(e => apply(e))
      apply(elem, childElems)
    }

    private[blogcode] def apply(elem: indexed.Elem, childElems: immutable.IndexedSeq[XbrliElem]): XbrliElem = elem.resolvedName match {
      case XbrliXbrlEName          => new XbrlInstance(elem, childElems)
      case LinkSchemaRefEName      => new SchemaRef(elem, childElems)
      case LinkLinkbaseRefEName    => new LinkbaseRef(elem, childElems)
      case LinkRoleRefEName        => new RoleRef(elem, childElems)
      case LinkArcroleRefEName     => new ArcroleRef(elem, childElems)
      case XbrliContextEName       => new XbrliContext(elem, childElems)
      case XbrliUnitEName          => new XbrliUnit(elem, childElems)
      case LinkFootnoteLinkEName   => new FootnoteLink(elem, childElems)
      case XbrliEntityEName        => new Entity(elem, childElems)
      case XbrliPeriodEName        => new Period(elem, childElems)
      case XbrliScenarioEName      => new Scenario(elem, childElems)
      case XbrliSegmentEName       => new Segment(elem, childElems)
      case XbrliIdentifierEName    => new Identifier(elem, childElems)
      case XbrliDivideEName        => new Divide(elem, childElems)
      case _ if Fact.accepts(elem) => Fact(elem, childElems)
      case _                       => new XbrliElem(elem, childElems)
    }
  }

  object XbrlInstance {

    def apply(elem: indexed.Elem): XbrlInstance = {
      require(elem.resolvedName == XbrliXbrlEName)
      XbrliElem.apply(elem).asInstanceOf[XbrlInstance]
    }
  }

  object Fact {

    def accepts(elem: indexed.Elem): Boolean = ItemFact.accepts(elem) || TupleFact.accepts(elem)

    private[blogcode] def apply(elem: indexed.Elem, childElems: immutable.IndexedSeq[XbrliElem]): Fact =
      if (ItemFact.accepts(elem)) ItemFact(elem, childElems) else TupleFact(elem, childElems)

    def isFactPath(path: Path): Boolean = {
      !path.isRoot &&
        !Set(Option(LinkNs), Option(XbrliNs)).contains(path.firstEntry.elementName.namespaceUriOption)
    }
  }

  object ItemFact {

    def accepts(elem: indexed.Elem): Boolean = {
      Fact.isFactPath(elem.path) &&
        elem.attributeOption(ContextRefEName).isDefined
    }

    private[blogcode] def apply(elem: indexed.Elem, childElems: immutable.IndexedSeq[XbrliElem]): ItemFact = {
      require(Fact.isFactPath(elem.path))
      require(elem.attributeOption(ContextRefEName).isDefined)

      val unitRefOption = elem.attributeOption(UnitRefEName)

      if (unitRefOption.isEmpty) new NonNumericItemFact(elem, childElems)
      else {
        if (elem.attributeOption(XsiNilEName) == Some("true"))
          new NilNumericItemFact(elem, childElems)
        else if (elem.findChildElem(withEName(XbrliNumeratorEName)).isDefined)
          new NonNilFractionItemFact(elem, childElems)
        else
          new NonNilNonFractionNumericItemFact(elem, childElems)
      }
    }
  }

  object TupleFact {

    def accepts(elem: indexed.Elem): Boolean = {
      Fact.isFactPath(elem.path) &&
        elem.attributeOption(ContextRefEName).isEmpty
    }

    private[blogcode] def apply(elem: indexed.Elem, childElems: immutable.IndexedSeq[XbrliElem]): TupleFact = {
      require(Fact.isFactPath(elem.path))
      require(elem.attributeOption(ContextRefEName).isEmpty)

      new TupleFact(elem, childElems)
    }
  }
}

object AbstractBlogXbrlTestSupport {

  val XbrliNs = "http://www.xbrl.org/2003/instance"
  val LinkNs = "http://www.xbrl.org/2003/linkbase"
  val XmlNs = "http://www.w3.org/XML/1998/namespace"
  val XsiNs = "http://www.w3.org/2001/XMLSchema-instance"
  val XbrldiNs = "http://xbrl.org/2006/xbrldi"

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
  val XbrliNumeratorEName = EName(XbrliNs, "numerator")
  val XbrliDenominatorEName = EName(XbrliNs, "denominator")
  val XbrliUnitNumeratorEName = EName(XbrliNs, "unitNumerator")
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
  val PrecisionEName = EName("precision")
  val DecimalsEName = EName("decimals")
}
