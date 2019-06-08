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

import java.net.URI




import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.queryapitests.AbstractAlternativeXmlBaseOnIndexedClarkElemApiTest
import eu.cdevreeze.yaidom.simple

/**
 * Alternative XML Base test case for indexed Elems.
 *
 * @author Chris de Vreeze
 */

class AlternativeXmlBaseOnIndexedClarkElemApiTest extends AbstractAlternativeXmlBaseOnIndexedClarkElemApiTest {

  type U = simple.Elem
  type E = Elem

  private val indexedElemBuilder = Elem

  protected def convertToDocElem(elem: simple.Elem, docUri: URI): E = {
    indexedElemBuilder(Some(docUri), elem)
  }

  protected def nullUri: URI = URI.create("")
}
