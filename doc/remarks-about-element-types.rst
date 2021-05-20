
================================================
Remarks about the current element type in yaidom
================================================

The element query API of yaidom uses **type members** for the "current element" type in query API traits. Typically some form of F-bounded
polymorphism is used in such cases to restrict the "current element" type to, well, the "current element" type, to the extent
that this is possible.

Earlier yaidom versions used type parameters for the element type, and later yaidom versions switched to type members instead.
This turned out to reduce the amount of boilerplate in practice, for example when using arbitrary element implementations in
"yaidom dialects". Indeed the use of type members instead of type parameters turned out to be a good choice. Morever, we can still
express pretty much the same with type members and type parameters (although type members cannot be used in self types).

After all:

* Type ``ElemApi`` (using a type member for the element type) is essentially type ``ElemApi[_]`` (using a type parameter for the element type)
* Type ``ElemApi { type ThisElem = E }`` (using a type member for the element type) is essentially type ``ElemApi[E]`` (using a type parameter for the element type)

What if we had used type classes instead of type members or type parameters to model F-bounded polymorphism? In our case, it would not have helped much:

* How do type classes for element query function APIs help for native yaidom elements? What do these native yaidom element types without the query API look like?
* How do these type classes help model node type hierarchies, supporting other kinds of nodes than just elements?
* Even when exposing these type classes through generic OO APIs, we would still want to write specific optimized OO element APIs
* After all, OO APIs can contain private redundant state, for optimal performance
* In other words, custom OO element APIs can **encapsulate** performance improvements, without introducing any breaking changes

Given that yaidom is an important low-level dependency used in several critical production applications, the possibility to
improve performance without breaking the API is quite essential. Note that with type classes we could still develop a custom
SaxonElem implementation delegating to a custom Saxon-oriented type class instance, but what's the point of (query API) type classes then?

So, using type members for the "current element type" in the yaidom element query API is still a good choice. No need for
type parameters, nor for type classes. The purely abstract element query API traits even relax type safety to the point where
the ThisElem type member is only restricted to be a "raw" sub-type of the enclosing trait. Only element implementations "fix" this type member
to the concrete element type itself.

Still, the need for release 1.10.2 of yaidom shows that not all is well. Implementation traits like ``ScopedElemLike`` have
become part of the public API of custom element implementations (often outside of yaidom, sometimes in "yaidom dialects").
This potentially makes it hard to optimize such custom element implementations in a non-breaking way.

This is also an issue in a subtle way, when we look with javap into the class files of custom element implementations.
Recall that the class files still contain generics in the method signatures (of methods like filterElemsOrSelf), and that erasure
only affects the byte code, not the method declarations (to some extent). What we can see in class files are method signatures like:

* ``def filterElemsOrSelf(p: ScopedElemLike => Boolean): immutable.IndexedSeq[ScopedElemLike]``

Optimizing the custom element implementation by removing (and replacing) the ``ScopedElemLike`` mix-in would thus be a
breaking change. In TQA (which uses yaidom), for class ``TaxonomyElem``, I would rather see a more stable "javap" method signature like:

* ``def filterElemsOrSelf(p: TaxonomyElem => Boolean): immutable.IndexedSeq[TaxonomyElem]``

There is a lesson to be learnt from this when further evolving the yaidom query API. Query API methods containing the ThisElem
element type in the method signature should be implemented only in the concrete element class. For yaidom dialects this would
be the common dialect element super-class. This may be at odds with the DRY principle, but API stability is more important here.

Alternatively these query API methods are implemented in a query API trait, but overridden in the concrete element implementation,
calling the super-type version of the method, but adapting the method signature through the override.

So, evolving yaidom further should ideally be done as follows with respect to the "current element type" in the query API:

* Keep using type members for the "current element type" (no type parameters, no type classes)
* Restrict these type members to be sub-types of the enclosing query API trait, but do not add any more complex type constraints (not even self types)
* Keep using the purely abstract element query API traits, with the above-mentioned relaxed type constraints on the element type member
* Yet **remove and no longer use the partial implementation traits**, especially for methods containing the element type in the signature
* Or: **keep partial implementation traits**, but encourage **overriding of most methods for better method signatures**

Then we can achieve the following:

* Easy (but sometimes cumbersome) implementation of different yaidom elements, including yaidom dialects (mostly outside of yaidom), like is the case now
* Ability to **tweak performance** of (custom) element implementations, without affecting **API stability**

It is the latter bullet point that needs more attention from now on.

There is a moral to the story, especially when modelling complex typing scenarios like F-bounded polymorphism. Sometimes it
makes sense to look at the **class files generated by the Scala compiler**, to get a feeling for what the Scala compiler sees
when loading the class file in the absence of the corresponding source file.

Related to this is that it makes sense to use **MiMa** on a regular basis in order to prevent any unwanted binary incompatibilities.
For more background on binary (and source) compatibility, see `binary-compatibility`_.

.. _`binary-compatibility`: https://docs.scala-lang.org/overviews/core/binary-compatibility-for-library-authors.html
