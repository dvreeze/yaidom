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

import java.net.URI

/**
 * Abstract API for "context-aware elements".
 *
 * WARNING: CONTEXT PATHS ARE CONSIDERED EXPERIMENTAL!
 *
 * @author Chris de Vreeze
 */
trait ContextAwareApi {

  /**
   * The optional document URI of the containing document, if any
   */
  def docUriOption: Option[URI]

  /**
   * The non-empty context path of this element. The last entry corresponds with this element.
   */
  def contextPath: ContextPath

  /**
   * The parent context path of this element, which always exists because this element always has a non-empty context path.
   */
  def parentContextPath: ContextPath

  /**
   * Returns the optional base URI, computed from the document URI, if any, and the XML base attributes of the
   * ancestors, if any.
   */
  def baseUriOption: Option[URI]
}

object ContextAwareApi {

  /**
   * The `ContextAwareApi` as potential type class trait. Each of the functions takes "this" element as first parameter.
   * Custom element implementations such as W3C DOM or Saxon NodeInfo can thus get this API without any wrapper object costs.
   */
  trait FunctionApi[E] {

    def docUriOption(thisElem: E): Option[URI]

    def contextPath(thisElem: E): ContextPath

    def parentContextPath(thisElem: E): ContextPath

    def baseUriOption(thisElem: E): Option[URI]
  }
}
