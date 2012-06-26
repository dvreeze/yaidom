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
package xlink

import java.{ util => jutil, io => jio }
import scala.collection.immutable
import org.junit.{ Test, Before, Ignore }
import org.junit.runner.RunWith
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.junit.JUnitRunner

/**
 * XLink test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XLinkTest extends Suite {

  @Test def testRetrieval() {
    doTest(sampleXml.wrappedElem)
  }

  private def doTest(root: Elem) {
    expect(Set(EName("courseload"), EName("tooltip"), EName("person"), EName("course"), EName("gpa"), EName("go"))) {
      val enames = sampleXml.wrappedElem collectFromElemsOrSelf { case e => e.resolvedName }
      enames.toSet
    }

    expect(Some(sampleXml.wrappedElem)) {
      val result = sampleXml.wrappedElem collectFromElemsOrSelf { case e if XLink.mustBeExtendedLink(e) => e }
      result.headOption
    }

    expect(Set("students/patjones62.xml", "profs/jaysmith7.xml", "courses/cs101.xml")) {
      val result = sampleXml.wrappedElem collectFromElems { case e if XLink.mustBeLocator(e) => Locator(e).href.toString }
      result.toSet
    }

    expect(Set("students/patjones62.xml", "profs/jaysmith7.xml", "courses/cs101.xml")) {
      val result = sampleXml.wrappedElem filterElems { e => XLink.mustBeLocator(e) } map { e => Locator(e).href.toString }
      result.toSet
    }

    val fromToHrefPairs: immutable.IndexedSeq[(String, String)] =
      for {
        arc <- sampleXml.wrappedElem collectFromChildElems { case e if XLink.mustBeArc(e) && Arc(e).fromOption == Some("CS-101") && Arc(e).toOption == Some("student62") => Arc(e) }
        fromLoc <- sampleXml.wrappedElem collectFromChildElems { case e if XLink.mustBeLocator(e) && Locator(e).labelOption == arc.fromOption => Locator(e) }
        toLoc <- sampleXml.wrappedElem collectFromChildElems { case e if XLink.mustBeLocator(e) && Locator(e).labelOption == arc.toOption => Locator(e) }
      } yield (fromLoc.href.toString, toLoc.href.toString)

    expect(Some("courses/cs101.xml")) {
      fromToHrefPairs.headOption map { _._1 }
    }

    expect(Some("students/patjones62.xml")) {
      fromToHrefPairs.headOption map { _._2 }
    }
  }

  private val sampleXml: xlink.ExtendedLink = {
    import NodeBuilder._
    import Scope._

    // Example from http://www.w3.org/TR/xlink/ (adapted)
    // In the original example, some xlink attributes are set in the DTD but not in the XML document. That's not supported here.

    val rootBuilder: ElemBuilder =
      elem(
        qname = QName("courseload"),
        attributes = Map(QName.parse("xlink:type") -> "extended"),
        namespaces = Declarations.from("xlink" -> "http://www.w3.org/1999/xlink"),
        children = Vector(
          elem(
            qname = QName("tooltip"),
            attributes = Map(QName.parse("xlink:type") -> "title"),
            children = Vector(text("Course Load for Pat Jones"))),
          elem(
            qname = QName("person"),
            attributes = Map(
              QName.parse("xlink:type") -> "locator",
              QName.parse("xlink:href") -> "students/patjones62.xml",
              QName.parse("xlink:label") -> "student62",
              QName.parse("xlink:role") -> "http://www.example.com/linkprops/student",
              QName.parse("xlink:title") -> "Pat Jones")),
          elem(
            qname = QName("person"),
            attributes = Map(
              QName.parse("xlink:type") -> "locator",
              QName.parse("xlink:href") -> "profs/jaysmith7.xml",
              QName.parse("xlink:label") -> "prof7",
              QName.parse("xlink:role") -> "http://www.example.com/linkprops/professor",
              QName.parse("xlink:title") -> "Dr. Jay Smith")),
          comment(" more remote resources for professors, teaching assistants, etc. "),
          elem(
            qname = QName("course"),
            attributes = Map(
              QName.parse("xlink:type") -> "locator",
              QName.parse("xlink:href") -> "courses/cs101.xml",
              QName.parse("xlink:label") -> "CS-101",
              QName.parse("xlink:title") -> "Computer Science 101")),
          comment(" more remote resources for courses, seminars, etc. "),
          elem(
            qname = QName("gpa"),
            attributes = Map(
              QName.parse("xlink:type") -> "resource",
              QName.parse("xlink:label") -> "PatJonesGPA",
              QName.parse("xlink:role") -> "http://www.example.com/linkprops/gpa"),
            children = Vector(text("3.5"))),
          elem(
            qname = QName("go"),
            attributes = Map(
              QName.parse("xlink:type") -> "arc",
              QName.parse("xlink:from") -> "student62",
              QName.parse("xlink:arcrole") -> "", // Required??
              QName.parse("xlink:to") -> "PatJonesGPA",
              QName.parse("xlink:show") -> "new",
              QName.parse("xlink:actuate") -> "onRequest",
              QName.parse("xlink:title") -> "Pat Jones's GPA")),
          elem(
            qname = QName("go"),
            attributes = Map(
              QName.parse("xlink:type") -> "arc",
              QName.parse("xlink:from") -> "CS-101",
              QName.parse("xlink:arcrole") -> "http://www.example.com/linkprops/auditor",
              QName.parse("xlink:to") -> "student62",
              QName.parse("xlink:show") -> "replace",
              QName.parse("xlink:actuate") -> "onRequest",
              QName.parse("xlink:title") -> "Pat Jones, auditing the course")),
          elem(
            qname = QName("go"),
            attributes = Map(
              QName.parse("xlink:type") -> "arc",
              QName.parse("xlink:from") -> "student62",
              QName.parse("xlink:arcrole") -> "http://www.example.com/linkprops/advisor",
              QName.parse("xlink:to") -> "prof7",
              QName.parse("xlink:show") -> "replace",
              QName.parse("xlink:actuate") -> "onRequest",
              QName.parse("xlink:title") -> "Dr. Jay Smith, advisor"))))

    val root: Elem = rootBuilder.build()

    xlink.ExtendedLink(root)
  }
}
