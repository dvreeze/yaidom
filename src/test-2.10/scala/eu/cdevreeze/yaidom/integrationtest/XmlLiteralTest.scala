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
import literal.XmlLiterals._

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
    
    val bookTitles = (doc.documentElement \ "Book") flatMap { e => (e \ "Title") map (_.text) }
    val expectedBookTitles = List(
        "A First Course in Database Systems",
        "Database Systems: The Complete Book",
        "Hector and Jeff's Database Hints",
        "Jennifer's Economical Database Hints")
        
    expect(expectedBookTitles) {
      bookTitles
    }
    
    val firstBookElemOption = doc.documentElement findChildElem { e => e.localName == "Book" && (e \@ "ISBN") == Some("ISBN-0-13-713526-2") }
    
    expect(Some("85")) {
      firstBookElemOption flatMap { e => (e \@ "Price") }
    }
    
    val expectedScope = Scope.from("" -> "http://bookstore", "books" -> "http://bookstore")
    
    expect(expectedScope) {
      doc.documentElement.scope
    }
    expect(Set(expectedScope)) {
      val result = firstBookElemOption.get.findAllElemsOrSelf map { _.scope }
      result.toSet
    }
  }

  @Test def testXmlLiteral2() {
    val doc: Document = getDocument2
    
    val bookTitles = (doc.documentElement \ "Book") flatMap { e => (e \ "Title") map (_.text) }
    val expectedBookTitles = List(
        "A First Course in Database Systems",
        "Database Systems: The Complete Book",
        "Hector and Jeff's Database Hints",
        "Jennifer's Economical Database Hints")
        
    expect(expectedBookTitles) {
      bookTitles
    }
    
    val firstBookElemOption = doc.documentElement findChildElem { e => e.localName == "Book" && (e \@ "ISBN") == Some("ISBN-0-13-713526-2") }
    
    expect(Some("85")) {
      firstBookElemOption flatMap { e => (e \@ "Price") }
    }
    
    val expectedScope = Scope.from("" -> "http://bookstore", "books" -> "http://bookstore")
    
    expect(expectedScope) {
      doc.documentElement.scope
    }
    expect(Set(expectedScope)) {
      val result = firstBookElemOption.get.findAllElemsOrSelf map { _.scope }
      result.toSet
    }
  }

  @Test def testXmlLiteral3() {
    val doc: Document = getDocument3
    
    val bookTitles = (doc.documentElement \ "Book") flatMap { e => (e \ "Title") map (_.text) }
    val expectedBookTitles = List(
        "A First Course in Database Systems",
        "Database Systems: The Complete Book",
        "Hector and Jeff's Database Hints",
        "Jennifer's Economical Database Hints")
        
    expect(expectedBookTitles) {
      bookTitles
    }
    
    val firstBookElemOption = doc.documentElement findChildElem { e => e.localName == "Book" && (e \@ "ISBN") == Some("ISBN-0-13-713526-2") }
    
    expect(Some("85")) {
      firstBookElemOption flatMap { e => (e \@ "Price") }
    }
    
    val expectedScope = Scope.from("books" -> "http://bookstore")
    
    expect(expectedScope) {
      doc.documentElement.scope
    }
    expect(Set(expectedScope)) {
      val result = firstBookElemOption.get.findAllElemsOrSelf map { _.scope }
      result.toSet
    }
  }

  @Test def testXmlLiteral4() {
    val doc: Document = getDocument4
    
    expect(1) {
      doc.documentElement.findAllElemsOrSelf.size
    }
  }

  @Test def testWrongXmlLiterals() {
    val doc = xml"""<a>${ "b" }</a>"""

    intercept[java.lang.RuntimeException] {
      xml"""<a>=${ "b" }</a>"""
    }

    intercept[java.lang.RuntimeException] {
      xml"""<a>ab${ "b" }cd</a>"""
    }

    intercept[java.lang.RuntimeException] {
      xml"""<${ "a" }>wrong</${ "a" }>"""
    }

    intercept[SAXParseException] {
      xml"""<a><b></a></b>"""
    }

    intercept[SAXParseException] {
      xml"""<a><b>${ "x" }</a></b>"""
    }

    intercept[java.lang.RuntimeException] {
      xml"""<a x=">${ "wrong" }<">abc</a>"""
    }

    intercept[java.lang.RuntimeException] {
      xml"""<a x="ab${ "wrong" }cd">abc</a>"""
    }
  }

  private def getDocument1: Document = {
    val doc =
      xml"""<?xml version="1.0" encoding="UTF-8"?>
<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
	<Book ISBN="ISBN-0-13-713526-2" Price=${ 85.toString } Edition="3rd">
		<Title>${ "A First Course in Database Systems" }</Title>
		<Authors>${
			val elemBuilders =
			  List(("Jeffrey", "Ullman"), ("Jennifer", "Widom")) map { case (firstName, lastName) =>
			    elem(
			      qname = QName("Author"),
			      children = Vector(
			        textElem(QName("First_Name"), firstName),
			        textElem(QName("Last_Name"), lastName)
			      ))
			  }
			val scope = Scope.from("" -> "http://bookstore")
			elemBuilders map { elemBuilder => elemBuilder.build(scope) }
		}</Authors>
	</Book>
	<Book ISBN="ISBN-0-13-815504-6" Price="100">
		<Title>${ "Database Systems: The Complete Book" }</Title>
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

  private def getDocument2: Document = {
    val elems = {
      val elemBuilders =
        List(("Jeffrey", "Ullman"), ("Jennifer", "Widom")) map { case (firstName, lastName) =>
          elem(
            qname = QName("Author"),
            children = Vector(
              textElem(QName("First_Name"), firstName),
              textElem(QName("Last_Name"), lastName)
            ))
        }
      val scope = Scope.from("" -> "http://bookstore")
      elemBuilders map { elemBuilder => elemBuilder.build(scope) }
    }
    
    val doc =
      xml"""
<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore">
	<Book ISBN="ISBN-0-13-713526-2" Price=${ 85.toString } Edition="3rd">
		<Title>${ "A First Course in Database Systems" }</Title>
		<Authors>${
          elems
		}</Authors>
	</Book>
	<Book ISBN="ISBN-0-13-815504-6" Price="100">
		<Title>${ "Database Systems: The Complete Book" }</Title>
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
    val doc =
      xml"""
<books:Bookstore xmlns:books="http://bookstore">
	<books:Book ISBN="ISBN-0-13-713526-2" Price=${ 85.toString } Edition="3rd">
		<books:Title>${ "A First Course in Database Systems" }</books:Title>
		<books:Authors>${
			val elemBuilders =
			  List(("Jeffrey", "Ullman"), ("Jennifer", "Widom")) map { case (firstName, lastName) =>
			    elem(
			      qname = QName("books:Author"),
			      children = Vector(
			        textElem(QName("books:First_Name"), firstName),
			        textElem(QName("books:Last_Name"), lastName)
			      ))
			  }
			val scope = Scope.from("books" -> "http://bookstore")
			elemBuilders map { elemBuilder => elemBuilder.build(scope) }
		}</books:Authors>
	</books:Book>
	<books:Book ISBN="ISBN-0-13-815504-6" Price="100">
		<books:Title>${ "Database Systems: The Complete Book" }</books:Title>
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
      
    doc
  }

  private def getDocument4: Document = {
    val doc =
      xml"""<books:Bookstore xmlns="http://bookstore" xmlns:books="http://bookstore" />"""

    doc
  }
}