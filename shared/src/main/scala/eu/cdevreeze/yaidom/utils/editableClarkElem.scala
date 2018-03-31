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

package eu.cdevreeze.yaidom.utils

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * Trait defining the contract for "editable" elements that can be functionally updated in the resolved name,
 * resolved attributes, or children.
 *
 * The implementation is very direct for resolved elements. For simple elements, the implementation is less
 * straightforward, because the scopes have to be edited as well. Strategies for using "fall-back" scopes can be
 * passed to the element editor (for simple elements).
 *
 * For simple editable elements, we must be able to prove the semantics of the offered operations in terms of the
 * same operations on the corresponding resolved elements. Moreover, we must be able to reason about the scopes
 * before and after performing an operation.
 *
 * @author Chris de Vreeze
 */
@deprecated(message = "For element creation, consider using 'ClarkNode.Elem' instead", since = "1.8.0")
sealed trait EditableClarkElem extends Any {

  type N

  type E <: N with ClarkElemApi.Aux[E]

  def toElem: E

  /**
   * Functionally updates the resolved name. For scoped elements, this sets the QName, and may affect the Scope.
   */
  def withResolvedName(newEName: EName): EditableClarkElem

  /**
   * Functionally updates the resolved attributes. For scoped elements, this sets the attribute QNames, and may affect the Scope.
   */
  def withResolvedAttributes(newResolvedAttributes: immutable.Iterable[(EName, String)]): EditableClarkElem

  /**
   * Functionally updates the children. For scoped elements, it is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def withChildren(newChildren: immutable.IndexedSeq[N]): EditableClarkElem

  /** Shorthand for `withChildren(newChildSeqs.flatten)` */
  def withChildSeqs(newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): EditableClarkElem

  /**
   * Functionally updates the resolved attributes by filtering. For scoped elements, this filters the (QName-keyed) attributes.
   */
  def filteringResolvedAttributes(p: (EName, String) => Boolean): EditableClarkElem

  /**
   * Functionally adds a resolved attribute. For scoped elements, this sets the attribute QName, and may affect the Scope.
   */
  def plusResolvedAttribute(attrEName: EName, attrValue: String): EditableClarkElem

  /**
   * Optionally functionally adds a resolved attribute. For scoped elements, this sets the attribute QName, if applicable, and may affect the Scope.
   */
  def plusResolvedAttributeOption(attrEName: EName, attrValueOption: Option[String]): EditableClarkElem

  /**
   * Functionally removes a resolved attribute.
   */
  def minusResolvedAttribute(attrEName: EName): EditableClarkElem

  /**
   * Functionally updates the children by filtering.
   */
  def filteringChildren(p: N => Boolean): EditableClarkElem

  /**
   * Functionally adds a child. For scoped elements, it is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChild(newChild: N): EditableClarkElem

  /**
   * Optionally functionally adds a child. For scoped elements, it is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChildOption(newChildOption: Option[N]): EditableClarkElem

  /**
   * Functionally inserts a child. For scoped elements, it is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChild(index: Int, newChild: N): EditableClarkElem

  /**
   * Optionally functionally inserts a child. For scoped elements, it is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChildOption(index: Int, newChildOption: Option[N]): EditableClarkElem

  /**
   * Functionally adds some children. For scoped elements, it is ensured by the implementation that no prefixed namespace undeclarations are
   * introduced. After all, they are illegal in XML 1.0.
   */
  def plusChildren(childSeq: immutable.IndexedSeq[N]): EditableClarkElem

  /**
   * Functionally removes a child.
   */
  def minusChild(index: Int): EditableClarkElem
}

/**
 * EditableClarkElem taking a resolved element.
 */
@deprecated(message = "For element creation, consider using 'ClarkNode.Elem' instead", since = "1.8.0")
final class EditableResolvedElem(val elem: resolved.Elem) extends AnyVal with EditableClarkElem {

  type N = resolved.Node

  type E = resolved.Elem

  def toElem: E = elem

  def withResolvedName(newEName: EName): EditableResolvedElem = {
    new EditableResolvedElem(elem.copy(resolvedName = newEName))
  }

  def withResolvedAttributes(newResolvedAttributes: immutable.Iterable[(EName, String)]): EditableResolvedElem = {
    new EditableResolvedElem(elem.copy(resolvedAttributes = newResolvedAttributes.toMap))
  }

