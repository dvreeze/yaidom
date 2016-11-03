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

package eu.cdevreeze.yaidom.queryapitests

import scala.Vector

import org.junit.Test
import org.junit.runner.RunWith
import org.scalacheck.Arbitrary
import org.scalacheck.Gen
import org.scalacheck.Gen.oneOf
import org.scalacheck.Gen.someOf
import org.scalacheck.Prop.propBoolean
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner
import org.scalatest.prop.Checkers

import eu.cdevreeze.yaidom.core.QName
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.parse

/**
 * ElemLike properties test case.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ElemLikePropTest extends FunSuite with Checkers {

  import Arbitrary.arbitrary

  // Simple "definitions"

  test("testFilterChildElemsDefinition") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterChildElems(p) == elem.findAllChildElems.filter(p)
    }, minSuccessful(100))
  }

  test("testFilterElemsOrSelfDefinition") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElemsOrSelf(p) == {
        Vector(elem).filter(p) ++ (elem.findAllChildElems flatMap (_.filterElemsOrSelf(p)))
      }
    }, minSuccessful(100))
  }

  test("testFindTopmostElemsOrSelfDefinition") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.findTopmostElemsOrSelf(p) == {
        if (p(elem)) Vector(elem)
        else (elem.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p)))
      }
    }, minSuccessful(100))
  }

  test("testFilterElemsDefinition") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElems(p) == {
        (elem.findAllChildElems flatMap (_.filterElemsOrSelf(p)))
      }
    }, minSuccessful(100))
  }

  test("testFindTopmostElemsDefinition") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.findTopmostElems(p) == {
        (elem.findAllChildElems flatMap (_.findTopmostElemsOrSelf(p)))
      }
    }, minSuccessful(100))
  }

  test("testFindAllElemsOrSelfDefinition") {
    check({ (elem: Elem) =>
      elem.findAllElemsOrSelf == elem.filterElemsOrSelf(_ => true)
    }, minSuccessful(100))
  }

  test("testFindAllElemsDefinition") {
    check({ (elem: Elem) =>
      elem.findAllElems == elem.filterElems(_ => true)
    }, minSuccessful(100))
  }

  // Simple theorems

  test("testFilterElemsOrSelfProperty") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElemsOrSelf(p) == elem.findAllElemsOrSelf.filter(p)
    }, minSuccessful(100))
  }

  test("testFilterElemsProperty") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.filterElems(p) == elem.findAllElems.filter(p)
    }, minSuccessful(100))
  }

  test("testFindTopmostElemsOrSelfProperty") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      (elem.findTopmostElemsOrSelf(p) flatMap (_.filterElemsOrSelf(p))) == elem.filterElemsOrSelf(p)
    }, minSuccessful(100))
  }

  test("testFindTopmostElemsProperty") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      (elem.findTopmostElems(p) flatMap (_.filterElemsOrSelf(p))) == elem.filterElems(p)
    }, minSuccessful(100))
  }

  test("testFindTopmostElemsOrSelfAlternativeDefinition") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.findTopmostElemsOrSelf(p) == {
        elem.filterElemsOrSelf(p) filter { e =>
          val hasNoMatchingAncestor = elem.filterElemsOrSelf(p) forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }
    }, minSuccessful(100))
  }

  test("testFindTopmostElemsAlternativeDefinition") {
    check({ (elem: Elem, p: Elem => Boolean) =>
      elem.findTopmostElems(p) == {
        elem.filterElems(p) filter { e =>
          val hasNoMatchingAncestor = elem.filterElems(p) forall { _.findElem(_ == e).isEmpty }
          hasNoMatchingAncestor
        }
      }
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
        classOf[ElemLikePropTest].getResource("/eu/cdevreeze/yaidom/integrationtest/" + s).toURI
      }

    val docs = uris.map(uri => docParser.parse(uri))
    val bigDoc = Document(Node.elem(qname = QName("all"), scope = Scope.Empty, children = docs.map(_.documentElement)))
    val smallDoc = Document(Node.emptyElem(qname = QName("a"), scope = Scope.Empty))
    docs :+ bigDoc :+ smallDoc
  }

  private val genElem: Gen[Elem] = {
    val allElems = docs.flatMap(_.documentElement.findAllElemsOrSelf)

    for {
      doc <- oneOf(docs)
      elems <- someOf(doc.documentElement.findAllElemsOrSelf)
      if elems.nonEmpty
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
