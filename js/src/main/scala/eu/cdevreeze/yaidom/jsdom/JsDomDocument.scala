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

package eu.cdevreeze.yaidom.jsdom

import java.net.URI

import scala.collection.immutable

import org.scalajs.dom.{ raw => sjsdom }

import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.BackingDocumentApi

/**
 * Wrapper around `org.scalajs.dom.raw.Document`. The yaidom wrapper is not considered to be a node, unlike the wrapped DOM
 * document (which is a DOM node).
 *
 * Use these wrappers only if there is a specific need for them. They are not immutable, and they are not thread-safe
 * (which is no issue in the browser).
 *
 * @author Chris de Vreeze
 */
final class JsDomDocument(val wrappedDocument: sjsdom.Document) extends BackingDocumentApi {
  require(wrappedDocument ne null) // scalastyle:off null

  type ThisDoc = JsDomDocument

  type DocElemType = JsDomElem

  final def children: immutable.IndexedSeq[CanBeDomDocumentChild] = {
    val childrenNodeList = wrappedDocument.childNodes

    nodeListToIndexedSeq(childrenNodeList) flatMap { node =>
      CanBeDomDocumentChild.wrapNodeOption(node)
    }
  }

  def documentElement: JsDomElem = JsDomNode.wrapElement(wrappedDocument.documentElement)

  def uriOption: Option[URI] = Option(wrappedDocument.documentURI).map(s => new URI(s))

  def comments: immutable.IndexedSeq[JsDomComment] = {
    children.collect({ case c: JsDomComment => c })
  }

  def processingInstructions: immutable.IndexedSeq[JsDomProcessingInstruction] = {
    children.collect({ case pi: JsDomProcessingInstruction => pi })
  }

  def xmlDeclarationOption: Option[XmlDeclaration] = None

  /** Helper method that converts a `NodeList` to an `IndexedSeq[org.scalajs.dom.raw.Node]` */
  private def nodeListToIndexedSeq(nodeList: sjsdom.NodeList): immutable.IndexedSeq[sjsdom.Node] = {
    val result = (0 until nodeList.length) map { i => nodeList.item(i) }
    result.toIndexedSeq
  }
}

object JsDomDocument {

  def apply(wrappedDoc: sjsdom.Document): JsDomDocument = new JsDomDocument(wrappedDoc)

  def wrapDocument(doc: sjsdom.Document): JsDomDocument = new JsDomDocument(doc)
}
