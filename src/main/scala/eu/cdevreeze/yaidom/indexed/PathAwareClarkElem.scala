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

package eu.cdevreeze.yaidom.indexed

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemLike
import eu.cdevreeze.yaidom.queryapi.Nodes

/**
 * Very lightweight path-aware element implementation. It offers the `ClarkElemApi` query API. It is optimized
 * for fast (just-in-time) element creation, not for fast querying. Use this whenever wanting to query for (pairs of
 * elements and) paths, for example to collect the paths before using them to functionally update the element tree.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
final class PathAwareClarkElem[U <: ClarkElemApi[U]](
  val path: Path,
  val elem: U) extends Nodes.Elem with ClarkElemLike[PathAwareClarkElem[U]] {

  def this(elem: U) = this(Path.Root, elem)

  private implicit val uTag: ClassTag[U] = classTag[U]

  final def findAllChildElems: immutable.IndexedSeq[PathAwareClarkElem[U]] = {
    elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        new PathAwareClarkElem(path.append(entry), e)
    }
  }

  final def resolvedName: EName = elem.resolvedName

  final def resolvedAttributes: immutable.Iterable[(EName, String)] = elem.resolvedAttributes

  final def text: String = elem.text

  final override def equals(obj: Any): Boolean = obj match {
    case other: PathAwareClarkElem[U] =>
      (other.path == this.path) && (other.elem == this.elem)
    case _ => false
  }

  final override def hashCode: Int = (path, elem).hashCode
}
