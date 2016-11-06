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

package eu.cdevreeze.yaidom.queryapitests.dom

import java.{ util => jutil }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.queryapitests.AbstractRobustQueryTest
import eu.cdevreeze.yaidom.resolved
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Query test case for DOM wrapper elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class RobustQueryTest extends AbstractRobustQueryTest {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests.dom")

  final type E = DomElem

  protected final val contactsElem: DomElem = {
    val dbf = DocumentBuilderFactory.newInstance

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db
    }

    val is = classOf[RobustQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/contacts.xml")

    val domDoc = createDocumentBuilder(dbf).parse(is)

    new DomElem(domDoc.getDocumentElement())
  }

  protected final def toResolvedElem(elem: E): resolved.Elem =
    resolved.Elem(convert.DomConversions.convertToElem(elem.wrappedNode, contactsElem.scope))
}
