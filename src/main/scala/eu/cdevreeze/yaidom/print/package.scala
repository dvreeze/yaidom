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

/**
 * Support for "printing" yaidom `Document`s and `Elem`s. This package offers the [[eu.cdevreeze.yaidom.print.DocumentPrinter]]
 * trait, as well as several implementations. Most of those implementations use the `convert` package to convert yaidom `Document`s to
 * JAXP artifacts, and all use JAXP (DOM, SAX or StAX).
 *
 * For example:
 * {{{
 * val docPrinter = DocumentPrinterUsingSax.newInstance()
 *
 * docPrinter.print(doc, "UTF-8", System.out)
 * }}}
 * This example chose a SAX-based implementation, and used the default configuration of that document printer.
 *
 * Having several different fully configurable JAXP-based implementations shows that yaidom is pessimistic about the transparency of parsing and
 * printing XML. It also shows that yaidom is optimistic about the available (heap) memory and processing power, because of the 2 separated
 * steps of JAXP parsing/printing and (in-memory) `convert` conversions. Using JAXP means that escaping of characters is something
 * that JAXP deals with, and that's definitely better than trying to do it yourself.
 *
 * One `DocumentPrinter` implementation does not use any `convert` conversion. That is `DocumentPrinterUsingSax`. It is likely the
 * fastest of the `DocumentPrinter` implementations, as well as the one using the least memory.
 *
 * The preferred `DocumentPrinter` for XML (not HTML) printing is `DocumentPrinterUsingDomLS`, if memory usage is not an
 * issue. This `DocumentPrinter` implementation is best integrated with DOM, and is highly configurable, although
 * DOM LS configuration is somewhat involved.
 *
 * This package depends on the [[eu.cdevreeze.yaidom]] and [[eu.cdevreeze.yaidom.convert]] packages, and not the other way around.
 *
 * @author Chris de Vreeze
 */
package object print
