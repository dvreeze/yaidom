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
 * Elem at the level of XLink awareness. It is either an XLink or not. It wraps a yaidom [[eu.cdevreeze.yaidom.Elem]].
 *
 * When using the traits and classes in this package, prefix them with the last part of the package name. So,
 * write `xlink.Elem` instead of globally importing classes/traits in the [[eu.cdevreeze.yaidom.xlink]] package.
 * This is analogous to the good practice of writing for example `immutable.IndexedSeq[T]` and
 * `mutable.IndexedSeq[T]` for Scala Collections.
 *
 * It is advisable not to use these `xlink.Elem`s "globally" at a large scale, because that may cause a lot of wrapping of yaidom
 * Elems as `xlink.Elem`s. Rather use `xlink.XLink`s "locally" as short-lived objects where they are useful.
 *
 * @author Chris de Vreeze
 */
sealed trait Elem extends Immutable {

  val wrappedElem: eu.cdevreeze.yaidom.Elem

  require(wrappedElem ne null)

  final def resolvedName: ExpandedName = wrappedElem.resolvedName

  final def resolvedAttributes: Map[ExpandedName, String] = wrappedElem.resolvedAttributes
}

/** XLink */
trait XLink extends Elem {
  require(wrappedElem.attributeOption(XLinkTypeExpandedName).isDefined, "Missing %s".format(XLinkTypeExpandedName))

  def xlinkType: String = wrappedElem.attribute(XLinkTypeExpandedName)

  def xlinkAttributes: Map[ExpandedName, String] = wrappedElem.resolvedAttributes filterKeys { a => a.namespaceUriOption == Some(XLinkNamespace.toString) }

  def arcroleOption: Option[String] = wrappedElem.attributeOption(XLinkArcroleExpandedName)
}

/** Simple or extended link */
trait Link extends XLink

final case class SimpleLink(override val wrappedElem: eu.cdevreeze.yaidom.Elem) extends Link {
  require(xlinkType == "simple")
  require(wrappedElem.attributeOption(XLinkHrefExpandedName).isDefined, "Missing %s".format(XLinkHrefExpandedName))

  def href: URI = wrappedElem.attributeOption(XLinkHrefExpandedName) map { s => URI.create(s) } getOrElse (sys.error("Missing %s".format(XLinkHrefExpandedName)))
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)
  def showOption: Option[String] = wrappedElem.attributeOption(XLinkShowExpandedName)
  def actuateOption: Option[String] = wrappedElem.attributeOption(XLinkActuateExpandedName)
}

final case class ExtendedLink(override val wrappedElem: eu.cdevreeze.yaidom.Elem) extends Link {
  require(xlinkType == "extended")

  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)

  def titleXLinks: immutable.IndexedSeq[Title] = wrappedElem.allChildElems collect { case e if XLink.mustBeTitle(e) => Title(e) }
  def locatorXLinks: immutable.IndexedSeq[Locator] = wrappedElem.allChildElems collect { case e if XLink.mustBeLocator(e) => Locator(e) }
  def arcXLinks: immutable.IndexedSeq[Arc] = wrappedElem.allChildElems collect { case e if XLink.mustBeArc(e) => Arc(e) }
  def resourceXLinks: immutable.IndexedSeq[Resource] = wrappedElem.allChildElems collect { case e if XLink.mustBeResource(e) => Resource(e) }
}

final case class Arc(override val wrappedElem: eu.cdevreeze.yaidom.Elem) extends XLink {
  require(xlinkType == "arc")
  require(arcroleOption.isDefined, "Missing %s".format(XLinkArcroleExpandedName))
  require(wrappedElem.attributeOption(XLinkFromExpandedName).isDefined, "Missing %s".format(XLinkFromExpandedName))
  require(wrappedElem.attributeOption(XLinkToExpandedName).isDefined, "Missing %s".format(XLinkToExpandedName))

  def from: String = wrappedElem.attributeOption(XLinkFromExpandedName).getOrElse(sys.error("Missing %s".format(XLinkFromExpandedName)))
  def to: String = wrappedElem.attributeOption(XLinkToExpandedName).getOrElse(sys.error("Missing %s".format(XLinkToExpandedName)))
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)
  def showOption: Option[String] = wrappedElem.attributeOption(XLinkShowExpandedName)
  def actuateOption: Option[String] = wrappedElem.attributeOption(XLinkActuateExpandedName)
  def orderOption: Option[String] = wrappedElem.attributeOption(XLinkOrderExpandedName)
  def useOption: Option[String] = wrappedElem.attributeOption(XLinkUseExpandedName)
  def priorityOption: Option[String] = wrappedElem.attributeOption(XLinkPriorityExpandedName)

  def titleXLinks: immutable.IndexedSeq[Title] = wrappedElem.allChildElems collect { case e if XLink.mustBeTitle(e) => Title(e) }
}

final case class Locator(override val wrappedElem: eu.cdevreeze.yaidom.Elem) extends XLink {
  require(xlinkType == "locator")
  require(wrappedElem.attributeOption(XLinkHrefExpandedName).isDefined, "Missing %s".format(XLinkHrefExpandedName))
  require(wrappedElem.attributeOption(XLinkLabelExpandedName).isDefined, "Missing %s".format(XLinkLabelExpandedName))

  def href: URI = wrappedElem.attributeOption(XLinkHrefExpandedName) map { s => URI.create(s) } getOrElse (sys.error("Missing %s".format(XLinkHrefExpandedName)))
  def label: String = wrappedElem.attributeOption(XLinkLabelExpandedName).getOrElse(sys.error("Missing %s".format(XLinkLabelExpandedName)))
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)

  def titleXLinks: immutable.IndexedSeq[Title] = wrappedElem.allChildElems collect { case e if XLink.mustBeTitle(e) => Title(e) }
}

final case class Resource(override val wrappedElem: eu.cdevreeze.yaidom.Elem) extends XLink {
  require(xlinkType == "resource")
  require(wrappedElem.attributeOption(XLinkLabelExpandedName).isDefined, "Missing %s".format(XLinkLabelExpandedName))

  def label: String = wrappedElem.attributeOption(XLinkLabelExpandedName).getOrElse(sys.error("Missing %s".format(XLinkLabelExpandedName)))
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleExpandedName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleExpandedName)
}

final case class Title(override val wrappedElem: eu.cdevreeze.yaidom.Elem) extends XLink {
  require(xlinkType == "title")
}

object Elem {

  def apply(e: eu.cdevreeze.yaidom.Elem): Elem = e match {
    case e if mustBeSimpleLink(e) => SimpleLink(e)
    case e if mustBeExtendedLink(e) => ExtendedLink(e)
    case e if mustBeTitle(e) => Title(e)
    case e if mustBeLocator(e) => Locator(e)
    case e if mustBeArc(e) => Arc(e)
    case e if mustBeResource(e) => Resource(e)
    case e if mustBeXLink(e) => {
      new {
        val wrappedElem: eu.cdevreeze.yaidom.Elem = e
      } with XLink
    }
    case e => {
      new {
        val wrappedElem: eu.cdevreeze.yaidom.Elem = e
      } with Elem
    }
  }
}

object XLink {

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
