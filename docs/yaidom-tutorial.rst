===============
Yaidom tutorial
===============

Introduction
============

Yaidom is yet-another-immutable DOM-like XML API, written in the `Scala`_ programming language. It is an XML API that:

* leverages the highly expressive Scala Collections API, inside and outside
* offers immutable thread-safe DOM-like element trees
* offers first-class support for namespaces
* interoperates very well with JAXP

This tutorial introduces the yaidom API. Some basic Scala knowledge is assumed, especially about the Scala
`Collections API`_.

The tutorial is organized as follows:

* Yaidom foundations

  * Mini-yaidom
  * Yaidom and namespaces

* Yaidom universal querying API

  * ParentElemLike querying API
  * ElemLike querying API
  * PathAwareElemLike querying API

* Working with default yaidom Elems

  * Default Elems
  * ElemBuilders
  * Parsing XML
  * Serializing XML
  * Functional updates

* Other element implementations

  * "Resolved" elements
  * Yaidom DOM wrappers

* Conclusion

.. _`Scala`: http://www.scala-lang.org
.. _`Collections API`: http://www.scala-lang.org/docu/files/collections-api/collections.html

Yaidom foundations
==================

Leveraging Scala Collections
----------------------------

The most important foundation of yaidom is Scala's Collections API. This can be illustrated by reconstructing a mini-version
of yaidom.

Assume a simple immutable DOM-like node class hierarchy defined as follows::

  import scala.collection.immutable

  /**
   * Naive node trait, with only subclasses for elements and text nodes.
   */
  sealed trait Node extends Immutable

  /**
   * Naive element class, which for example is not namespace-aware.
   */
  final class Elem(
    val name: String,
    val attributes: Map[String, String],
    val children: immutable.IndexedSeq[Node]) extends Node {

    def this(name: String, children: immutable.IndexedSeq[Node]) =
      this(name, Map(), children)

    def allChildElems: immutable.IndexedSeq[Elem] =
      children collect { case e: Elem => e }

    /**
     * Finds all descendant elements and self.
     */
    def findAllElemsOrSelf: immutable.IndexedSeq[Elem] = {
      val self = Elem.this
      val elms = allChildElems flatMap { _.findAllElemsOrSelf }
      self +: elms
    }

    /**
     * Finds all topmost descendant elements and self, obeying the given predicate.
     * If a matching element has been found, its descendants are not searched for matches (hence "topmost").
     */
    def findTopmostElemsOrSelf(p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
      if (p(Elem.this)) {
        Vector(Elem.this)
      } else {
        allChildElems flatMap { _.findTopmostElemsOrSelf(p) }
      }
    }

    def text: String = {
      val textStrings = children collect { case t: Text => t.text }
      textStrings.mkString
    }
  }

  final case class Text(val text: String) extends Node

Having Scala's Collections API, along with the DOM-like trees defined above, we can already write many element queries.
TODO: Code examples of "mini-yaidom" queries.

::

  In summary, using the Scala Collections API and a minimal "mini-yaidom" API, the contours of a powerful XML querying API
  already become visible.

Yaidom and namespaces
---------------------

TODO Mini-yaidom is not enough, of course. Namespaces are first-class citizens in yaidom.
Qualified names, expanded names. Namespace declarations and in-scope namespaces ("scopes").
Reasoning about namespaces.

Yaidom universal querying API
=============================

ParentElemLike querying API
---------------------------

Element-centric yaidom querying API versus implementations. See earlier take-away point about Scala Collections API.

Show queries using ParentElemLike (using namespaces), and show how these queries work for "normal" yaidom Elems,
as well as for DOM wrapper elements and "resolved" elements. The ParentElemLike API is the most important API in yaidom.

Take-away point: one size does not fit all for element trees (different characteristics).

Take-away point: these different node class hierarchies can still share the same querying API(s).

Take-away point: there is no magic at all in the yaidom querying API, even if the resulting queries are somewhat more
verbose than XPath expressions (but XPath is a different thing altogether).

ElemLike querying API
---------------------

Show convenience methods offered by the ElemLike API as well (using EName arguments instead of element predicates).
Show shorthand notations as well.

PathAwareElemLike querying API
------------------------------

Sometimes we want to query for "paths" to elements rather than for elements themselves. Knowing the path (relative to a
root) we know the element, but the reverse does not hold, of course.

PathAwareElemLike examples.

Working with default yaidom Elems
=================================

Default Elems
-------------

Explain the default yaidom Elems. They are immutable.

ElemBuilders
------------

Explain ElemBuilders, and how to construct Elems from scratch. Explain namespace handling.

Parsing XML
-----------

Explain parsing in yaidom.

Take-away point: XML parsing is quite complex in its details. Yaidom leaves XML parser configuration completely open instead
of hiding it.

Serializing XML
---------------

Explain serializing in yaidom.

Take-away point: XML serialization is quite complex in its details. Yaidom leaves XML serializer configuration completely open instead
of hiding it.

Functional updates
------------------

Show the (also general) UpdatableElemLike API. Explain correct namespace handling.

Other element implementations
=============================

"Resolved" elements
-------------------

Explain "resolved" elements and their purpose.

Yaidom DOM wrappers
-------------------

Explain yaidom DOM wrappers, and how to use them.

Conclusion
==========

What yaidom does, what it does not, how we can deal with some limitations.

