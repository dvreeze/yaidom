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

package eu.cdevreeze.yaidom.java8.functions

import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

import scala.collection.immutable
import scala.compat.java8.FunctionConverters.enrichAsScalaFromPredicate

import eu.cdevreeze.yaidom.java8.functionapi.ElemFunctionApi
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeJavaStreamFunction
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeSingletonStream
import eu.cdevreeze.yaidom.queryapi.ElemApi

/**
 * Equivalent of `ElemApi` (with implementation), but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait ElemFunctions[E <: ElemApi.Aux[E]] extends ElemFunctionApi[E] {

  // ElemApi methods

  final def findAllChildElems(elem: E): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findAllChildElems))
  }

  final def findAllElems(elem: E): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findAllElems))
  }

  final def findAllElemsOrSelf(elem: E): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findAllElemsOrSelf))
  }

  final def filterChildElems(elem: E, p: Predicate[E]): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.filterChildElems(p.asScala)))
  }

  final def filterElems(elem: E, p: Predicate[E]): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.filterElems(p.asScala)))
  }

  final def filterElemsOrSelf(elem: E, p: Predicate[E]): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.filterElemsOrSelf(p.asScala)))
  }

  final def findChildElem(elem: E, p: Predicate[E]): Optional[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findChildElem(p.asScala).toIndexedSeq)).findFirst
  }

  final def findElem(elem: E, p: Predicate[E]): Optional[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findElem(p.asScala).toIndexedSeq)).findFirst
  }

  final def findElemOrSelf(elem: E, p: Predicate[E]): Optional[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findElemOrSelf(p.asScala).toIndexedSeq)).findFirst
  }

  final def findTopmostElems(elem: E, p: Predicate[E]): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findTopmostElems(p.asScala)))
  }

  final def findTopmostElemsOrSelf(elem: E, p: Predicate[E]): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findTopmostElemsOrSelf(p.asScala)))
  }

  final def getChildElem(elem: E, p: Predicate[E]): E = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => immutable.IndexedSeq(e.getChildElem(p.asScala)))).findFirst.get
  }
}
