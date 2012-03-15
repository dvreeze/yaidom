=========
CHANGELOG
=========


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
