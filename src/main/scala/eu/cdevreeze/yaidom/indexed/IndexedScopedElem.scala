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

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi.Nodes
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport

/**
 * Indexed Scoped element. Like `IndexedClarkElem` but instead of being and indexing
 * a `ClarkElemApi`, it is and indexes a `ScopedElemApi`. Other than that, see the
 * documentation for `IndexedClarkElem`.
 *
 * The optional parent base URI is stored for very fast (optional) base URI computation. This is helpful in
 * an XBRL context, where URI resolution against a base URI is typically a very frequent operation.
 *
 * @tparam U The underlying element type
 *
 * @author Chris de Vreeze
 */
final class IndexedScopedElem[U <: ScopedElemApi[U]] private (
  val docUriOption: Option[URI],
  val rootElem: U,
  val path: Path,
  val elem: U) extends Nodes.Elem with IndexedScopedElemLike[IndexedScopedElem[U], U] {

  private implicit val uTag: ClassTag[U] = classTag[U]

  /**
   * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
   * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
   * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
   * class, it can be reasoned that these assertions must hold.)
   */
  private[yaidom] def assertConsistency(): Unit = {
    assert(elem == rootElem.getElemOrSelfByPath(path), "Corrupt element!")
  }

  final def findAllChildElems: immutable.IndexedSeq[IndexedScopedElem[U]] = {
    elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        new IndexedScopedElem(docUriOption, rootElem, path.append(entry), e)
    }
  }

  final def baseUriOption: Option[URI] = {
    XmlBaseSupport.findBaseUriByDocUriAndPath(docUriOption, rootElem, path)(XmlBaseSupport.JdkUriResolver)
  }

  final def parentBaseUriOption: Option[URI] = {
    if (path.isEmpty) {
      docUriOption
    } else {
      XmlBaseSupport.findBaseUriByDocUriAndPath(docUriOption, rootElem, path.parentPath)(XmlBaseSupport.JdkUriResolver)
    }
  }

  final override def equals(obj: Any): Boolean = obj match {
    case other: IndexedScopedElem[U] =>
      (other.docUriOption == this.docUriOption) && (other.rootElem == this.rootElem) &&
        (other.path == this.path) && (other.elem == this.elem)
    case _ => false
  }

  final override def hashCode: Int = (docUriOption, rootElem, path, elem).hashCode

  /**
   * Returns the document URI, falling back to the empty URI if absent.
   */
  final def docUri: URI = docUriOption.getOrElse(new URI(""))

  /**
   * Returns the base URI, falling back to the empty URI if absent.
   */
  final def baseUri: URI = baseUriOption.getOrElse(new URI(""))
}

object IndexedScopedElem {

  def apply[U <: ScopedElemApi[U]](docUriOption: Option[URI], rootElem: U, path: Path): IndexedScopedElem[U] = {
    new IndexedScopedElem[U](docUriOption, rootElem, path, rootElem.getElemOrSelfByPath(path))
  }

  def apply[U <: ScopedElemApi[U]](docUri: URI, rootElem: U, path: Path): IndexedScopedElem[U] = {
    apply(Some(docUri), rootElem, path)
  }

  def apply[U <: ScopedElemApi[U]](rootElem: U, path: Path): IndexedScopedElem[U] = {
    new IndexedScopedElem[U](None, rootElem, path, rootElem.getElemOrSelfByPath(path))
  }

  def apply[U <: ScopedElemApi[U]](docUriOption: Option[URI], rootElem: U): IndexedScopedElem[U] = {
    apply(docUriOption, rootElem, Path.Empty)
  }

  def apply[U <: ScopedElemApi[U]](docUri: URI, rootElem: U): IndexedScopedElem[U] = {
    apply(Some(docUri), rootElem, Path.Empty)
  }

  def apply[U <: ScopedElemApi[U]](rootElem: U): IndexedScopedElem[U] = {
    apply(None, rootElem, Path.Empty)
  }
}
