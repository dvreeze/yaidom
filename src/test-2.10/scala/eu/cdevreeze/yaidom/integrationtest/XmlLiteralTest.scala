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
package integrationtest

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner
import org.xml.sax.SAXParseException
import NodeBuilder._
import literal._

/**
 * XML literal test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlLiteralTest extends Suite {

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.integrationtest")

  @Test def testXmlLiteral1() {
    val doc: Document = getDocument1

    val bookTitles = (doc.documentElement \ (_.localName == "Book")) flatMap { e => (e \ (_.localName == "Title")) map (_.text) }
    val expectedBookTitles = List(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")

    expectResult(expectedBookTitles) {
      bookTitles
    }

    val firstBookElemOption = doc.documentElement findChildElem { e => e.localName == "Book" && (e \@ EName("ISBN")) == Some("ISBN-0-13-713526-2") }

    expectResult(Some("85")) {
      firstBookElemOption flatMap { e => (e \@ EName("Price")) }
    }
    expectResult(List(("Jeffrey", "Ullman"), ("Jennifer", "Widom"))) {
      (firstBookElemOption.get \\ (_.localName == "Author")) map { author =>
        val firstName = (author \ (_.localName == "First_Name")).map(_.text).mkString
        val lastName = (author \ (_.localName == "Last_Name")).map(_.text).mkString
        (firstName, lastName)
      }
    }

    val expectedScope = Scope.from("" -> "http://bookstore", "books" -> "http://bookstore")

    expectResult(expectedScope) {
      doc.documentElement.scope
    }
    expectResult(Set(expectedScope)) {
      val result = firstBookElemOption.get.findAllElemsOrSelf map { _.scope }
      result.toSet
    }

    expectResult(Set("http://bookstore")) {
      val result = doc.documentElement.findAllElemsOrSelf map { e => e.resolvedName.namespaceUriOption.getOrElse("http://bogusNamespace") }
      result.toSet
    }
  }

  @Test def testXmlLiteral2() {
    val doc: Document = getDocument2

    val bookTitles = (doc.documentElement \ (_.localName == "Book")) flatMap { e => (e \ (_.localName == "Title")) map (_.text) }
    val expectedBookTitles = List(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")

    expectResult(expectedBookTitles) {
      bookTitles
    }

    val firstBookElemOption = doc.documentElement findChildElem { e => e.localName == "Book" && (e \@ EName("ISBN")) == Some("ISBN-0-13-713526-2") }

    expectResult(Some("85")) {
      firstBookElemOption flatMap { e => (e \@ EName("Price")) }
    }
    expectResult(List(("Jeffrey", "Ullman"), ("Jennifer", "Widom"))) {
      (firstBookElemOption.get \\ (_.localName == "Author")) map { author =>
        val firstName = (author \ (_.localName == "First_Name")).map(_.text).mkString
        val lastName = (author \ (_.localName == "Last_Name")).map(_.text).mkString
        (firstName, lastName)
      }
    }

    val expectedScope = Scope.from("" -> "http://bookstore", "books" -> "http://bookstore")

    expectResult(expectedScope) {
      doc.documentElement.scope
    }
    expectResult(Set(expectedScope)) {
      val result = firstBookElemOption.get.findAllElemsOrSelf map { _.scope }
      result.toSet
    }

    expectResult(Set("http://bookstore")) {
      val result = doc.documentElement.findAllElemsOrSelf map { e => e.resolvedName.namespaceUriOption.getOrElse("http://bogusNamespace") }
      result.toSet
    }
  }

  @Test def testXmlLiteral3() {
    val doc: Document = getDocument3

    val bookTitles = (doc.documentElement \ (_.localName == "Book")) flatMap { e => (e \ (_.localName == "Title")) map (_.text) }
    val expectedBookTitles = List(
      "A First Course in Database Systems",
      "Database Systems: The Complete Book",
      "Hector and Jeff's Database Hints",
      "Jennifer's Economical Database Hints")

    expectResult(expectedBookTitles) {
      bookTitles
    }

    val firstBookElemOption = doc.documentElement findChildElem { e => e.localName == "Book" && (e \@ EName("ISBN")) == Some("ISBN-0-13-713526-2") }

    expectResult(Some("85")) {
      firstBookElemOption flatMap { e => (e \@ EName("Price")) }
    }
    expectResult(List(("Jeffrey", "Ullman"), ("Jennifer", "Widom"))) {
      (firstBookElemOption.get \\ (_.localName == "Author")) map { author =>
        val firstName = (author \ (_.localName == "First_Name")).map(_.text).mkString
        val lastName = (author \ (_.localName == "Last_Name")).map(_.text).mkString
        (firstName, lastName)
      }
    }

    val expectedScope = Scope.from("books" -> "http://bookstore")

    expectResult(expectedScope) {
      doc.documentElement.scope
    }
    expectResult(Set(expectedScope)) {
      val result = firstBookElemOption.get.findAllElemsOrSelf map { _.scope }
      result.toSet
    }

    expectResult(Set("http://bookstore")) {
      val result = doc.documentElement.findAllElemsOrSelf map { e => e.resolvedName.namespaceUriOption.getOrElse("http://bogusNamespace") }
      result.toSet
    }
  }

  @Test def testXmlLiteral4() {
    val doc: Document = getDocument4

    expectResult(1) {
      doc.documentElement.findAllElemsOrSelf.size
    }

    expectResult(Set("http://bookstore")) {
      val result = doc.documentElement.findAllElemsOrSelf map { e => e.resolvedName.namespaceUriOption.getOrElse("http://bogusNamespace") }
      result.toSet
    }
  }

  @Test def testWrongXmlLiterals() {
    xmlElem"""<a>${"b"}</a>"""

    intercept[java.lang.Exception] {
      xmlElem"""<a>=${"b"}</a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a>ab${"b"}cd</a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<${"a"}>wrong</${"a"}>"""
    }

    intercept[SAXParseException] {
      xmlElem"""<a><b></a></b>"""
    }

    intercept[SAXParseException] {
      xmlElem"""<a><b>${"x"}</a></b>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a x=">${"wrong"}<">abc</a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a x="ab${"wrong"}cd">abc</a>"""
    }

    xmlElem"""<a x=${1.toString}>abc</a>"""

    intercept[java.lang.Exception] {
      xmlElem"""<a x=${1}>abc</a>"""
    }

    xmlElem"""<a x=${"http://aRealAttribute"}>abc</a>"""

    intercept[java.lang.Exception] {
      xmlElem"""<a xmlns=${"http://notARealAttribute"}>abc</a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a xmlns:pref=${"http://notARealAttribute"}>abc</a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a><!-- ignored comment -->${"b"}</a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a><!-- ignored comment ${"b"}--></a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a><![CDATA[ ignored CDATA ]]>${"b"}</a>"""
    }

    intercept[java.lang.Exception] {
      xmlElem"""<a><![CDATA[ ignored CDATA ${"b"}]]></a>"""
    }
  }

  @Test def testUpdateEmployee() {
    val doc: Document = getEmployeeDocument

    expectResult(11) {
      doc.documentElement.findAllElemsOrSelf.size
    }
    expectResult(1) {
      doc.allComments.size
    }

    val ns = "http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser"

    def updateEmployee(empElem: Elem): Elem = {
      require(empElem.resolvedName == EName(ns, "Employee"))

      val gender = (empElem \ (_.localName == "gender")) map (_.text) mkString ""
      val genderPrefix = if (gender == "Male") "M" else "F"
      val newId = genderPrefix + (empElem \@ EName("id")).head

      val newName = (empElem \ (_.localName == "name")).map(_.text).mkString.toUpperCase

      xmlElem"""<Employee xmlns="http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser" id=${newId}>
		<name>${newName}</name>
		<age>${(empElem \ (_.localName == "age")).map(_.text).mkString}</age>
		<role>${(empElem \ (_.localName == "role")).map(_.text).mkString}</role>
		<salary>10000</salary>
	</Employee>"""
    }

    val f: PartialFunction[Elem, Elem] = {
      case empElem: Elem if empElem.localName == "Employee" => updateEmployee(empElem)
      case elem: Elem => elem
    }
    val newDoc = doc.replaceAllElemsOrSelf(f)

    val resolvedNewRoot = resolved.Elem(newDoc.documentElement).removeAllInterElementWhitespace

    expectResult(11) {
      resolvedNewRoot.findAllElemsOrSelf.size
    }
    expectResult(List(
      EName(ns, "Employees"),
      EName(ns, "Employee"),
      EName(ns, "name"),
      EName(ns, "age"),
      EName(ns, "role"),
      EName(ns, "salary"),
      EName(ns, "Employee"),
      EName(ns, "name"),
      EName(ns, "age"),
      EName(ns, "role"),
      EName(ns, "salary"))) {
      resolvedNewRoot.findAllElemsOrSelf map { _.resolvedName }
    }
    expectResult(List("M1", "F2")) {
      resolvedNewRoot.filterElems(EName(ns, "Employee")) map (_.attribute(EName("id")))
    }
    expectResult(List(
      EName(ns, "name"),
      EName(ns, "age"),
      EName(ns, "role"),
      EName(ns, "salary"),
      EName(ns, "name"),
      EName(ns, "age"),
      EName(ns, "role"),
      EName(ns, "salary"))) {

      for {
        empElem <- resolvedNewRoot.filterElems(EName(ns, "Employee"))
        empChildElem <- empElem.findAllChildElems
      } yield empChildElem.resolvedName
    }

    expectResult(List("PANKAJ", "LISA")) {
      for {
        empElem <- resolvedNewRoot.filterElems(EName(ns, "Employee"))
        nameElem <- empElem.filterChildElems(EName(ns, "name"))
      } yield nameElem.text
    }

    val expectedResolvedNewRoot = {
      import resolved._

      Elem(
        EName(ns, "Employees"),
        Map(),
        Vector(
          Elem(
            EName(ns, "Employee"),
            Map(EName("id") -> "M1"),
            Vector(
              Elem(
                EName(ns, "name"),
                Map(),
                Vector(Text("PANKAJ"))),
              Elem(
                EName(ns, "age"),
                Map(),
                Vector(Text("29"))),
              Elem(
                EName(ns, "role"),
                Map(),
                Vector(Text("Java Developer"))),
              Elem(
                EName(ns, "salary"),
                Map(),
                Vector(Text("10000"))))),
          Elem(
            EName(ns, "Employee"),
            Map(EName("id") -> "F2"),
            Vector(
              Elem(
                EName(ns, "name"),
                Map(),
                Vector(Text("LISA"))),
              Elem(
                EName(ns, "age"),
                Map(),
                Vector(Text("35"))),
              Elem(
                EName(ns, "role"),
                Map(),
                Vector(Text("CSS Developer"))),
              Elem(
                EName(ns, "salary"),
                Map(),
                Vector(Text("10000"))))))).removeAllInterElementWhitespace
    }

    expectResult(expectedResolvedNewRoot) {
      resolvedNewRoot
    }
  }

  private def getDocument1: Document = {
    val elm =
      xmlElem"""<?xml version="1.0" encoding="UTF-8"?>
<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
	<Book ISBN="ISBN-0-13-713526-2" Price=${85.toString} Edition=${"3rd"}>
		<Title>${"A First Course in Database Systems"}</Title>
		<Authors>${
        val elemBuilders =
          List(("Jeffrey", "Ullman"), ("Jennifer", "Widom")) map {
            case (firstName, lastName) =>
              elem(
                qname = QName("Author"),
                namespaces = Declarations.from("" -> "http://bookstore"),
                children = Vector(
                  textElem(QName("First_Name"), firstName),
                  textElem(QName("Last_Name"), lastName)))
          }

        require(elemBuilders forall (_.canBuild(Scope.Empty)))
        elemBuilders map { elemBuilder => elemBuilder.build() }
      }</Authors>
	</Book>
	<Book ISBN="ISBN-0-13-815504-6" Price="100">
		<Title>${"Database Systems: The Complete Book"}</Title>
		<Authors>
			<Author>
				<First_Name>Hector</First_Name>
				<Last_Name>Garcia-Molina</Last_Name>
			</Author>
			<Author>
				<First_Name>Jeffrey</First_Name>
				<Last_Name>Ullman</Last_Name>
			</Author>
			<Author>
				<First_Name>Jennifer</First_Name>
				<Last_Name>Widom</Last_Name>
			</Author>
		</Authors>
		<Remark>Buy this book bundled with "A First Course" - a great deal!
		</Remark>
	</Book>
	<Book ISBN="ISBN-0-11-222222-3" Price="50">
		<Title>Hector and Jeff's Database Hints</Title>
		<Authors>
			<Author>
				<First_Name>Jeffrey</First_Name>
				<Last_Name>Ullman</Last_Name>
			</Author>
			<Author>
				<First_Name>Hector</First_Name>
				<Last_Name>Garcia-Molina</Last_Name>
			</Author>
		</Authors>
		<Remark>An indispensable companion to your textbook</Remark>
	</Book>
	<Book ISBN="ISBN-9-88-777777-6" Price="25">
		<Title>Jennifer's Economical Database Hints</Title>
		<Authors>
			<Author>
				<First_Name>Jennifer</First_Name>
				<Last_Name>Widom</Last_Name>
			</Author>
		</Authors>
	</Book>
	<Magazine Month="January" Year="2009">
		<Title>National Geographic</Title>
	</Magazine>
	<Magazine Month="February" Year="2009">
		<Title>National Geographic</Title>
	</Magazine>
	<Magazine Month="February" Year="2009">
		<Title>Newsweek</Title>
	</Magazine>
	<Magazine Month="March" Year="2009">
		<Title>Hector and Jeff's Database Hints</Title>
	</Magazine>
</books:Bookstore>
"""

    Document(elm)
  }

  private def getDocument2: Document = {
    val elems = {
      val elemBuilders =
        List(("Jeffrey", "Ullman"), ("Jennifer", "Widom")) map {
          case (firstName, lastName) =>
            elem(
              qname = QName("Author"),
              namespaces = Declarations.from("" -> "http://bookstore"),
              children = Vector(
                textElem(QName("First_Name"), firstName),
                textElem(QName("Last_Name"), lastName)))
        }

      require(elemBuilders forall (_.canBuild(Scope.Empty)))
      elemBuilders map { elemBuilder => elemBuilder.build() }
    }

    val doc =
      xmlDoc"""
<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
	<Book ISBN="ISBN-0-13-713526-2" Price=${85.toString} Edition="3rd">
		<Title>${"A First Course in Database Systems"}</Title>
		<Authors>${
        elems
      }</Authors>
	</Book>
	<Book ISBN="ISBN-0-13-815504-6" Price="100">
		<Title>${"Database Systems: The Complete Book"}</Title>
		<Authors>
			<Author>
				<First_Name>Hector</First_Name>
				<Last_Name>Garcia-Molina</Last_Name>
			</Author>
			<Author>
				<First_Name>Jeffrey</First_Name>
				<Last_Name>Ullman</Last_Name>
			</Author>
			<Author>
				<First_Name>Jennifer</First_Name>
				<Last_Name>Widom</Last_Name>
			</Author>
		</Authors>
		<Remark>Buy this book bundled with "A First Course" - a great deal!
		</Remark>
	</Book>
	<Book ISBN="ISBN-0-11-222222-3" Price="50">
		<Title>Hector and Jeff's Database Hints</Title>
		<Authors>
			<Author>
				<First_Name>Jeffrey</First_Name>
				<Last_Name>Ullman</Last_Name>
			</Author>
			<Author>
				<First_Name>Hector</First_Name>
				<Last_Name>Garcia-Molina</Last_Name>
			</Author>
		</Authors>
		<Remark>An indispensable companion to your textbook</Remark>
	</Book>
	<Book ISBN="ISBN-9-88-777777-6" Price="25">
		<Title>Jennifer's Economical Database Hints</Title>
		<Authors>
			<Author>
				<First_Name>Jennifer</First_Name>
				<Last_Name>Widom</Last_Name>
			</Author>
		</Authors>
	</Book>
	<Magazine Month="January" Year="2009">
		<Title>National Geographic</Title>
	</Magazine>
	<Magazine Month="February" Year="2009">
		<Title>National Geographic</Title>
	</Magazine>
	<Magazine Month="February" Year="2009">
		<Title>Newsweek</Title>
	</Magazine>
	<Magazine Month="March" Year="2009">
		<Title>Hector and Jeff's Database Hints</Title>
	</Magazine>
</books:Bookstore>
"""

    doc
  }

  private def getDocument3: Document = {
    val elm =
      xmlElem"""
<books:Bookstore xmlns:books="http://bookstore">
	<books:Book ISBN="ISBN-0-13-713526-2" Price=${85.toString} Edition=${"3rd"}>
		<books:Title>${"A First Course in Database Systems"}</books:Title>
		<books:Authors>${
        val elemBuilders =
          List(("Jeffrey", "Ullman"), ("Jennifer", "Widom")) map {
            case (firstName, lastName) =>
              elem(
                qname = QName("books:Author"),
                namespaces = Declarations.from("books" -> "http://bookstore"),
                children = Vector(
                  textElem(QName("books:First_Name"), firstName),
                  textElem(QName("books:Last_Name"), lastName)))
          }

        require(elemBuilders forall (_.canBuild(Scope.Empty)))
        elemBuilders map { elemBuilder => elemBuilder.build() }
      }</books:Authors>
	</books:Book>
	<books:Book ISBN="ISBN-0-13-815504-6" Price="100">
		<books:Title>${"Database Systems: The Complete Book"}</books:Title>
		<books:Authors>
			<books:Author>
				<books:First_Name>Hector</books:First_Name>
				<books:Last_Name>Garcia-Molina</books:Last_Name>
			</books:Author>
			<books:Author>
				<books:First_Name>Jeffrey</books:First_Name>
				<books:Last_Name>Ullman</books:Last_Name>
			</books:Author>
			<books:Author>
				<books:First_Name>Jennifer</books:First_Name>
				<books:Last_Name>Widom</books:Last_Name>
			</books:Author>
		</books:Authors>
		<books:Remark>Buy this book bundled with "A First Course" - a great deal!
		</books:Remark>
	</books:Book>
	<books:Book ISBN="ISBN-0-11-222222-3" Price="50">
		<books:Title>Hector and Jeff's Database Hints</books:Title>
		<books:Authors>
			<books:Author>
				<books:First_Name>Jeffrey</books:First_Name>
				<books:Last_Name>Ullman</books:Last_Name>
			</books:Author>
			<books:Author>
				<books:First_Name>Hector</books:First_Name>
				<books:Last_Name>Garcia-Molina</books:Last_Name>
			</books:Author>
		</books:Authors>
		<books:Remark>An indispensable companion to your textbook</books:Remark>
	</books:Book>
	<books:Book ISBN="ISBN-9-88-777777-6" Price="25">
		<books:Title>Jennifer's Economical Database Hints</books:Title>
		<books:Authors>
			<books:Author>
				<books:First_Name>Jennifer</books:First_Name>
				<books:Last_Name>Widom</books:Last_Name>
			</books:Author>
		</books:Authors>
	</books:Book>
	<books:Magazine Month="January" Year="2009">
		<books:Title>National Geographic</books:Title>
	</books:Magazine>
	<books:Magazine Month="February" Year="2009">
		<books:Title>National Geographic</books:Title>
	</books:Magazine>
	<books:Magazine Month="February" Year="2009">
		<books:Title>Newsweek</books:Title>
	</books:Magazine>
	<books:Magazine Month="March" Year="2009">
		<books:Title>Hector and Jeff's Database Hints</books:Title>
	</books:Magazine>
</books:Bookstore>
"""

    Document(elm)
  }

  private def getDocument4: Document = {
    val elm =
      xmlElem"""<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore" />"""

    Document(elm)
  }

  private def getEmployeeDocument: Document = {
    val doc =
      xmlDoc"""
    <!-- See http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser 
	(but updated with namespace) -->
<Employees
	xmlns="http://www.journaldev.com/901/how-to-edit-xml-file-in-java-dom-parser">
	<Employee id="1">
		<name>Pankaj</name>
		<age>29</age>
		<role>Java Developer</role>
		<gender>Male</gender>
	</Employee>
	<Employee id="2">
		<name>Lisa</name>
		<age>35</age>
		<role>CSS Developer</role>
		<gender>Female</gender>
	</Employee>
</Employees>"""

    doc
  }
}
