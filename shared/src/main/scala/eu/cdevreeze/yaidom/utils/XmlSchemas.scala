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

package eu.cdevreeze.yaidom.utils

import scala.collection.immutable
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemLike

/**
 * Simple (package-private) utility for querying XML schemas for element declarations, attribute declarations, etc.
 *
 * @author Chris de Vreeze
 */
@deprecated(message = "No longer needed when the deprecated ENameProviderUtils and QNameProviderUtils are no longer used", since = "1.8.0")
private[utils] object XmlSchemas {

  private val scope = Scope.from("xs" -> "http://www.w3.org/2001/XMLSchema")

  /**
   * Any element in an xs:schema (including xs:schema itself).
   */
  sealed class XsdElem private[utils] (val elem: BackingElemApi) extends SubtypeAwareElemLike with ClarkElemLike {

    type ThisElem = XsdElem

    def thisElem: ThisElem = this

    final override def findAllChildElems: immutable.IndexedSeq[XsdElem] = {
      elem.findAllChildElems.map(e => XsdElem(e))
    }

    final override def resolvedName: EName = elem.resolvedName

    final override def resolvedAttributes: immutable.Iterable[(EName, String)] = elem.resolvedAttributes

    final override def text: String = elem.text
  }

  /**
   * The xs:schema element
   */
  final class SchemaRoot private[utils] (elem: BackingElemApi) extends XsdElem(elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(resolvedName == QName("xs", "schema").res, s"Expected xs:schema, but found ${resolvedName}")
    require(elem.path.entries.isEmpty, s"Expected root element, but found path ${elem.path}")

    def targetNamespaceOption: Option[String] =
      (this \@ QName("targetNamespace").res)

    def targetNamespacePrefixOption: Option[String] = {
      val tnsOption = targetNamespaceOption
      tnsOption flatMap (tns => elem.scope.withoutDefaultNamespace.inverse.getOrElse(tns, Set[String]()).headOption)
    }

    def elementFormDefaultOption: Option[String] =
      (this \@ QName("elementFormDefault").res)

    def attributeFormDefaultOption: Option[String] =
      (this \@ QName("attributeFormDefault").res)

    def findAllGlobalElementDeclarations(): immutable.IndexedSeq[GlobalElementDeclaration] = {
      findAllChildElemsOfType(classTag[GlobalElementDeclaration])
    }

    def findGlobalElementDeclaration(targetEName: EName): Option[GlobalElementDeclaration] = {
      findChildElemOfType(classTag[GlobalElementDeclaration]) { elem => elem.targetEName == targetEName }
    }

    def findAllGlobalAttributeDeclarations(): immutable.IndexedSeq[GlobalAttributeDeclaration] = {
      findAllChildElemsOfType(classTag[GlobalAttributeDeclaration])
    }

    def findGlobalAttributeDeclaration(targetEName: EName): Option[GlobalAttributeDeclaration] = {
      findChildElemOfType(classTag[GlobalAttributeDeclaration]) { elem => elem.targetEName == targetEName }
    }

    def findAllLocalElementDeclarations(): immutable.IndexedSeq[LocalElementDeclaration] = {
      findAllElemsOfType(classTag[LocalElementDeclaration])
    }

    def findAllLocalAttributeDeclarations(): immutable.IndexedSeq[LocalAttributeDeclaration] = {
      findAllElemsOfType(classTag[LocalAttributeDeclaration])
    }
  }

  /**
   * A top-level xs:element
   */
  final class GlobalElementDeclaration private[utils] (elem: BackingElemApi) extends XsdElem(elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(resolvedName == QName("xs", "element").res, s"Expected xs:element, but found ${resolvedName}")
    require(elem.path.entries.size == 1, s"Expected top-level element declaration, but found path ${elem.path}")

    def name: String =
      (this \@ QName("name").res).getOrElse(sys.error(s"Expected @name in ${elem.resolvedName}"))

    def targetEName: EName = {
      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      tnsOption.map(tns => EName(tns, name)).getOrElse(EName(name))
    }

    def typeAttributeOption: Option[EName] = {
      elem.attributeAsResolvedQNameOption(QName("type").res)
    }

    def substitutionGroupOption: Option[EName] = {
      elem.attributeAsResolvedQNameOption(QName("substitutionGroup").res)
    }
  }

  /**
   * A local xs:element
   */
  final class LocalElementDeclaration private[utils] (elem: BackingElemApi) extends XsdElem(elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(resolvedName == QName("xs", "element").res, s"Expected xs:element, but found ${resolvedName}")
    require(elem.path.entries.size > 1, s"Expected local element declaration, but found path ${elem.path}")

    def name: String =
      (this \@ QName("name").res).getOrElse(sys.error(s"Expected @name in ${elem.resolvedName}"))

    def targetEName: EName = {
      val elementFormDefaultOption = (elem.rootElem \@ QName("elementFormDefault").res).map(s => s == "qualified")
      val elementFormOption = (this \@ QName("form").res).map(s => s == "qualified")
      val elementFormQualified = elementFormOption.getOrElse(elementFormDefaultOption.getOrElse(false))

      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      if (elementFormQualified) EName(tnsOption, name) else EName(name)
    }

    def typeAttributeOption: Option[EName] = {
      elem.attributeAsResolvedQNameOption(QName("type").res)
    }
  }

  /**
   * An xs:element referring to a global element declaration
   */
  final class ElementReference private[utils] (elem: BackingElemApi) extends XsdElem(elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(resolvedName == QName("xs", "element").res, s"Expected xs:element, but found ${resolvedName}")
    require(elem.path.entries.size > 1, s"Expected element reference, but found path ${elem.path}")

    def ref: EName = {
      elem.attributeAsResolvedQName(QName("ref").res)
    }
  }

  /**
   * A top-level xs:attribute
   */
  final class GlobalAttributeDeclaration private[utils] (elem: BackingElemApi) extends XsdElem(elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(resolvedName == QName("xs", "attribute").res, s"Expected xs:attribute, but found ${resolvedName}")
    require(elem.path.entries.size == 1, s"Expected top-level attribute declaration, but found path ${elem.path}")

    def name: String =
      (this \@ QName("name").res).getOrElse(sys.error(s"Expected @name in ${elem.resolvedName}"))

    def targetEName: EName = {
      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      tnsOption.map(tns => EName(tns, name)).getOrElse(EName(name))
    }

    def typeAttributeOption: Option[EName] = {
      elem.attributeAsResolvedQNameOption(QName("type").res)
    }
  }

  /**
   * A local xs:attribute
   */
  final class LocalAttributeDeclaration private[utils] (elem: BackingElemApi) extends XsdElem(elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(resolvedName == QName("xs", "attribute").res, s"Expected xs:attribute, but found ${resolvedName}")
    require(elem.path.entries.size > 1, s"Expected local attribute declaration, but found path ${elem.path}")

    def name: String =
      (this \@ QName("name").res).getOrElse(sys.error(s"Expected @name in ${elem.resolvedName}"))

    def targetEName: EName = {
      val attributeFormDefaultOption = (elem.rootElem \@ QName("attributeFormDefault").res).map(s => s == "qualified")
      val attributeFormOption = (this \@ QName("form").res).map(s => s == "qualified")
      val attributeFormQualified = attributeFormOption.getOrElse(attributeFormDefaultOption.getOrElse(false))

      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      if (attributeFormQualified) EName(tnsOption, name) else EName(name)
    }

    def typeAttributeOption: Option[EName] = {
      elem.attributeAsResolvedQNameOption(QName("type").res)
    }
  }

  /**
   * An xs:attribute referring to a global attribute declaration
   */
  final class AttributeReference private[utils] (elem: BackingElemApi) extends XsdElem(elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(resolvedName == QName("xs", "attribute").res, s"Expected xs:attribute, but found ${resolvedName}")
    require(elem.path.entries.size > 1, s"Expected attribute reference, but found path ${elem.path}")

    def ref: EName = {
      elem.attributeAsResolvedQName(QName("ref").res)
    }
  }

  trait XsdElemFactory[B <: XsdElem] {

    def tryToCreate(elem: BackingElemApi): Option[B]
  }

  object XsdElem {

    def apply(elem: BackingElemApi): XsdElem = {
      SchemaRoot.tryToCreate(elem).
        orElse(GlobalElementDeclaration.tryToCreate(elem)).
        orElse(LocalElementDeclaration.tryToCreate(elem)).
        orElse(ElementReference.tryToCreate(elem)).
        orElse(GlobalAttributeDeclaration.tryToCreate(elem)).
        orElse(LocalAttributeDeclaration.tryToCreate(elem)).
        orElse(AttributeReference.tryToCreate(elem)).
        getOrElse(new XsdElem(elem))
    }
  }

  object SchemaRoot extends XsdElemFactory[SchemaRoot] {

    def apply(elem: BackingElemApi): SchemaRoot = new SchemaRoot(elem)

    def tryToCreate(elem: BackingElemApi): Option[SchemaRoot] = {
      import scope._

      if (elem.resolvedName == QName("xs", "schema").res) Some(new SchemaRoot(elem)) else None
    }
  }

  object GlobalElementDeclaration extends XsdElemFactory[GlobalElementDeclaration] {

    def tryToCreate(elem: BackingElemApi): Option[GlobalElementDeclaration] = {
      import scope._

      if (elem.resolvedName == QName("xs", "element").res && elem.path.entries.size == 1) {
        Some(new GlobalElementDeclaration(elem))
      } else {
        None
      }
    }
  }

  object LocalElementDeclaration extends XsdElemFactory[LocalElementDeclaration] {

    def tryToCreate(elem: BackingElemApi): Option[LocalElementDeclaration] = {
      import scope._

      if (elem.resolvedName == QName("xs", "element").res &&
        elem.path.entries.size >= 2 &&
        elem.attributeOption(EName("ref")).isEmpty) {
        Some(new LocalElementDeclaration(elem))
      } else {
        None
      }
    }
  }

  object ElementReference extends XsdElemFactory[ElementReference] {

    def tryToCreate(elem: BackingElemApi): Option[ElementReference] = {
      import scope._

      if (elem.resolvedName == QName("xs", "element").res &&
        elem.path.entries.size >= 2 &&
        elem.attributeOption(EName("ref")).isDefined) {
        Some(new ElementReference(elem))
      } else {
        None
      }
    }
  }

  object GlobalAttributeDeclaration extends XsdElemFactory[GlobalAttributeDeclaration] {

    def tryToCreate(elem: BackingElemApi): Option[GlobalAttributeDeclaration] = {
      import scope._

      if (elem.resolvedName == QName("xs", "attribute").res && elem.path.entries.size == 1) {
        Some(new GlobalAttributeDeclaration(elem))
      } else {
        None
      }
    }
  }

  object LocalAttributeDeclaration extends XsdElemFactory[LocalAttributeDeclaration] {

    def tryToCreate(elem: BackingElemApi): Option[LocalAttributeDeclaration] = {
      import scope._

      if (elem.resolvedName == QName("xs", "attribute").res &&
        elem.path.entries.size >= 2 &&
        elem.attributeOption(EName("ref")).isEmpty) {
        Some(new LocalAttributeDeclaration(elem))
      } else {
        None
      }
    }
  }

  object AttributeReference extends XsdElemFactory[AttributeReference] {

    def tryToCreate(elem: BackingElemApi): Option[AttributeReference] = {
      import scope._

      if (elem.resolvedName == QName("xs", "attribute").res &&
        elem.path.entries.size >= 2 &&
        elem.attributeOption(EName("ref")).isDefined) {
        Some(new AttributeReference(elem))
      } else {
        None
      }
    }
  }
}
