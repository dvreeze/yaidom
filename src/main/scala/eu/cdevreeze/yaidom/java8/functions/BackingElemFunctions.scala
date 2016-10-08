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

import java.net.URI
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

import scala.compat.java8.FunctionConverters.enrichAsScalaFromPredicate
import scala.compat.java8.OptionConverters.RichOptionForJava8

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.java8.functionapi.BackingElemFunctionApi
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeJavaStreamFunction
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeSingletonStream
import eu.cdevreeze.yaidom.queryapi.BackingElemApi

/**
 * Equivalent of `BackingElemApi` (with implementation), but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait BackingElemFunctions[E <: BackingElemApi.Aux[E]] extends BackingElemFunctionApi[E] with ScopedElemFunctions[E] {

  // IndexedScopedElemApi own methods

  final def namespaces(elem: E): Declarations = {
    elem.namespaces
  }

  // IndexedClarkElemApi own methods

  final def docUriOption(elem: E): Optional[URI] = {
    elem.docUriOption.asJava
  }

  final def docUri(elem: E): URI = {
    elem.docUri
  }

  final def rootElem(elem: E): E = {
    elem.rootElem
  }

  final def path(elem: E): Path = {
    elem.path
  }

  final def baseUriOption(elem: E): Optional[URI] = {
    elem.baseUriOption.asJava
  }

  final def baseUri(elem: E): URI = {
    elem.baseUri
  }

  final def parentBaseUriOption(elem: E): Optional[URI] = {
    elem.parentBaseUriOption.asJava
  }

  final def reverseAncestryOrSelfENames(elem: E): Stream[EName] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.reverseAncestryOrSelfENames))
  }

  final def reverseAncestryENames(elem: E): Stream[EName] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.reverseAncestryENames))
  }

  final def reverseAncestryOrSelf(elem: E): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.reverseAncestryOrSelf))
  }

  final def reverseAncestry(elem: E): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.reverseAncestry))
  }

  // HasParentApi methods

  final def parentOption(elem: E): Optional[E] = {
    elem.parentOption.asJava
  }

  final def parent(elem: E): E = {
    elem.parent
  }

  final def ancestorsOrSelf(elem: E): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.ancestorsOrSelf))
  }

  final def ancestors(elem: E): Stream[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.ancestors))
  }

  final def findAncestorOrSelf(elem: E, p: Predicate[E]): Optional[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findAncestorOrSelf(p.asScala).toIndexedSeq)).findFirst
  }

  final def findAncestor(elem: E, p: Predicate[E]): Optional[E] = {
    makeSingletonStream(elem).flatMap(
      makeJavaStreamFunction(e => e.findAncestor(p.asScala).toIndexedSeq)).findFirst
  }
}
