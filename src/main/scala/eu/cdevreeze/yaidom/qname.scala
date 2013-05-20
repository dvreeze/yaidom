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
 * Qualified name. See http://www.w3.org/TR/xml-names11/.
 * Semantically like a `QName` in Anti-XML, and not like a `QName` in Java.
 *
 * There are 2 types of `QName`s:
 * <ul>
 * <li>[[eu.cdevreeze.yaidom.UnprefixedName]], which only contains a local part</li>
 * <li>[[eu.cdevreeze.yaidom.PrefixedName]], which combines a non-empty prefix with a local part</li>
 * </ul>
 *
 * QNames are meaningless outside their scope, which resolves the `QName` as an [[eu.cdevreeze.yaidom.EName]].
 *
 * Typical usage may lead to an explosion of different QName objects that are equal. Therefore, application code
 * is encouraged to define and use constants for frequently used QNames. For example, for the XML Schema namespace:
 * {{{
 * val XsPrefix = "xs"
 *
 * val XsElementQName = QName(XsPrefix, "element")
 * val XsAttributeQName = QName(XsPrefix, "attribute")
 * val XsComplexTypeQName = QName(XsPrefix, "complexType")
 * val XsSimpleTypeQName = QName(XsPrefix, "simpleType")
 * // ...
 * }}}
 * In this example, the QName constant names are in upper camel case, starting with the prefix, followed by the local part,
 * and ending with suffix "QName".
 *
 * Implementation note: It was tried as alternative implementation to define the QName subclasses as (Scala 2.10) value classes.
 * The QName would then wrap the qualified name as string representation (with or without prefix). One cost would be that parsing
 * the (optional) prefix and the local name would occur far more frequently. Another cost would be that the alternative implementation
 * would not directly express that a QName is a combination of an optional prefix and a local part. Therefore that alternative
 * implementation has been abandoned.
 *
 * @author Chris de Vreeze
 */
sealed trait QName extends Immutable with Serializable {

  def localPart: String
  def prefixOption: Option[String]
}

final case class UnprefixedName(override val localPart: String) extends QName {
  require(localPart ne null)
  require(XmlStringUtils.isAllowedElementLocalName(localPart), "'%s' is not an allowed name".format(localPart))

  override def prefixOption: Option[String] = None

  /** The `String` representation as it appears in XML, that is, the localPart */
  override def toString: String = localPart
}

final case class PrefixedName(prefix: String, override val localPart: String) extends QName {
  require(prefix ne null)
  require(XmlStringUtils.isAllowedPrefix(prefix), "'%s' is not an allowed prefix name".format(prefix))
  require(localPart ne null)
  require(XmlStringUtils.isAllowedElementLocalName(localPart), "'%s' is not an allowed name".format(localPart))

  override def prefixOption: Option[String] = Some(prefix)

  /** The `String` representation as it appears in XML. For example, <code>xs:schema</code> */
  override def toString: String = "%s:%s".format(prefix, localPart)
}

object QName {

  /** Creates a `QName` from an optional prefix and a localPart */
  def apply(prefixOption: Option[String], localPart: String): QName =
    prefixOption map { pref => PrefixedName(pref, localPart) } getOrElse (UnprefixedName(localPart))

  /** Creates a `PrefixedName` from a prefix and a localPart */
  def apply(prefix: String, localPart: String): QName = PrefixedName(prefix, localPart)

  /** Shorthand for `parse(s)` */
  def apply(s: String): QName = parse(s)

  /** Parses a `String` into a `QName`. The `String` must conform to the `toString` format of a `PrefixedName` or `UnprefixedName` */
  def parse(s: String): QName = {
    val arr = s.split(':')
    require(arr.size <= 2, "Expected at most 1 colon in QName '%s'".format(s))

    arr.size match {
      case 1 => UnprefixedName(s)
      case 2 => PrefixedName(arr(0), arr(1))
      case _ => sys.error("Did not expect more than 1 colon in QName '%s'".format(s))
    }
  }
}
