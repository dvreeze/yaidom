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

import Scope.DefaultNsPrefix
import eu.cdevreeze.yaidom.XmlStringUtils

/**
 * Scope mapping prefixes to namespace URIs, as well as holding an optional default namespace. In other words, <em>in-scope
 * namespaces</em>.
 *
 * The purpose of a [[eu.cdevreeze.yaidom.core.Scope]] is to resolve [[eu.cdevreeze.yaidom.core.QName]]s as [[eu.cdevreeze.yaidom.core.EName]]s.
 *
 * For example, consider the following XML:
 * {{{
 * <book:Bookstore xmlns:book="http://bookstore/book">
 *   <book:Book ISBN="978-0321356680" Price="35" Edition="2">
 *     <book:Title>Effective Java (2nd Edition)</book:Title>
 *     <book:Authors>
 *       <auth:Author xmlns:auth="http://bookstore/author">
 *         <auth:First_Name>Joshua</auth:First_Name>
 *         <auth:Last_Name>Bloch</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 * </book:Bookstore>
 * }}}
 * Then the (only) author element has the following scope:
 * {{{
 * Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author")
 * }}}
 *
 * After all, the root element has the following scope:
 * {{{
 * Scope.Empty.resolve(Declarations.from("book" -> "http://bookstore/book"))
 * }}}
 * which is the same as:
 * {{{
 * Scope.from("book" -> "http://bookstore/book")
 * }}}
 *
 * The (only) book element has no namespace declarations, so it has the same scope. That is also true for the authors element
 * inside the book element. The (only) author element introduces a new namespace, and its scope is as follows:
 * {{{
 * Scope.from("book" -> "http://bookstore/book").resolve(Declarations.from("auth" -> "http://bookstore/author"))
 * }}}
 * which is indeed:
 * {{{
 * Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author")
 * }}}
 *
 * The author element `QName("auth:Author")` has (optional) resolved name:
 * {{{
 * Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author").resolveQNameOption(QName("auth:Author"))
 * }}}
 * which is:
 * {{{
 * Some(EName("{http://bookstore/author}Author"))
 * }}}
 *
 * A `Scope` must not contain prefix "xmlns" and must not contain namespace URI "http://www.w3.org/2000/xmlns/".
 * Moreover, a `Scope` must not contain the XML namespace (prefix "xml", namespace URI "http://www.w3.org/XML/1998/namespace").
 *
 * The Scope is backed by a map from prefixes (or the empty string for the default namespace) to (non-empty) namespace URIs.
 *
 * This class depends on Declarations, but not the other way around.
 *
 * ==Concise querying using QNames which are converted to ENames==
 *
 * EName-based querying is more robust than QName-based querying, but ENames are more verbose than QNames. We can get the
 * best of both ENames and QNames by querying using (concise) QNames, and by using a Scope converting them to (stable) ENames.
 *
 * For example:
 * {{{
 * val scope = Scope.from("xs" -> "http://www.w3.org/2001/XMLSchema")
 * import scope._
 *
 * val elemDecls = schemaElem \\ withEName(QName("xs", "element").res)
 * }}}
 *
 * This is exactly equivalent to the following query:
 * {{{
 * import HasENameApi._
 *
 * val elemDecls = schemaElem \\ withEName("http://www.w3.org/2001/XMLSchema", "element")
 * }}}
 *
 * ==Scope more formally==
 *
 * '''In order to get started using the class, this more formal section can safely be skipped. On the other hand, this section
 * may provide a deeper understanding of the class.'''
 *
 * Method `resolve` resolves a `Declarations` against this Scope, returning a new Scope. It could be defined by the following equality:
 * {{{
 * scope.resolve(declarations) == {
 *   val m = (scope.prefixNamespaceMap ++ declarations.withoutUndeclarations.prefixNamespaceMap) --
 *     declarations.retainingUndeclarations.prefixNamespaceMap.keySet
 *   Scope(m)
 * }
 * }}}
 * The actual implementation may be more efficient than that, but it is consistent with this definition.
 *
 * Method `relativize` relativizes a Scope against this Scope, returning a `Declarations`. It could be defined by the following equality:
 * {{{
 * scope1.relativize(scope2) == {
 *   val declared = scope2.prefixNamespaceMap filter { case (pref, ns) => scope1.prefixNamespaceMap.getOrElse(pref, "") != ns }
 *   val undeclared = scope1.prefixNamespaceMap.keySet -- scope2.prefixNamespaceMap.keySet
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 * Again, the actual implementation may be more efficient than that, but it is consistent with this definition.
 *
 * ===1. Property about two Scopes, and its proof===
 *
 * Methods `relativize` and `resolve` obey the following equality:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)) == scope2
 * }}}
 *
 * Below follows the proof. We distinguish among the following cases:
 * <ul>
 * <li>Prefix `p` has the same mappings in `scope1` and `scope2`</li>
 * <li>Prefix `p` has different mappings in `scope1` and `scope2`</li>
 * <li>Prefix `p` only belongs to `scope1`</li>
 * <li>Prefix `p` only belongs to `scope2`</li>
 * <li>Prefix `p` belongs to neither scope</li>
 * </ul>
 * Prefix `p` can be the empty string, for the default namespace. For each of these cases, we prove that:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p) == scope2.prefixNamespaceMap.get(p)
 * }}}
 * Since there are no other cases, that would complete the proof.
 *
 * If prefix `p` has the same mappings in both scopes, then:
 * {{{
 * scope1.relativize(scope2).prefixNamespaceMap.get(p).isEmpty
 * }}}
 * so the following equalities hold:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap(p)
 * scope1.prefixNamespaceMap(p)
 * scope2.prefixNamespaceMap(p)
 * }}}
 * so:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p) == scope2.prefixNamespaceMap.get(p)
 * }}}
 *
 * If prefix `p` has different mappings in both scopes, then:
 * {{{
 * scope1.relativize(scope2).prefixNamespaceMap(p) == scope2.prefixNamespaceMap(p)
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap(p) == scope2.prefixNamespaceMap(p)
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p) == scope2.prefixNamespaceMap.get(p)
 * }}}
 *
 * If prefix `p` only belongs to `scope1`, then:
 * {{{
 * scope1.relativize(scope2).prefixNamespaceMap(p) == "" // undeclaration
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p).isEmpty
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p) == scope2.prefixNamespaceMap.get(p) // both empty
 * }}}
 *
 * if prefix `p` only belongs to `scope2`, then:
 * {{{
 * scope1.relativize(scope2).prefixNamespaceMap(p) == scope2.prefixNamespaceMap(p)
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap(p) == scope2.prefixNamespaceMap(p)
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p) == scope2.prefixNamespaceMap.get(p)
 * }}}
 *
 * if prefix `p` belongs to neither scope, then obviously:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p).isEmpty
 * scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(p) == scope2.prefixNamespaceMap.get(p) // both empty
 * }}}
 *
 * ===2. Property about Scope and Declarations===
 *
 * Methods `relativize` and `resolve` also obey the following equality:
 * {{{
 * scope.relativize(scope.resolve(declarations)) == scope.minimize(declarations)
 * }}}
 * where `scope.minimize(declarations)` is defined by the following equality:
 * {{{
 * scope.minimize(declarations) == {
 *   val declared = declarations.withoutUndeclarations.prefixNamespaceMap filter { case (pref, ns) => scope.prefixNamespaceMap.getOrElse(pref, "") != ns }
 *   val undeclared = declarations.retainingUndeclarations.prefixNamespaceMap.keySet.intersect(scope.prefixNamespaceMap.keySet)
 *   Declarations(declared) ++ Declarations.undeclaring(undeclared)
 * }
 * }}}
 *
 * It can be proven by distinguishing among the following cases:
 * <ul>
 * <li>Prefix `p` has the same mappings in `scope` and `declarations` (so no undeclaration)</li>
 * <li>Prefix `p` has different mappings in `scope` and `declarations` (but no undeclaration)</li>
 * <li>Prefix `p` belongs to `scope` and is undeclared in `declarations`</li>
 * <li>Prefix `p` only belongs to `scope`, and does not occur in `declarations`</li>
 * <li>Prefix `p` only occurs in `declarations`, without being undeclared, and does not occur in `scope`</li>
 * <li>Prefix `p` only occurs in `declarations`,  in an undeclaration, and does not occur in `scope`</li>
 * <li>Prefix `p` neither occurs in `scope` nor in `declarations`</li>
 * </ul>
 * Prefix `p` can be the empty string, for the default namespace. For each of these cases, it can be proven that:
 * {{{
 * scope.relativize(scope.resolve(declarations)).prefixNamespaceMap.get(p) == scope.minimize(declarations).prefixNamespaceMap.get(p)
 * }}}
 * Since there are no other cases, that would complete the proof. The proof itself is left as an exercise for the reader, as
 * they say.
 *
 * This and the preceding (proven) property are analogous to corresponding properties in the `URI` class.
 *
 * @author Chris de Vreeze
 */
