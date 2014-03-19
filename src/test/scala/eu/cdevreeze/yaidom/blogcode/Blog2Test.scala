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
 * Code of yaidom blog 2 ("namespaces in yaidom"). The blog uses the excellent article http://www.lenzconsulting.com/namespaces/.
 * The reader is encouraged to read that article, and read this 2nd yaidom article to illustrate the same content using yaidom.
 * (Also mention http://www.datypic.com/books/defxmlschema/chapter03.html.)
 *
 * This article again shows basic querying in yaidom, and dives deeper into namespaces (using the excellent article from
 * Evan Lenz). It shows that indeed yaidom is about precision, clarity (of semantics) and minimality (not to be confused with
 * conciseness), and that namespaces are first-class citizens in yaidom. Precision may conflict with "correctness", in that
 * yaidom does not regard namespace declarations to be attributes, for example.
 *
 * Encourage the reader to play with Scala and yaidom in the REPL.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class Blog2Test extends Suite {

  private val pathToParentDir: java.io.File =
    (new java.io.File(classOf[Blog2Test].getResource("feed1.xml").toURI)).getParentFile

  /**
   * Finding element QNames and ENames in feed1.xml.
   */
  @Test def testFindNamesInFeed1(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val doc: Document =
      docParser.parse(new File(parentDir, "feed1.xml"))

    val docElem = doc.documentElement

    // The example, showing QName and ENames

    import ElemApi._

    // Asking for all (descendant-or-self) element QNames (using "default" yaidom elements)

    val elemQNames = docElem.findAllElemsOrSelf.map(_.qname).toSet

    assertResult(
      Set(
        QName("feed"),
        QName("title"),
        QName("rights"),
        QName("xhtml", "div"),
        QName("xhtml", "strong"),
        QName("xhtml", "em"))) {

        elemQNames
      }

    // Asking for all (descendant-or-self) element ENames (using "default" yaidom elements)

    val elemENames = docElem.findAllElemsOrSelf.map(_.resolvedName).toSet

    val atomNs = "http://www.w3.org/2005/Atom"
    val xhtmlNs = "http://www.w3.org/1999/xhtml"

    assertResult(
      Set(
        EName(atomNs, "feed"),
        EName(atomNs, "title"),
        EName(atomNs, "rights"),
        EName(xhtmlNs, "div"),
        EName(xhtmlNs, "strong"),
        EName(xhtmlNs, "em"))) {

        elemENames
      }
  }

  /**
   * Analyzing namespaces in feed1.xml, using scopes and declarations.
   */
  @Test def testNamespacesInFeed1(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val doc: Document =
      docParser.parse(new File(parentDir, "feed1.xml"))

    val docElem = doc.documentElement

    // The example, showing namespace resolution

    import ElemApi._

    // Using getChildElem, knowing that precisely one child element matches (otherwise an exception is thrown)

    val feedElem = docElem
    val titleElem = feedElem.getChildElem(withLocalName("title"))
    val rightsElem = feedElem.getChildElem(withLocalName("rights"))

    val atomElems = docElem \\ (e => e.qname.prefixOption.isEmpty)

    assertResult(Set(feedElem, titleElem, rightsElem)) {
      atomElems.toSet
    }

    val divElem = docElem.findElem(withLocalName("div")).get
    val strongElem = docElem.findElem(withLocalName("strong")).get
    val emElem = docElem.findElem(withLocalName("em")).get

    val xhtmlElems = docElem \\ (e => e.qname.prefixOption == Some("xhtml"))

    assertResult(Set(divElem, strongElem, emElem)) {
      xhtmlElems.toSet
    }

    // The Scope (in-scope namespaces) that all elements turn out to have

    val scope = Scope.from(
      "" -> "http://www.w3.org/2005/Atom",
      "xhtml" -> "http://www.w3.org/1999/xhtml",
      "my" -> "http://xmlportfolio.com/xmlguild-examples")

    // Calculating with scopes and (namespace) declarations

    val feedElemDecls = Declarations.from(
      "" -> "http://www.w3.org/2005/Atom",
      "xhtml" -> "http://www.w3.org/1999/xhtml",
      "my" -> "http://xmlportfolio.com/xmlguild-examples")

    // Indeed all elements have the same scope

    assertResult(scope) {
      Scope.Empty.resolve(feedElemDecls)
    }
    assertResult(Set(scope)) {
      feedElem.findAllElemsOrSelf.map(_.scope).toSet
    }

    // Checking the namespaces of "atom" and "xhtml" element names

    val defaultNs = scope.prefixNamespaceMap("")

    assertResult(Set(defaultNs)) {
      atomElems.flatMap(e => e.resolvedName.namespaceUriOption).toSet
    }

    val xhtmlNs = scope.prefixNamespaceMap("xhtml")

    assertResult(Set(xhtmlNs)) {
      xhtmlElems.flatMap(e => e.resolvedName.namespaceUriOption).toSet
    }

    // The default namespace does not affect unprefixed attributes
    // By the way, this is the first time we show attribute querying in yaidom

    val scopeWithoutDefaultNs = scope.withoutDefaultNamespace

    assertResult(scope.prefixNamespaceMap - "") {
      scopeWithoutDefaultNs.prefixNamespaceMap
    }
    assertResult(scopeWithoutDefaultNs) {
      feedElem.attributeScope
    }

    assertResult(Some("xhtml")) {
      rightsElem.attributeOption(EName("type"))
    }
    assertResult(Some("xhtml")) {
      rightsElem \@ EName("type")
    }
    assertResult(Some("silly")) {
      rightsElem \@ EName("http://xmlportfolio.com/xmlguild-examples", "type")
    }
    assertResult(Set(EName("type"), EName("http://xmlportfolio.com/xmlguild-examples", "type"))) {
      rightsElem.resolvedAttributes.toMap.keySet
    }
  }

  /**
   * Comparing feed1 and feed2 for equality.
   */
  @Test def testCompareFeed1AndFeed2(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val feed1Doc: Document =
      docParser.parse(new File(parentDir, "feed1.xml"))

    val feed2Doc: Document =
      docParser.parse(new File(parentDir, "feed2.xml"))

    // The example, showing equality

    import ElemApi._

    assertResult(resolved.Elem(feed1Doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(feed2Doc.documentElement).removeAllInterElementWhitespace
    }

    // Checking scope

    val atomNs = "http://www.w3.org/2005/Atom"
    val exampleNs = "http://xmlportfolio.com/xmlguild-examples"
    val xhtmlNs = "http://www.w3.org/1999/xhtml"

    val expectedDivElemScope =
      Scope.Empty.
        resolve(Declarations.from("" -> atomNs)).
        resolve(Declarations.from("example" -> exampleNs)).
        resolve(Declarations.from("" -> xhtmlNs))

    assertResult(Scope.from("example" -> exampleNs, "" -> xhtmlNs)) {
      expectedDivElemScope
    }

    val divElem = feed2Doc.documentElement.findElem(withLocalName("div")).get

    assertResult(Set(expectedDivElemScope)) {
      divElem.findAllElemsOrSelf.map(_.scope).toSet
    }
  }

  /**
   * Comparing feed1 and feed3 for equality.
   */
  @Test def testCompareFeed1AndFeed3(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val feed1Doc: Document =
      docParser.parse(new File(parentDir, "feed1.xml"))

    val feed3Doc: Document =
      docParser.parse(new File(parentDir, "feed3.xml"))

    // The example, showing equality

    import ElemApi._

    assertResult(resolved.Elem(feed1Doc.documentElement).removeAllInterElementWhitespace) {
      resolved.Elem(feed3Doc.documentElement).removeAllInterElementWhitespace
    }
  }

  /**
   * Checking some properties (in feed1.xml).
   */
  @Test def testPropertiesInFeed1(): Unit = {
    import java.io.File
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom._

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    // Replace the following path!
    val parentDir = new File(pathToParentDir.getPath)

    val doc: Document =
      docParser.parse(new File(parentDir, "feed1.xml"))

    val docElem = doc.documentElement

    // The example, checking some properties

    import ElemApi._

    val allElems = docElem.findAllElemsOrSelf

    // Each element has as resolved name the result of resolving its QName against the Scope of the element

    assertResult(true) {
      allElems.forall(elem => elem.scope.resolveQNameOption(elem.qname).get == elem.resolvedName)
    }

    // The attribute scope of each element is the scope without default namespace

    assertResult(true) {
      allElems forall (e => e.attributeScope == e.scope.withoutDefaultNamespace)
    }

    // Each element has as resolved attributes the result of resolving its attributes against the attribute scope of the element

    assertResult(true) {
      allElems forall { elem =>
        val attrs = elem.attributes
        val resolvedAttrs = attrs map {
          case (attrQName, attrValue) =>
            val resolvedAttrName = elem.attributeScope.resolveQNameOption(attrQName).get
            (resolvedAttrName -> attrValue)
        }

        resolvedAttrs.toMap == elem.resolvedAttributes.toMap
      }
    }
  }
}
