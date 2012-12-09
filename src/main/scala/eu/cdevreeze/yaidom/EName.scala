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

import javax.xml.XMLConstants
import javax.xml.namespace.{ QName => JQName }

/**
 * Expanded name. See http://www.w3.org/TR/xml-names11/. It has a localPart and an optional namespace URI.
 * Semantically like a `QName` in Java, but not keeping the prefix.
 *
 * To get an [[eu.cdevreeze.yaidom.EName]] from a [[eu.cdevreeze.yaidom.QName]],
 * the latter needs to be resolved against a [[eu.cdevreeze.yaidom.Scope]].
 *
 * The short class name illustrates that expanded names are at least as important as qualified names, and should be
 * equally easy to construct (using the companion object).
 *
 * Typical usage may lead to an explosion of different EName objects that are equal. That is a bit wasteful.
 * Of course the user can define constants for ENames that are used often. Maybe in the future SIP-15 (value classes)
 * can help make ENames completely inlined, thus making it efficient to use them at a large scale.
 *
 * @author Chris de Vreeze
 */
final case class EName(namespaceUriOption: Option[String], localPart: String) extends Immutable {
  require(namespaceUriOption ne null)
  require {
    namespaceUriOption forall { ns => (ns ne null) && (ns.length > 0) }
  }
  require(localPart ne null)
  require(XmlStringUtils.isAllowedElementLocalName(localPart), "'%s' is not an allowed name".format(localPart))

  /** Given an optional prefix, creates a `QName` from this `EName` */
  def toQName(prefixOption: Option[String]): QName = {
    require(namespaceUriOption.isDefined || prefixOption.isEmpty)
    QName(prefixOption, localPart)
  }

  /** Given an optional prefix, creates a `javax.xml.namespace.QName` from this EName */
  def toJavaQName(prefixOption: Option[String]): JQName = {
    require(namespaceUriOption.isDefined || prefixOption.isEmpty)
    new JQName(namespaceUriOption.getOrElse(XMLConstants.NULL_NS_URI), localPart, prefixOption.getOrElse(XMLConstants.DEFAULT_NS_PREFIX))
  }

  /** The `String` representation, in the format of the `javax.xml.namespace.QName.toString` method */
  override def toString: String = toJavaQName(None).toString
}

object EName {

  /** Creates an `EName` from a namespaceUri and a localPart */
  def apply(namespaceUri: String, localPart: String): EName = EName(Some(namespaceUri), localPart)

  /** Shorthand for `parse(s)` */
  def apply(s: String): EName = parse(s)

  /** Creates an `EName` from a `javax.xml.namespace.QName` */
  def fromJavaQName(jqname: JQName): EName = jqname match {
    case jqname: JQName if (jqname.getNamespaceURI eq null) || (jqname.getNamespaceURI == XMLConstants.NULL_NS_URI) =>
      EName(None, jqname.getLocalPart)
    case _ => EName(Some(jqname.getNamespaceURI), jqname.getLocalPart)
  }

  /** Parses a `String` into an `EName`. The `String` must conform to the `toString` format of an `EName` */
  def parse(s: String): EName = s match {
    case s if s.startsWith("{") =>
      val idx = s indexWhere { c => c == '}' }
      require(idx >= 2 && idx < s.length - 1)
      val ns = s.slice(1, idx)
      val localPart = s.slice(idx + 1, s.length)
      EName(Some(ns), localPart)
    case _ =>
      require(s.indexOf("{") < 0)
      require(s.indexOf("}") < 0)
      EName(None, s)
  }
}
