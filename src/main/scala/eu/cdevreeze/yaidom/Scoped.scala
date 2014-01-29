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
 * Class that supports robust QName-based querying within an explicitly given Scope. QName-based querying is more concise
 * than EName-based querying, and by explicitly declaring a Scope for those QNames, the semantics are the same as for the
 * equivalent EName-based queries.
 *
 * For example:
 * {{{
 * val usedScope = Scope.from("xs" -> "http://www.w3.org/2001/XMLSchema")
 * val scoped = Scoped(usedScope)
 *
 * import scoped._
 *
 * val elemDecls = schemaElem \\ withQNameInScope("xs", "element")
 * }}}
 *
 * This is exactly equivalent to the following query:
 * {{{
 * import ElemApi._
 *
 * val elemDecls = schemaElem \\ withEName("http://www.w3.org/2001/XMLSchema", "element")
 * }}}
 *
 * @author Chris de Vreeze
 */
final class Scoped(val usedScope: Scope) {

  /**
   * Returns the equivalent of `{ e => usedScope.resolveQNameOption(qname) == Some(e.resolvedName) }`
   */
  def withQNameInScope(qname: QName): (ElemApi[_] => Boolean) = { elem =>
    val enameOption = usedScope.resolveQNameOption(qname)
    enameOption == Some(elem.resolvedName)
  }

  /**
   * Returns the equivalent of `withQNameInScope(qnameProvider.getQName(prefixOption, localPart))`
   */
  def withQNameInScope(prefixOption: Option[String], localPart: String)(implicit qnameProvider: QNameProvider): (ElemApi[_] => Boolean) = {
    withQNameInScope(qnameProvider.getQName(prefixOption, localPart))
  }

  /**
   * Returns the equivalent of `withQNameInScope(qnameProvider.getQName(prefix, localPart))`
   */
  def withQNameInScope(prefix: String, localPart: String)(implicit qnameProvider: QNameProvider): (ElemApi[_] => Boolean) = {
    withQNameInScope(qnameProvider.getQName(prefix, localPart))
  }

  /**
   * Returns the equivalent of `withQNameInScope(qnameProvider.getQName(None, localPart))`
   */
  def withUnprefixedQNameInScope(localPart: String)(implicit qnameProvider: QNameProvider): (ElemApi[_] => Boolean) = {
    withQNameInScope(qnameProvider.getQName(None, localPart))
  }
}

object Scoped {

  /**
   * Creates a Scope for queries that can use QNames rather than ENames, without loss of meaning.
   */
  def apply(usedScope: Scope): Scoped = new Scoped(usedScope)
}
