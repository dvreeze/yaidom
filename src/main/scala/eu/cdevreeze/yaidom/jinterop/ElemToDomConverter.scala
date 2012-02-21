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
import org.w3c.dom.{ Element }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import eu.cdevreeze.yaidom
import ElemToDomConverter._

/**
 * Converter from [[eu.cdevreeze.yaidom.Elem]] to a DOM element, and from [[eu.cdevreeze.yaidom.Document]] to a DOM Document.
 *
 * @author Chris de Vreeze
 */
trait ElemToDomConverter extends ElemConverter[ElementProducer] with DocumentConverter[DocumentProducer] {

  def convertDocument(document: yaidom.Document): DocumentProducer = {
    { (doc: org.w3c.dom.Document) =>
      // This also sets the document element on the document
      val docRoot: Element = convertElem(document.documentElement, doc, doc, Scope.Empty)
      // This also sets the PIs on the document
      val pis: immutable.IndexedSeq[org.w3c.dom.ProcessingInstruction] =
        document.processingInstructions map { pi => convertProcessingInstruction(pi, doc, doc) }
      // This also sets the comments on the document
      val comments: immutable.IndexedSeq[org.w3c.dom.Comment] =
        document.comments map { com => convertComment(com, doc, doc) }

      if (document.baseUriOption.isDefined) doc.setDocumentURI(document.baseUriOption.get.toString)

      doc
    }
  }

  def convertElem(elm: Elem): ElementProducer = {
    { (doc: org.w3c.dom.Document) =>
      val element = convertElem(elm, doc, doc, Scope.Empty)
      element
    }
  }

  private def convertNode(node: Node, doc: org.w3c.dom.Document, parent: org.w3c.dom.Node, parentScope: Scope): org.w3c.dom.Node = {
    node match {
      case e: Elem => convertElem(e, doc, parent, parentScope)
      case t: Text if t.isCData => convertCData(t, doc, parent)
      case t: Text => require(!t.isCData); convertText(t, doc, parent)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi, doc, parent)
      case er: EntityRef => convertEntityRef(er, doc, parent)
      case c: Comment => convertComment(c, doc, parent)
    }
  }

  private def convertElem(elm: Elem, doc: org.w3c.dom.Document, parent: org.w3c.dom.Node, parentScope: Scope): Element = {
    // Not tail-recursive, but the recursion depth should be limited

    val element = createElementWithoutChildren(elm, doc, parentScope)
    val childNodes: immutable.IndexedSeq[org.w3c.dom.Node] =
      elm.children map { ch => convertNode(ch, doc, element, elm.scope) }

    for (ch <- childNodes) element.appendChild(ch)

    parent.appendChild(element)
    element
  }

  private def convertCData(cdata: Text, doc: org.w3c.dom.Document, parent: org.w3c.dom.Node): org.w3c.dom.CDATASection = {
    val domCData = doc.createCDATASection(cdata.text)

    parent.appendChild(domCData)
    domCData
  }

  private def convertText(text: Text, doc: org.w3c.dom.Document, parent: org.w3c.dom.Node): org.w3c.dom.Text = {
    val domText = doc.createTextNode(text.text)

    parent.appendChild(domText)
    domText
  }

  private def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction, doc: org.w3c.dom.Document, parent: org.w3c.dom.Node): org.w3c.dom.ProcessingInstruction = {

    val domPi = doc.createProcessingInstruction(processingInstruction.target, processingInstruction.data)

    parent.appendChild(domPi)
    domPi
  }

  private def convertEntityRef(entityRef: EntityRef, doc: org.w3c.dom.Document, parent: org.w3c.dom.Node): org.w3c.dom.EntityReference = {
    val domEntityRef = doc.createEntityReference(entityRef.entity)

    parent.appendChild(domEntityRef)
    domEntityRef
  }

  private def convertComment(comment: Comment, doc: org.w3c.dom.Document, parent: org.w3c.dom.Node): org.w3c.dom.Comment = {
    val domComment = doc.createComment(comment.text)

    parent.appendChild(domComment)
    domComment
  }

  private def createElementWithoutChildren(elm: Elem, doc: org.w3c.dom.Document, parentScope: Scope): Element = {
    val element =
      if (elm.resolvedName.namespaceUriOption.isEmpty) {
        doc.createElement(elm.qname.localPart)
      } else {
        doc.createElementNS(elm.resolvedName.namespaceUriOption.get, elm.qname.toString)
      }

    val namespaceDeclarations: Scope.Declarations = parentScope.relativize(elm.scope)

    for ((prefix, ns) <- namespaceDeclarations.toMap) {
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

  type ElementProducer = (org.w3c.dom.Document => Element)

  type DocumentProducer = (org.w3c.dom.Document => org.w3c.dom.Document)
}
