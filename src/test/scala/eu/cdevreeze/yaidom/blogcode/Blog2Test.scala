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

package eu.cdevreeze.yaidom.blogcode

import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ FunSuite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import eu.cdevreeze.yaidom.queryapi.HasENameApi

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
class Blog2Test extends FunSuite {

  private val pathToParentDir: java.io.File =
    (new java.io.File(classOf[Blog2Test].getResource("feed1.txt").toURI)).getParentFile

  /**
   * The code of the entire article.
   */
  test("testFindNamesInFeed1") {
    import java.net.URI
    import javax.xml.parsers._
    import scala.collection.immutable
    import eu.cdevreeze.yaidom.core._
    import eu.cdevreeze.yaidom.simple._
    import eu.cdevreeze.yaidom.parse
    import eu.cdevreeze.yaidom.resolved

    import HasENameApi._

    // Using a yaidom DocumentParser that used SAX internally
    val docParser = parse.DocumentParserUsingSax.newInstance

    val feed1Doc: Document =
      docParser.parse(new java.io.File(pathToParentDir, "feed1.txt"))

    val feed1DocElem = feed1Doc.documentElement

    val elemQNames = feed1DocElem.findAllElemsOrSelf.map(_.qname).toSet

    require(elemQNames ==
      Set(
        QName("feed"),
        QName("title"),
        QName("rights"),
        QName("xhtml", "div"),
        QName("xhtml", "strong"),
        QName("xhtml", "em")))

    // Writing the QNames differently, corresponding to the string representations:

    require(elemQNames ==
      Set(
        QName("feed"),
        QName("title"),
        QName("rights"),
        QName("xhtml:div"),
        QName("xhtml:strong"),
        QName("xhtml:em")))

    val elemENames = feed1DocElem.findAllElemsOrSelf.map(_.resolvedName).toSet

    val atomNs = "http://www.w3.org/2005/Atom"
    val xhtmlNs = "http://www.w3.org/1999/xhtml"

    require(elemENames ==
      Set(
        EName(atomNs, "feed"),
        EName(atomNs, "title"),
        EName(atomNs, "rights"),
        EName(xhtmlNs, "div"),
        EName(xhtmlNs, "strong"),
        EName(xhtmlNs, "em")))

    // Writing the ENames differently, using James Clark notation:

    require(elemENames ==
      Set(
        EName("{http://www.w3.org/2005/Atom}feed"),
        EName("{http://www.w3.org/2005/Atom}title"),
        EName("{http://www.w3.org/2005/Atom}rights"),
        EName("{http://www.w3.org/1999/xhtml}div"),
        EName("{http://www.w3.org/1999/xhtml}strong"),
        EName("{http://www.w3.org/1999/xhtml}em")))

    // Namespaces and in-scope namespaces

    val feed1ElemDecls = Declarations.from(
      "" -> "http://www.w3.org/2005/Atom",
      "xhtml" -> "http://www.w3.org/1999/xhtml",
      "my" -> "http://xmlportfolio.com/xmlguild-examples")

    val feed1ElemScope = Scope.Empty.resolve(feed1ElemDecls)

    val expectedFeed1ElemScope = Scope.from(
      "" -> "http://www.w3.org/2005/Atom",
      "xhtml" -> "http://www.w3.org/1999/xhtml",
      "my" -> "http://xmlportfolio.com/xmlguild-examples")

    require(feed1ElemScope == expectedFeed1ElemScope)

    require(feed1DocElem.findAllElemsOrSelf.forall(e => e.scope == feed1ElemScope))

    val allElems = feed1DocElem.findAllElemsOrSelf

    // The default namespace is the atom namespace
    val allAtomElems = allElems.filter(e => e.qname.prefixOption.isEmpty)

    require(allAtomElems.forall(e =>
      e.scope.resolveQNameOption(e.qname) == Some(e.resolvedName)))

    // Indeed, the ENames are in the atom namespace
    require(allAtomElems.forall(e =>
      e.resolvedName.namespaceUriOption == Some(atomNs)))

    val allXhtmlElems = allElems.filter(e => e.qname.prefixOption == Some("xhtml"))

    require(allXhtmlElems.forall(e =>
      e.scope.resolveQNameOption(e.qname) == Some(e.resolvedName)))

    // Indeed, the ENames are in the xhtml namespace
    require(allXhtmlElems.forall(e =>
      e.resolvedName.namespaceUriOption == Some(xhtmlNs)))

    require(feed1DocElem.findAllElemsOrSelf.forall(e =>
      e.scope.resolveQNameOption(e.qname) == Some(e.resolvedName)))

    require(feed1DocElem.findAllElemsOrSelf.forall(e =>
      e.resolvedName.localPart == e.qname.localPart))

    require(feed1DocElem.findAllElemsOrSelf.forall(e =>
      e.resolvedName.namespaceUriOption ==
        e.scope.prefixNamespaceMap.get(e.qname.prefixOption.getOrElse(""))))

    // Attribute querying and resolution

    // Get the rights child element of the root element
    val rights1Elem: Elem = feed1DocElem.getChildElem(withEName(atomNs, "rights"))

    require(rights1Elem \@ EName("type") == Some("xhtml"))

    val examplesNs = "http://xmlportfolio.com/xmlguild-examples"

    require(rights1Elem \@ EName(examplesNs, "type") == Some("silly"))

    val rights1ElemAttrs = rights1Elem.attributes

    require(rights1ElemAttrs.toMap.keySet ==
      Set(QName("type"), QName("my", "type")))

    val rights1ElemResolvedAttrs = rights1Elem.resolvedAttributes

    require(rights1ElemResolvedAttrs.toMap.keySet ==
      Set(EName("type"), EName(examplesNs, "type")))

    require {
      feed1DocElem.findAllElemsOrSelf forall { elem =>
        val attrs = elem.attributes
        val resolvedAttrs = attrs map {
          case (attrQName, attrValue) =>
            val resolvedAttrName = elem.attributeScope.resolveQNameOption(attrQName).get
            (resolvedAttrName -> attrValue)
        }

        resolvedAttrs.toMap == elem.resolvedAttributes.toMap
      }
    }

    require(feed1DocElem.findAllElemsOrSelf.forall(e =>
      e.attributeScope == e.scope.withoutDefaultNamespace))

    // Equivalent XML documents

    val feed2Doc: Document =
      docParser.parse(new java.io.File(pathToParentDir, "feed2.txt"))

    val feed2DocElem = feed2Doc.documentElement

    val div2Elem = feed2DocElem.findElem(withEName(xhtmlNs, "div")).get

    val feed2ElemDecls = Declarations.from("" -> atomNs)
    val rights2ElemDecls = Declarations.from("example" -> examplesNs)
    val div2ElemDecls = Declarations.from("" -> xhtmlNs)

    val div2ElemScope =
      Scope.Empty.resolve(feed2ElemDecls).resolve(rights2ElemDecls).resolve(div2ElemDecls)

    require(div2ElemScope == Scope.from("" -> xhtmlNs, "example" -> examplesNs))

    val feed1ResolvedElem = resolved.Elem(feed1DocElem)
    val feed2ResolvedElem = resolved.Elem(feed2DocElem)

    require(feed1ResolvedElem.removeAllInterElementWhitespace ==
      feed2ResolvedElem.removeAllInterElementWhitespace)

    require(feed1DocElem.findAllElemsOrSelf.map(_.resolvedName) ==
      feed2DocElem.findAllElemsOrSelf.map(_.resolvedName))

    require(feed1DocElem.findAllElemsOrSelf.map(_.resolvedAttributes) ==
      feed2DocElem.findAllElemsOrSelf.map(_.resolvedAttributes))

    val feed3Doc: Document =
      docParser.parse(new java.io.File(pathToParentDir, "feed3.txt"))

    val feed3DocElem = feed3Doc.documentElement

    val div3Elem = feed3DocElem.findElem(withEName(xhtmlNs, "div")).get

    val feed3ElemDecls =
      Declarations.from("" -> atomNs, "xhtml" -> xhtmlNs, "my" -> examplesNs)
    val rights3ElemDecls = feed3ElemDecls
    val div3ElemDecls = Declarations.from("xhtml" -> xhtmlNs, "my" -> examplesNs)

    val div3ElemScope =
      Scope.Empty.resolve(feed3ElemDecls).resolve(rights3ElemDecls).resolve(div3ElemDecls)

    require(div3ElemScope ==
      Scope.from("" -> atomNs, "xhtml" -> xhtmlNs, "my" -> examplesNs))

    // The namespace declarations in the rights and div elements added no information
    require(
      feed3DocElem.getChildElem(withEName(atomNs, "rights")).scope ==
        feed3DocElem.scope)
    require(div3Elem.scope == feed3DocElem.scope)

    val feed3ResolvedElem = resolved.Elem(feed3DocElem)

    require(feed1ResolvedElem.removeAllInterElementWhitespace ==
      feed3ResolvedElem.removeAllInterElementWhitespace)
  }
}
