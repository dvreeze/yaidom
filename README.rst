======
Yaidom
======

Yaidom is a uniform XML query API, written in Scala. Equally important, yaidom provides several specific-purpose DOM-like tree
implementations adhering to this XML query API. What's more, other implementations can easily be added.

Yaidom initially stood for yet another (Scala) immutable DOM-like API. Over time, the yaidom XML query API grew more important
than any single DOM-like tree implementation, whether immutable or mutable. Currently, the name yaidom reflects the ease with which
new DOM-like tree implementations can be added, while having them conform to the yaidom query API.

Why do we need yet another Scala (tree-oriented) XML library? After all, there are alternatives such as the standard Scala XML library.
Indeed, yaidom has several nice properties:

* the **uniform XML query API**, playing well with the **Scala Collections API** (and leveraging it internally)
* multiple (existing or future) **specific-purpose DOM-like tree implementations**, conforming to this XML query API
* among them, a nice immutable "default" DOM-like tree implementation
* precise and first-class **namespace support**
* **precision, clarity and minimality** in its genes, at the expense of some (but not much) verbosity and lack of XPath support
* acceptance of some XML realities, such as the peculiarities of "XML equality" and whitespace in XML
* and therefore no attempt to abstract **JAXP** away during parsing/serialization, but instead leveraging it (for parsing/serializing)
* easy **conversions** between several element implementations
* support for so-called **yaidom dialects**, using abstract query API traits for the backing elements and therefore supporting multiple XML backends
* a scope mainly limited to basic namespace-aware XML processing, and therefore not offering any XSD and DTD support

Usage
=====

Yaidom versions can be found in the Maven central repository. Assuming version 1.10.1, yaidom can be added as dependency
as follows (in an SBT or Maven build):

**SBT**::

    libraryDependencies += "eu.cdevreeze.yaidom" %%% "yaidom" % "1.10.1"

**Maven2**::

    <dependency>
      <groupId>eu.cdevreeze.yaidom</groupId>
      <artifactId>yaidom_2.13</artifactId>
      <version>1.10.1</version>
    </dependency>

Note that yaidom itself has a few dependencies, which will be transitive dependencies in projects that use yaidom.
Yaidom has been cross-built for several Scala versions, leading to artifactIds referring to different Scala (binary) versions.

One transitive dependency is Saxon-HE (9.9). If Saxon-EE is used in combination with yaidom, the Saxon-HE dependency must
be explicitly excluded!

Yaidom (1.8.X and later) requires Java version 1.8 or later!
