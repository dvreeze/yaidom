=========
CHANGELOG
=========


1.5.0-M1
========

Version 1.5.0-M1 improves the functional query API. It is now more consistent with the query API and transformation API.
It is hopefully useful and easy to use (especially methods like updateTopmostElemsOrSelf), and should have good runtime performance.
Update support for indexed elements is also planned for version 1.5.0, but is not yet available in version 1.5.0-M1.

The main changes in this version are:

* Trait ``UpdatableElemApi`` has been enhanced with many new functional update methods, deprecating the old updatedXXX methods
* The simple ``Document`` class has been enhanced with several of these new update methods too (using delegation)
* Method ``findAllChildElemsWithPathEntries`` is now in trait ``IsNavigableApi`` (for the user this makes no difference)
* Class ``ElemWithPath`` has been added as a very lightweight "indexed element", and is used in the new update support
* Added lazy indexed elements, trading query performance for construction time performance
* Easy creation of ``IndexedClarkElem`` and ``IndexedScopedElem`` instances
* Document parsers and printers can now be configured with a custom conversion strategy
* Bug fix for yaidom-0003, and partial bug fix for yaidom-0002
* Removal of previously deprecated code

Upgrading from version 1.4.2 to this version requires recompilation of code using yaidom. Other than that, successful
compilation is likely, but deprecation warnings will occur for much of the old functional update API. The document
parsers and printers now have an extra conversion strategy primary constructor parameter, so if these constructors are
used instead of the factory methods, compilation errors will occur, but they are easy to fix (prefer the factory methods).


1.4.2
=====

Version 1.4.2 undid the deprecation warnings on indexed element and document apply (factory) methods. This version is what version
1.4.0 should have been, and it is advisable to prefer this version over 1.4.0 and 1.4.1.


1.4.1
=====

Version 1.4.1 fixes broken XML Base support, due to a regression. It contains some breaking changes, but only compared
to version 1.4.0 (which is broken in its XML Base support). The most important changes are:

* Fixed the bug in getting the parent base URI of an indexed element
* URI resolution (in XML Base) is sensitive, so indexed element creation now requires a URI resolution strategy to be passed
* Old indexed element factory methods have been deprecated (they use a default URI resolver)

Indexed element creation now goes through a builder, which keeps a URI resolver. The builder could be a global long-lived object.


1.4.0
=====

Version 1.4.0 combines the changes in the 3 milestone releases leading up to this version. For example, it supports:

* XML declarations
* retained document child order
* indexed elements with different underlying element types
* easy conversion of different element types to resolved elements
* better functional update support
* removing the distinction between indexed and docaware elements, and deprecation of docaware elements

Some of these features are supported by cleant up query API traits, without significantly altering the public query API
of the different element implementations. For example:

* indexed documents contain child nodes of quite different types, but they now have a common useful super-type; this is used for keeping the document child order
* traits ``ScopedElemApi`` (offered by all "practical" element implementations) and its super-type ``ClarkElemApi`` (also offered by "minimal" element implementations such as resolved elements) are quite central query API traits; "indexed" element support also uses this distinction

There are some breaking changes in this release, compared to version 1.3.6, but fixing compilation errors in code using
yaidom should be rather straightforward. For example:

* method ``findChildElemByPathEntry`` no longer can nor needs to be overridden
* construction of indexed documents may need an extra parameter for the optional XML declaration
* sometimes conversions from ``Nodes.Comment`` to ``simple.Comment`` (or similar conversions for processing instructions) need to be inserted
* method ``ancestryENames`` is now called ``reverseAncestryENames``, etc.
* there may be very many deprecation warnings for the use of docaware elements, but they can be fixed at any time

When creating a new element implementation (with yaidom 1.4.0), consider the following design choices:

* do we want to have a custom node hierarchy for these elements, including text nodes, comment nodes, etc.?

  * if so, mix in the ``Nodes.Node`` sub-types throughout the custom node hierarchy
  * and consider adding a custom ``CanBeDocumentChild`` sub-type that is also a node in this hierarchy
  * if not, still mix in ``Nodes.Elem`` into the custom element type, thus promising that the element can be a document child
  * for the custom element and text node types, even consider mixing in the ``ResolvedNode.Node`` sub-types (for easy conversions to resolved elements)

* do we want to have a custom document type?

  * if so, let it mix in ``DocumentApi``
  * and let it have child nodes that at least have type ``CanBeDocumentChild`` (or a more appropriate sub-type) in common

* what element query API traits do we want the element implementation to offer?

  * is it a minimal element implementation offering just the ``ClarkElemApi`` query API (and ``ClarkElemLike`` implementation)?
  * or is it a practical element implementation offering the ``ScopedElemApi`` query API?
  * do we want the element to be "indexed", thus using types like ``IndexedScopedElemApi`` (or even final class ``IndexedScopedElem``)?
  * do we want to mix in other traits for functional updates, transformations etc.?

* what state does the element implementation have?

  * if the element is a wrapper around an element from other libraries (especially if mutable), the state should be only the wrapped element


1.4.0-M3
========

Version 1.4.0-M3 made some relatively small (but possibly breaking) changes compared to version 1.4.0-M2.

The main changes in this version are:

* Docaware elements now deprecated
* Improved ``Scope.includingNamespace`` etc., and therefore "editable element support"
* Added methods ``plusChildren`` and ``withChildSeqs``
* Document child order is retained (for different document implementations)
* DOM wrapper documents are no longer nodes, according to yaidom
* SAX-based parsing now also parses the XML declaration, if any
* Separated ``ResolvedNodes.Node`` (convertible to resolved elements) from ``Nodes.Node`` (little more than marker traits)


1.4.0-M2
========

Version 1.4.0-M2 mainly fixed a potential performance problem, introduced with version 1.4.0-M1.

The main changes in this version are:

* Indexed elements (formerly docaware elements) again store the parent base URI, for fast base URI computation
* The docaware package is finally obsolete, in that it now only contains aliases to types of indexed elements and documents and their companion objects
* Generic class IndexedDocument now only takes one type parameter (for the element) instead of two


