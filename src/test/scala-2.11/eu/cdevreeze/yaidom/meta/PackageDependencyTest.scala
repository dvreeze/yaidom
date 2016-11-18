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

import org.junit.Test
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

  private val mainScalaDir: File = {
    val euDir = new File(classOf[PackageDependencyTest].getResource("/eu").toURI)
    assert(euDir.isDirectory)
    val testClassDir = euDir.getParentFile
    assert(testClassDir.isDirectory)
    val projDir = getProjectDir(testClassDir)
    val resultDir = new File(projDir, "src/main/scala")
    resultDir.ensuring(_.isDirectory)
  }

  test("testParentPackage") {
    testPackageDependencies(None, Set(), Set())
  }

  test("testCorePackage") {
    testPackageDependencies(Some("core"), Set(), Set())
  }

  test("testQueryApiPackage") {
    testPackageDependencies(
      Some("queryapi"),
      Set("core"),
      Set("core"))
  }

  test("testResolvedPackage") {
    testPackageDependencies(
      Some("resolved"),
      Set("core", "queryapi"),
      Set("core", "queryapi"))
  }

  test("testSimplePackage") {
    testPackageDependencies(
      Some("simple"),
      Set("core", "queryapi", "resolved"),
      Set("core", "queryapi", "resolved"))
  }

  test("testIndexedPackage") {
    testPackageDependencies(
      Some("indexed"),
      Set("core", "queryapi", "resolved", "simple"),
      Set("core", "queryapi", "resolved", "simple"))
  }

  test("testConvertPackage") {
    testPackageDependencies(
      Some("convert"),
      Set("core", "simple"),
      Set("core", "queryapi", "resolved", "simple"))
  }

  test("testParsePackage") {
    testPackageDependencies(
      Some("parse"),
      Set("convert", "simple"),
      Set("core", "queryapi", "resolved", "simple", "convert"))
  }

  test("testPrintPackage") {
    testPackageDependencies(
      Some("print"),
      Set("convert", "simple"),
      Set("core", "queryapi", "resolved", "simple", "convert"))
  }

  test("testDomPackage") {
    testPackageDependencies(
      Some("dom"),
      Set("core", "queryapi", "resolved", "convert"),
      Set("core", "queryapi", "resolved", "convert"))
  }

  test("testScalaXmlPackage") {
    testPackageDependencies(
      Some("scalaxml"),
      Set("core", "queryapi", "resolved", "convert"),
      Set("core", "queryapi", "resolved", "convert"))
  }

  private def testPackageDependencies(
    yaidomSubPackageOption: Option[String],
    minimalSubPackages: Set[String],
    allowedSubPackages: Set[String]): Unit = {

    require(minimalSubPackages.subsetOf(allowedSubPackages))

    val dir =
      if (yaidomSubPackageOption.isEmpty) {
        new File(mainScalaDir, "eu/cdevreeze/yaidom")
      } else {
        new File(new File(mainScalaDir, "eu/cdevreeze/yaidom"), yaidomSubPackageOption.get)
      }

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(Set.empty) {
      usedYaidomSubpackages.diff(allowedSubPackages)
    }
    assertResult(Set.empty) {
      minimalSubPackages.diff(usedYaidomSubpackages)
    }
  }

  private def findSourcesInDir(dir: File): immutable.IndexedSeq[Source] = {
    require(dir.isDirectory)
    val sourceFiles = dir.listFiles.toVector.filter(f => f.isFile && f.getName.endsWith(".scala"))
    sourceFiles.map(f => f.parse[Source].get)
  }

  private def findImporters(source: Source): immutable.Seq[Importer] = {
    source collect { case i: Importer => i }
  }

  private def isYaidomImporter(importer: Importer): Boolean = {
    val termNames = importer collect { case tn: Term.Name => tn }

    termNames.take(3).map(_.structure) ==
      List(Term.Name("eu"), Term.Name("cdevreeze"), Term.Name("yaidom")).map(_.structure)
  }

  private def usedYaidomSubPackages(importer: Importer): Set[String] = {
    if (isYaidomImporter(importer)) {
      val termNames = importer collect { case tn: Term.Name => tn }

      val termNameOption = termNames.drop(3).headOption

      termNameOption match {
        case Some(name) if yaidomSubPackages.contains(name.value.trim) => Set(name.value.trim)
        case Some(name) => Set()
        case None =>
          importer.importees.flatMap(_.collect({ case n: Name if yaidomSubPackages.contains(n.value) => n.value })).toSet
      }
    } else {
      Set()
    }
  }

  private val yaidomSubPackages: Set[String] = {
    Set("convert", "core", "dom", "indexed", "java8", "parse", "print", "queryapi", "resolved", "scalaxml", "simple", "utils")
  }

  private def getProjectDir(dir: File): File = {
    require((dir ne null) && dir.isDirectory, s"Expected directory '${dir}' but this is not a directory")

    if (dir.getName == "yaidom") {
      dir
    } else {
      val parentDir = dir.getParentFile
      require(parentDir ne null, s"Unexpected directory '${dir}' without parent directory")

      // Recursive call
      getProjectDir(parentDir)
    }
  }
}
