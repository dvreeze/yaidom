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

import java.{ util => jutil, io => jio }
import javax.xml.transform.{ TransformerFactory, Transformer, OutputKeys }
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.parsers.{ DocumentBuilderFactory, DocumentBuilder }
import scala.collection.immutable
import jinterop.DomConversions._

/**
 * DOM-based Document printer.
 *
 * It may be the case that the DocumentPrinter does not indent. See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446.
 * This problem appears especially when creating a Document from scratch, using NodeBuilders.
 * A possible (implementation-specific) workaround is to create the DocumentPrinter as follows:
 * {{{
 * val documentBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
 *
 * val transformerFactory: TransformerFactory = TransformerFactory.newInstance
 * transformerFactory.setAttribute("indent-number", java.lang.Integer.valueOf(2))
 *
 * val documentPrinter = new DocumentPrinterUsingDom(documentBuilderFactory, transformerFactory)
 * }}}
 */
final class DocumentPrinterUsingDom(
  val documentBuilderFactory: DocumentBuilderFactory,
  val documentBuilderCreator: DocumentBuilderFactory => DocumentBuilder,
  val transformerFactory: TransformerFactory,
  val transformerCreator: TransformerFactory => Transformer) extends DocumentPrinter {

  def this(dbf: DocumentBuilderFactory, tf: TransformerFactory) = this(
    documentBuilderFactory = dbf,
    documentBuilderCreator = { docBuilderFactory => docBuilderFactory.newDocumentBuilder() },
    transformerFactory = tf,
    transformerCreator = { transFactory =>
      val t = transFactory.newTransformer()
      t.setOutputProperty(OutputKeys.INDENT, "yes")
      t
    })

  def print(doc: Document): String = {
    val docBuilder = documentBuilderCreator(documentBuilderFactory)
    val domDocument: org.w3c.dom.Document = convertDocument(doc)(docBuilder.newDocument)

    val transformer = transformerCreator(transformerFactory)

    val domSource = new DOMSource(domDocument)

    // See bug http://bugs.sun.com/bugdatabase/view_bug.do?bug_id=6296446
    val sw = new jio.StringWriter
    val streamResult = new StreamResult(sw)

    transformer.transform(domSource, streamResult)

    val result = sw.toString
    result
  }
}

object DocumentPrinterUsingDom {

  def newInstance(): DocumentPrinterUsingDom = {
    val documentBuilderFactory: DocumentBuilderFactory = DocumentBuilderFactory.newInstance
    val transformerFactory: TransformerFactory = TransformerFactory.newInstance
    new DocumentPrinterUsingDom(documentBuilderFactory, transformerFactory)
  }
}
