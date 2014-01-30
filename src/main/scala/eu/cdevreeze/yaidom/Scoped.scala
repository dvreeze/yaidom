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
 * Class that supports EName-based querying through the use of QNames and a given scope.
 *
 * For example:
 * {{{
 * val usedScope = Scope.from("xs" -> "http://www.w3.org/2001/XMLSchema")
 * val scoped = Scoped(usedScope)
 *
 * import scoped._
 *
 * val elemDecls = schemaElem \\ withEName(QName("xs", "element").e)
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

  implicit class ToEName(val qname: QName) {

    /**
     * Expands (or resolves) the QName, using the "usedScope".
     */
    def e: EName = usedScope.resolveQNameOption(qname).getOrElse(sys.error(s"Could not resolve $qname with scope $usedScope"))
  }
}

object Scoped {

  /**
   * Creates a Scope for queries that can use QNames rather than ENames, without loss of meaning.
   */
  def apply(usedScope: Scope): Scoped = new Scoped(usedScope)
}
