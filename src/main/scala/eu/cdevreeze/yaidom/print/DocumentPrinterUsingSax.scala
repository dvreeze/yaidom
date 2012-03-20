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
package print

import java.{ io => jio, util => jutil }
import org.xml.sax.{ Attributes, XMLReader }
import org.xml.sax.helpers.AttributesImpl
import javax.xml.transform.{ TransformerFactory, URIResolver }
import javax.xml.transform.sax.{ SAXTransformerFactory, TransformerHandler }
import javax.xml.transform.stream.StreamResult

/**
 * SAX-based `Document` printer. It should be the fastest of the `DocumentPrinter` implementations, and use the least memory.
 *
 * See http://blogger.ziesemer.com/2007/06/xml-generation-in-java.html#SAXMethod.
 *
 * A `DocumentPrinterUsingSax` instance can be re-used multiple times, from the same thread.
 * If the `SAXTransformerFactory` is thread-safe, it can even be re-used from multiple threads.
 */
final class DocumentPrinterUsingSax(
  val saxTransformerFactory: SAXTransformerFactory) extends DocumentPrinter {

  def print(doc: Document): String = {
    val handler = saxTransformerFactory.newTransformerHandler()

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val sw = new jio.StringWriter
    val streamResult = new StreamResult(sw)

    handler.setResult(streamResult)
    generateEventsForDocument(doc, handler)

    val result = sw.toString
    result
  }

  private def generateEventsForDocument(doc: Document, handler: TransformerHandler) {
    handler.startDocument()

    for (pi <- doc.processingInstructions) generateEventsForProcessingInstruction(pi, handler)
    for (comment <- doc.comments) generateEventsForComment(comment, handler)
    generateEventsForElem(doc.documentElement, Scope.Empty, handler)

    handler.endDocument()
  }

  private def generateEventsForElem(elm: Elem, parentScope: Scope, handler: TransformerHandler) {
    val namespaces: Scope.Declarations = parentScope.relativize(elm.scope)
    val namespacesMap = namespaces.toMap

    for ((prefix, nsUri) <- namespacesMap) handler.startPrefixMapping(prefix, nsUri)

    generateStartElementEvent(elm, parentScope, handler)

    // Recursive calls. Not tail-recursive, but recursion depth should be limited.

    for (node <- elm.children) {
      generateEventsForNode(node, elm.scope, handler)
    }

    generateEndElementEvent(elm, parentScope, handler)

    for ((prefix, nsUri) <- namespacesMap) handler.endPrefixMapping(prefix)
  }

  private def generateEventsForNode(node: Node, parentScope: Scope, handler: TransformerHandler) {
    node match {
      case elm: Elem => generateEventsForElem(elm, parentScope, handler)
      case text: Text => generateEventsForText(text, handler)
      case er: EntityRef => () // What can we do?
      case pi: ProcessingInstruction => generateEventsForProcessingInstruction(pi, handler)
      case comment: Comment => generateEventsForComment(comment, handler)
    }
  }

  private def generateEventsForText(text: Text, handler: TransformerHandler) {
    if (text.isCData) handler.startCDATA()

    handler.characters(text.text.toCharArray, 0, text.text.length)

    if (text.isCData) handler.endCDATA()
  }

  private def generateEventsForProcessingInstruction(processingInstruction: ProcessingInstruction, handler: TransformerHandler) {
    handler.processingInstruction(processingInstruction.target, processingInstruction.data)
  }

  private def generateEventsForComment(comment: Comment, handler: TransformerHandler) {
    handler.comment(comment.text.toCharArray, 0, comment.text.length)
  }

  private def generateStartElementEvent(elm: Elem, parentScope: Scope, handler: TransformerHandler) {
    val uri = elm.resolvedName.namespaceUriOption.getOrElse("")
    val localName = elm.qname.localPart // Correct?
    val qname = elm.qname.toString

    val attrs: Attributes = getAttributesAndNamespaceDeclarations(elm, parentScope)

    handler.startElement(uri, localName, qname, attrs)
  }

  private def generateEndElementEvent(elm: Elem, parentScope: Scope, handler: TransformerHandler) {
    val uri = elm.resolvedName.namespaceUriOption.getOrElse("")
    val localName = elm.qname.localPart // Correct?
    val qname = elm.qname.toString

    handler.endElement(uri, localName, qname)
  }

  private def getAttributesAndNamespaceDeclarations(elm: Elem, parentScope: Scope): Attributes = {
    val attrs = new AttributesImpl

    // 1. Normal attributes

    for ((attQName, attValue) <- elm.attributes) {
      val attEName = elm.attributeScope.resolveQName(attQName).getOrElse(sys.error("Corrupt non-resolvable attribute: %s".format(attQName)))
      val uri = attEName.namespaceUriOption.getOrElse("")
      val localName = attQName.localPart // Correct?
      val qname = attQName.toString
      val tpe = ""

      attrs.addAttribute(uri, localName, qname, tpe, attValue)
    }

    // 2. Namespace declarations and undeclarations

    val namespaces: Scope.Declarations = parentScope.relativize(elm.scope)
    val namespacesMap = namespaces.toMap

    val tpe = ""

    for ((prefix, nsUri) <- namespacesMap) {
      if (prefix == "") {
        attrs.addAttribute("", "xmlns", "xmlns", tpe, nsUri)
      } else {
        val qname = "xmlns:%s".format(prefix)
        attrs.addAttribute("", prefix, qname, tpe, nsUri)
      }
    }

    attrs
  }
}

object DocumentPrinterUsingSax {

  /** Returns `newInstance(TransformerFactory.newInstance().asInstanceOf[SAXTransformerFactory])` */
  def newInstance(): DocumentPrinterUsingSax = {
    val tf = TransformerFactory.newInstance()
    require(tf.getFeature(SAXTransformerFactory.FEATURE), "The TransformerFactory %s is not a SAXTransformerFactory".format(tf.getClass))
    val stf = tf.asInstanceOf[SAXTransformerFactory]

    newInstance(stf)
  }

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(saxTransformerFactory: SAXTransformerFactory): DocumentPrinterUsingSax =
    new DocumentPrinterUsingSax(saxTransformerFactory)
}
