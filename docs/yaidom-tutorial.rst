===============
Yaidom tutorial
===============

Introduction
============

Yaidom is *yet another immutable DOM-like* XML API, written in the `Scala`_ programming language. It is an XML API that:

* leverages the highly expressive Scala Collections API
* offers immutable thread-safe DOM-like element trees
* offers first-class support for namespaces
* interoperates very well with `JAXP`_

This tutorial introduces the yaidom API. Some basic Scala knowledge is assumed, especially about the Scala
`Collections API`_.

The tutorial is organized as follows:

* Yaidom foundations

  * Leveraging Scala Collections
  * Yaidom and namespaces
  * JAXP

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
.. _`JAXP`: http://en.wikipedia.org/wiki/Java_API_for_XML_Processing

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
    def findAllElemsOrSelf: immutable.IndexedSeq[Elem] =
      self +: (allChildElems flatMap (_.findAllElemsOrSelf))

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
      bookOrMagazine <- bookstore.allChildElems
      if Set("Book", "Magazine").contains(bookOrMagazine.name)
      title <- bookOrMagazine.allChildElems find { _.name == "Title" }
    } yield title


  // XPath: doc("bookstore.xml")//Title

  val titles =
    for (title <- bookstore.findAllElemsOrSelf if title.name == "Title") yield title


  // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

  val isbns =
    for (book <- bookstore.allChildElems if book.name == "Book") yield book.attributes("ISBN")


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

  val titlesOfCheapBooks =
    for {
      book <- bookstore.allChildElems
      if (book.name == "Book") && (book.attributes("Price").toInt < 90)
      title <- book.allChildElems find { _.name == "Title" }
    } yield title


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

  def authorLastAndFirstNames(bookElem: Elem): immutable.IndexedSeq[(String, String)] = {
    for {
      author <- bookElem.findAllElemsOrSelf
      if author.name == "Author"
    } yield {
      val lastNames = author.allChildElems filter { _.name == "Last_Name" } map { _.text.trim }
      val firstNames = author.allChildElems filter { _.name == "First_Name" } map { _.text.trim }
      (lastNames.mkString, firstNames.mkString)
    }
  }

  val cheapUllmanBookTitles =
    for {
      book <- bookstore.allChildElems
      if (book.name == "Book") &&
        (book.attributes("Price").toInt < 90 && authorLastAndFirstNames(book).contains(("Ullman", "Jeffrey")))
    } yield book.allChildElems.find(_.name == "Title").get


  // XPath: doc("bookstore.xml")//Book[Authors/Author/Last_Name = "Ullman" and count(Authors/Author[Last_Name = "Widom"]) = 0]

  def findAuthorNames(bookElem: Elem): immutable.IndexedSeq[String] = {
    for {
      author <- bookElem.findAllElemsOrSelf
      if author.name == "Author"
      lastName <- author.allChildElems
      if lastName.name == "Last_Name"
    } yield lastName.text.trim
  }

  val ullmanButNotWidomBookTitles =
    for {
      book <- bookstore.allChildElems
      if book.name == "Book"
      authorNames = findAuthorNames(book)
      if authorNames.contains("Ullman") && !authorNames.contains("Widom")
    } yield book.allChildElems.find(_.name == "Title").get

The queries above are more verbose than the equivalent XPath expressions, but they are also easy to understand semantically.
Using the Scala Collections API, along with only a few ``Elem`` methods such as ``findAllElemsOrSelf`` and ``allChildElems``,
much (namespace-agnostic) XML querying is already possible. This says a lot about the expressive power of Scala's Collections
API, as a *universal query API*.

Yaidom queries are less verbose than the "mini-yaidom" queries above, but a lot of what the yaidom query API offers is just
convenience methods. The foundation is still the same: core ``Elem`` methods ``allChildElems`` and ``findAllElemsOrSelf``,
and the rest is offered by the Scala Collections API itself (and by some ``Elem`` convenience methods or syntactic sugar).
As an example of such a convenience method, yaidom offers method ``elem.filterElemsOrSelf(p)``, which is equivalent to
``elem.findAllElemsOrSelf.filter(p)``.

