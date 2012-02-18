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

package eu.cdevreeze

/**
 * Yet another immutable DOM-like API. Hence the name <em>yaidom</em>. This is not an implementation of W3C DOM.
 * Instead, this is a Scala-ish DOM-like API. Foremost, that means that this API is centered around
 * Scala Collections of immutable nodes.
 *
 * By design, some characteristics of this API are:
 * <ul>
 * <li>The DOM-like trees of this API are (deeply) <em>immutable</em> and therefore <em>thread-safe</em>.
 * This facilitates safe multi-threaded querying of these trees.</li>
 * <li>This API leverages the highly <em>expressive</em> <em>Scala Collections API</em>.
 * Querying yaidom trees is easy to achieve using Scala <code>for</code> comprehensions,
 * manipulating immutable Scala collections. It should often even be attractive to use this API instead of
 * XPath, XSLT and/or XQuery, at the cost of somewhat more verbosity and loss of (schema) typing information.</li>
 * <li>Yaidom supports <em>multiple implementations</em> of nodes. In this package, the default implementations of nodes
 * (and elements, documents, texts etc.) are offered. Yet in the [[eu.cdevreeze.yaidom.xlink]] sub-package nodes with XLink-awareness are
 * offered. In the [[eu.cdevreeze.yaidom.expanded]] sub-package elements contain no prefixes. To support these and other node implementations,
 * this package offers some traits that provide partial implementations. Typical "element node" implementations mix in
 * traits [[eu.cdevreeze.yaidom.ElemLike]] and [[eu.cdevreeze.yaidom.TextParentLike]], and typical "text node" implementations mix in trait
 * [[eu.cdevreeze.yaidom.TextLike]].</li>
 * <li>This API explicitly models <em>qualified names</em>, <em>expanded names</em> and <em>namespace scopes</em>.
 * See for example http://www.w3.org/TR/xml-names11/. By explicitly offering these concepts as classes, this API
 * can hide some XML complexities (like predefined namespaces) behind the implementations of these classes. These concepts
 * are even so prominent in yaidom that yaidom may feel a bit "static" and somewhat verbose.</li>
 * <li>This API is easier to use for data-oriented XML with a known structure (defined in an XSD) than for free format XML.
 * Namespaces are first-class citizens in yaidom. DTDs are not (but DTDs do not understand namespaces anyway).</li>
 * <li>This API has good interop with standard Java XML APIs (JAXP). Printing and parsing XML are delegated to JAXP.</li>
 * </ul>
 *
 * Some limitations of this API are:
 * <ul>
 * <li>This API does not have a low memory footprint.</li>
 * <li>Yaidom has no XPath(-like) support. The API does not unify nodes with sequences of nodes.
 * That makes code using yaidom more verbose than XPath(-like) code found in APIs like Scala's XML library or
 * Anti-XML. On the other hand, code using yaidom is very easy to reason about, and yaidom is also easy to implement.</li>
 * <li>Besides not offering any XPath(-like) support, yaidom is not very ambitious in other ways either. For example,
 * yaidom offers no XML literals (but does offer a more verbose DSL for building XML). As another example, yaidom
 * does not simplify printing/parsing of XML. The upside is that yaidom has a very good power-to-weight ratio.</li>
 * <li>Yaidom deals only with XML, not with HTML.</li>
 * <li>Immutability (using strict evaluation) has some drawbacks too, such as the elements not keeping a reference
 * to the parent element, or the missing convenience of in-place updates.</li>
 * <li>This API is not meant to follow the W3C DOM standards. Instead, it is about leveraging Scala to query and transform
 * immutable XML trees.</li>
 * <li>DTDs are not explicitly supported in this API. DTDs are awkward anyway for describing XML that uses namespaces
 * (see for example http://docstore.mik.ua/orelly/xml/xmlnut/ch04_04.htm). Related to this is the
 * absence of attribute types in yaidom (only the attribute value can be obtained).</li>
 * <li>Compared to commonly used DOM APIs, such as Java's standard DOM API, JDOM and XOM, besides the limitations
 * mentioned above, other features are missing as well, such as rich support for base URIs.</li>
 * </ul>
 *
 * To some extent, yaidom is a Scala-ish DOM-like API much like JDOM is a Java-ish DOM API. Yaidom is Scala-ish in the
 * pervasive use of immutable Scala collections, and the use of Options over nulls. It is clear from method and variable
 * names where Options are used. Also, methods whose names start with "find" tend to return Options.
 *
 * Yaidom has been inspired by Anti-XML. The Anti-XML library tackles some weaknesses of Scala's standard XML API, w.r.t.
 * robustness, ease of use and performance. Yaidom tries to achieve the same, except that it is less ambitious, foremost
 * in not offering any XPath(-like) support. Robustness comes from Scala's Collections API. Ease of use in yaidom's case
 * means more verbosity than Anti-XML, but it also means code that is trivial to understand and reason about.
 * Performance comes from Scala's Collections API.
 *
 * This package contains the following parts:
 * <ol>
 * <li>Basic concepts such as [[eu.cdevreeze.yaidom.QName]], [[eu.cdevreeze.yaidom.ExpandedName]] and
 * [[eu.cdevreeze.yaidom.Scope]].</li>
 * <li>Traits for partial implementations of DOM-like trees, such as [[eu.cdevreeze.yaidom.ElemLike]] and
 * [[eu.cdevreeze.yaidom.TextParentLike]] (for "element nodes"), and [[eu.cdevreeze.yaidom.TextLike]] (for "text nodes").</li>
 * <li>The default "node implementation", as trait [[eu.cdevreeze.yaidom.Node]] and its subtypes, such as
 * [[eu.cdevreeze.yaidom.Elem]] (which indeed mixes in the above-mentioned traits for "element nodes").</li>
 * <li>Trait [[eu.cdevreeze.yaidom.NodeBuilder]] and its subtypes, such as [[eu.cdevreeze.yaidom.ElemBuilder]].
 * Node builders can be used in a DSL-like fashion, for creation of Elems. Node builders postpone the choice of Scopes,
 * whereas the Nodes that they create all must have a fixed Scope, so node builders are indeed intended to be handy for creation
 * of node trees.</li>
 * <li>Contracts (as traits) for conversions from and to Documents or Elems</li>
 * </ol>
 *
 * The dependencies in this package are as follows, from bottom to top:
 * <ol>
 * <li>Singleton object [[eu.cdevreeze.yaidom.XmlStringUtils]]</li>
 * <li>Trait [[eu.cdevreeze.yaidom.QName]] and its subtypes</li>
 * <li>Class [[eu.cdevreeze.yaidom.ExpandedName]]</li>
 * <li>Classes [[eu.cdevreeze.yaidom.Scope]] and [[eu.cdevreeze.yaidom.Scope.Declarations]]. At the same level
 * is class [[eu.cdevreeze.yaidom.ElemPath]].</li>
 * <li>Traits [[eu.cdevreeze.yaidom.ElemLike]] and [[eu.cdevreeze.yaidom.TextParentLike]].
 * Trait [[eu.cdevreeze.yaidom.TextLike]] is also at this level.</li>
 * <li>Trait [[eu.cdevreeze.yaidom.Node]] and its subtypes, such as [[eu.cdevreeze.yaidom.Elem]].</li>
 * <li>Trait [[eu.cdevreeze.yaidom.NodeBuilder]] and its subtypes, such as [[eu.cdevreeze.yaidom.ElemBuilder]]. At the same level are
 * [[eu.cdevreeze.yaidom.ConverterToElem]], [[eu.cdevreeze.yaidom.ElemConverter]], etc.</li>
 * </ol>
 * Dependencies are all uni-directional. All types in this package are (deeply) immutable.
 * That holds even for the [[eu.cdevreeze.yaidom.NodeBuilder]] instances.
 *
 * In this package are some ("explicit" and therefore safe) implicit conversions, for treating a String as QName,
 * ExpandedName, Scope.Declarations, Scope, etc.
 *
 * Parsing and printing of XML is not handled in this package. Even the <code>toString</code> methods for nodes
 * use the NodeBuilder DSL syntax rather than XML string syntax. Hence the complex details of character escaping,
 * "ignorable whitespace" etc. are not handled in this package. Parsing and printing of XML are offered by the
 * [[eu.cdevreeze.yaidom.parse]] and [[eu.cdevreeze.yaidom.print]] subpackages, which depend on the [[eu.cdevreeze.yaidom.jinterop]] subpackage.
 * Those subpackages depend on this package, and not the other way around. Put differently, they are in this namespace.
 *
 * @author Chris de Vreeze
 */
