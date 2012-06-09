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
import eu.cdevreeze.yaidom.Predef._
import XLink._

/**
 * Immutable XLink. See See http://www.w3.org/TR/xlink11/. An `XLink` wraps a yaidom [[eu.cdevreeze.yaidom.Elem]].
 * The XLink support is without any support for XPointer.
 *
 * @author Chris de Vreeze
 */
sealed abstract class XLink(val wrappedElem: Elem) extends Immutable {
  require(wrappedElem ne null)
  require(
    wrappedElem.attributeOption(XLinkTypeEName).isDefined || wrappedElem.attributeOption(XLinkHrefEName).isDefined,
    "Missing both %s and %s".format(XLinkTypeEName, XLinkHrefEName))

  def xlinkType: String = wrappedElem.attributeOption(XLinkTypeEName).getOrElse("simple")

  def xlinkAttributes: Map[EName, String] =
    wrappedElem.resolvedAttributes filterKeys { a => a.namespaceUriOption == Some(XLinkNamespace.toString) }
}

/** Simple or extended link */
abstract class Link(override val wrappedElem: Elem) extends XLink(wrappedElem)

final class SimpleLink(override val wrappedElem: Elem) extends Link(wrappedElem) {
  require(xlinkType == "simple")
  require(
    wrappedElem.attributeOption(XLinkTypeEName).isDefined || wrappedElem.attributeOption(XLinkHrefEName).isDefined,
    "Missing both %s and %s".format(XLinkTypeEName, XLinkHrefEName))

  def hrefOption: Option[URI] = wrappedElem.attributeOption(XLinkHrefEName) map { s => URI.create(s) }
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleEName)
  def arcroleOption: Option[String] = wrappedElem.attributeOption(XLinkArcroleEName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleEName)
  def showOption: Option[String] = wrappedElem.attributeOption(XLinkShowEName)
  def actuateOption: Option[String] = wrappedElem.attributeOption(XLinkActuateEName)
}

final class ExtendedLink(override val wrappedElem: Elem) extends Link(wrappedElem) {
  require(xlinkType == "extended")

  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleEName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleEName)

  def titleXLinks: immutable.IndexedSeq[Title] = wrappedElem.allChildElems collect { case e if XLink.mustBeTitle(e) => Title(e) }
  def locatorXLinks: immutable.IndexedSeq[Locator] = wrappedElem.allChildElems collect { case e if XLink.mustBeLocator(e) => Locator(e) }
  def arcXLinks: immutable.IndexedSeq[Arc] = wrappedElem.allChildElems collect { case e if XLink.mustBeArc(e) => Arc(e) }
  def resourceXLinks: immutable.IndexedSeq[Resource] = wrappedElem.allChildElems collect { case e if XLink.mustBeResource(e) => Resource(e) }
}

final class Arc(override val wrappedElem: Elem) extends XLink(wrappedElem) {
  require(xlinkType == "arc")

  def fromOption: Option[String] = wrappedElem.attributeOption(XLinkFromEName)
  def toOption: Option[String] = wrappedElem.attributeOption(XLinkToEName)
  def arcroleOption: Option[String] = wrappedElem.attributeOption(XLinkArcroleEName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleEName)
  def showOption: Option[String] = wrappedElem.attributeOption(XLinkShowEName)
  def actuateOption: Option[String] = wrappedElem.attributeOption(XLinkActuateEName)

  def titleXLinks: immutable.IndexedSeq[Title] = wrappedElem.allChildElems collect { case e if XLink.mustBeTitle(e) => Title(e) }
}

final class Locator(override val wrappedElem: Elem) extends XLink(wrappedElem) {
  require(xlinkType == "locator")
  require(wrappedElem.attributeOption(XLinkHrefEName).isDefined, "Missing %s".format(XLinkHrefEName))

  def href: URI = wrappedElem.attributeOption(XLinkHrefEName) map { s => URI.create(s) } getOrElse (sys.error("Missing %s".format(XLinkHrefEName)))
  def labelOption: Option[String] = wrappedElem.attributeOption(XLinkLabelEName)
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleEName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleEName)

  def titleXLinks: immutable.IndexedSeq[Title] = wrappedElem.allChildElems collect { case e if XLink.mustBeTitle(e) => Title(e) }
}

