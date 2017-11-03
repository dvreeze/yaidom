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

package eu.cdevreeze.yaidom.queryapitests.simple

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapitests.AbstractRobustQueryTest
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Elem

/**
 * Query test case for simple elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class RobustQueryTest extends AbstractRobustQueryTest {

  final type E = Elem

  protected final val contactsElem: Elem = {
    val docParser = DocumentParserUsingDom.newInstance

    val is = classOf[RobustQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/contacts.xml")

    docParser.parse(is).documentElement
  }

  protected final def toResolvedElem(elem: E): resolved.Elem =
    resolved.Elem(elem)
}