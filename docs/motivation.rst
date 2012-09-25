==========
Motivation
==========

Introduction
============

XML processing in Java using commonly known APIs is known to be rather clunky. Scala's standard XML API therefore initially came as
a welcome change. After all, at least from my perspective, Scala is a far more expressive and productive language than Java,
so this should at least to some extent hold for Scala's XML API as well.

When doing non-trivial XML processing, Scala's XML API does not really shine, however. This was clearly shown
here_, by Daniel Spiewak. In his view, the standard Scala XML API suffers from issues w.r.t usability, reliability and
performance. These issues caused him to start working on an alternative Scala DOM-like XML API, called Anti-XML.
In all fairness, it must be said that Scala's standard XML API predates Scala 2.8, and therefore predates the Scala 2.8 Collections
API.

I am in need of a Scala DOM-like XML API, for a lot of non-trivial XML processing. Indeed Scala's own XML API does
not fit my needs for XML processing, but why not use Anti-XML instead? Why create my own alternative, named yaidom?

Yaidom was created because I feel strongly about some characteristics of XML processing, and I want a Scala DOM-like XML API:

* that respects those characteristics
* that is centered around immutable and therefore thread-safe node trees
* and that leverages Scala's Collections API, both from the outside and on the inside.

It is the first of these requirements that kept me from using Anti-XML.

The remainder of this document motivates in more detail why yaidom has been created, as such a Scala-esque DOM-like XML API.

.. _here: http://anti-xml.org/

Common themes
=============

Before discussing the characteristics of XML that motivate why yaidom was created, I would like to mention some common themes
of yaidom:

* Conciseness is good, but if a small sacrifice in conciseness means much clearer semantics, it may well be worth making the sacrifice
* In my opinion, it is not good for an API to suggest transparency where there hardly is any transparency (think "leaky abstractions")
* Power is good, but a good power-to-weight ratio may even be better

These common themes show that I may be willing to sacrifice a little bit of conciseness or power, if something else can be
gained from that.

Parsing and printing XML are not transparent
============================================

Most XML parsers can be configured in many ways, with good reason. Should the parser validate the XML or not?
Is inter-element whitespace significant or not? Should CDATA sections be combined with surrounding text or not?
Should the parser resolve entities, such as external DTDs, or not? Java XML parsers can be told about these and
many other parsing configuration options.

For DOM parsing this also means that the resulting DOM tree may depend a lot on the chosen configuration of the DOM parser.
Hence, in general there is no simple 1-to-1 correspondence between XML strings and DOM trees. Similar remarks hold the
other way around, when serializing DOM trees to XML strings.

So, XML parsing and printing are not transparent. Yet some XML libraries act like they are. Yaidom, on the other hand,
does not make any such suggestions:

* Yaidom has no ``toString`` method for elements that (supposedly transparently) converts the DOM-like tree to XML strings.
* Yaidom clearly exposes a JAXP DOM, SAX or StAX parser that can be configured in any way that they can be configured outside of yaidom
* Yaidom does not suggest to do a better job of parsing and printing XML than JAXP does, instead strongly suggesting that some knowledge of JAXP parser configuration is essential
* The yaidom node tree classes do not even live in the same package as the parsing and printing support (but yaidom is careful in keeping dependencies among packages unidirectional)

Therefore, parsing and printing XML are slightly more verbose in yaidom than in many other APIs, and I would argue that this
is a good thing.

No stable notion of equality for XML
====================================

Related to the previous issue is that there is no stable notion of equality for XML trees. Like mentioned above:

* There is in general no simple 1-to-1 correspondence between DOM trees and XML strings
* An XML document may depend on a lot of external context (DTDs, XSDs etc.)

This makes it very hard to define a stable notion of equality for XML. Yet Scala's standard XML library defines equality for
XML, without it being clear what it means exactly for 2 XML trees to be equal. Anti-XML defines XML elements as case classes,
suggesting a "true" notion of equality, based on the structure of the element alone. Moreover, in Anti-XML's case, namespace prefixes
are significant for equality comparisons, yet it is very common to consider 2 XML documents that only differ in namespace
prefixes to be equal.

Hence I do not want a DOM-like XML API to suggest in any way that there is a stable notion of equality for XML trees.
The Scales XML library, while not being a DOM-like API, indeed does require the user to do some work telling the application how to
compare 2 XML documents for equality. Also see the XMLUnit_ library, which also expects the user to take charge of equality comparisons
for XML. I think this is a good thing.

Yaidom does offer some notion of equality, but not for normal yaidom node trees. Yaidom has a separate node class hierarchy
that is meant for equality comparisons, and that contains only element and text nodes, and only namespace URIs instead of
namespace prefixes. The user of yaidom is responsible for converting a normal yaidom node tree to such a "resolved" node tree,
before making any equality comparisons.

