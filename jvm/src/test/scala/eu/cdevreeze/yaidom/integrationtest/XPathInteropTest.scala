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

package eu.cdevreeze.yaidom.integrationtest

import scala.collection.immutable

import org.junit.runner.RunWith
import org.scalatest.BeforeAndAfterAll
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.w3c.dom.Node
import org.w3c.dom.NodeList

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.convert.DomConversions.convertToElem
import eu.cdevreeze.yaidom.convert.DomConversions.nodeListToIndexedSeq
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.jvm.NamespaceContexts
import eu.cdevreeze.yaidom.dom
import eu.cdevreeze.yaidom.parse
import eu.cdevreeze.yaidom.queryapi.HasENameApi
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory

/**
 * XPath interoperability test. This test shows that DOM Node lists obtained with XPath queries can be processed further using
 * yaidom.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XPathInteropTest extends FunSuite with BeforeAndAfterAll {

  test("testProcessXPathResults") {
    val ns = "http://bookstore"
    val domNodes = runXPath("//bk:Book", Scope.from("bk" -> ns))

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

  test("testXPathWithMissingNamespaceBinding") {
    val domNodes = runXPath("//Book", Scope.Empty)

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))

    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(0) {
      domElems.size
    }
  }

  test("testXPathWithDuplicateNamespaceBindings") {
    // Note that there is no real reason that a Scope passed as namespace context must be invertible!
    val ns = "http://bookstore"
    val domNodes = runXPath("//bk:Bookstore/book:Book", Scope.from("" -> ns, "bk" -> ns, "book" -> ns))

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))

    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(4) {
      domElems.size
    }
  }

  private def runXPath(xpathExpr: String, scope: Scope): immutable.IndexedSeq[Node] = {
    // Another good use case for Scopes, viz. as factories of JAXP NamespaceContext objects.
    val namespaceContext = NamespaceContexts.scopeToNamespaceContext(scope)

    val xpathFactory =
      XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "net.sf.saxon.xpath.XPathFactoryImpl", null)
    val xpath = xpathFactory.newXPath()
    xpath.setNamespaceContext(namespaceContext)

    val expr = xpath.compile(xpathExpr)

    val is = classOf[XPathInteropTest].getResourceAsStream("books.xml")
    val docParser = parse.DocumentParserUsingSax.newInstance
    val doc = docParser.parse(is)
    val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder()
    val domDoc = convert.DomConversions.convertDocument(doc)(db.newDocument())

    val nodeList = expr.evaluate(domDoc, XPathConstants.NODESET).asInstanceOf[NodeList]

    // Converting NodeList to a Scala IndexedSeq of (DOM) Node instances.
    val domNodes = nodeListToIndexedSeq(nodeList)
    domNodes
  }
}