1.4.0-M1
========

Version 1.4.0-M1 made the core of yaidom meaner and cleaner, except for the addition of XML declaration support.
There are breaking changes, but (with recompilation of code using yaidom) there should not be too many of them.

The changes in this version are:

* There are now 2 main query API abstractions, that combine several orthogonal query API traits:

  * ``ClarkElemApi``, which reminds of the James Clark minimal XML element tree abstraction
  * ``ScopedElemApi``, which extends ``ClarkElemApi``, forming the minimal practical XML element tree abstraction (with QNames and Scopes)
  
* ``ScopedElemApi`` now (indirectly) extends ``IsNavigableApi``:

  * What's more, even ``ClarkElemApi`` extends ``IsNavigableApi``
  * After all, this makes sense for "James Clark element trees", and 2 main query API abstractions suffice
  * ``ClarkElemApi`` extends ``ElemApi``, ``IsNavigableApi``, ``HasENameApi`` and ``HasTextApi``
  * ``ScopedElemApi`` extends ``ClarkElemApi``, ``HasQNameApi`` and ``HasScopeApi``
  * So the net effect on ``ScopedElemApi`` is that it now (indirectly) mixes in ``IsNavigableApi``
  * Also added method ``findReverseAncestryOrSelfByPath`` to ``IsNavigableApi`` (e.g. for fast XML Base computation)
  
* Made "indexed" elements much more generic, and removed the distinction between "indexed" and "docaware" documents:

  * New trait ``IndexedClarkElemApi``, which extends ``ClarkElemApi``, abstracts over indexed elements
  * New trait ``IndexedScopedElemApi`` is similar, but it extends ``ScopedElemApi`` as well as ``IndexedClarkElemApi``
  * Classes ``IndexedClarkElem`` and ``IndexedScopedElem`` extend ``IndexedClarkElemApi`` and ``IndexedScopedElemApi``, respectively
  * The old indexed elements are type ``IndexedScopedElem[simple.Elem]``
  * And so are the old docaware elements, so they can be deprecated soon!
  * Indeed indexed elements now have XML Base support
  * The indexed and docaware Elem companion objects (currently) remained (as did the indexed Document classes/objects)
  
* Support for XML declarations in document classes
* Added some convenience methods to ``Scope``, and used them in new element editor utilities
* Conversions from yaidom to SAX events no longer internal to DocumentPrinterUsingSax

* Added minimal node tree abstraction (``Nodes.Node`` and sub-types):

  * This helped in removing the (wrong) dependency of the "simple" package on the "resolved" package
  * What's more, resolved elements can now be created from other element implementations than just simple elements

* Small bug fixes, such as improved SAX-based parsing and more reliable DOM to yaidom conversions
* Many more tests


1.3.6
=====

Version 1.3.6 removed the alternative "docaware" and "indexed" elements introduced in version 1.3.5. These element
implementations (optimized for fast creation) offer too little "bang for the buck", so they have been removed.
As for "docaware" and "indexed" elements, they are again as in version 1.3.4. No other changes were made in this
release.


1.3.5
=====

Version 1.3.5 is a small performance release. There are no breaking changes. There are now 2 versions of "docaware" and
"indexed" elements, with the default version being optimized for fast querying, and the alternative version being optimized
for fast creation. The dependency on Apache Commons is gone (and pretty printing output is somewhat different).

The changes in this version are:

* No more dependency on Apache Commons

  * Pretty printing of element trees no longer does any "Java escaping", but outputs Scala multiline string literals instead
  * The resulting tree representation is no longer valid Scala code if the "multiline string" contains triple quotes
  * This rare scenario can be dealt with on an ad-hoc basis, if the tree representation happens to be used as Scala code
  * Pretty printing is probably faster than before, due to the fact that Apache Commons "Java escaping" is gone
  
* Added alternative "docaware" and "indexed" elements

  * They live in the ``docaware.alt`` and ``indexed.alt`` sub-packages
  * The alternatives are optimized for fast creation, not for fast querying
  * Therefore, they make better "backing" objects of "sub-type-aware" elements
  * For code re-use, super-traits ``AbstractDocawareElem`` and ``AbstractIndexedElem`` have been introduced

* Bug fixes

  * Bug fix in method ``plusChild``
  * Bug fix in error message of ``ScopedElemLike.textAsResolvedQNameOption``
  * Bug fixes in test code, found by the excellent Artima SuperSafe tool
  * Moved the ``equals`` and ``hashCode`` methods up, from the element class to the node class (in 2 element implementations)


1.3.4
=====

Version 1.3.4 is a minor performance release. There are no breaking changes. The performance improvements are in
the construction of the core objects, such as expanded names, qualified names, etc.

The changes in this version are:

* ``EName`` and ``QName`` construction has become less expensive

  * This is important, since these names are created so often
  * The increased construction speed comes at the expense of removed validity checks
  * These checks can still be performed, using new method ``validated``, but that is the responsibility of the user
  * Note that class ``javax.xml.namespace.QName`` also performs no validity checks on the passed construction parameters

* ``Scope`` and ``Declarations`` construction has become less expensive

  * This is important, since these objects are created so often
  * The checks are still there, but are cheaper, because they now involve much less collections processing
  * In this case, it is rather important to retain the checks, for internal consistency and conceptual clarity
  * For example, the "xml" namespace gets "special" treatment in the yaidom "namespaces theory"

This release was made after profiling by Andrea Desole and Nick Evans had shown that much time was spent in creation
of yaidom core objects.


1.3.3
=====

Version 1.3.3 is a maintenance release. The (few) breaking changes are hardly interesting. The performance fix
in attribute retrieval may be the most important change in this release.

The changes in this version are:

* Breaking change: removed ``TreeReprParsers``

  * Hence no more parsing of the element tree string format
  * No more dependency on Scala parser combinators

* Breaking change: better streaming support in ``StaxEventsToYaidomConversions``

  * Also renamed, refactored and added "event state" data classes, for better streaming support