The "mini-yaidom" above also shows immutable element trees, just like the real yaidom API offers. These immutable element
trees are thread-safe.

Still the question remains: why not use a standard query language like `XQuery`_? Some possible reasons are:

* XQuery is a complex language. The different specifications of XQuery or related to it illustrate its complexity well.
* XQuery has a type system based on `XML Schema`_, which is known to be very complex in itself.
* Non-trivial computations are better and more directly expressed in a programming language like Scala than in XPath/XQuery or XQuery extension functions.
* There are too few mature open source XQuery libraries.
* The standard API for XQuery is `XQJ`_, which is to XML databases what JDBC is to relational databases. What if we only want to process XML in-memory?

So, having Scala (and a Java runtime) at our disposal, we can use Scala's Collections API as XML query language, without having to
resort to more complex XML querying libraries and setups.

**In summary, using the Scala Collections API and only a minimal "mini-yaidom" API, it already becomes obvious
that the Scala Collections API plus only a few core element query methods make for a powerful XML query language.
Indeed, the Scala Collections API lays most of the foundation of yaidom.**

.. _`XQuery`: http://www.w3.org/TR/xquery/
.. _`XML Schema`: http://www.w3.org/TR/xmlschema-2/
.. _`XQJ`: http://www.jcp.org/aboutJava/communityprocess/edr/jsr225/

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
``g`` everywhere (also in the namespace declaration, of course), without changing the "meaning" of the document.

The namespace declaration in the root element above leads to *in-scope namespaces*, or *scope*, from the root all the way down
to all descendants of the root (that is, the entire document). The namespace scope at each element is the accumulated effect of
the namespace declarations in the element and its ancestry. In this example, each element has the same scope, because only the
root element has declarations of namespaces, which are in scope throughout the document. The namespace scope contains only one
mapping from prefix ``f`` to namespace name ``http://www.w3schools.com/furniture``.

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
to the "internal consistency" of yaidom. They also help a lot in keeping the implementation of yaidom fairly simple, especially
in conversions between yaidom and DOM nodes. Along with the Scala Collections API and the "mini-yaidom" of the preceding section,
as well as JAXP, they are the foundation of yaidom.

**In summary, yaidom clearly distinguishes between qualified names and expanded names, and between namespace declarations
and in-scope namespaces. This is the second foundation of yaidom.**

.. _`Namespaces in XML 1.0`: http://www.w3.org/TR/REC-xml-names/
.. _W3Schools: http://www.w3schools.com/xml/xml_namespaces.asp
.. _`James Clark`: http://www.jclark.com/xml/xmlns.htm

JAXP
----

When creating an XML processing library, it is very tempting to make parsing and serialization of XML look easy.
That is especially the case in Scala, because of its expressiveness. Unfortunately, the expressiveness and orthogonality
of Scala do not extend to the domain of XML processing. For many XML documents in the wild, the specific XML parser configuration
affects the resulting DOM tree or SAX events. Details of whitespace handling, entity resolution, namespace handling etc. may
depend heavily on the XML parser or serializer configuration, and are often obscure. To make things worse, different XML-related
specifications often contradict each other or are incompatible. For example, DTDs do not understand namespaces.

Yaidom takes the position that details of XML parsing and serialization are hard, and are best left to JAXP. Yaidom also does not
try to make parsing and serialization look easy, but instead encourages the user to take control over configuration of XML
parsers and serializers, instead of hiding parsers and serializers behind a clean but naive API.

Yaidom offers several JAXP-based ``DocumentParser`` and ``DocumentPrinter`` implementations. There are implementations
based on SAX, DOM, StAX and DOM Load/Save. As said above, the user has full control over JAXP configuration. For example,
using a ``DocumentParserUsingDom``, the yaidom user can suppress entity resolution (for performance and/or security reasons)
by configuring an ``EntityResolver``, as if DOM were used directly.

**In summary, JAXP is the third foundation of yaidom. The gory details of XML parsing and serialization are left to JAXP,
and yaidom makes no effort hiding JAXP, thus giving the user full control over JAXP parser/serializer configuration.**

Yaidom uniform query API
========================

ParentElemLike trait
--------------------