  def withChildren(newChildren: immutable.IndexedSeq[N]): EditableResolvedElem = {
    new EditableResolvedElem(elem.copy(children = newChildren))
  }

  def withChildSeqs(newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): EditableResolvedElem = {
    withChildren(newChildSeqs.flatten)
  }

  def filteringResolvedAttributes(p: (EName, String) => Boolean): EditableResolvedElem = {
    withResolvedAttributes(elem.resolvedAttributes filter { case (en, v) => p(en, v) })
  }

  def plusResolvedAttribute(attrEName: EName, attrValue: String): EditableResolvedElem = {
    val newAttrs = elem.resolvedAttributes.toMap + (attrEName -> attrValue)
    withResolvedAttributes(newAttrs)
  }

  def plusResolvedAttributeOption(attrEName: EName, attrValueOption: Option[String]): EditableResolvedElem = {
    val newAttrs =
      if (attrValueOption.isEmpty) {
        elem.resolvedAttributes.toMap
      } else {
        elem.resolvedAttributes.toMap ++ Map(attrEName -> attrValueOption.get)
      }

    withResolvedAttributes(newAttrs)
  }

  def minusResolvedAttribute(attrEName: EName): EditableResolvedElem = {
    filteringResolvedAttributes { case (en, v) => attrEName != en }
  }

  def filteringChildren(p: N => Boolean): EditableResolvedElem = {
    withChildren(elem.children.filter(p))
  }

  def plusChild(newChild: N): EditableResolvedElem = {
    new EditableResolvedElem(elem.plusChild(newChild))
  }

  def plusChildOption(newChildOption: Option[N]): EditableResolvedElem = {
    new EditableResolvedElem(elem.plusChildOption(newChildOption))
  }

  def plusChild(index: Int, newChild: N): EditableResolvedElem = {
    new EditableResolvedElem(elem.plusChild(index, newChild))
  }

  def plusChildOption(index: Int, newChildOption: Option[N]): EditableResolvedElem = {
    new EditableResolvedElem(elem.plusChildOption(index, newChildOption))
  }

  def plusChildren(childSeq: immutable.IndexedSeq[N]): EditableResolvedElem = {
    new EditableResolvedElem(elem.plusChildren(childSeq))
  }

  def minusChild(index: Int): EditableResolvedElem = {
    new EditableResolvedElem(elem.minusChild(index))
  }
}

/**
 * EditableClarkElem taking a simple element, as well as a "fall-back" function mapping namespace URIs to prefixes
 * (the empty string as prefix for the default namespace).
 *
 * Preferably, this "fall-back" function is backed by an invertible Scope, without default namespace. After all,
 * introducing a default namespace could affect resolution of QNames in text content or attribute values. For other
 * namespace URIs that are not even in that fall-back scope, a prefix could be generated.
 *
 * The methods that functionally set or add children make sure that no prefixed namespace undeclarations are
 * introduced.
 */
