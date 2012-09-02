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
 * This facilitates safe multi-threaded <em>querying</em> of these trees, at the cost of a relatively high memory footprint.</li>
 * <li>This API leverages the highly <em>expressive</em> <em>Scala Collections API</em>, on the inside and from the outside.
 * Querying yaidom trees is easy to achieve using Scala <code>for</code> comprehensions,
 * manipulating immutable Scala collections. It should often even be attractive to use this API instead of
 * XPath, XSLT and/or XQuery, at the cost of somewhat more verbosity, loss of (schema) typing information, and loss of a standard
 * XML query language. Much of the attraction of yaidom for querying lies in its simplicity. That is, understanding the Scala
 * Collections API (from the perspective of a user) is almost all that is needed for understanding querying in yaidom. In
 * comparison, XSLT is known to be a hard language to master.</li>
 * <li>Yaidom is also Scala-ish in the use of `Option`s over nulls. It is clear from method and variable
 * names where `Option`s are used. Also, methods whose names start with "find" tend to return `Option`s.</li>
 * <li>This API explicitly models and distinguishes between <em>qualified names</em> and <em>expanded names</em>. The same holds for
 * <em>namespace declarations</em> and <em>namespace scopes</em>. See for example http://www.w3.org/TR/xml-names11/.
 * The clear distinction between qualified names and expanded names helps in making yaidom queries easy to understand.
 * Yaidom queries indeed have simple and precise semantics. Moreover, the four concepts (QNames, ENames, declarations and scopes) obey
 * some obvious properties, that help in keeping yaidom small and simple, both in API and implementation.</li>
 * <li><em>Namespaces</em> are first-class citizens in yaidom. DTDs are not (but DTDs do not understand namespaces anyway).
 * See for example http://docstore.mik.ua/orelly/xml/xmlnut/ch04_04.htm.</li>
 * <li>This API has good interop with standard Java XML APIs, that is, <em>JAXP</em>. Printing and parsing XML (and details of "escaping")
 * are delegated to JAXP, without taking away any control over JAXP configuration. By configuring JAXP objects (for parsing or
 * printing XML), we keep some control over the exact XML string representations of serialized yaidom elements.</li>
 * </ul>
 *
 * The yaidom API is most suitable for processing <em>"data-oriented"</em> XML, roughly having the following properties:
 * <ul>
 * <li>Tags and text are not mixed.</li>
 * <li>Inter-element whitespace is typically "ignorable", and only used for formatting.</li>
 * <li>The structure of the XML is known, and typically described in an XSD.</li>
 * <li>Namespaces are heavily used.</li>
 * <li>DTDs are not used.</li>
 * <li>The exact string representation is not important (such as short or long form of empty elements, the order of attributes,
 * including namespace declarations, even the exact namespace prefixes used, etc.). A "DOM-centric" view of XML may well
 * suffice for processing such XML.</li>
 * </ul>
 * That is not to say that yaidom can only be used for processing such "data-oriented" XML.
 *
 * Yaidom is not a very ambitious API:
 * <ul>
 * <li>The API has no XPath(-like) support, and does not unify nodes with sequences of nodes.
 * That makes code using yaidom more verbose than XPath(-like) code found in APIs like Scala's XML library or
 * Anti-XML. On the other hand, code using yaidom is very easy to reason about, and yaidom is also easy to implement.</li>
 * <li>Yaidom does not simplify printing/parsing of XML. The upside is that yaidom has a very good power-to-weight ratio.</li>
 * <li>Yaidom only knows about XML, not about HTML. On the other hand, TagSoup can be used as SAX Parser.</li>
 * <li>Yaidom does not know about XML Schema types (or DTD types), for example types of attributes.</li>
 * <li>APIs such as JAXP, JDOM and XOM are more feature-rich, for example in support for base URIs.</li>
 * <li>The API does not try to define equality for XML trees, because that's a very vague notion for XML. At least that is
 * the short story. See "resolved" [[eu.cdevreeze.yaidom.resolved.Elem]] for "bare bones" elements that do obey some notion of
 * equality.</li>
 * </ul>
 *
 * Yaidom has been inspired by Anti-XML. The Anti-XML library tackles some weaknesses of Scala's standard XML API, w.r.t.
 * robustness, ease of use and performance. Yaidom tries to achieve the same (in a different way), but yaidom is less ambitious,
 * foremost in not offering any XPath(-like) support.
 *
 * Example usage:
 * {{{
 * val bookstoreElm = doc.documentElement
 * require(bookstoreElm.localName == "Bookstore")
 *
 * val bookElms =
 *   for {
 *     bookElm <- bookstoreElm \ "Book"
 *     price = bookElm.attribute(EName("Price"))
 *     if price.toInt < 90
 *   } yield bookElm
 * }}}
 *
 * The <em>for</em>-comprehension is equivalent to:
 * {{{
 * val bookElms =
 *   for {
 *     bookElm <- bookstoreElm filterChildElems { _.localName == "Book" }
 *     price = bookElm.attribute(EName("Price"))
 *     if price.toInt < 90
 *   } yield bookElm
 * }}}
 *
 * This package contains the following parts, in order of dependencies (starting with the class without any dependencies):
 * <ol>
 * <li>Basic concepts such as [[eu.cdevreeze.yaidom.QName]], [[eu.cdevreeze.yaidom.EName]], [[eu.cdevreeze.yaidom.Declarations]] and
 * [[eu.cdevreeze.yaidom.Scope]]. At the same level is class [[eu.cdevreeze.yaidom.ElemPath]].</li>
 * <li>Trait [[eu.cdevreeze.yaidom.ElemAwareElemLike]], which offers the core yaidom querying API. It is extended by trait
 * [[eu.cdevreeze.yaidom.PathAwareElemLike]], which is extended by trait [[eu.cdevreeze.yaidom.ElemLike]], which in turn is extended
 * by trait [[eu.cdevreeze.yaidom.UpdatableElemLike]]. At the same level is trait [[eu.cdevreeze.yaidom.HasText]]. All these APIs
 * turn small abstract APIs into rich APIs with implementations, and are mixed in by "element" classes.</li>
 * <li>The "node tree type hierarchy", as sealed trait [[eu.cdevreeze.yaidom.Node]] and its subtypes, such as
 * [[eu.cdevreeze.yaidom.Elem]] (which indeed mixes in the above-mentioned traits `UpdatableElemLike` and `HasText`).</li>
 * <li>Trait [[eu.cdevreeze.yaidom.NodeBuilder]] and its subtypes, such as [[eu.cdevreeze.yaidom.ElemBuilder]].
 * Node builders can be used in a DSL-like fashion, for creation of Elems. Node builders postpone the choice of `Scope`s
 * (whereas the `Node`s that they create all must have a fixed `Scope`), so node builders are indeed intended to be handy for creation
 * of node trees. At the same level are [[eu.cdevreeze.yaidom.ConverterToElem]], [[eu.cdevreeze.yaidom.ElemConverter]], etc.</li>
 * </ol>
 * Dependencies are all uni-directional. All types in this package are (deeply) immutable.
 * That holds even for the [[eu.cdevreeze.yaidom.NodeBuilder]] instances.
 *
 * Parsing and printing of XML are not handled in this package. Even the `toString` methods for nodes
 * use the `NodeBuilder` DSL syntax rather than XML string syntax. Hence the complex details of character escaping,
 * "ignorable whitespace" etc. are not handled in this package. Parsing and printing of XML are offered by the
 * [[eu.cdevreeze.yaidom.parse]] and [[eu.cdevreeze.yaidom.print]] subpackages, which depend on the [[eu.cdevreeze.yaidom.convert]] subpackage.
 * Those subpackages depend on this package, and not the other way around. Put differently, they are in this namespace.
 *
 * Yaidom also offers packages [[eu.cdevreeze.yaidom.resolved]] and [[eu.cdevreeze.yaidom.xlink]]. the `resolved` package offers
 * "bare bones" elements, stripped down to "essentials" (replacing prefixes by namespace URIs, removing comments, etc.), such that
 * those elements can be compared for some notion of equality. The `xlink` package offers some basic XLink support.
 *
 * @author Chris de Vreeze
 */
package object yaidom
