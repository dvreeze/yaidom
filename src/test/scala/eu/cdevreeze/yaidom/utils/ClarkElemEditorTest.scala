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

package eu.cdevreeze.yaidom.utils

import java.{ util => jutil }

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import NamespaceUtils.pushUpPrefixedNamespaces
import NamespaceUtils.stripUnusedNamespaces
import NamespaceUtils.findAllENames
import NamespaceUtils.findAllNamespaces
import NamespaceUtils.findENamesInElementItself
import NamespaceUtils.findNamespacesInElementItself
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * ClarkElemEditor test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ClarkElemEditorTest extends FunSuite {

  private val docParser = DocumentParserUsingSax.newInstance

  test("testCreateXbrlInstance") {
    val instance = NamespaceUtils.pushUpPrefixedNamespaces(createInstance())

    val unusedScope = Scope.from("company" -> "http://www.example.com/company")

    assertResult(scope ++ unusedScope ++ fallbackScope.filterKeys(Set("xbrldi", "iso4217"))) {
      instance.scope
    }

    assertResult(Set(instance.scope)) {
      instance.findAllElemsOrSelf.map(_.scope).toSet
    }

    val uri = classOf[ClarkElemEditorTest].getResource("sample-xbrl-instance.xml").toURI

    val parsedInstance = docParser.parse(uri).documentElement

    val filteredInstance = parsedInstance.minusAttribute(QName("xsi:schemaLocation")) transformChildElemsToNodeSeq {
      case e if e.localName == "schemaRef" =>
        Vector(e)
      case e if e.localName == "context" && e.attributeOption(EName("id")) == Some("I-2007") =>
        Vector(e)
      case e if e.localName == "unit" && e.attributeOption(EName("id")) == Some("U-Monetary") =>
        Vector(e)
      case e if e.localName == "CashAndCashEquivalents" &&
        e.attributeOption(EName("contextRef")) == Some("I-2007") &&
        e.attributeOption(EName("unitRef")) == Some("U-Monetary") =>
        Vector(e)
      case e =>
        Vector()
    }

    assertResult(resolved.Elem(filteredInstance).removeAllInterElementWhitespace.coalesceAndNormalizeAllText) {
      resolved.Elem(instance).removeAllInterElementWhitespace.coalesceAndNormalizeAllText
    }
  }

  private def createContext(): simple.Elem = {
    import simple.Node._

    val editor = SimpleElemEditor.newInstanceUsingScopeAndDefaultPrefixGenerator(fallbackScope)

    val identifier =
      textElem(QName("identifier"), Vector(QName("scheme") -> "http://www.sec.gov/CIK"), scope, "1234567890")

    val dimScope = scope.filterKeys(Set("", "gaap")) ++ Scope.from("xbrldi" -> "http://xbrl.org/2006/xbrldi")

    // We can create the xbrldi:explicitMember elements with minimal scopes, and still have no prefixed
    // namespace undeclarations, thanks to the SimpleElemEditor.

    val segment =
      editor.wrap(emptyElem(QName("segment"), scope)).
        plusChildren(Vector(
          textElem(QName("xbrldi:explicitMember"), Vector(QName("dimension") -> "gaap:EntityAxis"), dimScope, "gaap:ABCCompanyDomain"),
          textElem(QName("xbrldi:explicitMember"), Vector(QName("dimension") -> "gaap:BusinessSegmentAxis"), dimScope, "gaap:ConsolidatedGroupDomain"),
          textElem(QName("xbrldi:explicitMember"), Vector(QName("dimension") -> "gaap:VerificationAxis"), dimScope, "gaap:UnqualifiedOpinionMember"),
          textElem(QName("xbrldi:explicitMember"), Vector(QName("dimension") -> "gaap:PremiseAxis"), dimScope, "gaap:ActualMember"),
          textElem(QName("xbrldi:explicitMember"), Vector(QName("dimension") -> "gaap:ReportDateAxis"), dimScope, "gaap:ReportedAsOfMarch182008Member"))).
        toElem

    // No prefixed namespace undeclarations

    assertResult(true) {
      segment.findAllElemsOrSelf.forall(_.scope.filterKeys(Set("", "xlink", "link", "gaap")) == scope)
    }

    assertResult(Set(fallbackScope.filterKeys(Set("xbrldi")) ++ scope)) {
      segment.findAllElems.map(_.scope).toSet
    }

    // Now pushing up prefixed namespace declarations

    assertResult(Set(fallbackScope.filterKeys(Set("xbrldi")) ++ scope)) {
      NamespaceUtils.pushUpPrefixedNamespaces(segment).findAllElemsOrSelf.map(_.scope).toSet
    }

    val entity =
      elem(QName("entity"), scope, Vector(identifier, segment))

    val period =
      emptyElem(QName("period"), scope).plusChild(textElem(QName("instant"), scope, "2007-12-31"))

    val context =
      editor.wrap(emptyElem(QName("context"), scope).plusAttribute(QName("id"), "I-2007")).
        plusChildren(Vector(entity, period)).
        toElem

    NamespaceUtils.pushUpPrefixedNamespaces(context)
  }

  private def createUnit(): simple.Elem = {
    import simple.Node._

    elem(QName("unit"), Vector(QName("id") -> "U-Monetary"), scope, Vector(
      textElem(QName("measure"), scope ++ fallbackScope.filterKeys(Set("iso4217")), "iso4217:USD")))
  }

  private def createFact(): simple.Elem = {
    import simple.Node._

    textElem(QName("gaap:CashAndCashEquivalents"), scope, "1000").
      plusAttribute(QName("id"), "Item-01").
      plusAttribute(QName("contextRef"), "I-2007").
      plusAttribute(QName("unitRef"), "U-Monetary").
      plusAttribute(QName("decimals"), "INF")
  }

  private def createInstance(): simple.Elem = {
    import simple.Node._

    val editor = SimpleElemEditor.newInstanceUsingScopeAndDefaultPrefixGenerator(fallbackScope)

    val sc = scope.filterKeys(Set("", "xlink", "link"))

    val schemaRef = emptyElem(QName("link:schemaRef"), sc).
      plusAttribute(QName("xlink:type"), "simple").
      plusAttribute(QName("xlink:href"), "gaap.xsd")

    val unusedScope = Scope.from("company" -> "http://www.example.com/company")

    val instance =
      editor.wrap(emptyElem(QName("xbrl"), scope.retainingDefaultNamespace ++ unusedScope)).
        plusChild(schemaRef).
        plusChild(createContext()).
        plusChild(createUnit()).
        plusChild(createFact()).
        toElem

    // The unused "company" namespace is not undeclared anywhere

    assertResult(true) {
      instance.findAllElemsOrSelf.forall(e => e.scope.filterKeys(Set("company")) == unusedScope)
    }
    assertResult(scope.retainingDefaultNamespace ++ unusedScope) {
      instance.scope
    }
    assertResult(Set(instance.scope)) {
      instance.findAllElemsOrSelf.map(e => e.scope.filterKeys(instance.scope.keySet)).toSet
    }

    instance
  }

  private val scope = Scope.from(
    "" -> "http://www.xbrl.org/2003/instance",
    "xlink" -> "http://www.w3.org/1999/xlink",
    "link" -> "http://www.xbrl.org/2003/linkbase",
    "gaap" -> "http://xasb.org/gaap")

  private val fallbackScope = Scope.from(
    "xbrli" -> "http://www.xbrl.org/2003/instance",
    "xlink" -> "http://www.w3.org/1999/xlink",
    "link" -> "http://www.xbrl.org/2003/linkbase",
    "iso4217" -> "http://www.xbrl.org/2003/iso4217",
    "xbrldi" -> "http://xbrl.org/2006/xbrldi")
}
