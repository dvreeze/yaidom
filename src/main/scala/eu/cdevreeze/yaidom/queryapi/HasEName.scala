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

/**
 * Trait partly implementing the contract for elements that have a EName, as well as attributes with EName keys.
 *
 * Using this trait (possibly in combination with other "element traits") we can abstract over several element implementations.
 *
 * @author Chris de Vreeze
 */
trait HasEName extends HasENameApi {

  /**
   * The local name, that is, the local part of the EName
   */
  final def localName: String = resolvedName.localPart

  /**
   * Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option`.
   */
  final def attributeOption(expandedName: EName): Option[String] = {
    resolvedAttributes collectFirst { case (en, v) if (en == expandedName) => v }
  }

  /**
   * Returns the value of the attribute with the given expanded name, and throws an exception otherwise.
   */
  final def attribute(expandedName: EName): String =
    attributeOption(expandedName).getOrElse(sys.error(s"Missing attribute $expandedName"))

  /**
   * Returns the first found attribute value of an attribute with the given local name, if any, wrapped in an `Option`.
   * Because of differing namespaces, it is possible that more than one such attribute exists, although this is not often the case.
   */
  final def findAttributeByLocalName(localName: String): Option[String] = {
    resolvedAttributes collectFirst { case (en, v) if en.localPart == localName => v }
  }

  /**
   * Shorthand for `attributeOption(expandedName)`.
   */
  final def \@(expandedName: EName): Option[String] = attributeOption(expandedName)
}
