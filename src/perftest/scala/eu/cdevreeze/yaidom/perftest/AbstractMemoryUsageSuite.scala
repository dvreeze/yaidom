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
package perftest

import java.{ util => jutil }
import java.io.File
import java.lang.management.ManagementFactory
import scala.util.Try
import org.scalatest.FunSuite
import testtag.PerformanceTest

/**
 * Abstract memory usage suite super-class, for different yaidom element types.
 *
 * It requires sbt to run with "-Dperftest.rootDir=/path/to/rootdir". All files under that rootDir that are regarded to be XML files
 * (due to the extension in the file name, such as ".xml", ".xsd", etc.) are parsed.
 *
 * In order to get some useful logging output, and in order for the test to check reasonable assertions, make sure to run each
 * concrete suite that is a sub-class of this abstract suite in isolation! For example:
 * {{{
 * test-only eu.cdevreeze.yaidom.perftest.MemoryUsageSuiteForElem
 * }}}
 *
 * @author Chris de Vreeze
 */
abstract class AbstractMemoryUsageSuite extends FunSuite {

  type E <: ElemLike[E] with HasText

  private val logger: jutil.logging.Logger = jutil.logging.Logger.getLogger("eu.cdevreeze.yaidom.perftest")

  private def rootDir: File = {
    Option(System.getProperty("perftest.rootDir")).map(f => new File(f)).getOrElse(
      sys.error(s"Missing system property 'perftest.rootDir'. All XML files somewhere below the rootDir are used by the test."))
  }

  test("On querying, memory usage should be within reasonable bounds", PerformanceTest) {
    require(rootDir.isDirectory, s"Expected directory $rootDir, but this is not an existing directory")

    logger.info(s"Entering test. Test class: ${this.getClass.getName}")

    val memBean = ManagementFactory.getMemoryMXBean

    def getUsedHeapMemoryInMiB(): Long = convertByteCountToMiB(memBean.getHeapMemoryUsage.getUsed)

    val xmlFiles = findFiles(rootDir).filter(f => Set(".xml", ".xsd").exists(ext => f.getName.endsWith(ext)))

    memBean.gc()
    val heapMemBeforeParsingInMiB = getUsedHeapMemoryInMiB
    logger.info(s"Heap memory usage before parsing XML: ${heapMemBeforeParsingInMiB} MiB")

    val totalXmlFileLength = xmlFiles.map(_.length).sum
    val totalXmlFileLengthInMiB = convertByteCountToMiB(totalXmlFileLength)

    logger.info(s"Total of the XML file lengths (of ${xmlFiles.size} XML files): $totalXmlFileLengthInMiB MiB")

    val docElems = parseXmlFiles(xmlFiles).flatMap(_.toOption)

    memBean.gc()
    val heapMemAfterParsingInMiB = getUsedHeapMemoryInMiB
    logger.info(s"Heap memory usage after parsing ${docElems.size} XML files: ${heapMemAfterParsingInMiB} MiB")

    expectResult(
      true,
      s"Parsed XML should not need more than $maxMemoryToFileLengthRatio times the memory that their combined byte count") {
        (heapMemAfterParsingInMiB - heapMemBeforeParsingInMiB) <= maxMemoryToFileLengthRatio.toLong * totalXmlFileLengthInMiB
      }

    val allDocElem = createCommonRootParent(docElems)

    memBean.gc()
    logger.info(s"Heap memory usage after creating large combined XML: ${getUsedHeapMemoryInMiB} MiB")

    val allDocElems = allDocElem.findAllElemsOrSelf
    logger.info(s"The combined XML has ${allDocElems.size} elements")

    memBean.gc()
    logger.info(s"Heap memory usage after this query on the large combined XML: ${getUsedHeapMemoryInMiB} MiB")

    val allDocElemsWithNS = allDocElem \\ (elem => elem.resolvedName.namespaceUriOption.isDefined)
    logger.info(s"The combined XML has ${allDocElemsWithNS.size} elements with names having a namespace")

    memBean.gc()
    logger.info(s"Heap memory usage after this query on the large combined XML: ${getUsedHeapMemoryInMiB} MiB")

    val elementNameNamespaces = allDocElem.findAllElemsOrSelf.flatMap(_.resolvedName.namespaceUriOption).distinct.sorted

    val attrNamespaces =
      allDocElem.findAllElemsOrSelf.flatMap(e => e.resolvedAttributes).flatMap(_._1.namespaceUriOption).distinct.sorted

    logger.info(s"The combined XML has ${elementNameNamespaces.size} different namespaces in element names")
    logger.info(s"The combined XML has ${attrNamespaces.size} different namespaces in attribute names")

    memBean.gc()
    logger.info(s"Heap memory usage after these queries on the large combined XML: ${getUsedHeapMemoryInMiB} MiB")

    logger.info(s"Leaving test. Test class: ${this.getClass.getName}")
  }

  private def findFiles(dir: File): Vector[File] = {
    require(dir.isDirectory)
    val files = dir.listFiles.toVector
    files.filter(_.isFile) ++ files.filter(_.isDirectory).flatMap(d => findFiles(d))
  }

  private def convertByteCountToMiB(byteCount: Long): Long = byteCount >> 20

  protected def parseXmlFiles(files: Vector[File]): Vector[Try[E]]

  protected def getDocumentParser: parse.DocumentParser = {
    val parserClass =
      Class.forName(System.getProperty("perftest.documentParser", "eu.cdevreeze.yaidom.parse.DocumentParserUsingSax")).asInstanceOf[Class[parse.DocumentParser]]

    val thisClass = classOf[AbstractMemoryUsageSuite]
    val enameFiles =
      List(
        new File(thisClass.getResource("/eu/cdevreeze/yaidom/enames-xs.txt").toURI),
        new File(thisClass.getResource("/eu/cdevreeze/yaidom/enames-xlink.txt").toURI),
        new File(thisClass.getResource("/eu/cdevreeze/yaidom/enames-link.txt").toURI))
    val enameCache =
      enameFiles flatMap { file => scala.io.Source.fromFile(file).getLines.toVector } map { s => EName.parse(s) }
    val enameProvider =
      new ENameProvider.ENameProviderUsingImmutableMap(enameCache.toSet + EName("{http://www.xbrl.org/2003/instance}periodType"))

    val qnameFiles =
      List(
        new File(thisClass.getResource("/eu/cdevreeze/yaidom/qnames-xs.txt").toURI),
        new File(thisClass.getResource("/eu/cdevreeze/yaidom/qnames-xlink.txt").toURI),
        new File(thisClass.getResource("/eu/cdevreeze/yaidom/qnames-link.txt").toURI))
    val qnameCache =
      qnameFiles flatMap { file => scala.io.Source.fromFile(file).getLines.toVector } map { s => QName.parse(s) }
    val qnameProvider =
      new QNameProvider.QNameProviderUsingImmutableMap(qnameCache.toSet + QName("xbrli:periodType"))

    val parserFactoryMethod = parserClass.getDeclaredMethod("newInstance", classOf[ENameProvider], classOf[QNameProvider])
    parserFactoryMethod.invoke(null, enameProvider, qnameProvider).asInstanceOf[parse.DocumentParser]
  }

  protected def createCommonRootParent(rootElems: Vector[E]): E

  protected def maxMemoryToFileLengthRatio: Int
}