package object yaidom {

  /** "Implicit class" for converting a String to a [[eu.cdevreeze.yaidom.QName]] */
  final class ToParsedQName(val s: String) {
    def qname: QName = QName.parse(s)
  }

  /** Implicit conversion enriching a String with a <code>qname</code> method that turns the String into a [[eu.cdevreeze.yaidom.QName]] */
  implicit def toParsedQName(s: String): ToParsedQName = new ToParsedQName(s)

  /** "Implicit class" for converting a String to an [[eu.cdevreeze.yaidom.ExpandedName]] */
  final class ToParsedExpandedName(val s: String) {
    def ename: ExpandedName = ExpandedName.parse(s)
  }

  /** Implicit conversion enriching a String with a <code>ename</code> method that turns the String into an [[eu.cdevreeze.yaidom.ExpandedName]] */
  implicit def toParsedExpandedName(s: String): ToParsedExpandedName = new ToParsedExpandedName(s)

  /** Namespace. It offers a method to create an [[eu.cdevreeze.yaidom.ExpandedName]] with that namespace from a given localPart */
  final class Namespace(val ns: String) {
    def ename(localPart: String): ExpandedName = ExpandedName(ns, localPart)
  }

  /** "Implicit class" for converting a String to a Namespace */
  final class ToNamespace(val s: String) {
    def ns: Namespace = new Namespace(s)
  }

  /** Implicit conversion enriching a String with a <code>ns</code> method that turns the String into a Namespace */
  implicit def toNamespace(s: String): ToNamespace = new ToNamespace(s)

  /** "Implicit class" for converting a Map[String, String] to a [[eu.cdevreeze.yaidom.Scope.Declarations]] */
  final class ToNamespaces(val m: Map[String, String]) {
    def namespaces: Scope.Declarations = Scope.Declarations.fromMap(m)
  }

  /** Implicit conversion enriching a Map[String, String] with a <code>namespaces</code> method that turns the Map into a [[eu.cdevreeze.yaidom.Scope.Declarations]] */
  implicit def toNamespaces(m: Map[String, String]): ToNamespaces = new ToNamespaces(m)

  /** "Implicit class" for converting a Map[String, String] to a [[eu.cdevreeze.yaidom.Scope]] */
  final class ToScope(val m: Map[String, String]) {
    def scope: Scope = Scope.fromMap(m)
  }

  /** Implicit conversion enriching a Map[String, String] with a <code>scope</code> method that turns the Map into a [[eu.cdevreeze.yaidom.Scope]] */
  implicit def toScope(m: Map[String, String]): ToScope = new ToScope(m)
}
