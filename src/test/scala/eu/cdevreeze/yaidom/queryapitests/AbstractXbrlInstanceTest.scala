/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.queryapitests

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import AbstractXbrlInstanceTestSupport.XbrliNs
import AbstractXbrlInstanceTestSupport.XmlNs
import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDomLS
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withLocalName
import eu.cdevreeze.yaidom.utils.DocumentENameExtractor
import eu.cdevreeze.yaidom.utils.NamespaceUtils
import eu.cdevreeze.yaidom.utils.SimpleTextENameExtractor
import eu.cdevreeze.yaidom.utils.TextENameExtractor

/**
 * Code showing yaidom XBRL instance dialect support, using type classes for/inside backed elements.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractXbrlInstanceTest extends Suite with AbstractXbrlInstanceTestSupport {

  protected val sampleXbrlInstanceFile: java.io.File =
    (new java.io.File(classOf[AbstractXbrlInstanceTest].getResource("sample-Instance-Proof.xml").toURI))

  private val xbrliDocumentENameExtractor: DocumentENameExtractor = {
    // Not complete, but good enough for this example!

    new DocumentENameExtractor {

      def findElemTextENameExtractor(elem: yaidom.indexed.Elem): Option[TextENameExtractor] = elem.resolvedName match {
        case EName(Some(xbrliNs), "measure") if elem.path.containsName(EName(xbrliNs, "unit")) =>
          Some(SimpleTextENameExtractor)
        case EName(Some(xbrldiNs), "explicitMember") =>
          Some(SimpleTextENameExtractor)
        case _ => None
      }

      def findAttributeValueENameExtractor(elem: yaidom.indexed.Elem, attributeEName: EName): Option[TextENameExtractor] = elem.resolvedName match {
        case EName(Some(xbrldiNs), "explicitMember") if attributeEName == EName("dimension") =>
          Some(SimpleTextENameExtractor)
        case _ => None
      }
    }
  }

  protected def elemWithApi: ElemWithApi

  private val xbrlInstance: XbrlInstance = {
    val result = XbrlInstance(elemWithApi)
    result
  }

  /**
   * Simple XBRL instance queries.
   */
  @Test def testSimpleQueries(): Unit = {
    // Check that all gaap:AverageNumberEmployees facts have unit U-Pure.

    val gaapNs = "http://xasb.org/gaap"
    val avgNumEmployeesFacts = xbrlInstance.filterTopLevelItems(withEName(gaapNs, "AverageNumberEmployees"))

    assertResult(7) {
      avgNumEmployeesFacts.size
    }
    assertResult(true) {
      avgNumEmployeesFacts.forall(_.unitRefOption == Some("U-Pure"))
    }

    // Check the unit itself, minding the default namespace

    val uPureUnit = xbrlInstance.allUnitsById("U-Pure")

    // Mind the default namespace. Note the precision of yaidom and its namespace support that makes this easy.
    assertResult(EName(XbrliNs, "pure")) {
      uPureUnit.measures.head
    }

    // Knowing the units are the same, the gaap:AverageNumberEmployees facts are uniquely identified by contexts.

    val avgNumEmployeesFactsByContext: Map[String, ItemFact] = {
      avgNumEmployeesFacts.groupBy(_.contextRef).mapValues(_.head)
    }

    assertResult(Set("D-2003", "D-2004", "D-2005", "D-2007-BS1", "D-2007-BS2", "D-2006", "D-2007")) {
      avgNumEmployeesFactsByContext.keySet
    }
    assertResult("220") {
      avgNumEmployeesFactsByContext("D-2003").text
    }
  }

  /**
   * Simple XBRL instance queries for getting comments, CDATA sections etc.
   */
  @Test def testSpecificNodeQueries(): Unit = {
    // All contexts have a comment

    assertResult(true) {
      val contexts = xbrlInstance.allContextsById.values.toVector
      contexts forall (e => !e.toSimpleElem.commentChildren.isEmpty)
    }

    // The gaap:ManagementDiscussionAndAnalysisTextBlock fact (occurring once) has 4 CDATA text children
    // Be careful: the detection of CDATA text nodes may depend on the underlying XML parser and its configuration

    if (!xbrlInstance.backingElem.elem.isInstanceOf[net.sf.saxon.om.NodeInfo]) {
      assertResult(true) {
        // Being lazy, and forgetting about the namespace
        val facts = xbrlInstance.filterTopLevelFacts(withLocalName("ManagementDiscussionAndAnalysisTextBlock"))
        facts.flatMap(e => e.toSimpleElem.textChildren.filter(_.isCData)).size >= 1
      }
    }
  }

  /**
   * Checks use of recommended namespace prefixes (rule 2.1.5). Shows simple FRIS rules, and shows yaidom Scopes
   * and namespace support.
   */
  @Test def testUsedNamespacePrefixes(): Unit = {
    // First check only root element has ("significant") namespace declarations

    assertResult(true) {
      val rootScope = xbrlInstance.scope

      // Converting to simple element first, because repeated scope computation can be expensive (e.g. DOM).
      xbrlInstance.toSimpleElem.findAllElemsOrSelf.forall(e => e.scope == rootScope)
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
      val subscope = xbrlInstance.scope.withoutDefaultNamespace filter {
        case (pref, ns) =>
          standardPrefixes.contains(pref) || standardNamespaceUris.contains(ns)
      }
      subscope.subScopeOf(standardScope)
    }

    // We could check rule 2.1.6 about recommended namespace prefixes in a similar fashion.
  }

  /**
   * Checks the absence of unused namespaces (rule 2.1.7). Shows more advanced yaidom namespace support.
   */
  @Test def testNoUnusedNamespaces(): Unit = {
    // Now using indexed documents for finding unused namespaces.
    val indexedElem = yaidom.indexed.Elem(xbrlInstance.toSimpleElem)

    val namespaceUrisDeclared = indexedElem.scope.inverse.keySet

    import NamespaceUtils._

    // Check that the used namespaces are almost exactly those declared in the root element (approximately rule 2.1.7)

    val companyNs = "http://www.example.com/company"

    assertResult(namespaceUrisDeclared.diff(Set(companyNs))) {
      findAllNamespaces(indexedElem, xbrliDocumentENameExtractor).diff(Set(XmlNs))
    }

    // Strip unused company namespace, and compare Scopes again

    // Indexed elements contain a Path relative to the root
    val paths = indexedElem.findAllElemsOrSelf.map(_.path)

    val editedIndexedRootElem =
      yaidom.indexed.Elem(stripUnusedNamespaces(indexedElem, xbrliDocumentENameExtractor))

    assertResult(true) {
      paths forall { path =>
        val orgElem = indexedElem.getElemOrSelfByPath(path)
        val editedElem = editedIndexedRootElem.getElemOrSelfByPath(path)

        orgElem.scope.filterKeys(orgElem.scope.keySet - "company") == editedElem.scope
      }
    }

    // Now using resolved elements, which offer much of the same query API, we can compare the instances for equality

    val resolvedOrgElem = yaidom.resolved.Elem(indexedElem.elem)
    val resolvedEditedElem = yaidom.resolved.Elem(editedIndexedRootElem.elem)

    // No need to manipulate whitespace before comparison in this case
    assertResult(resolvedOrgElem) {
      resolvedEditedElem
    }
  }

  /**
   * Checks the order of child elements (rule 2.1.10).
   */
  @Test def testOrderInXbrlInstance(): Unit = {
    // Rule 2.1.10: the order of child elements must be as specified in this rule

    val remainingChildElems =
      xbrlInstance.findAllChildElems dropWhile {
        case e: SchemaRef => true
        case e            => false
      } dropWhile {
        case e: LinkbaseRef => true
        case e              => false
      } dropWhile {
        case e: RoleRef => true
        case e          => false
      } dropWhile {
        case e: ArcroleRef => true
        case e             => false
      } dropWhile {
        case e: XbrliContext => true
        case e               => false
      } dropWhile {
        case e: XbrliUnit => true
        case e            => false
      } dropWhile {
        case e: Fact => true
        case e       => false
      } dropWhile {
        case e: FootnoteLink => true
        case e               => false
      }

    assertResult(true) {
      remainingChildElems.isEmpty
    }
  }

  /**
   * Checks some context-related requirements (rule 2.4.2, for example).
   */
  @Test def testContextProperties(): Unit = {
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
   * Checks some fact-related requirements (rule 2.8.3, for example).
   */
  @Test def testFactProperties(): Unit = {
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

  private def transformContextForSEqualityComparison(context: XbrliContext): yaidom.resolved.Elem = {
    // Ignoring "normalization" of dates and QNames, as well as order of dimensions etc.
    val elem = context.toSimpleElem.copy(attributes = Vector())
    yaidom.resolved.Elem(elem).coalesceAndNormalizeAllText.removeAllInterElementWhitespace
  }
}
