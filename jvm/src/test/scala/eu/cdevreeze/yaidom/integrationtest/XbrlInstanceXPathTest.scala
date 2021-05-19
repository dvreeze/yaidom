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

import eu.cdevreeze.yaidom.convert
import eu.cdevreeze.yaidom.convert.DomConversions.nodeListToIndexedSeq
import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.core.jvm.JavaQNames
import eu.cdevreeze.yaidom.core.jvm.NamespaceContexts
import eu.cdevreeze.yaidom.dom
import eu.cdevreeze.yaidom.parse
import javax.xml.xpath.XPathConstants
import javax.xml.xpath.XPathFactory
import javax.xml.xpath.XPathFunction
import javax.xml.xpath.XPathFunctionException
import javax.xml.xpath.XPathFunctionResolver
import org.scalatest.BeforeAndAfterAll
import org.scalatest.funsuite.AnyFunSuite
import org.w3c.dom.NodeList

/**
 * XBRL instance querying test using XPath for querying and XPath functions for XBRL support, where the XPath functions
 * are created using yaidom queries on yaidom wrappers around DOM trees.
 *
 * This test points to another potential major use case for yaidom, namely as a way of creating XPath functions.
 * There is no need for yaidom to reinvent XPath evaluation APIs (compared to the JAXP XPath API or Saxon's XPath
 * APIs), but using yaidom at the background for custom XPath function implementations can be quite handy.
 *
 * For a real XBRL function library, see the specification at http://specifications.xbrl.org/registries/functions-registry-1.0/.
 * Note that XBRL depends on XPath 2.0, not on XPath 1.0 which we use in this test case. Another big simplification in
 * this test case is that we do not consider the XBRL taxonomy on which the XBRL instance is based.
 *
 * @author Chris de Vreeze
 */
class XbrlInstanceXPathTest extends AnyFunSuite with BeforeAndAfterAll {

  import XbrlInstanceXPathTest.CustomFunctionNs
  import XbrlInstanceXPathTest.LinkNs
  import XbrlInstanceXPathTest.XLinkNs
  import XbrlInstanceXPathTest.XbrliNs

  private val docParser = parse.DocumentParserUsingSax.newInstance()
  private val doc =
    docParser.parse(classOf[XbrlInstanceXPathTest].getResourceAsStream("sample-xbrl-instance.xml"))

  test("testQueryForUnits") {
    // Another good use case for Scopes, viz. as factories of JAXP NamespaceContext objects.
    val scope = Scope.from("xbrli" -> XbrliNs, "link" -> LinkNs, "xlink" -> XLinkNs)
    val namespaceContext = NamespaceContexts.scopeToNamespaceContext(scope)

    val xpathFactory =
      XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "net.sf.saxon.xpath.XPathFactoryImpl", null)
    val xpath = xpathFactory.newXPath()
    xpath.setNamespaceContext(namespaceContext)

    val expr = xpath.compile("//xbrli:unit")

    val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder()
    val domDoc = convert.DomConversions.convertDocument(doc)(db.newDocument())

    val nodeList = expr.evaluate(domDoc, XPathConstants.NODESET).asInstanceOf[NodeList]

