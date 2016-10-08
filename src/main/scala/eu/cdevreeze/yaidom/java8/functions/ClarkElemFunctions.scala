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
import eu.cdevreeze.yaidom.java8.functionapi.ClarkElemFunctionApi
import eu.cdevreeze.yaidom.java8.functionapi.JavaStreamUtil.makeStream
import eu.cdevreeze.yaidom.java8.functionapi.ResolvedAttr
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi

/**
 * Equivalent of `ClarkElemApi` (with implementation), but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait ClarkElemFunctions[E <: ClarkElemApi.Aux[E]] extends ClarkElemFunctionApi[E] with ElemFunctions[E] with IsNavigableFunctions[E] {

  // HasENameApi methods

  final def resolvedName(elem: E): EName = {
    elem.resolvedName
  }

  final def resolvedAttributes(elem: E): Stream[ResolvedAttr] = {
    makeStream(elem.resolvedAttributes.toIndexedSeq.map(attr => ResolvedAttr(attr._1, attr._2)))
  }

  final def localName(elem: E): String = {
    elem.localName
  }

  final def attributeOption(elem: E, expandedName: EName): Optional[String] = {
    elem.attributeOption(expandedName).asJava
  }

  final def attribute(elem: E, expandedName: EName): String = {
    elem.attribute(expandedName)
  }

  final def findAttributeByLocalName(elem: E, localName: String): Optional[String] = {
    elem.findAttributeByLocalName(localName).asJava
  }

  // HasTextApi

  final def text(elem: E): String = {
    elem.text
  }

  final def trimmedText(elem: E): String = {
    elem.trimmedText
  }

  final def normalizedText(elem: E): String = {
    elem.normalizedText
  }
}
