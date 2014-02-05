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
 * Code of yaidom blog 1 ("yaidom querying"). The blog uses examples from http://xbrl.squarespace.com/.
 * The blog should show yaidom's strengths (such as leveraging Scala Collections, and namespace handling),
 * and be accessible and interesting to the readers. So it should respect the limited time of the readers.
 * A little bit of Scala knowledge is assumed, in particular the basics of Scala Collections. A little bit
 * of XML Schema knowledge is assumed as well.
 *
 * The (code in this) blog shows yaidom queries, using different element representations. It also shows namespace handling
 * in yaidom, and XML comparisons based on "resolved" elements.
 *
 * The queries retrieve facts in an XBRL instance, or perform consistency checks.
 *
 * Before showing any code, introduce XBRL and XBRL instances briefly, using the HelloWorld.xml instance.
 *
 * Note: blog 2 will treat transformations, and blog 3 will cover some advanced concepts, such as configuring yaidom.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class Blog1Test extends Suite {

  /**
   * Showing trivial queries for child elements, descendant elements, or descendant-or-self elements.
   */
  @Test def testQueryXbrlInstance(): Unit = {
    val docParser = parse.DocumentParserUsingDom.newInstance

    val doc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xml").toURI)

    // Query "unit" child elements, using the local name

    val unitElems = doc.documentElement.filterChildElems(e => e.localName == "unit")

    assertResult(1) {
      unitElems.size
    }

    // Query "context" child elements, using the local name

    val contextElems = doc.documentElement.filterChildElems(e => e.localName == "context")

    assertResult(2) {
      contextElems.size
    }

    assertResult(contextElems) {
      doc.documentElement \ (e => e.localName == "context")
    }

    // Query "context" descendant elements, using the local name. The same elements are returned.

    assertResult(contextElems) {
      doc.documentElement.filterElems(e => e.localName == "context")
    }

    // Query "context" descendant-or-self elements, using the local name. Again the same elements are returned.

    assertResult(contextElems) {
      doc.documentElement.filterElemsOrSelf(e => e.localName == "context")
    }

    assertResult(contextElems) {
      doc.documentElement \\ (e => e.localName == "context")
    }

    // Query "context" child elements, using the expanded name

    assertResult(contextElems) {
      doc.documentElement.filterElems(e => e.resolvedName == EName(xbrliNamespace, "context"))
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

    val tns = schemaDoc.documentElement.attribute(EName("targetNamespace"))

    val itemDecls = findAllItemDeclarations(schemaDoc.documentElement)
    val itemDecls2 = findAllItemDeclarations(schemaDoc2.documentElement)

    // These item declarations are the same, ignoring prefixes

    assertResult(itemDecls.map(e => resolved.Elem(e).removeAllInterElementWhitespace)) {
      itemDecls2.map(e => resolved.Elem(e).removeAllInterElementWhitespace)
    }
  }

  /**
   * Showing that different element implementations can be queried using the same queries!
   */
  @Test def testQueryApiIsUniform(): Unit = {
    val docParser = parse.DocumentParserUsingSax.newInstance

    val schemaDoc: Document = docParser.parse(classOf[Blog1Test].getResource("HelloWorld.xsd").toURI)

    val tns = schemaDoc.documentElement.attribute(EName("targetNamespace"))

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
   * Finds all item declarations, accepting any ElemApi[_] element tree holding the schema.
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
