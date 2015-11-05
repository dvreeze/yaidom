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

import java.net.URI

import scala.collection.immutable
import scala.reflect.ClassTag
import scala.reflect.classTag

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.queryapi.Nodes

/**
 * Very lightweight lazy indexed element implementation. It offers the `IndexedScopedElemApi` query API. It is optimized
 * for fast (just-in-time) element creation, not for fast querying.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
final class LazyIndexedScopedElem[U <: ScopedElemApi[U]] private (
  val docUriOption: Option[URI],
  val rootElem: U,
  val path: Path,
  val elem: U) extends Nodes.Elem with IndexedScopedElemLike[LazyIndexedScopedElem[U], U] {

  private implicit val uTag: ClassTag[U] = classTag[U]

  final def findAllChildElems: immutable.IndexedSeq[LazyIndexedScopedElem[U]] = {
    elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        new LazyIndexedScopedElem(docUriOption, rootElem, path.append(entry), e)
    }
  }

  final def baseUriOption: Option[URI] = {
    XmlBaseSupport.findBaseUriByDocUriAndPath(docUriOption, rootElem, path)(XmlBaseSupport.JdkUriResolver)
  }

  final override def equals(obj: Any): Boolean = obj match {
    case other: LazyIndexedScopedElem[U] =>
      (other.docUriOption == this.docUriOption) && (other.rootElem == this.rootElem) &&
        (other.path == this.path) && (other.elem == this.elem)
    case _ => false
  }

  final override def hashCode: Int = (docUriOption, rootElem, path, elem).hashCode
}

object LazyIndexedScopedElem {

  def apply[U <: ScopedElemApi[U]](docUriOption: Option[URI], rootElem: U, path: Path): LazyIndexedScopedElem[U] = {
    new LazyIndexedScopedElem[U](docUriOption, rootElem, path, rootElem.getElemOrSelfByPath(path))
  }

  def apply[U <: ScopedElemApi[U]](rootElem: U, path: Path): LazyIndexedScopedElem[U] = {
    new LazyIndexedScopedElem[U](None, rootElem, path, rootElem.getElemOrSelfByPath(path))
  }

  def apply[U <: ScopedElemApi[U]](docUriOption: Option[URI], rootElem: U): LazyIndexedScopedElem[U] = {
    apply(docUriOption, rootElem, Path.Empty)
  }

  def apply[U <: ScopedElemApi[U]](rootElem: U): LazyIndexedScopedElem[U] = {
    apply(None, rootElem, Path.Empty)
  }
}
