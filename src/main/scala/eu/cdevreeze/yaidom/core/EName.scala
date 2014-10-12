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

package eu.cdevreeze.yaidom.core

import eu.cdevreeze.yaidom.XmlStringUtils
import javax.xml.XMLConstants
import javax.xml.namespace.{ QName => JQName }

/**
 * Expanded name. See http://www.w3.org/TR/xml-names11/. It has a localPart and an optional namespace URI.
 * Semantically like a `QName` in Java, but not keeping the prefix.
 *
 * To get an [[eu.cdevreeze.yaidom.core.EName]] from a [[eu.cdevreeze.yaidom.core.QName]],
 * the latter needs to be resolved against a [[eu.cdevreeze.yaidom.Scope]].
 *
 * The short class name illustrates that expanded names are at least as important as qualified names, and should be
 * equally easy to construct (using the companion object).
 *
 * Typical usage may lead to an explosion of different EName objects that are equal. Therefore, application code
 * is encouraged to define and use constants for frequently used ENames. For example, for the XML Schema namespace
 * (and analogous to the XLink constants in yaidom):
 * {{{
 * val XsNamespace = "http://www.w3.org/2001/XMLSchema"
 *
 * val XsElementEName = EName(XsNamespace, "element")
 * val XsAttributeEName = EName(XsNamespace, "attribute")
 * val XsComplexTypeEName = EName(XsNamespace, "complexType")
 * val XsSimpleTypeEName = EName(XsNamespace, "simpleType")
 * // ...
 * }}}
 * In this example, the EName constant names are in upper camel case, starting with the ("preferred") prefix, followed by the
 * local part, and ending with suffix "EName".
 *
 * Implementation note: It was tried as alternative implementation to define EName as (Scala 2.10) value class. The EName would
 * then wrap the expanded name as string representation (in James Clark notation). One cost would be that parsing the (optional)
 * namespace URI and the local name would occur far more frequently. Another cost would be that the alternative implementation
 * would not directly express that an EName is a combination of an optional namespace URI and a local part. Therefore that alternative
 * implementation has been abandoned.
 *
 * @author Chris de Vreeze
 */
final case class EName(namespaceUriOption: Option[String], localPart: String) extends Immutable {
  require(namespaceUriOption ne null)
  require {
    namespaceUriOption forall { ns => (ns ne null) && (ns.length > 0) }
  }
  require(localPart ne null)
  require(XmlStringUtils.isAllowedElementLocalName(localPart), s"'${localPart}' is not an allowed name")

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
  override def toString: String = namespaceUriOption match {
    case None => localPart
    case Some(nsUri) => "{" + nsUri + "}" + localPart
  }
}

object EName {

  /** Creates an `EName` from a namespaceUri and a localPart */
  def apply(namespaceUri: String, localPart: String): EName = EName(Some(namespaceUri), localPart)

  /** Shorthand for `parse(s)` */
  def apply(s: String): EName = parse(s)

  /** Creates an `EName` from a `javax.xml.namespace.QName` */
  def fromJavaQName(jqname: JQName): EName = EName(jqname.toString)

  /** Parses a `String` into an `EName`. The `String` must conform to the `toString` format of an `EName` */
  def parse(s: String): EName = {
    if (s.startsWith("{")) {
      val idx = s.indexOf('}')
      require(idx >= 2 && idx < s.length - 1)
      val ns = s.substring(1, idx)
      val localPart = s.substring(idx + 1)
      EName(Some(ns), localPart)
    } else {
      require(s.indexOf("{") < 0)
      require(s.indexOf("}") < 0)
      EName(None, s)
    }
  }
}
