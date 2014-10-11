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

package eu.cdevreeze.yaidom.print

import java.io.ByteArrayOutputStream
import java.io.OutputStream

import eu.cdevreeze.yaidom.defaultelem.Document
import eu.cdevreeze.yaidom.defaultelem.Elem

/**
 * Partial `DocumentPrinter` implementation.
 *
 * @author Chris de Vreeze
 */
abstract class AbstractDocumentPrinter extends DocumentPrinter {

  /** Converts the `Document` to a byte array, using the given encoding. May use a lot of memory for large XML documents. */
  final def print(doc: Document, encoding: String): Array[Byte] = {
    val bos = new ByteArrayOutputStream
    print(doc, encoding, bos)
    bos.toByteArray
  }

  /** Converts the `Elem` to a byte array, omitting the XML declaration */
  final def print(elm: Elem, encoding: String): Array[Byte] = {
    val printer = omittingXmlDeclaration
    printer.print(Document(elm), encoding)
  }

  /**
   * Serializes the `Elem` to an output stream, omitting the XML declaration.
   * This method should close the output stream afterwards.
   */
  final def print(elm: Elem, encoding: String, outputStream: OutputStream): Unit = {
    val printer = omittingXmlDeclaration
    printer.print(Document(elm), encoding, outputStream)
  }

  /**
   * Converts the `Elem` to a `String`, omitting the XML declaration. May use a lot of memory for large XML documents.
   *
   * To have more control over the character encoding of the output, when converting the String to bytes
   * (and of the encoding mentioned in the XML declaration, if any), consider using one of the other overloaded
   * `print` methods taking an `Elem`.
   */
  final def print(elm: Elem): String = {
    val printer = omittingXmlDeclaration
    printer.print(Document(elm))
  }
}