final case class Scope(prefixNamespaceMap: Map[String, String]) {
  import Scope._

  validate(prefixNamespaceMap)

  /** Returns true if this Scope is empty. Faster than comparing this Scope against the empty Scope. */
  def isEmpty: Boolean = prefixNamespaceMap.isEmpty

  /** Returns true if this Scope is not empty. */
  def nonEmpty: Boolean = !isEmpty

  /** Returns the default namespace, if any, wrapped in an Option */
  def defaultNamespaceOption: Option[String] = prefixNamespaceMap.get(DefaultNsPrefix)

  /** Returns an adapted copy of this Scope, but retaining only the default namespace, if any */
  def retainingDefaultNamespace: Scope = {
    val m = prefixNamespaceMap filter { case (pref, ns) => pref == DefaultNsPrefix }
    if (m.isEmpty) Scope.Empty else Scope(m)
  }

  /** Returns an adapted copy of this Scope, but without the default namespace, if any */
  def withoutDefaultNamespace: Scope = if (defaultNamespaceOption.isEmpty) this else Scope(prefixNamespaceMap - DefaultNsPrefix)

  /**
   * Returns true if the inverse exists, that is, each namespace URI has a unique prefix
   * (including the empty prefix for the default namespace, if applicable).
   *
   * In other words, returns true if the inverse of `toMap` is also a mathematical function, mapping namespace URIs to unique prefixes.
   *
   * Invertible scopes offer a one-to-one correspondence between QNames and ENames. This is needed, for example, for `Path`s.
   * Only if there is such a one-to-one correspondence, the indexes in `Path`s and `PathBuilder`s are stable, when converting
   * between the two.
   */
  def isInvertible: Boolean = prefixNamespaceMap.keySet.size == namespaces.size

  /** Returns true if this is a subscope of the given parameter `Scope`. A `Scope` is considered subscope of itself. */
  def subScopeOf(scope: Scope): Boolean = {
    val thisMap = prefixNamespaceMap
    val otherMap = scope.prefixNamespaceMap

    thisMap.keySet.subsetOf(otherMap.keySet) && {
      thisMap.keySet forall { pref => thisMap(pref) == otherMap(pref) }
    }
  }

  /** Returns true if this is a superscope of the given parameter `Scope`. A `Scope` is considered superscope of itself. */
  def superScopeOf(scope: Scope): Boolean = scope.subScopeOf(this)

  /** Returns `Scope.from(this.prefixNamespaceMap.filter(p))`. */
  def filter(p: ((String, String)) => Boolean): Scope = Scope.from(this.prefixNamespaceMap.filter(p))

  /** Returns `Scope.from(this.prefixNamespaceMap.filterKeys(p))`. */
  def filterKeys(p: String => Boolean): Scope = Scope.from(this.prefixNamespaceMap.filterKeys(p).toMap)

  /** Returns `this.prefixNamespaceMap.keySet`. */
  def keySet: Set[String] = this.prefixNamespaceMap.keySet

  /** Returns `this.prefixNamespaceMap.values.toSet`. Hence, the "XML namespace" is not returned. */
  def namespaces: Set[String] = this.prefixNamespaceMap.values.toSet

  /** Returns `filter(kv => p(kv._2))`. */
  def filterNamespaces(p: String => Boolean): Scope = {
    filter(kv => p(kv._2))
  }

  /**
   * Returns `resolveQNameOption(qname)(enameProvider).get`.
   */
  def resolveQName(qname: QName)(implicit enameProvider: ENameProvider): EName = {
    resolveQNameOption(qname)(enameProvider)
      .getOrElse(sys.error(s"Could not resolve QName '$qname' in scope '${Scope.this}'"))
  }

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
  def resolveQNameOption(qname: QName)(implicit enameProvider: ENameProvider): Option[EName] = {
    import enameProvider._

    qname match {
      case unprefixedName: UnprefixedName if defaultNamespaceOption.isEmpty => Some(getNoNsEName(unprefixedName.localPart))
      case unprefixedName: UnprefixedName => Some(getEName(defaultNamespaceOption.get, unprefixedName.localPart))
      case prefixedName: PrefixedName =>
        // The prefix scope (as Map), with the implicit "xml" namespace added
        val completePrefixScopeMap: Map[String, String] = (prefixNamespaceMap - DefaultNsPrefix) + ("xml" -> XmlNamespace)
        completePrefixScopeMap.get(prefixedName.prefix) map { nsUri => getEName(nsUri, prefixedName.localPart) }
    }
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

      assert(declared.prefixNamespaceMap.keySet.intersect(undeclarations.prefixNamespaceMap.keySet).isEmpty)
      val m = (prefixNamespaceMap ++ declared.prefixNamespaceMap) -- undeclarations.prefixNamespaceMap.keySet
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
      val newlyDeclared: Map[String, String] = scope.prefixNamespaceMap filter {
        case (pref, ns) =>
          assert(ns.length > 0)
          Scope.this.prefixNamespaceMap.getOrElse(pref, "") != ns
      }

      val removed: Set[String] = Scope.this.prefixNamespaceMap.keySet.diff(scope.prefixNamespaceMap.keySet)
      val undeclarations: Map[String, String] = (removed map (pref => (pref -> ""))).toMap

      assert(newlyDeclared.keySet.intersect(removed).isEmpty)
      val m: Map[String, String] = newlyDeclared ++ undeclarations

      Declarations(m)
    }
  }

  /**
   * Returns the smallest sub-declarations `decl` of `declarations` such that `this.resolve(decl) == this.resolve(declarations)`
   */
  def minimize(declarations: Declarations): Declarations = {
    val declared = declarations.withoutUndeclarations.prefixNamespaceMap filter { case (pref, ns) => this.prefixNamespaceMap.getOrElse(pref, "") != ns }
    val undeclared = declarations.retainingUndeclarations.prefixNamespaceMap.keySet.intersect(this.prefixNamespaceMap.keySet)

    val result = Declarations(declared) ++ Declarations.undeclaring(undeclared)

    assert(this.resolve(declarations) == this.resolve(result))
    result
  }

  /** Returns `Scope(this.prefixNamespaceMap ++ scope.prefixNamespaceMap)` */
  def append(scope: Scope): Scope = Scope(this.prefixNamespaceMap ++ scope.prefixNamespaceMap)

  /** Returns `Scope(this.prefixNamespaceMap -- prefixes)` */
  def minus(prefixes: Set[String]): Scope = Scope(this.prefixNamespaceMap -- prefixes)

  /** Alias for `append` */
  def ++(scope: Scope): Scope = append(scope)

  /** Alias for `minus` */
  def --(prefixes: Set[String]): Scope = minus(prefixes)

  /** Alias for `append(Scope.from((prefix, namespace)))` */
  def append(prefix: String, namespace: String): Scope = append(Scope.from((prefix, namespace)))

  /** Creates a `String` representation of this `Scope`, as it is shown in XML */
  def toStringInXml: String = {
    val defaultNsString = if (defaultNamespaceOption.isEmpty) "" else """xmlns="%s"""".format(defaultNamespaceOption.get)
    val prefixScopeString = (prefixNamespaceMap - DefaultNsPrefix) map { case (pref, ns) => """xmlns:%s="%s"""".format(pref, ns) } mkString (" ")
    List(defaultNsString, prefixScopeString) filterNot { _ == "" } mkString (" ")
  }

  /**
   * Returns the inverse of this Scope, as Map from namespace URIs to collections of prefixes. These prefixes also include
   * the empty String if this Scope has a default namespace.
   */
  def inverse: Map[String, Set[String]] = {
    val nsPrefixPairs = this.prefixNamespaceMap.toSeq map { case (prefix, ns) => (ns, prefix) }
    val nsPrefixPairsGroupedByNs = nsPrefixPairs groupBy { case (ns, prefix) => ns }

    val result = nsPrefixPairsGroupedByNs mapValues { xs =>
      val result = xs map { case (ns, prefix) => prefix }
      result.toSet
    }

    assert(result.values forall (_.nonEmpty))
    result.toMap
  }

  /**
   * Returns an invertible Scope having the same namespaces but only one prefix per namespace.
   * If this Scope has a default namespace, the returned Scope possibly has that default namespace
   * as well.
   */
  def makeInvertible: Scope = {
    val prefixNsMap = inverse.mapValues(_.head).collect { case (ns, pref) => pref -> ns }.toMap
    Scope.from(prefixNsMap).ensuring(_.isInvertible)
  }

  /**
   * Returns the prefixes for the given namespace URI. The result includes the empty string for the default namespace, if
   * the default namespace is indeed equal to the passed namespace URI. The result does not include "xml" for the
   * implicit "xml" namespace (with namespace URI http://www.w3.org/XML/1998/namespace).
   *
   * The result is equivalent to:
   * {{{
   * this.inverse.getOrElse(namespaceUri, Set())
   * }}}
   *
   * This method can be handy when "inserting" an "element" into a parent tree, if one wants to reuse prefixes of the
   * parent tree.
   */
  def prefixesForNamespace(namespaceUri: String): Set[String] = {
    require(namespaceUri.nonEmpty, s"Empty namespace URI not allowed")

    val prefixes = this.prefixNamespaceMap.toSeq collect { case (prefix, ns) if ns == namespaceUri => prefix }
    prefixes.toSet
  }

  /**
   * Returns one of the prefixes for the given namespace URI, if any, and otherwise falling back to a prefix (or exception)
   * computed by the 2nd parameter. The result may be the empty string for the default namespace, if
   * the default namespace is indeed equal to the passed namespace URI. The result is "xml" if the
   * namespace URI is "http://www.w3.org/XML/1998/namespace".
   *
   * If the given namespace is the default namespace, but if there is also a non-empty prefix for the namespace, that
   * non-empty prefix is returned. Otherwise, if the given namespace is the default namespace, the empty string is returned.
   *
   * The prefix fallback is only used if `prefixesForNamespace(namespaceUri).isEmpty`. If the fallback prefix conflicts
   * with an already used prefix (including ""), an exception is thrown.
   *
   * This method can be handy when "inserting" an "element" into a parent tree, if one wants to reuse prefixes of the
   * parent tree.
   */
  def prefixForNamespace(namespaceUri: String, getFallbackPrefix: () => String): String = {
    require(namespaceUri.nonEmpty, s"Empty namespace URI not allowed")

    if (namespaceUri == XmlNamespace) {
      "xml"
    } else {
      val prefixes = prefixesForNamespace(namespaceUri)

      if (prefixes.isEmpty) {
        val pref = getFallbackPrefix()
        require(pref != "xml", s"Fallback prefix $pref for namespace $namespaceUri not allowed")

        val foundNs = prefixNamespaceMap.getOrElse(pref, namespaceUri)
        if (foundNs != namespaceUri) {
          sys.error(s"Prefix $pref already bound to namespace $foundNs, so cannot be bound to $namespaceUri in the same scope")
        }

        pref
      } else {
        if (prefixes == Set("")) "" else (prefixes - "").head
      }
    }
  }

  /**
   * Convenience method, returning the equivalent of:
   * {{{
   * this.resolve(
   *   Declarations.from(prefixForNamespace(namespaceUri, getFallbackPrefix) -> namespaceUri))
   * }}}
   *
   * If the namespace is "http://www.w3.org/XML/1998/namespace", this Scope is returned.
   *
   * If the fallback prefix is used and conflicts with an already used prefix (including ""), an exception is thrown,
   * as documented for method `prefixForNamespace`.
   *
   * The following property holds:
   * {{{
   * this.subScopeOf(this.includingNamespace(namespaceUri, getFallbackPrefix))
   * }}}
   */
  def includingNamespace(namespaceUri: String, getFallbackPrefix: () => String): Scope = {
    require(namespaceUri.nonEmpty, s"Empty namespace URI not allowed")

    if (namespaceUri == XmlNamespace || !prefixesForNamespace(namespaceUri).isEmpty) {
      this
    } else {
      assert(namespaceUri != XmlNamespace)

      // Throws an exception if the prefix has already been bound to another namespace
      val prefix = prefixForNamespace(namespaceUri, getFallbackPrefix)
      this.resolve(Declarations.from(prefix -> namespaceUri))
    }
  }

  /**
   * Implicit class extending QNames with easy conversions to ENames, using this Scope.
   */
  implicit class ToEName(val qname: QName) {

    /**
     * Expands (or resolves) the QName, using this Scope.
     */
    def res: EName = Scope.this.resolveQNameOption(qname).getOrElse(sys.error(s"Could not resolve $qname with scope ${Scope.this}"))
  }
}

