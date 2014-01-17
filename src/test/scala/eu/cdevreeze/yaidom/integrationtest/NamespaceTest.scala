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
package integrationtest

import java.{ util => jutil, io => jio }
import javax.xml.parsers._
import javax.xml.transform.TransformerFactory
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import parse._
import print._

/**
 * Test case testing the use of namespaces in immutable Documents.
 *
 * Acknowledgments: This test uses the excellent article http://lenzconsulting.com/namespaces/ on "Understanding XML Namespaces".
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class NamespaceTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  val nsAtom = "http://www.w3.org/2005/Atom"
  val nsXhtml = "http://www.w3.org/1999/xhtml"
  val nsExamples = "http://xmlportfolio.com/xmlguild-examples"

  @Test def testFeed1(): Unit = {
    testFeed("feed1.xml")

    val docParser = DocumentParserUsingSax.newInstance
    val doc = docParser.parse(classOf[NamespaceTest].getResourceAsStream("feed1.xml"))
    val rootElm = doc.documentElement

    expectResult(List(QName("feed"), QName("title"), QName("rights"), QName("xhtml:div"), QName("xhtml:strong"), QName("xhtml:em"))) {
      rootElm.findAllElemsOrSelf map { _.qname }
    }

    val rootElmBuilder = NodeBuilder.fromElem(doc.documentElement)(Scope.Empty)

    expectResult(Declarations.from("" -> nsAtom, "xhtml" -> nsXhtml, "my" -> nsExamples)) {
      rootElmBuilder.namespaces
    }
    assert(rootElmBuilder.findAllElems forall (eb => eb.namespaces.isEmpty))
  }

  @Test def testFeed2(): Unit = {
    testFeed("feed2.xml")

    val docParser = DocumentParserUsingSax.newInstance
    val doc = docParser.parse(classOf[NamespaceTest].getResourceAsStream("feed2.xml"))
    val rootElm = doc.documentElement

    expectResult(List(QName("feed"), QName("title"), QName("rights"), QName("div"), QName("strong"), QName("em"))) {
      rootElm.findAllElemsOrSelf map { _.qname }
    }

    val rootElmBuilder = NodeBuilder.fromElem(doc.documentElement)(Scope.Empty)

    expectResult(Declarations.from("" -> nsAtom)) {
      rootElmBuilder.namespaces
    }
    expectResult(Declarations.from("example" -> nsExamples)) {
      rootElmBuilder findElem { eb => eb.qname == QName("rights") } map { _.namespaces } getOrElse (Declarations.Empty)
    }
    expectResult(Declarations.from("" -> nsXhtml)) {
      rootElmBuilder findElem { eb => eb.qname == QName("div") } map { _.namespaces } getOrElse (Declarations.Empty)
    }
  }

  @Test def testFeed3(): Unit = {
    testFeed("feed3.xml")

    val docParser = DocumentParserUsingSax.newInstance
    val doc = docParser.parse(classOf[NamespaceTest].getResourceAsStream("feed3.xml"))
    val rootElm = doc.documentElement

    expectResult(List(QName("feed"), QName("title"), QName("rights"), QName("xhtml:div"), QName("xhtml:strong"), QName("xhtml:em"))) {
      rootElm.findAllElemsOrSelf map { _.qname }
    }

    val rootElmBuilder = NodeBuilder.fromElem(doc.documentElement)(Scope.Empty)

    expectResult(Declarations.from("" -> nsAtom, "xhtml" -> nsXhtml, "my" -> nsExamples)) {
      rootElmBuilder.namespaces
    }
    // Superfluous namespace declarations not restored
    expectResult(Declarations.Empty) {
      rootElmBuilder findElem { eb => eb.qname == QName("rights") } map { _.namespaces } getOrElse (Declarations.Empty)
    }
    // Superfluous namespace declarations not restored
    expectResult(Declarations.Empty) {
      rootElmBuilder findElem { eb => eb.qname == QName("div") } map { _.namespaces } getOrElse (Declarations.Empty)
    }
  }

  @Test def testFeedEquality(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    val doc1 = docParser.parse(classOf[NamespaceTest].getResourceAsStream("feed1.xml"))
    val rootElm1 = doc1.documentElement

    val doc2 = docParser.parse(classOf[NamespaceTest].getResourceAsStream("feed2.xml"))
    val rootElm2 = doc2.documentElement

    expectResult(resolved.Elem(rootElm1)) {
      resolved.Elem(rootElm2)
    }
  }

  @Test def testUndeclareDefaultNamespace(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    val doc1 = docParser.parse(classOf[NamespaceTest].getResourceAsStream("simpleStylesheet1.xsl"))
    val rootElm1 = doc1.documentElement

    val doc2 = docParser.parse(classOf[NamespaceTest].getResourceAsStream("simpleStylesheet2.xsl"))
    val rootElm2 = doc2.documentElement

    expectResult(resolved.Elem(rootElm1)) {
      resolved.Elem(rootElm2)
    }

    val elm1Option = rootElm1 findElem { e => e.qname == QName("xsl:template") }

    expectResult(Some(EName("{http://www.w3.org/1999/XSL/Transform}template"))) {
      elm1Option.map(_.resolvedName)
    }

    val elm2Option = rootElm2 findElem { e => e.qname == QName("template") }

    expectResult(Some(EName("{http://www.w3.org/1999/XSL/Transform}template"))) {
      elm2Option.map(_.resolvedName)
    }

    val htmlElm1Option = rootElm1 findElem { e => e.qname == QName("html") }

    expectResult(Some(EName("html"))) {
      htmlElm1Option.map(_.resolvedName)
    }

    val htmlElm2Option = rootElm2 findElem { e => e.qname == QName("html") }

    expectResult(Some(EName("html"))) {
      htmlElm2Option.map(_.resolvedName)
    }
  }

  @Test def testOverrideNamespace(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    val s = """|<my:foo xmlns:my="http://example.com/uri1">
               |  <my:bar xmlns:my="http://example.com/uri2"/>
               |</my:foo>""".stripMargin

    val doc = docParser.parse(new jio.ByteArrayInputStream(s.getBytes("utf-8")))
    val rootElm = doc.documentElement

    expectResult(List(EName("{http://example.com/uri1}foo"), EName("{http://example.com/uri2}bar"))) {
      rootElm.findAllElemsOrSelf collect { case e => e.resolvedName }
    }
  }

  @Test def testNamespaceIsNotUri(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    // Relative URIs should not be used! In any case, namespace URI comparison is string comparison.
    val s = """|<my1:foo xmlns:my1="http://example.com/uri1">
               |  <my2:bar xmlns:my2="http://EXAMPLE.COM/uri2/"><my3:baz xmlns:my3="../uri3"/></my2:bar>
               |</my1:foo>""".stripMargin

    val doc = docParser.parse(new jio.ByteArrayInputStream(s.getBytes("utf-8")))
    val rootElm = doc.documentElement

    val enames =
      List(
        EName("{http://example.com/uri1}foo"),
        EName("{http://EXAMPLE.COM/uri2/}bar"),
        EName("{../uri3}baz"))

    expectResult(enames) {
      rootElm.findAllElemsOrSelf collect { case e => e.resolvedName }
    }

    expectResult(3) {
      Set(EName("{http://example.com/uri}foo"), EName("{http://example.com/uri/}foo"), EName("{http://EXAMPLE.COM/uri}foo")).size
    }
  }

  @Test def testUndeclareNonDefaultNamespace(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    // Only XML version 1.1 allows the use of prefixed namespace undeclarations

    val s = """|<?xml version="1.1"?>
               |<my:foo xmlns:my="http://example.com/uri1">
               |  <my2:bar xmlns:my="" xmlns:my2="http://example.com/uri2"/>
               |</my:foo>""".stripMargin

    val doc = docParser.parse(new jio.ByteArrayInputStream(s.getBytes("utf-8")))
    val rootElm = doc.documentElement

    expectResult(List(EName("{http://example.com/uri1}foo"), EName("{http://example.com/uri2}bar"))) {
      rootElm.findAllElemsOrSelf collect { case e => e.resolvedName }
    }
  }

  @Test def testXmlNamespace(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    val s = """|<my:foo xmlns:my="http://example.com/uri">
               |  <my:bar xml:lang="en"/>
               |</my:foo>""".stripMargin

    val doc = docParser.parse(new jio.ByteArrayInputStream(s.getBytes("utf-8")))
    val rootElm = doc.documentElement

    expectResult(List(EName("{http://example.com/uri}foo"), EName("{http://example.com/uri}bar"))) {
      rootElm.findAllElemsOrSelf collect { case e => e.resolvedName }
    }

    expectResult(Some("en")) {
      rootElm findElem { e => e.localName == "bar" } flatMap
        { e => e.attributeOption(EName("{http://www.w3.org/XML/1998/namespace}lang")) }
    }
  }

  @Test def testUndeclareAnotherNonDefaultNamespace(): Unit = {
    val docParser = DocumentParserUsingSax.newInstance

    // Only XML version 1.1 allows the use of prefixed namespace undeclarations

    val s = """|<?xml version="1.1"?>
               |<my:doc xmlns:my="http://xmlportfolio.com/xmlguild-examples">
               |  <simple xmlns:my="">
               |    <remark>We don't use namespaces.</remark>
               |  </simple>
               |</my:doc>""".stripMargin

    val doc = docParser.parse(new jio.ByteArrayInputStream(s.getBytes("utf-8")))
    val rootElm = doc.documentElement

    expectResult(Scope.from("my" -> "http://xmlportfolio.com/xmlguild-examples")) {
      rootElm.scope
    }

    val simpleElmOption = rootElm findElem { _.qname == QName("simple") }
    val simpleElm = simpleElmOption.getOrElse(sys.error("Expected element 'simple'"))

    expectResult(EName("simple")) {
      simpleElm.resolvedName
    }
    expectResult(Scope.Empty) {
      simpleElm.scope
    }
    expectResult(List(EName("simple"), EName("remark"))) {
      simpleElm.findAllElemsOrSelf map { _.resolvedName }
    }
  }

  private def testFeed(fileName: String): Unit = {
    val docParser = DocumentParserUsingSax.newInstance
    val doc = docParser.parse(classOf[NamespaceTest].getResourceAsStream(fileName))
    val rootElm = doc.documentElement

    val feedElmOption = rootElm findElemOrSelf { e => e.resolvedName == EName(nsAtom, "feed") }
    expectResult(true) {
      feedElmOption.isDefined
    }
    expectResult(rootElm) {
      feedElmOption.get
    }

    val titleElmOption = rootElm findElemOrSelf { e => e.resolvedName == EName(nsAtom, "title") }
    expectResult(true) {
      titleElmOption.isDefined
    }

    val rightsElmOption = rootElm findElemOrSelf { e => e.resolvedName == EName(nsAtom, "rights") }
    expectResult(true) {
      rightsElmOption.isDefined
    }

    expectResult(List(feedElmOption.get, titleElmOption.get, rightsElmOption.get)) {
      rootElm filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsAtom) }
    }

    val divElmOption = rootElm findElemOrSelf { e => e.resolvedName == EName(nsXhtml, "div") }
    expectResult(true) {
      divElmOption.isDefined
    }

    val strongElmOption = rootElm findElemOrSelf { e => e.resolvedName == EName(nsXhtml, "strong") }
    expectResult(true) {
      strongElmOption.isDefined
    }

    val emElmOption = rootElm findElemOrSelf { e => e.resolvedName == EName(nsXhtml, "em") }
    expectResult(true) {
      emElmOption.isDefined
    }

    expectResult(List(divElmOption.get, strongElmOption.get, emElmOption.get)) {
      rootElm filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsXhtml) }
    }
    expectResult("verbally process") {
      strongElmOption.get.text
    }
    expectResult(3) {
      // With an IBM JRE, more than 3 text children are found
      divElmOption.get.removeAllInterElementWhitespace.textChildren.size.min(3)
    }
    expectResult("from the authors.") {
      divElmOption.get.removeAllInterElementWhitespace.textChildren.filterNot(_.text.trim.isEmpty).last.text.trim
    }
    expectResult(List(strongElmOption.get, emElmOption.get)) {
      divElmOption.get.findAllChildElems
    }
    expectResult(5) {
      // With an IBM JRE, more than 5 children are found
      divElmOption.get.removeAllInterElementWhitespace.children.size.min(5)
    }
    expectResult(resolved.Elem(emElmOption.get)) {
      val child: Node = divElmOption.get.removeAllInterElementWhitespace.findChildElem(_.localName == "em").get
      resolved.Node(child)
    }

    expectResult(Set(nsAtom, nsXhtml)) {
      val namespaces = rootElm.findAllElemsOrSelf flatMap { e => e.resolvedName.namespaceUriOption }
      namespaces.toSet
    }

    expectResult("xhtml") {
      rightsElmOption.get.attributeOption(EName("type")).getOrElse("")
    }
    expectResult("silly") {
      rightsElmOption.get.attributeOption(EName(nsExamples, "type")).getOrElse("")
    }

    val rootElm2 = NodeBuilder.fromElem(doc.documentElement)(Scope.Empty).build(Scope.Empty)

    expectResult(resolved.Elem(rootElm)) {
      resolved.Elem(rootElm2)
    }

    val rootElm3 = NodeBuilder.fromElem(doc.documentElement)(Scope.Empty).build(Scope.from("" -> nsAtom))

    expectResult(resolved.Elem(rootElm)) {
      resolved.Elem(rootElm3)
    }

    val rootElm4 = NodeBuilder.fromElem(doc.documentElement)(Scope.from("atom" -> nsAtom)).build(Scope.Empty)

    expectResult(resolved.Elem(rootElm)) {
      resolved.Elem(rootElm4)
    }

    val rootElm5 = NodeBuilder.fromElem(doc.documentElement)(Scope.from("" -> nsAtom)).build(Scope.from("" -> nsAtom))

    expectResult(resolved.Elem(rootElm)) {
      resolved.Elem(rootElm5)
    }

    val docPrinter = DocumentPrinterUsingSax.newInstance

    val xml = docPrinter.print(doc)
    val doc6 = docParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    expectResult(resolved.Elem(rootElm)) {
      resolved.Elem(doc6.documentElement)
    }

    expectResult(resolved.Elem(rootElm).findAllElemsOrSelf) {
      rootElm.findAllElemsOrSelf map { e => resolved.Elem(e) }
    }

    expectResult(resolved.Elem(rootElm) filterElemsOrSelf (_.resolvedName.namespaceUriOption == Some(nsAtom))) {
      rootElm filterElemsOrSelf { e => e.resolvedName.namespaceUriOption == Some(nsAtom) } map { e => resolved.Elem(e) }
    }
  }
}
