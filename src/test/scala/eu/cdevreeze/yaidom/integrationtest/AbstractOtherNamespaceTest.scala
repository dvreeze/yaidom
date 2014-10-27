/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.integrationtest

import java.{ io => jio }
import java.{ util => jutil }

import org.junit.Test
import org.scalatest.Suite

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.NodeBuilder
import eu.cdevreeze.yaidom.parse.DocumentParser
import eu.cdevreeze.yaidom.queryapi.HasENameApi.ToHasElemApi
import eu.cdevreeze.yaidom.resolved

/**
 * Test case testing the use of namespaces in immutable Documents.
 *
 * Acknowledgments: This test uses the examples in http://www.datypic.com/books/defxmlschema/chapter03.html, that are also used
 * in the excellent book Definitive XML Schema.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractOtherNamespaceTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  def documentParser: DocumentParser

  def documentParserForXml11: DocumentParser

  @Test def testNamespaceDeclaration(): Unit = {
    val xml =
      """|<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(List(QName("prod", "product"), QName("prod", "number"), QName("prod", "size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.qname }
    }

    val ns = "http://datypic.com/prod"

    assertResult(List(EName(ns, "product"), EName(ns, "number"), EName(ns, "size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("10") {
      val sizeElemOption = (doc.documentElement \\ EName(ns, "size")).headOption
      sizeElemOption.map(_.text).getOrElse("")
    }
  }

  @Test def testMultipleNamespaceDeclarations(): Unit = {
    val xml =
      """|<ord:order xmlns:ord="http://datypic.com/ord"
         |           xmlns:prod="http://datypic.com/prod">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("ord", "order"),
        QName("ord", "number"),
        QName("ord", "items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prodNumberElemOption = (doc.documentElement \\ EName(nsProd, "number")).headOption
      prodNumberElemOption.map(_.text).getOrElse("")
    }
  }

  @Test def testDefaultNamespaceDeclaration(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord"
         |       xmlns:prod="http://datypic.com/prod">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </items>
         |</order>
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prodNumberElemOption = (doc.documentElement \\ EName(nsProd, "number")).headOption
      prodNumberElemOption.map(_.text).getOrElse("")
    }

    // Show equivalence with the XML of the preceding test

    val equivalentXml =
      """|<ord:order xmlns:ord="http://datypic.com/ord"
         |           xmlns:prod="http://datypic.com/prod">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val equivalentDoc = documentParser.parse(new jio.ByteArrayInputStream(equivalentXml.getBytes("UTF-8")))

    val resolvedEquivalentElem = resolved.Elem(equivalentDoc.documentElement)

    val resolvedElem = resolved.Elem(doc.documentElement)

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        resolvedEquivalentElem.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(resolvedEquivalentElem.findAllElemsOrSelf map (_.resolvedName)) {
      resolvedElem.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(resolvedEquivalentElem) {
      resolvedElem
    }
  }

  @Test def testNamespaceDeclarationsInMultipleTags(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <prod:product xmlns:prod="http://datypic.com/prod">
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </items>
         |</order>
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prodNumberElemOption = (doc.documentElement \\ EName(nsProd, "number")).headOption
      prodNumberElemOption.map(_.text).getOrElse("")
    }

    // Show equivalence with the XML of the preceding test

    val equivalentXml =
      """|<ord:order xmlns:ord="http://datypic.com/ord"
         |           xmlns:prod="http://datypic.com/prod">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val equivalentDoc = documentParser.parse(new jio.ByteArrayInputStream(equivalentXml.getBytes("UTF-8")))

    val resolvedEquivalentElem = resolved.Elem(equivalentDoc.documentElement)

    val resolvedElem = resolved.Elem(doc.documentElement)

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        resolvedEquivalentElem.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(resolvedEquivalentElem.findAllElemsOrSelf map (_.resolvedName)) {
      resolvedElem.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(resolvedEquivalentElem) {
      resolvedElem
    }
  }

  @Test def testInvalidPrefixOutsideOfScope(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <prod:product xmlns:prod="http://datypic.com/prod">
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |    <prod:product>
         |      <prod:number>559</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </items>
         |</order>      
         |""".stripMargin.trim

    intercept[java.lang.Exception] {
      documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))
    }
  }

  @Test def testOverridingNamespaceDeclaration(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord"
         |       xmlns:prod="http://datypic.com/prod">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <prod:product>
         |      <prod:number xmlns:prod="http://datypic.com/prod2">
         |        557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </items>
         |</order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"
    val nsProd2 = "http://datypic.com/prod2"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd2, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prod2NumberElemOption = (doc.documentElement \\ EName(nsProd2, "number")).headOption
      prod2NumberElemOption.map(_.text).getOrElse("").trim
    }

    // Show equivalence with the XML of the preceding test, after updating the prod2:number to a prod:number

    val almostEquivalentXml =
      """|<ord:order xmlns:ord="http://datypic.com/ord"
         |           xmlns:prod="http://datypic.com/prod">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val almostEquivalentDoc = documentParser.parse(new jio.ByteArrayInputStream(almostEquivalentXml.getBytes("UTF-8")))

    val resolvedAlmostEquivalentElem = resolved.Elem(almostEquivalentDoc.documentElement)

    val f: resolved.Elem => resolved.Elem = {
      case e: resolved.Elem if e.resolvedName == EName(nsProd, "number") =>
        val scope = doc.documentElement.scope ++ Scope.from("prod" -> nsProd2)
        val v = (doc.documentElement \\ EName(nsProd2, "number")).map(_.text).mkString
        val result = NodeBuilder.textElem(QName("prod", "number"), v).build(scope)
        resolved.Elem(result)
      case e: resolved.Elem => e
    }
    val resolvedEquivalentElem = resolvedAlmostEquivalentElem.transformElemsOrSelf(f)

    val resolvedElem = resolved.Elem(doc.documentElement)

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd2, "number"),
        EName(nsProd, "size"))) {
        resolvedEquivalentElem.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(resolvedEquivalentElem.findAllElemsOrSelf map (_.resolvedName)) {
      resolvedElem.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(resolvedEquivalentElem) {
      resolvedElem
    }
  }

  @Test def testOverridingDefaultNamespaceDeclaration(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <product xmlns="http://datypic.com/prod">
         |      <number>557</number>
         |      <size system="US-DRESS">10</size>
         |    </product>
         |  </items>
         |</order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("product"),
        QName("number"),
        QName("size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prodNumberElemOption = (doc.documentElement \\ EName(nsProd, "number")).headOption
      prodNumberElemOption.map(_.text).getOrElse("")
    }

    // Show equivalence with the XML of the preceding test

    val equivalentXml =
      """|<ord:order xmlns:ord="http://datypic.com/ord"
         |           xmlns:prod="http://datypic.com/prod">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val equivalentDoc = documentParser.parse(new jio.ByteArrayInputStream(equivalentXml.getBytes("UTF-8")))

    val resolvedEquivalentElem = resolved.Elem(equivalentDoc.documentElement)

    val resolvedElem = resolved.Elem(doc.documentElement)

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        resolvedEquivalentElem.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(resolvedEquivalentElem.findAllElemsOrSelf map (_.resolvedName)) {
      resolvedElem.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(resolvedEquivalentElem) {
      resolvedElem
    }
  }

  @Test def testUndeclaringDefaultNamespace(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <product xmlns="">
         |      <number>557</number>
         |      <size system="US-DRESS">10</size>
         |    </product>
         |  </items>
         |</order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("product"),
        QName("number"),
        QName("size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName("product"),
        EName("number"),
        EName("size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prodNumberElemOption = (doc.documentElement \\ EName("number")).headOption
      prodNumberElemOption.map(_.text).getOrElse("")
    }
  }

  @Test def testUndeclaringPrefixedNamespace(): Unit = {
    val xml =
      """|<?xml version="1.1" encoding="utf-8"?>
         |<ord:order xmlns:ord="http://datypic.com/ord">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product xmlns:ord="" xmlns:prod="http://datypic.com/prod">
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val doc = documentParserForXml11.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("ord", "order"),
        QName("ord", "number"),
        QName("ord", "items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prodNumberElemOption = (doc.documentElement \\ EName(nsProd, "number")).headOption
      prodNumberElemOption.map(_.text).getOrElse("")
    }

    // Show equivalence with the XML of the preceding test

    val equivalentXml =
      """|<ord:order xmlns:ord="http://datypic.com/ord"
         |           xmlns:prod="http://datypic.com/prod">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val equivalentDoc = documentParser.parse(new jio.ByteArrayInputStream(equivalentXml.getBytes("UTF-8")))

    val resolvedEquivalentElem = resolved.Elem(equivalentDoc.documentElement)

    val resolvedElem = resolved.Elem(doc.documentElement)

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        resolvedEquivalentElem.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(resolvedEquivalentElem.findAllElemsOrSelf map (_.resolvedName)) {
      resolvedElem.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(resolvedEquivalentElem) {
      resolvedElem
    }
  }

  @Test def testTwoAttributesWithSameLocalName(): Unit = {
    val xml =
      """|<product xmlns="http://datypic.com/prod"
         |         xmlns:app="http://datypic.com/app">
         |  <number>557</number>
         |  <size app:system="R32" system="US-DRESS">10</size>
         |</product>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(List(QName("product"), QName("number"), QName("size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.qname }
    }

    val ns = "http://datypic.com/prod"
    val nsApp = "http://datypic.com/app"

    assertResult(List(EName(ns, "product"), EName(ns, "number"), EName(ns, "size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS", QName("app", "system") -> "R32")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS", EName(nsApp, "system") -> "R32")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("10") {
      val sizeElemOption = (doc.documentElement \\ EName(ns, "size")).headOption
      sizeElemOption.map(_.text).getOrElse("")
    }
  }

  @Test def testTwoMoreAttributesWithSameLocalName(): Unit = {
    val xml =
      """|<product xmlns="http://datypic.com/prod"
         |         xmlns:prod="http://datypic.com/prod">
         |  <number>557</number>
         |  <size system="US-DRESS" prod:system="R32">10</size>
         |</product>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(List(QName("product"), QName("number"), QName("size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.qname }
    }

    val ns = "http://datypic.com/prod"

    assertResult(List(EName(ns, "product"), EName(ns, "number"), EName(ns, "size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(QName("system") -> "US-DRESS", QName("prod", "system") -> "R32")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS", EName(ns, "system") -> "R32")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("10") {
      val sizeElemOption = (doc.documentElement \\ EName(ns, "size")).headOption
      sizeElemOption.map(_.text).getOrElse("")
    }
  }

  @Test def testInvalidDuplicateAttributes(): Unit = {
    val xml =
      """|<product xmlns:prod="http://datypic.com/prod"
         |         xmlns:prod2="http://datypic.com/prod">
         |  <number>557</number>
         |  <size prod:system="US-DRESS" prod2:system="R32">10</size>
         |</product>      
         |""".stripMargin.trim

    intercept[java.lang.Exception] {
      documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))
    }
  }

  @Test def testSummaryExample(): Unit = {
    val xml =
      """|<envelope>
         |  <order xmlns="http://datypic.com/ord"
         |         xmlns:prod="http://datypic.com/prod">
         |    <number>123ABBCC123</number>
         |    <items>
         |      <product xmlns="http://datypic.com/prod">
         |        <number prod:id="prod557">557</number>
         |        <name xmlns="">Short-Sleeved Linen Blouse</name>
         |        <prod:size system="US-DRESS">10</prod:size>
         |        <prod:color xmlns:prod="http://datypic.com/prod2"
         |                    prod:value="blue"/>
         |      </product>
         |    </items>
         |  </order>
         |</envelope>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("envelope"),
        QName("order"),
        QName("number"),
        QName("items"),
        QName("product"),
        QName("number"),
        QName("name"),
        QName("prod", "size"),
        QName("prod", "color"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"
    val nsProd2 = "http://datypic.com/prod2"

    assertResult(
      List(
        EName("envelope"),
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName("name"),
        EName(nsProd, "size"),
        EName(nsProd2, "color"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(Nil) {
      doc.documentElement.attributes
    }

    assertResult(Nil) {
      doc.documentElement.resolvedAttributes
    }

    assertResult(Map(EName(nsProd, "id") -> "prod557")) {
      val prodNumberElemOption = (doc.documentElement \\ EName(nsProd, "number")).headOption
      prodNumberElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult(Map(QName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.attributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName("system") -> "US-DRESS")) {
      val sizeElemOption = (doc.documentElement \\ (_.localName == "size")).headOption
      sizeElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult(Map()) {
      val nameElemOption = (doc.documentElement \\ (_.localName == "name")).headOption
      nameElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult(Map(EName(nsProd2, "value") -> "blue")) {
      val colorElemOption = (doc.documentElement \\ (_.localName == "color")).headOption
      colorElemOption.map(_.resolvedAttributes.toMap).getOrElse(Map())
    }

    assertResult("123ABBCC123") {
      val ordNumberElemOption = (doc.documentElement \\ EName(nsOrd, "number")).headOption
      ordNumberElemOption.map(_.text).getOrElse("")
    }

    assertResult("557") {
      val prodNumberElemOption = (doc.documentElement \\ EName(nsProd, "number")).headOption
      prodNumberElemOption.map(_.text).getOrElse("")
    }

    // Show equivalence with the XML of the preceding test

    val equivalentXml =
      """|<envelope>
         |  <ord:order xmlns:ord="http://datypic.com/ord"
         |             xmlns:prod="http://datypic.com/prod"
         |             xmlns:prod2="http://datypic.com/prod2">
         |    <ord:number>123ABBCC123</ord:number>
         |    <ord:items>
         |      <prod:product>
         |        <prod:number prod:id="prod557">557</prod:number>
         |        <name>Short-Sleeved Linen Blouse</name>
         |        <prod:size system="US-DRESS">10</prod:size>
         |        <prod2:color prod2:value="blue"/>
         |      </prod:product>
         |    </ord:items>
         |  </ord:order>
         |</envelope>      
         |""".stripMargin.trim

    val equivalentDoc = documentParser.parse(new jio.ByteArrayInputStream(equivalentXml.getBytes("UTF-8")))

    val resolvedEquivalentElem = resolved.Elem(equivalentDoc.documentElement)

    val resolvedElem = resolved.Elem(doc.documentElement)

    assertResult(
      List(
        EName("envelope"),
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName("name"),
        EName(nsProd, "size"),
        EName(nsProd2, "color"))) {
        resolvedEquivalentElem.findAllElemsOrSelf map { _.resolvedName }
      }

    assertResult(resolvedEquivalentElem.findAllElemsOrSelf map (_.resolvedName)) {
      resolvedElem.findAllElemsOrSelf map { _.resolvedName }
    }

    assertResult(resolvedEquivalentElem) {
      resolvedElem
    }
  }

  @Test def testFixNamespaceDeclaration(): Unit = {
    val xml =
      """|<prod:product xmlns:prod="http://datypic.com/prod">
         |  <prod:number>557</prod:number>
         |  <prod:size system="US-DRESS">10</prod:size>
         |</prod:product>
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(List(QName("prod", "product"), QName("prod", "number"), QName("prod", "size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.qname }
    }

    val ns = "http://datypic.com/prod"

    assertResult(List(EName(ns, "product"), EName(ns, "number"), EName(ns, "size"))) {
      doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
    }

    val fixedDoc = organizeNamespaces(doc)

    assertResult(true) {
      NodeBuilder.fromElem(fixedDoc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(fixedDoc.documentElement)
    }
  }

  @Test def testFixMultipleNamespaceDeclarations(): Unit = {
    val xml =
      """|<ord:order xmlns:ord="http://datypic.com/ord"
         |           xmlns:prod="http://datypic.com/prod">
         |  <ord:number>123ABBCC123</ord:number>
         |  <ord:items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </ord:items>
         |</ord:order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("ord", "order"),
        QName("ord", "number"),
        QName("ord", "items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    val fixedDoc = organizeNamespaces(doc)

    assertResult(true) {
      NodeBuilder.fromElem(fixedDoc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(fixedDoc.documentElement)
    }
  }

  @Test def testFixNamespacesHavingDefaultNamespaceDeclaration(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord"
         |       xmlns:prod="http://datypic.com/prod">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <prod:product>
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </items>
         |</order>
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    val fixedDoc = organizeNamespaces(doc)

    assertResult(true) {
      NodeBuilder.fromElem(fixedDoc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(fixedDoc.documentElement)
    }
  }

  @Test def testFixNamespaceDeclarationsInMultipleTags(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <prod:product xmlns:prod="http://datypic.com/prod">
         |      <prod:number>557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </items>
         |</order>
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    val fixedDoc = organizeNamespaces(doc)

    assertResult(false) {
      NodeBuilder.fromElem(doc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(true) {
      NodeBuilder.fromElem(fixedDoc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(fixedDoc.documentElement)
    }
  }

  @Test def testFixNamespacesHavingOverriddenNamespaceDeclaration(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord"
         |       xmlns:prod="http://datypic.com/prod">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <prod:product>
         |      <prod:number xmlns:prod="http://datypic.com/prod2">
         |        557</prod:number>
         |      <prod:size system="US-DRESS">10</prod:size>
         |    </prod:product>
         |  </items>
         |</order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("prod", "product"),
        QName("prod", "number"),
        QName("prod", "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"
    val nsProd2 = "http://datypic.com/prod2"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd2, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    val fixedDoc = organizeNamespaces(doc)

    assertResult(false) {
      NodeBuilder.fromElem(doc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(false) {
      NodeBuilder.fromElem(fixedDoc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(fixedDoc.documentElement)
    }
  }

  @Test def testFixNamespacesHavingOverriddenDefaultNamespaceDeclaration(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <product xmlns="http://datypic.com/prod">
         |      <number>557</number>
         |      <size system="US-DRESS">10</size>
         |    </product>
         |  </items>
         |</order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("product"),
        QName("number"),
        QName("size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"
    val nsProd = "http://datypic.com/prod"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName(nsProd, "product"),
        EName(nsProd, "number"),
        EName(nsProd, "size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    val fixedDoc = organizeNamespaces(doc)

    assertResult(false) {
      NodeBuilder.fromElem(doc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(false) {
      NodeBuilder.fromElem(fixedDoc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(fixedDoc.documentElement)
    }
  }

  @Test def testFixNamespacesHavingUndeclaredDefaultNamespace(): Unit = {
    val xml =
      """|<order xmlns="http://datypic.com/ord">
         |  <number>123ABBCC123</number>
         |  <items>
         |    <product xmlns="">
         |      <number>557</number>
         |      <size system="US-DRESS">10</size>
         |    </product>
         |  </items>
         |</order>      
         |""".stripMargin.trim

    val doc = documentParser.parse(new jio.ByteArrayInputStream(xml.getBytes("UTF-8")))

    assertResult(
      List(
        QName("order"),
        QName("number"),
        QName("items"),
        QName("product"),
        QName("number"),
        QName("size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.qname }
      }

    val nsOrd = "http://datypic.com/ord"

    assertResult(
      List(
        EName(nsOrd, "order"),
        EName(nsOrd, "number"),
        EName(nsOrd, "items"),
        EName("product"),
        EName("number"),
        EName("size"))) {
        doc.documentElement.findAllElemsOrSelf map { _.resolvedName }
      }

    val fixedDoc = organizeNamespaces(doc)

    assertResult(false) {
      NodeBuilder.fromElem(doc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(false) {
      NodeBuilder.fromElem(fixedDoc.documentElement)(Scope.Empty).allDeclarationsAreAtTopLevel
    }

    assertResult(resolved.Elem(doc.documentElement)) {
      resolved.Elem(fixedDoc.documentElement)
    }
  }

  private def organizeNamespaces(doc: Document): Document =
    doc.withDocumentElement(organizeNamespaces(doc.documentElement))

  private def organizeNamespaces(rootElem: Elem): Elem = {
    val maxScope = findMaxTopLevelScope(rootElem)

    def fillScope(e: Elem): Elem = {
      val newScope = maxScope ++ e.scope
      require(e.scope.withoutDefaultNamespace.subScopeOf(newScope.withoutDefaultNamespace))

      e.copy(scope = newScope)
    }

    rootElem.transformElemsOrSelf(fillScope _)
  }

  private def findMaxTopLevelScope(elem: Elem): Scope = {
    elem.findAllElemsOrSelf.foldLeft(elem.scope.withoutDefaultNamespace) { (acc, e) =>
      acc ++ (e.scope.withoutDefaultNamespace filterKeys (pref => !acc.keySet.contains(pref)))
    }
  }
}
