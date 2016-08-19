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

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName

/**
 * Partial implementation of `ScopedElemApi`.
 *
 * @author Chris de Vreeze
 */
trait ScopedElemLike extends ScopedElemApi with ClarkElemLike {

  type ThisElemApi <: ScopedElemLike

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

  type Aux[A] = ScopedElemLike { type ThisElem = A }
}
