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
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    assertResult(Nil) {
      importers.filter(i => isYaidomImporter(i))
    }
  }

  test("testCorePackage") {
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom/core").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(Set()) {
      usedYaidomSubpackages
    }
  }

  test("testQueryApiPackage") {
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom/queryapi").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(Set("core")) {
      usedYaidomSubpackages
    }
  }

  test("testResolvedPackage") {
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom/resolved").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(Set("core", "queryapi")) {
      usedYaidomSubpackages
    }
  }

  test("testSimplePackage") {
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom/simple").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(Set("core", "queryapi", "resolved")) {
      usedYaidomSubpackages
    }
  }

  test("testConvertPackage") {
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom/convert").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(true) {
      usedYaidomSubpackages.subsetOf(Set("core", "queryapi", "resolved", "simple"))
    }
    assertResult(true) {
      Set("core", "simple").subsetOf(usedYaidomSubpackages)
    }
  }

  test("testParsePackage") {
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom/parse").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(true) {
      usedYaidomSubpackages.subsetOf(Set("core", "queryapi", "resolved", "simple", "convert"))
    }
    assertResult(true) {
      Set("simple", "convert").subsetOf(usedYaidomSubpackages)
    }
  }

  test("testPrintPackage") {
    val dir = new File(mainScalaDir, "eu/cdevreeze/yaidom/print").ensuring(_.isDirectory)

    val sources = findSourcesInDir(dir)
    val importers = sources.flatMap(source => findImporters(source))

    val usedYaidomSubpackages: Set[String] = importers.flatMap(i => usedYaidomSubPackages(i)).toSet

    assertResult(true) {
      usedYaidomSubpackages.subsetOf(Set("core", "queryapi", "resolved", "simple", "convert"))
    }
    assertResult(true) {
      Set("simple", "convert").subsetOf(usedYaidomSubpackages)
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
