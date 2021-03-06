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

import eu.cdevreeze.yaidom.dom.DomElem
import eu.cdevreeze.yaidom.queryapitests.AbstractRobustQueryTest
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Query test case for DOM wrapper elements.
 *
 * @author Chris de Vreeze
 */
class RobustQueryTest extends AbstractRobustQueryTest {

  final type E = DomElem

  protected final val contactsElem: DomElem = {
    val dbf = DocumentBuilderFactory.newInstance()

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db
    }

    val is = classOf[RobustQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/contacts.xml")

    val domDoc = createDocumentBuilder(dbf).parse(is)

    new DomElem(domDoc.getDocumentElement())
  }
}
