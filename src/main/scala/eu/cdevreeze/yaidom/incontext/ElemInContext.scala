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
 * An element with its context, starting with the root element.
 *
 * @author Chris de Vreeze
 */
final class ElemInContext(
  val rootElem: Elem,
  val elemPath: ElemPath) extends PathAwareElemLike[ElemInContext] with Immutable {

  val elem: Elem = rootElem.findWithElemPath(elemPath).getOrElse(sys.error("Path %s must exist".format(elemPath)))

  override def allChildElems: immutable.IndexedSeq[ElemInContext] = {
    val childElemPathEntries = elem.allChildElemPathEntries
    childElemPathEntries map { entry => new ElemInContext(rootElem, elemPath.append(entry)) }
  }

  override def resolvedName: EName = elem.resolvedName

  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = elem.resolvedAttributes
}
