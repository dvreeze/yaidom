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
import scala.collection.mutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path

/**
 * Partial implementation of `ClarkElemApi`.
 *
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait ClarkElemLike[E <: ClarkElemLike[E]] extends ClarkElemApi[E] with ElemLike[E] with IsNavigable[E] with HasEName with HasText { self: E =>

  /**
   * Finds the child element with the given `Path.Entry` (where this element is the root), if any, wrapped in an `Option`.
   *
   * This method is final, so more efficient implementations for sub-types are not supported. This implementation
   * is only efficient if finding all child elements as well as computing their resolved names is efficient.
   * That is not the case for DOM wrappers or Scala XML Elem wrappers (due to their expensive Scope computations).
   * On the other hand, those wrapper element implementations are convenient, but not intended for heavy use in
   * production. Hence, this method should typically be fast enough.
   */
  final override def findChildElemByPathEntry(entry: Path.Entry): Option[E] = {
    // The previous implementation used immutable.IndexedSeq.toStream, which turned out to be surprisingly inefficient.
    // This inefficiency was noticed when calling method IsNavigable.findReverseAncestryOrSelfByPath
    // (and therefore this method) many times. Thanks to Johan Walters for pointing out this performance issue.

    var sameENameIdx = 0
    val childElemOption = findAllChildElems find { e =>
      val ename = e.resolvedName
      if (ename == entry.elementName) {
        if (entry.index == sameENameIdx) true
        else {
          sameENameIdx += 1
          false
        }
      } else false
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
  final override def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, Path.Entry)] = {
    val nextEntries = mutable.Map[EName, Int]()

    findAllChildElems map { e =>
      val ename = e.resolvedName
      val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
      nextEntries.put(ename, entry.index + 1)
      (e, entry)
    }
  }
}
