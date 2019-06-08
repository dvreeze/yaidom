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

import org.scalatest.funsuite.AnyFunSuite

/**
 * EName test case.
 *
 * @author Chris de Vreeze
 */
class ENameTest extends AnyFunSuite {

  private val bookstoreNs = "http://bookstore"

  test("testNoNamespaceName") {
    val ename = EName(None, "Bookstore")

    assertResult("Bookstore") {
      ename.localPart
    }
    assertResult(None) {
      ename.namespaceUriOption
    }

    val ename2 = EName("Bookstore")

    assertResult("Bookstore") {
      ename2.localPart
    }
    assertResult(None) {
      ename2.namespaceUriOption
    }
    assertResult(ename) {
      ename2
    }
    assertResult(ename.hashCode) {
      ename2.hashCode
    }

    val ename3 = EName.fromUriQualifiedNameString("Q{}Bookstore")

    assertResult("Bookstore") {
      ename3.localPart
    }
    assertResult(None) {
      ename3.namespaceUriOption
    }
    assertResult(ename) {
      ename3
    }
    assertResult(ename.hashCode) {
      ename3.hashCode
    }
    assertResult("Q{}Bookstore") {
      ename3.toUriQualifiedNameString
    }

    val ename4 = EName("Bookstore")

    assertResult("Bookstore") {
      ename4.localPart
    }
    assertResult(None) {
      ename4.namespaceUriOption
    }
    assertResult(ename) {
      ename4
    }
    assertResult(ename.hashCode) {
      ename4.hashCode
    }

    val ename5 = EName("Bookstore")

    assertResult("Bookstore") {
      ename5.localPart
    }
    assertResult(None) {
      ename5.namespaceUriOption
    }
    assertResult(ename) {
      ename5
    }
    assertResult(ename.hashCode) {
      ename5.hashCode
    }

    val ename6 = EName(" Bookstore  ")

    assertResult(ename) {
      ename6
    }

    intercept[Exception] {
      EName(null)
    }
    intercept[Exception] {
      EName("").validated
    }
    intercept[Exception] {
      EName.parse("").validated
    }

    val enOption = ename match {
      case en@EName(None, localPart) => Some(en)
      case _ => None
    }

    assertResult(Some(ename)) {
      enOption
    }
  }

  test("testNameInNamespace") {
    val ename = EName(bookstoreNs, "Bookstore")

    assertResult("Bookstore") {
      ename.localPart
    }
    assertResult(Some(bookstoreNs)) {
      ename.namespaceUriOption
    }

    val ename2: EName = EName(bookstoreNs, "Bookstore")

    assertResult("Bookstore") {
      ename2.localPart
    }
    assertResult(Some(bookstoreNs)) {
      ename2.namespaceUriOption
    }
    assertResult(ename) {
      ename2
    }
    assertResult(ename.hashCode) {
      ename2.hashCode
    }

    val ename3: EName = EName(Some(bookstoreNs), "Bookstore")

    assertResult("Bookstore") {
      ename3.localPart
    }
    assertResult(Some(bookstoreNs)) {
      ename3.namespaceUriOption
    }
    assertResult(ename) {
      ename3
    }
    assertResult(ename.hashCode) {
      ename3.hashCode
    }

    val ename4 = EName(s"{$bookstoreNs}Bookstore")

    assertResult("Bookstore") {
      ename4.localPart
    }
    assertResult(Some(bookstoreNs)) {
      ename4.namespaceUriOption
    }
    assertResult(ename) {
      ename4
    }
    assertResult(ename.hashCode) {
      ename4.hashCode
    }

    val ename5 = EName.fromUriQualifiedNameString(s"Q{$bookstoreNs}Bookstore")

    assertResult("Bookstore") {
      ename5.localPart
    }
    assertResult(Some(bookstoreNs)) {
      ename5.namespaceUriOption
    }
    assertResult(ename) {
      ename5
    }
    assertResult(ename.hashCode) {
      ename5.hashCode
    }
    assertResult(s"Q{$bookstoreNs}Bookstore") {
      ename5.toUriQualifiedNameString
    }

    val ename6 = EName(s"  {$bookstoreNs}Bookstore   ")

    assertResult(ename) {
      ename6
    }

    intercept[Exception] {
      EName(null.asInstanceOf[Option[String]], null)
    }
    intercept[Exception] {
      EName(null.asInstanceOf[Option[String]], "b")
    }
    intercept[Exception] {
      EName("a", null)
    }
    intercept[Exception] {
      EName("", "").validated
    }
    intercept[Exception] {
      EName("", "b").validated
    }
    intercept[Exception] {
      EName("a", "").validated
    }
    intercept[Exception] {
      EName.parse("{}").validated
    }
    intercept[Exception] {
      EName.parse("{}x").validated
    }
    intercept[Exception] {
      EName("a", "b:c").validated
    }
    intercept[Exception] {
      EName.parse("a{").validated
    }
    intercept[Exception] {
      EName.parse("}b").validated
    }
    intercept[Exception] {
      EName.parse(s"{$bookstoreNs} Bookstore").validated
    }

    val enOption = ename match {
      case en@EName(Some(ns), localPart) => Some(en)
      case _ => None
    }

    assertResult(Some(ename)) {
      enOption
    }
  }
}
