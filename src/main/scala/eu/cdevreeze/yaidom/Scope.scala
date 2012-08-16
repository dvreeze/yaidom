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
 * Scope mapping prefixes to namespace URIs, as well as holding an optional default namespace.
 *
 * The purpose of a [[eu.cdevreeze.yaidom.Scope]] is to resolve [[eu.cdevreeze.yaidom.QName]]s as [[eu.cdevreeze.yaidom.EName]]s.
 *
 * A `Scope` must not contain prefix "xmlns" and must not contain namespace URI "http://www.w3.org/2000/xmlns/".
 *
 * The Scope is backed by a map from prefixes (or the empty string for the default namespace) to (non-empty) namespace URIs.
 * 
 * This class depends on Declarations, but not the other way around.
 *
 * @author Chris de Vreeze
 */
final case class Scope(map: Map[String, String]) extends Immutable {
  require(map ne null)
  require {
    map.keySet forall { pref => pref ne null }
  }
  require {
    map.values forall { ns => (ns ne null) && (ns != "") && (ns != "http://www.w3.org/2000/xmlns/") }
  }
  require {
    prefixScope.keySet forall { pref => XmlStringUtils.isAllowedPrefix(pref) && (pref != "xmlns") }
  }

  def defaultNamespaceOption: Option[String] = map.get("")

  def prefixScope: Map[String, String] = (map - "")

  /** The prefix scope, with the implicit "xml" namespace added */
  def completePrefixScope: Map[String, String] = prefixScope + ("xml" -> "http://www.w3.org/XML/1998/namespace")

  /**
   * Returns true if the inverse exists, that is, each namespace URI has a unique prefix
   * (including the empty prefix for the default namespace, if applicable).
   *
   * In other words, returns true if the inverse of `toMap` is also a mathematical function, mapping namespace URIs to unique prefixes.
   *
   * Invertible scopes offer a one-to-one correspondence between QNames and ENames. This is needed, for example, for `ElemPaths`s.
   * Only if there is such a one-to-one correspondence, the indexes in `ElemPath`s and `ElemPathBuilder`s are stable, when converting
   * between the two.
   */
  def isInvertible: Boolean = {
    map.keySet.size == map.values.toSet.size
  }

  /** Returns true if this is a subscope of the given parameter `Scope`. A `Scope` is considered subscope of itself. */
  def subScopeOf(scope: Scope): Boolean = {
    val thisMap = map
    val otherMap = scope.map

    thisMap.keySet.subsetOf(otherMap.keySet) && {
      thisMap.keySet forall { pref => thisMap(pref) == otherMap(pref) }
    }
  }

  /** Returns true if this is a superscope of the given parameter `Scope`. A `Scope` is considered superscope of itself. */
  def superScopeOf(scope: Scope): Boolean = scope.subScopeOf(this)

  /** Tries to resolve the given `QName` against this `Scope`, returning `None` otherwise */
  def resolveQName(qname: QName): Option[EName] = qname match {
    case unprefixedName: UnprefixedName if defaultNamespaceOption.isEmpty => Some(EName(unprefixedName.localPart))
    case unprefixedName: UnprefixedName => Some(EName(defaultNamespaceOption.get, unprefixedName.localPart))
    case prefixedName: PrefixedName =>
      completePrefixScope.get(prefixedName.prefix) map { nsUri => EName(nsUri, prefixedName.localPart) }
  }

  /**
   * Resolves the given declarations against this `Scope`, returning an "updated" `Scope`.
   *
   * Inspired by `java.net.URI`, which has a similar method for URIs.
   */
  def resolve(declarations: Declarations): Scope = {
    if (declarations == Declarations.Empty) this else {
      val m = (map ++ declarations.declared) -- declarations.undeclaredSet
      Scope(m)
    }
  }

  /**
   * Relativizes the given `Scope` against this `Scope`, returning a `Declarations` object.
   *
   * Inspired by `java.net.URI`, which has a similar method for URIs.
   */
  def relativize(scope: Scope): Declarations = {
    val declared: Scope = {
      val defaultNs: Option[String] = (Scope.this.defaultNamespaceOption, scope.defaultNamespaceOption) match {
        case (None, _) => scope.defaultNamespaceOption
        case (_, None) => None
        case (someNs1, someNs2) if someNs1.get == someNs2.get => None
        case (_, _) => scope.defaultNamespaceOption
      }
      val prefixScope: Map[String, String] = scope.prefixScope collect {
        case (pref, nsUri) if (!Scope.this.prefixScope.contains(pref)) || (Scope.this.prefixScope(pref) != scope.prefixScope(pref)) => (pref -> nsUri)
      }

      val defaultNsMap = if (defaultNs.isEmpty) Map() else Map("" -> defaultNs.get)
      Scope(defaultNsMap ++ prefixScope)
    }
    val defaultNamespaceUndeclared: Boolean = this.defaultNamespaceOption.isDefined && scope.defaultNamespaceOption.isEmpty
    val undeclaredPrefixes: Set[String] = Scope.this.prefixScope.keySet.diff(scope.prefixScope.keySet)
    val undeclaredOptionalPrefixes = {
      undeclaredPrefixes map { pref => Some(pref) }
    } ++ (if (defaultNamespaceUndeclared) Set(None) else Set())

    val undeclaredMap: Map[String, String] =
      (undeclaredOptionalPrefixes map (prefOption => if (prefOption.isEmpty) "" -> "" else prefOption.get -> "")).toMap
    Declarations(declared.map ++ undeclaredMap)
  }

  /** Creates a `String` representation of this `Scope`, as it is shown in XML */
  def toStringInXml: String = {
    val defaultNsString = if (defaultNamespaceOption.isEmpty) "" else """xmlns="%s"""".format(defaultNamespaceOption.get)
    val prefixScopeString = prefixScope map { kv => """xmlns:%s="%s"""".format(kv._1, kv._2) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }
}

object Scope {

  /** The "empty" `Scope` */
  val Empty = Scope(Map())

  def from(m: (String, String)*): Scope = Scope(Map[String, String](m: _*))
}
