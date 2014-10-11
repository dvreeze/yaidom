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

import java.io.OutputStream

import eu.cdevreeze.yaidom.defaultelem.Document
import eu.cdevreeze.yaidom.defaultelem.Elem

/**
 * [[eu.cdevreeze.yaidom.Document]] printer (to the XML as a `String` or byte array). This trait is purely abstract.
 *
 * Implementing classes deal with the details of printing yaidom documents as XML strings or byte arrays.
 * The [[eu.cdevreeze.yaidom]] package itself is agnostic of those details.
 *
 * Typical implementations use DOM or StAX, but make them easier to use in the tradition of the "template" classes
 * of the Spring framework. That is, resource management is done as much as possible by the `DocumentPrinter`,
 * typical usage is easy, and complex scenarios are still possible. The idea is that the document printer is configured once, and
 * that it should be re-usable multiple times.
 *
 * Although `DocumentPrinter` instances should be re-usable multiple times, implementing classes are encouraged to indicate
 * to what extent re-use of a `DocumentPrinter` instance is indeed supported (single-threaded, or even multi-threaded).
 *
 * @author Chris de Vreeze
 */
trait DocumentPrinter {

  /**
   * Serializes the `Document` to an output stream, using the given encoding.
   * This method should close the output stream afterwards.
   *
   * May use a lot of memory for large XML documents, although not as much as the `print` method that returns a byte array.
   */
  def print(doc: Document, encoding: String, outputStream: OutputStream): Unit

  /** Converts the `Document` to a byte array, using the given encoding. May use a lot of memory for large XML documents. */
  def print(doc: Document, encoding: String): Array[Byte]

  /**
   * Converts the `Document` to a `String`. May use a lot of memory for large XML documents.
   *
   * To have more control over the character encoding of the output, when converting the String to bytes
   * (and of the encoding mentioned in the XML declaration, if any), consider using one of the other overloaded
   * `print` methods taking a `Document`.
   */
  def print(doc: Document): String

  /** Returns a copy of this `DocumentPrinter` that omits XML declarations */
  def omittingXmlDeclaration: DocumentPrinter

  /** Converts the `Elem` to a byte array, omitting the XML declaration */
  def print(elm: Elem, encoding: String): Array[Byte]

  /**
   * Serializes the `Elem` to an output stream, omitting the XML declaration.
   * This method should close the output stream afterwards.
   */
  def print(elm: Elem, encoding: String, outputStream: OutputStream): Unit

  /**
   * Converts the `Elem` to a `String`, omitting the XML declaration. May use a lot of memory for large XML documents.
   *
   * To have more control over the character encoding of the output, when converting the String to bytes
   * (and of the encoding mentioned in the XML declaration, if any), consider using one of the other overloaded
   * `print` methods taking an `Elem`.
   */
  def print(elm: Elem): String
}
