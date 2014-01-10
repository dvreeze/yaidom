/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom

import scala.collection.{ immutable, mutable }

/**
 * API and implementation trait for elements as containers of elements, each having a name and possible attributes.
 * This trait extends trait [[eu.cdevreeze.yaidom.ParentElemLike]], adding knowledge about names of elements and of attributes.
 *
 * More precisely, this trait adds the following abstract methods to the abstract methods required by its super-trait:
 * `resolvedName` and `resolvedAttributes`. Based on these abstract methods (and the super-trait), this trait offers a rich API
 * for querying elements by (expanded) name, and for querying attributes.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.ElemApi]]. See the documentation of that trait
 * for examples of usage, and for a light formal treatment.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ElemLike[E <: ElemLike[E]] extends ParentElemLike[E] with ElemApi[E] { self: E =>

  def resolvedName: EName

  def resolvedAttributes: immutable.Iterable[(EName, String)]

  final def localName: String = resolvedName.localPart

  final def attributeOption(expandedName: EName): Option[String] = resolvedAttributes.toMap.get(expandedName)

  final def attribute(expandedName: EName): String =
    attributeOption(expandedName).getOrElse(sys.error(s"Missing attribute $expandedName"))

  final def findAttributeByLocalName(localName: String): Option[String] = {
    val matchingAttrs = resolvedAttributes filter { case (en, v) => en.localPart == localName }
    matchingAttrs.map(_._2).headOption
  }

  final def \@(expandedName: EName): Option[String] = attributeOption(expandedName)

  final def filterChildElems(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems { e => e.resolvedName == expandedName }

  final def \(expandedName: EName): immutable.IndexedSeq[E] = filterChildElems(expandedName)

  final def findChildElem(expandedName: EName): Option[E] = {
    findChildElem { e => e.resolvedName == expandedName }
  }

  final def getChildElem(expandedName: EName): E = {
    val result = filterChildElems(expandedName)
    require(result.size == 1, s"Expected exactly 1 child element $expandedName, but found ${result.size} of them")
    result.head
  }

  final def filterElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf { e => e.resolvedName == expandedName }

  final def \\(expandedName: EName): immutable.IndexedSeq[E] = filterElemsOrSelf(expandedName)

  final def filterElems(expandedName: EName): immutable.IndexedSeq[E] = filterElems { e => e.resolvedName == expandedName }

  final def findTopmostElemsOrSelf(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElemsOrSelf { e => e.resolvedName == expandedName }

  final def \\!(expandedName: EName): immutable.IndexedSeq[E] = findTopmostElemsOrSelf(expandedName)

  final def findTopmostElems(expandedName: EName): immutable.IndexedSeq[E] =
    findTopmostElems { e => e.resolvedName == expandedName }

  final def findElemOrSelf(expandedName: EName): Option[E] =
    findElemOrSelf { e => e.resolvedName == expandedName }

  final def findElem(expandedName: EName): Option[E] =
    findElem { e => e.resolvedName == expandedName }
}
