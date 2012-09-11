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

/**
 * [[eu.cdevreeze.yaidom.Document]] printer (to the XML as a `String`).
 *
 * Implementing classes deal with the details of printing yaidom documents as XML strings.
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

  /** Converts the `Document` to a `String`. May use a lot of memory for large XML documents. */
  def print(doc: Document): String

  /** Returns a copy of this `DocumentPrinter` that omits XML declarations */
  def omittingXmlDeclaration: DocumentPrinter

  /** Converts the `Elem` to a `String`, omitting the XML declaration */
  final def print(elm: Elem): String = {
    val printer = omittingXmlDeclaration
    printer.print(Document(elm))
  }
}
