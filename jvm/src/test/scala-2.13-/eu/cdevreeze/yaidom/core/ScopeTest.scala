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

package eu.cdevreeze.yaidom.core

import scala.collection.JavaConverters.asScalaIteratorConverter

import eu.cdevreeze.yaidom.core.jvm.NamespaceContexts
import org.scalatest.FunSuite

/**
 * Scope test case.
 *
 * @author Chris de Vreeze
 */
class ScopeTest extends FunSuite {

  test("testCreateScope") {
    intercept[Exception] {
      Scope.from("" -> null)
    }
    intercept[Exception] {
      val prefix: String = null
      Scope.from(prefix -> "a")
    }
    intercept[Exception] {
      Scope.from("a" -> "")
    }
    assertResult(Map("" -> "http://a")) {
      Scope.from("" -> "http://a").prefixNamespaceMap
    }
    assertResult(Map("" -> "http://a", "b" -> "http://b")) {
      Scope.from("" -> "http://a", "b" -> "http://b").prefixNamespaceMap
    }
    assertResult(Some("http://a")) {
      Scope.from("" -> "http://a", "b" -> "http://b").defaultNamespaceOption
    }
    assertResult(None) {
      Scope.from("b" -> "http://b").defaultNamespaceOption
    }
    assertResult(Map("b" -> "http://b")) {
      Scope.from("b" -> "http://b").withoutDefaultNamespace.prefixNamespaceMap
    }
  }

  test("testCreateDeclarations") {
    intercept[Exception] {
      Declarations.from("" -> null)
    }
    intercept[Exception] {
      val prefix: String = null
      Declarations.from(prefix -> "a")
    }
    assertResult(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").prefixNamespaceMap
    }
    assertResult(Map()) {
      Declarations.from("" -> "http://a").withoutDefaultNamespace.prefixNamespaceMap
    }
    assertResult(Map("a" -> "")) {
      Declarations.from("a" -> "").prefixNamespaceMap
    }
    assertResult(Map()) {
      Declarations.from("a" -> "").withoutUndeclarations.prefixNamespaceMap
    }
    assertResult(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").prefixNamespaceMap
    }
    assertResult(Map("" -> "http://a", "b" -> "http://b")) {
      Declarations.from("" -> "http://a", "b" -> "http://b").prefixNamespaceMap
    }
    assertResult(Scope.from("" -> "http://a", "b" -> "http://b").prefixNamespaceMap) {
      Declarations.from("" -> "http://a", "b" -> "http://b").withoutUndeclarations.prefixNamespaceMap
    }
    assertResult(Map("b" -> "http://b")) {
      Declarations.from("b" -> "http://b").prefixNamespaceMap
    }

    val decls1 = Declarations.from("" -> "http://a", "b" -> "http://b", "c" -> "")

    assertResult(Declarations.from("b" -> "http://b")) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
    assertResult(decls1.withoutDefaultNamespace.withoutUndeclarations) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
  }

