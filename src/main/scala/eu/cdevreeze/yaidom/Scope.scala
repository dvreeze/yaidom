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

import scala.collection.immutable

/**
 * Scope mapping prefixes to namespace URIs, as well as holding an optional default namespace. In other words, <em>in-scope
 * namespaces</em>.
 *
 * The purpose of a [[eu.cdevreeze.yaidom.Scope]] is to resolve [[eu.cdevreeze.yaidom.QName]]s as [[eu.cdevreeze.yaidom.EName]]s.
 *
 * A `Scope` must not contain prefix "xmlns" and must not contain namespace URI "http://www.w3.org/2000/xmlns/".
 * Moreover, a `Scope` must not contain the XML namespace (prefix "xml", namespace URI "http://www.w3.org/XML/1998/namespace").
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
 * The actual implementation may be more efficient than that, but it is consistent with this definition.
 *
 * Method `relativize` relativizes a Scope against this Scope, returning a `Declarations`. It could be defined by the following equality:
 * {{{
 * scope1.relativize(scope2) == {
 *   val declared = scope2.map filter { case (pref, ns) => scope1.map.getOrElse(pref, "") != ns }
 *   val undeclared = scope1.map.keySet -- scope2.map.keySet
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 * Again, the actual implementation may be more efficient than that, but it is consistent with this definition.
 *
 * ===1. Property about relativize and resolve, and its proof===
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
 *   val properDeclarations = scope1.relativize(scope2).withoutUndeclarations.map
 *   val undeclared = scope1.relativize(scope2).retainingUndeclarations.map.keySet
 *   (scope1.map ++ properDeclarations) -- undeclared
 * }
 * }}}
 * so:
 * {{{
 * scope1.resolve(decl).map == {
 *   val properDeclarations = scope2.map filter { case (pref, ns) => scope1.map.getOrElse(pref, "") != ns }
 *   val undeclared = scope1.map.keySet -- scope2.map.keySet
 *
 *   assert((scope1.map ++ properDeclarations).keySet == scope1.map.keySet.union(scope2.map.keySet))
 *   (scope1.map ++ properDeclarations) -- undeclared
 * }
 * }}}
 * so (visualising with Venn diagrams for prefix sets):
 * {{{
 * scope1.resolve(decl).map == {
 *   val properDeclarations = scope2.map filter { case (pref, ns) => scope1.map.getOrElse(pref, "") != ns }
 *   (scope1.map ++ properDeclarations) filterKeys (scope2.map.keySet)
 * }
 * }}}
 * The RHS clearly has as keys the keys of `scope2.map` and the mapped values (per key) are also those found in `scope2.map`. Hence,
 * {{{
 * scope1.resolve(scope1.relativize(scope2)).map == scope2.map
 * }}}
 * so:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)) == scope2
 * }}}
 *
 * ===2. Another property about relativize and resolve, and its proof===
 *
 * Methods `relativize` and `resolve` also obey the following equality:
 * {{{
 * scope.relativize(scope.resolve(declarations)) == scope.minimized(declarations)
 * }}}
 * where `scope.minimized(declarations)` is defined by the following equality:
 * {{{
 * scope.minimized(declarations) == {
 *   val declared = declarations.withoutUndeclarations.map filter { case (pref, ns) => scope.map.getOrElse(pref, "") != ns }
 *   val undeclared = declarations.retainingUndeclarations.map.keySet.intersect(scope.map.keySet)
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 *
 * Below follows a proof of this property. For arbitrary Scope `sc`, we have:
 * {{{
 * scope.relativize(sc) == {
 *   val declared = sc.map filter { case (pref, ns) => scope.map.getOrElse(pref, "") != ns }
 *   val undeclared = scope.map.keySet -- sc.map.keySet
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 * Here, `sc` stands for `scope.resolve(declarations)`, so:
 * {{{
 * scope.relativize(sc) == {
 *   val declared = scope.resolve(declarations).map filter { case (pref, ns) => scope.map.getOrElse(pref, "") != ns }
 *   val undeclared = scope.map.keySet -- scope.resolve(declarations).map.keySet
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 * so:
 * {{{
 * scope.relativize(sc) == {
 *   val newScope = Scope((scope.map ++ declarations.withoutUndeclarations.map) -- declarations.retainingUndeclarations.map.keySet)
 *   val declared = newScope.map filter { case (pref, ns) => scope.map.getOrElse(pref, "") != ns }
 *   val undeclared = scope.map.keySet -- newScope.map.keySet
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 * so:
 * {{{
 * scope.relativize(sc) == {
 *   val declared = declarations.withoutUndeclarations.map filter { case (pref, ns) => scope.map.getOrElse(pref, "") != ns }
 *   val undeclared = declarations.retainingUndeclarations.map.keySet.intersect(scope.map.keySet)
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 * so, as a result:
 * {{{
 * scope.relativize(scope.resolve(declarations)) == scope.minimized(declarations)
 * }}}
 *
 * This and the preceding (proven) property are analogous to corresponding properties in the `URI` class.
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
  require(!map.keySet.contains("xml"), "A Scope must not contain the prefix 'xml'")
  require(
    map.values forall (ns => (ns != "http://www.w3.org/XML/1998/namespace")),
    "A Scope must not contain namespace URI 'http://www.w3.org/XML/1998/namespace'")

  /** Returns true if this Scope is empty. Faster than comparing this Scope against the empty Scope. */
  def isEmpty: Boolean = map.isEmpty

  /** Returns the default namespace, if any, wrapped in an Option */
  def defaultNamespaceOption: Option[String] = map.get(DefaultNsPrefix)

  /** Returns an adapted copy of this Scope, but retaining only the default namespace, if any */
  def retainingDefaultNamespace: Scope = {
    val m = map filter { case (pref, ns) => pref == DefaultNsPrefix }
    if (m.isEmpty) Scope.Empty else Scope(m)
  }

  /** Returns an adapted copy of this Scope, but without the default namespace, if any */
  def withoutDefaultNamespace: Scope = if (defaultNamespaceOption.isEmpty) this else Scope(map - DefaultNsPrefix)

  /**
   * Returns true if the inverse exists, that is, each namespace URI has a unique prefix
   * (including the empty prefix for the default namespace, if applicable).
   *
   * In other words, returns true if the inverse of `toMap` is also a mathematical function, mapping namespace URIs to unique prefixes.
   *
   * Invertible scopes offer a one-to-one correspondence between QNames and ENames. This is needed, for example, for `ElemPath`s.
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

  /** Returns `Scope.from(this.map.filter(p))`. */
  def filter(p: ((String, String)) => Boolean): Scope = Scope.from(this.map.filter(p))

  /** Returns `Scope.from(this.map.filterKeys(p))`. */
  def filterKeys(p: String => Boolean): Scope = Scope.from(this.map.filterKeys(p))

  /** Returns `this.map.keySet`. */
  def keySet: Set[String] = this.map.keySet

  /**
   * Tries to resolve the given `QName` against this `Scope`, returning `None` for prefixed names whose prefixes are unknown
   * to this `Scope`.
   *
   * Note that the `subScopeOf` relation keeps the `resolveQNameOption` result the same, provided there is no default namespace.
   * That is, if `scope1.withoutDefaultNamespace.subScopeOf(scope2.withoutDefaultNamespace)`, then for each QName `qname`
   * such that `scope1.withoutDefaultNamespace.resolveQNameOption(qname).isDefined`, we have:
   * {{{
   * scope1.withoutDefaultNamespace.resolveQNameOption(qname) == scope2.withoutDefaultNamespace.resolveQNameOption(qname)
   * }}}
   */
  def resolveQNameOption(qname: QName): Option[EName] = qname match {
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
      val declared: Declarations = declarations.withoutUndeclarations
      val undeclarations: Declarations = declarations.retainingUndeclarations

      assert(declared.map.keySet.intersect(undeclarations.map.keySet).isEmpty)
      val m = (map ++ declared.map) -- undeclarations.map.keySet
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
      val newlyDeclared: Map[String, String] = scope.map filter {
        case (pref, ns) =>
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

  /**
   * Returns the smallest sub-declarations `decl` of `declarations` such that `this.resolve(decl) == this.resolve(declarations)`
   */
  def minimized(declarations: Declarations): Declarations = {
    val declared = declarations.withoutUndeclarations.map filter { case (pref, ns) => this.map.getOrElse(pref, "") != ns }
    val undeclared = declarations.retainingUndeclarations.map.keySet.intersect(this.map.keySet)

    val result = Declarations(declared) ++ Declarations.undeclaring(undeclared)

    assert(this.resolve(declarations) == this.resolve(result))
    result
  }

  /**
   * Returns `scope.retainingDefaultNamespace ++ this.withoutDefaultNamespace.notUndeclaring(scope.withoutDefaultNamespace)`.
   *
   * Note that for each QName `qname` for which `scope.resolveQNameOption(qname).isDefined`, we have:
   * {{{
   * scope.resolveQNameOption(qname) == this.notUndeclaringPrefixes(scope).resolveQNameOption(qname)
   * }}}
   * and that:
   * {{{
   * scope.subScopeOf(this.notUndeclaringPrefixes(scope))
   * }}}
   *
   * This property is handy when adding child elements to a parent `Elem`. By invoking this method (recursively) for the descendant
   * elements, against the parent `Scope`, we can create `Elem` trees without any unnecessary undeclarations
   * (which are implicit, of course, because `Elem`s contain Scopes, not Declarations). There is indeed a corresponding method
   * in class `Elem`, which does just that.
   */
  def notUndeclaringPrefixes(scope: Scope): Scope = {
    val onlyDefault = scope.retainingDefaultNamespace
    val withoutDefault = this.withoutDefaultNamespace.notUndeclaring(scope.withoutDefaultNamespace)
    val result = onlyDefault ++ withoutDefault

    assert(scope.subScopeOf(result))
    assert(scope.defaultNamespaceOption == result.defaultNamespaceOption)
    result
  }

  /** Returns `Scope(this.map ++ scope.map)` */
  def ++(scope: Scope): Scope = Scope(this.map ++ scope.map)

  /** Returns `Scope(this.map -- prefixes)` */
  def --(prefixes: Set[String]): Scope = Scope(this.map -- prefixes)

  /** Creates a `String` representation of this `Scope`, as it is shown in XML */
  def toStringInXml: String = {
    val defaultNsString = if (defaultNamespaceOption.isEmpty) "" else """xmlns="%s"""".format(defaultNamespaceOption.get)
    val prefixScopeString = (map - DefaultNsPrefix) map { case (pref, ns) => """xmlns:%s="%s"""".format(pref, ns) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }

  /**
   * Returns `this.resolve(this.relativize(scope).withoutUndeclarations)`. In other words, returns the minimal superscope `sc` of
   * `scope` such that `this.relativize(sc)` contains no namespace undeclarations.
   *
   * Note that:
   * {{{
   * scope.subScopeOf(this.notUndeclaring(scope))
   * }}}
   * This does not necessarily mean that `scope.resolveQNameOption(qname) == this.notUndeclaring(scope).resolveQNameOption(qname)`.
   * After all, for any unqualified name `qname`, if this scope has a default namespace but the passed scope does not, the LHS
   * uses no namespace to resolve `qname`, whereas the RHS uses the default namespace.
   */
  def notUndeclaring(scope: Scope): Scope = {
    val decls = this.relativize(scope)
    val result = this.resolve(decls.withoutUndeclarations)

    assert(scope.subScopeOf(result))
    assert(this.relativize(result).retainingUndeclarations.isEmpty)
    result
  }

  /**
   * Returns the inverse of this Scope, as Map from namespace URIs to collections of prefixes. These prefixes also include
   * the empty String if this Scope has a default namespace.
   */
  def inverse: Map[String, Set[String]] = {
    val nsPrefixPairs = this.map.toSeq map { case (prefix, ns) => (ns, prefix) }
    val nsPrefixPairsGroupedByNs = nsPrefixPairs groupBy { case (ns, prefix) => ns }

    val result = nsPrefixPairsGroupedByNs mapValues { xs =>
      val result = xs map { case (ns, prefix) => prefix }
      result.toSet
    }

    assert(result.values forall (prefixes => !prefixes.isEmpty))
    result
  }

  /**
   * Returns a prefix mapping to the given namespace URI, if any, wrapped in an `Option`. If multiple prefixes map to
   * the given namespace URI, it is undetermined which of the prefixes is returned.
   *
   * This method can be handy when "inserting" an "element" into a parent tree, if one wants to reuse prefixes of the
   * parent tree.
   */
  def prefixOption(namespaceUri: String): Option[String] = {
    this.map collectFirst { case (pref, ns) if (pref != DefaultNsPrefix) && (ns == namespaceUri) => pref }
  }
}

object Scope {

  /** The "empty" `Scope` */
  val Empty = Scope(Map())

  /**
   * Same as the constructor, but removing the 'xml' prefix, if any.
   * Therefore this call is easier to use than the constructor or default `apply` method.
   */
  def from(m: Map[String, String]): Scope = {
    if (m.contains("xml")) {
      require(m("xml") == "http://www.w3.org/XML/1998/namespace",
        "The 'xml' prefix must map to 'http://www.w3.org/XML/1998/namespace'")
    }
    Scope(m - "xml")
  }

  /** Returns `from(Map[String, String](m: _*))` */
  def from(m: (String, String)*): Scope = from(Map[String, String](m: _*))

  val DefaultNsPrefix = ""
}
