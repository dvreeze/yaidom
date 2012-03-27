=========
CHANGELOG
=========


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
