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
 * Yet another immutable DOM-like API. This is not W3C DOM, not even close. Instead, this is
 * a DOM-like API optimized for Scala. Foremost, that means that this API is centered around
 * Scala Collections of nodes. Below, when mentioning DOM trees, the DOM-like trees of this
 * API are meant, not W3C DOM trees, unless mentioned otherwise.
 *
 * By design, some characteristics of this API are:
 * <ul>
 * <li>The DOM trees of this API are <em>immutable</em> and <em>thread-safe</em>.
 * This facilitates safe multi-threaded processing of these trees.</li>
 * <li>This API leverages the highly <em>expressive</em> <em>Scala Collections API</em>.
 * Querying DOM trees is easy to achieve using Scala <code>for</code> comprehensions,
 * using immutable Scala collections. It should even be attractive to use this API instead of
 * XPath, XSLT and/or XQuery, at the cost of slightly more verbosity and loss of typing information.</li>
 * <li>This API explicitly models <em>qualified names</em>, <em>expanded names</em> and <em>namespace scopes</em>.
 * See for example http://www.w3.org/TR/xml-names11/. By explicitly offering these concepts, this API
 * can hide several XML complexities behind their implementations. It does give this API a somewhat "strict"
 * or "precise" appearance.</li>
 * <li>This API aims at being easy to use for data-oriented XML with a known structure (typically described
 * by an XSD), and known namespaces. It is not geared towards XML that freely mixes text and tags.</li>
 * <li>This API has good interop with standard Java XML APIs (JAXP).</li>
 * </ul>
 *
 * Some non-goals or limitations of this API are:
 * <ul>
 * <li>This API does not have a low memory footprint.</li>
 * <li>This API does not try to solve all problems. Sometimes SAX, StAX or DOM is better.
 * In particular, immutability (using strict evaluation) has some drawbacks too, such as the elements not keeping a reference
 * to the parent element, or the missing convenience of in-place updates. Furthermore, this API is not meant to offer XPath
 * support (arguably, it does not need it).</li>
 * <li>This API is not meant to follow the W3C DOM standards. As a consequence, it is also far less complete, and
 * less "correct".</li>
 * </ul>
 *
 * Compared to Java's standard DOM API, this API does not follow the DOM specifications (such as DOM level 2 Core),
 * so it contains far less functionality. For example, attribute types are absent, normalized text can not be obtained,
 * documents are poorly modeled, parent elements can not be obtained, etc. On the other hand, this API is
 * far more modern, and safe to use in a multi-threaded environment.
 *
 * Compared to JDOM, which unlike the standard DOM API leverages idiomatic Java, this API in a similar vein attempts
 * to leverage idiomatic Scala. Immutability, Scala collection processing and Options instead of nulls are
 * characteristic of this API. Like is the case for DOM, JDOM has more far more functionality than this API.
 *
 * XOM is another idiomatic Java XML API. It focuses on correctness, among other things. To check yaidom for correctness,
 * it makes sense to compare the yaidom API against XOM. Unlike yaidom Nodes, XOM Nodes are mutable. Like Java's standard
 * DOM API, XOM is far more complete than yaidom (even sporting XPath support). Also note that yaidom does not support
 * base URIs.
 *
 * Like Scala's standard XML API, this API is centered around immutable XML trees. Again, this API is far less
 * ambitious. For one, XPath-like syntax is not offered. That has some advantages too, like a much simpler and more
 * straightforward implementation. This API does not need Scala XML's NodeSeq (where Node is NodeSeq, which is Seq[Node]).
 * Moreover, whereas Scala's XML API dates from before Scala 2.8, this API leverages Scala's 2.8+ Collections API.
 *
 * The Anti-XML library tackles some weaknesses of Scala's standard XML API, w.r.t. robustness, ease of use and performance.
 * It is mostly the Anti-XML API that inspired this API. Unlike Anti-XML this API does not offer XPath-like syntax, which
 * made this API very simple to implement (and understand), at the cost of moderate verbosity. Again, this API is less
 * ambitious.
 *
 * The dependencies in this package are as follows, from bottom to top:
 * <ol>
 * <li>Trait [[eu.cdevreeze.yaidom.QName]] and its subtypes</li>
 * <li>Class [[eu.cdevreeze.yaidom.ExpandedName]]</li>
 * <li>Class [[eu.cdevreeze.yaidom.Scope]]</li>
 * <li>Traits [[eu.cdevreeze.yaidom.ElemLike]] and [[eu.cdevreeze.yaidom.HasText]], containing common functions for "Element-like" classes</li>
 * <li>Trait [[eu.cdevreeze.yaidom.Node]] and its subtypes, such as [[eu.cdevreeze.yaidom.Elem]] (which extends [[eu.cdevreeze.yaidom.ElemLike]])</li>
 * <li>Trait [[eu.cdevreeze.yaidom.NodeBuilder]] and its subtypes, such as [[eu.cdevreeze.yaidom.ElemBuilder]]. At the same level are
 * [[eu.cdevreeze.yaidom.ConverterToElem]] and [[eu.cdevreeze.yaidom.ElemConverter]]</li>
 * </ol>
 * Dependencies are all uni-directional, from top to bottom. All types in this package are (deeply) immutable.
 * That holds even for the [[eu.cdevreeze.yaidom.NodeBuilder]] instances.
 *
 * @author Chris de Vreeze
 */
package object yaidom
