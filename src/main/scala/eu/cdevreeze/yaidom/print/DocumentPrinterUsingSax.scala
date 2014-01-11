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
import javax.xml.transform.{ TransformerFactory, URIResolver, OutputKeys }
import javax.xml.transform.sax.{ SAXTransformerFactory, TransformerHandler }
import javax.xml.transform.stream.StreamResult

/**
 * SAX-based `Document` printer. It should be the fastest of the `DocumentPrinter` implementations, and use the least memory.
 *
 * See http://blogger.ziesemer.com/2007/06/xml-generation-in-java.html#SAXMethod.
 *
 * If more flexibility is needed in configuring the `DocumentPrinter` than offered by this class, consider
 * writing a wrapper `DocumentPrinter` which wraps a `DocumentPrinterUsingSax`, but adapts the `print` method.
 * This would make it possible to adapt the generation of SAX events, for example.
 *
 * A `DocumentPrinterUsingSax` instance can be re-used multiple times, from the same thread.
 * If the `SAXTransformerFactory` is thread-safe, it can even be re-used from multiple threads.
 * Typically a `SAXTransformerFactory` cannot be trusted to be thread-safe, however. In a web application,
 * one (safe) way to deal with that is to use one `SAXTransformerFactory` instance per request.
 *
 * @author Chris de Vreeze
 */
