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
import java.util.function.Predicate
import java.util.stream.Stream

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * Equivalent of `ClarkElemApi`, but returning Java 8 Streams, to be used in Java code.
 *
 * @author Chris de Vreeze
 */
trait ClarkElemStreamApi {

  type ThisElem <: ClarkElemStreamApi

  // ElemApi methods

  def findAllChildElems: Stream[ThisElem]

  def findAllElems: Stream[ThisElem]

  def findAllElemsOrSelf: Stream[ThisElem]

  def filterChildElems(p: Predicate[ThisElem]): Stream[ThisElem]

  def filterElems(p: Predicate[ThisElem]): Stream[ThisElem]

  def filterElemsOrSelf(p: Predicate[ThisElem]): Stream[ThisElem]

  def findChildElem(p: Predicate[ThisElem]): Optional[ThisElem]

  def findElem(p: Predicate[ThisElem]): Optional[ThisElem]

  def findElemOrSelf(p: Predicate[ThisElem]): Optional[ThisElem]

  def findTopmostElems(p: Predicate[ThisElem]): Stream[ThisElem]

  def findTopmostElemsOrSelf(p: Predicate[ThisElem]): Stream[ThisElem]

  def getChildElem(p: Predicate[ThisElem]): ThisElem

  // IsNavigableApi methods

  def findAllChildElemsWithPathEntries: Stream[ClarkElemStreamApi.ElemWithPathEntry[ThisElem]]

  def findChildElemByPathEntry(entry: Path.Entry): Optional[ThisElem]

  def getChildElemByPathEntry(entry: Path.Entry): ThisElem

  def findElemOrSelfByPath(path: Path): Optional[ThisElem]

  def getElemOrSelfByPath(path: Path): ThisElem

  def findReverseAncestryOrSelfByPath(path: Path): Optional[Stream[ThisElem]]

  def getReverseAncestryOrSelfByPath(path: Path): Stream[ThisElem]

  // HasENameApi methods

  def resolvedName: EName

  def resolvedAttributes: Stream[ClarkElemStreamApi.ResolvedAttr]

  def localName: String

  def attributeOption(expandedName: EName): Optional[String]

  def attribute(expandedName: EName): String

  def findAttributeByLocalName(localName: String): Optional[String]

  // HasTextApi

  def text: String

  def trimmedText: String

  def normalizedText: String
}

object ClarkElemStreamApi {

  final case class ElemWithPathEntry[E <: ClarkElemStreamApi](val elem: E, val pathEntry: Path.Entry)

  final case class ResolvedAttr(val ename: EName, val value: String)
}
