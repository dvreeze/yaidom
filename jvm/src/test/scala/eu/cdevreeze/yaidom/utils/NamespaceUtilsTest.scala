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

package eu.cdevreeze.yaidom.utils

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.queryapi.BackingElemApi
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.utils.NamespaceUtils.findAllENames
import eu.cdevreeze.yaidom.utils.NamespaceUtils.findAllNamespaces
import eu.cdevreeze.yaidom.utils.NamespaceUtils.findENamesInElementItself
import eu.cdevreeze.yaidom.utils.NamespaceUtils.findNamespacesInElementItself
import eu.cdevreeze.yaidom.utils.NamespaceUtils.pushUpPrefixedNamespaces
import eu.cdevreeze.yaidom.utils.NamespaceUtils.stripUnusedNamespaces
import org.scalatest.funsuite.AnyFunSuite

/**
 * NamespaceUtilsTest test case. See http://www.lenzconsulting.com/namespaces/ for the test data.
 *
 * @author Chris de Vreeze
 */
class NamespaceUtilsTest extends AnyFunSuite {

  private val docParser = DocumentParserUsingSax.newInstance

  test("testPushUpNamespacesInFeed1") {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("feed1.txt"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem.from(rootElem)) {
      resolved.Elem.from(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(true) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
  }

  test("testPushUpNamespacesInFeed2") {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("feed2.txt"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem.from(rootElem)) {
      resolved.Elem.from(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(false) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
    assertResult(true) {
      editedRootElemBuilder.findAllElems.forall(e => e.namespaces.prefixNamespaceMap.keySet.subsetOf(Set("")))
    }
    assertResult(true) {
      editedRootElemBuilder.namespaces.prefixNamespaceMap.contains("example")
    }
  }

  test("testPushUpNamespacesInFeed3") {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("feed3.txt"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem.from(rootElem)) {
      resolved.Elem.from(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(true) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
    assertResult(Set("", "xhtml", "my")) {
      editedRootElemBuilder.namespaces.prefixNamespaceMap.keySet
    }
  }

  test("testPushUpNamespacesInObfuscatedXml") {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("obfuscated.xml"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem.from(rootElem)) {
      resolved.Elem.from(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(false) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
    assertResult(Map("" -> "http://not-b", "a" -> "http://a", "b" -> "http://root-b")) {
      editedRootElemBuilder.namespaces.prefixNamespaceMap
    }
    assertResult(true) {
      editedRootElemBuilder.findAllChildElems.dropRight(1).forall(e => e.namespaces.prefixNamespaceMap.keySet.contains("b"))
    }
    assertResult(true) {
      editedRootElemBuilder.findAllChildElems.flatMap(e => e.findAllElems) forall { e =>
        e.namespaces.prefixNamespaceMap.keySet.subsetOf(Set("b")) &&
          e.namespaces.prefixNamespaceMap.forall(kv => kv._1 == "b" && kv._2 == "http://b")
      }
    }
  }

  test("testPushUpNamespacesInStrangeNsXml") {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("strange-ns.xml"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem.from(rootElem)) {
      resolved.Elem.from(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(true) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
    assertResult(Map("" -> "http://d", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c")) {
      editedRootElemBuilder.namespaces.prefixNamespaceMap
    }
  }

  test("testStripNamespaces") {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("content.xml"))
    val rootElem = indexed.Elem(doc.documentElement)

    assertResult(11) {
      rootElem.scope.prefixNamespaceMap.size
    }

    val lastTableOption = rootElem.filterElems(_.qname == QName("table:table")).lastOption

    assertResult(true) {
      lastTableOption.isDefined
    }

    val lastTable = lastTableOption.get

    assertResult(rootElem.scope) {
      lastTable.scope
    }
    assertResult(rootElem.scope.prefixNamespaceMap.keySet) {
      lastTable.findAllElemsOrSelf.flatMap(_.scope.prefixNamespaceMap.keySet).toSet
    }

    val editedLastTable = stripUnusedNamespaces(lastTable, DocumentENameExtractor.NoOp)

    assertResult(resolved.Elem.from(lastTable.underlyingElem)) {
      resolved.Elem.from(editedLastTable)
    }

    assertResult(Set("table")) {
      editedLastTable.findAllElemsOrSelf.flatMap(_.scope.prefixNamespaceMap.keySet).toSet
    }
  }

  test("testFindUsedNamespaces") {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("HelloWorld.xml"))
    val rootElem = indexed.Elem(doc.documentElement)

    val xbrliENameExtractor = new XbrliDocumentENameExtractor

    val measureElem = rootElem.findElem(_.resolvedName == EName(XbrliNs, "measure")).get

    assertResult(true) {
      findAllENames(rootElem, xbrliENameExtractor).contains(EName(Iso4217Ns, "USD"))
    }
    assertResult(true) {
      findAllNamespaces(rootElem, xbrliENameExtractor).contains(Iso4217Ns)
    }
    assertResult(EName(Iso4217Ns, "USD")) {
      measureElem.textAsResolvedQName
    }
    assertResult(true) {
      findENamesInElementItself(measureElem, xbrliENameExtractor).contains(EName(Iso4217Ns, "USD"))
    }
    assertResult(true) {
      findNamespacesInElementItself(measureElem, xbrliENameExtractor).contains(Iso4217Ns)
    }

    assertResult(7) {
      measureElem.scope.prefixNamespaceMap.size
    }
    assertResult(rootElem.scope) {
      measureElem.scope
    }
    assertResult(Scope.from("" -> XbrliNs, "xbrli" -> XbrliNs, "iso4217" -> Iso4217Ns)) {
      stripUnusedNamespaces(measureElem, xbrliENameExtractor).scope
    }
  }

  private val XbrliNs = "http://www.xbrl.org/2003/instance"
  private val Iso4217Ns = "http://www.xbrl.org/2003/iso4217"

  private val xbrliMeasureENameExtractor = SimpleTextENameExtractor

  final class XbrliDocumentENameExtractor extends DocumentENameExtractor {

    def findElemTextENameExtractor(elem: BackingElemApi): Option[TextENameExtractor] = {
      if (elem.rootElem.resolvedName == EName(XbrliNs, "xbrl") && elem.resolvedName == EName(XbrliNs, "measure")) {
        Some(xbrliMeasureENameExtractor)
      } else None
    }

    def findAttributeValueENameExtractor(elem: BackingElemApi, attributeEName: EName): Option[TextENameExtractor] = None
  }

}
