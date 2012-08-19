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
 * ==Scope more formally==
 *
 * Method `resolve` resolves a `Declarations` against this Scope, returning a new Scope. It could be defined by the following equality:
 * {{{
 * scope.resolve(declarations) == {
 *   val m = (scope.map ++ declarations.withoutUndeclarations.map) -- declarations.retainingUndeclarations.map.keySet
 *   Scope(m)
 * }
 * }}}
 * The actual implementation is a lot more efficient than that, but it is consistent with this definition.
 *
 * Method `relativize` relativizes a Scope against this Scope, returning a `Declarations`. It could be defined by the following equality:
 * {{{
 * scope1.relativize(scope2) == {
 *   val declared = scope2.map filter { case (pref, ns) => scope1.map.getOrElse(pref, "") != ns }
 *   val undeclared = scope1.map.keySet -- scope2.map.keySet
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 * Again, the actual implementation is more efficient than that, but it is consistent with this definition.
 *
 * Methods `relativize` and `resolve` obey the following equality:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)) == scope2
 * }}}
 *
 * This property can be proven easily. After all, for arbitrary Declarations `decl`, we have:
 * {{{
 * scope1.resolve(decl).map == { (scope1.map ++ decl.withoutUndeclarations.map) -- decl.retainingUndeclarations.map.keySet }
 * }}}
 * Here, `decl` stands for `scope1.relativize(scope2)`, so:
 * {{{
 * scope1.resolve(decl).map == {
 *   val properDeclarations = scope2.map filter { case (pref, ns) => scope1.map.getOrElse(pref, "") != ns }
 *   val undeclared = scope1.map.keySet -- scope2.map.keySet
 *   (scope1.map ++ properDeclarations) -- undeclared
 * }
 * }}}
 * so (visualising with Venn diagrams for prefix sets):
 * {{{
 * scope1.resolve(decl).map == {
 *   val properDeclarations = scope2.map filter { case (pref, ns) => scope1.map.getOrElse(pref, "") != ns }
 *   (scope1.map ++ properDeclarations) filter { case (pref, ns) => scope2.map.contains(pref) }
 * }
 * }}}
 * The RHS clearly has as keys the keys of `scope2.map` and the mapped values are also those in `scope2.map`. Hence,
 * {{{
 * scope1.resolve(scope1.relativize(scope2)).map == scope2.map
 * }}}
 * so:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)) == scope2
 * }}}
 *
 * @author Chris de Vreeze
 */
final case class Scope(map: Map[String, String]) extends Immutable {
  import Scope._

  require(map ne null)
  require {
    map.keySet forall { pref => pref ne null }
  }
  require {
    map.values forall { ns => (ns ne null) && (ns != "") && (ns != "http://www.w3.org/2000/xmlns/") }
  }
  require {
    (map - DefaultNsPrefix).keySet forall { pref => XmlStringUtils.isAllowedPrefix(pref) && (pref != "xmlns") }
  }

  /** Returns true if this Scope is empty. Faster than comparing this Scope against the empty Scope. */
  def isEmpty: Boolean = map.isEmpty

  /** Returns the default namespace, if any, wrapped in an Option */
  def defaultNamespaceOption: Option[String] = map.get(DefaultNsPrefix)

  /** Returns an adapted copy of this Scope, but without the default namespace, if any */
  def withoutDefaultNamespace: Scope = if (defaultNamespaceOption.isEmpty) this else Scope(map - DefaultNsPrefix)

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
  def isInvertible: Boolean = map.keySet.size == map.values.toSet.size

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
      // The prefix scope (as Map), with the implicit "xml" namespace added
      val completePrefixScopeMap: Map[String, String] = (map - DefaultNsPrefix) + ("xml" -> "http://www.w3.org/XML/1998/namespace")
      completePrefixScopeMap.get(prefixedName.prefix) map { nsUri => EName(nsUri, prefixedName.localPart) }
  }

  /**
   * Resolves the given declarations against this `Scope`, returning an "updated" `Scope`.
   *
   * Inspired by `java.net.URI`, which has a similar method for URIs.
   */
  def resolve(declarations: Declarations): Scope = {
    if (declarations.isEmpty) this else {
      val declared: Map[String, String] = declarations.map filter { kv => kv._2.length > 0 }

      val undeclarations: Declarations = declarations.retainingUndeclarations
      assert(declared.keySet.intersect(undeclarations.map.keySet).isEmpty)
      val m = (map ++ declared) -- undeclarations.map.keySet
      Scope(m)
    }
  }

  /**
   * Relativizes the given `Scope` against this `Scope`, returning a `Declarations` object.
   *
   * Inspired by `java.net.URI`, which has a similar method for URIs.
   */
  def relativize(scope: Scope): Declarations = {
    if (Scope.this == scope) Declarations.Empty else {
      val newlyDeclared: Map[String, String] = scope.map filter { kv =>
        val pref = kv._1
        val ns = kv._2
        assert(ns.length > 0)
        Scope.this.map.getOrElse(pref, "") != ns
      }

      val removed: Set[String] = Scope.this.map.keySet -- scope.map.keySet
      val undeclarations: Map[String, String] = (removed map (pref => (pref -> ""))).toMap

      assert(newlyDeclared.keySet.intersect(removed).isEmpty)
      val m: Map[String, String] = newlyDeclared ++ undeclarations

      Declarations(m)
    }
  }

  /** Creates a `String` representation of this `Scope`, as it is shown in XML */
  def toStringInXml: String = {
    val defaultNsString = if (defaultNamespaceOption.isEmpty) "" else """xmlns="%s"""".format(defaultNamespaceOption.get)
    val prefixScopeString = (map - DefaultNsPrefix) map { kv => """xmlns:%s="%s"""".format(kv._1, kv._2) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }
}

object Scope {

  /** The "empty" `Scope` */
  val Empty = Scope(Map())

  def from(m: (String, String)*): Scope = Scope(Map[String, String](m: _*))

  val DefaultNsPrefix = ""
}
