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

import eu.cdevreeze.yaidom.queryapitests.AbstractXbrlInstanceQueryTest
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem
import eu.cdevreeze.yaidom.scalaxml.ScalaXmlNode

/**
 * XBRL instance query test case for Scala XML wrapper elements.
 *
 * @author Chris de Vreeze
 */
class XbrlInstanceQueryTest extends AbstractXbrlInstanceQueryTest {

  final type E = ScalaXmlElem

  protected final val xbrlInstance: ScalaXmlElem = {
    val is = classOf[XbrlInstanceQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/sample-xbrl-instance.xml")

    val xmlParser = scala.xml.parsing.ConstructingParser.fromSource(scala.io.Source.fromInputStream(is), true)

    val root: ScalaXmlElem = ScalaXmlNode.wrapElement(xmlParser.document().docElem.asInstanceOf[scala.xml.Elem])
    root
  }
}
