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
 * QName test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class QNameTest extends Suite {

  @Test def testUnprefixedName(): Unit = {
    val qname = UnprefixedName("Bookstore")

    assertResult("Bookstore") {
      qname.localPart
    }
    assertResult(None) {
      qname.prefixOption
    }

    val qname2 = QName("Bookstore")

    assertResult("Bookstore") {
      qname2.localPart
    }
    assertResult(None) {
      qname2.prefixOption
    }
    assertResult(qname) {
      qname2
    }
    assertResult(qname.hashCode) {
      qname2.hashCode
    }

    val qname3 = QName(None, "Bookstore")

    assertResult("Bookstore") {
      qname3.localPart
    }
    assertResult(None) {
      qname3.prefixOption
    }
    assertResult(qname) {
      qname3
    }
    assertResult(qname.hashCode) {
      qname3.hashCode
    }

    val qname4 = QName("Bookstore")

    assertResult("Bookstore") {
      qname4.localPart
    }
    assertResult(None) {
      qname4.prefixOption
    }
    assertResult(qname) {
      qname4
    }
    assertResult(qname.hashCode) {
      qname4.hashCode
    }

    val qname5 = QName("Bookstore")

    assertResult("Bookstore") {
      qname5.localPart
    }
    assertResult(None) {
      qname5.prefixOption
    }
    assertResult(qname) {
      qname5
    }
    assertResult(qname.hashCode) {
      qname5.hashCode
    }

    intercept[Exception] {
      UnprefixedName(null)
    }
    intercept[Exception] {
      UnprefixedName("")
    }
    intercept[Exception] {
      UnprefixedName("a:b")
    }
    intercept[Exception] {
      QName.parse("")
    }
    intercept[Exception] {
      QName.parse(":")
    }
  }

  @Test def testPrefixedName(): Unit = {
    val qname = PrefixedName("books", "Bookstore")

    assertResult("Bookstore") {
      qname.localPart
    }
    assertResult(Some("books")) {
      qname.prefixOption
    }
    assertResult("books") {
      qname.prefix
    }

    val qname2: PrefixedName = PrefixedName("books", "Bookstore")

    assertResult("Bookstore") {
      qname2.localPart
    }
    assertResult(Some("books")) {
      qname2.prefixOption
    }
    assertResult("books") {
      qname2.prefix
    }
    assertResult(qname) {
      qname2
    }
    assertResult(qname.hashCode) {
      qname2.hashCode
    }

    val qname3: PrefixedName = QName(Some("books"), "Bookstore").asInstanceOf[PrefixedName]

    assertResult("Bookstore") {
      qname3.localPart
    }
    assertResult(Some("books")) {
      qname3.prefixOption
    }
    assertResult("books") {
      qname3.prefix
    }
    assertResult(qname) {
      qname3
    }
    assertResult(qname.hashCode) {
      qname3.hashCode
    }

    val qname4 = QName("books:Bookstore").asInstanceOf[PrefixedName]

    assertResult("Bookstore") {
      qname4.localPart
    }
    assertResult(Some("books")) {
      qname4.prefixOption
    }
    assertResult("books") {
      qname4.prefix
    }
    assertResult(qname) {
      qname4
    }
    assertResult(qname.hashCode) {
      qname4.hashCode
    }

    val qname5 = QName("books:Bookstore").asInstanceOf[PrefixedName]

    assertResult("Bookstore") {
      qname5.localPart
    }
    assertResult(Some("books")) {
      qname5.prefixOption
    }
    assertResult("books") {
      qname5.prefix
    }
    assertResult(qname) {
      qname5
    }
    assertResult(qname.hashCode) {
      qname5.hashCode
    }

    intercept[Exception] {
      PrefixedName(null, null)
    }
    intercept[Exception] {
      PrefixedName(null, "b")
    }
    intercept[Exception] {
      PrefixedName("a", null)
    }
    intercept[Exception] {
      PrefixedName("", "")
    }
    intercept[Exception] {
      PrefixedName("", "b")
    }
    intercept[Exception] {
      PrefixedName("a", "")
    }
    intercept[Exception] {
      PrefixedName("a:c", "b")
    }
    intercept[Exception] {
      PrefixedName("a", "b:c")
    }
    intercept[Exception] {
      QName.parse("a:")
    }
    intercept[Exception] {
      QName.parse(":b")
    }
  }
}
