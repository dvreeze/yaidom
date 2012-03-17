=====================
Development checklist
=====================

During development, I check the code against the following best practices.

Completeness of the source files
================================

Checking the completeness of the source files:

* License file
* License info in every source file
* Documentation in every source file
* Up-to-date README.rst
* When tagging, up-to-date change log
* Use package objects, and document them (as overview of the package/namespace)

API Design, according to Joshua Bloch
=====================================

The well-known presentation by Joshua Bloch on API Design can be found here_. I think it is a good thing to check the code
against Bloch's recommendations on API Design, and violate them only with good reason.

Bloch characterizes a good API as follows:

* Easy to learn
* Easy to use
* Hard to misuse
* It should lead to client code that is use to use and easy to maintain
* Sufficiently powerful
* Easy to extend
* Appropriate to the audience

(Personal note: if the API can be used easily while experimenting with it in the Scala REPL, without looking at the documentation,
it may well be a good API.)

.. _here: http://www.infoq.com/presentations/effective-api-design

Process of API design, according to Joshua Bloch
------------------------------------------------

* Write to the API early and often (and to SPI as well, if applicable)

General principles
------------------

* The API must do one thing, and do it well. Good names drive development
* When in doubt, leave it out;. Look for a good power-to-weight ratio
* Implementation details should not leak into the API
* Minimize accessibility of everything
* Names matter, and code should read like prose
* Documentation matters
* Document the state space carefully (which becomes less of an issue with functional programming)
* Consider performance consequences of API design decisions (but do not optimize prematurely)
* The API must coexist peacefully with the platform (JVM, JDK, Scala etc.)

Class design
------------

* Minimize mutability (which is obviously the case in a functional style of programming)
* Subclass only where it makes sense (IS-A relationship must hold), consider prohibiting inheritance where not designing for inheritance

Method design
-------------

* Do not make the client do what the module could do
* Do not violate the principle of least astonishment
* Fail fast
* Provide programmatic access to all data available in String form
* Overload with care
* Use appropriate parameter and return types
* Use consistent parameter ordering across methods
* Avoid long parameter lists
* Avoid return values that demand exceptional processing

Exception design
----------------

* Throw exceptions to indicate exceptional conditions
* Favor unchecked exceptions
* Include failure-capture information in exceptions

Some Scala best practices
=========================

* Code should be as functional as possible

  * Prefer immutability
  * Prefer side-effect free functions (or keep side-effects localized inside functions)
  * Think in expressions rather than statements
  * Clojure inventor Rich Hickey even calls "mutable" `the new spaghetti code`_. I tend to agree
  
* Document immutability, e.g. with marker interface Immutable, and do not violate any promise of immutability
* Prefer Option over null

.. _`the new spaghetti code`: http://clojure.org/state

Some Maven best practices
=========================

* Write pom.xml as documented in the `Maven Repository Usage Guide`_
* Do not use other Maven repositories (than Maven Central), whenever possible
* There must be clear unidirectional dependencies between Maven modules

.. _`Maven Repository Usage Guide`: https://docs.sonatype.org/display/Repository/Sonatype+OSS+Maven+Repository+Usage+Guide

Some Spring best practices
==========================

* Dependencies between packages (and inside packages) must be unidirectional, and as obvious as possible
* Emulate Spring "templates" where appropriate, leaving resource management to "template", without taking away control from the user
of the API
* Programming against contracts is (still) good, especially at layer boundaries (this is also good for testability)

Some other good practices
=========================

Note that some of the following good practices apply more to applications than to libraries:

* This should be obvious: software should just work

  * A web application against a database should respect that database
  * A (multi-user) web application should indeed work as a multi-user app
  * An XML parser should be configurable
  * A data processing library should be able to handle larger data volumes

* Seriously consider not using any abstractions that leak too much, no matter how popular they might be
* When using frameworks, still remain in the driver seat

  * Take charge of the architecture
  * Choose what to use, how to use it, what not to use
  
* Consider maintenance costs (of the software in production) when choosing an architecture

  * How hard are version migrations?
  * How hard is it to reason about the state of a stopped system?
  
* When mixing Scala and Java, be explicit at boundaries between idiomatic Scala and idiomatic Java
* Choose layers wisely (if applicable), typically based on abstraction levels
* Less is often more

  * Do you need multiple Maven modules?
  * Do you really need to implement Serializable (sometimes a hidden web framework cost)?
