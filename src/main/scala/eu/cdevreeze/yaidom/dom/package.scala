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
 * Wrapper around class `org.w3c.dom.Element`, adapting it to the [[eu.cdevreeze.yaidom.ElemLike]] API.
 *
 * This wrapper is not thread-safe, and should only be used if the immutable element classes such as [[eu.cdevreeze.yaidom.Elem]]
 * are not the best fit.
 *
 * Such scenarios could be as follows:
 * <ul>
 * <li>Conversions from DOM to [[eu.cdevreeze.yaidom.Elem]] (and back) have more runtime costs than needed or wanted.</li>
 * <li>Roundtripping from XML string to "tree", and back to XML string should keep the resulting XML string as much as possible the same.</li>
 * <li>In-place updates (instead of "functional updates") of DOM trees are desired.</li>
 * </ul>
 *
 * Yet be aware that the advantages of immutability and thread-safety (offered by immutable `Elem` classes) are lost when using
 * this wrapper API. Mutable DOM trees are also very easy to break, even via the `ParentElemLike` API, if element predicates with
 * side-effects are used.
 *
 * To explain the "roundtripping" item above, note that class [[eu.cdevreeze.yaidom.Elem]] considers attributes in an element unordered,
 * let alone namespace declarations. That is consistent with the XML InfoSet specification, but can sometimes be impractical.
 * Using `org.w3c.dom.Element` instances, parsed from XML input sources, chances are that this order is retained.
 *
 * There are of course limitations to what formatting data is retained in a DOM tree. A good example is the short versus long form
 * of an empty element. Typically parsers do not pass any information about this distinction, so it is unknown whether the XML input source
 * used the long or short form for an empty element.
 *
 * It should also be noted that the configuration of XML parsers and serializers can be of substantial influence on the extent that
 * "roundtripping" keeps the XML string the same. Whitespace handling is one such area in which different configurations can lead
 * to quite different "roundtripping" results.
 *
 * Note that in one way these wrappers are somewhat unnatural: the `ElemLike` API uses immutable Scala collections everywhere,
 * whereas the elements of those collections are mutable (!) DOM node wrappers. The wrappers are idiomatic Scala in their use of
 * the Scala Collections API, whereas the wrapped DOM nodes come from a distant past, when imperative programming and "mutability
 * everywhere" ruled.
 *
 * In comparison to XPath against DOM trees, the `ElemLike` API may be more verbose, but it requires no setup and
 * "result set handling" boilerplate.
 *
 * @author Chris de Vreeze
 */
package object dom
