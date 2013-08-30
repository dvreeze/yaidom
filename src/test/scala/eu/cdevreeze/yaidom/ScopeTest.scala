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

  @Test def testCreateScope() {
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
      Scope.from("" -> "http://a").map
    }
    expectResult(Map("" -> "http://a", "b" -> "http://b")) {
      Scope.from("" -> "http://a", "b" -> "http://b").map
    }
    expectResult(Some("http://a")) {
      Scope.from("" -> "http://a", "b" -> "http://b").defaultNamespaceOption
    }
    expectResult(None) {
      Scope.from("b" -> "http://b").defaultNamespaceOption
    }
    expectResult(Map("b" -> "http://b")) {
      Scope.from("b" -> "http://b").withoutDefaultNamespace.map
    }
  }

  @Test def testCreateDeclarations() {
    intercept[Exception] {
      Declarations.from("" -> null)
    }
    intercept[Exception] {
      val prefix: String = null
      Declarations.from(prefix -> "a")
    }
    expectResult(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").map
    }
    expectResult(Map()) {
      Declarations.from("" -> "http://a").withoutDefaultNamespace.map
    }
    expectResult(Map("a" -> "")) {
      Declarations.from("a" -> "").map
    }
    expectResult(Map()) {
      Declarations.from("a" -> "").withoutUndeclarations.map
    }
    expectResult(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").map
    }
    expectResult(Map("" -> "http://a", "b" -> "http://b")) {
      Declarations.from("" -> "http://a", "b" -> "http://b").map
    }
    expectResult(Scope.from("" -> "http://a", "b" -> "http://b").map) {
      Declarations.from("" -> "http://a", "b" -> "http://b").withoutUndeclarations.map
    }
    expectResult(Map("b" -> "http://b")) {
      Declarations.from("b" -> "http://b").map
    }

    val decls1 = Declarations.from("" -> "http://a", "b" -> "http://b", "c" -> "")

    expectResult(Declarations.from("b" -> "http://b")) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
    expectResult(decls1.withoutDefaultNamespace.withoutUndeclarations) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
  }

  @Test def testResolveDeclarations() {
    val scope1 = Scope.Empty

    expectResult(0) {
      scope1.map.size
    }

    val declarations2 = Declarations.from("" -> "http://a", "a" -> "http://a", "x" -> "")
    val scope2 = scope1.resolve(declarations2)

    expectResult(Some("http://a")) {
      scope2.defaultNamespaceOption
    }
    expectResult(Map("a" -> "http://a")) {
      scope2.withoutDefaultNamespace.map
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
      Scope.from(declarations2.withoutUndeclarations.map)
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
      scope3.map
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
      Scope.from((scope2.map ++ declarations3.withoutUndeclarations.map) -- declarations3.retainingUndeclarations.map.keySet)
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
      scope4.map
    }
    expectResult(Map("a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.withoutDefaultNamespace.map
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
      Scope.from((scope3.map ++ declarations4.withoutUndeclarations.map) -- declarations4.retainingUndeclarations.map.keySet)
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

  @Test def testResolveQName() {
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

  @Test def testResolveQNameNotUndeclaringPrefixes() {
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
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error("QName %s not resolved".format(qname))) }
    }

    expectResult(expectedENames) {
      qnames map { qname =>
        (scope1.withoutDefaultNamespace ++ scope2).resolveQNameOption(qname).getOrElse(
          sys.error("QName %s not resolved".format(qname)))
      }
    }
  }

  @Test def testResolveQNameNotUndeclaringNamespaces() {
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
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error("QName %s not resolved".format(qname))) }
    }

    expectResult(expectedENames) {
      qnames map { qname =>
        (scope1 ++ scope2).resolveQNameOption(qname).getOrElse(sys.error("QName %s not resolved".format(qname)))
      }
    }
  }

  @Test def testInverse() {
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

  @Test def testPrefixesForNamespace() {
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
}
