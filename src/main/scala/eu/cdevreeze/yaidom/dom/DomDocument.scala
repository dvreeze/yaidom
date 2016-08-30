/*
 * Copyright 2011-2014 Chris de Vreeze
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

package eu.cdevreeze.yaidom.dom

import java.net.URI
import java.nio.charset.Charset

import scala.collection.immutable

import org.w3c

import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.convert.DomConversions
import eu.cdevreeze.yaidom.queryapi.DocumentApi

/**
 * Wrapper around `org.w3c.dom.Document`. The yaidom wrapper is not considered to be a node, unlike the wrapped DOM
 * document (which is a DOM node).
 *
 * Use these wrappers only if there is a specific need for them. They are not immutable, and they are not thread-safe.
 *
 * @author Chris de Vreeze
 */
final class DomDocument(val wrappedDocument: w3c.dom.Document) extends DocumentApi {
  require(wrappedDocument ne null)

  type ThisDocApi = DomDocument

  type ThisDoc = DomDocument

  type DocElemType = DomElem

  final def children: immutable.IndexedSeq[CanBeDomDocumentChild] = {
    val childrenNodeList = wrappedDocument.getChildNodes

    DomConversions.nodeListToIndexedSeq(childrenNodeList) flatMap { node =>
      CanBeDomDocumentChild.wrapNodeOption(node)
    }
  }

  def documentElement: DomElem = DomNode.wrapElement(wrappedDocument.getDocumentElement)

  def uriOption: Option[URI] = Option(wrappedDocument.getDocumentURI).map(s => new URI(s))

  def comments: immutable.IndexedSeq[DomComment] = {
    children.collect({ case c: DomComment => c })
  }

  def processingInstructions: immutable.IndexedSeq[DomProcessingInstruction] = {
    children.collect({ case pi: DomProcessingInstruction => pi })
  }

  def xmlDeclarationOption: Option[XmlDeclaration] = {
    val xmlVersionOption = Option(wrappedDocument.getXmlVersion)
    val xmlDeclOption = xmlVersionOption map { xmlVersion =>
      XmlDeclaration.fromVersion(xmlVersion).
        withEncodingOption(Option(wrappedDocument.getXmlEncoding).map(cs => Charset.forName(cs))).
        withStandaloneOption(Some(wrappedDocument.getXmlStandalone))
    }
    xmlDeclOption
  }
}

object DomDocument {

  def apply(wrappedDoc: w3c.dom.Document): DomDocument = new DomDocument(wrappedDoc)

  def wrapDocument(doc: w3c.dom.Document): DomDocument = new DomDocument(doc)
}
