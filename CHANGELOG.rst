=========
CHANGELOG
=========


0.6.9
=====

This is still another version leading up to version 0.7.0. It does contain a few breaking changes.

* Big breaking API change: XML literals are gone (i.e. hidden), and replaced by conversions from Scala XML to yaidom

  * The conversions from Scala XML to yaidom make it possible to create Scala XML literals, and immediately convert them to yaidom
  * Yaidom XML literals, on the other hand, need a lot of work before they become useful
  * One problem with the yaidom XML literals is the runtime cost, of XML parsing at each use (instead of having a macro "compile" them)
  * Another problem with yaidom XML literals is the restrictions w.r.t. where parameters can be used
  * The conversions between Scala XML Elems and yaidom Elems are one-way, from Scala XML to yaidom
  * These conversions make it possible to use Scala XML literals as if they are "yaidom XML literals"
  * These conversions even work around nasty XML Scala namespace-related bugs, such as SI-6939
  
* Breaking API change: removed overloaded ``\``, ``\\``, ``\\!`` and ``\@`` methods taking just a local name (as string)

  * An experiment was conducted to make EName and QName (Scala 2.10) value classes, to avoid EName/QName object explosion
  * In this experiment, the overloads above had to go (and they violated the "precision" of yaidom anyway)
  * This experimental change has been reverted (for now), but I want to keep the option open to use value classes for EName/QName in the future
  * So the overloaded methods above have been removed, and are not expected to come back
  * In the spirit of "precise" querying, also renamed ``findAttribute`` (taking a local name) to ``findAttributeByLocalName``

* Breaking API change: renamed ``baseUriOption`` to ``uriOption``, and ``withBaseUriOption`` to ``withUriOption``
* Breaking API change: removed method ``QName.prefixOptionFromJavaQName``
* Added some overloaded ``DocumentParser.parse`` methods
* ``LabeledXLink`` is no longer a trait with a val, but is now an abstract class

As for the maturity of parts of yaidom:

* Its querying support is most mature. The APIs ("abstractions") are simple and clear, and seem to work well.
* Its functional update support is still rather basic. It should first mature outside of yaidom, instead of further postponing version 0.7.0.
* Its XML literal support simply was not useful yet, so an alternative has been provided in version 0.6.9 (instead of further postponing version 0.7.0).


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
