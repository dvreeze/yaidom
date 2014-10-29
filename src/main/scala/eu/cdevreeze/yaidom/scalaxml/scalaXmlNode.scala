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

package eu.cdevreeze.yaidom.scalaxml

import scala.collection.immutable

import eu.cdevreeze.yaidom.XmlStringUtils
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions
import eu.cdevreeze.yaidom.convert.ScalaXmlConversions.toQName
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.IsNavigable
import eu.cdevreeze.yaidom.queryapi.ScopedElemLike

/**
 * Wrappers around `scala.xml.Node` and subclasses, such that the wrapper around `scala.xml.Elem` conforms to the
 * [[eu.cdevreeze.yaidom.queryapi.ElemApi]] API.
 *
 * Use these wrappers only if there is a specific need for them, because these wrappers do have their costs (such as the creation
 * of lots of query result objects, the repeated costs of querying element/attribute names, the conceptual differences between
 * Scala XML and native yaidom nodes, etc.).
 *
 * For some namespace-related pitfalls, see [[eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem]].
 *
 * The wrappers are very light-weight, and typically very short-lived. On the other hand, each query may create many wrapper
 * instances for the query results. By design, the only state of each wrapper instance is the wrapped Scala XML node.
 *
 * @author Chris de Vreeze
 */
sealed trait ScalaXmlNode {

  type DomType <: scala.xml.Node

  def wrappedNode: DomType

  final override def toString: String = wrappedNode.toString
}

/**
 * Wrapper around `scala.xml.Elem`, conforming to the [[eu.cdevreeze.yaidom.queryapi.ElemApi]] API.
 *
 * '''See the documentation of the mixed-in query API trait(s) for more details on the uniform query API offered by this class.'''
 *
 * Keep in mind that the `HasENameApi` specific part of the API is a '''broken abstraction'''. If the wrapped Scala XML element
 * misses some namespace declarations for used element or attribute names, these element and/or attribute names
 * cannot be resolved, and exceptions are thrown when querying for them! The `ElemApi` part of the API does not
 * suffer from this broken abstraction, so is less dangerous to use.
 *
 * The wrapper instances are very light-weight, and typically very short-lived. On the other hand, each query may create many wrapper
 * instances for the query results. By design, the only state of each wrapper instance is the wrapped Scala XML Elem.
 */
