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

package eu.cdevreeze.yaidom.utils

import java.{ util => jutil }

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.Suite
import org.scalatest.junit.JUnitRunner

import NamespaceUtils.pushUpPrefixedNamespaces
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.parse.DocumentParserUsingSax
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.NodeBuilder

/**
 * NamespaceUtilsTest test case. See http://www.lenzconsulting.com/namespaces/ for the test data.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class NamespaceUtilsTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.utils")

  private val docParser = DocumentParserUsingSax.newInstance

  @Test def testPushUpNamespacesInFeed1(): Unit = {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("feed1.txt"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(true) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
  }

  @Test def testPushUpNamespacesInFeed2(): Unit = {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("feed2.txt"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
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

  @Test def testPushUpNamespacesInFeed3(): Unit = {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("feed3.txt"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(true) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
    assertResult(Set("", "xhtml", "my")) {
      editedRootElemBuilder.namespaces.prefixNamespaceMap.keySet
    }
  }

  @Test def testPushUpNamespacesInObfuscatedXml(): Unit = {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("obfuscated.xml"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
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

  @Test def testPushUpNamespacesInStrangeNsXml(): Unit = {
    val doc = docParser.parse(classOf[NamespaceUtilsTest].getResourceAsStream("strange-ns.xml"))
    val rootElem = doc.documentElement

    val editedRootElem = pushUpPrefixedNamespaces(rootElem)

    assertResult(resolved.Elem(rootElem)) {
      resolved.Elem(editedRootElem)
    }

    val editedRootElemBuilder = NodeBuilder.fromElem(editedRootElem)(Scope.Empty)

    assertResult(true) {
      editedRootElemBuilder.allDeclarationsAreAtTopLevel
    }
    assertResult(Map("" -> "http://d", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c")) {
      editedRootElemBuilder.namespaces.prefixNamespaceMap
    }
  }
}
