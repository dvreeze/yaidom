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
import java.util.stream.Stream

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.java8.functionapi.ElemPathEntryPair
import eu.cdevreeze.yaidom.java8.functionapi.IsNavigableFunctionApi
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeJavaStreamFunction
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeSingletonStream
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeStream
import eu.cdevreeze.yaidom.queryapi.IsNavigableApi

/**
 * Equivalent of `IsNavigableApi` (with implementation), but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait IsNavigableFunctions[E <: IsNavigableApi.Aux[E]] extends IsNavigableFunctionApi[E] {

  // IsNavigableApi methods

  final def findAllChildElemsWithPathEntries(elem: E): Stream[ElemPathEntryPair[E]] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(
        e => e.findAllChildElemsWithPathEntries.map({ case (elm, pe) => ElemPathEntryPair(elm, pe) })))
  }

  final def findChildElemByPathEntry(elem: E, entry: Path.Entry): Optional[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findChildElemByPathEntry(entry).toIndexedSeq)).findFirst
  }

  final def getChildElemByPathEntry(elem: E, entry: Path.Entry): E = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => immutable.IndexedSeq(e.getChildElemByPathEntry(entry)))).findFirst.get
  }

  final def findElemOrSelfByPath(elem: E, path: Path): Optional[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findElemOrSelfByPath(path).toIndexedSeq)).findFirst
  }

  final def getElemOrSelfByPath(elem: E, path: Path): E = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => immutable.IndexedSeq(e.getElemOrSelfByPath(path)))).findFirst.get
  }

  final def findReverseAncestryOrSelfByPath(elem: E, path: Path): Optional[Stream[E]] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findReverseAncestryOrSelfByPath(path).map(elms => makeStream(elms)).toIndexedSeq)).findFirst
  }

  final def getReverseAncestryOrSelfByPath(elem: E, path: Path): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.getReverseAncestryOrSelfByPath(path)))
  }
}
