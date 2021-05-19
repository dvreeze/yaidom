/*
 * Copyright 2011-2021 Chris de Vreeze
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

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.core.AbsolutePath
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.PathConversions
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.BackingNodes
import eu.cdevreeze.yaidom.queryapi.HasParent
import eu.cdevreeze.yaidom.queryapi.IndexedScopedElemApi
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike
import eu.cdevreeze.yaidom.queryapi.XmlBaseSupport
import eu.cdevreeze.yaidom.simple

/**
 * "Indexed" nodes, where the element nodes are essentially simple elements with their ancestries.
 *
 * @author Chris de Vreeze
 */
object IndexedNode {

  /**
   * Node super-type of indexed elements.
   *
   * @author Chris de Vreeze
   */
  sealed trait Node extends BackingNodes.Node

  sealed trait CanBeDocumentChild extends Node with BackingNodes.CanBeDocumentChild

  /**
   * Indexed element, so a simple element with context, such as ancestors and document URI.
   *
   * The optional parent base URI is stored for very fast (optional) base URI computation. This is helpful in
   * an XBRL context, where URI resolution against a base URI is typically a very frequent operation.
   *
   * @author Chris de Vreeze
   */
  final class Elem private[IndexedNode] (
      val docUriOption: Option[URI],
      val parentBaseUriOption: Option[URI],
      val underlyingRootElem: simple.Elem,
      val path: Path,
      val underlyingElem: simple.Elem)
      extends IndexedScopedElemApi
      with CanBeDocumentChild
      with BackingNodes.Elem
      with ScopedElemLike
      with HasParent {

    type ThisElem = Elem

    type ThisNode = Node

    /**
     * Asserts internal consistency of the element. That is, asserts that the redundant fields are mutually consistent.
     * These assertions are not invoked during element construction, for performance reasons. Test code may invoke this
     * method. Users of the API do not need to worry about this method. (In fact, looking at the implementation of this
     * class, it can be reasoned that these assertions must hold.)
     */
    private[yaidom] def assertConsistency(): Unit = {
      assert(underlyingElem == underlyingRootElem.getElemOrSelfByPath(path), "Corrupt element!")
      assert(parentBaseUriOption == computeParentBaseUriOption(this), "Corrupt element!")
    }

    def thisElem: ThisElem = this

    def children: IndexedSeq[Node] = {
      val childElems = findAllChildElems
      var childElemIdx = 0

      val children =
        underlyingElem.children.flatMap {
          case che: simple.Elem =>
            val e = childElems(childElemIdx)
            childElemIdx += 1
            Some(e)
          case ch: simple.Text =>
            Some(Text(ch.text, false))
          case ch: simple.Comment =>
            Some(Comment(ch.text))
          case ch: simple.ProcessingInstruction =>
            Some(ProcessingInstruction(ch.target, ch.data))
          case ch: simple.EntityRef =>
            Some(EntityRef(ch.entity))
          case null =>
            None
        }

      assert(childElemIdx == childElems.size)

      children
    }

    def findAllChildElems: IndexedSeq[ThisElem] = {
      val baseUriOpt = baseUriOption

      underlyingElem.findAllChildElemsWithPathEntries.map {
        case (e, entry) =>
          new Elem(docUriOption, baseUriOpt, underlyingRootElem, path.append(entry), e)
      }
    }

    def qname: QName = underlyingElem.qname

    def attributes: IndexedSeq[(QName, String)] = underlyingElem.attributes.toIndexedSeq

    def scope: Scope = underlyingElem.scope

    /**
     * Returns the optional base URI. This method is fast, due to the use of the optional base URI of
     * the parent element, if any.
     */
    def baseUriOption: Option[URI] = {
      XmlBaseSupport.findBaseUriByParentBaseUri(parentBaseUriOption, underlyingElem)(XmlBaseSupport.JdkUriResolver)
    }

    /**
     * Returns the base URI, falling back to the empty URI if absent.
     */
    def baseUri: URI = baseUriOption.getOrElse(new URI(""))

    /**
     * Returns the document URI, falling back to the empty URI if absent.
     */
    def docUri: URI = docUriOption.getOrElse(new URI(""))

    def resolvedName: EName = underlyingElem.resolvedName

    def resolvedAttributes: collection.immutable.Iterable[(EName, String)] =
      underlyingElem.resolvedAttributes

    def text: String = underlyingElem.text

    def reverseAncestryOrSelfENames: IndexedSeq[EName] = {
      rootElem.resolvedName +: path.entries.map(_.elementName)
    }

    def reverseAncestryENames: IndexedSeq[EName] = {
      reverseAncestryOrSelfENames.dropRight(1)
    }

    def reverseAncestry: IndexedSeq[ThisElem] = {
      reverseAncestryOrSelf.init
    }

    override def equals(obj: Any): Boolean = obj match {
      case other: Elem =>
        (other.docUriOption == this.docUriOption) && (other.underlyingRootElem == this.underlyingRootElem) &&
          (other.path == this.path) && (other.underlyingElem == this.underlyingElem)
      case _ => false
    }

    override def hashCode: Int = (docUriOption, underlyingRootElem, path, underlyingElem).hashCode

    def rootElem: ThisElem = {
      new Elem(docUriOption, docUriOption, underlyingRootElem, Path.Empty, underlyingRootElem)
    }

    def reverseAncestryOrSelf: IndexedSeq[ThisElem] = {
      val resultOption = rootElem.findReverseAncestryOrSelfByPath(path)

      assert(resultOption.isDefined, s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
      assert(
        resultOption.get.nonEmpty,
        s"Corrupt data! The reverse ancestry-or-self (of $resolvedName) cannot be empty")
      assert(resultOption.get.last == thisElem)

      resultOption.get
    }

    def absolutePath: AbsolutePath = {
      PathConversions.convertPathToAbsolutePath(path, underlyingRootElem.resolvedName)
    }

    def parentOption: Option[ThisElem] = {
      path.parentPathOption.map(pp => Elem(this.docUriOption, this.underlyingRootElem, pp))
    }

    def namespaces: Declarations = {
      val parentScope = this.path.parentPathOption
        .map { path =>
          rootElem.getElemOrSelfByPath(path).scope
        }
        .getOrElse(Scope.Empty)
      parentScope.relativize(this.scope)
    }
  }

  final case class Text(text: String, isCData: Boolean) extends Node with BackingNodes.Text {
    require(text ne null) // scalastyle:off null
    if (isCData) require(!text.containsSlice("]]>"))

    /** Returns `text.trim`. */
    def trimmedText: String = text.trim

    /** Returns `XmlStringUtils.normalizeString(text)` .*/
    def normalizedText: String = XmlStringUtils.normalizeString(text)
  }

  final case class ProcessingInstruction(target: String, data: String)
      extends CanBeDocumentChild
      with BackingNodes.ProcessingInstruction {
    require(target ne null) // scalastyle:off null
    require(data ne null) // scalastyle:off null
  }

  /**
   * An entity reference. For example:
   * {{{
   * &hello;
   * }}}
   * We obtain this entity reference as follows:
   * {{{
   * EntityRef("hello")
   * }}}
   */
  final case class EntityRef(entity: String) extends Node with BackingNodes.EntityRef {
    require(entity ne null) // scalastyle:off null
  }

  final case class Comment(text: String) extends CanBeDocumentChild with BackingNodes.Comment {
    require(text ne null) // scalastyle:off null
  }

  private[yaidom] def computeParentBaseUriOption(elm: Elem): Option[URI] = {
    if (elm.path.isEmpty) {
      elm.docUriOption
    } else {
      XmlBaseSupport.findBaseUriByDocUriAndPath(elm.docUriOption, elm.underlyingRootElem, elm.path.parentPath)(
        XmlBaseSupport.JdkUriResolver)
    }
  }

  object Elem {

    def apply(docUriOption: Option[URI], underlyingRootElem: simple.Elem, path: Path): Elem = {
      val underlyingElem = underlyingRootElem.getElemOrSelfByPath(path)

      val parentBaseUriOption = path.parentPathOption
        .map { pp =>
          XmlBaseSupport.findBaseUriByDocUriAndPath(docUriOption, underlyingRootElem, pp)(XmlBaseSupport.JdkUriResolver)
        }
        .getOrElse(docUriOption)

      new Elem(docUriOption, parentBaseUriOption, underlyingRootElem, path, underlyingElem)
    }

    def apply(docUri: URI, underlyingRootElem: simple.Elem, path: Path): Elem = {
      apply(Some(docUri), underlyingRootElem, path)
    }

    def apply(underlyingRootElem: simple.Elem, path: Path): Elem = {
      apply(None, underlyingRootElem, path)
    }

    def apply(docUriOption: Option[URI], underlyingRootElem: simple.Elem): Elem = {
      apply(docUriOption, underlyingRootElem, Path.Empty)
    }

    def apply(docUri: URI, underlyingRootElem: simple.Elem): Elem = {
      apply(Some(docUri), underlyingRootElem, Path.Empty)
    }

    def apply(underlyingRootElem: simple.Elem): Elem = {
      apply(None, underlyingRootElem, Path.Empty)
    }
  }
}
