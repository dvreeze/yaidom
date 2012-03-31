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
 * This facilitates safe multi-threaded querying of these trees, at the cost of a relatively high memory footprint.</li>
 * <li>This API leverages the highly <em>expressive</em> <em>Scala Collections API</em>.
 * Querying yaidom trees is easy to achieve using Scala <code>for</code> comprehensions,
 * manipulating immutable Scala collections. It should often even be attractive to use this API instead of
 * XPath, XSLT and/or XQuery, at the cost of somewhat more verbosity and loss of (schema) typing information.</li>
 * <li>Yaidom is also Scala-ish in the use of `Option`s over nulls. It is clear from method and variable
 * names where `Option`s are used. Also, methods whose names start with "find" tend to return `Option`s.</li>
 * <li>This API explicitly models <em>qualified names</em>, <em>expanded names</em> and <em>namespace scopes</em>.
 * See for example http://www.w3.org/TR/xml-names11/.</li>
 * <li>This API is easiest to use for querying "data-oriented" XML with a known structure (defined in an XSD).
 * Namespaces are first-class citizens in yaidom. DTDs are not (but DTDs do not understand namespaces anyway).
 * See for example http://docstore.mik.ua/orelly/xml/xmlnut/ch04_04.htm. The API is not optimized for "free format" XML,
 * mixing tags and text. Neither does yaidom make it easy to perform pattern matching on node trees, which is less of an issue
 * if the structure of the XML is known.</li>
 * <li>This API has good interop with standard Java XML APIs (JAXP). Printing and parsing XML (and details of "escaping")
 * are delegated to JAXP, without taking away any control over JAXP configuration.</li>
 * </ul>
 *
 * Yaidom is not a very ambitious API:
 * <ul>
 * <li>The API has no XPath(-like) support, and does not unify nodes with sequences of nodes.
 * That makes code using yaidom more verbose than XPath(-like) code found in APIs like Scala's XML library or
 * Anti-XML. On the other hand, code using yaidom is very easy to reason about, and yaidom is also easy to implement.</li>
 * <li>Yaidom does not simplify printing/parsing of XML. The upside is that yaidom has a very good power-to-weight ratio.</li>
 * <li>Yaidom only knows about XML, not about HTML.</li>
 * <li>Yaidom does not know about XML Schema types (or DTD types), for example types of attribute.</li>
 * <li>APIs such as JAXP, JDOM and XOM are more feature-rich, for example in support for base URIs.</li>
 * </ul>
 *
 * Yaidom has been inspired by Anti-XML. The Anti-XML library tackles some weaknesses of Scala's standard XML API, w.r.t.
 * robustness, ease of use and performance. Yaidom tries to achieve the same, although yaidom is less ambitious, foremost
 * in not offering any XPath(-like) support.
 *
 * Example usage:
 * {{{
 * val ns = "http://bookstore".ns
 *
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val bookElms =
 *   for {
 *     bookElm <- bookstoreElm \ { _.localName == "Book" }
 *     val price = bookElm.attribute("Price".ename)
 *     if price.toInt < 90
 *   } yield bookElm
 * }}}
 * Note the implicit conversions from Strings to namespace URIs and to expanded names.
 *
 * The <em>for</em>-comprehension is equivalent to:
 * {{{
 * val bookElms =
 *   for {
 *     bookElm <- bookstoreElm filterChildElems { _.localName == "Book" }
 *     val price = bookElm.attribute("Price".ename)
 *     if price.toInt < 90
 *   } yield bookElm
 * }}}
 *
 * This package contains the following parts, in order of dependencies (starting with the class without any dependencies):
 * <ol>
 * <li>Basic concepts such as [[eu.cdevreeze.yaidom.QName]], [[eu.cdevreeze.yaidom.ExpandedName]] and
 * [[eu.cdevreeze.yaidom.Scope]]. At the same level is class [[eu.cdevreeze.yaidom.ElemPath]].</li>
 * <li>Trait [[eu.cdevreeze.yaidom.ElemLike]], which partly implements "element nodes".</li>
 * <li>The "node tree type hierarchy", as sealed trait [[eu.cdevreeze.yaidom.Node]] and its subtypes, such as
 * [[eu.cdevreeze.yaidom.Elem]] (which indeed mixes in the above-mentioned trait `ElemLike`).</li>
 * <li>Trait [[eu.cdevreeze.yaidom.NodeBuilder]] and its subtypes, such as [[eu.cdevreeze.yaidom.ElemBuilder]].
 * Node builders can be used in a DSL-like fashion, for creation of Elems. Node builders postpone the choice of `Scope`s,
 * whereas the `Node`s that they create all must have a fixed `Scope`, so node builders are indeed intended to be handy for creation
 * of node trees. At the same level are [[eu.cdevreeze.yaidom.ConverterToElem]], [[eu.cdevreeze.yaidom.ElemConverter]], etc.</li>
 * </ol>
 * Dependencies are all uni-directional. All types in this package are (deeply) immutable.
 * That holds even for the [[eu.cdevreeze.yaidom.NodeBuilder]] instances.
 *
 * In this package are some ("explicit" and therefore safe) implicit conversions, for treating a `String` as `QName`,
 * `ExpandedName` or `Namespace`, and for treating a `Map[String, String]` as `Scope.Declarations` or `Scope`.
 *
 * Parsing and printing of XML is not handled in this package. Even the `toString` methods for nodes
 * use the `NodeBuilder` DSL syntax rather than XML string syntax. Hence the complex details of character escaping,
 * "ignorable whitespace" etc. are not handled in this package. Parsing and printing of XML are offered by the
 * [[eu.cdevreeze.yaidom.parse]] and [[eu.cdevreeze.yaidom.print]] subpackages, which depend on the [[eu.cdevreeze.yaidom.jinterop]] subpackage.
 * Those subpackages depend on this package, and not the other way around. Put differently, they are in this namespace.
 *
 * @author Chris de Vreeze
 */