  test("testResolveDeclarations") {
    val scope1 = Scope.Empty

    assertResult(0) {
      scope1.prefixNamespaceMap.size
    }

    val declarations2 = Declarations.from("" -> "http://a", "a" -> "http://a", "x" -> "")
    val scope2 = scope1.resolve(declarations2)

    assertResult(Some("http://a")) {
      scope2.defaultNamespaceOption
    }
    assertResult(Map("a" -> "http://a")) {
      scope2.withoutDefaultNamespace.prefixNamespaceMap
    }
    assertResult(Declarations.from("" -> "http://a", "a" -> "http://a")) {
      scope1.relativize(scope2)
    }
    assertResult(true) {
      scope1.subScopeOf(scope1)
    }
    assertResult(true) {
      scope1.subScopeOf(scope2)
    }
    assertResult(false) {
      scope2.subScopeOf(scope1)
    }
    assertResult(Scope.Empty) {
      scope2.resolve(Declarations.from("" -> "")).resolve(Declarations.from("a" -> ""))
    }
    assertResult(scope2) {
      Scope.from(declarations2.withoutUndeclarations.prefixNamespaceMap)
    }
    assertResult(scope2) {
      scope1.resolve(scope1.relativize(scope2))
    }
    assertResult(declarations2 -- Set("x")) {
      scope1.minimize(declarations2)
    }
    assertResult(scope1.minimize(declarations2)) {
      scope1.relativize(scope1.resolve(declarations2))
    }

    val declarations3 = Declarations.from("a" -> "http://a", "b" -> "http://b", "c" -> "http://c")
    val scope3 = scope2.resolve(declarations3)

    assertResult(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c")) {
      scope3.prefixNamespaceMap
    }
    assertResult(scope2.minimize(declarations3)) {
      scope2.relativize(scope3)
    }
    assertResult(true) {
      scope2.subScopeOf(scope3)
    }
    assertResult(false) {
      scope3.subScopeOf(scope2)
    }
    assertResult(scope2) {
      scope3.resolve(Declarations.from("b" -> "", "c" -> ""))
    }
    assertResult(scope3) {
      Scope.from((scope2.prefixNamespaceMap ++ declarations3.withoutUndeclarations.prefixNamespaceMap) -- declarations3.retainingUndeclarations.prefixNamespaceMap.keySet)
    }
    assertResult(scope3) {
      scope2.resolve(scope2.relativize(scope3))
    }
    assertResult(declarations3 -- Set("a")) {
      scope2.minimize(declarations3)
    }
    assertResult(scope2.minimize(declarations3)) {
      scope2.relativize(scope2.resolve(declarations3))
    }

    val declarations4 = Declarations.from("c" -> "http://ccc", "d" -> "http://d")
    val scope4 = scope3.resolve(declarations4)

    assertResult(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.prefixNamespaceMap
    }
    assertResult(Map("a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.withoutDefaultNamespace.prefixNamespaceMap
    }
    assertResult(declarations4) {
      scope3.relativize(scope4)
    }
    assertResult(false) {
      scope3.subScopeOf(scope4)
    }
    assertResult(false) {
      scope4.subScopeOf(scope3)
    }
    assertResult(scope3) {
      scope4.resolve(Declarations.from("c" -> "http://c", "d" -> ""))
    }
    assertResult(scope2) {
      scope4.resolve(Declarations.from("b" -> "", "c" -> "", "d" -> ""))
    }
    assertResult(scope1) {
      scope4.resolve(Declarations.from("" -> "", "a" -> "", "b" -> "", "c" -> "", "d" -> ""))
    }
    assertResult(Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4
    }
    assertResult(scope4) {
      Scope.from((scope3.prefixNamespaceMap ++ declarations4.withoutUndeclarations.prefixNamespaceMap) -- declarations4.retainingUndeclarations.prefixNamespaceMap.keySet)
    }
    assertResult(scope4) {
      scope3.resolve(scope3.relativize(scope4))
    }
    assertResult(declarations4) {
      scope3.minimize(declarations4)
    }
    assertResult(scope3.minimize(declarations4)) {
      scope3.relativize(scope3.resolve(declarations4))
    }
  }

  test("testResolveQName") {
    val scope1 = Scope.from()

    assertResult(Some(EName("book"))) {
      scope1.resolveQNameOption(QName("book"))
    }

    assertResult(None) {
      scope1.resolveQNameOption(QName("book:book"))
    }

    val scope2 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    assertResult(Some(EName("{http://a}book"))) {
      scope2.resolveQNameOption(QName("book"))
    }

    assertResult(None) {
      scope2.resolveQNameOption(QName("book:book"))
    }

    assertResult(Some(EName("{http://a}book"))) {
      scope2.resolveQNameOption(QName("a:book"))
    }

    assertResult(Some(EName("{http://ccc}bookstore"))) {
      scope2.resolveQNameOption(QName("c:bookstore"))
    }

    assertResult(Some(EName("{http://www.w3.org/XML/1998/namespace}lang"))) {
      scope2.resolveQNameOption(QName("xml:lang"))
    }
  }

  test("testResolveQNameNotUndeclaringPrefixes") {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d")
    val scope2 = Scope.from("" -> "http://e", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    assertResult(Declarations.from("" -> "http://e", "a" -> "", "c" -> "http://ccc")) {
      scope1.relativize(scope2)
    }

    assertResult(scope2 ++ Scope.from("a" -> "http://a")) {
      scope1.withoutDefaultNamespace ++ scope2
    }

    val qnames = List(QName("x"), QName("b", "y"), QName("c", "z"), QName("d", "z"))

    val expectedENames = List(EName("http://e", "x"), EName("http://b", "y"), EName("http://ccc", "z"), EName("http://d", "z"))

    assertResult(expectedENames) {
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error(s"QName $qname not resolved")) }
    }

    assertResult(expectedENames) {
      qnames map { qname =>
        (scope1.withoutDefaultNamespace ++ scope2).resolveQNameOption(qname).getOrElse(
          sys.error(s"QName $qname not resolved"))
      }
    }
  }

