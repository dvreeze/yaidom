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
 * Namespace declarations (and undeclarations), typically at the level of one element.
 *
 * The Declarations is backed by a map from prefixes (or the empty string for the default namespace) to namespace URIs (or the empty string).
 * If the mapped value is the empty string, it is an undeclaration.
 *
 * Prefix 'xml' is not allowed as key in this map. That prefix, mapping to namespace URI 'http://www.w3.org/XML/1998/namespace',
 * is always available, without needing any declaration.
 *
 * This class does not depend on Scopes.
 *
 * @author Chris de Vreeze
 */
final case class Declarations(map: Map[String, String]) extends Immutable {
  import Declarations._

  require(map ne null)
  require {
    map.keySet forall { pref => pref ne null }
  }
  require {
    map.values forall { ns => (ns ne null) && (ns != "http://www.w3.org/2000/xmlns/") }
  }
  require {
    (map - DefaultNsPrefix).keySet forall { pref => XmlStringUtils.isAllowedPrefix(pref) && (pref != "xmlns") }
  }
  require(!map.keySet.contains("xml"), "A Declarations must not contain the prefix 'xml'")
  require(
    map.values forall (ns => (ns != "http://www.w3.org/XML/1998/namespace")),
    "A Declarations must not contain namespace URI 'http://www.w3.org/XML/1998/namespace'")

  /** Returns true if this Declarations is empty. Faster than comparing this Declarations against the empty Declarations. */
  def isEmpty: Boolean = map.isEmpty

  /** Returns an adapted copy of this Declarations, but retaining only the undeclarations, if any */
  def retainingUndeclarations: Declarations = {
    val m = map filter { case (pref, ns) => ns == "" }
    if (m.isEmpty) Declarations.Empty else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but without any undeclarations, if any */
  def withoutUndeclarations: Declarations = {
    val m = map filter { case (pref, ns) => ns != "" }
    if (m.size == map.size) this else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but retaining only the default namespace, if any */
  def retainingDefaultNamespace: Declarations = {
    val m = map filter { case (pref, ns) => pref == DefaultNsPrefix }
    if (m.isEmpty) Declarations.Empty else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but without the default namespace, if any */
  def withoutDefaultNamespace: Declarations = {
    if (!map.contains(DefaultNsPrefix)) this else Declarations(map - DefaultNsPrefix)
  }

  /** Returns `Declarations(this.map ++ declarations.map)` */
  def ++(declarations: Declarations): Declarations = Declarations(this.map ++ declarations.map)

  /** Returns `Declarations(this.map -- prefixes)` */
  def --(prefixes: Set[String]): Declarations = Declarations(this.map -- prefixes)

  /** Returns true if this is a sub-declarations of the given parameter `Declarations`. A `Declarations` is considered sub-declarations of itself. */
  def subDeclarationsOf(declarations: Declarations): Boolean = {
    val thisMap = map
    val otherMap = declarations.map

    thisMap.keySet.subsetOf(otherMap.keySet) && {
      thisMap.keySet forall { pref => thisMap(pref) == otherMap(pref) }
    }
  }

  /** Returns true if this is a super-declarations of the given parameter `Declarations`. A `Declarations` is considered super-declarations of itself. */
  def superDeclarationsOf(declarations: Declarations): Boolean = declarations.subDeclarationsOf(this)

  /** Creates a `String` representation of this `Declarations`, as it is shown in an XML element */
  def toStringInXml: String = {
    val declaredString = properDeclarationsToStringInXml
    val defaultNamespaceUndeclared: Boolean = map.get(DefaultNsPrefix) == Some("")
    val defaultNsUndeclaredString = if (defaultNamespaceUndeclared) """xmlns=""""" else ""
    val undeclaredPrefixes: Set[String] = ((map - DefaultNsPrefix) filter (kv => kv._2 == "")).keySet
    val undeclaredPrefixesString = undeclaredPrefixes map { pref => """xmlns:%s=""""".format(pref) } mkString (" ")

    List(declaredString, defaultNsUndeclaredString, undeclaredPrefixesString) filterNot { _ == "" } mkString (" ")
  }

  private def properDeclarationsToStringInXml: String = {
    val declaredMap = map filter { case (pref, ns) => ns.length > 0 }
    val defaultNsString = if (!declaredMap.contains(DefaultNsPrefix)) "" else """xmlns="%s"""".format(declaredMap(DefaultNsPrefix))
    val prefixScopeString = (declaredMap - DefaultNsPrefix) map { case (pref, ns) => """xmlns:%s="%s"""".format(pref, ns) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }
}

object Declarations {

  /** The "empty" `Declarations` */
  val Empty = Declarations(Map())

  /**
   * Same as the constructor, but removing the 'xml' prefix, if any.
   * Therefore this call is easier to use than the constructor or default `apply` method.
   */
  def from(m: Map[String, String]): Declarations = {
    if (m.contains("xml")) {
      require(m("xml") == "http://www.w3.org/XML/1998/namespace",
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
}
