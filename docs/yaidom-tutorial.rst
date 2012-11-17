===============
Yaidom tutorial
===============

Introduction
============

Yaidom is yet-another-immutable DOM-like XML API, written in the `Scala`_ programming language. It is an XML API that:

* leverages the highly expressive Scala Collections API, on the inside and from the outside
* offers immutable thread-safe DOM-like element trees
* offers first-class support for namespaces
* interoperates very well with JAXP

This tutorial introduces the yaidom API. Some basic Scala knowledge is assumed, especially about the Scala
`Collections API`_.

The tutorial is organized as follows:

* Yaidom foundations

  * Leveraging Scala Collections
  * Yaidom and namespaces

* Yaidom uniform querying API

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
    val children: immutable.IndexedSeq[Node]) extends Node { self =>

    def this(name: String, children: immutable.IndexedSeq[Node]) =
      this(name, Map(), children)

    def allChildElems: immutable.IndexedSeq[Elem] =
      children collect { case e: Elem => e }

    /**
     * Finds all descendant elements and self.
     */
    def findAllElemsOrSelf: immutable.IndexedSeq[Elem] = {
      val elms = allChildElems flatMap { _.findAllElemsOrSelf }
      self +: elms
    }

    /**
     * Finds all topmost descendant elements and self obeying the given predicate.
     * If a matching element has been found, its descendants are not searched for matches
     * (hence "topmost").
     */
    def findTopmostElemsOrSelf(p: Elem => Boolean): immutable.IndexedSeq[Elem] = {
      if (p(Elem.this)) Vector(Elem.this)
      else allChildElems flatMap { _.findTopmostElemsOrSelf(p) }
    }

    def text: String = {
      val textStrings = children collect { case t: Text => t.text }
      textStrings.mkString
    }
  }

  final case class Text(val text: String) extends Node

Having Scala's Collections API, along with the DOM-like trees defined above, we can already write many element queries.

Before doing so, let's first define some XML data as a "mini-yaidom" DOM-like tree. This example XML comes from the
online course "Introduction to Databases", by professor Widom at Stanford University, and is used here with permission.
The sample XML data represents a bookstore, and is created in "mini-yaidom" as follows::

  def textElem(elmName: String, txt: String): Elem =
    new Elem(
      name = elmName,
      attributes = Map(),
      children = Vector(Text(txt)))

  val book1: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-0-13-713526-2", "Price" -> "85", "Edition" -> "3rd"),
      children = Vector(
        textElem("Title", "A First Course in Database Systems"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jeffrey"),
                textElem("Last_Name", "Ullman"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jennifer"),
                textElem("Last_Name", "Widom")))))))
  }

  val book2: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-0-13-815504-6", "Price" -> "100"),
      children = Vector(
        textElem("Title", "Database Systems: The Complete Book"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Hector"),
                textElem("Last_Name", "Garcia-Molina"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jeffrey"),
                textElem("Last_Name", "Ullman"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jennifer"),
                textElem("Last_Name", "Widom"))))),
        textElem("Remark", "Buy this book bundled with \"A First Course\" - a great deal!")))
  }

  val book3: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-0-11-222222-3", "Price" -> "50"),
      children = Vector(
        textElem("Title", "Hector and Jeff's Database Hints"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jeffrey"),
                textElem("Last_Name", "Ullman"))),
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Hector"),
                textElem("Last_Name", "Garcia-Molina"))))),
        textElem("Remark", "An indispensable companion to your textbook")))
  }

  val book4: Elem = {
    new Elem(
      name = "Book",
      attributes = Map("ISBN" -> "ISBN-9-88-777777-6", "Price" -> "25"),
      children = Vector(
        textElem("Title", "Jennifer's Economical Database Hints"),
        new Elem(
          name = "Authors",
          children = Vector(
            new Elem(
              name = "Author",
              children = Vector(
                textElem("First_Name", "Jennifer"),
                textElem("Last_Name", "Widom")))))))
  }

  val magazine1: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "January", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "National Geographic")))
  }

  val magazine2: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "February", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "National Geographic")))
  }

  val magazine3: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "February", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "Newsweek")))
  }

  val magazine4: Elem = {
    new Elem(
      name = "Magazine",
      attributes = Map("Month" -> "March", "Year" -> "2009"),
      children = Vector(
        textElem("Title", "Hector and Jeff's Database Hints")))
  }

  val bookstore: Elem = {
    new Elem(
      name = "Bookstore",
      children = Vector(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4))
  }

Having this bookstore DOM-like tree, we can write queries against it. Note that class ``Elem`` has very few query methods
on its own. In the queries, most work is done by Scala's Collections API. Some queries are::

  // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

  val bookOrMagazineTitles =
    for {
      bookOrMagazine <- bookstore.allChildElems filter { e => Set("Book", "Magazine").contains(e.name) }
    } yield {
      val result = bookOrMagazine.allChildElems find { _.name == "Title" }
      result.get
    }


  // XPath: doc("bookstore.xml")//Title

  val titles =
    for (title <- bookstore.findAllElemsOrSelf filter (_.name == "Title")) yield title


  // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

  val isbns =
    for (book <- bookstore.allChildElems filter (_.name == "Book")) yield book.attributes("ISBN")


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

  val titlesOfCheapBooks =
    for {
      book <- bookstore.allChildElems filter { _.name == "Book" }
      if book.attributes("Price").toInt < 90
    } yield {
      val result = book.allChildElems find { _.name == "Title" }
      result.get
    }


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

  val cheapUllmanBookTitles =
    for {
      book <- bookstore.allChildElems filter { _.name == "Book" }
      if book.attributes("Price").toInt < 90
      authors = book.allChildElems.filter(_.name == "Authors").head
      authorLastName <- authors.allChildElems filter { _.name == "Author" } flatMap { e => e.allChildElems filter (_.name == "Last_Name") } map { _.text.trim }
      if authorLastName == "Ullman"
      authorFirstName <- authors.allChildElems filter { _.name == "Author" } flatMap { e => e.allChildElems filter (_.name == "First_Name") } map { _.text.trim }
      if authorFirstName == "Jeffrey"
    } yield book.allChildElems.find(_.name == "Title").get


  // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

  val ullmanButNotWidomBookTitles =
    for {
      book <- bookstore.allChildElems filter { _.name == "Book" }
      authorNames = {
        val result = book.findAllElemsOrSelf filter { _.name == "Author" } map { _.allChildElems.find(_.name == "Last_Name").get.text.trim }
        result.toSet
      }
      if authorNames.contains("Ullman") && !authorNames.contains("Widom")
    } yield book.allChildElems.find(_.name == "Title").get

The queries above are more verbose than the equivalent XPath expressions, but they are also easy to understand semantically.
Besides ``Elem`` methods ``findAllElemsOrSelf``, ``allChildElems`` and possibly ``findTopmostElemsOrSelf``, Scala's Collections
API does the rest.

**In summary, using the Scala Collections API and a minimal "mini-yaidom" API, the contours
of a powerful XML querying API already become visible.**

Yaidom and namespaces
---------------------

TODO Mini-yaidom is not enough, of course. Namespaces are first-class citizens in yaidom.
Qualified names, expanded names. Namespace declarations and in-scope namespaces ("scopes").
Reasoning about namespaces.

Yaidom uniform querying API
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

