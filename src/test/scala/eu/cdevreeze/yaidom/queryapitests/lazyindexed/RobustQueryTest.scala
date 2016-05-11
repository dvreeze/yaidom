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

package eu.cdevreeze.yaidom.queryapitests.lazyindexed

import java.{ util => jutil }

import org.junit.runner.RunWith
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.indexed.LazyIndexedScopedElem
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapitests.AbstractRobustQueryTest
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * Query test case for lazy indexed elements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class RobustQueryTest extends AbstractRobustQueryTest {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.queryapitests.lazyindexed")

  final type E = LazyIndexedScopedElem[simple.Elem]

  protected final val contactsElem: LazyIndexedScopedElem[simple.Elem] = {
    val docParser = DocumentParserUsingDom.newInstance

    val is = classOf[RobustQueryTest].getResourceAsStream("/eu/cdevreeze/yaidom/queryapitests/contacts.xml")

    LazyIndexedScopedElem[simple.Elem](Some(new java.net.URI("")), docParser.parse(is).documentElement)
  }

  protected final def toResolvedElem(elem: E): resolved.Elem =
    resolved.Elem(elem.elem)
}
