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

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import eu.cdevreeze.yaidom.core.EName
import eu.cdevreeze.yaidom.core.Path
import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.indexed
import eu.cdevreeze.yaidom.parse.DocumentParserUsingDom
import eu.cdevreeze.yaidom.queryapi.ClarkNodes
import eu.cdevreeze.yaidom.resolved
import eu.cdevreeze.yaidom.simple

/**
 * XML diff test case. Yaidom resolved elements are a good basis for a namespace-aware XML difference tool, provided
 * the documents to compare are first transformed. Indexed elements can help in locating differences between documents.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class XmlDiffTest extends FunSuite {

  test("testNamespaceAwareness") {
    val docParser = DocumentParserUsingDom.newInstance()

    val is1 = classOf[XmlDiffTest].getResourceAsStream("feed1.xml")
    val doc1: simple.Document = docParser.parse(is1)

    val is2 = classOf[XmlDiffTest].getResourceAsStream("feed2.xml")
    val doc2: simple.Document = docParser.parse(is2)

    val diffs = XmlDiffTest.findDiffs(doc1.documentElement, doc2.documentElement)

    assertResult(Set.empty) {
      diffs.allDiffs
    }
  }

  test("testNamespaceAwarenessAgain") {
    val docParser = DocumentParserUsingDom.newInstance()

    val is1 = classOf[XmlDiffTest].getResourceAsStream("feed1.xml")
    val doc1: simple.Document = docParser.parse(is1)

    // Now parsing feed3.xml
    val is2 = classOf[XmlDiffTest].getResourceAsStream("feed3.xml")
    val doc2: simple.Document = docParser.parse(is2)

    val diffs = XmlDiffTest.findDiffs(doc1.documentElement, doc2.documentElement)

    assertResult(Set.empty) {
      diffs.allDiffs
    }
  }

  test("testDiffInOneSharedElem") {
    val docParser = DocumentParserUsingDom.newInstance()

    val is1 = classOf[XmlDiffTest].getResourceAsStream("feed1.xml")
    val doc1: simple.Document = docParser.parse(is1)

    // Now parsing feed3.xml
    val is2 = classOf[XmlDiffTest].getResourceAsStream("feed3.xml")
    val doc2: simple.Document = docParser.parse(is2)

    val editedDoc2 = doc2 transformElemsOrSelf {
      case e if e.localName == "title" => e.copy(children = Vector(simple.Text("Sample Feed", false)))
      case e                           => e
    }

    val diffs = XmlDiffTest.findDiffs(doc1.documentElement, editedDoc2.documentElement)

    assertResult(Set(Path.Empty, Path.from(EName("{http://www.w3.org/2005/Atom}title") -> 0))) {
      diffs.allDiffs
    }
    assertResult(Set(Path.Empty, Path.from(EName("{http://www.w3.org/2005/Atom}title") -> 0))) {
      diffs.inBothElemsButDiffering
    }
  }

  test("testDiffInOneNonSharedElem") {
    val docParser = DocumentParserUsingDom.newInstance()

    val is1 = classOf[XmlDiffTest].getResourceAsStream("feed1.xml")
    val doc1: simple.Document = docParser.parse(is1)

    // Now parsing feed3.xml
    val is2 = classOf[XmlDiffTest].getResourceAsStream("feed3.xml")
    val doc2: simple.Document = docParser.parse(is2)

    val editedDoc2 = doc2 transformElemsOrSelf {
      case e if e.localName == "title" => e.copy(qname = QName(e.qname.prefixOption, "Title"))
      case e                           => e
    }

    val diffs = XmlDiffTest.findDiffs(doc1.documentElement, editedDoc2.documentElement)

    assertResult(Set(
      Path.Empty,
      Path.from(EName("{http://www.w3.org/2005/Atom}title") -> 0),
      Path.from(EName("{http://www.w3.org/2005/Atom}Title") -> 0))) {
      diffs.allDiffs
    }
    assertResult(Set(Path.Empty)) {
      diffs.inBothElemsButDiffering
    }
    assertResult(Set(Path.from(EName("{http://www.w3.org/2005/Atom}title") -> 0))) {
      diffs.onlyInFirstElem
    }
    assertResult(Set(Path.from(EName("{http://www.w3.org/2005/Atom}Title") -> 0))) {
      diffs.onlyInSecondElem
    }
  }
}

object XmlDiffTest {

  /**
   * Finds the locations of differences between the 2 elements. The method exploits IndexedClarkElems of resolved
   * elements in its implementation.
   *
   * This algorithm is naive in more than one way. First of all, it matches on Paths, for better or for worse.
   * Paths are not locations, as in child node indexes. Moreover, if a difference is found at a Path occurring
   * in both elements, then all paths in the ancestry of that path are returned as well.
   *
   * Both issues are resolvable. The matching function could be made pluggable. Using IndexedClarkElems is still
   * useful, for difference reporting purposes.
   */
  def findDiffs[E <: ClarkNodes.Elem.Aux[_, E]](elem1: E, elem2: E): Diffs = {
    val indexedResolvedElem1 =
      indexed.IndexedClarkElem(resolved.Elem.from(elem1).removeAllInterElementWhitespace.coalesceAndNormalizeAllText)
    val indexedResolvedElem2 =
      indexed.IndexedClarkElem(resolved.Elem.from(elem2).removeAllInterElementWhitespace.coalesceAndNormalizeAllText)

    val allElems1 = indexedResolvedElem1.findAllElemsOrSelf
    val allElems2 = indexedResolvedElem2.findAllElemsOrSelf

    val allElem1Paths = allElems1.map(_.path)
    val allElem2Paths = allElems2.map(_.path)

    val onlyInFirstElem = allElem1Paths.toSet.diff(allElem2Paths.toSet)
    val onlyInSecondElem = allElem2Paths.toSet.diff(allElem1Paths.toSet)

    val allElems1ByPath = allElems1.groupBy(_.path)
    val allElems2ByPath = allElems2.groupBy(_.path)

    assert(allElems1ByPath.forall(_._2.size == 1))
    assert(allElems2ByPath.forall(_._2.size == 1))

    val commonPaths = allElem1Paths.toSet.intersect(allElem2Paths.toSet)

    val inBothElemsButDiffering =
      commonPaths.filter(p => allElems1ByPath(p).head.underlyingElem != allElems2ByPath(p).head.underlyingElem)

    Diffs(onlyInFirstElem, onlyInSecondElem, inBothElemsButDiffering)
  }

  final case class Diffs(
    val onlyInFirstElem:         Set[Path],
    val onlyInSecondElem:        Set[Path],
    val inBothElemsButDiffering: Set[Path]) {

    def allDiffs = onlyInFirstElem.union(onlyInSecondElem).union(inBothElemsButDiffering)
  }
}
