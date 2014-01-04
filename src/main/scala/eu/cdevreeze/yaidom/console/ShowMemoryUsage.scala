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
package console

import java.io._
import java.net.URI
import scala.util.Try
import eu.cdevreeze.yaidom._
import eu.cdevreeze.yaidom.parse._
import eu.cdevreeze.yaidom.print._
import java.lang.management.ManagementFactory

/**
 * "Script" showing memory usage of yaidom, when parsing XML and querying it.
 *
 * @author Chris de Vreeze
 */
private[yaidom] abstract class ShowMemoryUsage[E <: ElemApi[E] with HasText] extends Runnable {

  def rootDir: File

  def parseXmlFiles(files: Vector[File]): Vector[Try[E]]

  def createCommonRootParent(rootElems: Vector[E]): E

  def run(): Unit = {
    val startMs = System.currentTimeMillis()

    require(rootDir.isDirectory, s"Expected directory $rootDir, but this is not an existing directory")

    val memBean = ManagementFactory.getMemoryMXBean

    def getUsedHeapMemoryInMiB(): Long = memBean.getHeapMemoryUsage.getUsed >> 20

    val xmlFiles = findFiles(rootDir).filter(f => Set(".xml", ".xsd").exists(ext => f.getName.endsWith(ext)))

    memBean.gc()
    println(s"Heap memory usage before parsing XML: ${getUsedHeapMemoryInMiB} MiB")

    val totalXmlFileLength = xmlFiles.map(_.length).sum

    println()
    println(s"Total of the XML file lengths (of ${xmlFiles.size} XML files): $totalXmlFileLength")

    val docElems = parseXmlFiles(xmlFiles).flatMap(_.toOption)

    memBean.gc()
    println()
    println(s"Heap memory usage after parsing ${docElems.size} XML files: ${getUsedHeapMemoryInMiB} MiB")

    val allDocElem = createCommonRootParent(docElems)

    memBean.gc()
    println()
    println(s"Heap memory usage after creating large super-XML: ${getUsedHeapMemoryInMiB} MiB")

    val allDocElems = allDocElem.findAllElemsOrSelf
    println()
    println(s"The super-XML has ${allDocElems.size} elements")

    memBean.gc()
    println(s"Heap memory usage after this query on the large super-XML: ${getUsedHeapMemoryInMiB} MiB")

    val allDocElemsWithNS = allDocElem \\ (elem => elem.resolvedName.namespaceUriOption.isDefined)
    println()
    println(s"The super-XML has ${allDocElemsWithNS.size} elements with names having a namespace")

    memBean.gc()
    println(s"Heap memory usage after this query on the large super-XML: ${getUsedHeapMemoryInMiB} MiB")

    val elementNameNamespaces = allDocElem.findAllElemsOrSelf.flatMap(_.resolvedName.namespaceUriOption).distinct.sorted

    val attrNamespaces =
      allDocElem.findAllElemsOrSelf.flatMap(e => e.resolvedAttributes).flatMap(_._1.namespaceUriOption).distinct.sorted

    println()
    println(s"The super-XML has ${elementNameNamespaces.size} different namespaces in element names")
    println(s"The super-XML has ${attrNamespaces.size} different namespaces in attribute names")

    memBean.gc()
    println(s"Heap memory usage after these queries on the large super-XML: ${getUsedHeapMemoryInMiB} MiB")

    val endMs = System.currentTimeMillis()

    println()
    println(s"This script took ${(endMs - startMs) / 1000} seconds to run")
  }

  private def findFiles(dir: File): Vector[File] = {
    require(dir.isDirectory)
    val files = dir.listFiles.toVector
    files.filter(_.isFile) ++ files.filter(_.isDirectory).flatMap(d => findFiles(d))
  }
}

private[yaidom] object ShowMemoryUsage {

  def main(args: Array[String]): Unit = {
    require(args.size == 2, "Usage: ShowMemoryUsage <script FQCN> <root dir>")

    val scriptClass = Class.forName(args(0)).asInstanceOf[Class[ShowMemoryUsage[_]]]

    val rootDir = new File(args(1))
    require(rootDir.isDirectory, s"Expected directory $rootDir, but this is not an existing directory")

    val scriptConstructor = scriptClass.getConstructor(classOf[File])
    val script = scriptConstructor.newInstance(rootDir)

    script.run()
  }
}
