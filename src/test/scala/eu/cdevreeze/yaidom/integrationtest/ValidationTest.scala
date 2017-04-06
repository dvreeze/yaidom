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

import scala.Vector
import scala.collection.immutable

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.NodeBuilder.elem
import eu.cdevreeze.yaidom.simple.NodeBuilder.textElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax

/**
 * Test case using yaidom for validation of XML documents, playing with an "alternative schema language" (inspired by Relax NG and
 * Schematron).
 *
 * Acknowledgments: This test uses material from http://www.w3.org/TR/xmlschema-0/.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ValidationTest extends FunSuite {

  import ValidationTest._

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  // Specific validators for this "schema", putting the DSL into action. In other words, the functions below ARE the "schema".

  import NameOccurrences._

  def validateUSAddress(elm: Elem): ValidationResult = {
    val validator =
      and(
        sequence(
          one(EName("name")) validatedBy { e => validateStringElem(e) },
          one(EName("street")) validatedBy { e => validateStringElem(e) },
          one(EName("city")) validatedBy { e => validateStringElem(e) },
          one(EName("state")) validatedBy { e => validateStringElem(e) },
          one(EName("zip")) validatedBy { e => validateStringElem(e) }),
        hasAttribute(EName("country")))
    validator.apply(elm)
  }

  def validateItems(elm: Elem): ValidationResult = {
    val validator =
      sequence(
        zeroOrMore(EName("item")) validatedBy (and(
          sequence(
            one(EName("productName")) validatedBy { e => validateStringElem(e) },
            one(EName("quantity")) validatedBy { e => validateStringElem(e) },
            one(EName("USPrice")) validatedBy { e => validateStringElem(e) },
            zeroOrOne(EName("comment")) validatedBy { e => validateStringElem(e) },
            zeroOrOne(EName("shipDate")) validatedBy { e => validateStringElem(e) }),
          hasAttribute(EName("partNum")))))
    validator.apply(elm)
  }

  def validatePurchaseOrder(elm: Elem): ValidationResult = {
    val validator =
      and(
        sequence(
          one(EName("shipTo")) validatedBy { e => validateUSAddress(e) },
          one(EName("billTo")) validatedBy { e => validateUSAddress(e) },
          zeroOrOne(EName("comment")) validatedBy { e => validateStringElem(e) },
          one(EName("items")) validatedBy { e => validateItems(e) }),
        hasAttribute(EName("orderDate")))
    validator.apply(elm)
  }

  def validateRootElement(elm: Elem): ValidationResult = {
    val validator =
      and(
        hasName(EName("purchaseOrder")),
        hasAttribute(EName("orderDate")),
        validatePurchaseOrder _)
    validator.apply(elm)
  }

  // The tests themselves

  test("testValidateAddress") {
    val elmBuilder =
      elem(
        qname = QName("Address"),
        attributes = Vector(QName("country") -> "US"),
        children = Vector(
          textElem(QName("name"), "Alice Smith"),
          textElem(QName("street"), "123 Maple Street"),
          textElem(QName("city"), "Mill Valley"),
          textElem(QName("state"), "CA"),
          textElem(QName("zip"), "90952")))
    val addressElm = elmBuilder.build()

    val validationResult = validateUSAddress(addressElm)

    assertResult(true) {
      validationResult.success
    }

    val wrongAddressElm = addressElm.plusChild(textElem(QName("bogus"), "true").build())

    val nonOkValidationResult = validateUSAddress(wrongAddressElm)

    assertResult(false) {
      nonOkValidationResult.success
    }

    val anotherWrongAddressElm = addressElm.plusChild(2, textElem(QName("bogus"), "true").build())

    val anotherNonOkValidationResult = validateUSAddress(anotherWrongAddressElm)

    assertResult(false) {
      anotherNonOkValidationResult.success
    }
  }

  test("testValidatePurchaseOrder") {
    val docParser = DocumentParserUsingSax.newInstance
    val is = classOf[ValidationTest].getResourceAsStream("po.xml")
    val doc = docParser.parse(is)

    val validationResult = validateRootElement(doc.documentElement)

    assertResult(true) {
      validationResult.success
    }

    val extraCommentElm = textElem(QName("comment"), "That's it for now, folks").build()
    val wrongDoc = doc.withDocumentElement(doc.documentElement.plusChild(extraCommentElm))

    val nonOkValidationResult = validateRootElement(wrongDoc.documentElement)

    assertResult(false) {
      nonOkValidationResult.success
    }
  }
}

object ValidationTest {

  final class ValidationResult private (val success: Boolean, val messages: immutable.Seq[String])

  object ValidationResult {

    val ok: ValidationResult = new ValidationResult(true, immutable.Seq())

    def notOk(message: String): ValidationResult = new ValidationResult(false, Vector(message))

    def notOk(messages: immutable.Seq[String]): ValidationResult = new ValidationResult(false, messages)
  }

  /** The central validator type */
  type ElemValidator = (Elem => ValidationResult)

  /** Given an EName, its minOccurs and maxOccurs (None for unbounded maxOccurs) */
  final case class NameOccurrences(name: EName, minOccurs: Int, maxOccursOption: Option[Int]) {

    def validatedBy(validator: ElemValidator): NameOccurrencesWithValidator = new NameOccurrencesWithValidator(this, validator)
  }

  object NameOccurrences {

    def one(name: EName): NameOccurrences = NameOccurrences(name, 1, Some(1))

    def zeroOrOne(name: EName): NameOccurrences = NameOccurrences(name, 0, Some(1))

    def zeroOrMore(name: EName): NameOccurrences = NameOccurrences(name, 0, None)
  }

  final class NameOccurrencesWithValidator(val nameOccurrences: NameOccurrences, val validator: ElemValidator)

  // "Schema language" as DSL, containing higher-order functions on validators

  def hasName(ename: EName): ElemValidator = { elm: Elem =>
    val success = elm.resolvedName == ename
    if (success) ValidationResult.ok
    else ValidationResult.notOk(s"Expected name $ename but found name ${elm.resolvedName} instead")
  }

  def hasAttribute(ename: EName): ElemValidator = { elm: Elem =>
    val success = elm.attributeOption(ename).isDefined
    if (success) ValidationResult.ok else ValidationResult.notOk(s"Attribute $ename not found")
  }

  def sequence(nameOccsWithValidators: NameOccurrencesWithValidator*): ElemValidator = { elm: Elem =>
    var remainingChildElms = elm.findAllChildElems
    var lastValidationResult = ValidationResult.ok

    for (nameOccsWithValidator <- nameOccsWithValidators) {
      if (lastValidationResult.success) {
        val matchingChildElms = remainingChildElms takeWhile { e => e.resolvedName == nameOccsWithValidator.nameOccurrences.name }

        if (matchingChildElms.size < nameOccsWithValidator.nameOccurrences.minOccurs ||
          nameOccsWithValidator.nameOccurrences.maxOccursOption.getOrElse(java.lang.Integer.MAX_VALUE) < matchingChildElms.size) {

          lastValidationResult = ValidationResult.notOk(s"Too few or too many ${nameOccsWithValidator.nameOccurrences.name} elements")
        } else {
          for (elm <- matchingChildElms) {
            if (lastValidationResult.success) {
              lastValidationResult = nameOccsWithValidator.validator(elm)
            }
          }
        }

        remainingChildElms = remainingChildElms dropWhile { e => e.resolvedName == nameOccsWithValidator.nameOccurrences.name }
      }
    }

    if (lastValidationResult.success && remainingChildElms.nonEmpty) {
      lastValidationResult = ValidationResult.notOk("There were remaining (non-matched) elements")
    }

    lastValidationResult
  }

  def and(validators: ElemValidator*): ElemValidator = {
    require(validators.size >= 1, "Expected at least one validator")

    { e: Elem =>
      validators map (vld => vld(e)) find (vldRes => !vldRes.success) getOrElse (ValidationResult.ok)
    }
  }

  def validateStringElem(elm: Elem): ValidationResult = {
    val ok = elm.findAllChildElems.isEmpty
    if (ok) ValidationResult.ok else ValidationResult.notOk(s"${elm.resolvedName} is not a string element")
  }
}
