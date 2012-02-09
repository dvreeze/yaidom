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

  @Test def testUnprefixedName() {
    val qname = UnprefixedName("Bookstore")

    expect("Bookstore") {
      qname.localPart
    }
    expect(None) {
      qname.prefixOption
    }

    val qname2 = QName("Bookstore")

    expect("Bookstore") {
      qname2.localPart
    }
    expect(None) {
      qname2.prefixOption
    }
    expect(qname) {
      qname2
    }
    expect(qname.hashCode) {
      qname2.hashCode
    }

    val qname3 = QName(None, "Bookstore")

    expect("Bookstore") {
      qname3.localPart
    }
    expect(None) {
      qname3.prefixOption
    }
    expect(qname) {
      qname3
    }
    expect(qname.hashCode) {
      qname3.hashCode
    }

    val qname4 = QName.parse("Bookstore")

    expect("Bookstore") {
      qname4.localPart
    }
    expect(None) {
      qname4.prefixOption
    }
    expect(qname) {
      qname4
    }
    expect(qname.hashCode) {
      qname4.hashCode
    }

    val qname5 = "Bookstore".qname

    expect("Bookstore") {
      qname5.localPart
    }
    expect(None) {
      qname5.prefixOption
    }
    expect(qname) {
      qname5
    }
    expect(qname.hashCode) {
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
      "".qname
    }
    intercept[Exception] {
      ":".qname
    }
  }

  @Test def testPrefixedName() {
    val qname = PrefixedName("books", "Bookstore")

    expect("Bookstore") {
      qname.localPart
    }
    expect(Some("books")) {
      qname.prefixOption
    }
    expect("books") {
      qname.prefix
    }

    val qname2: PrefixedName = QName("books", "Bookstore").asInstanceOf[PrefixedName]

    expect("Bookstore") {
      qname2.localPart
    }
    expect(Some("books")) {
      qname2.prefixOption
    }
    expect("books") {
      qname2.prefix
    }
    expect(qname) {
      qname2
    }
    expect(qname.hashCode) {
      qname2.hashCode
    }

    val qname3: PrefixedName = QName(Some("books"), "Bookstore").asInstanceOf[PrefixedName]

    expect("Bookstore") {
      qname3.localPart
    }
    expect(Some("books")) {
      qname3.prefixOption
    }
    expect("books") {
      qname3.prefix
    }
    expect(qname) {
      qname3
    }
    expect(qname.hashCode) {
      qname3.hashCode
    }

    val qname4 = QName.parse("books:Bookstore").asInstanceOf[PrefixedName]

    expect("Bookstore") {
      qname4.localPart
    }
    expect(Some("books")) {
      qname4.prefixOption
    }
    expect("books") {
      qname4.prefix
    }
    expect(qname) {
      qname4
    }
    expect(qname.hashCode) {
      qname4.hashCode
    }

    val qname5 = "books:Bookstore".qname.asInstanceOf[PrefixedName]

    expect("Bookstore") {
      qname5.localPart
    }
    expect(Some("books")) {
      qname5.prefixOption
    }
    expect("books") {
      qname5.prefix
    }
    expect(qname) {
      qname5
    }
    expect(qname.hashCode) {
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
      "a:".qname
    }
    intercept[Exception] {
      ":b".qname
    }
  }
}
