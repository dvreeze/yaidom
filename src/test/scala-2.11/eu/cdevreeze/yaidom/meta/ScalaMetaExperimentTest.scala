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

package eu.cdevreeze.yaidom.meta

import org.junit.Test
import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.meta._

/**
 * Test case experimenting with Scala.meta. This experiment helps in writing a test case that checks for unidirectional
 * package dependencies in yaidom, by inspecting import statements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class ScalaMetaExperimentTest extends FunSuite {

  test("testSimplePackageImport") {
    // Building a Tree using quasi-quotes, and manually, and comparing the two

    val import1 = q"import eu.cdevreeze.yaidom.core"

    val expImport =
      Import(
        List(
          Importer(
            Term.Select(
              Term.Select(
                Term.Name("eu"),
                Term.Name("cdevreeze")),
              Term.Name("yaidom")),
            List(Importee.Name(Name.Indeterminate("core"))))))

    assertResult(expImport.structure) {
      import1.structure
    }

    // Parsing the import statement into the "same" tree

    assertResult(import1.structure) {
      "import eu.cdevreeze.yaidom.core".parse[Stat].get.structure
    }

    // Collecting name sub-trees, and comparing structure and syntax with expectation

    assertResult(List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom")).map(_.structure)) {
      import1 collect { case tn: Term.Name => tn.structure }
    }
    assertResult(List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom")).map(_.syntax)) {
      import1 collect { case tn: Term.Name => tn.syntax }
    }
    assertResult("eu.cdevreeze.yaidom") {
      import1 collect { case tn: Term.Name => tn.syntax } mkString (".")
    }

    // Dissecting the import statement (without pattern matching on quasi-quotes)

    assertResult(List("eu.cdevreeze.yaidom.core")) {
      import1 collect { case imp: Importer => imp } map (_.syntax)
    }
    assertResult(List("eu.cdevreeze.yaidom")) {
      import1 collect { case Importer(ref, _) => ref } map (_.syntax)
    }
    assertResult(List(q"eu.cdevreeze.yaidom").map(_.structure)) {
      import1 collect { case Importer(ref, _) => ref } map (_.structure)
    }

    assertResult(List(Importee.Name(Name.Indeterminate("core")).structure)) {
      import1 collect { case imp: Importee => imp } map (_.structure)
    }
    assertResult(Seq("core")) {
      val importees = import1 collect { case imp: Importee => imp }
      importees flatMap { _ collect { case Name.Indeterminate(n) => n } }
    }
  }

  test("testMultipleClassImport") {
    // Building a Tree using quasi-quotes, and manually, and comparing the two

    val import1 = q"import eu.cdevreeze.yaidom.core.{ Scope, Path, EName }"

    val expImport =
      Import(
        List(
          Importer(
            Term.Select(
              Term.Select(
                Term.Select(
                  Term.Name("eu"),
                  Term.Name("cdevreeze")),
                Term.Name("yaidom")),
              Term.Name("core")),
            List(
              Importee.Name(Name.Indeterminate("Scope")),
              Importee.Name(Name.Indeterminate("Path")),
              Importee.Name(Name.Indeterminate("EName"))))))

    assertResult(expImport.structure) {
      import1.structure
    }

    // Parsing the import statement into the "same" tree

    assertResult(import1.structure) {
      "import eu.cdevreeze.yaidom.core.{ Scope, Path, EName }".parse[Stat].get.structure
    }

    // Collecting name sub-trees, and comparing structure and syntax with expectation

    assertResult(List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom"), Term.Name("core")).map(_.structure)) {
      import1 collect { case tn: Term.Name => tn.structure }
    }
    assertResult(List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom"), Term.Name("core")).map(_.syntax)) {
      import1 collect { case tn: Term.Name => tn.syntax }
    }
    assertResult("eu.cdevreeze.yaidom.core") {
      import1 collect { case tn: Term.Name => tn.syntax } mkString (".")
    }

    // Dissecting the import statement (without pattern matching on quasi-quotes)

    assertResult(List("eu.cdevreeze.yaidom.core.{ Scope, Path, EName }")) {
      import1 collect { case imp: Importer => imp } map (_.syntax)
    }
    assertResult(List("eu.cdevreeze.yaidom.core")) {
      import1 collect { case Importer(ref, _) => ref } map (_.syntax)
    }
    assertResult(List(q"eu.cdevreeze.yaidom.core").map(_.structure)) {
      import1 collect { case Importer(ref, _) => ref } map (_.structure)
    }

    assertResult(
      List(
        Importee.Name(Name.Indeterminate("Scope")),
        Importee.Name(Name.Indeterminate("Path")),
        Importee.Name(Name.Indeterminate("EName"))).map(_.structure)) {

        import1 collect { case imp: Importee => imp } map (_.structure)
      }
    assertResult(Seq("Scope", "Path", "EName")) {
      val importees = import1 collect { case imp: Importee => imp }
      importees flatMap { _ collect { case Name.Indeterminate(n) => n } }
    }
  }

  test("testRenameImport") {
    // Building a Tree using quasi-quotes, and manually, and comparing the two

    val import1 = q"import eu.cdevreeze.yaidom.indexed.{ Elem => IndexedElem }"

    val expImport =
      Import(
        List(
          Importer(
            Term.Select(
              Term.Select(
                Term.Select(
                  Term.Name("eu"),
                  Term.Name("cdevreeze")),
                Term.Name("yaidom")),
              Term.Name("indexed")),
            List(
              Importee.Rename(Name.Indeterminate("Elem"), Name.Indeterminate("IndexedElem"))))))

    assertResult(expImport.structure) {
      import1.structure
    }

    // Parsing the import statement into the "same" tree

    assertResult(import1.structure) {
      "import eu.cdevreeze.yaidom.indexed.{ Elem => IndexedElem }".parse[Stat].get.structure
    }

    // Collecting name sub-trees, and comparing structure and syntax with expectation

    assertResult(List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom"), Term.Name("indexed")).map(_.structure)) {
      import1 collect { case tn: Term.Name => tn.structure }
    }
    assertResult(List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom"), Term.Name("indexed")).map(_.syntax)) {
      import1 collect { case tn: Term.Name => tn.syntax }
    }
    assertResult("eu.cdevreeze.yaidom.indexed") {
      import1 collect { case tn: Term.Name => tn.syntax } mkString (".")
    }

    // Dissecting the import statement (without pattern matching on quasi-quotes)

    assertResult(List("eu.cdevreeze.yaidom.indexed.{ Elem => IndexedElem }")) {
      import1 collect { case imp: Importer => imp } map (_.syntax)
    }
    assertResult(List("eu.cdevreeze.yaidom.indexed")) {
      import1 collect { case Importer(ref, _) => ref } map (_.syntax)
    }
    assertResult(List(q"eu.cdevreeze.yaidom.indexed").map(_.structure)) {
      import1 collect { case Importer(ref, _) => ref } map (_.structure)
    }

    assertResult(
      List(
        Importee.Rename(Name.Indeterminate("Elem"), Name.Indeterminate("IndexedElem"))).map(_.structure)) {

        import1 collect { case imp: Importee => imp } map (_.structure)
      }
    assertResult(Seq("Elem", "IndexedElem")) {
      val importees = import1 collect { case imp: Importee => imp }
      importees flatMap { _ collect { case Name.Indeterminate(n) => n } }
    }
  }

  test("testCompilationUnit") {
    // Parsing the Source

    val source1 =
      """
object Elems {

  trait AnyElemApi {

    type ThisElem <: AnyElemApi

    def thisElem: ThisElem
  }

  trait ElemApi extends AnyElemApi {

    type ThisElem <: ElemApi

    def filterChildElems(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

    def findAllChildElems(): immutable.IndexedSeq[ThisElem]

    def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem]

    def findAllElemsOrSelf(): immutable.IndexedSeq[ThisElem]
  }

  trait ElemLike extends ElemApi { self =>

    type ThisElem <: ElemLike { type ThisElem = self.ThisElem }

    final def findAllChildElems(): immutable.IndexedSeq[ThisElem] = {
      filterChildElems(_ => true)
    }

    final def filterElemsOrSelf(p: ThisElem => Boolean): immutable.IndexedSeq[ThisElem] = {
      Vector(thisElem).filter(p) ++ findAllChildElems.flatMap(e => e.filterElemsOrSelf(p))
    }

    final def findAllElemsOrSelf(): immutable.IndexedSeq[ThisElem] = {
      filterElemsOrSelf(_ => true)
    }
  }
}""".parse[Source].get

    val expectedTrait1 =
      Defn.Trait(
        Nil,
        Type.Name("AnyElemApi"),
        Nil,
        Ctor.Primary(Nil, Ctor.Ref.Name("this"), Nil),
        Template(
          Nil,
          Nil,
          Term.Param(Nil, Name.Anonymous(), None, None),
          Some(
            List(
              Decl.Type(Nil, Type.Name("ThisElem"), Nil, Type.Bounds(None, Some(Type.Name("AnyElemApi")))),
              Decl.Def(Nil, Term.Name("thisElem"), Nil, Nil, Type.Name("ThisElem"))))))

    val expectedTrait2 =
      Defn.Trait(
        Nil,
        Type.Name("ElemApi"),
        Nil,
        Ctor.Primary(Nil, Ctor.Ref.Name("this"), Nil),
        Template(
          Nil,
          List(Ctor.Ref.Name("AnyElemApi")),
          Term.Param(Nil, Name.Anonymous(), None, None),
          Some(
            List(
              Decl.Type(Nil, Type.Name("ThisElem"), Nil, Type.Bounds(None, Some(Type.Name("ElemApi")))),
              Decl.Def(
                Nil,
                Term.Name("filterChildElems"),
                Nil,
                List(List(Term.Param(Nil, Term.Name("p"), Some(Type.Function(List(Type.Name("ThisElem")), Type.Name("Boolean"))), None))),
                Type.Apply(Type.Select(Term.Name("immutable"), Type.Name("IndexedSeq")), List(Type.Name("ThisElem")))),
              Decl.Def(
                Nil,
                Term.Name("findAllChildElems"),
                Nil,
                List(List()),
                Type.Apply(Type.Select(Term.Name("immutable"), Type.Name("IndexedSeq")), List(Type.Name("ThisElem")))),
              Decl.Def(
                Nil,
                Term.Name("filterElemsOrSelf"),
                Nil,
                List(List(Term.Param(Nil, Term.Name("p"), Some(Type.Function(List(Type.Name("ThisElem")), Type.Name("Boolean"))), None))),
                Type.Apply(Type.Select(Term.Name("immutable"), Type.Name("IndexedSeq")), List(Type.Name("ThisElem")))),
              Decl.Def(
                Nil,
                Term.Name("findAllElemsOrSelf"),
                Nil,
                List(List()),
                Type.Apply(Type.Select(Term.Name("immutable"), Type.Name("IndexedSeq")), List(Type.Name("ThisElem"))))))))

    val expectedTrait3 =
      Defn.Trait(
        Nil,
        Type.Name("ElemLike"),
        Nil,
        Ctor.Primary(Nil, Ctor.Ref.Name("this"), Nil),
        Template(
          Nil,
          List(Ctor.Ref.Name("ElemApi")),
          Term.Param(Nil, Term.Name("self"), None, None),
          Some(
            List(
              Decl.Type(
                Nil,
                Type.Name("ThisElem"),
                Nil,
                Type.Bounds(
                  None,
                  Some(
                    Type.Refine(
                      Some(Type.Name("ElemLike")),
                      List(Defn.Type(Nil, Type.Name("ThisElem"), Nil, Type.Select(Term.Name("self"), Type.Name("ThisElem")))))))),
              Defn.Def(
                List(Mod.Final()),
                Term.Name("findAllChildElems"),
                Nil,
                List(List()),
                Some(Type.Apply(Type.Select(Term.Name("immutable"), Type.Name("IndexedSeq")), List(Type.Name("ThisElem")))),
                Term.Block(
                  List(
                    Term.Apply(Term.Name("filterChildElems"), List(Term.Function(List(Term.Param(Nil, Name.Anonymous(), None, None)), Lit(true))))))),
              Defn.Def(
                List(Mod.Final()),
                Term.Name("filterElemsOrSelf"),
                Nil,
                List(List(Term.Param(Nil, Term.Name("p"), Some(Type.Function(List(Type.Name("ThisElem")), Type.Name("Boolean"))), None))),
                Some(Type.Apply(Type.Select(Term.Name("immutable"), Type.Name("IndexedSeq")), List(Type.Name("ThisElem")))),
                Term.Block(
                  List(
                    Term.ApplyInfix(
                      Term.Apply(Term.Select(Term.Apply(Term.Name("Vector"), List(Term.Name("thisElem"))), Term.Name("filter")), List(Term.Name("p"))),
                      Term.Name("++"),
                      Nil,
                      List(
                        Term.Apply(
                          Term.Select(Term.Name("findAllChildElems"), Term.Name("flatMap")),
                          List(
                            Term.Function(
                              List(Term.Param(Nil, Term.Name("e"), None, None)),
                              Term.Apply(Term.Select(Term.Name("e"), Term.Name("filterElemsOrSelf")), List(Term.Name("p"))))))))))),
              Defn.Def(
                List(Mod.Final()),
                Term.Name("findAllElemsOrSelf"),
                Nil,
                List(List()),
                Some(Type.Apply(Type.Select(Term.Name("immutable"), Type.Name("IndexedSeq")), List(Type.Name("ThisElem")))),
                Term.Block(
                  List(
                    Term.Apply(Term.Name("filterElemsOrSelf"), List(Term.Function(List(Term.Param(Nil, Name.Anonymous(), None, None)), Lit(true)))))))))))

    val expectedSource =
      Source(
        List(
          Defn.Object(
            Nil,
            Term.Name("Elems"),
            Template(
              Nil,
              Nil,
              Term.Param(Nil, Name.Anonymous(), None, None),
              Some(
                List(
                  expectedTrait1,
                  expectedTrait2,
                  expectedTrait3))))))

    assertResult(expectedSource.structure) {
      source1.structure
    }

    val traitDefs = source1 collect { case tr: Defn.Trait => tr }

    assertResult(List("AnyElemApi", "ElemApi", "ElemLike").map(t => Type.Name(t).structure)) {
      traitDefs.map(_.name.structure)
    }

    val elemApiTrait = traitDefs(1) ensuring (_.name.syntax.trim == "ElemApi")
    val elemLikeTrait = traitDefs(2) ensuring (_.name.syntax.trim == "ElemLike")

    assertResult(true) {
      val elemLikeFunDefs = (elemLikeTrait collect { case f: Defn.Def => f.name.structure }).toSet
      val elemApiFunDecls = (elemApiTrait collect { case f: Decl.Def => f.name.structure }).toSet
      elemLikeFunDefs.subsetOf(elemApiFunDecls)
    }
  }
}
