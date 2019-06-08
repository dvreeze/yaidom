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

import java.time.LocalDate

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.print.DocumentPrinterUsingDom
import eu.cdevreeze.yaidom.queryapi.ClarkElemApi.withEName
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple
import eu.cdevreeze.yaidom.utils.NamespaceUtils
import org.scalatest.funsuite.AnyFunSuite

/**
 * Code of yaidom XBRL blog ("XBRL, Scala and yaidom"). The blog introduces yaidom in the context
 * of XBRL instances. The examples show alternative "implementations" of some XBRL formulas.
 *
 * The examples are from www.xbrlsite.com, by Charles Hoffman.
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
class Blog2XbrlTest extends AnyFunSuite {

  private val sampleXbrlInstanceFile: java.io.File =
    (new java.io.File(classOf[Blog2XbrlTest].getResource("company-instance.xml").toURI))

  private val XbrliNs = "http://www.xbrl.org/2003/instance"
  private val XbrldiNs = "http://xbrl.org/2006/xbrldi"
  private val LinkNs = "http://www.xbrl.org/2003/linkbase"

  private val GaapNs = "http://xasb.org/gaap"
  private val FrmNs = "http://www.xbrlsite.com/Schemas/frm"
  private val CompanyNs = "http://www.ABCCompany.com/company"

  /**
   * Simple XBRL instance queries. Shows XBRL instances, and introduces yaidom ScopedElemApi query API, as well as
   * Scala Collections. If you know ElemApi method `filterElemsOrSelf`, you basically know all of its methods.
   */
  test("testSimpleInstanceQueries") {
    // Let variable "doc" in principle be a document of any yaidom document type (here it is "simple")

    val doc = docParser.parse(sampleXbrlInstanceFile)

    val docElem = doc.documentElement

    // Check that all gaap:AverageNumberEmployees facts have unit U-Pure.

    val xbrliNs = "http://www.xbrl.org/2003/instance"
    val gaapNs = "http://xasb.org/gaap"

    val avgNumEmployeesFacts =
      docElem.filterChildElems(withEName(gaapNs, "AverageNumberEmployees"))

    assertResult(10) {
      avgNumEmployeesFacts.size
    }
    assertResult(true) {
      avgNumEmployeesFacts.forall(fact => fact.attributeOption(EName("unitRef")).contains("U-Pure"))
    }

    val onlyUPure =
      avgNumEmployeesFacts.forall(_.attributeOption(EName("unitRef")).contains("U-Pure"))

    assertResult(true) {
      onlyUPure
    }

    // Check the unit itself, minding the default namespace

    val uPureUnit =
      docElem.getChildElem(e => e.resolvedName == EName(xbrliNs, "unit") && (e \@ EName("id")).contains("U-Pure"))

    assertResult("pure") {
      uPureUnit.getChildElem(withEName(XbrliNs, "measure")).text
    }
    // Mind the default namespace. Note the precision of yaidom and its namespace support that makes this easy.
    assertResult(EName(XbrliNs, "pure")) {
      uPureUnit.getChildElem(withEName(XbrliNs, "measure")).textAsResolvedQName
    }

    // Now we get the measure element text, as QName, resolving it to an EName (expanded name)
    assertResult(EName(xbrliNs, "pure")) {
      uPureUnit.getChildElem(withEName(xbrliNs, "measure")).textAsResolvedQName
    }

    // Having the same unit, the gaap:AverageNumberEmployees facts are uniquely identified by contexts.

    // Method mapValues deprecated since Scala 2.13.0.
    val avgNumEmployeesFactsByContext =
      avgNumEmployeesFacts.groupBy(_.attribute(EName("contextRef")))
        .map { case (ctxRef, facts) => ctxRef -> facts.head }

    assertResult(
      Set("D-2006", "D-2007", "D-2008", "D-2009", "D-2010", "D-2010-BS1", "D-2010-BS2", "D-2010-CON", "D-2010-E", "D-2010-ALL")) {
      avgNumEmployeesFactsByContext.keySet
    }
    assertResult("220") {
      avgNumEmployeesFactsByContext("D-2006").text
    }
  }

  /**
   * XBRL instance queries creating an alternative representation, emphasizing aspects. Now we use indexed elements
   * (needed for location aspect), with the same query API.
   *
   * Try to do this using XSLT instead (or XQuery)...
   *
   * See http://xbrlreview.blogspot.nl/p/json-financialreport-network-httpwww.html for a similar representation,
   * but in JSON.
   */
  test("testFactAspectQueries") {
    val fact =
      (idoc.documentElement findChildElem { e =>
        e.resolvedName == EName(GaapNs, "AverageNumberEmployees") &&
          e.attributeOption(EName("contextRef")).contains("D-2007")
      }).head

    assertResult(240) {
      fact.text.toInt
    }

    assertResult(EName(GaapNs, "AverageNumberEmployees")) {
      conceptAspect(fact)
    }
    assertResult(Path.Empty) {
      locationAspect(fact)
    }
    assertResult(Some(("http://regulator.gov/id", "1234567890"))) {
      entityIdentifierAspectOption(fact)
    }
    // Note the precise namespace support of yaidom
    assertResult(Map(
      EName(FrmNs, "ReportingScenarioAxis") -> EName(FrmNs, "ActualMember"),
      EName(FrmNs, "ReportDateAxis") -> EName(CompanyNs, "ReportedAsOfMarch182011Member"))) {

      explicitDimensionAspects(fact)
    }

    // Build alternative representation of the instance

    val altInstanceElem = makeAlternativeInstance(idoc)

    val docPrinter = DocumentPrinterUsingDom.newInstance
    val altInstanceXmlString =
      docPrinter.print(altInstanceElem.prettify(2))

    if (System.getProperty("Blog2XbrlTest.debug", "false").toBoolean) println(altInstanceXmlString)

    val topLevelFacts =
      idoc.documentElement.filterChildElems(e => !Set(XbrliNs, LinkNs).contains(e.resolvedName.namespaceUriOption.getOrElse("")))
    val facts = topLevelFacts.flatMap(_.findAllElemsOrSelf)

    assertResult(facts.map(_.resolvedName).toSet) {
      altInstanceElem.filterElems(withEName(None, "conceptAspect")).map(e => EName.parse(e.text)).toSet
    }
    assertResult(Set(Path.Empty)) {
      altInstanceElem.filterElems(withEName(None, "locationAspect")).map(e => Path.fromResolvedCanonicalXPath(e.text)).toSet
    }
    assertResult(Set("http://regulator.gov/id")) {
      altInstanceElem.filterElems(withEName(None, "entityIdentifierAspect")).flatMap(e => e.attributeOption(EName("scheme"))).toSet
    }
    assertResult(Set("1234567890")) {
      altInstanceElem.filterElems(withEName(None, "entityIdentifierAspect")).map(_.text).toSet
    }
    assertResult(Set(EName(XbrliNs, "instant"), EName(XbrliNs, "startDate"), EName(XbrliNs, "endDate"))) {
      altInstanceElem.filterElems(withEName(None, "periodAspect")).flatMap(_.findAllChildElems.map(_.resolvedName)).toSet
    }
    assertResult(Map(EName(FrmNs, "ReportingScenarioAxis") -> EName(FrmNs, "ReportingScenariosAllMember"))) {
      val dims = altInstanceElem.filterElems(withEName(None, "dimensionAspect"))

      dims.map(e => (EName.parse(e.attribute(EName("dimension"))) -> EName.parse(e.text))).toMap filter { kv =>
        kv._1 == EName(FrmNs, "ReportingScenarioAxis") && kv._2 == EName(FrmNs, "ReportingScenariosAllMember")
      }
    }
  }

  /**
   * Simulating an assertion, using the aspect querying functions. The simulation helps understand the assertion.
   *
   * The assertion is <code>$v:VARIABLE_BalanceStart + $v:VARIABLE_Change = $v:VARIABLE_BalanceEnd</code> (the first occurrence).
   *
   * See http://www.xbrlsite.com/DigitalFinancialReporting/ComprehensiveExample/2011-07-15/gaap-formula.xml.
   */
  test("testSimulatedAssertion1") {
    val topLevelFacts =
      idocElem.filterChildElems(e =>
        !Set(XbrliNs, LinkNs).contains(e.resolvedName.namespaceUriOption.getOrElse("")))
    val facts = topLevelFacts.flatMap(_.findAllElemsOrSelf)

    val balanceFacts =
      facts.filter(withEName(GaapNs, "CashAndCashEquivalentsPerCashFlowStatement"))

    val changeFacts =
      facts.filter(withEName(GaapNs, "CashFlowNet"))

    // Implicit filtering, to filter the cartesian product of 3 fact value spaces

    def mustBeEvaluated(
      balanceStartFact: indexed.Elem,
      changeFact: indexed.Elem,
      balanceEndFact: indexed.Elem): Boolean = {

      // Compare on so-called uncovered aspects, so all ones except concept and period

      val currFacts = List(balanceStartFact, changeFact, balanceEndFact)

      val dimensions = currFacts.flatMap(e => explicitDimensionAspects(e).keySet).toSet

      currFacts.map(e => locationAspect(e)).distinct.size == 1 &&
        currFacts.map(e => entityIdentifierAspectOption(e)).distinct.size == 1 &&
        dimensions.forall(dim =>
          currFacts.map(e => explicitDimensionAspectOption(e, dim)).toSet.size == 1) &&
        currFacts.map(e => unitAspectOption(e)).distinct.size == 1 && {
        // Instant-duration
        // The comparison is naive, but still verbose

        import LocalDate.parse

        val balanceStartInstantOption =
          periodAspectOption(balanceStartFact).flatMap(
            _.findElem(withEName(XbrliNs, "instant"))).map(e => parse(e.text))
        val balanceEndInstantOption =
          periodAspectOption(balanceEndFact).flatMap(
            _.findElem(withEName(XbrliNs, "instant"))).map(e => parse(e.text))
        val changeStartOption =
          periodAspectOption(changeFact).flatMap(
            _.findElem(withEName(XbrliNs, "startDate"))).map(e => parse(e.text))
        val changeEndOption =
          periodAspectOption(changeFact).flatMap(
            _.findElem(withEName(XbrliNs, "endDate"))).map(e => parse(e.text))

        balanceStartInstantOption.isDefined && balanceEndInstantOption.isDefined &&
          changeStartOption.isDefined && changeEndOption.isDefined && {

          val balanceStart = balanceStartInstantOption.get
          val balanceEnd = balanceEndInstantOption.get
          val changeStart = changeStartOption.get
          val changeEnd = changeEndOption.get

          (balanceStart == changeStart || balanceStart.plusDays(1) == changeStart) &&
            (balanceEnd == changeEnd)
        }
      }
    }

    // The assertion test itself

    def performAssertionTest(
      balanceStartFact: indexed.Elem,
      changeFact: indexed.Elem,
      balanceEndFact: indexed.Elem): Blog2XbrlTest.EvaluationResult = {

      // Here we recognize the XPath expression shown earlier
      val result =
        balanceStartFact.text.toInt + changeFact.text.toInt == balanceEndFact.text.toInt

      Blog2XbrlTest.EvaluationResult(
        Map(
          "startBalance" -> balanceStartFact,
          "change" -> changeFact,
          "endBalance" -> balanceEndFact), result)
    }

    // Executing the assertion

    val evalResults =
      for {
        startBalance <- balanceFacts
        change <- changeFacts
        endBalance <- balanceFacts
        if mustBeEvaluated(startBalance, change, endBalance)
      } yield {
        performAssertionTest(startBalance, change, endBalance)
      }

    assertResult(2) {
      evalResults.size
    }
    assertResult(true) {
      evalResults.forall(_.result)
    }
    assertResult(Set(
      Map("startBalance" -> 1000, "change" -> -1000, "endBalance" -> 0),
      Map("startBalance" -> -3000, "change" -> 4000, "endBalance" -> 1000))) {

      // Method mapValues deprecated since Scala 2.13.0.
      evalResults.map(_.facts.map { case (k, facts) => k -> facts.text.toInt }.toMap).toSet
    }
  }

  private val docParser = DocumentParserUsingSax.newInstance

  private val idoc = indexed.Document(docParser.parse(sampleXbrlInstanceFile))
  private val idocElem = idoc.documentElement

  // Method mapValues deprecated since Scala 2.13.0.
  val contextsById: Map[String, indexed.Elem] =
    idocElem.filterChildElems(withEName(XbrliNs, "context"))
      .groupBy(_.attribute(EName("id")))
      .map { case (id, ctxs) => id -> ctxs.head }
      .toMap

  // Method mapValues deprecated since Scala 2.13.0.
  val unitsById: Map[String, indexed.Elem] =
    idocElem.filterChildElems(withEName(XbrliNs, "unit"))
      .groupBy(_.attribute(EName("id")))
      .map { case (id, uns) => id -> uns.head }
      .toMap

  // See http://www.xbrl.org/Specification/variables/REC-2009-06-22/.

  def conceptAspect(fact: indexed.Elem): EName = fact.resolvedName

  // Yaidom Paths wijzen een element binnen een element tree aan.

  def locationAspect(fact: indexed.Elem): Path =
    fact.path.parentPathOption.getOrElse(Path.Empty)

  def entityIdentifierAspectOption(fact: indexed.Elem): Option[(String, String)] = {
    val contextOption =
      fact.attributeOption(EName("contextRef")).map(id => contextsById(id))

    val identifierOption =
      contextOption.flatMap(_.findElem(withEName(XbrliNs, "identifier")))
    val schemeOption =
      identifierOption.flatMap(_.attributeOption(EName("scheme")))
    val identifierValueOption =
      identifierOption.map(_.text)

    for {
      scheme <- schemeOption
      identifierValue <- identifierValueOption
    } yield (scheme, identifierValue)
  }

  def periodAspectOption(fact: indexed.Elem): Option[simple.Elem] = {
    val contextOption =
      fact.attributeOption(EName("contextRef")).map(id => contextsById(id))

    val periodOption =
      contextOption.flatMap(_.findElem(withEName(XbrliNs, "period")))
    periodOption.map(_.underlyingElem)
  }

  // Forgetting about complete segment, non-XDT segment, complete scenario and
  // non-XDT scenario for now. Also ignoring typed dimensions.

  def explicitDimensionAspects(fact: indexed.Elem): Map[EName, EName] = {
    val contextOption =
      fact.attributeOption(EName("contextRef")).map(id => contextsById(id))

    val memberElems =
      contextOption.toVector.flatMap(_.filterElems(withEName(XbrldiNs, "explicitMember")))
    memberElems.map(e =>
      (e.attributeAsResolvedQName(EName("dimension")) -> e.textAsResolvedQName)).toMap
  }

  // Convenience method

  def explicitDimensionAspectOption(
    fact: indexed.Elem,
    dimension: EName): Option[EName] = {

    // Method filterKeys deprecated since Scala 2.13.0.
    explicitDimensionAspects(fact).filter { case (dim, mem) => Set(dimension).contains(dim) }.headOption.map(_._2)
  }

  def unitAspectOption(fact: indexed.Elem): Option[simple.Elem] = {
    val unitOption =
      fact.attributeOption(EName("unitRef")).map(id => unitsById(id))
    unitOption.map(_.underlyingElem)
  }

  // Compare aspects, naively, and without knowledge about XML Schema types.
  // Period aspect comparisons are more tricky than this naive implementation suggests.
  // Use equality on the results of the functions below for (period and unit) aspect
  // comparisons.

  def comparablePeriodAspectOption(fact: indexed.Elem): Option[Set[resolved.Elem]] = {
    periodAspectOption(fact).map(e =>
      e.findAllChildElems.map(che => resolved.Elem.from(che).removeAllInterElementWhitespace).toSet)
  }

  def comparableUnitAspectOption(fact: indexed.Elem): Option[Set[resolved.Elem]] = {
    unitAspectOption(fact).map(e =>
      e.findAllChildElems.map(che => resolved.Elem.from(che).removeAllInterElementWhitespace).toSet)
  }

  // Create alternative XBRL instance representation

  def makeAlternativeInstance(indexedDoc: indexed.Document): simple.Elem = {
    import simple.Node._

    val sc = Scope.Empty

    val topLevelFacts =
      indexedDoc.documentElement.filterChildElems(e => !Set(XbrliNs, LinkNs).contains(e.resolvedName.namespaceUriOption.getOrElse("")))
    val facts = topLevelFacts.flatMap(_.findAllElemsOrSelf)

    val altInstanceFacts =
      facts map { fact =>
        val aspectsElem =
          emptyElem(QName("aspects"), sc).
            plusChild(textElem(QName("conceptAspect"), sc, conceptAspect(fact).toString)).
            plusChild(textElem(QName("locationAspect"), sc, locationAspect(fact).toResolvedCanonicalXPath)).
            plusChildOption(entityIdentifierAspectOption(fact).map(kv => textElem(QName("entityIdentifierAspect"), Vector(QName("scheme") -> kv._1), sc, kv._2.toString))).
            plusChildOption(periodAspectOption(fact).map(p => elem(QName("periodAspect"), sc, p.findAllChildElems))).
            plusChildren(explicitDimensionAspects(fact).toVector.map(kv => textElem(QName("dimensionAspect"), Vector(QName("dimension") -> kv._1.toString), sc, kv._2.toString))).
            plusChildOption(unitAspectOption(fact).map(u => elem(QName("unitAspect"), sc, u.findAllChildElems)))

        val factElem =
          emptyElem(QName("fact"), sc).plusChild(aspectsElem).plusChild(textElem(QName("factValue"), sc, fact.text))
        factElem
      }
    val altInstanceElem =
      elem(QName("xbrlInstance"), sc, altInstanceFacts)
    NamespaceUtils.pushUpPrefixedNamespaces(altInstanceElem)
  }
}

object Blog2XbrlTest {

  final case class EvaluationResult(val facts: Map[String, indexed.Elem], val result: Boolean) {

    override def toString: String = {
      // Method mapValues deprecated since Scala 2.13.0.
      s"EvaluationResult(result: $result, facts: ${facts.map { case (k, fact) => k -> fact.underlyingElem }})"
    }
  }

}
