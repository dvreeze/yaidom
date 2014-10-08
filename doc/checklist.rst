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
* As for minimality, does the API have a good signal-to-noise ratio (or much "bang for the buck")?
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
* Production quality: thread-safe (if applicable), supporting large data sets, well performing, configurable, well documented, etc.

.. _`The trouble with types`: http://www.infoq.com/presentations/data-types-issues


Other checks
============

Other checks (w.r.t. documentation and bookkeeping) are:

* Road map, and sensible versioning strategy.
* Proper dependency management (e.g., supporting use of the library in an sbt or Maven build).
* Issue tracking.
* Test coverage.
* Builds against different JDKs, including at least one IBM JDK.
* Scala code can be used relatively easily from Java.
* All sources contain the license info, and there is a license file.
* All sources contain documentation, including the author.
* Change log up-to-date.
* README up-to-date, including version info.
* Running a diff against the preceding release (using diff or meld).

