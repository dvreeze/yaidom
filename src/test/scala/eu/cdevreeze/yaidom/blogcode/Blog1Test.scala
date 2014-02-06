/*
 * Copyright 2011 Chris de Vreeze
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

package eu.cdevreeze.yaidom
package blogcode

import java.{ util => jutil, io => jio }
import javax.xml.parsers.DocumentBuilderFactory
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import Blog1Test._

/**
 * Code of yaidom blog 1 ("yaidom querying"). The blog uses examples from http://xbrl.squarespace.com/. All credits for
 * the examples go to Charles Hoffman (the "father of XBRL").
 *
 * The blog should show yaidom's strengths (such as leveraging Scala Collections, and namespace handling),
 * and be accessible and interesting to the readers. So it should respect the limited time of the readers.
 *
 * It is assumed that the reader knows the basics of XML (including namespaces and XML Schema), knows a bit of Scala
 * (in particular the Scala Collections API), and has some experience with Java XML processing (in particular JAXP).
 * No prior XBRL experience is assumed.
 *
 * The (code in this) blog shows some yaidom queries, using different element representations. It also shows namespace handling
 * in yaidom, and XML comparisons based on "resolved" elements.
 *
 * The queries retrieve facts in an XBRL instance, or perform consistency checks.
 *
 * Before showing any code, introduce XBRL and XBRL instances briefly, using the HelloWorld.xml instance.
 *
 * Note: blog 2 will treat transformations, and blog 3 will cover some advanced concepts, such as configuring yaidom.
 *
 * In the blog, first introduce XBRL briefly, and introduce yaidom (including its namespaces support).
 * Mention that yaidom respects XML namespaces, leverages Scala and its Collections API, and leverages JAXP.
 * Also mention that one-size-fits-all for element representations is not how yaidom looks at XML processing,
 * although the element query API should be as much as possible the same across element representations.
 *
 * Yaidom's namespace support and multiple element representations sharing the same element query API make yaidom
 * unique as a Scala XML library. As for namespaces, the article http://www.lenzconsulting.com/namespaces/ can be
 * illustrated by yaidom examples that tell the same story (since yaidom distinguishes between qualified names and expanded
 * names, just like the article does).
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class Blog1Test extends Suite {

  /**
   * Showing trivial queries for child elements, descendant elements, or descendant-or-self elements.
   *
   * Before treating this part, a brief introduction is needed in the blog. Who is the assumed reader? What is yaidom and
   * why was it developed? What is XBRL, very briefly, to explain the examples? Next ENames and QNames need to be explained
   * (referring to http://www.lenzconsulting.com/namespaces/).
   */
  @Test def testQueryXbrlInstance(): Unit = {
    val docParser = parse.DocumentParserUsingDom.newInstance

    val doc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xml").toURI)

    // Query "unit" child elements, using the local name

    val unitElems = doc.documentElement.filterChildElems(e => e.localName == "unit")

    assertResult(1) {
      unitElems.size
    }

    // Query "context" child elements (filterChildElems or "\"), using the local name

    val contextElems = doc.documentElement.filterChildElems(e => e.localName == "context")

    assertResult(2) {
      contextElems.size
    }

    assertResult(contextElems) {
      doc.documentElement \ (e => e.localName == "context")
    }

    // Query "context" descendant elements (filterElems), using the local name. The same elements are returned.

    assertResult(contextElems) {
      doc.documentElement.filterElems(e => e.localName == "context")
    }

    // Query "context" descendant-or-self elements (filterElemsOrSelf or "\\"), using the local name. Again the same elements are returned.

    assertResult(contextElems) {
      doc.documentElement.filterElemsOrSelf(e => e.localName == "context")
    }

    assertResult(contextElems) {
      doc.documentElement \\ (e => e.localName == "context")
    }

    // Query "context" child elements, using the expanded name

    assertResult(contextElems) {
      doc.documentElement.filterChildElems(e => e.resolvedName == EName(xbrliNamespace, "context"))
    }

    // Query facts in the HelloWorld namespace

    val factElems = doc.documentElement filterChildElems (e => e.resolvedName.namespaceUriOption == Some(helloWorldNamespace))

    assertResult(12) {
      factElems.size
    }

    assertResult(factElems) {
      doc.documentElement \ (e => e.resolvedName.namespaceUriOption == Some(helloWorldNamespace))
    }

    assertResult(Set(
      "Land", "BuildingsNet", "FurnitureAndFixturesNet", "ComputerEquipmentNet", "OtherPropertyPlantAndEquipmentNet", "PropertyPlantAndEquipmentNet")) {

      factElems.map(e => e.localName).toSet
    }
  }

  /**
   * Somewhat more interesting queries on schema document and XBRL instance document.
   *
   * Before treating this part, XBRL contexts, units and facts need to be explained briefly, as well as taxonomy schemas.
   * A query (on the schema document, as XML) is introduced, and this query turns out to be independent of element representation.
   */
  @Test def testCheckInstanceAgainstSchema(): Unit = {
    // This time using a SAX parser under the hood
    val docParser = parse.DocumentParserUsingSax.newInstance

    val instanceDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xml").toURI)
    val schemaDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI)

    import ElemApi._

    // First query the schema for item declarations

    val tns = schemaDoc.documentElement.attribute(EName("targetNamespace"))

    val itemDecls: immutable.IndexedSeq[Elem] = findAllItemDeclarations(schemaDoc.documentElement)

    val itemDeclsByTargetENames: Map[EName, Elem] = {
      itemDecls.map(e => (EName(tns, e.attribute(EName("name"))) -> e)).toMap
    }

    // Now check the facts in the XBRL instance against these item declarations, mainly for periodType "instant"

    val contextElemsById: Map[String, Elem] = {
      val contextElems = instanceDoc.documentElement.filterChildElems(withEName(xbrliNamespace, "context"))
      contextElems.groupBy(e => e.attribute(EName("id"))).mapValues(elems => elems.head)
    }

    val factElems = instanceDoc.documentElement \ (e => e.resolvedName.namespaceUriOption == Some(helloWorldNamespace))

    // Are all facts for declared concepts?

    assertResult(true) {
      factElems.map(e => e.resolvedName).toSet.subsetOf(itemDeclsByTargetENames.keySet)
    }

    // Is the period always "instant", both in the XBRL instance and in the defining schema?

    assertResult(true) {
      factElems forall { elem =>
        val ename = elem.resolvedName
        val itemDecl = itemDeclsByTargetENames.getOrElse(ename, sys.error(s"Missing item declaration for concept $ename"))

        val expectedPeriodType = itemDecl.attribute(EName(xbrliNamespace, "periodType"))

        val contextId = elem.attribute(EName("contextRef"))

        val contextElem = contextElemsById.getOrElse(contextId, sys.error(s"Missing @contextRef in $elem"))
        val period = contextElem.getChildElem(withEName(xbrliNamespace, "period"))

        (period.findChildElem(withEName(xbrliNamespace, "instant")).isDefined) && (expectedPeriodType == "instant")
      }
    }
  }

  /**
   * Showing that prefixes are insignificant, and introduce "resolved" elements.
   *
   * Before treating this part, mention that prefixes are insignificant (referring again to http://www.lenzconsulting.com/namespaces/).
   * This is illustrated with an equivalent schema document without default namespace. Introduce "resolved" elements, and
   * show equivalence of both schema documents.
   */
  @Test def testPrefixesAreInsignificant(): Unit = {
    val docParser = parse.DocumentParserUsingSax.newInstance

    val schemaDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI)

    // Now read an equivalent schema document, where the default namespace no longer exists
    val schemaDoc2: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld2.xsd").toURI)

    // Use so-called "resolved" elements to compare them for equivalent, ignoring prefixes

    assertResult(resolved.Elem(schemaDoc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(schemaDoc2.documentElement).removeAllInterElementWhitespace
    }

    // Again query the schema for item declarations, in both cases, using exactly the same queries!

    val itemDecls = findAllItemDeclarations(schemaDoc.documentElement)
    val itemDecls2 = findAllItemDeclarations(schemaDoc2.documentElement)

    // These item declarations are the same, ignoring prefixes

    assertResult(itemDecls.map(e => resolved.Elem(e).removeAllInterElementWhitespace)) {
      itemDecls2.map(e => resolved.Elem(e).removeAllInterElementWhitespace)
    }
  }

  /**
   * Showing that different element implementations can be queried using the same queries!
   *
   * Before treating this part, show that the same element query can be applied to "resolved" elements, returning
   * "equivalent" results.
   */
  @Test def testQueryApiIsUniform(): Unit = {
    val docParser = parse.DocumentParserUsingSax.newInstance

    val schemaDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI)

    val itemDecls = findAllItemDeclarations(schemaDoc.documentElement)

    val resolvedSchemaDocElem = resolved.Elem(schemaDoc.documentElement)

    // The "resolved" element can be queried using the exact same query!

    val resolvedItemDecls = findAllItemDeclarations(resolvedSchemaDocElem)

    // The query results are equivalent

    assertResult(itemDecls.map(e => resolved.Elem(e))) {
      resolvedItemDecls
    }
  }

  /**
   * Showing that different element implementations can be queried using the same queries, also for DOM wrappers!
   *
   * Before treating this part, show that the same element query can be applied to "DOM wrapper" elements, returning
   * "equivalent" results.
   */
  @Test def testQueryApiIsUniformForDomWrappersToo(): Unit = {
    val dbf = DocumentBuilderFactory.newInstance
    val db = dbf.newDocumentBuilder
    val d = db.parse(new jio.File(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI))

    val wrapperDoc = dom.DomDocument(d)

    // The "DOM wrapper" element can be queried using the exact same query!

    val itemDecls = findAllItemDeclarations(wrapperDoc.documentElement)

    // Convert to yaidom Elems and show equivalence

    val docParser = parse.DocumentParserUsingSax.newInstance
    val schemaDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI)

    assertResult(findAllItemDeclarations(schemaDoc.documentElement).map(e => resolved.Elem(e))) {
      itemDecls map { e =>
        val parentScope = schemaDoc.documentElement.scope
        val convertedElem = convert.DomConversions.convertToElem(e.wrappedNode, parentScope)
        resolved.Elem(convertedElem)
      }
    }
  }

  /**
   * Showing that different element implementations can be queried using the same queries, also for "indexed" elements!
   * These "indexed" elements know their ancestry but are immutable!
   *
   * Before treating this part, show that the same element query can be applied to "indexed" elements, returning
   * "equivalent" results.
   */
  @Test def testQueryApiIsUniformForIndexedElemsToo(): Unit = {
    val docParser = parse.DocumentParserUsingSax.newInstance
    val schemaDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI)

    val indexedSchemaDoc = indexed.Document(schemaDoc)

    val indexedItemDecls = findAllItemDeclarations(indexedSchemaDoc.documentElement)

    // Convert to yaidom Elems and show equivalence

    assertResult(findAllItemDeclarations(schemaDoc.documentElement).map(e => resolved.Elem(e))) {
      indexedItemDecls.map(e => resolved.Elem(e.elem))
    }

    // Using "indexed" elements, we can query more directly if facts all correspond to declared concepts

    val instanceDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xml").toURI)

    val factElems = instanceDoc.documentElement \ (e => e.resolvedName.namespaceUriOption == Some(helloWorldNamespace))

    // Are all facts for declared concepts?

    val targetENames: Set[EName] =
      indexedItemDecls.map(e => e.toGlobalElementDeclaration.targetEName).toSet

    assertResult(true) {
      factElems.map(e => e.resolvedName).toSet.subsetOf(targetENames)
    }
  }

  /**
   * Showing a somewhat more interesting query, using one specific element representation, thus being able to use specific
   * methods of that element representation in the query.
   *
   * Before treating this part, briefly mention XBRL formulas, and admit that this example element query is quite trivial in
   * comparison.
   *
   * Closing remarks: yaidom element querying can be made more friendly (using QNames in a certain Scope), and yaidom can be
   * configured in many ways. That is out of scope for the first blog.
   */
  @Test def testSimpleFormula(): Unit = {
    val docParser = parse.DocumentParserUsingSax.newInstance
    val schemaDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI)

    val itemDecls = findAllItemDeclarationsRobustly(indexed.Document(schemaDoc).documentElement)

    assertResult(findAllItemDeclarations(schemaDoc.documentElement)) {
      itemDecls.map(_.indexedElem.elem)
    }

    // All item declarations happen to be numeric

    assertResult(Set(EName(xbrliNamespace, "monetaryItemType"))) {
      val result = itemDecls map { elem =>
        elem.indexedElem.elem.attributeAsResolvedQNameOption(EName("type"))
      }
      result.toSet.flatten
    }

    val numericItemDecls = itemDecls

    // For all concepts in the schema, check that the instance has corresponding facts for all combinations of contexts and units.

    val instanceDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xml").toURI)

    import ElemApi._

    val contextElems = instanceDoc.documentElement.filterChildElems(withEName(xbrliNamespace, "context"))
    val unitElems = instanceDoc.documentElement.filterChildElems(withEName(xbrliNamespace, "unit"))

    val numericFactsByContextUnitPair: Map[(String, String), immutable.IndexedSeq[Elem]] = {
      // In our sample instance, the below is true
      val factElems = instanceDoc.documentElement \ (_.resolvedName.namespaceUriOption == Some(helloWorldNamespace))

      factElems groupBy { elem =>
        val contextRef = (elem \@ EName("contextRef")).get
        val unitRef = (elem \@ EName("unitRef")).get
        (contextRef, unitRef)
      }
    }

    val contextUnitPairs: Set[(String, String)] = {
      val result =
        for {
          contextElem <- contextElems
          unitElem <- unitElems
        } yield (contextElem.attribute(EName("id")), unitElem.attribute(EName("id")))

      result.toSet
    }

    assertResult(contextUnitPairs) {
      numericFactsByContextUnitPair.keySet
    }

    val itemTargetENames: Set[EName] = itemDecls.map(_.targetEName).toSet

    assertResult(true) {
      numericFactsByContextUnitPair.values forall (elems => elems.map(_.resolvedName).toSet == itemTargetENames)
    }
  }

  /**
   * Finds all item declarations, accepting any ElemApi[_] element tree holding the schema. The general nature of the query
   * comes at a price, viz. less robustness of the query. After all, the substitution group could be encoded using a different
   * prefix than "xbrli", depending on which namespaces are in-scope.
   */
  private def findAllItemDeclarations[E <: ElemApi[E]](docElem: E): immutable.IndexedSeq[E] = {
    require(docElem.resolvedName == EName(xsNamespace, "schema"))

    import ElemApi._

    // Note we cannot use method attributeAsResolvedQNameOption here!

    for {
      elemDecl <- docElem \ withEName(xsNamespace, "element")
      if (elemDecl \@ EName("substitutionGroup")) == Some("xbrli:item")
    } yield elemDecl
  }

  /**
   * Finds all item declarations, robustly. Only for "indexed" yaidom Elems.
   */
  private def findAllItemDeclarationsRobustly(docElem: indexed.Elem): immutable.IndexedSeq[GlobalElementDeclaration] = {
    require(docElem.resolvedName == EName(xsNamespace, "schema"))

    import ElemApi._

    for {
      elemDecl <- docElem \ withEName(xsNamespace, "element")
      if elemDecl.elem.attributeAsResolvedQNameOption(EName("substitutionGroup")) == Some(EName(xbrliNamespace, "item"))
    } yield elemDecl.toGlobalElementDeclaration
  }
}

object Blog1Test {

  val xsNamespace = "http://www.w3.org/2001/XMLSchema"
  val xbrliNamespace = "http://www.xbrl.org/2003/instance"
  val helloWorldNamespace = "http://xbrl.squarespace.com/HelloWorld"

  final class GlobalElementDeclaration(val indexedElem: indexed.Elem) {
    require(indexedElem.rootElem.resolvedName == EName(xsNamespace, "schema"))
    require(indexedElem.path.entries.size == 1)
    require(indexedElem.resolvedName == EName(xsNamespace, "element"))

    def tnsOption: Option[String] = (indexedElem.rootElem \@ EName("targetNamespace"))

    def targetEName: EName = EName(tnsOption, (indexedElem \@ EName("name")).get)
  }

  implicit class ToGlobalElementDeclaration(val indexedElem: indexed.Elem) extends AnyVal {

    def toGlobalElementDeclaration: GlobalElementDeclaration = new GlobalElementDeclaration(indexedElem)
  }
}
