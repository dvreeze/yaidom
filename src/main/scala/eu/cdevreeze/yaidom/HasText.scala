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

import scala.collection.immutable

/**
 * Supertrait for [[eu.cdevreeze.yaidom.Elem]] and other element-like classes, such as [[eu.cdevreeze.yaidom.xlink.Elem]],
 * offering methods to obtain text children.
 *
 * The only abstract method is <code>textChildren</code>.
 * Based on this method alone, this trait offers a richer API for querying text.
 *
 * Whitespace handling in XML is a rather dodgy area. See for example http://cafeconleche.org/books/xmljava/chapters/ch06s10.html,
 * about "ignorable whitespace". After all, typical XML input is nicely formatted w.r.t. indenting, but those indents are
 * often not really considered a part of the "data" represented by that XML input. Consider also the possible presence of
 * CDATA sections and entity references, and XML parser configuration options to influence whitespace handling.
 * All in all, whitespace handling in XML is pretty complex. This API does not try to make that any easier. It is geared
 * towards "data"-oriented XML rather than free format XML (mixing text with elements). To get the most out of this API,
 * configure the XML parser to combine adjacent text, and if applicable ask for the trimmed text. If adjacent text is indeed
 * combined by the parser, the first text child of an element is likely to be the only text child.
 *
 * Considering these complexities, no effort was done (in yaidom) to offer a method to obtain the node "value".
 *
 * Typical element-like classes mix in this trait as well as [[eu.cdevreeze.yaidom.ElemLike]].
 */
trait HasText[T <: { def text: String }] {

  /** Returns the text children */
  def textChildren: immutable.Seq[T]

  /** Returns the first text child, if any, and None otherwise */
  final def firstTextChildOption: Option[T] = textChildren.headOption

  /** Returns the first text child's value, if any, and None otherwise */
  final def firstTextValueOption: Option[String] = textChildren.headOption map { _.text }

  /** Returns the first text child, if any, and throws an exception otherwise */
  final def firstTextChild: T = firstTextChildOption.getOrElse(sys.error("Missing text child"))

  /** Returns the first text child's value, if any, and throws an exception otherwise */
  final def firstTextValue: String = firstTextValueOption.getOrElse(sys.error("Missing text child"))

  /** Returns the first text child's value, if any, trimming whitespace, and None otherwise */
  final def firstTrimmedTextValueOption: Option[String] = firstTextValueOption map { v => v.trim }

  /** Returns the first text child's value, if any, and throws an exception otherwise */
  final def firstTrimmedTextValue: String = firstTextValue.trim
}
