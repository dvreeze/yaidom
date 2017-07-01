=================
Yaidom check list
=================


Introduction
============

Before releasing any new yaidom version, some checks are performed. These checks are summed up in this document.
Two kinds of checks are performed: code checks and other checks.


Code checks
===========

The (somewhat more functional) library code checks are as follows:

* Is the scope of the library clear? E.g., namespaces are within the scope of the library, but schema types are not.
* Who is the intended audience of the library?
* Is the API clear, correct, minimal, and the opposite of "random"? See `The trouble with types`_. Yet note that correctness is not an absolute notion.
* As for clarity and correctness, how well can you learn the domain (of XML, e.g. namespaces) by using the API and its vocabulary?
* As for clarity and correctness, how well can you reason about the code in the first place? (Immutability helps in this regard.)
* As for clarity, correctness and minimality, is the API (partly) a (mathematical) theory, and also documented as such? That could provide an extra safety net.
* As for clarity and minimality, is it easy to get started with the library without documentation, for basic usage? For example, by experimenting in the Scala REPL.
* As for minimality, does the API have a good "signal-to-noise ratio" (or much "bang for the buck", or a small "conceptual surface area")? Joshua Bloch: "When in doubt, leave it out".
* As for minimality, how "orthogonal" is the API? E.g., are query API traits like LEGO, or do they sit in a fixed inheritance hierarchy?
* Given the chosen scope, is the API complete enough? E.g., check yaidom against Saxon and XQuery.
* On the other hand, still strive for minimality, and keep in mind that completeness may mean good interop with other APIs (JAXP, wrapped DOM-like APIs etc.).
* Does the API use names consistently?
* Is the programming style consistent? E.g., a moderate Scala OO-functional style, preferring immutability, but not too alien to Java programmers.
* Is more advanced usage supported? E.g., tuning the library for decreased memory usage.
* How extensible is the library?

Other code checks are:

* Scala packages contain package objects, containing documentation relevant to the package/namespace.
* Only clear unidirectional dependencies among packages (and classes).
* Proper deprecation of changes that are not backward compatible.
* Avoidance of deprecated or soon to be deprecated language features, such as procedure syntax in Scala.
* Avoidance of problematic language features or language feature combinations, such as the use of val or var in a trait.
* Avoidance of problematic dependencies; the fewer JARs the API depends on, the better; the more common they are, the better.
* Production quality: thread-safe (if applicable), supporting large data sets, well performing, configurable, well documented, needs no network connection, etc.

.. _`The trouble with types`: http://www.infoq.com/presentations/data-types-issues


Other checks
============

Other checks (w.r.t. documentation and bookkeeping) are:

* Road map.
* Sensible versioning strategy.
* Proper dependency management (e.g., supporting use of the library in an sbt or Maven build).
* Issue tracking.
* Check the code coverage.
* Run scalastyle (and/or similar tools) against the code base, and check the results.
* Builds against different JDKs, including at least one IBM JDK.
* Scala code can be used relatively easily from Java (distinguish between Java < 8 and >= 8).
* All sources contain the license info, and there is a license file.
* All sources contain documentation, including the author.
* Change log up-to-date.
* README up-to-date, including version info.
* No sufficiently urgent TODOs are left.
* Running a diff against the preceding release (using diff or meld).


Notes on genericity
===================

How generic should a library be? Yaidom is by design generic in its support for "arbitrary" DOM-like trees, but other than that
yaidom is quite conservative w.r.t. genericity. For example:

* The collections are all immutable IndexedSeq collections.
* Yaidom's query API offers mostly "element filtering" methods (no map-like operations, no need for CanBuildFrom, and elements are more central than nodes in general).
* The query API mainly offers only 3 "axes" for element nodes (child elements, descendant elements, and descendant-or-self elements).

Yaidom also uses no "generic tree API". After all, each domain may have somewhat different needs from such a generic tree API.
For example, in the domain of XML there are different kinds of nodes, and element nodes have a very central position.
Moreover, for XML DOM-like trees it is natural to assume finite (and typically even limited) tree depths, whereas in
many other domains the tree query API should let the user choose a maximum tree depth. Another example is the choice
of how to represent immutable trees that still know about their context (ancestor nodes). Yaidom made a specific choice
in this regard ("indexed" elements).

Yaidom's conservative approach towards genericity for example means that streams are not first-class citizens in the
query API. So in theory yaidom is not a powerful API (w.r.t. genericity). In practice things are not that bad:

