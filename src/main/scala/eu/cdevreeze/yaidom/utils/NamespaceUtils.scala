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

package eu.cdevreeze.yaidom.utils

import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Elem

/**
 * Utility for manipulating namespaces.
 *
 * @author Chris de Vreeze
 */
object NamespaceUtils {

  /**
   * Pushes up prefixed namespace declarations as far to the root element as possible.
   * Preferably all prefixed namespace declarations end up in the root element afterwards.
   *
   * Note that a `simple.Elem` does not explicitly store namespace declarations, but these are implicit
   * instead, as the difference between the Scope and the Scope of its parent element.
   *
   * The function can be defined as:
   * {{{
   * elem.notUndeclaringPrefixes(findSuitableParentScope(elem))
   * }}}
   *
   * Therefore:
   * {{{
   * resolved.Elem(pushUpPrefixedNamespaces(elem)) == resolved.Elem(elem)
   * }}}
   * and the result contains no prefixed namespace undeclarations (not allowed in XML 1.0).
   */
  def pushUpPrefixedNamespaces(elem: Elem): Elem = {
    elem.notUndeclaringPrefixes(findSuitableParentScope(elem))
  }

  private def findSuitableParentScope(elem: Elem): Scope = {
    // Recursive calls
    val combinedChildScope =
      elem.findAllChildElems.foldLeft(Scope.Empty) {
        case (accScope, che) =>
          accScope ++ (findSuitableParentScope(che).withoutDefaultNamespace)
      }
    assert(combinedChildScope.defaultNamespaceOption.isEmpty)
    combinedChildScope ++ elem.scope.withoutDefaultNamespace
  }
}
