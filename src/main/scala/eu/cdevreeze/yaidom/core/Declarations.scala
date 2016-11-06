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
 * Namespace declarations (and undeclarations), typically at the level of one element.
 *
 * For example, consider the following XML:
 * {{{
 * <book:Bookstore xmlns:book="http://bookstore/book" xmlns:auth="http://bookstore/author">
 *   <book:Book ISBN="978-0321356680" Price="35" Edition="2">
 *     <book:Title>Effective Java (2nd Edition)</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Joshua</auth:First_Name>
 *         <auth:Last_Name>Bloch</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 * </book:Bookstore>
 * }}}
 * Then only the root element contains namespace declarations, viz.:
 * {{{
 * Declarations.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author")
 * }}}
 *
 * The `Declarations` is backed by a map from prefixes (or the empty string for the default namespace) to namespace URIs (or the empty string).
 * If the mapped value is the empty string, it is an undeclaration.
 *
 * Prefix 'xml' is not allowed as key in this map. That prefix, mapping to namespace URI 'http://www.w3.org/XML/1998/namespace',
 * is always available, without needing any declaration.
 *
 * This class does not depend on the `Scope` class.
 *
 * There are no methods for subset relationships on namespace declarations (unlike for class `Scope`).
 * After all, in the presence of namespace undeclarations, such a subset relationship would become a bit unnatural.
 *
 * @author Chris de Vreeze
 */
final case class Declarations(prefixNamespaceMap: Map[String, String]) extends Immutable {
  import Declarations._

  validate(prefixNamespaceMap)

  /** Returns true if this Declarations is empty. Faster than comparing this Declarations against the empty Declarations. */
  def isEmpty: Boolean = prefixNamespaceMap.isEmpty

  /** Returns an adapted copy of this Declarations, but retaining only the undeclarations, if any */
  def retainingUndeclarations: Declarations = {
    val m = prefixNamespaceMap filter { case (pref, ns) => ns == "" }
    if (m.isEmpty) Declarations.Empty else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but without any undeclarations, if any */
  def withoutUndeclarations: Declarations = {
    val m = prefixNamespaceMap filter { case (pref, ns) => ns != "" }
    if (m.size == prefixNamespaceMap.size) this else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but retaining only the default namespace, if any */
  def retainingDefaultNamespace: Declarations = {
    val m = prefixNamespaceMap filter { case (pref, ns) => pref == DefaultNsPrefix }
    if (m.isEmpty) Declarations.Empty else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but without the default namespace, if any */
  def withoutDefaultNamespace: Declarations = {
    if (!prefixNamespaceMap.contains(DefaultNsPrefix)) this else Declarations(prefixNamespaceMap - DefaultNsPrefix)
  }

  /** Returns `Declarations(this.prefixNamespaceMap ++ declarations.prefixNamespaceMap)` */
  def append(declarations: Declarations): Declarations = Declarations(this.prefixNamespaceMap ++ declarations.prefixNamespaceMap)

  /** Returns `Declarations(this.prefixNamespaceMap -- prefixes)` */
  def minus(prefixes: Set[String]): Declarations = Declarations(this.prefixNamespaceMap -- prefixes)

  /** Alias for `append` */
  def ++(declarations: Declarations): Declarations = append(declarations)

  /** Alias for `minus` */
  def --(prefixes: Set[String]): Declarations = minus(prefixes)

  /** Alias for `append(Declarations.from((prefix, namespace)))` */
  def append(prefix: String, namespace: String): Declarations = append(Declarations.from((prefix, namespace)))

  /** Creates a `String` representation of this `Declarations`, as it is shown in an XML element */
  def toStringInXml: String = {
    val declaredString = properDeclarationsToStringInXml
    val defaultNamespaceUndeclared: Boolean = prefixNamespaceMap.get(DefaultNsPrefix) == Some("")
    val defaultNsUndeclaredString = if (defaultNamespaceUndeclared) """xmlns=""""" else ""
    val undeclaredPrefixes: Set[String] = ((prefixNamespaceMap - DefaultNsPrefix) filter (kv => kv._2 == "")).keySet
    val undeclaredPrefixesString = undeclaredPrefixes map { pref => """xmlns:%s=""""".format(pref) } mkString (" ")

    List(declaredString, defaultNsUndeclaredString, undeclaredPrefixesString) filterNot { _ == "" } mkString (" ")
  }

  private def properDeclarationsToStringInXml: String = {
    val declaredMap = prefixNamespaceMap filter { case (pref, ns) => ns.length > 0 }
    val defaultNsString = if (!declaredMap.contains(DefaultNsPrefix)) "" else """xmlns="%s"""".format(declaredMap(DefaultNsPrefix))
    val prefixScopeString = (declaredMap - DefaultNsPrefix) map { case (pref, ns) => """xmlns:%s="%s"""".format(pref, ns) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }
}

object Declarations {

  private def validate(prefixNamespaceMap: Map[String, String]): Unit = {
    require(prefixNamespaceMap ne null)

    prefixNamespaceMap foreach {
      case (pref, ns) =>
        require(pref ne null, s"No null prefix allowed in declarations $prefixNamespaceMap")
        require(ns ne null, s"No null namespace allowed in declarations $prefixNamespaceMap")
        require(
          !XmlStringUtils.containsColon(pref),
          s"The prefix must not contain any colon in declarations $prefixNamespaceMap")
        require(
          pref != "xmlns",
          s"The prefix must not be 'xmlns' in declarations $prefixNamespaceMap")
        require(
          pref != "xml",
          s"No 'xml' prefix allowed in declarations $prefixNamespaceMap")
        require(
          ns != "http://www.w3.org/2000/xmlns/",
          s"No 'http://www.w3.org/2000/xmlns/' namespace allowed in declarations $prefixNamespaceMap")
        require(
          ns != XmlNamespace,
          s"No 'http://www.w3.org/XML/1998/namespace' namespace allowed in declarations $prefixNamespaceMap")
    }
  }

  /** The "empty" `Declarations` */
  val Empty = Declarations(Map())

  /**
   * Same as the constructor, but removing the 'xml' prefix, if any.
   * Therefore this call is easier to use than the constructor or default `apply` method.
   */
  def from(m: Map[String, String]): Declarations = {
    if (m.contains("xml")) {
      require(
        m("xml") == XmlNamespace,
        "The 'xml' prefix must map to 'http://www.w3.org/XML/1998/namespace'")
    }
    Declarations(m - "xml")
  }

  /** Returns `from(Map[String, String](m: _*))` */
  def from(m: (String, String)*): Declarations = from(Map[String, String](m: _*))

  /** Returns a `Declarations` that contains (only) undeclarations for the given prefixes */
  def undeclaring(prefixes: Set[String]): Declarations = {
    val m = (prefixes map (pref => (pref -> ""))).toMap
    Declarations(m)
  }

  val DefaultNsPrefix = ""

  val XmlNamespace = "http://www.w3.org/XML/1998/namespace"
}
