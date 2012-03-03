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
package xlink

import java.net.URI
import scala.collection.immutable
import XLink._

/**
 * Node at the level of XLink awareness. An `Elem` at this level is either an `XLink` or not. A `Node` wraps a yaidom [[eu.cdevreeze.yaidom.Node]]
 * (without the children, if applicable).
 *
 * When using the traits and classes in this package, prefix them with the last part of the package name. So,
 * write `xlink.Elem` instead of globally importing classes/traits in the [[eu.cdevreeze.yaidom.xlink]] package.
 * This is analogous to the good practice of writing for example `immutable.IndexedSeq[T]` and
 * `mutable.IndexedSeq[T]` for Scala Collections.
 *
 * @author Chris de Vreeze
 */
sealed trait Node extends Immutable {

  type NormalNode <: eu.cdevreeze.yaidom.Node

  def toNormalNode: NormalNode
}

trait ParentNode extends Node {

  def children: immutable.IndexedSeq[Node]
}

final class Document(
  val baseUriOption: Option[URI],
  val documentElement: Elem,
  val processingInstructions: immutable.IndexedSeq[ProcessingInstruction],
  val comments: immutable.IndexedSeq[Comment]) extends ParentNode {

  require(baseUriOption ne null)
  require(documentElement ne null)
  require(processingInstructions ne null)
  require(comments ne null)

  type NormalNode = eu.cdevreeze.yaidom.Document

  override def children: immutable.IndexedSeq[Node] =
    processingInstructions ++ comments ++ immutable.IndexedSeq[Node](documentElement)

  def toNormalNode: eu.cdevreeze.yaidom.Document =
    new eu.cdevreeze.yaidom.Document(
      baseUriOption,
      documentElement.toNormalNode,
      processingInstructions map { pi => pi.toNormalNode },
      comments map { c => c.toNormalNode })
}

class Elem(
  val qname: QName,
  val attributes: Map[QName, String],
  val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends ParentNode with NodeAwareElemLike[Node, Elem] with TextAwareElemLike[Text] { self =>

  require(qname ne null)
  require(attributes ne null)
  require(scope ne null)
  require(children ne null)

  type NormalNode = eu.cdevreeze.yaidom.Elem

  final val wrappedElemWithoutChildren: eu.cdevreeze.yaidom.Elem =
    eu.cdevreeze.yaidom.Elem(qname, attributes, scope, immutable.IndexedSeq())

  final override def resolvedName: ExpandedName = wrappedElemWithoutChildren.resolvedName

  final override def resolvedAttributes: Map[ExpandedName, String] = wrappedElemWithoutChildren.resolvedAttributes

  final override def allChildElems: immutable.IndexedSeq[Elem] = children collect { case e: Elem => e }

  final override def textChildren: immutable.IndexedSeq[Text] = children collect { case t: Text => t }

  final override def withChildren(newChildren: immutable.IndexedSeq[Node]): Elem = {
    new Elem(qname, attributes, scope, newChildren)
  }

  final override def toNormalNode: eu.cdevreeze.yaidom.Elem =
    eu.cdevreeze.yaidom.Elem(
      qname,
      attributes,
      scope,
      children map { ch => ch.toNormalNode })
}

final case class Text(text: String, isCData: Boolean) extends Node with TextLike {
  require(text ne null)
  if (isCData) require(!text.containsSlice("]]>"))

  type NormalNode = eu.cdevreeze.yaidom.Text

  override def toNormalNode: eu.cdevreeze.yaidom.Text = eu.cdevreeze.yaidom.Text(text, isCData)

  override def toString: String = toNormalNode.toString
}

final case class ProcessingInstruction(target: String, data: String) extends Node {
  require(target ne null)
  require(data ne null)

  type NormalNode = eu.cdevreeze.yaidom.ProcessingInstruction

  override def toNormalNode: eu.cdevreeze.yaidom.ProcessingInstruction = eu.cdevreeze.yaidom.ProcessingInstruction(target, data)

  override def toString: String = toNormalNode.toString
}

final case class EntityRef(entity: String) extends Node {
  require(entity ne null)

  type NormalNode = eu.cdevreeze.yaidom.EntityRef

  override def toNormalNode: eu.cdevreeze.yaidom.EntityRef = eu.cdevreeze.yaidom.EntityRef(entity)

  override def toString: String = toNormalNode.toString
}

final case class Comment(text: String) extends Node {
  require(text ne null)

  type NormalNode = eu.cdevreeze.yaidom.Comment

  override def toNormalNode: eu.cdevreeze.yaidom.Comment = eu.cdevreeze.yaidom.Comment(text)

  override def toString: String = toNormalNode.toString
}

class XLink(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends Elem(qname, attributes, scope, children) {

  require(wrappedElemWithoutChildren.attributeOption(XLinkTypeExpandedName).isDefined, "Missing %s".format(XLinkTypeExpandedName))

  final def xlinkType: String = wrappedElemWithoutChildren.attribute(XLinkTypeExpandedName)

  final def xlinkAttributes: Map[ExpandedName, String] = wrappedElemWithoutChildren.resolvedAttributes filterKeys { a => a.namespaceUriOption == Some(XLinkNamespace.toString) }

  final def arcroleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkArcroleExpandedName)
}

/** Simple or extended link */
abstract class Link(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends XLink(qname, attributes, scope, children) {
}

final case class SimpleLink(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends Link(qname, attributes, scope, children) {

  require(xlinkType == "simple")
  require(wrappedElemWithoutChildren.attributeOption(XLinkHrefExpandedName).isDefined, "Missing %s".format(XLinkHrefExpandedName))

  def href: URI = wrappedElemWithoutChildren.attributeOption(XLinkHrefExpandedName) map { s => URI.create(s) } getOrElse (sys.error("Missing %s".format(XLinkHrefExpandedName)))
  def roleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkTitleExpandedName)
  def showOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkShowExpandedName)
  def actuateOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkActuateExpandedName)
}