object Scope {

  // scalastyle:off null
  private def validate(prefixNamespaceMap: Map[String, String]): Unit = {
    require(prefixNamespaceMap ne null)

    prefixNamespaceMap foreach {
      case (pref, ns) =>
        require(pref ne null, s"No null prefix allowed in scope $prefixNamespaceMap")
        require(ns ne null, s"No null namespace allowed in scope $prefixNamespaceMap")
        require(ns != "", s"No empty namespace allowed in scope $prefixNamespaceMap")
        require(
          !XmlStringUtils.containsColon(pref),
          s"The prefix must not contain any colon in scope $prefixNamespaceMap")
        require(
          pref != "xmlns",
          s"The prefix must not be 'xmlns' in scope $prefixNamespaceMap")
        require(
          pref != "xml",
          s"No 'xml' prefix allowed in scope $prefixNamespaceMap")
        require(
          ns != "http://www.w3.org/2000/xmlns/",
          s"No 'http://www.w3.org/2000/xmlns/' namespace allowed in scope $prefixNamespaceMap")
        require(
          ns != XmlNamespace,
          s"No 'http://www.w3.org/XML/1998/namespace' namespace allowed in scope $prefixNamespaceMap")
    }
  }

  /** The "empty" `Scope` */
  val Empty = Scope(Map())

  /**
   * Same as the constructor, but removing the 'xml' prefix, if any.
   * Therefore this call is easier to use than the constructor or default `apply` method.
   */
  def from(m: Map[String, String]): Scope = {
    if (m.contains("xml")) {
      require(
        m("xml") == XmlNamespace,
        "The 'xml' prefix must map to 'http://www.w3.org/XML/1998/namespace'")
    }
    Scope(m - "xml")
  }

  /** Returns `from(Map[String, String](m: _*))` */
  def from(m: (String, String)*): Scope = from(Map[String, String](m: _*))

  val DefaultNsPrefix = ""

  val XmlNamespace = Declarations.XmlNamespace
}
