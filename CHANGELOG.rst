=========
CHANGELOG
=========


1.9.0
=====

Yaidom 1.9.0 is a performance-oriented release. The most important changes are:

* Method ``SaxonElem.path`` now more efficiently uses Saxon's ``AbsolutePath`` class underneath, thus improving its performance
* This led to the introduction of class ``AbsolutePath`` to yaidom, which is quite useful in its own right
* Related to that are the added conversions between ENames and namespace-URI qualified EQ-names
* Addition of ENameProviders and QNameProviders backed by a Caffeine cache
* Further improvements in the ``SaxonElem`` implementation (for performance and clarity)
* Enhancements in the Java 8 wrapper API
* Removal of deprecated classes and methods, which account for most of the breaking changes

To reduce memory footprint in applications using yaidom, it is important to activate the new EName and QName
providers backed by a Caffeine cache. See the scripts in the yaidom project for how to use them. Of course,
a cache is a rather heavy tool for implementing name pooling, but the slight loss in execution speed is more than
compensated by the reduced memory footprint.

So what is the performance story for yaidom? See also Li Haoyi's article on Benchmarking Scala Collections
for some background. This article implies that future versions of yaidom that target Scala 2.13 and later can likely
have much better performance by exploiting the immutable ``ArraySeq`` collection instead of ``Vector`` collections
which are now used all over the place inside yaidom (for good reasons, though). It is important that yaidom can
keep its immutability and thread-safety promises with regard to the native element implementations (although
a Scala Vector or List is only thread-safe when "published safely").

Still, yaidom query performance on the JVM is quite good at the moment, but mind the following:

* When using the element query API for querying descendant elements, query performance is quite good
* This holds for most element implementations, especially for the Saxon wrappers and the 3 native element implementations
* When querying for ancestor elements and/or absolute paths, it depends on the element implementation how expensive these queries are
* The Saxon wrappers seem to be doing quite well when querying for ancestors, and reasonably well when querying for absolute paths
* When working in-memory with complete XML documents over 100 MB, only the Saxon wrapper element implementations are useful in practice
* The transformation/update APIs have not been battle-tested to the extent that the query API has
* When working with large (and/or very many) XML documents, take care not to lean too much on potentially slow "reverse axis" (or path) queries

Besides the breaking changes due to removed deprecated code, there are hardly any breaking changes for applications
that do not create their own element implementations, because they are not affected by newly added query API methods.
The only breaking changes for such applications are the nodeInfo2EName and NodeInfo2QName methods in the SaxonNode companion object.

