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

package eu.cdevreeze.yaidom.queryapitests.resolved

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapitests.AbstractXbrlInstanceQueryTest
import eu.cdevreeze.yaidom.resolved.Elem

/**
 * XBRL instance query test case for resolved elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XbrlInstanceQueryTest extends AbstractXbrlInstanceQueryTest {

  final type E = Elem

  protected final val xbrlInstance: Elem = {
    val docParser = DocumentParserUsingDom.newInstance()

    val is = classOf[XbrlInstanceQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/sample-xbrl-instance.xml")

    val doc = docParser.parse(is)
    Elem(doc.documentElement)
  }

  protected final def toResolvedElem(elem: E): Elem = elem
}