  test("testResolveQNameNotUndeclaringNamespaces") {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d")
    val scope2 = Scope.from("b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    assertResult(Declarations.from("" -> "", "a" -> "", "c" -> "http://ccc")) {
      scope1.relativize(scope2)
    }

    assertResult(scope2 ++ Scope.from("a" -> "http://a") ++ scope1.retainingDefaultNamespace) {
      scope1 ++ scope2
    }

    assertResult(Some(EName("x"))) {
      scope2.resolveQNameOption(QName("x"))
    }

    assertResult(Some(EName("http://a", "x"))) {
      (scope1 ++ scope2).resolveQNameOption(QName("x"))
    }

    val qnames = List(QName("b", "y"), QName("c", "z"), QName("d", "z"))

    val expectedENames = List(EName("http://b", "y"), EName("http://ccc", "z"), EName("http://d", "z"))

    assertResult(expectedENames) {
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error(s"QName $qname not resolved")) }
    }

    assertResult(expectedENames) {
      qnames map { qname =>
        (scope1 ++ scope2).resolveQNameOption(qname).getOrElse(sys.error(s"QName $qname not resolved"))
      }
    }
  }

  test("testInverse") {
    val scope =
      Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "dd" -> "http://d")

    val scopeInverse = scope.inverse

    val expectedScopeInverse = Map(
      "http://a" -> Set("", "a"),
      "http://b" -> Set("b"),
      "http://c" -> Set("c"),
      "http://d" -> Set("d", "dd"))

    assertResult(expectedScopeInverse) {
      scopeInverse
    }
  }

  test("testPrefixesForNamespace") {
    val scope =
      Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "dd" -> "http://d")

    assertResult(Set("a", "")) {
      scope.prefixesForNamespace("http://a")
    }
    assertResult(Set()) {
      scope.prefixesForNamespace("http://abc")
    }
    assertResult(Set("c")) {
      scope.prefixesForNamespace("http://c")
    }
    assertResult(Set("d", "dd")) {
      scope.prefixesForNamespace("http://d")
    }
  }

  test("testPropertyAboutMultipleScopes") {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "h" -> "http://h")
    val scope2 = Scope.from("b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d", "e" -> "http://e")

    assertResult(scope2) {
      scope1.resolve(scope1.relativize(scope2))
    }

    // Prefix has the same mappings in scope1 and scope2

    testPropertyAboutMultipleScopes(scope1, scope2, "b")
    testPropertyAboutMultipleScopes(scope1, scope2, "d")

    assertResult(Some("http://b")) {
      scope2.prefixNamespaceMap.get("b")
    }

    // Prefix has different mappings in scope1 and scope2

    testPropertyAboutMultipleScopes(scope1, scope2, "c")

    assertResult(Some("http://ccc")) {
      scope2.prefixNamespaceMap.get("c")
    }

    // Prefix only belongs to scope1

    testPropertyAboutMultipleScopes(scope1, scope2, "")
    testPropertyAboutMultipleScopes(scope1, scope2, "h")

    assertResult(None) {
      scope2.prefixNamespaceMap.get("h")
    }

    // Prefix only belongs to scope2

    testPropertyAboutMultipleScopes(scope1, scope2, "e")

    assertResult(Some("http://e")) {
      scope2.prefixNamespaceMap.get("e")
    }

    // Prefix belongs to neither scope

    testPropertyAboutMultipleScopes(scope1, scope2, "k")

    assertResult(None) {
      scope2.prefixNamespaceMap.get("k")
    }
  }

  test("testPropertyAboutScopeAndDeclaration") {
    val scope = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "g" -> "http://g", "h" -> "http://h")
    val decls = Declarations.from("b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d", "g" -> "", "e" -> "http://e", "m" -> "")

    assertResult(scope.minimize(decls)) {
      scope.relativize(scope.resolve(decls))
    }

    assertResult(decls -- Set("b", "d", "m")) {
      scope.minimize(decls)
    }

    // Prefix has the same mappings in scope and declarations (so no undeclaration)

    testPropertyAboutScopeAndDeclaration(scope, decls, "b")
    testPropertyAboutScopeAndDeclaration(scope, decls, "d")

    assertResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("b")
    }
    assertResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("d")
    }

    // Prefix has different mappings in scope and declarations (but no undeclaration)

    testPropertyAboutScopeAndDeclaration(scope, decls, "c")

    assertResult(Some("http://ccc")) {
      scope.minimize(decls).prefixNamespaceMap.get("c")
    }

    // Prefix belongs to scope and is undeclared in declarations

    testPropertyAboutScopeAndDeclaration(scope, decls, "g")

    assertResult(Some("")) {
      scope.minimize(decls).prefixNamespaceMap.get("g")
    }

    // Prefix only belongs to scope, and does not occur in declarations

    testPropertyAboutScopeAndDeclaration(scope, decls, "")
    testPropertyAboutScopeAndDeclaration(scope, decls, "h")

    assertResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("")
    }
    assertResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("h")
    }

    // Prefix only occurs in declarations, without being undeclared, and does not occur in scope

    testPropertyAboutScopeAndDeclaration(scope, decls, "e")

    assertResult(Some("http://e")) {
      scope.minimize(decls).prefixNamespaceMap.get("e")
    }

    // Prefix only occurs in declarations, in an undeclaration, and does not occur in scope

    testPropertyAboutScopeAndDeclaration(scope, decls, "m")

    assertResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("m")
    }

    // Prefix neither occurs in scope nor in declarations

    testPropertyAboutScopeAndDeclaration(scope, decls, "x")

    assertResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("x")
    }
  }

  test("testIncludeNamespace") {
    val scope =
      Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "dd" -> "http://d")

    def getPrefix(ns: String): String = ns match {
      case "http://e" => "e"
      case "http://f" => ""
      case _ => sys.error(s"Unknown namespace: $ns")
    }

    assertResult("a") {
      val ns = "http://a"
      scope.prefixForNamespace(ns, () => getPrefix(ns))
    }
    assertResult(scope) {
      val ns = "http://a"
      scope.includingNamespace(ns, () => getPrefix(ns))
    }

    assertResult("") {
      val ns = "http://a"
      (scope -- Set("a")).prefixForNamespace(ns, () => getPrefix(ns))
    }
    assertResult(scope -- Set("a")) {
      val ns = "http://a"
      (scope -- Set("a")).includingNamespace(ns, () => getPrefix(ns))
    }

    assertResult("b") {
      val ns = "http://b"
      scope.prefixForNamespace(ns, () => getPrefix(ns))
    }
    assertResult(scope) {
      val ns = "http://b"
      scope.includingNamespace(ns, () => getPrefix(ns))
    }

    assertResult(true) {
      val ns = "http://d"
      Set("d", "dd").contains(scope.prefixForNamespace(ns, () => getPrefix(ns)))
    }
    assertResult(scope) {
      val ns = "http://d"
      scope.includingNamespace(ns, () => getPrefix(ns))
    }

    assertResult("e") {
      val ns = "http://e"
      scope.prefixForNamespace(ns, () => getPrefix(ns))
    }
    assertResult(scope ++ Scope.from("e" -> "http://e")) {
      val ns = "http://e"
      scope.includingNamespace(ns, () => getPrefix(ns))
    }
    assertResult(true) {
      val ns = "http://e"
      scope.subScopeOf(scope.includingNamespace(ns, () => getPrefix(ns)))
    }

    assertResult("") {
      val ns = "http://f"
      scope.withoutDefaultNamespace.prefixForNamespace(ns, () => getPrefix(ns))
    }
    assertResult(scope.withoutDefaultNamespace ++ Scope.from("" -> "http://f")) {
      val ns = "http://f"
      scope.withoutDefaultNamespace.includingNamespace(ns, () => getPrefix(ns))
    }
    assertResult(true) {
      val ns = "http://f"
      scope.withoutDefaultNamespace.subScopeOf(scope.withoutDefaultNamespace.includingNamespace(ns, () => getPrefix(ns)))
    }

    intercept[Exception] {
      val ns = "http://f"
      scope.prefixForNamespace(ns, () => getPrefix(ns))
    }
    intercept[Exception] {
      val ns = "http://f"
      scope.includingNamespace(ns, () => getPrefix(ns))
    }

    assertResult("xml") {
      val ns = Scope.XmlNamespace
      scope.prefixForNamespace(ns, () => getPrefix(ns))
    }
    assertResult(scope) {
      val ns = Scope.XmlNamespace
      scope.includingNamespace(ns, () => getPrefix(ns))
    }
  }

  test("testConvertToNamespaceContext") {
    val scope =
      Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "dd" -> "http://d")
    val namespaceContext = NamespaceContexts.scopeToNamespaceContext(scope)

    assertResult("http://a") {
      namespaceContext.getNamespaceURI("")
    }
    assertResult("http://a") {
      namespaceContext.getNamespaceURI("a")
    }
    assertResult("http://b") {
      namespaceContext.getNamespaceURI("b")
    }
    assertResult("http://c") {
      namespaceContext.getNamespaceURI("c")
    }
    assertResult("http://d") {
      namespaceContext.getNamespaceURI("d")
    }
    assertResult("http://d") {
      namespaceContext.getNamespaceURI("dd")
    }
    assertResult("") {
      namespaceContext.getNamespaceURI("abc")
    }
    assertResult("http://www.w3.org/XML/1998/namespace") {
      namespaceContext.getNamespaceURI("xml")
    }
    assertResult("http://www.w3.org/2000/xmlns/") {
      namespaceContext.getNamespaceURI("xmlns")
    }
    assertResult("") {
      NamespaceContexts.scopeToNamespaceContext(scope.withoutDefaultNamespace).getNamespaceURI("")
    }

    assertResult(Set("a", "")) {
      namespaceContext.getPrefixes("http://a").asScala.toSet
    }
    assertResult(Set()) {
      namespaceContext.getPrefixes("http://abc").asScala.toSet
    }
    assertResult(Set("c")) {
      namespaceContext.getPrefixes("http://c").asScala.toSet
    }
    assertResult(Set("d", "dd")) {
      namespaceContext.getPrefixes("http://d").asScala.toSet
    }
    assertResult(Set("xml")) {
      namespaceContext.getPrefixes("http://www.w3.org/XML/1998/namespace").asScala.toSet
    }
    assertResult(Set("xmlns")) {
      namespaceContext.getPrefixes("http://www.w3.org/2000/xmlns/").asScala.toSet
    }

    assertResult(true) {
      Set("a", "").contains(namespaceContext.getPrefix("http://a"))
    }
    assertResult(null) {
      namespaceContext.getPrefix("http://abc")
    }
    assertResult(true) {
      Set("c").contains(namespaceContext.getPrefix("http://c"))
    }
    assertResult(true) {
      Set("d", "dd").contains(namespaceContext.getPrefix("http://d"))
    }
    assertResult("xml") {
      namespaceContext.getPrefix("http://www.w3.org/XML/1998/namespace")
    }
    assertResult("xmlns") {
      namespaceContext.getPrefix("http://www.w3.org/2000/xmlns/")
    }
  }

  private def testPropertyAboutMultipleScopes(scope1: Scope, scope2: Scope, prefix: String): Unit = {
    assertResult(scope2.prefixNamespaceMap.get(prefix)) {
      scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(prefix)
    }
  }

  private def testPropertyAboutScopeAndDeclaration(scope: Scope, declarations: Declarations, prefix: String): Unit = {
    assertResult(scope.minimize(declarations).prefixNamespaceMap.get(prefix)) {
      scope.relativize(scope.resolve(declarations)).prefixNamespaceMap.get(prefix)
    }
  }
}
