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

import scala.compat.java8.OptionConverters.RichOptionForJava8

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.java8.functionapi.Attr
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeStream
import eu.cdevreeze.yaidom.java8.functionapi.ScopedElemFunctionApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi

/**
 * Equivalent of `ScopedElemApi` (with implementation), but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait ScopedElemFunctions[E <: ScopedElemApi.Aux[E]] extends ScopedElemFunctionApi[E] with ClarkElemFunctions[E] {

  // HasQNameApi methods

  final def qname(elem: E): QName = {
    elem.qname
  }

  final def attributes(elem: E): Stream[Attr] = {
    makeStream(elem.attributes.toIndexedSeq.map(attr => Attr(attr._1, attr._2)))
  }

  // HasScopeApi

  final def scope(elem: E): Scope = {
    elem.scope
  }

  // ScopedElemApi own methods

  final def attributeAsQNameOption(elem: E, expandedName: EName): Optional[QName] = {
    elem.attributeAsQNameOption(expandedName).asJava
  }

  final def attributeAsQName(elem: E, expandedName: EName): QName = {
    elem.attributeAsQName(expandedName)
  }

  final def attributeAsResolvedQNameOption(elem: E, expandedName: EName): Optional[EName] = {
    elem.attributeAsResolvedQNameOption(expandedName).asJava
  }

  final def attributeAsResolvedQName(elem: E, expandedName: EName): EName = {
    elem.attributeAsResolvedQName(expandedName)
  }

  final def textAsQName(elem: E): QName = {
    elem.textAsQName
  }

  final def textAsResolvedQName(elem: E): EName = {
    elem.textAsResolvedQName
  }
}
