================
Some XML gotchas
================

Yaidom is rather conservative in that it does not try to abstract away JAXP, and
in that it does not want to deal with low level details of parsing/printing itself.

That is with good reason, because it is hard to do a better job than JAXP (bugs notwithstanding),
considering the many gotchas of XML technology.

Some areas in which XML can be quite complex or counter-intuitive are:

* Namespaces
* Entity resolution
* Whitespace handling
* XML as data description format
* XML Schema

There are more areas of XML that are full of surprises or complexities, such as XPointer,
XML catalogs etc., but these areas are not considered here.

In this document some of those XML surprises are described, and briefly it is described what
yaidom's approach is w.r.t. those issues, if applicable. Very often, yaidom tries to avoid the issue,
for example by leaving it to JAXP (or to JAXP and the yaidom user!) to deal with the issue.

Foremost yaidom does only one thing, and tries to do that well, namely represent parsed XML as
Scala-ish immutable thread-safe DOM-like trees, that can be queried well.

In any case, to use yaidom effectively, one must be familiar with JAXP, and with configuration of
JAXP parsers and transformers.

Namespaces
==========

XML namespaces come with many surprises, complexities and issues, such as the following:

* Simply put: DTDs do not understand XML namespaces. See for example `Why don't namespaces support DTDs?`_ or `XML namespaces and DTDs`_
* Confusion w.r.t. names (of elements and attributes), such as qualified names (QNames) and expanded names:

  * Each XML library seems to redefine/re-implement the notion of qualified names, often interpreting them as expanded names
  * Expanded names rarely explicitly occur in XML libraries, although they should be at least as prominent as qualified names
  * There is no official string representation of expanded names, although `James Clark`_ notation is often used in documentation
* Namespace URIs mostly look like URLs, but are just identifiers:

  * URLs suggest the possibility of looking something up on the web, but namespace URIs do not entail any such promise
  * URL comparison is not strictly string comparison, but namespace URI comparison is string comparison (be careful with trailing slashes, for example)
* Conceptual circularity: namespace declarations are considered to be attributes, but attribute names can themselves have a namespace, thus depending on namespace declarations

  * Namespace declarations are fortunately easy to recognize in XML, but mind the distinction between prefixed namespace declarations and default namespace declarations
* Prefixes themselves are considered to be insignificant, but be careful with DTD validation and some attribute values
* Unprefixed attributes, unlike unprefixed elements (when using a default namespace), are not in any namespace
* Some namespaces are reserved, namely the "xml" namespace (namespace URI: "http://www.w3.org/XML/1998/namespace") and the "xmlns" namespace (namespace URI: "http://www.w3.org/2000/xmlns/")
* Even text strings in XML can be subject to namespace interpretation (for example in XML Schemas), although that is the responsibility of the application

Looking at namespaces alone, it is already clear that reasoning about "equality" of XML documents (in isolation)
is very hard in general.

Yaidom's approach w.r.t. namespaces (and DTDs) can be summarized as follows:

* Yaidom does not consider DTDs first-class citizens (although the underlying JAXP parser may retrieve and use DTDs)
* Yaidom considers namespaces first-class citizens
* Yaidom clearly distinguishes among qualified names, expanded names, namespace scopes and namespace declarations (including undeclarations)
* Yaidom does not consider namespace declarations to be attributes!

.. _`Why don't namespaces support DTDs?`: http://www.oreillynet.com/xml/blog/2007/04/why_dont_namespaces_support_dt.html
.. _`XML namespaces and DTDs`: http://www.rpbourret.com/xml/NamespacesFAQ.htm#dtd
.. _`James Clark`: http://www.jclark.com/xml/xmlns.htm

Entity resolution
=================

In the well-known `W3C's Excessive DTD Traffic`_ blog, the W3C raised awareness of excessive traffic to W3C servers looking up
DTDs. It is interesting to read this blog, as well as the many reactions. Many programmers were not even aware that the XML parser(s)
used in their application code made all those HTTP requests. Yet XML parsers have good reasons to read DTDs. The well-known
reasons for that are entity definitions and validation against the DTD (be it with poor namespace support), but Elliotte Rusty Harold
also refers to default attribute values, including namespace declarations.

Yes, that's right, attribute values can be implicit in the XML document, and come from the DTD. I'd call that quite a design flaw in XML.

Still, DTD lookup could profit from the use of local caches. Transparent caching is hard, however. The W3C recommends the use of
XML catalogs, but XML catalogs have not caught on. Moreover, do we need XML catalogs and even standard API access to those XML catalogs
just for parsing the simplest of X(HT)ML documents? Sounds unreasonable to me.

If there is no clear robust and easy way out of this excessive DTD traffic situation, then maybe there is a fundamental underlying problem?

To me, it does not feel right that "during" schema/DTD validation the complete schema/DTD set is looked up and resolved.
Compare this to (relational) databases. Just imagine that during constraint checking ("schema validation") the constraints themselves
had to be looked up (on the internet) and assembled. For databases, that sounds absurd, but it is nevertheless to some extent a good analogy.
In all fairness, and in defense of XML, the design goals differ vastly between database and XML, of course.

