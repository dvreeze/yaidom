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

package eu.cdevreeze.yaidom.integrationtest

import scala.Vector

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.queryapi.ElemWithPath
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node

/**
 * Test case simulating XSLT. Thanks to Daniel K. Schneider for the examples.
 * See http://edutechwiki.unige.ch/en/XSLT_Tutorial_-_Basics.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class SimulateXsltTest extends FunSuite {

  import Node._

  test("testTransformHelloElem") {
    // Here we use updateTopmostXXX twice, once for the child elements and once for the root element
    // We can recognize the "pattern matches" (although order of updating topmost elements matters)

    val title = helloElem.findElem(_.resolvedName == EName("title")).map(_.text).getOrElse("")

    val htmlElem =
      helloElem updateTopmostElemsWithNodeSeq { (elm, path) =>
        elm.resolvedName match {
          case EName(None, "title") =>
            Some(Vector(textElem(QName("h1"), Vector(QName("align") -> "center"), scope, elm.text)))
          case EName(None, "content") =>
            Some(Vector(textElem(QName("p"), Vector(QName("align") -> "center"), scope, elm.text)))
          case EName(None, "comment") =>
            Some(Vector(
              emptyElem(QName("hr"), scope),
              textElem(QName("i"), scope, elm.text)))
          case _ =>
            None
        }
      } updateTopmostElemsOrSelf { (elm, path) =>
        elm.resolvedName match {
          case EName(None, "page") =>
            val html =
              emptyElem(QName("html"), scope).
                plusChild(
                  elem(
                    QName("head"),
                    scope,
                    Vector(
                      textElem(QName("title"), scope, title)))).
                  plusChild(
                    elem(
                      QName("body"),
                      Vector(QName("bgcolor") -> "#ffffff"),
                      scope,
                      elm.findAllChildElems))
            Some(html)
          case _ =>
            None
        }
      }

    assertResult(resolved.Elem.from(expectedHtmlElem).removeAllInterElementWhitespace) {
      resolved.Elem.from(htmlElem).removeAllInterElementWhitespace
    }
  }

  test("testTransformHelloElemUsingPaths") {
    // Here we use updateElemsOrSelfWithNodeSeq once, after collecting the paths of the elements to update
    // (of the child elements and the root element)
    // Again, we can recognize the "pattern matches" (and order of updating is handled by the update method)

    val title = helloElem.findElem(_.resolvedName == EName("title")).map(_.text).getOrElse("")

    val paths = ElemWithPath(helloElem).findAllElemsOrSelf.map(_.path).toSet

    val htmlElems =
      helloElem.updateElemsOrSelfWithNodeSeq(paths) { (elm, path) =>
        elm.resolvedName match {
          case EName(None, "title") =>
            Vector(textElem(QName("h1"), Vector(QName("align") -> "center"), scope, elm.text))
          case EName(None, "content") =>
            Vector(textElem(QName("p"), Vector(QName("align") -> "center"), scope, elm.text))
          case EName(None, "comment") =>
            Vector(
              emptyElem(QName("hr"), scope),
              textElem(QName("i"), scope, elm.text))
          case EName(None, "page") =>
            val html =
              emptyElem(QName("html"), scope).
                plusChild(
                  elem(
                    QName("head"),
                    scope,
                    Vector(
                      textElem(QName("title"), scope, title)))).
                  plusChild(
                    elem(
                      QName("body"),
                      Vector(QName("bgcolor") -> "#ffffff"),
                      scope,
                      elm.findAllChildElems))
            Vector(html)
          case _ =>
            Vector(elm)
        }
      }
    val htmlElem = htmlElems.head.asInstanceOf[Elem]

    assertResult(resolved.Elem.from(expectedHtmlElem).removeAllInterElementWhitespace) {
      resolved.Elem.from(htmlElem).removeAllInterElementWhitespace
    }
  }

  test("testTransformHelloElemUsingQuery") {
    // Here we use a "pull"/query approach rather than a "push"/update approach
    // We can recognize the "HTML template"
    // This may be the easiest and best approach (cf. XQuery versus XSLT)

    val title = helloElem.findElem(_.resolvedName == EName("title")).map(_.text).getOrElse("")
    val content = helloElem.findElem(_.resolvedName == EName("content")).map(_.text).getOrElse("")
    val comment = helloElem.findElem(_.resolvedName == EName("comment")).map(_.text).getOrElse("")

    val htmlElem =
      emptyElem(QName("html"), scope).
        plusChild(elem(QName("head"), scope, Vector(textElem(QName("title"), scope, title)))).
        plusChild(
          elem(
            QName("body"),
            Vector(QName("bgcolor") -> "#ffffff"),
            scope,
            Vector(
              textElem(QName("h1"), Vector(QName("align") -> "center"), scope, title),
              textElem(QName("p"), Vector(QName("align") -> "center"), scope, content),
              emptyElem(QName("hr"), scope),
              textElem(QName("i"), scope, comment))))

    assertResult(resolved.Elem.from(expectedHtmlElem).removeAllInterElementWhitespace) {
      resolved.Elem.from(htmlElem).removeAllInterElementWhitespace
    }
  }

  private val scope = Scope.Empty

  private val helloElem =
    emptyElem(QName("page"), scope).
      plusChild(textElem(QName("title"), scope, "Hello")).
      plusChild(textElem(QName("content"), scope, "Here is some content")).
      plusChild(textElem(QName("comment"), scope, "Written by DKS"))

  private val expectedHtmlElem =
    emptyElem(QName("html"), scope).
      plusChild(elem(QName("head"), scope, Vector(textElem(QName("title"), scope, "Hello")))).
      plusChild(
        elem(
          QName("body"),
          Vector(QName("bgcolor") -> "#ffffff"),
          scope,
          Vector(
            textElem(QName("h1"), Vector(QName("align") -> "center"), scope, "Hello"),
            textElem(QName("p"), Vector(QName("align") -> "center"), scope, "Here is some content"),
            emptyElem(QName("hr"), scope),
            textElem(QName("i"), scope, "Written by DKS"))))
}
