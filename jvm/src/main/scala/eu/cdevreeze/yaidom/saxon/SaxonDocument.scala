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

package eu.cdevreeze.yaidom.saxon

import java.net.URI

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi
import net.sf.saxon.`type`.Type
import net.sf.saxon.om.AxisInfo
import net.sf.saxon.om.NodeInfo
import net.sf.saxon.om.TreeInfo

/**
 * Saxon document, wrapping a Saxon (9.7+) TreeInfo object.
 *
 * @author Chris de Vreeze
 */
// scalastyle:off null
final class SaxonDocument(val wrappedTreeInfo: TreeInfo) extends BackingDocumentApi {
  require(wrappedNode ne null)
  require(wrappedNode.getNodeKind == Type.DOCUMENT, s"Expected document but got node kind ${wrappedNode.getNodeKind}")

  type ThisDoc = SaxonDocument

  type DocElemType = SaxonElem

  def wrappedNode: NodeInfo = wrappedTreeInfo.getRootNode

  def uriOption: Option[URI] = Option(wrappedNode.getSystemId).map(s => URI.create(s)) // ??

  def documentElement: SaxonElem =
    (children collectFirst { case e: SaxonElem => e }).getOrElse(sys.error(s"Missing document element"))

  def children: immutable.IndexedSeq[SaxonCanBeDocumentChild] = {
    val it = wrappedNode.iterateAxis(AxisInfo.CHILD)

    val nodes = Stream.continually(it.next()).takeWhile(_ ne null).toVector

    nodes.flatMap(nodeInfo => SaxonNode.wrapNodeOption(nodeInfo)) collect {
      case ch: SaxonCanBeDocumentChild => ch
    }
  }

  def processingInstructions: immutable.IndexedSeq[SaxonProcessingInstruction] =
    children.collect({ case pi: SaxonProcessingInstruction => pi })

  def comments: immutable.IndexedSeq[SaxonComment] =
    children.collect({ case c: SaxonComment => c })

  def xmlDeclarationOption: Option[XmlDeclaration] = None
}

object SaxonDocument {

  def wrapDocument(doc: TreeInfo): SaxonDocument = new SaxonDocument(doc)
}