package object yaidom {

  /** "Implicit class" for converting a `String` to a [[eu.cdevreeze.yaidom.QName]] */
  final class ToParsedQName(val s: String) {
    def qname: QName = QName.parse(s)
  }

  /** Implicit conversion enriching a `String` with a `qname` method that turns the `String` into a [[eu.cdevreeze.yaidom.QName]] */
  implicit def toParsedQName(s: String): ToParsedQName = new ToParsedQName(s)

  /** "Implicit class" for converting a `String` to an [[eu.cdevreeze.yaidom.ExpandedName]] */
  final class ToParsedExpandedName(val s: String) {
    def ename: ExpandedName = ExpandedName.parse(s)
  }

  /** Implicit conversion enriching a `String` with a `ename` method that turns the `String` into an [[eu.cdevreeze.yaidom.ExpandedName]] */
  implicit def toParsedExpandedName(s: String): ToParsedExpandedName = new ToParsedExpandedName(s)

  /** Namespace. It offers a method to create an [[eu.cdevreeze.yaidom.ExpandedName]] with that namespace from a given localPart */
  final class Namespace(val ns: String) {
    def ename(localPart: String): ExpandedName = ExpandedName(ns, localPart)

    /** Returns `ns`, that is, the namespace URI as `String` */
    override def toString: String = ns
  }

  /** "Implicit class" for converting a `String` to a `Namespace` */
  final class ToNamespace(val s: String) {
    def ns: Namespace = new Namespace(s)
  }

  /** Implicit conversion enriching a `String` with a `ns` method that turns the `String` into a `Namespace` */
  implicit def toNamespace(s: String): ToNamespace = new ToNamespace(s)

  /** "Implicit class" for converting a `Map[String, String]` to a [[eu.cdevreeze.yaidom.Scope.Declarations]] */
  final class ToNamespaces(val m: Map[String, String]) {
    def namespaces: Scope.Declarations = Scope.Declarations.fromMap(m)
  }

  /** Implicit conversion enriching a `Map[String, String]` with a `namespaces` method that turns the `Map` into a [[eu.cdevreeze.yaidom.Scope.Declarations]] */
  implicit def toNamespaces(m: Map[String, String]): ToNamespaces = new ToNamespaces(m)

  /** "Implicit class" for converting a `Map[String, String]` to a [[eu.cdevreeze.yaidom.Scope]] */
  final class ToScope(val m: Map[String, String]) {
    def scope: Scope = Scope.fromMap(m)
  }

  /** Implicit conversion enriching a `Map[String, String]` with a `scope` method that turns the `Map` into a [[eu.cdevreeze.yaidom.Scope]] */
  implicit def toScope(m: Map[String, String]): ToScope = new ToScope(m)
}
