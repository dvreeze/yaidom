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

package eu.cdevreeze.yaidom
package print

import java.{ io => jio }

/**
 * Thread-local DocumentPrinter. This class exists because typical JAXP factory objects (DocumentBuilderFactory etc.) are
 * not thread-safe, but still expensive to create. Using this DocumentPrinter facade backed by a thread local DocumentPrinter,
 * we can create a ThreadLocalDocumentPrinter once, and re-use it all the time without having to worry about thread-safety
 * issues.
 *
 * Note that each ThreadLocalDocumentPrinter instance (!) has its own thread-local document printer. Typically it makes no sense
 * to have more than one ThreadLocalDocumentPrinter instance in one application. In a Spring application, for example, a single
 * instance of a ThreadLocalDocumentPrinter can be configured.
 *
 * @author Chris de Vreeze
 */
final class ThreadLocalDocumentPrinter(val docPrinterCreator: () => DocumentPrinter) extends AbstractDocumentPrinter {

  private val threadLocalDocPrinter: ThreadLocal[DocumentPrinter] = new ThreadLocal[DocumentPrinter] {

    protected override def initialValue(): DocumentPrinter = docPrinterCreator()
  }

  private val threadLocalDocPrinterOmittingXmlDeclaration: ThreadLocal[DocumentPrinter] = new ThreadLocal[DocumentPrinter] {

    protected override def initialValue(): DocumentPrinter = docPrinterCreator().omittingXmlDeclaration
  }

  /**
   * Returns the DocumentPrinter instance attached to the current thread.
   */
  def documentPrinterOfCurrentThread: DocumentPrinter = threadLocalDocPrinter.get

  /**
   * Serializes the `Document` to an output stream, using the given encoding, and using the DocumentPrinter attached to the current thread.
   */
  def print(doc: Document, encoding: String, outputStream: jio.OutputStream): Unit =
    documentPrinterOfCurrentThread.print(doc, encoding, outputStream)

  /**
   * Converts the `Document` to a `String`, using the DocumentPrinter attached to the current thread.
   */
  def print(doc: Document): String =
    documentPrinterOfCurrentThread.print(doc)

  /**
   * Returns a new ThreadLocalDocumentPrinter instance, but omitting the XML declaration.
   */
  def omittingXmlDeclaration: DocumentPrinter =
    new ThreadLocalDocumentPrinter({ () => docPrinterCreator().omittingXmlDeclaration })
}
