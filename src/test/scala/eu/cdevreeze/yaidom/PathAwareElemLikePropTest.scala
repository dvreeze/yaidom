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
import org.scalacheck.{ Prop, Gen, Arbitrary }
import org.scalatest.{ Suite, BeforeAndAfterAll }
import org.scalatest.prop.Checkers
import org.scalatest.junit.JUnitRunner

/**
 * PathAwareElemLike properties test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class PathAwareElemLikePropTest extends Suite with Checkers {

  import Prop._
  import Gen._
  import Arbitrary.arbitrary

  // Consistency of findAllChildElemsWithPathEntries

  @Test def testFindAllChildElemsWithPathEntriesProperty(): Unit = {
    check({ (elem: Elem) =>
      elem.findAllChildElemsWithPathEntries.map(_._1) == elem.findAllChildElems
    }, minSuccessful(100))
  }

  @Test def testFindAllChildElemsWithPathEntriesConsistencyProperty(): Unit = {
    check({ (elem: Elem) =>
      elem.findAllChildElemsWithPathEntries forall { case (che, pe) => elem.findChildElemByPathEntry(pe).get == che }
    }, minSuccessful(100))
  }

  // Simple "definitions"

  @Test def testFilterChildElemPathsDefinition(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterChildElemPaths(p) == {
        elem.findAllChildElemsWithPathEntries collect { case (che, pe) if p(che) => ElemPath(Vector(pe)) }
      }
    }, minSuccessful(100))
  }

  @Test def testFilterElemOrSelfPathsDefinition(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElemOrSelfPaths(p) == {
        (if (p(elem)) Vector(ElemPath.Root) else Vector()) ++ {
          elem.findAllChildElemsWithPathEntries flatMap {
            case (che, pe) =>
              che.filterElemOrSelfPaths(p).map(_.prepend(pe))
          }
        }
      }
    }, minSuccessful(100))
  }

  @Test def testFindTopmostElemOrSelfPathsDefinition(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.findTopmostElemOrSelfPaths(p) == {
        if (p(elem)) Vector(ElemPath.Root)
        else
          elem.findAllChildElemsWithPathEntries flatMap {
            case (che, pe) =>
              che.findTopmostElemOrSelfPaths(p).map(_.prepend(pe))
          }
      }
    }, minSuccessful(100))
  }

  @Test def testFilterElemPathsDefinition(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElemPaths(p) == {
        elem.findAllChildElemsWithPathEntries flatMap {
          case (che, pe) =>
            che.filterElemOrSelfPaths(p).map(_.prepend(pe))
        }
      }
    }, minSuccessful(100))
  }

  @Test def testFindTopmostElemPathsDefinition(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.findTopmostElemPaths(p) == {
        elem.findAllChildElemsWithPathEntries flatMap {
          case (che, pe) =>
            che.findTopmostElemOrSelfPaths(p).map(_.prepend(pe))
        }
      }
    }, minSuccessful(100))
  }

  @Test def testFindAllElemOrSelfPathsDefinition(): Unit = {
    check({ (elem: Elem) =>
      elem.findAllElemOrSelfPaths == elem.filterElemOrSelfPaths(_ => true)
    }, minSuccessful(100))
  }

  @Test def testFindAllElemPathsDefinition(): Unit = {
    check({ (elem: Elem) =>
      elem.findAllElemPaths == elem.filterElemPaths(_ => true)
    }, minSuccessful(100))
  }

  // Simple theorems

  @Test def testFilterElemOrSelfPathsProperty(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElemOrSelfPaths(p) == {
        elem.findAllElemOrSelfPaths.filter(path => p(elem.findElemOrSelfByPath(path).get))
      }
    }, minSuccessful(100))
  }

  @Test def testFilterElemPathsProperty(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElemPaths(p) == {
        elem.findAllElemPaths.filter(path => p(elem.findElemOrSelfByPath(path).get))
      }
    }, minSuccessful(100))
  }

  // TODO findTopmostElemOrSelfPaths etc.

  // Knowing that (elem.findAllChildElemsWithPathEntries map (_._1)) == elem.findAllChildElems, the following follows:

  @Test def testFilterChildElemPathsMapProperty(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      (elem.filterChildElemPaths(p) map (path => elem.findElemOrSelfByPath(path).get)) == elem.filterChildElems(p)
    }, minSuccessful(100))
  }

  @Test def testFilterElemOrSelfPathsMapProperty(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      (elem.filterElemOrSelfPaths(p) map (path => elem.findElemOrSelfByPath(path).get)) == elem.filterElemsOrSelf(p)
    }, minSuccessful(100))
  }

  @Test def testFilterElemPathsMapProperty(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      (elem.filterElemPaths(p) map (path => elem.findElemOrSelfByPath(path).get)) == elem.filterElems(p)
    }, minSuccessful(100))
  }

  @Test def testFindTopmostElemOrSelfPathsMapProperty(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      (elem.findTopmostElemOrSelfPaths(p) map (path => elem.findElemOrSelfByPath(path).get)) == elem.findTopmostElemsOrSelf(p)
    }, minSuccessful(100))
  }

  @Test def testFindTopmostElemPathsMapProperty(): Unit = {
    check({ (elem: Elem, p: Elem => Boolean) =>
      (elem.findTopmostElemPaths(p) map (path => elem.findElemOrSelfByPath(path).get)) == elem.findTopmostElems(p)
    }, minSuccessful(100))
  }

  @Test def testFindAllElemOrSelfPathsMapProperty(): Unit = {
    check({ (elem: Elem) =>
      (elem.findAllElemOrSelfPaths map (path => elem.findElemOrSelfByPath(path).get)) == elem.findAllElemsOrSelf
    }, minSuccessful(100))
  }

  @Test def testFindAllElemPathsMapProperty(): Unit = {
    check({ (elem: Elem) =>
      (elem.findAllElemPaths map (path => elem.findElemOrSelfByPath(path).get)) == elem.findAllElems
    }, minSuccessful(100))
  }

  // Generators of test data

  private val docParser = parse.DocumentParserUsingSax.newInstance

  private val docs: Vector[Document] = {
    val uris = Vector(
      "airportsBelgium.xml",
      "books.xml",
      "books-with-strange-namespaces.xml",
      "trivialXmlWithEuro.xml",
      "airportsGermany.xml",
      "trivialXmlWithPI.xml") map { s =>
        classOf[PathAwareElemLikePropTest].getResource("/eu/cdevreeze/yaidom/integrationtest/" + s).toURI
      }

    val docs = uris.map(uri => docParser.parse(uri))
    val bigDoc = Document(Node.elem(qname = QName("all"), scope = Scope.Empty, children = docs.map(_.documentElement)))
    val smallDoc = Document(Node.elem(qname = QName("a"), scope = Scope.Empty))
    docs :+ bigDoc :+ smallDoc
  }

  private val genElem: Gen[Elem] = {
    val allElems = docs.flatMap(_.documentElement.findAllElemsOrSelf)

    for {
      doc <- oneOf(docs)
      elems <- someOf(doc.documentElement.findAllElemsOrSelf)
      elem <- oneOf(elems)
    } yield elem
  }

  private val genElemPredicate: Gen[(Elem => Boolean)] = {
    oneOf(Seq(
      { elem: Elem => !elem.scope.filterKeys(Set("xs")).isEmpty },
      { elem: Elem => elem.qname.localPart.contains("e") },
      { elem: Elem => elem.findAllChildElems.size >= 10 },
      { elem: Elem => elem.textChildren.size >= 10 },
      { elem: Elem => !elem.attributes.isEmpty }))
  }

  private implicit val arbritraryElem: Arbitrary[Elem] = Arbitrary(genElem)

  private implicit val arbritraryElemPredicate: Arbitrary[(Elem => Boolean)] = Arbitrary(genElemPredicate)
}
