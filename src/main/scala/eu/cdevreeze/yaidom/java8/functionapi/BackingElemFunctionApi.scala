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

import java.net.URI
import java.util.Optional
import java.util.function.Predicate
import java.util.stream.Stream

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * Equivalent of `BackingElemApi`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait BackingElemFunctionApi[E] extends ScopedElemFunctionApi[E] {

  // IndexedScopedElemApi own methods

  def namespaces(elem: E): Declarations

  // IndexedClarkElemApi own methods

  def docUriOption(elem: E): Optional[URI]

  def docUri(elem: E): URI

  def rootElem(elem: E): E

  def path(elem: E): Path

  def baseUriOption(elem: E): Optional[URI]

  def baseUri(elem: E): URI

  def parentBaseUriOption(elem: E): Optional[URI]

  def reverseAncestryOrSelfENames(elem: E): Stream[EName]

  def reverseAncestryENames(elem: E): Stream[EName]

  def reverseAncestryOrSelf(elem: E): Stream[E]

  def reverseAncestry(elem: E): Stream[E]

  // HasParentApi methods

  def parentOption(elem: E): Optional[E]

  def parent(elem: E): E

  def ancestorsOrSelf(elem: E): Stream[E]

  def ancestors(elem: E): Stream[E]

  def findAncestorOrSelf(elem: E, p: Predicate[E]): Optional[E]

  def findAncestor(elem: E, p: Predicate[E]): Optional[E]
}
