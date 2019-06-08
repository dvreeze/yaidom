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

package eu.cdevreeze.yaidom.queryapitests.scalaxml

import java.{util => jutil}

import eu.cdevreeze.yaidom.queryapitests.AbstractScopedElemLikeQueryTest
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlNode
import org.xml.sax.InputSource

/**
 * Query test case for Scala XML wrapper elements.
 *
 * @author Chris de Vreeze
 */
class ScopedElemLikeQueryTest extends AbstractScopedElemLikeQueryTest {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests.scalaxml")

  final type E = ScalaXmlElem

  protected final val xsdSchemaElem: ScalaXmlElem = {
    // See http://richard.dallaway.com/2013-02-06

    val resolvingXmlLoader = new scala.xml.factory.XMLLoader[scala.xml.Elem] {
      override def adapter: scala.xml.parsing.FactoryAdapter = new scala.xml.parsing.NoBindingFactoryAdapter() {
        override def resolveEntity(publicId: String, systemId: String): org.xml.sax.InputSource = {
          logger.info(s"Trying to resolve entity. Public ID: $publicId. System ID: $systemId")

          if (systemId.endsWith("/XMLSchema.dtd") || systemId.endsWith("\\XMLSchema.dtd") || (systemId == "XMLSchema.dtd")) {
            new InputSource(classOf[ScopedElemLikeQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/XMLSchema.dtd"))
          } else if (systemId.endsWith("/datatypes.dtd") || systemId.endsWith("\\datatypes.dtd") || (systemId == "datatypes.dtd")) {
            new InputSource(classOf[ScopedElemLikeQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/datatypes.dtd"))
          } else {
            // Default behaviour
            null
          }
        }
      }
    }

    val is = classOf[ScopedElemLikeQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/XMLSchema.xsd")

    val root: ScalaXmlElem = ScalaXmlNode.wrapElement(resolvingXmlLoader.load(is))
    root
  }
}
