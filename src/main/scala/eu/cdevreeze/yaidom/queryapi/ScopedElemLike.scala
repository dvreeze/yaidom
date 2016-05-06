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

package eu.cdevreeze.yaidom.queryapi

import scala.collection.immutable
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName

/**
 * Partial implementation of `ScopedElemApi`.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ScopedElemLike[E <: ScopedElemLike[E]] extends ScopedElemApi[E] with ClarkElemLike[E] { self: E =>

  // Implementation note: this is not DRY because it is pretty much the same code as in the corresponding type class.
  // Yet I did not want to depend on a val or def of the appropriate type class instance, so chose for code repetition.

  final def attributeAsQNameOption(expandedName: EName): Option[QName] =
    attributeOption(expandedName).map(v => QName(v.trim))

  final def attributeAsQName(expandedName: EName): QName =
    attributeAsQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))

  final def attributeAsResolvedQNameOption(expandedName: EName): Option[EName] = {
    attributeAsQNameOption(expandedName) map { qn =>
      scope.resolveQNameOption(qn).getOrElse(
        sys.error(s"Could not resolve QName-valued attribute value $qn, given scope [${scope}]"))
    }
  }

  final def attributeAsResolvedQName(expandedName: EName): EName =
    attributeAsResolvedQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))

  final def textAsQName: QName = QName(text.trim)

  final def textAsResolvedQName: EName =
    scope.resolveQNameOption(textAsQName).getOrElse(
      sys.error(s"Could not resolve QName-valued element text $textAsQName, given scope [${scope}]"))
}

object ScopedElemLike {

  /**
   * The `ScopedElemLike` as type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] extends ScopedElemApi.FunctionApi[E] with ClarkElemLike.FunctionApi[E] {

    final def attributeAsQNameOption(thisElem: E, expandedName: EName): Option[QName] =
      attributeOption(thisElem, expandedName).map(v => QName(v.trim))

    final def attributeAsQName(thisElem: E, expandedName: EName): QName =
      attributeAsQNameOption(thisElem, expandedName).getOrElse(
        sys.error(s"Missing QName-valued attribute $expandedName"))

    final def attributeAsResolvedQNameOption(thisElem: E, expandedName: EName): Option[EName] = {
      attributeAsQNameOption(thisElem, expandedName) map { qn =>
        scope(thisElem).resolveQNameOption(qn).getOrElse(
          sys.error(s"Could not resolve QName-valued attribute value $qn, given scope [${scope(thisElem)}]"))
      }
    }

    final def attributeAsResolvedQName(thisElem: E, expandedName: EName): EName =
      attributeAsResolvedQNameOption(thisElem, expandedName).getOrElse(
        sys.error(s"Missing QName-valued attribute $expandedName"))

    final def textAsQName(thisElem: E): QName = QName(text(thisElem).trim)

    final def textAsResolvedQName(thisElem: E): EName =
      scope(thisElem).resolveQNameOption(textAsQName(thisElem)).getOrElse(
        sys.error(s"Could not resolve QName-valued element text ${textAsQName(thisElem)}, given scope [${scope(thisElem)}]"))
  }
}
