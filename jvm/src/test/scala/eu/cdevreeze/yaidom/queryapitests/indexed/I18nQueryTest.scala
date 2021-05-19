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

package eu.cdevreeze.yaidom.queryapitests.indexed

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.queryapitests.AbstractI18nQueryTest
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory

/**
 * Query test case for indexed elements.
 *
 * @author Chris de Vreeze
 */
class I18nQueryTest extends AbstractI18nQueryTest {

  final type E = Elem

  protected final val rootElem: Elem = {
    val dbf = DocumentBuilderFactory.newInstance()

    def createDocumentBuilder(documentBuilderFactory: DocumentBuilderFactory): DocumentBuilder = {
      val db = documentBuilderFactory.newDocumentBuilder()
      db
    }

    val is = classOf[I18nQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/internationalization-instance-valid.xml")

    val domDoc = createDocumentBuilder(dbf).parse(is)

    Elem(
      Some(new java.net.URI("")),
      convert.DomConversions.convertToElem(domDoc.getDocumentElement(), Scope.Empty))
  }
}
