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

package eu.cdevreeze.yaidom.utils

import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi

/**
 * Utility for manipulating and finding namespaces.
 *
 * @author Chris de Vreeze
 */
object NamespaceUtils {

  // TODO No dependency on simple.Elem

  /**
   * Pushes up prefixed namespace declarations as far to the root element as possible.
   * Preferably all prefixed namespace declarations end up in the root element afterwards.
   *
   * Note that a `simple.Elem` does not explicitly store namespace declarations, but these are implicit
   * instead, as the difference between the Scope and the Scope of its parent element.
   *
   * The function can be defined as:
   * {{{
   * elem.notUndeclaringPrefixes(findCombinedScopeWithoutDefaultNamespace(elem))
   * }}}
   *
   * Therefore:
   * {{{
   * resolved.Elem(pushUpPrefixedNamespaces(elem)) == resolved.Elem(elem)
   * }}}
   * and the result contains no prefixed namespace undeclarations (not allowed in XML 1.0).
   */
  def pushUpPrefixedNamespaces(elem: Elem): Elem = {
    elem.notUndeclaringPrefixes(findCombinedScopeWithoutDefaultNamespace(elem))
  }

  /**
   * Finds the combined Scope of the entire element tree, without default namespace. In case of conflicts, Scopes of
   * elements higher in the tree take precedence, and conflicting Scopes in sibling elements are combined by choosing
   * one of the namespaces for the prefix in question.
   */
  def findCombinedScopeWithoutDefaultNamespace(elem: ScopedElemApi): Scope = {
    // Recursive calls
    val combinedChildScope =
      elem.findAllChildElems.foldLeft(Scope.Empty) {
        case (accScope, che) =>
          accScope ++ (findCombinedScopeWithoutDefaultNamespace(che).withoutDefaultNamespace)
      }
    assert(combinedChildScope.defaultNamespaceOption.isEmpty)
    combinedChildScope ++ elem.scope.withoutDefaultNamespace
  }

  // TODO No dependency on indexed.Elem and simple.Elem

  /**
   * Returns an adapted copy (as simple Elem) where all unused namespaces have been removed from the Scopes (of the
   * element and its descendants). To determine the used namespaces, method `findAllNamespaces` is called on the
   * element.
   *
   * The root element of the given indexed element must be the root element of the document.
   *
   * This method is very useful when retrieving and serializing a small fragment of an XML tree in which most
   * namespace declarations in the root element are not needed for the retrieved fragment.
   */
  def stripUnusedNamespaces(elem: indexed.Elem, documentENameExtractor: DocumentENameExtractor): Elem = {
    val usedNamespaces = findAllNamespaces(elem, documentENameExtractor)

    val resultElem = elem.underlyingElem transformElemsOrSelf { e =>
      e.copy(scope = e.scope filter { case (pref, ns) => usedNamespaces.contains(ns) })
    }
    resultElem
  }

  /**
   * Finds all ENames, in element names, attribute names, but also in element content and attribute values,
   * using the given DocumentENameExtractor. The element and all its descendants are taken into account.
   *
   * The root element of the given indexed element must be the root element of the document.
   */
  def findAllENames(elem: BackingElemApi, documentENameExtractor: DocumentENameExtractor): Set[EName] = {
    val enames =
      elem.findAllElemsOrSelf.flatMap(e => findENamesInElementItself(e, documentENameExtractor)).toSet
    enames
  }

  /**
   * Returns `findAllENames(elem, documentENameExtractor).flatMap(_.namespaceUriOption)`.
   * That is, finds all namespaces used in the element and its descendants.
   *
   * The root element of the given indexed element must be the root element of the document.
   */
  def findAllNamespaces(elem: BackingElemApi, documentENameExtractor: DocumentENameExtractor): Set[String] = {
    findAllENames(elem, documentENameExtractor).flatMap(_.namespaceUriOption)
  }

  /**
   * Finds the ENames, in element name, attribute names, but also in element content and attribute values,
   * using the given DocumentENameExtractor. Only the element itself is taken into consideration, not its
   * descendants.
   *
   * The root element of the given indexed element must be the root element of the document.
   */
  def findENamesInElementItself(elem: BackingElemApi, documentENameExtractor: DocumentENameExtractor): Set[EName] = {
    val scope = elem.scope

    val enamesInElemText: Set[EName] =
      documentENameExtractor.findElemTextENameExtractor(elem).map(_.extractENames(scope, elem.text)).getOrElse(Set())

    val enamesInAttrValues: Set[EName] =
      (elem.resolvedAttributes flatMap {
        case (attrEName, attrValue) =>
          documentENameExtractor.findAttributeValueENameExtractor(elem, attrEName).map(_.extractENames(scope, attrValue)).getOrElse(Set())
      }).toSet

    Set(elem.resolvedName).union(elem.resolvedAttributes.toMap.keySet).union(enamesInElemText).union(enamesInAttrValues)
  }

  /**
   * Returns `findENamesInElementItself(elem, documentENameExtractor).flatMap(_.namespaceUriOption)`.
   * That is, finds the namespaces used in the element itself, ignoring its descendants.
   *
   * The root element of the given indexed element must be the root element of the document.
   */
  def findNamespacesInElementItself(elem: BackingElemApi, documentENameExtractor: DocumentENameExtractor): Set[String] = {
    findENamesInElementItself(elem, documentENameExtractor).flatMap(_.namespaceUriOption)
  }
}
