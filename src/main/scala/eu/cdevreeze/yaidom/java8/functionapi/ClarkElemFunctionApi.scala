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
import java.util.stream.Stream

import eu.cdevreeze.yaidom.core.EName

/**
 * Equivalent of `ClarkElemApi`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait ClarkElemFunctionApi[E] extends ElemFunctionApi[E] with IsNavigableFunctionApi[E] {

  // HasENameApi methods

  def resolvedName(elem: E): EName

  def resolvedAttributes(elem: E): Stream[ResolvedAttr]

  def localName(elem: E): String

  def attributeOption(elem: E, expandedName: EName): Optional[String]

  def attribute(elem: E, expandedName: EName): String

  def findAttributeByLocalName(elem: E, localName: String): Optional[String]

  // HasTextApi

  def text(elem: E): String

  def trimmedText(elem: E): String

  def normalizedText(elem: E): String
}
