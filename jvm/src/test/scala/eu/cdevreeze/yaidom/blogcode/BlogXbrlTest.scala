/*
 * Copyright 2011-2017 Chris de Vreeze
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package eu.cdevreeze.yaidom.blogcode

import eu.cdevreeze.yaidom.blogcode.AbstractBlogXbrlTestSupport.XbrliNs
import eu.cdevreeze.yaidom.blogcode.AbstractBlogXbrlTestSupport.XmlNs
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi.withEName
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi.withLocalName
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.utils.DocumentENameExtractor
import eu.cdevreeze.yaidom.utils.NamespaceUtils
import eu.cdevreeze.yaidom.utils.SimpleTextENameExtractor
import eu.cdevreeze.yaidom.utils.TextENameExtractor
import org.scalatest.funsuite.AnyFunSuite

/**
 * Code of yaidom XBRL blog ("introducing yaidom, using XBRL examples"). The blog introduces yaidom in the context
 * of XBRL instances. The examples show implementations of some FRIS International rules, gradually introducing
 * more advanced yaidom element implementations.
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
class BlogXbrlTest extends AnyFunSuite with AbstractBlogXbrlTestSupport {

  private val sampleXbrlInstanceFile: java.io.File =
    (new java.io.File(classOf[BlogXbrlTest].getResource("sample-Instance-Proof.xml").toURI))

  private val xbrliDocumentENameExtractor: DocumentENameExtractor = {
    // Not complete, but good enough for this example!

    new DocumentENameExtractor {

      def findElemTextENameExtractor(elem: BackingElemApi): Option[TextENameExtractor] = elem.resolvedName match {
        case EName(Some(xbrliNs), "measure") if elem.path.containsName(EName(xbrliNs, "unit")) =>
          Some(SimpleTextENameExtractor)
        case EName(Some(xbrldiNs), "explicitMember") =>
          Some(SimpleTextENameExtractor)
        case _ => None
      }

      def findAttributeValueENameExtractor(elem: BackingElemApi, attributeEName: EName): Option[TextENameExtractor] = elem.resolvedName match {
        case EName(Some(xbrldiNs), "explicitMember") if attributeEName == EName("dimension") =>
          Some(SimpleTextENameExtractor)
        case _ => None
      }
    }
  }

  private val xbrlInstance: XbrlInstance = {
    val docParser = DocumentParserUsingStax.newInstance()

    val doc = docParser.parse(sampleXbrlInstanceFile)
    val indexedDoc = indexed.Document(doc)

    XbrlInstance(indexedDoc.documentElement)
  }

  /**
   * Simple XBRL instance queries. Shows XBRL instances, and introduces yaidom ScopedElemApi query API, as well as
   * Scala Collections. If you know ElemApi method `filterElemsOrSelf`, you basically know all of its methods.
   */
  test("testSimpleQueries") {
    val docParser = DocumentParserUsingSax.newInstance()

    val doc = docParser.parse(sampleXbrlInstanceFile)

    // Check that all gaap:AverageNumberEmployees facts have unit U-Pure.

    val gaapNs = "http://xasb.org/gaap"
    val avgNumEmployeesFacts = doc.documentElement.filterChildElems(withEName(gaapNs, "AverageNumberEmployees"))

    assertResult(7) {
      avgNumEmployeesFacts.size
    }
    assertResult(true) {
      avgNumEmployeesFacts.forall(fact => fact.attributeOption(EName("unitRef")).contains("U-Pure"))
    }

    // Check the unit itself, minding the default namespace

    val uPureUnit =
      doc.documentElement.getChildElem(e => e.resolvedName == EName(XbrliNs, "unit") && (e \@ EName("id")).contains("U-Pure"))

    assertResult("pure") {
      uPureUnit.getChildElem(withEName(XbrliNs, "measure")).text
    }
    // Mind the default namespace. Note the precision of yaidom and its namespace support that makes this easy.
    assertResult(EName(XbrliNs, "pure")) {
      uPureUnit.getChildElem(withEName(XbrliNs, "measure")).textAsResolvedQName
    }

    // Knowing the units are the same, the gaap:AverageNumberEmployees facts are uniquely identified by contexts.

    val avgNumEmployeesFactsByContext: Map[String, simple.Elem] = {
      // Method mapValues deprecated since Scala 2.13.0.
      avgNumEmployeesFacts.groupBy(_.attribute(EName("contextRef")))
        .map { case (ctxRef, facts) => ctxRef -> facts.head }.toMap
    }

    assertResult(Set("D-2003", "D-2004", "D-2005", "D-2007-BS1", "D-2007-BS2", "D-2006", "D-2007")) {
      avgNumEmployeesFactsByContext.keySet
    }
    assertResult("220") {
      avgNumEmployeesFactsByContext("D-2003").text
    }
  }

  /**
   * Simple XBRL instance queries for getting comments, CDATA sections etc. This shows that element implementations
   * such as simple elements not only offer uniform yaidom query APIs, but also offer methods to query for specific
   * nodes such as comments and CDATA text.
   */
  test("testSpecificNodeQueries") {
    // Now use DOM-based DocumentParser
    val docParser = DocumentParserUsingDom.newInstance()

    val doc = docParser.parse(sampleXbrlInstanceFile)

    // Top level comment, outside the document element

    assertResult("Created by Charles Hoffman, CPA, 2008-03-27") {
      doc.comments.map(_.text.trim).mkString
    }

    // All contexts have a comment

    assertResult(true) {
      val contexts = doc.documentElement.filterChildElems(withEName(XbrliNs, "context"))
      contexts forall (e => e.commentChildren.nonEmpty)
    }

    // The gaap:ManagementDiscussionAndAnalysisTextBlock fact (occurring once) has 4 CDATA text children
    // Be careful: the detection of CDATA text nodes may depend on the underlying XML parser and its configuration

    assertResult(true) {
      // Being lazy, and forgetting about the namespace
      val facts = doc.documentElement.filterChildElems(withLocalName("ManagementDiscussionAndAnalysisTextBlock"))
      facts.flatMap(e => e.textChildren.filter(_.isCData)).size >= 1
    }
  }

  /**
   * Checks use of recommended namespace prefixes (rule 2.1.5). Shows simple FRIS rules, and shows yaidom Scopes
   * and namespace support.
   */
  test("testUsedNamespacePrefixes") {
    // Now use StAX-based DocumentParser
    val docParser = DocumentParserUsingStax.newInstance()

    val doc = docParser.parse(sampleXbrlInstanceFile)

    // First check only root element has ("significant") namespace declarations

    assertResult(true) {
      val rootScope = doc.documentElement.scope

      doc.documentElement.findAllElemsOrSelf.forall(e => e.scope == rootScope)
    }

    // Check standard namespace prefixes (rule 2.1.5)

    val standardScope = Scope.from(
      "xbrli" -> "http://www.xbrl.org/2003/instance",
      "xlink" -> "http://www.w3.org/1999/xlink",
      "link" -> "http://www.xbrl.org/2003/linkbase",
      "xsi" -> "http://www.w3.org/2001/XMLSchema-instance",
      "iso4217" -> "http://www.xbrl.org/2003/iso4217")

    val standardPrefixes = standardScope.keySet
    val standardNamespaceUris = standardScope.inverse.keySet

    assertResult(true) {
      val subscope = doc.documentElement.scope.withoutDefaultNamespace filter {
        case (pref, ns) =>
          standardPrefixes.contains(pref) || standardNamespaceUris.contains(ns)
      }
      subscope.subScopeOf(standardScope)
    }

    // We could check rule 2.1.6 about recommended namespace prefixes in a similar fashion.
  }

  /**
   * Checks the absence of unused namespaces (rule 2.1.7). Shows more advanced yaidom namespace support.
   * Introduces yaidom indexed and resolved elements, sharing much of the same query API with simple elements.
   * Also shows how yaidom is useful despite its lack of schema type knowledge (although a yaidom facade wrapping
   * type-aware Saxon-EE trees is quite feasible).
   */
  test("testNoUnusedNamespaces") {
    // Now use DOM-LS-based DocumentParser
    val docParser = DocumentParserUsingDomLS.newInstance()

    val doc = docParser.parse(sampleXbrlInstanceFile)
    // Introducing indexed elements, offering the same query API and more
    val indexedDoc = indexed.Document(doc)

    val namespaceUrisDeclared = indexedDoc.documentElement.scope.inverse.keySet

    import NamespaceUtils._

    // Check that the used namespaces are almost exactly those declared in the root element (approximately rule 2.1.7)

    val companyNs = "http://www.example.com/company"

    assertResult(namespaceUrisDeclared.diff(Set(companyNs))) {
      findAllNamespaces(indexedDoc.documentElement, xbrliDocumentENameExtractor).diff(Set(XmlNs))
    }

    // Strip unused company namespace, and compare Scopes again

    // Indexed elements contain a Path relative to the root
    val paths = indexedDoc.documentElement.findAllElemsOrSelf.map(_.path)

    val editedIndexedRootElem =
      indexed.Elem(stripUnusedNamespaces(indexedDoc.documentElement, xbrliDocumentENameExtractor))

    assertResult(true) {
      paths forall { path =>
        val orgElem = indexedDoc.documentElement.getElemOrSelfByPath(path)
        val editedElem = editedIndexedRootElem.getElemOrSelfByPath(path)

        orgElem.scope.filterKeys(orgElem.scope.keySet - "company") == editedElem.scope
      }
    }

    // Now using resolved elements, which offer much of the same query API, we can compare the instances for equality

    val resolvedOrgElem = resolved.Elem.from(doc.documentElement)
    val resolvedEditedElem = resolved.Elem.from(editedIndexedRootElem.underlyingElem)

    // No need to manipulate whitespace before comparison in this case
    assertResult(resolvedOrgElem) {
      resolvedEditedElem
    }
  }

  /**
   * Checks the order of child elements (rule 2.1.10). Shows yaidom's extensibility, in that new element implementations
   * can easily be added. Also shows the SubtypeAwareElemApi query API trait, for more type-safe querying in custom
   * XML dialects (such as the XBRL instances dialect used here).
   */
  test("testOrderInXbrlInstance") {
    // Rule 2.1.10: the order of child elements must be as specified in this rule

    val remainingChildElems =
      xbrlInstance.findAllChildElems dropWhile {
        case e: SchemaRef => true
        case e => false
      } dropWhile {
        case e: LinkbaseRef => true
        case e => false
      } dropWhile {
        case e: RoleRef => true
        case e => false
      } dropWhile {
        case e: ArcroleRef => true
        case e => false
      } dropWhile {
        case e: XbrliContext => true
        case e => false
      } dropWhile {
        case e: XbrliUnit => true
        case e => false
      } dropWhile {
        case e: Fact => true
        case e => false
      } dropWhile {
        case e: FootnoteLink => true
        case e => false
      }

    assertResult(true) {
      remainingChildElems.isEmpty
    }
  }

  /**
   * Checks some context-related requirements (rule 2.4.2, for example). Shows how yaidom's extensibility helps in
   * supporting different XML dialects such as XBRL instances, which is to be preferred to raw XML element support.
   */
  test("testContextProperties") {
    // Rule 2.4.2: all contexts must be used. It is also checked that all context references are indeed correct.
    // Note how friendly the XBRL instance model is as compared to raw XML elements.

    val contextIds = xbrlInstance.allContextsById.keySet

    val usedContextIds = xbrlInstance.findAllItems.map(_.contextRef).toSet

    assertResult(true) {
      usedContextIds.subsetOf(contextIds)
    }
    // Some contexts are not used, namely I-2004, D-2007-LI-ALL and I-2003
    assertResult(Set("I-2004", "D-2007-LI-ALL", "I-2003")) {
      contextIds.diff(usedContextIds)
    }

    // Rule 2.4.1: no S-equal contexts should occur
    // Roughly, contexts are S-equal if they only differ in the attributes on the context elements
    // We can use "resolved" elements for the comparison, after some transformation (removing context attributes, for example)
    // This comparison is not exact, because there is no type information, but as an approximation it is not too bad

    val contextsBySEqualityGroup = xbrlInstance.allContexts.groupBy(e => transformContextForSEqualityComparison(e))

    assertResult(true) {
      contextsBySEqualityGroup.size == xbrlInstance.allContexts.size
    }
  }

  /**
   * Checks some fact-related requirements (rule 2.8.3, for example). Again shows yaidom's support for XML dialects.
   */
  test("testFactProperties") {
    // Rule 2.8.3: concepts are either top-level or nested in tuples, both not both

    val topLevelConceptNames = xbrlInstance.allTopLevelFactsByEName.keySet

    val nestedConceptNames = xbrlInstance.allTopLevelTuples.flatMap(_.findAllFacts).map(_.resolvedName).toSet

    assertResult(true) {
      topLevelConceptNames.intersect(nestedConceptNames).isEmpty
    }
    // Of course, the following holds
    assertResult(true) {
      xbrlInstance.findAllFacts.map(_.resolvedName).toSet == (topLevelConceptNames.union(nestedConceptNames))
    }
  }

  private def transformContextForSEqualityComparison(context: XbrliContext): resolved.Elem = {
    // Ignoring "normalization" of dates and QNames, as well as order of dimensions etc.
    val elem = context.indexedElem.underlyingElem.copy(attributes = Vector())
    resolved.Elem.from(elem).coalesceAndNormalizeAllText.removeAllInterElementWhitespace
  }
}
