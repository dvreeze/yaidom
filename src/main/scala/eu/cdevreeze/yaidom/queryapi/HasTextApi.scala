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

package eu.cdevreeze.yaidom.queryapi

/**
 * Trait defining the contract for elements as text containers.
 * Typical element types are both an [[eu.cdevreeze.yaidom.queryapi.ElemLike]] as well as a [[eu.cdevreeze.yaidom.queryapi.HasText]].
 *
 * @author Chris de Vreeze
 */
trait HasTextApi {

  /**
   * Returns the concatenation of the text values of (the implicit) text children, including whitespace and CData.
   * Non-text children are ignored. If there are no text children, the empty string is returned.
   *
   * Therefore, element children are ignored and do not contribute to the resulting text string.
   */
  def text: String

  /** Returns `text.trim`. */
  def trimmedText: String

  /** Returns `XmlStringUtils.normalizeString(text)`. */
  def normalizedText: String
}

object HasTextApi {

  /**
   * The `HasTextApi` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] {

    def text(thisElem: E): String

    def trimmedText(thisElem: E): String

    def normalizedText(thisElem: E): String
  }
}
