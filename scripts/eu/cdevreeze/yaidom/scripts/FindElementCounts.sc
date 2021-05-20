
// Run amm in scripts folder
// In amm session, use command "import $exec.eu.cdevreeze.yaidom.scripts.FindElementCounts"

// Taking yaidom version 1.12.0

import $ivy.`eu.cdevreeze.yaidom::yaidom:1.12.0`

import java.io._
import scala.collection.immutable
import scala.util.Try
import eu.cdevreeze.yaidom.core._
import eu.cdevreeze.yaidom._
import net.sf.saxon.s9api.Processor

ENameProvider.globalENameProvider.become(jvm.CaffeineENameProvider.fromMaximumCacheSize(5000))
QNameProvider.globalQNameProvider.become(jvm.CaffeineQNameProvider.fromMaximumCacheSize(5000))

val processor = new Processor(false)

// Helper functions for the script

def isProbableXmlFile(f: File): Boolean = {
  val name = Option(f.getName).getOrElse("")
  List("xml", "xsd", "xbrl").exists(suffix => name.endsWith("." + suffix))
}

def hasLengthLte(lth: Long)(f: File): Boolean = {
  f.length <= lth
}

def filterFiles(rootDir: File, p: File => Boolean): immutable.IndexedSeq[File] = {
  require(rootDir.isDirectory, s"Not a directory: $rootDir")
  
  val filesInDir = Option(rootDir.listFiles).map(_.toIndexedSeq).getOrElse(immutable.IndexedSeq())
  
  filesInDir
    .flatMap { f =>
      if (f.isFile) {
        immutable.IndexedSeq(f).filter(p)
      } else if (f.isDirectory) {
        // Recursive call
        filterFiles(f, p)
      } else {
        immutable.IndexedSeq()
      }
    }
}

// Data and logic used by the script

final case class ElementDepth(elementName: EName, depth: Int)

def extractElementDepths(rootElem: queryapi.BackingNodes.Elem): immutable.IndexedSeq[ElementDepth] = {
  rootElem
    .findAllElemsOrSelf
    .map(e => ElementDepth(e.resolvedName, e.path.entries.size))
}

def extractElementDepthsFromFile(f: File): immutable.IndexedSeq[ElementDepth] = {
  // Note that the DOM tree is only local to this method, so it is ready for garbage collection almost immediately
  
  val optSaxonDoc =
    Try(processor.newDocumentBuilder().build(f)).map(node => saxon.SaxonDocument.wrapDocument(node.getUnderlyingNode.getTreeInfo))
      .toOption

  optSaxonDoc.toIndexedSeq.flatMap(doc => extractElementDepths(doc.documentElement))
}

type Depth = Int

def groupElementCounts(elementDepths: immutable.IndexedSeq[ElementDepth]): Map[EName, Map[Depth, Int]] = {
  elementDepths
    .groupBy(_.elementName)
    .view.mapValues(_.groupBy(_.depth).view.mapValues(_.size).toMap).toMap
}

// The script itself

def findElementCounts(rootDir: File): Unit = {
  val start = System.currentTimeMillis()

  val probableXmlFiles = filterFiles(rootDir, isProbableXmlFile)
    .filter(hasLengthLte(50000000L))
  
  println(s"Found ${probableXmlFiles.size} probable XML files")

  println()  
  
  val elementDepths = probableXmlFiles.zipWithIndex
    .flatMap { case (f, idx) =>
      if (idx % 500 == 0) {
        println(s"Processed $idx (probable) XML documents so far")
      }
      
      extractElementDepthsFromFile(f)
    }

  println(s"Processed all ${probableXmlFiles.size} (probable) XML documents")

  println()  
  println(s"Found ${elementDepths.size} elements (with their depths)")

  println(s"Found ${elementDepths.groupBy(_.elementName).keySet.size} different element names")
  
  val groupedElementCounts = groupElementCounts(elementDepths)

  println()  
  
  groupedElementCounts.toIndexedSeq.sortBy(_._1.toString)
    .foreach { case (ename, depthCounts) =>
      depthCounts.toIndexedSeq.sortBy(_._1)
        .foreach { case (depth, count) =>
          println(s"[ name: $ename, depth: $depth, count: $count ]")
        }
    }
    
  val elementNameCounts: Map[EName, Int] = groupedElementCounts.view.mapValues(_.values.toIndexedSeq.sum).toMap
  
  println()
  println("Most occurring element names:")
  println()
  
  for {
    (ename, count) <- elementNameCounts.toIndexedSeq.sortBy(_._2).reverse.take(30)
  } {
    println(s"[ name: $ename, count: $count ]")
  }

  val end = System.currentTimeMillis()

  println()
  println(s"This function took ${end - start} ms (about ${(end - start) / 1000} seconds)")
}

// Now call function findElementCounts(rootDir), passing a rootDir as File object.

