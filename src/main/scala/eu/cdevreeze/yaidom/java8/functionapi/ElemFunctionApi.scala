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

package eu.cdevreeze.yaidom.java8.functionapi

import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

/**
 * Equivalent of `ElemApi`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait ElemFunctionApi[E] {

  // ElemApi methods

  def findAllChildElems(elem: E): Stream[E]

  def findAllElems(elem: E): Stream[E]

  def findAllElemsOrSelf(elem: E): Stream[E]

  def filterChildElems(elem: E, p: Predicate[E]): Stream[E]

  def filterElems(elem: E, p: Predicate[E]): Stream[E]

  def filterElemsOrSelf(elem: E, p: Predicate[E]): Stream[E]

  def findChildElem(elem: E, p: Predicate[E]): Optional[E]

  def findElem(elem: E, p: Predicate[E]): Optional[E]

  def findElemOrSelf(elem: E, p: Predicate[E]): Optional[E]

  def findTopmostElems(elem: E, p: Predicate[E]): Stream[E]

  def findTopmostElemsOrSelf(elem: E, p: Predicate[E]): Stream[E]

  def getChildElem(elem: E, p: Predicate[E]): E
}
