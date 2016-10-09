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

import eu.cdevreeze.yaidom.java8.functions.ScopedElemFunctions
import eu.cdevreeze.yaidom.simple.Elem

/**
 * ScopedElemFunctionApi applied to native yaidom simple elements. Easy to use in Java 8 code.
 *
 * @author Chris de Vreeze
 */
final class SimpleElems extends ScopedElemFunctions[Elem]

object SimpleElems {

  def getInstance: SimpleElems = new SimpleElems
}
