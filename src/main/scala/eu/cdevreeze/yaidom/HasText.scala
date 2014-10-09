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

/**
 * Trait defining the contract for elements as text containers.
 * Typical element types are both an [[eu.cdevreeze.yaidom.ElemLike]] as well as a [[eu.cdevreeze.yaidom.HasText]].
 *
 * @author Chris de Vreeze
 */
trait HasText {

  /**
   * Returns the concatenation of the texts of (the implicit) text children, including whitespace and CData.
   * Non-text children are ignored. If there are no text children, the empty string is returned.
   */
  def text: String

  /** Returns `text.trim`. */
  final def trimmedText: String = text.trim

  /** Returns `XmlStringUtils.normalizeString(text)`. */
  final def normalizedText: String = XmlStringUtils.normalizeString(text)
}