Yaidom takes the position that one size does not fit all, when it comes to XML processing. (On the other hand, yaidom is a DOM-like
API, and does not know the exact XML strings from which DOM-like trees are parsed). For example, the default ``Elem``
class represents immutable (thread-safe) element nodes (that do not know about their parent elements). As another example,
yaidom offers immutable elements that can be compared for some notion of equality, but carry less data than the default
element class. As yet another example, yaidom offers wrappers around DOM elements.

All these different element classes have one thing in common, viz. the *same yaidom query API*. The yaidom query API consists
of a Scala *trait* inheritance tree. The root trait is the ``ParentElemLike`` trait.

Each trait in the query API inheritance tree turns a small API into a *rich API*. In particular, the ``ParentElemLike``
trait turns a small API that implements only method ``allChildElems`` into a rich query API. The rich API contains the
fundamental method ``findAllElemsOrSelf``, just like in the "mini-yaidom" above. It also offers convenience methods, such as
method ``filterElemsOrSelf`` (which takes an element predicate).

Below we use the ``ParentElemLike`` API to rewrite the queries given earlier, where we used "mini-yaidom". First the same
DOM-like tree is created, this time in yaidom. We create elements of the default ``Elem`` element class. To do so, we
use so-called ``ElemBuilders``. The distinction between ``Elem`` and ``ElemBuilder`` is explained later in this tutorial.
The sample XML data is created in yaidom as follows::

  import eu.cdevreeze.yaidom._
  import NodeBuilder._

  val book1: ElemBuilder = {
    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
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
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-815504-6", QName("Price") -> "100"),
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
      attributes = Vector(QName("ISBN") -> "ISBN-0-11-222222-3", QName("Price") -> "50"),
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
      attributes = Vector(QName("ISBN") -> "ISBN-9-88-777777-6", QName("Price") -> "25"),
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
      attributes = Vector(QName("Month") -> "January", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  val magazine2: ElemBuilder = {
    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "National Geographic")))
  }

  val magazine3: ElemBuilder = {
    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "February", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Newsweek")))
  }

  val magazine4: ElemBuilder = {
    elem(
      qname = QName("Magazine"),
      attributes = Vector(QName("Month") -> "March", QName("Year") -> "2009"),
      children = Vector(
        textElem(QName("Title"), "Hector and Jeff's Database Hints")))
  }

  val bookstore: Elem = {
    elem(
      qname = QName("Bookstore"),
      children = Vector(
        book1, book2, book3, book4, magazine1, magazine2, magazine3, magazine4)).build(Scope.Empty)
  }

We can now use convenience methods offered by trait ``ParentElemLike``. For example, instead of writing::

  elem.findAllElemsOrSelf filter { e => e.localName == "Book" }

we can now write::

  elem filterElemsOrSelf { e => e.localName == "Book" }

Method ``localName`` is offered by subtrait ``ElemLike``, and returns the local part of the element name. After all, the
element name may have a namespace.

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
trait ``ParentElemLike`` offers many convenience methods that make the rewritten queries less verbose than the "mini-yaidom"
versions.

The queries above can become more concise by using operator notation ``\`` for ``filterChildElems`` and ``\\`` for
``filterElemsOrSelf``. Below we will see more convenience methods, leading to more conciseness without loss of clarity.

If we had used different element classes than the default yaidom ``Elem`` class, such as ``eu.cdevreeze.yaidom.resolved.Elem`` or
``eu.cdevreeze.yaidom.dom.DomElem``, the query code above would stay the same! Indeed, the ``ParentElemLike`` trait is a
*uniform* XML query API in yaidom (or in future yaidom extensions).

To summarize:

* Yaidom offers an *element-centric query API*
* This query API is based on the *Scala Collections API*
* More precisely, the underlying *core query API* is the Scala Collections API plus core methods ``allChildElems`` and ``findAllElemsOrSelf``
* The base trait of the query API, ``ParentElemLike``, turns a small API (method ``allChildElems``) into a *rich API*
* The *fundamental query method* ``findAllElemsOrSelf`` is defined in terms of ``allChildElems``, just like in the "mini-yaidom" example
* This rich API also offers many convenience query methods for child elements, descendant elements and descendant-or-self elements
* This API is *uniform*, in that this trait is mixed in (as query API) by different element classes in yaidom, even by yaidom wrappers for DOM
* Indeed this API knows almost nothing about XML elements (just that it has method ``allChildElems``), which makes it easy to mix in
* The ``ParentElemLike`` API is trivial to understand semantically, due to Scala's Collections API as its clearly visible foundation
* Although the API is more verbose than XPath, due to its simplicity and the expressive power of Scala, it can be very useful for XML querying

