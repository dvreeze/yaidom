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

import scala.collection.immutable

/**
 * This `ElemFunctions` singleton object makes it easy to query for elements having specific resolved names, among other things.
 *
 * For example, using method `havingEName`:
 * {{{
 * import ElemFunctions._
 *
 * val bookstoreNamespace = "http://bookstore/book"
 * val authorNamespace = "http://bookstore/author"
 * require(bookstoreElem.resolvedName == EName(bookstoreNamespace, "Bookstore"))
 *
 * val cheapBookElems =
 *   for {
 *     bookElem <- bookstoreElem \ havingEName(bookstoreNamespace, "Book")
 *     price <- bookElem \@ EName("Price")
 *     if price.toInt < 90
 *   } yield bookElem
 * }}}
 *
 * Yaidom users are encouraged to use predicate-creating methods like `havingEName`. These methods are useful in queries such as
 * those offered by `ElemApi` and `PathAwareElemApi`. Moreover, we can use method `havingLocalName` if we do not want to consider
 * the namespace when querying. Another reason to use one of the overloaded `havingEName` methods is that we can prevent the creation
 * of many very short-lived EName objects (or at least do not have to rely on JVM optimizations that make such object creation
 * extremely cheap).
 */
object ElemFunctions {

  /**
   * Returns the equivalent of `{ elem => elem.resolvedName == ename }`.
   */
  def havingEName[E <: ElemApi[E]](ename: EName): (E => Boolean) = { elem =>
    elem.resolvedName == ename
  }

  /**
   * Returns the equivalent of `{ elem => elem.resolvedName == EName(namespaceUriOption, localPart) }`.
   *
   * The implementation prevents unnecessary EName object creation, however.
   */
  def havingEName[E <: ElemApi[E]](namespaceUriOption: Option[String], localPart: String): (E => Boolean) = { elem =>
    (elem.resolvedName.namespaceUriOption == namespaceUriOption) && (elem.resolvedName.localPart == localPart)
  }

  /**
   * Returns the equivalent of `{ elem => elem.resolvedName == EName(namespaceUri, localPart) }`.
   *
   * The implementation prevents unnecessary EName object creation, however.
   */
  def havingEName[E <: ElemApi[E]](namespaceUri: String, localPart: String): (E => Boolean) = { elem =>
    (elem.resolvedName.namespaceUriOption == Some(namespaceUri)) && (elem.resolvedName.localPart == localPart)
  }

  /**
   * Returns the equivalent of `{ elem => elem.resolvedName == EName(None, localPart) }`.
   *
   * The implementation prevents unnecessary EName object creation, however.
   */
  def havingEName[E <: ElemApi[E]](localPart: String): (E => Boolean) = { elem =>
    (elem.resolvedName.namespaceUriOption.isEmpty) && (elem.resolvedName.localPart == localPart)
  }

  /**
   * Returns the equivalent of `{ elem => elem.resolvedName == EName(enameRepr) }`.
   *
   * The implementation prevents unnecessary EName object creation, however.
   */
  def havingEncodedEName[E <: ElemApi[E]](enameRepr: String): (E => Boolean) = { elem =>
    elem.resolvedName.toString == enameRepr
  }

  /**
   * Returns the equivalent of `{ elem => elem.localName == localName }`.
   */
  def havingLocalName[E <: ElemApi[E]](localName: String): (E => Boolean) = { elem =>
    elem.localName == localName
  }
}
