/*
 * Copyright 2011-2017 Chris de Vreeze
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

/**
 * Expanded name. See http://www.w3.org/TR/xml-names11/. It has a localPart and an optional namespace URI.
 * Semantically like a `QName` in Java, but not keeping the prefix.
 *
 * To get an [[eu.cdevreeze.yaidom.core.EName]] from a [[eu.cdevreeze.yaidom.core.QName]],
 * the latter needs to be resolved against a [[eu.cdevreeze.yaidom.core.Scope]].
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
final case class EName(namespaceUriOption: Option[String], localPart: String) {
  require(namespaceUriOption ne null) // scalastyle:off null
  require(localPart ne null) // scalastyle:off null

  /** Given an optional prefix, creates a `QName` from this `EName` */
  def toQName(prefixOption: Option[String]): QName = {
    require(
      namespaceUriOption.isDefined || prefixOption.isEmpty, s"Prefix only allowed if namespace non-empty in EName '${this}'")
    QName(prefixOption, localPart)
  }

  /** The `String` representation, in the format of the `javax.xml.namespace.QName.toString` method */
  override def toString: String = namespaceUriOption match {
    case None => localPart
    case Some(nsUri) => "{" + nsUri + "}" + localPart
  }

  /**
   * Returns the string representation of this EName as URI qualified name, as a braced URI literal followed by
   * an NC-name. See for example https://www.w3.org/TR/xpath-31/#prod-xpath31-URIQualifiedName.
   */
  def toUriQualifiedNameString: String = {
    val ns = namespaceUriOption.getOrElse("")

    s"Q{$ns}$localPart"
  }

  /**
   * Partially validates the EName, throwing an exception if found not valid.
   * If not found invalid, returns this.
   *
   * It is the responsibility of the user of this class to call this method, if needed.
   * Fortunately, this method facilitates method chaining, because the EName itself is returned.
   */
  // scalastyle:off null
  def validated: EName = {
    require(
      namespaceUriOption.forall(ns => (ns ne null) && (ns.length > 0)),
      s"Empty (as opposed to absent) namespace URI not allowed in EName '${this}'")
    require(XmlStringUtils.isAllowedElementLocalName(localPart), s"'${localPart}' is not an allowed name in EName '${this}'")
    this
  }
}

object EName {

  /** Creates an `EName` from a namespaceUri and a localPart */
  def apply(namespaceUri: String, localPart: String): EName = EName(Some(namespaceUri), localPart)

  /** Shorthand for `parse(s)` */
  def apply(s: String): EName = parse(s)

  /**
   * Parses a `String` into an `EName`. The `String` (after trimming) must conform to the `toString` format of an `EName`.
   */
  def parse(s: String): EName = {
    val st = s.trim

    if (st.startsWith("{")) {
      val idx = st.indexOf('}')
      require(idx >= 2 && idx < st.length - 1, s"Opening brace not closed or at incorrect location in EName '${st}'")
      val ns = st.substring(1, idx)
      val localPart = st.substring(idx + 1)
      EName(Some(ns), localPart)
    } else {
      require(st.indexOf("{") < 0, s"No opening brace allowed unless at the beginning in EName '${st}'")
      require(st.indexOf("}") < 0, s"Closing brace without matching opening brace not allowed in EName '${st}'")
      EName(None, st)
    }
  }

  /**
   * Parses a `String` into an `EName`. The `String` must conform to the `toUriQualifiedNameString` format of an `EName`.
   */
  def fromUriQualifiedNameString(uriQualifiedName: String): EName = {
    require(
      uriQualifiedName.startsWith("Q{"),
      s"Not an URI qualified name (it does not start with 'Q{'): $uriQualifiedName")

    val endBraceIdx = uriQualifiedName.indexOf("}")

    require(endBraceIdx > 0, s"Not an URI qualified name (it has no '}'): $uriQualifiedName")

    val ns = uriQualifiedName.substring(2, endBraceIdx)
    val nsOption = if (ns.isEmpty) None else Some(ns)

    val localPart = uriQualifiedName.substring(endBraceIdx + 1)

    require(localPart.length > 0, s"Not an URI qualified name (it has no local part): $uriQualifiedName")

    EName(nsOption, localPart)
  }
}