Now add hardly supported XML standards such as XLink and XPointer to the mix, and entity resolution gets even more hairy.

The excessive DTD traffic problem is most pressing when using XML parsers for parsing HTML.

Yaidom limits itself to the following recommendations w.r.t. entity resolution:

* When parsing HTML, yaidom recommends the use of the TagSoup SAXParserFactory, which not only successfully parses the worst of HTML documents, but also does not bombard the W3C servers
* When parsing normal (well-formed) XML, consider using a dummy EntityResolver that does not resolve any entities (see scaladoc documentation), risking some loss of information (entities, default attribute values, etc.)

.. _`W3C's Excessive DTD Traffic`: http://www.w3.org/blog/systeam/2008/02/08/w3c_s_excessive_dtd_traffic/

Whitespace handling
===================

XML can be prettified by indentation, so by adding ("ignorable") whitespace. Yet how is an XML parser to know if the whitespace can
be ignored or not? If the SAX parser does not validate, it is hard to tell what this parser will do: pass this whitespace text to the
characters() method or to the ignorableWhitespace() method? See `Ignorable White Space`_.

Looking at whitespace handling alone, it is already clear that reasoning about "equality" of XML documents (in isolation)
is next to impossible in general.

Yaidom's approach w.r.t. whitespace handling is summarized as follows:

* Yaidom leaves this to JAXP
* Hence the user of yaidom is responsible for proper JAXP parser/transformer configuration
* Yet, inspired by "Spring templates", such parser/transformer configuration needs to be done only once

.. _`Ignorable White Space`: http://www.cafeconleche.org/books/xmljava/chapters/ch06s10.html

XML as data description format
==============================

As the successor to the supposedly even more complex SGML, XML can be document-oriented or data-oriented. So it can mix tags and text
freely, or it can limit the occurrence of text to the content of leaf element nodes only. Servicing those "2 worlds" must have implications
for (the complexity of) schema languages as well.

There are several degrees of freedom in how to represent data as XML, but this freedom does not necessarily help in better interpreting the data.
For one, there is the distinction between elements and attributes. When to use what?

Then there are empty tags and non-empty tags without any children. When to use what? It may matter, for example in the case of
XHTML where the script tag must be non-empty.

There is also the choice of leaving data out by either using the xsi:nil attribute, or by simply leaving the element out.

Thinking in terms of "programming language types", such as Maps, Lists, Sets etc. it is usually not clear from an XML document which is
which, without validating against a schema. In the XML document a parent element may have several children, but without consulting the
schema it is hard to tell if the order of child elements matters, how many of them may occur, etc. This does not make XML ideal for
representation of data.

Of course, there is not much yaidom can do here. Put simply, whatever the XML parser passed to yaidom is stored as yaidom Documents.

XML Schema
==========

In `W3C XML Schema: DOs and DON'Ts`_, Kohsuke Kawaguchi (of Hudson/Jenkins fame, among other things) illustrates the complexity
of XML Schema. Some (partly) disagree; see `W3C XML Schema Design Patterns: Avoiding Complexity`_. Both articles show one thing:
XML Schema is (very) complex.

Part of what makes XML Schema so complex is revealed in `MSL. A Model for W3C XML Schema`_, in particular Appendix A. For example,
restriction in XML Schema is not transitive, which is quite counter-intuitive. The set of rules defining restriction is of enormous
complexity, and according to the authors of the MSL paper ad-hoc as well. No wonder it is so hard to gain an in-depth understanding
of XML Schema.

Yaidom is unaware of schema types (or DTD types). Attribute values are simply strings in yaidom.

.. _`W3C XML Schema: DOs and DON'Ts`: http://www.kohsuke.org/xmlschema/XMLSchemaDOsAndDONTs.html
.. _`W3C XML Schema Design Patterns: Avoiding Complexity`: http://msdn.microsoft.com/en-us/library/aa468564.aspx
.. _`MSL. A Model for W3C XML Schema`: http://www.google.nl/url?sa=t&rct=j&q=xml%20schema%20type%20system%20wadler&source=web&cd=1&ved=0CDoQFjAA&url=http%3A%2F%2Fciteseerx.ist.psu.edu%2Fviewdoc%2Fdownload%3Fdoi%3D10.1.1.109.2857%26rep%3Drep1%26type%3Dpdf&ei=1wCCT-vXHcam8gPHu7CuBg&usg=AFQjCNGokq1mkcfWi9xHArf27Sm1x4fXvw

Conclusion
==========

Put very negatively, XML technology is an ongoing story of scope creep, technical debt, complexity, and excessive conceptual weight
(or, put differently, a very low power-to-weight ratio).

It is telling that it is extremely hard to come up with a solid notion of "equality" for XML documents (yes, I know, there is an
XPath function fn:deep-equal).

Fortunately, in practice most XML (out of our control) that we deal with is "reasonably sane". On the other hand, the more we
control the XML ourselves, the more we can keep it simple.

In any case, I prefer to leave many hairy details of dealing with XML to JAXP. That's why yaidom has a rather limited scope.
It tries to do one thing well, and that is representing XML DOM-like trees in such a way that they can be queried and manipulated
easily, in a thread-safe manner. To the yaidom user this means that JAXP knowledge is essential.
