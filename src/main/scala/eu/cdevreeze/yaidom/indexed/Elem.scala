/*
 * Copyright 2011-2017 Chris de Vreeze
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

import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.queryapi
import eu.cdevreeze.yaidom.simple

/**
 * Factory object for `Elem` instances, where `Elem` is a type alias for `IndexedScopedElem[simple.Elem]`.
 * This object also contains an `ElemTransformationApi` implementation for these elements.
 *
 * @author Chris de Vreeze
 */
object Elem {

  def apply(underlyingRootElem: simple.Elem): Elem = {
    IndexedScopedNode.Elem(underlyingRootElem)
  }

  def apply(docUriOption: Option[URI], underlyingRootElem: simple.Elem): Elem = {
    IndexedScopedNode.Elem(docUriOption, underlyingRootElem)
  }

  def apply(docUri: URI, underlyingRootElem: simple.Elem): Elem = {
    IndexedScopedNode.Elem(docUri, underlyingRootElem)
  }

  def apply(underlyingRootElem: simple.Elem, path: Path): Elem = {
    IndexedScopedNode.Elem(underlyingRootElem, path)
  }

  def apply(docUriOption: Option[URI], underlyingRootElem: simple.Elem, path: Path): Elem = {
    IndexedScopedNode.Elem(docUriOption, underlyingRootElem, path)
  }

  def apply(docUri: URI, underlyingRootElem: simple.Elem, path: Path): Elem = {
    IndexedScopedNode.Elem(docUri, underlyingRootElem, path)
  }

  object ElemTransformations extends queryapi.ElemTransformationLike {

    // The challenge below is in dealing with Paths that are volatile, and in calling function f at the right time with the right arguments.

    type Node = IndexedScopedNode.Node

    type Elem = IndexedScopedNode.Elem[simple.Elem]

    def transformChildElems(elem: Elem, f: Elem => Elem): Elem = {
      val oldPathToElemMap: Map[Path, Elem] =
        elem.findAllChildElems.map(e => (e.path -> e)).toMap.ensuring(!_.contains(Path.Empty))

      // Updating the underlying root element (ignoring the root element)

      val newUnderlyingRootElem: simple.Elem =
        elem.underlyingRootElem.updateElems(oldPathToElemMap.keySet) { (elm, path) =>
          assert(oldPathToElemMap.contains(path))

          // Apply the function, and return the underlying element, thus losing "ancestry data" resulting from the function application.
          f(oldPathToElemMap(path)).underlyingElem
        }

      val newRootElem =
        apply(elem.rootElem.docUri, newUnderlyingRootElem, elem.rootElem.path.ensuring(_.isEmpty))

      // The transformations were only for child elements of elem, so its Path must still be valid for the result element.
      newRootElem.findElemOrSelfByPath(elem.path).ensuring(_.isDefined).get
    }

    def transformChildElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Elem = {
      val oldPathToElemMap: Map[Path, Elem] =
        elem.findAllChildElems.map(e => (e.path -> e)).toMap.ensuring(!_.contains(Path.Empty))

      // Updating the underlying root element (ignoring the root element)

      val newUnderlyingRootElem: simple.Elem =
        elem.underlyingRootElem.updateElemsWithNodeSeq(oldPathToElemMap.keySet) { (elm, path) =>
          assert(oldPathToElemMap.contains(path))

          // Apply the function, and return the underlying nodes, thus losing "ancestry data" resulting from the function application.
          f(oldPathToElemMap(path)).map(n => getUnderlyingNode(n))
        }

      val newRootElem =
        apply(elem.rootElem.docUri, newUnderlyingRootElem, elem.rootElem.path.ensuring(_.isEmpty))

      // The transformations were only for child elements of elem, so its Path must still be valid for the result element.
      newRootElem.findElemOrSelfByPath(elem.path).ensuring(_.isDefined).get
    }

    private def getUnderlyingNode(node: Node): simple.Node = {
      node match {
        case e: IndexedScopedNode.Elem[_] =>
          e.asInstanceOf[IndexedScopedNode.Elem[simple.Elem]].underlyingElem
        case t: IndexedScopedNode.Text =>
          simple.Text(t.text, false)
        case c: IndexedScopedNode.Comment =>
          simple.Comment(c.text)
        case pi: IndexedScopedNode.ProcessingInstruction =>
          simple.ProcessingInstruction(pi.target, pi.data)
        case er: IndexedScopedNode.EntityRef =>
          simple.EntityRef(er.entity)
      }
    }
  }
}
