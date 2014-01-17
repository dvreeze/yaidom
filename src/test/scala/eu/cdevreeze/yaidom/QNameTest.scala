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

    expectResult("Bookstore") {
      qname.localPart
    }
    expectResult(None) {
      qname.prefixOption
    }

    val qname2 = QName("Bookstore")

    expectResult("Bookstore") {
      qname2.localPart
    }
    expectResult(None) {
      qname2.prefixOption
    }
    expectResult(qname) {
      qname2
    }
    expectResult(qname.hashCode) {
      qname2.hashCode
    }

    val qname3 = QName(None, "Bookstore")

    expectResult("Bookstore") {
      qname3.localPart
    }
    expectResult(None) {
      qname3.prefixOption
    }
    expectResult(qname) {
      qname3
    }
    expectResult(qname.hashCode) {
      qname3.hashCode
    }

    val qname4 = QName("Bookstore")

    expectResult("Bookstore") {
      qname4.localPart
    }
    expectResult(None) {
      qname4.prefixOption
    }
    expectResult(qname) {
      qname4
    }
    expectResult(qname.hashCode) {
      qname4.hashCode
    }

    val qname5 = QName("Bookstore")

    expectResult("Bookstore") {
      qname5.localPart
    }
    expectResult(None) {
      qname5.prefixOption
    }
    expectResult(qname) {
      qname5
    }
    expectResult(qname.hashCode) {
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

    expectResult("Bookstore") {
      qname.localPart
    }
    expectResult(Some("books")) {
      qname.prefixOption
    }
    expectResult("books") {
      qname.prefix
    }

    val qname2: PrefixedName = PrefixedName("books", "Bookstore")

    expectResult("Bookstore") {
      qname2.localPart
    }
    expectResult(Some("books")) {
      qname2.prefixOption
    }
    expectResult("books") {
      qname2.prefix
    }
    expectResult(qname) {
      qname2
    }
    expectResult(qname.hashCode) {
      qname2.hashCode
    }

    val qname3: PrefixedName = QName(Some("books"), "Bookstore").asInstanceOf[PrefixedName]

    expectResult("Bookstore") {
      qname3.localPart
    }
    expectResult(Some("books")) {
      qname3.prefixOption
    }
    expectResult("books") {
      qname3.prefix
    }
    expectResult(qname) {
      qname3
    }
    expectResult(qname.hashCode) {
      qname3.hashCode
    }

    val qname4 = QName("books:Bookstore").asInstanceOf[PrefixedName]

    expectResult("Bookstore") {
      qname4.localPart
    }
    expectResult(Some("books")) {
      qname4.prefixOption
    }
    expectResult("books") {
      qname4.prefix
    }
    expectResult(qname) {
      qname4
    }
    expectResult(qname.hashCode) {
      qname4.hashCode
    }

    val qname5 = QName("books:Bookstore").asInstanceOf[PrefixedName]

    expectResult("Bookstore") {
      qname5.localPart
    }
    expectResult(Some("books")) {
      qname5.prefixOption
    }
    expectResult("books") {
      qname5.prefix
    }
    expectResult(qname) {
      qname5
    }
    expectResult(qname.hashCode) {
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