final class ScalaXmlElem(
  override val wrappedNode: scala.xml.Elem) extends ScalaXmlNode with ScopedElemLike[ScalaXmlElem] with IsNavigable[ScalaXmlElem] { self =>

  require(wrappedNode ne null)

  override type DomType = scala.xml.Elem

  override def findAllChildElems: immutable.IndexedSeq[ScalaXmlElem] = children collect { case e: ScalaXmlElem => e }

  /**
   * Returns the resolved name of the element. Note that there is no guarantee that the element name can be resolved!
   */
  override def resolvedName: EName = {
    val qname = ScalaXmlConversions.toQName(wrappedNode)
    scope.resolveQNameOption(qname).getOrElse(
      sys.error(s"Could not resolve QName from prefix ${Option(wrappedNode.prefix).getOrElse("")} and label ${wrappedNode.label}"))
  }

  /**
   * Returns the "resolved attributes". Note that there is no guarantee that the attributes names can be resolved!
   */
  override def resolvedAttributes: immutable.IndexedSeq[(EName, String)] = {
    val attrScope = scope.withoutDefaultNamespace
    attributes map {
      case (attrName, attrValue) =>
        val ename = attrScope.resolveQNameOption(attrName).getOrElse(
          sys.error(s"Could not resolve attribute name $attrName"))
        (ename, attrValue)
    }
  }

  def children: immutable.IndexedSeq[ScalaXmlNode] = {
    wrappedNode.child.toIndexedSeq flatMap { n: scala.xml.Node => ScalaXmlNode.wrapNodeOption(n) }
  }

  override def qname: QName = ScalaXmlConversions.toQName(wrappedNode)

  override def attributes: immutable.IndexedSeq[(QName, String)] = ScalaXmlConversions.extractAttributes(wrappedNode.attributes)

  /**
   * Returns the scope of the element. Note that there is no guarantee that this scope is complete!
   */
  override def scope: Scope = ScalaXmlConversions.extractScope(wrappedNode.scope)

  override def findChildElemByPathEntry(entry: Path.Entry): Option[ScalaXmlElem] = {
    import ScalaXmlConversions._

    // Scope only computed once, for acceptable performance
    val sc = scope

    val filteredChildrenWithIndex = wrappedNode.child.toStream.zipWithIndex filter {
      case (e: scala.xml.Elem, idx) if sc.resolveQNameOption(toQName(e)).getOrElse(sys.error(s"Corrupt! Unresolved ${toQName(e)}")) == entry.elementName => true
      case _ => false
    }

    val idxOption = filteredChildrenWithIndex.drop(entry.index).headOption.map(_._2)
    val result = idxOption.map(idx => ScalaXmlElem(wrappedNode.child(idx).asInstanceOf[scala.xml.Elem]))
    assert(result.forall(_.resolvedName == entry.elementName))
    result
  }

  /** Returns the text children */
  def textChildren: immutable.IndexedSeq[ScalaXmlText] = children collect { case t: ScalaXmlText => t }

  /** Returns the comment children */
  def commentChildren: immutable.IndexedSeq[ScalaXmlComment] = children collect { case c: ScalaXmlComment => c }

  /**
   * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  override def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }
}

final class ScalaXmlText(override val wrappedNode: scala.xml.Text) extends ScalaXmlNode {
  require(wrappedNode ne null)

  override type DomType = scala.xml.Text

  def text: String = wrappedNode.text

  def trimmedText: String = text.trim

  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

final class ScalaXmlCData(override val wrappedNode: scala.xml.PCData) extends ScalaXmlNode {
  require(wrappedNode ne null)

  override type DomType = scala.xml.PCData

  def text: String = wrappedNode.text

  def trimmedText: String = text.trim

  def normalizedText: String = XmlStringUtils.normalizeString(text)
}

/**
 * Wrapper around a Scala XML Atom that is not Text or PCData
 */
final class ScalaXmlAtom(override val wrappedNode: scala.xml.Atom[_]) extends ScalaXmlNode {
  require(wrappedNode ne null)

  override type DomType = scala.xml.Atom[_]

  def text: String = wrappedNode.data.toString
}

final class ScalaXmlProcessingInstruction(override val wrappedNode: scala.xml.ProcInstr) extends ScalaXmlNode {
  require(wrappedNode ne null)

  override type DomType = scala.xml.ProcInstr
}

final class ScalaXmlEntityRef(override val wrappedNode: scala.xml.EntityRef) extends ScalaXmlNode {
  require(wrappedNode ne null)

  override type DomType = scala.xml.EntityRef
}

final class ScalaXmlComment(override val wrappedNode: scala.xml.Comment) extends ScalaXmlNode {
  require(wrappedNode ne null)

  override type DomType = scala.xml.Comment

  def text: String = wrappedNode.commentText
}

object ScalaXmlNode {

  def wrapNodeOption(node: scala.xml.Node): Option[ScalaXmlNode] = {
    node match {
      case e: scala.xml.Elem => Some(new ScalaXmlElem(e))
      case cdata: scala.xml.PCData => Some(new ScalaXmlCData(cdata))
      case t: scala.xml.Text => Some(new ScalaXmlText(t))
      case at: scala.xml.Atom[_] =>
        // Possibly an evaluated "parameter" in an XML literal
        Some(new ScalaXmlAtom(at))
      case pi: scala.xml.ProcInstr => Some(new ScalaXmlProcessingInstruction(pi))
      case er: scala.xml.EntityRef => Some(new ScalaXmlEntityRef(er))
      case c: scala.xml.Comment => Some(new ScalaXmlComment(c))
      case _ => None
    }
  }

  def wrapElement(elm: scala.xml.Elem): ScalaXmlElem = new ScalaXmlElem(elm)
}

object ScalaXmlElem {

  def apply(wrappedNode: scala.xml.Elem): ScalaXmlElem = new ScalaXmlElem(wrappedNode)
}

object ScalaXmlText {

  def apply(wrappedNode: scala.xml.Text): ScalaXmlText = new ScalaXmlText(wrappedNode)
}

object ScalaXmlCData {

  def apply(wrappedNode: scala.xml.PCData): ScalaXmlCData = new ScalaXmlCData(wrappedNode)
}

object ScalaXmlAtom {

  def apply(wrappedNode: scala.xml.Atom[_]): ScalaXmlAtom = new ScalaXmlAtom(wrappedNode)
}

object ScalaXmlProcessingInstruction {

  def apply(wrappedNode: scala.xml.ProcInstr): ScalaXmlProcessingInstruction =
    new ScalaXmlProcessingInstruction(wrappedNode)
}

object ScalaXmlEntityRef {

  def apply(wrappedNode: scala.xml.EntityRef): ScalaXmlEntityRef = new ScalaXmlEntityRef(wrappedNode)
}

object ScalaXmlComment {

  def apply(wrappedNode: scala.xml.Comment): ScalaXmlComment = new ScalaXmlComment(wrappedNode)
}
