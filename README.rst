=========================
Yet Another Immutable DOM
=========================

Yaidom (yet another immutable DOM) is a Scala-ish DOM-like API. Yaidom is DOM-like in that it represents
XML as in-memory trees of elements, text nodes, and other nodes. Intentionally, yaidom does not implement W3C DOM.
Yaidom is Scala-ish in that it leverages immutability and the Scala Collections API.

Why do we need yet another Scala DOM-like API? The standard Scala XML library has issues w.r.t. usability
and reliability, as described here_. The Anti-XML library aims at being a better alternative.
Yet it too has some issues of its own. For example, the prefix parts of qualified names are far more prominent
than namespace URIs (see `this issue`_). Anti-XML also has relatively much
conceptual weight.

Yaidom is a Scala DOM-like API that has been inspired by Anti-XML, but with some different underlying design choices.

.. _here: http://anti-xml.org/
.. _this issue: https://github.com/djspiewak/anti-xml/issues/78

Yaidom in a nutshell
====================

Some characteristics of yaidom are:

* **DOM-like**

  *  Like DOM, Yaidom represents XML as trees of nodes, stored entirely in memory
  *  Only XML is modeled, not HTML (yet TagSoup can be used as SAX parser)

* **Scala-ish**

  *  The node trees are immutable and therefore thread-safe
  *  Yaidom leverages the Scala Collections API
  *  Option types are used instead of null
  *  Yaidom trees can be queried easily using Scala *for comprehensions*

* **Strict when it comes to names and namespaces**

  *  Qualified names, expanded names, namespace scopes and namespace declarations are explicitly modeled
  *  Namespaces are first-class citizens in the API
  *  DTDs are not first-class citizens

* **Element-centric querying**

  *  Querying is offered by the ElemLike (implementation) trait (used together with the Scala Collections API)
  *  This trait is centered around elements only, which keeps this API simple (but still powerful)
  *  Other nodes (text, comments, etc.) are queried by querying for their parent elements
  *  Most ElemLike methods return/filter/transform elements in one of 3 "core" element sets: child elements, descendant elements, or descendant-or-self elements
  *  The ElemLike trait makes yaidom extensible, facilitating the definition of custom element trees

* **Conceptually simple**

  *  Few concepts in the API to learn and understand
  *  Also easy to implement, which helps in avoiding many hard to fix bugs
  *  Less ambitious than Scala's XML library and Anti-XML (no XPath-like support, no value equality)
  *  Leaves many hard parts to Java's JAXP, such as parsing and printing, but via easy to use wrappers
  *  Good interop with Java

As said above, yaidom has no XPath-like support, and it does not try to unify nodes with collections of nodes.
Yet yaidom trees can be queried quite well using Scala *for comprehensions*. This is more verbose than XPath-like
syntax (as in Scala's XML library, and in Anti-XML), but more straightforward to understand, use, and implement.

Yaidom quite intentionally leaves the hard parts of parsing and printing to Java's JAXP. Think for example about
"ignorable whitespace", or the many ways to configure a parser. Yaidom merely represents the parsed XML as immutable
Scala-ish node trees.

Usage
=====

Yaidom versions can be found in the Maven central repository. Assuming version 0.6.13, yaidom can be added as dependency
as follows (in an SBT or Maven build):

**SBT**::

    libraryDependencies += "eu.cdevreeze.yaidom" %% "yaidom" % "0.6.13"

**Maven2**::

    <dependency>
      <groupId>eu.cdevreeze.yaidom</groupId>
      <artifactId>yaidom_2.10</artifactId>
      <version>0.6.13</version>
    </dependency>

Note that yaidom itself has a few dependencies, which will be transitive dependencies in projects that use yaidom.
Yaidom has been cross-built for several Scala versions, leading to artifactIds yaidom_2.9.1, yaidom_2.9.2, etc..

Yaidom requires Java version 1.6 or later.
