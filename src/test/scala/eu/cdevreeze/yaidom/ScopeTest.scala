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
import eu.cdevreeze.yaidom.Predef._

/**
 * Scope test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScopeTest extends Suite {

  @Test def testCreateScope() {
    intercept[Exception] {
      Map("" -> null).scope
    }
    intercept[Exception] {
      val prefix: String = null
      Map(prefix -> "a").scope
    }
    intercept[Exception] {
      Map("a" -> "").scope
    }
    expect(Map("" -> "http://a")) {
      Map("" -> "http://a").scope.toMap
    }
    expect(Map("" -> "http://a", "b" -> "http://b")) {
      Map("" -> "http://a", "b" -> "http://b").scope.toMap
    }
    expect(Some("http://a")) {
      Map("" -> "http://a", "b" -> "http://b").scope.defaultNamespaceOption
    }
    expect(Map("b" -> "http://b")) {
      Map("b" -> "http://b").scope.prefixScope
    }
  }

  @Test def testCreateDeclarations() {
    import Scope._

    intercept[Exception] {
      Map("" -> null).namespaces
    }
    intercept[Exception] {
      val prefix: String = null
      Map(prefix -> "a").namespaces
    }
    intercept[Exception] {
      Declarations(Map("" -> "http://a").scope, Set(None))
    }
    intercept[Exception] {
      Declarations(Map("a" -> "http://a").scope, Set(Some("a")))
    }
    expect(Map("a" -> "")) {
      Map("a" -> "").namespaces.toMap
    }
    expect(Map("" -> "http://a")) {
      Map("" -> "http://a").namespaces.toMap
    }
    expect(Map("" -> "http://a", "b" -> "http://b")) {
      Map("" -> "http://a", "b" -> "http://b").namespaces.toMap
    }
    expect(Map("" -> "http://a", "b" -> "http://b").scope) {
      Map("" -> "http://a", "b" -> "http://b").namespaces.declared
    }
    expect(Map("b" -> "http://b")) {
      Map("b" -> "http://b").namespaces.toMap
    }
  }

  @Test def testResolveDeclarations() {
    import Scope._

    val scope1 = Scope.Empty

    expect(0) {
      scope1.toMap.size
    }

    val declarations2 = Map("" -> "http://a", "a" -> "http://a").namespaces
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
      scope2.resolve(Map("" -> "").namespaces).resolve(Map("a" -> "").namespaces)
    }

    val declarations3 = Map("b" -> "http://b", "c" -> "http://c").namespaces
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
      scope3.resolve(Map("b" -> "", "c" -> "").namespaces)
    }

    val declarations4 = Map("c" -> "http://ccc", "d" -> "http://d").namespaces
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
      scope4.resolve(Map("c" -> "http://c", "d" -> "").namespaces)
    }
    expect(scope2) {
      scope4.resolve(Map("b" -> "", "c" -> "", "d" -> "").namespaces)
    }
    expect(scope1) {
      scope4.resolve(Map("" -> "", "a" -> "", "b" -> "", "c" -> "", "d" -> "").namespaces)
    }
    expect(Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d").scope) {
      scope4
    }
  }

  @Test def testResolveQName() {
    val scope1 = Map[String, String]().scope

    expect(Some("book".ename)) {
      scope1.resolveQName("book".qname)
    }
    expect(None) {
      scope1.resolveQName("book:book".qname)
    }

    val scope2 = Map("" -> "http://a", "a" -> "http://a", "b" -> "http://b", "c" -> "http://ccc", "d" -> "http://d").scope

    expect(Some("{http://a}book".ename)) {
      scope2.resolveQName("book".qname)
    }
    expect(None) {
      scope2.resolveQName("book:book".qname)
    }
    expect(Some("{http://a}book".ename)) {
      scope2.resolveQName("a:book".qname)
    }
    expect(Some("{http://ccc}bookstore".ename)) {
      scope2.resolveQName("c:bookstore".qname)
    }
    expect(Some("{http://www.w3.org/XML/1998/namespace}lang".ename)) {
      scope2.resolveQName("xml:lang".qname)
    }
  }
}
