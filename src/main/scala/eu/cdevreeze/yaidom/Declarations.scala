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
 */
final case class Declarations(declared: Scope, undeclaredOptionalPrefixes: Set[Option[String]]) extends Immutable {
  require(declared ne null)
  require(undeclaredOptionalPrefixes ne null)
  require {
    undeclaredOptionalPrefixes forall { prefOption =>
      (prefOption ne null) && {
        prefOption forall { pref => XmlStringUtils.isAllowedPrefix(pref) && (pref != "xmlns") }
      }
    }
  }
  require(declared.toMap.keySet.intersect(undeclaredSet).isEmpty)
  require(!defaultNamespaceUndeclared || declared.defaultNamespaceOption.isEmpty)

  /** Convenience constructor if there are no undeclarations */
  def this(declared: Scope) = this(declared, Set())

  def defaultNamespaceUndeclared: Boolean = undeclaredOptionalPrefixes.contains(None)

  def undeclaredPrefixes: Set[String] = undeclaredOptionalPrefixes collect { case prefOption if prefOption.isDefined => prefOption.get }

  /** Returns the `Set` of undeclared prefixes, with an undeclared default namespace represented by the empty `String` */
  def undeclaredSet: Set[String] = defaultNamespaceUndeclared match {
    case false => undeclaredPrefixes
    case true => undeclaredPrefixes + ""
  }

  /** Creates a `Map` from prefixes to (possibly "undeclared") namespaces URIs, giving the default namespace the empty `String` as prefix */
  def toMap: Map[String, String] = {
    val undeclaredMap: Map[String, String] = {
      val result = undeclaredOptionalPrefixes map { prefixOption => if (prefixOption.isEmpty) ("" -> "") else (prefixOption.get -> "") }
      result.toMap
    }
    val declaredMap: Map[String, String] = declared.toMap
    undeclaredMap ++ declaredMap
  }

  /** Creates a `String` representation of this `Declarations`, as it is shown in an XML element */
  def toStringInXml: String = {
    val declaredString = declared.toStringInXml
    val defaultNsUndeclaredString = if (defaultNamespaceUndeclared) """xmlns=""""" else ""
    val undeclaredPrefixesString = undeclaredPrefixes map { pref => """xmlns:%s=""""".format(pref) } mkString (" ")

    List(declaredString, defaultNsUndeclaredString, undeclaredPrefixesString) filterNot { _ == "" } mkString (" ")
  }
}

object Declarations {

  /** The "empty" `Declarations` */
  val Empty = Declarations(declared = Scope.Empty, undeclaredOptionalPrefixes = Set())

  def from(m: (String, String)*): Declarations = fromMap(Map[String, String](m: _*))

  def fromMap(m: Map[String, String]): Declarations = {
    require {
      m.keySet forall { pref => pref ne null }
    }
    require {
      m.values forall { ns => ns ne null }
    }

    val scope = {
      val scopeMap = m filter { kv => !kv._2.isEmpty }
      Scope.fromMap(scopeMap)
    }
    val defaultNamespaceUndeclared: Boolean = {
      val defaultNs = m.get("")
      defaultNs == Some("")
    }
    val undeclaredPrefixes = {
      val result = m filterKeys { pref => pref != "" } filter { kv => kv._2 == "" }
      result.keySet
    }
    val undeclaredOptionalPrefixes = {
      undeclaredPrefixes map { pref => Some(pref) }
    } ++ (if (defaultNamespaceUndeclared) Set(None) else Set())

    Declarations(scope, undeclaredOptionalPrefixes)
  }
}
