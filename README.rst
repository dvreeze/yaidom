=========================
Yet Another Immutable DOM
=========================

Yaidom (yet another immutable DOM) is a Scala DOM-like API. It represents XML as in-memory trees, offering multiple DOM-like
tree implementations.

Why do we need yet another Scala DOM-like API? After all, there are alternatives such as the standard Scala XML library and Anti-XML.
Yaidom grew out of a desire to:

* offer precise and first-class **namespace support**
* support **multiple DOM-like** tree implementations that make sense in different situations
* support **extensibility**, w.r.t. new DOM-like tree implementations
* make the default DOM-like element implementation (deeply) **immutable**
* offer only one **uniform element query API**, in spite of these different tree implementations
* leverage the **Scala Collections API** in the element query API, and forget about any XPath support
* accept some XML realities, like the vagueness surrounding the notion of "XML equality"
* leverage **JAXP** for parsing and serialization, without offering any broken XML parser/serializer abstractions
* limit the scope of the API to XML and namespaces, thus ignoring (schema) types etc.
* offer a **precise, clear and minimal** XML API overall

Usage
=====

Yaidom versions can be found in the Maven central repository. Assuming version 0.8.0, yaidom can be added as dependency
as follows (in an SBT or Maven build):

**SBT**::

    libraryDependencies += "eu.cdevreeze.yaidom" %% "yaidom" % "0.8.0"

**Maven2**::

    <dependency>
      <groupId>eu.cdevreeze.yaidom</groupId>
      <artifactId>yaidom_2.10</artifactId>
      <version>0.8.0</version>
    </dependency>

Note that yaidom itself has a few dependencies, which will be transitive dependencies in projects that use yaidom.
Yaidom has been cross-built for several Scala versions, leading to artifactIds referring to different Scala (binary) versions.

Yaidom requires Java version 1.6 or later.
