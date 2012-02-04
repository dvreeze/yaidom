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
 * about "ignorable whitespace" (or "element content whitespace"). After all, typical XML input is nicely formatted w.r.t. indenting,
 * but those indents are often not really considered a part of the "data" represented by that XML input, especially if the
 * XML is validated by the parser against a schema. Consider also the possible presence of CDATA sections and entity references,
 * and XML parser configuration options to influence whitespace handling. To make matters even worse, see for example
 * http://www.xmlplease.com/normalized about normalized strings etc.
 *
 * All in all, whitespace handling in XML is pretty complex. This API does not try to make that any easier. It is geared
 * towards "data"-oriented XML rather than free format XML (mixing text with elements). To obtain text, you can always configure the XML
 * parser to combine adjacent text, and treat CData as normal text, or to validate the XML so the parser knows which whitespace
 * is "ignorable". Yet when using methods <code>trimmedText</code> or <code>normalizedText</code>, many of these complexities are
 * somewhat hidden.
 *
 * Typical element-like classes mix in this trait as well as [[eu.cdevreeze.yaidom.ElemLike]].
 *
 * Part of this API was inspired by the JDOM library.
 */
trait HasText[T <: TextLike] {

  /** Returns the text children (including CData), in the correct order */
  def textChildren: immutable.Seq[T]

  /**
   * Returns the concatenation of the texts of text children, including whitespace and CData. Non-text children are ignored.
   * If there are no text children, the empty string is returned.
   */
  final def text: String = {
    val textStrings = textChildren map { t => t.text }
    textStrings.mkString
  }

  /** Returns <code>text.trim</code>. */
  final def trimmedText: String = text.trim

  /** Returns <code>XmlStringUtils.normalizeString(text)</code>. */
  final def normalizedText: String = XmlStringUtils.normalizeString(text)

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
