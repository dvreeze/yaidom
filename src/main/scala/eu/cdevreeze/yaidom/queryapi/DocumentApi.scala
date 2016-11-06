/*
 * Copyright 2011-2017 Chris de Vreeze
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
 * Minimal API for Documents, having a type parameter for the element type.
 *
 * This is a purely abstract API trait. It can be useful in generic code abstracting over multiple element implementations.
 *
 * @author Chris de Vreeze
 */
trait DocumentApi extends AnyDocumentApi {

  type ThisDoc <: DocumentApi

  /** Returns the document element */
  def documentElement: DocElemType

  /** Returns the optional document URI, wrapped in an Option */
  def uriOption: Option[URI]
}

object DocumentApi {

  /**
   * This document API type, restricting the type members to the passed type parameters.
   *
   * @tparam D The document type itself
   * @tparam E The document element type
   */
  type Aux[D, E] = DocumentApi { type ThisDoc = D; type DocElemType = E }
}
