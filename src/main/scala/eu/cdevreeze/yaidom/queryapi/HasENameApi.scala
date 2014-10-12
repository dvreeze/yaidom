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

/**
 * Trait defining the contract for elements that have a EName, as well as attributes with EName keys.
 *
 * Using this trait (possibly in combination with other "element traits") we can abstract over several element implementations.
 *
 * @author Chris de Vreeze
 */
trait HasENameApi {

  /**
   * The EName of the element
   */
  def resolvedName: EName

  /**
   * The resolved attributes of the element as mapping from ENames to values
   */
  def resolvedAttributes: immutable.Iterable[(EName, String)]

  /**
   * The local name, that is, the local part of the EName
   */
  def localName: String

  /**
   * Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option`.
   */
  def attributeOption(expandedName: EName): Option[String]

  /**
   * Returns the value of the attribute with the given expanded name, and throws an exception otherwise.
   */
  def attribute(expandedName: EName): String

  /**
   * Returns the first found attribute value of an attribute with the given local name, if any, wrapped in an `Option`.
   * Because of differing namespaces, it is possible that more than one such attribute exists, although this is not often the case.
   */
  def findAttributeByLocalName(localName: String): Option[String]

  /**
   * Shorthand for `attributeOption(expandedName)`.
   */
  def \@(expandedName: EName): Option[String]
}

/**
 * This companion object offers some convenience factory methods for "element predicates", that can be used in yaidom queries.
 * These factory objects turn ENames and local names into "element predicates".
 *
 * For example:
 * {{{
 * elem \\ (_.ename == EName(xsNamespace, "element"))
 * }}}
 * can also be written as:
 * {{{
 * elem \\ withEName(xsNamespace, "element")
 * }}}
 * (thus avoiding EName instance construction, whether or not this makes any difference in practice).
 *
 * If the namespace is "obvious", and more friendly local-name-based querying is desired, the following could be written:
 * {{{
 * elem \\ withLocalName("element")
 * }}}
 */
object HasENameApi {

  /**
   * Returns the equivalent of `{ _.ename == ename }`
   */
  def withEName(ename: EName): (ElemApi[_] with HasENameApi => Boolean) = { elem =>
    elem.resolvedName == ename
  }

  /**
   * Returns the equivalent of `{ _.ename == EName(namespaceOption, localPart) }`, but without creating any EName instance.
   */
  def withEName(namespaceOption: Option[String], localPart: String): (ElemApi[_] with HasENameApi => Boolean) = { elem =>
    val resolvedName = elem.resolvedName
    (resolvedName.namespaceUriOption == namespaceOption) && (resolvedName.localPart == localPart)
  }

  /**
   * Returns the equivalent of `withEName(Some(namespace), localPart)`
   */
  def withEName(namespace: String, localPart: String): (ElemApi[_] with HasENameApi => Boolean) =
    withEName(Some(namespace), localPart)

  /**
   * Returns the equivalent of `withEName(None, localPart)`
   */
  def withNoNsEName(localPart: String): (ElemApi[_] with HasENameApi => Boolean) =
    withEName(None, localPart)

  /**
   * Returns the equivalent of `{ _.localName == localName }`
   */
  def withLocalName(localName: String): (ElemApi[_] with HasENameApi => Boolean) = { elem =>
    elem.localName == localName
  }
}