* Performance fix in ``HasEName.attributeOption`` (the inefficient ``toMap`` conversion is gone)
* More tests (XML Base, i18n, etc.), and refactored tests
* Woodstox StAX parser used in test code (for XML 1.1 support)


1.3.2
=====

Version 1.3.2 is like version 1.3.1, but with more documentation and test cases with respect to XML Base support in
doc-aware elements.


1.3.1
=====

Version 1.3.1 is like version 1.3, except that XML Base support has been improved with respect to performance
(in version 1.3 XML Base support was too slow to be useful).

Breaking change: method ``baseUriOfAncestorOrSelf`` has been removed. Doc-aware elements now also keep the parent
base URI as state.


1.3
===

Version 1.3 is like version 1.2, except that the aliases in the root package to ``core`` and ``simple`` have been
removed entirely.

Moreover, method ``baseUri`` has been added to ``docaware.Elem`` (thus implementing XML Base).

Note that versions 1.1 and 1.2 were only meant as intermediate versions leading up to version 1.3. It makes sense to
compare version 1.3 to version 1.0 w.r.t. performance. In version 1.0, "simple" elements stored (in each element node!)
a Map from path entries to child node indices. In version 1.3 (even in version 1.1) that is no longer the case.

This means that path-based navigation (see ``IsNavigableApi``) is no longer effectively in constant time. Hence path-based
navigation in bulk, and as a consequence functional updates in bulk (see ``UpdatableElemApi``) are much slower in
version 1.3 than in version 1.0! So bulk navigation is now really a bad idea.

The upside is that in version 1.3 there are no longer any costs associated with the above-mentioned Map (per element).
As a consequence, in version 1.3 parsing and transforming (simple) elements is a bit faster and uses somewhat less
memory than in version 1.0. Given that typically bulk navigation is avoided, the overall performance is better using
version 1.3 than version 1.0 of yaidom.


1.2
===

Version 1.2 is like version 1.1, except that the aliases in the root package to ``core`` and ``simple`` have been
deprecated. In version 1.3, these deprecated aliases will be removed.


1.1
===

Version 1.1 is much more than a minor release. It has a lot of breaking changes. See the road map document.

Here is why yaidom 1.1 is an important release:

* Yaidom has been reconstructed by making the query API cleaner and more orthogonal under the hood, and therefore more flexible
* Related to this query API reorganization: the top-level package has been split into 3 sub-packages
* Most element implementations now offer more of the yaidom query API, and therefore become more interchangeable
* Yaidom is now both faster and less memory-hungry
* Yaidom is not only extensible w.r.t. element implementations (even more so than before), but also to support "XML dialects"
* Namespace-related utilities have been added

The (mostly breaking) changes in this version are:

* The root package has been split into sub-packages ``core``, ``queryapi`` and ``simple``

  * Package ``core`` contains core concepts, such as expanded names, qualified names etc.
  * Package ``queryapi`` contains the query API traits
  * Package ``simple`` contains the default (simple) element implementation
  * In version 1.1, there are aliases to ``core`` and ``simple`` classes, to ease the transition to yaidom 1.2 and 1.3
  
* The query API traits have been re-organized, renamed, and made more orthogonal:

  * The old inheritance hierarchy is gone
  * The ``PathAwareElemApi`` trait is gone, with no replacement (use indexed elements instead)
  * ``ParentElemApi`` (1.0) has been renamed to ``ElemApi``
  * ``ElemApi`` (1.0) is now ``ElemApi with HasENameApi``
  * ``NavigableElemApi`` (1.0) is now ``ElemApi with HasENameApi with IsNavigableApi``
  * ``UpdatableElemApi`` minus ``PathAwareElemApi`` (1.0) is now ``ElemApi with HasENameApi with UpdatableElemApi``
  * ``SubtypeAwareParentElemApi`` (1.0) has been renamed to ``SubtypeAwareElemApi``
  * The (1.1) combination ``ElemApi with HasENameApi with HasQNameApi with HasScopeApi with HasTextApi`` (with some additional methods) is called ``ScopedElemApi``
  
* Most element implementations now mix in ``ScopedElemApi with IsNavigableApi``, therefore offering almost the same query API
* Yaidom (simple, docaware, indexed) elements now store less data per element, thus reducing memory usage

  * Not only memory usage went down, but yaidom became faster as well (unless performing Path-based navigation in bulk)
  
* A test case shows how yaidom (and its ``SubtypeAwareElemApi`` query API trait) can be used to support individual XML dialects

  * The test case also shows how to do that while keeping the "XML backend implementation" pluggable
  * Type-safe querying for such XML dialects thus becomes feasible using yaidom
  
* Namespace-related utilities have been added, for moving up namespace declarations, stripping unused namespaces etc.
* The Node and NodeBuilder creation DSLs have been cleaned up a bit, resulting in breaking changes
* Small additions, such as method ``plusChildOption``, Path method ``append``, and method ``ancestryOrSelfENames``
* Upgraded Scala 2.11 version, as well as versions of dependencies


1.0
===

Version 1.0 is basically version 0.8.2, given the "1.0 status". Yaidom is now considered mature enough for a 1.0 release,
at least by the author and his colleagues, who use yaidom extensively in production code.

Several (small) libraries depending on this "yaidom core", and leveraging its extensibility, would make sense.
Think for example about Saxon yaidom wrappers (offering the ElemApi query API, at least), or XML Schema support (offering
the SubtypeAwareParentElemApi query API, at least).

Compared to version 0.8.2, there are no changes worth mentioning.


0.8.2
=====

Version 0.8.2 is a minor release, except for the addition of one new query trait. There are no breaking changes in this version.

The changes in this version are:

* Introduced trait ``SubtypeAwareParentElemApi`` and its implementation ``SubtypeAwareParentElemLike``:

  * These traits bring the ``ParentElemApi`` query API to object hierarchies
  * For example, when implementing XML schema components as immutable "elements", these traits come in handy as mix-ins
  * Many more XML (immutable) object hierarchies could use these traits, such as XBRL instance support and XLink support
  * The traits are not used by yaidom itself (except for internals in the utils package)
  * The ``SubtypeAwareParentElemLike`` trait is trivially implemented in terms of ``ParentElemLike``, and only offers convenience

