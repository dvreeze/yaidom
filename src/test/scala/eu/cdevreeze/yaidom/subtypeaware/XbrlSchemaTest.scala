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

package eu.cdevreeze.yaidom
package subtypeaware

import scala.collection.immutable
import scala.reflect.classTag

import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import SubtypeAwareParentElemApi.anyElem
import eu.cdevreeze.yaidom.Document
import eu.cdevreeze.yaidom.EName
import eu.cdevreeze.yaidom.IsNavigable
import eu.cdevreeze.yaidom.Path
import eu.cdevreeze.yaidom.indexed
import parse.DocumentParserUsingSax

/**
 * Test case using yaidom sub-type-aware elements for XBRL schema processing.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlSchemaTest extends Suite {

  import XbrlSchemaTest._

  val xsNs = "http://www.w3.org/2001/XMLSchema"

  def testQueryXbrlSchema(): Unit = {
    val parser = DocumentParserUsingSax.newInstance()
    val doc: Document = parser.parse(classOf[XbrlSchemaTest].getResourceAsStream("gaap.xsd"))

    val xbrlSchemaDoc = indexed.Document(doc)
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

    val paths = xbrlSchema.wrappedElem.elem.findAllElemOrSelfPaths

    assertResult(paths) {
      xbrlSchema.findAllElemsOrSelfOfType(classTag[XsdElem]) map { _.wrappedElem.path }
    }

    val elemsContainingPlus =
      xbrlSchema.filterElemsOfType(classTag[XsdElem]) { e =>
        e.attributeOption(EName("name")).getOrElse("").contains("Plus")
      }
    val pathsOfElemsContainingPlus =
      xbrlSchema.wrappedElem.elem filterElemPaths { e => e.attributeOption(EName("name")).getOrElse("").contains("Plus") }

    assertResult(pathsOfElemsContainingPlus) {
      elemsContainingPlus map (_.wrappedElem.path)
    }

    assertResult(true) {
      elemsContainingPlus forall { e =>
        val indexedElem = e.wrappedElem
        indexedElem.rootElem.findElemOrSelfByPath(indexedElem.path) == Some(indexedElem.elem)
      }
    }
  }

  def testQueryMultipleXsds(): Unit = {
    val parser = DocumentParserUsingSax.newInstance()
    val ipoDoc: Document = parser.parse(classOf[XbrlSchemaTest].getResourceAsStream("ipo.xsd"))
    val addressDoc: Document = parser.parse(classOf[XbrlSchemaTest].getResourceAsStream("address.xsd"))

    val ipoSchemaDoc = indexed.Document(ipoDoc)
    val ipoSchema: XsdRootElem = new XsdRootElem(ipoSchemaDoc.documentElement)

    val addressSchemaDoc = indexed.Document(addressDoc)
    val addressSchema: XsdRootElem = new XsdRootElem(addressSchemaDoc.documentElement)

    val tns = ipoSchema.targetNamespaceOption.getOrElse("")

    val elemDecls = ipoSchema.findAllElemsOfType(classTag[GlobalElementDeclaration])

    assertResult(Set(EName(tns, "purchaseOrder"), EName(tns, "comment"))) {
      elemDecls.map(_.targetEName).toSet
    }

    val itemsTypeDefOption = ipoSchema.findChildElemOfType(classTag[XsdElem]) { elem =>
      elem.resolvedName == EName(xsNs, "complexType") && elem.attributeOption(EName("name")) == Some("Items")
    }

    assertResult(true) {
      itemsTypeDefOption.isDefined
    }
    assertResult(itemsTypeDefOption.get.wrappedElem) {
      val result =
        ipoSchema getChildElem { elem =>
          elem.resolvedName == EName(xsNs, "complexType") && elem.attributeOption(EName("name")) == Some("Items")
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

  class XsdElem(val wrappedElem: indexed.Elem) extends ElemLike[XsdElem] with HasEName with IsNavigable[XsdElem] with SubtypeAwareParentElemLike[XsdElem] {

    override def findAllChildElems: immutable.IndexedSeq[XsdElem] =
      wrappedElem.findAllChildElems.map(e => XsdElem(e))

    override def resolvedName: EName = wrappedElem.resolvedName

    override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = wrappedElem.resolvedAttributes

    override def findChildElemByPathEntry(entry: Path.Entry): Option[XsdElem] =
      wrappedElem.findChildElemByPathEntry(entry).map(elem => XsdElem(elem))
  }

  final class XsdRootElem(wrappedElem: indexed.Elem) extends XsdElem(wrappedElem) {
    require(resolvedName == EName(nsSchema, "schema"))
    require(wrappedElem.path.isRoot)

    def targetNamespaceOption: Option[String] = attributeOption(EName("targetNamespace"))
  }

  final class GlobalElementDeclaration(wrappedElem: indexed.Elem) extends XsdElem(wrappedElem) {
    require(resolvedName == EName(nsSchema, "element"))
    require(wrappedElem.path.entries.size == 1)

    def targetEName: EName = {
      val tnsOption = wrappedElem.rootElem.attributeOption(EName("targetNamespace"))
      EName(tnsOption, wrappedElem.attribute(EName("name")))
    }

    def typeAttrOption: Option[EName] = {
      wrappedElem.elem.attributeAsResolvedQNameOption(EName("type"))
    }
  }

  final class ElementReference(wrappedElem: indexed.Elem) extends XsdElem(wrappedElem) {
    require(resolvedName == EName(nsSchema, "element"))
    require(wrappedElem.path.entries.size >= 2)

    def ref: EName = wrappedElem.elem.attributeAsResolvedQName(EName("ref"))
  }

  object XsdElem {

    def apply(elem: indexed.Elem): XsdElem = elem.resolvedName match {
      case EName(nsSchema, "schema") => new XsdRootElem(elem)
      case EName(nsSchema, "element") if elem.path.entries.size == 1 =>
        new GlobalElementDeclaration(elem)
      case EName(nsSchema, "element") if elem.elem.attributeAsResolvedQNameOption(EName("ref")).isDefined =>
        new ElementReference(elem)
      case _ => new XsdElem(elem)
    }
  }
}
