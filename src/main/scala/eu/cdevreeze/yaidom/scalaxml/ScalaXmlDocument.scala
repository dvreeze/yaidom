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

package eu.cdevreeze.yaidom.scalaxml

import java.net.URI
import java.nio.charset.Charset

import scala.collection.immutable

import eu.cdevreeze.yaidom.core.XmlDeclaration
import eu.cdevreeze.yaidom.queryapi.DocumentApi

/**
 * Wrapper around a `scala.xml.Document`.
 *
 * @author Chris de Vreeze
 */
final class ScalaXmlDocument(val wrappedDocument: scala.xml.Document) extends DocumentApi {
  require(wrappedDocument ne null)

  type ThisDocApi = ScalaXmlDocument

  type ThisDoc = ScalaXmlDocument

  type DocElemType = ScalaXmlElem

  final def children: immutable.IndexedSeq[CanBeScalaXmlDocumentChild] = {
    wrappedDocument.children.toIndexedSeq flatMap { node =>
      node match {
        case e: scala.xml.Elem => Some(ScalaXmlElem(e))
        case pi: scala.xml.ProcInstr => Some(ScalaXmlProcessingInstruction(pi))
        case c: scala.xml.Comment => Some(ScalaXmlComment(c))
        case _ => None
      }
    }
  }

  def documentElement: ScalaXmlElem = ScalaXmlNode.wrapElement(wrappedDocument.docElem.asInstanceOf[scala.xml.Elem])

  def uriOption: Option[URI] = Option(wrappedDocument.baseURI).map(s => new URI(s))

  def comments: immutable.IndexedSeq[ScalaXmlComment] = {
    wrappedDocument.children.toIndexedSeq.collect({ case c: scala.xml.Comment => ScalaXmlComment(c) })
  }

  def processingInstructions: immutable.IndexedSeq[ScalaXmlProcessingInstruction] = {
    wrappedDocument.children.toIndexedSeq.collect({ case pi: scala.xml.ProcInstr => ScalaXmlProcessingInstruction(pi) })
  }

  def xmlDeclarationOption: Option[XmlDeclaration] = {
    val xmlVersionOption = wrappedDocument.version
    val xmlDeclOption = xmlVersionOption map { xmlVersion =>
      XmlDeclaration.fromVersion(xmlVersion).
        withEncodingOption(wrappedDocument.encoding.map(cs => Charset.forName(cs))).
        withStandaloneOption(wrappedDocument.standAlone)
    }
    xmlDeclOption
  }
}

object ScalaXmlDocument {

  def apply(wrappedDoc: scala.xml.Document): ScalaXmlDocument = new ScalaXmlDocument(wrappedDoc)

  def wrapDocument(doc: scala.xml.Document): ScalaXmlDocument = new ScalaXmlDocument(doc)
}
