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

import java.rmi.server.UID

/**
 * `Document` with the `ElemPath`s stored with the elements (via the UIDs). With this index, parent nodes can be found very efficiently.
 *
 * @author Chris de Vreeze
 */
final class IndexedDocument(val document: Document) extends Immutable {

  /** The index from the element `UID`s to the `ElemPath`s. Computed only once, during construction of this `IndexedDocument`. */
  val elemPaths: Map[UID, ElemPath] = document.documentElement.getElemPaths

  /**
   * Finds the parent of the given `Elem`, if any, wrapped in an `Option`.
   * If the given `Elem` is not found (via its UID), or has no parent (which means it is the document root element), `None` is returned.
   */
  def findParent(elm: Elem): Option[Elem] = {
    val parentElmOption: Option[Elem] =
      for {
        elemPath <- elemPaths.get(elm.uid)
        parentPath <- elemPath.parentPathOption
        foundElm <- document.documentElement.findWithElemPath(parentPath)
      } yield foundElm

    parentElmOption
  }
}
