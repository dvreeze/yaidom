
// Run amm in scripts folder
// In amm session, use command "import $exec.eu.cdevreeze.yaidom.scripts.InitSaxonYaidomSession"

// Taking yaidom version 1.8.0-M4

import $ivy.`eu.cdevreeze.yaidom::yaidom:1.8.0-M4`

// Imports that (must) remain available after this initialization script

import java.net.URI
import java.io._
import scala.collection.immutable

import eu.cdevreeze.yaidom.core._
import eu.cdevreeze.yaidom._

import net.sf.saxon.s9api.Processor

// Easy creation of element predicates, even implicitly from ENames

import queryapi.HasENameApi._

val processor = new Processor(false)

val docBuilder = processor.newDocumentBuilder()

def parseDocument(file: File): saxon.SaxonDocument = {
  val xdmNode = docBuilder.build(file)
  saxon.SaxonDocument.wrapDocument(xdmNode.getUnderlyingNode.getTreeInfo)
}

def parseDocument(uri: URI): saxon.SaxonDocument = {
  val xdmNode = docBuilder.build(new javax.xml.transform.stream.StreamSource(uri.toURL.openStream()))
  saxon.SaxonDocument.wrapDocument(xdmNode.getUnderlyingNode.getTreeInfo)
}

def queryForPathCounts(doc: saxon.SaxonDocument): Map[List[EName], Int] = {
  doc.documentElement.findAllElemsOrSelf.groupBy(_.reverseAncestryOrSelfENames.toList).mapValues(_.size)
}

println("Now the REPL has been set up for ad-hoc yaidom querying and transformations, using Saxon tiny trees")
println("Use method parseDocument(file) or parseDocument(uri) to parse a document into a saxon.SaxonDocument")
println("Call method queryForPathCounts(saxonDoc) in order to get a feel for the structure of the document (especially if it is very large)")
