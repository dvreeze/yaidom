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

import java.io.File

import org.junit.runner.RunWith
import org.scalatest.FunSuite
import org.scalatest.junit.JUnitRunner

import scala.collection.immutable
import scala.meta._

/**
 * Test case checking for unidirectional package dependencies in yaidom, by inspecting import statements.
 *
 * @author Chris de Vreeze
 */
@RunWith(classOf[JUnitRunner])
class PackageDependencyTest extends FunSuite {

  /**
   * Sub-package of yaidom, for example List("core") or List("java8", "indexed").
   */
  type SubPackage = List[String]

  private val mainScalaDirs: immutable.IndexedSeq[File] = {
    val euDir = new File(classOf[PackageDependencyTest].getResource("/eu").toURI)
    assert(euDir.isDirectory)
    val testClassDir = euDir.getParentFile
    assert(testClassDir.isDirectory)
    val projDir = getProjectDir(testClassDir).ensuring(_.getName == "jvm")
    val jvmResultDir = new File(projDir, "src/main/scala")

    val sharedProjectDir = new File(projDir.getParentFile, "shared")
    val sharedResultDir = new File(sharedProjectDir, "src/main/scala")

    immutable.IndexedSeq(jvmResultDir, sharedResultDir).ensuring(_.forall(_.isDirectory))
  }

  test("testParentPackage") {
    testPackageDependencies(None, Set(), Set())
  }

  test("testCorePackage") {
    testPackageDependencies(Some(List("core")), Set(), Set())
  }

  test("testQueryApiPackage") {
    testPackageDependencies(
      Some(List("queryapi")),
      Set(List("core")),
      Set(List("core")))
  }

  test("testResolvedPackage") {
    testPackageDependencies(
      Some(List("resolved")),
      Set(List("core"), List("queryapi")),
      Set(List("core"), List("queryapi")))
  }

  test("testSimplePackage") {
    testPackageDependencies(
      Some(List("simple")),
      Set(List("core"), List("queryapi"), List("resolved")),
      Set(List("core"), List("queryapi"), List("resolved")))
  }

  test("testIndexedPackage") {
    testPackageDependencies(
      Some(List("indexed")),
      Set(List("core"), List("queryapi"), List("resolved"), List("simple")),
      Set(List("core"), List("queryapi"), List("resolved"), List("simple")))
  }

  test("testConvertPackage") {
    testPackageDependencies(
      Some(List("convert")),
      Set(List("core"), List("simple")),
      Set(List("core"), List("queryapi"), List("resolved"), List("simple")))
  }

  test("testParsePackage") {
    testPackageDependencies(
      Some(List("parse")),
      Set(List("convert"), List("simple")),
      Set(List("core"), List("queryapi"), List("resolved"), List("simple"), List("convert")))
  }

  test("testPrintPackage") {
    testPackageDependencies(
      Some(List("print")),
      Set(List("convert"), List("simple")),
      Set(List("core"), List("queryapi"), List("resolved"), List("simple"), List("convert")))
  }

