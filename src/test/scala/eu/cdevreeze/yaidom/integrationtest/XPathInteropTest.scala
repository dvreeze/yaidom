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

package eu.cdevreeze.yaidom
package integrationtest

import java.{ util => jutil, io => jio }
import javax.xml.xpath._
import org.xml.sax.InputSource
import org.w3c.dom.NodeList
import scala.collection.{ immutable, mutable }
import org.junit.{ Test, Before }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import convert.DomConversions._
import parse._
import print._
import eu.cdevreeze.yaidom.queryapi.HasENameApi

/**
 * XPath interoperability test. This test shows that DOM Node lists obtained with XPath queries can be processed further using
 * yaidom.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XPathInteropTest extends Suite with BeforeAndAfterAll {

  @Test def testProcessXPathResults(): Unit = {
    val ns = "http://bookstore"

    // Another good use case for Scopes, viz. as factories of JAXP NamespaceContext objects.
    val scope = Scope.from("bk" -> ns)
    val namespaceContext = scope.toNamespaceContext

    val xpath = XPathFactory.newInstance().newXPath()
    xpath.setNamespaceContext(namespaceContext)

    val expr = "//bk:Book"

    val is = classOf[XPathInteropTest].getResourceAsStream("books.xml")
    val docParser = parse.DocumentParserUsingSax.newInstance
    val doc = docParser.parse(is)
    val db = javax.xml.parsers.DocumentBuilderFactory.newInstance().newDocumentBuilder()
    val domDoc = convert.DomConversions.convertDocument(doc)(db.newDocument())

    // Passing a DOMSource, to make sure that a NodeList is returned (even for a Saxon XPathFactory)
    val domSource = new javax.xml.transform.dom.DOMSource(domDoc)
    val nodeList = xpath.evaluate(expr, domSource, XPathConstants.NODESET).asInstanceOf[NodeList]

    // Converting NodeList to a Scala IndexedSeq of (DOM) Node instances.
    val domNodes = nodeListToIndexedSeq(nodeList)

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))

    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(4) {
      domElems.size
    }

    // Now converting DOM elements to yaidom Elems

    val bookElems = domElems map { e =>
      val parentScope = dom.DomElem(e).parentOption.map(_.scope).getOrElse(Scope.Empty)
      require(parentScope.inverse.contains(ns), s"Expected parent scope $parentScope to contain namespace $ns")
      convertToElem(e, parentScope)
    }

    assertResult(4) {
      bookElems.size
    }
    assertResult(Set(EName(ns, "Book"))) {
      bookElems.map(_.resolvedName).toSet
    }

    import HasENameApi._

    val authors: Set[String] = {
      val result =
        for {
          bookElem <- bookElems
          authorElem <- bookElem \\ withEName(ns, "Author")
          firstNameElem <- authorElem \ withEName(ns, "First_Name")
          lastNameElem <- authorElem \ withEName(ns, "Last_Name")
        } yield s"${firstNameElem.text} ${lastNameElem.text}"
      result.toSet
    }

    assertResult(Set("Hector Garcia-Molina", "Jeffrey Ullman", "Jennifer Widom")) {
      authors
    }
  }
}
