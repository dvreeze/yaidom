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
 * Supertrait for [[eu.cdevreeze.yaidom.Elem]] and other element-like classes, such as [[eu.cdevreeze.yaidom.expanded.Elem]],
 * offering methods to obtain text from text children.
 *
 * The only abstract method is <code>textChildren</code>.
 * Based on this method alone, this trait offers a richer API for querying text.
 *
 * This API was inspired by the JDOM library. The methods offered by this trait to some extent hide the complexities of whitespace
 * handling in XML. See for example http://cafeconleche.org/books/xmljava/chapters/ch06s10.html, about "ignorable whitespace"
 * (or "element content whitespace"). Or see http://www.xmlplease.com/normalized, w.r.t. normalized strings etc. "Ignorable
 * whitespace" (with and without schema validation), CData sections, entity references, and the corresponding XML parser configurations
 * all contribute to the complexity of whitespace handling in XML. Again, this trait tries to hide some of those complexities.
 *
 * Typical element-like classes mix in this trait as well as [[eu.cdevreeze.yaidom.ElemLike]].
 */
trait TextParentLike[T <: TextLike] {

  /** Returns the text children (including CData), in the correct order */
  def textChildren: immutable.IndexedSeq[T]

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
}
