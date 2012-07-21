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
 * @author Chris de Vreeze
 */
final case class Scope(defaultNamespaceOption: Option[String], prefixScope: Map[String, String]) extends Immutable {
  require(defaultNamespaceOption ne null)
  require {
    defaultNamespaceOption forall { ns => (ns ne null) && (ns.length > 0) && (ns != "http://www.w3.org/2000/xmlns/") }
  }
  require(prefixScope ne null)
  require {
    prefixScope forall { kv =>
      val pref = kv._1
      val ns = kv._2
      (pref ne null) && XmlStringUtils.isAllowedPrefix(pref) && (pref != "xmlns") &&
        (ns ne null) && (ns.length > 0) && (ns != "http://www.w3.org/2000/xmlns/")
    }
  }

  /** The prefix scope, with the implicit "xml" namespace added */
  def completePrefixScope: Map[String, String] = prefixScope + ("xml" -> "http://www.w3.org/XML/1998/namespace")

  /** Creates a `Map` from prefixes to namespaces URIs, giving the default namespace the empty `String` as prefix */
  def toMap: Map[String, String] = {
    val defaultNsMap: Map[String, String] = if (defaultNamespaceOption.isEmpty) Map() else Map("" -> defaultNamespaceOption.get)
    defaultNsMap ++ prefixScope
  }

  /**
   * Returns true if the inverse exists, that is, each namespace URI has a unique prefix
   * (including the empty prefix for the default namespace, if applicable).
   */
  def isInvertible: Boolean = {
    val m = toMap
    m.keySet.size == m.values.toSet.size
  }

  /** Returns true if this is a subscope of the given parameter `Scope`. A `Scope` is considered subscope of itself. */
  def subScopeOf(scope: Scope): Boolean = {
    val thisMap = toMap
    val otherMap = scope.toMap

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
  def resolve(declarations: Scope.Declarations): Scope = {
    if (declarations == Scope.Declarations.Empty) this else {
      val m = (toMap ++ declarations.declared.toMap) -- declarations.undeclaredSet
      Scope.fromMap(m)
    }
  }

  /**
   * Relativizes the given `Scope` against this `Scope`, returning a `Declarations` object.
   *
   * Inspired by `java.net.URI`, which has a similar method for URIs.
   */
  def relativize(scope: Scope): Scope.Declarations = {
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

      Scope(defaultNs, prefixScope)
    }
    val defaultNamespaceUndeclared: Boolean = this.defaultNamespaceOption.isDefined && scope.defaultNamespaceOption.isEmpty
    val undeclaredPrefixes: Set[String] = Scope.this.prefixScope.keySet.diff(scope.prefixScope.keySet)
    val undeclaredOptionalPrefixes = {
      undeclaredPrefixes map { pref => Some(pref) }
    } ++ (if (defaultNamespaceUndeclared) Set(None) else Set())

    Scope.Declarations(declared, undeclaredOptionalPrefixes)
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
  val Empty = Scope(defaultNamespaceOption = None, prefixScope = Map())

  def from(m: (String, String)*): Scope = fromMap(Map[String, String](m: _*))

  /** Parses the given `Map` into a `Scope`. The `Map` must follow the `Scope.toMap` format. */
  def fromMap(m: Map[String, String]): Scope = {
    require {
      m.keySet forall { pref => pref ne null }
    }
    require {
      m.values forall { ns => ns ne null }
    }
    require {
      m.values forall { ns => ns != "" }
    }

    val defaultNamespace: Option[String] = m.get("")
    val prefixScope: Map[String, String] = m - ""
    Scope(defaultNamespace, prefixScope)
  }

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

    /** Creates a `String` representation of this `Scope.Declarations`, as it is shown in an XML element */
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
}
