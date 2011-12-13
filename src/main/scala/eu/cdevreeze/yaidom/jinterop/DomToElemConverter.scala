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
import org.w3c.dom.{ Document, Element, Attr, NamedNodeMap, NodeList }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }

/**
 * Converter from DOM Element to Elem
 *
 * @author Chris de Vreeze
 */
trait DomToElemConverter extends ConverterToElem[Element] {

  def convertToElem(v: Element): Elem = {
    val qname: QName = QName(Option(v.getPrefix), Option(v.getLocalName).getOrElse(v.getTagName))
    val attributes: Map[QName, String] = convertAttributes(v.getAttributes)

    val nsMap: Map[String, String] = {
      val nsOption = Option(v.getNamespaceURI)
      val prefixOption = Option(v.getPrefix)

      val elementScope: Map[String, String] = {
        val result: Option[(String, String)] =
          if (nsOption.isEmpty) None else {
            if (prefixOption.isEmpty) Some("" -> nsOption.get) else Some(prefixOption.get -> nsOption.get)
          }

        if (result.isDefined) Map(result.get) else Map()
      }

      val attributeScope: Map[String, String] = getAttributeScope(v.getAttributes)

      elementScope ++ attributeScope
    }

    Elem(
      qname = qname,
      attributes = attributes,
      scope = Scope.fromMap(nsMap),
      children = convertNodeList(v.getChildNodes).flatMap(n => convertToNodeOption(n)))
  }

  private def convertToNodeOption(v: org.w3c.dom.Node): Option[Node] = {
    v match {
      case e: Element => Some(convertToElem(e))
      case cdata: org.w3c.dom.CDATASection => Some(convertToCData(cdata))
      case t: org.w3c.dom.Text => Some(convertToText(t))
      case pi: org.w3c.dom.ProcessingInstruction => Some(convertToProcessingInstruction(pi))
      // TODO EntityRef
      case _ => None
    }
  }

  private def convertToText(v: org.w3c.dom.Text): Text = Text(v.getData)

  private def convertToProcessingInstruction(v: org.w3c.dom.ProcessingInstruction): ProcessingInstruction =
    ProcessingInstruction(v.getTarget, v.getData)

  private def convertToCData(v: org.w3c.dom.CDATASection): CData = CData(v.getData)

  private def convertAttributes(domAttributes: NamedNodeMap): Map[QName, String] = {
    (0 until domAttributes.getLength).map(i => {
      val attr = domAttributes.item(i).asInstanceOf[Attr]
      val qname: QName = QName(Option(attr.getPrefix), Option(attr.getLocalName).getOrElse(attr.getName))

      (qname -> attr.getValue)
    }).toMap
  }

  private def getAttributeScope(domAttributes: NamedNodeMap): Map[String, String] = {
    (0 until domAttributes.getLength).flatMap(i => {
      val attr = domAttributes.item(i).asInstanceOf[Attr]
      val prefixOption = Option(attr.getPrefix)

      // No default namespace for attributes!
      if (prefixOption.isEmpty) None else {
        require(attr.getNamespaceURI ne null)
        Some(prefixOption.get -> attr.getNamespaceURI)
      }
    }).toMap
  }

  private def convertNodeList(nodeList: NodeList): immutable.IndexedSeq[org.w3c.dom.Node] = {
    (0 until nodeList.getLength).map(i => nodeList.item(i)).toIndexedSeq
  }
}