The XML equality issues also make me a bit skeptical about pattern matching for XML.

.. _XMLUnit: http://xmlunit.sourceforge.net/

Qualified versus expanded names
===============================

The `XML Namespaces`_ specification clearly distinguishes between qualified names and expanded names. It is a very important
distinction:

* Qualified names are easy to use, but have no meaning without any context that binds the prefixes (if any)
* Expanded names have meaning on their own, but are not as easy to use as qualified names

Qualified names are resolved as expanded names by binding prefixes to namespace URIs.

Unfortunately, the distinction between qualified and expanded names is not clear in most XML APIs. Very often, only
qualified names are defined, and often (like in JAXP) a qualified name in the API is really an expanded name, but also to some
extent a qualified name, because it contains a prefix (which may be insignificant for equality comparisons on the QName).

Yaidom clearly distinguishes between the 2 kinds of names. It is always clear in yaidom which of the 2 are meant.
Just like qualified names, expanded names are first-class citizens in yaidom, so one could do very precise namespace-aware
querying in yaidom, without caring about the prefixes used in the XML document.

Another important distinction, at least in my opinion, is that between namespace (un)declarations and scopes (or: in-scope namespaces).
The latter map prefixes (or the empty string, for the default namespace) to namespace URIs.

This distinction is needed, because in yaidom immutable ("functional") Elems hold the scope (in-scope namespaces), but not
any namespace declarations, whereas ElemBuilders hold namespace declarations but no scopes. Again, immutable Elems must
on the one hand contain enough data in order to resolve qualified names, and on the other hand be useful building blocks
for larger Elem trees. Hence they know about in-scope namespaces (which they need to resolve qualified names), but not about
(own) namespace declarations. Being "functional", they also do not know about parent elements. This is all very different from
the mutable elements found in other XML libraries.