ElemLike trait
--------------

The ``ParentElemLike`` trait knows almost nothing about the elements. It only knows that elements can have child elements.
Yet typical element classes contain methods for element name (EName and/or QName), attributes, etc. This is where the
``ElemLike`` trait comes in. It extends trait ``ParentElemLike``, and turns a small API with methods ``allChildElems``,
``resolvedName`` and ``resolvedAttributes`` into a *rich API* in which queries for elements or attributes can be passed
names instead of predicates.

In other words, trait ``ElemLike`` adds only convenience methods to super-trait ``ParentElemLike`` (which itself consists mostly
of convenience methods, as discussed above).

Most element classes in yaidom not only mix in trait ``ParentElemLike``, but sub-trait ``ElemLike`` as well. Hence the queries
we write using the ``ElemLike`` API can often be used unchanged for different element types in yaidom.

Using the ``ElemLike`` trait, we can make the queries above more concise, without losing any clarity. This time we do not use
local parts of names in the queries, but the full expanded names (which happen to have no namespace). These more concise versions
are::

  // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

  val bookOrMagazineTitles =
    for {
      bookOrMagazine <- bookstore filterChildElems { e => Set(EName("Book"), EName("Magazine")).contains(e.resolvedName) }
      title <- bookOrMagazine.findChildElem(EName("Title"))
    } yield title


  // XPath: doc("bookstore.xml")//Title
  // Note the use of method filterElems instead of filterElemsOrSelf

  val titles =
    for (title <- bookstore.filterElems(EName("Title"))) yield title


  // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

  val isbns =
    for (book <- bookstore.filterChildElems(EName("Book"))) yield book.attribute(EName("ISBN"))


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title

  val titlesOfCheapBooks =
    for {
      book <- bookstore.filterChildElems(EName("Book"))
      price <- book.attributeOption(EName("Price"))
      if price.toInt < 90
    } yield book.getChildElem(EName("Title"))

Using operator notation ``\`` for ``filterChildElems`` and ``\\`` for ``filterElemsOrSelf``, we could write::

  // XPath: doc("bookstore.xml")/Bookstore/(Book | Magazine)/Title

  val bookOrMagazineTitles =
    for {
      bookOrMagazine <- bookstore \ { e => Set(EName("Book"), EName("Magazine")).contains(e.resolvedName) }
      title <- bookOrMagazine.findChildElem(EName("Title"))
    } yield title


  // XPath: doc("bookstore.xml")//Title

  val titles =
    for (title <- bookstore \\ EName("Title")) yield title


  // XPath: doc("bookstore.xml")/Bookstore/Book/data(@ISBN)

  val isbns =
    for (book <- bookstore \ EName("Book")) yield book.attribute(EName("ISBN"))


  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90]/Title
  // Note the use of operator notation for method attributeOption

  val titlesOfCheapBooks =
    for {
      book <- bookstore \ EName("Book")
      price <- book \@ EName("Price")
      if price.toInt < 90
    } yield book.getChildElem(EName("Title"))

Of course, in these versions of the queries, the search criteria are ENames instead of local names, so we have to get
the namespaces in those ENames right, if any.

To summarize:

* Trait ``ElemLike`` extends trait ``ParentElemLike``, adding knowledge about ENames of elements and attributes
* Trait ``ElemLike`` turns a small API (methods ``allChildElems``, ``resolvedName`` and ``resolvedAttributes``) into a rich API
* This trait only adds convenience methods for EName-based querying to the super-trait, so adds no core query methods
* Most element classes in yaidom mix in trait ``ElemLike`` (not just its super-trait)

PathAwareElemLike trait
-----------------------

