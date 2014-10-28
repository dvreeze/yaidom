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
 * Shorthand for `ElemApi[E] with HasENameApi with HasQNameApi with HasScopeApi with HasTextApi` with some additional methods that
 * use the scope for resolving QName-valued text and attribute values. In other words, an element query API typically
 * supported by element implementations, because most element implementations know about scopes, QNames, ENames and
 * text content, as well as offering the `ElemApi` query API.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ScopedElemApi[E <: ScopedElemApi[E]] extends ElemApi[E] with HasENameApi with HasQNameApi with HasScopeApi with HasTextApi { self: E =>

  /**
   * Returns the QName value of the attribute with the given expanded name, if any, wrapped in an `Option`.
   * If the attribute exists, but its value is not a QName, an exception is thrown.
   */
  def attributeAsQNameOption(expandedName: EName): Option[QName]

  /** Returns the QName value of the attribute with the given expanded name, and throws an exception otherwise */
  def attributeAsQName(expandedName: EName): QName

  /**
   * Returns the resolved QName value (as EName) of the attribute with the given expanded name, if any, wrapped in an `Option`.
   * None is returned if the attribute does not exist. If the QName value cannot be resolved given the scope of the element,
   * an exception is thrown.
   */
  def attributeAsResolvedQNameOption(expandedName: EName): Option[EName]

  /**
   * Returns the resolved QName value (as EName) of the attribute with the given expanded name, and throws an exception otherwise
   */
  def attributeAsResolvedQName(expandedName: EName): EName

  /** Returns `QName(text.trim)` */
  def textAsQName: QName

  /** Returns the equivalent of `scope.resolveQNameOption(textAsQName).get` */
  def textAsResolvedQName: EName
}
