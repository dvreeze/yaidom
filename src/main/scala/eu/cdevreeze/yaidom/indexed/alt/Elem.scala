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

package eu.cdevreeze.yaidom.indexed.alt

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.indexed.AbstractIndexedElem
import eu.cdevreeze.yaidom.simple

/**
 * Like an "indexed" element, except that the implementation prefers fast element tree creation over fast querying.
 *
 * @author Chris de Vreeze
 */
final class Elem private[indexed] (
  val rootElem: simple.Elem,
  val path: Path,
  val elem: simple.Elem) extends AbstractIndexedElem[Elem] {

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(elem == rootElem.getElemOrSelfByPath(path), "Corrupt element!")
    assert(findAllChildElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
  }

  /**
   * Returns all child elements, in the correct order.
   *
   * These child elements share the same rootElem with this element, but differ in the paths, which have one more
   * "path entry".
   */
  override def findAllChildElems: immutable.IndexedSeq[Elem] = {
    val childElems = elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        new Elem(rootElem, path.append(entry), e)
    }
    childElems
  }

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem => (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  override def hashCode: Int = (rootElem, path).hashCode
}

object Elem {

  /**
   * Calls `apply(rootElem, Path.Root)`
   */
  def apply(rootElem: simple.Elem): Elem = {
    apply(rootElem, Path.Root)
  }

  /**
   * Factory method for "indexed2 elements".
   */
  def apply(rootElem: simple.Elem, path: Path): Elem = {
    // Expensive call, so invoked only once
    val elem = rootElem.getElemOrSelfByPath(path)

    new Elem(rootElem, path, elem)
  }
}
