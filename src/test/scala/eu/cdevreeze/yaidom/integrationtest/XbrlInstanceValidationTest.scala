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

package eu.cdevreeze.yaidom.integrationtest

import scala.collection.immutable
import scala.reflect.classTag

import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ElemApi.anyElem
import eu.cdevreeze.yaidom.queryapi.IndexedScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike

/**
 * Test case using yaidom XML dialect support for "schema validation" of XBRL instance w.r.t. the syntactic structure.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceValidationTest extends Suite {
  import XbrlInstanceValidationTest._

  def testValidateInstance(): Unit = {
    val parser = DocumentParserUsingSax.newInstance()

    val doc: Document =
      parser.parse(classOf[XbrlInstanceValidationTest].getResourceAsStream("sample-xbrl-instance.xml"))

    val caDoc = indexed.Document(doc)

    val xbrliElem = XbrliElem(caDoc.documentElement)

    val xbrlInstance = xbrliElem.findElemOrSelfOfType(classTag[XbrliXbrlElem])(anyElem).get

    assertResult(xbrliElem) {
      xbrlInstance
    }

    assertResult(true) {
      xbrlInstance.findAllFacts.size >= 100
    }

    assertResult(xbrlInstance.findAllFacts.map(_.underlyingElem)) {
      xbrlInstance.findAllItems.map(_.underlyingElem)
    }

    assertResult(Vector()) {
      xbrlInstance.findAllTuples
    }

    import Validation._

    assertResult(OkResult) {
      validateSequenceOccurringOnce(
        xbrlInstance.findAllChildElems,
        Vector(
          atLeastOnce(hasEName(LinkSchemaRefEName)),
          atLeastZeroTimes(hasEName(LinkLinkbaseRefEName)),
          atLeastZeroTimes(hasEName(LinkRoleRefEName)),
          atLeastZeroTimes(hasEName(LinkArcroleRefEName)),
          { elems =>
            val remainder = elems dropWhile {
              case e: XbrliFactElem        => true
              case e: XbrliContextElem     => true
              case e: XbrliUnitElem        => true
              case e: LinkFootnoteLinkElem => true
              case e                       => false
            }
            (remainder.isEmpty, remainder)
          }),
        { elems => s"The child elements of the xbrl root element are not conform the specified sequence" })
    }
  }
}

object XbrlInstanceValidationTest {

  val XsNamespace = "http://www.w3.org/2001/XMLSchema"
  val XbrliNamespace = "http://www.xbrl.org/2003/instance"
  val LinkNamespace = "http://www.xbrl.org/2003/linkbase"
  val XLinkNamespace = "http://www.w3.org/1999/xlink"

  val XbrliXbrlEName = EName(XbrliNamespace, "xbrl")
  val XbrliContextEName = EName(XbrliNamespace, "context")
  val XbrliUnitEName = EName(XbrliNamespace, "unit")
  val XbrliEntityEName = EName(XbrliNamespace, "entity")
  val XbrliPeriodEName = EName(XbrliNamespace, "period")
  val XbrliScenarioEName = EName(XbrliNamespace, "scenario")
  val XbrliInstantEName = EName(XbrliNamespace, "instant")
  val XbrliStartDateEName = EName(XbrliNamespace, "startDate")
  val XbrliEndDateEName = EName(XbrliNamespace, "endDate")
  val XbrliForeverEName = EName(XbrliNamespace, "forever")
  val XbrliIdentifierEName = EName(XbrliNamespace, "identifier")
  val XbrliSegmentEName = EName(XbrliNamespace, "segment")
  val XbrliMeasureEName = EName(XbrliNamespace, "measure")
  val XbrliDivideEName = EName(XbrliNamespace, "divide")
  val XbrliUnitNumeratorEName = EName(XbrliNamespace, "unitNumerator")
  val XbrliUnitDenominatorEName = EName(XbrliNamespace, "unitDenominator")

  val LinkSchemaRefEName = EName(LinkNamespace, "schemaRef")
  val LinkLinkbaseRefEName = EName(LinkNamespace, "linkbaseRef")
  val LinkRoleRefEName = EName(LinkNamespace, "roleRef")
  val LinkArcroleRefEName = EName(LinkNamespace, "arcroleRef")
  val LinkFootnoteLinkEName = EName(LinkNamespace, "footnoteLink")

  val ContextRefEName = EName("contextRef")
  val UnitRefEName = EName("unitRef")

  /**
   * Type-safe XBRL instance DOM element, knowing nothing about the taxonomy, and knowing nothing about validity.
   * Only XML well-formedness is assumed. Therefore this class hierarchy is useful for validation of the XBRL instance
   * against the schema etc.
   *
   * XBRL instances can be embedded so it is not assumed that the root element is xbrli:xbrl.
   *
   * Note that the underlying element must know its ancestry-or-self in order to recognize elements that must represent facts.
   *
   * Also note that the underlying element is of ANY element type that is a sub-type of IndexedScopedElemApi.
   * Hence, the XBRL instance data model does not depend on any particular backing element implementation, and
   * indeed multiple backing element implementations can be plugged in. Also note that the underlying IndexedScopedElemApi
   * backing element contract does not force the XBRL instance data model to use generics. That makes pattern matching
   * on those XBRL data model classes easier.
   *
   * Creating this class hierarchy is an effort, but it is a one-time effort potentially paying off very many times.
   */
  sealed class XbrliElem(val underlyingElem: IndexedScopedElemApi) extends ScopedElemLike with SubtypeAwareElemLike {

    type ThisElem = XbrliElem

    def thisElem: ThisElem = this

    def findAllChildElems: immutable.IndexedSeq[XbrliElem] = {
      underlyingElem.findAllChildElems.map(e => XbrliElem(e))
    }

    def resolvedName: EName = underlyingElem.resolvedName

    def resolvedAttributes: immutable.Iterable[(EName, String)] = underlyingElem.resolvedAttributes

    def text: String = underlyingElem.text

    def scope: Scope = underlyingElem.scope

    def qname: QName = underlyingElem.qname

    def attributes: immutable.Iterable[(QName, String)] = underlyingElem.attributes
  }

  final class XbrliXbrlElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliXbrlEName)

    def findAllFacts: immutable.IndexedSeq[XbrliFactElem] = {
      findAllElemsOfType(classTag[XbrliFactElem])
    }

    def findAllItems: immutable.IndexedSeq[XbrliItemElem] = {
      findAllElemsOfType(classTag[XbrliItemElem])
    }

    def findAllTuples: immutable.IndexedSeq[XbrliTupleElem] = {
      findAllElemsOfType(classTag[XbrliTupleElem])
    }
  }

  final class XbrliContextElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliContextEName)
  }

  final class XbrliUnitElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliUnitEName)
  }

  /**
   * An element which must be an item or a tuple, due to `mustBeFact` returning true.
   */
  sealed abstract class XbrliFactElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(XbrliElem.mustBeFact(underlyingElem))
  }

  /**
   * An element which must be an item, due to `mustBeItem` returning true.
   */
  final class XbrliItemElem(underlyingElem: IndexedScopedElemApi) extends XbrliFactElem(underlyingElem) {
    require(XbrliElem.mustBeItem(underlyingElem))
  }

  /**
   * An element which must be a tuple, due to `mustBeTuple` returning true.
   */
  final class XbrliTupleElem(underlyingElem: IndexedScopedElemApi) extends XbrliFactElem(underlyingElem) {
    require(XbrliElem.mustBeTuple(underlyingElem))
  }

  final class XbrliEntityElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliEntityEName)
  }

  final class XbrliPeriodElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliPeriodEName)
  }

  final class XbrliScenarioElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliScenarioEName)
  }

  final class XbrliInstantElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliInstantEName)
  }

  final class XbrliStartDateElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliStartDateEName)
  }

  final class XbrliEndDateElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliEndDateEName)
  }

  final class XbrliForeverElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliForeverEName)
  }

  final class XbrliIdentifierElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliIdentifierEName)
  }

  final class XbrliSegmentElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliSegmentEName)
  }

  final class XbrliMeasureElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliMeasureEName)
  }

  final class XbrliDivideElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliDivideEName)
  }

  final class XbrliUnitNumeratorElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliUnitNumeratorEName)
  }

  final class XbrliUnitDenominatorElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == XbrliUnitDenominatorEName)
  }

  final class LinkSchemaRefElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == LinkSchemaRefEName)
  }

  final class LinkLinkbaseRefElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == LinkLinkbaseRefEName)
  }

  final class LinkRoleRefElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == LinkRoleRefEName)
  }

  final class LinkArcroleRefElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == LinkArcroleRefEName)
  }

  final class LinkFootnoteLinkElem(underlyingElem: IndexedScopedElemApi) extends XbrliElem(underlyingElem) {
    require(resolvedName == LinkFootnoteLinkEName)
  }

  object XbrliElem {

    val DisallowedFactAncestorOrSelfENames =
      Set(LinkSchemaRefEName, LinkLinkbaseRefEName, LinkRoleRefEName, LinkArcroleRefEName, XbrliContextEName, XbrliUnitEName, LinkFootnoteLinkEName)

    /**
     * Returns true if the element is not xbrli:xbrl, and also not a descendant-or-self of an element with an EName in DisallowedFactAncestorOrSelfENames.
     */
    def mustBeFact(e: IndexedScopedElemApi): Boolean = {
      if (e.resolvedName == XbrliXbrlEName) false
      else {
        val notAllowed = e.path.entries exists { entry =>
          DisallowedFactAncestorOrSelfENames.contains(entry.elementName)
        }
        !notAllowed
      }
    }

    /**
     * Returns `mustBeFact(e) && e.attributeOption(ContextRefEName).isDefined`.
     */
    def mustBeItem(e: IndexedScopedElemApi): Boolean = {
      mustBeFact(e) && e.attributeOption(ContextRefEName).isDefined
    }

    /**
     * Returns `mustBeFact(e) && e.attributeOption(ContextRefEName).isEmpty`.
     */
    def mustBeTuple(e: IndexedScopedElemApi): Boolean = {
      mustBeFact(e) && e.attributeOption(ContextRefEName).isEmpty
    }

    def apply(e: IndexedScopedElemApi): XbrliElem = {
      if (mustBeFact(e)) {
        if (e.attributeOption(ContextRefEName).isDefined) new XbrliItemElem(e) else new XbrliTupleElem(e)
      } else {
        e.resolvedName match {
          case XbrliXbrlEName            => new XbrliXbrlElem(e)
          case XbrliContextEName         => new XbrliContextElem(e)
          case XbrliUnitEName            => new XbrliUnitElem(e)
          case XbrliEntityEName          => new XbrliEntityElem(e)
          case XbrliPeriodEName          => new XbrliPeriodElem(e)
          case XbrliScenarioEName        => new XbrliScenarioElem(e)
          case XbrliInstantEName         => new XbrliInstantElem(e)
          case XbrliStartDateEName       => new XbrliStartDateElem(e)
          case XbrliEndDateEName         => new XbrliEndDateElem(e)
          case XbrliForeverEName         => new XbrliForeverElem(e)
          case XbrliIdentifierEName      => new XbrliIdentifierElem(e)
          case XbrliSegmentEName         => new XbrliSegmentElem(e)
          case XbrliMeasureEName         => new XbrliMeasureElem(e)
          case XbrliDivideEName          => new XbrliDivideElem(e)
          case XbrliUnitNumeratorEName   => new XbrliUnitNumeratorElem(e)
          case XbrliUnitDenominatorEName => new XbrliUnitDenominatorElem(e)
          case LinkSchemaRefEName        => new LinkSchemaRefElem(e)
          case LinkLinkbaseRefEName      => new LinkLinkbaseRefElem(e)
          case LinkRoleRefEName          => new LinkRoleRefElem(e)
          case LinkArcroleRefEName       => new LinkArcroleRefElem(e)
          case LinkFootnoteLinkEName     => new LinkFootnoteLinkElem(e)
          case _                         => new XbrliElem(e)
        }
      }
    }
  }

  sealed trait ValidationResult

  case object OkResult extends ValidationResult

  final case class ErrorResult(val code: String, val message: String) extends ValidationResult

  final case class ValidationResultSeq(val results: immutable.IndexedSeq[ValidationResult])

  type ElemValidation = XbrliElem => ValidationResultSeq

  type ElemSeqValidation = immutable.IndexedSeq[XbrliElem] => ValidationResultSeq

  type BooleanWithRemainder = (Boolean, immutable.IndexedSeq[XbrliElem])

  type ValidatingElemConsumer = immutable.IndexedSeq[XbrliElem] => BooleanWithRemainder

  object Validation {

    def validateSequenceOccurringOnce(
      elems: immutable.IndexedSeq[XbrliElem],
      validatingConsumers: immutable.IndexedSeq[ValidatingElemConsumer],
      makeErrorMessage: immutable.IndexedSeq[XbrliElem] => String): ValidationResult = {

      val (isValid, remainder) =
        validatingConsumers.foldLeft((true, elems)) {
          case ((stillValid, accRemainder), validatingConsumer) =>
            if (stillValid) {
              validatingConsumer(accRemainder)
            } else {
              (stillValid, accRemainder)
            }
        }

      require(!isValid || remainder.isEmpty)

      if (isValid) {
        OkResult
      } else {
        ErrorResult(
          "incorrect-sequence",
          makeErrorMessage(elems))
      }
    }

    def atLeastOnce(p: XbrliElem => Boolean): ValidatingElemConsumer = {
      { elems =>
        val remaining = elems.dropWhile(p)
        (remaining.size <= elems.size - 1, remaining)
      }
    }

    def atLeastZeroTimes(p: XbrliElem => Boolean): ValidatingElemConsumer = {
      { elems =>
        val remaining = elems.dropWhile(p)
        (true, remaining)
      }
    }

    def hasEName(ename: EName)(elem: XbrliElem): Boolean = {
      elem.resolvedName == ename
    }
  }
}