final class Resource(override val wrappedElem: Elem) extends XLink(wrappedElem) {
  require(xlinkType == "resource")

  def labelOption: Option[String] = wrappedElem.attributeOption(XLinkLabelEName)
  def roleOption: Option[String] = wrappedElem.attributeOption(XLinkRoleEName)
  def titleOption: Option[String] = wrappedElem.attributeOption(XLinkTitleEName)
}

final class Title(override val wrappedElem: Elem) extends XLink(wrappedElem) {
  require(xlinkType == "title")
}

object XLink {

  val XLinkNamespace = URI.create("http://www.w3.org/1999/xlink").toString.ns

  val XLinkTypeEName = XLinkNamespace.ename("type")
  val XLinkHrefEName = XLinkNamespace.ename("href")
  val XLinkArcroleEName = XLinkNamespace.ename("arcrole")
  val XLinkRoleEName = XLinkNamespace.ename("role")
  val XLinkTitleEName = XLinkNamespace.ename("title")
  val XLinkShowEName = XLinkNamespace.ename("show")
  val XLinkActuateEName = XLinkNamespace.ename("actuate")
  val XLinkFromEName = XLinkNamespace.ename("from")
  val XLinkToEName = XLinkNamespace.ename("to")
  val XLinkLabelEName = XLinkNamespace.ename("label")
  val XLinkOrderEName = XLinkNamespace.ename("order")
  val XLinkUseEName = XLinkNamespace.ename("use")
  val XLinkPriorityEName = XLinkNamespace.ename("priority")

  def mustBeXLink(e: Elem): Boolean = {
    mustBeLink(e) || mustBeTitle(e) || mustBeLocator(e) || mustBeArc(e) || mustBeResource(e)
  }

  def mustBeLink(e: Elem): Boolean = mustBeSimpleLink(e) || mustBeExtendedLink(e)

  def mustBeSimpleLink(e: Elem): Boolean = {
    if (e.attributeOption(XLinkTypeEName).isEmpty)
      e.attributeOption(XLinkHrefEName).isDefined
    else
      e.attributeOption(XLinkTypeEName) == Some("simple")
  }

  def mustBeExtendedLink(e: Elem): Boolean = e.attributeOption(XLinkTypeEName) == Some("extended")

  def mustBeTitle(e: Elem): Boolean = e.attributeOption(XLinkTypeEName) == Some("title")

  def mustBeLocator(e: Elem): Boolean = e.attributeOption(XLinkTypeEName) == Some("locator")

  def mustBeArc(e: Elem): Boolean = e.attributeOption(XLinkTypeEName) == Some("arc")

  def mustBeResource(e: Elem): Boolean = e.attributeOption(XLinkTypeEName) == Some("resource")

  def apply(e: Elem): XLink = e match {
    case e if mustBeSimpleLink(e) => SimpleLink(e)
    case e if mustBeExtendedLink(e) => ExtendedLink(e)
    case e if mustBeTitle(e) => Title(e)
    case e if mustBeLocator(e) => Locator(e)
    case e if mustBeArc(e) => Arc(e)
    case e if mustBeResource(e) => Resource(e)
    case e => sys.error("Not an XLink: %s".format(e))
  }
}

object Link {

  def apply(e: Elem): Link = e match {
    case e if mustBeSimpleLink(e) => SimpleLink(e)
    case e if mustBeExtendedLink(e) => ExtendedLink(e)
  }
}

object SimpleLink {

  def apply(e: Elem): SimpleLink = new SimpleLink(e)
}

object ExtendedLink {

  def apply(e: Elem): ExtendedLink = new ExtendedLink(e)
}

object Arc {

  def apply(e: Elem): Arc = new Arc(e)
}

object Locator {

  def apply(e: Elem): Locator = new Locator(e)
}

object Resource {

  def apply(e: Elem): Resource = new Resource(e)
}

object Title {

  def apply(e: Elem): Title = new Title(e)
}
