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
package incontext

import scala.collection.immutable

/**
 * An element within its context. In other words, an element as a pair containing the root element (as standard yaidom Elem)
 * and an element path (from that root element) to this element.
 *
 * @author Chris de Vreeze
 */
final class Elem(
  val rootElem: eu.cdevreeze.yaidom.Elem,
  val elemPath: ElemPath) extends ElemLike[Elem] with Immutable {

  val elem: eu.cdevreeze.yaidom.Elem =
    rootElem.findWithElemPath(elemPath).getOrElse(sys.error("Path %s must exist".format(elemPath)))

  override def allChildElems: immutable.IndexedSeq[Elem] = {
    val childElemPathEntries = elem.allChildElemPathEntries
    childElemPathEntries map { entry => new Elem(rootElem, elemPath.append(entry)) }
  }

  override def resolvedName: EName = elem.resolvedName

  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes

  override def equals(obj: Any): Boolean = obj match {
    case other: Elem => (other.rootElem == this.rootElem) && (other.elemPath == this.elemPath)
    case _ => false
  }

  override def hashCode: Int = (rootElem, elemPath).hashCode
}

object Elem {

  def apply(rootElem: eu.cdevreeze.yaidom.Elem, elemPath: ElemPath): Elem = {
    new Elem(rootElem, elemPath)
  }

  def apply(rootElem: eu.cdevreeze.yaidom.Elem): Elem = {
    apply(rootElem, ElemPath.Root)
  }
}