* Added methods ``comments`` and ``processingInstructions`` to docaware and indexed Documents
* More test coverage
* Made creation of indexed and docaware elements a bit faster (by no longer running some "obviously true" assertions)
* Reworked the internal XmlSchemas API, in the utils package (it uses SubtypeAwareParentElemLike now)


0.8.1
=====

Version 0.8.1 is much like version 0.8.0, but it targets Scala 2.11.X as well as 2.10.X. There are no breaking changes in this version.

The changes in this version are:

* Built for Scala 2.11.X as well as Scala 2.10.X
* Introduced ``NavigableElemApi`` between ``ElemApi`` and ``PathAwareElemApi``:

  * This new query API trait offers Path-based navigation, but not Path-aware querying
  * ``NavigableElemApi`` contains (existing) methods like ``findChildElemByPathEntry`` and ``findElemOrSelfByPath``
  * Analogously, ``NavigableElemLike`` sits between ``ElemLike`` and ``PathAwareElemLike``
  * The net effect is that ``PathAwareElemApi`` and ``PathAwareElemLike`` offer the same API as before, without any breaking changes
  * Yet now "indexed" and "docaware" Elems mix in trait ``NavigableElemApi``, thus offering (fast) Path-based navigation, making these Elems more useful

* A Scope can also be used as JAXP NamespaceContext factory, thus facilitating the use of JAXP XPath support (even in Java code!)

In summary, version 0.8.1 is like 0.8.0, but it supports Scala 2.11.X, and makes "indexed" and "docaware" Elems more useful.


0.8.0
=====

Version 0.8.0 is much like version 0.7.1, but it drops support for Scala 2.9.X, and prunes deprecated code.

The changes in this version are:

* Scala 2.9.X is no longer supported, and Scala 2.10 features are (finally) used:

  * From now on, string interpolation is used in yaidom implementation code
  * Modularized language features also help increase quality, because the compiler performs more QA
  * Futures (and promises) are used in test code where concurrency is involved
  * Implicit (value!) classes can also be used
  * On the other hand, experiments with value classes for ENames and QNames did not work out, and using them for "wrapper elements" would require query API traits to be "universal"
  * It can also be risky to have non-local dependencies on restrictions imposed by value classes and universal traits, so value classes have rarely been used

* Deprecated code was removed
* First round of (potential) performance improvements:

  * Large scale duplication of equal EName and QName objects (in yaidom DOM-like trees) causes a large memory footprint
  * Using ``ENameProvider`` and ``QNameProvider`` instances, introduced in this version, memory usage can be decreased to a large extent
  * Yet it was not desirable to destabilize the API by introducing implicit parameters (containing implementation details) all over the place
  * So in the end (newly introduced) implicit parameters are rare and they are used only deep in the implementation
  * And ENameProvider and QNameProvider strategies can only be chosen at a global level
  * Some ENameProvider and QNameProvider implementations have indeed been provided

* Added easy conversions from QNames to ENames, given some Scope:

  * Now we can write queries based on stable ENames, but writing only QNames (that are easily converted to ENames, given a Scope)

* Added "thread-local" DocumentParser and DocumentPrinter classes, for use in an "enterprise" application
* Added ``HasQName`` trait, to enable abstraction over elements that expose QNames
* Upgraded some (test) dependencies to newer versions, e.g. ScalaTest was upgraded to version 2.0
* Removed (soon to be deprecated?) procedure syntax
* More tests


0.7.1
=====

Version 0.7.1 has one big API change: renaming ElemPath to Path (and ElemPathBuilder to PathBuilder), deprecating the old names.
This change makes the query API (in particular PathAwareElemApi) more clear: it is now more obvious what methods like
``findAllElemPaths`` mean, given the yaidom convention that in query methods "Elems" means "descendant elements", and "ElemsOrSelf"
means "descendant-or-self elements". The idea of renaming ElemPath to Path came from Nick Evans.

In spite of the API changes, this version should be a drop-in replacement for version 0.7.0, except that the changed parts
of the API now lead to deprecation warnings. It is advisable to adapt code using yaidom in such a way that those deprecation warnings
disappear. It is likely that version 0.8.0 (which may or may not be the next version) will no longer contain the deprecated classes
and methods.

The changes in this version are:

* Renaming ``ElemPath`` (and ``ElemPathBuilder``) to ``Path`` (and ``PathBuilder``), deprecating the old names

  * Also renamed ``elemPath`` in "indexed" and "docaware" Elems to ``path``, deprecating the old name
  * the idea is to talk consistently about "paths", not about "element paths" (or "elem paths")

* Added "docaware" elements (mixing in trait ElemApi), which are like "indexed" elements, but also keeping the document URI
* Renamed ``findWithElemPath`` to ``findElemOrSelfByPath`` (deprecating the old name). Also renamed ``findWithElemPathEntry`` and ``getWithElemPath``.
* Added convenience methods for creating "element predicates", for example to make it slightly easier to query using local names
* Many more tests


0.7.0
=====

Version 0.7.0, finally. Starting with this version, API stability and proper deprecation will be considered important.

* XLink support has been removed from core yaidom, and will live in its own project


0.6.14
======

This version improves the Scaladoc documentation. This will probably become version 0.7.0.

* Reworked the Scaladoc documentation (better showing how to use the API), and removed obsolete (non-Scaladoc) documentation.
* Breaking API change: ``indexed.Elem`` no longer mixes in ``HasParent``, and is now more efficient when querying
* Bug fixes in methods ``updatedAtPathEntries`` and ``updatedWithNodeSeqAtPathEntries``
* Tested against IBM JDK (ibm-java-x86_64-60)


0.6.13
======

