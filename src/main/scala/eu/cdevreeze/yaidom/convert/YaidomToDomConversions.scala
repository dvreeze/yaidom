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
package convert

import java.{ util => jutil }
import javax.xml.XMLConstants
import org.w3c.dom.{ Element }
import scala.collection.JavaConverters._
import scala.collection.{ immutable, mutable }
import eu.cdevreeze.yaidom
import YaidomToDomConversions._

/**
 * Converter from yaidom nodes to DOM nodes, in particular from [[eu.cdevreeze.yaidom.Elem]] to a `org.w3c.dom.Element`,
 * and from  [[eu.cdevreeze.yaidom.Document]] to a `org.w3c.dom.Document`.
 *
 * @author Chris de Vreeze
 */
trait YaidomToDomConversions extends ElemConverter[ElementProducer] with DocumentConverter[DocumentProducer] {

  /** Converts a yaidom `Document` to a function from DOM documents (as node factories) to (filled) DOM documents */
  final def convertDocument(document: yaidom.Document): DocumentProducer = {
    { (doc: org.w3c.dom.Document) =>
      val docRoot: Element = convertElem(document.documentElement, doc, Scope.Empty)
      doc.appendChild(docRoot)

      val pis: immutable.IndexedSeq[org.w3c.dom.ProcessingInstruction] =
        document.processingInstructions map { pi => convertProcessingInstruction(pi, doc) }
      for (pi <- pis) doc.appendChild(pi)

      val comments: immutable.IndexedSeq[org.w3c.dom.Comment] =
        document.comments map { com => convertComment(com, doc) }
      for (c <- comments) doc.appendChild(c)

      if (document.baseUriOption.isDefined) doc.setDocumentURI(document.baseUriOption.get.toString)

      doc
    }
  }

  /** Same as `{ doc => convertElem(elm, doc, Scope.Empty) }` */
  final def convertElem(elm: Elem): ElementProducer = {
    { (doc: org.w3c.dom.Document) =>
      val element = convertElem(elm, doc, Scope.Empty)
      element
    }
  }

  /**
   * Converts a yaidom node to a DOM node. A DOM document is passed as node factory. If the node is an element,
   * the passed parent scope is used as in `convertElem(e, doc, parentScope)`.
   */
  final def convertNode(node: Node, doc: org.w3c.dom.Document, parentScope: Scope): org.w3c.dom.Node = {
    node match {
      case e: Elem => convertElem(e, doc, parentScope)
      case t: Text => convertText(t, doc)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi, doc)
      case er: EntityRef => convertEntityRef(er, doc)
      case c: Comment => convertComment(c, doc)
    }
  }

  /**
   * Converts a yaidom `Elem` to a DOM element. A DOM document is passed as node factory.
   * The passed parent scope is used as follows: the namespace declarations on the result DOM element are:
   * `parentScope.relativize(elm.scope)`.
   */
  final def convertElem(elm: Elem, doc: org.w3c.dom.Document, parentScope: Scope): Element = {
    // Not tail-recursive, but the recursion depth should be limited

    val element = createElementWithoutChildren(elm, doc, parentScope)
    val childNodes: immutable.IndexedSeq[org.w3c.dom.Node] =
      elm.children map { ch => convertNode(ch, doc, elm.scope) }

    for (ch <- childNodes) element.appendChild(ch)

    element
  }

  /**
   * Converts a yaidom `Text` to a DOM `Text`. A DOM document is passed as node factory.
   */
  final def convertText(text: Text, doc: org.w3c.dom.Document): org.w3c.dom.Text = {
    val domText =
      if (text.isCData) {
        doc.createCDATASection(text.text)
      } else {
        doc.createTextNode(text.text)
      }

    domText
  }

  /**
   * Converts a yaidom `ProcessingInstruction` to a DOM `ProcessingInstruction`. A DOM document is passed as node factory.
   */
  final def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction, doc: org.w3c.dom.Document): org.w3c.dom.ProcessingInstruction = {

    val domPi = doc.createProcessingInstruction(processingInstruction.target, processingInstruction.data)
    domPi
  }

  /**
   * Converts a yaidom `EntityRef` to a DOM `EntityReference`. A DOM document is passed as node factory.
   */
  final def convertEntityRef(entityRef: EntityRef, doc: org.w3c.dom.Document): org.w3c.dom.EntityReference = {
    val domEntityRef = doc.createEntityReference(entityRef.entity)
    domEntityRef
  }

  /**
   * Converts a yaidom `Comment` to a DOM `Comment`. A DOM document is passed as node factory.
   */
  final def convertComment(comment: Comment, doc: org.w3c.dom.Document): org.w3c.dom.Comment = {
    val domComment = doc.createComment(comment.text)
    domComment
  }

  private def createElementWithoutChildren(elm: Elem, doc: org.w3c.dom.Document, parentScope: Scope): Element = {
    val element =
      if (elm.resolvedName.namespaceUriOption.isEmpty) {
        doc.createElement(elm.localName)
      } else {
        doc.createElementNS(elm.resolvedName.namespaceUriOption.get, elm.qname.toString)
      }

    val namespaceDeclarations: Declarations = parentScope.relativize(elm.scope)

    for ((prefix, ns) <- namespaceDeclarations.map) {
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
        val attrEName = elm.attributeScope.resolveQName(attrQName).getOrElse(sys.error(
          "Attribute name '%s' should resolve to an EName in scope [%s]".format(attrQName, elm.attributeScope)))
        val attrJavaQName = attrEName.toJavaQName(attrQName.prefixOption)
        element.setAttributeNS(attrJavaQName.getNamespaceURI, attrQName.toString, attrValue)
      }
    }

    element
  }
}

object YaidomToDomConversions {

  /** Producer of a DOM `Element`, given a DOM `Document` as factory of DOM objects */
  type ElementProducer = (org.w3c.dom.Document => Element)

  /** Producer of a DOM `Document`, given the DOM `Document` as factory of DOM objects */
  type DocumentProducer = (org.w3c.dom.Document => org.w3c.dom.Document)
}