    // Converting NodeList to a Scala IndexedSeq of (DOM) Node instances.
    val domNodes = nodeListToIndexedSeq(nodeList)

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))
    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(3) {
      domElems.size
    }
  }

  test("testQueryForTopLevelFacts") {
    // Another good use case for Scopes, viz. as factories of JAXP NamespaceContext objects.
    val scope = Scope.from("xbrli" -> XbrliNs, "link" -> LinkNs, "xlink" -> XLinkNs, "my" -> CustomFunctionNs)
    val namespaceContext = NamespaceContexts.scopeToNamespaceContext(scope)

    val xpathFactory =
      XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "net.sf.saxon.xpath.XPathFactoryImpl", null)
    val xpath = xpathFactory.newXPath()
    xpath.setNamespaceContext(namespaceContext)

    val functionResolver = new XPathFunctionResolver {

      def resolveFunction(functionQName: javax.xml.namespace.QName, arity: Int): XPathFunction = {
        (JavaQNames.javaQNameToEName(functionQName), arity) match {
          case (EName(Some(CustomFunctionNs), "find-top-level-facts-in-instance"), 1) =>
            XbrlInstanceXPathTest.FindTopLevelFactsInInstance
          case _ =>
            sys.error(s"Unknown function $functionQName with arity $arity")
        }
      }
    }

    xpath.setXPathFunctionResolver(functionResolver)

    val expr = xpath.compile("my:find-top-level-facts-in-instance(/*)")

    val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder()
    val domDoc = convert.DomConversions.convertDocument(doc)(db.newDocument())

    val nodeList = expr.evaluate(domDoc, XPathConstants.NODESET).asInstanceOf[NodeList]

    // Converting NodeList to a Scala IndexedSeq of (DOM) Node instances.
    val domNodes = nodeListToIndexedSeq(nodeList)

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))
    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(true) {
      domElems.size > 500
    }

    val expectedTopLevelFacts =
      doc.documentElement.filterChildElems(e => !Set(XbrliNs, LinkNs).contains(e.resolvedName.namespaceUriOption.getOrElse("")))

    assertResult(expectedTopLevelFacts.size) {
      domElems.size
    }
  }

  test("testQueryForTopLevelItems") {
    // Another good use case for Scopes, viz. as factories of JAXP NamespaceContext objects.
    val scope = Scope.from("xbrli" -> XbrliNs, "link" -> LinkNs, "xlink" -> XLinkNs, "my" -> CustomFunctionNs)
    val namespaceContext = NamespaceContexts.scopeToNamespaceContext(scope)

    val xpathFactory =
      XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "net.sf.saxon.xpath.XPathFactoryImpl", null)
    val xpath = xpathFactory.newXPath()
    xpath.setNamespaceContext(namespaceContext)

    val functionResolver = new XPathFunctionResolver {

      def resolveFunction(functionQName: javax.xml.namespace.QName, arity: Int): XPathFunction = {
        (JavaQNames.javaQNameToEName(functionQName), arity) match {
          case (EName(Some(CustomFunctionNs), "find-top-level-facts-in-instance"), 1) =>
            XbrlInstanceXPathTest.FindTopLevelFactsInInstance
          case (EName(Some(CustomFunctionNs), "is-item"), 1) =>
            XbrlInstanceXPathTest.IsItem
          case _ =>
            sys.error(s"Unknown function $functionQName with arity $arity")
        }
      }
    }

    xpath.setXPathFunctionResolver(functionResolver)

    val expr = xpath.compile("my:find-top-level-facts-in-instance(/*)[my:is-item(.)]")

    val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder()
    val domDoc = convert.DomConversions.convertDocument(doc)(db.newDocument())

    val nodeList = expr.evaluate(domDoc, XPathConstants.NODESET).asInstanceOf[NodeList]

    // Converting NodeList to a Scala IndexedSeq of (DOM) Node instances.
    val domNodes = nodeListToIndexedSeq(nodeList)

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))
    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(true) {
      domElems.size > 500
    }

    val expectedTopLevelItems =
      doc.documentElement.filterChildElems(e => !Set(XbrliNs, LinkNs).contains(e.resolvedName.namespaceUriOption.getOrElse(""))).
        filter(e => e.attributeOption(EName("contextRef")).isDefined)

    assertResult(expectedTopLevelItems.size) {
      domElems.size
    }
  }

  test("testQueryForTopLevelTuples") {
    // Another good use case for Scopes, viz. as factories of JAXP NamespaceContext objects.
    val scope = Scope.from("xbrli" -> XbrliNs, "link" -> LinkNs, "xlink" -> XLinkNs, "my" -> CustomFunctionNs)
    val namespaceContext = NamespaceContexts.scopeToNamespaceContext(scope)

    val xpathFactory =
      XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "net.sf.saxon.xpath.XPathFactoryImpl", null)
    val xpath = xpathFactory.newXPath()
    xpath.setNamespaceContext(namespaceContext)

    val functionResolver = new XPathFunctionResolver {

      def resolveFunction(functionQName: javax.xml.namespace.QName, arity: Int): XPathFunction = {
        (JavaQNames.javaQNameToEName(functionQName), arity) match {
          case (EName(Some(CustomFunctionNs), "find-top-level-facts-in-instance"), 1) =>
            XbrlInstanceXPathTest.FindTopLevelFactsInInstance
          case _ =>
            sys.error(s"Unknown function $functionQName with arity $arity")
        }
      }
    }

    xpath.setXPathFunctionResolver(functionResolver)

    val expr = xpath.compile("my:find-top-level-facts-in-instance(/*)/*")

    val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder()
    val domDoc = convert.DomConversions.convertDocument(doc)(db.newDocument())

    val nodeList = expr.evaluate(domDoc, XPathConstants.NODESET).asInstanceOf[NodeList]

    // Converting NodeList to a Scala IndexedSeq of (DOM) Node instances.
    val domNodes = nodeListToIndexedSeq(nodeList)

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))
    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(0) {
      domElems.size
    }
  }

  test("testQueryForSomeTopLevelItems") {
    // Another good use case for Scopes, viz. as factories of JAXP NamespaceContext objects.
    val scope = Scope.from("xbrli" -> XbrliNs, "link" -> LinkNs, "xlink" -> XLinkNs, "my" -> CustomFunctionNs)
    val namespaceContext = NamespaceContexts.scopeToNamespaceContext(scope)

    val xpathFactory =
      XPathFactory.newInstance(XPathFactory.DEFAULT_OBJECT_MODEL_URI, "net.sf.saxon.xpath.XPathFactoryImpl", null)
    val xpath = xpathFactory.newXPath()
    xpath.setNamespaceContext(namespaceContext)

    val functionResolver = new XPathFunctionResolver {

      def resolveFunction(functionQName: javax.xml.namespace.QName, arity: Int): XPathFunction = {
        (JavaQNames.javaQNameToEName(functionQName), arity) match {
          case (EName(Some(CustomFunctionNs), "find-top-level-facts-in-instance"), 1) =>
            XbrlInstanceXPathTest.FindTopLevelFactsInInstance
          case (EName(Some(CustomFunctionNs), "is-item"), 1) =>
            XbrlInstanceXPathTest.IsItem
          case _ =>
            sys.error(s"Unknown function $functionQName with arity $arity")
        }
      }
    }

    xpath.setXPathFunctionResolver(functionResolver)

    val exprString =
      """my:find-top-level-facts-in-instance(/*)[my:is-item(.) and
        local-name(.) = 'AverageNumberEmployees' and namespace-uri(.) = 'http://xasb.org/gaap']"""

    val expr =
      xpath.compile(exprString)

    val dbf = javax.xml.parsers.DocumentBuilderFactory.newInstance()
    dbf.setNamespaceAware(true)
    val db = dbf.newDocumentBuilder()
    val domDoc = convert.DomConversions.convertDocument(doc)(db.newDocument())

    val nodeList = expr.evaluate(domDoc, XPathConstants.NODESET).asInstanceOf[NodeList]

    // Converting NodeList to a Scala IndexedSeq of (DOM) Node instances.
    val domNodes = nodeListToIndexedSeq(nodeList)

    require(domNodes.forall(_.isInstanceOf[org.w3c.dom.Element]))
    val domElems = domNodes map (n => n.asInstanceOf[org.w3c.dom.Element])

    assertResult(7) {
      domElems.size
    }

    val expectedTopLevelItems =
      doc.documentElement.filterChildElems(e => !Set(XbrliNs, LinkNs).contains(e.resolvedName.namespaceUriOption.getOrElse(""))).
        filter(e => e.attributeOption(EName("contextRef")).isDefined).
        filter(e => e.localName == "AverageNumberEmployees" && e.resolvedName.namespaceUriOption.contains("http://xasb.org/gaap"))

    assertResult(expectedTopLevelItems.size) {
      domElems.size
    }
  }
}