This version contains small breaking and non-breaking changes, and partly reworked documentation. Hopefully version 0.7.0
will be the same, except for the documentation.

* Reworked main package documentation, mainly to clarify usage of the API with examples
* Breaking API change: renamed ``Scope`` and ``Declararations`` fields ``map`` to ``prefixNamespaceMap``
* Breaking API change: removed ``Scope`` method ``prefixOption``, and added method ``prefixesForNamespace``
* Breaking API change: altered signature of ``ElemPath`` object method ``from``, for consistency with ``ElemPathBuilder``
* Added ``ElemPath`` method ``elementNameOption``
* Added generic trait ``DocumentApi``


0.6.12
======

This version improves on the last "functional update/transformation" support, by restoring bulk updates (this time with a
less inefficient implementation) and removing the transformation methods that need "context".

* Added ``UpdatableElemApi`` bulk update methods ``updatedAtPathEntries`` and ``updatedAtPaths``

  * Added ``updatedWithNodeSeqAtPathEntries`` and ``updatedWithNodeSeqAtPaths`` as well
  * Also added update methods for Documents

* Breaking API change: ``TransformableElemApi`` (overloaded) methods taking "context" have been removed
* Breaking API change: removed (unnecessary) ``Scope`` methods ``notUndeclaring`` and ``notUndeclaringPrefixes``
* Breaking API change: renamed ``Scope`` method ``minimized`` to ``minimize``
* Breaking API change: ``YaidomToScalaXmlConversions`` methods ``convertNode`` and (overloaded) ``convertElem`` take extra NamespaceBinding parameter
* Added collections methods ``filter``, ``filterKeys`` and ``keySet`` to ``Scope`` (for convenience)
* Added ``Elem`` methods for getting QName-valued attribute values or text values as QNames or ENames (for convenience)
* Clarified broken abstractions such as ``ElemApi`` when using Scala XML as backend
* Bug fix: ``YaidomToScalaXmlConversions`` method ``convertElem`` tries to prevent duplicate namespace declarations
* Added ``apply`` factory methods to Scala XML wrapper nodes and DOM wrapper nodes (for convenience)


0.6.11
======

This version offers completely reworked "functional update/transformation" support. The ElemPath-based bulk updates have
been removed, because they were far too inefficient. The "transformation" support, however, has been enhanced a lot.

* Big breaking API change: ``UpdatableElemApi`` has been made smaller

  * All functional updates taking a PartialFunction have been removed (``updated``, ``topmostUpdated`` and ``updatedWithNodeSeq``)
  * They were bulk updates (implicitly) based on element paths, which is very inefficient
  * Added ``updated`` method taking an ``ElemPath.Entry`` (and a function in its 2nd parameter list)
  
* Big breaking API change: ``TransformableElemApi`` has been enhanced a lot

  * Like ``UpdatableElemApi``, trait ``TransformableElemApi`` now takes 2 type parameters, viz. the node type and the element type
  * Method ``transform`` has been renamed to ``transformElemsOrSelf``
  * Added methods such as ``transformElems``, ``transformChildElems``
  * Also added methods such as ``transformElemsOrSelfToNodeSeq``, ``transformElemsToNodeSeq`` and ``transformChildElemsToNodeSeq``
  * Trait ``TransformableElemApi`` elegantly reminds of ``ParentElemLike``, except that it is for querying instead of updates
  * Trait ``TransformableElemApi`` is even mixed in by ``ElemBuilder``
  
In summary, the functional update support of the preceding release was not good enough to be frozen (in upcoming version 0.7.0).
Hence this version 0.6.11.


0.6.10
======

This version improves "functional update" support as well as "Scala XML literal" support (before version 0.7.0 arrives).

* Improved "functional update" support

  * Added ``updatedWithNodeSeq`` and ``topmostUpdatedWithNodeSeq`` methods to ``UpdatableElemApi`` and ``UpdatableElemLike``
  * These methods are defined (directly or indirectly) in terms of ``updated``
  * Yet these methods make functional updates more practical, by offering updates that replace elements by collections of nodes
  * They are even powerful enough to express what are separate operations in XQuery Update, such as insertions, deletions etc.

* Added ``TransformableElemApi`` and ``TransformableElemLike``

  * "Transformations" apply a transformation function to all descendant-or-self methods
  * In contrast, "(functional) updates" apply update functions only to elements at given (implicit or explicit) paths
  * "Transformations" and "(functional) updates" can express pretty much the same, but have different performance characteristics
  * Roughly, if only a few elements in an element tree need to be updated, prefer "updates", and otherwise prefer "transformations"

* Added ``YaidomToScalaXmlConversions``,  as a result of which there are now conversions between Scala XML and yaidom in both directions
* Added ``ScalaXmlElem``, which is an ``ElemLike`` query API wrapper around Scala XML elements
* Added ``AbstractDocumentPrinter``, making ``DocumentPrinter`` purely abstract (analogous to document parsers)
* Richer ``prettify`` method, optionally changing newline characters and optionally using tabs instead of spaces
* Added ``copy`` method to classes Elem and ElemBuilder
* Some documentation changes and bug fixes, and more tests

This version offers many "tools" for creation of and updates to XML trees, such as support for Scala XML literals (converting them
to yaidom and vice versa, or querying them using the yaidom query API), "transformations", and (functional) updates (replacing
elements by elements, or elements by node collections).


0.6.9
=====

This is still another version leading up to version 0.7.0. It does contain a few breaking changes.

* Big breaking API change: XML literals are gone (i.e. hidden), and replaced by conversions from Scala XML to yaidom

  * The conversions from Scala XML to yaidom make it possible to create Scala XML literals, and immediately convert them to yaidom Elems
  * Yaidom XML literals, on the other hand, still need a lot of work before they become useful
  * One problem with the yaidom XML literals concerns the runtime costs of XML parsing at each use (instead of having a macro "compile" them)
  * Another problem with yaidom XML literals concerns the restrictions w.r.t. the locations of parameters
  * The conversions between Scala XML Elems and yaidom Elems are one-way only, from Scala XML to yaidom
  * These conversions make it possible to use Scala XML literals as if they are "yaidom XML literals"
  * These conversions even work around nasty XML Scala namespace-related bugs, such as SI-6939
  