final class DocumentPrinterUsingSax(
  val saxTransformerFactory: SAXTransformerFactory,
  val transformerHandlerCreator: SAXTransformerFactory => TransformerHandler) extends AbstractDocumentPrinter {

  def print(doc: Document, encoding: String, outputStream: jio.OutputStream): Unit = {
    val handler = transformerHandlerCreator(saxTransformerFactory)

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val streamResult = new StreamResult(outputStream)

    try {
      handler.getTransformer.setOutputProperty(OutputKeys.ENCODING, encoding)
      handler.setResult(streamResult)
      generateEventsForDocument(doc, handler)
    } finally {
      outputStream.close()
    }
  }

  def print(doc: Document): String = {
    val handler = transformerHandlerCreator(saxTransformerFactory)

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val sw = new jio.StringWriter
    val streamResult = new StreamResult(sw)

    handler.setResult(streamResult)
    generateEventsForDocument(doc, handler)

    val result = sw.toString
    result
  }

  def omittingXmlDeclaration: DocumentPrinterUsingSax = {
    val newTransformerHandlerCreator = { tf: SAXTransformerFactory =>
      val transformerHandler = transformerHandlerCreator(tf)
      transformerHandler.getTransformer().setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes")
      transformerHandler
    }

    new DocumentPrinterUsingSax(
      saxTransformerFactory,
      newTransformerHandlerCreator)
  }

  private def generateEventsForDocument(doc: Document, handler: TransformerHandler): Unit = {
    handler.startDocument()

    for (pi <- doc.processingInstructions) generateEventsForProcessingInstruction(pi, handler)
    for (comment <- doc.comments) generateEventsForComment(comment, handler)
    generateEventsForElem(doc.documentElement, Scope.Empty, handler)

    handler.endDocument()
  }

  private def generateEventsForElem(elm: Elem, parentScope: Scope, handler: TransformerHandler): Unit = {
    val namespaces: Declarations = parentScope.relativize(elm.scope)
    val namespacesMap = namespaces.prefixNamespaceMap

    for ((prefix, nsUri) <- namespacesMap) handler.startPrefixMapping(prefix, nsUri)

    generateStartElementEvent(elm, parentScope, handler)

    // Recursive calls. Not tail-recursive, but recursion depth should be limited.

    for (node <- elm.children) {
      generateEventsForNode(node, elm.scope, handler)
    }

    generateEndElementEvent(elm, parentScope, handler)

    for ((prefix, nsUri) <- namespacesMap) handler.endPrefixMapping(prefix)
  }

  private def generateEventsForNode(node: Node, parentScope: Scope, handler: TransformerHandler): Unit = {
    node match {
      case elm: Elem => generateEventsForElem(elm, parentScope, handler)
      case text: Text => generateEventsForText(text, handler)
      case er: EntityRef => () // What can we do?
      case pi: ProcessingInstruction => generateEventsForProcessingInstruction(pi, handler)
      case comment: Comment => generateEventsForComment(comment, handler)
    }
  }

  private def generateEventsForText(text: Text, handler: TransformerHandler): Unit = {
    if (text.isCData) handler.startCDATA()

    handler.characters(text.text.toCharArray, 0, text.text.length)

    if (text.isCData) handler.endCDATA()
  }

  private def generateEventsForProcessingInstruction(processingInstruction: ProcessingInstruction, handler: TransformerHandler): Unit = {
    handler.processingInstruction(processingInstruction.target, processingInstruction.data)
  }

  private def generateEventsForComment(comment: Comment, handler: TransformerHandler): Unit = {
    handler.comment(comment.text.toCharArray, 0, comment.text.length)
  }

  private def generateStartElementEvent(elm: Elem, parentScope: Scope, handler: TransformerHandler): Unit = {
    val uri = elm.resolvedName.namespaceUriOption.getOrElse("")
    val localName = elm.localName // Correct?
    val qname = elm.qname.toString

    val attrs: Attributes = getAttributesAndNamespaceDeclarations(elm, parentScope)

    handler.startElement(uri, localName, qname, attrs)
  }

  private def generateEndElementEvent(elm: Elem, parentScope: Scope, handler: TransformerHandler): Unit = {
    val uri = elm.resolvedName.namespaceUriOption.getOrElse("")
    val localName = elm.localName // Correct?
    val qname = elm.qname.toString

    handler.endElement(uri, localName, qname)
  }

  private def getAttributesAndNamespaceDeclarations(elm: Elem, parentScope: Scope): Attributes = {
    val attrs = new AttributesImpl

    // 1. Normal attributes

    val attrScope = elm.attributeScope

    for ((attQName, attValue) <- elm.attributes) {
      val attEName = attrScope.resolveQNameOption(attQName).getOrElse(sys.error(s"Corrupt non-resolvable attribute: $attQName"))
      val uri = attEName.namespaceUriOption.getOrElse("")
      val localName = attQName.localPart // Correct?
      val qname = attQName.toString
      val tpe = ""

      attrs.addAttribute(uri, localName, qname, tpe, attValue)
    }

    // 2. Namespace declarations and undeclarations

    val namespaces: Declarations = parentScope.relativize(elm.scope)
    val namespacesMap = namespaces.prefixNamespaceMap

    val tpe = ""

    for ((prefix, nsUri) <- namespacesMap) {
      if (prefix == "") {
        attrs.addAttribute("", "xmlns", "xmlns", tpe, nsUri)
      } else {
        val qname = s"xmlns:$prefix"
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
    assert(tf.getFeature(SAXTransformerFactory.FEATURE), s"The TransformerFactory ${tf.getClass} is not a SAXTransformerFactory")
    val stf = tf.asInstanceOf[SAXTransformerFactory]

    newInstance(stf)
  }

  /** Invokes the 2-arg `newInstance` method, with trivial "transformerHandlerCreator" */
  def newInstance(saxTransformerFactory: SAXTransformerFactory): DocumentPrinterUsingSax =
    newInstance(
      saxTransformerFactory,
      { tf => tf.newTransformerHandler() })

  /** Returns a new instance, by invoking the primary constructor */
  def newInstance(
    saxTransformerFactory: SAXTransformerFactory,
    transformerHandlerCreator: SAXTransformerFactory => TransformerHandler): DocumentPrinterUsingSax = {

    new DocumentPrinterUsingSax(saxTransformerFactory, transformerHandlerCreator)
  }
}
