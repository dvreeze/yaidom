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
      Scope.from("" -> "http://a").toMap
    }
    expect(Map("" -> "http://a", "b" -> "http://b")) {
      Scope.from("" -> "http://a", "b" -> "http://b").toMap
    }
    expect(Some("http://a")) {
      Scope.from("" -> "http://a", "b" -> "http://b").defaultNamespaceOption
    }
    expect(Map("b" -> "http://b")) {
      Scope.from("b" -> "http://b").prefixScope
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
    intercept[Exception] {
      Declarations(Scope.from("" -> "http://a"), Set(None))
    }
    intercept[Exception] {
      Declarations(Scope.from("a" -> "http://a"), Set(Some("a")))
    }
    expect(Map("a" -> "")) {
      Declarations.from("a" -> "").toMap
    }
    expect(Map("" -> "http://a")) {
      Declarations.from("" -> "http://a").toMap
    }
    expect(Map("" -> "http://a", "b" -> "http://b")) {
      Declarations.from("" -> "http://a", "b" -> "http://b").toMap
    }
    expect(Scope.from("" -> "http://a", "b" -> "http://b")) {
      Declarations.from("" -> "http://a", "b" -> "http://b").declared
    }
    expect(Map("b" -> "http://b")) {
      Declarations.from("b" -> "http://b").toMap
    }
  }

  @Test def testResolveDeclarations() {
    val scope1 = Scope.Empty

    expect(0) {
      scope1.toMap.size
    }

    val declarations2 = Declarations.from("" -> "http://a", "a" -> "http://a")
    val scope2 = scope1.resolve(declarations2)

    expect(Some("http://a")) {
      scope2.defaultNamespaceOption
    }
    expect(Map("a" -> "http://a")) {
      scope2.prefixScope
    }
    expect(declarations2) {
      scope1.relativize(scope2)
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

    val declarations3 = Declarations.from("b" -> "http://b", "c" -> "http://c")
    val scope3 = scope2.resolve(declarations3)

    expect(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://c")) {
      scope3.toMap
    }
    expect(declarations3) {
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

    val declarations4 = Declarations.from("c" -> "http://ccc", "d" -> "http://d")
    val scope4 = scope3.resolve(declarations4)

    expect(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.toMap
    }
    expect(Map("a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")) {
      scope4.prefixScope
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
  }

  @Test def testResolveQName() {
    val scope1 = Scope.from()

    expect(Some(EName("book"))) {
      scope1.resolveQName(QName("book"))
    }
    expect(None) {
      scope1.resolveQName(QName("book:book"))
    }

    val scope2 = Scope.from("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d")

    expect(Some(EName("{http://a}book"))) {
      scope2.resolveQName(QName("book"))
    }
    expect(None) {
      scope2.resolveQName(QName("book:book"))
    }
    expect(Some(EName("{http://a}book"))) {
      scope2.resolveQName(QName("a:book"))
    }
    expect(Some(EName("{http://ccc}bookstore"))) {
      scope2.resolveQName(QName("c:bookstore"))
    }
    expect(Some(EName("{http://www.w3.org/XML/1998/namespace}lang"))) {
      scope2.resolveQName(QName("xml:lang"))
    }
  }
}