* Breaking API change: removed overloaded ``\``, ``\\``, ``\\!`` and ``\@`` methods taking just a local name (as string)

  * An experiment was conducted to make EName and QName (Scala 2.10) value classes, to avoid EName/QName object explosion
  * In this experiment, the overloads above had to go (besides, they violated the "precision" of yaidom anyway)
  * This experimental change has been reverted (for now), but I want to keep the option open to use value classes for EName/QName in the future
  * So the overloaded methods above have been removed (probably permanently)
  * In the spirit of "precise" querying, also renamed ``findAttribute`` (taking a local name) to ``findAttributeByLocalName``

* Breaking API change: renamed ``baseUriOption`` to ``uriOption``, and ``withBaseUriOption`` to ``withUriOption``
* Breaking API change: removed method ``QName.prefixOptionFromJavaQName``
* Added some overloaded ``DocumentParser.parse`` methods
* ``LabeledXLink`` is no longer a trait with a val, but is now an abstract class

As for the maturity of parts of yaidom:

* Its querying support is the most mature part. The APIs ("abstractions") are simple and clear, and seem to work well.
* Its functional update support is still rather basic. It should first mature, without postponing version 0.7.0 too much.
* Its XML literal support simply is not useful yet, so an alternative has been provided in version 0.6.9 (instead of further postponing version 0.7.0).


0.6.8
=====

This version is probably the last release before version 0.7.0. It does contain a few breaking changes.

* Breaking API change: renamed method ``allChildElems`` to ``findAllChildElems``
* Breaking API changes (related):

  * Renamed ``allChildElemsWithPathEntries`` to ``findAllChildElemsWithPathEntries``
  * Renamed ``allChildElemPathEntries`` to ``findAllChildElemPathEntries``
  * Renamed ``allChildElemPaths`` to ``findAllChildElemPaths``
  
* Breaking API changes: removed methods ``collectFromChildElems``, ``collectFromElems`` and ``collectFromElemsOrSelf``
* Breaking API change: removed method ``getIndex``
* Added ``indexed.Elem`` methods ``scope`` and ``namespaces``
* Added method ``Elem.minusAttribute``
* Performance improvements to ``Elem.toString``
* Worked on XML literal support, but the result is still highly experimental
* Scala and ScalaTest upgrade (versions 2.10.1 and 1.9.1, respectively)

Hopefully only documentation updates and small non-breaking fixes will be the difference between version 0.6.8 and
upcoming version 0.7.0. In other words, hopefully the API is stable from now on.


0.6.7
=====

This version is again one step closer to version 0.7.0. It contains small improvements, and contains only "smallish" breaking changes.

* Added ``HasParent`` API, mixed in by ``indexed.Elem`` and ``DomElem``, without changing those classes from the outside
* Added purely abstract ``ParentElemApi``, ``ElemApi`` etc., which are implemented by ``ParentElemLike``, ``ElemLike`` etc.
* Added ``ElemBuilder`` methods ``canBuild``, ``nonDeclaredPrefixes`` and ``allDeclarationsAreAtTopLevel``
* Added ``Scope`` methods ``inverse`` and ``prefixOption``
* Breaking API change: removed ``ElemBuilder.withChildNodes``
* Breaking API change: removed confusing methods ``Declarations.subDeclarationsOf`` and ``Declarations.superDeclarationsOf``
* Breaking API change: XLink labels need not be unique within extended links. This affects the extended link methods like ``labeledXLinks``.
* Moved method ``plusChild`` (taking one parameter) up to ``UpdatableElemLike``
* A few bug fixes
* More tests, and more documentation


0.6.6
=====

This version is one step closer to version 0.7.0. It introduces so-called "indexed" elements, (almost) without changing the
query API and the "conceptual surface area".

* Small breaking API change: removed obsolete method ``UpdatableElemLike.findChildPathEntry``
* Added "indexed" elements, which mix in trait ElemLike:

  * "Indexed" elements are a "top-down notion" of elements, knowing about their ancestry

* Added some "functional update" methods, such as ``plusChild``, ``minusChild``, ``topmostUpdated``, and changed the meaning of ``updated``
* Reworked some internals for better performance (at the cost of more memory usage):

  * Made ``PathAwareElemLike`` methods ``findWithElemPathEntry`` and ``allChildElemsWithPathEntries`` abstract
  * Element path based querying (and method ``findWithElemPathEntry`` in particular) is much faster now

* More tests


0.6.5
=====

This version prepares the future upgrade to version 0.7.0, which will take stability of the API far more seriously (with proper
deprecation of obsolete code). Much cleanup of the API has therefore be done in this release 0.6.5. Many (mostly small) breaking API changes
have been performed in this release. The foundations of the API are clear, and the packages, types and their methods now all
have a clear purpose. Moreover, consistency of the API has improved somewhat. As a result of this API cleanup, it is to be
expected that future release 0.7.0 will be pretty much like this release, except for cleaned up documentation.

* Breaking API changes: The ``updated`` methods now return single elements instead of node collections, so they can now be called on the "root element path"
* Breaking API change: Renamed ``Scope.resolveQName`` to ``Scope.resolveQNameOption``
* Breaking API change: Removed ``IndexedDocument``
* Breaking API change: Removed ``Node.uid`` (and method ``getElemPaths``)
* Breaking API change: Made ``XmlStringUtils`` internal to yaidom
* Breaking API change: Moved method ``prefixOptionFromJavaQName`` from ``EName`` to ``QName``
* Breaking API change: Removed ``ElemPath.fromXPaths``
* Breaking API change: Renamed ``DomNode.wrapOption`` to ``DomNode.wrapNodeOption``
* Added method ``Elem.plusAttribute`` (now that attributes can be ordered)
* Experimental, and only for Scala 2.10: XML literals (a first naive version)


0.6.4
=====

