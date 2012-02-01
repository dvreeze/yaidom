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
import javax.xml.transform.{ TransformerFactory, OutputKeys }
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.parsers.DocumentBuilderFactory
import scala.collection.immutable
import jinterop.DomConversions._

/** DOM-based Document printer. */
final class DocumentPrinterUsingDom(
  val documentBuilderFactory: DocumentBuilderFactory,
  val transformerFactory: TransformerFactory) extends DocumentPrinter {

  def printXml(doc: Document): String = {
    val docBuilder = documentBuilderFactory.newDocumentBuilder
    val domDocument: org.w3c.dom.Document = convertDocument(doc)(docBuilder.newDocument)

    val transformer = transformerFactory.newTransformer
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")

    val domSource = new DOMSource(domDocument)

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
