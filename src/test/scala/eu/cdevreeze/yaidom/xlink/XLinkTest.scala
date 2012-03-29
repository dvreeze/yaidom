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
    expect(Set("courseload".ename, "tooltip".ename, "person".ename, "course".ename, "gpa".ename, "go".ename)) {
      val enames = sampleXml.wrappedElem collectFromElemsOrSelf { case e => e.resolvedName }
      enames.toSet
    }

    expect(Some(sampleXml.wrappedElem)) {
      sampleXml.wrappedElem collectFromElemsOrSelf { case e if XLink.mustBeExtendedLink(e) => e } headOption
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

    // Example from http://www.w3.org/TR/xlink/ (adapted)
    // In the original example, some xlink attributes are set in the DTD but not in the XML document. That's not supported here.

    val rootBuilder: ElemBuilder =
      elem(
        qname = "courseload".qname,
        attributes = Map("xlink:type".qname -> "extended"),
        namespaces = Map("xlink" -> "http://www.w3.org/1999/xlink").namespaces,
        children = immutable.IndexedSeq(
          elem(
            qname = "tooltip".qname,
            attributes = Map("xlink:type".qname -> "title"),
            children = immutable.IndexedSeq(text("Course Load for Pat Jones"))),
          elem(
            qname = "person".qname,
            attributes = Map(
              "xlink:type".qname -> "locator",
              "xlink:href".qname -> "students/patjones62.xml",
              "xlink:label".qname -> "student62",
              "xlink:role".qname -> "http://www.example.com/linkprops/student",
              "xlink:title".qname -> "Pat Jones")),
          elem(
            qname = "person".qname,
            attributes = Map(
              "xlink:type".qname -> "locator",
              "xlink:href".qname -> "profs/jaysmith7.xml",
              "xlink:label".qname -> "prof7",
              "xlink:role".qname -> "http://www.example.com/linkprops/professor",
              "xlink:title".qname -> "Dr. Jay Smith")),
          comment(" more remote resources for professors, teaching assistants, etc. "),
          elem(
            qname = "course".qname,
            attributes = Map(
              "xlink:type".qname -> "locator",
              "xlink:href".qname -> "courses/cs101.xml",
              "xlink:label".qname -> "CS-101",
              "xlink:title".qname -> "Computer Science 101")),
          comment(" more remote resources for courses, seminars, etc. "),
          elem(
            qname = "gpa".qname,
            attributes = Map(
              "xlink:type".qname -> "resource",
              "xlink:label".qname -> "PatJonesGPA",
              "xlink:role".qname -> "http://www.example.com/linkprops/gpa"),
            children = immutable.IndexedSeq(text("3.5"))),
          elem(
            qname = "go".qname,
            attributes = Map(
              "xlink:type".qname -> "arc",
              "xlink:from".qname -> "student62",
              "xlink:arcrole".qname -> "", // Required??
              "xlink:to".qname -> "PatJonesGPA",
              "xlink:show".qname -> "new",
              "xlink:actuate".qname -> "onRequest",
              "xlink:title".qname -> "Pat Jones's GPA")),
          elem(
            qname = "go".qname,
            attributes = Map(
              "xlink:type".qname -> "arc",
              "xlink:from".qname -> "CS-101",
              "xlink:arcrole".qname -> "http://www.example.com/linkprops/auditor",
              "xlink:to".qname -> "student62",
              "xlink:show".qname -> "replace",
              "xlink:actuate".qname -> "onRequest",
              "xlink:title".qname -> "Pat Jones, auditing the course")),
          elem(
            qname = "go".qname,
            attributes = Map(
              "xlink:type".qname -> "arc",
              "xlink:from".qname -> "student62",
              "xlink:arcrole".qname -> "http://www.example.com/linkprops/advisor",
              "xlink:to".qname -> "prof7",
              "xlink:show".qname -> "replace",
              "xlink:actuate".qname -> "onRequest",
              "xlink:title".qname -> "Dr. Jay Smith, advisor"))))

    val root: Elem = rootBuilder.build()

    xlink.ExtendedLink(root)
  }
}
