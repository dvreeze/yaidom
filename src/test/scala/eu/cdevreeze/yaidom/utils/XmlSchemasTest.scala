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

import java.{ util => jutil }

import scala.reflect.classTag

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

import XmlSchemas.AttributeReference
import XmlSchemas.ElementReference
import XmlSchemas.SchemaRoot
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * XmlSchemas test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlSchemasTest extends FunSuite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.utils")

  test("testQueryXsd") {
    val scope = Scope.from("xs" -> "http://www.w3.org/2001/XMLSchema")
    import scope._

    import XmlSchemas._

    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db.setEntityResolver(new EntityResolver {
        def resolveEntity(publicId: String, systemId: String): InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[XmlSchemasTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[XmlSchemasTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val domParser = DocumentParserUsingDom.newInstance(dbf, createDocumentBuilder _)

    val is = classOf[XmlSchemasTest].getResourceAsStream("XMLSchema.xsd")

    val root: Elem = domParser.parse(is).documentElement

    val schemaRoot = SchemaRoot(indexed.Elem(root))

    // Global element declarations

    val globalElemDecls = schemaRoot.findAllGlobalElementDeclarations
    val globalElemDeclENames = globalElemDecls.map(_.targetEName).toSet

    val expectedGlobalElemDeclENames: Set[EName] =
      Set(QName("xs", "schema").res, QName("xs", "annotation").res, QName("xs", "documentation").res,
        QName("xs", "import").res, QName("xs", "complexType").res, QName("xs", "complexContent").res,
        QName("xs", "unique").res, QName("xs", "sequence").res, QName("xs", "element").res,
        QName("xs", "attribute").res, QName("xs", "choice").res, QName("xs", "group").res,
        QName("xs", "simpleType").res, QName("xs", "restriction").res, QName("xs", "enumeration").res,
        QName("xs", "list").res, QName("xs", "union").res, QName("xs", "key").res,
        QName("xs", "selector").res, QName("xs", "field").res, QName("xs", "attributeGroup").res,
        QName("xs", "anyAttribute").res, QName("xs", "whiteSpace").res, QName("xs", "fractionDigits").res,
        QName("xs", "pattern").res, QName("xs", "any").res, QName("xs", "appinfo").res,
        QName("xs", "minLength").res, QName("xs", "maxInclusive").res, QName("xs", "minInclusive").res,
        QName("xs", "notation").res, QName("xs", "all").res, QName("xs", "include").res,
        QName("xs", "keyref").res, QName("xs", "length").res, QName("xs", "maxLength").res,
        QName("xs", "maxExclusive").res, QName("xs", "minExclusive").res, QName("xs", "redefine").res,
        QName("xs", "simpleContent").res, QName("xs", "totalDigits").res)

    assertResult(Set[EName]()) {
      globalElemDeclENames diff expectedGlobalElemDeclENames
    }

    assertResult(Set[EName]()) {
      expectedGlobalElemDeclENames diff globalElemDeclENames
    }

    // Global attribute declarations

    val globalAttrDecls = schemaRoot.findAllGlobalAttributeDeclarations
    val globalAttrDeclENames = globalAttrDecls.map(_.targetEName).toSet

    assertResult(Set[EName]()) {
      globalAttrDecls.map(_.targetEName).toSet
    }

    // Specific global element declaration

    val elemDeclForGlobalElemDeclOption = schemaRoot.findGlobalElementDeclaration(QName("xs:element").res)

    assertResult(Some(QName("xs", "element").res)) {
      elemDeclForGlobalElemDeclOption.map(e => e.targetEName)
    }
    assertResult(Some("element")) {
      elemDeclForGlobalElemDeclOption.flatMap(e => e.elem \@ QName("id").res)
    }
    assertResult(Some(QName("xs", "topLevelElement").res)) {
      elemDeclForGlobalElemDeclOption.flatMap(e => e.typeAttributeOption)
    }
    assertResult(None) {
      elemDeclForGlobalElemDeclOption.flatMap(e => e.substitutionGroupOption)
    }

    // Local element declarations

    val localElemDecls = schemaRoot.findAllLocalElementDeclarations
    val localElemDeclENames = localElemDecls.map(_.targetEName).toSet

    assertResult(true) {
      localElemDecls exists { e =>
        e.targetEName == QName("xs:group").res && e.typeAttributeOption.contains(QName("xs:groupRef").res)
      }
    }
    assertResult(true) {
      localElemDecls exists { e =>
        e.targetEName == QName("xs:element").res && e.typeAttributeOption.contains(QName("xs:localElement").res)
      }
    }

    // Local attribute declarations

    val localAttrDecls = schemaRoot.findAllLocalAttributeDeclarations
    val localAttrDeclENames = localAttrDecls.map(_.targetEName).toSet

    assertResult(true) {
      localAttrDecls exists { e =>
        e.targetEName == QName("minOccurs").res && e.typeAttributeOption.contains(QName("xs:nonNegativeInteger").res)
      }
    }

    assertResult(true) {
      Set(QName("id").res, QName("targetNamespace").res, QName("elementFormDefault").res, QName("maxOccurs").res).subsetOf(
        localAttrDeclENames)
    }

    // Element references

    val elemRefs = schemaRoot.findAllElemsOfType(classTag[ElementReference])
    val elemRefENames = elemRefs.map(_.ref).toSet

    assertResult(true) {
      elemRefs.filter(e => e.ref == QName("xs:annotation").res).size >= 10
    }
    assertResult(true) {
      schemaRoot.findAllElemsOrSelf exists { elem =>
        elem.findAllChildElemsOfType(classTag[ElementReference]).map(_.ref) ==
          List("xs:simpleType", "xs:complexType", "xs:group", "xs:attributeGroup").map(s => QName(s).res)
      }
    }

    // Attribute references

    val attrRefs = schemaRoot.findAllElemsOfType(classTag[AttributeReference])
    val attrRefENames = attrRefs.map(_.ref).toSet

    assertResult(Set(QName("xml:lang").res)) {
      attrRefENames.toSet
    }
  }
}