Sometimes we want to query for "paths" to elements rather than for elements themselves. Recall the following example, given earlier,
but this time in yaidom instead of "mini-yaidom"::

  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

  def authorLastAndFirstNames(bookElem: Elem): immutable.IndexedSeq[(String, String)] = {
    for {
      author <- bookElem.filterElemsOrSelf(EName("Author"))
    } yield {
      val lastNames = author.filterChildElems(EName("Last_Name")) map { _.text.trim }
      val firstNames = author.filterChildElems(EName("First_Name")) map { _.text.trim }
      (lastNames.mkString, firstNames.mkString)
    }
  }

  val cheapUllmanBookTitles =
    for {
      book <- bookstore.filterChildElems(EName("Book"))
      if (book.attribute(EName("Price")).toInt < 90 && authorLastAndFirstNames(book).contains(("Ullman", "Jeffrey")))
    } yield book.getChildElem(EName("Title"))

In the query above a top-down approach was used. Per "cheap" book, its author descendants were analyzed and filtered. What if
we want to folllow a bottom-up approach, and start from matching authors and look up the matching books in the ancestry of the
author? For the immutable ``Elem`` classes in yaidom that is a problem, because these immutable elements do not know their
parents.

There is a way to get the ancestry of an element, if we know the "path" from the document element to that element.
As we will see shortly, we can query for "paths" just like we can query for elements, and having such "paths", it is relatively
cheap to get the parent element, grandparent element etc.

The above-mentioned "paths" are represented by class ``eu.cdevreeze.yaidom.ElemPath``. Class ``eu.cdevreeze.yaidom.ElemPathBuilder``
can be used to create ``ElemPath`` instances. Let's give an example, in the context of the bookstore above::

  val book4Path = ElemPathBuilder.from(QName("Book") -> 3).build(Scope.Empty)
  
  val foundBook4: Elem = bookstore.getWithElemPath(book4Path) // Jennifer's Economical Database Hints
  
  val lastNamePath = ElemPathBuilder.from(
    QName("Book") -> 3,
    QName("Authors") -> 0,
    QName("Author") -> 0,
    QName("Last_Name") -> 0).build(Scope.Empty)

  val foundLastName: Elem = bookstore.getWithElemPath(lastNamePath) // Widom

So, first we build a "path" for the child element named "Book" with (0-based) index 3, that is, the 4th child element named "Book".
Then we look up the element with that path, taking the bookstore element as root. This indeed returns the 4th book in the bookstore.
Note that the root itself is not mentioned in the "path". That's one big difference with XPath.

Next we look up the last name of the first author of that book. That is, the 4th child element named "Book", from that the
first child element named "Authors", from that the first child element named "Author", and finally from that the first child
element named "Last_Name". When applying that "path" to the bookstore element, this indeed results in the first author's last name.

Now that we know the basics of ``ElemPath``, we can turn to the part of the yaidom query API that deals with "paths".
Trait ``PathAwareElemLike`` is that API. It contains query methods for obtaining ElemPaths instead of elements, as well as
methods to get an element given an ElemPath (for example, method ``getWithElemPath`` above).

Trait ``PathAwareElemLike`` extends trait ``ElemLike``, because it knows about element paths and therefore about (resolved)
element names.

Trait ``PathAwareElemLike`` mirrors trait ``ParentElemLike``, in that each query in ``ParentElemLike`` that returns elements
has a counterpart in ``PathAwareElemLike`` that returns ElemPaths instead of elements.

Let's now rewrite the query at the beginning of this section, this time in a bottom-up manner, using trait ``PathAwareElemLike``::

  // XPath: doc("bookstore.xml")/Bookstore/Book[@Price < 90 and Authors/Author[Last_Name = "Ullman" and First_Name = "Jeffrey"]]/Title

  def authorLastAndFirstName(authorElem: Elem): (String, String) = {
    val lastNames = authorElem.filterChildElems(EName("Last_Name")) map { _.text.trim }
    val firstNames = authorElem.filterChildElems(EName("First_Name")) map { _.text.trim }
    (lastNames.mkString, firstNames.mkString)
  }

  val cheapUllmanBookTitles =
    for {
      authorPath <- bookstore filterElemOrSelfPaths { _.resolvedName == EName("Author") }
      authorElem = bookstore.getWithElemPath(authorPath)
      if authorLastAndFirstName(authorElem) == ("Ullman", "Jeffrey")
      bookPath <- authorPath findAncestorPath { _.endsWithName(EName("Book")) }
      bookElem = bookstore.getWithElemPath(bookPath)
      if bookElem.attributeOption(EName("Price")).map(_.toInt).getOrElse(0) < 90
    } yield bookElem.getChildElem(EName("Title"))