final case class ExtendedLink(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends Link(qname, attributes, scope, children) {

  require(xlinkType == "extended")

  def roleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkRoleExpandedName)

  def titleXLinks: immutable.IndexedSeq[Title] = children collect { case e: Title => e }
  def locatorXLinks: immutable.IndexedSeq[Locator] = children collect { case e: Locator => e }
  def arcXLinks: immutable.IndexedSeq[Arc] = children collect { case e: Arc => e }
  def resourceXLinks: immutable.IndexedSeq[Resource] = children collect { case e: Resource => e }
}

final case class Arc(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends XLink(qname, attributes, scope, children) {

  require(xlinkType == "arc")
  require(arcroleOption.isDefined, "Missing %s".format(XLinkArcroleExpandedName))
  require(wrappedElemWithoutChildren.attributeOption(XLinkFromExpandedName).isDefined, "Missing %s".format(XLinkFromExpandedName))
  require(wrappedElemWithoutChildren.attributeOption(XLinkToExpandedName).isDefined, "Missing %s".format(XLinkToExpandedName))

  def from: String = wrappedElemWithoutChildren.attributeOption(XLinkFromExpandedName).getOrElse(sys.error("Missing %s".format(XLinkFromExpandedName)))
  def to: String = wrappedElemWithoutChildren.attributeOption(XLinkToExpandedName).getOrElse(sys.error("Missing %s".format(XLinkToExpandedName)))
  def titleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkTitleExpandedName)
  def showOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkShowExpandedName)
  def actuateOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkActuateExpandedName)
  def orderOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkOrderExpandedName)
  def useOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkUseExpandedName)
  def priorityOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkPriorityExpandedName)

  def titleXLinks: immutable.IndexedSeq[Title] = children collect { case e: Title => e }
}

final case class Locator(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends XLink(qname, attributes, scope, children) {

  require(xlinkType == "locator")
  require(wrappedElemWithoutChildren.attributeOption(XLinkHrefExpandedName).isDefined, "Missing %s".format(XLinkHrefExpandedName))
  require(wrappedElemWithoutChildren.attributeOption(XLinkLabelExpandedName).isDefined, "Missing %s".format(XLinkLabelExpandedName))

  def href: URI = wrappedElemWithoutChildren.attributeOption(XLinkHrefExpandedName) map { s => URI.create(s) } getOrElse (sys.error("Missing %s".format(XLinkHrefExpandedName)))
  def label: String = wrappedElemWithoutChildren.attributeOption(XLinkLabelExpandedName).getOrElse(sys.error("Missing %s".format(XLinkLabelExpandedName)))
  def roleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkTitleExpandedName)

  def titleXLinks: immutable.IndexedSeq[Title] = children collect { case e: Title => e }
}