object XbrlInstanceXPathTest {

  private val XbrliNs = "http://www.xbrl.org/2003/instance"
  private val LinkNs = "http://www.xbrl.org/2003/linkbase"
  private val XLinkNs = "http://www.w3.org/1999/xlink"

  private val CustomFunctionNs = "http://customFunctions"

  object FindTopLevelFactsInInstance extends XPathFunction {

    private val LinkAndXbrliNs = Set(XbrliNs, LinkNs)

    @throws[XPathFunctionException]
    override def evaluate(args: java.util.List[_]): AnyRef = {
      require(args.size == 1, s"Expected precisely 1 argument")
      val xbrlInstanceArg = args.get(0).asInstanceOf[org.w3c.dom.Element]

      val xbrlInstance = dom.DomElem(xbrlInstanceArg)

      val topLevelFacts =
        xbrlInstance.filterChildElems(e => !LinkAndXbrliNs.contains(e.resolvedName.namespaceUriOption.getOrElse("")))
      val result = nodeSeqToNodeList(topLevelFacts.map(_.wrappedNode))
      result
    }
  }

  object IsItem extends XPathFunction {

    @throws[XPathFunctionException]
    override def evaluate(args: java.util.List[_]): AnyRef = {
      require(args.size == 1, s"Expected precisely 1 argument")
      val factArg = args.get(0).asInstanceOf[org.w3c.dom.Element]

      val fact = dom.DomElem(factArg)

      val isItem = fact.attributeOption(EName("contextRef")).isDefined
      java.lang.Boolean.valueOf(isItem)
    }
  }

  private def nodeSeqToNodeList(nodes: Seq[org.w3c.dom.Node]): NodeList = {
    new NodeList {

      def getLength: Int = nodes.size

      def item(index: Int): org.w3c.dom.Node = {
        if (index >= 0 && index < nodes.size) nodes(index) else null
      }
    }
  }
}
