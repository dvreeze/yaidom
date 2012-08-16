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
  require(map ne null)
  require {
    map.keySet forall { pref => pref ne null }
  }
  require {
    map.values forall { ns => (ns ne null) && (ns != "http://www.w3.org/2000/xmlns/") }
  }
  require {
    (map - "").keySet forall { pref => XmlStringUtils.isAllowedPrefix(pref) && (pref != "xmlns") }
  }

  def declared: Map[String, String] = map filter { kv => kv._2.length > 0 }

  def undeclaredOptionalPrefixes: Set[Option[String]] = {
    val result = map filter { kv => kv._2.length == 0 } map { kv => val pref = kv._1; if (pref.length == 0) None else Some(pref) }
    result.toSet
  }

  def defaultNamespaceUndeclared: Boolean = undeclaredOptionalPrefixes.contains(None)

  def undeclaredPrefixes: Set[String] = undeclaredOptionalPrefixes collect { case prefOption if prefOption.isDefined => prefOption.get }

  /** Returns the `Set` of undeclared prefixes, with an undeclared default namespace represented by the empty `String` */
  def undeclaredSet: Set[String] = defaultNamespaceUndeclared match {
    case false => undeclaredPrefixes
    case true => undeclaredPrefixes + ""
  }

  /** Creates a `String` representation of this `Declarations`, as it is shown in an XML element */
  def toStringInXml: String = {
    val declaredString = properDeclarationsToStringInXml
    val defaultNsUndeclaredString = if (defaultNamespaceUndeclared) """xmlns=""""" else ""
    val undeclaredPrefixesString = undeclaredPrefixes map { pref => """xmlns:%s=""""".format(pref) } mkString (" ")

    List(declaredString, defaultNsUndeclaredString, undeclaredPrefixesString) filterNot { _ == "" } mkString (" ")
  }

  private def properDeclarationsToStringInXml: String = {
    val declaredMap = declared
    val defaultNsString = if (!declaredMap.contains("")) "" else """xmlns="%s"""".format(declaredMap(""))
    val prefixScopeString = (declaredMap - "") map { kv => """xmlns:%s="%s"""".format(kv._1, kv._2) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }
}

object Declarations {

  /** The "empty" `Declarations` */
  val Empty = Declarations(Map())

  def from(m: (String, String)*): Declarations = Declarations(Map[String, String](m: _*))
}
