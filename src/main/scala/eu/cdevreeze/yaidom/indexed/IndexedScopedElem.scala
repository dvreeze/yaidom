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
import eu.cdevreeze.yaidom.queryapi.ScopedElemApi
import eu.cdevreeze.yaidom.queryapi.Nodes

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
  val parentBaseUriOption: Option[URI],
  val rootElem: U,
  childElems: immutable.IndexedSeq[IndexedScopedElem[U]],
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
    assert(childElems.map(_.elem) == elem.findAllChildElems, "Corrupt element!")
    assert(childElems.forall(_.docUriOption eq this.docUriOption), "Corrupt element!")
  }

  final def findAllChildElems: immutable.IndexedSeq[IndexedScopedElem[U]] = childElems

  final override def equals(obj: Any): Boolean = obj match {
    case other: IndexedScopedElem[U] =>
      (other.docUriOption == docUriOption) && (other.rootElem == this.rootElem) && (other.path == this.path)
    case _ => false
  }

  final override def hashCode: Int = (docUriOption, rootElem, path).hashCode

  final def baseUriOption: Option[URI] = {
    IndexedClarkElemLike.getNextBaseUriOption(parentBaseUriOption, elem)
  }

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

  /**
   * Returns the same as `apply(None, rootElem)`.
   */
  def apply[U <: ScopedElemApi[U]](rootElem: U): IndexedScopedElem[U] =
    apply(None, rootElem)

  /**
   * Returns the same as `apply(docUriOption, rootElem, Path.Root)`.
   */
  def apply[U <: ScopedElemApi[U]](docUriOption: Option[URI], rootElem: U): IndexedScopedElem[U] =
    apply(docUriOption, rootElem, Path.Root)

  /**
   * Returns the same as `apply(None, rootElem, path)`.
   */
  def apply[U <: ScopedElemApi[U]](rootElem: U, path: Path): IndexedScopedElem[U] = {
    apply(None, rootElem, path)
  }

  /**
   * Expensive recursive factory method for "indexed elements".
   */
  def apply[U <: ScopedElemApi[U]](docUriOption: Option[URI], rootElem: U, path: Path): IndexedScopedElem[U] = {
    // Expensive call, so invoked only once
    val elem = rootElem.findElemOrSelfByPath(path).getOrElse(
      sys.error(s"Could not find the element with path $path from root ${rootElem.resolvedName}"))

    val parentBaseUriOption: Option[URI] =
      path.parentPathOption.flatMap(pp => IndexedClarkElemLike.computeBaseUriOption(docUriOption, rootElem, pp)).orElse(docUriOption)

    apply(docUriOption, parentBaseUriOption, rootElem, path, elem)
  }

  private def apply[U <: ScopedElemApi[U]](
    docUriOption: Option[URI],
    parentBaseUriOption: Option[URI],
    rootElem: U,
    path: Path,
    elem: U): IndexedScopedElem[U] = {

    val baseUriOption = IndexedClarkElemLike.getNextBaseUriOption(parentBaseUriOption, elem)

    // Recursive calls
    val childElems = elem.findAllChildElemsWithPathEntries map {
      case (e, entry) =>
        apply(docUriOption, baseUriOption, rootElem, path.append(entry), e)
    }

    new IndexedScopedElem(docUriOption, parentBaseUriOption, rootElem, childElems, path, elem)
  }
}