Breaking changes compared to version 1.8.1 (in SBT, run: yaidomJVM/*:mimaReportBinaryIssues):

* method apply(eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem in object eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem's type is different in current version, where it is (eu.cdevreeze.yaidom.resolved.Elem)eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem instead of (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem.apply")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#XsdElem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$XsdElem$")
* object eu.cdevreeze.yaidom.utils.SimpleElemEditor#DefaultPrefixGenerator does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.SimpleElemEditor$DefaultPrefixGenerator$")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#GlobalAttributeDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$GlobalAttributeDeclaration$")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#LocalElementDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$LocalElementDeclaration$")
* object eu.cdevreeze.yaidom.utils.ResolvedElemEditor does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.ResolvedElemEditor$")
* class eu.cdevreeze.yaidom.utils.ResolvedElemEditor does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.ResolvedElemEditor")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#ElementReference does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$ElementReference$")
* object eu.cdevreeze.yaidom.utils.EditableResolvedElem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.EditableResolvedElem$")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#GlobalElementDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$GlobalElementDeclaration")
* class eu.cdevreeze.yaidom.utils.ENameProviderUtils does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.ENameProviderUtils")
* object eu.cdevreeze.yaidom.utils.XmlSchemas does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$")
* class eu.cdevreeze.yaidom.utils.EditableResolvedElem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.EditableResolvedElem")
* class eu.cdevreeze.yaidom.utils.SimpleElemEditor does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.SimpleElemEditor")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#GlobalAttributeDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$GlobalAttributeDeclaration")
* interface eu.cdevreeze.yaidom.utils.XmlSchemas#XsdElemFactory does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$XsdElemFactory")
* class eu.cdevreeze.yaidom.utils.XmlSchemas does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#LocalAttributeDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$LocalAttributeDeclaration")
* class eu.cdevreeze.yaidom.utils.EditableSimpleElem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.EditableSimpleElem")
* object eu.cdevreeze.yaidom.utils.QNameProviderUtils does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.QNameProviderUtils$")
* class eu.cdevreeze.yaidom.utils.QNameProviderUtils does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.QNameProviderUtils")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#LocalElementDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$LocalElementDeclaration")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#LocalAttributeDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$LocalAttributeDeclaration$")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#SchemaRoot does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$SchemaRoot")
* interface eu.cdevreeze.yaidom.utils.ClarkElemEditor does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.ClarkElemEditor")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#XsdElem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$XsdElem")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#ElementReference does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$ElementReference")
* object eu.cdevreeze.yaidom.utils.ENameProviderUtils does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.ENameProviderUtils$")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#AttributeReference does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$AttributeReference$")
* class eu.cdevreeze.yaidom.utils.XmlSchemas#AttributeReference does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$AttributeReference")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#SchemaRoot does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$SchemaRoot$")
* interface eu.cdevreeze.yaidom.utils.EditableClarkElem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.EditableClarkElem")
* object eu.cdevreeze.yaidom.utils.XmlSchemas#GlobalElementDeclaration does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.XmlSchemas$GlobalElementDeclaration$")
* object eu.cdevreeze.yaidom.utils.SimpleElemEditor does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.utils.SimpleElemEditor$")
* abstract method absolutePath()eu.cdevreeze.yaidom.core.AbsolutePath in interface eu.cdevreeze.yaidom.queryapi.IndexedClarkElemApi is present only in current version
  filter with: ProblemFilters.exclude[ReversedMissingMethodProblem]("eu.cdevreeze.yaidom.queryapi.IndexedClarkElemApi.absolutePath")
* abstract method nodeKind()eu.cdevreeze.yaidom.queryapi.Nodes#NodeKind in interface eu.cdevreeze.yaidom.queryapi.Nodes#Node is present only in current version
  filter with: ProblemFilters.exclude[ReversedMissingMethodProblem]("eu.cdevreeze.yaidom.queryapi.Nodes#Node.nodeKind")
* method nodeInfo2EName(net.sf.saxon.om.NodeInfo)eu.cdevreeze.yaidom.core.EName in object eu.cdevreeze.yaidom.saxon.SaxonNode does not have a correspondent in current version
  filter with: ProblemFilters.exclude[DirectMissingMethodProblem]("eu.cdevreeze.yaidom.saxon.SaxonNode.nodeInfo2EName")
* method nodeInfo2QName(net.sf.saxon.om.NodeInfo)eu.cdevreeze.yaidom.core.QName in object eu.cdevreeze.yaidom.saxon.SaxonNode does not have a correspondent in current version
  filter with: ProblemFilters.exclude[DirectMissingMethodProblem]("eu.cdevreeze.yaidom.saxon.SaxonNode.nodeInfo2QName")
* deprecated method apply(eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.resolved.Elem in object eu.cdevreeze.yaidom.resolved.Elem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[DirectMissingMethodProblem]("eu.cdevreeze.yaidom.resolved.Elem.apply")


1.8.1
=====

Yaidom 1.8.1 is almost the same as version 1.8.0. It only updated some dependencies, and simplified conversions
to resolved elements in test code. There are no breaking changes.


1.8.0
=====

Yaidom 1.8.0 is almost the same as 1.8.0-M4. It is largely the same as 1.7.1, except for the following (main) changes:

* Added main query API traits ``BackingNodes.Elem``, ``ScopedNodes.Elem`` and ``ClarkNodes.Elem``

  * The 3 main query API abstractions to be used by element implementations are ``BackingNodes.Elem``, ``ScopedNodes.Elem`` and ``ClarkNodes.Elem``
  * This is also true for "yaidom dialects"
  * These traits mix in the new trait ``HasChildNodesApi``, promising a method to get all child nodes (not only element nodes)
  * See the explanation of these traits in the release notes of version 1.8.0-M4

* Improved conversions to simple and resolved elements, and made them more generic

  * These conversions work for any element implementation that uses the main query API traits mentioned above
  * See the release notes of version 1.8.0-M4

* Improved element creation

  * Yaidom resolved elements are not only useful for equality tests, but also for ad-hoc element creation
  * See the release notes of version 1.8.0-M4

* Yaidom 1.8.0 dropped support for Java 6 and 7
* Saxon wrapper elements

  * It has been copied from TQA, where it will no longer live
  * It requires Saxon 9.8 or 9.7, and works for Saxon-HE, Saxon-PE and Saxon-EE
  * It has good query performance, and is quite memory-efficient, when using the default Saxon tiny tree implementation
  * On the JVM, the Saxon wrapper elements are the best yaidom element implementation available
  * See the release notes of version 1.8.0-M3

* An XPath evaluation API has been added

  * It has been inspired by the JAXP XPath API, but it is more Scala-friendly, more type-safe, and more yaidom-friendly
  * It is not as complete as the JAXP standard XPath API, because it does not yet model functions and variables
  * There is a Saxon JAXP backed implementation of this API (JVM-only)
  * See the release notes of version 1.8.0-M3

* Removed ``ResolvedNodes`` object
* Deprecated some code, especially in the utils package
* Also deprecated method ``resolved.Elem.apply``, introducing method ``resolved.Elem.from`` in its place

This brings yaidom even closer to its "hour glass" vision than versions 1.7.X. The addition of yaidom Saxon wrappers is
a very important one. Without it, the portfolio of yaidom element implementations (on the JVM) would be a lot more limited.
At the other end of the "hour glass", the new main query API traits help a lot in defining "yaidom XML dialects" and in
abstracting over backing elements. The improved conversions to simple and resolved elements also increase yaidom's power
at very low "conceptual costs".

Breaking changes compared to version 1.7.1 (in SBT, run: yaidomJVM/*:mimaReportBinaryIssues):

* the type hierarchy of interface eu.cdevreeze.yaidom.simple.CanBeDocumentChild is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.simple.CanBeDocumentChild")
* the type hierarchy of class eu.cdevreeze.yaidom.simple.Comment is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.simple.Comment")
* the type hierarchy of class eu.cdevreeze.yaidom.simple.Text is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Text,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.simple.Text")
* the type hierarchy of class eu.cdevreeze.yaidom.simple.EntityRef is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.simple.EntityRef")
* the type hierarchy of interface eu.cdevreeze.yaidom.simple.Node is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.simple.Node")
* the type hierarchy of class eu.cdevreeze.yaidom.simple.ProcessingInstruction is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.simple.ProcessingInstruction")
* the type hierarchy of class eu.cdevreeze.yaidom.simple.Elem is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Elem,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.simple.Elem")
* method apply(eu.cdevreeze.yaidom.resolved.ResolvedNodes#Elem)eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem in object eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem in current version does not have a correspondent with same parameter signature among (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem, (eu.cdevreeze.yaidom.resolved.Elem)eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.java8.resolvedelem.ResolvedElem.apply")
* the type hierarchy of class eu.cdevreeze.yaidom.scalaxml.ScalaXmlCData is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Text,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlCData")
* the type hierarchy of class eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Elem,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlElem")
* the type hierarchy of interface eu.cdevreeze.yaidom.scalaxml.CanBeScalaXmlDocumentChild is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.CanBeScalaXmlDocumentChild")
* the type hierarchy of class eu.cdevreeze.yaidom.scalaxml.ScalaXmlAtom is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlAtom")
* the type hierarchy of class eu.cdevreeze.yaidom.scalaxml.ScalaXmlProcessingInstruction is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlProcessingInstruction")
* the type hierarchy of class eu.cdevreeze.yaidom.scalaxml.ScalaXmlComment is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlComment")
* the type hierarchy of interface eu.cdevreeze.yaidom.scalaxml.ScalaXmlNode is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlNode")
* the type hierarchy of class eu.cdevreeze.yaidom.scalaxml.ScalaXmlEntityRef is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlEntityRef")
* the type hierarchy of class eu.cdevreeze.yaidom.scalaxml.ScalaXmlText is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Text,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.scalaxml.ScalaXmlText")
* method apply(eu.cdevreeze.yaidom.resolved.ResolvedNodes#Text)eu.cdevreeze.yaidom.resolved.Text in object eu.cdevreeze.yaidom.resolved.Text in current version does not have a correspondent with same parameter signature among (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Text)eu.cdevreeze.yaidom.resolved.Text, (java.lang.String)eu.cdevreeze.yaidom.resolved.Text
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.resolved.Text.apply")
* method apply(eu.cdevreeze.yaidom.resolved.ResolvedNodes#Node)eu.cdevreeze.yaidom.resolved.Node in object eu.cdevreeze.yaidom.resolved.Node does not have a correspondent in current version
  filter with: ProblemFilters.exclude[DirectMissingMethodProblem]("eu.cdevreeze.yaidom.resolved.Node.apply")
* interface eu.cdevreeze.yaidom.resolved.ResolvedNodes#Elem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.resolved.ResolvedNodes$Elem")
* class eu.cdevreeze.yaidom.resolved.ResolvedNodes does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.resolved.ResolvedNodes")
* method apply(eu.cdevreeze.yaidom.resolved.ResolvedNodes#Elem)eu.cdevreeze.yaidom.resolved.Elem in object eu.cdevreeze.yaidom.resolved.Elem's type is different in current version, where it is (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.resolved.Elem instead of (eu.cdevreeze.yaidom.resolved.ResolvedNodes#Elem)eu.cdevreeze.yaidom.resolved.Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.resolved.Elem.apply")
* interface eu.cdevreeze.yaidom.resolved.ResolvedNodes#Text does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.resolved.ResolvedNodes$Text")
* object eu.cdevreeze.yaidom.resolved.ResolvedNodes does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.resolved.ResolvedNodes$")
* the type hierarchy of class eu.cdevreeze.yaidom.resolved.Elem is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Elem,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.resolved.Elem")
* the type hierarchy of class eu.cdevreeze.yaidom.resolved.Text is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Text,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.resolved.Text")
* interface eu.cdevreeze.yaidom.resolved.ResolvedNodes#Node does not have a correspondent in current version
  filter with: ProblemFilters.exclude[MissingClassProblem]("eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node")
* the type hierarchy of interface eu.cdevreeze.yaidom.resolved.Node is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.resolved.Node")
* method apply(scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkElemApi,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in current version does not have a correspondent with same parameter signature among (scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.apply")
* method apply(java.net.URI,eu.cdevreeze.yaidom.queryapi.ClarkElemApi,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in current version does not have a correspondent with same parameter signature among (scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.apply")
* method apply(eu.cdevreeze.yaidom.queryapi.ClarkElemApi,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in current version does not have a correspondent with same parameter signature among (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.apply")
* method apply(scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkElemApi)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in current version does not have a correspondent with same parameter signature among (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.apply")
* method apply(java.net.URI,eu.cdevreeze.yaidom.queryapi.ClarkElemApi)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in current version does not have a correspondent with same parameter signature among (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem, (scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.apply")
* method apply(eu.cdevreeze.yaidom.queryapi.ClarkElemApi)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem's type is different in current version, where it is (eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem instead of (eu.cdevreeze.yaidom.queryapi.ClarkElemApi)eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.apply")
* method getChildren(eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem)scala.collection.immutable.IndexedSeq in object eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[DirectMissingMethodProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.getChildren")
* method apply(scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedElemApi,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in current version does not have a correspondent with same parameter signature among (java.net.URI,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.apply")
* method apply(java.net.URI,eu.cdevreeze.yaidom.queryapi.ScopedElemApi,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in current version does not have a correspondent with same parameter signature among (java.net.URI,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.apply")
* method apply(eu.cdevreeze.yaidom.queryapi.ScopedElemApi,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in current version does not have a correspondent with same parameter signature among (scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.apply")
* method apply(scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedElemApi)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in current version does not have a correspondent with same parameter signature among (scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.apply")
* method apply(java.net.URI,eu.cdevreeze.yaidom.queryapi.ScopedElemApi)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in current version does not have a correspondent with same parameter signature among (scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (java.net.URI,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem, (eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.apply")
* method apply(eu.cdevreeze.yaidom.queryapi.ScopedElemApi)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem in object eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem's type is different in current version, where it is (eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem instead of (eu.cdevreeze.yaidom.queryapi.ScopedElemApi)eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.apply")
* method getChildren(eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem)scala.collection.immutable.IndexedSeq in object eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem does not have a correspondent in current version
  filter with: ProblemFilters.exclude[DirectMissingMethodProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.getChildren")
* method this(scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedElemApi,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ScopedElemApi)Unit in class eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem's type is different in current version, where it is (scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ScopedNodes#Elem)Unit instead of (scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ScopedElemApi,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ScopedElemApi)Unit
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedScopedNode#Elem.this")
* method this(scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkElemApi,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ClarkElemApi)Unit in class eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem's type is different in current version, where it is (scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)Unit instead of (scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkElemApi,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ClarkElemApi)Unit
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.IndexedClarkNode#Elem.this")
* method underlyingRootElem()eu.cdevreeze.yaidom.queryapi.ClarkElemApi in class eu.cdevreeze.yaidom.indexed.AbstractIndexedClarkElem has a different result type in current version, where it is eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem rather than eu.cdevreeze.yaidom.queryapi.ClarkElemApi
  filter with: ProblemFilters.exclude[IncompatibleResultTypeProblem]("eu.cdevreeze.yaidom.indexed.AbstractIndexedClarkElem.underlyingRootElem")
* method underlyingElem()eu.cdevreeze.yaidom.queryapi.ClarkElemApi in class eu.cdevreeze.yaidom.indexed.AbstractIndexedClarkElem has a different result type in current version, where it is eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem rather than eu.cdevreeze.yaidom.queryapi.ClarkElemApi
  filter with: ProblemFilters.exclude[IncompatibleResultTypeProblem]("eu.cdevreeze.yaidom.indexed.AbstractIndexedClarkElem.underlyingElem")
* method this(scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkElemApi,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ClarkElemApi)Unit in class eu.cdevreeze.yaidom.indexed.AbstractIndexedClarkElem's type is different in current version, where it is (scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ClarkNodes#Elem)Unit instead of (scala.Option,scala.Option,eu.cdevreeze.yaidom.queryapi.ClarkElemApi,eu.cdevreeze.yaidom.core.Path,eu.cdevreeze.yaidom.queryapi.ClarkElemApi)Unit
  filter with: ProblemFilters.exclude[IncompatibleMethTypeProblem]("eu.cdevreeze.yaidom.indexed.AbstractIndexedClarkElem.this")
* abstract method children()scala.collection.immutable.IndexedSeq in interface eu.cdevreeze.yaidom.queryapi.HasChildNodesApi is inherited by class AbstractIndexedClarkElem in current version.
  filter with: ProblemFilters.exclude[InheritedNewAbstractMethodProblem]("eu.cdevreeze.yaidom.queryapi.HasChildNodesApi.children")
* the type hierarchy of class eu.cdevreeze.yaidom.dom.DomElem is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Elem,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.dom.DomElem")
* the type hierarchy of class eu.cdevreeze.yaidom.dom.DomEntityRef is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.dom.DomEntityRef")
* the type hierarchy of interface eu.cdevreeze.yaidom.dom.CanBeDomDocumentChild is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.dom.CanBeDomDocumentChild")
* the type hierarchy of interface eu.cdevreeze.yaidom.dom.DomNode is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.dom.DomNode")
* the type hierarchy of class eu.cdevreeze.yaidom.dom.DomComment is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.dom.DomComment")
* the type hierarchy of class eu.cdevreeze.yaidom.dom.DomProcessingInstruction is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.dom.DomProcessingInstruction")
* the type hierarchy of class eu.cdevreeze.yaidom.dom.DomText is different in current version. Missing types {eu.cdevreeze.yaidom.resolved.ResolvedNodes$Text,eu.cdevreeze.yaidom.resolved.ResolvedNodes$Node}
  filter with: ProblemFilters.exclude[MissingTypesProblem]("eu.cdevreeze.yaidom.dom.DomText")


1.8.0-M4
========

Milestone 4 of yaidom 1.8.0 brings yaidom even closer to its "hour glass" vision. The abstract query API
mainly exposes 3 query API "flavors", and all element implementations fall in one of these 3 categories.
One of these flavors is ``BackingNodes.Elem``, and it is the abstraction used for backing elements in
yaidom XML dialect support. Implementations of this query API are indexed elements and Saxon wrapper elements.

The main changes in version 1.8.0-M4 (compared with milestone 3) are:

* Replaced ``BackingElemNodeApi`` by ``BackingNodes.Elem`` etc.

  * The 3 main query API abstractions to be used by element implementations are ``BackingNodes.Elem``, ``ScopedNodes.Elem`` and ``ClarkNodes.Elem``
  * "Backing" elements inherit from "scoped" elements, who inherit from "Clark" elements
  * Each element implementation now directly inherits from one of these 3 abstractions
  * Element implementations that extend ``BackingNodes.Elem`` must extend the other ``BackingNodes`` node types for non-element nodes, etc.
  * These 3 new main abstractions give clarity to yaidom users, but also make conversions like the ones below feasible
  * Direct ``ClarkNodes.Elem`` implementations include "resolved" elements; they know about ENames but not about QNames
  * Direct ``ScopedNodes.Elem`` implementations include "simple" elements; they know about QNames but not about their ancestor nodes
  * ``BackingNodes.Elem`` implementations include Saxon wrappers and native indexed elements; they know about ancestor nodes, base URI etc.
  * The abstraction used by yaidom XML dialects for the backing elements (e.g. in the TQA project) is ``BackingNodes.Elem``

* Improved conversions to simple and resolved elements, and made them more generic

  * Any ``ScopedNodes.Elem`` can be converted to a simple element
  * Any ``ClarkNodes.Elem`` can be converted to a simple element, given a Scope without default namespace
  * Any ``ClarkNodes.Elem`` can be converted to a resolved element
  * These conversion methods are all called ``from`` (and the ``apply`` conversion method for resolved elements has been deprecated)
  * Note how these conversions do not complicate dependencies among packages, since these conversions only depend on the queryapi package
  * This improved element conversion story is useful for the TQA project in its support for programmatic taxonomy creation

* Improved element creation

  * Yaidom resolved elements are not only useful for equality tests, but also for ad-hoc element creation
  * After all, while creating resolved element trees, one does not have to worry about namespace prefixes
  * Resolved elements now also have some methods for adding/deleting/filtering attributes
  * The resolved Node companion object now extends the new trait ``ElemCreationApi``
  * See above for how resolved elements can easily be converted to simple elements, provided we have a suitable Scope
  * A new ``utils.ClarkNode.Elem`` class has been added; as opposed to resolved nodes, it knows about other nodes than elements and text
  * This improved element creation story is useful for the TQA project in its support for programmatic taxonomy creation

* Deprecated some code, mainly in the ``utils`` package
* Added ``Scope`` methods ``makeInvertible`` and ``resolveQName``


1.8.0-M3
========

The main changes in version 1.8.0-M3 (compared with milestone 2) are:

* A yaidom Saxon wrapper implementation of `BackingElemNodeApi` has been added

  * It has been copied from TQA, where it will no longer live
  * It requires Saxon 9.8 or 9.7, and works for Saxon-HE, Saxon-PE and Saxon-EE
  * It has good query performance, and is quite memory-efficient, when using the default Saxon tiny tree implementation
  * If future Saxon major versions require breaking changes in the yaidom wrappers, we may have to deploy separate artifacts for them
  * On the other hand, the Saxon wrappers are overall the best and most powerful yaidom implementations, so they should be included in yaidom

* An XPath evaluation API has been added

  * It has been inspired by the JAXP XPath API, but it is more Scala-friendly, more type-safe, and more yaidom-friendly
  * It is not as complete as the JAXP standard XPath API, because it does not yet model functions and variables
  * There is a Saxon JAXP backed implementation of this API (JVM-only)
  * Therefore we can use XPath 3.1 (also standard functions, even JSON support), and use yaidom queries on XPath evaluation results, etc.
  * There is also an implementation for JS DOM  (JS-only), but that one only offers basic XPath 1.0 support
  * It may seem that expanding yaidom with (error-prone) XPath support may make yaidom less "stable"
  * On the other hand, nothing else in yaidom depends on its XPath support, and the API is rather clean
  * Moreover, this opens up so many possibilities (especially on the JVM), mixing yaidom and XPath queries at will
  * It also fits in the overall vision of yaidom as an "hour glass" easily integrating with XPath

* The Scala XML wrappers are now common code shared by JVM and JS (although not all of Scala XML runs on JS runtimes)
* Upgraded many dependencies, given that Java 6 and 7 are no longer supported


1.8.0-M2
========

The main changes in version 1.8.0-M2 (compared with milestone 1) are:

* Removed ``ResolvedNodes`` object
* Java 6 and 7 as targets no longer supported


1.8.0-M1
========

The 1.8.X versions make the "core" element abstractions aware of child nodes (and therefore different
kinds of nodes). The main changes in version 1.8.0-M1 are:

* Added query API trait ``HasChildNodesApi``, containing method ``children``

  * This query API trait extends ``AnyElemNodeApi``, and therefore it is abstract in the node type (as well as the element type)
  * There are sub-traits (top to bottom) ``ClarkElemNodeApi``, ``ScopedElemNodeApi`` and ``BackingElemNodeApi``
  * For example, ``ClarkElemNodeApi`` extends ``ClarkElemApi`` and ``HasChildNodesApi``
  * Traits ``ClarkElemNodeApi``, ``ScopedElemNodeApi`` and ``BackingElemNodeApi`` are now the important element abstractions
  * Trait ``ResolvedNodes.Elem`` now extends ``ClarkElemNodeApi``, therefore having a (better defined) ``children`` method
  * All yaidom element implementations now mix in (at least) ``ResolvedNodes.Elem``
  * Moreover, most yaidom element implementations mix in ``ScopedElemNodeApi``, and some even ``BackingElemNodeApi``
  * "Yaidom dialects" should now use ``BackingElemNodeApi`` as general element node abstraction
  * Trait ``BackingDocumentApi`` now has a ``BackingElemNodeApi`` document element
  * "Yaidom dialects" should now use this ``BackingDocumentApi`` as general document abstraction

* Class ``JsDomDocument`` now mixes in trait ``BackingDocumentApi``


1.7.1
=====

Same as 1.7.0, except for the following changes:

* Added ``BackingDocumentApi`` (containing a ``BackingElemApi`` document element)
* Upgraded Scala.js to version 0.6.22


1.7.0
=====

This yaidom version is about bringing yaidom to Scala.js as second target platform. This fits very well
in the vision of yaidom as "hour glass", with support for multiple XML dialects on one side and support for
multiple element implementations on the other side. It also validates the overall design of yaidom, because
without disciplined management of package dependencies in yaidom it would have been very hard to target
Scala.js. Fortunately, the yaidom code shared by the JVM and JS platforms includes the core and queryapi
packages, as well as the native simple, resolved and indexed element implementations.

Although milestone release 1.7.0-M1 primarily tried to improve on support for element transformations and
updates, the vision for versions 1.7.X has changed to support for Scala.js, as described above.

Version 1.7.0 contains several breaking changes, but most of them of a rather trivial nature. Migrating from
versions 1.6.X to 1.7.0 should therefore be rather easy, but does require recompilation of code using
yaidom, maybe with a few trivial code changes here and there.

The main changes in this version (compared to 1.6.4) are:

* Support for Scala.js, sharing most of yaidom for both platforms (JVM and JS); see version 1.7.0-M2
* Targeting Scala.js, a JS DOM wrapper implementation; see versions 1.7.0-M2, 1.7.0-M7 and 1.7.0-M8
* JAXP-dependent methods in classes ``EName`` and ``Scope`` have been moved to JVM-dependent utilities

This version is much like version 1.7.0-M8, but the JS DOM wrapper implementation has slightly improved since then.


1.7.0-M8
========

This milestone release further improves on the support for yaidom in the browser:

* The yaidom JS DOM wrapper has value equality and much better performance
* Upgraded Scala.js to version 0.6.21
* Added test (using Scala.meta) to help prevent linking errors in Scala.js


1.7.0-M7
========

This milestone release improves on the support for yaidom in the browser:

* The yaidom JS DOM wrapper now offers the ``BackingElemApi`` interface, making it useful in projects like TQA
* Breaking changes: JVM-specific methods in classes ``EName`` and ``Scope`` have been moved to separate utilities
* Fixed release bug: artifacts for Scala 2.13.0-M2 are no longer empty
* Breaking changes: pruned some code, like some ``ENameProvider`` and ``QNameProvider`` implementations
* Also removed or ignored some test code that made Travis builds fail on out-of-memory errors.


1.7.0-M6
========

Same as 1.7.0-M5, except for a small change in the build.sbt, trying to please Nexus.


1.7.0-M5
========

Same as 1.7.0-M4, except for a major overhaul of the build.sbt. Let's hope third time is a charm.


1.7.0-M4
========

Same as 1.7.0-M3, except for some changes in build.sbt, in yet another attempt to publish artifacts to Nexus.


1.7.0-M3
========

Same as 1.7.0-M2, except for some changes in build.sbt, in an attempt to publish artifacts to Nexus.


1.7.0-M2
========

Version 1.7.0-M2 is the second milestone release for yaidom 1.7.0. The theme of yaidom 1.7.X is no longer
improved update/transformation support, but support for "yaidom in the browser", through Scala.js.

This milestone release uses Scala.js. The yaidom code base is split among a shared part, a jvm part and
a js part (respecting the main differences between JVMs and JavaScript runtimes):

* The shared code contains the core and queryapi packages, as well as the native yaidom simple, indexed and resolved element implementations.
* The jvm code contains DOM and Scala XML wrappers, as well as conversions and document parsers and printers (and Java 8 bridges).
* The js code contains JS DOM wrappers and related conversions.


1.7.0-M1
========

Version 1.7.0-M1 is the first milestone release for yaidom 1.7.0. It tries to bring the vision of yaidom
as generic XML query, update/transformation and creation API one step closer. It does so by offering
functional update/transformation support for indexed elements, which by their nature know their ancestry.
It turns out that the known properties about yaidom functional updates and transformations still hold
for elements that know their ancestry.

There are breaking changes in this release, but with re-compilation not too many changes should be needed
in application code using yaidom.

The main changes are (this was before version 1.6.3):

* Introduction of ``ElemTransformationApi`` and ``ElemUpdateApi`` traits, for "arbitrary elements"

  * This is an API of functions on elements, and not an OO API like ``TransformableElemApi``
  * Corresponding ``ElemTransformationLike`` and ``ElemUpdateLike`` partial implementations
  * Indexed elements (with simple underlying elements) now supporting those traits
  * Some properties about ``ElemTransformationApi`` in terms of ``ElemUpdateApi`` made explicit (and proven)

* Faster ``simple.Elem.toString``
* ``NamespaceUtils`` more generic in the query part
* Some refactorings leading to cleaner and more idiomatic Scala code


1.6.4
=====

Version 1.6.4 fixes a bug introduced in version 1.6.3. The DocumentParserUsingStax of version 1.6.3
created an XMLEventReader from a SAXSource, which may not work in some XML stacks.

There are no breaking changes.


1.6.3
=====

Version 1.6.3 improves on version 1.6.2, and incorporates the functional element transformation and
update APIs of version 1.7.0-M1, but leaves out their implementations (for indexed elements).
The reason is that we are not close enough to version 1.7.0, but we want to have a release with other
improvements, while the 4 new API traits might just as well be included now.

This release "should" be a drop-in replacement for version 1.6.2, without the need for recompilation.
Only code directly inheriting from AbstractDocumentParser would cause the need for recompilation, so
make sure this is not the case before using version 1.6.3 without recompilation.

There is another catch, though, and that is that deprecated methods have been removed.

The main changes are:

* Introduction of ``ElemTransformationApi`` and ``ElemUpdateApi`` traits (see version 1.7.0-M1), without using them
* Faster ``simple.Elem.toString``
* Document parsers can now take a SAX InputSource
* ``NamespaceUtils`` more generic in the query part
* Some refactorings leading to cleaner and more idiomatic Scala code


1.6.2
=====

Version 1.6.2 replaced the methods for canonical XPath expressions by ``Path`` methods that replace QNames by
ENames (in James Clark notation) in those "canonical XPaths". The old methods are still available, but have been deprecated.

The main changes are:

* Introduced ``Path`` methods ``toResolvedCanonicalXPath`` and ``fromResolvedCanonicalXPath``, deprecating the old ones
* Added method ``nonEmpty`` to ``Path``, ``PathBuilder``, ``Scope`` and ``Declarations``
* Added methods ``namespaces`` and ``filterNamespaces`` to ``Scope``

Version 1.6.2 has no breaking changes compared to version 1.6.1 and 1.6.0, except that the "canonical
XPath" methods have been deprecated. If calls to those methods are replaced, version 1.6.2 can otherwise be used
as if it were version 1.6.0.

Note that version 1.6.2 is even more true to its vision of preferring ENames to QNames than previous versions.


1.6.1
=====

Version 1.6.1 speeds up base URI computation for indexed elements, by storing the optional parent base URI.
This is important in an XBRL context, where the base URI is used extensively, for example when resolving XLink arcs.
This change is a non-breaking change.


1.6.0
=====

Version 1.6.0 is the same as version 1.6.0-M7. Version 1.6.0 is a release that aims at improving the quality of the
library, compared to versions 1.5.X, while trying to make yaidom still leaner and meaner.

IMPORTANT NOTE: Yaidom 1.6.0 for Scala 2.12 has an erroneous optional dependency on scala-java8-compat_2.11!

Version 1.6.0 has many breaking changes compared to 1.5.1, but code using yaidom is relatively easy to adapt in order
to make it compile and work with yaidom 1.6.0. 

The main changes compared to versions 1.5.X are as follows:

* The query API traits now use type members instead of type parameters

  * This removes some clutter in the query API traits, because unlike type parameters, type members do not have to be repeated everywhere
  * This is also logical in that type parameters are just alternative syntax for type members (in the new Scala compiler dotty)
  * The partial implementation traits in the query API (XXXLike) use F-bounded polymorphism with self types in the same way as before, but now encoded with type members
  * The purely abstract traits in the query API (XXXApi) are now less restrictive, however, in that the type member (for "this" element) is only restricted to a sub-type of the "raw" query API trait
  * This makes it easy to use purely abstract query API traits as "interfaces" abstracting over concrete element implementations
  * A new purely abstract trait ``BackingElemApi`` (combining several purely abstract query API traits) does just that, and may be used to abstract over concrete backing elements of XML dialects that themselves offer the yaidom query API, but more type-safe
  * Like before, the solution easily scales to more query API traits, but now encoded with type members (so the solution is still simple enough)
  * Moving a code base from yaidom 1.5.X to 1.6.0 is easy w.r.t. mixing in the query API traits in element implementations (see the yaidom ones)
  * Code that only uses the query API (as opposed to creating new element implementations) is hardly affected by the move to yaidom 1.6.0
  
* The "eager" indexed elements have been removed

  * They were expensive to (recursively) create, but very fast to query, because the child elements were stored as fields
  * Yet for performance reasons they required to hop to the underlying element type when querying for the ancestry, which is not nice from an API point of view
  * Now the "lazy" indexed elements are the only ones remaining (a 'Clark' and a 'Scoped' variant)
  * They are slightly slower in querying, but fast to create, fast in querying the ancestry, fast to (functionally) update, and more friendly from an API point of view
  * For a user migrating to yaidom 1.6.0, re-compilation is almost enough when using the "new" indexed elements
  * Yet keep in mind that XML Base computation is surely less efficient than it was for the "old" indexed elements (it used to be stored in the element)
  
* All element implementations, including the indexed ones, now have a Node super-type

  * All element implementations reside in a Node hierarchy with specific sub-types for the abstract ``Nodes.Node`` type and its "own" type hierarchy
  * Hence an indexed Document no longer needs to hold comments and processing instructions from another Node hierarchy (such as simple nodes)
  
* Improved whitespace handling and DOM tree printing; see the release notes of version 1.6.0-M7
* Improved support for StAX-based streaming; now many streaming scenarios are possible where only parts of the XML are turned into trees in memory; see the release notes of version 1.6.0-M7
* Many bug fixes, including the ones documented as yaidom issues (also see above)
* Cross-compiling for Scala 2.12 as well (and dropping support for Scala 2.10)
* Experimental support for Java 8 interop, including a mirrored query API using Java 8 Streams (this part of yaidom requires Java 8)


1.6.0-M7
=======

Milestone 7 contains the following improvements over the previous milestone:

* Improved whitespace handling and DOM tree printing

  * Refactored and simplified the prettifying implementation (in ``PrettyPrinting``)
  * As a result, improved performance of ``simple.Elem.prettify`` (and applied a small bug fix, of a bug that hardly manifests itself)
  * Improved performance of ``simple.Elem.toString`` (which prints the DOM tree), thus hopefully fixing issue yaidom-0001
  * The result of DOM tree printing is again valid Scala code itself for creating the DOM tree as NodeBuilder
  * Refactored methods like ``removeAllInterElementWhitespace``, ``coalesceAllAdjacentText`` etc., and made the API slightly more general
  * As a result, fixed issue yaidom-0004
  * Added tests for whitespace handling and DOM tree printing

* Cross-compilation for Scala 2.12.0, and upgraded some dependencies (including the Saxon-HE test dependency)
* Improved support for StAX-based streaming, while allowing for some breaking changes

  * Fixed the test case that no longer worked for Scala 2.12, and should not have worked in the first place, because of repeated ``buffered`` calls on the same ``Iterator``
  * Refactored ``StaxEventsToYaidomConversions``, using new classes ``AncestryPath`` and ``EventWithAncestry``
  * Added some interesting tests to ``StreamingLargeXmlTest``, showing XBRL streaming, cheap XBRL entrypoint detection, and even traversal of entire wikipedia abstracts file (the latter test is ignored)


1.6.0-M6
========

Milestone 6 of version 1.6.0 offers improved experimental support for Java 8, compared to the previous milestone.
The streaming query API is now an OO API instead of a functional API.


1.6.0-M5
========

Milestone 5 of version 1.6.0 offers some experimental support for Java 8, making yaidom easy to use in Java 8. To that end,
yaidom offers a Java 8 facade to its query API, using the Java 8 Stream and Optional APIs.


1.6.0-M4
========

Milestone 4 of 1.6.0 fixes compilation errors against Scala 2.12.0-RC1. The query API traits with partial implementations
had to be more strict in the constraints on type member ThisElem, analogous to the constraints on the corresponding
type parameters in yaidom before version 1.6.X. The gain is in the fact that type member ThisElemApi (or ThisDocApi)
is no longer needed; type member ThisElem (or ThisDoc) suffices.

So the net result is that the query API traits differ from the ones in yaidom before version 1.6.X in the following way:

* Type members are used instead of type parameters, thus improving readability and reducing clutter
* The purely abstract query API traits have simple non-restrictive type constraints on the type members (not involving the "self" type)
* This makes query API (combination) trait BackingElemApi an easy to use abstraction over multiple element implementations
* The partial implementation query API traits have type constraints analogous to the ones in yaidom before version 1.6.X
* The resulting query API is consistent and simple, like before, but better supporting abstractions over element implementations

Other changes are:

* Scala 2.10 is no longer supported. Instead, cross-compilation against Scala 2.12.0-RC1 is done.
* Scalatest has been upgraded to version 3.0.0
* One streaming test case is ignored, because of infinite loops (whatever the cause) in Scala 2.12.0-RC1. This must be analyzed.


1.6.0-M3
========

Milestone 3 of 1.6.0 got rid of the element down-casts in code against "raw" BackingElemApi traits, by "overriding"
query API methods of super-types in BackingElemApi, thus restricting the return types to the ThisElem type member
in BackingElemApi. This is good news, because it means that XML dialect support against generic backends (implementing
BackingElemApi) is easy and safe to implement.


1.6.0-M2
========

Milestone 2 of 1.6.0 contains relatively small changes, some of them (somewhat) breaking. For example:

* Indexed elements now have a node super-type too

  * Hence, all yaidom element implementations have a corresponding node super-type, with at least element and text sub-types
  * Now "indexed documents" no longer (need to) hold simple comment and processing instruction nodes

* Added ``BackingElemApi``, as abstraction for "generic backing elements" in XML dialect support
* Documentation of type members and type parameters in query API
* Bug fix in comment (thanks, Matthias Hogerheijde)


1.6.0-M1
========

Version 1.6.0 (M1) contains several breaking changes, although the impact on client code is limited in that the compiler errors
are easy to fix.

The changes are as follows:

* The query API traits now use type members instead of type parameters

  * This removes some clutter
  * This also postpones some type constraints, thus making these query API traits easier to use for generic "bridge elements"
  * Indeed, this is a trade-off between ease of implementation of the XXXLike traits and ease of use as a generic "backing element" API, where the latter is considered more important
  * Moreover, the Scala compiler itself moves to the encoding of type parameters (directly) as type members

* The "indexed element" query API now retains the same element type when returning ancestor elements
* Therefore the "eager indexed elements" have been removed, and the "normal" indexed elements and documents are now the "lazy" ones

As a result, yaidom becomes leaner and meaner.


1.5.1
=====

Version 1.5.1 is a minor bug fix release, containing no breaking changes. It is a drop-in replacement for version 1.5.0.

The fixes are as follows:

* Method ``findChildElemByPathEntry`` has been made more efficient (so finding element ancestors has become more efficient)
* Parsing QNames and ENames from a string now first trims whitespace

Thanks to Johan Walters for pointing out both issues.


1.5.0
=====

Version 1.5.0 is the same as version 1.5.0-M2. The main contribution of version 1.5.0 compared to version 1.4.2 is
a more stable and consistent functional update API for elements. It is now consistent with the yaidom query API
as well as the transformation API.


1.5.0-M2
========

Version 1.5.0-M2 is almost like version 1.5.0-M1, but has a few small differences:

* Renamed ``Path.Root`` to ``Path.Empty`` and ``Path.isRoot`` to ``Path.isEmpty`` (with deprecation)
* Added some extractors for QNames, Paths and simple elements, for use in pattern matching
* Documented the reasons for not having any functional update support for indexed elements
* Added Java-friendly aliases (``plus`` and ``minus``) for symbolic Scope and Declarations operations
* Some bug fixes (such as exception handling around sensitive getFeature call)
* More tests, for example showing yaidom used for implementing custom XPath functions

Indexed elements have no support for functional updates, because these functional updates are expensive, due to
the required re-computation of Paths of many sibling elements, causing updates to their ancestors as well. So, if
we want to use indexed elements, and at the same time need to do a lot of functional updates, consider using the
lazy indexed element variants, such as ``LazyIndexedScopeElem``, due to their low creation costs.

Yaidom now offers some more patterns to match on, offered by some added extractors. This was an idea of Johan Walters,
who even went a lot further in showing several elegant "chains of pattern matches".


1.5.0-M1
========

Version 1.5.0-M1 improves the functional query API. It is now more consistent with the query API and transformation API.
It is hopefully useful and easy to use (especially methods like updateTopmostElemsOrSelf), and should have good runtime performance.
Update support for indexed elements is also considered for version 1.5.0, but is not yet available in version 1.5.0-M1.

The main changes in this version are:

* Trait ``UpdatableElemApi`` has been enhanced with many new functional update methods, deprecating the old updatedXXX methods
* The simple ``Document`` class has been enhanced with several of these new update methods too (using delegation)
* Method ``findAllChildElemsWithPathEntries`` is now in trait ``IsNavigableApi`` (for the user this makes no difference)
* Class ``ElemWithPath`` has been added as a very lightweight "indexed element", and is used in the new update support
* Added lazy indexed elements, trading query performance for construction time performance
* Easy creation of ``IndexedClarkElem`` and ``IndexedScopedElem`` instances
* Document parsers and printers can now be configured with a custom conversion strategy
* Bug fix for yaidom-0003, and partial bug fix for yaidom-0002
* Removal of previously deprecated code

Upgrading from version 1.4.2 to this version requires recompilation of code using yaidom. Other than that, successful
compilation is likely, but deprecation warnings will occur for much of the old functional update API. The document
parsers and printers now have an extra conversion strategy primary constructor parameter, so if these constructors are
used instead of the factory methods, compilation errors will occur, but they are easy to fix (prefer the factory methods).


1.4.2
=====

Version 1.4.2 undid the deprecation warnings on indexed element and document apply (factory) methods. This version is what version
1.4.0 should have been, and it is advisable to prefer this version over 1.4.0 and 1.4.1.


1.4.1
=====

Version 1.4.1 fixes broken XML Base support, due to a regression. It contains some breaking changes, but only compared
to version 1.4.0 (which is broken in its XML Base support). The most important changes are:

* Fixed the bug in getting the parent base URI of an indexed element
* URI resolution (in XML Base) is sensitive, so indexed element creation now requires a URI resolution strategy to be passed
* Old indexed element factory methods have been deprecated (they use a default URI resolver)

Indexed element creation now goes through a builder, which keeps a URI resolver. The builder could be a global long-lived object.


1.4.0
=====

Version 1.4.0 combines the changes in the 3 milestone releases leading up to this version. For example, it supports:

* XML declarations
* retained document child order
* indexed elements with different underlying element types
* easy conversion of different element types to resolved elements
* better functional update support
* removing the distinction between indexed and docaware elements, and deprecation of docaware elements

Some of these features are supported by cleant up query API traits, without significantly altering the public query API
of the different element implementations. For example:

* indexed documents contain child nodes of quite different types, but they now have a common useful super-type; this is used for keeping the document child order
* traits ``ScopedElemApi`` (offered by all "practical" element implementations) and its super-type ``ClarkElemApi`` (also offered by "minimal" element implementations such as resolved elements) are quite central query API traits; "indexed" element support also uses this distinction

There are some breaking changes in this release, compared to version 1.3.6, but fixing compilation errors in code using
yaidom should be rather straightforward. For example:

* method ``findChildElemByPathEntry`` no longer can nor needs to be overridden
* construction of indexed documents may need an extra parameter for the optional XML declaration
* sometimes conversions from ``Nodes.Comment`` to ``simple.Comment`` (or similar conversions for processing instructions) need to be inserted
* method ``ancestryENames`` is now called ``reverseAncestryENames``, etc.
* there may be very many deprecation warnings for the use of docaware elements, but they can be fixed at any time

When creating a new element implementation (with yaidom 1.4.0), consider the following design choices:

* do we want to have a custom node hierarchy for these elements, including text nodes, comment nodes, etc.?

  * if so, mix in the ``Nodes.Node`` sub-types throughout the custom node hierarchy
  * and consider adding a custom ``CanBeDocumentChild`` sub-type that is also a node in this hierarchy
  * if not, still mix in ``Nodes.Elem`` into the custom element type, thus promising that the element can be a document child
  * for the custom element and text node types, even consider mixing in the ``ResolvedNode.Node`` sub-types (for easy conversions to resolved elements)

* do we want to have a custom document type?

  * if so, let it mix in ``DocumentApi``
  * and let it have child nodes that at least have type ``CanBeDocumentChild`` (or a more appropriate sub-type) in common

* what element query API traits do we want the element implementation to offer?

  * is it a minimal element implementation offering just the ``ClarkElemApi`` query API (and ``ClarkElemLike`` implementation)?
  * or is it a practical element implementation offering the ``ScopedElemApi`` query API?
  * do we want the element to be "indexed", thus using types like ``IndexedScopedElemApi`` (or even final class ``IndexedScopedElem``)?
  * do we want to mix in other traits for functional updates, transformations etc.?

* what state does the element implementation have?

  * if the element is a wrapper around an element from other libraries (especially if mutable), the state should be only the wrapped element


1.4.0-M3
========

Version 1.4.0-M3 made some relatively small (but possibly breaking) changes compared to version 1.4.0-M2.

The main changes in this version are:

* Docaware elements now deprecated
* Improved ``Scope.includingNamespace`` etc., and therefore "editable element support"
* Added methods ``plusChildren`` and ``withChildSeqs``
* Document child order is retained (for different document implementations)
* DOM wrapper documents are no longer nodes, according to yaidom
* SAX-based parsing now also parses the XML declaration, if any
* Separated ``ResolvedNodes.Node`` (convertible to resolved elements) from ``Nodes.Node`` (little more than marker traits)


1.4.0-M2
========

Version 1.4.0-M2 mainly fixed a potential performance problem, introduced with version 1.4.0-M1.

The main changes in this version are:

* Indexed elements (formerly docaware elements) again store the parent base URI, for fast base URI computation
* The docaware package is finally obsolete, in that it now only contains aliases to types of indexed elements and documents and their companion objects
* Generic class IndexedDocument now only takes one type parameter (for the element) instead of two


1.4.0-M1
========

Version 1.4.0-M1 made the core of yaidom meaner and cleaner, except for the addition of XML declaration support.
There are breaking changes, but (with recompilation of code using yaidom) there should not be too many of them.

The changes in this version are:

* There are now 2 main query API abstractions, that combine several orthogonal query API traits:

  * ``ClarkElemApi``, which reminds of the James Clark minimal XML element tree abstraction
  * ``ScopedElemApi``, which extends ``ClarkElemApi``, forming the minimal practical XML element tree abstraction (with QNames and Scopes)
  
* ``ScopedElemApi`` now (indirectly) extends ``IsNavigableApi``:

  * What's more, even ``ClarkElemApi`` extends ``IsNavigableApi``
  * After all, this makes sense for "James Clark element trees", and 2 main query API abstractions suffice
  * ``ClarkElemApi`` extends ``ElemApi``, ``IsNavigableApi``, ``HasENameApi`` and ``HasTextApi``
  * ``ScopedElemApi`` extends ``ClarkElemApi``, ``HasQNameApi`` and ``HasScopeApi``
  * So the net effect on ``ScopedElemApi`` is that it now (indirectly) mixes in ``IsNavigableApi``
  * Also added method ``findReverseAncestryOrSelfByPath`` to ``IsNavigableApi`` (e.g. for fast XML Base computation)
  
* Made "indexed" elements much more generic, and removed the distinction between "indexed" and "docaware" documents:

  * New trait ``IndexedClarkElemApi``, which extends ``ClarkElemApi``, abstracts over indexed elements
  * New trait ``IndexedScopedElemApi`` is similar, but it extends ``ScopedElemApi`` as well as ``IndexedClarkElemApi``
  * Classes ``IndexedClarkElem`` and ``IndexedScopedElem`` extend ``IndexedClarkElemApi`` and ``IndexedScopedElemApi``, respectively
  * The old indexed elements are type ``IndexedScopedElem[simple.Elem]``
  * And so are the old docaware elements, so they can be deprecated soon!
  * Indeed indexed elements now have XML Base support
  * The indexed and docaware Elem companion objects (currently) remained (as did the indexed Document classes/objects)
  
* Support for XML declarations in document classes
* Added some convenience methods to ``Scope``, and used them in new element editor utilities
* Conversions from yaidom to SAX events no longer internal to DocumentPrinterUsingSax

* Added minimal node tree abstraction (``Nodes.Node`` and sub-types):

  * This helped in removing the (wrong) dependency of the "simple" package on the "resolved" package
  * What's more, resolved elements can now be created from other element implementations than just simple elements

* Small bug fixes, such as improved SAX-based parsing and more reliable DOM to yaidom conversions
* Many more tests


1.3.6
=====

Version 1.3.6 removed the alternative "docaware" and "indexed" elements introduced in version 1.3.5. These element
implementations (optimized for fast creation) offer too little "bang for the buck", so they have been removed.
As for "docaware" and "indexed" elements, they are again as in version 1.3.4. No other changes were made in this
release.


1.3.5
=====

Version 1.3.5 is a small performance release. There are no breaking changes. There are now 2 versions of "docaware" and
"indexed" elements, with the default version being optimized for fast querying, and the alternative version being optimized
for fast creation. The dependency on Apache Commons is gone (and pretty printing output is somewhat different).

The changes in this version are:

* No more dependency on Apache Commons

  * Pretty printing of element trees no longer does any "Java escaping", but outputs Scala multiline string literals instead
  * The resulting tree representation is no longer valid Scala code if the "multiline string" contains triple quotes
  * This rare scenario can be dealt with on an ad-hoc basis, if the tree representation happens to be used as Scala code
  * Pretty printing is probably faster than before, due to the fact that Apache Commons "Java escaping" is gone
  
* Added alternative "docaware" and "indexed" elements

  * They live in the ``docaware.alt`` and ``indexed.alt`` sub-packages
  * The alternatives are optimized for fast creation, not for fast querying
  * Therefore, they make better "backing" objects of "sub-type-aware" elements
  * For code re-use, super-traits ``AbstractDocawareElem`` and ``AbstractIndexedElem`` have been introduced

* Bug fixes

  * Bug fix in method ``plusChild``
  * Bug fix in error message of ``ScopedElemLike.textAsResolvedQNameOption``
  * Bug fixes in test code, found by the excellent Artima SuperSafe tool
  * Moved the ``equals`` and ``hashCode`` methods up, from the element class to the node class (in 2 element implementations)


1.3.4
=====

Version 1.3.4 is a minor performance release. There are no breaking changes. The performance improvements are in
the construction of the core objects, such as expanded names, qualified names, etc.

The changes in this version are:

* ``EName`` and ``QName`` construction has become less expensive

  * This is important, since these names are created so often
  * The increased construction speed comes at the expense of removed validity checks
  * These checks can still be performed, using new method ``validated``, but that is the responsibility of the user
  * Note that class ``javax.xml.namespace.QName`` also performs no validity checks on the passed construction parameters

* ``Scope`` and ``Declarations`` construction has become less expensive

  * This is important, since these objects are created so often
  * The checks are still there, but are cheaper, because they now involve much less collections processing
  * In this case, it is rather important to retain the checks, for internal consistency and conceptual clarity
  * For example, the "xml" namespace gets "special" treatment in the yaidom "namespaces theory"

This release was made after profiling by Andrea Desole and Nick Evans had shown that much time was spent in creation
of yaidom core objects.


1.3.3
=====

Version 1.3.3 is a maintenance release. The (few) breaking changes are hardly interesting. The performance fix
in attribute retrieval may be the most important change in this release.

The changes in this version are:

* Breaking change: removed ``TreeReprParsers``

  * Hence no more parsing of the element tree string format
  * No more dependency on Scala parser combinators

* Breaking change: better streaming support in ``StaxEventsToYaidomConversions``

  * Also renamed, refactored and added "event state" data classes, for better streaming support

* Performance fix in ``HasEName.attributeOption`` (the inefficient ``toMap`` conversion is gone)
* More tests (XML Base, i18n, etc.), and refactored tests
* Woodstox StAX parser used in test code (for XML 1.1 support)


1.3.2
=====

Version 1.3.2 is like version 1.3.1, but with more documentation and test cases with respect to XML Base support in
doc-aware elements.


1.3.1
=====

Version 1.3.1 is like version 1.3, except that XML Base support has been improved with respect to performance
(in version 1.3 XML Base support was too slow to be useful).

Breaking change: method ``baseUriOfAncestorOrSelf`` has been removed. Doc-aware elements now also keep the parent
base URI as state.


1.3
===

Version 1.3 is like version 1.2, except that the aliases in the root package to ``core`` and ``simple`` have been
removed entirely.

Moreover, method ``baseUri`` has been added to ``docaware.Elem`` (thus implementing XML Base).

Note that versions 1.1 and 1.2 were only meant as intermediate versions leading up to version 1.3. It makes sense to
compare version 1.3 to version 1.0 w.r.t. performance. In version 1.0, "simple" elements stored (in each element node!)
a Map from path entries to child node indices. In version 1.3 (even in version 1.1) that is no longer the case.

This means that path-based navigation (see ``IsNavigableApi``) is no longer effectively in constant time. Hence path-based
navigation in bulk, and as a consequence functional updates in bulk (see ``UpdatableElemApi``) are much slower in
version 1.3 than in version 1.0! So bulk navigation is now really a bad idea.

The upside is that in version 1.3 there are no longer any costs associated with the above-mentioned Map (per element).
As a consequence, in version 1.3 parsing and transforming (simple) elements is a bit faster and uses somewhat less
memory than in version 1.0. Given that typically bulk navigation is avoided, the overall performance is better using
version 1.3 than version 1.0 of yaidom.


1.2
===

Version 1.2 is like version 1.1, except that the aliases in the root package to ``core`` and ``simple`` have been
deprecated. In version 1.3, these deprecated aliases will be removed.


1.1
===

Version 1.1 is much more than a minor release. It has a lot of breaking changes. See the road map document.

Here is why yaidom 1.1 is an important release:

* Yaidom has been reconstructed by making the query API cleaner and more orthogonal under the hood, and therefore more flexible
* Related to this query API reorganization: the top-level package has been split into 3 sub-packages
* Most element implementations now offer more of the yaidom query API, and therefore become more interchangeable
* Yaidom is now both faster and less memory-hungry
* Yaidom is not only extensible w.r.t. element implementations (even more so than before), but also to support "XML dialects"
* Namespace-related utilities have been added

The (mostly breaking) changes in this version are:

* The root package has been split into sub-packages ``core``, ``queryapi`` and ``simple``

  * Package ``core`` contains core concepts, such as expanded names, qualified names etc.
  * Package ``queryapi`` contains the query API traits
  * Package ``simple`` contains the default (simple) element implementation
  * In version 1.1, there are aliases to ``core`` and ``simple`` classes, to ease the transition to yaidom 1.2 and 1.3
  
* The query API traits have been re-organized, renamed, and made more orthogonal:

  * The old inheritance hierarchy is gone
  * The ``PathAwareElemApi`` trait is gone, with no replacement (use indexed elements instead)
  * ``ParentElemApi`` (1.0) has been renamed to ``ElemApi``
  * ``ElemApi`` (1.0) is now ``ElemApi with HasENameApi``
  * ``NavigableElemApi`` (1.0) is now ``ElemApi with HasENameApi with IsNavigableApi``
  * ``UpdatableElemApi`` minus ``PathAwareElemApi`` (1.0) is now ``ElemApi with HasENameApi with UpdatableElemApi``
  * ``SubtypeAwareParentElemApi`` (1.0) has been renamed to ``SubtypeAwareElemApi``
  * The (1.1) combination ``ElemApi with HasENameApi with HasQNameApi with HasScopeApi with HasTextApi`` (with some additional methods) is called ``ScopedElemApi``
  
* Most element implementations now mix in ``ScopedElemApi with IsNavigableApi``, therefore offering almost the same query API
* Yaidom (simple, docaware, indexed) elements now store less data per element, thus reducing memory usage

  * Not only memory usage went down, but yaidom became faster as well (unless performing Path-based navigation in bulk)
  
* A test case shows how yaidom (and its ``SubtypeAwareElemApi`` query API trait) can be used to support individual XML dialects

  * The test case also shows how to do that while keeping the "XML backend implementation" pluggable
  * Type-safe querying for such XML dialects thus becomes feasible using yaidom
  
* Namespace-related utilities have been added, for moving up namespace declarations, stripping unused namespaces etc.
* The Node and NodeBuilder creation DSLs have been cleaned up a bit, resulting in breaking changes
* Small additions, such as method ``plusChildOption``, Path method ``append``, and method ``ancestryOrSelfENames``
* Upgraded Scala 2.11 version, as well as versions of dependencies


1.0
===

Version 1.0 is basically version 0.8.2, given the "1.0 status". Yaidom is now considered mature enough for a 1.0 release,
at least by the author and his colleagues, who use yaidom extensively in production code.

Several (small) libraries depending on this "yaidom core", and leveraging its extensibility, would make sense.
Think for example about Saxon yaidom wrappers (offering the ElemApi query API, at least), or XML Schema support (offering
the SubtypeAwareParentElemApi query API, at least).

Compared to version 0.8.2, there are no changes worth mentioning.


0.8.2
=====

Version 0.8.2 is a minor release, except for the addition of one new query trait. There are no breaking changes in this version.

The changes in this version are:

* Introduced trait ``SubtypeAwareParentElemApi`` and its implementation ``SubtypeAwareParentElemLike``:

  * These traits bring the ``ParentElemApi`` query API to object hierarchies
  * For example, when implementing XML schema components as immutable "elements", these traits come in handy as mix-ins
  * Many more XML (immutable) object hierarchies could use these traits, such as XBRL instance support and XLink support
  * The traits are not used by yaidom itself (except for internals in the utils package)
  * The ``SubtypeAwareParentElemLike`` trait is trivially implemented in terms of ``ParentElemLike``, and only offers convenience

* Added methods ``comments`` and ``processingInstructions`` to docaware and indexed Documents
* More test coverage
* Made creation of indexed and docaware elements a bit faster (by no longer running some "obviously true" assertions)
* Reworked the internal XmlSchemas API, in the utils package (it uses SubtypeAwareParentElemLike now)


0.8.1
=====

Version 0.8.1 is much like version 0.8.0, but it targets Scala 2.11.X as well as 2.10.X. There are no breaking changes in this version.

The changes in this version are:

* Built for Scala 2.11.X as well as Scala 2.10.X
* Introduced ``NavigableElemApi`` between ``ElemApi`` and ``PathAwareElemApi``:

  * This new query API trait offers Path-based navigation, but not Path-aware querying
  * ``NavigableElemApi`` contains (existing) methods like ``findChildElemByPathEntry`` and ``findElemOrSelfByPath``
  * Analogously, ``NavigableElemLike`` sits between ``ElemLike`` and ``PathAwareElemLike``
  * The net effect is that ``PathAwareElemApi`` and ``PathAwareElemLike`` offer the same API as before, without any breaking changes
  * Yet now "indexed" and "docaware" Elems mix in trait ``NavigableElemApi``, thus offering (fast) Path-based navigation, making these Elems more useful

* A Scope can also be used as JAXP NamespaceContext factory, thus facilitating the use of JAXP XPath support (even in Java code!)

In summary, version 0.8.1 is like 0.8.0, but it supports Scala 2.11.X, and makes "indexed" and "docaware" Elems more useful.


0.8.0
=====

Version 0.8.0 is much like version 0.7.1, but it drops support for Scala 2.9.X, and prunes deprecated code.

The changes in this version are:

* Scala 2.9.X is no longer supported, and Scala 2.10 features are (finally) used:

  * From now on, string interpolation is used in yaidom implementation code
  * Modularized language features also help increase quality, because the compiler performs more QA
  * Futures (and promises) are used in test code where concurrency is involved
  * Implicit (value!) classes can also be used
  * On the other hand, experiments with value classes for ENames and QNames did not work out, and using them for "wrapper elements" would require query API traits to be "universal"
  * It can also be risky to have non-local dependencies on restrictions imposed by value classes and universal traits, so value classes have rarely been used

* Deprecated code was removed
* First round of (potential) performance improvements:

  * Large scale duplication of equal EName and QName objects (in yaidom DOM-like trees) causes a large memory footprint
  * Using ``ENameProvider`` and ``QNameProvider`` instances, introduced in this version, memory usage can be decreased to a large extent
  * Yet it was not desirable to destabilize the API by introducing implicit parameters (containing implementation details) all over the place
  * So in the end (newly introduced) implicit parameters are rare and they are used only deep in the implementation
  * And ENameProvider and QNameProvider strategies can only be chosen at a global level
  * Some ENameProvider and QNameProvider implementations have indeed been provided

* Added easy conversions from QNames to ENames, given some Scope:

  * Now we can write queries based on stable ENames, but writing only QNames (that are easily converted to ENames, given a Scope)

* Added "thread-local" DocumentParser and DocumentPrinter classes, for use in an "enterprise" application
* Added ``HasQName`` trait, to enable abstraction over elements that expose QNames
* Upgraded some (test) dependencies to newer versions, e.g. ScalaTest was upgraded to version 2.0
* Removed (soon to be deprecated?) procedure syntax
* More tests


0.7.1
=====

Version 0.7.1 has one big API change: renaming ElemPath to Path (and ElemPathBuilder to PathBuilder), deprecating the old names.
This change makes the query API (in particular PathAwareElemApi) more clear: it is now more obvious what methods like
``findAllElemPaths`` mean, given the yaidom convention that in query methods "Elems" means "descendant elements", and "ElemsOrSelf"
means "descendant-or-self elements". The idea of renaming ElemPath to Path came from Nick Evans.

In spite of the API changes, this version should be a drop-in replacement for version 0.7.0, except that the changed parts
of the API now lead to deprecation warnings. It is advisable to adapt code using yaidom in such a way that those deprecation warnings
disappear. It is likely that version 0.8.0 (which may or may not be the next version) will no longer contain the deprecated classes
and methods.

The changes in this version are:

* Renaming ``ElemPath`` (and ``ElemPathBuilder``) to ``Path`` (and ``PathBuilder``), deprecating the old names

  * Also renamed ``elemPath`` in "indexed" and "docaware" Elems to ``path``, deprecating the old name
  * the idea is to talk consistently about "paths", not about "element paths" (or "elem paths")

* Added "docaware" elements (mixing in trait ElemApi), which are like "indexed" elements, but also keeping the document URI
* Renamed ``findWithElemPath`` to ``findElemOrSelfByPath`` (deprecating the old name). Also renamed ``findWithElemPathEntry`` and ``getWithElemPath``.
* Added convenience methods for creating "element predicates", for example to make it slightly easier to query using local names
* Many more tests


0.7.0
=====

Version 0.7.0, finally. Starting with this version, API stability and proper deprecation will be considered important.

* XLink support has been removed from core yaidom, and will live in its own project


0.6.14
======

This version improves the Scaladoc documentation. This will probably become version 0.7.0.

* Reworked the Scaladoc documentation (better showing how to use the API), and removed obsolete (non-Scaladoc) documentation.
* Breaking API change: ``indexed.Elem`` no longer mixes in ``HasParent``, and is now more efficient when querying
* Bug fixes in methods ``updatedAtPathEntries`` and ``updatedWithNodeSeqAtPathEntries``
* Tested against IBM JDK (ibm-java-x86_64-60)


0.6.13
======

This version contains small breaking and non-breaking changes, and partly reworked documentation. Hopefully version 0.7.0
will be the same, except for the documentation.

* Reworked main package documentation, mainly to clarify usage of the API with examples
* Breaking API change: renamed ``Scope`` and ``Declararations`` fields ``map`` to ``prefixNamespaceMap``
* Breaking API change: removed ``Scope`` method ``prefixOption``, and added method ``prefixesForNamespace``
* Breaking API change: altered signature of ``ElemPath`` object method ``from``, for consistency with ``ElemPathBuilder``
* Added ``ElemPath`` method ``elementNameOption``
* Added generic trait ``DocumentApi``


0.6.12
======

This version improves on the last "functional update/transformation" support, by restoring bulk updates (this time with a
less inefficient implementation) and removing the transformation methods that need "context".

* Added ``UpdatableElemApi`` bulk update methods ``updatedAtPathEntries`` and ``updatedAtPaths``

  * Added ``updatedWithNodeSeqAtPathEntries`` and ``updatedWithNodeSeqAtPaths`` as well
  * Also added update methods for Documents

* Breaking API change: ``TransformableElemApi`` (overloaded) methods taking "context" have been removed
* Breaking API change: removed (unnecessary) ``Scope`` methods ``notUndeclaring`` and ``notUndeclaringPrefixes``
* Breaking API change: renamed ``Scope`` method ``minimized`` to ``minimize``
* Breaking API change: ``YaidomToScalaXmlConversions`` methods ``convertNode`` and (overloaded) ``convertElem`` take extra NamespaceBinding parameter
* Added collections methods ``filter``, ``filterKeys`` and ``keySet`` to ``Scope`` (for convenience)
* Added ``Elem`` methods for getting QName-valued attribute values or text values as QNames or ENames (for convenience)
* Clarified broken abstractions such as ``ElemApi`` when using Scala XML as backend
* Bug fix: ``YaidomToScalaXmlConversions`` method ``convertElem`` tries to prevent duplicate namespace declarations
* Added ``apply`` factory methods to Scala XML wrapper nodes and DOM wrapper nodes (for convenience)


0.6.11
======

This version offers completely reworked "functional update/transformation" support. The ElemPath-based bulk updates have
been removed, because they were far too inefficient. The "transformation" support, however, has been enhanced a lot.

* Big breaking API change: ``UpdatableElemApi`` has been made smaller

  * All functional updates taking a PartialFunction have been removed (``updated``, ``topmostUpdated`` and ``updatedWithNodeSeq``)
  * They were bulk updates (implicitly) based on element paths, which is very inefficient
  * Added ``updated`` method taking an ``ElemPath.Entry`` (and a function in its 2nd parameter list)
  
* Big breaking API change: ``TransformableElemApi`` has been enhanced a lot

  * Like ``UpdatableElemApi``, trait ``TransformableElemApi`` now takes 2 type parameters, viz. the node type and the element type
  * Method ``transform`` has been renamed to ``transformElemsOrSelf``
  * Added methods such as ``transformElems``, ``transformChildElems``
  * Also added methods such as ``transformElemsOrSelfToNodeSeq``, ``transformElemsToNodeSeq`` and ``transformChildElemsToNodeSeq``
  * Trait ``TransformableElemApi`` elegantly reminds of ``ParentElemLike``, except that it is for querying instead of updates
  * Trait ``TransformableElemApi`` is even mixed in by ``ElemBuilder``
  
In summary, the functional update support of the preceding release was not good enough to be frozen (in upcoming version 0.7.0).
Hence this version 0.6.11.


0.6.10
======

This version improves "functional update" support as well as "Scala XML literal" support (before version 0.7.0 arrives).

* Improved "functional update" support

  * Added ``updatedWithNodeSeq`` and ``topmostUpdatedWithNodeSeq`` methods to ``UpdatableElemApi`` and ``UpdatableElemLike``
  * These methods are defined (directly or indirectly) in terms of ``updated``
  * Yet these methods make functional updates more practical, by offering updates that replace elements by collections of nodes
  * They are even powerful enough to express what are separate operations in XQuery Update, such as insertions, deletions etc.

* Added ``TransformableElemApi`` and ``TransformableElemLike``

  * "Transformations" apply a transformation function to all descendant-or-self methods
  * In contrast, "(functional) updates" apply update functions only to elements at given (implicit or explicit) paths
  * "Transformations" and "(functional) updates" can express pretty much the same, but have different performance characteristics
  * Roughly, if only a few elements in an element tree need to be updated, prefer "updates", and otherwise prefer "transformations"

* Added ``YaidomToScalaXmlConversions``,  as a result of which there are now conversions between Scala XML and yaidom in both directions
* Added ``ScalaXmlElem``, which is an ``ElemLike`` query API wrapper around Scala XML elements
* Added ``AbstractDocumentPrinter``, making ``DocumentPrinter`` purely abstract (analogous to document parsers)
* Richer ``prettify`` method, optionally changing newline characters and optionally using tabs instead of spaces
* Added ``copy`` method to classes Elem and ElemBuilder
* Some documentation changes and bug fixes, and more tests

This version offers many "tools" for creation of and updates to XML trees, such as support for Scala XML literals (converting them
to yaidom and vice versa, or querying them using the yaidom query API), "transformations", and (functional) updates (replacing
elements by elements, or elements by node collections).


0.6.9
=====

This is still another version leading up to version 0.7.0. It does contain a few breaking changes.

* Big breaking API change: XML literals are gone (i.e. hidden), and replaced by conversions from Scala XML to yaidom

  * The conversions from Scala XML to yaidom make it possible to create Scala XML literals, and immediately convert them to yaidom Elems
  * Yaidom XML literals, on the other hand, still need a lot of work before they become useful
  * One problem with the yaidom XML literals concerns the runtime costs of XML parsing at each use (instead of having a macro "compile" them)
  * Another problem with yaidom XML literals concerns the restrictions w.r.t. the locations of parameters
  * The conversions between Scala XML Elems and yaidom Elems are one-way only, from Scala XML to yaidom
  * These conversions make it possible to use Scala XML literals as if they are "yaidom XML literals"
  * These conversions even work around nasty XML Scala namespace-related bugs, such as SI-6939
  
* Breaking API change: removed overloaded ``\``, ``\\``, ``\\!`` and ``\@`` methods taking just a local name (as string)

  * An experiment was conducted to make EName and QName (Scala 2.10) value classes, to avoid EName/QName object explosion
  * In this experiment, the overloads above had to go (besides, they violated the "precision" of yaidom anyway)
  * This experimental change has been reverted (for now), but I want to keep the option open to use value classes for EName/QName in the future
  * So the overloaded methods above have been removed (probably permanently)
  * In the spirit of "precise" querying, also renamed ``findAttribute`` (taking a local name) to ``findAttributeByLocalName``

* Breaking API change: renamed ``baseUriOption`` to ``uriOption``, and ``withBaseUriOption`` to ``withUriOption``
* Breaking API change: removed method ``QName.prefixOptionFromJavaQName``
* Added some overloaded ``DocumentParser.parse`` methods
* ``LabeledXLink`` is no longer a trait with a val, but is now an abstract class

As for the maturity of parts of yaidom:

* Its querying support is the most mature part. The APIs ("abstractions") are simple and clear, and seem to work well.
* Its functional update support is still rather basic. It should first mature, without postponing version 0.7.0 too much.
* Its XML literal support simply is not useful yet, so an alternative has been provided in version 0.6.9 (instead of further postponing version 0.7.0).


0.6.8
=====

This version is probably the last release before version 0.7.0. It does contain a few breaking changes.

* Breaking API change: renamed method ``allChildElems`` to ``findAllChildElems``
* Breaking API changes (related):

  * Renamed ``allChildElemsWithPathEntries`` to ``findAllChildElemsWithPathEntries``
  * Renamed ``allChildElemPathEntries`` to ``findAllChildElemPathEntries``
  * Renamed ``allChildElemPaths`` to ``findAllChildElemPaths``
  
* Breaking API changes: removed methods ``collectFromChildElems``, ``collectFromElems`` and ``collectFromElemsOrSelf``
* Breaking API change: removed method ``getIndex``
* Added ``indexed.Elem`` methods ``scope`` and ``namespaces``
* Added method ``Elem.minusAttribute``
* Performance improvements to ``Elem.toString``
* Worked on XML literal support, but the result is still highly experimental
* Scala and ScalaTest upgrade (versions 2.10.1 and 1.9.1, respectively)

Hopefully only documentation updates and small non-breaking fixes will be the difference between version 0.6.8 and
upcoming version 0.7.0. In other words, hopefully the API is stable from now on.


0.6.7
=====

This version is again one step closer to version 0.7.0. It contains small improvements, and contains only "smallish" breaking changes.

* Added ``HasParent`` API, mixed in by ``indexed.Elem`` and ``DomElem``, without changing those classes from the outside
* Added purely abstract ``ParentElemApi``, ``ElemApi`` etc., which are implemented by ``ParentElemLike``, ``ElemLike`` etc.
* Added ``ElemBuilder`` methods ``canBuild``, ``nonDeclaredPrefixes`` and ``allDeclarationsAreAtTopLevel``
* Added ``Scope`` methods ``inverse`` and ``prefixOption``
* Breaking API change: removed ``ElemBuilder.withChildNodes``
* Breaking API change: removed confusing methods ``Declarations.subDeclarationsOf`` and ``Declarations.superDeclarationsOf``
* Breaking API change: XLink labels need not be unique within extended links. This affects the extended link methods like ``labeledXLinks``.
* Moved method ``plusChild`` (taking one parameter) up to ``UpdatableElemLike``
* A few bug fixes
* More tests, and more documentation


0.6.6
=====

This version is one step closer to version 0.7.0. It introduces so-called "indexed" elements, (almost) without changing the
query API and the "conceptual surface area".

* Small breaking API change: removed obsolete method ``UpdatableElemLike.findChildPathEntry``
* Added "indexed" elements, which mix in trait ElemLike:

  * "Indexed" elements are a "top-down notion" of elements, knowing about their ancestry

* Added some "functional update" methods, such as ``plusChild``, ``minusChild``, ``topmostUpdated``, and changed the meaning of ``updated``
* Reworked some internals for better performance (at the cost of more memory usage):

  * Made ``PathAwareElemLike`` methods ``findWithElemPathEntry`` and ``allChildElemsWithPathEntries`` abstract
  * Element path based querying (and method ``findWithElemPathEntry`` in particular) is much faster now

* More tests


0.6.5
=====

This version prepares the future upgrade to version 0.7.0, which will take stability of the API far more seriously (with proper
deprecation of obsolete code). Much cleanup of the API has therefore be done in this release 0.6.5. Many (mostly small) breaking API changes
have been performed in this release. The foundations of the API are clear, and the packages, types and their methods now all
have a clear purpose. Moreover, consistency of the API has improved somewhat. As a result of this API cleanup, it is to be
expected that future release 0.7.0 will be pretty much like this release, except for cleaned up documentation.

* Breaking API changes: The ``updated`` methods now return single elements instead of node collections, so they can now be called on the "root element path"
* Breaking API change: Renamed ``Scope.resolveQName`` to ``Scope.resolveQNameOption``
* Breaking API change: Removed ``IndexedDocument``
* Breaking API change: Removed ``Node.uid`` (and method ``getElemPaths``)
* Breaking API change: Made ``XmlStringUtils`` internal to yaidom
* Breaking API change: Moved method ``prefixOptionFromJavaQName`` from ``EName`` to ``QName``
* Breaking API change: Removed ``ElemPath.fromXPaths``
* Breaking API change: Renamed ``DomNode.wrapOption`` to ``DomNode.wrapNodeOption``
* Added method ``Elem.plusAttribute`` (now that attributes can be ordered)
* Experimental, and only for Scala 2.10: XML literals (a first naive version)


0.6.4
=====

* Breaking API changes: Throughout the yaidom library (except for "resolved elements"), attributes in elements are now ordered (for "better roundtripping")!
* Added ``DocumentPrinter.print`` methods that print to an OutputStream, and therefore typically save memory
* Fixed method ``DocumentPrinterUsingStax.omittingXmlDeclaration``
* Improved ``DocumentParser`` classes with respect to character encoding detection
* ``StaxEventsToYaidomConversions`` can now produce an Iterator of XMLEvents, thus enabling less memory-hungry StAX-based parsing
* Indeed, ``DocumentParserUsingStax`` uses these Iterator-producing conversions, thus leading to far less memory usage
* Added ``ElemPath`` convenience methods ``findAncestorOrSelfPath`` and ``findAncestorPath``
* Breaking API change: removed superfluous ``childIndexOf`` method (twice)
* Added yaidom tutorial
* Removed half-baked support for Java 5 (requiring at least Java 6 from now on)


0.6.3
=====

* Enabled cross-building and publishing (to Sonatype repository) for different Scala versions, using sbt
* Added DOM Load/Save based document parser and printer
* Document printers can now print to byte arrays, given some character encoding
* Extended XLinks know their resources and locators by label
* Bug fix in `YaidomToDomConversions`: top-level comments occur before the document element, not after
* Tests now also run on Java 5, including an IBM JRE 5
* Small fixes, code cleanup and documentation additions


0.6.2
=====

In this version, yaidom clearly became 2 things: an element querying API (trait ``ParentElemLike`` and sub-traits), and concrete
(immutable and mutable) element classes into which those traits are mixed in. The element querying API can also be mixed in into
element classes that are not part of yaidom, such as ``ParentElemLike`` wrappers around JDOM or XOM.

* Breaking API change: made class ``Declarations`` a top-level class, because "namespace declarations" are an independent concept
* Breaking API changes to classes ``Scope`` and ``Declarations``:

  * Simplified the implementations, with both classes now backed by maps from prefixes to namespace URIs
  * Removed several methods (that are not often used outside the yaidom library itself)
  * Added several methods, thus making both classes more internally consistent than before
  * Added properties and their proofs to the documentation of both classes

* Added trait ``ParentElemLike``, as an independent abstraction, offering a rich "base" element querying API:

  * Trait ``ElemLike`` extends this new trait
  * Trait ``ParentElemLike`` has only abstract method ``allChildElems``, and no further "knowledge" than that
  * This trait is also mixed in by ``ElemBuilder``
  * The documentation of trait ``ParentElemLike`` contains several properties with their proofs
  * The subtrait ``ElemLike`` got some new attribute querying methods

* Added class ``ElemPathBuilder``
* Fixed class ``ElemPath``, using new method ``Scope.isInvertible``
* Added trait ``UpdatableElemLike``:

  * Mixed in by different element classes
  * Breaking API change: ``Elem.updated`` methods now returning node collections instead of single elements
  * Also clarified and re-implemented ``Elem.updated`` for speed (in different ``Elem`` classes)
  * Added methods like ``withUpdatedChildren`` and ``withPatchedChildren``

* Added trait ``PathAwareElemLike``, which mirrors trait ``ParentElemLike``, but returns element paths instead of elements
* Added ``dom`` package:

  * ``ElemLike`` wrappers around W3C DOM nodes

* Adapted ``convert`` package:

  * Breaking API changes: renamed several singleton objects
  * Many conversion methods are now public
  * "Conversion" API became more consistent
  * Removed 2 ``convertToElem`` methods that were easy to use incorrectly

* Breaking API change: ``Document`` is no longer a ``Node``, and ``DocBuilder`` no longer a ``NodeBuilder``
* ``Node`` has a similar DSL for creating node trees as ``NodeBuilder``, using methods like ``elem``, ``text`` etc.
* Added some convenience methods to ``ElemBuilder``, like ``withChildren`` and ``plusChild``
* Added convenience method ``NodeBuilder.textElem``
* Added ``Elem`` methods ``prettify`` and ``notUndeclaringPrefixes``
* Documented namespace-awareness for Document parsers
* Added motivation document
* Added test case for some "mini-yaidom", which can be used in an article explaining yaidom
* Added many other tests
* Added sbt build file


0.6.1
=====

* Small breaking API change, and (bigger) implementation change: renamed and re-implemented the ``toAstString`` methods:

  * They are now called ``toTreeRepr`` (for "tree representation"), for ``Node`` and ``NodeBuilder``
  * The implementation is easier to understand, using a new ``PrettyPrinting`` singleton object as ``toTreeRepr`` implementation detail
  * The ``toTreeRepr`` output has also slightly changed, for example child ``List`` became child ``Vector``
  
* Added singleton object ``TreeReprParsers``, generating parsers for the ``toTreeRepr`` String output

  * It uses the Scala parser combinator API, extending ``JavaTokenParsers``
  * These tree representations represent parsed XML trees, so they are much closer to ``Node`` and ``NodeBuilder``
  * The tree representations are valid Scala code themselves (using ``NodeBuilder`` methods)
  * An extra dependency was added, namely Apache Commons Lang

* ``Node`` and ``NodeBuilder`` are now serializable:

  * So they could in principle be stored efficiently as a BLOB in the database, and quickly materialized again
  
* Minor breaking API changes, tightening the collection type for child nodes:

  * The ``NodeBuilder.elem`` factory method now takes an ``immutable.IndexedSeq[NodeBuilder]``
  
* The ``EName`` and ``QName`` one-arg ``apply`` methods now behave like the ``parse`` methods, so they no longer require only a local part


0.6.0
=====

* Breaking API change: renamed ``ExpandedName`` to ``EName`` (after which some implicit conversions started to make less sense, and they have indeed been removed)
* Breaking API change: removed all (!) implicit conversion methods

  * ``EName`` and ``QName`` factory methods work just fine
  * The ``Scope`` and ``Scope.Declarations`` factory methods ``from`` have been added, which are easy to use

* Breaking API change: renamed ``ElemLike`` method ``filterChildElemsNamed`` to ``filterChildElems``, etc.
* Added overloaded ``\``, ``\\`` and ``\\!`` methods taking an expanded name, or even a local name, to ``ElemLike``
* Moved method ``localName`` to ``ElemLike``
* Added trait ``HasText`` (in practice element types mix in both ``ElemLike`` and ``HasText``)
* More tests, and some test cleanup after the above-mentioned changes


0.5.2
=====

* Breaking API change: renamed the ``jinterop`` package to ``convert``:

  * In principle we could later add conversions from/to Scala standard library XML to this package, without the need to rename this package again
  
* The ``ElemLike`` operators now stand for ``filterElemsOrSelf`` and ``findTopmostElemsOrSelf`` (instead of ``filterElems`` and ``findTopmostElems``, resp.):

  * This is more consistent with XPath, so less surprising

* Some QA by the Scala 2.10.0-M3 compiler, fixing some warnings:

  * This includes the removal of the (remaining) postfix operators
  * API change: the implicit conversions are now in ``Predef`` objects that must be explicitly imported
  * Also removed keyword ``val`` from ``for`` comprehensions

* More tests


0.5.1
=====

* Added so-called "resolved" nodes, which can be compared for (some notion of value) equality
* Changes in ``ElemLike``:

  * Major documentation changes, clarifying the fundamental properties of the ``ElemLike`` API
  * Breaking API changes: removed methods ``getIndexToParent``, ``findParentInTree`` and ``getIndexByElemPath``
  * Fixed inconsistency: method ``findChildElem`` returns the first found child element obeying the given predicate, no longer assuming that there is at most one such element
  
* A yaidom ``Node`` (again) has a UID, thus enabling the extension of nodes with additional data, using the UID as key
* Added ``IndexedDocument``, whose ``findParent`` method is efficient (leveraging the UIDs mentioned above)
* Small additions to ``ElemPath``: new methods ``ancestorOrSelfPaths`` and ``ancestorPaths``
* Documentation recommends use of TagSoup for parsing HTML (also added test case method using TagSoup)
* Added support for printing an ``Elem`` without XML declaration
* Added document about some XML gotchas


0.5.0
=====

* Breaking changes in ``ElemLike`` API, renaming almost all methods!

  * The core element collection retrieval methods are (abstract) ``allChildElems`` (not renamed), and ``findAllElems`` and ``findAllElemsOrSelf`` (after renaming)
  * The other (renamed) element collection retrieval methods taking a predicate are ``filterChildElems``, ``filterElems``, ``filterElemsOrself``, ``findTopmostElems`` and ``findTopmostElemsOrSelf``
  * The element (collection) retrieval methods taking an ExpandedName are now called ``filterChildElemsNamed`` etc.
  * There are shorthand operator notations for methods ``filterChildElems``, ``filterElems`` and ``findTopmostElems``
  * Methods returning at most one element are now called ``findChildElem``, ``getChildElem`` etc.
  * Why all this method renaming?
  
    * Except for "property" ``allChildElems``, the retrieval methods now start with verbs, as should be the case
    * Those verbs are closer to Scala's Collections API vocabulary, and thus convey more meaning
    * In method names, nouns refer to the "core element set" (children, descendants, decendants-or-self), and verbs (and optional adjective, preposition etc.)
      refer to the operation on that data ("filter", "find topmost", "collect from" etc.)
    * Since method names start with verbs, name clashes with variables holding retrieval method results are far less likely
    * The core element collection retrieval methods are easy to distinguish from the other element (collection) retrieval methods
    * Operator notation ```\```, ```\\``` and ```\\!```, when used appropriately, can remove a lot of clutter
    
* Made ``ElemPath`` easier to construct
* Small improvements, such as slightly less verbose ``ElemBuilder`` construction


0.4.4
=====

* Improved ``ElemLike``

  * Better more consistent documentation
  * Added some methods for consistency
  * Far better performance
  * Breaking API change: renamed ``childElemOption`` to ``singleChildElemOption`` and ``childElem`` to ``singleChildElem``
  
* Added ``DocumentPrinterUsingSax``
* Added ``Elem.localName`` convenience method
* Introduced JCIP (Java Concurrency in Practice) annotation @NotThreadSafe (in SAX handlers)
* Small documentation changes and refactorings (including banning of postfix operators)
* More test code


0.4.3
=====

* API changes in ``xlink`` package

  * Added ``Link.apply`` and ``XLink.mustBeXLink`` methods

* API change: renamed ``DocumentBuilder`` to ``DocBuilder`` to prevent conflict with DOM ``DocumentBuilder`` (which may well be in scope)
* API changes (and documentation updates) in ``parse`` package

  * The ``DocumentParser`` implementations have only 1 constructor, and several ``newInstance`` factory methods, one of which calls the constructor
  * ``DocumentParserUsingSax`` instances are now re-usable, because now ``ElemProducingSaxHandler`` producing functions (instead of instances) are passed
  
* API changes (and documentation updates) in ``print`` package

  * The ``DocumentPrinter`` implementations have only 1 constructor, and several ``newInstance`` factory methods, one of which calls the constructor
  
* Small API changes:

  * Added 1-arg ``Document`` factory method, taking a root ``Elem``
  * Added ``Document.withBaseUriOption`` method
  * Added some methods to ``ElemPath`` (for consistency)
  
* More documentation, and added missing package objects (with documentation)


0.4.2
=====

* API changes in trait ``ElemLike``

  * Renamed method ``firstElems`` to ``topmostElems`` and ``firstElemsWhere`` to ``topmostElemsWhere``

* Bug fix: erroneously rejected XML element names starting with string "xml"


0.4.1
=====

* XLink support largely redone (with breaking API changes)

  * Removed top level ``Elem`` in the ``xlink`` package (wrapping a normal ``Elem``)

* Renamed implementation trait ``ElemAsElemContainer`` back to ``ElemLike``
* More tests, including new test class ``XbrlInstanceTest``
