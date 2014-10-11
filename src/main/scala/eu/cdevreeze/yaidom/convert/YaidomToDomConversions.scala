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

package eu.cdevreeze.yaidom.convert

import scala.collection.immutable

import org.w3c.dom.Element

import eu.cdevreeze.yaidom
import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.defaultelem
import eu.cdevreeze.yaidom.defaultelem.Comment
import eu.cdevreeze.yaidom.defaultelem.DocumentConverter
import eu.cdevreeze.yaidom.defaultelem.Elem
import eu.cdevreeze.yaidom.defaultelem.ElemConverter
import eu.cdevreeze.yaidom.defaultelem.EntityRef
import eu.cdevreeze.yaidom.defaultelem.Node
import eu.cdevreeze.yaidom.defaultelem.ProcessingInstruction
import eu.cdevreeze.yaidom.defaultelem.Text
import YaidomToDomConversions.DocumentProducer
import YaidomToDomConversions.ElementProducer

/**
 * Converter from yaidom nodes to DOM nodes, in particular from [[eu.cdevreeze.yaidom.Elem]] to a `org.w3c.dom.Element`,
 * and from  [[eu.cdevreeze.yaidom.Document]] to a `org.w3c.dom.Document`.
 *
 * @author Chris de Vreeze
 */
trait YaidomToDomConversions extends ElemConverter[ElementProducer] with DocumentConverter[DocumentProducer] {

  /** Converts a yaidom `Document` to a function from DOM documents (as node factories) to (filled) DOM documents */
  final def convertDocument(document: defaultelem.Document): DocumentProducer = {
    { (doc: org.w3c.dom.Document) =>
      val pis: immutable.IndexedSeq[org.w3c.dom.ProcessingInstruction] =
        document.processingInstructions map { pi => convertProcessingInstruction(pi, doc) }
      for (pi <- pis) doc.appendChild(pi)

      val comments: immutable.IndexedSeq[org.w3c.dom.Comment] =
        document.comments map { com => convertComment(com, doc) }
      for (c <- comments) doc.appendChild(c)

      val docRoot: Element = convertElem(document.documentElement, doc, Scope.Empty)
      doc.appendChild(docRoot)

      if (document.uriOption.isDefined) doc.setDocumentURI(document.uriOption.get.toString)

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

    for ((prefix, ns) <- namespaceDeclarations.prefixNamespaceMap) {
      if (prefix == "") {
        element.setAttribute("xmlns", ns)
      } else {
        element.setAttributeNS("http://www.w3.org/2000/xmlns/", s"xmlns:$prefix", ns)
      }
    }

    val attrScope = elm.attributeScope

    for ((attrQName, attrValue) <- elm.attributes) {
      if (attrQName.prefixOption.isEmpty) {
        element.setAttribute(attrQName.localPart, attrValue)
      } else {
        val attrEName = attrScope.resolveQNameOption(attrQName).getOrElse(sys.error(
          s"Attribute name '${attrQName}' should resolve to an EName in scope [${attrScope}]"))
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
