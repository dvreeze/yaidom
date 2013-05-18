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
import EName._

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
 * Class EName is a value class for efficiency, thus avoiding an explosion of objects that are equal.
 *
 * @author Chris de Vreeze
 */
final class EName private (val value: String) extends AnyVal with Serializable {

  def namespaceUriOption: Option[String] = parseNamespaceUriOption(value)

  def localPart: String = parseLocalPart(value)

  /** Given an optional prefix, creates a `QName` from this `EName` */
  def toQName(prefixOption: Option[String]): QName = {
    val nsUriOption = namespaceUriOption
    val localName = localPart
    require(nsUriOption.isDefined || prefixOption.isEmpty)
    QName(prefixOption, localName)
  }

  /** Given an optional prefix, creates a `javax.xml.namespace.QName` from this EName */
  def toJavaQName(prefixOption: Option[String]): JQName = {
    val nsUriOption = namespaceUriOption
    val localName = localPart
    require(nsUriOption.isDefined || prefixOption.isEmpty)
    new JQName(nsUriOption.getOrElse(XMLConstants.NULL_NS_URI), localName, prefixOption.getOrElse(XMLConstants.DEFAULT_NS_PREFIX))
  }

  /** The `String` representation, in the format of the `javax.xml.namespace.QName.toString` method */
  override def toString: String = toJavaQName(None).toString
}

object EName {

  /** Creates an `EName` from an optional namespaceUri and a localPart */
  def apply(namespaceUriOption: Option[String], localPart: String): EName = {
    if (namespaceUriOption.isEmpty) apply(localPart) else {
      val nsUri = namespaceUriOption.get
      apply(s"{$nsUri}$localPart")
    }
  }

  /** Creates an `EName` from a namespaceUri and a localPart */
  def apply(namespaceUri: String, localPart: String): EName = {
    apply(s"{$namespaceUri}$localPart")
  }

  /** Shorthand for `new EName(s)`, but first checking the string value */
  def apply(s: String): EName = {
    check(s)
    new EName(s)
  }

  /** Creates an `EName` from a `javax.xml.namespace.QName` */
  def fromJavaQName(jqname: JQName): EName = EName(jqname.toString)

  private def parseNamespaceUriOption(value: String): Option[String] = {
    if (value.startsWith("{")) {
      val idx = value.indexOf('}')
      require(idx >= 2 && idx < value.length - 1)
      val ns = value.substring(1, idx)
      Some(ns)
    } else {
      require(value.indexOf("{") < 0)
      require(value.indexOf("}") < 0)
      None
    }
  }

  private def parseLocalPart(value: String): String = {
    if (value.startsWith("{")) {
      val idx = value.indexOf('}')
      require(idx >= 2 && idx < value.length - 1)
      val localName = value.substring(idx + 1)
      localName
    } else {
      require(value.indexOf("{") < 0)
      require(value.indexOf("}") < 0)
      value
    }
  }

  private def check(value: String) {
    require(value ne null)
    val nsUriOption = parseNamespaceUriOption(value)
    val localName = parseLocalPart(value)

    require(nsUriOption ne null)
    require {
      nsUriOption forall { ns => (ns ne null) && (ns.length > 0) }
    }
    require(localName ne null)
    require(XmlStringUtils.isAllowedElementLocalName(localName), "'%s' is not an allowed name".format(localName))
  }
}
