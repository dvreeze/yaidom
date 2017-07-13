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
 * This object also contains `ElemTransformationApi` and `ElemUpdateApi` implementations for these elements.
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

  /**
   * Returns the given simple element as indexed element, ignoring
   * the Path. In other words, returns `apply(docUriOption, elem)`.
   */
  def ignoringPath(elem: simple.Elem, docUriOption: Option[URI]): Elem = {
    apply(docUriOption, elem)
  }

  /**
   * Returns the given simple element as indexed element, ignoring
   * the ancestry (Path) and document URI. In other words, returns
   * `ignoringPath(elem, None)`.
   */
  def ignoringAncestry(elem: simple.Elem): Elem = {
    ignoringPath(elem, None)
  }

  /**
   * Returns the given simple node as indexed node, ignoring the Path.
   */
  def ignoringPath(node: simple.Node, docUriOption: Option[URI]): IndexedScopedNode.Node = {
    node match {
      case e: simple.Elem =>
        Elem(docUriOption, e)
      case simple.Text(text, isCData) =>
        IndexedScopedNode.Text(text, isCData)
      case simple.Comment(text) =>
        IndexedScopedNode.Comment(text)
      case simple.ProcessingInstruction(target, data) =>
        IndexedScopedNode.ProcessingInstruction(target, data)
      case simple.EntityRef(entity) =>
        IndexedScopedNode.EntityRef(entity)
    }
  }

  /**
   * Returns the given simple node as indexed node, ignoring the ancestry (Path) and document URI.
   */
  def ignoringAncestry(node: simple.Node): IndexedScopedNode.Node = {
    ignoringPath(node, None)
  }

  /**
   * Safe ElemTransformationApi implementation. It is safe in that it decorates an UnsafeElemTransformations
   * object with the additional behavior of changing the element transformation functions into safe ones that
   * make sure that the optional document URI and the ancestry remain the same after the transformation.
   */
  object ElemTransformations extends queryapi.ElemTransformationApi {

    type Node = IndexedScopedNode.Node

    type Elem = IndexedScopedNode.Elem[simple.Elem]

    def transformChildElems(elem: Elem, f: Elem => Elem): Elem = {
      // No fixing needed, due to knowledge about the implementation
      UnsafeElemTransformations.transformChildElems(elem, f)
    }

    def transformChildElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Elem = {
      // No fixing needed, due to knowledge about the implementation
      UnsafeElemTransformations.transformChildElemsToNodeSeq(elem, f)
    }

    def transformElemsOrSelf(elem: Elem, f: Elem => Elem): Elem = {
      UnsafeElemTransformations.transformElemsOrSelf(elem, fixElemTransformation(f))
    }

    def transformElems(elem: Elem, f: Elem => Elem): Elem = {
      UnsafeElemTransformations.transformElems(elem, fixElemTransformation(f))
    }

    def transformElemsOrSelfToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): immutable.IndexedSeq[Node] = {
      UnsafeElemTransformations.transformElemsOrSelfToNodeSeq(elem, fixElemToNodeSeqTransformation(f))
    }

    def transformElemsToNodeSeq(elem: Elem, f: Elem => immutable.IndexedSeq[Node]): Elem = {
      UnsafeElemTransformations.transformElemsToNodeSeq(elem, fixElemToNodeSeqTransformation(f))
    }

    private def fixElemTransformation(f: Elem => Elem): (Elem => Elem) = {
      { (elm: Elem) =>
        val ownChildNodeIndex =
          elm.parentOption.flatMap(pe => elm.path.lastEntryOption.map(entry => pe.underlyingElem.childNodeIndex(entry))).getOrElse(-1)

        assert(elm.parentOption.isEmpty || ownChildNodeIndex >= 0)

        val newUnderlyingElem = f(elm).underlyingElem

        val newUnderlyingRootElem: simple.Elem =
          elm.underlyingRootElem.updateElemOrSelf(elm.path, newUnderlyingElem)

        val newRootElem =
          apply(elm.rootElem.docUriOption, newUnderlyingRootElem, elm.rootElem.path.ensuring(_.isEmpty))

        // Parent Path stable before/after transformation
        val parentPathOption = elm.path.parentPathOption

        val newParentElemOption =
          parentPathOption.map(ppath => newRootElem.findElemOrSelfByPath(ppath).ensuring(_.isDefined).get)

        // Dependence on ElemUpdates!
        newParentElemOption.map(e => ElemUpdates.children(e).apply(ownChildNodeIndex).asInstanceOf[Elem]).getOrElse(newRootElem).
          ensuring(_.path.parentPathOption == elm.path.parentPathOption)
      }
    }

    private def fixElemToNodeSeqTransformation(f: Elem => immutable.IndexedSeq[Node]): (Elem => immutable.IndexedSeq[Node]) = {
      { (elm: Elem) =>
        val ownChildNodeIndex =
          elm.parentOption.flatMap(pe => elm.path.lastEntryOption.map(entry => pe.underlyingElem.childNodeIndex(entry))).getOrElse(-1)

        assert(elm.parentOption.isEmpty || ownChildNodeIndex >= 0)

        val newUnderlyingNodeSeq = f(elm).map(n => getUnderlyingNode(n))

        val newUnderlyingRootNodeSeq: immutable.IndexedSeq[simple.Node] =
          elm.underlyingRootElem.updateElemsOrSelfWithNodeSeq(Set(elm.path)) { case (e, path) => newUnderlyingNodeSeq }

        val newRootNodeSeq = newUnderlyingRootNodeSeq.map(n => ignoringPath(n, elm.rootElem.docUriOption))

        // Parent Path stable before/after transformation
        val parentPathOption = elm.path.parentPathOption

        assert(parentPathOption.isEmpty || (newRootNodeSeq.size == 1 && newRootNodeSeq.head.isInstanceOf[IndexedScopedNode.Elem[_]]))

        val newParentElemOption =
          parentPathOption.map(ppath => newRootNodeSeq.head.asInstanceOf[Elem].findElemOrSelfByPath(ppath).ensuring(_.isDefined).get)

        // Dependence on ElemUpdates!
        newParentElemOption.map(e => ElemUpdates.children(e).slice(ownChildNodeIndex, ownChildNodeIndex + newUnderlyingNodeSeq.size)).
          getOrElse(newRootNodeSeq)
      }
    }
  }

  /**
   * Unsafe ElemTransformationApi implementation. It is unsafe in that the passed element transformation functions
   * are called as-is, even if they lead to corrupt transformation results.
   */
  object UnsafeElemTransformations extends queryapi.ElemTransformationLike {

    // The challenge below is in dealing with Paths that are volatile, and in calling function f at the right time with the right arguments.
    // In particular, ancestor elements cannot trust Paths of descendant elements after updates.

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
        apply(elem.rootElem.docUriOption, newUnderlyingRootElem, elem.rootElem.path.ensuring(_.isEmpty))

      // The transformations were only for child elements of elem, so its Path must still be valid for the result element.
      newRootElem.findElemOrSelfByPath(elem.path).ensuring(_.isDefined).get.ensuring(_.path == elem.path)
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
        apply(elem.rootElem.docUriOption, newUnderlyingRootElem, elem.rootElem.path.ensuring(_.isEmpty))

      // The transformations were only for child elements of elem, so its Path must still be valid for the result element.
      newRootElem.findElemOrSelfByPath(elem.path).ensuring(_.isDefined).get.ensuring(_.path == elem.path)
    }
  }

  object ElemUpdates extends queryapi.ElemUpdateLike {

    // The challenge below is in dealing with Paths that are volatile, and in calling function f at the right time with the right arguments.
    // In particular, ancestor elements cannot trust Paths of descendant elements after updates.

    type Node = IndexedScopedNode.Node

    type Elem = IndexedScopedNode.Elem[simple.Elem]

    def children(elem: Elem): immutable.IndexedSeq[Node] = {
      var childElems = elem.findAllChildElems.toList

      val resultNodes: immutable.IndexedSeq[Node] =
        elem.underlyingElem.children map {
          case e: simple.Elem =>
            val hd :: tail = childElems
            childElems = tail
            assert(hd.resolvedName == e.resolvedName)
            hd
          case simple.Text(text, isCData) =>
            IndexedScopedNode.Text(text, isCData)
          case simple.Comment(comment) =>
            IndexedScopedNode.Comment(comment)
          case simple.ProcessingInstruction(target, data) =>
            IndexedScopedNode.ProcessingInstruction(target, data)
          case simple.EntityRef(entity) =>
            IndexedScopedNode.EntityRef(entity)
        }

      assert(childElems.isEmpty)
      resultNodes
    }

    def withChildren(elem: Elem, newChildren: immutable.IndexedSeq[Node]): Elem = {
      // Updating the underlying root element

      val newUnderlyingRootElem: simple.Elem =
        elem.underlyingRootElem.updateElemOrSelf(elem.path) { e =>
          e.withChildren(newChildren.map(ch => getUnderlyingNode(ch)))
        }

      val newRootElem =
        apply(elem.rootElem.docUriOption, newUnderlyingRootElem, elem.rootElem.path.ensuring(_.isEmpty))

      // The updates were only for child nodes of elem, so its Path must still be valid for the result element.
      newRootElem.findElemOrSelfByPath(elem.path).ensuring(_.isDefined).get.ensuring(_.path == elem.path)
    }

    def collectChildNodeIndexes(elem: Elem, pathEntries: Set[Path.Entry]): Map[Path.Entry, Int] = {
      elem.underlyingElem.collectChildNodeIndexes(pathEntries)
    }

    def findAllChildElemsWithPathEntries(elem: Elem): immutable.IndexedSeq[(Elem, Path.Entry)] = {
      elem.findAllChildElems.map(e => (e, e.path.lastEntry))
    }
  }

  private def getUnderlyingNode(node: IndexedScopedNode.Node): simple.Node = {
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