Note the use of method ``ElemPath.findAncestorPath`` to find a path to an ancestor element.

It is wise not to overuse ElemPaths. After all, they depend on an implicit root element, so it is best to use them rather locally.
Moreover, indexing using ElemPaths is not very efficient. So querying for large collections of paths and then using them to
find elements is rarely useful.

To summarize:

* Trait ``PathAwareElemLike`` extends trait ``ElemLike``, adding queries for finding element paths instead of elements
* Trait ``PathAwareElemLike`` turns a small API (methods ``allChildElems``, ``resolvedName`` and ``resolvedAttributes``) into a rich API
* The query methods in this trait are handy for a bottom-up style of querying, but it is wise not to overuse element paths
* Some element classes in yaidom mix in trait ``PathAwareElemLike`` (since they know about resolved element names etc.), and therefore offer all of this query API
* Yet class ``ElemBuilder`` only mixes in trait ``ParentElemLike`` (since it does not know about resolved element names etc.)

Working with default yaidom Elems
=================================

Default Elems
-------------

As mentioned earlier, yaidom does not think that one size fits all, when it comes to DOM-like class hierarchies.
After all, there are many subtle abstraction levels at which an XML document can be looked at, ranging from the exact XML strings
to DOM-like representations keeping only parts of the XML InfoSet. These different implicit abstraction levels also come into
play when considering the notion(s) of equality for XML. For example, at a high level of abstraction the exact (namespace)
prefixes are often considered irrelevant, when comparing XML documents for equality.

Yaidom's default element class tries to find some "middle ground". It does not define any semantic notion of equality.

The default element class in yaidom is ``eu.cdevreeze.yaidom.Elem``. It is part of a ``Node`` hierarchy that includes
classes like ``Text``, ``Comment`` and others. Class ``Elem`` has the following characteristics:

* It mixes in trait ``PathAwareElemLike``, and therefore offers all of that *query API*
* It is *immutable* and thread-safe
* Therefore, Elems do not know about their parent elements, but using element paths from a root element this should mostly not be a problem
* Elems are reasonably easy to construct from scratch, using ``ElemBuilders``
* There is excellent support for parsing and serializing these Elems, using ``DocumentParser`` and ``DocumentPrinter`` implementations, resp.
* Elems are reasonably good at "lossless roundtripping", keeping differences in the XML text limited after parsing and serializing
* These Elems offer support for "functional updates" (see below)
* The Elem class keeps the following state: element QName, attributes (mapping QNames to string values), in-scope namespaces, and a list of child nodes
* Although this Elem class keeps in-scope namespaces, it does not keep namespace declarations, thus enabling Elem creation from other Elem child nodes
* When serializing an Elem, namespace declarations are inserted by relativizing the scope of the element against the parent scope.

Hence, the default Elem class is immutable, and otherwise tries to find a balance between competing design forces for DOM-like trees.
The extent to which "lossless roundtripping" is supported shows the compromise made. For example:

