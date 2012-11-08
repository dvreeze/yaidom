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
package editable

import scala.collection.immutable

/**
 * Immutable node, keeping the order of attributes (including namespace declarations), and therefore better suited for
 * roundtripping. Still, roundtripping is not totally lossless, because the short or long form of empty elements is not
 * stored.
 *
 * Better roundtripping than for normal yaidom elements does have a price. For one, memory usage is larger. Furthermore,
 * "editable" elements "compose" worse than normal yaidom elements, because "editable" elements know their namespace
 * declarations as well as their scopes, and therefore are more difficult to nest w.r.t. namespaces.
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable

final class Elem(
  val qname: QName,
  val pseudoAttributes: immutable.IndexedSeq[(String, String)],
  val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends Node with UpdatableElemLike[Node, Elem] with HasText { self =>

  require(qname ne null)
  require(pseudoAttributes ne null)
  require(scope ne null)
  require(children ne null)

  val attributes: Map[QName, String] = {
    val nonNsPseudoAttributes = pseudoAttributes filter { case (attrName, attrValue) => !isNamespacePseudoAttribute(attrName) }
    nonNsPseudoAttributes.toMap map { case (attrName, attrValue) => QName(attrName) -> attrValue }
  }

  val declarations: Declarations = {
    val nsPseudoAttributes = pseudoAttributes filter { case (attrName, attrValue) => isNamespacePseudoAttribute(attrName) }
    val prefixMap = nsPseudoAttributes.toMap map {
      case (attrName, attrValue) =>
        val arr = attrName.split(':')
        assert(arr.length >= 1 && arr.length <= 2)
        require(arr(0) == "xmlns")
        val prefixOption: Option[String] = if (arr.length == 1) None else Some(arr(1))
        (prefixOption.getOrElse(""), attrValue)
    }
    Declarations(prefixMap)
  }

  require(scope.resolve(declarations) == scope, "Expected the scope to contain the declarations for element %s".format(qname))

  /** The attribute `Scope`, which is the same `Scope` but without the default namespace (which is not used for attributes) */
  val attributeScope: Scope = Scope(scope.map - "")

  /** The `Elem` name as `EName`, obtained by resolving the element `QName` against the `Scope` */
  override val resolvedName: EName =
    scope.resolveQName(qname).getOrElse(sys.error("Element name '%s' should resolve to an EName in scope [%s]".format(qname, scope)))

  /** The attributes as a `Map` from `EName`s (instead of `QName`s) to values, obtained by resolving attribute `QName`s against the attribute scope */
  override val resolvedAttributes: Map[EName, String] = {
    attributes map { kv =>
      val attName = kv._1
      val attValue = kv._2
      val expandedName = attributeScope.resolveQName(attName).getOrElse(sys.error("Attribute name '%s' should resolve to an EName in scope [%s]".format(attName, attributeScope)))
      (expandedName -> attValue)
    }
  }

  /** Returns the element children */
  override def allChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  // TODO Is the next operation safe and sufficiently easy to use w.r.t. namespaces? Should it fix namespace scopes?

  /** Creates a copy, but with (only) the children passed as parameter `newChildren` */
  override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(qname, pseudoAttributes, scope, newChildren)
  }

  override def childNodeIndex(childPathEntry: ElemPath.Entry): Int = {
    (0 to childPathEntry.index).foldLeft(-1) {
      case (acc, nextPathEntryIndex) =>
        children.indexWhere({
          case e: Elem if e.resolvedName == childPathEntry.elementName => true
          case n: Node => false
        }, acc + 1)
    }
  }

  override def findChildPathEntry(idx: Int): Option[ElemPath.Entry] = {
    val node = children(idx)

    node match {
      case e: Elem =>
        val cnt = children.take(idx) count {
          case che: Elem if che.resolvedName == e.resolvedName => true
          case chn: Node => false
        }
        Some(ElemPath.Entry(e.resolvedName, cnt))
      case n: Node => None
    }
  }

  /** Returns `withChildren(self.children :+ newChild)`. */
  def plusChild(newChild: Node): Elem = withChildren(self.children :+ newChild)

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[Text] = children collect { case t: Text => t }

  /**
   * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  private def isNamespacePseudoAttribute(s: String): Boolean = (s == "xmlns") || (s.startsWith("xmlns:"))
}

final case class Text(text: String, isCData: Boolean) extends Node {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

  /** Returns `text.trim`. */
  def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)` .*/
  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

final case class ProcessingInstruction(target: String, data: String) extends Node {
  require(target ne null)
  require(data ne null)
}

final case class EntityRef(entity: String) extends Node {
  require(entity ne null)
}

final case class Comment(text: String) extends Node {
  require(text ne null)
}
