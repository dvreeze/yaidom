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
package utils

import scala.collection.immutable

/**
 * Simple (package-private) utility for querying XML schemas for element declarations, attribute declarations, etc.
 *
 * @author Chris de Vreeze
 */
private[utils] object XmlSchemas {

  private val scope = Scope.from("xs" -> "http://www.w3.org/2001/XMLSchema")

  /**
   * The xs:schema element
   */
  final class SchemaRoot(val elem: indexed.Elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(elem.resolvedName == QName("xs", "schema").res, s"Expected xs:schema, but found ${elem.resolvedName}")
    require(elem.path.entries.isEmpty, s"Expected root element, but found path ${elem.path}")

    def targetNamespaceOption: Option[String] =
      (elem \@ QName("targetNamespace").res)

    def elementFormDefaultOption: Option[String] =
      (elem \@ QName("elementFormDefault").res)

    def attributeFormDefaultOption: Option[String] =
      (elem \@ QName("attributeFormDefault").res)

    def findAllGlobalElementDeclarations(): immutable.IndexedSeq[GlobalElementDeclaration] = {
      elem.filterChildElems(QName("xs", "element").res).map(e => e.toGlobalElementDeclaration)
    }

    def findGlobalElementDeclaration(targetEName: EName): Option[GlobalElementDeclaration] = {
      val likelyResultOption =
        elem findChildElem { e =>
          (e.resolvedName == QName("xs", "element").res) && (e \@ QName("name").res) == Some(targetEName.localPart)
        }
      likelyResultOption.map(e => e.toGlobalElementDeclaration).filter(e => e.targetEName == targetEName)
    }

    def findAllGlobalAttributeDeclarations(): immutable.IndexedSeq[GlobalAttributeDeclaration] = {
      elem.filterChildElems(QName("xs", "attribute").res).map(e => e.toGlobalAttributeDeclaration)
    }

    def findGlobalAttributeDeclaration(targetEName: EName): Option[GlobalAttributeDeclaration] = {
      val likelyResultOption =
        elem findChildElem { e =>
          (e.resolvedName == QName("xs", "attribute").res) && (e \@ QName("name").res) == Some(targetEName.localPart)
        }
      likelyResultOption.map(e => e.toGlobalAttributeDeclaration).filter(e => e.targetEName == targetEName)
    }

    def findAllLocalElementDeclarations(): immutable.IndexedSeq[LocalElementDeclaration] = {
      elem filterElems { e =>
        (e.resolvedName == QName("xs", "element").res) && (e.path.entries.size >= 2) && ((e \@ QName("ref").res).isEmpty)
      } map { e =>
        e.toLocalElementDeclaration
      }
    }

    def findAllLocalAttributeDeclarations(): immutable.IndexedSeq[LocalAttributeDeclaration] = {
      elem filterElems { e =>
        (e.resolvedName == QName("xs", "attribute").res) && (e.path.entries.size >= 2) && ((e \@ QName("ref").res).isEmpty)
      } map { e =>
        e.toLocalAttributeDeclaration
      }
    }
  }

  implicit class ToSchemaRoot(val elem: indexed.Elem) extends AnyVal {

    def toSchemaRoot: SchemaRoot = new SchemaRoot(elem)
  }

  /**
   * A top-level xs:element
   */
  final class GlobalElementDeclaration(val elem: indexed.Elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(elem.resolvedName == QName("xs", "element").res, s"Expected xs:element, but found ${elem.resolvedName}")
    require(elem.path.entries.size == 1, s"Expected top-level element declaration, but found path ${elem.path}")

    def name: String =
      (elem \@ QName("name").res).getOrElse(sys.error(s"Expected @name, but found ${elem.elem}"))

    def targetEName: EName = {
      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      tnsOption.map(tns => EName(tns, name)).getOrElse(EName(name))
    }

    def typeAttributeOption: Option[EName] = {
      elem.elem.attributeAsResolvedQNameOption(QName("type").res)
    }

    def substitutionGroupOption: Option[EName] = {
      elem.elem.attributeAsResolvedQNameOption(QName("substitutionGroup").res)
    }
  }

  implicit class ToGlobalElementDeclaration(val elem: indexed.Elem) extends AnyVal {

    def toGlobalElementDeclaration: GlobalElementDeclaration = new GlobalElementDeclaration(elem)
  }

  /**
   * A local xs:element
   */
  final class LocalElementDeclaration(val elem: indexed.Elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(elem.resolvedName == QName("xs", "element").res, s"Expected xs:element, but found ${elem.resolvedName}")
    require(elem.path.entries.size > 1, s"Expected local element declaration, but found path ${elem.path}")

    def name: String =
      (elem \@ QName("name").res).getOrElse(sys.error(s"Expected @name, but found ${elem.elem}"))

    def targetEName: EName = {
      val elementFormDefaultOption = (elem.rootElem \@ QName("elementFormDefault").res).map(s => s == "qualified")
      val elementFormOption = (elem \@ QName("form").res).map(s => s == "qualified")
      val elementFormQualified = elementFormOption.getOrElse(elementFormDefaultOption.getOrElse(false))

      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      if (elementFormQualified) EName(tnsOption, name) else EName(name)
    }

    def typeAttributeOption: Option[EName] = {
      elem.elem.attributeAsResolvedQNameOption(QName("type").res)
    }
  }

  implicit class ToLocalElementDeclaration(val elem: indexed.Elem) extends AnyVal {

    def toLocalElementDeclaration: LocalElementDeclaration = new LocalElementDeclaration(elem)
  }

  /**
   * An xs:element referring to a global element declaration
   */
  final class ElementReference(val elem: indexed.Elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(elem.resolvedName == QName("xs", "element").res, s"Expected xs:element, but found ${elem.resolvedName}")
    require(elem.path.entries.size > 1, s"Expected element reference, but found path ${elem.path}")

    def ref: EName = {
      elem.elem.attributeAsResolvedQName(QName("ref").res)
    }
  }

  implicit class ToElementReference(val elem: indexed.Elem) extends AnyVal {

    def toElementReference: ElementReference = new ElementReference(elem)
  }

  /**
   * A top-level xs:attribute
   */
  final class GlobalAttributeDeclaration(val elem: indexed.Elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(elem.resolvedName == QName("xs", "attribute").res, s"Expected xs:attribute, but found ${elem.resolvedName}")
    require(elem.path.entries.size == 1, s"Expected top-level attribute declaration, but found path ${elem.path}")

    def name: String =
      (elem \@ QName("name").res).getOrElse(sys.error(s"Expected @name, but found ${elem.elem}"))

    def targetEName: EName = {
      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      tnsOption.map(tns => EName(tns, name)).getOrElse(EName(name))
    }

    def typeAttributeOption: Option[EName] = {
      elem.elem.attributeAsResolvedQNameOption(QName("type").res)
    }
  }

  implicit class ToGlobalAttributeDeclaration(val elem: indexed.Elem) extends AnyVal {

    def toGlobalAttributeDeclaration: GlobalAttributeDeclaration = new GlobalAttributeDeclaration(elem)
  }

  /**
   * A local xs:attribute
   */
  final class LocalAttributeDeclaration(val elem: indexed.Elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(elem.resolvedName == QName("xs", "attribute").res, s"Expected xs:attribute, but found ${elem.resolvedName}")
    require(elem.path.entries.size > 1, s"Expected local attribute declaration, but found path ${elem.path}")

    def name: String =
      (elem \@ QName("name").res).getOrElse(sys.error(s"Expected @name, but found ${elem.elem}"))

    def targetEName: EName = {
      val attributeFormDefaultOption = (elem.rootElem \@ QName("attributeFormDefault").res).map(s => s == "qualified")
      val attributeFormOption = (elem \@ QName("form").res).map(s => s == "qualified")
      val attributeFormQualified = attributeFormOption.getOrElse(attributeFormDefaultOption.getOrElse(false))

      val tnsOption = (elem.rootElem \@ QName("targetNamespace").res)
      if (attributeFormQualified) EName(tnsOption, name) else EName(name)
    }

    def typeAttributeOption: Option[EName] = {
      elem.elem.attributeAsResolvedQNameOption(QName("type").res)
    }
  }

  implicit class ToLocalAttributeDeclaration(val elem: indexed.Elem) extends AnyVal {

    def toLocalAttributeDeclaration: LocalAttributeDeclaration = new LocalAttributeDeclaration(elem)
  }

  /**
   * An xs:attribute referring to a global attribute declaration
   */
  final class AttributeReference(val elem: indexed.Elem) {
    import scope._

    require(elem.rootElem.resolvedName == QName("xs", "schema").res, s"Expected root element xs:schema, but found ${elem.rootElem.resolvedName}")
    require(elem.resolvedName == QName("xs", "attribute").res, s"Expected xs:attribute, but found ${elem.resolvedName}")
    require(elem.path.entries.size > 1, s"Expected attribute reference, but found path ${elem.path}")

    def ref: EName = {
      elem.elem.attributeAsResolvedQName(QName("ref").res)
    }
  }

  implicit class ToAttributeReference(val elem: indexed.Elem) extends AnyVal {

    def toAttributeReference: AttributeReference = new AttributeReference(elem)
  }
}
