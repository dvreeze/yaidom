===============
Yaidom tutorial
===============

Introduction
============

Yaidom is *yet another immutable DOM-like* XML API, written in the `Scala`_ programming language. It is an XML API that:

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

* Yaidom uniform query API

  * ParentElemLike trait
  * ElemLike trait
  * PathAwareElemLike trait

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

  // This is NOT yaidom! This is some "mini-yaidom" looking a lot like yaidom, if we ignore namespaces

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

  // This is NOT yaidom! This is some "mini-yaidom" looking a lot like yaidom, if we ignore namespaces

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

Having this bookstore DOM-like tree, we can write queries against it. Note that "mini-yaidom" class ``Elem`` has very few
query methods on its own. In the queries, most work is done by Scala's Collections API. Some queries are::

  // This is NOT yaidom! This is some "mini-yaidom" looking a lot like yaidom, if we ignore namespaces

  // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

  val bookOrMagazineTitles =
    for {
      bookOrMagazine <- bookstore.allChildElems filter { e => Set("Book", "Magazine").contains(e.name) }
      title <- bookOrMagazine.allChildElems find { _.name == "Title" }
    } yield title


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
      title <- book.allChildElems find { _.name == "Title" }
    } yield title


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

  def authorLastAndFirstNames(bookElem: Elem): immutable.IndexedSeq[(String, String)] = {
    for {
      author <- bookElem.findAllElemsOrSelf filter { e => e.name == "Author" }
    } yield {
      val lastNames = author.allChildElems filter { _.name == "Last_Name" } map { _.text.trim }
      val firstNames = author.allChildElems filter { _.name == "First_Name" } map { _.text.trim }
      (lastNames.mkString, firstNames.mkString)
    }
  }

  val cheapUllmanBookTitles =
    for {
      book <- bookstore.allChildElems filter { _.name == "Book" }
      if book.attributes("Price").toInt < 90 && authorLastAndFirstNames(book).contains(("Ullman", "Jeffrey"))
    } yield book.allChildElems.find(_.name == "Title").get


  // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

  def findAuthorNames(bookElem: Elem): immutable.IndexedSeq[String] = {
    for {
      author <- bookElem.findAllElemsOrSelf filter { _.name == "Author" }
      lastName <- author.allChildElems filter { _.name == "Last_Name" }
    } yield lastName.text.trim
  }

  val ullmanButNotWidomBookTitles =
    for {
      book <- bookstore.allChildElems filter { _.name == "Book" }
      authorNames = findAuthorNames(book)
      if authorNames.contains("Ullman") && !authorNames.contains("Widom")
    } yield book.allChildElems.find(_.name == "Title").get

The queries above are more verbose than the equivalent XPath expressions, but they are also easy to understand semantically.
Using the Scala Collections API, along with only a few ``Elem`` methods such as ``findAllElemsOrSelf`` and ``allChildElems``,
much (namespace-agnostic) XML querying is already possible. This says a lot about the expressive power of Scala's Collections
API, as a *universal query API*.

Yaidom queries are less verbose than the queries above, but a lot of what the yaidom query API offers is just syntactic sugar.
The foundation is still the same: core ``Elem`` methods ``allChildElems`` and ``findAllElemsOrSelf``, and the rest is offered
by the Scala Collections API itself, and/or by some syntactic sugar. As an example of the latter, yaidom offers method
``elem.filterElemsOrSelf(p)``, which is equivalent to ``elem.findAllElemsOrSelf.filter(p)``.

The "mini-yaidom" above also shows immutable element trees, just like the real yaidom API offers. These immutable element
trees are thread-safe.

**In summary, using the Scala Collections API and only a minimal "mini-yaidom" API, the contours
of a powerful XML query API already become visible. Indeed, the Scala Collections API lays the foundation
of yaidom.**

Yaidom and namespaces
---------------------

The "mini-yaidom" above offers no support for namespaces, unlike the real yaidom API. Good namespace support is another
foundation of yaidom.

One important distinction is that between *qualified names* and *expanded names*. Alas, many XML APIs do not clearly
distinguish between the two. For a formal description of these 2 types of names, see `Namespaces in XML 1.0`_.

For example, consider the following simple XML document (from W3Schools_)::

  <f:table xmlns:f="http://www.w3schools.com/furniture">
    <f:name>African Coffee Table</f:name>
    <f:width>80</f:width>
    <f:length>120</f:length>
  </f:table>

The qualified names in this example are:

* ``f:table``
* ``f:name``
* ``f:width``
* ``f:length``

These qualified names all use the same prefix ``f``. This prefix is introduced in the XML by the *namespace declaration*
``xmlns:f="http://www.w3schools.com/furniture"``, occurring in the root element. This namespace declaration binds the
prefix ``f`` to the namespace URI ``http://www.w3schools.com/furniture``. Although it looks like an URL, it is just
a namespace name, and there is no promise of any document behind the name interpreted as URL. (This confuses XML beginners
a lot.)