* Attribute order is maintained, although the XML InfoSet specification does not consider attribute order relevant
* Yet namespace declaration order is not preserved while "roundtripping"
* Whitespace outside the document element is lost (a yaidom ``Document`` has a document element and can have comments and processing instructions, and that's it)
* The difference between the 2 forms of an empty element is not preserved
* DTDs have no explicit support in yaidom, let alone default attributes

Creating Elems directly is somewhat cumbersome, because the in-scope namespaces must be passed for each element in the tree.
For example, using default namespace "http://bookstore", we could write::

  val book1: Elem = {
    import Node._ // This import is used for direct Elem creation (not via ElemBuilders)

    val scope = Scope.from("" -> "http://bookstore")

    elem(
      qname = QName("Book"),
      attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
      scope = scope,
      children = Vector(
        textElem(QName("Title"), scope, "A First Course in Database Systems"),
        elem(
          qname = QName("Authors"),
          scope = scope,
          children = Vector(
            elem(
              qname = QName("Author"),
              scope = scope,
              children = Vector(
                textElem(QName("First_Name"), scope, "Jeffrey"),
                textElem(QName("Last_Name"), scope, "Ullman"))),
            elem(
              qname = QName("Author"),
              scope = scope,
              children = Vector(
                textElem(QName("First_Name"), scope, "Jennifer"),
                textElem(QName("Last_Name"), scope, "Widom")))))))
  }

If we print the result of ``book1.toString``, we get::

  elem(
    qname = QName("Book"),
    attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
    namespaces = Declarations.from("" -> "http://bookstore"),
    children = Vector(
      elem(
        qname = QName("Title"),
        children = Vector(
          text("A First Course in Database Systems")
        )
      ),
      elem(
        qname = QName("Authors"),
        children = Vector(
          elem(
            qname = QName("Author"),
            children = Vector(
              elem(
                qname = QName("First_Name"),
                children = Vector(
                  text("Jeffrey")
                )
              ),
              elem(
                qname = QName("Last_Name"),
                children = Vector(
                  text("Ullman")
                )
              )
            )
          ),
          elem(
            qname = QName("Author"),
            children = Vector(
              elem(
                qname = QName("First_Name"),
                children = Vector(
                  text("Jennifer")
                )
              ),
              elem(
                qname = QName("Last_Name"),
                children = Vector(
                  text("Widom")
                )
              )
            )
          )
        )
      )
    )
  )

In this String representation it is visible that the root element has a default namespace declaration, and the other
elements have no namespace declarations. Indeed, the root element has a scope that must be created by a namespace declaration,
whereas the other elements all have the same scope, so need no namespace declarations themselves. In the next section a
better way is shown to create elements from scratch.

Ideally all namespace declarations are in the root element. In any case, be careful not to undeclare namespaces. This is not
allowed in XML 1.0 (except for default namespaces). Yet it is very easy to accidently undeclare namespaces. For example, above
we could have passed the empty scope to descendant elements of the root element, which would lead to namespace undeclarations.
Again, it is much safer to create elements from scratch using ``ElemBuilders``, as shown in the next section. When parsing
XML into ``Elems``, namespace scopes are created by the ``DocumentParser``.

If a created ``Elem`` has any namespace undeclarations, invoke method ``notUndeclaringPrefixes``, and use the resulting Elem instead.
Otherwise serialization may lead to a corrupt XML document.

ElemBuilders
------------

Class ``ElemBuilder`` is what the name suggests: a builder of ``Elems``. ElemBuilders do not carry any scopes, and that makes
them easier to use than Elems when creating Elems from scratch. Since ElemBuilders have no scopes, they have no way to resolve
own QNames (of the element itself and its attributes). That's ok, because of the purpose of ElemBuilders.

So, Elems carry scopes but no namespace declarations, whereas ElemBuilders carry namespace declarations but no scopes.

Let's now create the same book element as above, this time using an ``ElemBuilder``. Here is how::

  val book1: Elem = {
    import NodeBuilder._ // This import is used for ElemBuilder creation

    val elemBuilder = {
      elem(
        qname = QName("Book"),
        attributes = Vector(QName("ISBN") -> "ISBN-0-13-713526-2", QName("Price") -> "85", QName("Edition") -> "3rd"),
        namespaces = Declarations.from("" -> "http://bookstore"),
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

    // Only now a parent scope is passed, which is empty, because the root element already declared the used namespaces
    val scope = Scope.Empty

    elemBuilder.build(scope)
  }

Here we also knew the namespaces used in the element tree, but we declared this (default) namespace only once. The call
``elemBuilder.build(scope)`` then recursively invokes ``parentScope.resolve(namespaceDeclarations)``, thus giving each created
Elem its namespace scope. Using ElemBuilders, the danger of accidently creating namespace undeclarations is minimal, because
one would have to explicitly do so instead of implicitly.

Normally, elements are created by parsing an XML document, however. That is the topic of the next section.

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

