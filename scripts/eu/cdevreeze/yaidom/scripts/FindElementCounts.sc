
// Run amm in scripts folder
// In amm session, use command "import $exec.eu.cdevreeze.yaidom.scripts.FindElementCounts"

// Taking yaidom version 1.8.1

import $ivy.`eu.cdevreeze.yaidom::yaidom:1.8.1`

import java.io._
import scala.collection.immutable
import scala.util.Try
import eu.cdevreeze.yaidom.core._
import eu.cdevreeze.yaidom._

def isProbableXmlFile(f: File): Boolean = {
  val name = Option(f.getName).getOrElse("")
  List("xml", "xsd", "xbrl").exists(suffix => name.endsWith("." + suffix))
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

// The DocumentParserUsingStax is the most efficient one of the DocumentParser implementations offered by yaidom

val docParser = parse.DocumentParserUsingStax.newInstance()

final case class ElementDepth(elementName: EName, depth: Int)

def extractElementDepths(rootElem: simple.Elem): immutable.IndexedSeq[ElementDepth] = {
  indexed.Elem(rootElem)
    .findAllElemsOrSelf
    .map(e => ElementDepth(e.resolvedName, e.path.entries.size))
}

def extractElementDepthsFromFile(f: File): immutable.IndexedSeq[ElementDepth] = {
  // Note that the DOM tree is only local to this method, so it is ready for garbage collection almost immediately
  
  Try(docParser.parse(f)).toOption.toIndexedSeq
    .flatMap(doc => extractElementDepths(doc.documentElement))
}

type Depth = Int

def groupElementCounts(elementDepths: immutable.IndexedSeq[ElementDepth]): Map[EName, Map[Depth, Int]] = {
  elementDepths
    .groupBy(_.elementName)
    .mapValues(_.groupBy(_.depth).mapValues(_.size))
}

// The script itself

def findElementCounts(rootDir: File): Unit = {
  val probableXmlFiles = filterFiles(rootDir, isProbableXmlFile)
  
  println(s"Found ${probableXmlFiles.size} probable XML files")

  println()  
  
  val elementDepths = probableXmlFiles.zipWithIndex
    .flatMap { case (f, idx) =>
      if (idx % 500 == 0) {
        println(s"Processed $idx (probable) XML documents so far")
      }
      
      extractElementDepthsFromFile(f)
    }

  println()  
  println(s"Found ${elementDepths.size} elements (with their depths)")
  println(s"Found ${elementDepths.map(_.elementName).distinct.size} different element names")
  
  val groupedElementCounts = groupElementCounts(elementDepths)

  println()  
  
  groupedElementCounts.toIndexedSeq.sortBy(_._1.toString)
    .foreach { case (ename, depthCounts) =>
      depthCounts.toIndexedSeq.sortBy(_._1)
        .foreach { case (depth, count) =>
          println(s"[ name: $ename, depth: $depth, count: $count ]")
        }
    }
    
  val elementNameCounts: Map[EName, Int] = groupedElementCounts.mapValues(_.values.toIndexedSeq.sum)
  
  println()
  println("Most occurring element names:")
  println()
  
  for {
    (ename, count) <- elementNameCounts.toIndexedSeq.sortBy(_._2).reverse.take(30)
  } {
    println(s"[ name: $ename, count: $count ]")
  }
}

// Now call function findElementCounts(rootDir), passing a rootDir as File object.