* Yaidom is simple, easy to understand, and has a small conceptual surface area.
* While yaidom keeps evolving, this small conceptual surface area is retained as much as possible.
* It is easy to reason about performance of the library.
* It is easy to maintain the library, staying within a low "complexity budget", as long as yaidom sticks to its philosophy (see above).
* With a little effort on the part of the user of the API, a lot is still possible (such as streaming scenarios).
* Yaidom interoperates well with other powerful libraries, such as the Scala Collections API and JAXP for bootstrapping.

Maybe not generalizing too much is indeed not such a bad thing. For example, maybe Scala's Collections API
took genericity a bit too far, offering much of the same API for immutable and mutable collections (and for strictly
and lazily evaluated collections).


Notes on versioning
===================

Libraries should have a clear versioning strategy. Ideally there would be one versioning strategy adopted by all software
projects. Many developers consider `Semantic Versioning 2.0.0`_ to be that universal strategy.

Yaidom has not adopted Semantic Versioning. After all, not all incompatible API changes are the same. I would like to
think of yaidom 2 as a complete rethink of the library, so for that I would like to use versions 2.X.Y. Currently (2015) yaidom
1.X.Y is evolving, sometimes introducing breaking changes. Minor version bumps correspond to "themes", or to deprecations
or removal of deprecations:

* Version 1.1 improves orthogonality (much of it under the hood for typical uses of the library)
* Version 1.2 deprecates some code
* Version 1.3 removes these deprecations
* Version 1.4 makes yaidom still meaner and cleaner w.r.t. indexed elements and 2 main query APIs: "clark" and "scoped"
* Version 1.5 improves functional updates (and makes them more consistent with the rest of the API)
* Version 1.6 improves the quality of APIs and implementations

During this evolution it is tried to make yaidom meaner and cleaner. Much of it is discovered, rather than designed up-front.
Discovering the "core of yaidom" requires experimentation, lots of it. For example, the functional update support of
version 1.5 required many committed (!) attempts to have it evolve into a worthy companion to the query and transformation APIs.
The constant need for experimentation makes evolving yaidom without sufficiently frequent backwards-incompatible changes
quite hard to achieve. Of course, in spite of the experimentation, once a new version is released, the impact should be
clear and no larger than needed. Deprecation may help, but not always.

Fortunately, the public API of yaidom 1.X is getting more and more stable. The query API and transformation API
have been reasonably stable from the user point of view for quite some time. The update API is also getting more stable.
Yet it does not follow Semantic Versioning.

The world according to `Semantic Versioning 2.0.0`_ does not really exist. Upgrading dependencies on other
libraries still requires conscious decisions, and can not be left to tools alone. These libraries should at least have a clear
change log, and some versioning strategy that users come to rely on.

For a critique of semantic versioning, see `Why Semantic Versioning Isn't`_. After all, if I would like to speak of
yaidom 2.0 at some point, why do I have to call it yaidom 48.0 instead? That does not convey any semantics at all.
The impact of the version is invisible when using just another major version bump.

Not all breaking changes are equal. If in yaidom a query method is moved from one query API trait to another, it is
likely that yaidom users will not notice this in the use of the API, because using the query API traits directly is
not a typical use of the library. Still, it would be a breaking change, requiring recompilation of code using yaidom.
With Scala it is very easy to cause such "innocent" breaking changes. Deprecation does not help either in this case.
I would like the user to know, via the change log, but I would not like to increase the major version number in this case.

What's more, sometimes dependencies get very messy. As a well-known example, consider the `Xerces version hell`_.

Yaidom does adopt some underlying ideas of Semantic Versioning, such as:

* For each version, have and communicate an explicit public API of the library
* Have and communicate an explicit versioning strategy

Yaidom's versioning strategy is as follows:

* Yaidom bumps major versions only for entire rethinks of the library.
* Yaidom bumps minor versions where semantic versioning requires major version bumps.
* Yaidom bumps patch versions mostly for backwards-compatible additions and bug fixes, although currently this backwards-compatibility is not guaranteed.

The change log should make the impact of yaidom version bumps clear, however. Typical non-backwards-compatible changes
in patch versions fix problems introduced in the preceding version. This means that a change in yaidom becomes more
stable if it survives one patch version bump (unless overridden by the next minor version bump).

.. _`Semantic Versioning 2.0.0`: http://semver.org/
.. _`Why Semantic Versioning Isn't`: https://gist.github.com/jashkenas/cbd2b088e20279ae2c8e
.. _`Xerces version hell`: http://stackoverflow.com/questions/11677572/dealing-with-xerces-hell-in-java-maven