Using this namespace declaration, the qualified names above are resolved as expanded names. These expanded names, written
in `James Clark`_ notation, are as follows:

* ``{http://www.w3schools.com/furniture}table``
* ``{http://www.w3schools.com/furniture}name``
* ``{http://www.w3schools.com/furniture}width``
* ``{http://www.w3schools.com/furniture}length``

These expanded names do not occur in XML documents. Expanded names are too long to be practical. On the other hand,
prefixed names have no meaning outside their context (namely in-scope namespaces), whereas expanded names have an
existence on their own. Moreover, prefixes themselves are just placeholders, and can easily be replaced by other prefixes
without changing the meaning of the XML document. For example, in the XML above, we could replace prefix ``f`` by prefix
``g`` everywhere (also in the namespace declaration, of course), without changing the meaning of the document.

The namespace declaration in the root element above leads to *in-scope namespaces*, or *scope*, from the root all the way down
to all descendants of the root (that is, the entire document). The namespace scope at each element is the accumulated effect of
the namespace declarations in the element and its ancestry. In this example, each element has the same scope, because only the
root element has namespace declarations. The namespace scope contains only one mapping from prefix ``f`` to namespace name
``http://www.w3schools.com/furniture``.

The concepts mentioned above are modelled in yaidom by the following classes:

* ``eu.cdevreeze.yaidom.QName``, for example unprefixed name ``QName("book")`` and prefixed name ``QName("b:book")``
* ``eu.cdevreeze.yaidom.EName``, for example ``EName("book")`` (without namespace) and ``EName("{http://bookstore}book")``
* ``eu.cdevreeze.yaidom.Declarations``
* ``eu.cdevreeze.yaidom.Scope``

Scopes and declarations are backed by a ``Map`` from prefixes to namespace names. If the prefix is the empty string,
the default namespace is meant. In namespace declarations, if the namespace name is empty, a namespace undeclaration
is meant. (Note that unlike XML 1.1, XML 1.0 does not allow namespace undeclarations, except for default namespaces.)

The following code snippet shows resolution of qualified names as expanded names, given a scope::

  val scope1 = Scope.from() // empty scope

  scope1.resolveQName(QName("book")) // Some(EName("book"))
  scope1.resolveQName(QName("book:book")) // None

  val scope2 =
    Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

  scope2.resolveQName(QName("book")) // Some(EName("{http://a}book"))
  scope2.resolveQName(QName("book:book")) // None
  scope2.resolveQName(QName("a:book")) // Some(EName("{http://a}book"))
  scope2.resolveQName(QName("c:bookstore")) // Some(EName("{http://ccc}bookstore"))
  scope2.resolveQName(QName("xml:lang")) // Some(EName("{http://www.w3.org/XML/1998/namespace}lang"))

Scopes and declarations can be calculated with. That is, given a scope, and using a declarations as "delta" against it,
we get another scope. In other words, ``scope1.resolve(declarations1)`` results in another ``Scope``. Likewise, the
"difference" between 2 scopes is a declarations. In other words, ``scope1.relativize(scope2)`` results in a ``Declarations``.

Scopes and declarations obey some interesting properties. For example::

  scope1.resolve(scope1.relativize(scope2)) == scope2

These properties, as well as the definitions of ``Scope`` methods ``resolve`` and ``relativize`` contribute significantly
to the "internal consistency" of yaidom. They also helped a lot in making yaidom easier to implement, especially in conversions
between yaidom and DOM nodes. Along with the Scala Collections API and the "mini-yaidom" of the preceding section, they are
the foundation of yaidom.

**In summary, yaidom clearly distinguishes between qualified names and expanded names, and between namespace declarations
and in-scope namespaces. This is the second foundation of yaidom.**

.. _`Namespaces in XML 1.0`: http://www.w3.org/TR/REC-xml-names/
.. _W3Schools: http://www.w3schools.com/xml/xml_namespaces.asp
.. _`James Clark`: http://www.jclark.com/xml/xmlns.htm

Yaidom uniform query API
========================

ParentElemLike trait
--------------------

Yaidom takes the position that one size does not fit all, when it comes to XML processing. For example, the default ``Elem``
class represents immutable (thread-safe) element nodes (that do not know about their parent elements). As another example,
yaidom offers immutable elements that can be compared for some notion of equality, but carry less data than the default
elements. As yet another example, yaidom offers wrappers around DOM elements.

All these different element classes have one thing in common, viz. the *same yaidom query API*. The yaidom query API consists
of a Scala *trait* inheritance tree. The root trait is the ``ParentElemLike`` trait.

Each trait in the query API inheritance tree turns a small API into a *rich API*. In particular, the ``ParentElemLike``
trait turns a small API that implements only method ``allChildElems`` into a rich query API. The rich API contains the
fundamental method ``findAllElemsOrSelf``, just like in the "mini-yaidom" above. It also offers syntactic sugar, such as method
``filterElemsOrSelf`` (which takes an element predicate).

Below we use the ``ParentElemLike`` API to rewrite the queries given earlier, where we used "mini-yaidom". First the same
DOM-like tree is created, this time in yaidom. We create elements of the default ``Elem`` element class. To do so, we
use so-called ``ElemBuilder``s. The distinction between ``Elem`` and ``ElemBuilder`` is explained later in this tutorial.
The sample XML data is created in yaidom as follows::

  import eu.cdevreeze.yaidom._
  import NodeBuilder._

  val book1: ElemBuilder = {
    elem(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      children = Vector(
        textElem(QName("Title"), "A First Course in Database Systems"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jeffrey"),
                textElem(QName("Last_Name"), "Ullman"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jennifer"),
                textElem(QName("Last_Name"), "Widom")))))))
  }

  val book2: ElemBuilder = {
    elem(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
      children = Vector(
        textElem(QName("Title"), "Database Systems: The Complete Book"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Hector"),
                textElem(QName("Last_Name"), "Garcia-Molina"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jeffrey"),
                textElem(QName("Last_Name"), "Ullman"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jennifer"),
                textElem(QName("Last_Name"), "Widom"))))),
        textElem(QName("Remark"), "Buy this book bundled with \"A First Course\" - a great deal!")))
  }

  val book3: ElemBuilder = {
    elem(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
      children = Vector(
        textElem(QName("Title"), "Hector and Jeff's Database Hints"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jeffrey"),
                textElem(QName("Last_Name"), "Ullman"))),
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Hector"),
                textElem(QName("Last_Name"), "Garcia-Molina"))))),
        textElem(QName("Remark"), "An indispensable companion to your textbook")))
  }

  val book4: ElemBuilder = {
    elem(
      qname = QName("Book"),
      attributes = Map(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
      children = Vector(
        textElem(QName("Title"), "Jennifer's Economical Database Hints"),
        elem(
          qname = QName("Authors"),
          children = Vector(
            elem(
              qname = QName("Author"),
              children = Vector(
                textElem(QName("First_Name"), "Jennifer"),
                textElem(QName("Last_Name"), "Widom")))))))
  }

  val magazine1: ElemBuilder = {
    elem(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "January", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  val magazine2: ElemBuilder = {
    elem(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  val magazine3: ElemBuilder = {
    elem(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Newsweek")))
  }

  val magazine4: ElemBuilder = {
    elem(
      qname = QName("Magazine"),
      attributes = Map(QName("Month") -> "March", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Hector and Jeff's Database Hints")))
  }

  val bookstore: Elem = {
    elem(
      qname = QName("Bookstore"),
      children = Vector(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)
  }

Using (almost) only ``ParentElemLike`` query methods on the bookstore element, we get the following rewritten queries (the
first 4 of them)::

  // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

  val bookOrMagazineTitles =
    for {
      bookOrMagazine <- bookstore filterChildElems { e => Set("Book", "Magazine").contains(e.localName) }
      title <- bookOrMagazine findChildElem { _.localName == "Title" }
    } yield title


  // XPath: doc("bookstore.xml")//Title
  // Note the use of method filterElems instead of filterElemsOrSelf

  val titles =
    for (title <- bookstore filterElems (_.localName == "Title")) yield title


  // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

  val isbns =
    for (book <- bookstore filterChildElems (_.localName == "Book")) yield book.attribute(EName("ISBN"))


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

  val titlesOfCheapBooks =
    for {
      book <- bookstore filterChildElems { _.localName == "Book" }
      price <- book.attributeOption(EName("Price"))
      if price.toInt < 90
    } yield book.getChildElem(EName("Title"))

Note the obvious equivalence to the "mini-yaidom" queries given earlier. Besides *core query method* ``findAllElemsOrSelf``,
trait ``ParentElemLike`` offers a lot of syntactic sugar that made the rewritten queries less verbose than the "mini-yaidom"
versions.

To summarize:

* Yaidom offers an *element-centric query API*
* This query API is based on the *Scala Collections API*, from the inside and on the outside
* The base trait of this query API, ``ParentElemLike``, turns a small API (method ``allChildElems``) into a *rich API*
* This rich API contains the fundamental query method ``findAllElemsOrSelf``, just like in the "mini-yaidom" example
* This rich API also offers many convenience query methods for child elements, descendant elements and descendant-or-self elements
* This API is *uniform*, in that this trait is mixed in by different element classes in yaidom, even by yaidom wrappers for DOM
* The ``ParentElemLike`` API is trivial to understand semantically, due to Scala's Collections API as its foundation
* The API may be more verbose than XPath, but due to its simplicity (w.r.t. its semantics), it can still be attractive for XML querying

ElemLike trait
--------------

Show convenience methods offered by the ElemLike API as well (using EName arguments instead of element predicates).
Show shorthand notations as well.

PathAwareElemLike trait
-----------------------

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

