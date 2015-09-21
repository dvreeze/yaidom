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

import scala.Vector
import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path

/**
 * Easy-to-use API and implementation trait for functionally updatable elements.
 *
 * The purely abstract API offered by this trait is [[eu.cdevreeze.yaidom.queryapi.RichUpdatableElemApi]].
 *
 * @tparam N The node supertype of the element subtype
 * @tparam E The captured element subtype
 *
 * @author Chris de Vreeze
 */
trait RichUpdatableElemLike[N, E <: N with RichUpdatableElemLike[N, E]] extends UpdatableElemLike[N, E] with ClarkElemLike[E] { self: E =>

  final def withUpdatedChildElems(f: (E, Path.Entry) => Option[immutable.IndexedSeq[N]]): E = {
    val childElemsWithPathEntries = findAllChildElemsWithPathEntries

    val pathEntriesWithNewNodeSeqs =
      childElemsWithPathEntries flatMap {
        case (elm, pathEntry) =>
          val newNodeSeqOpt = f(elm, pathEntry)

          newNodeSeqOpt.map(nodeSeq => (pathEntry -> nodeSeq))
      }

    val newNodeSeqsByPathEntry: Map[Path.Entry, immutable.IndexedSeq[N]] =
      pathEntriesWithNewNodeSeqs.toMap

    val resultElem = updatedWithNodeSeqAtPathEntries(newNodeSeqsByPathEntry.keySet) {
      case (elm, pathEntry) =>
        newNodeSeqsByPathEntry(pathEntry)
    }
    resultElem
  }

  final def withUpdatedElems(f: (E, Path) => Option[immutable.IndexedSeq[N]]): E = {
    def g(che: E, pathEntry: Path.Entry): Option[immutable.IndexedSeq[N]] = {
      // Recursive call
      val newChe =
        che withUpdatedElems { case (e, p) => f(e, p.prepend(pathEntry)) }

      val newNodesOption = f(newChe, Path(Vector(pathEntry)))

      val pathEntryUpdated = (newChe != che) || (newNodesOption.isDefined)

      if (pathEntryUpdated) newNodesOption.orElse(Some(Vector(newChe))) else None
    }

    val resultElem = withUpdatedChildElems(g)
    resultElem
  }
}
