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

  /** Returns true if this Declarations is empty. Faster than comparing this Declarations against the empty Declarations. */
  def isEmpty: Boolean = map.isEmpty

  /** Returns an adapted copy of this Declarations, but retaining only the undeclarations, if any */
  def retainingUndeclarations: Declarations = {
    val m = map filter { kv => kv._2 == "" }
    if (m.isEmpty) Declarations.Empty else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but without any undeclarations, if any */
  def withoutUndeclarations: Declarations = {
    val m = map filter { kv => kv._2 != "" }
    if (m.size == map.size) this else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but retaining only the default namespace, if any */
  def retainingDefaultNamespace: Declarations = {
    val m = map filter { kv => kv._1 == DefaultNsPrefix }
    if (m.isEmpty) Declarations.Empty else Declarations(m)
  }

  /** Returns an adapted copy of this Declarations, but without the default namespace, if any */
  def withoutDefaultNamespace: Declarations = {
    if (!map.contains(DefaultNsPrefix)) this else Declarations(map - DefaultNsPrefix)
  }

  /** Returns `Declarations(this.map ++ declarations.map)` */
  def ++(declarations: Declarations): Declarations = Declarations(this.map ++ declarations.map)

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
    val declaredMap = map filter { kv => kv._2.length > 0 }
    val defaultNsString = if (!declaredMap.contains(DefaultNsPrefix)) "" else """xmlns="%s"""".format(declaredMap(DefaultNsPrefix))
    val prefixScopeString = (declaredMap - DefaultNsPrefix) map { kv => """xmlns:%s="%s"""".format(kv._1, kv._2) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }
}

object Declarations {

  /** The "empty" `Declarations` */
  val Empty = Declarations(Map())

  def from(m: (String, String)*): Declarations = Declarations(Map[String, String](m: _*))

  /** Returns a `Declarations` that contains (only) undeclarations for the given prefixes */
  def undeclaring(prefixes: Set[String]): Declarations = {
    val m = (prefixes map (pref => (pref -> ""))).toMap
    Declarations(m)
  }

  val DefaultNsPrefix = ""
}
