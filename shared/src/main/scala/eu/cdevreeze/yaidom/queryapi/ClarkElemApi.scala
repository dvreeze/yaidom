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

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName

/**
 * The minimal element query API corresponding to James Clark's "labelled element tree" abstraction, which is
 * implemented as yaidom "resolved" elements.
 *
 * If a yaidom element implementation (whether in yaidom itself or a "yaidom extension")
 * does not mix in the `ClarkElemApi` trait, it is probably not to be considered "XML".
 * Indeed, in yaidom only the `ElemBuilder` class does not mix in this trait, and indeed
 * it is not "XML" (lacking any knowledge about expanded names etc.), only a builder of "XML".
 * Hence this trait is very important in yaidom, as the "minimal XML element query API".
 *
 * '''Generic code abstracting over yaidom element implementations should either use
 * this trait, or sub-trait `ScopedElemApi`, depending on the abstraction level.'''
 *
 * @author Chris de Vreeze
 */
trait ClarkElemApi extends ElemApi with IsNavigableApi {

  type ThisElem <: ClarkElemApi

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

  /**
   * Returns the concatenation of the text values of (the implicit) text children, including whitespace and CData.
   * Non-text children are ignored. If there are no text children, the empty string is returned.
   *
   * Therefore, element children are ignored and do not contribute to the resulting text string.
   */
  def text: String

  /** Returns `text.trim`. */
  def trimmedText: String

  /** Returns `XmlStringUtils.normalizeString(text)`. */
  def normalizedText: String
}

object ClarkElemApi {

  /**
   * This query API type, restricting ThisElem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = ClarkElemApi { type ThisElem = E }

  /**
   * Making ElemApi filter/find methods accept ENames which are implicitly converted to element predicates.
   */
  implicit class WithEName(val ename: EName) extends (ClarkElemApi => Boolean) {

    def apply(elem: ClarkElemApi): Boolean =
      (elem.resolvedName == this.ename)
  }

  /**
   * Returns the equivalent of `{ _.ename == ename }`
   */
  def withEName(ename: EName): (ClarkElemApi => Boolean) = { elem =>
    elem.resolvedName == ename
  }

  /**
   * Returns the equivalent of `{ _.ename == EName(namespaceOption, localPart) }`, but without creating any EName instance.
   */
  def withEName(namespaceOption: Option[String], localPart: String): (ClarkElemApi => Boolean) = { elem =>
    val resolvedName = elem.resolvedName
    (resolvedName.namespaceUriOption == namespaceOption) && (resolvedName.localPart == localPart)
  }

  /**
   * Returns the equivalent of `withEName(Some(namespace), localPart)`
   */
  def withEName(namespace: String, localPart: String): (ClarkElemApi => Boolean) =
    withEName(Some(namespace), localPart)

  /**
   * Returns the equivalent of `withEName(None, localPart)`
   */
  def withNoNsEName(localPart: String): (ClarkElemApi => Boolean) =
    withEName(None, localPart)

  /**
   * Returns the equivalent of `{ _.localName == localName }`
   */
  def withLocalName(localName: String): (ClarkElemApi => Boolean) = { elem =>
    elem.localName == localName
  }
}
