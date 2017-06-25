========
ROAD MAP
========


The vision is to grow yaidom into a generic element querying, update/transformation and element creation
API. This API is essentially EName-based rather than QName-based, even in its element creation API.
In other words, like always, namespaces are central in yaidom.

The library should be like an hour glass, with multiple element implementations at the bottom, but also
supporting multiple XML dialects at the top (abstracting from the element implementations). At the center
of the hour glass is the small generic element querying, update/transformation and element creation API.

This is the vision of yaidom 2.X. Yaidom 1.X already comes very close to this vision in its query API,
but much less so in its update/transformation API, let alone in its element creation API.

Note that standards like XQuery and XSLT miss the XML dialect support that yaidom offers, because they
are not (libraries in) OO languages (such as Scala). XQuery and XSLT use custom XPath functions (such as custom
XBRL XPath functions) instead. Future versions of yaidom should also integrate well with XPath (whether
in yaidom itself or not). For example, with Saxon JAXP XPath evaluators and Saxon-backed yaidom wrappers
around XPath evaluation results and XPath context items, yaidom could even play a role in implementing
custom XPath functions.


Towards version 1.6
===================

Version 1.6.0 must further simplify yaidom by removing "eager" indexed elements (replacing them by the "lazy" ones).
The API that had been used for "eager" indexed elements will then be used for the remaining ("lazy") indexed elements.

Querying for the ancestry of an element (using the HasParentApi query API trait) must retain the element type
(for performance, this is not realistic with "eager" indexed elements, so they must been removed).

Several experiments have been done for this version, but they have not made it into any release.

For example, functional APIs (taking an element as first explicit parameter) have been tried for each corresponding
query API trait. The OO query API traits are easier to use, so this experiment has been abandoned. So much for the
use of type classes for yaidom query APIs.

As another example, new element implementations have been tried, as an alternative to indexed elements. These elements
would know the ancestry but not the siblings, for example. It was decided that this alternative element implementation
offered too little bang for the buck, so this experiment has been abandoned too. Besides, yaidom should become leaner
and meaner, not the other way around.

A general element creation API is to be postponed to a later release.


Towards version 1.5
===================

Version 1.5.0 must have well-defined functional update support. Its functional update API must be consistent, and
analogous to the transformation API and the core element query API. It must also be useful, and more efficient and
powerful than the transformation API, while being almost as easy to use. An update API for indexed elements should also
be considered.


Towards version 1.4
===================

Version 1.3.6 is already meaner and cleaner than version 1.3. For example, there is no dependency anymore on
the Apache Commons library. Performance has also improved.

Version 1.4.0 tries to be even meaner and cleaner, while not sacrificing any performance. For example:

* The "indexed" elements are generic, and can contain any backing ("scoped") element, not just "simple" elements.
* While the query API traits are as much as possible orthogonal, there are 2 multiple-trait combinations that "stand out": ClarkElemApi and ScopedElemApi
* ClarkElemApi is the query API for James Clark's minimal labeled element tree abstraction (implemented as "resolved" elements)
* ScopedElemApi is the query API for practical element tree abstractions, as ClarkElemApi, but adding QNames and Scopes
* XML dialect support should typically take "scoped" elements (or their "indexed" variants)
* "Document-aware" elements can probably be phased out, and XML Base support can become a utility on top of "indexed" elements
* Optional document URIs could then be stored in indexed elements, to make the context of an element complete
* Package dependencies are cleaner than before; e.g. "simple" elements depend on "resolved" elements and not vice versa
* More consistency in method naming (especially for underlying elements), deprecating old names

Despite the attempts to make yaidom still meaner and cleaner, XML declarations have been added to document implementations.


Towards version 1.3
===================

After releasing version 1.0, the author of yaidom tried to write a blog post, showing the purpose and strength of
the different query API traits. That turned out to be difficult, because the query API traits are not sufficiently
**orthogonal**, and they depend on inheritance too much. For example, in yaidom 1.0 the inheritance relation from the
1.0 PathAwareElemApi to the 1.0 NavigableElemApi is too arbitrary.

In particular, the (1.0) ParentElemApi trait is by far the most important one, but the (1.0) ElemApi trait offers nothing fundamental
other than some "HasENameApi" capability, that can be combined with (1.0) ParentElemApi to replace the (1.0) ElemApi.
(It makes sense to rename ParentElemApi to ElemApi, thus effectively removing the old ElemApi API.) Most element
implementations can then be seen as the combination of ElemApi (after the renaming) and several "capabilities" (such as
knowing about ENames, about QNames, about text content, etc.), at least for the query API (as opposed to update/transformation API).

Fortunately, most element implementations make sense as they are (for the most part), and it is just the underlying
query API traits that need an overhaul. Given that the query API traits rarely occur in yaidom client code, the impact
of such an overhaul can be rather limited. Moreover, this internal overhaul is not fundamental enough to regard this
as part of "yaidom 2". Rather it is an evolution of "yaidom 1".

While cleaning up the query API traits, it also makes sense to split the root package of yaidom into 3 sub-packages:
"core" (with concepts such as QName, EName, Scope etc.), "queryapi" (with the query API traits) and "simple" (with
the default (simple) Elem implementation, along with ElemBuilder). This would impact a lot of import statements in yaidom
client code, and can be facilitated by aliases in the root yaidom package that can gradually be deprecated and removed.

It is also expected that the PathAwareElemApi is removed entirely. Using ElemApi (after renaming) on indexed elements
is far more powerful, since the predicate can filter both on element and path.

This leads to the road map below (shortly after 1.0).


1.1
===

Changes:

* Splitting the root package into 3 sub-packages (as mentioned above)
* Hiding this split as much as possible to the yaidom user, by aliases in the root package
* Rework the query API traits, which is indeed a breaking change (but easing the pain by offering an implicit conversion from ENames to predicates)
* Yet leave the net query API offered by the different element implementations as much as possible the same
* Only PathAwareElemApi disappears completely

Yaidom users upgrading from 1.0 to 1.1 are affected wherever query API traits are explicitly used in code.
In particular, the 1.0 ElemApi companion object no longer contains element predicates (they are now in the HasENameApi companion object).
Given that query API traits are typically not used that much in yaidom client code, the upgrade should not take too long
(and is low risk, with help of the compiler).

The loss of PathAwareElemApi (for simple elements) can be compensated by the use of indexed elements.


1.2
===

Changes:

* Deprecate the aliases in the root package, giving yaidom users the time to upgrade to version 1.3

Yaidom users upgrading from 1.1 to 1.2 have the time to fix deprecation warnings (for the aliases that no longer
exist in version 1.3).


1.3
===

Changes:

* Remove the deprecated aliases in the root package, resulting in a meaner and cleaner yaidom

Yaidom users upgrading from 1.2 to 1.3 had the time to fix the deprecation warnings, so this upgrade should be easy.

After version 1.3, and leading up to version 1.4, only performance improvements (and insignificant API changes) are
in scope. For example, construction of ENames and QNames is too expensive due to validations that typically are not
needed.

