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

package eu.cdevreeze.yaidom.queryapitests

import scala.collection.immutable
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.ElemApi.anyElem
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike
import eu.cdevreeze.yaidom.simple.Document
import org.scalatest.funsuite.AnyFunSuite

/**
 * Test case using yaidom sub-type-aware elements for XBRL schema processing.
 *
 * @author Chris de Vreeze
 */
class XbrlSchemaTest extends AnyFunSuite {

  import XbrlSchemaTest._

  val xsNs = "http://www.w3.org/2001/XMLSchema"

  test("testQueryXbrlSchema") {
    val parser = DocumentParserUsingSax.newInstance()
    val doc: eu.cdevreeze.yaidom.simple.Document =
      parser.parse(classOf[XbrlSchemaTest].getResourceAsStream("gaap.xsd"))

    val xbrlSchemaDoc = eu.cdevreeze.yaidom.indexed.Document(doc)
    val xbrlSchema: XsdRootElem = new XsdRootElem(xbrlSchemaDoc.documentElement)

    // Check concepts

    val elmDefs = xbrlSchema.findAllElemsOfType(classTag[GlobalElementDeclaration])

    assertResult(true) {
      elmDefs.size >= 100
    }
    assertResult(elmDefs.map(_.wrappedElem)) {
      xbrlSchema.filterElemsOfType(classTag[GlobalElementDeclaration])(anyElem).map(_.wrappedElem)
    }
    assertResult(elmDefs.map(_.wrappedElem)) {
      xbrlSchema.filterElemsOrSelfOfType(classTag[GlobalElementDeclaration])(anyElem).map(_.wrappedElem)
    }
    assertResult(elmDefs.map(_.wrappedElem)) {
      xbrlSchema.filterChildElemsOfType(classTag[GlobalElementDeclaration])(anyElem).map(_.wrappedElem)
    }
    assertResult(elmDefs.map(_.wrappedElem)) {
      xbrlSchema.findAllElemsOrSelfOfType(classTag[GlobalElementDeclaration]).map(_.wrappedElem)
    }
    assertResult(elmDefs.map(_.wrappedElem)) {
      xbrlSchema.findAllChildElemsOfType(classTag[GlobalElementDeclaration]).map(_.wrappedElem)
    }

    assertResult(Nil) {
      xbrlSchema.findAllChildElemsOfType(classTag[ElementReference])
    }

    val tns = "http://xasb.org/gaap"

    assertResult(tns) {
      xbrlSchema.targetNamespaceOption.getOrElse("")
    }

    assertResult(Set(EName(tns, "AMinusMinusMember"), EName(tns, "APlusPlusPlusMember"))) {
      val conceptENames = elmDefs map { e =>
        e.targetEName
      }
      val matchingConceptENames =
        conceptENames filter { ename => Set("AMinusMinusMember", "APlusPlusPlusMember").contains(ename.localPart) }
      matchingConceptENames.toSet
    }

    // Check equivalence of different ways to get the same concepts

    val paths = xbrlSchema.wrappedElem.findAllElemsOrSelf.map(_.path)

    assertResult(paths) {
      xbrlSchema.findAllElemsOrSelfOfType(classTag[XsdElem]) map {
        _.wrappedElem.path
      }
    }

    val elemsContainingPlus =
      xbrlSchema.filterElemsOfType(classTag[XsdElem]) { e =>
        e.attributeOption(EName("name")).getOrElse("").contains("Plus")
      }
    val pathsOfElemsContainingPlus =
      xbrlSchema.wrappedElem filterElems { e => e.attributeOption(EName("name")).getOrElse("").contains("Plus") } map (_.path)

    assertResult(pathsOfElemsContainingPlus) {
      elemsContainingPlus map (_.wrappedElem.path)
    }

    assertResult(true) {
      elemsContainingPlus forall { e =>
        val indexedElem = e.wrappedElem
        indexedElem.underlyingRootElem.findElemOrSelfByPath(indexedElem.path).contains(indexedElem.underlyingElem)
      }
    }
  }

