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

package eu.cdevreeze.yaidom.java8.elems

import eu.cdevreeze.yaidom.indexed.Elem
import eu.cdevreeze.yaidom.java8.functions.BackingElemFunctions

/**
 * BackingElemFunctionApi applied to native yaidom indexed elements.Easy to use in Java 8 code.
 *
 * @author Chris de Vreeze
 */
final class IndexedElems extends BackingElemFunctions[Elem]

object IndexedElems {

  def getInstance: IndexedElems = new IndexedElems
}
