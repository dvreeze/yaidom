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

package eu.cdevreeze.yaidom.blogcode

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.parse.DocumentParserUsingStax
import eu.cdevreeze.yaidom.queryapi.HasENameApi.withEName
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.utils.DocumentENameExtractor
import eu.cdevreeze.yaidom.utils.NamespaceUtils
import eu.cdevreeze.yaidom.utils.SimpleTextENameExtractor
import eu.cdevreeze.yaidom.utils.TextENameExtractor

/**
 * Code of yaidom XBRL blog ("introducing yaidom, using XBRL examples"). The blog introduces yaidom in the context
 * of XBRL instances. The examples show implementations of some FRIS International rules, gradually introducing
 * more advanced yaidom element implementations.
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class BlogXbrlTest extends Suite {

  private val sampleXbrlInstanceFile: java.io.File =
    (new java.io.File(classOf[BlogXbrlTest].getResource("sample-Instance-Proof.xml").toURI))

  private val xbrliNs = "http://www.xbrl.org/2003/instance"
  private val linkNs = "http://www.xbrl.org/2003/linkbase"
  private val xlinkNs = "http://www.w3.org/1999/xlink"
  private val xbrldiNs = "http://xbrl.org/2006/xbrldi"
  private val xsiNs = "http://www.w3.org/2001/XMLSchema-instance"
  // Needs no namespace declaration:
  private val xmlNs = "http://www.w3.org/XML/1998/namespace"

  private val xbrliDocumentENameExtractor: DocumentENameExtractor = {
    // Not complete, but good enough for this example!

    new DocumentENameExtractor {

      def findElemTextENameExtractor(elem: indexed.Elem): Option[TextENameExtractor] = elem.resolvedName match {
        case EName(Some(xbrliNs), "measure") if elem.path.containsName(EName(xbrliNs, "unit")) =>
          Some(SimpleTextENameExtractor)
        case EName(Some(xbrldiNs), "explicitMember") =>
          Some(SimpleTextENameExtractor)
        case _ => None
      }

      def findAttributeValueENameExtractor(elem: indexed.Elem, attributeEName: EName): Option[TextENameExtractor] = elem.resolvedName match {
        case EName(Some(xbrldiNs), "explicitMember") if attributeEName == EName("dimension") =>
          Some(SimpleTextENameExtractor)
        case _ => None
      }
    }
  }

  /**
   * Simple XBRL instance queries.
   */
  @Test def testSimpleQueries(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    val doc = docParser.parse(sampleXbrlInstanceFile)

    // Check that all gaap:AverageNumberEmployees facts have unit U-Pure.

    val gaapNs = "http://xasb.org/gaap"
    val avgNumEmployeesFacts = doc.documentElement.filterChildElems(withEName(gaapNs, "AverageNumberEmployees"))

    assertResult(7) {
      avgNumEmployeesFacts.size
    }
    assertResult(true) {
      avgNumEmployeesFacts.forall(fact => fact.attributeOption(EName("unitRef")) == Some("U-Pure"))
    }

    // Check the unit itself, minding the default namespace

    val uPureUnit =
      doc.documentElement.getChildElem(e => e.resolvedName == EName(xbrliNs, "unit") && (e \@ EName("id")) == Some("U-Pure"))

    assertResult("pure") {
      uPureUnit.getChildElem(withEName(xbrliNs, "measure")).text
    }
    // Mind the default namespace. Note the precision of yaidom and its namespace support that makes this easy.
    assertResult(EName(xbrliNs, "pure")) {
      uPureUnit.getChildElem(withEName(xbrliNs, "measure")).textAsResolvedQName
    }

    // Knowing the units are the same, the gaap:AverageNumberEmployees facts are uniquely identified by contexts.

    val avgNumEmployeesFactsByContext: Map[String, simple.Elem] = {
      avgNumEmployeesFacts.groupBy(_.attribute(EName("contextRef"))).mapValues(_.head)
    }

    assertResult(Set("D-2003", "D-2004", "D-2005", "D-2007-BS1", "D-2007-BS2", "D-2006", "D-2007")) {
      avgNumEmployeesFactsByContext.keySet
    }
    assertResult("220") {
      avgNumEmployeesFactsByContext("D-2003").text
    }
  }

  /**
   * Checks use of recommended namespace prefixes (rule 2.1.5).
   */
  @Test def testUsedNamespacePrefixes(): Unit = {
    // Now use StAX-based DocumentParser
    val docParser = DocumentParserUsingStax.newInstance

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
   * Checks the absence of unused namespaces (rule 2.1.7).
   */
  @Test def testNoUnusedNamespaces(): Unit = {
    // Now use DOM-based DocumentParser
    val docParser = DocumentParserUsingDom.newInstance

    val doc = docParser.parse(sampleXbrlInstanceFile)
    // Introducing indexed elements, offering the same query API and more
    val indexedDoc = indexed.Document(doc)

    val namespaceUrisDeclared = indexedDoc.documentElement.scope.inverse.keySet

    import NamespaceUtils._

    // Check that the used namespaces are almost exactly those declared in the root element (approximately rule 2.1.7)

    val companyNs = "http://www.example.com/company"

    assertResult(namespaceUrisDeclared.diff(Set(companyNs))) {
      findAllNamespaces(indexedDoc.documentElement, xbrliDocumentENameExtractor).diff(Set(xmlNs))
    }

    // Strip unused company namespace, and compare Scopes agains

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

    val resolvedOrgElem = resolved.Elem(doc.documentElement)
    val resolvedEditedElem = resolved.Elem(editedIndexedRootElem.elem)

    // No need to manipulate whitespace before comparison in this case
    assertResult(resolvedOrgElem) {
      resolvedEditedElem
    }
  }
}
