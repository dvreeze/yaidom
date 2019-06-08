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

package eu.cdevreeze.yaidom.java8.queryapi

import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Collectors
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.asJavaFunction
import scala.compat.java8.FunctionConverters.enrichAsJavaPredicate

/**
 * Equivalent of `ElemLike`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait StreamingElemLike[E <: StreamingElemLike[E]] extends StreamingElemApi[E] { self: E =>

  def findAllChildElems: Stream[E]

  final def findAllElems: Stream[E] = {
    filterElems(anyElem)
  }

  final def findAllElemsOrSelf: Stream[E] = {
    filterElemsOrSelf(anyElem)
  }

  final def filterChildElems(p: Predicate[E]): Stream[E] = {
    findAllChildElems.filter(p)
  }

  final def filterElems(p: Predicate[E]): Stream[E] = {
    findAllChildElems.flatMap(asJavaFunction(e => e.filterElemsOrSelf(p)))
  }

  final def filterElemsOrSelf(p: Predicate[E]): Stream[E] = {
    Stream.concat(
      Stream.of[E](self).filter(p),
      findAllChildElems.flatMap(asJavaFunction(e => e.filterElemsOrSelf(p))))
  }

  final def findChildElem(p: Predicate[E]): Optional[E] = {
    filterChildElems(p).findFirst
  }

  final def findElem(p: Predicate[E]): Optional[E] = {
    filterElems(p).findFirst
  }

  final def findElemOrSelf(p: Predicate[E]): Optional[E] = {
    filterElemsOrSelf(p).findFirst
  }

  final def findTopmostElems(p: Predicate[E]): Stream[E] = {
    findAllChildElems.flatMap(asJavaFunction(e => e.findTopmostElemsOrSelf(p)))
  }

  final def findTopmostElemsOrSelf(p: Predicate[E]): Stream[E] = {
    if (p.test(self)) {
      Stream.of[E](self)
    } else {
      findAllChildElems.flatMap(asJavaFunction(e => e.findTopmostElemsOrSelf(p)))
    }
  }

  // Not final. Also see issue SI-8905.
  def getChildElem(p: Predicate[E]): E = {
    val filteredChildElems = filterChildElems(p).collect(Collectors.toList[E])

    require(
      filteredChildElems.size == 1,
      s"Expected exactly 1 matching child element, but found ${filteredChildElems.size} of them")

    filteredChildElems.get(0)
  }

  private val anyElem: Predicate[E] = {
    ((e: E) => true).asJava
  }
}
