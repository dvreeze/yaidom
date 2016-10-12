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

package eu.cdevreeze.yaidom.java8.queryapi

import java.util.Optional
import java.util.stream.Stream

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.java8.Attr

/**
 * Equivalent of `ScopedElemApi`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait StreamingScopedElemApi[E <: StreamingScopedElemApi[E]] extends StreamingClarkElemApi[E] {

  // HasQNameApi methods

  def qname: QName

  def attributes: Stream[Attr]

  // HasScopeApi

  def scope: Scope

  // ScopedElemApi own methods

  def attributeAsQNameOption(expandedName: EName): Optional[QName]

  def attributeAsQName(expandedName: EName): QName

  def attributeAsResolvedQNameOption(expandedName: EName): Optional[EName]

  def attributeAsResolvedQName(expandedName: EName): EName

  def textAsQName: QName

  def textAsResolvedQName: EName
}
