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

package eu.cdevreeze.yaidom.utils

import java.{ util => jutil }

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner
import org.xml.sax.EntityResolver
import org.xml.sax.InputSource

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * ENameProviderUtils and QNameProviderUtils test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class NameProvidersTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.utils")

  @Test def testNameCaching(): Unit = {
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
            new InputSource(classOf[NameProvidersTest].getResourceAsStream("XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[NameProvidersTest].getResourceAsStream("datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      })
      db
    }

    val domParser = new DocumentParserUsingDom(dbf, createDocumentBuilder _)

    val is = classOf[NameProvidersTest].getResourceAsStream("XMLSchema.xsd")

    val root: Elem = domParser.parse(is).documentElement

    val enameProvider = ENameProviderUtils.newENameProviderUsingSchemas(Vector(root))

    val qnameProvider = QNameProviderUtils.newQNameProviderUsingSchemas(Vector(root))

    val xsElementQName = QName("xs:element")
    val xsElementEName = xsElementQName.res

    // Testing the ENameProvider

    assertResult(true) {
      enameProvider.cache.contains((xsElementEName.namespaceUriOption, xsElementEName.localPart))
    }

    assertResult(false) {
      enameProvider.cache.contains((xsElementEName.namespaceUriOption, "bla"))
    }

    assertResult(xsElementEName) {
      enameProvider.getEName(xsElementEName.namespaceUriOption, xsElementEName.localPart)
    }
    assertResult(xsElementEName) {
      enameProvider.getEName(xsElementEName.namespaceUriOption.get, xsElementEName.localPart)
    }
    assertResult(xsElementEName) {
      enameProvider.parseEName(xsElementEName.toString)
    }

    // Testing the QNameProvider

    assertResult(true) {
      qnameProvider.cache.contains((xsElementQName.prefixOption, xsElementQName.localPart))
    }

    assertResult(false) {
      qnameProvider.cache.contains((xsElementQName.prefixOption, "bla"))
    }

    assertResult(xsElementQName) {
      qnameProvider.getQName(xsElementQName.prefixOption, xsElementQName.localPart)
    }
    assertResult(xsElementQName) {
      qnameProvider.getQName(xsElementQName.prefixOption.get, xsElementQName.localPart)
    }
    assertResult(xsElementQName) {
      qnameProvider.parseQName(xsElementQName.toString)
    }
  }
}
