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
trait BackingElemStreamApi[E <: BackingElemStreamApi[E]] extends ScopedElemStreamApi[E] {

  // IndexedScopedElemApi own methods

  def namespaces: Declarations

  // IndexedClarkElemApi own methods

  def docUriOption: Optional[URI]

  def docUri: URI

  def rootElem: E

  def path: Path

  def baseUriOption: Optional[URI]

  def baseUri: URI

  def parentBaseUriOption: Optional[URI]

  def reverseAncestryOrSelfENames: Stream[EName]

  def reverseAncestryENames: Stream[EName]

  def reverseAncestryOrSelf: Stream[E]

  def reverseAncestry: Stream[E]

  // HasParentApi methods

  def parentOption: Optional[E]

  def parent: E

  def ancestorsOrSelf: Stream[E]

  def ancestors: Stream[E]

  def findAncestorOrSelf(p: Predicate[E]): Optional[E]

  def findAncestor(p: Predicate[E]): Optional[E]
}
