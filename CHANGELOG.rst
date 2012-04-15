=========
CHANGELOG
=========


0.5.1
=====

* Added so-called "resolved" nodes, which can be compared for (some notion of value) equality
* Changes in ``ElemLike``:

  * Major documentation changes, clarifying the fundamental properties of the ``ElemLike`` API
  * Breaking API changes: removed methods ``getIndexToParent``, ``findParentInTree`` and ``getIndexByElemPath``
  * Fixed inconsistency: method ``findChildElem`` returns the first found child element obeying the given predicate, no longer assuming that there is at most one such element
  
* Yaidom ``Node``s (again) have UIDs, enabling the extension of nodes with additional data, using the UID as key
* Added ``IndexedDocument``, whose ``findParent`` method is efficient (leveraging the UIDs mentioned above)
* Small additions to ``ElemPath``: methods ``ancestorOrSelfPaths`` and ``ancestorPaths``
* Documentation recommends use of TagSoup for parsing HTML (also added test case method using TagSoup)
* Added support for printing ``Elem``s without XML declarations
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