  test("testQueryMultipleXsds") {
    val parser = DocumentParserUsingSax.newInstance()
    val ipoDoc: Document = parser.parse(classOf[XbrlSchemaTest].getResourceAsStream("ipo.xsd"))

    // Why parse and not use this XSD?
    parser.parse(classOf[XbrlSchemaTest].getResourceAsStream("address.xsd"))

    val ipoSchemaDoc = eu.cdevreeze.yaidom.indexed.Document(ipoDoc)
    val ipoSchema: XsdRootElem = new XsdRootElem(ipoSchemaDoc.documentElement)

    val tns = ipoSchema.targetNamespaceOption.getOrElse("")

    val elemDecls = ipoSchema.findAllElemsOfType(classTag[GlobalElementDeclaration])

    assertResult(Set(EName(tns, "purchaseOrder"), EName(tns, "comment"))) {
      elemDecls.map(_.targetEName).toSet
    }

    val itemsTypeDefOption = ipoSchema.findChildElemOfType(classTag[XsdElem]) { elem =>
      elem.resolvedName == EName(xsNs, "complexType") && elem.attributeOption(EName("name")).contains("Items")
    }

    assertResult(true) {
      itemsTypeDefOption.isDefined
    }
    assertResult(itemsTypeDefOption.get.wrappedElem) {
      val result =
        ipoSchema getChildElem { elem =>
          elem.resolvedName == EName(xsNs, "complexType") && elem.attributeOption(EName("name")).contains("Items")
        }
      result.wrappedElem
    }

    val itemsTypeDef = itemsTypeDefOption.get

    assertResult(1) {
      itemsTypeDef.findAllElemsOfType(classTag[ElementReference]).size
    }
    val firstElementReference = itemsTypeDef.findAllElemsOfType(classTag[ElementReference]).head

    assertResult(Some(firstElementReference.wrappedElem)) {
      itemsTypeDef.findElemOfType(classTag[ElementReference])(anyElem).map(_.wrappedElem)
    }
    assertResult(Some(firstElementReference.wrappedElem)) {
      itemsTypeDef.findElemOrSelfOfType(classTag[ElementReference])(anyElem).map(_.wrappedElem)
    }
    assertResult(Some(firstElementReference.wrappedElem)) {
      itemsTypeDef.findTopmostElemsOfType(classTag[ElementReference])(anyElem).headOption.map(_.wrappedElem)
    }
    assertResult(Some(firstElementReference.wrappedElem)) {
      itemsTypeDef.findTopmostElemsOrSelfOfType(classTag[ElementReference])(anyElem).headOption.map(_.wrappedElem)
    }
  }
}

object XbrlSchemaTest {

  val nsSchema = "http://www.w3.org/2001/XMLSchema"
  val nsXbrli = "http://www.xbrl.org/2003/instance"
  val nsLink = "http://www.xbrl.org/2003/linkbase"
  val nsXLink = "http://www.w3.org/1999/xlink"

  class XsdElem(val wrappedElem: eu.cdevreeze.yaidom.indexed.Elem) extends SubtypeAwareElemLike with ClarkElemLike {

    type ThisElem = XsdElem

    def thisElem: ThisElem = this

    override def findAllChildElems: immutable.IndexedSeq[XsdElem] =
      wrappedElem.findAllChildElems.map(e => XsdElem(e))

    override def resolvedName: EName = wrappedElem.resolvedName

    override def resolvedAttributes: immutable.Iterable[(EName, String)] = wrappedElem.resolvedAttributes

    override def text: String = wrappedElem.text
  }

  final class XsdRootElem(wrappedElem: eu.cdevreeze.yaidom.indexed.Elem) extends XsdElem(wrappedElem) {
    require(resolvedName == EName(nsSchema, "schema"))
    require(wrappedElem.path.isEmpty)

    def targetNamespaceOption: Option[String] = attributeOption(EName("targetNamespace"))
  }

  final class GlobalElementDeclaration(wrappedElem: eu.cdevreeze.yaidom.indexed.Elem) extends XsdElem(wrappedElem) {
    require(resolvedName == EName(nsSchema, "element"))
    require(wrappedElem.path.entries.size == 1)

    def targetEName: EName = {
      val tnsOption = wrappedElem.rootElem.attributeOption(EName("targetNamespace"))
      EName(tnsOption, wrappedElem.attribute(EName("name")))
    }

    def typeAttrOption: Option[EName] = {
      wrappedElem.underlyingElem.attributeAsResolvedQNameOption(EName("type"))
    }
  }

  final class ElementReference(wrappedElem: eu.cdevreeze.yaidom.indexed.Elem) extends XsdElem(wrappedElem) {
    require(resolvedName == EName(nsSchema, "element"))
    require(wrappedElem.path.entries.size >= 2)

    def ref: EName = wrappedElem.underlyingElem.attributeAsResolvedQName(EName("ref"))
  }

  object XsdElem {

    def apply(elem: eu.cdevreeze.yaidom.indexed.Elem): XsdElem = elem.resolvedName match {
      case EName(nsSchema, "schema") => new XsdRootElem(elem)
      case EName(nsSchema, "element") if elem.path.entries.size == 1 =>
        new GlobalElementDeclaration(elem)
      case EName(nsSchema, "element") if elem.underlyingElem.attributeAsResolvedQNameOption(EName("ref")).isDefined =>
        new ElementReference(elem)
      case _ => new XsdElem(elem)
    }
  }

}
