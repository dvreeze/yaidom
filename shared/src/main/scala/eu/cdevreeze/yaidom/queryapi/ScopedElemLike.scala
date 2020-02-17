/*
 * Copyright 2011-2017 Chris de Vreeze
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
 * All methods are overridable. Hence element implementations mixing in this partial implementation trait can change the
 * implementation without breaking its API, caused by otherwise needed removal of this mixin. Arguably this trait should not
 * exist as part of the public API, because implementation details should not be part of the public API. Such implementation details
 * may be subtle, such as the (runtime) boundary on the ThisElem type member.
 *
 * @author Chris de Vreeze
 */
trait ScopedElemLike extends ScopedElemApi with ClarkElemLike {

  type ThisElem <: ScopedElemLike.Aux[ThisElem]

  def attributeAsQNameOption(expandedName: EName): Option[QName] = {
    attributeOption(expandedName).map(v => QName(v.trim))
  }

  def attributeAsQName(expandedName: EName): QName = {
    attributeAsQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))
  }

  def attributeAsResolvedQNameOption(expandedName: EName): Option[EName] = {
    attributeAsQNameOption(expandedName) map { qn =>
      scope.resolveQNameOption(qn).getOrElse(
        sys.error(s"Could not resolve QName-valued attribute value $qn, given scope [${scope}]"))
    }
  }

  def attributeAsResolvedQName(expandedName: EName): EName = {
    attributeAsResolvedQNameOption(expandedName).getOrElse(
      sys.error(s"Missing QName-valued attribute $expandedName"))
  }

  def textAsQName: QName = {
    QName(text.trim)
  }

  def textAsResolvedQName: EName = {
    scope.resolveQNameOption(textAsQName).getOrElse(
      sys.error(s"Could not resolve QName-valued element text $textAsQName, given scope [${scope}]"))
  }
}

object ScopedElemLike {

  /**
   * This query API type, restricting ThisElem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = ScopedElemLike { type ThisElem = E }
}
