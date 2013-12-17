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
 * the standard scala.xml API and Anti-XML. The latter API is considered by many to be an improvement over the former, but both APIs:
 * <ul>
 * <li>attempt to offer an XPath-like querying experience, thus somewhat blurring the distinction between nodes and node collections</li>
 * <li>lack first-class support for namespaces (and namespace URIs)</li>
 * <li>have limited (functional) update support</li>
 * </ul>
 *
 * Yaidom takes a different approach, avoiding XPath-like query support, and offering good namespace and decent (functional) update
 * support. Yaidom is also characterized by <em>mathematical precision</em> and clarity. Still, the API remains practical and
 * pragmatic. In particular, the API user has much configuration control over parsing and serialization, because yaidom exposes
 * the underlying JAXP parsers and serializers, which can be configured by the library user.
 *
 * Yaidom chooses its battles. For example, given that DTDs do not know about namespaces, yaidom offers good namespace
 * support, but ignores DTDs entirely. Of course the underlying XML parser may still validate XML against a DTD, if so desired.
 * As another example, yaidom tries to leave the handling of the gory details of XML processing (such as whitespace handling)
 * as much as possible to JAXP (and JAXP parser/serializer configuration). As yet another example, yaidom knows nothing about
 * (XML Schema) types of elements and attributes.
 *
 * Yaidom, and in particular this package, contains the following layers:
 * <ol>
 * <li><em>basic concepts</em>, such as (qualified and expanded) names of elements and attributes</li>
 * <li>the <em>uniform query API traits</em>, to query elements for child, descendant and descendant-or-self elements</li>
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
 * <li><em>qualified names</em>: prefixed names, such as `book:Title`, and unprefixed names, such as `Edition`</li>
 * <li><em>expanded names</em>: having a namespace, such as `{http://bookstore/book}Title` (in James Clark notation),
 * and not having a namespace, such as `Edition`</li>
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
 * Note: in the code examples below, we assume the following import:
 * {{{
 * import eu.cdevreeze.yaidom._
 * }}}
 *
 * To see the resolution of qualified names in action, consider the following sample XML:
 *
 * {{{
 * <book:Bookstore xmlns:book="http://bookstore/book" xmlns:auth="http://bookstore/author">
 *   <book:Book ISBN="978-0321356680" Price="35" Edition="2">
 *     <book:Title>Effective Java (2nd Edition)</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Joshua</auth:First_Name>
 *         <auth:Last_Name>Bloch</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 *   <book:Book ISBN="978-0981531649" Price="35" Edition="2">
 *     <book:Title>Programming in Scala: A Comprehensive Step-by-Step Guide, 2nd Edition</book:Title>
 *     <book:Authors>
 *       <auth:Author>
 *         <auth:First_Name>Martin</auth:First_Name>
 *         <auth:Last_Name>Odersky</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Lex</auth:First_Name>
 *         <auth:Last_Name>Spoon</auth:Last_Name>
 *       </auth:Author>
 *       <auth:Author>
 *         <auth:First_Name>Bill</auth:First_Name>
 *         <auth:Last_Name>Venners</auth:Last_Name>
 *       </auth:Author>
 *     </book:Authors>
 *   </book:Book>
 * </book:Bookstore>
 * }}}
 *
 * Consider the last element with qualified name `QName("book:Book")`. To resolve this qualified name as expanded name, we need to
 * know the namespaces in scope at that element. To compute the in-scope namespaces, we need to accumulate the namespace
 * declarations of the last `book:Book` element and of its ancestor element(s), starting with the root element.
 *
 * The start Scope is "parent scope" `Scope.Empty`. Then, in the root element we find namespace declarations:
 * {{{
 * Declarations.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author")
 * }}}
 * This leads to the following namespaces in scope at the root element:
 * {{{
 * Scope.Empty.resolve(Declarations.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author"))
 * }}}
 * which is equal to:
 * {{{
 * Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author")
 * }}}
 * We find no other namespace declarations in the last `book:Book` element or its ancestor(s), so the computed scope is also the scope
 * of the last `book:Book` element.
 *
 * Then `QName("book:Book")` is resolved as follows:
 * {{{
 * Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author").resolveQNameOption(QName("book:Book"))
 * }}}
 * which is equal to:
 * {{{
 * Some(EName("{http://bookstore/book}Book"))
 * }}}
 *
 * This namespace support in yaidom has mathematical rigor. The immutable classes `QName`, `EName`, `Declarations` and `Scope` have
 * precise definitions, reflected in their implementations, and they obey some interesting properties. For example, if we correctly
 * define Scope operation `relativize` (along with `resolve`), we get:
 * {{{
 * scope1.resolve(scope1.relativize(scope2)) == scope2
 * }}}
 *
 * This may not sound like much, but by getting the basics right, yaidom succeeds in offering first-class support for XML
 * namespaces, without the magic and namespace-related bugs often found in other XML libraries.
 *
 * There are 2 other basic concepts in this package, representing paths to elements:
 * <ul>
 * <li><em>path builders</em></li>
 * <li><em>paths</em></li>
 * </ul>
 * They are represented by immutable classes [[eu.cdevreeze.yaidom.PathBuilder]] and [[eu.cdevreeze.yaidom.Path]], respectively.
 *
 * Path builders are like canonical XPath expressions, yet they do not contain the root element itself, and indexing
 * starts with 0 instead of 1.
 *
 * For example, the last name of the first author of the last book element has path:
 * {{{
 * Path.from(
 *   EName("{http://bookstore/book}Book") -> 1,
 *   EName("{http://bookstore/book}Authors") -> 0,
 *   EName("{http://bookstore/author}Author") -> 0,
 *   EName("{http://bookstore/author}Last_Name") -> 0
 * )
 * }}}
 *
 * This path could be written as path builder as follows:
 * {{{
 * PathBuilder.from(QName("book:Book") -> 1, QName("book:Authors") -> 0, QName("auth:Author") -> 0, QName("auth:Last_Name") -> 0)
 * }}}
 *
 * Using the Scope mentioned earlier, the latter path builder resolves to the path given before that, by
 * invoking method `PathBuilder.build(scope)`. In order for this to work, the Scope must be <em>invertible</em>. That is,
 * there must be a one-to-one correspondence between prefixes ("" for the default namespace) and namespace URIs, because
 * otherwise the index numbers may differ. Also note that the prefixes `book` and `auth` in the path builder are
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
 * For example, consider the XML example given earlier, as a Scala XML literal named `bookstore`. We can wrap this Scala
 * XML Elem into a yaidom wrapper of type [[eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem]], named `bookstoreElem`. Then we can query
 * for all books, that is, all <em>descendant-or-self</em> elements with resolved (or expanded) name `EName("{http://bookstore/book}Book")`,
 * as follows:
 * {{{
 * bookstoreElem filterElemsOrSelf (elem => elem.resolvedName == EName("{http://bookstore/book}Book"))
 * }}}
 * The result would be an immutable IndexedSeq of `ScalaXmlElem` instances, holding 2 book elements.
 *
 * We could instead have written:
 * {{{
 * bookstoreElem.filterElemsOrSelf(EName("{http://bookstore/book}Book"))
 * }}}
 * with the same result.
 *
 * Instead of searching for appropriate descendant-or-self elements, we could have searched for <em>descendant</em> elements only,
 * without altering the result in this case:
 * {{{
 * bookstoreElem filterElems (elem => elem.resolvedName == EName("{http://bookstore/book}Book"))
 * }}}
 * or:
 * {{{
 * bookstoreElem.filterElems(EName("{http://bookstore/book}Book"))
 * }}}
 *
 * We could even have searched for appropriate <em>child</em> elements only, without altering the result in this case:
 * {{{
 * bookstoreElem filterChildElems (elem => elem.resolvedName == EName("{http://bookstore/book}Book"))
 * }}}
 * or:
 * {{{
 * bookstoreElem.filterChildElems(EName("{http://bookstore/book}Book"))
 * }}}
 * or, knowing that all child elements are books:
 * {{{
 * bookstoreElem.findAllChildElems
 * }}}
 *
 * We could find all authors of the Scala book as follows:
 * {{{
 * for {
 *   bookElem <- bookstoreElem filterChildElems (elem => elem.resolvedName == EName("{http://bookstore/book}Book"))
 *   if bookElem.attributeOption(EName("ISBN")) == Some("978-0981531649")
 *   authorElem <- bookElem filterElems (elem => elem.resolvedName == EName("{http://bookstore/author}Author"))
 * } yield authorElem
 * }}}
 * or:
 * {{{
 * for {
 *   bookElem <- bookstoreElem.filterChildElems(EName("{http://bookstore/book}Book"))
 *   if bookElem.attributeOption(EName("ISBN")) == Some("978-0981531649")
 *   authorElem <- bookElem.filterElems(EName("{http://bookstore/author}Author"))
 * } yield authorElem
 * }}}
 *
 * We could even use operator notation, as follows:
 * {{{
 * for {
 *   bookElem <- bookstoreElem \ (elem => elem.resolvedName == EName("{http://bookstore/book}Book"))
 *   if (bookElem \@ EName("ISBN")) == Some("978-0981531649")
 *   authorElem <- bookElem \\ (elem => elem.resolvedName == EName("{http://bookstore/author}Author"))
 * } yield authorElem
 * }}}
 * or:
 * {{{
 * for {
 *   bookElem <- bookstoreElem \ EName("{http://bookstore/book}Book")
 *   if (bookElem \@ EName("ISBN")) == Some("978-0981531649")
 *   authorElem <- bookElem \\ EName("{http://bookstore/author}Author")
 * } yield authorElem
 * }}}
 * where `\\` stands for `filterElemsOrSelf`.
 *
 * Now suppose the same XML is stored in a (org.w3c.dom) DOM tree, wrapped in a [[eu.cdevreeze.yaidom.dom.DomElem]] `bookstoreElem`.
 * Then the same queries would use exactly the same code as above! The result would be a collection of `DomElem` instances
 * instead of `ScalaXmlElem` instances, however. There are many more element implementations in yaidom, and they share
 * (most of) the same query API. Therefore this query API is called a <em>uniform</em> query API.
 *
 * The last example, using operator notation, looks a bit more "XPath-like". It is more verbose than queries in Scala XML, however,
 * partly because in yaidom these operators cannot be chained. Yet this is with good reason. Yaidom does not blur the
 * distinction between elements and element collections, and therefore does not offer any XPath experience. The small price
 * paid in verbosity is made up for by precision. The yaidom query API traits have very precise definitions of their
 * operations, as can be seen in the corresponding documentation.
 *
 * The uniform query API traits turn minimal APIs into richer APIs, where each richer API is defined very precisely in terms
 * of the minimal API. The top-level query API trait is [[eu.cdevreeze.yaidom.ParentElemLike]]. It needs to be given a method
 * implementation to query for child elements (not child nodes in general, but just child elements!), and it offers methods to query
 * for some or all child elements, descendant elements, and descendant-or-self elements. That is, the minimal API consists
 * of abstract method `findAllChildElems`, and it offers methods such as `filterChildElems`, `filterElems` and `filterElemsOrSelf`.
 * This trait has no knowledge about elements at all, other than the fact that <em>elements can have child elements</em>.
 *
 * Sub-trait [[eu.cdevreeze.yaidom.ElemLike]] adds minimal knowledge about elements themselves, viz. that elements have a
 * <em>"resolved"</em> (or expanded) name, and "resolved" attributes (mapping attribute expanded names to attribute values). That is,
 * it needs to be given implementations of abstract methods `resolvedName` and `resolvedAttributes`, and then offers methods to
 * query for attributes or child/descendant/descendant-or-self elements with a given expanded name. The trait is trivially defined
 * in terms of its super-trait.
 *
 * It is important to note that yaidom does not consider namespace declarations to be attributes themselves. Otherwise, there would
 * have been circular dependencies between both concepts, because attributes with namespaces require in-scope namespaces and therefore
 * namespace declarations for resolving the names of these attributes.
 *
 * Note that traits [[eu.cdevreeze.yaidom.ElemLike]] and [[eu.cdevreeze.yaidom.ParentElemLike]] only know about elements, not
 * about other kinds of nodes. Of course the actual element implementations mixing in this query API know about other node
 * types, but that knowledge is outside the uniform query API. Note that the example queries above only use the minimal
 * element knowledge that traits `ElemLike` and `ParentElemLike` have about elements. Therefore the query code can be used
 * unchanged for different element implementations.
 *
 * The `ElemLike` trait has sub-trait [[eu.cdevreeze.yaidom.PathAwareElemLike]]. It adds knowledge about <em>paths</em>.
 * Paths can be queried (in the same way that elements can be queried in trait `ParentElemLike`), and elements can be found
 * given a path.
 *
 * For example, to query for the Scala book authors, the following alternative code can be used (if the used element
 * implementation mixes in trait `PathAwareElemLike`, which is not the case for the Scala XML and DOM wrappers above):
 * {{{
 * for {
 *   authorPath <- bookstoreElem filterElemOrSelfPaths (elem => elem.resolvedName == EName("{http://bookstore/author}Author"))
 *   if authorPath.entries.contains(Path.Entry(EName("{http://bookstore/book}Book"), 1))
 * } yield bookstoreElem.getElemOrSelfByPath(authorPath)
 * }}}
 *
 * The `PathAwareElemLike` trait has sub-trait [[eu.cdevreeze.yaidom.UpdatableElemLike]]. This trait offers <em>functional updates</em>
 * at given paths. Whereas the super-traits know only about elements, this trait knows that elements have some node
 * super-type.
 *
 * Instead of functional updates at given paths, elements can also be "transformed" functionally without specifying
 * any paths. This is offered by trait [[eu.cdevreeze.yaidom.TransformableElemLike]], which unlike the traits above
 * has no super-traits. The Scala XML and DOM wrappers above do not mix in this trait.
 *
 * ==Some element implementations==
 *
 * The uniform query API traits, especially `ParentElemLike` and its sub-trait `ElemLike` are mixed in by many element
 * implementations. In this package there are 2 immutable element implementations, [[eu.cdevreeze.yaidom.ElemBuilder]]
 * and [[eu.cdevreeze.yaidom.Elem]].
 *
 * Class [[eu.cdevreeze.yaidom.Elem]] is the <em>default element implementatio</em>n of yaidom. It extends class [[eu.cdevreeze.yaidom.Node]].
 * The latter also has sub-classes for text nodes, comments, entity references and processing instructions. Class [[eu.cdevreeze.yaidom.Document]]
 * contains a document `Elem`, but is not a `Node` sub-class itself.
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
 * <li>Roundtripping cannot be entirely lossless, but this class does try to retain the attribute order (although irrelevant according to XML InfoSet)</li>
 * <li>Sub-packages `parse` and `print` offer `DocumentParser` and `DocumentPrinter` classes for parsing/serializing these default `Elem` (and `Document`) instances</li>
 * </ul>
 *
 * Creating such `Elem` trees by hand is a bit cumbersome, partly because scopes have to be passed to each `Elem` in the tree.
 * The latter is not needed if we use class [[eu.cdevreeze.yaidom.ElemBuilder]] to create element trees by hand. When the tree
 * has been fully created as `ElemBuilder`, invoke method `ElemBuilder.build(parentScope)` to turn it into an `Elem`.
 *
 * Like their super-classes `Node` and `NodeBuilder`, classes `Elem` and `ElemBuilder` have very much in common. Both are immutable,
 * easy to compose (`ElemBuilder` instances even more so), equality is reference equality, etc. The most important differences
 * are as follows:
 * <ul>
 * <li>Instead of a `Scope`, an `ElemBuilder` contains a `Declarations`</li>
 * <li>This makes an `ElemBuilder` easier to compose than an `Elem`, because no Scope needs to be passed around throughout the tree</li>
 * <li>Class `ElemBuilder` uses a minimal query API, mixing in only traits `ParentElemLike` and `TransformableElemLike`</li>
 * <li>After all, an `ElemBuilder` neither keeps nor knows about Scopes, so does not know about resolved element/attribute names</li>
 * </ul>
 *
 * The Effective Java book element in the XML example above could have been written as `ElemBuilder` (without the inter-element whitespace) as follows:
 * {{{
 * import NodeBuilder._
 *
 * elem(
 *   qname = QName("book:Book"),
 *   attributes = Vector(QName("ISBN") -> "978-0321356680", QName("Price") -> "35", QName("Edition") -> "2"),
 *   children = Vector(
 *     elem(
 *       qname = QName("book:Title"),
 *       children = Vector(
 *         text("Effective Java (2nd Edition)")
 *       )
 *     ),
 *     elem(
 *       qname = QName("book:Authors"),
 *       children = Vector(
 *         elem(
 *           qname = QName("auth:Author"),
 *           children = Vector(
 *             elem(
 *               qname = QName("auth:First_Name"),
 *               children = Vector(
 *                 text("Joshua")
 *               )
 *             ),
 *             elem(
 *               qname = QName("auth:Last_Name"),
 *               children = Vector(
 *                 text("Bloch")
 *               )
 *             )
 *           )
 *         )
 *       )
 *     )
 *   )
 * )
 * }}}
 *
 * This `ElemBuilder` (say, `eb`) lacks namespace declarations for prefixes `book` and `auth`. So, the following returns `false`:
 * {{{
 * eb.canBuild(Scope.Empty)
 * }}}
 * while the following returns `true`:
 * {{{
 * eb.canBuild(Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author"))
 * }}}
 * Indeed,
 * {{{
 * eb.build(Scope.from("book" -> "http://bookstore/book", "auth" -> "http://bookstore/author"))
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
 * each element. It mixes in the `ElemLike` query API, but knows its ancestry, despite being immutable! This element implementation
 * is handy for querying XML schemas, for example, because in schemas the ancestry of queried elements typically matters.</li>
 * <li>Immutable class [[eu.cdevreeze.yaidom.docaware.Elem]], which is like `indexed.Elem`, but also stores the document URI.</li>
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
 * <li>Package [[eu.cdevreeze.yaidom.convert]]. It contains conversions between default yaidom nodes on the one hand and DOM,
 * Scala XML, etc. on the other hand. The `convert` package depends on the yaidom root package.</li>
 * <li>Packages [[eu.cdevreeze.yaidom.parse]] and [[eu.cdevreeze.yaidom.print]], for parsing/printing Elems. They depend on
 * the packages mentioned above.</li>
 * <li>The other packages: [[eu.cdevreeze.yaidom.dom]], [[eu.cdevreeze.yaidom.indexed]], [[eu.cdevreeze.yaidom.docaware]],
 * [[eu.cdevreeze.yaidom.resolved]] and [[eu.cdevreeze.yaidom.scalaxml]]. They depend on (some of) the packages mentioned above,
 * and not on each other.</li>
 * </ol>
 * Indeed, all yaidom package dependencies are uni-directional.
 *
 * @author Chris de Vreeze
 */
package object yaidom
