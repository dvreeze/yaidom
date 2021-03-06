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

package eu.cdevreeze

/**
 * <em>Yaidom</em> is yet another Scala immutable DOM-like XML API. The best known Scala immutable DOM-like API is
 * the standard scala.xml API. It:
 * <ul>
 * <li>attempts to offer an XPath-like querying experience, thus somewhat blurring the distinction between nodes and node collections</li>
 * <li>lacks first-class support for XML namespaces</li>
 * <li>has limited (functional) update support</li>
 * </ul>
 *
 * Yaidom takes a different approach, avoiding XPath-like query support in its query API, and offering good namespace and decent (functional)
 * update support. Yaidom is also characterized by <em>almost mathematical precision</em> and clarity. Still, the API remains practical and
 * pragmatic. In particular, the API user has much configuration control over parsing and serialization, because yaidom exposes
 * the underlying JAXP parsers and serializers, which can be configured by the library user.
 *
 * Yaidom chooses its battles. For example, given that DTDs do not know about namespaces, yaidom offers good namespace
 * support, but ignores DTDs entirely. Of course the underlying XML parser may still validate XML against a DTD, if so desired.
 * As another example, yaidom tries to leave the handling of the gory details of XML processing (such as whitespace handling)
 * as much as possible to JAXP (and JAXP parser/serializer configuration). As yet another example, yaidom knows nothing about
 * (XML Schema) types of elements and attributes.
 *
 * As mentioned above, yaidom tries to treat basic XML processing with '''almost mathematical precision''', even if this is "incorrect".
 * At the same time, yaidom tries to be useful in practice. For example, yaidom compromises "correctness" in the following ways:
 * <ul>
 * <li>Yaidom does not generally consider documents to be nodes (called "document information items" in the XML Infoset),
 * thus introducing fewer constraints on DOM-like node construction</li>
 * <li>Yaidom does not consider attributes to be (non-child) nodes (called "attribute information items" in the XML Infoset),
 * thus introducing fewer constraints on DOM-like node construction</li>
 * <li>Yaidom does not consider namespace declarations to be attributes, thus facilitating a clear theory of namespaces</li>
 * <li>Yaidom tries to keep the order of the attributes (for better round-tripping), although attribute order is irrelevant
 * according to the XML Infoset</li>
 * <li>Very importantly, yaidom clearly distinguishes between qualified names (QNames) and expanded names (ENames),
 * which is essential in facilitating a clear theory of namespaces</li>
 * </ul>
 *
 * Yaidom, and in particular the [[eu.cdevreeze.yaidom.core]], [[eu.cdevreeze.yaidom.queryapi]], [[eu.cdevreeze.yaidom.resolved]] and
 * [[eu.cdevreeze.yaidom.simple]] sub-packages, contains the following layers:
 * <ol>
 * <li><em>basic concepts</em>, such as (qualified and expanded) names of elements and attributes (in the `core` package)</li>
 * <li>the <em>uniform query API traits</em>, to query elements for child, descendant and descendant-or-self elements (in the `queryapi` package)</li>
 * <li>some of the specific <em>element implementations</em>, mixing in those uniform query API traits (e.g. in the `resolved` and `simple` packages)</li>
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
 * They are represented by immutable classes [[eu.cdevreeze.yaidom.core.QName]] and [[eu.cdevreeze.yaidom.core.EName]], respectively.
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
 * They are represented by immutable classes [[eu.cdevreeze.yaidom.core.Declarations]] and [[eu.cdevreeze.yaidom.core.Scope]], respectively.
 *
 * Namespace declarations occur in XML, whereas in-scope namespaces do not. The latter are the accumulated effect of the
 * namespace declarations of the element itself, if any, and those in ancestor elements.
 *
 * Note: in the code examples below, we assume the following import:
 * {{{
 * import eu.cdevreeze.yaidom.core._
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
 * They are represented by immutable classes [[eu.cdevreeze.yaidom.core.PathBuilder]] and [[eu.cdevreeze.yaidom.core.Path]], respectively.
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
 * <em>element-centric</em> (unlike standard Scala XML).
 *
 * For example, consider the XML example given earlier, as a Scala XML literal named `bookstore`. We can wrap this Scala
 * XML Elem into a yaidom wrapper of type `ScalaXmlElem`, named `bookstoreElem`. Then we can query
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
 * with the same result, due to an implicit conversion from expanded names to element predicates.
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
 *   if bookElem.attributeOption(EName("ISBN")).contains("978-0981531649")
 *   authorElem <- bookElem filterElems (elem => elem.resolvedName == EName("{http://bookstore/author}Author"))
 * } yield authorElem
 * }}}
 * or:
 * {{{
 * for {
 *   bookElem <- bookstoreElem.filterChildElems(EName("{http://bookstore/book}Book"))
 *   if bookElem.attributeOption(EName("ISBN")).contains("978-0981531649")
 *   authorElem <- bookElem.filterElems(EName("{http://bookstore/author}Author"))
 * } yield authorElem
 * }}}
 *
 * We could even use operator notation, as follows:
 * {{{
 * for {
 *   bookElem <- bookstoreElem \ (elem => elem.resolvedName == EName("{http://bookstore/book}Book"))
 *   if (bookElem \@ EName("ISBN")).contains("978-0981531649")
 *   authorElem <- bookElem \\ (elem => elem.resolvedName == EName("{http://bookstore/author}Author"))
 * } yield authorElem
 * }}}
 * or:
 * {{{
 * for {
 *   bookElem <- bookstoreElem \ EName("{http://bookstore/book}Book")
 *   if (bookElem \@ EName("ISBN")).contains("978-0981531649")
 *   authorElem <- bookElem \\ EName("{http://bookstore/author}Author")
 * } yield authorElem
 * }}}
 * where `\\` stands for `filterElemsOrSelf`.
 *
 * There is no explicit support for filtering on the "self" element itself. In the example above, we might want to check if
 * the root element has the expected EName, for instance. That is easy to express using a simple idiom, however. The last
 * example then becomes:
 * {{{
 * for {
 *   bookstoreElem <- Vector(bookstoreElem)
 *   if bookstoreElem.resolvedName == EName("{http://bookstore/book}Bookstore")
 *   bookElem <- bookstoreElem \ EName("{http://bookstore/book}Book")
 *   if (bookElem \@ EName("ISBN")).contains("978-0981531649")
 *   authorElem <- bookElem \\ EName("{http://bookstore/author}Author")
 * } yield authorElem
 * }}}
 *
 * Now suppose the same XML is stored in a (org.w3c.dom) DOM tree, wrapped in a `DomElem` `bookstoreElem`.
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
 * of the minimal API. The most important (partly concrete) query API trait is [[eu.cdevreeze.yaidom.queryapi.ElemLike]]. It needs to be given
 * a method implementation to query for child elements (not child nodes in general, but just child elements!), and it offers methods to query
 * for some or all child elements, descendant elements, and descendant-or-self elements. That is, the minimal API consists
 * of abstract method `findAllChildElems`, and it offers methods such as `filterChildElems`, `filterElems` and `filterElemsOrSelf`.
 * This trait has no knowledge about elements at all, other than the fact that <em>elements can have child elements</em>.
 *
 * Trait [[eu.cdevreeze.yaidom.queryapi.ClarkElemApi]] needs minimal knowledge about elements themselves, viz. that elements have a
 * <em>"resolved"</em> (or expanded) name, and "resolved" attributes (mapping attribute expanded names to attribute values). That is,
 * it needs to be given implementations of abstract methods `resolvedName` and `resolvedAttributes`, and then offers methods to
 * query for individual attributes or the local name of the element.
 *
 * It is important to note that yaidom does not consider namespace declarations to be attributes themselves. Otherwise, there would
 * have been circular dependencies between both concepts, because attributes with namespaces require in-scope namespaces and therefore
 * namespace declarations for resolving the names of these attributes.
 *
 * Note that trait [[eu.cdevreeze.yaidom.queryapi.ElemLike]] only knows about elements, not about other kinds of nodes.
 * Of course the actual element implementations mixing in this query API know about other node types, but that knowledge is outside
 * the uniform query API. Note that the example queries above only use the minimal element knowledge that trait `ClarkElemApi`
 * has about elements. Therefore the query code can be used unchanged for different element implementations.
 *
 * Trait [[eu.cdevreeze.yaidom.queryapi.IsNavigable]] is used to navigate to an element given a Path.
 *
 * Trait [[eu.cdevreeze.yaidom.queryapi.UpdatableElemLike]] (which extends trait `IsNavigable`) offers <em>functional updates</em>
 * at given paths. Whereas the traits mentioned above know only about elements, this trait knows that elements have some node
 * super-type.
 *
 * Instead of functional updates at given paths, elements can also be "transformed" functionally without specifying
 * any paths. This is offered by trait [[eu.cdevreeze.yaidom.queryapi.TransformableElemLike]]. The Scala XML and DOM wrappers above do
 * not mix in this trait.
 *
 * ==Three uniform query API levels==
 *
 * Above, several individual query API traits were mentioned. There are, however, 3 <em>query API levels</em>
 * which are interesting for those who extend yaidom with new element implementations, but also for most users
 * of the yaidom query API. These levels are represented by "combination traits" that combine several
 * of the query API traits mentioned (or not mentioned) above.
 *
 * The most basic level is [[eu.cdevreeze.yaidom.queryapi.ClarkNodes.Elem]], which extends trait `ClarkElemApi`. Object
 * [[eu.cdevreeze.yaidom.queryapi.ClarkNodes]] also contains types for non-element nodes. All element
 * implementations that extend trait `ClarkNodes.Elem` should have a node hierarchy with all its kinds of
 * nodes extending the appropriate `ClarkNodes` member type.
 *
 * All element implementation directly or indirectly implement the `ClarkNodes.Elem` trait. The part of
 * the yaidom query API that knows about `ElemApi` querying and about ENames is the `ClarkNodes` query
 * API level. It does not know about QNames, in-scope namespaces, ancestor elements, base URIs, etc.
 *
 * The next level is [[eu.cdevreeze.yaidom.queryapi.ScopedNodes.Elem]]. It extends the `ClarkNodes.Elem`
 * trait, but offers knowledge about QNames and in-scope namespaces as well. Many element implementations
 * offer at least this query API level. The remarks about non-element nodes above also apply here, and apply below.
 *
 * The third level is [[eu.cdevreeze.yaidom.queryapi.BackingNodes.Elem]]. It extends the `ScopedNodes.Elem`
 * trait, but offers knowledge about ancestor elements and document/base URIs as well. This is the level
 * typically used for "backing elements" in "yaidom dialects", thus allowing for multiple "XML backends"
 * to be used behind "yaidom dialects". Yaidom dialects are specific "XML dialect" type-safe yaidom query APIs,
 * mixing in and leveraging trait [[eu.cdevreeze.yaidom.queryapi.SubtypeAwareElemApi]] (often in combination
 * with [[eu.cdevreeze.yaidom.queryapi.ScopedNodes.Elem]]).
 *
 * To get to know the yaidom query API and its 3 levels, it pays off to study the API documentation of traits
 * [[eu.cdevreeze.yaidom.queryapi.ClarkNodes.Elem]], [[eu.cdevreeze.yaidom.queryapi.ScopedNodes.Elem]] and
 * [[eu.cdevreeze.yaidom.queryapi.BackingNodes.Elem]].
 *
 * ==Some element implementations==
 *
 * In package `simple` there are 2 immutable element implementations, [[eu.cdevreeze.yaidom.simple.ElemBuilder]]
 * and [[eu.cdevreeze.yaidom.simple.Elem]]. Arguably, `ElemBuilder` is not an element implementation. Indeed,
 * it does not even offer the `ClarkNodes.Elem` query API.
 *
 * Class [[eu.cdevreeze.yaidom.simple.Elem]] is the <em>default element implementation</em> of yaidom. It extends class [[eu.cdevreeze.yaidom.simple.Node]].
 * The latter also has sub-classes for text nodes, comments, entity references and processing instructions. Class [[eu.cdevreeze.yaidom.simple.Document]]
 * contains a document `Elem`, but is not a `Node` sub-class itself. This node hierarchy offers the `ScopedNodes` query API,
 * so simple elements offer the `ScopedNodes.Elem` query API.
 *
 * The [[eu.cdevreeze.yaidom.simple.Elem]] class has the following characteristics:
 * <ul>
 * <li>It is <em>immutable</em>, and thread-safe</li>
 * <li>These elements therefore cannot be queried for their parent elements</li>
 * <li>It mixes in query API trait [[eu.cdevreeze.yaidom.queryapi.ScopedNodes.Elem]], [[eu.cdevreeze.yaidom.queryapi.UpdatableElemApi]] and
 * [[eu.cdevreeze.yaidom.queryapi.TransformableElemApi]]</li>
 * <li>Besides the element name, attributes and child nodes, it keeps a `Scope`, but no `Declarations`</li>
 * <li>This makes it easy to compose these elements, as long as scopes are passed explicitly throughout the element tree</li>
 * <li>Equality is reference equality, because it is hard to come up with a sensible equality for this element class</li>
 * <li>Roundtripping cannot be entirely lossless, but this class does try to retain the attribute order (although irrelevant according to XML Infoset)</li>
 * <li>Packages `parse` and `print` offer `DocumentParser` and `DocumentPrinter` classes for parsing/serializing these default
 * `Elem` (and `Document`) instances</li>
 * </ul>
 *
 * Creating such `Elem` trees by hand is a bit cumbersome, partly because scopes have to be passed to each `Elem` in the tree.
 * The latter is not needed if we use class [[eu.cdevreeze.yaidom.simple.ElemBuilder]] to create element trees by hand. When the tree
 * has been fully created as `ElemBuilder`, invoke method `ElemBuilder.build(parentScope)` to turn it into an `Elem`.
 *
 * Like their super-classes `Node` and `NodeBuilder`, classes `Elem` and `ElemBuilder` have very much in common. Both are immutable,
 * easy to compose (`ElemBuilder` instances even more so), equality is reference equality, etc. The most important differences
 * are as follows:
 * <ul>
 * <li>Instead of a `Scope`, an `ElemBuilder` contains a `Declarations`</li>
 * <li>This makes an `ElemBuilder` easier to compose than an `Elem`, because no Scope needs to be passed around throughout the tree</li>
 * <li>Class `ElemBuilder` uses a minimal query API, mixing in almost only traits `ElemLike` and `TransformableElemLike`</li>
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
 * in yaidom are for example:
 * <ul>
 * <li>Immutable class [[eu.cdevreeze.yaidom.simple.Elem]], the default (immutable) element implementation. See above.</li>
 * <li>Immutable class [[eu.cdevreeze.yaidom.simple.ElemBuilder]] for creating an `Elem` by hand. See above.</li>
 * <li>Immutable class [[eu.cdevreeze.yaidom.resolved.Elem]], which takes namespace prefixes out of the equation, and therefore
 * makes useful (namespace-aware) equality comparisons feasible. It offers the `ClarkNodes.Elem` query API (as well as
 * update/transformation support).</li>
 * <li>Immutable class [[eu.cdevreeze.yaidom.indexed.Elem]], which offers views on default Elems that know the ancestry of
 * each element. It offers the `BackingNodes.Elem` query API, so knows its ancestry, despite being immutable! This element implementation
 * is handy for querying XML schemas, for example, because in schemas the ancestry of queried elements typically matters.</li>
 * </ul>
 *
 * One yaidom wrapper that is very useful is a Saxon tiny tree yaidom wrapper, namely `SaxonElem` (JVM-only).
 * Like "indexed elements", it offers all of the `BackingNodes.Elem` query API. This element implementation is very efficient,
 * especially in memory footprint (when using the default tree model, namely tiny trees). It is therefore the most attractive element
 * implementation to use in "enterprise" production code, but only on the JVM. In combination with Saxon-EE (instead of Saxon-HE) the underlying
 * Saxon `NodeInfo` objects can even carry interesting type information.
 *
 * For ad-hoc element creation, consider using "resolved" elements. They are easy to create, because there is no need to worry about
 * namespace prefixes. Once created, they can be converted to "simple" elements, given an appropriate `Scope` (without default namespace).
 *
 * ==Packages and dependencies==
 *
 * Yaidom has the following packages, and layering between packages (mentioning the lowest layers first):
 * <ol>
 * <li>Package [[eu.cdevreeze.yaidom.core]], with the core concepts described above. It depends on no other yaidom packages.</li>
 * <li>Package [[eu.cdevreeze.yaidom.queryapi]], with the query API traits described above. It only depends on the `core` package.</li>
 * <li>Package [[eu.cdevreeze.yaidom.resolved]], with a minimal "James Clark" element implementation. It only depends on the `core` and
 * `queryapi` packages.</li>
 * <li>Package [[eu.cdevreeze.yaidom.simple]], with the default element implementation described above. It only depends on the `core` and `queryapi`
 * packages.</li>
 * <li>Package [[eu.cdevreeze.yaidom.indexed]], supporting "indexed" elements. It only depends on the `core`, `queryapi` and `simple`
 * packages.</li>
 * <li>Package `convert`. It contains conversions between default yaidom nodes on the one hand and DOM,
 * Scala XML, etc. on the other hand. The `convert` package depends on the yaidom `core`, `queryapi`, `resolved` and `simple` packages.</li>
 * <li>Package `eu.cdevreeze.yaidom.saxon`, with the Saxon wrapper element implementation described above. It only depends on the `core`, `queryapi`
 * and `convert` packages.</li>
 * <li>Packages `eu.cdevreeze.yaidom.parse` and `eu.cdevreeze.yaidom.print`, for parsing/printing Elems. They depend on
 * the packages mentioned above, except for `indexed` and `saxon`.</li>
 * <li>The other packages (except `utils`), such as `dom` and `scalaxml`. They depend on (some of) the packages mentioned above,
 * but not on each other.</li>
 * <li>Package [[eu.cdevreeze.yaidom.utils]], which depends on all the packages above.</li>
 * </ol>
 * Indeed, all yaidom package dependencies are uni-directional.
 *
 * ==Notes on performance==
 *
 * Yaidom can be quite memory-hungry. One particular cause of that is the possible creation of very many duplicate EName and
 * QName instances. This can be the case while parsing XML into yaidom documents, or while querying yaidom element trees.
 *
 * The user of the library can reduce memory consumption to a large extent, and yaidom facilitates that.
 *
 * As for querying, prefer:
 * {{{
 * import ClarkElemApi._
 *
 * bookstoreElem filterElemsOrSelf withEName("http://bookstore/book", "Book")
 * }}}
 * to:
 * {{{
 * bookstoreElem.filterElemsOrSelf(EName("http://bookstore/book", "Book"))
 * }}}
 * to avoid unnecessary (large scale) EName object creation.
 *
 * To reduce the memory footprint of parsed XML trees, see [[eu.cdevreeze.yaidom.core.ENameProvider]] and [[eu.cdevreeze.yaidom.core.QNameProvider]].
 *
 * For example, during the startup phase of an application, we could set the global ENameProvider as follows:
 * {{{
 * ENameProvider.globalENameProvider.become(new ENameProvider.ENameProviderUsingImmutableCache(knownENames))
 * }}}
 *
 * Note that the global ENameProvider or QNameProvider can typically be configured rather late during development, but the
 * memory cost savings can be substantial once configured. Also note that the global ENameProvider or QNameProvider can be used implicitly in
 * application code, by writing:
 * {{{
 * bookstoreElem filterElemsOrSelf getEName("http://bookstore/book", "Book")
 * }}}
 * using an implicit ENameProvider, whose members are in scope. Still, for querying the first alternative using `withEName` is
 * better, but there are likely many scenarios in yaidom client code where an implicit ENameProvider or QNameProvider makes sense.
 *
 * The bottom line is that yaidom can be configured to be far less memory-hungry, and that yaidom client code can also take
 * some responsibility in reducing memory usage. Again, the Saxon wrapper implementation is an excellent and efficient choice (but only on the JVM).
 *
 * @author Chris de Vreeze
 */
package object yaidom
