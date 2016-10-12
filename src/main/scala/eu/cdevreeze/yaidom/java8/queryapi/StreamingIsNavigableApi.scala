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

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.java8.ElemPathEntryPair

/**
 * Equivalent of `IsNavigableApi`, but returning Java 8 Streams and taking Java 8 Predicates, to be used
 * in Java code.
 *
 * @author Chris de Vreeze
 */
trait StreamingIsNavigableApi[E <: StreamingIsNavigableApi[E]] {

  // IsNavigableApi methods

  def findAllChildElemsWithPathEntries: Stream[ElemPathEntryPair[E]]

  def findChildElemByPathEntry(entry: Path.Entry): Optional[E]

  def getChildElemByPathEntry(entry: Path.Entry): E

  def findElemOrSelfByPath(path: Path): Optional[E]

  def getElemOrSelfByPath(path: Path): E

  def findReverseAncestryOrSelfByPath(path: Path): Optional[Stream[E]]

  def getReverseAncestryOrSelfByPath(path: Path): Stream[E]
}
