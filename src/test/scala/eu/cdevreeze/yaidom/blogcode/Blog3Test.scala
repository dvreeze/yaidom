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

import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Code of yaidom blog 3 ("more yaidom querying"). The blog uses examples from the XML Schema Part 0 (Primer).
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * The example "problem" is to find local element declarations in a schema, and match them with elements in the instance
 * document. To achieve that, we must be able to reason about the found element declarations. For example, we want to
 * determine the target namespace, and the surrounding content model.
 *
 * We will gradually introduce the different query API traits, and mention some element classes implementing them.
 * Each query API sub-trait has something to offer (w.r.t. the problem mentioned above) that its super-trait does
 * not offer, in order to make matching between element declaration and elements in the instance document easier.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class Blog3Test extends Suite {

  private val pathToParentDir: java.io.File =
    (new java.io.File(classOf[Blog3Test].getResource("po1.xsd").toURI)).getParentFile

  @Test def testParentElemLikeExample(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    val tns = "http://www.example.com/PO1"

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val xsdDoc: Document =
      docParser.parse(new File(parentDir, "po1.xsd"))

    val xmlDoc: Document =
      docParser.parse(new File(parentDir, "po1.xml"))

    // Turn XSD into an ElemBuilder, which cannot be queried about scopes and expanded names
    // Normally one would turn ElemBuilders (once "ready") into Elems, and not the other way around.

    val xsdElemBuilder = NodeBuilder.fromElem(xsdDoc.documentElement)(Scope.Empty)
    val xmlElemBuilder = NodeBuilder.fromElem(xmlDoc.documentElement)(Scope.Empty)

    // Needing more than the ParentElemLike API
    // Note that we do not even know if the element declarations found are local instead of global
    val shipToElemDecls =
      xsdElemBuilder filterElems { e =>
        e.qname.localPart == "element" &&
          e.attributes.toMap.get(QName("name")) == Some("shipTo")
      }
    require(shipToElemDecls.size == 1, s"Expected precisely 1 element declaration for 'shipTo'")

    val shipToElems =
      xmlElemBuilder filterElems { e =>
        e.qname.localPart == "billTo"
      }
    require(shipToElems.size == 1, s"Expected precisely 1 element named 'shipTo'")

    // But how do we know this element declaration and element match? We do not know (target) namespace, and we do
    // not know the (expected) element ancestry. Clearly, trait ParentElemLike does not really help us here.
    // That trait only knows that elements can have child elements, and as a consequence descendant and descendant-or-self
    // elements, but it knows nothing more about elements, not even the name and attributes.
  }

  @Test def testElemLikeExample(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._
    import eu.cdevreeze.yaidom.ElemApi._

    val xsNs = "http://www.w3.org/2001/XMLSchema"
    val tns = "http://www.example.com/PO1"

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val xsdDoc: Document =
      docParser.parse(new File(parentDir, "po1.xsd"))

    val xmlDoc: Document =
      docParser.parse(new File(parentDir, "po1.xml"))

    // Note that we do not even know if the element declarations found are local instead of global
    val shipToElemDecls =
      xsdDoc.documentElement filterElems { e =>
        e.resolvedName == EName(xsNs, "element") &&
          e.attributeOption(EName("name")) == Some("shipTo")
      }
    require(shipToElemDecls.size == 1, s"Expected precisely 1 element declaration for 'shipTo'")

    val shipToElems =
      xmlDoc.documentElement filterElems { e =>
        e.localName == "billTo"
      }
    require(shipToElems.size == 1, s"Expected precisely 1 element named 'shipTo'")

    // But how do we know this element declaration and element match? We do not know (target) namespace, and we do
    // not know the (expected) element ancestry. Also trait ElemLike does not really help us enough here.
    // That trait only knows about elements what trait ParentElemLike knows, plus the fact the elements have
    // expanded names and attributes with expanded names.

    // Now use Scala XML wrappers, and show equivalent results.

    val xsdElemAsScalaXmlWrapper: scalaxml.ScalaXmlElem =
      scalaxml.ScalaXmlElem(scala.xml.XML.loadFile(new File(parentDir, "po1.xsd")))

    val xmlElemAsScalaXmlWrapper: scalaxml.ScalaXmlElem =
      scalaxml.ScalaXmlElem(scala.xml.XML.loadFile(new File(parentDir, "po1.xml")))

    // Using the same ElemLike query API in exactly the same way

    val shipToElemDeclsAsScalaXmlWrappers =
      xsdElemAsScalaXmlWrapper filterElems { e =>
        e.resolvedName == EName(xsNs, "element") &&
          e.attributeOption(EName("name")) == Some("shipTo")
      }
    require(shipToElemDeclsAsScalaXmlWrappers.size == 1, s"Expected precisely 1 element declaration for 'shipTo'")

    val shipToElemsAsScalaXmlWrappers =
      xmlElemAsScalaXmlWrapper filterElems { e =>
        e.localName == "billTo"
      }
    require(shipToElemsAsScalaXmlWrappers.size == 1, s"Expected precisely 1 element named 'shipTo'")

    // Now show equivalence

    require(
      resolved.Elem(shipToElemDecls.head).removeAllInterElementWhitespace ==
        resolved.Elem(convert.ScalaXmlConversions.convertToElem(shipToElemDeclsAsScalaXmlWrappers.head.wrappedNode)).removeAllInterElementWhitespace)

    require(
      resolved.Elem(shipToElems.head).removeAllInterElementWhitespace ==
        resolved.Elem(convert.ScalaXmlConversions.convertToElem(shipToElemsAsScalaXmlWrappers.head.wrappedNode)).removeAllInterElementWhitespace)

    // We could have queried the resolved elements themselves, using the same query API in the same way

    val shipToElemDeclsAsResolvedElems =
      resolved.Elem(xsdDoc.documentElement) filterElems { e =>
        e.resolvedName == EName(xsNs, "element") &&
          e.attributeOption(EName("name")) == Some("shipTo")
      }
    require(shipToElemDeclsAsResolvedElems.size == 1, s"Expected precisely 1 element declaration for 'shipTo'")

    val shipToElemsAsResolvedElems =
      resolved.Elem(xmlDoc.documentElement) filterElems { e =>
        e.localName == "billTo"
      }
    require(shipToElemsAsResolvedElems.size == 1, s"Expected precisely 1 element named 'shipTo'")

    require(
      resolved.Elem(shipToElemDecls.head).removeAllInterElementWhitespace ==
        shipToElemDeclsAsResolvedElems.head.removeAllInterElementWhitespace)

    require(
      resolved.Elem(shipToElems.head).removeAllInterElementWhitespace ==
        shipToElemsAsResolvedElems.head.removeAllInterElementWhitespace)
  }

  @Test def testNavigableElemLikeExample(): Unit = {
    // TODO Use indexed or docaware elements; context matters! We use more than the NavigableElemLike trait, though!
  }
}
