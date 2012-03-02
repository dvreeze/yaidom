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
    doTest(sampleXml)
  }

  @Test def testConversions() {
    val normalRoot: eu.cdevreeze.yaidom.Elem = sampleXml.toNormalNode
    val root = xlink.Elem(normalRoot)

    doTest(root)
  }

  private def doTest(root: xlink.Elem) {
    expect(Set("courseload".ename, "tooltip".ename, "person".ename, "course".ename, "gpa".ename, "go".ename)) {
      val enames = sampleXml collectFromElemsOrSelf { case e => e.resolvedName }
      enames.toSet
    }

    expect(Some(sampleXml)) {
      sampleXml collectFromElemsOrSelf { case e: ExtendedLink => e } headOption
    }

    expect(Set("students/patjones62.xml", "profs/jaysmith7.xml", "courses/cs101.xml")) {
      val result = sampleXml collectFromElems { case e: Locator => e.href.toString }
      result.toSet
    }

    expect(Set("students/patjones62.xml", "profs/jaysmith7.xml", "courses/cs101.xml")) {
      val result = sampleXml elemsWhere { e => e.isInstanceOf[Locator] } map { e => e.asInstanceOf[Locator].href.toString }
      result.toSet
    }

    val fromToHrefPairs: immutable.IndexedSeq[(String, String)] =
      for {
        arc <- sampleXml collectFromChildElems { case arc: Arc if arc.from == "CS-101" && arc.to == "student62" => arc }
        fromLoc <- sampleXml collectFromChildElems { case loc: Locator if loc.label == arc.from => loc }
        toLoc <- sampleXml collectFromChildElems { case loc: Locator if loc.label == arc.to => loc }
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

    val rootBuilder: eu.cdevreeze.yaidom.ElemBuilder =
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

    val root: eu.cdevreeze.yaidom.Elem = rootBuilder.build()

    xlink.ExtendedLink(root)
  }
}
