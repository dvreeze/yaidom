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

/**
 * Qualified name. See http://www.w3.org/TR/xml-names11/.
 * Semantically like a QName in Anti-XML, and not like a QName in Java.
 *
 * There are 2 types of QNames:
 * <ul>
 * <li>UnprefixedNames, which only contain a local part</li>
 * <li>PrefixedNames, which combine a non-empty prefix with a local part</li>
 * </ul>
 *
 * QNames are meaningless outside their scope, which resolves the QName as an ExpandedName.
 *
 * @author Chris de Vreeze
 */
sealed trait QName extends Immutable {

  def localPart: String
  def prefixOption: Option[String]
}

final case class UnprefixedName(override val localPart: String) extends QName {
  require(localPart ne null)
  require(localPart.size > 0)
  require(localPart.indexOf(":") < 0)

  override def prefixOption: Option[String] = None

  /** The String representation as it appears in XML, that is, the localPart */
  override def toString: String = localPart
}

final case class PrefixedName(prefix: String, override val localPart: String) extends QName {
  require(prefix ne null)
  require(prefix.size > 0)
  require(prefix.indexOf(":") < 0)
  require(localPart ne null)
  require(localPart.size > 0)
  require(localPart.indexOf(":") < 0)

  override def prefixOption: Option[String] = Some(prefix)

  /** The String representation as it appears in XML E.g. <b>xs:schema</b> */
  override def toString: String = "%s:%s".format(prefix, localPart)
}

object QName {

  /** Creates a QName from an optional prefix and a localPart */
  def apply(prefix: Option[String], localPart: String): QName =
    prefix map { pref => PrefixedName(pref, localPart) } getOrElse (UnprefixedName(localPart))

  /** Creates a PrefixedName from a prefix and a localPart */
  def apply(prefix: String, localPart: String): QName = PrefixedName(prefix, localPart)

  /** Creates an UnprefixedName from a localPart */
  def apply(localPart: String): QName = UnprefixedName(localPart)

  /** Parses a String into a QName. The String must conform to the <b>toString</b> format of a PrefixedName or UnprefixedName */
  def parse(s: String): QName = {
    val arr = s.split(':')
    require(arr.size <= 2, "Expected at most 1 colon in QName '%s'".format(s))

    arr.size match {
      case 1 => QName(s)
      case 2 => QName(arr(0), arr(1))
      case _ => sys.error("Did not expect more than 1 colon in QName '%s'".format(s))
    }
  }

  /** "Implicit class" for converting a String to a QName */
  final class ToParsedQName(val s: String) {
    def qname: QName = QName.parse(s)
  }

  /** Implicit conversion enriching a String with a <code>qname</code> method that turns the String into a QName */
  implicit def toParsedQName(s: String): ToParsedQName = new ToParsedQName(s)
}