* Breaking API changes: Throughout the yaidom library (except for "resolved elements"), attributes in elements are now ordered (for "better roundtripping")!
* Added ``DocumentPrinter.print`` methods that print to an OutputStream, and therefore typically save memory
* Fixed method ``DocumentPrinterUsingStax.omittingXmlDeclaration``
* Improved ``DocumentParser`` classes with respect to character encoding detection
* ``StaxEventsToYaidomConversions`` can now produce an Iterator of XMLEvents, thus enabling less memory-hungry StAX-based parsing
* Indeed, ``DocumentParserUsingStax`` uses these Iterator-producing conversions, thus leading to far less memory usage
* Added ``ElemPath`` convenience methods ``findAncestorOrSelfPath`` and ``findAncestorPath``
* Breaking API change: removed superfluous ``childIndexOf`` method (twice)
* Added yaidom tutorial
* Removed half-baked support for Java 5 (requiring at least Java 6 from now on)


0.6.3
=====

* Enabled cross-building and publishing (to Sonatype repository) for different Scala versions, using sbt
* Added DOM Load/Save based document parser and printer
* Document printers can now print to byte arrays, given some character encoding
* Extended XLinks know their resources and locators by label
* Bug fix in `YaidomToDomConversions`: top-level comments occur before the document element, not after
* Tests now also run on Java 5, including an IBM JRE 5
* Small fixes, code cleanup and documentation additions


0.6.2
=====

In this version, yaidom clearly became 2 things: an element querying API (trait ``ParentElemLike`` and sub-traits), and concrete
(immutable and mutable) element classes into which those traits are mixed in. The element querying API can also be mixed in into
element classes that are not part of yaidom, such as ``ParentElemLike`` wrappers around JDOM or XOM.

* Breaking API change: made class ``Declarations`` a top-level class, because "namespace declarations" are an independent concept
* Breaking API changes to classes ``Scope`` and ``Declarations``:

  * Simplified the implementations, with both classes now backed by maps from prefixes to namespace URIs
  * Removed several methods (that are not often used outside the yaidom library itself)
  * Added several methods, thus making both classes more internally consistent than before
  * Added properties and their proofs to the documentation of both classes

* Added trait ``ParentElemLike``, as an independent abstraction, offering a rich "base" element querying API:

  * Trait ``ElemLike`` extends this new trait
  * Trait ``ParentElemLike`` has only abstract method ``allChildElems``, and no further "knowledge" than that
  * This trait is also mixed in by ``ElemBuilder``
  * The documentation of trait ``ParentElemLike`` contains several properties with their proofs
  * The subtrait ``ElemLike`` got some new attribute querying methods

* Added class ``ElemPathBuilder``
* Fixed class ``ElemPath``, using new method ``Scope.isInvertible``
* Added trait ``UpdatableElemLike``:

  * Mixed in by different element classes
  * Breaking API change: ``Elem.updated`` methods now returning node collections instead of single elements
  * Also clarified and re-implemented ``Elem.updated`` for speed (in different ``Elem`` classes)
  * Added methods like ``withUpdatedChildren`` and ``withPatchedChildren``

* Added trait ``PathAwareElemLike``, which mirrors trait ``ParentElemLike``, but returns element paths instead of elements
* Added ``dom`` package:

  * ``ElemLike`` wrappers around W3C DOM nodes

* Adapted ``convert`` package:

  * Breaking API changes: renamed several singleton objects
  * Many conversion methods are now public
  * "Conversion" API became more consistent
  * Removed 2 ``convertToElem`` methods that were easy to use incorrectly

* Breaking API change: ``Document`` is no longer a ``Node``, and ``DocBuilder`` no longer a ``NodeBuilder``
* ``Node`` has a similar DSL for creating node trees as ``NodeBuilder``, using methods like ``elem``, ``text`` etc.
* Added some convenience methods to ``ElemBuilder``, like ``withChildren`` and ``plusChild``
* Added convenience method ``NodeBuilder.textElem``
* Added ``Elem`` methods ``prettify`` and ``notUndeclaringPrefixes``
* Documented namespace-awareness for Document parsers
* Added motivation document
* Added test case for some "mini-yaidom", which can be used in an article explaining yaidom
* Added many other tests
* Added sbt build file


0.6.1
=====

* Small breaking API change, and (bigger) implementation change: renamed and re-implemented the ``toAstString`` methods:

  * They are now called ``toTreeRepr`` (for "tree representation"), for ``Node`` and ``NodeBuilder``
  * The implementation is easier to understand, using a new ``PrettyPrinting`` singleton object as ``toTreeRepr`` implementation detail
  * The ``toTreeRepr`` output has also slightly changed, for example child ``List`` became child ``Vector``
  
* Added singleton object ``TreeReprParsers``, generating parsers for the ``toTreeRepr`` String output

  * It uses the Scala parser combinator API, extending ``JavaTokenParsers``
  * These tree representations represent parsed XML trees, so they are much closer to ``Node`` and ``NodeBuilder``
  * The tree representations are valid Scala code themselves (using ``NodeBuilder`` methods)
  * An extra dependency was added, namely Apache Commons Lang

* ``Node`` and ``NodeBuilder`` are now serializable:

  * So they could in principle be stored efficiently as a BLOB in the database, and quickly materialized again
  
* Minor breaking API changes, tightening the collection type for child nodes:

  * The ``NodeBuilder.elem`` factory method now takes an ``immutable.IndexedSeq[NodeBuilder]``
  
* The ``EName`` and ``QName`` one-arg ``apply`` methods now behave like the ``parse`` methods, so they no longer require only a local part


0.6.0
=====

* Breaking API change: renamed ``ExpandedName`` to ``EName`` (after which some implicit conversions started to make less sense, and they have indeed been removed)
* Breaking API change: removed all (!) implicit conversion methods

  * ``EName`` and ``QName`` factory methods work just fine
  * The ``Scope`` and ``Scope.Declarations`` factory methods ``from`` have been added, which are easy to use

