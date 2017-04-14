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
 * Shorthand for `ClarkElemApi[E] with HasQNameApi with HasScopeApi` with some additional methods that
 * use the scope for resolving QName-valued text and attribute values. In other words, an element query API typically
 * supported by element implementations, because most element implementations know about scopes, QNames, ENames and
 * text content, as well as offering the `ElemApi` query API.
 *
 * '''Generic code abstracting over yaidom element implementations should either use
 * this trait, or super-trait `ClarkElemApi`, depending on the abstraction level.'''
 *
 * ==ScopedElemApi more formally==
 *
 * Scopes resolve QNames as ENames, so some properties are expected to hold for the element "name":
 * {{{
 * this.scope.resolveQNameOption(this.qname).contains(this.resolvedName)
 *
 * // Therefore:
 * this.resolvedName.localPart == this.qname.localPart
 *
 * this.resolvedName.namespaceUriOption ==
 *   this.scope.prefixNamespaceMap.get(this.qname.prefixOption.getOrElse(""))
 * }}}
 *
 * For the attribute "name" properties, first define:
 * {{{
 * val attributeScope = this.scope.withoutDefaultNamespace
 *
 * val resolvedAttrs = this.attributes map {
 *   case (attrQName, attrValue) =>
 *     val resolvedAttrName = attributeScope.resolveQNameOption(attrQName).get
 *     (resolvedAttrName -> attrValue)
 * }
 * }}}
 * Then the following must hold:
 * {{{
 * resolvedAttrs.toMap == this.resolvedAttributes.toMap
 * }}}
 *
 * @author Chris de Vreeze
 */
trait ScopedElemApi extends ClarkElemApi with HasQNameApi with HasScopeApi {

  type ThisElem <: ScopedElemApi

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

object ScopedElemApi {

  /**
   * This query API type, restricting ThisElem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = ScopedElemApi { type ThisElem = E }
}
