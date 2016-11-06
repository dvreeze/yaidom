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

package eu.cdevreeze.yaidom.convert

import scala.collection.JavaConverters.bufferAsJavaListConverter

import org.xml.sax.Attributes
import org.xml.sax.ContentHandler
import org.xml.sax.ext.LexicalHandler
import org.xml.sax.helpers.AttributesImpl

import eu.cdevreeze.yaidom.core.Declarations
import eu.cdevreeze.yaidom.core.Scope
import eu.cdevreeze.yaidom.simple.Comment
import eu.cdevreeze.yaidom.simple.Document
import eu.cdevreeze.yaidom.simple.DocumentConverter
import eu.cdevreeze.yaidom.simple.Elem
import eu.cdevreeze.yaidom.simple.ElemConverter
import eu.cdevreeze.yaidom.simple.EntityRef
import eu.cdevreeze.yaidom.simple.Node
import eu.cdevreeze.yaidom.simple.ProcessingInstruction
import eu.cdevreeze.yaidom.simple.Text
import YaidomToSaxEventsConversions.SaxEventsProducer

/**
 * Converter from yaidom nodes to SAX event producers, in particular from [[eu.cdevreeze.yaidom.simple.Elem]] to `SaxEventsProducer`,
 * and from  [[eu.cdevreeze.yaidom.simple.Document]] to `SaxEventsProducer`.
 *
 * @author Chris de Vreeze
 */
trait YaidomToSaxEventsConversions extends ElemConverter[SaxEventsProducer] with DocumentConverter[SaxEventsProducer] {

  /** Converts a yaidom `Document` to a `SaxEventsProducer` */
  final def convertDocument(doc: Document): SaxEventsProducer = {
    { (handler: ContentHandler) =>
      handler.startDocument()

      doc.children foreach { ch => convertNode(ch, Scope.Empty)(handler) }

      handler.endDocument()
    }
  }

  /**
   * Converts a yaidom `Elem` to a `SaxEventsProducer`.
   * The assumed parent scope is the empty scope.
   */
  final def convertElem(elm: Elem): SaxEventsProducer = {
    convertElem(elm, Scope.Empty)
  }

  /**
   * Converts a yaidom node to a `SaxEventsProducer`.
   * The given parent scope is used, in case the node is an `Elem`.
   */
  final def convertNode(node: Node, parentScope: Scope): SaxEventsProducer = {
    node match {
      case e: Elem                   => convertElem(e, parentScope)
      case t: Text                   => convertText(t)
      case pi: ProcessingInstruction => convertProcessingInstruction(pi)
      // Difficult to convert yaidom EntityRef to SAX event producer, because of missing declaration
      case er: EntityRef             => (handler => ())
      case c: Comment                => convertComment(c)
    }
  }

  /**
   * Converts a yaidom `Elem` to a `SaxEventsProducer`.
   * The given parent scope is used, that is, the prefix mappings before the outer "start element event" correspond to
   * `parentScope.relativize(elm.scope)`.
   */
  final def convertElem(elm: Elem, parentScope: Scope): SaxEventsProducer = {
    { (handler: ContentHandler) =>
      // Not tail-recursive, but the recursion depth should be limited

      val namespaces: Declarations = parentScope.relativize(elm.scope)
      val namespacesMap = namespaces.prefixNamespaceMap

      for ((prefix, nsUri) <- namespacesMap) handler.startPrefixMapping(prefix, nsUri)

      generateStartElementEvent(elm, handler, parentScope)

      // Recursive calls. Not tail-recursive, but recursion depth should be limited.

      for (node <- elm.children) {
        convertNode(node, elm.scope)(handler)
      }

      generateEndElementEvent(elm, handler, parentScope)

      for ((prefix, nsUri) <- namespacesMap) handler.endPrefixMapping(prefix)
    }
  }

  /**
   * Converts a yaidom `Text` to a `SaxEventsProducer`.
   */
  final def convertText(text: Text): SaxEventsProducer = {
    { (handler: ContentHandler) =>
      handler match {
        case handler: ContentHandler with LexicalHandler =>
          if (text.isCData) handler.startCDATA()
          handler.characters(text.text.toCharArray, 0, text.text.length)
          if (text.isCData) handler.endCDATA()
        case _ =>
          handler.characters(text.text.toCharArray, 0, text.text.length)
      }
    }
  }

  /**
   * Converts a yaidom `ProcessingInstruction` to a `SaxEventsProducer`.
   */
  final def convertProcessingInstruction(
    processingInstruction: ProcessingInstruction): SaxEventsProducer = {
    { (handler: ContentHandler) =>
      handler.processingInstruction(processingInstruction.target, processingInstruction.data)
    }
  }

  /**
   * Converts a yaidom `Comment` to a `SaxEventsProducer`.
   */
  final def convertComment(comment: Comment): SaxEventsProducer = {
    { (handler: ContentHandler) =>
      handler match {
        case handler: ContentHandler with LexicalHandler =>
          handler.comment(comment.text.toCharArray, 0, comment.text.length)
        case _ => ()
      }
    }
  }

  private def generateStartElementEvent(elm: Elem, handler: ContentHandler, parentScope: Scope): Unit = {
    val uri = elm.resolvedName.namespaceUriOption.getOrElse("")

    val attrs: Attributes = getAttributes(elm)

    handler.startElement(uri, elm.localName, elm.qname.toString, attrs)
  }

  private def generateEndElementEvent(elm: Elem, handler: ContentHandler, parentScope: Scope): Unit = {
    val uri = elm.resolvedName.namespaceUriOption.getOrElse("")

    handler.endElement(uri, elm.localName, elm.qname.toString)
  }

  private def getAttributes(elm: Elem): Attributes = {
    val attrs = new AttributesImpl

    addNormalAttributes(elm, attrs)
    attrs
  }

  /**
   * Gets the normal (non-namespace-declaration) attributes, and adds them to the passed Attributes object.
   * This method is called internally, providing the attributes that are passed to the startElement call.
   */
  final def addNormalAttributes(elm: Elem, attrs: AttributesImpl): Attributes = {
    val attrScope = elm.attributeScope

    for ((attrQName, attrValue) <- elm.attributes) {
      val attrEName = attrScope.resolveQNameOption(attrQName).getOrElse(sys.error(s"Corrupt non-resolvable attribute: $attrQName"))
      val uri = attrEName.namespaceUriOption.getOrElse("")
      val tpe = "CDATA"

      attrs.addAttribute(uri, attrQName.localPart, attrQName.toString, tpe, attrValue)
    }

    attrs
  }

  /**
   * Gets the namespace-declaration attributes, and adds them to the passed Attributes object.
   * This method is not called internally.
   */
  final def addNamespaceDeclarationAttributes(elm: Elem, parentScope: Scope, attrs: AttributesImpl): Attributes = {
    val namespaces: Declarations = parentScope.relativize(elm.scope)
    val namespacesMap = namespaces.prefixNamespaceMap

    val tpe = "CDATA"
    val xmlNs = "http://www.w3.org/XML/1998/namespace"

    for ((prefix, nsUri) <- namespacesMap) {
      if (prefix == "") {
        attrs.addAttribute(xmlNs, "xmlns", "xmlns", tpe, nsUri)
      } else {
        val qname = s"xmlns:$prefix"
        attrs.addAttribute(xmlNs, prefix, qname, tpe, nsUri)
      }
    }

    attrs
  }
}

object YaidomToSaxEventsConversions {

  /** Producer of SAX events, given a `ContentHandler` on which the SAX event handlers are invoked */
  type SaxEventsProducer = (ContentHandler => Unit)
}
