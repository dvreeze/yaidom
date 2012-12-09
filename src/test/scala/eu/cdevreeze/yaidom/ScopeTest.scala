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
    expect(Map("" -> "http://a")) {
      Scope.from("" -> "http://a").map
    }
    expect(Map("" -> "http://a", "b" -> "http://b")) {
      Scope.from("" -> "http://a", "b" -> "http://b").map
    }
    expect(Some("http://a")) {
      Scope.from("" -> "http://a", "b" -> "http://b").defaultNamespaceOption
    }
    expect(None) {
      Scope.from("b" -> "http://b").defaultNamespaceOption
    }
    expect(Map("b" -> "http://b")) {
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
    expect(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").map
    }
    expect(Map()) {
      Declarations.from("" -> "http://a").withoutDefaultNamespace.map
    }
    expect(Map("a" -> "")) {
      Declarations.from("a" -> "").map
    }
    expect(Map()) {
      Declarations.from("a" -> "").withoutUndeclarations.map
    }
    expect(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").map
    }
    expect(Map("" -> "http://a", "b" -> "http://b")) {
      Declarations.from("" -> "http://a", "b" -> "http://b").map
    }
    expect(Scope.from("" -> "http://a", "b" -> "http://b").map) {
      Declarations.from("" -> "http://a", "b" -> "http://b").withoutUndeclarations.map
    }
    expect(Map("b" -> "http://b")) {
      Declarations.from("b" -> "http://b").map
    }

    val decls1 = Declarations.from("" -> "http://a", "b" -> "http://b", "c" -> "")

    expect(Declarations.from("b" -> "http://b")) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
    expect(decls1.withoutDefaultNamespace.withoutUndeclarations) {
      decls1.withoutUndeclarations.withoutDefaultNamespace
    }
  }

  @Test def testResolveDeclarations() {
    val scope1 = Scope.Empty

    expect(0) {
      scope1.map.size
    }

    val declarations2 = Declarations.from("" -> "http://a", "a" -> "http://a", "x" -> "")
    val scope2 = scope1.resolve(declarations2)

    expect(Some("http://a")) {
      scope2.defaultNamespaceOption
    }
    expect(Map("a" -> "http://a")) {
      scope2.withoutDefaultNamespace.map
    }
    expect(Declarations.from("" -> "http://a", "a" -> "http://a")) {
      scope1.relativize(scope2)
    }
    expect(true) {
      scope1.subScopeOf(scope1)
    }
    expect(true) {
      scope1.subScopeOf(scope2)
    }
    expect(false) {
      scope2.subScopeOf(scope1)
    }
    expect(Scope.Empty) {
      scope2.resolve(Declarations.from("" -> "")).resolve(Declarations.from("a" -> ""))
    }
    expect(scope2) {
      Scope.from(declarations2.withoutUndeclarations.map)
    }
    expect(scope2) {
      scope1.resolve(scope1.relativize(scope2))
    }
    expect(declarations2 -- Set("x")) {
      scope1.minimized(declarations2)
    }
    expect(scope1.minimized(declarations2)) {
      scope1.relativize(scope1.resolve(declarations2))
    }

    val declarations3 = Declarations.from("a" -> "http://a", "b" -> "http://b", "c" -> "http://c")
    val scope3 = scope2.resolve(declarations3)

    expect(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c")) {
      scope3.map
    }
    expect(scope2.minimized(declarations3)) {
      scope2.relativize(scope3)
    }
    expect(true) {
      scope2.subScopeOf(scope3)
    }
    expect(false) {
      scope3.subScopeOf(scope2)
    }
    expect(scope2) {
      scope3.resolve(Declarations.from("b" -> "", "c" -> ""))
    }
    expect(scope3) {
      Scope.from((scope2.map ++ declarations3.withoutUndeclarations.map) -- declarations3.retainingUndeclarations.map.keySet)
    }
    expect(scope3) {
      scope2.resolve(scope2.relativize(scope3))
    }
    expect(declarations3 -- Set("a")) {
      scope2.minimized(declarations3)
    }
    expect(scope2.minimized(declarations3)) {
      scope2.relativize(scope2.resolve(declarations3))
    }

    val declarations4 = Declarations.from("c" -> "http://ccc", "d" -> "http://d")
    val scope4 = scope3.resolve(declarations4)

    expect(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.map
    }
    expect(Map("a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.withoutDefaultNamespace.map
    }
    expect(declarations4) {
      scope3.relativize(scope4)
    }
    expect(false) {
      scope3.subScopeOf(scope4)
    }
    expect(false) {
      scope4.subScopeOf(scope3)
    }
    expect(scope3) {
      scope4.resolve(Declarations.from("c" -> "http://c", "d" -> ""))
    }
    expect(scope2) {
      scope4.resolve(Declarations.from("b" -> "", "c" -> "", "d" -> ""))
    }
    expect(scope1) {
      scope4.resolve(Declarations.from("" -> "", "a" -> "", "b" -> "", "c" -> "", "d" -> ""))
    }
    expect(Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4
    }
    expect(scope4) {
      Scope.from((scope3.map ++ declarations4.withoutUndeclarations.map) -- declarations4.retainingUndeclarations.map.keySet)
    }
    expect(scope4) {
      scope3.resolve(scope3.relativize(scope4))
    }
    expect(declarations4) {
      scope3.minimized(declarations4)
    }
    expect(scope3.minimized(declarations4)) {
      scope3.relativize(scope3.resolve(declarations4))
    }
  }

  @Test def testResolveQName() {
    val scope1 = Scope.from()

    expect(Some(EName("book"))) {
      scope1.resolveQNameOption(QName("book"))
    }
    expect(None) {
      scope1.resolveQNameOption(QName("book:book"))
    }

    val scope2 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    expect(Some(EName("{http://a}book"))) {
      scope2.resolveQNameOption(QName("book"))
    }
    expect(None) {
      scope2.resolveQNameOption(QName("book:book"))
    }
    expect(Some(EName("{http://a}book"))) {
      scope2.resolveQNameOption(QName("a:book"))
    }
    expect(Some(EName("{http://ccc}bookstore"))) {
      scope2.resolveQNameOption(QName("c:bookstore"))
    }
    expect(Some(EName("{http://www.w3.org/XML/1998/namespace}lang"))) {
      scope2.resolveQNameOption(QName("xml:lang"))
    }
  }

  @Test def testResolveQNameNotUndeclaringPrefixes() {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d")
    val scope2 = Scope.from("" -> "http://e", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    expect(Declarations.from("" -> "http://e", "a" -> "", "c" -> "http://ccc")) {
      scope1.relativize(scope2)
    }

    expect(scope2 ++ Scope.from("a" -> "http://a")) {
      scope1.notUndeclaringPrefixes(scope2)
    }

    val qnames = List(QName("x"), QName("b", "y"), QName("c", "z"), QName("d", "z"))

    val expectedENames = List(EName("http://e", "x"), EName("http://b", "y"), EName("http://ccc", "z"), EName("http://d", "z"))

    expect(expectedENames) {
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error("QName %s not resolved".format(qname))) }
    }

    expect(expectedENames) {
      qnames map { qname => scope1.notUndeclaringPrefixes(scope2).resolveQNameOption(qname).getOrElse(sys.error("QName %s not resolved".format(qname))) }
    }
  }

  @Test def testResolveQNameNotUndeclaringNamespaces() {
    val scope1 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c", "d" -> "http://d")
    val scope2 = Scope.from("b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    expect(Declarations.from("" -> "", "a" -> "", "c" -> "http://ccc")) {
      scope1.relativize(scope2)
    }

    expect(scope2 ++ Scope.from("a" -> "http://a") ++ scope1.retainingDefaultNamespace) {
      scope1.notUndeclaring(scope2)
    }

    expect(Some(EName("x"))) {
      scope2.resolveQNameOption(QName("x"))
    }

    expect(Some(EName("http://a", "x"))) {
      scope1.notUndeclaring(scope2).resolveQNameOption(QName("x"))
    }

    val qnames = List(QName("b", "y"), QName("c", "z"), QName("d", "z"))

    val expectedENames = List(EName("http://b", "y"), EName("http://ccc", "z"), EName("http://d", "z"))

    expect(expectedENames) {
      qnames map { qname => scope2.resolveQNameOption(qname).getOrElse(sys.error("QName %s not resolved".format(qname))) }
    }

    expect(expectedENames) {
      qnames map { qname => scope1.notUndeclaring(scope2).resolveQNameOption(qname).getOrElse(sys.error("QName %s not resolved".format(qname))) }
    }
  }
}
