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
 * Scope mapping prefixes to namespace URIs, as well as holding an optional default
 * namespace.
 *
 * The purpose of a Scope is to resolve QNames as ExpandedNames.
 *
 * @author Chris de Vreeze
 */
final case class Scope(defaultNamespace: Option[String], prefixScope: Map[String, String]) extends Immutable {
  require(defaultNamespace ne null)
  require(defaultNamespace.forall(ns => (ns ne null) && (ns.length > 0)))
  require(prefixScope ne null)
  require(prefixScope.forall(kv => {
    val pref = kv._1
    val ns = kv._2
    (pref ne null) && (pref.length > 0) && (ns ne null) && (ns.length > 0)
  }))

  /** The prefix scope, with the implicit "xml" namespace added */
  def completePrefixScope: Map[String, String] = prefixScope + ("xml" -> "http://www.w3.org/XML/1998/namespace")

  /** Creates a Map from prefixes to namespaces URIs, giving the default namespace the empty String as prefix */
  def toMap: Map[String, String] = {
    val defaultNsMap: Map[String, String] = if (defaultNamespace.isEmpty) Map() else Map("" -> defaultNamespace.get)
    defaultNsMap ++ prefixScope
  }

  /** Returns true if this is a subscope of the given parameter Scope. A Scope is considered subscope of itself. */
  def subScopeOf(scope: Scope): Boolean = {
    val thisMap = toMap
    val otherMap = scope.toMap

    thisMap.keySet.subsetOf(otherMap.keySet) && thisMap.keySet.forall(pref => thisMap(pref) == otherMap(pref))
  }

  /** Returns true if this is a superscope of the given parameter Scope. A Scope is considered superscope of itself. */
  def superScopeOf(scope: Scope): Boolean = scope.subScopeOf(this)

  /** Tries to resolve the given QName against this Scope, returning None otherwise */
  def resolveQName(qname: QName): Option[ExpandedName] = qname match {
    case unprefixedName: UnprefixedName if defaultNamespace.isEmpty => Some(ExpandedName(unprefixedName.localPart))
    case unprefixedName: UnprefixedName => Some(ExpandedName(defaultNamespace.get, unprefixedName.localPart))
    case prefixedName: PrefixedName =>
      completePrefixScope.get(prefixedName.prefix).map(nsUri => ExpandedName(nsUri, prefixedName.localPart))
  }

  /**
   * Resolves the given declarations against this Scope, returning an updated Scope.
   *
   * Inspired by java.net.URI, which has a similar method for URIs.
   */
  def resolve(declarations: Scope.Declarations): Scope = {
    val m = (toMap ++ declarations.declared.toMap) -- declarations.undeclaredSet
    Scope.fromMap(m)
  }

  /**
   * Relativizes the given Scope against this Scope, returning a Declarations object.
   *
   * Inspired by java.net.URI, which has a similar method for URIs.
   */
  def relativize(scope: Scope): Scope.Declarations = {
    val declared: Scope = {
      val defaultNs: Option[String] = (Scope.this.defaultNamespace, scope.defaultNamespace) match {
        case (None, _) => scope.defaultNamespace
        case (_, None) => None
        case (Some(ns1), Some(ns2)) if ns1 == ns2 => None
        case (_, _) => scope.defaultNamespace
      }
      val prefixScope: Map[String, String] = scope.prefixScope.collect { case (pref, nsUri) if (!Scope.this.prefixScope.contains(pref)) || (Scope.this.prefixScope(pref) != scope.prefixScope(pref)) => (pref -> nsUri) }

      Scope(defaultNs, prefixScope)
    }
    val defaultNamespaceUndeclared: Boolean = this.defaultNamespace.isDefined && scope.defaultNamespace.isEmpty
    val undeclaredPrefixes: Set[String] = Scope.this.prefixScope.keySet.diff(scope.prefixScope.keySet)

    Scope.Declarations(declared, defaultNamespaceUndeclared, undeclaredPrefixes)
  }

  /** Creates a String representation of this Scope, as it is shown in XML */
  def toStringInXml: String = {
    val defaultNsString = if (defaultNamespace.isEmpty) "" else """xmlns="%s"""".format(defaultNamespace.get)
    val prefixScopeString = prefixScope.map(kv => """xmlns:%s="%s"""".format(kv._1, kv._2)).mkString(" ")
    List(defaultNsString, prefixScopeString).filterNot(_ == "").mkString(" ")
  }
}

object Scope {

  /** The "empty" Scope */
  val Empty = Scope(defaultNamespace = None, prefixScope = Map())

  /** Parses the given Map into a Scope. The Map must follow the Scope.toMap format. */
  def fromMap(m: Map[String, String]): Scope = {
    require(m.keySet.forall(pref => pref ne null))
    require(m.values.forall(ns => ns ne null))
    require(m.values.forall(ns => ns != ""))

    val defaultNamespace: Option[String] = m.get("")
    val prefixScope: Map[String, String] = m - ""
    Scope(defaultNamespace, prefixScope)
  }

  /**
   * Namespace declarations (and undeclarations), typically at the level of one Element.
   */
  final case class Declarations(declared: Scope, defaultNamespaceUndeclared: Boolean, undeclaredPrefixes: Set[String]) extends Immutable {
    require(declared ne null)
    require(undeclaredPrefixes ne null)
    require(undeclaredPrefixes.forall(pref => (pref ne null) && (pref.length > 0)))
    require(declared.toMap.keySet.intersect(undeclaredSet).isEmpty)
    require(!defaultNamespaceUndeclared || declared.defaultNamespace.isEmpty)

    /** Convenience constructor if there are no undeclarations */
    def this(declared: Scope) = this(declared, false, Set())

    /** Returns the Set of undeclared prefixes, with an undeclared default namespace represented by the empty String */
    def undeclaredSet: Set[String] = defaultNamespaceUndeclared match {
      case false => undeclaredPrefixes
      case true => undeclaredPrefixes + ""
    }

    /** Creates a String representation of this Scope.Declarations, as it is shown in an XML element */
    def toStringInXml: String = {
      val declaredString = declared.toStringInXml
      val defaultNsUndeclaredString = if (defaultNamespaceUndeclared) """xmlns=""""" else ""
      val undeclaredPrefixesString = undeclaredPrefixes.map(pref => """xmlns:%s=""""".format(pref)).mkString(" ")

      List(declaredString, defaultNsUndeclaredString, undeclaredPrefixesString).filterNot(_ == "").mkString(" ")
    }
  }

  object Declarations {

    def fromMap(m: Map[String, String]): Declarations = {
      require(m.keySet.forall(pref => pref ne null))
      require(m.values.forall(ns => ns ne null))

      val scope = Scope.fromMap(m.filter(kv => !kv._2.isEmpty))
      val defaultNamespaceUndeclared: Boolean = {
        val defaultNs = m.get("")
        defaultNs == Some("")
      }
      val undeclaredPrefixes = m.filterKeys(pref => pref != "").filter(kv => kv._2 == "").keySet

      Declarations(scope, defaultNamespaceUndeclared, undeclaredPrefixes)
    }
  }
}
