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
import org.w3c.dom.{ Document, Element }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import ElemToDomConverter._

/**
 * Converter from Elem to a DOM element.
 *
 * @author Chris de Vreeze
 */
trait ElemToDomConverter extends ElemConverter[ElementProducer] {

  def convertElem(elm: Elem): ElementProducer = {
    { (doc: Document) =>
      val element = convertElem(elm, doc, doc, Scope.Empty)
      element
    }
  }

  private def convertNode(node: Node, doc: Document, parent: org.w3c.dom.Node, parentScope: Scope): org.w3c.dom.Node = {
    node match {
      case e: Elem => convertElem(e, doc, parent, parentScope)
      case t: Text => convertText(t, doc, parent)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi, doc, parent)
      case t: CData => convertCData(t, doc, parent)
      case er: EntityRef => convertEntityRef(er, doc, parent)
    }
  }

  private def convertElem(elm: Elem, doc: Document, parent: org.w3c.dom.Node, parentScope: Scope): Element = {
    // Not tail-recursive, but the recursion depth should be limited

    val element = createElementWithoutChildren(elm, doc, parentScope)
    val childNodes: immutable.IndexedSeq[org.w3c.dom.Node] = elm.children.map(ch => {
      convertNode(ch, doc, element, elm.scope)
    })

    for (ch <- childNodes) element.appendChild(ch)

    parent.appendChild(element)
    element
  }

  private def convertText(text: Text, doc: Document, parent: org.w3c.dom.Node): org.w3c.dom.Text = {
    val domText = doc.createTextNode(text.text)

    parent.appendChild(domText)
    domText
  }

  private def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction, doc: Document, parent: org.w3c.dom.Node): org.w3c.dom.ProcessingInstruction = {

    val domPi = doc.createProcessingInstruction(processingInstruction.target, processingInstruction.data)

    parent.appendChild(domPi)
    domPi
  }

  private def convertCData(cdata: CData, doc: Document, parent: org.w3c.dom.Node): org.w3c.dom.CDATASection = {
    val domCData = doc.createCDATASection(cdata.text)

    parent.appendChild(domCData)
    domCData
  }

  private def convertEntityRef(entityRef: EntityRef, doc: Document, parent: org.w3c.dom.Node): org.w3c.dom.EntityReference = {
    val domEntityRef = doc.createEntityReference(entityRef.entity)

    parent.appendChild(domEntityRef)
    domEntityRef
  }

  private def createElementWithoutChildren(elm: Elem, doc: Document, parentScope: Scope): Element = {
    val element =
      if (elm.resolvedName.namespaceUri.isEmpty) {
        doc.createElement(elm.qname.localPart)
      } else {
        doc.createElementNS(elm.resolvedName.namespaceUri.get, elm.qname.toString)
      }

    val namespaceDeclarations: Scope.Declarations = parentScope.relativize(elm.scope)

    for ((prefix, ns) <- namespaceDeclarations.declared.toMap) {
      if (prefix == "") {
        element.setAttribute("xmlns", ns)
      } else {
        element.setAttributeNS("http://www.w3.org/2000/xmlns/", "xmlns:%s".format(prefix), ns)
      }
    }

    for ((attrQName, attrValue) <- elm.attributes) {
      if (attrQName.prefixOption.isEmpty) {
        element.setAttribute(attrQName.localPart, attrValue)
      } else {
        val attrExpandedName = elm.attributeScope.resolveQName(attrQName).getOrElse(sys.error(
          "Attribute name '%s' should resolve to an ExpandedName in scope [%s]".format(attrQName, elm.attributeScope)))
        val attrJavaQName = attrExpandedName.toJavaQName(attrQName.prefixOption)
        element.setAttributeNS(attrJavaQName.getNamespaceURI, attrQName.toString, attrValue)
      }
    }

    element
  }
}

object ElemToDomConverter {

  type ElementProducer = (Document => Element)
}