For Elem construction, it is not handy to pass the same Scope again and again while building the Elem tree. That is where
ElemBuilders come in: they keep namespace declarations but do not know any Scopes, so cannot resolve any qualified names.
That's just fine, because ElemBuilders are only builders of Elems. In other words, ElemBuilders postpone "scope passing"
until the last moment. Note that very often ElemBuilders are not even needed, if Elems are produced by parsing XML (using
one of yaidom's DocumentParsers).

In a complete Elem tree, we do know where the namespace declarations are, because we can "relativize" each Elem Scope against
the Scope of its parent, which produces namespace declarations. It turns out that Scopes and Declarations obey some interesting
properties, and that we can "calculate" with them. The latter helped a lot in keeping many classes in yaidom smaller and
simpler than otherwise would have been the case.

In the context of Anti-XML, see the following `Anti-XML issue`_. Yaidom's handling of namespaces and prefixes may not be
ideal (we still must keep "namespace context" around somehow), but yaidom clearly models the distinction between qualified names
and expanded names, and between namespace declarations and in-scope namespaces (and between ElemBuilders and Elems). The
model cannot get rid of all namespace clumsiness, but at least the model is clear and practical.

.. _`XML Namespaces`: http://www.w3.org/TR/REC-xml-names/
.. _`Anti-XML issue`: https://github.com/djspiewak/anti-xml/issues/78

The Node and NodeSeq issue
==========================

In Scala's standard XML library we can do very concise XPath-like quering, like:
``"foo" \ "bar" \ "baz"``

This concise syntax does not come for free. To blur the distinction between singleton node collections and single nodes,
the library has a very strange inheritance hierarchy for nodes, where ``Node`` extends ``NodeSeq`` which in turn extends
``Seq[Node]``

In `Working with Scala's XML support`_ Daniel Spiewak (before his work on Anti-XML) further explained the issue. Anti-XML
also offers a similar concise XPath-like syntax, but in a different way. It does require the user of Anti-XML to understand
some (Anti-XML) concepts that have no relation to the "domain of XML", such as ``Group``

Yaidom is less ambitious in this regard. In yaidom, the above XPath-like expression becomes the following somewhat more verbose expression:
``"foo" \ "bar" flatMap { _ \ "baz" }``

It could also be written using for-comprehensions, but, yes, this is more verbose than the XPath-like expression above.
Yet it is also very clear semantically what is returned:
``"foo" \ "bar"``
returns an ``immutable.IndexedSeq[Elem]`` and so does
``"foo" \ "bar" flatMap { _ \ "baz" }``

Hence it is trivial to understand the expression from a Collections point of view. In yaidom, a node is a node, and a collection
of nodes is a collection of nodes. That is very easy to understand, and in my opinion warrants a slight increase in verbosity.

.. _`Working with Scala's XML support`: http://www.codecommit.com/blog/scala/working-with-scalas-xml-support

The clarity of element-centric querying
=======================================

Talking about simple semantics, we can take this a bit further, and consider elements more central in queries than other kinds
of nodes. After all, whichever the configuration of the XML parser, it should always find the same element nodes, but that does not
necessarily hold for text nodes, comments etc.

It is surprising how easy it can be to create a powerful querying API, leveraging Scala's Collections API, in an element-only
universe (at least from the perspective of the API). Indeed, yaidom offers trait ``ElemLike`` which knows only about elements
(which is indeed the single type parameter of the trait, representing the captured element type itself). The trait turns a minimal
API (abstract methods such as ``allChildElems``) into a rich element-centric querying API.

Trait ``ElemLike`` could itself have been minimal. If it implemented only methods ``findAllElemsOrSelf`` and ``findTopmostElemsOrSelf``
(in terms of method ``allChildElems``), Scala's Collections API could do the rest when querying for elements. For ease of use,
and to a lesser extent for performance, the trait is a lot richer than that.

Personally I like this element-centric approach in the core yaidom querying API a lot. The power-to-weight ratio is excellent.
Of course querying for specific text nodes is somewhat more involved, but not that much more involved. (Trait ``ElemLike`` does not
know about text nodes, but class ``Elem`` into which the trait is mixed in does know about them.) Using immutable element trees and
eager evaluation, parent elements can not be queried for, but there are other means in yaidom to obtain element ancestors.

Leveraging Scala's Collections API, a trait like ``ElemLike`` as general element query API (used as mixin in multiple element classes)
was really a low hanging fruit.

Of course, in comparison XPath is a lot richer than the ElemLike API, but it is also quite different, because:

* XPath is more about "navigation" (in any direction, including up to ancestors) than "node set transformations"
* In XPath, the notion of "root" is somewhat vague
* XPath is not mainly about element nodes, but other kinds of nodes (including attribute nodes) as well
* XPath blurs the distinction between singleton node collections and the single nodes themselves
* There is a lot of (implicit) existential quantification in XPath
* XPath 2.0 even leverages the very complex XML Schema type system

The yaidom "query language" ``ElemLike`` is trivial in comparison, but still quite powerful for its size.

The semantics of queries in yaidom are very easy to understand, and very close to Scala's Collections API, and these are "traits" that
I value very much. It is not often that I long for the power of XPath (or even XQuery) instead of yaidom's ``ElemLike`` API.

No correctness at all costs
===========================

Yaidom does not try to achieve "correctness" at all costs. What is correctness anyway, if some parts of XML technology do not go
well with other ones? Case in point: DTDs and namespaces. Other case in point: different XML specs with different angles on what
is XML. The XML spec takes more of an "XML-as-text-obeying-XML-rules" approach, whereas the InfoSet spec takes more of an
"XML-as-node-tree" approach (like DOM).

Hence yaidom makes some pragmatics choices, such as:

* For ease of use, attributes in yaidom are not nodes
* Namespace declarations in yaidom are not attributes (avoiding the conceptual circularity between namespace declarations and attributes with namespaces)
* Namespace undeclarations are allowed in yaidom, even if the XML version is 1.0

No completeness at all costs
============================

Yaidom certainly does not try to achieve "completeness" at all costs. Whereas namespaces are first-class citizens in yaidom, DTDs are
not. Yaidom has no API for modelling DTDs. Of course the XML parser can still be configured to validate the XML against a DTD, or to
use a DTD for resolving some entity, but beyond that yaidom itself does not provide any support for DTDs. This is just one example
of yaidom deliberately sacrificing "completeness".

The need for good interop with JAXP
===================================

It was mentioned above that parsing and printing XML is not transparent, and that yaidom does not suggest to do a better job than
JAXP in that regard. On the contrary, yaidom requires the user to choose a DOM, SAX or StAX based XML parser or printer, and encourages
parser/printer configuration like you would typically do when using JAXP directly.

Inspired by the Spring framework "template" classes, yaidom does make the use of JAXP underneath a bit easier, yet without taking away
any control from the user.

Conclusion
==========

I wanted a Scala-esque DOM-like XML library, centered around immutable thread-safe nodes. I also wanted that library to be somewhat
less ambitious and more "pessimistic" than existing alternatives, such as the standard Scala XML library or Anti-XML. Moreover, I
wanted the library to be sufficiently ease to use for XML scripting tasks (powered by Scala Collections). Hence, I created yaidom.

By the way, in one way yaidom is pretty optimistic, namely the availability of (heap) memory. Maybe in Scala 2.10, with the help of
SIP-15 (value classes), yaidom can become more memory-efficient.

In summary, yaidom fits my XML processing needs better than the alternatives. That's why I created it. I would like it to be(come)
useful to others as well (and/or to at least have some influence on the future of XML processing in Scala, if I may dream about that).
