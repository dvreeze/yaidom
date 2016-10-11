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
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.java8.Attr

/**
 * Equivalent of `ScopedElemApi`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait ScopedElemFunctionApi[E] extends ClarkElemFunctionApi[E] {

  // HasQNameApi methods

  def qname(elem: E): QName

  def attributes(elem: E): Stream[Attr]

  // HasScopeApi

  def scope(elem: E): Scope

  // ScopedElemApi own methods

  def attributeAsQNameOption(elem: E, expandedName: EName): Optional[QName]

  def attributeAsQName(elem: E, expandedName: EName): QName

  def attributeAsResolvedQNameOption(elem: E, expandedName: EName): Optional[EName]

  def attributeAsResolvedQName(elem: E, expandedName: EName): EName

  def textAsQName(elem: E): QName

  def textAsResolvedQName(elem: E): EName
}
