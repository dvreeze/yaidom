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
 * The QName subclasses are value classes for efficiency, thus avoiding an explosion of objects that are equal.
 *
 * @author Chris de Vreeze
 */
sealed trait QName extends Any {

  def localPart: String
  def prefixOption: Option[String]
}

final class UnprefixedName private (val value: String) extends AnyVal with QName with Serializable {

  override def localPart: String = value

  override def prefixOption: Option[String] = None

  /** The `String` representation as it appears in XML, that is, the localPart */
  override def toString: String = value
}

object UnprefixedName {

  /** Shorthand for `new UnprefixedName(s)`, but first checking the parameter */
  def apply(localPart: String): UnprefixedName = {
    require(localPart ne null)
    require(XmlStringUtils.isAllowedElementLocalName(localPart), "'%s' is not an allowed name".format(localPart))

    new UnprefixedName(localPart)
  }
}

final class PrefixedName private (val value: String) extends AnyVal with QName with Serializable {

  def prefix: String = value.substring(0, value.indexOf(':'))

  override def localPart: String = value.substring(value.indexOf(':') + 1)

  override def prefixOption: Option[String] = Some(prefix)

  /** The `String` representation as it appears in XML. For example, <code>xs:schema</code> */
  override def toString: String = value
}

object PrefixedName {

  /** Shorthand for `new PrefixedName(s)`, but first checking the parameters */
  def apply(prefix: String, localPart: String): PrefixedName = {
    require(prefix ne null)
    require(XmlStringUtils.isAllowedPrefix(prefix), "'%s' is not an allowed prefix name".format(prefix))
    require(localPart ne null)
    require(XmlStringUtils.isAllowedElementLocalName(localPart), "'%s' is not an allowed name".format(localPart))

    new PrefixedName(s"$prefix:$localPart")
  }
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
