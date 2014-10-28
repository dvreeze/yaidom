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
trait ScopedElemLike[E <: ScopedElemLike[E]] extends ScopedElemApi[E] with ElemLike[E] with HasEName with HasText { self: E =>

  final def attributeAsQNameOption(expandedName: EName): Option[QName] =
    attributeOption(expandedName).map(v => QName(v.trim))

  final def attributeAsQName(expandedName: EName): QName =
    attributeAsQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))

  final def attributeAsResolvedQNameOption(expandedName: EName): Option[EName] = {
    attributeAsQNameOption(expandedName) map { qname =>
      scope.resolveQNameOption(qname).getOrElse(
        sys.error(s"Could not resolve QName-valued attribute value $qname, given scope [${scope}]"))
    }
  }

  final def attributeAsResolvedQName(expandedName: EName): EName =
    attributeAsResolvedQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))

  final def textAsQName: QName = QName(text.trim)

  final def textAsResolvedQName: EName =
    scope.resolveQNameOption(textAsQName).getOrElse(
      sys.error(s"Could not resolve QName-valued element text $qname, given scope [${scope}]"))
}
