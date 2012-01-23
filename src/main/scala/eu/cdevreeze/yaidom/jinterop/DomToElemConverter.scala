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
package jinterop

import java.{ util => jutil }
import javax.xml.XMLConstants
import org.w3c.dom.{ Element, Attr, NamedNodeMap, NodeList }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }

/**
 * Converter from DOM Element to Elem, and from DOM Document to (yaidom) Document.
 *
 * @author Chris de Vreeze
 */
trait DomToElemConverter extends ConverterToElem[Element] with ConverterToDocument[org.w3c.dom.Document] {

  def convertToDocument(v: org.w3c.dom.Document): Document = {
    Document(
      documentElement = convertToElem(v.getDocumentElement, Scope.Empty),
      processingInstructions =
        nodeListToIndexedSeq(v.getChildNodes) collect { case pi: org.w3c.dom.ProcessingInstruction => convertToProcessingInstruction(pi) },
      comments =
        nodeListToIndexedSeq(v.getChildNodes) collect { case c: org.w3c.dom.Comment => convertToComment(c) })
  }

  def convertToElem(v: Element): Elem = {
    convertToElem(v, Scope.Empty)
  }

  /** Given a parent scope, converts an org.w3c.dom.Element to a yaidom Elem */
  private def convertToElem(v: Element, parentScope: Scope): Elem = {
    val qname: QName = toQName(v)
    val attributes: Map[QName, String] = convertAttributes(v.getAttributes)

    val namespaceDeclarations: Scope.Declarations = extractNamespaceDeclarations(v.getAttributes)
    val newScope: Scope = parentScope.resolve(namespaceDeclarations)

    Elem(
      qname = qname,
      attributes = attributes,
      scope = newScope,
      children = nodeListToIndexedSeq(v.getChildNodes) flatMap { n => convertToNodeOption(n, newScope) })
  }

  /** Given a parent scope, converts an org.w3c.dom.Node to an optional yaidom Node */
  private def convertToNodeOption(v: org.w3c.dom.Node, parentScope: Scope): Option[Node] = {
    v match {
      case e: Element => Some(convertToElem(e, parentScope))
      case cdata: org.w3c.dom.CDATASection => Some(convertToCData(cdata))
      case t: org.w3c.dom.Text => Some(convertToText(t))
      case pi: org.w3c.dom.ProcessingInstruction => Some(convertToProcessingInstruction(pi))
      case er: org.w3c.dom.EntityReference => Some(convertToEntityRef(er))
      case c: org.w3c.dom.Comment => Some(convertToComment(c))
      case _ => None
    }
  }

  /** Converts an org.w3c.dom.Text to a yaidom Text */
  private def convertToText(v: org.w3c.dom.Text): Text = Text(v.getData)

  /** Converts an org.w3c.dom.ProcessingInstruction to a yaidom ProcessingInstruction */
  private def convertToProcessingInstruction(v: org.w3c.dom.ProcessingInstruction): ProcessingInstruction =
    ProcessingInstruction(v.getTarget, v.getData)

  /** Converts an org.w3c.dom.CDATASection to a yaidom CData */
  private def convertToCData(v: org.w3c.dom.CDATASection): CData = CData(v.getData)

  /** Converts an org.w3c.dom.EntityReference to a yaidom EntityRef */
  private def convertToEntityRef(v: org.w3c.dom.EntityReference): EntityRef = EntityRef(v.getNodeName)

  /** Converts an org.w3c.dom.Comment to a yaidom Comment */
  private def convertToComment(v: org.w3c.dom.Comment): Comment = Comment(v.getData)

  /** Converts a NamedNodeMap to a Map[QName, String] */
  private def convertAttributes(domAttributes: NamedNodeMap): Map[QName, String] = {
    (0 until domAttributes.getLength).flatMap(i => {
      val attr = domAttributes.item(i).asInstanceOf[Attr]

      if (isNamespaceDeclaration(attr)) None else {
        val qname: QName = toQName(attr)
        Some(qname -> attr.getValue)
      }
    }).toMap
  }

  /** Converts the namespace declarations in a NamedNodeMap to a Scope.Declarations */
  private def extractNamespaceDeclarations(domAttributes: NamedNodeMap): Scope.Declarations = {
    val nsMap = {
      val result = (0 until domAttributes.getLength) flatMap { i =>
        val attr = domAttributes.item(i).asInstanceOf[Attr]

        if (isNamespaceDeclaration(attr)) {
          val result = extractNamespaceDeclaration(attr)
          Some(result) map { pair => (pair._1.getOrElse(""), pair._2) }
        } else None
      }
      result.toMap
    }
    Scope.Declarations.fromMap(nsMap)
  }

  /** Converts a NodeList to an IndexedSeq[org.w3c.dom.Node] */
  private def nodeListToIndexedSeq(nodeList: NodeList): immutable.IndexedSeq[org.w3c.dom.Node] = {
    (0 until nodeList.getLength) map { i => nodeList.item(i) } toIndexedSeq
  }

  /** Extracts the QName of an org.w3c.dom.Element */
  private def toQName(v: org.w3c.dom.Element): QName = {
    val name: String = v.getTagName
    val arr = name.split(':')
    require(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) UnprefixedName(arr(0)) else PrefixedName(arr(0), arr(1))
  }

  /** Extracts the QName of an org.w3c.dom.Attr. If the Attr is a namespace declaration, the prefix or unprefixed name is "xmlns" */
  private def toQName(v: org.w3c.dom.Attr): QName = {
    val name: String = v.getName
    val arr = name.split(':')
    require(arr.length >= 1 && arr.length <= 2)
    if (arr.length == 1) UnprefixedName(arr(0)) else PrefixedName(arr(0), arr(1))
  }

  /** Returns true if the org.w3c.dom.Attr is a namespace declaration */
  private def isNamespaceDeclaration(v: org.w3c.dom.Attr): Boolean = {
    val name: String = v.getName
    val arr = name.split(':')
    require(arr.length >= 1 && arr.length <= 2)
    val result = arr(0) == "xmlns"
    result
  }

  /** Extracts (optional) prefix and namespace. Call only if isNamespaceDeclaration(v). */
  private def extractNamespaceDeclaration(v: org.w3c.dom.Attr): (Option[String], String) = {
    val name: String = v.getName
    val arr = name.split(':')
    require(arr.length >= 1 && arr.length <= 2)
    require(arr(0) == "xmlns")
    val prefixOption: Option[String] = if (arr.length == 1) None else Some(arr(1))
    val attrValue: String = v.getValue
    (prefixOption, attrValue)
  }

  private def empty2Null(s: String): String = if (s == "") null else s
}