  test("testDomPackage") {
    testPackageDependencies(
      Some(List("dom")),
      Set(List("core"), List("queryapi"), List("resolved"), List("convert")),
      Set(List("core"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testScalaXmlPackage") {
    testPackageDependencies(
      Some(List("scalaxml")),
      Set(List("core"), List("queryapi"), List("resolved"), List("convert")),
      Set(List("core"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testJava8Package") {
    testPackageDependencies(
      Some(List("java8")),
      Set(List("core")),
      Set(List("core"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testJava8QueryApiPackage") {
    testPackageDependencies(
      Some(List("java8", "queryapi")),
      Set(List("core"), List("java8")),
      Set(List("core"), List("java8"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testJava8SimpleElemPackage") {
    testPackageDependencies(
      Some(List("java8", "simpleelem")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("simple")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("simple"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testJava8IndexedElemPackage") {
    testPackageDependencies(
      Some(List("java8", "indexedelem")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("indexed")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("indexed"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testJava8ScalaXmlElemPackage") {
    testPackageDependencies(
      Some(List("java8", "scalaxmlelem")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("scalaxml")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("scalaxml"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testJava8DomElemPackage") {
    testPackageDependencies(
      Some(List("java8", "domelem")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("dom")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("dom"), List("queryapi"), List("resolved"), List("convert")))
  }

  test("testJava8ResolvedElemPackage") {
    testPackageDependencies(
      Some(List("java8", "resolvedelem")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("resolved")),
      Set(List("core"), List("java8"), List("java8", "queryapi"), List("resolved"), List("queryapi"), List("convert")))
  }

  private def testPackageDependencies(
    yaidomSubPackageOption: Option[SubPackage],
    minimalSubPackages: Set[SubPackage],
    allowedSubPackages: Set[SubPackage]): Unit = {

    require(minimalSubPackages.subsetOf(allowedSubPackages))

    val dirs =
      if (yaidomSubPackageOption.isEmpty) {
        mainScalaDirs.map(d => new File(d, "eu/cdevreeze/yaidom"))
      } else {
        mainScalaDirs.map(d => new File(new File(d, "eu/cdevreeze/yaidom"), yaidomSubPackageOption.get.mkString("/")))
      }

    val sources = dirs.flatMap(d => findSourcesInDir(d))
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[SubPackage] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(Set.empty) {
      usedYaidomSubpackages.diff(allowedSubPackages)
    }
    assertResult(Set.empty) {
      minimalSubPackages.diff(usedYaidomSubpackages)
    }
  }

  private def findSourcesInDir(dir: File): immutable.IndexedSeq[Source] = {
    require(!dir.isFile)

    if (dir.isDirectory) {
      val sourceFiles = dir.listFiles.toVector.filter(f => f.isFile && f.getName.endsWith(".scala"))
      sourceFiles.map(f => f.parse[Source].get)
    } else {
      immutable.IndexedSeq()
    }
  }

  private def findImporters(source: Source): immutable.Seq[Importer] = {
    source collect { case i: Importer => i }
  }

  private def isYaidomImporter(importer: Importer): Boolean = {
    val termNames = importer.ref collect { case tn: Term.Name => tn }

    termNames.take(3).map(_.structure) ==
      List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom")).map(_.structure)
  }

  private def usedYaidomSubPackages(importer: Importer): Set[SubPackage] = {
    if (isYaidomImporter(importer)) {
      val termNames = importer.ref collect { case tn: Term.Name => tn }

      val termNamesAfterYaidom: List[Name] = termNames.drop(3)

      val importeeNames: Seq[Name] =
        importer.importees.flatMap(_.collect({ case n: Name => n }))

      val nameListsAfterYaidom: Seq[List[Name]] =
        importeeNames.map(name => termNamesAfterYaidom ::: List(name))

      val nameValueListsAfterYaidom: Seq[List[String]] =
        nameListsAfterYaidom.map(names => names.map(_.value))

      yaidomSubPackages.filter(subPkg => nameValueListsAfterYaidom.exists(names => names.startsWith(subPkg)))
    } else {
      Set()
    }
  }

  private val yaidomSubPackages: Set[SubPackage] = {
    Set(
      List("convert"),
      List("core"),
      List("dom"),
      List("indexed"),
      List("java8"),
      List("java8", "domelem"),
      List("java8", "indexedelem"),
      List("java8", "queryapi"),
      List("java8", "resolvedelem"),
      List("java8", "scalaxmlelem"),
      List("java8", "simpleelem"),
      List("parse"),
      List("print"),
      List("queryapi"),
      List("resolved"),
      List("scalaxml"),
      List("simple"),
      List("utils"))
  }

  private def getProjectDir(dir: File): File = {
    require((dir ne null) && dir.isDirectory, s"Expected directory '${dir}' but this is not a directory")

    if ((dir.getName == "yaidom") || (dir.getName == "jvm") || (dir.getName == "shared")) {
      dir
    } else {
      val parentDir = dir.getParentFile
      require(parentDir ne null, s"Unexpected directory '${dir}' without parent directory")

      // Recursive call
      getProjectDir(parentDir)
    }
  }
}
