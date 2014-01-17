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

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * Scope test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScopeTest extends Suite {

  @Test def testCreateScope(): Unit = {
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
    expectResult(Map("" -> "http://a")) {
      Scope.from("" -> "http://a").prefixNamespaceMap
    }
    expectResult(Map("" -> "http://a", "b" -> "http://b")) {
      Scope.from("" -> "http://a", "b" -> "http://b").prefixNamespaceMap
    }
    expectResult(Some("http://a")) {
      Scope.from("" -> "http://a", "b" -> "http://b").defaultNamespaceOption
    }
    expectResult(None) {
      Scope.from("b" -> "http://b").defaultNamespaceOption
    }
    expectResult(Map("b" -> "http://b")) {
      Scope.from("b" -> "http://b").withoutDefaultNamespace.prefixNamespaceMap
    }
  }

  @Test def testCreateDeclarations(): Unit = {
    intercept[Exception] {
      Declarations.from("" -> null)
    }
    intercept[Exception] {
      val prefix: String = null
      Declarations.from(prefix -> "a")
    }
    expectResult(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").prefixNamespaceMap
    }
    expectResult(Map()) {
      Declarations.from("" -> "http://a").withoutDefaultNamespace.prefixNamespaceMap
    }
    expectResult(Map("a" -> "")) {
      Declarations.from("a" -> "").prefixNamespaceMap
    }
    expectResult(Map()) {
      Declarations.from("a" -> "").withoutUndeclarations.prefixNamespaceMap
    }
    expectResult(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").prefixNamespaceMap
    }
    expectResult(Map("" -> "http://a", "b" -> "http://b")) {
      Declarations.from("" -> "http://a", "b" -> "http://b").prefixNamespaceMap
    }
    expectResult(Scope.from("" -> "http://a", "b" -> "http://b").prefixNamespaceMap) {
      Declarations.from("" -> "http://a", "b" -> "http://b").withoutUndeclarations.prefixNamespaceMap
    }
    expectResult(Map("b" -> "http://b")) {
      Declarations.from("b" -> "http://b").prefixNamespaceMap
    }

    val decls1 = Declarations.from("" -> "http://a", "b" -> "http://b", "c" -> "")

    expectResult(Declarations.from("b" -> "http://b")) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
    expectResult(decls1.withoutDefaultNamespace.withoutUndeclarations) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
  }

  @Test def testResolveDeclarations(): Unit = {
    val scope1 = Scope.Empty

    expectResult(0) {
      scope1.prefixNamespaceMap.size
    }

    val declarations2 = Declarations.from("" -> "http://a", "a" -> "http://a", "x" -> "")
    val scope2 = scope1.resolve(declarations2)

    expectResult(Some("http://a")) {
      scope2.defaultNamespaceOption
    }
    expectResult(Map("a" -> "http://a")) {
      scope2.withoutDefaultNamespace.prefixNamespaceMap
    }
    expectResult(Declarations.from("" -> "http://a", "a" -> "http://a")) {
      scope1.relativize(scope2)
    }
    expectResult(true) {
      scope1.subScopeOf(scope1)
    }
    expectResult(true) {
      scope1.subScopeOf(scope2)
    }
    expectResult(false) {
      scope2.subScopeOf(scope1)
    }
    expectResult(Scope.Empty) {
      scope2.resolve(Declarations.from("" -> "")).resolve(Declarations.from("a" -> ""))
    }
    expectResult(scope2) {
      Scope.from(declarations2.withoutUndeclarations.prefixNamespaceMap)
    }
    expectResult(scope2) {
      scope1.resolve(scope1.relativize(scope2))
    }
    expectResult(declarations2 -- Set("x")) {
      scope1.minimize(declarations2)
    }
    expectResult(scope1.minimize(declarations2)) {
      scope1.relativize(scope1.resolve(declarations2))
    }

    val declarations3 = Declarations.from("a" -> "http://a", "b" -> "http://b", "c" -> "http://c")
    val scope3 = scope2.resolve(declarations3)

    expectResult(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c")) {
      scope3.prefixNamespaceMap
    }
    expectResult(scope2.minimize(declarations3)) {
      scope2.relativize(scope3)
    }
    expectResult(true) {
      scope2.subScopeOf(scope3)
    }
    expectResult(false) {
      scope3.subScopeOf(scope2)
    }
    expectResult(scope2) {
      scope3.resolve(Declarations.from("b" -> "", "c" -> ""))
    }
    expectResult(scope3) {
      Scope.from((scope2.prefixNamespaceMap ++ declarations3.withoutUndeclarations.prefixNamespaceMap) -- declarations3.retainingUndeclarations.prefixNamespaceMap.keySet)
    }
    expectResult(scope3) {
      scope2.resolve(scope2.relativize(scope3))
    }
    expectResult(declarations3 -- Set("a")) {
      scope2.minimize(declarations3)
    }
    expectResult(scope2.minimize(declarations3)) {
      scope2.relativize(scope2.resolve(declarations3))
    }

    val declarations4 = Declarations.from("c" -> "http://ccc", "d" -> "http://d")
    val scope4 = scope3.resolve(declarations4)

    expectResult(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.prefixNamespaceMap
    }
    expectResult(Map("a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.withoutDefaultNamespace.prefixNamespaceMap
    }
    expectResult(declarations4) {
      scope3.relativize(scope4)
    }
    expectResult(false) {
      scope3.subScopeOf(scope4)
    }
    expectResult(false) {
      scope4.subScopeOf(scope3)
    }
    expectResult(scope3) {
      scope4.resolve(Declarations.from("c" -> "http://c", "d" -> ""))
    }
    expectResult(scope2) {
      scope4.resolve(Declarations.from("b" -> "", "c" -> "", "d" -> ""))
    }
    expectResult(scope1) {
      scope4.resolve(Declarations.from("" -> "", "a" -> "", "b" -> "", "c" -> "", "d" -> ""))
    }
    expectResult(Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4
    }
    expectResult(scope4) {
      Scope.from((scope3.prefixNamespaceMap ++ declarations4.withoutUndeclarations.prefixNamespaceMap) -- declarations4.retainingUndeclarations.prefixNamespaceMap.keySet)
    }
    expectResult(scope4) {
      scope3.resolve(scope3.relativize(scope4))
    }
    expectResult(declarations4) {
      scope3.minimize(declarations4)
    }
    expectResult(scope3.minimize(declarations4)) {
      scope3.relativize(scope3.resolve(declarations4))
    }
  }

  @Test def testResolveQName(): Unit = {
    val scope1 = Scope.from()

    expectResult(Some(EName("book"))) {
      scope1.resolveQNameOption(QName("book"))
    }

    expectResult(None) {
      scope1.resolveQNameOption(QName("book:book"))
    }

    val scope2 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    expectResult(Some(EName("{http://a}book"))) {
      scope2.resolveQNameOption(QName("book"))
    }

    expectResult(None) {
      scope2.resolveQNameOption(QName("book:book"))
    }

    expectResult(Some(EName("{http://a}book"))) {
      scope2.resolveQNameOption(QName("a:book"))
    }

    expectResult(Some(EName("{http://ccc}bookstore"))) {
      scope2.resolveQNameOption(QName("c:bookstore"))
    }

    expectResult(Some(EName("{http://www.w3.org/XML/1998/namespace}lang"))) {
      scope2.resolveQNameOption(QName("xml:lang"))
    }
  }

  @Test def testResolveQNameNotUndeclaringPrefixes(): Unit = {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d")
    val scope2 = Scope.from("" -> "http://e", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    expectResult(Declarations.from("" -> "http://e", "a" -> "", "c" -> "http://ccc")) {
      scope1.relativize(scope2)
    }

    expectResult(scope2 ++ Scope.from("a" -> "http://a")) {
      scope1.withoutDefaultNamespace ++ scope2
    }

    val qnames = List(QName("x"), QName("b", "y"), QName("c", "z"), QName("d", "z"))

    val expectedENames = List(EName("http://e", "x"), EName("http://b", "y"), EName("http://ccc", "z"), EName("http://d", "z"))

    expectResult(expectedENames) {
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error(s"QName $qname not resolved")) }
    }

    expectResult(expectedENames) {
      qnames map { qname =>
        (scope1.withoutDefaultNamespace ++ scope2).resolveQNameOption(qname).getOrElse(
          sys.error(s"QName $qname not resolved"))
      }
    }
  }

  @Test def testResolveQNameNotUndeclaringNamespaces(): Unit = {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d")
    val scope2 = Scope.from("b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    expectResult(Declarations.from("" -> "", "a" -> "", "c" -> "http://ccc")) {
      scope1.relativize(scope2)
    }

    expectResult(scope2 ++ Scope.from("a" -> "http://a") ++ scope1.retainingDefaultNamespace) {
      scope1 ++ scope2
    }

    expectResult(Some(EName("x"))) {
      scope2.resolveQNameOption(QName("x"))
    }

    expectResult(Some(EName("http://a", "x"))) {
      (scope1 ++ scope2).resolveQNameOption(QName("x"))
    }

    val qnames = List(QName("b", "y"), QName("c", "z"), QName("d", "z"))

    val expectedENames = List(EName("http://b", "y"), EName("http://ccc", "z"), EName("http://d", "z"))

    expectResult(expectedENames) {
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error(s"QName $qname not resolved")) }
    }

    expectResult(expectedENames) {
      qnames map { qname =>
        (scope1 ++ scope2).resolveQNameOption(qname).getOrElse(sys.error(s"QName $qname not resolved"))
      }
    }
  }

  @Test def testInverse(): Unit = {
    val scope =
      Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "dd" -> "http://d")

    val scopeInverse = scope.inverse

    val expectedScopeInverse = Map(
      "http://a" -> Set("", "a"),
      "http://b" -> Set("b"),
      "http://c" -> Set("c"),
      "http://d" -> Set("d", "dd"))

    expectResult(expectedScopeInverse) {
      scopeInverse
    }
  }

  @Test def testPrefixesForNamespace(): Unit = {
    val scope =
      Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "dd" -> "http://d")

    expectResult(Set("a", "")) {
      scope.prefixesForNamespace("http://a")
    }
    expectResult(Set()) {
      scope.prefixesForNamespace("http://abc")
    }
    expectResult(Set("c")) {
      scope.prefixesForNamespace("http://c")
    }
    expectResult(Set("d", "dd")) {
      scope.prefixesForNamespace("http://d")
    }
  }

  @Test def testPropertyAboutMultipleScopes(): Unit = {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "h" -> "http://h")
    val scope2 = Scope.from("b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d", "e" -> "http://e")

    expectResult(scope2) {
      scope1.resolve(scope1.relativize(scope2))
    }

    // Prefix has the same mappings in scope1 and scope2

    testPropertyAboutMultipleScopes(scope1, scope2, "b")
    testPropertyAboutMultipleScopes(scope1, scope2, "d")

    expectResult(Some("http://b")) {
      scope2.prefixNamespaceMap.get("b")
    }

    // Prefix has different mappings in scope1 and scope2

    testPropertyAboutMultipleScopes(scope1, scope2, "c")

    expectResult(Some("http://ccc")) {
      scope2.prefixNamespaceMap.get("c")
    }

    // Prefix only belongs to scope1

    testPropertyAboutMultipleScopes(scope1, scope2, "")
    testPropertyAboutMultipleScopes(scope1, scope2, "h")

    expectResult(None) {
      scope2.prefixNamespaceMap.get("h")
    }

    // Prefix only belongs to scope2

    testPropertyAboutMultipleScopes(scope1, scope2, "e")

    expectResult(Some("http://e")) {
      scope2.prefixNamespaceMap.get("e")
    }

    // Prefix belongs to neither scope

    testPropertyAboutMultipleScopes(scope1, scope2, "k")

    expectResult(None) {
      scope2.prefixNamespaceMap.get("k")
    }
  }

  @Test def testPropertyAboutScopeAndDeclaration(): Unit = {
    val scope = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d", "g" -> "http://g", "h" -> "http://h")
    val decls = Declarations.from("b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d", "g" -> "", "e" -> "http://e", "m" -> "")

    expectResult(scope.minimize(decls)) {
      scope.relativize(scope.resolve(decls))
    }

    expectResult(decls -- Set("b", "d", "m")) {
      scope.minimize(decls)
    }

    // Prefix has the same mappings in scope and declarations (so no undeclaration)

    testPropertyAboutScopeAndDeclaration(scope, decls, "b")
    testPropertyAboutScopeAndDeclaration(scope, decls, "d")

    expectResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("b")
    }
    expectResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("d")
    }

    // Prefix has different mappings in scope and declarations (but no undeclaration)

    testPropertyAboutScopeAndDeclaration(scope, decls, "c")

    expectResult(Some("http://ccc")) {
      scope.minimize(decls).prefixNamespaceMap.get("c")
    }

    // Prefix belongs to scope and is undeclared in declarations

    testPropertyAboutScopeAndDeclaration(scope, decls, "g")

    expectResult(Some("")) {
      scope.minimize(decls).prefixNamespaceMap.get("g")
    }

    // Prefix only belongs to scope, and does not occur in declarations

    testPropertyAboutScopeAndDeclaration(scope, decls, "")
    testPropertyAboutScopeAndDeclaration(scope, decls, "h")

    expectResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("")
    }
    expectResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("h")
    }

    // Prefix only occurs in declarations, without being undeclared, and does not occur in scope

    testPropertyAboutScopeAndDeclaration(scope, decls, "e")

    expectResult(Some("http://e")) {
      scope.minimize(decls).prefixNamespaceMap.get("e")
    }

    // Prefix only occurs in declarations, in an undeclaration, and does not occur in scope

    testPropertyAboutScopeAndDeclaration(scope, decls, "m")

    expectResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("m")
    }

    // Prefix neither occurs in scope nor in declarations

    testPropertyAboutScopeAndDeclaration(scope, decls, "x")

    expectResult(None) {
      scope.minimize(decls).prefixNamespaceMap.get("x")
    }
  }

  private def testPropertyAboutMultipleScopes(scope1: Scope, scope2: Scope, prefix: String): Unit = {
    expectResult(scope2.prefixNamespaceMap.get(prefix)) {
      scope1.resolve(scope1.relativize(scope2)).prefixNamespaceMap.get(prefix)
    }
  }

  private def testPropertyAboutScopeAndDeclaration(scope: Scope, declarations: Declarations, prefix: String): Unit = {
    expectResult(scope.minimize(declarations).prefixNamespaceMap.get(prefix)) {
      scope.relativize(scope.resolve(declarations)).prefixNamespaceMap.get(prefix)
    }
  }
}
