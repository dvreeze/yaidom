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
 * <em>Yaidom</em> is yet another Scala immutable DOM-like XML API. The other well-known Scala immutable DOM-like APIs are
 * the standard scala.xml API and Anti-XML. The latter API is considered an improvement over the former, but both APIs:
 * <ul>
 * <li>attempt to offer XPath-like querying, blurring the distinction between nodes and node collections</li>
 * <li>lack first-class support for namespaces (and namespace URIs)</li>
 * <li>have limited (functional) update support</li>
 * </ul>
 *
 * Yaidom takes a different approach, avoiding XPath-like query support, and offering good namespace and (functional) update
 * support. Yaidom also values <em>mathematical precision</em> and clarity. Still, the API remains practical and
 * pragmatic. In particular, the user has much configuration control over parsing and serialization, because yaidom exposes
 * the underlying JAXP parsers and serializers.
 *
 * Yaidom chose its battles. For example, knowing that DTDs do not know about namespaces, yaidom chose for good namespace
 * support, but ignores DTDs entirely. Of course the underlying XML parser may still validate XML against a DTD, if so desired.
 * As another example, yaidom tries to leave the handling of gory details of XML processing (such as whitespace handling)
 * as much as possible to JAXP. As yet another example, yaidom knows nothing about (XML Schema) types of elements and attributes.
 *
 * Yaidom, and in particular this package, contains the following layers:
 * <ol>
 * <li><em>basic concepts</em>, such as (qualified and expanded) names of elements and attributes</li>
 * <li>the <em>uniform query API traits</em>, to query elements for descendant elements</li>
 * <li>some of the specific <em>element implementations</em>, mixing in those uniform query API traits</li>
 * </ol>
 *
 * It makes sense to read this documentation, because it helps in getting up-to-speed with yaidom.
 *
 * ==Basic concepts==
 *
 * In real world XML, elements (and sometimes attributes) tend to have names within a certain namespace. There are 2 kinds of names
 * at play here:
 * <ul>
 * <li><em>qualified names</em>, such as `fo:block`</li>
 * <li><em>expanded names</em>, such as `{http://www.w3.org/1999/XSL/Format}block` (in James Clark notation)</li>
 * </ul>
 * They are represented by immutable classes [[eu.cdevreeze.yaidom.QName]] and [[eu.cdevreeze.yaidom.EName]], respectively.
 *
 * Qualified names occur in XML, whereas expanded names do not. Yet qualified names have no meaning on their own. They need
 * to be resolved to expanded names, via the in-scope namespaces. Note that the term "qualified name" is often used for what
 * yaidom (and the Namespaces specification) calls "expanded name", and that most XML APIs do not distinguish between the
 * 2 kinds of names. Yaidom has to clearly make this distinction, in order to model namespaces correctly.
 *
 * To resolve qualified names to expanded names, yaidom distinguishes between:
 * <ul>
 * <li><em>namespace declarations</em></li>
 * <li><em>in-scope namespaces</em></li>
 * </ul>
 * They are represented by immutable classes [[eu.cdevreeze.yaidom.Declarations]] and [[eu.cdevreeze.yaidom.Scope]], respectively.
 *
 * Namespace declarations occur in XML, whereas in-scope namespaces do not. The latter are the accumulated effect of the
 * namespace declarations of the element itself, if any, and those in ancestor elements.
 *
 * To see the resolution of qualified names in action, consider the following sample XML (from http://xmlgraphics.apache.org/fop/quickstartguide.html):
 *
 * {{{
 * <xsl:stylesheet version="1.0" xmlns:fo="http://www.w3.org/1999/XSL/Format" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
 *   <xsl:output method="xml" indent="yes"/>
 *   <xsl:template match="/">
 *     <fo:root>
 *       <fo:layout-master-set>
 *         <fo:simple-page-master master-name="A4-portrait" page-height="29.7cm" page-width="21.0cm" margin="2cm">
 *           <fo:region-body/>
 *         </fo:simple-page-master>
 *       </fo:layout-master-set>
 *       <fo:page-sequence master-reference="A4-portrait">
 *         <fo:flow flow-name="xsl-region-body">
 *           <fo:block>
 *             Hello, <xsl:value-of select="name"/>!
 *           </fo:block>
 *         </fo:flow>
 *       </fo:page-sequence>
 *     </fo:root>
 *   </xsl:template>
 * </xsl:stylesheet>
 * }}}
 *
 * Consider the element with qualified name `QName("fo:block")`. To resolve this qualified name as expanded name, we need to
 * know the namespaces in scope at that element. To compute the in-scope namespaces, we need to accumulate the namespace
 * declarations of the fo:block element and of its ancestor elements, from the outside in.
 *
 * That is, we start with `Scope.Empty`. Then, in the root element we find namespace declarations:
 * {{{
 * Declarations.from("fo" -> "http://www.w3.org/1999/XSL/Format", "xsl" -> "http://www.w3.org/1999/XSL/Transform")
 * }}}
 * This leads to the following namespaces in scope at the root element:
 * {{{
 * Scope.Empty.resolve(Declarations.from("fo" -> "http://www.w3.org/1999/XSL/Format", "xsl" -> "http://www.w3.org/1999/XSL/Transform"))
 * }}}
 * which is equal to:
 * {{{
 * Scope.from("fo" -> "http://www.w3.org/1999/XSL/Format", "xsl" -> "http://www.w3.org/1999/XSL/Transform")
 * }}}
 * We find no other namespace declarations in the ancestry of the block element, so the computed scope is also the scope
 * of the block element.
 *
 * Then `QName("fo:block")` is resolved as follows:
 * {{{
 * Scope.from("fo" -> "http://www.w3.org/1999/XSL/Format", "xsl" -> "http://www.w3.org/1999/XSL/Transform").resolveQNameOption(QName("fo:block"))
 * }}}
 * which is equal to:
 * {{{
 * Some(EName("{http://www.w3.org/1999/XSL/Format}block"))
 * }}}
 *
 * This namespace support in yaidom has mathematical rigor. The immutable classes `QName`, `EName`, `Declarations` and `Scope` have
 * precise definitions, reflected in their implementations, and obey some interesting properties. For example, if we define Scope
 * operation `relativize` (along with `resolve`), we get:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)) == scope2
 * }}}
 *
 * This may not sound like much, but by getting the basics right, yaidom succeeds in offering first-class support for XML
 * namespaces, without the magic and namespace-related bugs often found in other XML libraries.
 *
 * There are 2 other basic concepts in this package, representing paths to elements:
 * <ul>
 * <li><em>element path builders</em></li>
 * <li><em>element paths</em></li>
 * </ul>
 * They are represented by immutable classes [[eu.cdevreeze.yaidom.ElemPathBuilder]] and [[eu.cdevreeze.yaidom.ElemPath]], respectively.
 *
 * Element path builders are like canonical XPath expressions, yet they do not contain the root element itself, and indexing
 * starts with 0 instead of 1.
 *
 * For example, the block element mentioned above has element path:
 * {{{
 * ElemPath.from(
 *   EName("{http://www.w3.org/1999/XSL/Transform}template") -> 0,
 *   EName("{http://www.w3.org/1999/XSL/Format}root") -> 0,
 *   EName("{http://www.w3.org/1999/XSL/Format}page-sequence") -> 0,
 *   EName("{http://www.w3.org/1999/XSL/Format}flow") -> 0,
 *   EName("{http://www.w3.org/1999/XSL/Format}block") -> 0
 * )
 * }}}
 *
 * This path could be written as element path builder as follows:
 * {{{
 * ElemPathBuilder.from(
 *   QName("xsl:template") -> 0, QName("fo:root") -> 0, QName("fo:page-sequence") -> 0, QName("fo:flow") -> 0, QName("fo:block") -> 0)
 * }}}
 *
 * Using the Scope mentioned earlier, the latter element path builder resolves to the element path given before that, by
 * invoking method `ElemPathBuilder.build(scope)`. In order for this to work, the Scope must be <em>invertible</em>. That is,
 * there must be a one-to-one correspondence between prefixes ("" for the default namespace) and namespace URIs, because
 * otherwise the index numbers may not match. Also note that the prefixes `xsl` and `fo` in the element path builder are
 * arbitrary, and need not match with the prefixes used in the XML tree itself.
 *
 * ==Uniform query API traits==
 *
 * Yaidom provides a relatively small query API, to query an individual element for collections of <em>child elements</em>,
 * <em>descendant elements</em> or <em>descendant-or-self elements</em>. The resulting collections are immutable Scala
 * collections, that can further be manipulated using the Scala Collections API.
 *
 * This query API is <em>uniform</em>, in that different element implementations share (most of) the same query API. It is also
 * <em>element-centric</em> (unlike standard Scala XML and Anti-XML).
 *
 * For example, consider the XML example given earlier, as a Scala XML literal named `xslt`. Then we can wrap this Scala
 * XML Elem into a yaidom wrapper of type [[eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem]], named `xsltElem`, and query for
 * all descendant-or-self elements with resolved (or expanded) name `EName("{http://www.w3.org/1999/XSL/Format}block")` as follows:
 * {{{
 * xsltElem filterElemsOrSelf (elem => elem.resolvedName == EName("{http://www.w3.org/1999/XSL/Format}block"))
 * }}}
 * The result would be an immutable IndexedSeq of `ScalaXmlElem` instances, holding precisely 1 element.
 *
 * We could have written:
 * {{{
 * xsltElem.filterElemsOrSelf(EName("{http://www.w3.org/1999/XSL/Format}block"))
 * }}}
 * instead, with the same result.
 *
 * Instead of searching for appropriate descendant-or-self elements, we could have searched for descendant elements only,
 * without altering the result in this case:
 * {{{
 * xsltElem filterElems (elem => elem.resolvedName == EName("{http://www.w3.org/1999/XSL/Format}block"))
 * }}}
 * or:
 * {{{
 * xsltElem.filterElems(EName("{http://www.w3.org/1999/XSL/Format}block"))
 * }}}
 *
 * We could also find the same elements in a more defensive way, it our intent is to find such block elements only inside
 * template elements:
 * {{{
 * for {
 *   templateElem <- xsltElem.filterChildElems(EName("{http://www.w3.org/1999/XSL/Transform}template"))
 *   blockElem <- templateElem.filterElems(EName("{http://www.w3.org/1999/XSL/Format}block"))
 * } yield blockElem
 * }}}
 * which is equivalent to:
 * {{{
 * for {
 *   templateElem <- xsltElem filterChildElems (elem => elem.resolvedName == EName("{http://www.w3.org/1999/XSL/Transform}template"))
 *   blockElem <- templateElem filterElems (elem => elem.resolvedName == EName("{http://www.w3.org/1999/XSL/Format}block"))
 * } yield blockElem
 * }}}
 *
 * We could even use operator notation, as follows:
 * {{{
 * for {
 *   templateElem <- xsltElem \ EName("{http://www.w3.org/1999/XSL/Transform}template")
 *   blockElem <- templateElem \\ EName("{http://www.w3.org/1999/XSL/Format}block")
 * } yield blockElem
 * }}}
 *
 * Now suppose the same XML is stored in a (org.w3c.dom) DOM tree, wrapped in a [[eu.cdevreeze.yaidom.dom.DomElem]] `xsltElem`.
 * Then the same queries would use exactly the same code as above. The result would be a collection of `DomElem` instances
 * instead of `ScalaXmlElem` instances, however. There are many more element implementations in yaidom, and they share
 * (most of) the same query API. Therefore this query API is called a <em>uniform</em> query API.
 *
 * The last example, using operator notation, looks a bit more "XPath-like". Yet it is more verbose than queries in Scala XML,
 * partly because in yaidom these operators cannot be chained. Yet this is with good reason. Yaidom does not blur the
 * distinction between elements and element collections, and therefore does not offer an XPath experience. The small price
 * paid in verbosity is made up for by precision. The yaidom query API traits have very precise definitions of their
 * operations, as can be seen in the corresponding documentation.
 *
 * The uniform query API traits turn minimal APIs into richer APIs, where these richer APIs are defined very precisely in terms
 * of the minimal API. The top-level query API trait is [[eu.cdevreeze.yaidom.ParentElemLike]]. It needs to be given a method
 * to query for child elements (not child nodes in general, but just child elements!), and it offers methods to query
 * for some or all child elements, descendant elements, and descendant-or-self elements. That is, the minimal API consists
 * of abstract method `findAllChildElems`, and it offers methods such as `filterChildElems`, `filterElems` and `filterElemsOrSelf`.
 * This trait has no knowledge about elements at all, other than the fact that elements can have child elements.
 *
 * Subtrait [[eu.cdevreeze.yaidom.ElemLike]] adds minimal knowledge about elements themselves, viz. that elements have a
 * "resolved" (or expanded) name, and "resolved" attributes (mapping attribute expanded names to attribute values). That is,
 * it needs to be given implementations of abstract methods `resolvedName` and `resolvedAttributes`, and then offers methods to
 * query for attributes or child/descendant/descendant-or-self elements with a given expanded name. The trait is trivially defined
 * in terms of its supertrait.
 *
 * It is important to note that yaidom does not consider namespace declarations to be attributes themselves. Thus, there are no
 * circular dependencies between both concepts, because attributes with namespaces require in-scope namespaces and therefore
 * namespace declarations for resolving the names of these attributes.
 *
 * Note that traits [[eu.cdevreeze.yaidom.ElemLike]] and [[eu.cdevreeze.yaidom.ParentElemLike]] only know about elements, not
 * about other kinds of nodes. Of course the actual element implementations mixing in this query API know about other node
 * types. but that knowledge is outside the uniform query API. Note that the example queries above only use the minimal
 * element knowledge that traits `ElemLike` and `ParentElemLike` have about elements. Therefore the query code can be used
 * unchanged for different element implementations.
 *
 * The `ElemLike` trait has subtrait [[eu.cdevreeze.yaidom.PathAwareElemLike]]. It adds knowledge about element paths. Element
 * paths can be queried (in the same way that elements can be queried in trait `ParentElemLike`), and elements can be found
 * given an element path.
 *
 * For example, to query for the element paths of the above-mentioned block element (relative to the root element), the following
 * code can be used (if the used element implementation mixes in trait `PathAwareElemLike`):
 * {{{
 * for {
 *   blockElemPath <- xsltElem filterElemOrSelfPaths (elem => elem.resolvedName == EName("{http://www.w3.org/1999/XSL/Format}block"))
 *   if blockElemPath.containsName(EName("{http://www.w3.org/1999/XSL/Transform}template"))
 * } yield blockElemPath
 * }}}
 *
 * The `PathAwareElemLike` trait has subtrait [[eu.cdevreeze.yaidom.UpdatableElemLike]]. This trait offers functional updates
 * at given element paths. Whereas the super-traits know only about elements, this trait knows that elements have some node
 * supertype.
 *
 * Instead of functional updates at given element paths, elements can also be "transformed" functionally without specifying
 * any element paths. This is offered by trait [[eu.cdevreeze.yaidom.TransformableElemLike]], which unlike the traits above
 * has no supertraits. The Scala XML and DOM wrappers above do not mix in this trait.
 *
 * ==Some element implementations==
 *
 * The uniform query API traits, especially `ParentElemLike` and its subtrait `ElemLike` are mixed in by many element
 * implementations. In this package there are 2 immutable element implementations, [[eu.cdevreeze.yaidom.ElemBuilder]]
 * and [[eu.cdevreeze.yaidom.Elem]].
 *
 * Class [[eu.cdevreeze.yaidom.Elem]] is the default element implementation of yaidom. It extends class [[eu.cdevreeze.yaidom.Node]].
 * The latter also has subclasses for text nodes, comments, entity references and processing instructions. Class [[eu.cdevreeze.yaidom.Document]]
 * contains a document `Elem`, but is not a `Node` subclass itself.
 *
 * The [[eu.cdevreeze.yaidom.Elem]] class has the following characteristics:
 * <ul>
 * <li>It is <em>immutable</em>, and thread-safe</li>
 * <li>These elements therefore cannot be queried for their parent elements</li>
 * <li>It mixes in both query API traits [[eu.cdevreeze.yaidom.UpdatableElemLike]] and [[eu.cdevreeze.yaidom.TransformableElemLike]]</li>
 * <li>Therefore this element class offers almost all of the yaidom query API, except `HasParent`</li>
 * <li>Besides the tag name, attributes and child nodes, it keeps a `Scope`, but no `Declarations`</li>
 * <li>This makes it easy to compose these elements, as long as scopes are passed explicitly throughout the element tree</li>
 * <li>Equality is reference equality, because it is hard to come up with a sensible equality for this element class</li>
 * <li>Roundtripping cannot be entirely lossless, but this class does try to keep the attribute order (although irrelevant according to XML InfoSet)</li>
 * <li>Sub-packages `parse` and `print` offer `DocumentParser` and `DocumentPrinter` classes for parsing/serializing these default `Elem` (and `Document`) instances</li>
 * </ul>
 *
 * Creating such `Elem` trees by hand is a bit cumbersome, partly because scopes have to be passed to each `Elem` in the tree.
 * The latter is not needed if we use class [[eu.cdevreeze.yaidom.ElemBuilder]] to create element trees by hand. When the tree
 * has been fully created as `ElemBuilder`, invoke method `ElemBuilder.build(parentScope)` to turn it into an `Elem`.
 *
 * Classes `Elem` and `ElemBuilder`, like their superclasses `Node` and `NodeBuilder`, have very much in common. Both are immutable,
 * easy to compose (`ElemBuilder` instances even more so), equality is reference equality, etc. The most important differences
 * are as follows:
 * <ul>
 * <li>Instead of a `Scope`, an `ElemBuilder` contains a `Declarations`</li>
 * <li>This makes an `ElemBuilder` easier to compose than an `Elem`, because no Scope needs to be passed around throughout the tree</li>
 * <li>Class `ElemBuilder` uses a minimal query API, mixing in only traits `ParentElemLike` and `TransformableElemLike`</li>
 * <li>After all, an `ElemBuilder` neither keeps nor knows about Scopes</li>
 * </ul>
 *
 * The page-sequence element in the XML example above could have been written as `ElemBuilder` (without the inter-element whitespace) as follows:
 * {{{
 * import NodeBuilder._
 *
 * elem(
 *   qname = QName("fo:page-sequence"),
 *   attributes = Vector(QName("master-reference") -> "A4-portrait"),
 *   children = Vector(
 *     elem(
 *       qname = QName("fo:flow"),
 *       attributes = Vector(QName("flow-name") -> "xsl-region-body"),
 *       children = Vector(
 *         elem(
 *           qname = QName("fo:block"),
 *           children = Vector(
 *             text("\n" +
 *                  "            Hello, "),
 *             elem(
 *               qname = QName("xsl:value-of"),
 *               attributes = Vector(QName("select") -> "name")
 *             ),
 *             text("!\n" +
 *                  "          ")
 *           )
 *         )
 *       )
 *     )
 *   )
 * )
 * }}}
 *
 * This `ElemBuilder` (say, `eb`) lacks namespace declarations for prefixes `fo` and `xslt`. So, the following returns `false`:
 * {{{
 * eb.canBuild(Scope.Empty)
 * }}}
 * while the following returns `true`:
 * {{{
 * eb.canBuild(Scope.from("fo" -> "http://www.w3.org/1999/XSL/Format", "xsl" -> "http://www.w3.org/1999/XSL/Transform"))
 * }}}
 * Indeed,
 * {{{
 * eb.build(Scope.from("fo" -> "http://www.w3.org/1999/XSL/Format", "xsl" -> "http://www.w3.org/1999/XSL/Transform"))
 * }}}
 * returns the element tree as `Elem`.
 *
 * Note that the distinction between `ElemBuilder` and `Elem` "solves" the mismatch that immutable ("functional") element trees are
 * constructed in a bottom-up manner, while namespace scoping works in a top-down manner. (See also Anti-XML issue 78, in
 * https://github.com/djspiewak/anti-xml/issues/78).
 *
 * There are many more element implementations in yaidom, most of them in sub-packages of this package. Yaidom is extensible
 * in that new element implementations can be invented, for example elements that are better "roundtrippable" (at the expense of
 * "composability"), or yaidom wrappers around other DOM-like APIs (such as XOM or JDOM2). The current element implementations
 * in yaidom are:
 * <ul>
 * <li>Immutable class [[eu.cdevreeze.yaidom.Elem]], the default (immutable) element implementation. See above.</li>
 * <li>Immutable class [[eu.cdevreeze.yaidom.ElemBuilder]] for creating an `Elem` by hand. See above.</li>
 * <li>Immutable class [[eu.cdevreeze.yaidom.resolved.Elem]], which takes namespace prefixes out of the equation, and therefore
 * makes useful (namespace-aware) equality comparisons feasible. It mixes in the same query API traits as the default
 * element implementation.</li>
 * <li>Immutable class [[eu.cdevreeze.yaidom.indexed.Elem]], which offers views on default Elems that know the ancestry of
 * each element. It mixes in both the `ElemLike` and `HasParent` query APIs, despite being immutable! This element implementation
 * is handy for querying XML schemas, for example, because in schemas the ancestry of queried elements typically matters.</li>
 * <li>Class [[eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem]], which wraps a Scala XML Elem. It mixes in the `ElemLike` query API.</li>
 * <li>Class [[eu.cdevreeze.yaidom.dom.DomElem]], which wraps a (mutable!) DOM Element. It mixes in both the `ElemLike` and `HasParent`
 * query APIs.</li>
 * </ul>
 * This illustrates that especially trait `ElemLike` is a uniform query API in yaidom.
 *
 * ==Packages and dependencies==
 *
 * Yaidom has the following packages, and layering between packages:
 * <ol>
 * <li>Package [[eu.cdevreeze.yaidom]], with the content described above. It depends on no other yaidom packages.</li>
 * <li>Package [[eu.cdevreeze.yaidom.convert]]. It contains conversions between yaidom and DOM, Scala XML, etc.
 * This package depends on the yaidom root package.</li>
 * <li>Packages [[eu.cdevreeze.yaidom.parse]] and [[eu.cdevreeze.yaidom.print]], for parsing/printing Elems. They depend on
 * the packages mentioned above.</li>
 * <li>The other packages: [[eu.cdevreeze.yaidom.dom]], [[eu.cdevreeze.yaidom.indexed]], [[eu.cdevreeze.yaidom.resolved]],
 * [[eu.cdevreeze.yaidom.scalaxml]] and [[eu.cdevreeze.yaidom.xlink]]. They depend on (some of) the packages mentioned above,
 * and not on each other.</li>
 * </ol>
 * Indeed, all yaidom package dependencies are uni-directional.
 *
 * @author Chris de Vreeze
 */
package object yaidom