final case class Resource(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends XLink(qname, attributes, scope, children) {

  require(xlinkType == "resource")
  require(wrappedElemWithoutChildren.attributeOption(XLinkLabelExpandedName).isDefined, "Missing %s".format(XLinkLabelExpandedName))

  def label: String = wrappedElemWithoutChildren.attributeOption(XLinkLabelExpandedName).getOrElse(sys.error("Missing %s".format(XLinkLabelExpandedName)))
  def roleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElemWithoutChildren.attributeOption(XLinkTitleExpandedName)
}

final case class Title(
  override val qname: QName,
  override val attributes: Map[QName, String],
  override val scope: Scope,
  override val children: immutable.IndexedSeq[Node]) extends XLink(qname, attributes, scope, children) {

  require(xlinkType == "title")
}

object Node {

  def apply(n: eu.cdevreeze.yaidom.Node): Node = n match {
    case d: eu.cdevreeze.yaidom.Document => Document(d)
    case t: eu.cdevreeze.yaidom.Text => Text(t.text, t.isCData)
    case pi: eu.cdevreeze.yaidom.ProcessingInstruction => ProcessingInstruction(pi.target, pi.data)
    case er: eu.cdevreeze.yaidom.EntityRef => EntityRef(er.entity)
    case c: eu.cdevreeze.yaidom.Comment => Comment(c.text)
    case e: eu.cdevreeze.yaidom.Elem if mustBeSimpleLink(e) => SimpleLink(e)
    case e: eu.cdevreeze.yaidom.Elem if mustBeExtendedLink(e) => ExtendedLink(e)
    case e: eu.cdevreeze.yaidom.Elem if mustBeTitle(e) => Title(e)
    case e: eu.cdevreeze.yaidom.Elem if mustBeLocator(e) => Locator(e)
    case e: eu.cdevreeze.yaidom.Elem if mustBeArc(e) => Arc(e)
    case e: eu.cdevreeze.yaidom.Elem if mustBeResource(e) => Resource(e)
    case e: eu.cdevreeze.yaidom.Elem if mustBeXLink(e) => XLink(e)
    case e: eu.cdevreeze.yaidom.Elem => Elem(e)
  }
}

object Document {

  def apply(d: eu.cdevreeze.yaidom.Document): Document = {
    new Document(
      d.baseUriOption,
      Elem(d.documentElement),
      d.processingInstructions map { pi => ProcessingInstruction(pi.target, pi.data) },
      d.comments map { c => Comment(c.text) })
  }
}

object Elem {

  def apply(e: eu.cdevreeze.yaidom.Elem): Elem = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    new Elem(e.qname, e.attributes, e.scope, children)
  }
}

object XLink {

  def apply(e: eu.cdevreeze.yaidom.Elem): XLink = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    new XLink(e.qname, e.attributes, e.scope, children)
  }

  val XLinkNamespace = URI.create("http://www.w3.org/1999/xlink")

  val XLinkTypeExpandedName = ExpandedName(XLinkNamespace.toString, "type")
  val XLinkHrefExpandedName = ExpandedName(XLinkNamespace.toString, "href")
  val XLinkArcroleExpandedName = ExpandedName(XLinkNamespace.toString, "arcrole")
  val XLinkRoleExpandedName = ExpandedName(XLinkNamespace.toString, "role")
  val XLinkTitleExpandedName = ExpandedName(XLinkNamespace.toString, "title")
  val XLinkShowExpandedName = ExpandedName(XLinkNamespace.toString, "show")
  val XLinkActuateExpandedName = ExpandedName(XLinkNamespace.toString, "actuate")
  val XLinkFromExpandedName = ExpandedName(XLinkNamespace.toString, "from")
  val XLinkToExpandedName = ExpandedName(XLinkNamespace.toString, "to")
  val XLinkLabelExpandedName = ExpandedName(XLinkNamespace.toString, "label")
  val XLinkOrderExpandedName = ExpandedName(XLinkNamespace.toString, "order")
  val XLinkUseExpandedName = ExpandedName(XLinkNamespace.toString, "use")
  val XLinkPriorityExpandedName = ExpandedName(XLinkNamespace.toString, "priority")

  def mustBeXLink(e: eu.cdevreeze.yaidom.Elem): Boolean = e.attributeOption(XLinkTypeExpandedName).isDefined

  def mustBeLink(e: eu.cdevreeze.yaidom.Elem): Boolean = mustBeSimpleLink(e) || mustBeExtendedLink(e)

  def mustBeSimpleLink(e: eu.cdevreeze.yaidom.Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("simple")

  def mustBeExtendedLink(e: eu.cdevreeze.yaidom.Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("extended")

  def mustBeTitle(e: eu.cdevreeze.yaidom.Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("title")

  def mustBeLocator(e: eu.cdevreeze.yaidom.Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("locator")

  def mustBeArc(e: eu.cdevreeze.yaidom.Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("arc")

  def mustBeResource(e: eu.cdevreeze.yaidom.Elem): Boolean = e.attributeOption(XLinkTypeExpandedName) == Some("resource")
}

object SimpleLink {

  def apply(e: eu.cdevreeze.yaidom.Elem): SimpleLink = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    SimpleLink(e.qname, e.attributes, e.scope, children)
  }
}

object ExtendedLink {

  def apply(e: eu.cdevreeze.yaidom.Elem): ExtendedLink = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    ExtendedLink(e.qname, e.attributes, e.scope, children)
  }
}

object Title {

  def apply(e: eu.cdevreeze.yaidom.Elem): Title = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    Title(e.qname, e.attributes, e.scope, children)
  }
}

object Locator {

  def apply(e: eu.cdevreeze.yaidom.Elem): Locator = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    Locator(e.qname, e.attributes, e.scope, children)
  }
}

object Arc {

  def apply(e: eu.cdevreeze.yaidom.Elem): Arc = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    Arc(e.qname, e.attributes, e.scope, children)
  }
}

object Resource {

  def apply(e: eu.cdevreeze.yaidom.Elem): Resource = {
    // Recursive calls of XLink Node factory methods
    val children = e.children map { ch => Node(ch) }

    Resource(e.qname, e.attributes, e.scope, children)
  }
}