* Breaking API change: renamed ``ElemLike`` method ``filterChildElemsNamed`` to ``filterChildElems``, etc.
* Added overloaded ``\``, ``\\`` and ``\\!`` methods taking an expanded name, or even a local name, to ``ElemLike``
* Moved method ``localName`` to ``ElemLike``
* Added trait ``HasText`` (in practice element types mix in both ``ElemLike`` and ``HasText``)
* More tests, and some test cleanup after the above-mentioned changes


0.5.2
=====

* Breaking API change: renamed the ``jinterop`` package to ``convert``:

  * In principle we could later add conversions from/to Scala standard library XML to this package, without the need to rename this package again
  
* The ``ElemLike`` operators now stand for ``filterElemsOrSelf`` and ``findTopmostElemsOrSelf`` (instead of ``filterElems`` and ``findTopmostElems``, resp.):

  * This is more consistent with XPath, so less surprising

* Some QA by the Scala 2.10.0-M3 compiler, fixing some warnings:

  * This includes the removal of the (remaining) postfix operators
  * API change: the implicit conversions are now in ``Predef`` objects that must be explicitly imported
  * Also removed keyword ``val`` from ``for`` comprehensions

* More tests


0.5.1
=====

* Added so-called "resolved" nodes, which can be compared for (some notion of value) equality
* Changes in ``ElemLike``:

  * Major documentation changes, clarifying the fundamental properties of the ``ElemLike`` API
  * Breaking API changes: removed methods ``getIndexToParent``, ``findParentInTree`` and ``getIndexByElemPath``
  * Fixed inconsistency: method ``findChildElem`` returns the first found child element obeying the given predicate, no longer assuming that there is at most one such element
  
* A yaidom ``Node`` (again) has a UID, thus enabling the extension of nodes with additional data, using the UID as key
* Added ``IndexedDocument``, whose ``findParent`` method is efficient (leveraging the UIDs mentioned above)
* Small additions to ``ElemPath``: new methods ``ancestorOrSelfPaths`` and ``ancestorPaths``
* Documentation recommends use of TagSoup for parsing HTML (also added test case method using TagSoup)
* Added support for printing an ``Elem`` without XML declaration
* Added document about some XML gotchas


0.5.0
=====

* Breaking changes in ``ElemLike`` API, renaming almost all methods!

  * The core element collection retrieval methods are (abstract) ``allChildElems`` (not renamed), and ``findAllElems`` and ``findAllElemsOrSelf`` (after renaming)
  * The other (renamed) element collection retrieval methods taking a predicate are ``filterChildElems``, ``filterElems``, ``filterElemsOrself``, ``findTopmostElems`` and ``findTopmostElemsOrSelf``
  * The element (collection) retrieval methods taking an ExpandedName are now called ``filterChildElemsNamed`` etc.
  * There are shorthand operator notations for methods ``filterChildElems``, ``filterElems`` and ``findTopmostElems``
  * Methods returning at most one element are now called ``findChildElem``, ``getChildElem`` etc.
  * Why all this method renaming?
  
    * Except for "property" ``allChildElems``, the retrieval methods now start with verbs, as should be the case
    * Those verbs are closer to Scala's Collections API vocabulary, and thus convey more meaning
    * In method names, nouns refer to the "core element set" (children, descendants, decendants-or-self), and verbs (and optional adjective, preposition etc.)
      refer to the operation on that data ("filter", "find topmost", "collect from" etc.)
    * Since method names start with verbs, name clashes with variables holding retrieval method results are far less likely
    * The core element collection retrieval methods are easy to distinguish from the other element (collection) retrieval methods
    * Operator notation ```\```, ```\\``` and ```\\!```, when used appropriately, can remove a lot of clutter
    
* Made ``ElemPath`` easier to construct
* Small improvements, such as slightly less verbose ``ElemBuilder`` construction


0.4.4
=====

* Improved ``ElemLike``

  * Better more consistent documentation
  * Added some methods for consistency
  * Far better performance
  * Breaking API change: renamed ``childElemOption`` to ``singleChildElemOption`` and ``childElem`` to ``singleChildElem``
  
* Added ``DocumentPrinterUsingSax``
* Added ``Elem.localName`` convenience method
* Introduced JCIP (Java Concurrency in Practice) annotation @NotThreadSafe (in SAX handlers)
* Small documentation changes and refactorings (including banning of postfix operators)
* More test code


0.4.3
=====

* API changes in ``xlink`` package

  * Added ``Link.apply`` and ``XLink.mustBeXLink`` methods

* API change: renamed ``DocumentBuilder`` to ``DocBuilder`` to prevent conflict with DOM ``DocumentBuilder`` (which may well be in scope)
* API changes (and documentation updates) in ``parse`` package

  * The ``DocumentParser`` implementations have only 1 constructor, and several ``newInstance`` factory methods, one of which calls the constructor
  * ``DocumentParserUsingSax`` instances are now re-usable, because now ``ElemProducingSaxHandler`` producing functions (instead of instances) are passed
  
* API changes (and documentation updates) in ``print`` package

  * The ``DocumentPrinter`` implementations have only 1 constructor, and several ``newInstance`` factory methods, one of which calls the constructor
  
* Small API changes:

  * Added 1-arg ``Document`` factory method, taking a root ``Elem``
  * Added ``Document.withBaseUriOption`` method
  * Added some methods to ``ElemPath`` (for consistency)
  
* More documentation, and added missing package objects (with documentation)


0.4.2
=====

* API changes in trait ``ElemLike``

  * Renamed method ``firstElems`` to ``topmostElems`` and ``firstElemsWhere`` to ``topmostElemsWhere``

* Bug fix: erroneously rejected XML element names starting with string "xml"


0.4.1
=====

* XLink support largely redone (with breaking API changes)

  * Removed top level ``Elem`` in the ``xlink`` package (wrapping a normal ``Elem``)

* Renamed implementation trait ``ElemAsElemContainer`` back to ``ElemLike``
* More tests, including new test class ``XbrlInstanceTest``
