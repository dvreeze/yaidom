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
import scala.collection.mutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * Partial implementation of `ClarkElemApi`.
 *
 * @author Chris de Vreeze
 */
trait ClarkElemLike extends ClarkElemApi with ElemLike with IsNavigable {

  type ThisElem <: ClarkElemLike.Aux[ThisElem]

  /**
   * Finds the child element with the given `Path.Entry` (where this element is the root), if any, wrapped in an `Option`.
   *
   * This method is final, so more efficient implementations for sub-types are not supported. This implementation
   * is only efficient if finding all child elements as well as computing their resolved names is efficient.
   * That is not the case for DOM wrappers or Scala XML Elem wrappers (due to their expensive Scope computations).
   * On the other hand, those wrapper element implementations are convenient, but not intended for heavy use in
   * production. Hence, this method should typically be fast enough.
   */
  final override def findChildElemByPathEntry(entry: Path.Entry): Option[ThisElem] = {
    // The previous implementation used immutable.IndexedSeq.toStream, which turned out to be surprisingly inefficient.
    // This inefficiency was noticed when calling method IsNavigable.findReverseAncestryOrSelfByPath
    // (and therefore this method) many times. Thanks to Johan Walters for pointing out this performance issue.

    var sameENameIdx = 0
    val childElemOption = findAllChildElems find { e =>
      val ename = e.resolvedName

      if (ename == entry.elementName) {
        if (entry.index == sameENameIdx) {
          true
        } else {
          sameENameIdx += 1
          false
        }
      } else {
        false
      }
    }
    assert(childElemOption.forall(_.resolvedName == entry.elementName))
    childElemOption
  }

  /**
   * Returns all child elements paired with their path entries.
   *
   * This method is final, so more efficient implementations for sub-types are not supported. This implementation
   * is only efficient if finding all child elements as well as computing their resolved names is efficient.
   * That is not the case for DOM wrappers or Scala XML Elem wrappers (due to their expensive Scope computations).
   * On the other hand, those wrapper element implementations are convenient, but not intended for heavy use in
   * production. Hence, this method should typically be fast enough.
   */
  final override def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(ThisElem, Path.Entry)] = {
    val nextEntries = mutable.Map[EName, Int]()

    findAllChildElems map { e =>
      val ename = e.resolvedName
      val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
      nextEntries.put(ename, entry.index + 1)
      (e, entry)
    }
  }

  /**
   * The local name, that is, the local part of the EName
   */
  final def localName: String = {
    resolvedName.localPart
  }

  /**
   * Returns the value of the attribute with the given expanded name, if any, wrapped in an `Option`.
   */
  final def attributeOption(expandedName: EName): Option[String] = {
    resolvedAttributes find { case (en, v) => (en == expandedName) } map (_._2)
  }

  /**
   * Returns the value of the attribute with the given expanded name, and throws an exception otherwise.
   */
  final def attribute(expandedName: EName): String = {
    attributeOption(expandedName).getOrElse(sys.error(s"Missing attribute $expandedName"))
  }

  /**
   * Returns the first found attribute value of an attribute with the given local name, if any, wrapped in an `Option`.
   * Because of differing namespaces, it is possible that more than one such attribute exists, although this is not often the case.
   */
  final def findAttributeByLocalName(localName: String): Option[String] = {
    resolvedAttributes find { case (en, v) => en.localPart == localName } map (_._2)
  }

  /**
   * Shorthand for `attributeOption(expandedName)`.
   */
  final def \@(expandedName: EName): Option[String] = {
    attributeOption(expandedName)
  }

  /** Returns `text.trim`. */
  final def trimmedText: String = {
    text.trim
  }

  /** Returns `XmlStringUtils.normalizeString(text)`. */
  final def normalizedText: String = {
    XmlStringUtils.normalizeString(text)
  }
}

object ClarkElemLike {

  /**
   * This query API type, restricting ThisElem to the type parameter.
   *
   * @tparam E The element self type
   */
  type Aux[E] = ClarkElemLike { type ThisElem = E }
}
