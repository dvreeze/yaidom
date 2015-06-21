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

  final override def findChildElemByPathEntry(entry: Path.Entry): Option[E] = {
    val filteredChildElems = findAllChildElems.toStream filter { e => e.resolvedName == entry.elementName }

    val childElemOption = filteredChildElems.drop(entry.index).headOption
    assert(childElemOption.forall(_.resolvedName == entry.elementName))
    childElemOption
  }

  final def findAllChildElemsWithPathEntries: immutable.IndexedSeq[(E, Path.Entry)] = {
    val nextEntries = mutable.Map[EName, Int]()

    findAllChildElems map { e =>
      val ename = e.resolvedName
      val entry = Path.Entry(ename, nextEntries.getOrElse(ename, 0))
      nextEntries.put(ename, entry.index + 1)
      (e, entry)
    }
  }
}