@deprecated(message = "For element creation, consider using 'ClarkNode.Elem' instead", since = "1.8.0")
final class EditableSimpleElem(
    val elem: simple.Elem,
    val getFallbackPrefixForNamespace: String => String) extends EditableClarkElem {

  type N = simple.Node

  type E = simple.Elem

  def toElem: E = elem

  def withResolvedName(newEName: EName): EditableSimpleElem = {
    if (newEName.namespaceUriOption.isEmpty) {
      new EditableSimpleElem(elem.copy(qname = QName(None, newEName.localPart)), getFallbackPrefixForNamespace)
    } else {
      val nsUri = newEName.namespaceUriOption.get

      val newScope = elem.scope.includingNamespace(nsUri, () => getFallbackPrefixForNamespace(nsUri))

      val prefix = newScope.prefixForNamespace(nsUri, () => sys.error("Cannot happen!"))
      val prefixOption = if (prefix.isEmpty) None else Some(prefix)

      new EditableSimpleElem(elem.copy(qname = QName(prefixOption, newEName.localPart), scope = newScope), getFallbackPrefixForNamespace)
    }
  }

  def withResolvedAttributes(newResolvedAttributes: immutable.Iterable[(EName, String)]): EditableSimpleElem = {
    var currScope: Scope = elem.scope

    val newAttributes: immutable.Iterable[(QName, String)] =
      newResolvedAttributes map {
        case (attrEName, attrValue) =>
          if (attrEName.namespaceUriOption.isEmpty) {
            (QName(None, attrEName.localPart), attrValue)
          } else {
            val nsUri = attrEName.namespaceUriOption.get

            val newScope = currScope.includingNamespace(nsUri, () => getFallbackPrefixForNamespace(nsUri))

            val prefix = newScope.prefixForNamespace(nsUri, () => sys.error("Cannot happen!"))
            val prefixOption = if (prefix.isEmpty) None else Some(prefix)

            currScope = newScope
            (QName(prefixOption, attrEName.localPart), attrValue)
          }
      }

    new EditableSimpleElem(elem.copy(attributes = newAttributes.toVector, scope = currScope), getFallbackPrefixForNamespace)
  }

  def withChildren(newChildren: immutable.IndexedSeq[N]): EditableSimpleElem = {
    val editedNewChildren = newChildren map {
      case e: E => e.notUndeclaringPrefixes(elem.scope)
      case n: N => n
    }
    val newElem = elem.copy(children = editedNewChildren)
    new EditableSimpleElem(newElem, getFallbackPrefixForNamespace)
  }

  def withChildSeqs(newChildSeqs: immutable.IndexedSeq[immutable.IndexedSeq[N]]): EditableSimpleElem = {
    withChildren(newChildSeqs.flatten)
  }

  def filteringResolvedAttributes(p: (EName, String) => Boolean): EditableSimpleElem = {
    withResolvedAttributes(elem.resolvedAttributes filter { case (en, v) => p(en, v) })
  }

  def plusResolvedAttribute(attrEName: EName, attrValue: String): EditableSimpleElem = {
    val newAttrs = elem.resolvedAttributes.toMap + (attrEName -> attrValue)
    withResolvedAttributes(newAttrs)
  }

  def plusResolvedAttributeOption(attrEName: EName, attrValueOption: Option[String]): EditableSimpleElem = {
    val newAttrs =
      if (attrValueOption.isEmpty) {
        elem.resolvedAttributes.toMap
      } else {
        elem.resolvedAttributes.toMap ++ Map(attrEName -> attrValueOption.get)
      }

    withResolvedAttributes(newAttrs)
  }

  def minusResolvedAttribute(attrEName: EName): EditableSimpleElem = {
    filteringResolvedAttributes { case (en, v) => attrEName != en }
  }

  def filteringChildren(p: N => Boolean): EditableSimpleElem = {
    withChildren(elem.children.filter(p))
  }

  def plusChild(newChild: N): EditableSimpleElem = {
    val editedNewChild = newChild match {
      case e: E => e.notUndeclaringPrefixes(elem.scope)
      case n: N => n
    }
    val newElem = elem.plusChild(editedNewChild)
    new EditableSimpleElem(newElem, getFallbackPrefixForNamespace)
  }

  def plusChildOption(newChildOption: Option[N]): EditableSimpleElem = {
    val editedNewChildOption = newChildOption map {
      case e: E => e.notUndeclaringPrefixes(elem.scope)
      case n: N => n
    }
    val newElem = elem.plusChildOption(editedNewChildOption)
    new EditableSimpleElem(newElem, getFallbackPrefixForNamespace)
  }

  def plusChild(index: Int, newChild: N): EditableSimpleElem = {
    val editedNewChild = newChild match {
      case e: E => e.notUndeclaringPrefixes(elem.scope)
      case n: N => n
    }
    val newElem = elem.plusChild(index, editedNewChild)
    new EditableSimpleElem(newElem, getFallbackPrefixForNamespace)
  }

  def plusChildOption(index: Int, newChildOption: Option[N]): EditableSimpleElem = {
    val editedNewChildOption = newChildOption map {
      case e: E => e.notUndeclaringPrefixes(elem.scope)
      case n: N => n
    }
    val newElem = elem.plusChildOption(index, editedNewChildOption)
    new EditableSimpleElem(newElem, getFallbackPrefixForNamespace)
  }

  def plusChildren(childSeq: immutable.IndexedSeq[N]): EditableSimpleElem = {
    childSeq.foldLeft(this) {
      case (accElem, ch) =>
        accElem.plusChild(ch)
    }
  }

  def minusChild(index: Int): EditableSimpleElem = {
    new EditableSimpleElem(elem.minusChild(index), getFallbackPrefixForNamespace)
  }
}
