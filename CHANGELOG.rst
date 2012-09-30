=========
CHANGELOG
=========


0.6.2
=====

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
