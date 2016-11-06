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
import java.util.stream.Stream

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.java8.ResolvedAttr

/**
 * Equivalent of `ClarkElemApi`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait StreamingClarkElemApi[E <: StreamingClarkElemApi[E]] extends StreamingElemApi[E] with StreamingIsNavigableApi[E] {

  // HasENameApi methods

  def resolvedName: EName

  def resolvedAttributes: Stream[ResolvedAttr]

  def localName: String

  def attributeOption(expandedName: EName): Optional[String]

  def attribute(expandedName: EName): String

  def findAttributeByLocalName(localName: String): Optional[String]

  // HasTextApi

  def text: String

  def trimmedText: String

  def normalizedText: String
}
